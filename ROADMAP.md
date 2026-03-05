# ThingCraft Roadmap

## Current Snapshot

- M0 complete: project scaffold, CI, docs baseline.
- M1 complete: fixed simulation schedule separated from frame loop.
- M2 complete: fly camera, interpolation, and screen-space strafe controls are functioning.
- M3 complete: chunk residency manager tracks requested/generating/meshing/ready/evicting plus dirty/remesh propagation states around the camera.
- M4 complete: deterministic biome/climate fields, surface terrain, cave carving (Alpha worm algorithm), ore vein placement (7 ore types), and Alpha-style dungeon room/chest placement implemented (loot + spawner block-entity payloads still stubbed).
- M5 complete: chunk face-culling mesh extraction, worker-thread generation/meshing pipeline, incremental per-chunk GPU upload/apply path, terrain-atlas sampling, face-aware texturing for selected blocks (grass/log), biome-driven grass colormap tinting, directional face shading, corrected face winding, fixed-tick block interaction requests (raycast break/place + dirty remesh propagation), per-edit column height/light refresh, and renderer frustum culling are implemented.
- M6 complete: async lighting worker lane plus Alpha-style rendered light integration are validated end-to-end (bounded dispatch, queue-based sky/block propagation, stale-result dropping, boundary-scoped neighbor invalidation, urgent edit relight/remesh lanes, neighbor-lighting-stable meshing, and brightness-table-based face shading from propagated light).
- M7 in progress: Wave 1 includes hotbar-driven block placement (`1..9`), finite stack state for those slots (place consumes, break pickups refill matching slots), and torch placement wired through runtime relight/remesh propagation. Wave 2 adds oak tree generation (biome-density-driven), player physics (gravity, AABB collision, jumping, fly/walk toggle via `F`), and a basic HUD (crosshair + hotbar overlay). Wave 3 adds liquid rendering (semi-transparent water with alpha blending, opaque lava with correct atlas textures, split opaque/transparent meshing pipeline), foliage biome tinting (leaf blocks colored from Alpha `foliagecolor.png`), and birch/pine tree shape variants with biome-driven selection. Wave 4 adds fixed-tick day/night state (Alpha time curve + ambient darkness/fog) and budgeted liquid simulation (water/lava flow metadata, source/flowing transitions, downward+lateral spread, and lava-water obsidian/cobblestone reactions). Wave 5 adds entity framework and dropped item entities.
- M7 fluid follow-up landed: liquid scheduling now prioritizes player-driven wakeups, chunk-load seeding is selective to reduce backlog, and frame-time-adaptive fluid budgets + regression harnesses guard against "blocked edits / all-at-once" updates.
- Worldgen stability follow-up landed during M7: tree decoration now runs as a world-space population pass (vanilla-style source chunk influence with cross-chunk writes), eliminating chunk-border tree clipping/truncation artifacts.
- Rendering/lighting parity follow-up landed during M7: linear fog + alpha-test discard in shader, section-level (`16x16x16`) meshing/upload path, per-lane worker pools, neighbor-edge snapshot job payloads, and GPU mesh buffer pooling.
- Rendering performance follow-up landed during M7: chunk-level frustum early-out (chunk AABB gate before section tests), runtime `u16` index packing with `u32` fallback, autoscaled streaming worker defaults from available cores, and a first-pass greedy top-face meshing merge for merge-safe opaque terrain.
- Liquid rendering parity follow-up landed during M7: per-frame Alpha-style `WaterSprite`/`WaterSideSprite` texture simulation is now applied to atlas tiles (including side-sprite 2x2 replication), and liquid top faces use Alpha flow-angle UV mapping with side-sprite selection when flow exists.
- Atmosphere parity follow-up landed during M7: fog clear color now follows Alpha sky/fog biome blending rules, far/normal view distances render a dedicated sky pass, deterministic Alpha-style stars are rendered with night brightness falloff, and clouds now render as Alpha fancy 3D block volumes (`environment/clouds.png`, Y=108.33, 0.03 scroll speed, face-shaded with depth-aware fog).
- Fog parity follow-up landed during M7: clear-fog color is now multiplied by Alpha-style smoothed player-position fog brightness (`world.getBrightness` + view-distance bias + tick smoothing/interpolation), restoring underground/shadow fog darkening behavior.
- Underwater parity follow-up landed during M7: submerged rendering now applies Alpha-style medium behavior (water/lava fog color overrides, exponential fog density switching in shader, and first-person `misc/water.png` overlay with yaw/pitch UV scroll + brightness modulation).
- Leaf rendering parity follow-up landed during M7: leaf vertices now carry a dedicated material marker and runtime `THINGCRAFT_FANCY_GRAPHICS` controls fancy cutout leaves (`true`) vs fast opaque leaves (`false`, with Alpha tile `+1` remap).
- M7 Wave 5 landed: entity framework and dropped item entities. Breaking a block now spawns a physics-driven item entity (Alpha ItemEntity constants: gravity 0.04, bounce -0.5, ground friction 0.588, default 10-tick pickup delay, 5-minute despawn), while player-thrown drops follow Alpha `PlayerEntity.dropItem` (40-tick pickup delay + forward throw impulse). Pickup uses Alpha range parity (`PlayerEntity` grown AABB by `1.0` on X/Z) and Alpha-style 3-tick pickup visual (`EntityPickupParticle` item-to-player lerp) after collection. Items render as Y-axis billboarded terrain atlas sprites with bobbing animation, drawn via the existing chunk shader pipeline in a single batched draw call.
- M7 survival-loop follow-up landed: player now has explicit vitals state (health/prev-health, invulnerability timers, hurt timers), landing applies Alpha-style fall damage, first-person camera applies Alpha walk view-bobbing, and HUD rendering now uses textured Alpha assets (`gui.png`, `icons.png`, terrain atlas) for hotbar/crosshair/hearts instead of debug rectangles.
- M7 survival follow-up (phase 2) in progress: environment damage loop now ticks drowning breath, underwater damage, lava/fire burn timers, and temporary respawn reset; HUD now renders Alpha-style underwater air bubbles.
- M7 survival follow-up (phase 2) advanced: death now enters a manual-respawn state (`R` after death timer) with HUD fade overlay, hotbar item rendering now approximates Alpha's item path with mini 3-face block icons for solid blocks, and dead-state control gating now unlocks mouse + suppresses movement/block interaction until respawn.
- M7 survival follow-up (phase 2) camera parity pass landed: first-person camera now applies Alpha-style hurt/death transforms (`applyHurtCam`) and death-time FOV modulation from `GameRenderer.getFov`.
- M7 movement parity follow-up landed: player water/lava locomotion now follows Alpha `MobEntity.moveRelative` fluid constants (water drag `0.8`, lava drag `0.5`, fluid gravity `0.02`, swim-up impulse `+0.04`, wall-surface jump assist to `0.3`) with fluid contact tracked on `PhysicsBody`.
- M7 movement parity follow-up landed: crouch/sneak now mirrors Alpha core behavior (`Shift` sneak state, movement input scaled by `0.3`, edge-safe ground motion to prevent walking off block edges, no step-up while sneaking, and lowered eye-height camera/raycast offset).
- M7 first-person parity follow-up landed: Alpha-style `ItemInHandRenderer` behavior is now integrated with a dedicated first-person render pass (held block + empty right arm), equip/swing interpolation over 8-tick use cycles, player-position brightness tinting, and skin UV mapping from `mob/char.png`.
- M7 first-person shading follow-up landed: held 3D tool sprites now use Alpha-style per-face normal lighting in first-person (`Lighting.turnOn`-equivalent ambient + two diffuse lights) instead of flat brightness tinting.
- M7 crafting/menu follow-up landed: world right-click block interaction now runs through a place-first/use-second action pipeline (future-proof for block menus), with `CraftingTable` opening a `3x3` crafting menu wired to the same ECS `CraftingRegistry` as player `2x2` crafting. Chest/furnace interaction paths are now explicit menu stubs with TODO hooks for container/smelting state.
- M7 ECS gameplay architecture follow-up landed: mining state, inventory state, and inventory command queue are now ECS resources (`gameplay.rs`), fixed-tick inventory command processing emits explicit gameplay events (changed/drop), post-break side effects are centralized in one helper, and targeted regressions are pinned for drop-metadata preservation + instant/non-harvest break cooldown behavior.
- M7 inventory follow-up landed: inventory slot/cursor stack-count text now renders through a new HUD bitmap-font path (`font/default.png`) with Alpha-style right-aligned numeric overlays and shadowed text treatment.
- M7 mining parity follow-up landed: left-click block breaking now uses an Alpha-style hold-to-mine fixed-tick loop with target-lock progression, retarget reset, per-tick hardness-based break accumulation, continuous swing-looping while mining, destroy-stage crack overlay rendering, and 5-tick post-break cooldown; tool-assisted mining is staged behind dedicated stub hooks for upcoming tool/tier implementation. Block registry parity has also been expanded with Alpha-style hardness/material/flag metadata (hardness, explosion resistance, random-tick, collision/raytrace), and mining now reads hardness directly from the registry.
- Worldgen parity follow-up landed during M7: terrain generation now uses Alpha-exact 3D density field approach with 7 noise generators (`PerlinNoise` octave stacking of `ImprovedNoise`), trilinear interpolation across a 5x17x5 sample grid, climate-modulated height shaping, and a separate top-down surface builder (`buildSurfaces`). Biome source now uses `PerlinSimplexNoise` (octave-stacked simplex noise) instead of `Fbm<OpenSimplex>`, with Alpha-exact seeding order, post-processing transforms, and bulk region sampling. The IceDesert biome variant (which Alpha never produces) has been removed. Bedrock is now an irregular layer at y 0-4 (not flat y=0), and sand/gravel beaches generate from dedicated surface noise. Decoration features (lakes, flowers, cacti, sugar cane, snow cover, pumpkins, reed) are now integrated, with parity tuning ongoing.
- Worldgen follow-up: surface lake population (`LakeFeature`-style water/lava passes) is now enabled again in region generation; decoration-rate parity validation for the full feature set remains in progress.

