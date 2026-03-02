use std::collections::VecDeque;

use crate::world::{
    BlockRegistry, ChunkData, CHUNK_DEPTH, CHUNK_EDGE_SLICE_VOLUME, CHUNK_HEIGHT,
    CHUNK_SECTION_COUNT, CHUNK_VOLUME, CHUNK_WIDTH, SECTION_HEIGHT,
};

const LIGHT_DIRECTIONS: [[i32; 3]; 6] = [
    [-1, 0, 0],
    [1, 0, 0],
    [0, -1, 0],
    [0, 1, 0],
    [0, 0, -1],
    [0, 0, 1],
];

#[derive(Debug, Clone, Default)]
pub struct CardinalChunkNeighborsOwned {
    pub neg_x: Option<LightEdgeSlice>,
    pub pos_x: Option<LightEdgeSlice>,
    pub neg_z: Option<LightEdgeSlice>,
    pub pos_z: Option<LightEdgeSlice>,
}

#[derive(Debug, Clone)]
pub struct LightEdgeSlice {
    sky_light: Box<[u8; CHUNK_EDGE_SLICE_VOLUME]>,
    block_light: Box<[u8; CHUNK_EDGE_SLICE_VOLUME]>,
}

impl LightEdgeSlice {
    #[must_use]
    pub fn from_neg_x(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, NeighborFace::NegX)
    }

    #[must_use]
    pub fn from_pos_x(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, NeighborFace::PosX)
    }

    #[must_use]
    pub fn from_neg_z(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, NeighborFace::NegZ)
    }

    #[must_use]
    pub fn from_pos_z(chunk: &ChunkData) -> Self {
        Self::from_chunk_face(chunk, NeighborFace::PosZ)
    }

    fn from_chunk_face(chunk: &ChunkData, face: NeighborFace) -> Self {
        let mut sky_light = Box::new([0_u8; CHUNK_EDGE_SLICE_VOLUME]);
        let mut block_light = Box::new([0_u8; CHUNK_EDGE_SLICE_VOLUME]);
        for lateral in 0..CHUNK_WIDTH as u8 {
            for y in 0..CHUNK_HEIGHT as u8 {
                let (x, z) = match face {
                    NeighborFace::NegX => ((CHUNK_WIDTH as u8) - 1, lateral),
                    NeighborFace::PosX => (0, lateral),
                    NeighborFace::NegZ => (lateral, (CHUNK_DEPTH as u8) - 1),
                    NeighborFace::PosZ => (lateral, 0),
                };
                let index = edge_index(y, lateral);
                sky_light[index] = chunk.sky_light(x, y, z);
                block_light[index] = chunk.block_light(x, y, z);
            }
        }
        Self {
            sky_light,
            block_light,
        }
    }

    fn channel_at(&self, channel: LightChannel, y: u8, lateral: u8) -> u8 {
        let index = edge_index(y, lateral);
        match channel {
            LightChannel::Sky => self.sky_light[index],
            LightChannel::Block => self.block_light[index],
        }
    }
}

#[derive(Clone, Copy)]
enum NeighborFace {
    NegX,
    PosX,
    NegZ,
    PosZ,
}

#[derive(Debug, Clone)]
pub struct LightingOutput {
    pub sky_light: Box<[u8; CHUNK_VOLUME]>,
    pub block_light: Box<[u8; CHUNK_VOLUME]>,
    pub changed: bool,
    pub changed_sections_mask: u8,
}

#[derive(Debug, Clone, Copy)]
enum LightChannel {
    Sky,
    Block,
}

