# Render Pipeline Comparison Report: Minecraft Alpha v1.2.6 vs Thingcraft

## Executive Summary

This report compares the rendering pipeline of the original Minecraft Alpha v1.2.6 (decompiled source in `resources/decomp/`) against the Thingcraft client implementation (`thingcraft-client/src/`). Visual analysis is grounded in side-by-side screenshots and cross-referenced against both codebases.

Overall, the core terrain rendering — lighting model, face shading, fog formula, biome tinting, and water texture animation — is faithfully reproduced. The main visual discrepancies stem from **missing atmospheric passes** (sky dome, celestial bodies, sunrise/sunset) and **missing HUD elements** (held item, health bar, item icons in hotbar).

> Post-report implementation update: leaf rendering now supports both Alpha-style fancy cutout and fast opaque modes (`THINGCRAFT_FANCY_GRAPHICS`), so item #2 in recommendations has been addressed.

---

## 1. Render Pass Order

### Minecraft Alpha (GameRenderer.java:319–412)

| # | Pass | Details |
|---|------|---------|
| 1 | Clear | `glClearColor` to computed fog color |
| 2 | Sky dome | Light sky dome + sunrise gradient + sun/moon + stars + dark sky dome (if `viewDistance < 2`) |
| 3 | Opaque terrain | Layer 0 chunk display lists, depth write ON |
| 4 | Entities | Mobs, items, boats, etc. |
| 5 | Lit particles | |
| 6 | Unlit particles | |
| 7 | Block outline | Wireframe on targeted block |
| 8 | Transparent terrain | Layer 1 (water, ice, leaves in fancy), blend ON, cull OFF; fancy mode does a depth-only pre-pass |
| 9 | Clouds | With fog, alpha blending |
| 10 | Depth clear + Hand | `glClear(DEPTH)` then render held item |
| 11 | Screen effects | Underwater overlay, fire overlay, hurt tint |
| 12 | GUI/HUD | Hearts, hotbar, crosshair, debug text |

### Thingcraft (renderer.rs:1350–1497)

| # | Pass | Details |
|---|------|---------|
| 1 | Clear | Screen cleared to fog color |
| 2 | Sky quad | Full-screen triangle, uniform color (if `render_sky == true`) |
| 3 | Opaque terrain | Section meshes, depth write ON |
| 4 | Transparent terrain | Water/liquid, depth write OFF, alpha blend |
| 5 | Clouds | Fog-blended, alpha blend |
| 6 | Debug lines | Optional chunk borders |
| 7 | HUD | Crosshair + hotbar rectangles (separate render pass, no depth) |

### Differences

- **Missing passes in Thingcraft:** Entities, particles, block outline, mining progress, held item, screen overlays (fire/underwater/hurt). See §13 for block outline detail, §9 for held item.
- **Sky pass is simplified:** Flat color quad vs multi-component sky dome (light dome + dark dome + sunrise fan + sun/moon + stars). See §2.
- **No depth-only pre-pass** for transparent geometry in fancy mode.
- **Transparent pass does not disable face culling.** MC Alpha sets `glDisable(GL_CULL_FACE)` for the transparent pass so water faces are visible from both sides. Thingcraft's transparent pipeline *also* disables culling — verified in `renderer.rs` pipeline descriptor.

---

## 2. Sky Rendering

### Minecraft Alpha (WorldRenderer.java:532–624)

The sky is a **multi-layer dome**:

1. **Light sky dome** — display list sphere colored with biome sky color, with OpenGL fog applied (fog start=0, fog end=80% render distance). The fog produces a natural **color gradient toward the horizon**.
2. **Sunrise/sunset fan** — 16-vertex gradient fan at the horizon, using warm colors from `Dimension.getSunriseColor()`. Only visible near dawn/dusk.
3. **Sun** — Textured 30×30 quad at +Y=100, rotated by `timeOfDay * 360°`.
4. **Moon** — Textured 20×20 quad at −Y=100, opposite the sun.
5. **Stars** — Randomly placed quads, brightness fades with daylight.
6. **Dark sky dome** — Below-horizon dome at Y=−16, colored at `(skyR × 0.2 + 0.04, skyG × 0.2 + 0.04, skyB × 0.6 + 0.1)`. Prevents bright color below horizon.

### Thingcraft (renderer.rs:2321–2344)

The sky is a **single full-screen triangle** that outputs a uniform color:

