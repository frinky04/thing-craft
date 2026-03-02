use std::collections::HashMap;

use crate::world::{
    BlockRegistry, ChunkData, ChunkPos, CHUNK_DEPTH, CHUNK_EDGE_SLICE_VOLUME, CHUNK_HEIGHT,
    CHUNK_SECTION_COUNT, CHUNK_WIDTH, SECTION_HEIGHT,
};

const AIR_ID: u8 = 0;

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
                sky_light[index] = chunk.sky_light(x, y, z);
                block_light[index] = chunk.block_light(x, y, z);
            }
        }

        Self {
            blocks,
            sky_light,
            block_light,
        }
    }

    fn block_at(&self, y: u8, lateral: u8) -> u8 {
        self.blocks[edge_index(y, lateral)]
    }

    fn raw_light_at(&self, y: u8, lateral: u8) -> u8 {
        let index = edge_index(y, lateral);
        self.sky_light[index].max(self.block_light[index])
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
    pub light_rgba: [u8; 4],
}

impl MeshVertex {
    pub const ATTRS: [wgpu::VertexAttribute; 3] =
        wgpu::vertex_attr_array![0 => Float32x3, 1 => Float32x2, 2 => Unorm8x4];

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
    build_chunk_mesh_with_neighbor_lookup(
        chunk,
        registry,
        0,
        CHUNK_HEIGHT as i32,
        |x, y, z| neighbor_block(chunk, x, y, z),
        |x, y, z| neighbor_light(chunk, x, y, z),
    )
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
    let section = usize::from(section_y);
    assert!(section < CHUNK_SECTION_COUNT, "section_y out of range");
    let y_start = section * SECTION_HEIGHT;
    let y_end = y_start + SECTION_HEIGHT;
    build_chunk_mesh_with_neighbor_lookup(
        chunk,
        registry,
        y_start as i32,
        y_end as i32,
        |x, y, z| neighbor_block_with_slices(chunk, neighbor_slices, x, y, z),
        |x, y, z| neighbor_light_with_slices(chunk, neighbor_slices, x, y, z),
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
            build_chunk_mesh_with_neighbor_lookup(
                chunk,
                registry,
                0,
                CHUNK_HEIGHT as i32,
                |x, y, z| neighbor_block_from_region(chunks, &index_by_pos, chunk.pos, x, y, z),
                |x, y, z| neighbor_light_from_region(chunks, &index_by_pos, chunk.pos, x, y, z),
            )
        })
        .collect();
    merge_meshes(&meshes)
}

fn build_chunk_mesh_with_neighbor_lookup<FB, FL>(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    y_start: i32,
    y_end: i32,
    mut neighbor_block_lookup: FB,
    mut neighbor_light_lookup: FL,
) -> ChunkMesh
where
    FB: FnMut(i32, i32, i32) -> u8,
    FL: FnMut(i32, i32, i32) -> u8,
{
    let mut mesh = ChunkMesh::default();
    let chunk_origin_x = chunk.pos.x as f32 * CHUNK_WIDTH as f32;
    let chunk_origin_z = chunk.pos.z as f32 * CHUNK_DEPTH as f32;

    for local_x in 0..CHUNK_WIDTH as i32 {
        for local_z in 0..CHUNK_DEPTH as i32 {
            for y in y_start..y_end {
                let block_id = chunk.block(local_x as u8, y as u8, local_z as u8);
                if block_id == AIR_ID || !registry.is_defined_block(block_id) {
                    continue;
                }

                for face in FACES {
                    let nx = local_x + face.offset[0];
                    let ny = y + face.offset[1];
                    let nz = local_z + face.offset[2];
                    let neighbor_id = neighbor_block_lookup(nx, ny, nz);
                    if neighbor_id == block_id || registry.is_face_occluder(neighbor_id) {
                        continue;
                    }
                    let neighbor_light = neighbor_light_lookup(nx, ny, nz);

                    append_face(
                        &mut mesh,
                        [
                            local_x as f32 + chunk_origin_x,
                            y as f32,
                            local_z as f32 + chunk_origin_z,
                        ],
                        face,
                        registry.sprite_index_for_face(block_id, face.offset),
                        resolve_face_tint(
                            chunk,
                            registry,
                            block_id,
                            face.offset,
                            local_x as u8,
                            local_z as u8,
                        ),
                        neighbor_light,
                    );
                }
            }
        }
    }

    mesh
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

fn neighbor_light(chunk: &ChunkData, x: i32, y: i32, z: i32) -> u8 {
    if y < 0 {
        return 0;
    }
    if y >= CHUNK_HEIGHT as i32 {
        return 15;
    }

    let local_x = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
    let local_z = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
    raw_light_level(chunk, local_x, y as u8, local_z)
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

fn neighbor_light_with_slices(
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
        return raw_light_level(chunk, x as u8, y as u8, z as u8);
    }

    if x < 0 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors.neg_x.as_ref().map_or_else(
            || raw_light_level(chunk, 0, y as u8, z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8),
            |neighbor| neighbor.raw_light_at(y as u8, lateral),
        );
    }
    if x >= CHUNK_WIDTH as i32 {
        let lateral = z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8;
        return neighbors.pos_x.as_ref().map_or_else(
            || {
                raw_light_level(
                    chunk,
                    (CHUNK_WIDTH - 1) as u8,
                    y as u8,
                    z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8,
                )
            },
            |neighbor| neighbor.raw_light_at(y as u8, lateral),
        );
    }
    if z < 0 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors.neg_z.as_ref().map_or_else(
            || raw_light_level(chunk, x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8, y as u8, 0),
            |neighbor| neighbor.raw_light_at(y as u8, lateral),
        );
    }
    if z >= CHUNK_DEPTH as i32 {
        let lateral = x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8;
        return neighbors.pos_z.as_ref().map_or_else(
            || {
                raw_light_level(
                    chunk,
                    x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8,
                    y as u8,
                    (CHUNK_DEPTH - 1) as u8,
                )
            },
            |neighbor| neighbor.raw_light_at(y as u8, lateral),
        );
    }

    raw_light_level(
        chunk,
        x.clamp(0, CHUNK_WIDTH as i32 - 1) as u8,
        y as u8,
        z.clamp(0, CHUNK_DEPTH as i32 - 1) as u8,
    )
}

