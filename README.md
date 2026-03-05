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
- Alpha-style fog and alpha-test discard are now integrated in the terrain shader
- Section-level (`16x16x16`) chunk meshing + per-section GPU uploads are active
- Streaming workers now support per-lane worker pool sizing (`GEN/LIGHT/MESH`)

## Development Commands

```bash
cargo check --workspace
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
cargo run -p thingcraft-client
```

World seed launch overrides:

```bash
# deterministic seed
cargo run -p thingcraft-client -- --seed 123456789

# hexadecimal seed
cargo run -p thingcraft-client -- --seed 0xA1260001

# time-based random seed
cargo run -p thingcraft-client -- --random-seed
```

Environment fallback is also supported: `THINGCRAFT_WORLD_SEED=<seed>` (CLI flags take precedence).

For debug runtime stats without `wgpu` internals flooding output:

```bash
RUST_LOG=debug cargo run -p thingcraft-client
```

Streaming tuning (useful to push boundary walls farther out while exploring underground):

```bash
THINGCRAFT_VIEW_RADIUS=5 THINGCRAFT_GEN_BUDGET=12 THINGCRAFT_LIGHT_BUDGET=12 THINGCRAFT_MESH_BUDGET=12 cargo run -p thingcraft-client
```

If edit-heavy scenes cause upload spikes, cap section uploads per frame:

```bash
THINGCRAFT_UPLOAD_BUDGET=12 cargo run -p thingcraft-client
```

Worker pool tuning:

```bash
THINGCRAFT_GEN_WORKERS=2 THINGCRAFT_LIGHT_WORKERS=4 THINGCRAFT_MESH_WORKERS=4 cargo run -p thingcraft-client
```

Tracy profiling (recommended for perf investigation):

```bash
THINGCRAFT_TRACY=1 cargo run -p thingcraft-client --features tracy

# Optional GPU pass timings (if adapter supports timestamp queries)
THINGCRAFT_GPU_TIMESTAMPS=1 cargo run -p thingcraft-client --features tracy

# Pass toggles for quick A/B perf checks
THINGCRAFT_RENDER_TRANSPARENT=0 THINGCRAFT_RENDER_CLOUDS=0 THINGCRAFT_RENDER_FIRST_PERSON=0 THINGCRAFT_RENDER_HUD=0 cargo run -p thingcraft-client --features tracy

## Deterministic Benchmark Mode

For repeatable profiling without manual movement:

```powershell
.\scripts\run_bench.ps1
```

This runs three scripted phases (`still`, `turn`, `move`), auto-exits, and writes a CSV to `docs/reports/benchmarks/`.
Each run also captures phase screenshots to:
- `docs/reports/benchmarks/<bench_name>_screens/still.png`
- `docs/reports/benchmarks/<bench_name>_screens/turn.png`
- `docs/reports/benchmarks/<bench_name>_screens/move.png`

And updates rolling latest references:
- `docs/reports/benchmarks/latest_still.png`
- `docs/reports/benchmarks/latest_turn.png`
- `docs/reports/benchmarks/latest_move.png`

To summarize any benchmark CSV (or latest by default):

```powershell
.\scripts\summarize_bench.ps1
.\scripts\summarize_bench.ps1 -Path docs\reports\benchmarks\bench_1772682786.csv
```
```

When enabled, the client emits tracing spans for:
- `frame`
- `fixed_tick`
- `streaming_tick`
- `render`

Open Tracy and connect to the running process to inspect these zones and identify hotspots.

## Controls

- `W/A/S/D`: Move horizontally
- `Space` / `Left Shift`: Move vertically while fly mode is enabled
- Mouse movement (after click): Look
- Left click: Capture cursor (first click), then break targeted block
- Right click (while captured): Place block on targeted face (consumes selected hotbar stack on successful placement)
- `1`..`9`: Select hotbar placement block (slot 9 is torch)
- Hotbar stacks are finite (`64` max per slot); breaking a block refills the matching hotbar slot when space is available
- `Escape`: Release cursor
- `F`: Toggle fly mode
- `B`: Toggle chunk border debug overlay

## Design Constraints

- Input does not mutate simulation state directly.
- Fixed-step simulation runs independently from render frame cadence.
- Authoritative world/camera data is kept in `f64` space, render transform is interpolated for frame presentation.
- Networking compatibility is preserved by routing player actions through command events.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/REFERENCES.md](docs/REFERENCES.md), and [ROADMAP.md](ROADMAP.md) for details.
