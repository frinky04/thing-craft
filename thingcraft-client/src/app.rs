use std::time::{Duration, Instant};

use anyhow::Result;
use glam::{Mat4, Vec3};
use tracing::{debug, error, info, warn};
use winit::dpi::PhysicalSize;
use winit::event::{DeviceEvent, ElementState, Event, MouseButton, WindowEvent};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::keyboard::{KeyCode, PhysicalKey};
use winit::window::{CursorGrabMode, Window, WindowBuilder};

use crate::ecs::EcsRuntime;
use crate::mesh::{build_region_mesh, ChunkMesh};
use crate::renderer::{RenderError, Renderer};
use crate::streaming::{world_pos_to_chunk_pos, ChunkStreamer, RenderMeshUpdate, ResidencyConfig};
use crate::time_step::FixedStepClock;
use crate::world::{BootstrapWorld, ChunkPos};

const NOISY_LOG_TARGET_DEFAULTS: [&str; 5] = [
    "wgpu_core=warn",
    "wgpu_hal=error",
    "naga=warn",
    "ash=warn",
    "calloop=warn",
];
const APPLY_STREAM_UPDATES_TO_RENDERER: bool = true;

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

    let residency_config = ResidencyConfig::default();
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
        stream_mesh_budget = residency_config.max_meshing_dispatch,
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
                    if APPLY_STREAM_UPDATES_TO_RENDERER {
                        for update in chunk_streamer.drain_render_updates() {
                            if apply_render_update(&mut renderer, update) && bootstrap_mesh_active {
                                renderer.set_scene_mesh(&ChunkMesh::default());
                                bootstrap_mesh_active = false;
                            }
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
                                ready_chunks = residency.ready,
                                generating_chunks = residency.generating,
                                meshing_chunks = residency.meshing,
                                evicting_chunks = residency.evicting,
                                in_flight_generation = residency.in_flight_generation,
                                in_flight_meshing = residency.in_flight_meshing,
                                gpu_chunk_meshes = renderer.chunk_mesh_count(),
                                "runtime stats"
                            );
                        } else {
                            debug!(
                                fps = report.fps,
                                tps = report.tps,
                                avg_frame_ms = report.avg_frame_ms,
                                avg_tick_ms = report.avg_tick_ms,
                                resident_chunks = residency.total,
                                ready_chunks = residency.ready,
                                generating_chunks = residency.generating,
                                meshing_chunks = residency.meshing,
                                evicting_chunks = residency.evicting,
                                in_flight_generation = residency.in_flight_generation,
                                in_flight_meshing = residency.in_flight_meshing,
                                gpu_chunk_meshes = renderer.chunk_mesh_count(),
                                "runtime stats"
                            );
                        }
                    }
                }
                WindowEvent::KeyboardInput { event, .. } => {
                    if let PhysicalKey::Code(code) = event.physical_key {
                        let is_pressed = event.state == ElementState::Pressed;
                        ecs_runtime.handle_key(code, is_pressed);

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
                    mouse_captured = true;
                    set_mouse_capture(window, true);
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
        .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info,thingcraft_client=debug"));

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

#[cfg(test)]
mod tests {
    use super::has_target_directive;

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
}
