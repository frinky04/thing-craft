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