## M0 - Repository Foundation

### Goal
Create a production-ready Rust project baseline with quality gates and documentation.

### In Scope
- Cargo workspace + `thingcraft-client` crate
- CI compile/test/lint matrix for Linux/macOS/Windows
- Base architecture docs and developer quickstart docs

### Out of Scope
- Gameplay mechanics
- Terrain/chunk systems

### Exit Criteria
- `cargo check`, `cargo test`, and `cargo clippy -- -D warnings` pass locally and in CI
- App window launches on desktop targets

### Risks / Pitfalls
- Dependency API churn (`winit`/`wgpu`) across versions
- Platform-specific cursor-grab differences

## M1 - Tick/Frame Separation

### Goal
Guarantee a fixed simulation schedule independent of render frequency.

### In Scope
- Fixed-step loop (default 20 TPS)
- Catch-up cap to prevent spiral-of-death stalls
- ECS schedule separation (`input`, `fixed_sim`, `render_prep`)

### Out of Scope
- World logic beyond minimal camera movement

### Exit Criteria
- Simulation tick remains stable during normal load
- Render loop remains responsive under temporary simulation spikes
- Runtime metrics expose FPS/TPS and timing summaries

### Risks / Pitfalls
- Unbounded command queue growth under extreme lag
- Hidden simulation work sneaking into render path

