use std::collections::HashMap;
use std::f32::consts::FRAC_PI_2;

use crate::world::{
    BiomeTintKind, BlockRegistry, ChunkData, ChunkPos, CHUNK_DEPTH, CHUNK_EDGE_SLICE_VOLUME,
    CHUNK_HEIGHT, CHUNK_SECTION_COUNT, CHUNK_WIDTH, ICE_ID, SECTION_HEIGHT,
};

const AIR_ID: u8 = 0;

// ---------------------------------------------------------------------------
// Liquid height helpers (MC Alpha 1.2.6 formulas)
// ---------------------------------------------------------------------------

/// Height of a source/falling liquid block: 8/9 ≈ 0.889.
const SOURCE_HEIGHT: f32 = 8.0 / 9.0;

/// MC formula: height = 1.0 - (effective_meta + 1) / 9.0
/// Source/falling (meta 0 or ≥8): 8/9 ≈ 0.889
/// Shallowest (meta 7): 1/9 ≈ 0.111
fn liquid_height_from_meta(meta: u8) -> f32 {
    let effective = if meta >= 8 { 0 } else { meta };
    1.0 - (effective as f32 + 1.0) / 9.0
}

/// Pre-fetched 3×3 neighborhood at (bx-1..bx+1, bz-1..bz+1) for a liquid block.
/// Avoids redundant lookups shared between the 4 corner-height computations and
/// the flow-angle computation.
struct LiquidNeighborhood {
    /// Block IDs in row-major [x+1][z+1] layout (9 entries for the 3×3 grid).
    ids: [u8; 9],
    /// Block IDs one layer above.
    above_ids: [u8; 9],
    /// Metadata values for the 3×3 grid.
    metas: [u8; 9],
}

impl LiquidNeighborhood {
    fn fetch(
        bx: i32,
        by: i32,
        bz: i32,
        block_lookup: &mut impl FnMut(i32, i32, i32) -> u8,
        meta_lookup: &mut impl FnMut(i32, i32, i32) -> u8,
    ) -> Self {
        let mut ids = [0_u8; 9];
        let mut above_ids = [0_u8; 9];
        let mut metas = [0_u8; 9];
        for dx in -1..=1_i32 {
            for dz in -1..=1_i32 {
                let i = ((dx + 1) * 3 + (dz + 1)) as usize;
                ids[i] = block_lookup(bx + dx, by, bz + dz);
                above_ids[i] = block_lookup(bx + dx, by + 1, bz + dz);
                metas[i] = meta_lookup(bx + dx, by, bz + dz);
            }
        }
        Self {
            ids,
            above_ids,
            metas,
        }
    }

    #[inline]
    fn idx(dx: i32, dz: i32) -> usize {
        ((dx + 1) * 3 + (dz + 1)) as usize
    }
}

/// Compute interpolated liquid height at a single corner vertex using pre-cached data.
/// Corner at block-relative offset (cdx, cdz) where cdx,cdz ∈ {0, 1}.
fn corner_height_from_cache(
    cdx: i32,
    cdz: i32,
    liquid_id: u8,
    registry: &BlockRegistry,
    nh: &LiquidNeighborhood,
) -> f32 {
    let mut total = 0.0_f32;
    let mut count = 0_u32;

    // Sample the 4 blocks sharing this corner: (cdx-1..cdx, cdz-1..cdz)
    for dx in (cdx - 1)..=cdx {
        for dz in (cdz - 1)..=cdz {
            let i = LiquidNeighborhood::idx(dx, dz);
            if registry.is_same_liquid_kind(liquid_id, nh.above_ids[i]) {
                return 1.0; // corner is fully submerged
            }

            let bid = nh.ids[i];
            if registry.is_same_liquid_kind(liquid_id, bid) {
                let meta = nh.metas[i];
                if meta == 0 || meta >= 8 {
                    total += SOURCE_HEIGHT * 10.0;
                    count += 10;
                } else {
                    total += liquid_height_from_meta(meta);
                    count += 1;
                }
            }
            // Non-liquid blocks (air, solid) are excluded from the average,
            // matching MC's getLiquidHeight() returning -1.0 for non-liquid.
        }
    }

    if count == 0 {
        1.0
    } else {
        total / count as f32
    }
}

/// Compute corner heights for all 4 top-face corners of a liquid block.
/// Returns [h_00, h_10, h_01, h_11] indexed by (dx, dz) from block origin.
fn compute_liquid_corner_heights(
    nh: &LiquidNeighborhood,
    liquid_id: u8,
    registry: &BlockRegistry,
) -> [f32; 4] {
    [
        corner_height_from_cache(0, 0, liquid_id, registry, nh),
        corner_height_from_cache(1, 0, liquid_id, registry, nh),
        corner_height_from_cache(0, 1, liquid_id, registry, nh),
        corner_height_from_cache(1, 1, liquid_id, registry, nh),
    ]
}

/// Compute the flow direction angle for UV rotation on liquid top faces.
/// Based on MC's LiquidBlock.getFlow() → atan2(flow.z, flow.x) - PI/2.
/// Uses the pre-cached neighborhood plus extra below-lookups for edge flow.
#[allow(clippy::too_many_arguments)]
fn compute_liquid_flow_angle(
    nh: &LiquidNeighborhood,
    liquid_id: u8,
    registry: &BlockRegistry,
    bx: i32,
    by: i32,
    bz: i32,
    block_lookup: &mut impl FnMut(i32, i32, i32) -> u8,
    meta_lookup: &mut impl FnMut(i32, i32, i32) -> u8,
) -> Option<f32> {
    let self_meta = nh.metas[LiquidNeighborhood::idx(0, 0)];
    let self_level = if self_meta >= 8 {
        0
    } else {
        i32::from(self_meta)
    };

    let mut flow_x = 0.0_f32;
    let mut flow_z = 0.0_f32;

    let offsets: [(i32, i32, f32, f32); 4] = [
        (-1, 0, -1.0, 0.0), // -X
        (1, 0, 1.0, 0.0),   // +X
        (0, -1, 0.0, -1.0), // -Z
        (0, 1, 0.0, 1.0),   // +Z
    ];

    for (dx, dz, fx, fz) in offsets {
        let i = LiquidNeighborhood::idx(dx, dz);
        let nid = nh.ids[i];

        if registry.is_same_liquid_kind(liquid_id, nid) {
            let n_meta = nh.metas[i];
            let n_level = if n_meta >= 8 { 0 } else { i32::from(n_meta) };
            let diff = (n_level - self_level) as f32;
            flow_x += fx * diff;
            flow_z += fz * diff;
        } else if !registry.is_solid(nid) {
            // Check block below for downward flow (not in the 3×3 cache)
            let below_id = block_lookup(bx + dx, by - 1, bz + dz);
            if registry.is_same_liquid_kind(liquid_id, below_id) {
                let below_meta = meta_lookup(bx + dx, by - 1, bz + dz);
                let below_level = if below_meta >= 8 {
                    0
                } else {
                    i32::from(below_meta)
                };
                let diff = (below_level - (self_level - 8)) as f32;
                flow_x += fx * diff;
                flow_z += fz * diff;
            }
        }
    }

    if flow_x == 0.0 && flow_z == 0.0 {
        None
    } else {
        Some(flow_z.atan2(flow_x) - FRAC_PI_2)
    }
}

