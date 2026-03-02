# Liquid System Comparison Report
## ThingCraft (Rust/wgpu) vs Minecraft Alpha 1.2.6 (Java)

---

## 1. Overview

### Minecraft Alpha 1.2.6
Minecraft's liquid system is built on a class hierarchy: `LiquidBlock` (base) → `FlowingLiquidBlock` (flow propagation) and `LiquidSourceBlock` (source behavior). Flow is driven by scheduled tick updates on the main thread. Rendering uses a dedicated tessellation path (`tesselateLiquid()`) with per-corner height interpolation, flow-angle-rotated textures, and procedurally animated water/lava sprites via `WaterSprite`/`LavaSprite` classes. The system is entirely single-threaded.

### ThingCraft
ThingCraft reimplements the liquid system within its ECS-based architecture. Liquid flow is managed by a `FluidScheduler` with priority queues and budgeted per-frame processing. Blocks use the same ID scheme (water 8/9, lava 10/11) with 4-bit metadata for depth state. Rendering uses a separate transparent pass with alpha blending. The system is designed for concurrency, with budget-capped tick processing that prevents frame stalls.

---

## 2. Block Definitions & IDs

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Flowing Water ID | 8 (`FlowingLiquidBlock`) | 8 (`FLOWING_WATER_ID`) |
| Water Source ID | 9 (`LiquidSourceBlock`) | 9 (`WATER_ID`) |
| Flowing Lava ID | 10 (`FlowingLiquidBlock`) | 10 (`FLOWING_LAVA_ID`) |
| Lava Source ID | 11 (`LiquidSourceBlock`) | 11 (`LAVA_ID`) |
| Non-solid | Yes | Yes (`solid: false`) |
| No collision | Yes | Yes |
| Water opacity | N/A (render layer 1) | 3 |
| Lava opacity | N/A (render layer 0) | 255 |
| Lava light emission | 15 | 15 |

Both systems use identical block IDs and the same source/flowing distinction. ThingCraft maps opacity values for the lighting engine whereas Minecraft handled this implicitly through render layers and brightness overrides.

---

## 3. Metadata & Depth State

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Metadata range | 0-15 (4-bit) | 0-15 (4-bit) |
| Source block | metadata 0 | metadata 0 |
| Flowing depth | 1-7 (increasing = shallower) | 1-7 (same interpretation) |
| Falling/downward | metadata >= 8 | metadata >= 8 |

**Identical.** Both systems use the same metadata encoding. Metadata 0 is a full source block, 1-7 represent increasing horizontal distance from the source, and values >= 8 indicate downward-flowing liquid (e.g., a waterfall column).

---

## 4. Flow Simulation

### 4.1 Tick Rates

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Water tick interval | 5 ticks | 5 ticks |
| Lava tick interval | 30 ticks | 30 ticks |

**Identical.**

### 4.2 Flow Step (Spread Factor)

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Water flow step | 1 | 1 |
| Lava flow step | 2 | 2 |

**Identical.** Both systems increase depth by 1 per block for water and by 2 per block for lava, meaning lava spreads half as far.

### 4.3 Propagation Algorithm

**Minecraft (FlowingLiquidBlock.tick):**
1. Get lowest depth from 4 horizontal neighbors
2. Add flow step to determine new level
3. Check above for source conversion
4. Count adjacent sources for source formation
5. Calculate spread directions via `getSpread()` (gap-finding BFS, depth cap 4)
6. Spread downward first (metadata 8+), then horizontally toward shortest path to a gap

**ThingCraft (tick_flowing_liquid):**
1. Get lowest depth from 4 horizontal neighbors via `get_lowest_depth()`
2. Add flow step to determine new level (cap at >= 8 → dissipate)
3. Check above for source conversion
4. Count adjacent sources for source formation
5. Calculate spread directions via `get_spread_directions()` (BFS-like, depth cap 4)
6. Spread downward first, then horizontally toward shortest path to gap

**Verdict: Functionally identical.** The algorithm is a faithful port. Both use the same gap-finding pathfinding with a depth cap of 4 blocks to determine which horizontal directions to spread toward.