## M2 - Fly Camera Vertical Slice

### Goal
Ship a first interactive loop proving input-command-processing and interpolation.

### In Scope
- Player/camera entities in ECS
- Input command queue + fixed-step command consumption
- Authoritative transform + interpolated render transform

### Out of Scope
- Voxel rendering
- Collision/physics world constraints

### Exit Criteria
- Smooth look/move controls
- No direct input-to-authoritative-state mutation
- Unit coverage for loop timing and interpolation behavior

### Risks / Pitfalls
- Cursor lock behavior inconsistent across window managers
- Input repeat semantics causing unintended toggles

## M3 - Chunk Data and Streaming Core

### Goal
Introduce chunk-addressed world storage and loading boundaries.

### In Scope
- Chunk coordinate model and region map
- Chunk residency lifecycle (requested/loading/active/unloaded)
- Read-only interfaces for meshing/lighting workers

### Out of Scope
- Final terrain algorithms
- Lighting propagation

### Exit Criteria
- Camera movement across chunk boundaries keeps correct chunk residency
- Chunk data interfaces can be consumed without ECS-lock contention

## M4 - Terrain Generation

### Goal
Generate Alpha-exact Overworld terrain and biome metadata.

### In Scope
- Alpha-exact `PerlinSimplexNoise` biome fields (temperature/downfall/biome)
- Alpha-exact 3D density terrain (7 `PerlinNoise` generators, 5x17x5 trilinear interpolation)
- Top-down surface builder (bedrock, sand/gravel beaches, biome surface blocks)
- Cave carving and core underground ore distribution and dungeon generation

### Out of Scope
- Decoration features (lakes, flowers, cacti, sugar cane, snow cover, pumpkins, reed)
- Full structure parity beyond dungeons
- Nether content

### Exit Criteria
- Generated chunks are deterministic for a given seed
- Biome fields drive block/material selection predictably
- Terrain shape uses 3D density with overhangs, not flat heightmap