#[derive(Debug, Clone, Copy, Default)]
#[allow(dead_code)]
pub struct CardinalChunkNeighbors<'a> {
    pub neg_x: Option<&'a ChunkData>,
    pub pos_x: Option<&'a ChunkData>,
    pub neg_z: Option<&'a ChunkData>,
    pub pos_z: Option<&'a ChunkData>,
}

#[derive(Debug, Clone)]
pub struct NeighborEdgeSliceOwned {
    blocks: Box<[u8; CHUNK_EDGE_SLICE_VOLUME]>,
    metadata: Box<[u8; CHUNK_EDGE_SLICE_VOLUME]>,
    sky_light: Box<[u8; CHUNK_EDGE_SLICE_VOLUME]>,
    block_light: Box<[u8; CHUNK_EDGE_SLICE_VOLUME]>,
}

impl NeighborEdgeSliceOwned {
    #[must_use]
    pub fn from_neg_x(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, FaceAxis::NegX)
    }

    #[must_use]
    pub fn from_pos_x(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, FaceAxis::PosX)
    }

    #[must_use]
    pub fn from_neg_z(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, FaceAxis::NegZ)
    }

    #[must_use]
    pub fn from_pos_z(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, FaceAxis::PosZ)
    }

    fn from_chunk_face(chunk: &ChunkData, face: FaceAxis) -> Self {
        let mut blocks = Box::new([AIR_ID; CHUNK_EDGE_SLICE_VOLUME]);
        let mut metadata = Box::new([0_u8; CHUNK_EDGE_SLICE_VOLUME]);
        let mut sky_light = Box::new([0_u8; CHUNK_EDGE_SLICE_VOLUME]);
        let mut block_light = Box::new([0_u8; CHUNK_EDGE_SLICE_VOLUME]);
        for lateral in 0..CHUNK_WIDTH as u8 {
            for y in 0..CHUNK_HEIGHT as u8 {
                let (x, z) = match face {
                    FaceAxis::NegX => ((CHUNK_WIDTH as u8) - 1, lateral),
                    FaceAxis::PosX => (0, lateral),
                    FaceAxis::NegZ => (lateral, (CHUNK_DEPTH as u8) - 1),
                    FaceAxis::PosZ => (lateral, 0),
                };
                let index = edge_index(y, lateral);
                blocks[index] = chunk.block(x, y, z);
                metadata[index] = chunk.block_metadata(x, y, z);
                sky_light[index] = chunk.sky_light(x, y, z);
                block_light[index] = chunk.block_light(x, y, z);
            }
        }

        Self {
            blocks,
            metadata,
            sky_light,
            block_light,
        }
    }

    fn block_at(&self, y: u8, lateral: u8) -> u8 {
        self.blocks[edge_index(y, lateral)]
    }

    fn metadata_at(&self, y: u8, lateral: u8) -> u8 {
        self.metadata[edge_index(y, lateral)]
    }

    fn sky_light_at(&self, y: u8, lateral: u8) -> u8 {
        self.sky_light[edge_index(y, lateral)]
    }

    fn block_light_at(&self, y: u8, lateral: u8) -> u8 {
        self.block_light[edge_index(y, lateral)]
    }
}

#[derive(Debug, Clone, Default)]
pub struct CardinalNeighborSlicesOwned {
    pub neg_x: Option<NeighborEdgeSliceOwned>,
    pub pos_x: Option<NeighborEdgeSliceOwned>,
    pub neg_z: Option<NeighborEdgeSliceOwned>,
    pub pos_z: Option<NeighborEdgeSliceOwned>,
}

impl CardinalNeighborSlicesOwned {
    #[must_use]
    #[allow(dead_code)]
    pub fn from_chunk_neighbors(neighbors: &CardinalChunkNeighbors<'_>) -> Self {
        Self {
            neg_x: neighbors.neg_x.map(NeighborEdgeSliceOwned::from_neg_x),
            pos_x: neighbors.pos_x.map(NeighborEdgeSliceOwned::from_pos_x),
            neg_z: neighbors.neg_z.map(NeighborEdgeSliceOwned::from_neg_z),
            pos_z: neighbors.pos_z.map(NeighborEdgeSliceOwned::from_pos_z),
        }
    }
}

#[derive(Clone, Copy)]
enum FaceAxis {
    NegX,
    PosX,
    NegZ,
    PosZ,
}

#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq, bytemuck::Pod, bytemuck::Zeroable)]
pub struct MeshVertex {
    pub position: [f32; 3],
    pub uv: [f32; 2],
    pub tint_rgba: [u8; 4],
    pub light_data: [u8; 4],
}

impl MeshVertex {
    pub const ATTRS: [wgpu::VertexAttribute; 4] = wgpu::vertex_attr_array![
        0 => Float32x3,
        1 => Float32x2,
        2 => Unorm8x4,
        3 => Uint8x4
    ];

    #[must_use]
    pub fn layout() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRS,
        }
    }
}

#[derive(Debug, Clone, Default)]
pub struct ChunkMesh {
    pub vertices: Vec<MeshVertex>,
    pub indices: Vec<u32>,
}

#[derive(Debug, Clone, Default)]
pub struct SplitChunkMesh {
    pub opaque: ChunkMesh,
    pub transparent: ChunkMesh,
}

#[derive(Clone, Copy)]
struct FaceAdjust {
    vertex_alpha: u8,
    y_offset: f32,
    side_y_shrink: f32,
}

impl FaceAdjust {
    const OPAQUE: Self = Self {
        vertex_alpha: u8::MAX,
        y_offset: 0.0,
        side_y_shrink: 0.0,
    };
}

#[derive(Clone, Copy)]
struct FaceDef {
    offset: [i32; 3],
    corners: [[f32; 3]; 4],
}