#[must_use]
pub fn relight_chunk(
    chunk: &ChunkData,
    neighbors: &CardinalChunkNeighborsOwned,
    registry: &BlockRegistry,
) -> LightingOutput {
    let mut sky_light = Box::new([0_u8; CHUNK_VOLUME]);
    let mut block_light = Box::new([0_u8; CHUNK_VOLUME]);

    let mut sky_queue = VecDeque::with_capacity(CHUNK_VOLUME);
    seed_initial_sky_light(&mut sky_light, &mut sky_queue, chunk, registry);
    seed_boundary_channel(
        &mut sky_light,
        &mut sky_queue,
        chunk,
        neighbors,
        registry,
        LightChannel::Sky,
    );
    propagate_channel(&mut sky_light, &mut sky_queue, chunk, registry);

    let mut block_queue = VecDeque::with_capacity(CHUNK_VOLUME / 4);
    seed_initial_block_light(&mut block_light, &mut block_queue, chunk, registry);
    seed_boundary_channel(
        &mut block_light,
        &mut block_queue,
        chunk,
        neighbors,
        registry,
        LightChannel::Block,
    );
    propagate_channel(&mut block_light, &mut block_queue, chunk, registry);

    let (changed, changed_sections_mask) = channel_differs(chunk, &sky_light, &block_light);
    LightingOutput {
        sky_light,
        block_light,
        changed,
        changed_sections_mask,
    }
}

fn seed_initial_sky_light(
    levels: &mut [u8; CHUNK_VOLUME],
    queue: &mut VecDeque<usize>,
    chunk: &ChunkData,
    registry: &BlockRegistry,
) {
    for local_x in 0..CHUNK_WIDTH as u8 {
        for local_z in 0..CHUNK_DEPTH as u8 {
            let mut sky = 15_u8;
            for y in (0..CHUNK_HEIGHT as u8).rev() {
                let block_id = chunk.block(local_x, y, local_z);
                let opacity = registry.opacity_of(block_id);
                if opacity > 0 {
                    sky = sky.saturating_sub(opacity.clamp(1, 15));
                }
                let index = light_index(local_x as usize, y as usize, local_z as usize);
                try_seed(levels, queue, index, sky);
            }
        }
    }
}

fn seed_initial_block_light(
    levels: &mut [u8; CHUNK_VOLUME],
    queue: &mut VecDeque<usize>,
    chunk: &ChunkData,
    registry: &BlockRegistry,
) {
    for local_x in 0..CHUNK_WIDTH as u8 {
        for local_z in 0..CHUNK_DEPTH as u8 {
            for y in 0..CHUNK_HEIGHT as u8 {
                let emitted = registry.emitted_light_of(chunk.block(local_x, y, local_z));
                if emitted == 0 {
                    continue;
                }
                let index = light_index(local_x as usize, y as usize, local_z as usize);
                try_seed(levels, queue, index, emitted);
            }
        }
    }
}

fn seed_boundary_channel(
    levels: &mut [u8; CHUNK_VOLUME],
    queue: &mut VecDeque<usize>,
    chunk: &ChunkData,
    neighbors: &CardinalChunkNeighborsOwned,
    registry: &BlockRegistry,
    channel: LightChannel,
) {
    for y in 0..CHUNK_HEIGHT as u8 {
        for z in 0..CHUNK_DEPTH as u8 {
            if let Some(neighbor) = neighbors.neg_x.as_ref() {
                let incoming = neighbor.channel_at(channel, y, z);
                seed_from_neighbor(levels, queue, chunk, registry, [0, y, z], incoming);
            }
            if let Some(neighbor) = neighbors.pos_x.as_ref() {
                let incoming = neighbor.channel_at(channel, y, z);
                seed_from_neighbor(
                    levels,
                    queue,
                    chunk,
                    registry,
                    [(CHUNK_WIDTH as u8) - 1, y, z],
                    incoming,
                );
            }
        }

        for x in 0..CHUNK_WIDTH as u8 {
            if let Some(neighbor) = neighbors.neg_z.as_ref() {
                let incoming = neighbor.channel_at(channel, y, x);
                seed_from_neighbor(levels, queue, chunk, registry, [x, y, 0], incoming);
            }
            if let Some(neighbor) = neighbors.pos_z.as_ref() {
                let incoming = neighbor.channel_at(channel, y, x);
                seed_from_neighbor(
                    levels,
                    queue,
                    chunk,
                    registry,
                    [x, y, (CHUNK_DEPTH as u8) - 1],
                    incoming,
                );
            }
        }
    }
}