```wgsl
@fragment
fn fs_main() -> @location(0) vec4<f32> {
    return vec4<f32>(sky.color, 1.0);
}
```

Sky color is correctly computed per-biome temperature via `alpha_biome_sky_rgb()` using HSV conversion (app.rs:1241–1246), matching MC Alpha's `Biome.getSkyColor()`.

### Visual Impact

| Feature | MC Alpha | Thingcraft | Visible Difference |
|---------|----------|------------|--------------------|
| Horizon gradient | Fog fades sky dome toward fog color | Flat uniform sky | **YES** — MC Alpha sky is brighter at zenith, fades toward horizon; Thingcraft is uniform |
| Below-horizon sky | Dark blue-ish dome | Same flat color as above | **YES** — Looking down shows different sky tint in MC Alpha |
| Sun/Moon | Textured rotating quads | Not rendered | **YES** — No celestial bodies |
| Stars | Visible at night | Not rendered | **YES** — No stars at night |
| Sunrise/sunset | Warm gradient fan | Not present | **YES** — No color gradient at dawn/dusk |

**Severity: MODERATE** — During daytime the sky difference is subtle (mainly the missing horizon gradient), but at sunrise/sunset/night the difference is dramatic.

---

## 3. Fog

### Formula Comparison

Both implementations use **linear fog** with identical parameters:

| Parameter | MC Alpha | Thingcraft | Match? |
|-----------|----------|------------|--------|
| Mode | `GL_LINEAR` (9729) | Linear in fragment shader | Yes |
| fog_start | `renderDistance × 0.25` | `render_distance_blocks × 0.25` | Yes |
| fog_end | `renderDistance` | `render_distance_blocks` | Yes |
| Sky fog_start | `0.0` | N/A (flat sky) | — |
| Sky fog_end | `renderDistance × 0.8` | N/A (flat sky) | — |

### Fog Color

MC Alpha (GameRenderer.java:452–490, Dimension.java:101–113):
```java
// Base fog RGB from dimension
R = 0.7529412 × (daylight × 0.94 + 0.06)
G = 0.8470588 × (daylight × 0.94 + 0.06)
B = 1.0       × (daylight × 0.91 + 0.09)

// Mix toward sky color by view distance
f = 1.0 - (1.0 / (4 - viewDistance))^0.25
fogColor += (skyColor - fogColor) × f

// Multiply by brightness at player position
fogColor *= fogBrightness
```

Thingcraft (app.rs:1184–1232):
```rust
// Same base fog formula
fn alpha_fog_color(time_of_day: f32) -> [f32; 3] {
    [0.752_941_2 * (f * 0.94 + 0.06),
     0.847_058_83 * (f * 0.94 + 0.06),
     1.0 * (f * 0.91 + 0.09)]
}

// Same sky-fog mixing
fn alpha_clear_fog_color(fog, sky, view_distance) -> [f32; 3] {
    sky_mix = 1.0 - (1.0 / (4.0 - vd)).powf(0.25);
    fog + (sky - fog) * sky_mix
}
```

### Differences

1. **Missing fog brightness modulation** — MC Alpha multiplies the final fog color by an interpolated brightness value sampled at the player's position (`fogBrightness`). This creates a subtle fog darkening when the player is in shadow or underground. Thingcraft omits this step.

2. **`DEFAULT_FOG_COLOR` fallback** — Thingcraft defines `const DEFAULT_FOG_COLOR: [f32; 3] = [0.04, 0.07, 0.12]` (renderer.rs:18), a very dark blue-gray used before the first frame's fog color is computed. MC Alpha always computes fog color fresh each frame. If any code path uses this fallback after initialization, it would look wrong.

3. **No underwater/lava fog** — MC Alpha switches to exponential fog when submerged: water fog density 0.1 with color `(0.02, 0.02, 0.2)`, lava fog density 2.0 with color `(0.6, 0.1, 0.0)`. Thingcraft does not implement this.

4. **No NV_fog_distance extension** — MC Alpha uses `GL_NV_fog_distance` for eye-radial fog when available. Thingcraft's per-fragment distance fog in the shader achieves the same effect (radial distance, not planar).

**Severity: LOW** — The core fog looks correct in the screenshots. The missing brightness modulation and underwater fog are edge cases.

---

## 4. Lighting & Brightness

### Brightness Curve

Both implementations use the identical non-linear brightness mapping:

