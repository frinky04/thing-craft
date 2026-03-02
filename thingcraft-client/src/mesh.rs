use crate::world::{BlockRegistry, ChunkData, CHUNK_DEPTH, CHUNK_HEIGHT, CHUNK_WIDTH};

const AIR_ID: u8 = 0;

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct MeshVertex {
    pub position: [f32; 3],
    pub uv: [f32; 2],
    pub emitted_light: u8,
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
            [1.0, 0.0, 0.0],
            [1.0, 1.0, 0.0],
            [0.0, 1.0, 0.0],
            [0.0, 0.0, 0.0],
        ],
    },
    // +Z
    FaceDef {
        offset: [0, 0, 1],
        corners: [
            [0.0, 0.0, 1.0],
            [0.0, 1.0, 1.0],
            [1.0, 1.0, 1.0],
            [1.0, 0.0, 1.0],
        ],
    },
];

#[must_use]
pub fn build_chunk_mesh(chunk: &ChunkData, registry: &BlockRegistry) -> ChunkMesh {
    let mut mesh = ChunkMesh::default();

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
                    let neighbor_id = neighbor_block(chunk, nx, ny, nz);
                    if registry.is_face_occluder(neighbor_id) {
                        continue;
                    }

                    append_face(
                        &mut mesh,
                        local_x as f32,
                        y as f32,
                        local_z as f32,
                        face,
                        registry.sprite_index_of(block_id),
                        registry.emitted_light_of(block_id),
                    );
                }
            }
        }
    }

    mesh
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

fn append_face(
    mesh: &mut ChunkMesh,
    base_x: f32,
    base_y: f32,
    base_z: f32,
    face: FaceDef,
    sprite_index: u16,
    emitted_light: u8,
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
    for (corner, tex) in face.corners.iter().zip(uv.iter()) {
        mesh.vertices.push(MeshVertex {
            position: [base_x + corner[0], base_y + corner[1], base_z + corner[2]],
            uv: *tex,
            emitted_light,
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
}