fn edge_index(y: u8, lateral: u8) -> usize {
    usize::from(lateral) * CHUNK_HEIGHT + usize::from(y)
}

fn resolve_face_tint(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    block_id: u8,
    face_offset: [i32; 3],
    local_x: u8,
    local_z: u8,
) -> [u8; 3] {
    if registry.face_uses_biome_tint(block_id, face_offset) {
        chunk.grass_tint_at(local_x, local_z)
    } else {
        registry.face_tint_rgb(block_id, face_offset)
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

fn neighbor_light_from_region(
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
        raw_light_level(&chunks[*index], local_x, y as u8, local_z)
    })
}

fn raw_light_level(chunk: &ChunkData, local_x: u8, y: u8, local_z: u8) -> u8 {
    chunk
        .sky_light(local_x, y, local_z)
        .max(chunk.block_light(local_x, y, local_z))
}

fn append_face(
    mesh: &mut ChunkMesh,
    base_pos: [f32; 3],
    face: FaceDef,
    sprite_index: u16,
    tint_rgb: [u8; 3],
    neighbor_light: u8,
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
    let shaded_tint = apply_alpha_light(tint_rgb, neighbor_light, face.offset);
    for (corner, tex) in face.corners.iter().zip(uv.iter()) {
        mesh.vertices.push(MeshVertex {
            position: [
                base_pos[0] + corner[0],
                base_pos[1] + corner[1],
                base_pos[2] + corner[2],
            ],
            uv: *tex,
            light_rgba: [shaded_tint[0], shaded_tint[1], shaded_tint[2], u8::MAX],
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

fn alpha_brightness(light_level: u8) -> f32 {
    let min_brightness = 0.05;
    let g = 1.0 - f32::from(light_level.min(15)) / 15.0;
    ((1.0 - g) / (g * 3.0 + 1.0)) * (1.0 - min_brightness) + min_brightness
}

fn apply_alpha_light(rgb: [u8; 3], light_level: u8, face_offset: [i32; 3]) -> [u8; 3] {
    let shade = alpha_face_scale(face_offset) * alpha_brightness(light_level);
    [
        (f32::from(rgb[0]) * shade).round().clamp(0.0, 255.0) as u8,
        (f32::from(rgb[1]) * shade).round().clamp(0.0, 255.0) as u8,
        (f32::from(rgb[2]) * shade).round().clamp(0.0, 255.0) as u8,
    ]
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
    fn face_light_uses_neighbor_raw_light_level() {
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
            let rgb = [
                vertex.light_rgba[0],
                vertex.light_rgba[1],
                vertex.light_rgba[2],
            ];
            let expected = apply_alpha_light([u8::MAX; 3], level, FACES[face_index].offset);
            assert_eq!(rgb, expected);
        }

        block[ChunkData::index(2, 1, 1)] = 14;
        sky[ChunkData::index(2, 1, 1)] = 2;
        chunk.apply_light_channels(&sky, &block);
        let mesh = build_chunk_mesh(&chunk, &registry);
        let east = mesh.vertices[4];
        let east_rgb = [east.light_rgba[0], east.light_rgba[1], east.light_rgba[2]];
        assert_eq!(
            east_rgb,
            apply_alpha_light([u8::MAX; 3], 14, FACES[1].offset)
        );
    }
}