const FACES: [FaceDef; 6] = [
    // -X
    FaceDef {
        offset: [-1, 0, 0],
        corners: [
            [0.0, 0.0, 1.0],
            [0.0, 1.0, 1.0],
            [0.0, 1.0, 0.0],
            [0.0, 0.0, 0.0],
        ],
    },
    // +X
    FaceDef {
        offset: [1, 0, 0],
        corners: [
            [1.0, 0.0, 0.0],
            [1.0, 1.0, 0.0],
            [1.0, 1.0, 1.0],
            [1.0, 0.0, 1.0],
        ],
    },
    // -Y
    FaceDef {
        offset: [0, -1, 0],
        corners: [
            [0.0, 0.0, 0.0],
            [1.0, 0.0, 0.0],
            [1.0, 0.0, 1.0],
            [0.0, 0.0, 1.0],
        ],
    },
    // +Y
    FaceDef {
        offset: [0, 1, 0],
        corners: [
            [0.0, 1.0, 1.0],
            [1.0, 1.0, 1.0],
            [1.0, 1.0, 0.0],
            [0.0, 1.0, 0.0],
        ],
    },
    // -Z
    FaceDef {
        offset: [0, 0, -1],
        corners: [
            [0.0, 0.0, 0.0],
            [0.0, 1.0, 0.0],
            [1.0, 1.0, 0.0],
            [1.0, 0.0, 0.0],
        ],
    },
    // +Z
    FaceDef {
        offset: [0, 0, 1],
        corners: [
            [1.0, 0.0, 1.0],
            [1.0, 1.0, 1.0],
            [0.0, 1.0, 1.0],
            [0.0, 0.0, 1.0],
        ],
    },
];

#[must_use]
pub fn build_chunk_mesh(chunk: &ChunkData, registry: &BlockRegistry) -> ChunkMesh {
    let split = build_split_chunk_mesh_with_neighbor_lookup(
        chunk,
        registry,
        0,
        CHUNK_HEIGHT as i32,
        |x, y, z| neighbor_block(chunk, x, y, z),
        |x, y, z| neighbor_metadata(chunk, x, y, z),
        |x, y, z| neighbor_sky_light(chunk, x, y, z),
        |x, y, z| neighbor_block_light(chunk, x, y, z),
    );
    merge_meshes(&[split.opaque, split.transparent])
}

#[must_use]
#[allow(dead_code)]
pub fn build_chunk_mesh_with_neighbors(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    neighbors: &CardinalChunkNeighbors<'_>,
) -> ChunkMesh {
    let neighbor_slices = CardinalNeighborSlicesOwned::from_chunk_neighbors(neighbors);
    build_chunk_mesh_with_neighbor_slices(chunk, registry, &neighbor_slices)
}

#[must_use]
#[allow(dead_code)]
pub fn build_chunk_mesh_with_neighbor_slices(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    neighbor_slices: &CardinalNeighborSlicesOwned,
) -> ChunkMesh {
    let mut section_meshes = Vec::with_capacity(CHUNK_SECTION_COUNT);
    for section_y in 0..CHUNK_SECTION_COUNT as u8 {
        section_meshes.push(build_chunk_section_mesh_with_neighbor_slices(
            chunk,
            registry,
            neighbor_slices,
            section_y,
        ));
    }
    merge_meshes(&section_meshes)
}

#[must_use]
pub fn build_chunk_section_mesh_with_neighbor_slices(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    neighbor_slices: &CardinalNeighborSlicesOwned,
    section_y: u8,
) -> ChunkMesh {
    let split = build_chunk_section_split_mesh_with_neighbor_slices(
        chunk,
        registry,
        neighbor_slices,
        section_y,
    );
    merge_meshes(&[split.opaque, split.transparent])
}

#[must_use]
pub fn build_chunk_section_split_mesh_with_neighbor_slices(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    neighbor_slices: &CardinalNeighborSlicesOwned,
    section_y: u8,
) -> SplitChunkMesh {
    let section = usize::from(section_y);
    assert!(section < CHUNK_SECTION_COUNT, "section_y out of range");
    let y_start = section * SECTION_HEIGHT;
    let y_end = y_start + SECTION_HEIGHT;
    build_split_chunk_mesh_with_neighbor_lookup(
        chunk,
        registry,
        y_start as i32,
        y_end as i32,
        |x, y, z| neighbor_block_with_slices(chunk, neighbor_slices, x, y, z),
        |x, y, z| neighbor_metadata_with_slices(chunk, neighbor_slices, x, y, z),
        |x, y, z| neighbor_sky_light_with_slices(chunk, neighbor_slices, x, y, z),
        |x, y, z| neighbor_block_light_with_slices(chunk, neighbor_slices, x, y, z),
    )
}

#[must_use]
pub fn build_region_mesh(chunks: &[ChunkData], registry: &BlockRegistry) -> ChunkMesh {
    if let [single] = chunks {
        return build_chunk_mesh(single, registry);
    }

    let index_by_pos: HashMap<ChunkPos, usize> = chunks
        .iter()
        .enumerate()
        .map(|(index, chunk)| (chunk.pos, index))
        .collect();

    let meshes: Vec<_> = chunks
        .iter()
        .map(|chunk| {
            let split = build_split_chunk_mesh_with_neighbor_lookup(
                chunk,
                registry,
                0,
                CHUNK_HEIGHT as i32,
                |x, y, z| neighbor_block_from_region(chunks, &index_by_pos, chunk.pos, x, y, z),
                |x, y, z| neighbor_metadata_from_region(chunks, &index_by_pos, chunk.pos, x, y, z),
                |x, y, z| neighbor_sky_light_from_region(chunks, &index_by_pos, chunk.pos, x, y, z),
                |x, y, z| {
                    neighbor_block_light_from_region(chunks, &index_by_pos, chunk.pos, x, y, z)
                },
            );
            merge_meshes(&[split.opaque, split.transparent])
        })
        .collect();
    merge_meshes(&meshes)
}