## M5 - Meshing and Culling

### Goal
Render chunk geometry efficiently with frustum-aware updates.

### In Scope
- Chunk meshing pipeline
- Visibility/culling strategy
- Mesh rebuild scheduling from world updates

### Out of Scope
- Smooth lighting/ambient occlusion

### Exit Criteria
- Single-player exploration sustains target performance budget
- Mesh rebuild latency remains bounded during normal block edits

## M6 - Lighting Queue Engine

### Goal
Implement asynchronous block-light and sunlight propagation.

### In Scope
- Queue-based light propagation model
- Worker-friendly chunk read model
- Integration path from light updates to meshing updates

### Out of Scope
- Non-Alpha lighting behaviors

### Exit Criteria
- Block/light changes update nearby regions without main-thread stalls
- Lighting correctness matches strict per-block Alpha-style expectations
- Validation status: implemented and covered by targeted tests (lighting boundary propagation/attenuation, boundary neighbor relight/remesh invalidation, stale-result dropping, and high-radius edit-latency scheduling guardrails).

## M7 - Alpha 1.2.6 Feature Waves

### Goal
Reach functional feature parity in controlled waves.

### In Scope
- Blocks/items/mechanics subsets
- Entities/mobs and AI basics
- Nether + dimension-specific content

### Out of Scope
- Multiplayer transport layer

### Exit Criteria
- `GAMEPLAY_COMPLETION.md` reflects validated gameplay parity progress
- `BOOSTRAP_COMPLETION.md` reflects validated bootstrap/implementation progress
- Regressions covered by targeted tests and playtest checklists

---

## Worldgen Decoration Pass (Follow-Up)

### Status: Implemented (Parity Tuning In Progress)

The core terrain shape (3D density field, surface builder, biome source) is now Alpha-exact. The decoration pass from Alpha's `OverworldChunkGenerator.populate()` is now wired into generation flow and runs **after** terrain + surface + caves/ores/dungeons.

### Implemented Features (Parity Validation Ongoing)

#### 1. Snow Cover
- **Source**: `BiomeDecorator.java` — places SNOW on top of exposed solid blocks when biome temperature < 0.5
- **Status**: Implemented
- **Scope**: Iterate chunk surface, check temperature, place snow block (ID 78) on top face

#### 2. Surface Lakes (Water & Lava)
- **Source**: `LakeFeature.java` — carves and fills small surface/underground liquid pools
- **Status**: Implemented
- **Scope**: Water lakes: 1 attempt per chunk (1-in-4 chance). Lava lakes: 1 attempt per chunk (1-in-8 chance, lower Y). Carve an irregular blob and fill with liquid.

#### 3. Sugar Cane (Reed)
- **Source**: `BiomeDecorator.java` — `reedPerChunk` (default 0, Swampland/Rainforest raise it)
- **Status**: Implemented
- **Scope**: Place 1-3 tall reed columns adjacent to water on sand/dirt/grass

#### 4. Flowers (Dandelion, Rose)
- **Source**: `BiomeDecorator.java` — `flowersPerChunk` (default 2)
- **Status**: Implemented
- **Scope**: Place yellow flower (ID 37) or red flower (ID 38) on grass surface blocks

#### 5. Brown/Red Mushrooms
- **Source**: `BiomeDecorator.java` — `mushroomsPerChunk` (default 0, +1-in-4 random chance)
- **Status**: Implemented
- **Scope**: Place mushroom blocks (IDs 39/40) on solid surfaces with low light level

#### 6. Cacti
- **Source**: `BiomeDecorator.java` — `cactiPerChunk` (Desert=10, otherwise 0)
- **Status**: Implemented
- **Scope**: Place 1-3 tall cactus columns (ID 81) on sand, requires no adjacent solid blocks

#### 7. Pumpkins
- **Source**: `BiomeDecorator.java` — rare (1-in-32 chance per chunk)
- **Status**: Implemented
- **Scope**: Place pumpkin block (ID 86) on grass with random facing metadata

#### 8. Clay Discs
- **Source**: `BiomeDecorator.java` — `clayPerChunk` (default 1)
- **Status**: Implemented
- **Scope**: Place disc-shaped clay deposits (ID 82) underwater in sand/dirt

### Remaining Work

1. Validate exact Alpha attempt counts/distribution against decompiled reference across large seeds.
2. Add targeted parity tests for lake shape constraints and frequency envelopes.
3. Tune edge-case placement behavior (cross-chunk boundaries and biome-specific rates) where needed.


