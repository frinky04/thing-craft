use std::cmp::Reverse;
use std::collections::{HashMap, HashSet};
use std::mem;
use std::sync::mpsc::{self, Receiver, Sender, TryRecvError};
use std::thread;

use crate::mesh::{build_chunk_mesh, merge_meshes, ChunkMesh};
use crate::world::{
    BlockRegistry, ChunkData, ChunkPos, OverworldChunkGenerator, CHUNK_DEPTH, CHUNK_WIDTH,
};

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum ChunkResidencyState {
    Requested,
    Generating,
    Meshing,
    Ready,
    Evicting,
}

#[derive(Debug, Clone, Copy)]
pub struct ResidencyConfig {
    pub view_radius: i32,
    pub max_generation_dispatch: usize,
    pub max_meshing_dispatch: usize,
}

impl Default for ResidencyConfig {
    fn default() -> Self {
        Self {
            view_radius: 3,
            max_generation_dispatch: 8,
            max_meshing_dispatch: 8,
        }
    }
}

#[derive(Debug, Clone, Copy, Default)]
pub struct ResidencyMetrics {
    pub requested: usize,
    pub generating: usize,
    pub meshing: usize,
    pub ready: usize,
    pub evicting: usize,
    pub total: usize,
    pub in_flight_generation: usize,
    pub in_flight_meshing: usize,
}

#[derive(Debug, Clone)]
pub enum RenderMeshUpdate {
    Upsert { pos: ChunkPos, mesh: ChunkMesh },
    Remove { pos: ChunkPos },
}

#[derive(Debug)]
struct ChunkResidencyEntry {
    state: ChunkResidencyState,
    chunk: Option<ChunkData>,
    mesh: Option<ChunkMesh>,
}

#[derive(Debug, Clone, Copy)]
struct GenerationJob {
    pos: ChunkPos,
}

#[derive(Debug)]
struct GenerationResult {
    chunk: ChunkData,
}

#[derive(Debug)]
struct MeshingJob {
    chunk: ChunkData,
}

#[derive(Debug)]
struct MeshingResult {
    pos: ChunkPos,
    chunk: ChunkData,
    mesh: ChunkMesh,
}

pub struct ChunkStreamer {
    config: ResidencyConfig,
    slots: HashMap<ChunkPos, ChunkResidencyEntry>,
    required: HashSet<ChunkPos>,
    generation_in_flight: HashSet<ChunkPos>,
    meshing_in_flight: HashSet<ChunkPos>,
    generation_tx: Option<Sender<GenerationJob>>,
    generation_rx: Receiver<GenerationResult>,
    meshing_tx: Option<Sender<MeshingJob>>,
    meshing_rx: Receiver<MeshingResult>,
    generation_thread: Option<thread::JoinHandle<()>>,
    meshing_thread: Option<thread::JoinHandle<()>>,
    mesh_dirty: bool,
    render_updates: Vec<RenderMeshUpdate>,
}

impl ChunkStreamer {
    #[must_use]
    pub fn new(seed: u64, registry: BlockRegistry, config: ResidencyConfig) -> Self {
        let (generation_tx, generation_job_rx) = mpsc::channel::<GenerationJob>();
        let (generation_result_tx, generation_rx) = mpsc::channel::<GenerationResult>();
        let (meshing_tx, meshing_job_rx) = mpsc::channel::<MeshingJob>();
        let (meshing_result_tx, meshing_rx) = mpsc::channel::<MeshingResult>();

        let generation_registry = registry.clone();
        let generation_thread = thread::Builder::new()
            .name("thingcraft-generation-worker".to_owned())
            .spawn(move || {
                let generator = OverworldChunkGenerator::new(seed);
                while let Ok(job) = generation_job_rx.recv() {
                    let chunk = generator.generate_chunk(job.pos, &generation_registry);
                    if generation_result_tx
                        .send(GenerationResult { chunk })
                        .is_err()
                    {
                        break;
                    }
                }
            })
            .expect("failed to spawn generation worker thread");

        let meshing_registry = registry;
        let meshing_thread = thread::Builder::new()
            .name("thingcraft-meshing-worker".to_owned())
            .spawn(move || {
                while let Ok(job) = meshing_job_rx.recv() {
                    let pos = job.chunk.pos;
                    let mesh = build_chunk_mesh(&job.chunk, &meshing_registry);
                    if meshing_result_tx
                        .send(MeshingResult {
                            pos,
                            chunk: job.chunk,
                            mesh,
                        })
                        .is_err()
                    {
                        break;
                    }
                }
            })
            .expect("failed to spawn meshing worker thread");

        Self {
            config,
            slots: HashMap::new(),
            required: HashSet::new(),
            generation_in_flight: HashSet::new(),
            meshing_in_flight: HashSet::new(),
            generation_tx: Some(generation_tx),
            generation_rx,
            meshing_tx: Some(meshing_tx),
            meshing_rx,
            generation_thread: Some(generation_thread),
            meshing_thread: Some(meshing_thread),
            mesh_dirty: true,
            render_updates: Vec::new(),
        }
    }

