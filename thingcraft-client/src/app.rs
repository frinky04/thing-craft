use std::collections::{HashMap, VecDeque};
use std::time::{Duration, Instant};

use anyhow::Result;
use glam::{DVec3, Mat4, Vec3};
use tracing::{debug, error, info, warn};
use winit::dpi::PhysicalSize;
use winit::event::{DeviceEvent, ElementState, Event, MouseButton, WindowEvent};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::keyboard::{KeyCode, PhysicalKey};
use winit::window::{CursorGrabMode, Window, WindowBuilder};

use crate::ecs::EcsRuntime;
use crate::hud;
use crate::mesh::{build_region_mesh, ChunkMesh};
use crate::renderer::{RenderError, Renderer};
use crate::streaming::{
    world_block_to_chunk_pos_and_local, world_pos_to_chunk_pos, ChunkStreamer, RenderMeshUpdate,
    ResidencyConfig,
};
use crate::time_step::FixedStepClock;
use crate::world::{BlockRegistry, BootstrapWorld, ChunkPos, CHUNK_DEPTH, CHUNK_HEIGHT, CHUNK_WIDTH};

const NOISY_LOG_TARGET_DEFAULTS: [&str; 5] = [
    "wgpu_core=warn",
    "wgpu_hal=error",
    "naga=warn",
    "ash=warn",
    "calloop=warn",
];
const APPLY_STREAM_UPDATES_TO_RENDERER: bool = true;
const BLOCK_INTERACTION_REACH_BLOCKS: f64 = 5.0;
const AIR_BLOCK_ID: u8 = 0;
const HOTBAR_BLOCK_IDS: [u8; 9] = [3, 1, 4, 12, 13, 17, 5, 20, 50];
const HOTBAR_STACK_LIMIT: u8 = 64;
const EDIT_LATENCY_SAMPLE_WINDOW: usize = 128;

#[derive(Debug, Clone, Copy)]
enum BlockInteractionKind {
    Break,
    Place { slot: usize },
}

#[derive(Debug, Clone, Copy)]
struct BlockInteractionRequest {
    origin: DVec3,
    direction: DVec3,
    kind: BlockInteractionKind,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
struct BlockRayHit {
    block: [i32; 3],
    normal: [i32; 3],
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
struct HotbarSlot {
    block_id: u8,
    count: u8,
}

impl Default for HotbarSlot {
    fn default() -> Self {
        Self {
            block_id: AIR_BLOCK_ID,
            count: 0,
        }
    }
}

#[derive(Debug, Clone, Eq, PartialEq)]
struct HotbarInventory {
    slots: [HotbarSlot; HOTBAR_BLOCK_IDS.len()],
}

#[derive(Debug, Default)]
struct LoopStats {
    frame_count: u64,
    tick_count: u64,
    frame_time_accum: Duration,
    tick_time_accum: Duration,
    since_last_report: Duration,
}

#[derive(Debug, Clone, Copy)]
struct LoopReport {
    fps: f64,
    tps: f64,
    avg_frame_ms: f64,
    avg_tick_ms: f64,
}

#[derive(Debug, Default)]
struct EditLatencyTracker {
    pending_by_chunk: HashMap<ChunkPos, Instant>,
    completed_samples: VecDeque<Duration>,
    completed_total: u64,
    latest: Option<Duration>,
}

#[derive(Debug, Clone, Copy, Default)]
struct EditLatencyMetrics {
    pending: usize,
    completed_total: u64,
    latest_ms: f64,
    avg_ms: f64,
    p95_ms: f64,
}

impl HotbarInventory {
    fn alpha_defaults() -> Self {
        Self {
            slots: HOTBAR_BLOCK_IDS.map(|block_id| HotbarSlot {
                block_id,
                count: HOTBAR_STACK_LIMIT,
            }),
        }
    }

    fn slot(&self, index: usize) -> Option<HotbarSlot> {
        self.slots.get(index).copied()
    }

    fn try_consume_from_slot(&mut self, index: usize) -> bool {
        let Some(slot) = self.slots.get_mut(index) else {
            return false;
        };
        if slot.count == 0 {
            return false;
        }
        slot.count -= 1;
        true
    }

    fn try_pickup_block(&mut self, block_id: u8) -> bool {
        if block_id == AIR_BLOCK_ID {
            return false;
        }

        let Some(slot) = self
            .slots
            .iter_mut()
            .find(|slot| slot.block_id == block_id && slot.count < HOTBAR_STACK_LIMIT)
        else {
            return false;
        };

        slot.count += 1;
        true
    }

    /// Lightweight snapshot of all slot counts for dirty-checking.
    fn snapshot_counts(&self) -> [u8; HOTBAR_BLOCK_IDS.len()] {
        std::array::from_fn(|i| self.slots[i].count)
    }
}

impl LoopStats {
    fn record_frame(
        &mut self,
        frame_delta: Duration,
        simulated_ticks: u32,
        tick_duration: Duration,
    ) -> Option<LoopReport> {
        self.frame_count += 1;
        self.tick_count += u64::from(simulated_ticks);
        self.frame_time_accum += frame_delta;
        self.tick_time_accum += tick_duration;
        self.since_last_report += frame_delta;

        if self.since_last_report < Duration::from_secs(1) {
            return None;
        }

        let report_window_seconds = self.since_last_report.as_secs_f64();
        let report = LoopReport {
            fps: self.frame_count as f64 / report_window_seconds,
            tps: self.tick_count as f64 / report_window_seconds,
            avg_frame_ms: (self.frame_time_accum.as_secs_f64() * 1000.0) / self.frame_count as f64,
            avg_tick_ms: if self.tick_count == 0 {
                0.0
            } else {
                (self.tick_time_accum.as_secs_f64() * 1000.0) / self.tick_count as f64
            },
        };

        self.frame_count = 0;
        self.tick_count = 0;
        self.frame_time_accum = Duration::ZERO;
        self.tick_time_accum = Duration::ZERO;
        self.since_last_report = Duration::ZERO;

        Some(report)
    }
}

impl EditLatencyTracker {
    fn record_block_edit(&mut self, world_x: i32, world_z: i32) {
        let now = Instant::now();
        for chunk in affected_chunks_for_block_edit(world_x, world_z) {
            self.pending_by_chunk.entry(chunk).or_insert(now);
        }
    }

