use std::collections::HashMap;

use crate::world::{BlockRegistry, ChunkData, ChunkPos, CHUNK_DEPTH, CHUNK_HEIGHT, CHUNK_WIDTH};

const AIR_ID: u8 = 0;

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
    build_chunk_mesh_with_neighbor_lookup(chunk, registry, |x, y, z| neighbor_block(chunk, x, y, z))
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
            build_chunk_mesh_with_neighbor_lookup(chunk, registry, |x, y, z| {
                neighbor_block_from_region(chunks, &index_by_pos, chunk.pos, x, y, z)
            })
        })
        .collect();
    merge_meshes(&meshes)
}

fn build_chunk_mesh_with_neighbor_lookup<F>(
    chunk: &ChunkData,
    registry: &BlockRegistry,
    mut neighbor_lookup: F,
) -> ChunkMesh
where
    F: FnMut(i32, i32, i32) -> u8,
{
    let mut mesh = ChunkMesh::default();
    let chunk_origin_x = chunk.pos.x as f32 * CHUNK_WIDTH as f32;
    let chunk_origin_z = chunk.pos.z as f32 * CHUNK_DEPTH as f32;

    for local_x in 0..CHUNK_WIDTH as i32 {
        for local_z in 0..CHUNK_DEPTH as i32 {
            for y in 0..CHUNK_HEIGHT as i32 {
                let block_id = chunk.block(local_x as u8, y as u8, local_z as u8);
                if block_id == AIR_ID || !registry.is_defined_block(block_id) {
                    continue;
                }

                for face in FACES {
                    let nx = local_x + face.offset[0];
                    let ny = y + face.offset[1];
                    let nz = local_z + face.offset[2];
                    let neighbor_id = neighbor_lookup(nx, ny, nz);
                    if registry.is_face_occluder(neighbor_id) {
                        continue;
                    }

                    append_face(
                        &mut mesh,
                        local_x as f32 + chunk_origin_x,
                        y as f32,
                        local_z as f32 + chunk_origin_z,
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

fn append_face(
    mesh: &mut ChunkMesh,
    base_x: f32,
    base_y: f32,
    base_z: f32,
    face: FaceDef,
    sprite_index: u16,
    tint_rgb: [u8; 3],
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
    let shade = face_shade(face.offset);
    let shaded_tint = apply_shade(tint_rgb, shade);
    for (corner, tex) in face.corners.iter().zip(uv.iter()) {
        mesh.vertices.push(MeshVertex {
            position: [base_x + corner[0], base_y + corner[1], base_z + corner[2]],
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

fn face_shade(face_offset: [i32; 3]) -> u8 {
    if face_offset[1] > 0 {
        255
    } else if face_offset[1] < 0 {
        128
    } else if face_offset[2] != 0 {
        204
    } else {
        153
    }
}

fn apply_shade(rgb: [u8; 3], shade: u8) -> [u8; 3] {
    [
        ((u16::from(rgb[0]) * u16::from(shade)) / 255) as u8,
        ((u16::from(rgb[1]) * u16::from(shade)) / 255) as u8,
        ((u16::from(rgb[2]) * u16::from(shade)) / 255) as u8,
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
        assert_eq!(face_shade([0, 1, 0]), 255);
        assert_eq!(face_shade([0, -1, 0]), 128);
        assert_eq!(face_shade([0, 0, 1]), 204);
        assert_eq!(face_shade([0, 0, -1]), 204);
        assert_eq!(face_shade([1, 0, 0]), 153);
        assert_eq!(face_shade([-1, 0, 0]), 153);
    }
}