#[allow(clippy::too_many_arguments)]
fn build_split_chunk_mesh_with_neighbor_lookup<FB, FM, FS, FBL>(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    y_start: i32,
    y_end: i32,
    mut neighbor_block_lookup: FB,
    mut neighbor_metadata_lookup: FM,
    mut neighbor_sky_light_lookup: FS,
    mut neighbor_block_light_lookup: FBL,
) -> SplitChunkMesh
where
    FB: FnMut(i32, i32, i32) -> u8,
    FM: FnMut(i32, i32, i32) -> u8,
    FS: FnMut(i32, i32, i32) -> u8,
    FBL: FnMut(i32, i32, i32) -> u8,
{
    let mut split = SplitChunkMesh::default();
    let chunk_origin_x = chunk.pos.x as f32 * CHUNK_WIDTH as f32;
    let chunk_origin_z = chunk.pos.z as f32 * CHUNK_DEPTH as f32;

    for local_x in 0..CHUNK_WIDTH as i32 {
        for local_z in 0..CHUNK_DEPTH as i32 {
            for y in y_start..y_end {
                let block_id = chunk.block(local_x as u8, y as u8, local_z as u8);
                if block_id == AIR_ID || !registry.is_defined_block(block_id) {
                    continue;
                }

                let is_liquid = registry.is_liquid(block_id);

                if is_liquid {
                    // === Liquid meshing path ===
                    let is_water = registry.is_water(block_id);

                    let above_id = neighbor_block_lookup(local_x, y + 1, local_z);
                    let is_surface = !registry.is_same_liquid_kind(block_id, above_id);

                    // Pre-fetch the 3×3 neighborhood (shared by corner heights + flow)
                    let nh = if is_surface {
                        Some(LiquidNeighborhood::fetch(
                            local_x,
                            y,
                            local_z,
                            &mut neighbor_block_lookup,
                            &mut neighbor_metadata_lookup,
                        ))
                    } else {
                        None
                    };

                    let corner_heights = match &nh {
                        Some(nh) => compute_liquid_corner_heights(nh, block_id, registry),
                        None => [1.0; 4], // fully submerged
                    };

                    let flow_angle = match &nh {
                        Some(nh) => compute_liquid_flow_angle(
                            nh,
                            block_id,
                            registry,
                            local_x,
                            y,
                            local_z,
                            &mut neighbor_block_lookup,
                            &mut neighbor_metadata_lookup,
                        ),
                        None => None,
                    };

                    let base_pos = [
                        local_x as f32 + chunk_origin_x,
                        y as f32,
                        local_z as f32 + chunk_origin_z,
                    ];

                    for face in FACES {
                        let nx = local_x + face.offset[0];
                        let ny = y + face.offset[1];
                        let nz = local_z + face.offset[2];
                        let neighbor_id = neighbor_block_lookup(nx, ny, nz);

                        // Culling: same-kind liquid, opaque occluder, ice (water only)
                        if face.offset[1] > 0 {
                            if !is_surface {
                                continue;
                            }
                        } else {
                            if registry.is_same_liquid_kind(block_id, neighbor_id)
                                || registry.is_face_occluder(neighbor_id)
                            {
                                continue;
                            }
                            if is_water && neighbor_id == ICE_ID {
                                continue;
                            }
                        }

                        let (face_sky, face_block_light) = resolve_liquid_light(
                            neighbor_id,
                            nx,
                            ny,
                            nz,
                            registry,
                            &mut neighbor_sky_light_lookup,
                            &mut neighbor_block_light_lookup,
                        );

                        // Water → transparent; lava → opaque (MC render layers)
                        let target_mesh = if is_water {
                            &mut split.transparent
                        } else {
                            &mut split.opaque
                        };

                        // Water texture carries its own blue colour & alpha
                        // (generated by `patch_procedural_tiles`).  Lava is
                        // fully opaque in the texture.  Neutral vertex tint
                        // for both so the texture shows through faithfully.
                        let (tint, alpha) = ([255_u8, 255, 255], 255_u8);

                        let mut sprite_index =
                            registry.sprite_index_for_face(block_id, face.offset);
                        // MC top-face path switches to the side sprite when flow exists.
                        if face.offset[1] > 0 && flow_angle.is_some() {
                            sprite_index = sprite_index.saturating_add(1);
                        }

                        append_liquid_face(
                            target_mesh,
                            base_pos,
                            face,
                            sprite_index,
                            tint,
                            alpha,
                            face_sky,
                            face_block_light,
                            &corner_heights,
                            is_surface,
                            if face.offset[1] > 0 { flow_angle } else { None },
                        );
                    }
                } else {
                    // === Standard opaque block path ===
                    for face in FACES {
                        let nx = local_x + face.offset[0];
                        let ny = y + face.offset[1];
                        let nz = local_z + face.offset[2];
                        let neighbor_id = neighbor_block_lookup(nx, ny, nz);

                        if neighbor_id == block_id || registry.is_face_occluder(neighbor_id) {
                            continue;
                        }

                        let (face_sky, face_block_light) = resolve_liquid_light(
                            neighbor_id,
                            nx,
                            ny,
                            nz,
                            registry,
                            &mut neighbor_sky_light_lookup,
                            &mut neighbor_block_light_lookup,
                        );

                        let tint = resolve_face_tint(
                            chunk,
                            registry,
                            block_id,
                            face.offset,
                            local_x as u8,
                            local_z as u8,
                        );

                        append_face(
                            &mut split.opaque,
                            [
                                local_x as f32 + chunk_origin_x,
                                y as f32,
                                local_z as f32 + chunk_origin_z,
                            ],
                            face,
                            registry.sprite_index_for_face(block_id, face.offset),
                            tint,
                            face_sky,
                            face_block_light,
                            registry.is_leaves(block_id),
                            FaceAdjust::OPAQUE,
                        );
                    }
                }
            }
        }
    }

    split
}

#[must_use]
pub fn merge_meshes(meshes: &[ChunkMesh]) -> ChunkMesh {
    let mut merged = ChunkMesh::default();
    for mesh in meshes {
        let base_index = merged.vertices.len() as u32;
        merged.vertices.extend_from_slice(&mesh.vertices);
        merged
            .indices
            .extend(mesh.indices.iter().map(|index| index + base_index));
    }
    merged
}

fn neighbor_block(chunk: &ChunkData, x: i32, y: i32, z: i32) -> u8 {
    if x < 0
        || x >= CHUNK_WIDTH as i32
        || y < 0
        || y >= CHUNK_HEIGHT as i32
        || z < 0
        || z >= CHUNK_DEPTH as i32
    {
        return AIR_ID;
    }

    chunk.block(x as u8, y as u8, z as u8)
}

fn neighbor_metadata(chunk: &ChunkData, x: i32, y: i32, z: i32) -> u8 {
    if x < 0
        || x >= CHUNK_WIDTH as i32
        || y < 0
        || y >= CHUNK_HEIGHT as i32
        || z < 0
        || z >= CHUNK_DEPTH as i32
    {
        return 0;
    }

    chunk.block_metadata(x as u8, y as u8, z as u8)
}