### 4.4 Source Block Creation

| Rule | Minecraft Alpha | ThingCraft |
|---|---|---|
| Condition | >= 2 adjacent sources with supporting block below (solid/source behavior in classic codepaths) | >= 2 adjacent sources AND (solid below OR source below) |
| Applies to | Water only | Water only |
| Result | Flowing → Source (meta 0) | Flowing → Source (meta 0) |

**Near parity.** ThingCraft's source-formation rule matches the classic "2 adjacent sources with support below" behavior used by Alpha-era water logic.

### 4.5 Lava Stall Mechanic

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Mechanism | `random.nextInt(4) != 0` (75% chance to skip) | Hash-based `lava_stall()` (deterministic pseudo-random) |
| Effect | Lava doesn't always spread each tick | Lava doesn't always spread each tick |

**Same intent, different RNG.** Minecraft uses Java's `Random` for a 75% skip chance. ThingCraft uses a deterministic hash function on cell coordinates to produce the stall effect. The ThingCraft approach is more predictable and avoids needing RNG state, which is better for reproducibility and potential future networking.

### 4.6 Liquid-Blocking Blocks

| Minecraft Alpha | ThingCraft |
|---|---|
| Doors (wood + iron) | Doors (64, 71) |
| Standing Sign | Signs (63, 68) |
| Ladder | Ladders (65) |
| Sugar Cane (Reeds) | Sugar Cane (83) |

**Identical set** (IDs 64, 71, 63, 68, 65, 83).

---

## 5. Lava-Water Collision

| Condition | Minecraft Alpha | ThingCraft |
|---|---|---|
| Source lava (meta 0) + water | Obsidian | Obsidian |
| Shallow lava (meta 1-4) + water | Cobblestone | Cobblestone |
| Deep lava (meta >= 5) + water | No reaction | No reaction |

**Identical behavior.** Both check 6 adjacent faces for contact.

Minecraft additionally plays a fizz sound effect and spawns 8 smoke particles on collision. ThingCraft does not currently have particle or sound systems, so these visual/audio cues are absent.

---

## 6. Lava Fire Spread

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Fire ignition | Yes - lava source randomly ignites flammable blocks | Not implemented |
| Mechanism | Random tick on source, 0-2 attempts per tick, 3x3x3 search | N/A |

**Missing in ThingCraft.** Minecraft's `LiquidSourceBlock` has a random tick system where lava sources periodically attempt to place fire blocks adjacent to flammable materials. This requires a flammability system and fire block to be implemented first.

---

## 7. Entity Interaction & Physics

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Flow drag on entities | Yes - `applyMaterialDrag()` pushes entities in flow direction | Not implemented |
| Swimming mechanics | Yes - buoyancy and movement speed changes | Not implemented |
| Flow direction vector | Calculated from neighbor depth differences | Not calculated |

**Missing in ThingCraft.** Minecraft calculates a normalized flow direction vector (`getFlow()`) from the depth differences of neighboring liquid blocks and applies it as velocity to entities within the liquid. This creates the current-dragging effect where entities are pushed downstream. ThingCraft marks liquids as non-solid so entities fall through them, but does not apply flow-based drag or swimming physics.

---

## 8. Rendering

### 8.1 Render Pipeline

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Water render layer | Layer 1 (translucent) | Transparent pass (alpha blending) |
| Lava render layer | Layer 0 (opaque-like) | Transparent pass (alpha blending) |
| Depth writes | Yes (standard) | Disabled for transparent pass |
| Face culling | Back-face | None (both sides visible) |
| Face skipping | Same liquid type or ice | Standard neighbor culling |

ThingCraft renders both water and lava through the same transparent pass with alpha blending and no depth writes. Minecraft rendered lava on the opaque layer (layer 0). ThingCraft's approach of disabling depth writes and face culling (cull mode: NONE) ensures water looks correct when the player is submerged, which is a deliberate improvement.

