# Render Pipeline Comparison Report: Minecraft Alpha v1.2.6 vs Thingcraft

## Executive Summary

This report compares the rendering pipeline of the original Minecraft Alpha v1.2.6 (decompiled source in `resources/decomp/`) against the Thingcraft client implementation (`thingcraft-client/src/`). Visual analysis is grounded in side-by-side screenshots and cross-referenced against both codebases.

Overall, the core terrain rendering — lighting model, face shading, fog formula, biome tinting, and water texture animation — is faithfully reproduced. The main visual discrepancies stem from **missing atmospheric passes** (sky dome, celestial bodies, sunrise/sunset) and **missing HUD elements** (held item, health bar, item icons in hotbar).

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

- **Missing passes in Thingcraft:** Entities, particles, block outline, mining progress, held item, screen overlays (fire/underwater/hurt).
- **Sky pass is simplified:** Flat color quad vs multi-component sky dome. See §2.
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

| # | Feature | Impact | Notes |
|---|---------|--------|-------|
| 1 | **No sky dome / horizon gradient** | Moderate | Sky is uniform vs graded in MC Alpha |
| 2 | **No sun/moon** | Moderate | Missing celestial bodies |
| 3 | **No stars** | Low | Only visible at night |
| 4 | **No sunrise/sunset colors** | Moderate | No warm gradient at dawn/dusk |
| 5 | **Always-cutout leaves** | Moderate | Leaves look holey vs MC Alpha's opaque fast-mode leaves |
| 6 | **No block-in-hand** | High | Most immediately noticeable missing feature |
| 7 | **No health hearts** | Moderate | Missing survival HUD |
| 8 | **No item textures in hotbar** | Moderate | Gray rectangles instead of item icons |
| 9 | **No fog brightness modulation** | Low | Subtle darkening in shadow areas missing |
| 10 | **No underwater/lava fog** | Low | Different fog mode when submerged |

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

## 13. Recommendations (Priority Order)

1. **Implement block-in-hand rendering** — Highest visual impact. Requires item mesh generation, separate depth space, and swing animation. Reference: `ItemInHandRenderer.java:40–204`.

2. **Add fast-mode opaque leaves** — Add a graphics toggle that renders leaf blocks without alpha discard, matching MC Alpha's fast-mode appearance. Simple: skip the `discard` in the fragment shader when the block ID is a leaf type and fast graphics is enabled.

3. **Implement sky dome with horizon gradient** — Replace flat sky triangle with a vertex-colored hemisphere. Apply the existing fog formula to the dome vertices to get natural horizon blending. Reference: `WorldRenderer.java:532–624`.

4. **Add sun and moon** — Two textured quads rotated by `timeOfDay × 360°`. Reference: `WorldRenderer.java:584–607`.

5. **Render item textures in hotbar slots** — Sample from the terrain atlas to draw 2D item icons in each hotbar slot.

6. **Add health bar** — Render heart textures above the hotbar using the same HUD pipeline.

7. **Add sunrise/sunset gradient** — Gradient fan primitive at the horizon during dawn/dusk. Reference: `WorldRenderer.java:559–581`.

8. **Fog brightness modulation** — Sample light level at player position and modulate fog color. Low priority since the effect is subtle.
