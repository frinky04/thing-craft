# Architecture Notes (Bootstrap)

## Core Loop Model

The runtime is explicitly split into three phases:

1. Input capture schedule (frame rate): gather device input and emit simulation commands.
2. Fixed simulation schedule (tick rate): consume commands and update authoritative state.
3. Render prep schedule (frame rate): interpolate authoritative state for rendering.

This ensures simulation stalls do not fully block presentation and avoids coupling gameplay to rendering cadence.

## ECS Boundaries

- Input systems are write-only to `SimCommandQueue` and never mutate gameplay transforms.
- Simulation systems are the only owners of authoritative transform mutations.
- Render prep systems read authoritative transforms and write interpolated render transforms.

## Coordinate Precision

Authoritative transform data uses `f64` (`Transform64`) to preserve precision over large world travel.
Render transforms are projected to `f32` (`RenderTransform`) for GPU-facing data.

## World Data Core

- Blocks are defined through a data-driven registry (`BlockRegistry`) keyed by block ID.
- The registry stores opacity/light/material metadata used by simulation and rendering.
- Chunk storage follows Alpha dimensions (`16x16x128`) and Alpha index layout:
  - `(x << 11) | (z << 7) | y`
- Block and sky light channels use nibble storage (`4-bit` packed values).
- A deterministic Overworld generator uses biome climate sampling + terrain noise to create startup chunks.
- A CPU chunk mesher generates indexed triangle geometry with face-culling and atlas UVs (including face-aware texture selection from the block registry).
- Vertex color modulation is used as the tint path; grass top tint is generated per column from biome temperature/downfall and sampled through Alpha `misc/grasscolor.png` (with a fallback map when unavailable).
- The chunk mesher now samples neighbor-facing raw light (`max(sky, block)`), maps it through Alpha's brightness curve, and applies Alpha face scales (`top=1.0`, `bottom=0.5`, `north/south=0.8`, `west/east=0.6`) before writing vertex color modulation.
- Region meshing performs neighbor-aware culling across chunk boundaries so interior shared faces are not emitted.
- Bootstrap startup currently pre-generates a small region (`3x3` chunks) and builds one combined region mesh for first render.
- The renderer owns GPU buffers/pipeline and draws chunk mesh indices each frame using camera view-projection uniforms and the Alpha terrain atlas texture.

## Streaming and Jobs

- Runtime chunk residency is managed with explicit states: `Requested`, `Generating`, `Meshing`, `Ready`, `Evicting`.
- Residency targets are derived from the camera chunk position with a configurable square radius.
- Chunk generation, chunk lighting, and chunk meshing run in per-lane worker pools (configurable worker counts per lane).
- Lighting jobs run a queue-based sunlight/block-light propagation pass against immutable chunk snapshots (center + cardinal neighbor edge-light slices), then return packed nibble-channel updates to the main thread plus section-diff masks.
- Meshing jobs consume cardinal neighbor edge slices (block + light boundary bands) and rebuild only dirty `16x16x16` sections.
- The main thread only:
  - computes residency deltas,
  - dispatches bounded job batches per frame,
  - consumes async worker results,
  - applies per-chunk render updates (GPU upsert/remove) from worker results.
- This keeps heavy world build steps off the render path while preserving deterministic state ownership on the main thread.
- Dispatch now prioritizes local relight/remesh lanes before generation and uses bounded in-flight depth per lane to prevent high-radius generation churn from starving nearby edit updates.
- Residency entries now track dirty state, and geometry remesh requests can propagate to cardinal neighbors for boundary edits.
- Lighting dirtiness tracks a per-chunk revision. In-flight lighting results are dropped if the chunk was re-dirtied before apply.
- Boundary lighting diffs are now scoped to chunk edges, so neighbor relight/remesh is only propagated when edge light channels actually changed.
- Meshing results are now applied only when the target chunk is still clean; stale in-flight meshes are dropped if new edits arrived while meshing.
- Meshing dispatch now requires neighboring lighting to be settled (no dirty/in-flight neighbor lighting) to reduce transient chunk-edge seam artifacts.
- Geometry dirtiness is tracked as an 8-bit section mask per chunk (`128 / 16 = 8` sections), with edge and Y-boundary propagation rules for edit correctness.

## Networking-Ready Input Pattern

Input is represented as commands (`SimCommandEvent`) that are processed by simulation.
This allows future network packets to feed the same command path without forking logic.

## Block Interaction Slice

- Mouse button input now enqueues explicit block interaction requests (break/place), rather than mutating chunks directly in the input event handler.
- Requests are consumed during fixed simulation ticks, preserving deterministic simulation ownership and keeping input/render paths side-effect free.
- Interaction targeting uses a voxel DDA raycast from the authoritative camera transform.
- Chunk edits are applied through `ChunkStreamer` world-coordinate mutation APIs, which immediately mark edited chunk geometry dirty and propagate boundary remesh to cardinal neighbors when needed.
- Edited columns immediately refresh chunk-local height/sky data and emitted-light seeds, with boundary-neighbor column refresh at chunk edges.

## Renderer Culling

- Chunk mesh draw calls are frustum-culled on CPU using camera view-projection planes and per-section AABBs (`16x16x16`).
- Runtime debug stats include visible chunk count and edit-to-visible mesh latency metrics.
- Chunk border debug mode now overlays per-chunk generation/lighting/meshing status bars to diagnose queue pressure and lighting churn near seams.
- Terrain shading now includes Alpha-style alpha cutout discard and linear fog blending in WGSL.

## Early Pitfalls to Avoid

- Running chunk generation/meshing/lighting in render callbacks.
- Allowing direct input systems to mutate world state.
- Treating ECS storage as the only source for background workers when contention appears.