```
g = 1.0 - level / 15.0
brightness = (1.0 - g) / (g × 3.0 + 1.0) × 0.95 + 0.05
```

MC Alpha (Dimension.java:42–48): Precomputed into `brightnessTable[16]`.
Thingcraft (renderer.rs WGSL): Computed per-fragment via `alpha_brightness()`.

**Result: IDENTICAL.**

### Face Shading Multipliers

| Face | MC Alpha (BlockRenderer.java:834–837) | Thingcraft (mesh.rs:1321–1331) | Match? |
|------|---------------------------------------|-------------------------------|--------|
| Top (+Y) | 1.0 | 1.0 | Yes |
| Bottom (−Y) | 0.5 | 0.5 | Yes |
| North/South (±Z) | 0.8 | 0.8 | Yes |
| East/West (±X) | 0.6 | 0.6 | Yes |

**Result: IDENTICAL.**

### Effective Light Computation

MC Alpha: `max(blockLight, max(skyLight - ambientDarkness, 0))`
Thingcraft (WGSL):
```wgsl
let day_sky = max(input.sky_light - camera.ambient_darkness, 0.0);
let effective = max(input.block_light, day_sky);
```

**Result: IDENTICAL.**

### Ambient Darkness

MC Alpha: Derived from celestial angle, range 0–11.
Thingcraft (app.rs:1178–1182): Same formula: `(g × 11.0) as u8` where `g` is clamped inverse cosine of time angle.

**Result: IDENTICAL.**

### Missing: Smooth Lighting / Ambient Occlusion

Neither MC Alpha v1.2.6 nor Thingcraft implements AO or smooth lighting. **Correct parity.**

---

## 5. Water / Liquid Rendering

### Water Texture Animation

Both implementations use the same procedural animation system:

**WaterSprite (top surface):**

| Property | MC Alpha (WaterSprite.java) | Thingcraft (mesh.rs `patch_procedural_tiles`) |
|----------|-----------------------------|----------------------------------------------|
| Grid size | 16×16 pixels | 16×16 pixels |
| Diffusion | 3-cell average / 3.3 | 3-cell average / 3.3 |
| Heat decay | 0.1 per frame | 0.1 per frame |
| Heat chance | 5% random | 5% random |
| Heat amount | 0.5 | 0.5 |
| Color R | 32 + h²×32 | 32 + h²×32 |
| Color G | 50 + h²×64 | 50 + h²×64 |
| Color B | 255 | 255 |
| Color A | 146 + h²×50 | 146 + h²×50 |

**WaterSideSprite (side faces):**

| Property | MC Alpha (WaterSideSprite.java) | Thingcraft |
|----------|--------------------------------|------------|
| Diffusion | Directional / 3.2 | Same |
| Heat decay | 0.3 per frame | 0.3 |
| Heat chance | 20% random | 20% |
| Scroll | Animated Y offset by tick | Same |
| Color | Same as top | Same |

**Result: IDENTICAL formula.**

### Water Vertex Colors

MC Alpha: `tesselator.color(faceScale × brightness, faceScale × brightness, faceScale × brightness)` — grayscale based on light level and face direction. The blue color comes entirely from the texture.

Thingcraft: Vertex tint is `[255, 255, 255]` with alpha 255. Light and face shading are packed into `light_data` and applied per-fragment. The blue comes from the texture.

**Result: Functionally equivalent.** The same visual output via different application points (vertex color vs fragment shader).

### Water Blend Mode

| Property | MC Alpha | Thingcraft |
|----------|----------|------------|
| Blend function | `GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA` | `wgpu::BlendState::ALPHA_BLENDING` (equivalent) |
| Depth write | Not explicitly disabled for water in pass 1 | Disabled (`depth_write_enabled: false`) |
| Face culling | Disabled (`glDisable(GL_CULL_FACE)`) | Disabled (pipeline `cull_mode: None`) |

**Note:** MC Alpha enables `glDepthMask(true)` after the transparent pass (line 390), implying depth write was potentially active during transparent rendering. Thingcraft explicitly disables depth write for the transparent pipeline. This is a minor correctness difference — disabling depth write for transparent geometry is the more correct approach and prevents water faces from occluding each other incorrectly.

### Water Height

MC Alpha: Source = full block face, flowing = `1.0 - (metadata + 1) / 9`.
Thingcraft: `SOURCE_HEIGHT = 8.0 / 9.0 ≈ 0.889`, flowing = `1.0 - (meta + 1) / 9.0`.

