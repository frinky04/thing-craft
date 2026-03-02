# ThingCraft Roadmap

## Current Snapshot

- M0 complete: project scaffold, CI, docs baseline.
- M1 complete: fixed simulation schedule separated from frame loop.
- M2 complete: fly camera, interpolation, and screen-space strafe controls are functioning.
- M3 in progress: chunk residency manager now tracks requested/generating/meshing/ready/evicting states around the camera.
- M4 started: deterministic biome/climate and startup overworld generation scaffold implemented.
- M5 in progress: chunk face-culling mesh extraction, worker-thread generation/meshing pipeline, incremental per-chunk GPU upload/apply path, terrain-atlas sampling, face-aware texturing for selected blocks (grass/log), biome-driven grass colormap tinting, directional face shading, and corrected face winding are implemented.

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
Generate Alpha-like Overworld terrain and biome metadata.

### In Scope
- 2D temperature/humidity biome fields
- 3D terrain noise and cave carving
- Core underground ore distribution and dungeon stubs

### Out of Scope
- Full structure parity beyond dungeons
- Nether content

### Exit Criteria
- Generated chunks are deterministic for a given seed
- Biome fields drive block/material selection predictably

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
- `COMPLETION.md` reflects validated parity progress
- Regressions covered by targeted tests and playtest checklists