    pub fn update_target(&mut self, center_chunk: ChunkPos) -> Option<ChunkMesh> {
        self.tick(center_chunk);
        self.rebuild_scene_mesh_if_dirty()
    }

    pub fn tick(&mut self, center_chunk: ChunkPos) {
        self.refresh_required_set(center_chunk);
        self.poll_generation_results();
        self.poll_meshing_results();
        self.dispatch_generation(center_chunk);
        self.dispatch_meshing(center_chunk);
        self.cleanup_evicted();
    }

    pub fn drain_render_updates(&mut self) -> Vec<RenderMeshUpdate> {
        mem::take(&mut self.render_updates)
    }

    #[must_use]
    pub fn metrics(&self) -> ResidencyMetrics {
        let mut metrics = ResidencyMetrics {
            total: self.slots.len(),
            in_flight_generation: self.generation_in_flight.len(),
            in_flight_meshing: self.meshing_in_flight.len(),
            ..Default::default()
        };

        for slot in self.slots.values() {
            match slot.state {
                ChunkResidencyState::Requested => metrics.requested += 1,
                ChunkResidencyState::Generating => metrics.generating += 1,
                ChunkResidencyState::Meshing => metrics.meshing += 1,
                ChunkResidencyState::Ready => metrics.ready += 1,
                ChunkResidencyState::Evicting => metrics.evicting += 1,
            }
        }

        metrics
    }

    fn refresh_required_set(&mut self, center_chunk: ChunkPos) {
        let mut required = HashSet::new();
        for dz in -self.config.view_radius..=self.config.view_radius {
            for dx in -self.config.view_radius..=self.config.view_radius {
                let pos = ChunkPos {
                    x: center_chunk.x + dx,
                    z: center_chunk.z + dz,
                };
                required.insert(pos);
                self.slots.entry(pos).or_insert(ChunkResidencyEntry {
                    state: ChunkResidencyState::Requested,
                    chunk: None,
                    mesh: None,
                });
            }
        }

        for (pos, slot) in &mut self.slots {
            if !required.contains(pos) && slot.state != ChunkResidencyState::Evicting {
                slot.state = ChunkResidencyState::Evicting;
                slot.chunk = None;
                slot.mesh = None;
                self.mesh_dirty = true;
                self.render_updates
                    .push(RenderMeshUpdate::Remove { pos: *pos });
            }
        }

        self.required = required;
    }

    fn dispatch_generation(&mut self, center_chunk: ChunkPos) {
        let Some(tx) = &self.generation_tx else {
            return;
        };

        let mut candidates: Vec<_> = self
            .slots
            .iter()
            .filter_map(|(pos, entry)| {
                if entry.state == ChunkResidencyState::Requested
                    && !self.generation_in_flight.contains(pos)
                    && self.required.contains(pos)
                {
                    Some(*pos)
                } else {
                    None
                }
            })
            .collect();

        candidates.sort_by_key(|pos| Reverse(chunk_distance_sq(*pos, center_chunk)));
        candidates.reverse();

        for pos in candidates
            .into_iter()
            .take(self.config.max_generation_dispatch)
        {
            if tx.send(GenerationJob { pos }).is_err() {
                break;
            }
            if let Some(slot) = self.slots.get_mut(&pos) {
                slot.state = ChunkResidencyState::Generating;
            }
            self.generation_in_flight.insert(pos);
        }
    }

