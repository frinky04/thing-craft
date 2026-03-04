# Performance & Optimization Report

**Date:** 2026-03-05
**Focus:** Rendering performance at higher view distances, frame stability, responsiveness

---

## Actionable Checklist

- [ ] **[HIGH] Implement greedy meshing** — merge coplanar same-texture faces into larger quads to reduce vertex/index count by ~3-5x
- [ ] **[HIGH] Use `u16` indices for chunk sections** — sections have at most 16x16x16 = 4096 blocks × 24 verts = 98,304 vertices, well under `u16::MAX` (65,535) for most sections; fall back to `u32` only when a section exceeds 65,535 vertices
- [ ] **[HIGH] Add chunk-level frustum early-out** — test the full 16x128x16 AABB once before iterating the 8 sections; most off-screen chunks will be rejected by a single test instead of 8
- [ ] **[HIGH] Increase default worker thread counts** — 1 gen / 2 light / 2 mesh workers underutilises modern CPUs; scale to `(physical_cores - 2).max(1)` split across pools, or at minimum 2/3/3
- [ ] **[MED] Pre-allocate mesh Vecs with capacity estimates** — `build_split_chunk_mesh_with_neighbor_lookup` creates `Vec::new()` for vertices/indices with no reserve; estimate ~1000 faces per section and pre-allocate
- [ ] **[MED] Use circular view radius instead of square** — current square loading loads `(2r+1)^2` chunks; a circular check (`dx*dx + dz*dz <= r*r`) cuts ~21% of corner chunks at radius 10+
- [ ] **[MED] Batch draw calls with multi-draw-indirect** — currently 1 draw call per visible chunk section; at view radius 10 this is ~800+ draw calls for opaque alone; consolidate into a single indirect draw
- [ ] **[MED] Skip empty sections during meshing** — track an `is_all_air` flag per section in `ChunkData`; skip meshing entirely for sections that contain only air
- [ ] **[MED] Raise default `max_render_upload_sections_per_tick`** — current cap of 24 sections/tick means a full 121-chunk load takes ~40 ticks (2 seconds at 20 TPS); consider 48-64 or tie it to frame headroom
- [ ] **[LOW] Use `FxHashMap` for chunk mesh storage** — `HashMap<ChunkPos, ...>` uses SipHash by default; chunk positions are small integers where FxHash is ~2-3x faster
- [ ] **[LOW] Sort transparent chunks back-to-front** — currently iterated in HashMap order; proper depth sorting eliminates blending artifacts with overlapping water/glass
- [ ] **[LOW] Consider Uint8x4 packed vertex position** — for positions local to a 16-block section, positions could be packed into much smaller representations, halving vertex bandwidth

---

## Detailed Analysis

### 1. Draw Call Overhead (HIGH impact at distance)

**Current state:** Each visible chunk section issues its own `set_vertex_buffer` + `set_index_buffer` + `draw_indexed` triplet (`renderer.rs:2857-2862`). Both the opaque and transparent passes do this independently.

**Impact at scale:**
| View radius | Chunks loaded | Sections (worst case) | Draw calls (opaque+transparent) |
|-------------|--------------|----------------------|-------------------------------|
| 5 (default) | 121 | ~968 | ~600 visible |
| 10 | 441 | ~3,528 | ~2,000 visible |
| 16 | 1,089 | ~8,712 | ~5,000 visible |

Each draw call has CPU-side overhead for state validation and command encoding. At 5,000+ draws this becomes the primary bottleneck.

**Recommendation:** Consolidate into **multi-draw-indirect** (`wgpu::RenderPass::multi_draw_indexed_indirect`). Pack all chunk sections into a shared large vertex/index buffer (or a few mega-buffers), build an indirect args buffer on the CPU each frame with only frustum-visible entries, and issue a single draw. This eliminates per-section `set_*_buffer` overhead entirely.

Alternatively, a simpler first step: merge all 8 section buffers of a single chunk into one buffer at upload time. This halves draw calls with minimal code change.

---

### 2. No Greedy Meshing (HIGH impact on vertex count)