    fn observe_render_update(&mut self, pos: ChunkPos) {
        let Some(started_at) = self.pending_by_chunk.remove(&pos) else {
            return;
        };

        let latency = Instant::now().saturating_duration_since(started_at);
        self.latest = Some(latency);
        self.completed_total = self.completed_total.saturating_add(1);
        self.completed_samples.push_back(latency);
        if self.completed_samples.len() > EDIT_LATENCY_SAMPLE_WINDOW {
            self.completed_samples.pop_front();
        }
    }

    fn metrics(&self) -> EditLatencyMetrics {
        let mut metrics = EditLatencyMetrics {
            pending: self.pending_by_chunk.len(),
            completed_total: self.completed_total,
            latest_ms: self
                .latest
                .map_or(0.0, |duration| duration.as_secs_f64() * 1000.0),
            ..Default::default()
        };

        if self.completed_samples.is_empty() {
            return metrics;
        }

        let mut sample_ms = Vec::with_capacity(self.completed_samples.len());
        let mut total_ms = 0.0_f64;
        for sample in &self.completed_samples {
            let value = sample.as_secs_f64() * 1000.0;
            total_ms += value;
            sample_ms.push(value);
        }
        metrics.avg_ms = total_ms / sample_ms.len() as f64;
        sample_ms.sort_by(|a, b| a.total_cmp(b));
        let p95_index = ((sample_ms.len() - 1) as f64 * 0.95).round() as usize;
        metrics.p95_ms = sample_ms[p95_index.min(sample_ms.len() - 1)];
        metrics
    }
}

pub fn run() -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(build_env_filter())
        .init();

    let event_loop = EventLoop::new()?;
    let window = WindowBuilder::new()
        .with_title("ThingCraft (Alpha 1.2.6 Rebuild)")
        .with_inner_size(PhysicalSize::new(1280, 720))
        .build(&event_loop)?;

    // Surface lifetime is bound to the window; leaking avoids self-referential capture issues
    // with winit's long-lived event loop closure.
    let window: &'static Window = Box::leak(Box::new(window));
    let mut renderer = pollster::block_on(Renderer::new(window))?;

    let mut ecs_runtime = EcsRuntime::new();
    let fixed_config = ecs_runtime.fixed_tick_config();
    let mut fixed_clock = FixedStepClock::new(fixed_config.tick_hz, fixed_config.max_catchup_steps);
    let bootstrap_world = BootstrapWorld::alpha_bootstrap();
    let bootstrap_mesh =
        build_region_mesh(&bootstrap_world.spawn_region, &bootstrap_world.registry);
    renderer.set_scene_mesh(&bootstrap_mesh);
    let mut bootstrap_mesh_active = true;

    let residency_config = resolve_residency_config();
    let mut chunk_streamer = ChunkStreamer::new(
        0xA126_0001,
        bootstrap_world.registry.clone(),
        residency_config,
    );
    let _ = chunk_streamer.update_target(ChunkPos { x: 0, z: 0 });
    for update in chunk_streamer.drain_render_updates() {
        if apply_render_update(&mut renderer, update) && bootstrap_mesh_active {
            renderer.set_scene_mesh(&ChunkMesh::default());
            bootstrap_mesh_active = false;
        }
    }

    let mut last_frame_start = Instant::now();
    let mut loop_stats = LoopStats::default();
    let mut mouse_captured = false;
    let mut block_interaction_requests = VecDeque::new();
    let mut edit_latency_tracker = EditLatencyTracker::default();
    let mut hotbar_inventory = HotbarInventory::alpha_defaults();
    let mut selected_hotbar_slot = 0_usize;
    let mut hud_dirty = true;
    let selected_slot = hotbar_inventory.slot(selected_hotbar_slot).unwrap_or_default();

    info!(
        tick_hz = fixed_config.tick_hz,
        max_catchup_steps = fixed_config.max_catchup_steps,
        block_count = bootstrap_world.registry.defined_block_count(),
        exclusions_ok = bootstrap_world.registry.alpha_exclusions_respected(),
        spawn_chunk_x = bootstrap_world.spawn_chunk.pos.x,
        spawn_chunk_z = bootstrap_world.spawn_chunk.pos.z,
        spawn_region_chunks = bootstrap_world.spawn_region.len(),
        center_height = bootstrap_world.spawn_chunk.height_at(8, 8),
        bootstrap_mesh_vertices = bootstrap_mesh.vertices.len(),
        bootstrap_mesh_indices = bootstrap_mesh.indices.len(),
        stream_view_radius = residency_config.view_radius,
        stream_gen_budget = residency_config.max_generation_dispatch,
        stream_light_budget = residency_config.max_lighting_dispatch,
        stream_mesh_budget = residency_config.max_meshing_dispatch,
        selected_hotbar_slot = 1,
        selected_place_block = selected_slot.block_id,
        selected_place_count = selected_slot.count,
        "thingcraft client booted"
    );