    fn dispatch_meshing(&mut self, center_chunk: ChunkPos) {
        let Some(tx) = &self.meshing_tx else {
            return;
        };

        let mut candidates: Vec<_> = self
            .slots
            .iter()
            .filter_map(|(pos, entry)| {
                if entry.state == ChunkResidencyState::Meshing
                    && entry.chunk.is_some()
                    && !self.meshing_in_flight.contains(pos)
                    && self.required.contains(pos)
                {
                    Some(*pos)
                } else {
                    None
                }
            })
            .collect();

        candidates.sort_by_key(|pos| Reverse(chunk_distance_sq(*pos, center_chunk)));
        candidates.reverse();

        for pos in candidates
            .into_iter()
            .take(self.config.max_meshing_dispatch)
        {
            let Some(slot) = self.slots.get(&pos) else {
                continue;
            };
            let Some(chunk) = slot.chunk.clone() else {
                continue;
            };

            if tx.send(MeshingJob { chunk }).is_err() {
                break;
            }
            self.meshing_in_flight.insert(pos);
        }
    }

    fn poll_generation_results(&mut self) {
        loop {
            match self.generation_rx.try_recv() {
                Ok(result) => {
                    let pos = result.chunk.pos;
                    self.generation_in_flight.remove(&pos);

                    if !self.required.contains(&pos) {
                        continue;
                    }

                    if let Some(slot) = self.slots.get_mut(&pos) {
                        slot.chunk = Some(result.chunk);
                        slot.mesh = None;
                        slot.state = ChunkResidencyState::Meshing;
                    }
                }
                Err(TryRecvError::Empty | TryRecvError::Disconnected) => return,
            }
        }
    }

    fn poll_meshing_results(&mut self) {
        loop {
            match self.meshing_rx.try_recv() {
                Ok(result) => {
                    self.meshing_in_flight.remove(&result.pos);

                    if !self.required.contains(&result.pos) {
                        continue;
                    }

                    if let Some(slot) = self.slots.get_mut(&result.pos) {
                        slot.chunk = Some(result.chunk);
                        slot.mesh = Some(result.mesh);
                        slot.state = ChunkResidencyState::Ready;
                        self.mesh_dirty = true;
                        if let Some(mesh) = &slot.mesh {
                            self.render_updates.push(RenderMeshUpdate::Upsert {
                                pos: result.pos,
                                mesh: mesh.clone(),
                            });
                        }
                    }
                }
                Err(TryRecvError::Empty | TryRecvError::Disconnected) => return,
            }
        }
    }

    fn cleanup_evicted(&mut self) {
        let to_remove: Vec<_> = self
            .slots
            .iter()
            .filter_map(|(pos, slot)| {
                if slot.state == ChunkResidencyState::Evicting
                    && !self.generation_in_flight.contains(pos)
                    && !self.meshing_in_flight.contains(pos)
                {
                    Some(*pos)
                } else {
                    None
                }
            })
            .collect();

        if to_remove.is_empty() {
            return;
        }

        for pos in to_remove {
            self.slots.remove(&pos);
            self.generation_in_flight.remove(&pos);
            self.meshing_in_flight.remove(&pos);
        }

        self.mesh_dirty = true;
    }

    fn rebuild_scene_mesh_if_dirty(&mut self) -> Option<ChunkMesh> {
        if !self.mesh_dirty {
            return None;
        }

        self.mesh_dirty = false;
        let meshes: Vec<_> = self
            .slots
            .values()
            .filter_map(|slot| {
                if slot.state == ChunkResidencyState::Ready {
                    slot.mesh.clone()
                } else {
                    None
                }
            })
            .collect();

        Some(merge_meshes(&meshes))
    }

    #[cfg(test)]
    fn slot_state(&self, pos: ChunkPos) -> Option<ChunkResidencyState> {
        self.slots.get(&pos).map(|slot| slot.state)
    }
}