fn seed_from_neighbor(
    levels: &mut [u8; CHUNK_VOLUME],
    queue: &mut VecDeque<usize>,
    chunk: &ChunkData,
    registry: &BlockRegistry,
    [local_x, y, local_z]: [u8; 3],
    incoming: u8,
) {
    if incoming == 0 {
        return;
    }

    let attenuation = light_attenuation(registry, chunk.block(local_x, y, local_z));
    let seeded = incoming.saturating_sub(attenuation);
    if seeded == 0 {
        return;
    }

    let index = light_index(local_x as usize, y as usize, local_z as usize);
    try_seed(levels, queue, index, seeded);
}

fn propagate_channel(
    levels: &mut [u8; CHUNK_VOLUME],
    queue: &mut VecDeque<usize>,
    chunk: &ChunkData,
    registry: &BlockRegistry,
) {
    while let Some(index) = queue.pop_front() {
        let level = levels[index];
        if level == 0 {
            continue;
        }

        let (x, y, z) = coords_from_light_index(index);
        for [dx, dy, dz] in LIGHT_DIRECTIONS {
            let nx = x + dx;
            let ny = y + dy;
            let nz = z + dz;
            if nx < 0
                || ny < 0
                || nz < 0
                || nx >= CHUNK_WIDTH as i32
                || ny >= CHUNK_HEIGHT as i32
                || nz >= CHUNK_DEPTH as i32
            {
                continue;
            }

            let neighbor_idx = light_index(nx as usize, ny as usize, nz as usize);
            let attenuation =
                light_attenuation(registry, chunk.block(nx as u8, ny as u8, nz as u8));
            let propagated = level.saturating_sub(attenuation);
            if propagated > levels[neighbor_idx] {
                levels[neighbor_idx] = propagated;
                queue.push_back(neighbor_idx);
            }
        }
    }
}

fn light_attenuation(registry: &BlockRegistry, block_id: u8) -> u8 {
    registry.opacity_of(block_id).clamp(1, 15)
}

fn try_seed(levels: &mut [u8; CHUNK_VOLUME], queue: &mut VecDeque<usize>, index: usize, value: u8) {
    if value > levels[index] {
        levels[index] = value;
        queue.push_back(index);
    }
}

fn light_index(local_x: usize, y: usize, local_z: usize) -> usize {
    (local_x << 11) | (local_z << 7) | y
}

fn coords_from_light_index(index: usize) -> (i32, i32, i32) {
    let x = (index >> 11) as i32;
    let z = ((index >> 7) & 0x0F) as i32;
    let y = (index & 0x7F) as i32;
    (x, y, z)
}

fn edge_index(y: u8, lateral: u8) -> usize {
    usize::from(lateral) * CHUNK_HEIGHT + usize::from(y)
}

