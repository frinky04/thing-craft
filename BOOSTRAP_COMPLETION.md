# ThingCraft Bootstrap Completion Checklist (Non-Parity)

This document tracks implementation/bootstrap milestones that are not direct Alpha parity items.

## Bootstrap Progress
- [x] Cross-platform desktop client skeleton (`winit` + `wgpu`) created.
- [x] Fixed 20 TPS simulation loop separated from render frame loop.
- [x] Standalone `bevy_ecs` schedules for input capture, fixed simulation, and render interpolation.
- [x] Command-queue input flow established (network-ready pattern for future multiplayer).
- [x] CI + lint/test baseline added.
- [x] Alpha-derived block registry skeleton added (with explicit Alpha-excluded block IDs).
- [x] Chunk core storage model added (`16x16x128`, nibble light channels, Alpha index layout).
- [x] Deterministic biome climate sampler and overworld startup chunk generator scaffold added.
- [x] CPU chunk meshing scaffold added (face culling + atlas UV generation).
- [x] `wgpu` scene pipeline now draws generated chunk geometry with camera uniforms.
- [x] Alpha `terrain.png` texture atlas is loaded and sampled by the chunk render pipeline.
- [x] Bootstrap now generates and renders a small chunk region (3x3 around spawn), not a single chunk.
- [x] Reference tracking doc added (`docs/REFERENCES.md`) for copied constants/behaviors.
- [x] Camera strafe basis and chunk `+Z/-Z` face winding corrected; side-face culling is now consistent.
- [x] Terrain atlas loader now resolves from both repo root and crate-local working directories.
- [x] Region meshing now culls hidden faces across chunk boundaries (not just within individual chunks).
- [x] Face-aware atlas sampling added for grass (top/bottom/side textures now differ as in Alpha).
- [x] Grass top now uses biome-driven tint sampled from Alpha `misc/grasscolor.png` (with fallback if missing).
- [x] Alpha-style directional face shading added (top bright, sides darkened, bottom darkest).
- [x] Oak log top/bottom face atlas mapping added (cap texture on `+Y/-Y`, side texture on lateral faces).
- [x] Chunk residency manager added with lifecycle states (`Requested`, `Generating`, `Meshing`, `Ready`, `Evicting`) around a camera-centered radius.
- [x] Background generation and meshing workers added (main thread now schedules jobs and applies async results).
- [x] Background lighting worker added: bounded relight dispatch, queue-based block/sky propagation with cardinal-neighbor snapshots, stale-result dropping, and relight metrics.
- [x] Chunk vertex lighting now samples propagated neighbor-facing light and maps through Alpha brightness-table + face scale multipliers (removes shader-side minimum-light clamp bias).
- [x] Renderer now supports incremental per-chunk mesh uploads/removals (avoids full merged scene re-upload on each boundary crossing).
- [x] Incremental streaming meshing now includes cardinal neighbor context and neighbor remesh triggers (prevents interior seam faces between loaded chunks).
- [x] Dirty/remesh propagation groundwork added: geometry dirty tracking per chunk, edge-propagated neighbor remesh marking, and remesh counters in runtime metrics.
- [x] Block interaction vertical slice added: camera voxel raycast emits break/place requests, fixed-tick world mutation applies them, and edited chunks trigger boundary-safe remesh propagation.
- [x] Block edits now refresh per-column height/sky and emitted-light seeds (including touched boundary-neighbor columns) to keep chunk metadata coherent after runtime world mutation.
- [x] Edit-path scheduling now has urgent lighting/meshing lanes, preserving near-player block update responsiveness under large view radii while background generation queues are full.
- [x] Meshing dispatch now waits for neighboring lighting stabilization to reduce transient chunk-border seam artifacts during asynchronous relight churn.
- [x] Chunk debug overlay now includes border boxes plus per-chunk generation/lighting/meshing status bars, and runtime stats include edit-to-visible mesh latency telemetry.
- [x] Renderer now frustum-culls chunk draw calls per frame and exposes `visible_chunks` runtime stats for resident-vs-visible perf tracking.
- [x] Cave carving added: Alpha-faithful worm algorithm with room/tunnel/fork logic, lava below Y=10, water-avoidance, and deterministic per-chunk seeding via Java LCG.
- [x] Ore vein placement added: dirt, gravel, coal, iron, gold, redstone, and diamond veins using Alpha's parametric ellipsoid sweep with correct per-ore attempt counts and Y-caps.
- [x] Dungeon stubs added: cobblestone/mossy-cobblestone rooms with mob spawner placement (8 attempts/chunk, wall-opening validation). Chest inventory and spawner entity config deferred to M7.