fn neighbor_sky_light(chunk: &ChunkData, x: i32, y: i32, z: i32) -> u8 {
    if y < 0 {
        return 0;
    }
    if y >= CHUNK_HEIGHT as i32 {
        return 15;
    }

    let local_x = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
    let local_z = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
    chunk.sky_light(local_x, y as u8, local_z)
}

fn neighbor_block_light(chunk: &ChunkData, x: i32, y: i32, z: i32) -> u8 {
    if y < 0 {
        return 0;
    }
    if y >= CHUNK_HEIGHT as i32 {
        return 0;
    }

    let local_x = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
    let local_z = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
    chunk.block_light(local_x, y as u8, local_z)
}

fn neighbor_block_with_slices(
    chunk: &ChunkData,
    neighbors: &CardinalNeighborSlicesOwned,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return AIR_ID;
    }

    if (0..CHUNK_WIDTH as i32).contains(&x) && (0..CHUNK_DEPTH as i32).contains(&z) {
        return chunk.block(x as u8, y as u8, z as u8);
    }

    if x < 0 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors
            .neg_x
            .as_ref()
            .map_or(AIR_ID, |neighbor| neighbor.block_at(y as u8, lateral));
    }
    if x >= CHUNK_WIDTH as i32 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors
            .pos_x
            .as_ref()
            .map_or(AIR_ID, |neighbor| neighbor.block_at(y as u8, lateral));
    }
    if z < 0 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors
            .neg_z
            .as_ref()
            .map_or(AIR_ID, |neighbor| neighbor.block_at(y as u8, lateral));
    }
    if z >= CHUNK_DEPTH as i32 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors
            .pos_z
            .as_ref()
            .map_or(AIR_ID, |neighbor| neighbor.block_at(y as u8, lateral));
    }

    AIR_ID
}

fn neighbor_metadata_with_slices(
    chunk: &ChunkData,
    neighbors: &CardinalNeighborSlicesOwned,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return 0;
    }

    if (0..CHUNK_WIDTH as i32).contains(&x) && (0..CHUNK_DEPTH as i32).contains(&z) {
        return chunk.block_metadata(x as u8, y as u8, z as u8);
    }

    if x < 0 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors
            .neg_x
            .as_ref()
            .map_or(0, |neighbor| neighbor.metadata_at(y as u8, lateral));
    }
    if x >= CHUNK_WIDTH as i32 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors
            .pos_x
            .as_ref()
            .map_or(0, |neighbor| neighbor.metadata_at(y as u8, lateral));
    }
    if z < 0 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors
            .neg_z
            .as_ref()
            .map_or(0, |neighbor| neighbor.metadata_at(y as u8, lateral));
    }
    if z >= CHUNK_DEPTH as i32 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors
            .pos_z
            .as_ref()
            .map_or(0, |neighbor| neighbor.metadata_at(y as u8, lateral));
    }

    0
}

fn neighbor_sky_light_with_slices(
    chunk: &ChunkData,
    neighbors: &CardinalNeighborSlicesOwned,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if y < 0 {
        return 0;
    }
    if y >= CHUNK_HEIGHT as i32 {
        return 15;
    }

    if (0..CHUNK_WIDTH as i32).contains(&x) && (0..CHUNK_DEPTH as i32).contains(&z) {
        return chunk.sky_light(x as u8, y as u8, z as u8);
    }

    if x < 0 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors.neg_x.as_ref().map_or_else(
            || chunk.sky_light(0, y as u8, z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8),
            |neighbor| neighbor.sky_light_at(y as u8, lateral),
        );
    }
    if x >= CHUNK_WIDTH as i32 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors.pos_x.as_ref().map_or_else(
            || {
                chunk.sky_light(
                    (CHUNK_WIDTH - 1) as u8,
                    y as u8,
                    z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8,
                )
            },
            |neighbor| neighbor.sky_light_at(y as u8, lateral),
        );
    }
    if z < 0 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors.neg_z.as_ref().map_or_else(
            || chunk.sky_light(x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8, y as u8, 0),
            |neighbor| neighbor.sky_light_at(y as u8, lateral),
        );
    }
    if z >= CHUNK_DEPTH as i32 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors.pos_z.as_ref().map_or_else(
            || {
                chunk.sky_light(
                    x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8,
                    y as u8,
                    (CHUNK_DEPTH - 1) as u8,
                )
            },
            |neighbor| neighbor.sky_light_at(y as u8, lateral),
        );
    }

    chunk.sky_light(
        x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8,
        y as u8,
        z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8,
    )
}

fn edge_index(y: u8, lateral: u8) -> usize {
    usize::from(lateral) * CHUNK_HEIGHT + usize::from(y)
}

/// MC liquid brightness: liquid blocks report max(self, above) brightness.
/// Apply when sampling light from a position that may be occupied by liquid.
fn resolve_liquid_light(
    neighbor_id: u8,
    nx: i32,
    ny: i32,
    nz: i32,
    registry: &BlockRegistry,
    sky_lookup: &mut impl FnMut(i32, i32, i32) -> u8,
    block_light_lookup: &mut impl FnMut(i32, i32, i32) -> u8,
) -> (u8, u8) {
    let sky = sky_lookup(nx, ny, nz);
    let bl = block_light_lookup(nx, ny, nz);
    if registry.is_liquid(neighbor_id) {
        (
            sky.max(sky_lookup(nx, ny + 1, nz)),
            bl.max(block_light_lookup(nx, ny + 1, nz)),
        )
    } else {
        (sky, bl)
    }
}

fn resolve_face_tint(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    block_id: u8,
    face_offset: [i32; 3],
    local_x: u8,
    local_z: u8,
) -> [u8; 3] {
    match registry.biome_tint_kind(block_id, face_offset) {
        BiomeTintKind::Grass => chunk.grass_tint_at(local_x, local_z),
        BiomeTintKind::Foliage => chunk.foliage_tint_at(local_x, local_z),
        BiomeTintKind::None => registry.face_tint_rgb(block_id, face_offset),
    }
}