**Difference:** MC Alpha source blocks render with corners averaged to full height (1.0) when liquid is above, but for a standalone source the corners vary by neighbor averaging. Thingcraft source blocks cap at 8/9. This means **Thingcraft water surface sits slightly lower** (by 1/9 of a block ≈ 1.8 pixels at default resolution) compared to MC Alpha.

### Visual Impact

From the screenshots, water color appears correct. The subtle purple/violet tint visible on water side faces in Thingcraft is **expected and correct** — it's the result of the 0.6× face shading multiplier applied to the blue-dominated water texture (R:32–64, G:50–114, B:255), producing a darker blue-purple appearance on east/west faces.

**Severity: LOW** — Water rendering is accurate. The 1/9 height difference is barely noticeable.

---

## 6. Cloud Rendering

### Fast Clouds Comparison

| Property | MC Alpha (WorldRenderer.java:626–676) | Thingcraft (renderer.rs) | Match? |
|----------|---------------------------------------|--------------------------|--------|
| Height | `120.0 - playerY + 0.33` (world Y = 120.33) | `CLOUD_LAYER_HEIGHT = 120.33` | Yes |
| Alpha | 0.8 | 0.8 | Yes |
| UV scale | 4.8828125E-4 (1/2048) | 0.00048828125 (1/2048) | Yes |
| Grid tile size | 32 blocks | `CLOUD_TILE_SIZE = 32.0` | Yes |
| Grid extent | ±256 blocks (16 tiles of 32) | ±128 (8 tiles of 32 = 16×16) | **Smaller** |
| Scroll speed | `ticks × 0.03` along X | `ticks × 0.03` along X | Yes |
| Scroll axis | X (only) | X (only, `uv_offset[1] = 0.0`) | Yes |
| Color | `getCloudColor()`: daylight-tinted white | `alpha_cloud_color()`: same formula | Yes |
| Blend mode | `SRC_ALPHA, ONE_MINUS_SRC_ALPHA` | `ALPHA_BLENDING` (equivalent) | Yes |
| Fog applied | Yes (GL fog during cloud pass) | Yes (per-fragment in cloud shader) | Yes |
| Texture filter | GL default (linear) | Linear mag/min, nearest mip | Close |

### Differences

1. **Grid extent** — MC Alpha: 16 tiles × 32 = 512 blocks total width. Thingcraft: 16 tiles × 32 = 512 blocks (same). Actually both match: MC Alpha iterates from `-i*j` to `+i*j` where `i=32, j=8`, giving -256 to +256, same as Thingcraft's ±8 tiles of 32 blocks.

2. **Fancy clouds** — MC Alpha has a separate `renderFancyClouds()` with 3D thick clouds (height at 108.33, thickness 4.0, top/bottom/side coloring). Thingcraft only implements fast-style flat clouds. Not an issue since the MC Alpha screenshot also shows fast clouds.

**Severity: NEGLIGIBLE** — Cloud rendering is visually very close.

---

## 7. Foliage Tinting

### Biome Tint System

MC Alpha uses biome-based color maps for grass and leaves:

```java
// Biome.java
int color = Block.getColor(world, x, y, z);
float r = (color >> 16 & 0xFF) / 255.0f;
// ... applied per vertex
```

Thingcraft uses a nearly identical system with 256×256 PNG color maps sampled by temperature/humidity:

```rust
// mesh.rs: resolve_face_tint()
match registry.biome_tint_kind(block_id, face_offset) {
    BiomeTintKind::Grass => chunk.grass_tint_at(local_x, local_z),
    BiomeTintKind::Foliage => chunk.foliage_tint_at(local_x, local_z),
    BiomeTintKind::None => registry.face_tint_rgb(block_id, face_offset),
}
```

### Tinted Faces

| Block | Face | MC Alpha | Thingcraft | Match? |
|-------|------|----------|------------|--------|
| Grass | Top only | Yes, biome color | `(GRASS_ID, 1) => Grass` | Yes |
| Grass | Sides | No tint (gray texture) | `_ => None` | Yes |
| Oak Leaves | All faces | Biome foliage color | `(OAK_LEAVES_ID, _) => Foliage` | Yes |

### Fallback Colors

Thingcraft defines fallback tints when color map PNGs are unavailable:
- Grass: `[126, 201, 86]` — reasonable default green
- Foliage: `[87, 174, 47]` — reasonable default dark green