    event_loop.run(move |event, event_loop| {
        event_loop.set_control_flow(ControlFlow::Poll);

        match event {
            Event::WindowEvent { event, window_id } if window_id == window.id() => match event {
                WindowEvent::CloseRequested => event_loop.exit(),
                WindowEvent::Resized(size) => {
                    renderer.resize(size);
                    hud_dirty = true;
                }
                WindowEvent::ScaleFactorChanged { .. } => {
                    renderer.resize(window.inner_size());
                    hud_dirty = true;
                }
                WindowEvent::RedrawRequested => {
                    let now = Instant::now();
                    let frame_delta = now.saturating_duration_since(last_frame_start);
                    last_frame_start = now;

                    ecs_runtime.run_input();

                    let tick_timer_start = Instant::now();
                    let ticks_to_run = fixed_clock.advance(frame_delta);
                    let fixed_dt_seconds = fixed_clock.tick_dt().as_secs_f64();
                    for _ in 0..ticks_to_run {
                        ecs_runtime.run_fixed(fixed_dt_seconds);
                        let fly_mode = ecs_runtime.is_fly_mode();
                        resolve_player_physics(
                            &mut ecs_runtime,
                            &chunk_streamer,
                            &bootstrap_world.registry,
                            fly_mode,
                        );
                        let pre_inv = hotbar_inventory.snapshot_counts();
                        process_block_interaction_requests(
                            &mut chunk_streamer,
                            &mut block_interaction_requests,
                            &mut edit_latency_tracker,
                            &mut hotbar_inventory,
                        );
                        if hotbar_inventory.snapshot_counts() != pre_inv {
                            hud_dirty = true;
                        }
                    }
                    let tick_duration = tick_timer_start.elapsed();

                    ecs_runtime.run_render_prep(fixed_clock.alpha() as f32);
                    let mut frame_camera: Option<crate::ecs::CameraSnapshot> = None;
                    let mut camera_chunk = ChunkPos { x: 0, z: 0 };
                    if let Some(snapshot) = ecs_runtime.camera_snapshot() {
                        let direction = direction_from_angles(
                            snapshot.interpolated.yaw,
                            snapshot.interpolated.pitch,
                        );
                        let view =
                            Mat4::look_to_rh(snapshot.interpolated.position, direction, Vec3::Y);
                        let projection = Mat4::perspective_rh_gl(
                            70_f32.to_radians(),
                            renderer.viewport_aspect().max(0.001),
                            0.05,
                            512.0,
                        );
                        renderer.update_camera((projection * view).to_cols_array_2d());
                        camera_chunk = world_pos_to_chunk_pos(
                            snapshot.authoritative.position.x,
                            snapshot.authoritative.position.z,
                        );
                        frame_camera = Some(snapshot);
                    }

                    chunk_streamer.tick(camera_chunk);
                    if renderer.chunk_border_debug_enabled() {
                        renderer.set_chunk_debug_states(chunk_streamer.debug_chunk_states());
                    }
                    if APPLY_STREAM_UPDATES_TO_RENDERER {
                        for update in chunk_streamer.drain_render_updates() {
                            let update_pos = match &update {
                                RenderMeshUpdate::Upsert { pos, .. }
                                | RenderMeshUpdate::Remove { pos } => *pos,
                            };
                            if apply_render_update(&mut renderer, update) && bootstrap_mesh_active {
                                renderer.set_scene_mesh(&ChunkMesh::default());
                                bootstrap_mesh_active = false;
                            }
                            edit_latency_tracker.observe_render_update(update_pos);
                        }
                    }

                    // Build and upload HUD vertices only when state changes.
                    if hud_dirty {
                        let (sw, sh) = renderer.screen_size();
                        let mut hud_verts = hud::build_crosshair_vertices(sw, sh);
                        let slot_counts: [u8; HOTBAR_BLOCK_IDS.len()] =
                            std::array::from_fn(|i| hotbar_inventory.slot(i).map_or(0, |s| s.count));
                        hud_verts.extend(hud::build_hotbar_vertices(
                            sw,
                            sh,
                            selected_hotbar_slot,
                            &slot_counts,
                        ));
                        renderer.update_hud(&hud_verts);
                        hud_dirty = false;
                    }

                    match renderer.render() {
                        Ok(()) => {}
                        Err(RenderError::Timeout) => {
                            warn!("surface timeout/loss while rendering; frame skipped");
                        }
                        Err(RenderError::OutOfMemory) => {
                            error!("surface out of memory; exiting");
                            event_loop.exit();
                            return;
                        }
                    }

                    if let Some(report) =
                        loop_stats.record_frame(frame_delta, ticks_to_run, tick_duration)
                    {
                        let residency = chunk_streamer.metrics();
                        let edit_latency = edit_latency_tracker.metrics();
                        if let Some(snapshot) = frame_camera {
                            debug!(
                                fps = report.fps,
                                tps = report.tps,
                                avg_frame_ms = report.avg_frame_ms,
                                avg_tick_ms = report.avg_tick_ms,
                                camera_x = snapshot.authoritative.position.x,
                                camera_y = snapshot.authoritative.position.y,
                                camera_z = snapshot.authoritative.position.z,
                                fly_mode = snapshot.fly_mode,
                                resident_chunks = residency.total,
                                resident_chunks_gpu = renderer.chunk_mesh_count(),
                                ready_chunks = residency.ready,
                                generating_chunks = residency.generating,
                                meshing_chunks = residency.meshing,
                                evicting_chunks = residency.evicting,
                                dirty_chunks = residency.dirty_chunks,
                                remesh_enqueued = residency.remesh_enqueued,
                                relight_enqueued = residency.relight_enqueued,
                                relight_dropped_stale = residency.relight_dropped_stale,
                                in_flight_generation = residency.in_flight_generation,
                                in_flight_lighting = residency.in_flight_lighting,
                                in_flight_meshing = residency.in_flight_meshing,
                                urgent_lighting = residency.urgent_lighting,
                                urgent_meshing = residency.urgent_meshing,
                                edit_latency_pending = edit_latency.pending,
                                edit_latency_completed = edit_latency.completed_total,
                                edit_latency_latest_ms = edit_latency.latest_ms,
                                edit_latency_avg_ms = edit_latency.avg_ms,
                                edit_latency_p95_ms = edit_latency.p95_ms,
                                visible_chunks = renderer.visible_chunk_count(),
                                "runtime stats"
                            );
                        } else {
                            debug!(
                                fps = report.fps,
                                tps = report.tps,
                                avg_frame_ms = report.avg_frame_ms,
                                avg_tick_ms = report.avg_tick_ms,
                                resident_chunks = residency.total,
                                resident_chunks_gpu = renderer.chunk_mesh_count(),
                                ready_chunks = residency.ready,
                                generating_chunks = residency.generating,
                                meshing_chunks = residency.meshing,
                                evicting_chunks = residency.evicting,
                                dirty_chunks = residency.dirty_chunks,
                                remesh_enqueued = residency.remesh_enqueued,
                                relight_enqueued = residency.relight_enqueued,
                                relight_dropped_stale = residency.relight_dropped_stale,
                                in_flight_generation = residency.in_flight_generation,
                                in_flight_lighting = residency.in_flight_lighting,
                                in_flight_meshing = residency.in_flight_meshing,
                                urgent_lighting = residency.urgent_lighting,
                                urgent_meshing = residency.urgent_meshing,
                                edit_latency_pending = edit_latency.pending,
                                edit_latency_completed = edit_latency.completed_total,
                                edit_latency_latest_ms = edit_latency.latest_ms,
                                edit_latency_avg_ms = edit_latency.avg_ms,
                                edit_latency_p95_ms = edit_latency.p95_ms,
                                visible_chunks = renderer.visible_chunk_count(),
                                "runtime stats"
                            );
                        }
                    }
                }
                WindowEvent::KeyboardInput { event, .. } => {
                    if let PhysicalKey::Code(code) = event.physical_key {
                        let is_pressed = event.state == ElementState::Pressed;
                        ecs_runtime.handle_key(code, is_pressed);

                        if is_pressed && !event.repeat {
                            if let Some(slot) = hotbar_slot_for_key(code) {
                                selected_hotbar_slot = slot;
                                hud_dirty = true;
                                let slot_state = hotbar_inventory.slot(slot).unwrap_or_default();
                                let block_id = slot_state.block_id;
                                let count = slot_state.count;
                                if bootstrap_world.registry.is_defined_block(block_id) {
                                    let block_name = bootstrap_world
                                        .registry
                                        .get(block_id)
                                        .map_or("unknown", |block| block.name);
                                    info!(
                                        hotbar_slot = slot + 1,
                                        place_block_id = block_id,
                                        place_stack_count = count,
                                        place_block_name = block_name,
                                        "updated selected placement block"
                                    );
                                } else {
                                    warn!(
                                        hotbar_slot = slot + 1,
                                        place_block_id = block_id,
                                        "hotbar slot points to undefined block id; selection ignored"
                                    );
                                }
                            }
                        }

                        if code == KeyCode::KeyB && is_pressed && !event.repeat {
                            let enabled = !renderer.chunk_border_debug_enabled();
                            renderer.set_chunk_border_debug(enabled);
                            info!(
                                chunk_border_debug = enabled,
                                "toggled chunk border debug overlay"
                            );
                        }

                        if code == KeyCode::Escape && is_pressed {
                            mouse_captured = false;
                            set_mouse_capture(window, false);
                        }
                    }
                }
                WindowEvent::MouseInput {
                    button: MouseButton::Left,
                    state: ElementState::Pressed,
                    ..
                } => {
                    if !mouse_captured {
                        mouse_captured = true;
                        set_mouse_capture(window, true);
                    } else {
                        enqueue_block_interaction_request(
                            &mut ecs_runtime,
                            &mut block_interaction_requests,
                            BlockInteractionKind::Break,
                        );
                    }
                }
                WindowEvent::MouseInput {
                    button: MouseButton::Right,
                    state: ElementState::Pressed,
                    ..
                } if mouse_captured => {
                    enqueue_block_interaction_request(
                        &mut ecs_runtime,
                        &mut block_interaction_requests,
                        BlockInteractionKind::Place {
                            slot: selected_hotbar_slot,
                        },
                    );
                }
                _ => {}
            },
            Event::DeviceEvent {
                event: DeviceEvent::MouseMotion { delta },
                ..
            } if mouse_captured => {
                ecs_runtime.add_mouse_delta(delta.0, delta.1);
            }
            Event::AboutToWait => {
                window.request_redraw();
            }
            _ => {}
        }
    })?;

