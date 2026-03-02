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
- The same vertex modulation path currently applies simple Alpha-style directional brightness per face as an interim lighting model.
- Region meshing performs neighbor-aware culling across chunk boundaries so interior shared faces are not emitted.
- Bootstrap startup currently pre-generates a small region (`3x3` chunks) and builds one combined region mesh for first render.
- The renderer owns GPU buffers/pipeline and draws chunk mesh indices each frame using camera view-projection uniforms and the Alpha terrain atlas texture.

## Streaming and Jobs

- Runtime chunk residency is managed with explicit states: `Requested`, `Generating`, `Meshing`, `Ready`, `Evicting`.
- Residency targets are derived from the camera chunk position with a configurable square radius.
- Chunk generation and chunk meshing run on dedicated worker threads.
- Meshing jobs include cardinal neighbor chunk snapshots so chunk-boundary face culling remains correct with incremental uploads.
- The main thread only:
  - computes residency deltas,
  - dispatches bounded job batches per frame,
  - consumes async worker results,
  - applies per-chunk render updates (GPU upsert/remove) from worker results.
- This keeps heavy world build steps off the render path while preserving deterministic state ownership on the main thread.
- Residency entries now track dirty state, and geometry remesh requests can propagate to cardinal neighbors for boundary edits.

## Networking-Ready Input Pattern

Input is represented as commands (`SimCommandEvent`) that are processed by simulation.
This allows future network packets to feed the same command path without forking logic.

## Block Interaction Slice

- Mouse button input now enqueues explicit block interaction requests (break/place), rather than mutating chunks directly in the input event handler.
- Requests are consumed during fixed simulation ticks, preserving deterministic simulation ownership and keeping input/render paths side-effect free.
- Interaction targeting uses a voxel DDA raycast from the authoritative camera transform.
- Chunk edits are applied through `ChunkStreamer` world-coordinate mutation APIs, which immediately mark edited chunk geometry dirty and propagate boundary remesh to cardinal neighbors when needed.

## Early Pitfalls to Avoid

- Running chunk generation/meshing/lighting in render callbacks.
- Allowing direct input systems to mutate world state.
- Treating ECS storage as the only source for background workers when contention appears.