**Severity: NEGLIGIBLE** — Foliage tinting appears correct in screenshots.

---

## 8. Leaf Rendering / Alpha Test

### MC Alpha

MC Alpha uses OpenGL `GL_ALPHA_TEST` for cutout transparency. The render mode depends on graphics settings:

- **Fast graphics:** Leaves are rendered in the **opaque pass** (layer 0). They appear as solid blocks with no transparency — leaf texture alpha is ignored.
- **Fancy graphics:** Leaves go through the **transparent pass** (layer 1) with alpha test enabled, creating cutout holes.

The MC Alpha screenshot shows **fast-style rendering** (flat clouds, opaque-looking leaves with dense canopy).

### Thingcraft

Thingcraft always renders leaves in the **opaque pass** with a fragment-shader alpha discard:

```wgsl
if (texel.a <= 0.1) {
    discard;
}
```

This means leaves **always** have cutout transparency, equivalent to MC Alpha's "fancy" mode.

### Visual Impact

In the screenshots:
- **MC Alpha:** Leaves appear as dense, opaque masses — the hallmark of "fast" leaf rendering.
- **Thingcraft:** Leaves show the checkerboard/random cutout pattern clearly — you can see through them.

**Severity: MODERATE** — This is a noticeable visual difference. Thingcraft should ideally support a "fast" leaf mode where leaves render as opaque blocks (no alpha discard for leaf block IDs).

---

## 9. Block-in-Hand Rendering

### MC Alpha (ItemInHandRenderer.java:40–344)

Renders the held item/block in first person at the bottom-right of the screen:

- Depth buffer cleared before rendering (separate depth space)
- Swing animation with `sin(attackProgress × π)`
- Position: `(0.7×scale, -0.65-lift, -0.9×scale)`
- Rotation: 50° Y, 335° Z
- 3D blocks rendered via `blockRenderer.renderAsItem()`
- Brightness matched to player position light level
- Screen effects: underwater overlay, fire overlay, damage tint

### Thingcraft

**Not implemented.** No held item or block-in-hand rendering exists.

### Visual Impact

Clearly visible in the screenshots — MC Alpha shows a sand block held in hand, Thingcraft shows nothing.

**Severity: HIGH** — This is one of the most immediately noticeable missing features in any gameplay screenshot.

---

## 10. HUD / GUI

### MC Alpha

- **Crosshair:** Small "+" at screen center
- **Health bar:** 10 hearts (texture-based, bouncing animation when damaged)
- **Hotbar:** 9 slots with item textures, stack counts, selection highlight
- **Version text:** "Minecraft Alpha v1.2.6" in top-left
- **Debug screen:** F3 overlay with coordinates, FPS, etc.

### Thingcraft (app.rs:578–591, renderer.rs HUD shader)

- **Crosshair:** White "+" at center, 10px arms, 2px thickness, 80% opacity — **correct**
- **Hotbar:** 9 colored rectangles, 40px each
  - Unselected: `[0.15, 0.15, 0.15, 0.7]` (dark gray)
  - Selected: `[0.4, 0.4, 0.4, 0.85]` (lighter gray)
  - Border: `[0.5, 0.5, 0.5, 0.6]` at 2px
- **Missing:** Health hearts, item textures in hotbar slots, stack count text, armor bar, version text

### Visual Impact

The hotbar in Thingcraft is placeholder-quality — gray boxes with no item icons, no stack counts rendered as text, no health display.

**Severity: MODERATE** — Functional but visually incomplete compared to MC Alpha.

---

## 11. Texture Filtering

| Context | MC Alpha | Thingcraft | Match? |
|---------|----------|------------|--------|
| Terrain atlas | `GL_NEAREST` (pixelated) | `mag: NEAREST, min: NEAREST, mip: NEAREST` | Yes |
| Clouds | `GL_LINEAR` (default) | `mag: LINEAR, min: LINEAR, mip: NEAREST` | Yes |
| GUI/HUD | N/A (2D) | N/A (solid-color quads) | — |

**Result: IDENTICAL** for the elements that exist.

---

## 12. Summary of Visual Differences (from screenshots)

### Confirmed Differences