fn neighbor_block_from_region(
    chunks: &[ChunkData],
    index_by_pos: &HashMap<ChunkPos, usize>,
    chunk_pos: ChunkPos,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return AIR_ID;
    }

    let world_x = chunk_pos.x * CHUNK_WIDTH as i32 + x;
    let world_z = chunk_pos.z * CHUNK_DEPTH as i32 + z;
    let neighbor_chunk_pos = ChunkPos {
        x: world_x.div_euclid(CHUNK_WIDTH as i32),
        z: world_z.div_euclid(CHUNK_DEPTH as i32),
    };
    let local_x = world_x.rem_euclid(CHUNK_WIDTH as i32) as u8;
    let local_z = world_z.rem_euclid(CHUNK_DEPTH as i32) as u8;

    index_by_pos
        .get(&neighbor_chunk_pos)
        .map_or(AIR_ID, |index| {
            chunks[*index].block(local_x, y as u8, local_z)
        })
}

fn neighbor_metadata_from_region(
    chunks: &[ChunkData],
    index_by_pos: &HashMap<ChunkPos, usize>,
    chunk_pos: ChunkPos,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return 0;
    }

    let world_x = chunk_pos.x * CHUNK_WIDTH as i32 + x;
    let world_z = chunk_pos.z * CHUNK_DEPTH as i32 + z;
    let neighbor_chunk_pos = ChunkPos {
        x: world_x.div_euclid(CHUNK_WIDTH as i32),
        z: world_z.div_euclid(CHUNK_DEPTH as i32),
    };
    let local_x = world_x.rem_euclid(CHUNK_WIDTH as i32) as u8;
    let local_z = world_z.rem_euclid(CHUNK_DEPTH as i32) as u8;

    index_by_pos.get(&neighbor_chunk_pos).map_or(0, |index| {
        chunks[*index].block_metadata(local_x, y as u8, local_z)
    })
}

fn neighbor_block_light_with_slices(
    chunk: &ChunkData,
    neighbors: &CardinalNeighborSlicesOwned,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if y < 0 || y >= CHUNK_HEIGHT as i32 {
        return 0;
    }

    if (0..CHUNK_WIDTH as i32).contains(&x) && (0..CHUNK_DEPTH as i32).contains(&z) {
        return chunk.block_light(x as u8, y as u8, z as u8);
    }

    if x < 0 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors.neg_x.as_ref().map_or_else(
            || chunk.block_light(0, y as u8, z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8),
            |neighbor| neighbor.block_light_at(y as u8, lateral),
        );
    }
    if x >= CHUNK_WIDTH as i32 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors.pos_x.as_ref().map_or_else(
            || {
                chunk.block_light(
                    (CHUNK_WIDTH - 1) as u8,
                    y as u8,
                    z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8,
                )
            },
            |neighbor| neighbor.block_light_at(y as u8, lateral),
        );
    }
    if z < 0 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors.neg_z.as_ref().map_or_else(
            || chunk.block_light(x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8, y as u8, 0),
            |neighbor| neighbor.block_light_at(y as u8, lateral),
        );
    }
    if z >= CHUNK_DEPTH as i32 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors.pos_z.as_ref().map_or_else(
            || {
                chunk.block_light(
                    x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8,
                    y as u8,
                    (CHUNK_DEPTH - 1) as u8,
                )
            },
            |neighbor| neighbor.block_light_at(y as u8, lateral),
        );
    }

    chunk.block_light(
        x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8,
        y as u8,
        z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8,
    )
}

fn neighbor_sky_light_from_region(
    chunks: &[ChunkData],
    index_by_pos: &HashMap<ChunkPos, usize>,
    chunk_pos: ChunkPos,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if y < 0 {
        return 0;
    }
    if y >= CHUNK_HEIGHT as i32 {
        return 15;
    }

    let world_x = chunk_pos.x * CHUNK_WIDTH as i32 + x;
    let world_z = chunk_pos.z * CHUNK_DEPTH as i32 + z;
    let neighbor_chunk_pos = ChunkPos {
        x: world_x.div_euclid(CHUNK_WIDTH as i32),
        z: world_z.div_euclid(CHUNK_DEPTH as i32),
    };
    let local_x = world_x.rem_euclid(CHUNK_WIDTH as i32) as u8;
    let local_z = world_z.rem_euclid(CHUNK_DEPTH as i32) as u8;

    index_by_pos.get(&neighbor_chunk_pos).map_or(15, |index| {
        chunks[*index].sky_light(local_x, y as u8, local_z)
    })
}

fn neighbor_block_light_from_region(
    chunks: &[ChunkData],
    index_by_pos: &HashMap<ChunkPos, usize>,
    chunk_pos: ChunkPos,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    if y < 0 || y >= CHUNK_HEIGHT as i32 {
        return 0;
    }

    let world_x = chunk_pos.x * CHUNK_WIDTH as i32 + x;
    let world_z = chunk_pos.z * CHUNK_DEPTH as i32 + z;
    let neighbor_chunk_pos = ChunkPos {
        x: world_x.div_euclid(CHUNK_WIDTH as i32),
        z: world_z.div_euclid(CHUNK_DEPTH as i32),
    };
    let local_x = world_x.rem_euclid(CHUNK_WIDTH as i32) as u8;
    let local_z = world_z.rem_euclid(CHUNK_DEPTH as i32) as u8;

    index_by_pos.get(&neighbor_chunk_pos).map_or(0, |index| {
        chunks[*index].block_light(local_x, y as u8, local_z)
    })
}

#[allow(clippy::too_many_arguments)]
fn append_face(
    mesh: &mut ChunkMesh,
    base_pos: [f32; 3],
    face: FaceDef,
    sprite_index: u16,
    tint_rgb: [u8; 3],
    neighbor_sky_light: u8,
    neighbor_block_light: u8,
    is_leaves: bool,
    adjust: FaceAdjust,
) {
    let atlas_tile = 1.0 / 16.0;
    let sprite_x = f32::from(sprite_index % 16) * atlas_tile;
    let sprite_y = f32::from(sprite_index / 16) * atlas_tile;
    let uv = [
        [sprite_x, sprite_y + atlas_tile],
        [sprite_x, sprite_y],
        [sprite_x + atlas_tile, sprite_y],
        [sprite_x + atlas_tile, sprite_y + atlas_tile],
    ];

    let base_index = mesh.vertices.len() as u32;
    let face_scale = (alpha_face_scale(face.offset) * 255.0)
        .round()
        .clamp(0.0, 255.0) as u8;
    for (corner, tex) in face.corners.iter().zip(uv.iter()) {
        let mut pos_y = base_pos[1] + corner[1];
        if adjust.y_offset != 0.0 {
            pos_y += adjust.y_offset;
        }
        if adjust.side_y_shrink != 0.0 && corner[1] > 0.5 {
            pos_y += adjust.side_y_shrink;
        }
        mesh.vertices.push(MeshVertex {
            position: [base_pos[0] + corner[0], pos_y, base_pos[2] + corner[2]],
            uv: *tex,
            tint_rgba: [tint_rgb[0], tint_rgb[1], tint_rgb[2], adjust.vertex_alpha],
            light_data: [
                neighbor_sky_light,
                neighbor_block_light,
                face_scale,
                u8::from(is_leaves),
            ],
        });
    }

    mesh.indices.extend_from_slice(&[
        base_index,
        base_index + 1,
        base_index + 2,
        base_index,
        base_index + 2,
        base_index + 3,
    ]);
}

