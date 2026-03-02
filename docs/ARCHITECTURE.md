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
- A CPU chunk mesher generates indexed triangle geometry with face-culling and atlas UVs.

## Networking-Ready Input Pattern

Input is represented as commands (`SimCommandEvent`) that are processed by simulation.
This allows future network packets to feed the same command path without forking logic.

## Early Pitfalls to Avoid

- Running chunk generation/meshing/lighting in render callbacks.
- Allowing direct input systems to mutate world state.
- Treating ECS storage as the only source for background workers when contention appears.