### 8.2 Height Interpolation

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Corner height sampling | 4 corners, samples adjacent blocks | Standard cube faces with metadata height |
| Height formula | `1.0 - (avg_height / sample_count)` | Metadata-driven flat height per block |
| Smooth transitions | Yes - averaged across neighbors | No - stepped height per block |

**Key difference.** Minecraft's `getLiquidHeight()` samples 4 corner vertices by averaging the depths of surrounding liquid blocks, creating smooth, sloped water surfaces that transition gradually between depth levels. ThingCraft currently renders liquid blocks as standard cube faces whose height is driven by metadata, producing a stepped/terraced appearance rather than smooth slopes.

### 8.3 Flow-Angle Texture Rotation

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Top face UV rotation | Yes - rotates UV by `atan2(flow.z, flow.x) - PI/2` | No - static UV mapping |
| Flow direction rendering | Texture rotates to show flow direction | Static texture orientation |

**Missing in ThingCraft.** Minecraft calculates the flow angle from the depth gradient and rotates the top face texture UVs accordingly, so water visually appears to flow in the correct direction. ThingCraft uses a static texture orientation.

### 8.4 Side Face Brightness

| Face | Minecraft Alpha | ThingCraft |
|---|---|---|
| Top | 1.0x | 1.0x (`alpha_face_scale`) |
| Bottom | 0.5x | 0.5x (`alpha_face_scale`) |
| North/South | 0.8x | 0.8x (`alpha_face_scale`) |
| East/West | 0.6x | 0.6x (`alpha_face_scale`) |

ThingCraft now encodes the same directional face shading profile into `light_data.z` per vertex and applies it in the shader (`face_scale * brightness`). This area is effectively at parity with Alpha's face-scale behavior.

### 8.5 Texture Animation

| Property | Minecraft Alpha | ThingCraft |
|---|---|---|
| Water animation | Procedural (`WaterSprite`) - smoothing filter + heat system | Static terrain-atlas sampling |
| Lava animation | Procedural (`LavaSprite`) - sinusoidal waves + heat | Static terrain-atlas sampling |
| Water color | Dynamic: R(32-64), G(50-114), B(255), A(146-196) | Atlas texture colors |
| Lava color | Dynamic: R(155-255), G(0-255 quadratic), B(0-128 quartic) | Atlas texture colors |
| Side animation | Scrolling offset (water: fast, lava: slow) | Not implemented (static UV sampling) |

**Different approach.** Minecraft generates water and lava textures procedurally every frame using a cellular-automaton-like system with per-pixel heat values, smoothing filters, and sinusoidal patterns. ThingCraft currently samples a static terrain atlas and does not yet implement liquid-specific animation.

---

## 9. Particles & Sound

| Feature | Minecraft Alpha | ThingCraft |
|---|---|---|
| Water bubbles | `WaterBubbleParticle` - size 0.02, 8-40 tick lifetime | Not implemented |
| Water splash | `WaterSplashParticle` - on entity entry | Not implemented |
| Lava ember particles | `LavaParticle` - random above lava surface | Not implemented |
| Ambient water sound | 1/64 chance per tick near flowing water | Not implemented |
| Lava fizz on collision | Sound + 8 smoke particles | Not implemented |

**Not implemented in ThingCraft.** The particle and sound systems are separate systems that would need to be built independently.

---

## 10. World Generation

| Feature | Minecraft Alpha | ThingCraft |
|---|---|---|
| Sea level water | Yes | Yes (Y=64) |
| Cave lava | `LiquidFallFeature` - waterfalls from ceilings | Yes (below Y=10) |
| Lava lakes | Feature generation | Present in cave generation |
| Sponge interaction | Partially implemented (empty loop body) | Not implemented |

Both systems place water at sea level and lava in deep caves. Minecraft also had `LiquidFallFeature` for generating waterfall/lavafall features from stone ceilings, which ThingCraft does not replicate as a distinct feature.

---

## 11. Scheduling & Update Architecture