| # | Feature | Impact | Status | Notes |
|---|---------|--------|--------|-------|
| 1 | **No sky dome / horizon gradient** | Moderate | TODO | Sky is uniform vs graded in MC Alpha |
| 2 | **No sun/moon** | Moderate | TODO | Missing celestial bodies |
| 3 | **No stars** | Low | TODO | Only visible at night |
| 4 | **No sunrise/sunset colors** | Moderate | TODO | No warm gradient at dawn/dusk |
| 5 | ~~Always-cutout leaves~~ | ~~Moderate~~ | **DONE** | `THINGCRAFT_FANCY_GRAPHICS` toggle added |
| 6 | **No block-in-hand** | High | TODO | Most immediately noticeable missing feature |
| 7 | **No health hearts** | Moderate | TODO | Missing survival HUD |
| 8 | **No item textures in hotbar** | Moderate | TODO | Gray rectangles instead of item icons |
| 9 | ~~No fog brightness modulation~~ | ~~Low~~ | **DONE** | `alpha_fog_brightness_target()` + `alpha_apply_fog_brightness()` |
| 10 | **No underwater/lava fog** | Low | TODO | Different fog mode when submerged |
| 11 | **No block outline** | Moderate | TODO | No wireframe on targeted block |

### Correctly Implemented (Matching MC Alpha)

| Feature | Status |
|---------|--------|
| Fog formula (linear, distance-based) | Correct |
| Fog color (dimension + sky mix by view distance) | Correct |
| Fog start/end distances | Correct |
| Brightness curve (non-linear, 0.05 min) | Correct |
| Face shading multipliers (1.0/0.5/0.8/0.6) | Correct |
| Effective light = max(block, sky - darkness) | Correct |
| Ambient darkness from time-of-day | Correct |
| Water texture animation (both top and side) | Correct |
| Water color formula (R:32+h²×32, G:50+h²×64, B:255) | Correct |
| Water alpha blending mode | Correct |
| Cloud height (120.33), alpha (0.8), UV scale (1/2048) | Correct |
| Cloud scroll speed (ticks × 0.03 on X axis) | Correct |
| Cloud color tinting by daylight | Correct |
| Biome-based grass/foliage tinting | Correct |
| Grass tint: top face only | Correct |
| Oak leaf tint: all faces | Correct |
| Nearest-neighbor terrain filtering | Correct |
| Crosshair rendering | Correct |
| No smooth lighting / AO (matching Alpha) | Correct |
| Render distance settings (256/128/64/32) | Correct |

---

## 13. Block Outline Rendering

### MC Alpha (WorldRenderer.java:879–938)

When the player looks at a block, MC Alpha draws a **wireframe outline** around it:

- **Color:** `rgba(0.0, 0.0, 0.0, 0.4)` — black at 40% opacity
- **Line width:** 2.0 pixels
- **Inflation:** The block shape is grown by `0.002` in each axis to prevent z-fighting
- **Blend mode:** `SRC_ALPHA, ONE_MINUS_SRC_ALPHA`
- **Depth write:** Disabled during outline rendering
- **Geometry:** Two horizontal loops (bottom face ring + top face ring) drawn as `GL_LINE_STRIP`, plus 4 vertical edges drawn as `GL_LINES`

The outline uses the block's collision/outline shape (`Block.getOutlineShape()`), translated relative to camera position.

### Thingcraft

**Not implemented.** Thingcraft has a `debug_line_pipeline` (renderer.rs) used for chunk border debug visualization, which could be extended for block outlines. The raycast system already computes the targeted block (`raycast_first_solid_block` in app.rs:875), so the hit position data is available.

### Implementation Spec

