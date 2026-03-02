use std::collections::{HashMap, HashSet};
use std::mem;
use std::sync::mpsc::{self, Receiver, Sender, TryRecvError};
use std::thread;

use crate::lighting::{relight_chunk, CardinalChunkNeighborsOwned, LightingOutput};
use crate::mesh::{
    build_chunk_mesh_with_neighbors, merge_meshes, CardinalChunkNeighbors, ChunkMesh,
};
use crate::world::{
    BlockRegistry, ChunkData, ChunkPos, OverworldChunkGenerator, CHUNK_DEPTH, CHUNK_HEIGHT,
    CHUNK_WIDTH,
};

const MAX_IN_FLIGHT_MULTIPLIER: usize = 4;
const MAX_URGENT_DISPATCH_BURST: usize = 4;

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum ChunkResidencyState {
    Requested,
    Generating,
    Meshing,
    Ready,
    Evicting,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
enum WorkPriority {
    Background,
    Urgent,
}

#[derive(Debug, Clone, Copy)]
pub struct ResidencyConfig {
    pub view_radius: i32,
    pub max_generation_dispatch: usize,
    pub max_lighting_dispatch: usize,
    pub max_meshing_dispatch: usize,
}

impl Default for ResidencyConfig {
    fn default() -> Self {
        Self {
            view_radius: 3,
            max_generation_dispatch: 8,
            max_lighting_dispatch: 8,
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
    pub dirty_chunks: usize,
    pub remesh_enqueued: u64,
    pub relight_enqueued: u64,
    pub relight_dropped_stale: u64,
    pub total: usize,
    pub in_flight_generation: usize,
    pub in_flight_lighting: usize,
    pub in_flight_meshing: usize,
    pub urgent_lighting: usize,
    pub urgent_meshing: usize,
}

#[derive(Debug, Clone)]
pub enum RenderMeshUpdate {
    Upsert { pos: ChunkPos, mesh: ChunkMesh },
    Remove { pos: ChunkPos },
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct ChunkDebugState {
    pub pos: ChunkPos,
    pub residency_state: ChunkResidencyState,
    pub dirty_geometry: bool,
    pub dirty_lighting: bool,
    pub in_flight_generation: bool,
    pub in_flight_lighting: bool,
    pub in_flight_meshing: bool,
}

#[derive(Debug)]
struct ChunkResidencyEntry {
    state: ChunkResidencyState,
    dirty: ChunkDirtyFlags,
    lighting_revision: u64,
    chunk: Option<ChunkData>,
    mesh: Option<ChunkMesh>,
}

#[derive(Debug, Clone, Copy, Default)]
struct ChunkDirtyFlags {
    geometry: bool,
    lighting: bool,
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
struct LightingJob {
    pos: ChunkPos,
    revision: u64,
    chunk: ChunkData,
    neighbors: CardinalChunkNeighborsOwned,
    priority: WorkPriority,
}

#[derive(Debug)]
struct LightingResult {
    pos: ChunkPos,
    revision: u64,
    lighting: LightingOutput,
    priority: WorkPriority,
}

#[derive(Debug)]
struct MeshingJob {
    chunk: ChunkData,
    neg_x: Option<ChunkData>,
    pos_x: Option<ChunkData>,
    neg_z: Option<ChunkData>,
    pos_z: Option<ChunkData>,
}

#[derive(Debug)]
struct MeshingResult {
    pos: ChunkPos,
    mesh: ChunkMesh,
}

pub struct ChunkStreamer {
    registry: BlockRegistry,
    config: ResidencyConfig,
    slots: HashMap<ChunkPos, ChunkResidencyEntry>,
    required: HashSet<ChunkPos>,
    generation_in_flight: HashSet<ChunkPos>,
    lighting_in_flight: HashSet<ChunkPos>,
    meshing_in_flight: HashSet<ChunkPos>,
    urgent_lighting: HashSet<ChunkPos>,
    urgent_meshing: HashSet<ChunkPos>,
    pending_render_upload: HashSet<ChunkPos>,
    coherence_pending: HashSet<ChunkPos>,
    generation_tx: Option<Sender<GenerationJob>>,
    generation_rx: Receiver<GenerationResult>,
    lighting_tx: Option<Sender<LightingJob>>,
    lighting_rx: Receiver<LightingResult>,
    meshing_tx: Option<Sender<MeshingJob>>,
    meshing_rx: Receiver<MeshingResult>,
    generation_thread: Option<thread::JoinHandle<()>>,
    lighting_thread: Option<thread::JoinHandle<()>>,
    meshing_thread: Option<thread::JoinHandle<()>>,
    mesh_dirty: bool,
    remesh_enqueued_total: u64,
    relight_enqueued_total: u64,
    relight_dropped_stale_total: u64,
    render_updates: Vec<RenderMeshUpdate>,
}

impl ChunkStreamer {
    #[must_use]
    pub fn new(seed: u64, registry: BlockRegistry, config: ResidencyConfig) -> Self {
        let (generation_tx, generation_job_rx) = mpsc::channel::<GenerationJob>();
        let (generation_result_tx, generation_rx) = mpsc::channel::<GenerationResult>();
        let (lighting_tx, lighting_job_rx) = mpsc::channel::<LightingJob>();
        let (lighting_result_tx, lighting_rx) = mpsc::channel::<LightingResult>();
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

        let lighting_registry = registry.clone();
        let lighting_thread = thread::Builder::new()
            .name("thingcraft-lighting-worker".to_owned())
            .spawn(move || {
                while let Ok(job) = lighting_job_rx.recv() {
                    let lighting = relight_chunk(&job.chunk, &job.neighbors, &lighting_registry);
                    if lighting_result_tx
                        .send(LightingResult {
                            pos: job.pos,
                            revision: job.revision,
                            lighting,
                            priority: job.priority,
                        })
                        .is_err()
                    {
                        break;
                    }
                }
            })
            .expect("failed to spawn lighting worker thread");

        let meshing_registry = registry.clone();
        let meshing_thread = thread::Builder::new()
            .name("thingcraft-meshing-worker".to_owned())
            .spawn(move || {
                while let Ok(job) = meshing_job_rx.recv() {
                    let pos = job.chunk.pos;
                    let neighbors = CardinalChunkNeighbors {
                        neg_x: job.neg_x.as_ref(),
                        pos_x: job.pos_x.as_ref(),
                        neg_z: job.neg_z.as_ref(),
                        pos_z: job.pos_z.as_ref(),
                    };
                    let mesh =
                        build_chunk_mesh_with_neighbors(&job.chunk, &meshing_registry, &neighbors);
                    if meshing_result_tx.send(MeshingResult { pos, mesh }).is_err() {
                        break;
                    }
                }
            })
            .expect("failed to spawn meshing worker thread");

        Self {
            registry,
            config,
            slots: HashMap::new(),
            required: HashSet::new(),
            generation_in_flight: HashSet::new(),
            lighting_in_flight: HashSet::new(),
            meshing_in_flight: HashSet::new(),
            urgent_lighting: HashSet::new(),
            urgent_meshing: HashSet::new(),
            pending_render_upload: HashSet::new(),
            coherence_pending: HashSet::new(),
            generation_tx: Some(generation_tx),
            generation_rx,
            lighting_tx: Some(lighting_tx),
            lighting_rx,
            meshing_tx: Some(meshing_tx),
            meshing_rx,
            generation_thread: Some(generation_thread),
            lighting_thread: Some(lighting_thread),
            meshing_thread: Some(meshing_thread),
            mesh_dirty: true,
            remesh_enqueued_total: 0,
            relight_enqueued_total: 0,
            relight_dropped_stale_total: 0,
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
        self.poll_lighting_results();
        self.poll_meshing_results();
        self.dispatch_lighting(center_chunk);
        self.dispatch_meshing(center_chunk);
        self.dispatch_generation(center_chunk);
        self.flush_render_uploads();
        self.cleanup_evicted();
    }

    pub fn drain_render_updates(&mut self) -> Vec<RenderMeshUpdate> {
        mem::take(&mut self.render_updates)
    }

    pub fn mark_chunk_geometry_dirty(&mut self, pos: ChunkPos) {
        self.enqueue_geometry_remesh(pos, WorkPriority::Background);
    }

    pub fn mark_chunk_lighting_dirty(&mut self, pos: ChunkPos) {
        self.enqueue_lighting_relight(pos, WorkPriority::Background);
    }

    pub fn mark_chunk_geometry_dirty_urgent(&mut self, pos: ChunkPos) {
        self.enqueue_geometry_remesh(pos, WorkPriority::Urgent);
    }

    pub fn mark_chunk_lighting_dirty_urgent(&mut self, pos: ChunkPos) {
        self.enqueue_lighting_relight(pos, WorkPriority::Urgent);
    }

    pub fn mark_block_geometry_dirty(&mut self, pos: ChunkPos, local_x: u8, local_z: u8) {
        if usize::from(local_x) >= CHUNK_WIDTH || usize::from(local_z) >= CHUNK_DEPTH {
            return;
        }

        let mut edge_coherence_targets = vec![pos];
        self.enqueue_geometry_remesh(pos, WorkPriority::Urgent);
        if local_x == 0 {
            let west = ChunkPos {
                x: pos.x - 1,
                z: pos.z,
            };
            self.enqueue_geometry_remesh(west, WorkPriority::Urgent);
            edge_coherence_targets.push(west);
        }
        if local_x == (CHUNK_WIDTH as u8 - 1) {
            let east = ChunkPos {
                x: pos.x + 1,
                z: pos.z,
            };
            self.enqueue_geometry_remesh(east, WorkPriority::Urgent);
            edge_coherence_targets.push(east);
        }
        if local_z == 0 {
            let north = ChunkPos {
                x: pos.x,
                z: pos.z - 1,
            };
            self.enqueue_geometry_remesh(north, WorkPriority::Urgent);
            edge_coherence_targets.push(north);
        }
        if local_z == (CHUNK_DEPTH as u8 - 1) {
            let south = ChunkPos {
                x: pos.x,
                z: pos.z + 1,
            };
            self.enqueue_geometry_remesh(south, WorkPriority::Urgent);
            edge_coherence_targets.push(south);
        }

        if edge_coherence_targets.len() > 1 {
            for chunk in edge_coherence_targets {
                self.coherence_pending.insert(chunk);
            }
        }
    }

    pub fn mark_block_lighting_dirty(&mut self, pos: ChunkPos, local_x: u8, local_z: u8) {
        if usize::from(local_x) >= CHUNK_WIDTH || usize::from(local_z) >= CHUNK_DEPTH {
            return;
        }

        self.enqueue_lighting_relight(pos, WorkPriority::Urgent);
        if local_x == 0 {
            self.enqueue_lighting_relight(
                ChunkPos {
                    x: pos.x - 1,
                    z: pos.z,
                },
                WorkPriority::Urgent,
            );
        }
        if local_x == (CHUNK_WIDTH as u8 - 1) {
            self.enqueue_lighting_relight(
                ChunkPos {
                    x: pos.x + 1,
                    z: pos.z,
                },
                WorkPriority::Urgent,
            );
        }
        if local_z == 0 {
            self.enqueue_lighting_relight(
                ChunkPos {
                    x: pos.x,
                    z: pos.z - 1,
                },
                WorkPriority::Urgent,
            );
        }
        if local_z == (CHUNK_DEPTH as u8 - 1) {
            self.enqueue_lighting_relight(
                ChunkPos {
                    x: pos.x,
                    z: pos.z + 1,
                },
                WorkPriority::Urgent,
            );
        }
    }

    pub fn debug_chunk_states(&self) -> Vec<ChunkDebugState> {
        let mut states = Vec::with_capacity(self.slots.len());
        for (pos, slot) in &self.slots {
            if !self.required.contains(pos) {
                continue;
            }

            states.push(ChunkDebugState {
                pos: *pos,
                residency_state: slot.state,
                dirty_geometry: slot.dirty.geometry,
                dirty_lighting: slot.dirty.lighting,
                in_flight_generation: self.generation_in_flight.contains(pos),
                in_flight_lighting: self.lighting_in_flight.contains(pos),
                in_flight_meshing: self.meshing_in_flight.contains(pos),
            });
        }
        states.sort_by_key(|state| (state.pos.x, state.pos.z));
        states
    }

    #[must_use]
    pub fn block_at_world(&self, world_x: i32, world_y: i32, world_z: i32) -> Option<u8> {
        if !(0..CHUNK_HEIGHT as i32).contains(&world_y) {
            return None;
        }

        let (pos, local_x, local_z) = world_block_to_chunk_pos_and_local(world_x, world_z);
        let slot = self.slots.get(&pos)?;
        if slot.state == ChunkResidencyState::Evicting {
            return None;
        }

        let chunk = slot.chunk.as_ref()?;
        Some(chunk.block(local_x, world_y as u8, local_z))
    }

    pub fn set_block_at_world(
        &mut self,
        world_x: i32,
        world_y: i32,
        world_z: i32,
        block_id: u8,
    ) -> bool {
        if !(0..CHUNK_HEIGHT as i32).contains(&world_y) || !self.registry.is_defined_block(block_id)
        {
            return false;
        }

        let (pos, local_x, local_z) = world_block_to_chunk_pos_and_local(world_x, world_z);
        let changed = {
            let Some(slot) = self.slots.get_mut(&pos) else {
                return false;
            };
            if slot.state == ChunkResidencyState::Evicting {
                return false;
            }

            let Some(chunk) = slot.chunk.as_mut() else {
                return false;
            };
            let local_y = world_y as u8;
            if chunk.block(local_x, local_y, local_z) == block_id {
                return false;
            }
            chunk.set_block(local_x, local_y, local_z, block_id);
            true
        };

        if changed {
            self.refresh_columns_after_block_edit(pos, local_x, local_z);
            self.mark_block_lighting_dirty(pos, local_x, local_z);
            self.mark_block_geometry_dirty(pos, local_x, local_z);
        }
        changed
    }

    #[must_use]
    pub fn metrics(&self) -> ResidencyMetrics {
        let mut metrics = ResidencyMetrics {
            total: self.slots.len(),
            in_flight_generation: self.generation_in_flight.len(),
            in_flight_lighting: self.lighting_in_flight.len(),
            in_flight_meshing: self.meshing_in_flight.len(),
            urgent_lighting: self.urgent_lighting.len(),
            urgent_meshing: self.urgent_meshing.len(),
            remesh_enqueued: self.remesh_enqueued_total,
            relight_enqueued: self.relight_enqueued_total,
            relight_dropped_stale: self.relight_dropped_stale_total,
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
            if slot.dirty.geometry || slot.dirty.lighting {
                metrics.dirty_chunks += 1;
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
                    dirty: ChunkDirtyFlags::default(),
                    lighting_revision: 0,
                    chunk: None,
                    mesh: None,
                });
            }
        }

        let mut evicted = Vec::new();
        for (pos, slot) in &mut self.slots {
            if !required.contains(pos) && slot.state != ChunkResidencyState::Evicting {
                slot.state = ChunkResidencyState::Evicting;
                slot.dirty = ChunkDirtyFlags::default();
                slot.chunk = None;
                slot.mesh = None;
                self.mesh_dirty = true;
                self.render_updates
                    .push(RenderMeshUpdate::Remove { pos: *pos });
                evicted.push(*pos);
            }
        }

        for pos in evicted {
            self.mark_neighbors_for_remesh(pos);
        }

        self.required = required;
    }

    fn dispatch_generation(&mut self, center_chunk: ChunkPos) {
        let Some(tx) = &self.generation_tx else {
            return;
        };
        let dispatch_budget = lane_dispatch_budget(
            self.config.max_generation_dispatch,
            self.generation_in_flight.len(),
        );
        if dispatch_budget == 0 {
            return;
        }

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

        sort_candidates_by_distance(&mut candidates, center_chunk);

        for pos in candidates.into_iter().take(dispatch_budget) {
            if tx.send(GenerationJob { pos }).is_err() {
                break;
            }
            if let Some(slot) = self.slots.get_mut(&pos) {
                slot.state = ChunkResidencyState::Generating;
            }
            self.generation_in_flight.insert(pos);
        }
    }

    fn dispatch_lighting(&mut self, center_chunk: ChunkPos) {
        let Some(tx) = &self.lighting_tx else {
            return;
        };
        let dispatch_budget = lane_dispatch_budget(
            self.config.max_lighting_dispatch,
            self.lighting_in_flight.len(),
        );
        if dispatch_budget == 0 {
            return;
        }

        self.urgent_lighting.retain(|pos| {
            self.slots.get(pos).is_some_and(|entry| {
                entry.chunk.is_some()
                    && entry.dirty.lighting
                    && entry.state != ChunkResidencyState::Evicting
                    && self.required.contains(pos)
            })
        });

        let mut urgent_candidates = Vec::new();
        let mut regular_candidates = Vec::new();
        for (pos, entry) in &self.slots {
            if entry.chunk.is_some()
                && entry.dirty.lighting
                && !self.lighting_in_flight.contains(pos)
                && self.required.contains(pos)
            {
                if self.urgent_lighting.contains(pos) {
                    urgent_candidates.push(*pos);
                } else {
                    regular_candidates.push(*pos);
                }
            }
        }

        sort_candidates_by_distance(&mut urgent_candidates, center_chunk);
        sort_candidates_by_distance(&mut regular_candidates, center_chunk);
        let total_dispatch_budget = boosted_dispatch_budget(
            self.config.max_lighting_dispatch,
            self.lighting_in_flight.len(),
            urgent_candidates.len(),
        );
        if total_dispatch_budget == 0 {
            return;
        }

        for pos in urgent_candidates
            .into_iter()
            .chain(regular_candidates)
            .take(total_dispatch_budget)
        {
            let Some(slot) = self.slots.get(&pos) else {
                continue;
            };
            let Some(chunk) = slot.chunk.clone() else {
                continue;
            };
            let revision = slot.lighting_revision;
            let priority = if self.urgent_lighting.contains(&pos) {
                WorkPriority::Urgent
            } else {
                WorkPriority::Background
            };

            let neighbors = CardinalChunkNeighborsOwned {
                neg_x: self.chunk_data_at(ChunkPos {
                    x: pos.x - 1,
                    z: pos.z,
                }),
                pos_x: self.chunk_data_at(ChunkPos {
                    x: pos.x + 1,
                    z: pos.z,
                }),
                neg_z: self.chunk_data_at(ChunkPos {
                    x: pos.x,
                    z: pos.z - 1,
                }),
                pos_z: self.chunk_data_at(ChunkPos {
                    x: pos.x,
                    z: pos.z + 1,
                }),
            };

            if tx
                .send(LightingJob {
                    pos,
                    revision,
                    chunk,
                    neighbors,
                    priority,
                })
                .is_err()
            {
                break;
            }

            if let Some(slot) = self.slots.get_mut(&pos) {
                slot.dirty.lighting = false;
            }
            self.lighting_in_flight.insert(pos);
            self.urgent_lighting.remove(&pos);
            self.relight_enqueued_total = self.relight_enqueued_total.saturating_add(1);
        }
    }

    fn dispatch_meshing(&mut self, center_chunk: ChunkPos) {
        let Some(tx) = &self.meshing_tx else {
            return;
        };
        let dispatch_budget = lane_dispatch_budget(
            self.config.max_meshing_dispatch,
            self.meshing_in_flight.len(),
        );
        if dispatch_budget == 0 {
            return;
        }

        self.urgent_meshing.retain(|pos| {
            self.slots.get(pos).is_some_and(|entry| {
                entry.state == ChunkResidencyState::Meshing
                    && entry.chunk.is_some()
                    && entry.dirty.geometry
                    && !entry.dirty.lighting
                    && !self.lighting_in_flight.contains(pos)
                    && !self.meshing_in_flight.contains(pos)
                    && self.required.contains(pos)
            })
        });

        let mut urgent_candidates = Vec::new();
        let mut regular_candidates = Vec::new();
        for (pos, entry) in &self.slots {
            if entry.state == ChunkResidencyState::Meshing
                && entry.chunk.is_some()
                && entry.dirty.geometry
                && !entry.dirty.lighting
                && !self.lighting_in_flight.contains(pos)
                && !self.meshing_in_flight.contains(pos)
                && self.required.contains(pos)
                && self.neighbor_lighting_ready(*pos)
            {
                if self.urgent_meshing.contains(pos) {
                    urgent_candidates.push(*pos);
                } else {
                    regular_candidates.push(*pos);
                }
            }
        }

        sort_candidates_by_distance(&mut urgent_candidates, center_chunk);
        sort_candidates_by_distance(&mut regular_candidates, center_chunk);
        let total_dispatch_budget = boosted_dispatch_budget(
            self.config.max_meshing_dispatch,
            self.meshing_in_flight.len(),
            urgent_candidates.len(),
        );
        if total_dispatch_budget == 0 {
            return;
        }

        for pos in urgent_candidates
            .into_iter()
            .chain(regular_candidates)
            .take(total_dispatch_budget)
        {
            let Some(slot) = self.slots.get(&pos) else {
                continue;
            };
            let Some(chunk) = slot.chunk.clone() else {
                continue;
            };

            let neg_x = self.chunk_data_at(ChunkPos {
                x: pos.x - 1,
                z: pos.z,
            });
            let pos_x = self.chunk_data_at(ChunkPos {
                x: pos.x + 1,
                z: pos.z,
            });
            let neg_z = self.chunk_data_at(ChunkPos {
                x: pos.x,
                z: pos.z - 1,
            });
            let pos_z = self.chunk_data_at(ChunkPos {
                x: pos.x,
                z: pos.z + 1,
            });

            if tx
                .send(MeshingJob {
                    chunk,
                    neg_x,
                    pos_x,
                    neg_z,
                    pos_z,
                })
                .is_err()
            {
                break;
            }
            if let Some(slot) = self.slots.get_mut(&pos) {
                slot.dirty.geometry = false;
            }
            self.meshing_in_flight.insert(pos);
            self.urgent_meshing.remove(&pos);
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
                        slot.dirty = ChunkDirtyFlags::default();
                        slot.state = ChunkResidencyState::Meshing;
                        slot.mesh = None;
                    }
                    self.mark_chunk_lighting_dirty(pos);
                    self.mark_neighbors_for_relight(pos);
                    self.mark_neighbors_for_remesh(pos);
                }
                Err(TryRecvError::Empty | TryRecvError::Disconnected) => return,
            }
        }
    }

    fn poll_lighting_results(&mut self) {
        loop {
            match self.lighting_rx.try_recv() {
                Ok(result) => {
                    self.lighting_in_flight.remove(&result.pos);

                    if !self.required.contains(&result.pos) {
                        continue;
                    }

                    let mut boundary_lighting_changed = false;
                    let mut should_mark_geometry_dirty = false;
                    if let Some(slot) = self.slots.get_mut(&result.pos) {
                        let is_stale =
                            slot.dirty.lighting || slot.lighting_revision != result.revision;
                        if is_stale {
                            self.relight_dropped_stale_total =
                                self.relight_dropped_stale_total.saturating_add(1);
                            slot.dirty.lighting = true;
                            continue;
                        }

                        let should_remesh = result.lighting.changed || slot.mesh.is_none();
                        let Some(chunk) = slot.chunk.as_mut() else {
                            continue;
                        };
                        boundary_lighting_changed = result.lighting.changed
                            && boundary_light_channels_changed(chunk, &result.lighting);
                        if result.lighting.changed {
                            chunk.apply_light_channels(
                                &result.lighting.sky_light,
                                &result.lighting.block_light,
                            );
                        }
                        should_mark_geometry_dirty = should_remesh;
                    }

                    if should_mark_geometry_dirty {
                        if result.priority == WorkPriority::Urgent {
                            self.mark_chunk_geometry_dirty_urgent(result.pos);
                        } else {
                            self.mark_chunk_geometry_dirty(result.pos);
                        }
                    }
                    if boundary_lighting_changed {
                        self.mark_neighbors_for_relight_with_priority(result.pos, result.priority);
                        self.mark_neighbors_for_remesh_with_priority(result.pos, result.priority);
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
                        if slot.dirty.geometry || slot.dirty.lighting {
                            slot.state = ChunkResidencyState::Meshing;
                            continue;
                        }

                        slot.mesh = Some(result.mesh);
                        slot.state = ChunkResidencyState::Ready;
                        self.mesh_dirty = true;
                        self.pending_render_upload.insert(result.pos);
                    }
                }
                Err(TryRecvError::Empty | TryRecvError::Disconnected) => return,
            }
        }
    }

    fn flush_render_uploads(&mut self) {
        let pending: Vec<_> = self.pending_render_upload.iter().copied().collect();
        if pending.is_empty() {
            return;
        }

        for pos in pending {
            if !self.required.contains(&pos) {
                self.pending_render_upload.remove(&pos);
                self.coherence_pending.remove(&pos);
                continue;
            }

            let Some(slot) = self.slots.get(&pos) else {
                self.pending_render_upload.remove(&pos);
                self.coherence_pending.remove(&pos);
                continue;
            };

            if slot.state != ChunkResidencyState::Ready
                || slot.dirty.geometry
                || slot.dirty.lighting
            {
                continue;
            }

            if self.coherence_pending.contains(&pos) && !self.neighbor_geometry_settled(pos) {
                continue;
            }

            let Some(mesh) = &slot.mesh else {
                continue;
            };

            self.render_updates.push(RenderMeshUpdate::Upsert {
                pos,
                mesh: mesh.clone(),
            });
            self.pending_render_upload.remove(&pos);
            self.coherence_pending.remove(&pos);
        }
    }

    fn cleanup_evicted(&mut self) {
        let to_remove: Vec<_> = self
            .slots
            .iter()
            .filter_map(|(pos, slot)| {
                if slot.state == ChunkResidencyState::Evicting
                    && !self.generation_in_flight.contains(pos)
                    && !self.lighting_in_flight.contains(pos)
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
            self.lighting_in_flight.remove(&pos);
            self.meshing_in_flight.remove(&pos);
            self.urgent_lighting.remove(&pos);
            self.urgent_meshing.remove(&pos);
            self.pending_render_upload.remove(&pos);
            self.coherence_pending.remove(&pos);
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

    fn chunk_data_at(&self, pos: ChunkPos) -> Option<ChunkData> {
        self.slots.get(&pos).and_then(|slot| {
            if slot.state == ChunkResidencyState::Evicting {
                None
            } else {
                slot.chunk.clone()
            }
        })
    }

    fn mark_neighbors_for_remesh(&mut self, pos: ChunkPos) {
        self.mark_neighbors_for_remesh_with_priority(pos, WorkPriority::Background);
    }

    fn mark_neighbors_for_remesh_with_priority(&mut self, pos: ChunkPos, priority: WorkPriority) {
        for neighbor in cardinal_neighbors(pos) {
            match priority {
                WorkPriority::Background => self.mark_chunk_geometry_dirty(neighbor),
                WorkPriority::Urgent => self.mark_chunk_geometry_dirty_urgent(neighbor),
            }
        }
    }

    fn mark_neighbors_for_relight(&mut self, pos: ChunkPos) {
        self.mark_neighbors_for_relight_with_priority(pos, WorkPriority::Background);
    }

    fn mark_neighbors_for_relight_with_priority(&mut self, pos: ChunkPos, priority: WorkPriority) {
        for neighbor in cardinal_neighbors(pos) {
            match priority {
                WorkPriority::Background => self.mark_chunk_lighting_dirty(neighbor),
                WorkPriority::Urgent => self.mark_chunk_lighting_dirty_urgent(neighbor),
            }
        }
    }

    fn enqueue_lighting_relight(&mut self, pos: ChunkPos, priority: WorkPriority) {
        let Some(slot) = self.slots.get_mut(&pos) else {
            return;
        };
        if slot.state == ChunkResidencyState::Evicting || slot.chunk.is_none() {
            return;
        }

        slot.dirty.lighting = true;
        slot.lighting_revision = slot.lighting_revision.wrapping_add(1);
        if priority == WorkPriority::Urgent {
            self.urgent_lighting.insert(pos);
        }
    }

    fn enqueue_geometry_remesh(&mut self, pos: ChunkPos, priority: WorkPriority) {
        let Some(slot) = self.slots.get_mut(&pos) else {
            return;
        };
        if slot.state == ChunkResidencyState::Evicting || slot.chunk.is_none() {
            return;
        }

        slot.dirty.geometry = true;
        self.pending_render_upload.insert(pos);
        if slot.state != ChunkResidencyState::Meshing {
            slot.state = ChunkResidencyState::Meshing;
            self.remesh_enqueued_total = self.remesh_enqueued_total.saturating_add(1);
        }
        if priority == WorkPriority::Urgent {
            self.urgent_meshing.insert(pos);
        }
    }

    fn neighbor_lighting_ready(&self, pos: ChunkPos) -> bool {
        for neighbor in cardinal_neighbors(pos) {
            let Some(slot) = self.slots.get(&neighbor) else {
                continue;
            };
            if slot.state == ChunkResidencyState::Evicting || slot.chunk.is_none() {
                continue;
            }
            if slot.dirty.lighting || self.lighting_in_flight.contains(&neighbor) {
                return false;
            }
        }
        true
    }

    fn neighbor_geometry_settled(&self, pos: ChunkPos) -> bool {
        for neighbor in cardinal_neighbors(pos) {
            if !self.required.contains(&neighbor) {
                continue;
            }

            let Some(slot) = self.slots.get(&neighbor) else {
                continue;
            };
            if slot.state == ChunkResidencyState::Evicting || slot.chunk.is_none() {
                continue;
            }
            if slot.dirty.geometry || self.meshing_in_flight.contains(&neighbor) {
                return false;
            }
        }
        true
    }

    fn refresh_columns_after_block_edit(&mut self, pos: ChunkPos, local_x: u8, local_z: u8) {
        self.refresh_single_column(pos, local_x, local_z);

        if local_x == 0 {
            self.refresh_single_column(
                ChunkPos {
                    x: pos.x - 1,
                    z: pos.z,
                },
                (CHUNK_WIDTH as u8) - 1,
                local_z,
            );
        }
        if local_x == (CHUNK_WIDTH as u8) - 1 {
            self.refresh_single_column(
                ChunkPos {
                    x: pos.x + 1,
                    z: pos.z,
                },
                0,
                local_z,
            );
        }
        if local_z == 0 {
            self.refresh_single_column(
                ChunkPos {
                    x: pos.x,
                    z: pos.z - 1,
                },
                local_x,
                (CHUNK_DEPTH as u8) - 1,
            );
        }
        if local_z == (CHUNK_DEPTH as u8) - 1 {
            self.refresh_single_column(
                ChunkPos {
                    x: pos.x,
                    z: pos.z + 1,
                },
                local_x,
                0,
            );
        }
    }

    fn refresh_single_column(&mut self, pos: ChunkPos, local_x: u8, local_z: u8) {
        let Some(slot) = self.slots.get_mut(&pos) else {
            return;
        };
        if slot.state == ChunkResidencyState::Evicting {
            return;
        }
        let Some(chunk) = slot.chunk.as_mut() else {
            return;
        };
        chunk.recalculate_column_height_and_sky_light(local_x, local_z, &self.registry);
        chunk.reseed_column_emitted_light(local_x, local_z, &self.registry);
    }
}

impl Drop for ChunkStreamer {
    fn drop(&mut self) {
        self.generation_tx.take();
        self.lighting_tx.take();
        self.meshing_tx.take();

        if let Some(handle) = self.generation_thread.take() {
            let _ = handle.join();
        }
        if let Some(handle) = self.lighting_thread.take() {
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
pub fn world_block_to_chunk_pos_and_local(world_x: i32, world_z: i32) -> (ChunkPos, u8, u8) {
    let chunk_x = world_x.div_euclid(CHUNK_WIDTH as i32);
    let chunk_z = world_z.div_euclid(CHUNK_DEPTH as i32);
    let local_x = world_x.rem_euclid(CHUNK_WIDTH as i32) as u8;
    let local_z = world_z.rem_euclid(CHUNK_DEPTH as i32) as u8;
    (
        ChunkPos {
            x: chunk_x,
            z: chunk_z,
        },
        local_x,
        local_z,
    )
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

fn sort_candidates_by_distance(candidates: &mut [ChunkPos], center_chunk: ChunkPos) {
    candidates.sort_by_key(|pos| chunk_distance_sq(*pos, center_chunk));
}

fn lane_dispatch_budget(max_dispatch: usize, in_flight: usize) -> usize {
    if max_dispatch == 0 {
        return 0;
    }
    let max_in_flight = max_dispatch.saturating_mul(MAX_IN_FLIGHT_MULTIPLIER);
    max_dispatch.min(max_in_flight.saturating_sub(in_flight))
}

fn boosted_dispatch_budget(
    max_dispatch: usize,
    in_flight: usize,
    urgent_candidates: usize,
) -> usize {
    let base_budget = lane_dispatch_budget(max_dispatch, in_flight);
    if base_budget == 0 || urgent_candidates == 0 {
        return base_budget;
    }

    let urgent_target = urgent_candidates.min(MAX_URGENT_DISPATCH_BURST);
    let max_in_flight = max_dispatch.saturating_mul(MAX_IN_FLIGHT_MULTIPLIER);
    let headroom = max_in_flight.saturating_sub(in_flight);
    base_budget.max(urgent_target).min(headroom)
}

fn cardinal_neighbors(pos: ChunkPos) -> [ChunkPos; 4] {
    [
        ChunkPos {
            x: pos.x - 1,
            z: pos.z,
        },
        ChunkPos {
            x: pos.x + 1,
            z: pos.z,
        },
        ChunkPos {
            x: pos.x,
            z: pos.z - 1,
        },
        ChunkPos {
            x: pos.x,
            z: pos.z + 1,
        },
    ]
}

fn boundary_light_channels_changed(chunk: &ChunkData, lighting: &LightingOutput) -> bool {
    let max_x = (CHUNK_WIDTH as u8) - 1;
    let max_z = (CHUNK_DEPTH as u8) - 1;

    for y in 0..CHUNK_HEIGHT as u8 {
        for z in 0..CHUNK_DEPTH as u8 {
            if boundary_light_cell_changed(chunk, lighting, 0, y, z)
                || boundary_light_cell_changed(chunk, lighting, max_x, y, z)
            {
                return true;
            }
        }

        for x in 1..max_x {
            if boundary_light_cell_changed(chunk, lighting, x, y, 0)
                || boundary_light_cell_changed(chunk, lighting, x, y, max_z)
            {
                return true;
            }
        }
    }

    false
}

fn boundary_light_cell_changed(
    chunk: &ChunkData,
    lighting: &LightingOutput,
    local_x: u8,
    y: u8,
    local_z: u8,
) -> bool {
    let index = ChunkData::index(local_x, y, local_z);
    chunk.sky_light(local_x, y, local_z) != lighting.sky_light[index]
        || chunk.block_light(local_x, y, local_z) != lighting.block_light[index]
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
                max_lighting_dispatch: 0,
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
                max_lighting_dispatch: 1,
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
                max_lighting_dispatch: 2,
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
    fn world_block_to_chunk_and_local_handles_negative_coordinates() {
        let (pos, local_x, local_z) = world_block_to_chunk_pos_and_local(0, 0);
        assert_eq!(pos, ChunkPos { x: 0, z: 0 });
        assert_eq!(local_x, 0);
        assert_eq!(local_z, 0);

        let (pos, local_x, local_z) = world_block_to_chunk_pos_and_local(-1, -1);
        assert_eq!(pos, ChunkPos { x: -1, z: -1 });
        assert_eq!(local_x, (CHUNK_WIDTH - 1) as u8);
        assert_eq!(local_z, (CHUNK_DEPTH - 1) as u8);

        let (pos, local_x, local_z) =
            world_block_to_chunk_pos_and_local(CHUNK_WIDTH as i32, CHUNK_DEPTH as i32);
        assert_eq!(pos, ChunkPos { x: 1, z: 1 });
        assert_eq!(local_x, 0);
        assert_eq!(local_z, 0);
    }

    #[test]
    fn lane_dispatch_budget_caps_in_flight_depth() {
        assert_eq!(lane_dispatch_budget(0, 0), 0);
        assert_eq!(lane_dispatch_budget(4, 0), 4);
        assert_eq!(lane_dispatch_budget(4, 10), 4);
        assert_eq!(lane_dispatch_budget(4, 16), 0);
        assert_eq!(lane_dispatch_budget(4, 32), 0);
    }

    #[test]
    fn boosted_dispatch_budget_allows_small_urgent_burst() {
        assert_eq!(boosted_dispatch_budget(0, 0, 2), 0);
        assert_eq!(boosted_dispatch_budget(1, 0, 0), 1);
        assert_eq!(boosted_dispatch_budget(1, 0, 2), 2);
        assert_eq!(boosted_dispatch_budget(1, 3, 4), 1);
        assert_eq!(boosted_dispatch_budget(1, 4, 2), 0);
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
                max_lighting_dispatch: 2,
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

    #[test]
    fn edge_dirty_marks_neighbor_for_remesh() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let east = ChunkPos { x: 1, z: 0 };
        let remesh_before = streamer.metrics().remesh_enqueued;
        streamer.mark_block_geometry_dirty(center, (CHUNK_WIDTH as u8) - 1, 8);
        let metrics = streamer.metrics();
        assert_eq!(
            streamer.slot_state(east),
            Some(ChunkResidencyState::Meshing)
        );
        assert!(metrics.remesh_enqueued > remesh_before);
        assert!(metrics.dirty_chunks > 0);
    }

    #[test]
    fn set_block_at_world_marks_chunk_meshing_dirty() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 2,
                max_lighting_dispatch: 2,
                max_meshing_dispatch: 2,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready == 1 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert_eq!(streamer.metrics().ready, 1);

        let mut target = None;
        'search: for y in (0..CHUNK_HEIGHT as i32).rev() {
            for z in 0..CHUNK_DEPTH as i32 {
                for x in 0..CHUNK_WIDTH as i32 {
                    if streamer.block_at_world(x, y, z).is_some_and(|id| id != 0) {
                        target = Some((x, y, z));
                        break 'search;
                    }
                }
            }
        }
        let (x, y, z) = target.expect("expected a non-air block in generated chunk");

        assert!(streamer.set_block_at_world(x, y, z, 0));
        assert_eq!(streamer.block_at_world(x, y, z), Some(0));
        assert_eq!(
            streamer.slot_state(center),
            Some(ChunkResidencyState::Meshing)
        );
    }

    #[test]
    fn set_block_at_world_on_chunk_edge_marks_neighbor_meshing_dirty() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let edge_x = CHUNK_WIDTH as i32 - 1;
        let edge_z = (CHUNK_DEPTH / 2) as i32;
        let mut target_y = None;
        for y in (0..CHUNK_HEIGHT as i32).rev() {
            if streamer
                .block_at_world(edge_x, y, edge_z)
                .is_some_and(|id| id != 0)
            {
                target_y = Some(y);
                break;
            }
        }
        let y = target_y.expect("expected non-air edge block");

        assert!(streamer.set_block_at_world(edge_x, y, edge_z, 0));
        assert_eq!(
            streamer.slot_state(ChunkPos { x: 1, z: 0 }),
            Some(ChunkResidencyState::Meshing)
        );
    }

    #[test]
    fn placing_torch_relights_chunk_with_emitted_light() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry.clone(),
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 2,
                max_lighting_dispatch: 2,
                max_meshing_dispatch: 2,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            let metrics = streamer.metrics();
            if metrics.ready == 1
                && metrics.in_flight_lighting == 0
                && metrics.in_flight_meshing == 0
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert_eq!(streamer.metrics().ready, 1);

        let mut place_target = None;
        for y in 1..(CHUNK_HEIGHT as i32 - 1) {
            if streamer.block_at_world(8, y, 8) == Some(0)
                && streamer
                    .block_at_world(8, y - 1, 8)
                    .is_some_and(|block| block != 0)
            {
                place_target = Some((8, y, 8));
                break;
            }
        }
        let (x, y, z) = place_target.expect("expected an air block above solid support");
        assert!(streamer.set_block_at_world(x, y, z, 50));

        for _ in 0..500 {
            streamer.tick(center);
            let metrics = streamer.metrics();
            if metrics.in_flight_lighting == 0
                && metrics.in_flight_meshing == 0
                && streamer
                    .slots
                    .get(&center)
                    .is_some_and(|slot| !slot.dirty.lighting)
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        let (_, local_x, local_z) = world_block_to_chunk_pos_and_local(x, z);
        let center_slot = streamer
            .slots
            .get(&center)
            .expect("center slot should exist");
        let center_chunk = center_slot
            .chunk
            .as_ref()
            .expect("center chunk should exist");
        assert_eq!(center_chunk.block(local_x, y as u8, local_z), 50);
        assert_eq!(
            center_chunk.block_light(local_x, y as u8, local_z),
            registry.emitted_light_of(50)
        );
    }

    #[test]
    fn in_flight_meshing_result_does_not_overwrite_block_edits() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 1,
                max_lighting_dispatch: 1,
                max_meshing_dispatch: 1,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        let mut target = None;
        for _ in 0..500 {
            streamer.tick(center);

            if target.is_none() {
                'search: for y in (0..CHUNK_HEIGHT as i32).rev() {
                    for z in 0..CHUNK_DEPTH as i32 {
                        for x in 0..CHUNK_WIDTH as i32 {
                            if streamer.block_at_world(x, y, z).is_some_and(|id| id != 0) {
                                target = Some((x, y, z));
                                break 'search;
                            }
                        }
                    }
                }
            }

            if target.is_some() && streamer.meshing_in_flight.contains(&center) {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        let (x, y, z) = target.expect("expected a mutable solid block");
        assert!(streamer.meshing_in_flight.contains(&center));
        assert!(streamer.set_block_at_world(x, y, z, 0));

        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready == 1 && streamer.metrics().in_flight_meshing == 0 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        assert_eq!(streamer.block_at_world(x, y, z), Some(0));
        assert_eq!(
            streamer.slot_state(center),
            Some(ChunkResidencyState::Ready)
        );
    }

    #[test]
    fn lighting_jobs_are_enqueued_for_generated_chunks() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 1,
                max_lighting_dispatch: 1,
                max_meshing_dispatch: 1,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready == 1 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        let metrics = streamer.metrics();
        assert_eq!(metrics.ready, 1);
        assert!(metrics.relight_enqueued > 0);
    }

    #[test]
    fn edge_dirty_marks_neighbor_for_relight() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let east = ChunkPos { x: 1, z: 0 };
        let relight_before = streamer.metrics().relight_enqueued;
        streamer.mark_block_lighting_dirty(center, (CHUNK_WIDTH as u8) - 1, 8);
        let east_dirty = streamer
            .slots
            .get(&east)
            .is_some_and(|slot| slot.dirty.lighting);
        assert!(east_dirty);

        streamer.tick(center);
        assert!(streamer.metrics().relight_enqueued > relight_before);
    }

    #[test]
    fn stale_lighting_result_is_dropped() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 1,
                max_lighting_dispatch: 1,
                max_meshing_dispatch: 1,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        let mut dirtied_while_in_flight = false;
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.lighting_in_flight.contains(&center) {
                streamer.mark_chunk_lighting_dirty(center);
                dirtied_while_in_flight = true;
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(dirtied_while_in_flight);

        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().relight_dropped_stale > 0 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        assert!(streamer.metrics().relight_dropped_stale > 0);
    }

    #[test]
    fn lighting_change_marks_neighbor_for_boundary_remesh() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let east = ChunkPos { x: 1, z: 0 };
        assert_eq!(streamer.slot_state(east), Some(ChunkResidencyState::Ready));

        streamer.config.max_meshing_dispatch = 0;
        let sky = [0_u8; crate::world::CHUNK_VOLUME];
        let block = [0_u8; crate::world::CHUNK_VOLUME];
        let center_slot = streamer
            .slots
            .get_mut(&center)
            .expect("center slot should exist");
        center_slot
            .chunk
            .as_mut()
            .expect("center chunk should exist")
            .apply_light_channels(&sky, &block);
        streamer.mark_chunk_lighting_dirty(center);

        for _ in 0..500 {
            streamer.tick(center);
            let east_dirty = streamer
                .slots
                .get(&east)
                .is_some_and(|slot| slot.dirty.geometry);
            if east_dirty && streamer.metrics().in_flight_lighting == 0 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        assert!(streamer
            .slots
            .get(&east)
            .is_some_and(|slot| slot.dirty.geometry));
        assert_eq!(
            streamer.slot_state(east),
            Some(ChunkResidencyState::Meshing)
        );
    }

    #[test]
    fn lighting_change_marks_neighbor_for_boundary_relight() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9 {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let east = ChunkPos { x: 1, z: 0 };
        let east_revision_before = streamer
            .slots
            .get(&east)
            .expect("east slot should exist")
            .lighting_revision;

        let sky = [0_u8; crate::world::CHUNK_VOLUME];
        let block = [0_u8; crate::world::CHUNK_VOLUME];
        let center_slot = streamer
            .slots
            .get_mut(&center)
            .expect("center slot should exist");
        center_slot
            .chunk
            .as_mut()
            .expect("center chunk should exist")
            .apply_light_channels(&sky, &block);
        streamer.mark_chunk_lighting_dirty(center);

        for _ in 0..500 {
            streamer.tick(center);
            let east_revision_after = streamer
                .slots
                .get(&east)
                .expect("east slot should exist")
                .lighting_revision;
            if east_revision_after > east_revision_before {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        let east_revision_after = streamer
            .slots
            .get(&east)
            .expect("east slot should exist")
            .lighting_revision;
        assert!(east_revision_after > east_revision_before);
    }

    #[test]
    fn interior_lighting_change_does_not_mark_neighbor_for_relight() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            let metrics = streamer.metrics();
            if metrics.ready >= 9
                && metrics.in_flight_lighting == 0
                && metrics.in_flight_meshing == 0
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        let metrics = streamer.metrics();
        assert!(metrics.ready >= 9);
        assert_eq!(metrics.in_flight_lighting, 0);

        let east = ChunkPos { x: 1, z: 0 };
        let east_revision_before = streamer
            .slots
            .get(&east)
            .expect("east slot should exist")
            .lighting_revision;

        assert!(streamer.set_block_at_world(8, 64, 8, 76));

        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().in_flight_lighting == 0
                && streamer
                    .slots
                    .get(&center)
                    .is_some_and(|slot| !slot.dirty.lighting)
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }

        let east_revision_after = streamer
            .slots
            .get(&east)
            .expect("east slot should exist")
            .lighting_revision;
        assert_eq!(east_revision_after, east_revision_before);
    }

    #[test]
    fn urgent_lighting_dispatch_preempts_regular_candidates() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 1,
                max_meshing_dispatch: 9,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9
                && streamer.metrics().in_flight_lighting == 0
                && streamer.metrics().in_flight_meshing == 0
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let regular_near = center;
        let urgent_far = ChunkPos { x: 1, z: 1 };

        streamer.mark_chunk_lighting_dirty(regular_near);
        streamer.mark_chunk_lighting_dirty_urgent(urgent_far);
        streamer.dispatch_lighting(center);

        assert!(streamer.lighting_in_flight.contains(&urgent_far));
        assert!(!streamer.lighting_in_flight.contains(&regular_near));
    }

    #[test]
    fn meshing_waits_for_neighbor_lighting_to_settle() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 1,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9
                && streamer.metrics().in_flight_lighting == 0
                && streamer.metrics().in_flight_meshing == 0
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let east = ChunkPos { x: 1, z: 0 };
        streamer.mark_chunk_geometry_dirty(center);
        if let Some(east_slot) = streamer.slots.get_mut(&east) {
            east_slot.dirty.lighting = true;
        }

        streamer.dispatch_meshing(center);
        assert!(!streamer.meshing_in_flight.contains(&center));

        if let Some(east_slot) = streamer.slots.get_mut(&east) {
            east_slot.dirty.lighting = false;
        }
        streamer.dispatch_meshing(center);
        assert!(streamer.meshing_in_flight.contains(&center));
    }

    #[test]
    fn urgent_meshing_can_dispatch_pair_when_budget_is_one() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 9,
                max_lighting_dispatch: 9,
                max_meshing_dispatch: 1,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        for _ in 0..500 {
            streamer.tick(center);
            if streamer.metrics().ready >= 9
                && streamer.metrics().in_flight_lighting == 0
                && streamer.metrics().in_flight_meshing == 0
            {
                break;
            }
            thread::sleep(Duration::from_millis(1));
        }
        assert!(streamer.metrics().ready >= 9);

        let east = ChunkPos { x: 1, z: 0 };
        streamer.mark_chunk_geometry_dirty_urgent(center);
        streamer.mark_chunk_geometry_dirty_urgent(east);
        streamer.dispatch_meshing(center);

        assert!(streamer.meshing_in_flight.contains(&center));
        assert!(streamer.meshing_in_flight.contains(&east));
    }

    #[test]
    fn coherence_barrier_holds_border_upload_until_neighbor_settles() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 1,
                max_generation_dispatch: 0,
                max_lighting_dispatch: 0,
                max_meshing_dispatch: 0,
            },
        );

        let center = ChunkPos { x: 0, z: 0 };
        let east = ChunkPos { x: 1, z: 0 };

        let mut center_chunk = ChunkData::new(center, 0);
        center_chunk.recalculate_height_map(&streamer.registry);
        center_chunk.seed_emitted_light(&streamer.registry);
        let mut east_chunk = ChunkData::new(east, 0);
        east_chunk.recalculate_height_map(&streamer.registry);
        east_chunk.seed_emitted_light(&streamer.registry);

        streamer.slots.insert(
            center,
            ChunkResidencyEntry {
                state: ChunkResidencyState::Ready,
                dirty: ChunkDirtyFlags::default(),
                lighting_revision: 0,
                chunk: Some(center_chunk),
                mesh: Some(ChunkMesh::default()),
            },
        );
        streamer.slots.insert(
            east,
            ChunkResidencyEntry {
                state: ChunkResidencyState::Ready,
                dirty: ChunkDirtyFlags::default(),
                lighting_revision: 0,
                chunk: Some(east_chunk),
                mesh: Some(ChunkMesh::default()),
            },
        );
        streamer.required.insert(center);
        streamer.required.insert(east);
        streamer.pending_render_upload.insert(center);
        streamer.coherence_pending.insert(center);

        if let Some(slot) = streamer.slots.get_mut(&east) {
            slot.dirty.geometry = true;
        }
        streamer.flush_render_uploads();
        assert!(streamer.render_updates.is_empty());
        assert!(streamer.pending_render_upload.contains(&center));

        if let Some(slot) = streamer.slots.get_mut(&east) {
            slot.dirty.geometry = false;
        }
        streamer.flush_render_uploads();
        assert!(streamer.render_updates.iter().any(
            |update| matches!(update, RenderMeshUpdate::Upsert { pos, .. } if *pos == center)
        ));
        assert!(!streamer.pending_render_upload.contains(&center));
    }

    #[test]
    fn debug_chunk_states_only_include_required_chunks() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(
            42,
            registry,
            ResidencyConfig {
                view_radius: 0,
                max_generation_dispatch: 0,
                max_lighting_dispatch: 0,
                max_meshing_dispatch: 0,
            },
        );
        streamer.tick(ChunkPos { x: 0, z: 0 });

        let states = streamer.debug_chunk_states();
        assert_eq!(states.len(), 1);
        assert_eq!(states[0].pos, ChunkPos { x: 0, z: 0 });
        assert_eq!(states[0].residency_state, ChunkResidencyState::Requested);
    }
}