/// Emit a liquid face with per-corner interpolated heights and optional UV rotation.
///
/// `corner_heights` = [h_00, h_10, h_01, h_11] indexed by (dx, dz) from block origin.
/// For surface blocks, top vertices use these heights; for submerged blocks heights are 1.0.
/// `flow_angle` rotates the top face (+Y) UVs to visually indicate flow direction.
/// `None` means no flow angle (static top sprite sampling).
#[allow(clippy::too_many_arguments)]
fn append_liquid_face(
    mesh: &mut ChunkMesh,
    base_pos: [f32; 3],
    face: FaceDef,
    sprite_index: u16,
    tint_rgb: [u8; 3],
    vertex_alpha: u8,
    neighbor_sky_light: u8,
    neighbor_block_light: u8,
    corner_heights: &[f32; 4],
    is_surface: bool,
    flow_angle: Option<f32>,
) {
    let atlas_tile = 1.0 / 16.0;
    let sprite_x = f32::from(sprite_index % 16) * atlas_tile;
    let sprite_y = f32::from(sprite_index / 16) * atlas_tile;
    let uv = if face.offset[1] > 0 {
        // Match MC top-liquid UV path:
        // - no flow: sample centered in base top sprite
        // - flowing: switch to side sprite and offset by sin/cos(flow_angle)
        let (center_u, center_v, angle) = match flow_angle {
            Some(angle) => (sprite_x + atlas_tile, sprite_y + atlas_tile, angle),
            None => (
                sprite_x + atlas_tile * 0.5,
                sprite_y + atlas_tile * 0.5,
                0.0,
            ),
        };
        let du = angle.sin() * atlas_tile * 0.5;
        let dv = angle.cos() * atlas_tile * 0.5;
        [
            [center_u - dv + du, center_v + dv + du], // (x=0,z=1)
            [center_u + dv + du, center_v + dv - du], // (x=1,z=1)
            [center_u + dv - du, center_v - dv - du], // (x=1,z=0)
            [center_u - dv - du, center_v - dv + du], // (x=0,z=0)
        ]
    } else {
        [
            [sprite_x, sprite_y + atlas_tile],
            [sprite_x, sprite_y],
            [sprite_x + atlas_tile, sprite_y],
            [sprite_x + atlas_tile, sprite_y + atlas_tile],
        ]
    };

    let base_index = mesh.vertices.len() as u32;
    let face_scale = (alpha_face_scale(face.offset) * 255.0)
        .round()
        .clamp(0.0, 255.0) as u8;

    for (corner, tex) in face.corners.iter().zip(uv.iter()) {
        let pos_y = if is_surface && corner[1] > 0.5 {
            // Top vertex: use per-corner interpolated height
            // Map corner (x, z) to height index: h_00, h_10, h_01, h_11
            let xi = corner[0].round() as usize;
            let zi = corner[2].round() as usize;
            debug_assert!(xi <= 1 && zi <= 1);
            base_pos[1] + corner_heights[xi + zi * 2]
        } else {
            base_pos[1] + corner[1]
        };

        mesh.vertices.push(MeshVertex {
            position: [base_pos[0] + corner[0], pos_y, base_pos[2] + corner[2]],
            uv: *tex,
            tint_rgba: [tint_rgb[0], tint_rgb[1], tint_rgb[2], vertex_alpha],
            light_data: [neighbor_sky_light, neighbor_block_light, face_scale, 0],
        });
    }

    mesh.indices.extend_from_slice(&[
        base_index,
        base_index + 1,
        base_index + 2,
        base_index,
        base_index + 2,
        base_index + 3,
    ]);
}

fn alpha_face_scale(face_offset: [i32; 3]) -> f32 {
    if face_offset[1] > 0 {
        1.0
    } else if face_offset[1] < 0 {
        0.5
    } else if face_offset[2] != 0 {
        0.8
    } else {
        0.6
    }
}