    Ok(())
}

fn direction_from_angles(yaw: f32, pitch: f32) -> Vec3 {
    let yaw = yaw as f64;
    let pitch = pitch as f64;

    let x = yaw.sin() * pitch.cos();
    let y = pitch.sin();
    let z = yaw.cos() * pitch.cos();
    Vec3::new(x as f32, y as f32, z as f32).normalize_or_zero()
}

fn direction_from_angles64(yaw: f64, pitch: f64) -> DVec3 {
    let x = yaw.sin() * pitch.cos();
    let y = pitch.sin();
    let z = yaw.cos() * pitch.cos();
    DVec3::new(x, y, z).normalize_or_zero()
}

fn enqueue_block_interaction_request(
    ecs_runtime: &mut EcsRuntime,
    queue: &mut VecDeque<BlockInteractionRequest>,
    kind: BlockInteractionKind,
) {
    let Some(snapshot) = ecs_runtime.camera_snapshot() else {
        return;
    };

    let direction =
        direction_from_angles64(snapshot.authoritative.yaw, snapshot.authoritative.pitch);
    if direction.length_squared() == 0.0 {
        return;
    }

    // In physics mode, position is at feet. Raycast from eye height.
    let mut origin = snapshot.authoritative.position;
    if !snapshot.fly_mode {
        origin.y += snapshot.physics.eye_height;
    }

    queue.push_back(BlockInteractionRequest {
        origin,
        direction,
        kind,
    });
}

fn process_block_interaction_requests(
    chunk_streamer: &mut ChunkStreamer,
    queue: &mut VecDeque<BlockInteractionRequest>,
    edit_latency_tracker: &mut EditLatencyTracker,
    hotbar_inventory: &mut HotbarInventory,
) {
    while let Some(request) = queue.pop_front() {
        process_single_block_interaction_request(
            chunk_streamer,
            request,
            edit_latency_tracker,
            hotbar_inventory,
        );
    }
}

fn process_single_block_interaction_request(
    chunk_streamer: &mut ChunkStreamer,
    request: BlockInteractionRequest,
    edit_latency_tracker: &mut EditLatencyTracker,
    hotbar_inventory: &mut HotbarInventory,
) {
    let Some(hit) = raycast_first_solid_block(
        request.origin,
        request.direction,
        BLOCK_INTERACTION_REACH_BLOCKS,
        |x, y, z| chunk_streamer.block_at_world(x, y, z),
    ) else {
        return;
    };

    match request.kind {
        BlockInteractionKind::Break => {
            let broken_block_id = chunk_streamer
                .block_at_world(hit.block[0], hit.block[1], hit.block[2])
                .unwrap_or(AIR_BLOCK_ID);
            if chunk_streamer.set_block_at_world(
                hit.block[0],
                hit.block[1],
                hit.block[2],
                AIR_BLOCK_ID,
            ) {
                hotbar_inventory.try_pickup_block(broken_block_id);
                edit_latency_tracker.record_block_edit(hit.block[0], hit.block[2]);
            }
        }
        BlockInteractionKind::Place { slot } => {
            if hit.normal == [0, 0, 0] {
                return;
            }
            let Some(slot_data) = hotbar_inventory.slot(slot) else {
                return;
            };
            if slot_data.count == 0 {
                return;
            }

            let block_id = slot_data.block_id;
            let [x, y, z] = hit.block;
            let Some(place_x) = x.checked_add(hit.normal[0]) else {
                return;
            };
            let Some(place_y) = y.checked_add(hit.normal[1]) else {
                return;
            };
            let Some(place_z) = z.checked_add(hit.normal[2]) else {
                return;
            };

            if chunk_streamer.block_at_world(place_x, place_y, place_z) == Some(AIR_BLOCK_ID)
                && chunk_streamer.set_block_at_world(place_x, place_y, place_z, block_id)
            {
                hotbar_inventory.try_consume_from_slot(slot);
                edit_latency_tracker.record_block_edit(place_x, place_z);
            }
        }
    }
}

fn affected_chunks_for_block_edit(world_x: i32, world_z: i32) -> Vec<ChunkPos> {
    let (pos, local_x, local_z) = world_block_to_chunk_pos_and_local(world_x, world_z);
    let mut affected = vec![pos];

    if local_x == 0 {
        affected.push(ChunkPos {
            x: pos.x - 1,
            z: pos.z,
        });
    }
    if local_x == (CHUNK_WIDTH as u8 - 1) {
        affected.push(ChunkPos {
            x: pos.x + 1,
            z: pos.z,
        });
    }
    if local_z == 0 {
        affected.push(ChunkPos {
            x: pos.x,
            z: pos.z - 1,
        });
    }
    if local_z == (CHUNK_DEPTH as u8 - 1) {
        affected.push(ChunkPos {
            x: pos.x,
            z: pos.z + 1,
        });
    }

    affected
}

fn hotbar_slot_for_key(code: KeyCode) -> Option<usize> {
    match code {
        KeyCode::Digit1 => Some(0),
        KeyCode::Digit2 => Some(1),
        KeyCode::Digit3 => Some(2),
        KeyCode::Digit4 => Some(3),
        KeyCode::Digit5 => Some(4),
        KeyCode::Digit6 => Some(5),
        KeyCode::Digit7 => Some(6),
        KeyCode::Digit8 => Some(7),
        KeyCode::Digit9 => Some(8),
        _ => None,
    }
}

fn raycast_first_solid_block<F>(
    origin: DVec3,
    direction: DVec3,
    max_distance: f64,
    mut block_lookup: F,
) -> Option<BlockRayHit>
where
    F: FnMut(i32, i32, i32) -> Option<u8>,
{
    if max_distance <= 0.0 {
        return None;
    }

    let dir = direction.normalize_or_zero();
    if dir.length_squared() == 0.0 {
        return None;
    }

    let mut voxel = [
        origin.x.floor() as i32,
        origin.y.floor() as i32,
        origin.z.floor() as i32,
    ];
    let step = [axis_step(dir.x), axis_step(dir.y), axis_step(dir.z)];
    let mut t_max = [
        axis_t_max(origin.x, dir.x, voxel[0], step[0]),
        axis_t_max(origin.y, dir.y, voxel[1], step[1]),
        axis_t_max(origin.z, dir.z, voxel[2], step[2]),
    ];
    let t_delta = [
        axis_t_delta(dir.x, step[0]),
        axis_t_delta(dir.y, step[1]),
        axis_t_delta(dir.z, step[2]),
    ];
    let mut distance = 0.0_f64;
    let mut hit_normal = [0, 0, 0];

    loop {
        if distance > max_distance {
            return None;
        }

        if block_lookup(voxel[0], voxel[1], voxel[2]).is_some_and(|block| block != AIR_BLOCK_ID) {
            return Some(BlockRayHit {
                block: voxel,
                normal: hit_normal,
            });
        }

        let (axis, next_distance) = min_axis_with_distance(t_max);
        if !next_distance.is_finite() || next_distance > max_distance {
            return None;
        }

        voxel[axis] += step[axis];
        distance = next_distance;
        t_max[axis] += t_delta[axis];
        hit_normal = [0, 0, 0];
        hit_normal[axis] = -step[axis];
    }
}

fn axis_step(component: f64) -> i32 {
    if component > 0.0 {
        1
    } else if component < 0.0 {
        -1
    } else {
        0
    }
}

fn axis_t_delta(component: f64, step: i32) -> f64 {
    if step == 0 {
        f64::INFINITY
    } else {
        1.0 / component.abs()
    }
}

fn axis_t_max(origin: f64, component: f64, voxel: i32, step: i32) -> f64 {
    if step == 0 {
        return f64::INFINITY;
    }
    let next_boundary = if step > 0 {
        f64::from(voxel + 1)
    } else {
        f64::from(voxel)
    };
    (next_boundary - origin) / component
}

fn min_axis_with_distance(t_max: [f64; 3]) -> (usize, f64) {
    if t_max[0] <= t_max[1] && t_max[0] <= t_max[2] {
        (0, t_max[0])
    } else if t_max[1] <= t_max[2] {
        (1, t_max[1])
    } else {
        (2, t_max[2])
    }
}

fn set_mouse_capture(window: &Window, captured: bool) {
    window.set_cursor_visible(!captured);

    if captured {
        if let Err(err) = window.set_cursor_grab(CursorGrabMode::Locked) {
            warn!(?err, "failed to lock cursor, falling back to confined mode");
            if let Err(confined_err) = window.set_cursor_grab(CursorGrabMode::Confined) {
                warn!(?confined_err, "failed to confine cursor");
            }
        }
    } else if let Err(err) = window.set_cursor_grab(CursorGrabMode::None) {
        warn!(?err, "failed to release cursor");
    }
}

fn apply_render_update(renderer: &mut Renderer<'_>, update: RenderMeshUpdate) -> bool {
    match update {
        RenderMeshUpdate::Upsert { pos, mesh } => {
            renderer.upsert_chunk_mesh(pos, &mesh);
            true
        }
        RenderMeshUpdate::Remove { pos } => {
            renderer.remove_chunk_mesh(pos);
            false
        }
    }
}

fn build_env_filter() -> tracing_subscriber::EnvFilter {
    let rust_log = std::env::var("RUST_LOG").ok();
    let mut filter = tracing_subscriber::EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info,thingcraft_client=info"));

    for directive in NOISY_LOG_TARGET_DEFAULTS {
        let target = directive.split('=').next().unwrap_or_default();
        let has_explicit_target = rust_log
            .as_deref()
            .is_some_and(|value| has_target_directive(value, target));
        if has_explicit_target {
            continue;
        }

        if let Ok(parsed) = directive.parse() {
            filter = filter.add_directive(parsed);
        }
    }

    filter
}

fn has_target_directive(env_filter: &str, target: &str) -> bool {
    env_filter.split(',').map(str::trim).any(|directive| {
        directive == target
            || directive
                .strip_prefix(target)
                .is_some_and(|suffix| suffix.starts_with('='))
    })
}

fn resolve_residency_config() -> ResidencyConfig {
    let mut config = ResidencyConfig::default();
    if let Some(parsed) = parse_env_u32("THINGCRAFT_VIEW_RADIUS") {
        config.view_radius = parsed as i32;
    }
    if let Some(parsed) = parse_env_u32("THINGCRAFT_GEN_BUDGET") {
        config.max_generation_dispatch = parsed as usize;
    }
    if let Some(parsed) = parse_env_u32("THINGCRAFT_LIGHT_BUDGET") {
        config.max_lighting_dispatch = parsed as usize;
    }
    if let Some(parsed) = parse_env_u32("THINGCRAFT_MESH_BUDGET") {
        config.max_meshing_dispatch = parsed as usize;
    }
    config
}

fn parse_env_u32(key: &str) -> Option<u32> {
    let raw = std::env::var(key).ok()?;
    match raw.parse::<u32>() {
        Ok(value) => Some(value),
        Err(err) => {
            warn!(env = key, value = %raw, ?err, "invalid env override; using default");
            None
        }
    }
}

// ---------------------------------------------------------------------------
// Player AABB physics resolution (from Entity.move() in Alpha)
// ---------------------------------------------------------------------------

/// Resolve player physics: sweep AABB against world blocks, update position.
/// Called after `ecs_runtime.run_fixed()` each tick when fly_mode is off.
fn resolve_player_physics(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    fly_mode: bool,
) {
    if fly_mode {
        return;
    }
    let Some((transform, physics)) = ecs_runtime.player_physics() else {
        return;
    };

    let half_w = physics.width / 2.0;
    let height = physics.height;

    // The position that apply_player_motion_system already wrote includes velocity.
    // We need to resolve collisions against the world.
    let target_pos = transform.position;
    // Reconstruct pre-move position from prev_position (which was saved before velocity applied).
    let start_pos = transform.prev_position;
    let mut delta = target_pos - start_pos;

    let mut pos = start_pos;
    let original_delta = delta;

    // Reusable scratch buffer for collecting block AABBs (avoids allocation per axis).
    let mut scratch = Vec::with_capacity(32);

    // Y axis first (gravity is the most important axis to resolve).
    let expanded_min_y = DVec3::new(
        pos.x - half_w,
        pos.y.min(pos.y + delta.y),
        pos.z - half_w,
    );
    let expanded_max_y = DVec3::new(
        pos.x + half_w,
        (pos.y + height).max(pos.y + height + delta.y),
        pos.z + half_w,
    );
    collect_solid_block_aabbs(&mut scratch, chunk_streamer, registry, expanded_min_y, expanded_max_y);
    delta.y = resolve_axis(pos, half_w, height, delta.y, &scratch, 1);
    pos.y += delta.y;

    // X axis.
    let expanded_min_x = DVec3::new(
        pos.x.min(pos.x + delta.x) - half_w,
        pos.y,
        pos.z - half_w,
    );
    let expanded_max_x = DVec3::new(
        pos.x.max(pos.x + delta.x) + half_w,
        pos.y + height,
        pos.z + half_w,
    );
    collect_solid_block_aabbs(&mut scratch, chunk_streamer, registry, expanded_min_x, expanded_max_x);
    delta.x = resolve_axis(pos, half_w, height, delta.x, &scratch, 0);
    pos.x += delta.x;

    // Z axis.
    let expanded_min_z = DVec3::new(
        pos.x - half_w,
        pos.y,
        pos.z.min(pos.z + delta.z) - half_w,
    );
    let expanded_max_z = DVec3::new(
        pos.x + half_w,
        pos.y + height,
        pos.z.max(pos.z + delta.z) + half_w,
    );
    collect_solid_block_aabbs(&mut scratch, chunk_streamer, registry, expanded_min_z, expanded_max_z);
    delta.z = resolve_axis(pos, half_w, height, delta.z, &scratch, 2);
    pos.z += delta.z;

    // Step-up: if horizontal movement was blocked and on_ground, try stepping up.
    let h_blocked =
        (original_delta.x - delta.x).abs() > 1e-10 || (original_delta.z - delta.z).abs() > 1e-10;
    let was_on_ground = physics.on_ground;
    if h_blocked && was_on_ground {
        let step = physics.step_height;
        // Try movement at +step_height offset.
        let step_pos = DVec3::new(start_pos.x, start_pos.y + step, start_pos.z);
        let mut step_delta = original_delta;
        step_delta.y = 0.0;

        // Resolve X at stepped height.
        let smin_x = DVec3::new(
            step_pos.x.min(step_pos.x + step_delta.x) - half_w,
            step_pos.y,
            step_pos.z - half_w,
        );
        let smax_x = DVec3::new(
            step_pos.x.max(step_pos.x + step_delta.x) + half_w,
            step_pos.y + height,
            step_pos.z + half_w,
        );
        collect_solid_block_aabbs(&mut scratch, chunk_streamer, registry, smin_x, smax_x);
        step_delta.x = resolve_axis(step_pos, half_w, height, step_delta.x, &scratch, 0);
        let mut spos = DVec3::new(step_pos.x + step_delta.x, step_pos.y, step_pos.z);

        let smin_z = DVec3::new(
            spos.x - half_w,
            spos.y,
            spos.z.min(spos.z + step_delta.z) - half_w,
        );
        let smax_z = DVec3::new(
            spos.x + half_w,
            spos.y + height,
            spos.z.max(spos.z + step_delta.z) + half_w,
        );
        collect_solid_block_aabbs(&mut scratch, chunk_streamer, registry, smin_z, smax_z);
        step_delta.z = resolve_axis(spos, half_w, height, step_delta.z, &scratch, 2);
        spos.z += step_delta.z;

        // Drop back down by step height.
        let smin_drop = DVec3::new(spos.x - half_w, spos.y - step, spos.z - half_w);
        let smax_drop = DVec3::new(spos.x + half_w, spos.y + height, spos.z + half_w);
        collect_solid_block_aabbs(&mut scratch, chunk_streamer, registry, smin_drop, smax_drop);
        let drop_dy = resolve_axis(spos, half_w, height, -step, &scratch, 1);
        spos.y += drop_dy;

        // Accept step-up if it resulted in more horizontal movement.
        let stepped_h_sq = step_delta.x * step_delta.x + step_delta.z * step_delta.z;
        let original_h_sq = delta.x * delta.x + delta.z * delta.z;
        if stepped_h_sq > original_h_sq {
            pos = spos;
            delta.x = step_delta.x;
            delta.z = step_delta.z;
        }
    }

    // Update on_ground: blocked downward means on ground.
    let on_ground = original_delta.y < -1e-10 && (original_delta.y - delta.y).abs() > 1e-10;

    // Fall distance tracking.
    let mut fall_distance = physics.fall_distance;
    if delta.y < 0.0 {
        fall_distance -= delta.y;
    } else {
        fall_distance = 0.0;
    }
    if on_ground {
        fall_distance = 0.0;
    }

    // Velocity: zero out any component that was collision-clamped.
    let mut velocity = physics.velocity;
    if (original_delta.x - delta.x).abs() > 1e-10 {
        velocity.x = 0.0;
    }
    if (original_delta.y - delta.y).abs() > 1e-10 {
        velocity.y = 0.0;
    }
    if (original_delta.z - delta.z).abs() > 1e-10 {
        velocity.z = 0.0;
    }

    ecs_runtime.apply_resolved_physics(pos, velocity, on_ground, fall_distance);
}

/// Collect AABBs of all solid blocks overlapping the given region into `out`.
/// Clears `out` before filling — reuse across calls to avoid allocation.
fn collect_solid_block_aabbs(
    out: &mut Vec<[f64; 6]>,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    min: DVec3,
    max: DVec3,
) {
    out.clear();
    let bx_min = min.x.floor() as i32;
    let bx_max = max.x.floor() as i32;
    let by_min = min.y.floor() as i32;
    let by_max = max.y.floor() as i32;
    let bz_min = min.z.floor() as i32;
    let bz_max = max.z.floor() as i32;

    for bx in bx_min..=bx_max {
        for by in by_min..=by_max {
            if by < 0 || by >= CHUNK_HEIGHT as i32 {
                continue;
            }
            for bz in bz_min..=bz_max {
                if let Some(block_id) = chunk_streamer.block_at_world(bx, by, bz) {
                    if registry.get(block_id).is_some_and(|b| b.solid) {
                        out.push([
                            bx as f64,
                            by as f64,
                            bz as f64,
                            bx as f64 + 1.0,
                            by as f64 + 1.0,
                            bz as f64 + 1.0,
                        ]);
                    }
                }
            }
        }
    }
}

/// Resolve movement along a single axis against block AABBs. Returns clamped delta.
/// `axis`: 0=X, 1=Y, 2=Z. The perpendicular axes are checked for overlap.
fn resolve_axis(
    pos: DVec3,
    half_w: f64,
    height: f64,
    mut delta: f64,
    aabbs: &[[f64; 6]],
    axis: usize,
) -> f64 {
    // Player extent on each axis: [min, max].
    let player_min = [pos.x - half_w, pos.y, pos.z - half_w];
    let player_max = [pos.x + half_w, pos.y + height, pos.z + half_w];

    // The two axes perpendicular to `axis`.
    let perp = match axis {
        0 => [1, 2],
        1 => [0, 2],
        _ => [0, 1],
    };

    for aabb in aabbs {
        // Check overlap on perpendicular axes.
        if player_max[perp[0]] <= aabb[perp[0]]
            || player_min[perp[0]] >= aabb[perp[0] + 3]
            || player_max[perp[1]] <= aabb[perp[1]]
            || player_min[perp[1]] >= aabb[perp[1] + 3]
        {
            continue;
        }

        if delta < 0.0 {
            // Moving negative: clamp so our min face doesn't pass block max face.
            let gap = aabb[axis + 3] - player_min[axis];
            if gap <= 0.0 && delta < gap {
                delta = gap;
            }
        } else if delta > 0.0 {
            // Moving positive: clamp so our max face doesn't pass block min face.
            let gap = aabb[axis] - player_max[axis];
            if gap >= 0.0 && delta > gap {
                delta = gap;
            }
        }
    }
    delta
}

#[cfg(test)]
mod tests {
    use glam::DVec3;
    use winit::keyboard::KeyCode;