### Minecraft Alpha
- **Single-threaded:** All liquid ticks run on the main game thread
- **Tick scheduling:** Uses world scheduled ticks backed by `TreeSet` (ordered by due tick) + `HashSet` (membership checks)
- **Per-tick cap:** `doScheduledTicks()` processes up to 1000 scheduled entries per world tick
- **Neighbor cascading:** Each liquid update can trigger neighbor updates that cascade immediately

### ThingCraft
- **Budgeted processing:** Fixed per-frame budget (default 384 cells, configurable via `THINGCRAFT_FLUID_BUDGET`)
- **Priority queue:** `FluidScheduler` with separate urgent and normal queues backed by `BTreeMap<u64, Vec<FluidCell>>`
- **Urgent priority:** Player-triggered changes (block break/place) get `FluidPriority::Urgent`, processed before normal ticks
- **Deduplication:** `HashMap<FluidCell, FluidScheduleEntry>` prevents duplicate scheduling of the same cell
- **Stale entry cleanup:** Auto-removed when processed
- **Chunk-load seeding:** Selective candidate enqueueing reduces startup/backlog pressure while still waking edge/cross-chunk flows

**Tradeoff summary.** Both systems schedule by due time, but ThingCraft adds explicit frame-budget control and urgent prioritization for interactive edits. Alpha remains single-threaded and constrained by its global scheduled-tick cap.

---

## 12. Performance & Optimization Comparison

### 12.1 Tick Processing

| Aspect | Minecraft Alpha | ThingCraft | Advantage |
|---|---|---|---|
| Processing model | Scheduled-tick loop, max 1000 processed per world tick | Budget-capped (384/frame by default) | ThingCraft (frame pacing) |
| Priority handling | Due-time ordered only | Urgent + Normal queues | ThingCraft |
| Duplicate prevention | `HashSet` membership for scheduled ticks | HashMap deduplication + promotion | Slight ThingCraft |
| Queue ordering | `TreeSet` sorted by due time | `BTreeMap` sorted by due tick | Parity |
| Spatial locality | None | Chunk-residency-bounded | ThingCraft |

### 12.2 Rendering

| Aspect | Minecraft Alpha | ThingCraft | Advantage |
|---|---|---|---|
| Texture animation | Procedural (CPU per-frame) | Static atlas sampling | Minecraft (visual quality) |
| Height interpolation | Per-corner averaging (smooth) | Per-block flat (stepped) | Minecraft (visual quality) |
| Flow direction visual | UV rotation per face | Static UVs | Minecraft (visual quality) |
| Depth handling | Standard depth writes | No depth writes + no culling | ThingCraft (correctness) |

### 12.3 Memory

| Aspect | Minecraft Alpha | ThingCraft |
|---|---|---|
| Per-block storage | Block ID + metadata (2 bytes) | Block ID + metadata nibble channel |
| Scheduler overhead | World tick list (shared) | Dedicated FluidScheduler with HashMap + 2x BTreeMap |
| Spatial indexing | None | Chunk-based residency |

---

## 13. Optimization Recommendations for ThingCraft

### 13.1 High Priority

**Smooth height interpolation for liquid surfaces**
The current per-block flat height rendering produces visible terracing. Implementing Minecraft's corner-averaging algorithm (`getLiquidHeight()`) would significantly improve visual quality. This involves sampling the 4 corner vertices of each liquid face by averaging the depths of the liquid block and its 3 diagonal neighbors at each corner. The formula is: `height = 1.0 - (sum_of_heights / count)`. This is a mesh-generation-only change with no simulation cost.

**Flow direction texture rotation**
Calculating the flow direction vector from neighbor depth differences and rotating the top face UVs by `atan2(flow.z, flow.x) - PI/2` would make water visually indicate its flow direction. This is purely a rendering enhancement computed during meshing - no per-frame cost.

### 13.2 Medium Priority

**Batch neighbor lookups during flow simulation**
The current `tick_flowing_liquid()` performs individual block lookups for each of 4-6 neighbors. Pre-fetching a local 3x3x3 neighborhood into a stack-allocated array before processing would improve cache locality and reduce repeated chunk lookups, especially at chunk boundaries where cross-chunk access requires additional indirection.

