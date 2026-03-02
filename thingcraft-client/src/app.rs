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
use crate::mesh::{build_region_mesh, ChunkMesh};
use crate::renderer::{RenderError, Renderer};
use crate::streaming::{
    world_block_to_chunk_pos_and_local, world_pos_to_chunk_pos, ChunkStreamer, RenderMeshUpdate,
    ResidencyConfig,
};
use crate::time_step::FixedStepClock;
use crate::world::{BootstrapWorld, ChunkPos, CHUNK_DEPTH, CHUNK_WIDTH};

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
const EDIT_LATENCY_SAMPLE_WINDOW: usize = 128;

#[derive(Debug, Clone, Copy)]
enum BlockInteractionKind {
    Break,
    Place { block_id: u8 },
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
    let mut selected_place_block_id = HOTBAR_BLOCK_IDS[0];

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
        selected_place_block = selected_place_block_id,
        "thingcraft client booted"
    );

    event_loop.run(move |event, event_loop| {
        event_loop.set_control_flow(ControlFlow::Poll);

        match event {
            Event::WindowEvent { event, window_id } if window_id == window.id() => match event {
                WindowEvent::CloseRequested => event_loop.exit(),
                WindowEvent::Resized(size) => renderer.resize(size),
                WindowEvent::ScaleFactorChanged { .. } => renderer.resize(window.inner_size()),
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
                        process_block_interaction_requests(
                            &mut chunk_streamer,
                            &mut block_interaction_requests,
                            &mut edit_latency_tracker,
                        );
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
                                let block_id = HOTBAR_BLOCK_IDS[slot];
                                if bootstrap_world.registry.is_defined_block(block_id) {
                                    selected_place_block_id = block_id;
                                    let block_name = bootstrap_world
                                        .registry
                                        .get(block_id)
                                        .map_or("unknown", |block| block.name);
                                    info!(
                                        hotbar_slot = slot + 1,
                                        place_block_id = selected_place_block_id,
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
                            block_id: selected_place_block_id,
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

    queue.push_back(BlockInteractionRequest {
        origin: snapshot.authoritative.position,
        direction,
        kind,
    });
}

fn process_block_interaction_requests(
    chunk_streamer: &mut ChunkStreamer,
    queue: &mut VecDeque<BlockInteractionRequest>,
    edit_latency_tracker: &mut EditLatencyTracker,
) {
    while let Some(request) = queue.pop_front() {
        process_single_block_interaction_request(chunk_streamer, request, edit_latency_tracker);
    }
}

fn process_single_block_interaction_request(
    chunk_streamer: &mut ChunkStreamer,
    request: BlockInteractionRequest,
    edit_latency_tracker: &mut EditLatencyTracker,
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
            if chunk_streamer.set_block_at_world(
                hit.block[0],
                hit.block[1],
                hit.block[2],
                AIR_BLOCK_ID,
            ) {
                edit_latency_tracker.record_block_edit(hit.block[0], hit.block[2]);
            }
        }
        BlockInteractionKind::Place { block_id } => {
            if hit.normal == [0, 0, 0] {
                return;
            }
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

#[cfg(test)]
mod tests {
    use glam::DVec3;
    use winit::keyboard::KeyCode;

    use super::{
        affected_chunks_for_block_edit, has_target_directive, hotbar_slot_for_key, parse_env_u32,
        raycast_first_solid_block, BlockRayHit, AIR_BLOCK_ID, HOTBAR_BLOCK_IDS,
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
}
