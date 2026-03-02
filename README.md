# ThingCraft

A Rust + `wgpu` + standalone `bevy_ecs` progressive rebuild of Minecraft Alpha 1.2.6.

## Current Status

The repository currently includes a bootstrap client with:

- Cross-platform desktop window bootstrap (`winit` + `wgpu` clear pass)
- Strict frame/render vs fixed simulation separation (20 TPS fixed simulation)
- ECS-driven fly camera using command-queue input flow
- Transform interpolation for smooth rendering when simulation cadence differs from frame cadence
- Alpha-derived block registry and chunk core model (`16x16x128`, nibble light storage)
- Deterministic Overworld chunk bootstrap with Alpha climate thresholds and waterline targeting `Y=64`
- CPU chunk mesher with face culling + terrain atlas UV assignment (geometry generation path)
- `wgpu` indexed draw pipeline now renders the generated startup chunk region using `terrain.png`

## Development Commands

```bash
cargo check --workspace
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
cargo run -p thingcraft-client
```

## Controls

- `W/A/S/D`: Move horizontally
- `Space` / `Left Shift`: Move vertically while fly mode is enabled
- Mouse movement (after click): Look
- Left click: Capture cursor
- `Escape`: Release cursor
- `F`: Toggle fly mode

## Design Constraints

- Input does not mutate simulation state directly.
- Fixed-step simulation runs independently from render frame cadence.
- Authoritative world/camera data is kept in `f64` space, render transform is interpolated for frame presentation.
- Networking compatibility is preserved by routing player actions through command events.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/REFERENCES.md](docs/REFERENCES.md), and [ROADMAP.md](ROADMAP.md) for details.
