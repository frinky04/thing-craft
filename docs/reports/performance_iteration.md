# Performance Iterations

## Iteration 1 (2026-03-05)

### Goal
- Reduce per-frame CPU scheduling overhead in chunk streaming without reducing code clarity.

### Change
- File: `thingcraft-client/src/streaming.rs`
- Updated dispatch candidate scans to iterate `required` chunk positions directly instead of scanning all residency slots:
  - `dispatch_generation`
  - `dispatch_lighting`
  - `dispatch_meshing`
- Kept existing distance sort and dispatch policy behavior intact.

### Validation
- `cargo check -p thingcraft-client`
- Streaming behavior tests:
  - `urgent_lighting_dispatch_preempts_regular_candidates`
  - `urgent_meshing_can_dispatch_pair_when_budget_is_one`

### Benchmark
- Baseline:
  - CSV: `docs/reports/benchmarks/bench_1772685291.csv`
  - Screens: `docs/reports/benchmarks/bench_1772685291_screens/`
- Candidate run:
  - CSV: `docs/reports/benchmarks/bench_1772685638.csv`
  - Screens: `docs/reports/benchmarks/bench_1772685638_screens/`

Phase average comparison (candidate - baseline):
- `still`: FPS `+39.55`, frame ms `-0.118`
- `turn`: FPS `+22.29`, frame ms `-0.147`
- `move`: FPS `+59.29`, frame ms `-0.110`

### Visual Regression Check
- Compared `still/turn/move` screenshots against baseline with direct image inspection.
- Result: no observable visual regression.

### Outcome
- Accepted.
- Optimization improves benchmark metrics across all scripted phases with no visual parity regressions.

## Iteration 2 (2026-03-05)

### Goal
- Reduce dispatch candidate sort cost with a lower-overhead sort primitive.

### Change
- File: `thingcraft-client/src/streaming.rs`
- Tried changing `sort_candidates_by_distance` from `sort_by_key` to `sort_unstable_by_key`.

### Validation
- `cargo check -p thingcraft-client`
- Streaming behavior tests:
  - `urgent_lighting_dispatch_preempts_regular_candidates`
  - `urgent_meshing_can_dispatch_pair_when_budget_is_one`

### Bench Results
- Candidate runs:
  - `bench_1772685984.csv`
  - `bench_1772686055.csv` (confirmation rerun)
- Both runs regressed versus the accepted iteration trajectory (especially `turn` and `move`), with the confirmation run materially worse.

### Outcome
- Rejected and reverted.
- Kept `sort_by_key` implementation.

## Iteration 3 (2026-03-05)

### Goal
- Reduce allocator churn in dispatch loops by reusing candidate vectors.

### Change
- File: `thingcraft-client/src/streaming.rs`
- Attempted to add persistent scratch `Vec<ChunkPos>` buffers for:
  - generation candidates
  - lighting urgent/regular candidates
  - meshing urgent/regular candidates
- Reused these buffers each tick instead of allocating fresh vectors.

### Validation
- `cargo check -p thingcraft-client`
- Streaming behavior tests:
  - `urgent_lighting_dispatch_preempts_regular_candidates`
  - `urgent_meshing_can_dispatch_pair_when_budget_is_one`

### Bench Results
- Candidate runs:
  - `bench_1772686370.csv`
  - plus confirmation behavior from rerun sequence showed sustained lower performance trend
- Regressed versus baseline and accepted iteration metrics (notably `still` and `turn` frame time).

### Outcome
- Rejected and reverted.
- Scratch-buffer candidate reuse was removed.

## Iteration 4 (2026-03-05)

### Goal
- Reduce per-frame streaming overhead by skipping `refresh_required_set` call/span when center chunk is unchanged.

### Change
- File: `thingcraft-client/src/streaming.rs`
- Attempted to guard the call in `tick`:
  - only run `refresh_required_set(center_chunk)` when `required_center != Some(center_chunk)`.

### Validation
- `cargo check -p thingcraft-client`
- Streaming behavior tests:
  - `urgent_lighting_dispatch_preempts_regular_candidates`
  - `urgent_meshing_can_dispatch_pair_when_budget_is_one`

### Bench Results
- Candidate runs:
  - `bench_1772686370.csv`
  - `bench_1772686658.csv` (confirmation rerun)
- Both runs regressed substantially versus baseline and accepted iteration metrics, especially in `still`, `turn`, and `move` frame time.

### Outcome
- Rejected and reverted.
- Retained original unconditional call structure.

## Iteration 5 (2026-03-05)

### Goal
- Reduce non-tracy runtime overhead from frequent span construction in `streaming.tick`.

### Change
- File: `thingcraft-client/src/streaming.rs`
- Added `streaming_tick_span!` macro to compile out detailed tick sub-spans when the `tracy` feature is disabled:
  - `#[cfg(feature = "tracy")]`: keep `tracing::info_span!(...).entered()`
  - `#[cfg(not(feature = "tracy"))]`: no-op (`None::<()>`)
- Applied the macro to sub-spans in:
  - `streaming.tick`
  - `streaming.refresh_required_set`
  - `streaming.poll_generation_results`
  - `streaming.poll_lighting_results`
  - `streaming.poll_meshing_results`
  - `streaming.dispatch_generation`
  - `streaming.dispatch_lighting`
  - `streaming.dispatch_meshing`
  - `streaming.flush_render_uploads`
  - `streaming.cleanup_evicted`

### Validation
- `cargo check -p thingcraft-client`
- `cargo check -p thingcraft-client --features tracy`
- Streaming behavior test:
  - `urgent_lighting_dispatch_preempts_regular_candidates`

### Benchmark
- Baseline:
  - CSV: `docs/reports/benchmarks/bench_1772685291.csv`
  - Screens: `docs/reports/benchmarks/bench_1772685291_screens/`
- Candidate:
  - CSV: `docs/reports/benchmarks/bench_1772687114.csv`
  - Screens: `docs/reports/benchmarks/bench_1772687114_screens/`

Phase average comparison (candidate - baseline):
- `still`: FPS `-4.69`, frame ms `-0.054`
- `turn`: FPS `+18.28`, frame ms `-0.140`
- `move`: FPS `+45.84`, frame ms `-0.084`

### Visual Regression Check
- Compared `still/turn/move` screenshots against `bench_1772685291_screens`.
- Result: no observable visual regression.

### Outcome
- Accepted.
- Kept the change; non-tracy builds avoid repeated detailed span overhead while preserving tracy instrumentation behavior.