    use super::{
        affected_chunks_for_block_edit, has_target_directive, hotbar_slot_for_key, parse_env_u32,
        raycast_first_solid_block, resolve_axis, BlockRayHit, HotbarInventory, AIR_BLOCK_ID,
        HOTBAR_BLOCK_IDS, HOTBAR_STACK_LIMIT,
    };
    use crate::world::{BlockRegistry, ChunkPos, CHUNK_DEPTH, CHUNK_WIDTH};

    #[test]
    fn detects_target_directives_exactly() {
        assert!(has_target_directive(
            "thingcraft_client=debug,wgpu_core=trace",
            "wgpu_core"
        ));
        assert!(has_target_directive("wgpu_hal=debug", "wgpu_hal"));
        assert!(!has_target_directive(
            "thingcraft_client=debug,wgpu=info",
            "wgpu_core"
        ));
    }

    #[test]
    fn env_u32_parser_handles_valid_and_invalid_values() {
        assert_eq!(parse_env_u32("THINGCRAFT_TEST_MISSING"), None);
    }

    #[test]
    fn block_edit_chunk_affects_cardinal_neighbors_on_edges() {
        assert_eq!(
            affected_chunks_for_block_edit(1, 1),
            vec![ChunkPos { x: 0, z: 0 }]
        );

        let edge_x = CHUNK_WIDTH as i32 - 1;
        assert_eq!(
            affected_chunks_for_block_edit(edge_x, 2),
            vec![ChunkPos { x: 0, z: 0 }, ChunkPos { x: 1, z: 0 }]
        );

        let edge_z = CHUNK_DEPTH as i32 - 1;
        assert_eq!(
            affected_chunks_for_block_edit(2, edge_z),
            vec![ChunkPos { x: 0, z: 0 }, ChunkPos { x: 0, z: 1 }]
        );
    }