impl Drop for ChunkStreamer {
    fn drop(&mut self) {
        self.generation_tx.take();
        self.meshing_tx.take();

        if let Some(handle) = self.generation_thread.take() {
            let _ = handle.join();
        }
        if let Some(handle) = self.meshing_thread.take() {
            let _ = handle.join();
        }
    }
}

#[must_use]
pub fn world_pos_to_chunk_pos(world_x: f64, world_z: f64) -> ChunkPos {
    ChunkPos {
        x: world_coord_to_chunk_coord(world_x, CHUNK_WIDTH as i32),
        z: world_coord_to_chunk_coord(world_z, CHUNK_DEPTH as i32),
    }
}

#[must_use]
fn world_coord_to_chunk_coord(world: f64, chunk_size: i32) -> i32 {
    (world.floor() as i32).div_euclid(chunk_size)
}

#[must_use]
fn chunk_distance_sq(a: ChunkPos, b: ChunkPos) -> i64 {
    let dx = i64::from(a.x - b.x);
    let dz = i64::from(a.z - b.z);
    dx * dx + dz * dz
}

#[cfg(test)]
mod tests {
    use std::thread;
    use std::time::Duration;

    use super::*;

    #[test]
    fn residency_requests_expected_radius_square() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 0,
                max_meshing_dispatch: 0,
            },
        );

        streamer.update_target(ChunkPos { x: 0, z: 0 });
        let metrics = streamer.metrics();
        assert_eq!(metrics.total, 9);
        assert_eq!(metrics.requested, 9);
    }

    #[test]
    fn residency_transitions_chunk_to_ready() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 1,
                max_meshing_dispatch: 1,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            let _ = streamer.update_target(center);
            if streamer.metrics().ready == 1 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        assert_eq!(streamer.metrics().ready, 1);
        assert_eq!(
            streamer.slot_state(center),
            Some(ChunkResidencyState::Ready)
        );
    }

    #[test]
    fn residency_evicts_out_of_range_chunks() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 2,
                max_meshing_dispatch: 2,
            },
        );

        let start = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            let _ = streamer.update_target(start);
            if streamer.metrics().ready == 1 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        let next = ChunkPos { x: 3, z: 0 };
        for _ in 0..500 {
            let _ = streamer.update_target(next);
            if streamer.metrics().ready == 1 && streamer.slot_state(start).is_none() {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        assert_eq!(streamer.slot_state(start), None);
        assert!(streamer.slot_state(next).is_some());
        assert_eq!(streamer.metrics().total, 1);
    }

    #[test]
    fn world_position_to_chunk_handles_negative_coordinates() {
        assert_eq!(world_pos_to_chunk_pos(0.0, 0.0), ChunkPos { x: 0, z: 0 });
        assert_eq!(world_pos_to_chunk_pos(15.9, 15.9), ChunkPos { x: 0, z: 0 });
        assert_eq!(world_pos_to_chunk_pos(16.0, 16.0), ChunkPos { x: 1, z: 1 });
        assert_eq!(
            world_pos_to_chunk_pos(-0.1, -0.1),
            ChunkPos { x: -1, z: -1 }
        );
        assert_eq!(
            world_pos_to_chunk_pos(-16.0, -16.0),
            ChunkPos { x: -1, z: -1 }
        );
        assert_eq!(
            world_pos_to_chunk_pos(-16.1, -16.1),
            ChunkPos { x: -2, z: -2 }
        );
    }

    #[test]
    fn residency_emits_render_updates_for_ready_and_evict() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 2,
                max_meshing_dispatch: 2,
            },
        );

        let start = ChunkPos { x: 0, z: 0 };
        let mut saw_upsert = false;
        for _ in 0..500 {
            streamer.tick(start);
            saw_upsert |= streamer
                .drain_render_updates()
                .iter()
                .any(|update| matches!(update, RenderMeshUpdate::Upsert { .. }));
            if streamer.metrics().ready == 1 && saw_upsert {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(saw_upsert);

        streamer.tick(ChunkPos { x: 3, z: 0 });
        let updates = streamer.drain_render_updates();
        assert!(updates
            .iter()
            .any(|update| matches!(update, RenderMeshUpdate::Remove { pos } if *pos == start)));
    }
}