fn channel_differs(
    chunk: &ChunkData,
    sky_light: &[u8; CHUNK_VOLUME],
    block_light: &[u8; CHUNK_VOLUME],
) -> (bool, u8) {
    let mut any_changed = false;
    let mut changed_sections_mask = 0_u8;
    for section in 0..CHUNK_SECTION_COUNT as u8 {
        let y_start = usize::from(section) * SECTION_HEIGHT;
        let y_end = y_start + SECTION_HEIGHT;
        let mut section_changed = false;
        for local_x in 0..CHUNK_WIDTH as u8 {
            for local_z in 0..CHUNK_DEPTH as u8 {
                for y in y_start..y_end {
                    let y = y as u8;
                    let index = light_index(local_x as usize, usize::from(y), local_z as usize);
                    if chunk.sky_light(local_x, y, local_z) != sky_light[index]
                        || chunk.block_light(local_x, y, local_z) != block_light[index]
                    {
                        section_changed = true;
                        any_changed = true;
                        break;
                    }
                }
                if section_changed {
                    break;
                }
            }
            if section_changed {
                break;
            }
        }
        if section_changed {
            changed_sections_mask |= 1 << section;
        }
    }
    (any_changed, changed_sections_mask)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::world::{ChunkPos, CHUNK_HEIGHT};

    #[test]
    fn block_light_propagates_and_stops_at_opaque_blocks() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, 0);
        chunk.set_block(8, 64, 8, 50);
        chunk.set_block(9, 64, 8, 1);

        let output = relight_chunk(&chunk, &CardinalChunkNeighborsOwned::default(), &registry);

        assert_eq!(output.block_light[light_index(8, 64, 8)], 14);
        assert_eq!(output.block_light[light_index(7, 64, 8)], 13);
        assert_eq!(output.block_light[light_index(9, 64, 8)], 0);
    }

    #[test]
    fn boundary_block_light_seeds_center_chunk() {
        let registry = BlockRegistry::alpha_1_2_6();
        let center = ChunkData::new(ChunkPos { x: 0, z: 0 }, 0);

        let mut west = ChunkData::new(ChunkPos { x: -1, z: 0 }, 0);
        west.set_block((CHUNK_WIDTH as u8) - 1, 64, 8, 50);
        west.reseed_column_emitted_light((CHUNK_WIDTH as u8) - 1, 8, &registry);

        let output = relight_chunk(
            &center,
            &CardinalChunkNeighborsOwned {
                neg_x: Some(LightEdgeSlice::from_neg_x(&west)),
                ..Default::default()
            },
            &registry,
        );

        assert_eq!(output.block_light[light_index(0, 64, 8)], 13);
    }

    #[test]
    fn boundary_sky_light_spreads_laterally_into_cavity() {
        let registry = BlockRegistry::alpha_1_2_6();

        let mut center = ChunkData::new(ChunkPos { x: 0, z: 0 }, 1);
        center.set_block(0, 64, 8, 0);
        center.set_block(1, 64, 8, 0);

        let mut west = ChunkData::new(ChunkPos { x: -1, z: 0 }, 0);
        west.recalculate_height_map(&registry);

        let output = relight_chunk(
            &center,
            &CardinalChunkNeighborsOwned {
                neg_x: Some(LightEdgeSlice::from_neg_x(&west)),
                ..Default::default()
            },
            &registry,
        );

        assert!(output.sky_light[light_index(0, 64, 8)] > 0);
        assert!(output.sky_light[light_index(1, 64, 8)] > 0);
    }

    #[test]
    fn boundary_sky_light_attenuates_by_one_from_neighbor() {
        let registry = BlockRegistry::alpha_1_2_6();

        let mut center = ChunkData::new(ChunkPos { x: 0, z: 0 }, 1);
        center.set_block(0, 64, 8, 0);
        center.set_block(1, 64, 8, 0);

        let mut west = ChunkData::new(ChunkPos { x: -1, z: 0 }, 0);
        west.recalculate_height_map(&registry);

        let output = relight_chunk(
            &center,
            &CardinalChunkNeighborsOwned {
                neg_x: Some(LightEdgeSlice::from_neg_x(&west)),
                ..Default::default()
            },
            &registry,
        );

        assert_eq!(output.sky_light[light_index(0, 64, 8)], 14);
        assert_eq!(output.sky_light[light_index(1, 64, 8)], 13);
    }

    #[test]
    fn unchanged_chunk_reports_no_lighting_diff() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, 0);
        chunk.recalculate_height_map(&registry);
        chunk.seed_emitted_light(&registry);

        let output = relight_chunk(&chunk, &CardinalChunkNeighborsOwned::default(), &registry);
        assert!(!output.changed);

        for y in 0..CHUNK_HEIGHT {
            let level = output.sky_light[light_index(8, y, 8)];
            assert_eq!(level, 15);
        }
    }
}