    #[test]
    fn hotbar_digit_keys_map_to_expected_slots() {
        assert_eq!(hotbar_slot_for_key(KeyCode::Digit1), Some(0));
        assert_eq!(hotbar_slot_for_key(KeyCode::Digit5), Some(4));
        assert_eq!(hotbar_slot_for_key(KeyCode::Digit9), Some(8));
        assert_eq!(hotbar_slot_for_key(KeyCode::KeyB), None);
    }

    #[test]
    fn hotbar_blocks_are_defined_in_alpha_registry() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(HOTBAR_BLOCK_IDS.len(), 9);
        for block_id in HOTBAR_BLOCK_IDS {
            assert!(registry.is_defined_block(block_id));
        }
    }

    #[test]
    fn hotbar_defaults_start_full_stacks() {
        let hotbar = HotbarInventory::alpha_defaults();
        for (slot_index, slot) in hotbar.slots.iter().enumerate() {
            assert_eq!(slot.block_id, HOTBAR_BLOCK_IDS[slot_index]);
            assert_eq!(slot.count, HOTBAR_STACK_LIMIT);
        }
    }

    #[test]
    fn hotbar_consume_stops_at_zero() {
        let mut hotbar = HotbarInventory::alpha_defaults();
        let slot = 0;
        for _ in 0..HOTBAR_STACK_LIMIT {
            assert!(hotbar.try_consume_from_slot(slot));
        }
        assert!(!hotbar.try_consume_from_slot(slot));
        assert_eq!(hotbar.slot(slot).map(|state| state.count), Some(0));
    }