**Coalesce chunk dirty flags**
When a liquid cell spreads to multiple neighbors in one tick, each `set_block_with_metadata_at_world_for_fluid()` call independently marks chunk geometry dirty. Batching mutations and marking dirty once per chunk per tick cycle would reduce redundant remeshing triggers.

**Adaptive budget scaling**
The fixed 384-cell budget works well on average, but could be dynamically scaled based on frame time headroom. When frames complete early (under target), increase the budget temporarily to let liquids settle faster. When frames are tight, reduce it. This would make large liquid events (dam breaks) resolve faster without impacting steady-state performance.

### 13.3 Low Priority

**Spatial partitioning for the fluid scheduler**
The current `HashMap<FluidCell, FluidScheduleEntry>` is globally keyed. For very large liquid events spanning many chunks, a per-chunk or per-region scheduler partition would improve both cache locality and allow selective processing of only nearby/visible chunks' liquid updates.

**Skip-tick optimization for stable liquids**
If a liquid cell processes and determines its state hasn't changed (same depth, same neighbors), it currently re-schedules itself. Adding a stability check that skips re-enqueue for settled cells would reduce scheduler pressure in large stable bodies of water. Minecraft implicitly had this by only scheduling ticks when state changed.

**Liquid mesh LOD**
For distant chunks, liquid surfaces could be simplified to flat planes at the average depth rather than per-block height calculation. This would reduce vertex count for large oceans/lakes visible at distance.

---

## 14. Feature Parity Summary

| Feature | Minecraft Alpha | ThingCraft | Status |
|---|---|---|---|
| Block IDs & metadata | 8/9/10/11, 4-bit depth | Identical | Complete |
| Water flow (5 tick, step 1) | Yes | Yes | Complete |
| Lava flow (30 tick, step 2) | Yes | Yes | Complete |
| Gap-finding spread algorithm | BFS depth 4 | BFS depth 4 | Complete |
| Source creation (2+ sources) | Yes | Yes | Complete |
| Lava stall mechanic | Random 75% skip | Hash-based stall | Complete (different RNG) |
| Lava-water collision | Obsidian/cobblestone | Obsidian/cobblestone | Complete |
| Liquid-blocking blocks | 6 block types | Same 6 block types | Complete |
| Downward flow (meta >= 8) | Yes | Yes | Complete |
| Smooth height interpolation | Per-corner averaging | Flat per-block | Incomplete |
| Flow direction UV rotation | Yes | No | Missing |
| Entity flow drag | Yes | No | Missing |
| Swimming physics | Yes | No | Missing |
| Fire ignition from lava | Yes | No | Missing |
| Water/lava particles | 3 particle types | No | Missing |
| Ambient liquid sounds | Yes | No | Missing |
| Procedural texture animation | Yes | No (static atlas sampling) | Missing |
| Per-face brightness | 4 brightness levels | 4 brightness levels (`alpha_face_scale`) | Complete |
| Sponge interaction | Partial | No | Missing (both) |

**Core simulation: near-parity.** Flow mechanics, timing, and lava-water collision behavior are implemented and close to Alpha, with modern scheduling differences.

**Rendering: partial parity.** Basic liquid rendering works and now includes Alpha-style directional face shading, but still lacks smooth corner-height interpolation and flow-direction UV rotation.

**Interaction systems: Not started.** Entity physics (flow drag, swimming) and environmental effects (fire, particles, sound) are not yet implemented.

---

*Report generated from decompiled Minecraft Alpha 1.2.6 source (`resources/decomp/src/`) and ThingCraft source analysis.*
*ThingCraft primary liquid files: `streaming.rs` (simulation), `world.rs` (registry), `renderer.rs` (render pipeline), `mesh.rs` (mesh generation).*
*Minecraft primary liquid files: `LiquidBlock.java`, `FlowingLiquidBlock.java`, `LiquidSourceBlock.java`, `BlockRenderer.java (tesselateLiquid)`, `WaterSprite.java`, `LavaSprite.java`.*