#[cfg(test)]
fn alpha_brightness(light_level: u8) -> f32 {
    let min_brightness = 0.05;
    let g = 1.0 - f32::from(light_level.min(15)) / 15.0;
    ((1.0 - g) / (g * 3.0 + 1.0)) * (1.0 - min_brightness) + min_brightness
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::world::{ChunkPos, CHUNK_VOLUME};

    #[test]
    fn single_block_produces_six_faces() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(1, 1, 1, 1);

        let mesh = build_chunk_mesh(&chunk, &registry);
        assert_eq!(mesh.vertices.len(), 24);
        assert_eq!(mesh.indices.len(), 36);
    }

    #[test]
    fn adjacent_blocks_cull_shared_face() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(1, 1, 1, 1);
        chunk.set_block(2, 1, 1, 1);

        let mesh = build_chunk_mesh(&chunk, &registry);
        assert_eq!(mesh.vertices.len(), 40);
        assert_eq!(mesh.indices.len(), 60);
    }

    #[test]
    fn adjacent_ice_blocks_cull_shared_face() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(1, 1, 1, 79);
        chunk.set_block(2, 1, 1, 79);

        let mesh = build_chunk_mesh(&chunk, &registry);
        assert_eq!(mesh.vertices.len(), 40);
        assert_eq!(mesh.indices.len(), 60);
    }

    #[test]
    fn neighbor_aware_meshing_culls_boundary_faces() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        let mut east_neighbor = ChunkData::new(ChunkPos { x: 1, z: 0 }, AIR_ID);
        chunk.set_block(15, 10, 5, 1);
        east_neighbor.set_block(0, 10, 5, 1);

        let isolated = build_chunk_mesh(&chunk, &registry);
        let neighbors = CardinalChunkNeighbors {
            pos_x: Some(&east_neighbor),
            ..Default::default()
        };
        let neighbor_aware = build_chunk_mesh_with_neighbors(&chunk, &registry, &neighbors);

        assert_eq!(isolated.vertices.len(), 24);
        assert_eq!(neighbor_aware.vertices.len(), 20);
        assert_eq!(isolated.indices.len(), 36);
        assert_eq!(neighbor_aware.indices.len(), 30);
    }

    #[test]
    fn full_chunk_is_non_empty_mesh() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, 1);
        for y in 0..(CHUNK_HEIGHT as u8) {
            for x in 0..(CHUNK_WIDTH as u8) {
                for z in 0..(CHUNK_DEPTH as u8) {
                    if y > 0 {
                        chunk.set_block(x, y, z, AIR_ID);
                    }
                }
            }
        }

        let mesh = build_chunk_mesh(&chunk, &registry);
        assert!(!mesh.vertices.is_empty());
        assert!(!mesh.indices.is_empty());
        assert!(mesh.vertices.len() < CHUNK_VOLUME);
    }

    #[test]
    fn merge_meshes_offsets_indices() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk_a = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        let mut chunk_b = ChunkData::new(ChunkPos { x: 1, z: 0 }, AIR_ID);
        chunk_a.set_block(1, 1, 1, 1);
        chunk_b.set_block(1, 1, 1, 1);

        let mesh_a = build_chunk_mesh(&chunk_a, &registry);
        let mesh_b = build_chunk_mesh(&chunk_b, &registry);
        let merged = merge_meshes(&[mesh_a.clone(), mesh_b.clone()]);

        assert_eq!(
            merged.vertices.len(),
            mesh_a.vertices.len() + mesh_b.vertices.len()
        );
        assert_eq!(
            merged.indices.len(),
            mesh_a.indices.len() + mesh_b.indices.len()
        );
        assert!(merged
            .indices
            .iter()
            .any(|index| *index >= mesh_a.vertices.len() as u32));
    }

    #[test]
    fn region_meshing_culls_shared_boundary_faces_across_chunks() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk_a = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        let mut chunk_b = ChunkData::new(ChunkPos { x: 1, z: 0 }, AIR_ID);

        chunk_a.set_block(15, 10, 5, 1);
        chunk_b.set_block(0, 10, 5, 1);

        let per_chunk_merged = merge_meshes(&[
            build_chunk_mesh(&chunk_a, &registry),
            build_chunk_mesh(&chunk_b, &registry),
        ]);
        let region_mesh = build_region_mesh(&[chunk_a, chunk_b], &registry);

        assert_eq!(per_chunk_merged.vertices.len(), 48);
        assert_eq!(per_chunk_merged.indices.len(), 72);
        assert_eq!(region_mesh.vertices.len(), 40);
        assert_eq!(region_mesh.indices.len(), 60);
    }

    #[test]
    fn region_meshing_matches_single_chunk_when_isolated() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 4, z: -2 }, AIR_ID);
        chunk.set_block(7, 20, 7, 1);
        chunk.set_block(8, 20, 7, 1);

        let single_mesh = build_chunk_mesh(&chunk, &registry);
        let region_mesh = build_region_mesh(&[chunk], &registry);

        assert_eq!(single_mesh.vertices.len(), region_mesh.vertices.len());
        assert_eq!(single_mesh.indices.len(), region_mesh.indices.len());
    }

    #[test]
    fn face_corners_follow_outward_winding() {
        for face in FACES {
            let a = glam::Vec3::from_array(face.corners[0]);
            let b = glam::Vec3::from_array(face.corners[1]);
            let c = glam::Vec3::from_array(face.corners[2]);
            let normal = (b - a).cross(c - a).normalize_or_zero();
            let outward = glam::Vec3::new(
                face.offset[0] as f32,
                face.offset[1] as f32,
                face.offset[2] as f32,
            );
            assert!(normal.dot(outward) > 0.99);
        }
    }

    #[test]
    fn directional_face_shading_matches_alpha_profile() {
        assert_eq!(alpha_face_scale([0, 1, 0]), 1.0);
        assert_eq!(alpha_face_scale([0, -1, 0]), 0.5);
        assert_eq!(alpha_face_scale([0, 0, 1]), 0.8);
        assert_eq!(alpha_face_scale([0, 0, -1]), 0.8);
        assert_eq!(alpha_face_scale([1, 0, 0]), 0.6);
        assert_eq!(alpha_face_scale([-1, 0, 0]), 0.6);
    }

    #[test]
    fn alpha_brightness_curve_matches_dimension_endpoints() {
        assert!((alpha_brightness(0) - 0.05).abs() < 0.0001);
        assert!((alpha_brightness(15) - 1.0).abs() < 0.0001);
    }

    #[test]
    fn face_light_encodes_neighbor_sky_block_and_face_scale() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(1, 1, 1, 1);

        let mut sky = [0_u8; CHUNK_VOLUME];
        let mut block = [0_u8; CHUNK_VOLUME];
        sky[ChunkData::index(0, 1, 1)] = 5; // -X
        sky[ChunkData::index(2, 1, 1)] = 15; // +X
        sky[ChunkData::index(1, 0, 1)] = 8; // -Y
        sky[ChunkData::index(1, 2, 1)] = 12; // +Y
        sky[ChunkData::index(1, 1, 0)] = 4; // -Z
        sky[ChunkData::index(1, 1, 2)] = 10; // +Z
        chunk.apply_light_channels(&sky, &block);

        let mesh = build_chunk_mesh(&chunk, &registry);
        assert_eq!(mesh.vertices.len(), 24);

        let expected_levels = [5_u8, 15, 8, 12, 4, 10];
        for (face_index, level) in expected_levels.into_iter().enumerate() {
            let vertex = mesh.vertices[face_index * 4];
            assert_eq!(vertex.light_data[0], level);
            assert_eq!(vertex.light_data[1], 0);
            let expected_scale = (alpha_face_scale(FACES[face_index].offset) * 255.0)
                .round()
                .clamp(0.0, 255.0) as u8;
            assert_eq!(vertex.light_data[2], expected_scale);
            assert_eq!(vertex.light_data[3], 0);
        }

        block[ChunkData::index(2, 1, 1)] = 14;
        sky[ChunkData::index(2, 1, 1)] = 2;
        chunk.apply_light_channels(&sky, &block);
        let mesh = build_chunk_mesh(&chunk, &registry);
        let east = mesh.vertices[4];
        assert_eq!(east.light_data[0], 2);
        assert_eq!(east.light_data[1], 14);
    }

    #[test]
    fn leaves_faces_encode_leaf_marker_for_shader_mode_switching() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(1, 1, 1, 18);

        let mesh = build_chunk_mesh(&chunk, &registry);
        assert!(!mesh.vertices.is_empty());
        assert!(mesh.vertices.iter().all(|vertex| vertex.light_data[3] == 1));
    }
}
