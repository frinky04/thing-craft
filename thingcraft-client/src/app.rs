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
use crate::inventory::{hit_test_slot, InventoryCommand, ItemKey, PlayerInventoryState};
use crate::mesh::{build_region_mesh, ChunkMesh, MeshVertex};
use crate::renderer::{RenderError, Renderer};
use crate::streaming::{
    world_block_to_chunk_pos_and_local, world_pos_to_chunk_pos, ChunkStreamer, RenderMeshUpdate,
    ResidencyConfig,
};
use crate::time_step::FixedStepClock;
use crate::world::{
    BiomeSource, BlockRegistry, BootstrapWorld, ChunkPos, MaterialKind, CHUNK_DEPTH, CHUNK_HEIGHT,
    CHUNK_WIDTH,
};

const NOISY_LOG_TARGET_DEFAULTS: [&str; 5] = [
    "wgpu_core=warn",
    "wgpu_hal=error",
    "naga=warn",
    "ash=warn",
    "calloop=warn",
];
const APPLY_STREAM_UPDATES_TO_RENDERER: bool = true;
const BLOCK_INTERACTION_REACH_BLOCKS: f64 = 5.0;
const AIR_BLOCK_ID: u8 = crate::inventory::AIR_BLOCK_ID;
const EDIT_LATENCY_SAMPLE_WINDOW: usize = 128;
const FLUID_TICK_BUDGET_DEFAULT: usize = 384;
const FLUID_TICK_BUDGET_HARD_MAX: usize = 1_000;
const FLUID_TICK_BUDGET_MIN: usize = 64;
const FLUID_BUDGET_ADAPT_STEP_MIN: usize = 16;
const FLUID_BUDGET_FRAME_HEADROOM_MS: f64 = 12.5;
const FLUID_BUDGET_FRAME_PRESSURE_MS: f64 = 19.0;
const FLUID_URGENT_SLICE_MIN: usize = 16;
const FLUID_URGENT_SLICE_DIVISOR: usize = 8;
const DAY_TICKS: u64 = 24_000;
const WORLD_SEED: u64 = 0xA126_0001;

#[derive(Debug, Clone, Copy)]
enum BlockInteractionKind {
    Break,
    Place,
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

#[derive(Debug, Clone, Copy)]
struct AdaptiveFluidBudget {
    current: usize,
    min: usize,
    max: usize,
}

#[derive(Debug, Clone, Copy)]
struct FirstPersonHandState {
    shown_block_id: Option<u8>,
    hand_height: f32,
    last_hand_height: f32,
    attack_progress: f32,
    last_attack_progress: f32,
    swing_ticks: i32,
    swing_active: bool,
}

impl Default for FirstPersonHandState {
    fn default() -> Self {
        Self {
            shown_block_id: None,
            hand_height: 0.0,
            last_hand_height: 0.0,
            attack_progress: 0.0,
            last_attack_progress: 0.0,
            swing_ticks: 0,
            swing_active: false,
        }
    }
}

impl FirstPersonHandState {
    fn trigger_swing(&mut self) {
        // Matches PlayerEntity.swingArm(): starts at -1 so the next tick begins at 0.
        self.swing_ticks = -1;
        self.swing_active = true;
    }

    fn trigger_item_used(&mut self) {
        // Matches ItemInHandRenderer.onBlockUsed()/onItemUsed().
        self.hand_height = 0.0;
    }

    fn tick(&mut self, selected_block_id: Option<u8>) {
        self.last_hand_height = self.hand_height;
        self.last_attack_progress = self.attack_progress;
        let target = if selected_block_id == self.shown_block_id {
            1.0
        } else {
            0.0
        };
        let mut delta = target - self.hand_height;
        delta = delta.clamp(-0.4, 0.4);
        self.hand_height += delta;
        if self.hand_height < 0.1 {
            self.shown_block_id = selected_block_id;
        }

        if self.swing_active {
            self.swing_ticks += 1;
            if self.swing_ticks >= 8 {
                self.swing_active = false;
                self.swing_ticks = 0;
            }
        } else {
            self.swing_ticks = 0;
        }
        self.attack_progress = self.swing_ticks as f32 / 8.0;
    }

    fn equip_progress(self, render_alpha: f32) -> f32 {
        self.last_hand_height + (self.hand_height - self.last_hand_height) * render_alpha
    }

    fn attack_progress(self, render_alpha: f32) -> f32 {
        // Matches MobEntity.getAttackAnimationProgress interpolation.
        let mut delta = self.attack_progress - self.last_attack_progress;
        if delta < 0.0 {
            delta += 1.0;
        }
        self.last_attack_progress + delta * render_alpha
    }
}

impl AdaptiveFluidBudget {
    fn new(base_budget: usize) -> Self {
        let max = FLUID_TICK_BUDGET_HARD_MAX.max(base_budget);
        let min = if base_budget <= FLUID_TICK_BUDGET_MIN {
            1
        } else {
            FLUID_TICK_BUDGET_MIN
        };
        Self {
            current: base_budget.clamp(min, max),
            min,
            max,
        }
    }

    fn current(self) -> usize {
        self.current
    }

    fn min(self) -> usize {
        self.min
    }

    fn max(self) -> usize {
        self.max
    }

    fn urgent_slice_for_tick(self, tick_budget: usize) -> usize {
        if tick_budget == 0 {
            return 0;
        }
        (tick_budget / FLUID_URGENT_SLICE_DIVISOR)
            .max(FLUID_URGENT_SLICE_MIN)
            .min(tick_budget)
    }