**Current state:** Every exposed block face emits 4 vertices + 6 indices individually (`mesh.rs:745-760`). A flat plain of grass at y=64 with 16x16 exposed top faces in a section generates 256 quads = 1,024 vertices + 1,536 indices, when greedy meshing could reduce it to as few as 1 quad = 4 vertices + 6 indices.

**Typical savings:** Greedy meshing reduces vertex count by 3-5x for natural terrain, more for flat areas. This directly reduces:
- GPU vertex processing load
- Memory bandwidth (28 bytes/vertex × millions of vertices)
- Buffer upload time
- VRAM usage

**Recommendation:** Implement a per-section greedy meshing pass that runs after face generation. For each face axis (6 directions), sweep the 16x16 slice and merge adjacent faces that share the same texture, tint, and light values into larger quads. This is the single highest-impact optimization for rendering at distance.

Note: Alpha Minecraft itself does not do greedy meshing, but this is a rendering optimization that doesn't change gameplay behavior.

---

### 3. Index Format Waste (HIGH — easy win)

**Current state:** All index buffers use `Uint32` (`renderer.rs` — every `set_index_buffer` call). A chunk section is 16x16x16 = 4,096 blocks. Even if every block exposed all 6 faces, that's 24,576 vertices — well within `u16::MAX` (65,535).

**Impact:** Every index is 4 bytes instead of 2. For terrain-heavy scenes this doubles index buffer size and bandwidth for no reason.

**Recommendation:** Use `u16` indices by default. Only fall back to `u32` for the rare section that exceeds 65,535 vertices (e.g., extremely complex sections with many billboard plants). The mesh builder already knows vertex count before upload, so the decision is trivial.

---

### 4. Frustum Culling Granularity (HIGH — easy win)

**Current state:** The render loop iterates all 8 sections of every chunk and tests each against the frustum individually (`renderer.rs:2845-2868`):

```rust
for (pos, sections) in &self.chunk_meshes {
    for (section_y, chunk_mesh) in sections.iter().enumerate() {
        if frustum.is_some_and(|view| {
            !view.intersects_chunk_section(*pos, section_y as u8)
        }) { continue; }
        // draw...
    }
}
```

A chunk that is entirely off-screen still performs 8 AABB-frustum tests (one per section).

**Recommendation:** Add a chunk-level early-out: test the full 16x128x16 AABB first. If it fails, skip all 8 sections. At view radius 10, roughly 50-60% of loaded chunks are behind the camera, saving ~1,700 individual section tests per frame.

```rust
for (pos, sections) in &self.chunk_meshes {
    if frustum.is_some_and(|view| !view.intersects_chunk(*pos)) {
        continue; // skip all 8 sections
    }
    for (section_y, chunk_mesh) in sections.iter().enumerate() {
        // existing per-section test...
    }
}
```

---

### 5. Worker Thread Underutilisation (HIGH — config change)

**Current state:** Default worker counts (`streaming.rs:199-205`):
- Generation: 1 thread
- Lighting: 2 threads
- Meshing: 2 threads
- Total: 5 threads

Most modern gaming PCs have 8-16+ cores. With only 5 workers, the streaming pipeline becomes the bottleneck when increasing view distance because chunk generation/meshing can't keep up.

**Impact:** At view radius 10, there are 441 chunks to process. With 1 generation worker and typical generation time of ~2-5ms per chunk, initial world load takes 1-2 seconds just for generation, then lighting and meshing queue behind it.

**Recommendation:** Auto-scale based on available cores:
```rust
let cores = std::thread::available_parallelism().map(|n| n.get()).unwrap_or(4);
let workers = (cores - 2).max(2); // reserve 2 for main + render
// Split: ~25% gen, ~35% light, ~40% mesh
```

Or at minimum raise defaults to 2/3/3 for gen/light/mesh.

---

### 6. Square vs Circular Chunk Loading (MED)

**Current state:** Chunks are loaded in a square pattern (`streaming.rs:1152-1153`):
```rust
for dz in -view_radius..=view_radius {
    for dx in -view_radius..=view_radius {
```