1. **Reuse `debug_line_pipeline`** — Same vertex format (position + color), same blend mode
2. **Generate 12-edge wireframe** from the targeted block's AABB, inflated by `0.002`
3. **Vertex color:** `[0.0, 0.0, 0.0, 0.4]`
4. **Draw after transparent pass**, before clouds (matching MC Alpha pass order)
5. **Depth write OFF**, depth test ON (so outline is hidden behind solid geometry but doesn't write to depth)

**Severity: MODERATE** — The block outline is critical gameplay feedback for knowing which block you're targeting.

---

## 14. Recommendations (Priority Order)

### Completed

| # | Feature | Status |
|---|---------|--------|
| ~~2~~ | **Fast-mode opaque leaves** | **Done** — `THINGCRAFT_FANCY_GRAPHICS` toggle switches leaves between fancy cutout and fast opaque. |
| ~~8~~ | **Fog brightness modulation** | **Done** — `alpha_fog_brightness_target()` samples light at player position, smoothly interpolates via `last_fog_brightness` / `fog_brightness`, and `alpha_apply_fog_brightness()` modulates fog color per frame (app.rs:386–583). |

### Remaining (with implementation detail)

#### 1. Block-in-Hand Rendering *(HIGH priority)*

Highest visual impact single missing feature.

**MC Alpha reference:** `ItemInHandRenderer.java:40–344`

**Required components:**
- **Separate depth space:** Clear depth buffer before hand rendering (MC Alpha: `glClear(GL_DEPTH_BUFFER_BIT)`)
- **3D block mesh:** For block items, render a small cube via `blockRenderer.renderAsItem()` using the terrain atlas
- **Transform chain:**
  ```
  translate(-swing_n * 0.4, sin(sqrt(swing) * PI * 2.0) * 0.2, -swing_l * 0.2)
  translate(0.7 * scale, -0.65 * scale - (1.0 - hand_height) * 0.6, -0.9 * scale)
  rotate_y(45°)
  // attack animation:
  rotate_y(-sin(swing² * PI) * 20°)
  rotate_z(-sin(sqrt(swing) * PI) * 20°)
  rotate_x(-sin(sqrt(swing) * PI) * 80°)
  scale(0.4)
  ```
  Where `swing = player.getAttackAnimationProgress(tickDelta)`, `scale = 0.8`, `hand_height` lerps 0→1 on item change
- **Lighting:** `world.getBrightness(floor(player.x), floor(player.y), floor(player.z))` applied as vertex color tint
- **Swing trigger:** `hand_height = 0.0` on block use/break, lerps back toward 1.0 at rate 0.4/tick
- **Item change animation:** When held item changes, `hand_height` drops toward 0.0, item swaps at `hand_height < 0.1`, then rises back

**Thingcraft approach:** Add a new render pass after clouds with its own depth clear. Generate a small indexed cube mesh from the selected hotbar block's atlas UVs. Apply the transform chain as a separate view-projection matrix uniform. The existing `terrain_atlas` bind group can be reused.

#### 3. Sky Dome with Horizon Gradient *(MODERATE priority)*

**MC Alpha reference:** `WorldRenderer.java:120–160, 532–624`

**Light sky dome geometry:**
- Flat grid of quads at `Y = +16.0`, tiled from `-64*k` to `+64*k` where `k = 256/64 + 2 = 6` → extends ±384 blocks
- Single uniform color (biome sky color) — the **horizon gradient comes from GL fog**, not vertex coloring
- GL fog settings for sky: `fog_start = 0.0`, `fog_end = renderDistance × 0.8`
- This causes far dome vertices to blend toward `fogColor`, while overhead vertices remain sky color → natural gradient

**Dark sky dome:**
- Same grid geometry but at `Y = -16.0`
- Color: `(skyR × 0.2 + 0.04, skyG × 0.2 + 0.04, skyB × 0.6 + 0.1)`
- Also has fog applied (same settings)

**Thingcraft approach:** Replace the full-screen triangle with two dome meshes (flat grids at Y=+16 and Y=-16, centered on camera). In the sky shader, apply per-fragment fog: `fog_t = clamp(distance / (render_dist * 0.8), 0.0, 1.0)`, mix dome color → fog color. The sky color and dark dome color are already computed on CPU.

#### 4. Sun and Moon *(MODERATE priority)*

**MC Alpha reference:** `WorldRenderer.java:584–607`

**Sun:**
- Textured quad, 30×30 units, centered at `(0, +100, 0)` relative to player
- Rotated by `timeOfDay × 360°` around X axis (east-west arc)
- Texture: `/terrain/sun.png` (32×32 PNG)
- Blend mode: `GL_ONE, GL_ONE` (additive)
- UV: full `(0,0)→(1,1)`

**Moon:**
- Textured quad, 20×20 units, centered at `(0, -100, 0)` relative to player (opposite sun)
- Same rotation as sun (they share the push/pop matrix)
- Texture: `/terrain/moon.png` (32×32 PNG)
- Blend mode: `GL_ONE, GL_ONE` (additive)

**Thingcraft approach:** Load sun/moon textures as separate GPU textures. Create a small 4-vertex quad pipeline with additive blending. Compute rotation matrix from `timeOfDay * 2π` around X, translate by ±100 on Y. Render after sky dome, before terrain. Disable depth write.

#### 5. Stars *(LOW priority)*

**MC Alpha reference:** `WorldRenderer.java:161–200, 609–613`

**Star generation (deterministic, seed `10842`):**
- 1500 star candidates; reject if `d²+e²+f² >= 1.0` or `< 0.01`
- Normalize position, scale to radius 100
- Each star is a 4-vertex quad oriented via `atan2` spherical coordinates, rotated by random angle
- Star size: `0.25 + random * 0.25` (in world units at distance 100)

**Star brightness:** `getStarBrightness(tickDelta)`:
```
g = 1.0 - (cos(timeOfDay * 2π) * 2.0 + 0.75)
g = clamp(g, 0.0, 1.0)
brightness = g² * 0.5
```
Stars are invisible during daytime (`brightness ≈ 0`), fully visible at midnight (`brightness ≈ 0.5`).

**Color:** `glColor4f(brightness, brightness, brightness, brightness)` — uniform white, alpha-faded

**Thingcraft approach:** Generate star vertex buffer once at init from seed `10842`. Render with alpha blending, same rotation as sun/moon. Pass `star_brightness` as a uniform to the fragment shader.

#### 6. Sunrise/Sunset Gradient *(MODERATE priority)*

**MC Alpha reference:** `WorldRenderer.java:559–581`, `Dimension.java:82–98`

**Visibility condition:** `getSunriseColor()` returns non-null when:
```
g = cos(timeOfDay * 2π)
|g| < 0.4  (i.e. near dawn/dusk)
```

**Color computation:**
```
i = (g / 0.4) * 0.5 + 0.5     // 0→1 fade
j = 1.0 - (1.0 - sin(i * π)) * 0.99
j = j²
R = i * 0.3 + 0.7              // warm orange-red
G = i² * 0.7 + 0.2
B = i² * 0.0 + 0.2
A = j                           // strong near center, fades at edges
```

**Geometry:** 16-vertex fan (`GL_TRIANGLE_FAN`) at the horizon:
- Center vertex at `(0, 100, 0)`, color = `(R, G, B, A)`
- 16 rim vertices at `(sin(θ) × 120, cos(θ) × 120, -cos(θ) × 40 × A)`, color = `(R, G, B, 0.0)` (alpha 0 at rim → smooth fade)
- Rotated 90° around X, then 0° or 180° around Z depending on whether `timeOfDay > 0.5`

**Thingcraft approach:** Add a small fan mesh (17 vertices: 1 center + 16 rim). Update vertex colors each frame from `getSunriseColor()` computation. Render after sky dome, before sun/moon, with alpha blending and depth write off.

#### 7. Block Outline Rendering *(MODERATE priority)*

Already detailed in §13 above. Reuse existing `debug_line_pipeline`. Generate 12-edge wireframe from targeted block AABB inflated by 0.002. Color `[0, 0, 0, 0.4]`.

#### 8. Item Textures in Hotbar *(MODERATE priority)*

**MC Alpha:** Each hotbar slot shows the item's sprite sampled from `/terrain.png` (for blocks) or `/gui/items.png` (for items).

**Thingcraft approach:** Extend the HUD pipeline to support textured quads. Sample the terrain atlas at the correct UV coordinates for each block in the hotbar. For block items, the sprite is the block's face texture (typically the front/side face). The terrain atlas is already loaded; add a second HUD pipeline variant that binds the atlas texture and uses UV coordinates instead of solid color.

#### 9. Health Bar *(MODERATE priority)*

**MC Alpha:** 10 heart icons rendered from `/gui/icons.png` above the hotbar. Each heart is 9×9 pixels. Full hearts, half hearts, and empty heart outlines are separate sprites.

**Thingcraft approach:** Load the GUI icons texture. Render 10 heart-sized quads above the hotbar using the textured HUD pipeline. Track player health as an ECS component. Bounce animation: when damaged, each heart offset Y by `random * 2` for a few frames.

#### 10. Underwater/Lava Fog *(LOW priority)*

**MC Alpha reference:** `GameRenderer.java:452–490`

When player head is submerged:
- **Water:** Fog mode = `GL_EXP` (exponential), density = `0.1`, color = `(0.02, 0.02, 0.2)`
- **Lava:** Fog mode = `GL_EXP`, density = `2.0`, color = `(0.6, 0.1, 0.0)`

**Thingcraft approach:** Check if camera Y is below water surface in the current chunk. Switch fog formula in the terrain shader from linear to exponential: `fog_t = 1.0 - exp(-density * distance)`. Pass fog mode + density as uniforms alongside existing fog start/end.