    fn observe_frame(
        &mut self,
        frame_delta: Duration,
        ticks_to_run: u32,
        processed_this_frame: usize,
        budget_left: usize,
    ) {
        let frame_ms = frame_delta.as_secs_f64() * 1000.0;
        let budget_used = self.current.saturating_sub(budget_left);
        let saturated = budget_left == 0 && processed_this_frame > 0;
        let step = (self.current / 8).max(FLUID_BUDGET_ADAPT_STEP_MIN);

        if frame_ms >= FLUID_BUDGET_FRAME_PRESSURE_MS && budget_used > 0 {
            self.current = self.current.saturating_sub(step).clamp(self.min, self.max);
            return;
        }

        if !saturated {
            return;
        }

        if frame_ms <= FLUID_BUDGET_FRAME_HEADROOM_MS
            || (ticks_to_run > 1 && frame_ms < FLUID_BUDGET_FRAME_PRESSURE_MS)
        {
            self.current = self.current.saturating_add(step).clamp(self.min, self.max);
        }
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
    let fancy_graphics = resolve_fancy_graphics();
    renderer.set_leaf_cutout_enabled(fancy_graphics);

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
        WORLD_SEED,
        bootstrap_world.registry.clone(),
        residency_config,
    );
    let biome_source = BiomeSource::new(WORLD_SEED);
    let alpha_view_distance = alpha_view_distance_setting(residency_config.view_radius);
    let render_distance_blocks = alpha_render_distance_blocks(alpha_view_distance);
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
    let mut inventory_open = false;
    let mut mouse_screen_pos = [0.0_f32, 0.0_f32];
    let mut block_interaction_requests = VecDeque::new();
    let mut inventory_commands: VecDeque<InventoryCommand> = VecDeque::new();
    let mut edit_latency_tracker = EditLatencyTracker::default();
    let mut inventory_state = PlayerInventoryState::alpha_defaults();
    let mut first_person_hand = FirstPersonHandState::default();
    let mut hud_dirty = true;
    let mut sim_ticks: u64 = 0;
    let mut time_offset_ticks: u64 = 0; // debug: accelerated by T key
    let mut time_accel_held = false;
    let mut last_fog_brightness = 1.0_f32;
    let mut fog_brightness = 1.0_f32;
    let base_fluid_tick_budget = resolve_fluid_tick_budget();
    let mut adaptive_fluid_budget = AdaptiveFluidBudget::new(base_fluid_tick_budget);
    let selected_slot = inventory_state.selected_stack();

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
        stream_upload_budget = residency_config.max_render_upload_sections_per_tick,
        stream_gen_workers = residency_config.generation_workers,
        stream_light_workers = residency_config.lighting_workers,
        stream_mesh_workers = residency_config.meshing_workers,
        fluid_tick_budget = base_fluid_tick_budget,
        fluid_tick_budget_min = adaptive_fluid_budget.min(),
        fluid_tick_budget_max = adaptive_fluid_budget.max(),
        fancy_graphics,
        selected_hotbar_slot = usize::from(inventory_state.selected_hotbar) + 1,
        selected_place_block = selected_slot.and_then(|stack| match stack.item {
            ItemKey::Block(id) if stack.count > 0 => Some(id),
            _ => None,
        }),
        selected_place_count = selected_slot.map_or(0, |stack| stack.count),
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
                },
                WindowEvent::ScaleFactorChanged { .. } => {
                    renderer.resize(window.inner_size());
                    hud_dirty = true;
                },
                WindowEvent::RedrawRequested => {
                    let now = Instant::now();
                    let frame_delta = now.saturating_duration_since(last_frame_start);
                    last_frame_start = now;

                    // Inventory screen should not preserve prior movement/look intents.
                    if inventory_open {
                        ecs_runtime.clear_input_state();
                    }
                    ecs_runtime.run_input();

                    let tick_timer_start = Instant::now();
                    let ticks_to_run = fixed_clock.advance(frame_delta);
                    let fixed_dt_seconds = fixed_clock.tick_dt().as_secs_f64();
                    let fluid_tick_budget = adaptive_fluid_budget.current();
                    let mut fluid_budget_remaining = fluid_tick_budget;
                    let mut fluid_processed_this_frame = 0_usize;
                    let mut urgent_fluid_processed_this_frame = 0_usize;
                    for tick_index in 0..ticks_to_run {
                        sim_ticks = sim_ticks.wrapping_add(1);
                        if time_accel_held {
                            time_offset_ticks = time_offset_ticks.wrapping_add(200);
                        }
                        chunk_streamer.begin_sim_tick(sim_ticks);
                        ecs_runtime.run_fixed(fixed_dt_seconds);
                        let fly_mode = ecs_runtime.is_fly_mode();
                        resolve_player_physics(
                            &mut ecs_runtime,
                            &chunk_streamer,
                            &bootstrap_world.registry,
                            fly_mode,
                        );
                        tick_player_survival(
                            ecs_runtime.world_mut(),
                            &chunk_streamer,
                            &bootstrap_world.registry,
                            sim_ticks,
                        );
                        // Entity framework ticks.
                        crate::entity::tick_entity_ages(ecs_runtime.world_mut());
                        crate::entity::tick_entity_physics(
                            ecs_runtime.world_mut(),
                            &chunk_streamer,
                            &bootstrap_world.registry,
                        );

                        let pre_inv = inventory_state.snapshot();

                        // Apply queued inventory commands in fixed tick (authoritative path).
                        while let Some(cmd) = inventory_commands.pop_front() {
                            let result = inventory_state.apply(cmd);
                            if result.changed {
                                hud_dirty = true;
                            }
                            if !result.dropped_to_world.is_empty() {
                                let drop_pos = ecs_runtime
                                    .camera_snapshot()
                                    .map(|snapshot| snapshot.authoritative.position + DVec3::new(0.0, 1.2, 0.0))
                                    .unwrap_or(DVec3::ZERO);
                                for dropped in result.dropped_to_world {
                                    let ItemKey::Block(block_id) = dropped.item;
                                    for _ in 0..dropped.count {
                                        crate::entity::spawn_dropped_item(
                                            ecs_runtime.world_mut(),
                                            block_id,
                                            drop_pos,
                                        );
                                    }
                                }
                            }
                        }

                        // Item pickup (player walks over dropped items).
                        if crate::entity::check_item_pickup(
                            ecs_runtime.world_mut(),
                            &mut inventory_state,
                        ) {
                            hud_dirty = true;
                        }

                        // Block interactions (break/place).
                        let item_spawns = process_block_interaction_requests(
                            &mut chunk_streamer,
                            &mut block_interaction_requests,
                            &mut edit_latency_tracker,
                            &mut inventory_state,
                        );
                        for (block_id, spawn_pos) in item_spawns {
                            crate::entity::spawn_dropped_item(
                                ecs_runtime.world_mut(),
                                block_id,
                                spawn_pos,
                            );
                        }

                        if inventory_state.snapshot() != pre_inv {
                            hud_dirty = true;
                        }
                        let selected_for_hand = inventory_state.selected_block_id();
                        first_person_hand.tick(selected_for_hand);
                        if fluid_budget_remaining > 0 {
                            let ticks_left = usize::try_from(ticks_to_run - tick_index).unwrap_or(1);
                            let tick_budget_slice = fluid_budget_remaining.div_ceil(ticks_left);

                            let urgent_slice_budget = adaptive_fluid_budget
                                .urgent_slice_for_tick(tick_budget_slice)
                                .min(fluid_budget_remaining);
                            if urgent_slice_budget > 0 {
                                let processed_urgent =
                                    chunk_streamer.tick_fluids_urgent(sim_ticks, urgent_slice_budget);
                                urgent_fluid_processed_this_frame += processed_urgent;
                                fluid_processed_this_frame += processed_urgent;
                                fluid_budget_remaining =
                                    fluid_budget_remaining.saturating_sub(processed_urgent);
                            }

                            let remaining_tick_budget = tick_budget_slice.min(fluid_budget_remaining);
                            if remaining_tick_budget > 0 {
                                let processed =
                                    chunk_streamer.tick_fluids(sim_ticks, remaining_tick_budget);
                                fluid_processed_this_frame += processed;
                                fluid_budget_remaining =
                                    fluid_budget_remaining.saturating_sub(processed);
                            }
                        }

                        for (block_id, [x, y, z]) in chunk_streamer.drain_fluid_item_drops() {
                            crate::entity::spawn_dropped_item(
                                ecs_runtime.world_mut(),
                                block_id,
                                DVec3::new(x, y, z),
                            );
                        }

                        // Alpha GameRenderer.tick(): smooth fog brightness toward sampled player
                        // brightness, biased by view-distance setting.
                        let tick_time_of_day = alpha_time_of_day(sim_ticks.wrapping_add(time_offset_ticks), 0.0);
                        let tick_ambient_darkness = alpha_ambient_darkness(tick_time_of_day);
                        let player_pos = ecs_runtime
                            .camera_snapshot()
                            .map_or(DVec3::ZERO, |snapshot| snapshot.authoritative.position);
                        last_fog_brightness = fog_brightness;
                        let target_fog_brightness = alpha_fog_brightness_target(
                            &chunk_streamer,
                            player_pos,
                            tick_ambient_darkness,
                            alpha_view_distance,
                        );
                        fog_brightness += (target_fog_brightness - fog_brightness) * 0.1;
                    }
                    let tick_duration = tick_timer_start.elapsed();
                    if ticks_to_run > 0 {
                        hud_dirty = true;
                    }
                    adaptive_fluid_budget.observe_frame(
                        frame_delta,
                        ticks_to_run,
                        fluid_processed_this_frame,
                        fluid_budget_remaining,
                    );
                    renderer.advance_dynamic_liquid_textures(ticks_to_run);

                    let render_alpha = fixed_clock.alpha() as f32;
                    let frame_fog_brightness = (last_fog_brightness
                        + (fog_brightness - last_fog_brightness) * render_alpha)
                        .clamp(0.0, 1.0);
                    ecs_runtime.run_render_prep(render_alpha);

                    let mut frame_camera: Option<crate::ecs::CameraSnapshot> = None;
                    let mut camera_chunk = ChunkPos { x: 0, z: 0 };
                    let time_of_day = alpha_time_of_day(sim_ticks.wrapping_add(time_offset_ticks), render_alpha);
                    let ambient_darkness = alpha_ambient_darkness(time_of_day);
                    let cloud_color = alpha_cloud_color(time_of_day);
                    let sunrise_color = alpha_sunrise_color(time_of_day);
                    let cloud_scroll = (sim_ticks as f32 + render_alpha) * 0.03;
                    let star_brightness = alpha_star_brightness(time_of_day);
                    let render_sky = alpha_view_distance < 2;
                    renderer.set_cloud_state(cloud_color, cloud_scroll);
                    renderer.set_sunrise_state(sunrise_color);
                    renderer.set_star_brightness(star_brightness);
                    renderer.set_time_of_day(time_of_day);
                    if let Some(snapshot) = ecs_runtime.camera_snapshot() {
                        // Build entity sprite mesh for this frame.
                        let entity_mesh = crate::entity::build_entity_sprite_mesh(
                            ecs_runtime.world_mut(),
                            snapshot.interpolated.yaw,
                            &bootstrap_world.registry,
                            &chunk_streamer,
                            ambient_darkness,
                            render_alpha,
                        );
                        renderer.update_entity_sprites(&entity_mesh);

                        let shadow_mesh = crate::entity::build_entity_shadow_mesh(
                            ecs_runtime.world_mut(),
                            &chunk_streamer,
                            &bootstrap_world.registry,
                            ambient_darkness,
                            snapshot.interpolated.position.to_array(),
                            render_alpha,
                        );
                        renderer.update_entity_shadows(&shadow_mesh);

                        let biome_sample = biome_source.sample(
                            snapshot.authoritative.position.x.floor() as i32,
                            snapshot.authoritative.position.z.floor() as i32,
                        );
                        let sky_color = alpha_sky_color(time_of_day, biome_sample.temperature as f32);
                        let fog_color = alpha_clear_fog_color(
                            alpha_fog_color(time_of_day),
                            sky_color,
                            alpha_view_distance,
                        );
                        let fog_color =
                            alpha_apply_fog_brightness(fog_color, frame_fog_brightness);
                        renderer.set_day_night(fog_color, sky_color, ambient_darkness, render_sky);

                        let direction = direction_from_angles(
                            snapshot.interpolated.yaw,
                            snapshot.interpolated.pitch,
                        );
                        let hurt_cam = alpha_hurt_cam_matrix(&snapshot, render_alpha);
                        let view_bob = alpha_view_bob_matrix(&snapshot, render_alpha);
                        let mut sky_camera_fx = hurt_cam * view_bob;
                        sky_camera_fx.w_axis = glam::Vec4::W;
                        let view =
                            Mat4::look_to_rh(snapshot.interpolated.position, direction, Vec3::Y);
                        let fov_degrees = alpha_camera_fov_degrees(&snapshot, render_alpha);
                        let projection = Mat4::perspective_rh_gl(
                            fov_degrees.to_radians(),
                            renderer.viewport_aspect().max(0.001),
                            0.05,
                            render_distance_blocks,
                        );
                        // Rotation-only view for sky/celestial (strips translation → infinite distance).
                        let sky_view = Mat4::look_to_rh(Vec3::ZERO, direction, Vec3::Y);
                        renderer.update_camera(
                            (projection * hurt_cam * view_bob * view).to_cols_array_2d(),
                            (projection * sky_camera_fx * sky_view).to_cols_array_2d(),
                            snapshot.interpolated.position.to_array(),
                            render_distance_blocks * 0.25,
                            render_distance_blocks,
                        );
                        let hand_view_proj = (projection * hurt_cam * view_bob).to_cols_array_2d();
                        let hand_brightness = alpha_first_person_brightness(
                            &chunk_streamer,
                            snapshot.interpolated.position,
                            ambient_darkness,
                        );
                        renderer.set_first_person_camera(hand_view_proj, hand_brightness);
                        let held_block = first_person_hand.shown_block_id;
                        let hand_item_mesh = build_first_person_item_mesh(
                            held_block,
                            &bootstrap_world.registry,
                            first_person_hand.equip_progress(render_alpha),
                            first_person_hand.attack_progress(render_alpha),
                        );
                        renderer.update_first_person_item_mesh(&hand_item_mesh);
                        let hand_arm_mesh = build_first_person_arm_mesh(
                            held_block.is_none(),
                            first_person_hand.equip_progress(render_alpha),
                            first_person_hand.attack_progress(render_alpha),
                        );
                        renderer.update_first_person_arm_mesh(&hand_arm_mesh);
                        camera_chunk = world_pos_to_chunk_pos(
                            snapshot.authoritative.position.x,
                            snapshot.authoritative.position.z,
                        );
                        // Per-frame raycast for block outline wireframe.
                        let ray_dir = DVec3::new(
                            direction.x as f64,
                            direction.y as f64,
                            direction.z as f64,
                        );
                        let ray_origin = DVec3::new(
                            snapshot.interpolated.position.x as f64,
                            snapshot.interpolated.position.y as f64,
                            snapshot.interpolated.position.z as f64,
                        );
                        let outline_hit = raycast_first_solid_block(
                            ray_origin,
                            ray_dir,
                            BLOCK_INTERACTION_REACH_BLOCKS,
                            |x, y, z| chunk_streamer.block_at_world(x, y, z),
                        );
                        renderer.set_block_outline(outline_hit.map(|h| h.block));

                        if snapshot.vitals.health <= 0 && mouse_captured {
                            mouse_captured = false;
                            set_mouse_capture(window, false);
                        }

                        frame_camera = Some(snapshot);
                    } else {
                        let biome_sample = biome_source.sample(0, 0);
                        let sky_color = alpha_sky_color(time_of_day, biome_sample.temperature as f32);
                        let fog_color = alpha_clear_fog_color(
                            alpha_fog_color(time_of_day),
                            sky_color,
                            alpha_view_distance,
                        );
                        let fog_color =
                            alpha_apply_fog_brightness(fog_color, frame_fog_brightness);
                        renderer.set_day_night(fog_color, sky_color, ambient_darkness, render_sky);
                        renderer.set_block_outline(None);
                    }

                    chunk_streamer.tick(camera_chunk);
                    if renderer.chunk_border_debug_enabled() {
                        renderer.set_chunk_debug_states(chunk_streamer.debug_chunk_states());
                    }
                    if APPLY_STREAM_UPDATES_TO_RENDERER {
                        for update in chunk_streamer.drain_render_updates() {
                            let update_pos = match &update {
                                RenderMeshUpdate::UpsertSection { pos, .. }
                                | RenderMeshUpdate::RemoveChunk { pos } => *pos,
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
                        let hud_verts = if inventory_open {
                            hud::build_inventory_vertices(
                                sw,
                                sh,
                                &inventory_state,
                                mouse_screen_pos,
                                &bootstrap_world.registry,
                            )
                        } else {
                            let slot_counts: [u8; crate::inventory::HOTBAR_SLOT_COUNT] =
                                std::array::from_fn(|i| {
                                    inventory_state.hotbar_stack(i).map_or(0, |stack| stack.count)
                                });
                            let slot_block_ids: [u8; crate::inventory::HOTBAR_SLOT_COUNT] =
                                std::array::from_fn(|i| {
                                    inventory_state.hotbar_stack(i).map_or(AIR_BLOCK_ID, |stack| {
                                        match stack.item {
                                            ItemKey::Block(block_id) => block_id,
                                        }
                                    })
                                });
                            let hud_state = hud::HudState {
                                selected_slot: usize::from(inventory_state.selected_hotbar),
                                slot_counts,
                                slot_block_ids,
                                health: frame_camera.map_or(20, |snapshot| snapshot.vitals.health),
                                prev_health: frame_camera
                                    .map_or(20, |snapshot| snapshot.vitals.prev_health),
                                invulnerable_timer: frame_camera
                                    .map_or(0, |snapshot| snapshot.vitals.invulnerable_timer),
                                breath: frame_camera.map_or(300, |snapshot| snapshot.vitals.breath),
                                breath_capacity: frame_camera
                                    .map_or(300, |snapshot| snapshot.vitals.breath_capacity),
                                submerged_in_water: frame_camera
                                    .is_some_and(|snapshot| snapshot.vitals.submerged_in_water),
                                armor_points: 0,
                                is_dead: frame_camera
                                    .is_some_and(|snapshot| snapshot.vitals.health <= 0),
                                death_ticks: frame_camera
                                    .map_or(0, |snapshot| snapshot.vitals.death_ticks),
                                sim_ticks,
                            };
                            hud::build_hud_vertices(sw, sh, &hud_state, &bootstrap_world.registry)
                        };
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
                                fluid_processed_frame = fluid_processed_this_frame,
                                fluid_processed_urgent_frame = urgent_fluid_processed_this_frame,
                                fluid_budget_active = fluid_tick_budget,
                                fluid_budget_left = fluid_budget_remaining,
                                ambient_darkness,
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
                            fluid_processed_frame = fluid_processed_this_frame,
                            fluid_processed_urgent_frame = urgent_fluid_processed_this_frame,
                            fluid_budget_active = fluid_tick_budget,
                            fluid_budget_left = fluid_budget_remaining,
                            ambient_darkness,
                            visible_chunks = renderer.visible_chunk_count(),
                            "runtime stats"
                        );
                    }
                }},
                WindowEvent::KeyboardInput { event, .. } => {
                    if let PhysicalKey::Code(code) = event.physical_key {
                        let is_pressed = event.state == ElementState::Pressed;
                        let player_dead = ecs_runtime.player_is_dead();
                        let allow_input_passthrough =
                            !player_dead || matches!(code, KeyCode::Escape | KeyCode::KeyR);
                        if allow_input_passthrough && !inventory_open {
                            ecs_runtime.handle_key(code, is_pressed);
                        }

                        if is_pressed && !event.repeat {
                            if player_dead && !matches!(code, KeyCode::KeyR | KeyCode::Escape) {
                                return;
                            }
                            if let Some(slot) = hotbar_slot_for_key(code) {
                                inventory_commands.push_back(InventoryCommand::SelectHotbar {
                                    index: slot as u8,
                                });
                                hud_dirty = true;
                                let slot_state = inventory_state.hotbar_stack(slot);
                                let block_id = slot_state.and_then(|stack| match stack.item {
                                    ItemKey::Block(id) if stack.count > 0 => Some(id),
                                    _ => None,
                                });
                                let count = slot_state.map_or(0, |stack| stack.count);
                                if let Some(block_id) = block_id {
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

                        // T key: hold to accelerate time of day (debug).
                        if code == KeyCode::KeyT {
                            time_accel_held = is_pressed;
                        }

                        if code == KeyCode::Escape && is_pressed {
                            if inventory_open {
                                inventory_commands.push_back(InventoryCommand::CloseInventory);
                                inventory_open = false;
                                if !ecs_runtime.player_is_dead() {
                                    mouse_captured = true;
                                    set_mouse_capture(window, true);
                                }
                            } else {
                                mouse_captured = false;
                                set_mouse_capture(window, false);
                            }
                            hud_dirty = true;
                        }

                        if code == KeyCode::KeyE && is_pressed && !event.repeat {
                            inventory_open = !inventory_open;
                            if inventory_open {
                                mouse_captured = false;
                                set_mouse_capture(window, false);
                                ecs_runtime.clear_input_state();
                            } else {
                                inventory_commands.push_back(InventoryCommand::CloseInventory);
                                if !ecs_runtime.player_is_dead() {
                                    mouse_captured = true;
                                    set_mouse_capture(window, true);
                                }
                            }
                            hud_dirty = true;
                        }

                        if code == KeyCode::KeyR && is_pressed && !event.repeat {
                            if try_respawn_player(&mut ecs_runtime) {
                                hud_dirty = true;
                                info!("player respawned");
                            }
                        }
                    }
                },
                WindowEvent::MouseInput {
                    button: MouseButton::Left,
                    state: ElementState::Pressed,
                    ..
                } => {
                    if ecs_runtime.player_is_dead() {
                        return;
                    }
                    if inventory_open {
                        let (sw, sh) = renderer.screen_size();
                        if let Some(slot) =
                            hit_test_slot(mouse_screen_pos[0], mouse_screen_pos[1], sw, sh)
                        {
                            inventory_commands.push_back(InventoryCommand::ClickSlot {
                                slot,
                                button: MouseButton::Left,
                            });
                        } else {
                            inventory_commands.push_back(InventoryCommand::ClickOutside {
                                button: MouseButton::Left,
                            });
                        }
                        hud_dirty = true;
                        return;
                    }
                    if !mouse_captured {
                        mouse_captured = true;
                        set_mouse_capture(window, true);
                    } else {
                        first_person_hand.trigger_swing();
                        enqueue_block_interaction_request(
                            &mut ecs_runtime,
                            &mut block_interaction_requests,
                            BlockInteractionKind::Break,
                        );
                    }
                },
                WindowEvent::MouseInput {
                    button: MouseButton::Right,
                    state: ElementState::Pressed,
                    ..
                } => {
                    if ecs_runtime.player_is_dead() {
                        return;
                    }
                    if inventory_open {
                        let (sw, sh) = renderer.screen_size();
                        if let Some(slot) =
                            hit_test_slot(mouse_screen_pos[0], mouse_screen_pos[1], sw, sh)
                        {
                            inventory_commands.push_back(InventoryCommand::ClickSlot {
                                slot,
                                button: MouseButton::Right,
                            });
                        } else {
                            inventory_commands.push_back(InventoryCommand::ClickOutside {
                                button: MouseButton::Right,
                            });
                        }
                        hud_dirty = true;
                        return;
                    }
                    if !mouse_captured {
                        return;
                    }
                    first_person_hand.trigger_swing();
                    first_person_hand.trigger_item_used();
                    enqueue_block_interaction_request(
                        &mut ecs_runtime,
                        &mut block_interaction_requests,
                        BlockInteractionKind::Place,
                    );
                },
                WindowEvent::MouseWheel { delta, .. } => {
                    if inventory_open || ecs_runtime.player_is_dead() {
                        return;
                    }
                    let amount = match delta {
                        winit::event::MouseScrollDelta::LineDelta(_, y) => y as i32,
                        winit::event::MouseScrollDelta::PixelDelta(p) => p.y as i32,
                    };
                    if amount != 0 {
                        inventory_commands.push_back(InventoryCommand::ScrollHotbar {
                            delta: amount.signum() as i8,
                        });
                        hud_dirty = true;
                    }
                }
                WindowEvent::CursorMoved { position, .. } => {
                    mouse_screen_pos = [position.x as f32, position.y as f32];
                    if inventory_open {
                        hud_dirty = true;
                    }
                }
                _ => {}
            },
            Event::DeviceEvent {
                event: DeviceEvent::MouseMotion { delta },
                ..
            } if mouse_captured && !inventory_open => {
                if ecs_runtime.player_is_dead() {
                    return;
                }
                ecs_runtime.add_mouse_delta(delta.0, delta.1);
            },
            Event::AboutToWait => {
                window.request_redraw();
            },
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

fn alpha_view_bob_matrix(snapshot: &crate::ecs::CameraSnapshot, render_alpha: f32) -> Mat4 {
    if snapshot.fly_mode {
        return Mat4::IDENTITY;
    }

    let walk_delta = snapshot.walk_bob.walk_distance - snapshot.walk_bob.last_walk_distance;
    let walk = snapshot.walk_bob.walk_distance + walk_delta * render_alpha;
    let bob = snapshot.walk_bob.last_bob
        + (snapshot.walk_bob.bob - snapshot.walk_bob.last_bob) * render_alpha;
    let tilt = snapshot.walk_bob.last_tilt
        + (snapshot.walk_bob.tilt - snapshot.walk_bob.last_tilt) * render_alpha;

    let sin_walk = (walk * std::f32::consts::PI).sin();
    let cos_walk = (walk * std::f32::consts::PI).cos();

    let translate = Mat4::from_translation(Vec3::new(
        sin_walk * bob * 0.5,
        -(cos_walk.abs() * bob),
        0.0,
    ));
    let roll = Mat4::from_rotation_z((sin_walk * bob * 3.0).to_radians());
    let pitch = Mat4::from_rotation_x(
        (((walk * std::f32::consts::PI + 0.2).cos().abs()) * bob * 5.0).to_radians(),
    );
    let tilt_rot = Mat4::from_rotation_x(tilt.to_radians());

    tilt_rot * pitch * roll * translate
}

fn alpha_hurt_cam_matrix(snapshot: &crate::ecs::CameraSnapshot, render_alpha: f32) -> Mat4 {
    let mut matrix = Mat4::IDENTITY;
    if snapshot.vitals.health <= 0 {
        let death = snapshot.vitals.death_ticks as f32 + render_alpha;
        let rot_z = 40.0 - 8000.0 / (death + 200.0);
        matrix = Mat4::from_rotation_z(rot_z.to_radians()) * matrix;
    }

    let damaged = snapshot.vitals.damaged_timer as f32 - render_alpha;
    if damaged < 0.0 || snapshot.vitals.damaged_time <= 0 {
        return matrix;
    }

    let phase = (damaged / snapshot.vitals.damaged_time as f32).powi(4) * std::f32::consts::PI;
    let swing = phase.sin() * 14.0;
    let yaw = snapshot.vitals.damaged_swing_dir;
    let hurt = Mat4::from_rotation_y((-yaw).to_radians())
        * Mat4::from_rotation_z((-swing).to_radians())
        * Mat4::from_rotation_y(yaw.to_radians());
    hurt * matrix
}

fn alpha_camera_fov_degrees(snapshot: &crate::ecs::CameraSnapshot, render_alpha: f32) -> f32 {
    let mut fov = if snapshot.vitals.submerged_in_water {
        60.0
    } else {
        70.0
    };
    if snapshot.vitals.health <= 0 {
        let death = snapshot.vitals.death_ticks as f32 + render_alpha;
        fov /= (1.0 - 500.0 / (death + 500.0)) * 2.0 + 1.0;
    }
    fov
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
    if snapshot.vitals.health <= 0 {
        return;
    }

    let direction =
        direction_from_angles64(snapshot.authoritative.yaw, snapshot.authoritative.pitch);
    if direction.length_squared() == 0.0 {
        return;
    }

    // In physics mode, position is at feet. Raycast from eye height.
    let mut origin = snapshot.authoritative.position;
    if !snapshot.fly_mode {
        origin.y += snapshot.physics.eye_height - snapshot.physics.render_eye_height_sneak_offset;
    }

    queue.push_back(BlockInteractionRequest {
        origin,
        direction,
        kind,
    });
}

/// Process all queued block interaction requests. Returns a list of (block_id, position)
/// pairs for items that should be spawned as dropped item entities.
fn process_block_interaction_requests(
    chunk_streamer: &mut ChunkStreamer,
    queue: &mut VecDeque<BlockInteractionRequest>,
    edit_latency_tracker: &mut EditLatencyTracker,
    inventory: &mut PlayerInventoryState,
) -> Vec<(u8, DVec3)> {
    let mut item_spawns = Vec::new();
    while let Some(request) = queue.pop_front() {
        process_single_block_interaction_request(
            chunk_streamer,
            request,
            edit_latency_tracker,
            inventory,
            &mut item_spawns,
        );
    }
    item_spawns
}

fn process_single_block_interaction_request(
    chunk_streamer: &mut ChunkStreamer,
    request: BlockInteractionRequest,
    edit_latency_tracker: &mut EditLatencyTracker,
    inventory: &mut PlayerInventoryState,
    item_spawns: &mut Vec<(u8, DVec3)>,
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
                if let Some(drop_block_id) = chunk_streamer.dropped_item_block_id(broken_block_id) {
                    let spawn_pos = DVec3::new(
                        hit.block[0] as f64 + 0.5,
                        hit.block[1] as f64 + 0.5,
                        hit.block[2] as f64 + 0.5,
                    );
                    item_spawns.push((drop_block_id, spawn_pos));
                }
                edit_latency_tracker.record_block_edit(hit.block[0], hit.block[2]);
            }
        }
        BlockInteractionKind::Place => {
            if hit.normal == [0, 0, 0] {
                return;
            }
            let Some(block_id) = inventory.selected_block_id() else {
                return;
            };
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
                let _ = inventory.apply(InventoryCommand::ConsumeSelected { amount: 1 });
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
        RenderMeshUpdate::UpsertSection {
            pos,
            section_y,
            mesh,
            transparent_mesh,
        } => {
            renderer.upsert_chunk_section_mesh(pos, section_y, &mesh);
            renderer.upsert_chunk_section_transparent_mesh(pos, section_y, &transparent_mesh);
            true
        }
        RenderMeshUpdate::RemoveChunk { pos } => {
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
    if let Some(parsed) = parse_env_u32("THINGCRAFT_UPLOAD_BUDGET") {
        config.max_render_upload_sections_per_tick = parsed as usize;
    }
    if let Some(parsed) = parse_env_u32("THINGCRAFT_GEN_WORKERS") {
        config.generation_workers = parsed as usize;
    }
    if let Some(parsed) = parse_env_u32("THINGCRAFT_LIGHT_WORKERS") {
        config.lighting_workers = parsed as usize;
    }
    if let Some(parsed) = parse_env_u32("THINGCRAFT_MESH_WORKERS") {
        config.meshing_workers = parsed as usize;
    }
    config
}

fn resolve_fluid_tick_budget() -> usize {
    parse_env_u32("THINGCRAFT_FLUID_BUDGET")
        .map_or(FLUID_TICK_BUDGET_DEFAULT, |value| value as usize)
        .clamp(1, FLUID_TICK_BUDGET_HARD_MAX)
}

fn resolve_fancy_graphics() -> bool {
    parse_env_bool("THINGCRAFT_FANCY_GRAPHICS").unwrap_or(true)
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

fn parse_env_bool(key: &str) -> Option<bool> {
    let raw = std::env::var(key).ok()?;
    match raw.trim().to_ascii_lowercase().as_str() {
        "1" | "true" | "yes" | "on" => Some(true),
        "0" | "false" | "no" | "off" => Some(false),
        _ => {
            warn!(env = key, value = %raw, "invalid bool env override; using default");
            None
        }
    }
}

fn alpha_time_of_day(time_ticks: u64, tick_delta: f32) -> f32 {
    let day_tick = (time_ticks % DAY_TICKS) as f32;
    let mut f = (day_tick + tick_delta) / DAY_TICKS as f32 - 0.25;
    if f < 0.0 {
        f += 1.0;
    }
    if f > 1.0 {
        f -= 1.0;
    }
    let g = f;
    f = 1.0 - (((f as f64 * std::f64::consts::PI).cos() + 1.0) as f32 / 2.0);
    g + (f - g) / 3.0
}

fn alpha_ambient_darkness(time_of_day: f32) -> u8 {
    let mut g = 1.0 - ((time_of_day * std::f32::consts::TAU).cos() * 2.0 + 0.5);
    g = g.clamp(0.0, 1.0);
    (g * 11.0) as u8
}

fn alpha_fog_color(time_of_day: f32) -> [f32; 3] {
    let mut f = (time_of_day * std::f32::consts::TAU).cos() * 2.0 + 0.5;
    f = f.clamp(0.0, 1.0);
    [
        0.752_941_2 * (f * 0.94 + 0.06),
        0.847_058_83 * (f * 0.94 + 0.06),
        1.0 * (f * 0.91 + 0.09),
    ]
}

fn alpha_cloud_color(time_of_day: f32) -> [f32; 3] {
    let mut daylight = (time_of_day * std::f32::consts::TAU).cos() * 2.0 + 0.5;
    daylight = daylight.clamp(0.0, 1.0);
    [
        daylight * 0.9 + 0.1,
        daylight * 0.9 + 0.1,
        daylight * 0.85 + 0.15,
    ]
}

fn alpha_sunrise_color(time_of_day: f32) -> Option<[f32; 4]> {
    let window = 0.4_f32;
    let g = (time_of_day * std::f32::consts::TAU).cos();
    if g < -window || g > window {
        return None;
    }

    let i = (g / window) * 0.5 + 0.5;
    let mut j = 1.0 - (1.0 - (i * std::f32::consts::PI).sin()) * 0.99;
    j *= j;
    Some([i * 0.3 + 0.7, i * i * 0.7 + 0.2, 0.2, j])
}

fn alpha_star_brightness(time_of_day: f32) -> f32 {
    let mut factor = 1.0 - ((time_of_day * std::f32::consts::TAU).cos() * 2.0 + 0.75);
    factor = factor.clamp(0.0, 1.0);
    factor * factor * 0.5
}

fn alpha_view_distance_setting(view_radius: i32) -> u8 {
    if view_radius >= 10 {
        0 // far
    } else if view_radius >= 5 {
        1 // normal
    } else if view_radius >= 3 {
        2 // short
    } else {
        3 // tiny
    }
}

fn alpha_render_distance_blocks(view_distance_setting: u8) -> f32 {
    (256_u32 >> view_distance_setting.min(3)) as f32
}

fn alpha_clear_fog_color(
    fog_color: [f32; 3],
    sky_color: [f32; 3],
    view_distance_setting: u8,
) -> [f32; 3] {
    let vd = view_distance_setting.min(3) as f32;
    let sky_mix = 1.0 - (1.0 / (4.0 - vd)).powf(0.25);
    [
        fog_color[0] + (sky_color[0] - fog_color[0]) * sky_mix,
        fog_color[1] + (sky_color[1] - fog_color[1]) * sky_mix,
        fog_color[2] + (sky_color[2] - fog_color[2]) * sky_mix,
    ]
}

fn alpha_apply_fog_brightness(fog_color: [f32; 3], brightness: f32) -> [f32; 3] {
    let clamped = brightness.clamp(0.0, 1.0);
    [
        fog_color[0] * clamped,
        fog_color[1] * clamped,
        fog_color[2] * clamped,
    ]
}

fn alpha_brightness_from_light_level(light_level: u8) -> f32 {
    let clamped = f32::from(light_level.min(15));
    let g = 1.0 - clamped / 15.0;
    ((1.0 - g) / (g * 3.0 + 1.0)) * 0.95 + 0.05
}

fn alpha_fog_brightness_target(
    chunk_streamer: &ChunkStreamer,
    player_pos: DVec3,
    ambient_darkness: u8,
    view_distance_setting: u8,
) -> f32 {
    let world_x = player_pos.x.floor() as i32;
    let world_y = player_pos.y.floor() as i32;
    let world_z = player_pos.z.floor() as i32;
    let sky_default = 15_u8.saturating_sub(ambient_darkness.min(15));
    let light_level = chunk_streamer
        .effective_light_at_world(world_x, world_y, world_z, ambient_darkness)
        .unwrap_or(sky_default);
    let brightness = alpha_brightness_from_light_level(light_level);
    let distance_bias = (3.0 - f32::from(view_distance_setting.min(3))) / 3.0;
    brightness * (1.0 - distance_bias) + distance_bias
}

fn alpha_first_person_brightness(
    chunk_streamer: &ChunkStreamer,
    player_pos: Vec3,
    ambient_darkness: u8,
) -> f32 {
    let world_x = player_pos.x.floor() as i32;
    let world_y = player_pos.y.floor() as i32;
    let world_z = player_pos.z.floor() as i32;
    let sky_default = 15_u8.saturating_sub(ambient_darkness.min(15));
    let light_level = chunk_streamer
        .effective_light_at_world(world_x, world_y, world_z, ambient_darkness)
        .unwrap_or(sky_default);
    alpha_brightness_from_light_level(light_level)
}

fn build_first_person_item_mesh(
    held_block: Option<u8>,
    registry: &BlockRegistry,
    equip_progress: f32,
    attack_progress: f32,
) -> ChunkMesh {
    let Some(block_id) = held_block else {
        return ChunkMesh::default();
    };
    if !registry.is_defined_block(block_id) {
        return ChunkMesh::default();
    }

    let mut mesh = ChunkMesh::default();
    let model = alpha_first_person_item_transform(equip_progress, attack_progress);
    let cube_faces = [
        (
            [0, -1, 0],
            [
                [-0.5, -0.5, -0.5],
                [0.5, -0.5, -0.5],
                [0.5, -0.5, 0.5],
                [-0.5, -0.5, 0.5],
            ],
            0.5_f32,
        ),
        (
            [0, 1, 0],
            [
                [-0.5, 0.5, 0.5],
                [0.5, 0.5, 0.5],
                [0.5, 0.5, -0.5],
                [-0.5, 0.5, -0.5],
            ],
            1.0_f32,
        ),
        (
            [0, 0, -1],
            [
                [-0.5, -0.5, -0.5],
                [-0.5, 0.5, -0.5],
                [0.5, 0.5, -0.5],
                [0.5, -0.5, -0.5],
            ],
            0.8_f32,
        ),
        (
            [0, 0, 1],
            [
                [0.5, -0.5, 0.5],
                [0.5, 0.5, 0.5],
                [-0.5, 0.5, 0.5],
                [-0.5, -0.5, 0.5],
            ],
            0.8_f32,
        ),
        (
            [-1, 0, 0],
            [
                [-0.5, -0.5, 0.5],
                [-0.5, 0.5, 0.5],
                [-0.5, 0.5, -0.5],
                [-0.5, -0.5, -0.5],
            ],
            0.6_f32,
        ),
        (
            [1, 0, 0],
            [
                [0.5, -0.5, -0.5],
                [0.5, 0.5, -0.5],
                [0.5, 0.5, 0.5],
                [0.5, -0.5, 0.5],
            ],
            0.6_f32,
        ),
    ];

    for (face, corners, shade) in cube_faces {
        let sprite = registry.sprite_index_for_face(block_id, face);
        let uv = terrain_sprite_uv(sprite);
        let shade_u8 = (shade * 255.0).round() as u8;
        push_first_person_quad(
            &mut mesh,
            &model,
            corners,
            uv,
            [shade_u8, shade_u8, shade_u8, 255],
        );
    }

    mesh
}

fn build_first_person_arm_mesh(
    render_arm: bool,
    equip_progress: f32,
    attack_progress: f32,
) -> ChunkMesh {
    if !render_arm {
        return ChunkMesh::default();
    }

    let mut mesh = ChunkMesh::default();
    let model = alpha_first_person_arm_transform(equip_progress, attack_progress);

    // Matches Alpha HumanoidModel rightArm: addBox(-3,-2,-2, 4,12,4) then setPos(-5,2,0).
    let v = [
        [-8.0, 0.0, -2.0],
        [-4.0, 0.0, -2.0],
        [-4.0, 12.0, -2.0],
        [-8.0, 12.0, -2.0],
        [-8.0, 0.0, 2.0],
        [-4.0, 0.0, 2.0],
        [-4.0, 12.0, 2.0],
        [-8.0, 12.0, 2.0],
    ]
    .map(|p| [p[0] / 16.0, p[1] / 16.0, p[2] / 16.0]);
    let faces = [
        ([5, 1, 2, 6], skin_uv_rect(48.0, 20.0, 52.0, 32.0)),
        ([0, 4, 7, 3], skin_uv_rect(40.0, 20.0, 44.0, 32.0)),
        ([5, 4, 0, 1], skin_uv_rect(44.0, 16.0, 48.0, 20.0)),
        ([2, 3, 7, 6], skin_uv_rect(48.0, 16.0, 52.0, 20.0)),
        ([1, 0, 3, 2], skin_uv_rect(44.0, 20.0, 48.0, 32.0)),
        ([4, 5, 6, 7], skin_uv_rect(52.0, 20.0, 56.0, 32.0)),
    ];
    for (idx, uv) in faces {
        let corners = [v[idx[0]], v[idx[1]], v[idx[2]], v[idx[3]]];
        push_first_person_quad(&mut mesh, &model, corners, uv, [255, 255, 255, 255]);
    }

    mesh
}

fn alpha_first_person_item_transform(equip_progress: f32, attack_progress: f32) -> Mat4 {
    let attack = attack_progress.clamp(0.0, 1.0);
    let attack_sqrt = attack.sqrt();
    let swing_sin = (attack * std::f32::consts::PI).sin();
    let swing_sin_sqrt = (attack_sqrt * std::f32::consts::PI).sin();
    let swing_sin_sqrt_twice = (attack_sqrt * std::f32::consts::TAU).sin();
    let swing_sin_sq = (attack * attack * std::f32::consts::PI).sin();
    let equip = equip_progress.clamp(0.0, 1.0);
    let h = 0.8_f32;

    Mat4::from_translation(Vec3::new(
        -swing_sin_sqrt * 0.4,
        swing_sin_sqrt_twice * 0.2,
        -swing_sin * 0.2,
    )) * Mat4::from_translation(Vec3::new(
        0.7 * h,
        -0.65 * h - (1.0 - equip) * 0.6,
        -0.9 * h,
    )) * Mat4::from_rotation_y(45.0_f32.to_radians())
        * Mat4::from_rotation_y((-swing_sin_sq * 20.0).to_radians())
        * Mat4::from_rotation_z((-swing_sin_sqrt * 20.0).to_radians())
        * Mat4::from_rotation_x((-swing_sin_sqrt * 80.0).to_radians())
        * Mat4::from_scale(Vec3::splat(0.4))
}

fn alpha_first_person_arm_transform(equip_progress: f32, attack_progress: f32) -> Mat4 {
    let attack = attack_progress.clamp(0.0, 1.0);
    let attack_sqrt = attack.sqrt();
    let swing_sin = (attack * std::f32::consts::PI).sin();
    let swing_sin_sq = (attack * attack * std::f32::consts::PI).sin();
    let swing_sin_sqrt = (attack_sqrt * std::f32::consts::PI).sin();
    let swing_sin_sqrt_twice = (attack_sqrt * std::f32::consts::TAU).sin();
    let equip = equip_progress.clamp(0.0, 1.0);
    let i = 0.8_f32;

    Mat4::from_translation(Vec3::new(
        -swing_sin_sqrt * 0.3,
        swing_sin_sqrt_twice * 0.4,
        -swing_sin * 0.4,
    )) * Mat4::from_translation(Vec3::new(
        0.8 * i,
        -0.75 * i - (1.0 - equip) * 0.6,
        -0.9 * i,
    )) * Mat4::from_rotation_y(45.0_f32.to_radians())
        * Mat4::from_rotation_y((swing_sin_sqrt * 70.0).to_radians())
        * Mat4::from_rotation_z((-swing_sin_sq * 20.0).to_radians())
        * Mat4::from_translation(Vec3::new(-1.0, 3.6, 3.5))
        * Mat4::from_rotation_z(120.0_f32.to_radians())
        * Mat4::from_rotation_x(200.0_f32.to_radians())
        * Mat4::from_rotation_y(-135.0_f32.to_radians())
        * Mat4::from_translation(Vec3::new(5.6, 0.0, 0.0))
}

fn terrain_sprite_uv(sprite: u16) -> [[f32; 2]; 4] {
    let texel = 1.0 / 256.0;
    let u0 = (sprite % 16) as f32 * 16.0 * texel;
    let v0 = (sprite / 16) as f32 * 16.0 * texel;
    let u1 = u0 + 16.0 * texel;
    let v1 = v0 + 16.0 * texel;
    [[u0, v1], [u1, v1], [u1, v0], [u0, v0]]
}

fn skin_uv_rect(u0: f32, v0: f32, u1: f32, v1: f32) -> [[f32; 2]; 4] {
    // Matches Polygon(u1,v1,u2,v2) mapping in Alpha's ModelPart (64x32 skin atlas).
    [
        [u1 / 64.0, v0 / 32.0],
        [u0 / 64.0, v0 / 32.0],
        [u0 / 64.0, v1 / 32.0],
        [u1 / 64.0, v1 / 32.0],
    ]
}

fn push_first_person_quad(
    mesh: &mut ChunkMesh,
    transform: &Mat4,
    corners: [[f32; 3]; 4],
    uv: [[f32; 2]; 4],
    tint_rgba: [u8; 4],
) {
    let base = mesh.vertices.len() as u32;
    for i in 0..4 {
        let p = transform.transform_point3(Vec3::from_array(corners[i]));
        mesh.vertices.push(MeshVertex {
            position: p.to_array(),
            uv: uv[i],
            tint_rgba,
            light_data: [0, 0, 0, 0],
        });
    }
    mesh.indices
        .extend_from_slice(&[base, base + 1, base + 2, base + 2, base + 3, base]);
}

fn alpha_sky_color(time_of_day: f32, temperature: f32) -> [f32; 3] {
    let mut daylight = (time_of_day * std::f32::consts::TAU).cos() * 2.0 + 0.5;
    daylight = daylight.clamp(0.0, 1.0);
    let [r, g, b] = alpha_biome_sky_rgb(temperature);
    [r * daylight, g * daylight, b * daylight]
}

fn alpha_biome_sky_rgb(temperature: f32) -> [f32; 3] {
    let temp = (temperature / 3.0).clamp(-1.0, 1.0);
    let hue = 0.622_222_24 - temp * 0.05;
    let saturation = 0.5 + temp * 0.1;
    hsv_to_rgb(hue, saturation, 1.0)
}

fn hsv_to_rgb(h: f32, s: f32, v: f32) -> [f32; 3] {
    if s <= 0.0 {
        return [v, v, v];
    }

    let mut hue = h % 1.0;
    if hue < 0.0 {
        hue += 1.0;
    }
    let sector = (hue * 6.0).floor();
    let frac = hue * 6.0 - sector;
    let p = v * (1.0 - s);
    let q = v * (1.0 - s * frac);
    let t = v * (1.0 - s * (1.0 - frac));
    match sector as i32 {
        0 => [v, t, p],
        1 => [q, v, p],
        2 => [p, v, t],
        3 => [p, q, v],
        4 => [t, p, v],
        _ => [v, p, q],
    }
}

fn tick_player_survival(
    world: &mut bevy_ecs::world::World,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    sim_ticks: u64,
) {
    let mut query = world.query_filtered::<(
        &mut crate::ecs::Transform64,
        &mut crate::ecs::PhysicsBody,
        &mut crate::ecs::PlayerVitals,
    ), bevy_ecs::query::With<crate::ecs::Player>>();
    let Some((transform, physics, mut vitals)) = query.iter_mut(world).next() else {
        return;
    };

    if vitals.health <= 0 {
        vitals.death_ticks += 1;
        if vitals.death_ticks > 20 {
            vitals.dead_ready = true;
        }
        return;
    }

    let half_w = physics.width / 2.0;
    let min_x = (transform.position.x - half_w).floor() as i32;
    let max_x = (transform.position.x + half_w).floor() as i32;
    let min_y = transform.position.y.floor() as i32;
    let max_y = (transform.position.y + physics.height).floor() as i32;
    let min_z = (transform.position.z - half_w).floor() as i32;
    let max_z = (transform.position.z + half_w).floor() as i32;

    let mut in_lava = false;
    let mut in_fire = false;
    for x in min_x..=max_x {
        for y in min_y..=max_y {
            if !(0..CHUNK_HEIGHT as i32).contains(&y) {
                continue;
            }
            for z in min_z..=max_z {
                let Some(block_id) = chunk_streamer.block_at_world(x, y, z) else {
                    continue;
                };
                in_lava |= registry.is_lava(block_id);
                in_fire |= registry
                    .get(block_id)
                    .is_some_and(|b| b.material == MaterialKind::Fire);
            }
        }
    }

    // Approximate Alpha eye-level submersion check.
    let eye_x = transform.position.x.floor() as i32;
    let eye_y = (transform.position.y + physics.eye_height - physics.render_eye_height_sneak_offset)
        .floor() as i32;
    let eye_z = transform.position.z.floor() as i32;
    let submerged_in_water = chunk_streamer
        .block_at_world(eye_x, eye_y, eye_z)
        .is_some_and(|block_id| registry.is_water(block_id));
    vitals.submerged_in_water = submerged_in_water;

    if submerged_in_water {
        vitals.breath -= 1;
        if vitals.breath == -20 {
            vitals.breath = 0;
            let _ = vitals.apply_damage(2);
        }
        vitals.on_fire_timer = 0;
    } else {
        vitals.breath = vitals.breath_capacity;
    }

    if in_lava {
        let _ = vitals.apply_damage(4);
        vitals.on_fire_timer = 600;
    }

    if in_fire && !submerged_in_water {
        vitals.on_fire_timer = vitals.on_fire_timer.max(300);
    }

    if vitals.on_fire_timer > 0 {
        if sim_ticks.is_multiple_of(20) {
            let _ = vitals.apply_damage(1);
        }
        vitals.on_fire_timer -= 1;
    }
}

fn try_respawn_player(ecs_runtime: &mut EcsRuntime) -> bool {
    let world = ecs_runtime.world_mut();
    let mut query = world.query_filtered::<(
        &mut crate::ecs::Transform64,
        &mut crate::ecs::PhysicsBody,
        &mut crate::ecs::FlyCamera,
        &mut crate::ecs::PlayerVitals,
    ), bevy_ecs::query::With<crate::ecs::Player>>();
    let Some((mut transform, mut physics, mut fly_camera, mut vitals)) =
        query.iter_mut(world).next()
    else {
        return false;
    };
    if !vitals.dead_ready {
        return false;
    }

    vitals.health = 20;
    vitals.prev_health = 20;
    vitals.invulnerable_timer = 0;
    vitals.prev_damage_taken = 0;
    vitals.damaged_time = 0;
    vitals.damaged_timer = 0;
    vitals.damaged_swing_dir = 0.0;
    vitals.breath = vitals.breath_capacity;
    vitals.on_fire_timer = 0;
    vitals.death_ticks = 0;
    vitals.dead_ready = false;

    transform.position = DVec3::new(0.0, 72.0, 0.0);
    transform.prev_position = transform.position;
    fly_camera.fly_mode = false;
    physics.velocity = DVec3::ZERO;
    physics.on_ground = false;
    physics.fall_distance = 0.0;
    ecs_runtime.clear_input_state();
    true
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
    let in_water = player_body_touches_material(
        transform.prev_position,
        physics.width,
        physics.height,
        chunk_streamer,
        |block_id| registry.is_water(block_id),
    );
    let in_lava = !in_water
        && player_body_touches_material(
            transform.prev_position,
            physics.width,
            physics.height,
            chunk_streamer,
            |block_id| registry.is_lava(block_id),
        );

    // The position that apply_player_motion_system already wrote includes velocity.
    // We need to resolve collisions against the world.
    let target_pos = transform.position;
    // Reconstruct pre-move position from prev_position (which was saved before velocity applied).
    let start_pos = transform.prev_position;
    let mut delta = target_pos - start_pos;
    if physics.on_ground && physics.sneaking {
        delta = clamp_sneak_edge_delta(
            start_pos,
            delta,
            physics.width,
            physics.height,
            chunk_streamer,
            registry,
        );
    }

    let mut pos = start_pos;
    let original_delta = delta;

    // Reusable scratch buffer for collecting block AABBs (avoids allocation per axis).
    let mut scratch = Vec::with_capacity(32);

    // Y axis first (gravity is the most important axis to resolve).
    let expanded_min_y = DVec3::new(pos.x - half_w, pos.y.min(pos.y + delta.y), pos.z - half_w);
    let expanded_max_y = DVec3::new(
        pos.x + half_w,
        (pos.y + height).max(pos.y + height + delta.y),
        pos.z + half_w,
    );
    collect_solid_block_aabbs(
        &mut scratch,
        chunk_streamer,
        registry,
        expanded_min_y,
        expanded_max_y,
    );
    delta.y = resolve_axis(pos, half_w, height, delta.y, &scratch, 1);
    pos.y += delta.y;

    // X axis.
    let expanded_min_x = DVec3::new(pos.x.min(pos.x + delta.x) - half_w, pos.y, pos.z - half_w);
    let expanded_max_x = DVec3::new(
        pos.x.max(pos.x + delta.x) + half_w,
        pos.y + height,
        pos.z + half_w,
    );
    collect_solid_block_aabbs(
        &mut scratch,
        chunk_streamer,
        registry,
        expanded_min_x,
        expanded_max_x,
    );
    delta.x = resolve_axis(pos, half_w, height, delta.x, &scratch, 0);
    pos.x += delta.x;

    // Z axis.
    let expanded_min_z = DVec3::new(pos.x - half_w, pos.y, pos.z.min(pos.z + delta.z) - half_w);
    let expanded_max_z = DVec3::new(
        pos.x + half_w,
        pos.y + height,
        pos.z.max(pos.z + delta.z) + half_w,
    );
    collect_solid_block_aabbs(
        &mut scratch,
        chunk_streamer,
        registry,
        expanded_min_z,
        expanded_max_z,
    );
    delta.z = resolve_axis(pos, half_w, height, delta.z, &scratch, 2);
    pos.z += delta.z;

    // Step-up: if horizontal movement was blocked and on_ground, try stepping up.
    let h_blocked =
        (original_delta.x - delta.x).abs() > 1e-10 || (original_delta.z - delta.z).abs() > 1e-10;
    let was_on_ground = physics.on_ground;
    if h_blocked && was_on_ground && physics.eye_height_sneak_offset < 0.05 {
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
    let in_water_at_end = player_body_touches_material(
        pos,
        physics.width,
        physics.height,
        chunk_streamer,
        |block_id| registry.is_water(block_id),
    );
    let in_lava_at_end = player_body_touches_material(
        pos,
        physics.width,
        physics.height,
        chunk_streamer,
        |block_id| registry.is_lava(block_id),
    );
    let in_fluid_at_end = in_water_at_end || in_lava_at_end;

    // Fall distance tracking.
    let mut fall_distance = physics.fall_distance;
    if delta.y < 0.0 {
        fall_distance -= delta.y;
    } else {
        fall_distance = 0.0;
    }
    if in_fluid_at_end {
        fall_distance = 0.0;
    }
    let mut damage_taken = 0;
    if on_ground {
        if fall_distance > 3.0 {
            damage_taken = (fall_distance - 3.0).ceil() as i32;
        }
        fall_distance = 0.0;
    }
    if in_fluid_at_end {
        damage_taken = 0;
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

    let colliding_horizontally =
        (original_delta.x - delta.x).abs() > 1e-10 || (original_delta.z - delta.z).abs() > 1e-10;
    let fluid_jump_assist = if in_water || in_lava {
        let drag = if in_water { 0.8 } else { 0.5 };
        let mut test_velocity = velocity;
        test_velocity.x *= drag;
        test_velocity.y *= drag;
        test_velocity.z *= drag;
        test_velocity.y -= 0.02;
        colliding_horizontally
            && can_move_player(
                pos,
                physics.width,
                physics.height,
                DVec3::new(
                    test_velocity.x,
                    test_velocity.y + 0.6 - (pos.y - start_pos.y),
                    test_velocity.z,
                ),
                chunk_streamer,
                registry,
            )
    } else {
        false
    };

    ecs_runtime.apply_resolved_physics(
        pos,
        velocity,
        on_ground,
        fall_distance,
        damage_taken,
        in_water,
        in_lava,
        fluid_jump_assist,
    );
}

/// Collect AABBs of all solid blocks overlapping the given region into `out`.
/// Clears `out` before filling — reuse across calls to avoid allocation.
pub(crate) fn collect_solid_block_aabbs(
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
                    if registry.is_collidable(block_id) {
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

fn player_body_touches_material(
    position: DVec3,
    width: f64,
    height: f64,
    chunk_streamer: &ChunkStreamer,
    is_material: impl Fn(u8) -> bool,
) -> bool {
    let half_w = width / 2.0;
    let min_x = (position.x - half_w).floor() as i32;
    let max_x = (position.x + half_w).floor() as i32;
    let min_y = position.y.floor() as i32;
    let max_y = (position.y + height).floor() as i32;
    let min_z = (position.z - half_w).floor() as i32;
    let max_z = (position.z + half_w).floor() as i32;

    for x in min_x..=max_x {
        for y in min_y..=max_y {
            if !(0..CHUNK_HEIGHT as i32).contains(&y) {
                continue;
            }
            for z in min_z..=max_z {
                let Some(block_id) = chunk_streamer.block_at_world(x, y, z) else {
                    continue;
                };
                if is_material(block_id) {
                    return true;
                }
            }
        }
    }
    false
}

fn clamp_sneak_edge_delta(
    position: DVec3,
    mut delta: DVec3,
    width: f64,
    height: f64,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
) -> DVec3 {
    let step = 0.05;
    while delta.x.abs() > 1e-10
        && can_move_player(
            position,
            width,
            height,
            DVec3::new(delta.x, -1.0, 0.0),
            chunk_streamer,
            registry,
        )
    {
        if delta.x.abs() < step {
            delta.x = 0.0;
        } else if delta.x > 0.0 {
            delta.x -= step;
        } else {
            delta.x += step;
        }
    }
    while delta.z.abs() > 1e-10
        && can_move_player(
            position,
            width,
            height,
            DVec3::new(0.0, -1.0, delta.z),
            chunk_streamer,
            registry,
        )
    {
        if delta.z.abs() < step {
            delta.z = 0.0;
        } else if delta.z > 0.0 {
            delta.z -= step;
        } else {
            delta.z += step;
        }
    }
    delta
}

fn can_move_player(
    position: DVec3,
    width: f64,
    height: f64,
    offset: DVec3,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
) -> bool {
    let half_w = width / 2.0;
    let moved_pos = position + offset;
    let mut scratch = Vec::with_capacity(32);
    collect_solid_block_aabbs(
        &mut scratch,
        chunk_streamer,
        registry,
        DVec3::new(moved_pos.x - half_w, moved_pos.y, moved_pos.z - half_w),
        DVec3::new(
            moved_pos.x + half_w,
            moved_pos.y + height,
            moved_pos.z + half_w,
        ),
    );
    for [bx0, by0, bz0, bx1, by1, bz1] in &scratch {
        if moved_pos.x + half_w > *bx0
            && moved_pos.x - half_w < *bx1
            && moved_pos.y + height > *by0
            && moved_pos.y < *by1
            && moved_pos.z + half_w > *bz0
            && moved_pos.z - half_w < *bz1
        {
            return false;
        }
    }
    true
}

/// Resolve movement along a single axis against block AABBs. Returns clamped delta.
/// `axis`: 0=X, 1=Y, 2=Z. The perpendicular axes are checked for overlap.
pub(crate) fn resolve_axis(
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
    use std::time::Duration;

    use glam::DVec3;
    use winit::keyboard::KeyCode;

    use super::{
        affected_chunks_for_block_edit, alpha_ambient_darkness, alpha_apply_fog_brightness,
        alpha_brightness_from_light_level, alpha_fog_brightness_target, alpha_star_brightness,
        alpha_sunrise_color, alpha_time_of_day, has_target_directive, hotbar_slot_for_key,
        parse_env_bool, parse_env_u32, raycast_first_solid_block, resolve_axis,
        AdaptiveFluidBudget, BlockRayHit, AIR_BLOCK_ID,
    };
    use crate::streaming::{ChunkStreamer, ResidencyConfig};
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
    fn env_bool_parser_accepts_common_true_false_forms() {
        std::env::set_var("THINGCRAFT_TEST_BOOL", "true");
        assert_eq!(parse_env_bool("THINGCRAFT_TEST_BOOL"), Some(true));
        std::env::set_var("THINGCRAFT_TEST_BOOL", "0");
        assert_eq!(parse_env_bool("THINGCRAFT_TEST_BOOL"), Some(false));
        std::env::set_var("THINGCRAFT_TEST_BOOL", "nope");
        assert_eq!(parse_env_bool("THINGCRAFT_TEST_BOOL"), None);
        std::env::remove_var("THINGCRAFT_TEST_BOOL");
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
        assert!((dx - 0.7).abs() < 1e-10, "expected dx=0.7, got {dx}");
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
        assert!((dz - 0.7).abs() < 1e-10, "expected dz=0.7, got {dz}");
    }

    #[test]
    fn alpha_time_of_day_wraps_into_unit_interval() {
        let t0 = alpha_time_of_day(0, 0.0);
        let t1 = alpha_time_of_day(24_000, 0.0);
        assert!((0.0..=1.0).contains(&t0));
        assert!((0.0..=1.0).contains(&t1));
        assert!((t0 - t1).abs() < 1e-6);
    }

    #[test]
    fn ambient_darkness_stays_within_alpha_bounds() {
        for tick in [0_u64, 6_000, 12_000, 18_000, 23_999] {
            let tod = alpha_time_of_day(tick, 0.0);
            let darkness = alpha_ambient_darkness(tod);
            assert!(darkness <= 11);
        }
    }

    #[test]
    fn star_brightness_matches_alpha_curve_shape() {
        let noon = alpha_star_brightness(alpha_time_of_day(6_000, 0.0));
        let midnight = alpha_star_brightness(alpha_time_of_day(18_000, 0.0));
        assert!(noon < 0.01);
        assert!(midnight > 0.45);
    }

    #[test]
    fn sunrise_color_only_exists_near_horizon_transition() {
        let noon = alpha_sunrise_color(alpha_time_of_day(6_000, 0.0));
        let dawn = alpha_sunrise_color(alpha_time_of_day(0, 0.0));
        assert!(noon.is_none());
        assert!(dawn.is_some());
    }

    #[test]
    fn alpha_brightness_curve_matches_dimension_table_endpoints() {
        assert!((alpha_brightness_from_light_level(0) - 0.05).abs() < 0.0001);
        assert!((alpha_brightness_from_light_level(15) - 1.0).abs() < 0.0001);
    }

    #[test]
    fn fog_color_brightness_multiplier_is_componentwise() {
        let fog = [0.4, 0.2, 1.0];
        let shaded = alpha_apply_fog_brightness(fog, 0.25);
        assert!((shaded[0] - 0.1).abs() < 0.0001);
        assert!((shaded[1] - 0.05).abs() < 0.0001);
        assert!((shaded[2] - 0.25).abs() < 0.0001);
    }

    #[test]
    fn fog_brightness_target_obeys_view_distance_bias() {
        let streamer = ChunkStreamer::new(
            123,
            BlockRegistry::alpha_1_2_6(),
            ResidencyConfig::default(),
        );
        let player_pos = DVec3::new(0.0, 72.0, 0.0);
        let ambient_darkness = 11;
        let far = alpha_fog_brightness_target(&streamer, player_pos, ambient_darkness, 0);
        let tiny = alpha_fog_brightness_target(&streamer, player_pos, ambient_darkness, 3);
        assert!(far > tiny);
        assert!((tiny - alpha_brightness_from_light_level(4)).abs() < 0.0001);
    }

    #[test]
    fn adaptive_fluid_budget_increases_with_headroom_and_saturation() {
        let mut budget = AdaptiveFluidBudget::new(384);
        let before = budget.current();
        budget.observe_frame(Duration::from_millis(10), 1, before, 0);
        assert!(budget.current() > before);
    }

    #[test]
    fn adaptive_fluid_budget_decreases_under_frame_pressure() {
        let mut budget = AdaptiveFluidBudget::new(384);
        let before = budget.current();
        budget.observe_frame(
            Duration::from_millis(24),
            1,
            100,
            before.saturating_sub(100),
        );
        assert!(budget.current() < before);
    }

    #[test]
    fn adaptive_fluid_budget_urgent_slice_is_bounded() {
        let budget = AdaptiveFluidBudget::new(384);
        assert_eq!(budget.urgent_slice_for_tick(0), 0);
        let slice = budget.urgent_slice_for_tick(20);
        assert!(slice >= 16);
        assert!(slice <= 20);
    }
}