At radius 10, this loads 441 chunks. A circle of radius 10 contains ~317 chunks — saving 28% of chunk work for corners the player can't see anyway (they're beyond fog distance).

**This matches Alpha behavior** (Alpha uses square loading), so this is a deliberate parity choice. However, since Alpha's fog hides distant corners anyway, circular loading would be visually indistinguishable while saving significant work.

---

### 7. Mesh Vec Pre-allocation (MED — easy win)

**Current state:** `SplitChunkMesh::default()` creates empty `Vec::new()` for vertices and indices (`mesh.rs:340-343`). The meshing loop then pushes thousands of entries, causing multiple reallocations.

**Recommendation:** Pre-allocate based on typical section density:
```rust
// Average exposed faces per section: ~500-2000
let estimated_verts = 2000;
let estimated_indices = 3000;
ChunkMesh {
    vertices: Vec::with_capacity(estimated_verts),
    indices: Vec::with_capacity(estimated_indices),
}
```

This eliminates ~5-8 reallocations per section mesh build, which matters when meshing thousands of sections.

---

### 8. Upload Budget Tuning (MED)

**Current state:** `max_render_upload_sections_per_tick: 24` (`streaming.rs:203`). At 20 TPS, this is 480 sections/second.

At view radius 10 with 441 chunks × ~4 non-empty sections average = ~1,764 sections to upload. This takes ~3.7 seconds just for GPU upload, contributing to the "loading lag" when increasing view distance.

**Recommendation:** Tie upload budget to frame time headroom, similar to the adaptive fluid budget already in `app.rs`. When frames are fast (< 12ms), upload more sections. When frames are slow (> 16ms), throttle back.

---

### 9. HashMap Performance (LOW — easy win)

**Current state:** `chunk_meshes: HashMap<ChunkPos, ...>` uses the default SipHash hasher. `ChunkPos` is just two `i32` values.

**Recommendation:** Use `rustc_hash::FxHashMap` or `ahash::AHashMap`. For small-key maps like chunk positions, these are 2-3x faster for lookups. The render loop iterates this map every frame, so faster iteration matters.

---

### 10. Transparent Sorting (LOW — correctness + perf)

**Current state:** Transparent chunks are iterated in HashMap order (`renderer.rs:2906`), not sorted by distance. This means water surfaces at different distances may render in arbitrary order.

**Impact:** With standard alpha blending, incorrect order causes visual artifacts (distant water rendering on top of near water). At higher view distances with more water bodies, this becomes more noticeable.

**Recommendation:** Sort transparent chunk sections back-to-front each frame. Since there are typically far fewer transparent sections than opaque ones, the sort cost is negligible.

---

### 11. Empty Section Skip (MED)

**Current state:** The meshing loop iterates all blocks in a section's Y range even if the section is entirely air (`mesh.rs:594-599`). While individual air blocks are skipped quickly, the iteration overhead adds up.

**Recommendation:** Track a per-section block count or `is_all_air` flag in `ChunkData`. Skip meshing dispatch entirely for empty sections. Underground chunks often have 3-4 completely empty sections above ground that are pointlessly iterated.

---

## Summary of Expected Impact

| Optimization | Effort | Frame time reduction | View distance scalability |
|---|---|---|---|
| Greedy meshing | High | 20-40% fewer GPU verts | Major improvement |
| Multi-draw-indirect | Medium | 30-50% less CPU draw overhead | Major improvement |
| u16 indices | Low | 5-10% less bandwidth | Moderate improvement |
| Chunk-level frustum early-out | Low | 5-15% less CPU cull time | Moderate improvement |
| More worker threads | Trivial | N/A (load time) | Major improvement |
| Circular loading | Low | ~20% fewer chunks | Moderate improvement |
| Vec pre-allocation | Trivial | 5-10% faster meshing | Minor improvement |
| FxHashMap | Trivial | 2-5% faster iteration | Minor improvement |

The highest-impact changes for view distance scaling are **greedy meshing** (reduces GPU work proportionally) and **multi-draw-indirect** (reduces CPU work proportionally). Together they should allow comfortable rendering at view radius 12-16 on mid-range hardware.