    #[test]
    fn hotbar_pickup_requires_matching_slot_and_stack_space() {
        let mut hotbar = HotbarInventory::alpha_defaults();
        let dirt_slot = 0;
        assert!(hotbar.try_consume_from_slot(dirt_slot));
        assert_eq!(
            hotbar.slot(dirt_slot),
            Some(super::HotbarSlot {
                block_id: HOTBAR_BLOCK_IDS[dirt_slot],
                count: HOTBAR_STACK_LIMIT - 1,
            })
        );

        assert!(hotbar.try_pickup_block(HOTBAR_BLOCK_IDS[dirt_slot]));
        assert_eq!(
            hotbar.slot(dirt_slot),
            Some(super::HotbarSlot {
                block_id: HOTBAR_BLOCK_IDS[dirt_slot],
                count: HOTBAR_STACK_LIMIT,
            })
        );
        assert!(!hotbar.try_pickup_block(HOTBAR_BLOCK_IDS[dirt_slot]));
        assert!(!hotbar.try_pickup_block(2));
    }

    #[test]
    fn raycast_hits_expected_voxel_and_face() {
        let hit = raycast_first_solid_block(
            DVec3::new(0.5, 1.5, 0.5),
            DVec3::new(1.0, 0.0, 0.0),
            8.0,
            |x, y, z| {
                if [x, y, z] == [3, 1, 0] {
                    Some(1)
                } else {
                    Some(AIR_BLOCK_ID)
                }
            },
        );

        assert_eq!(
            hit,
            Some(BlockRayHit {
                block: [3, 1, 0],
                normal: [-1, 0, 0],
            })
        );
    }

    #[test]
    fn raycast_misses_when_no_solid_voxel_exists() {
        let hit = raycast_first_solid_block(
            DVec3::new(0.5, 1.5, 0.5),
            DVec3::new(0.0, 0.0, 1.0),
            5.0,
            |_x, _y, _z| Some(AIR_BLOCK_ID),
        );
        assert_eq!(hit, None);
    }

    #[test]
    fn aabb_resolve_y_clamps_downward_movement() {
        // Player at y=5.0, falling down. Block at y=4 (top face at y=5).
        let pos = DVec3::new(5.0, 5.0, 5.0);
        let half_w = 0.3;
        let height = 1.8;
        let block = [4.0, 4.0, 4.0, 5.5, 5.0, 5.5]; // block from (4,4,4) to (5.5,5,5.5)
        let dy = resolve_axis(pos, half_w, height, -1.0, &[block], 1);
        // Should clamp to exactly 0 (already resting on block top).
        assert!((dy - 0.0).abs() < 1e-10, "expected dy=0, got {dy}");
    }

    #[test]
    fn aabb_resolve_y_allows_free_fall_when_no_block_below() {
        let pos = DVec3::new(5.0, 10.0, 5.0);
        let half_w = 0.3;
        let height = 1.8;
        let dy = resolve_axis(pos, half_w, height, -0.5, &[], 1);
        assert!((dy - (-0.5)).abs() < 1e-10, "expected free fall, got {dy}");
    }

    #[test]
    fn aabb_resolve_x_clamps_into_wall() {
        let pos = DVec3::new(5.0, 5.0, 5.0);
        let half_w = 0.3;
        let height = 1.8;
        // Wall block at x=6 (from x=6 to x=7, ahead of player).
        let wall = [6.0, 4.0, 4.0, 7.0, 7.0, 6.0];
        let dx = resolve_axis(pos, half_w, height, 2.0, &[wall], 0);
        // Player right edge at 5.3, wall starts at x=6. Gap = 6.0 - 5.3 = 0.7.
        assert!(
            (dx - 0.7).abs() < 1e-10,
            "expected dx=0.7, got {dx}"
        );
    }

    #[test]
    fn aabb_resolve_z_clamps_into_wall() {
        let pos = DVec3::new(5.0, 5.0, 5.0);
        let half_w = 0.3;
        let height = 1.8;
        // Wall block at z=6.
        let wall = [4.0, 4.0, 6.0, 6.0, 7.0, 7.0];
        let dz = resolve_axis(pos, half_w, height, 2.0, &[wall], 2);
        // Player front edge at 5.3, wall starts at z=6. Gap = 6.0 - 5.3 = 0.7.
        assert!(
            (dz - 0.7).abs() < 1e-10,
            "expected dz=0.7, got {dz}"
        );
    }
}
