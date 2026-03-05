use std::collections::{HashMap, HashSet, VecDeque};
use std::fs::{self, File};
use std::io::Write;
use std::path::PathBuf;
use std::time::{Duration, Instant};
use std::time::{SystemTime, UNIX_EPOCH};

use anyhow::Result;
use glam::{DVec3, Mat4, Vec3};
use rand::rngs::SmallRng;
use rand::{Rng, SeedableRng};
use tracing::{debug, error, info, warn};
#[cfg(feature = "tracy")]
use tracing_subscriber::layer::SubscriberExt;
#[cfg(feature = "tracy")]
use tracing_subscriber::util::SubscriberInitExt;
use winit::dpi::{PhysicalPosition, PhysicalSize};
use winit::event::{DeviceEvent, ElementState, Event, MouseButton, WindowEvent};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::keyboard::{KeyCode, PhysicalKey};
use winit::window::{CursorGrabMode, Window, WindowBuilder};

use crate::crafting::CraftingRegistry;
use crate::ecs::EcsRuntime;
use crate::gameplay::{
    apply_post_block_break_effects, run_inventory_command_system, InventoryCommandQueue,
    MiningState, MiningTarget, MiningToolKind,
};
use crate::hud;
use crate::inventory::{
    hit_test_slot_for_menu, inventory_layout_for_menu, menu_panel_size, BlockPos,
    ContainerRuntimeState, InventoryCommand, InventoryMenuKind, ItemKey, PlayerInventoryState,
};
use crate::mesh::{build_region_mesh, ChunkMesh, MeshVertex};
use crate::renderer::{RenderError, Renderer};
use crate::streaming::{
    world_block_to_chunk_pos_and_local, world_pos_to_chunk_pos, ChunkStreamer, RenderMeshUpdate,
    ResidencyConfig,
};
use crate::time_step::FixedStepClock;
use crate::tool::ToolRegistry;
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
const FIRE_BLOCK_ID: u8 = 51;
const CHEST_BLOCK_ID: u8 = 54;
const FURNACE_BLOCK_ID: u8 = 61;
const LIT_FURNACE_BLOCK_ID: u8 = 62;
const PUMPKIN_BLOCK_ID: u8 = 86;
const LIT_PUMPKIN_BLOCK_ID: u8 = 91;
const GRASS_BLOCK_ID: u8 = 2;
const STONE_BLOCK_ID: u8 = 1;
const COBBLESTONE_BLOCK_ID: u8 = 4;
const GLASS_BLOCK_ID: u8 = 20;
const BOOKSHELF_BLOCK_ID: u8 = 47;
const MOB_SPAWNER_BLOCK_ID: u8 = 52;
const SAND_BLOCK_ID: u8 = 12;
const DIRT_BLOCK_ID: u8 = 3;
const SAPLING_BLOCK_ID: u8 = 6;
const OAK_LEAVES_BLOCK_ID: u8 = 18;
const FARMLAND_BLOCK_ID: u8 = 60;
const FLOWING_WATER_BLOCK_ID: u8 = 8;
const WATER_BLOCK_ID: u8 = 9;
const COAL_ORE_BLOCK_ID: u8 = 16;
const DIAMOND_ORE_BLOCK_ID: u8 = 56;
const REDSTONE_ORE_BLOCK_ID: u8 = 73;
const LIT_REDSTONE_ORE_BLOCK_ID: u8 = 74;
const GRAVEL_BLOCK_ID: u8 = 13;
const SNOW_LAYER_BLOCK_ID: u8 = 78;
const SNOW_BLOCK_ID: u8 = 80;
const CLAY_BLOCK_ID: u8 = 82;
const GLOWSTONE_BLOCK_ID: u8 = 89;
const YELLOW_FLOWER_BLOCK_ID: u8 = 37;
const RED_FLOWER_BLOCK_ID: u8 = 38;
const BROWN_MUSHROOM_BLOCK_ID: u8 = 39;
const RED_MUSHROOM_BLOCK_ID: u8 = 40;
const CACTUS_BLOCK_ID: u8 = 81;
const SUGAR_CANE_BLOCK_ID: u8 = 83;
const OAK_LOG_BLOCK_ID: u8 = 17;
const ICE_BLOCK_ID: u8 = 79;
const COAL_ITEM_ID: u16 = 263;
const DIAMOND_ITEM_ID: u16 = 264;
const REDSTONE_ITEM_ID: u16 = 331;
const REEDS_ITEM_ID: u16 = 338;
const FLINT_ITEM_ID: u16 = 318;
const CLAY_BALL_ITEM_ID: u16 = 337;
const GLOWSTONE_DUST_ITEM_ID: u16 = 348;
const SNOWBALL_ITEM_ID: u16 = 332;
const RANDOM_TICK_CHUNK_RADIUS: i32 = 9;
const RANDOM_TICKS_PER_CHUNK: usize = 80;
const RANDOM_TICK_LCG_INCREMENT: i32 = 1_013_904_223;
const DEBUG_SIM_SPEEDUP_MULTIPLIER: usize = 8;
const DEBUG_SIM_SPEEDUP_TICK_CAP: usize = 256;
const BENCH_MOVE_ASCEND_BLOCKS: f64 = 20.0;

#[derive(Debug, Clone, Copy)]
struct BlockInteractionRequest {
    origin: DVec3,
    direction: DVec3,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
struct BlockRayHit {
    block: [i32; 3],
    normal: [i32; 3],
}

#[derive(Debug, Default, Clone, Copy)]
struct BlockInteractionEvents {
    open_menu: Option<InventoryMenuKind>,
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

#[derive(Debug, Clone)]
struct BenchConfig {
    enabled: bool,
    still_secs: u32,
    turn_secs: u32,
    move_secs: u32,
    turn_pixels_per_sec: f64,
    output_path: PathBuf,
}

struct BenchRuntime {
    config: BenchConfig,
    start: Instant,
    output: File,
    last_phase: BenchPhase,
    screenshot_run_dir: PathBuf,
    screenshot_latest_dir: PathBuf,
    captured_still: bool,
    captured_turn: bool,
    captured_move: bool,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
enum BenchPhase {
    Still,
    Turn,
    Move,
    Done,
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
    shown_item: Option<ItemKey>,
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
            shown_item: None,
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

    fn tick(&mut self, selected_item: Option<ItemKey>) {
        self.last_hand_height = self.hand_height;
        self.last_attack_progress = self.attack_progress;
        let target = if selected_item == self.shown_item {
            1.0
        } else {
            0.0
        };
        let mut delta = target - self.hand_height;
        delta = delta.clamp(-0.4, 0.4);
        self.hand_height += delta;
        if self.hand_height < 0.1 {
            self.shown_item = selected_item;
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

impl BenchRuntime {
    fn new(config: BenchConfig) -> Result<Self> {
        let screenshot_run_dir = screenshot_run_dir_for_output(&config.output_path);
        let screenshot_latest_dir = screenshot_latest_dir_for_output(&config.output_path);
        if let Some(parent) = config.output_path.parent() {
            if !parent.as_os_str().is_empty() {
                fs::create_dir_all(parent)?;
            }
        }
        let mut output = File::create(&config.output_path)?;
        writeln!(
            output,
            "phase,elapsed_s,fps,tps,avg_frame_ms,avg_tick_ms,resident_chunks,ready_chunks,generating_chunks,meshing_chunks,evicting_chunks,dirty_chunks,visible_chunks"
        )?;
        Ok(Self {
            config,
            start: Instant::now(),
            output,
            last_phase: BenchPhase::Still,
            screenshot_run_dir,
            screenshot_latest_dir,
            captured_still: false,
            captured_turn: false,
            captured_move: false,
        })
    }

    fn elapsed(&self) -> Duration {
        self.start.elapsed()
    }

    fn phase(&self) -> BenchPhase {
        let elapsed = self.elapsed().as_secs_f32();
        let still_end = self.config.still_secs as f32;
        let turn_end = still_end + self.config.turn_secs as f32;
        let move_end = turn_end + self.config.move_secs as f32;
        if elapsed < still_end {
            BenchPhase::Still
        } else if elapsed < turn_end {
            BenchPhase::Turn
        } else if elapsed < move_end {
            BenchPhase::Move
        } else {
            BenchPhase::Done
        }
    }

    fn phase_name(&self) -> &'static str {
        match self.phase() {
            BenchPhase::Still => "still",
            BenchPhase::Turn => "turn",
            BenchPhase::Move => "move",
            BenchPhase::Done => "done",
        }
    }

    fn apply_controls(&mut self, ecs_runtime: &mut EcsRuntime, frame_delta: Duration) {
        let phase = self.phase();
        if phase != self.last_phase {
            if matches!(phase, BenchPhase::Move) {
                ecs_runtime.offset_player_position(DVec3::new(0.0, BENCH_MOVE_ASCEND_BLOCKS, 0.0));
                ecs_runtime.set_look_angles(0.0, 0.0);
            }
            self.last_phase = phase;
        }

        let move_forward = matches!(phase, BenchPhase::Move);
        ecs_runtime.handle_key(KeyCode::KeyW, move_forward);
        if matches!(phase, BenchPhase::Turn) {
            let delta_x = self.config.turn_pixels_per_sec * frame_delta.as_secs_f64();
            ecs_runtime.add_mouse_delta(delta_x, 0.0);
        }
    }

    fn write_sample(
        &mut self,
        report: LoopReport,
        residency: crate::streaming::ResidencyMetrics,
        visible_chunks: usize,
    ) -> Result<()> {
        writeln!(
            self.output,
            "{},{:.3},{:.2},{:.2},{:.3},{:.3},{},{},{},{},{},{},{},",
            self.phase_name(),
            self.elapsed().as_secs_f64(),
            report.fps,
            report.tps,
            report.avg_frame_ms,
            report.avg_tick_ms,
            residency.total,
            residency.ready,
            residency.generating,
            residency.meshing,
            residency.evicting,
            residency.dirty_chunks,
            visible_chunks
        )?;
        Ok(())
    }

    fn take_screenshot_request(&mut self) -> Option<Vec<PathBuf>> {
        let elapsed_s = self.elapsed().as_secs_f32();
        let still_trigger = bench_phase_capture_offset(self.config.still_secs);
        let turn_trigger =
            self.config.still_secs as f32 + bench_phase_capture_offset(self.config.turn_secs);
        let move_trigger = self.config.still_secs as f32
            + self.config.turn_secs as f32
            + bench_phase_capture_offset(self.config.move_secs);

        if !self.captured_still && elapsed_s >= still_trigger {
            self.captured_still = true;
            return Some(self.screenshot_paths_for_label("still"));
        }
        if !self.captured_turn && elapsed_s >= turn_trigger {
            self.captured_turn = true;
            return Some(self.screenshot_paths_for_label("turn"));
        }
        if !self.captured_move && elapsed_s >= move_trigger {
            self.captured_move = true;
            return Some(self.screenshot_paths_for_label("move"));
        }
        None
    }

    fn screenshot_paths_for_label(&self, label: &str) -> Vec<PathBuf> {
        vec![
            self.screenshot_run_dir.join(format!("{label}.png")),
            self.screenshot_latest_dir
                .join(format!("latest_{label}.png")),
        ]
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
    let tracy_enabled = init_tracing();

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
    ecs_runtime
        .world_mut()
        .insert_resource(PlayerInventoryState::alpha_defaults());
    ecs_runtime
        .world_mut()
        .insert_resource(ContainerRuntimeState::default());
    ecs_runtime
        .world_mut()
        .insert_resource(CraftingRegistry::alpha_1_2_6());
    ecs_runtime
        .world_mut()
        .insert_resource(MiningState::default());
    ecs_runtime
        .world_mut()
        .insert_resource(InventoryCommandQueue::default());
    let fixed_config = ecs_runtime.fixed_tick_config();
    let mut fixed_clock = FixedStepClock::new(fixed_config.tick_hz, fixed_config.max_catchup_steps);
    let bootstrap_world = BootstrapWorld::alpha_bootstrap();
    let tool_registry = ToolRegistry::alpha_1_2_6();
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
    let bench_config = resolve_bench_config();
    let mut bench_runtime = if bench_config.enabled {
        Some(BenchRuntime::new(bench_config.clone())?)
    } else {
        None
    };
    let biome_source = BiomeSource::new(WORLD_SEED);
    let alpha_view_distance = alpha_view_distance_setting(residency_config.view_radius);
    let render_distance_blocks = alpha_render_distance_blocks(alpha_view_distance);
    let _ = chunk_streamer.update_target(ChunkPos { x: 0, z: 0 });
    {
        let _span = tracing::info_span!("apply_render_updates_bootstrap").entered();
        for update in chunk_streamer.drain_render_updates() {
            if apply_render_update(&mut renderer, update) && bootstrap_mesh_active {
                renderer.set_scene_mesh(&ChunkMesh::default());
                bootstrap_mesh_active = false;
            }
        }
    }

    let mut last_frame_start = Instant::now();
    let mut loop_stats = LoopStats::default();
    let mut mouse_captured = false;
    let mut left_mouse_held = false;
    let mut mining_swing_hold_ticks: u8 = 0;
    let mut mining_start_requested = false;
    let mut open_menu: Option<InventoryMenuKind> = None;
    let mut mouse_screen_pos = [0.0_f32, 0.0_f32];
    let mut block_interaction_requests = VecDeque::new();
    let mut edit_latency_tracker = EditLatencyTracker::default();
    let mut first_person_hand = FirstPersonHandState::default();
    let mut hud_dirty = true;
    let mut sim_ticks: u64 = 0;
    let mut random_tick_lcg: i32 = (WORLD_SEED as u32 ^ 0x9E37_79B9) as i32;
    let mut random_tick_rng = SmallRng::seed_from_u64(WORLD_SEED ^ 0xA11A_A11A_55AA_F00D);
    let mut sim_speedup_held = false;
    let mut last_fog_brightness = 1.0_f32;
    let mut fog_brightness = 1.0_f32;
    let base_fluid_tick_budget = resolve_fluid_tick_budget();
    let mut adaptive_fluid_budget = AdaptiveFluidBudget::new(base_fluid_tick_budget);
    let selected_slot = ecs_runtime
        .world()
        .resource::<PlayerInventoryState>()
        .selected_stack();
    let selected_hotbar = ecs_runtime
        .world()
        .resource::<PlayerInventoryState>()
        .selected_hotbar;

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
        bench_enabled = bench_config.enabled,
        selected_hotbar_slot = usize::from(selected_hotbar) + 1,
        selected_place_block = selected_slot.and_then(|stack| match stack.item {
            ItemKey::Block(id) if stack.count > 0 => Some(id),
            _ => None,
        }),
        selected_place_count = selected_slot.map_or(0, |stack| stack.count),
        "thingcraft client booted"
    );
    if let Some(bench) = &bench_runtime {
        info!(
            still_secs = bench.config.still_secs,
            turn_secs = bench.config.turn_secs,
            move_secs = bench.config.move_secs,
            turn_px_per_sec = bench.config.turn_pixels_per_sec,
            output = %bench.config.output_path.display(),
            screenshot_run_dir = %bench.screenshot_run_dir.display(),
            screenshot_latest_dir = %bench.screenshot_latest_dir.display(),
            "benchmark mode enabled"
        );
    }

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
                    let _frame_span = tracing::info_span!("frame").entered();
                    let now = Instant::now();
                    let frame_delta = now.saturating_duration_since(last_frame_start);
                    last_frame_start = now;

                    // Inventory screen should not preserve prior movement/look intents.
                    if open_menu.is_some() {
                        ecs_runtime.clear_input_state();
                    }
                    if let Some(bench) = &mut bench_runtime {
                        ecs_runtime.clear_input_state();
                        bench.apply_controls(&mut ecs_runtime, frame_delta);
                    }
                    ecs_runtime.run_input();

                    let tick_timer_start = Instant::now();
                    let base_ticks_to_run = fixed_clock.advance(frame_delta);
                    let ticks_to_run = if sim_speedup_held {
                        base_ticks_to_run
                            .saturating_mul(DEBUG_SIM_SPEEDUP_MULTIPLIER as u32)
                            .min(DEBUG_SIM_SPEEDUP_TICK_CAP as u32)
                    } else {
                        base_ticks_to_run
                    };
                    let fixed_dt_seconds = fixed_clock.tick_dt().as_secs_f64();
                    let fluid_tick_budget = adaptive_fluid_budget.current();
                    let mut fluid_budget_remaining = fluid_tick_budget;
                    let mut fluid_processed_this_frame = 0_usize;
                    let mut urgent_fluid_processed_this_frame = 0_usize;
                    for tick_index in 0..ticks_to_run {
                        let _tick_span = tracing::info_span!("fixed_tick").entered();
                        sim_ticks = sim_ticks.wrapping_add(1);
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

                        let pre_inv = ecs_runtime
                            .world()
                            .resource::<PlayerInventoryState>()
                            .snapshot();

                        // Inventory command fixed-step system: commands -> state change + drop events.
                        let inventory_events =
                            run_inventory_command_system(&mut ecs_runtime, open_menu);
                        if inventory_events.changed {
                            hud_dirty = true;
                        }
                        if !inventory_events.dropped_to_world.is_empty() {
                                let (drop_pos, drop_yaw, drop_pitch) = ecs_runtime
                                    .camera_snapshot()
                                    .map(|snapshot| {
                                        (
                                            snapshot.authoritative.position
                                                + DVec3::new(
                                                    0.0,
                                                    snapshot.physics.eye_height - 0.3,
                                                    0.0,
                                                ),
                                            snapshot.authoritative.yaw,
                                            snapshot.authoritative.pitch,
                                        )
                                    })
                                    .unwrap_or((DVec3::ZERO, 0.0, 0.0));
                                for dropped in inventory_events.dropped_to_world {
                                    crate::entity::spawn_player_dropped_item_stack(
                                        ecs_runtime.world_mut(),
                                        dropped,
                                        drop_pos,
                                        drop_yaw,
                                        drop_pitch,
                                    );
                                }
                        }
                        if tick_container_furnaces(
                            &mut ecs_runtime,
                            &mut chunk_streamer,
                            &mut edit_latency_tracker,
                        ) {
                            hud_dirty = true;
                        }

                        // Item pickup (player walks over dropped items).
                        if crate::entity::check_item_pickup(ecs_runtime.world_mut()) {
                            hud_dirty = true;
                        }

                        if !mouse_captured || open_menu.is_some() || ecs_runtime.player_is_dead() {
                            left_mouse_held = false;
                            mining_swing_hold_ticks = 0;
                            mining_start_requested = false;
                            ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                        }

                        if mining_start_requested {
                            if try_start_block_mining(
                                &mut ecs_runtime,
                                &mut chunk_streamer,
                                &bootstrap_world.registry,
                                &tool_registry,
                                &mut edit_latency_tracker,
                            ) {
                                hud_dirty = true;
                            }
                            mining_start_requested = false;
                        }

                        if left_mouse_held {
                            mining_swing_hold_ticks = mining_swing_hold_ticks.saturating_add(1);
                            if mining_swing_hold_ticks >= 5 {
                                first_person_hand.trigger_swing();
                                mining_swing_hold_ticks = 0;
                            }
                            if tick_block_mining(
                                &mut ecs_runtime,
                                &mut chunk_streamer,
                                &bootstrap_world.registry,
                                &tool_registry,
                                &mut edit_latency_tracker,
                            ) {
                                hud_dirty = true;
                            }
                        } else {
                            mining_swing_hold_ticks = 0;
                            ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                        }

                        // Block interactions (place-first, then block-use/menu fallback).
                        let interaction_events = process_block_interaction_requests(
                            &mut ecs_runtime,
                            &bootstrap_world.registry,
                            &mut chunk_streamer,
                            &mut block_interaction_requests,
                            &mut edit_latency_tracker,
                        );
                        if let Some(menu) = interaction_events.open_menu {
                            match menu {
                                InventoryMenuKind::Chest { .. } => info!("opened chest menu"),
                                InventoryMenuKind::Furnace { .. } => info!("opened furnace menu"),
                                InventoryMenuKind::CraftingTable | InventoryMenuKind::Player => {}
                            }
                            open_menu = Some(menu);
                            mouse_captured = false;
                            left_mouse_held = false;
                            mining_swing_hold_ticks = 0;
                            ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                            set_mouse_capture(window, false);
                            ecs_runtime.clear_input_state();
                            let size = window.inner_size();
                            let center = PhysicalPosition::new(
                                f64::from(size.width) * 0.5,
                                f64::from(size.height) * 0.5,
                            );
                            if let Err(err) = window.set_cursor_position(center) {
                                warn!(?err, "failed to center cursor on menu open");
                            }
                            mouse_screen_pos = [center.x as f32, center.y as f32];
                            hud_dirty = true;
                        }

                        if ecs_runtime
                            .world()
                            .resource::<PlayerInventoryState>()
                            .snapshot()
                            != pre_inv
                        {
                            hud_dirty = true;
                        }
                        let selected_for_hand = ecs_runtime
                            .world()
                            .resource::<PlayerInventoryState>()
                            .selected_item_key();
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

                        if let Some(snapshot) = ecs_runtime.camera_snapshot() {
                            let center_chunk = world_pos_to_chunk_pos(
                                snapshot.authoritative.position.x,
                                snapshot.authoritative.position.z,
                            );
                            if tick_random_blocks(
                                &mut ecs_runtime,
                                &mut chunk_streamer,
                                &bootstrap_world.registry,
                                &mut edit_latency_tracker,
                                center_chunk,
                                &mut random_tick_lcg,
                                &mut random_tick_rng,
                            ) {
                                hud_dirty = true;
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
                        let tick_time_of_day = alpha_time_of_day(sim_ticks, 0.0);
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
                    let time_of_day = alpha_time_of_day(sim_ticks, render_alpha);
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
                        let entity_meshes = crate::entity::build_entity_sprite_mesh(
                            ecs_runtime.world_mut(),
                            snapshot.interpolated.yaw,
                            &bootstrap_world.registry,
                            &tool_registry,
                            &chunk_streamer,
                            ambient_darkness,
                            render_alpha,
                        );
                        renderer.update_entity_sprites(&entity_meshes.terrain, &entity_meshes.items);

                        let shadow_mesh = crate::entity::build_entity_shadow_mesh(
                            ecs_runtime.world_mut(),
                            &chunk_streamer,
                            &bootstrap_world.registry,
                            ambient_darkness,
                            snapshot.interpolated.position.to_array(),
                            render_alpha,
                        );
                        renderer.update_entity_shadows(&shadow_mesh);

                        let submerged_in_lava = alpha_is_eye_submerged_in_material(
                            snapshot.authoritative.position,
                            &snapshot.physics,
                            &chunk_streamer,
                            &bootstrap_world.registry,
                            EyeLiquid::Lava,
                        );
                        let biome_sample = biome_source.sample(
                            snapshot.authoritative.position.x.floor() as i32,
                            snapshot.authoritative.position.z.floor() as i32,
                        );
                        let sky_color = alpha_sky_color(time_of_day, biome_sample.temperature as f32);
                        let mut fog_color = alpha_clear_fog_color(
                            alpha_fog_color(time_of_day),
                            sky_color,
                            alpha_view_distance,
                        );
                        if snapshot.vitals.submerged_in_water {
                            fog_color = [0.02, 0.02, 0.2];
                        } else if submerged_in_lava {
                            fog_color = [0.6, 0.1, 0.0];
                        }
                        fog_color = alpha_apply_fog_brightness(fog_color, frame_fog_brightness);
                        renderer.set_day_night(fog_color, sky_color, ambient_darkness, render_sky);
                        renderer
                            .set_submersion_fog(snapshot.vitals.submerged_in_water, submerged_in_lava);

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
                            &snapshot.physics,
                            ambient_darkness,
                        );
                        renderer.set_first_person_camera(hand_view_proj, hand_brightness);
                        let held_item = first_person_hand.shown_item;
                        let hand_item_mesh = build_first_person_item_mesh(
                            held_item,
                            &bootstrap_world.registry,
                            &tool_registry,
                            first_person_hand.equip_progress(render_alpha),
                            first_person_hand.attack_progress(render_alpha),
                            snapshot.interpolated.yaw,
                            snapshot.interpolated.pitch,
                        );
                        let is_tool = matches!(held_item, Some(ItemKey::Tool(_)));
                        renderer.update_first_person_item_mesh(&hand_item_mesh, is_tool);
                        let hand_arm_mesh = build_first_person_arm_mesh(
                            held_item.is_none(),
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
                            |x, y, z| {
                                let block_id = chunk_streamer.block_at_world(x, y, z)?;
                                let allow_liquids =
                                    selected_item_allows_liquid_targeting_stub(
                                        ecs_runtime.world().resource::<PlayerInventoryState>(),
                                    );
                                let metadata =
                                    chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
                                let can_hit = is_raycast_targetable_block(
                                    &bootstrap_world.registry,
                                    block_id,
                                    metadata,
                                    allow_liquids,
                                );
                                if can_hit { Some(block_id) } else { None }
                            },
                        );
                        renderer.set_block_outline(outline_hit.map(|h| h.block));
                        let mining_state = *ecs_runtime.world().resource::<MiningState>();
                        let crack_target = mining_state.target.map(|t| [t.x, t.y, t.z]);
                        renderer.set_block_crack_overlay(
                            crack_target,
                            mining_state.render_progress(render_alpha),
                        );

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
                        renderer.set_submersion_fog(false, false);
                        renderer.set_block_outline(None);
                        renderer.set_block_crack_overlay(None, 0.0);
                    }

                    {
                        let _stream_span = tracing::info_span!("streaming_tick").entered();
                        chunk_streamer.tick(camera_chunk);
                    }
                    if renderer.chunk_border_debug_enabled() {
                        renderer.set_chunk_debug_states(chunk_streamer.debug_chunk_states());
                    }
                    if APPLY_STREAM_UPDATES_TO_RENDERER {
                        let _span = tracing::info_span!("apply_render_updates").entered();
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
                        let inventory_state = ecs_runtime.world().resource::<PlayerInventoryState>();
                        let slot_items: [hud::HudSlotItem; crate::inventory::HOTBAR_SLOT_COUNT] =
                            std::array::from_fn(|i| {
                                hud::hud_slot_item_from_stack(
                                    inventory_state.hotbar_stack(i),
                                    &tool_registry,
                                )
                            });
                        let hud_state = hud::HudState {
                            selected_slot: usize::from(inventory_state.selected_hotbar),
                            slot_items,
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
                            view_yaw: frame_camera.map_or(0.0, |snapshot| snapshot.interpolated.yaw),
                            view_pitch: frame_camera
                                .map_or(0.0, |snapshot| snapshot.interpolated.pitch),
                            overlay_brightness: frame_camera.map_or(1.0, |snapshot| {
                                alpha_first_person_brightness(
                                    &chunk_streamer,
                                    snapshot.interpolated.position,
                                    &snapshot.physics,
                                    ambient_darkness,
                                )
                            }),
                            armor_points: 0,
                            is_dead: frame_camera
                                .is_some_and(|snapshot| snapshot.vitals.health <= 0),
                            death_ticks: frame_camera
                                .map_or(0, |snapshot| snapshot.vitals.death_ticks),
                            sim_ticks,
                        };
                        let mut hud_verts =
                            hud::build_hud_vertices(sw, sh, &hud_state, &bootstrap_world.registry);
                        if let Some(menu) = open_menu {
                            let containers = ecs_runtime.world().resource::<ContainerRuntimeState>();
                            let mut inventory_verts = hud::build_inventory_vertices(
                                sw,
                                sh,
                                &inventory_state,
                                &containers,
                                menu,
                                mouse_screen_pos,
                                &bootstrap_world.registry,
                                &tool_registry,
                            );
                            hud_verts.append(&mut inventory_verts);
                        }
                        renderer.update_hud(&hud_verts);
                        hud_dirty = false;
                    }

                    if let Some(bench) = &mut bench_runtime {
                        if let Some(paths) = bench.take_screenshot_request() {
                            renderer.request_screenshot_paths(paths);
                        }
                    }

                    let render_result = {
                        let _render_span = tracing::info_span!("render").entered();
                        renderer.render()
                    };
                    match render_result {
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
                    if tracy_enabled {
                        trace_tracy_frame_mark();
                    }

                    if let Some(report) =
                        loop_stats.record_frame(frame_delta, ticks_to_run, tick_duration)
                    {
                        let residency = chunk_streamer.metrics();
                        let edit_latency = edit_latency_tracker.metrics();
                        if let Some(bench) = &mut bench_runtime {
                            if let Err(err) = bench.write_sample(
                                report,
                                residency,
                                renderer.visible_chunk_count(),
                            ) {
                                warn!(?err, "failed to write benchmark sample");
                            }
                        }
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
                    if let Some(bench) = &bench_runtime {
                        if bench.phase() == BenchPhase::Done {
                            info!(
                                output = %bench.config.output_path.display(),
                                "benchmark finished; exiting"
                            );
                            event_loop.exit();
                            return;
                        }
                    }
                }},
                WindowEvent::KeyboardInput { event, .. } => {
                    if bench_runtime.is_some() {
                        return;
                    }
                    if let PhysicalKey::Code(code) = event.physical_key {
                        let is_pressed = event.state == ElementState::Pressed;
                        let player_dead = ecs_runtime.player_is_dead();
                        let allow_input_passthrough =
                            !player_dead || matches!(code, KeyCode::Escape | KeyCode::KeyR);
                        if allow_input_passthrough && open_menu.is_none() {
                            ecs_runtime.handle_key(code, is_pressed);
                        }

                        if is_pressed && !event.repeat {
                            if player_dead && !matches!(code, KeyCode::KeyR | KeyCode::Escape) {
                                return;
                            }
                            if let Some(slot) = hotbar_slot_for_key(code) {
                                ecs_runtime
                                    .world_mut()
                                    .resource_mut::<InventoryCommandQueue>()
                                    .pending
                                    .push_back(InventoryCommand::SelectHotbar { index: slot as u8 });
                                hud_dirty = true;
                                let slot_state = ecs_runtime
                                    .world()
                                    .resource::<PlayerInventoryState>()
                                    .hotbar_stack(slot);
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

                        // T key: hold to accelerate simulation ticks (debug).
                        if code == KeyCode::KeyT {
                            sim_speedup_held = is_pressed;
                        }

                        if code == KeyCode::Escape && is_pressed {
                            if let Some(menu) = open_menu {
                                ecs_runtime
                                    .world_mut()
                                    .resource_mut::<InventoryCommandQueue>()
                                    .pending
                                    .push_back(InventoryCommand::CloseMenu { menu });
                                open_menu = None;
                                left_mouse_held = false;
                                mining_swing_hold_ticks = 0;
                                ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                                if !ecs_runtime.player_is_dead() {
                                    mouse_captured = true;
                                    set_mouse_capture(window, true);
                                }
                            } else {
                                mouse_captured = false;
                                left_mouse_held = false;
                                mining_swing_hold_ticks = 0;
                                ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                                set_mouse_capture(window, false);
                            }
                            hud_dirty = true;
                        }

                        if code == KeyCode::KeyE && is_pressed && !event.repeat {
                            if let Some(menu) = open_menu {
                                open_menu = None;
                                ecs_runtime
                                    .world_mut()
                                    .resource_mut::<InventoryCommandQueue>()
                                    .pending
                                    .push_back(InventoryCommand::CloseMenu { menu });
                                left_mouse_held = false;
                                mining_swing_hold_ticks = 0;
                                ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                                if !ecs_runtime.player_is_dead() {
                                    mouse_captured = true;
                                    set_mouse_capture(window, true);
                                }
                            } else {
                                open_menu = Some(InventoryMenuKind::Player);
                                mouse_captured = false;
                                left_mouse_held = false;
                                mining_swing_hold_ticks = 0;
                                ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                                set_mouse_capture(window, false);
                                ecs_runtime.clear_input_state();
                                let size = window.inner_size();
                                let center = PhysicalPosition::new(
                                    f64::from(size.width) * 0.5,
                                    f64::from(size.height) * 0.5,
                                );
                                if let Err(err) = window.set_cursor_position(center) {
                                    warn!(?err, "failed to center cursor on inventory open");
                                }
                                mouse_screen_pos = [center.x as f32, center.y as f32];
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
                    if bench_runtime.is_some() {
                        return;
                    }
                    if ecs_runtime.player_is_dead() {
                        return;
                    }
                    if let Some(menu) = open_menu {
                        let (sw, sh) = renderer.screen_size();
                        if let Some(slot) =
                            hit_test_slot_for_menu(mouse_screen_pos[0], mouse_screen_pos[1], sw, sh, menu)
                        {
                            ecs_runtime
                                .world_mut()
                                .resource_mut::<InventoryCommandQueue>()
                                .pending
                                .push_back(InventoryCommand::ClickSlot {
                                    slot,
                                    button: MouseButton::Left,
                                });
                        } else if !is_point_inside_inventory_panel(
                            mouse_screen_pos[0],
                            mouse_screen_pos[1],
                            sw,
                            sh,
                            menu,
                        ) {
                            ecs_runtime
                                .world_mut()
                                .resource_mut::<InventoryCommandQueue>()
                                .pending
                                .push_back(InventoryCommand::ClickOutside {
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
                        left_mouse_held = true;
                        mining_swing_hold_ticks = 0;
                        mining_start_requested = true;
                    }
                },
                WindowEvent::MouseInput {
                    button: MouseButton::Left,
                    state: ElementState::Released,
                    ..
                } => {
                    if bench_runtime.is_some() {
                        return;
                    }
                    left_mouse_held = false;
                    mining_swing_hold_ticks = 0;
                    mining_start_requested = false;
                    ecs_runtime.world_mut().resource_mut::<MiningState>().stop();
                }
                WindowEvent::MouseInput {
                    button: MouseButton::Right,
                    state: ElementState::Pressed,
                    ..
                } => {
                    if bench_runtime.is_some() {
                        return;
                    }
                    if ecs_runtime.player_is_dead() {
                        return;
                    }
                    if let Some(menu) = open_menu {
                        let (sw, sh) = renderer.screen_size();
                        if let Some(slot) =
                            hit_test_slot_for_menu(mouse_screen_pos[0], mouse_screen_pos[1], sw, sh, menu)
                        {
                            ecs_runtime
                                .world_mut()
                                .resource_mut::<InventoryCommandQueue>()
                                .pending
                                .push_back(InventoryCommand::ClickSlot {
                                    slot,
                                    button: MouseButton::Right,
                                });
                        } else if !is_point_inside_inventory_panel(
                            mouse_screen_pos[0],
                            mouse_screen_pos[1],
                            sw,
                            sh,
                            menu,
                        ) {
                            ecs_runtime
                                .world_mut()
                                .resource_mut::<InventoryCommandQueue>()
                                .pending
                                .push_back(InventoryCommand::ClickOutside {
                                    button: MouseButton::Right,
                                });
                        }
                        hud_dirty = true;
                        return;
                    }
                    if !mouse_captured {
                        return;
                    }
                    if enqueue_block_interaction_request(
                        &mut ecs_runtime,
                        &chunk_streamer,
                        &bootstrap_world.registry,
                        &mut block_interaction_requests,
                    ) {
                        first_person_hand.trigger_swing();
                        first_person_hand.trigger_item_used();
                    }
                },
                WindowEvent::MouseWheel { delta, .. } => {
                    if bench_runtime.is_some() {
                        return;
                    }
                    if open_menu.is_some() || ecs_runtime.player_is_dead() {
                        return;
                    }
                    let amount = match delta {
                        winit::event::MouseScrollDelta::LineDelta(_, y) => y as i32,
                        winit::event::MouseScrollDelta::PixelDelta(p) => p.y as i32,
                    };
                    if amount != 0 {
                        ecs_runtime
                            .world_mut()
                            .resource_mut::<InventoryCommandQueue>()
                            .pending
                            .push_back(InventoryCommand::ScrollHotbar {
                                delta: amount.signum() as i8,
                            });
                        hud_dirty = true;
                    }
                }
                WindowEvent::CursorMoved { position, .. } => {
                    if bench_runtime.is_some() {
                        return;
                    }
                    mouse_screen_pos = [position.x as f32, position.y as f32];
                    if open_menu.is_some() {
                        hud_dirty = true;
                    }
                }
                _ => {}
            },
            Event::DeviceEvent {
                event: DeviceEvent::MouseMotion { delta },
                ..
            } if mouse_captured && open_menu.is_none() => {
                if bench_runtime.is_some() {
                    return;
                }
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

fn init_tracing() -> bool {
    #[cfg(feature = "tracy")]
    {
        if parse_env_bool("THINGCRAFT_TRACY").unwrap_or(false) {
            tracing_subscriber::registry()
                .with(build_env_filter())
                .with(tracing_tracy::TracyLayer::default())
                .init();
            info!("tracy profiling enabled");
            return true;
        }
    }

    tracing_subscriber::fmt()
        .with_env_filter(build_env_filter())
        .init();
    false
}

fn trace_tracy_frame_mark() {
    #[cfg(feature = "tracy")]
    tracing::info!(tracy.frame_mark = true);
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

fn current_block_interaction_ray(ecs_runtime: &mut EcsRuntime) -> Option<(DVec3, DVec3)> {
    let snapshot = ecs_runtime.camera_snapshot()?;
    if snapshot.vitals.health <= 0 {
        return None;
    }

    let direction =
        direction_from_angles64(snapshot.authoritative.yaw, snapshot.authoritative.pitch);
    if direction.length_squared() == 0.0 {
        return None;
    }

    // In physics mode, position is at feet. Raycast from eye height.
    let mut origin = snapshot.authoritative.position;
    if !snapshot.fly_mode {
        origin.y += snapshot.physics.eye_height - snapshot.physics.render_eye_height_sneak_offset;
    }

    Some((origin, direction))
}

fn raycast_current_mining_target(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
) -> Option<MiningTarget> {
    let allow_liquids = false;
    let hit = raycast_interaction_target(ecs_runtime, chunk_streamer, registry, allow_liquids)?;
    let block_id = chunk_streamer
        .block_at_world(hit.block[0], hit.block[1], hit.block[2])
        .unwrap_or(AIR_BLOCK_ID);
    if block_id == AIR_BLOCK_ID {
        return None;
    }
    if registry
        .get(block_id)
        .is_some_and(|def| def.material == MaterialKind::Liquid)
    {
        return None;
    }
    Some(MiningTarget {
        x: hit.block[0],
        y: hit.block[1],
        z: hit.block[2],
    })
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
enum DropRule {
    None,
    SelfBlock,
    Block(u8),
    Item {
        item_id: u16,
        min_count: u8,
        max_count: u8,
    },
    ChanceBlock {
        block_id: u8,
        chance_denominator: u8,
    },
    ChanceItem {
        item_id: u16,
        chance_denominator: u8,
        fallback_block_id: Option<u8>,
    },
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
enum PlacementRule {
    Default,
    PlantSoil,
    SolidSupport,
    SugarCane,
    Cactus,
    SnowLayer,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
enum SurvivalRule {
    None,
    Flower,
    Mushroom,
    SugarCane,
    Cactus,
    SnowLayer,
    Sapling,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
struct BlockBehavior {
    drop_rule: DropRule,
    placement_rule: PlacementRule,
    survival_rule: SurvivalRule,
}

const DEFAULT_BLOCK_BEHAVIOR: BlockBehavior = BlockBehavior {
    drop_rule: DropRule::SelfBlock,
    placement_rule: PlacementRule::Default,
    survival_rule: SurvivalRule::None,
};

fn block_behavior(block_id: u8) -> BlockBehavior {
    match block_id {
        AIR_BLOCK_ID | FLOWING_WATER_BLOCK_ID | WATER_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::None,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        LIT_FURNACE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Block(FURNACE_BLOCK_ID),
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        STONE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Block(COBBLESTONE_BLOCK_ID),
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        GRASS_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Block(DIRT_BLOCK_ID),
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        GLASS_BLOCK_ID | BOOKSHELF_BLOCK_ID | MOB_SPAWNER_BLOCK_ID | ICE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::None,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        COAL_ORE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: COAL_ITEM_ID,
                min_count: 1,
                max_count: 1,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        DIAMOND_ORE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: DIAMOND_ITEM_ID,
                min_count: 1,
                max_count: 1,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        REDSTONE_ORE_BLOCK_ID | LIT_REDSTONE_ORE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: REDSTONE_ITEM_ID,
                min_count: 4,
                max_count: 5,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        OAK_LEAVES_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::ChanceBlock {
                block_id: SAPLING_BLOCK_ID,
                chance_denominator: 20,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        GRAVEL_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::ChanceItem {
                item_id: FLINT_ITEM_ID,
                chance_denominator: 10,
                fallback_block_id: Some(GRAVEL_BLOCK_ID),
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        CLAY_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: CLAY_BALL_ITEM_ID,
                min_count: 4,
                max_count: 4,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        GLOWSTONE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: GLOWSTONE_DUST_ITEM_ID,
                min_count: 1,
                max_count: 1,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        SNOW_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: SNOWBALL_ITEM_ID,
                min_count: 4,
                max_count: 4,
            },
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        SNOW_LAYER_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: SNOWBALL_ITEM_ID,
                min_count: 1,
                max_count: 1,
            },
            placement_rule: PlacementRule::SnowLayer,
            survival_rule: SurvivalRule::SnowLayer,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        SAPLING_BLOCK_ID => BlockBehavior {
            placement_rule: PlacementRule::PlantSoil,
            survival_rule: SurvivalRule::Sapling,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        CACTUS_BLOCK_ID => BlockBehavior {
            placement_rule: PlacementRule::Cactus,
            survival_rule: SurvivalRule::Cactus,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        SUGAR_CANE_BLOCK_ID => BlockBehavior {
            drop_rule: DropRule::Item {
                item_id: REEDS_ITEM_ID,
                min_count: 1,
                max_count: 1,
            },
            placement_rule: PlacementRule::SugarCane,
            survival_rule: SurvivalRule::SugarCane,
        },
        YELLOW_FLOWER_BLOCK_ID | RED_FLOWER_BLOCK_ID => BlockBehavior {
            placement_rule: PlacementRule::PlantSoil,
            survival_rule: SurvivalRule::Flower,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        BROWN_MUSHROOM_BLOCK_ID | RED_MUSHROOM_BLOCK_ID => BlockBehavior {
            placement_rule: PlacementRule::SolidSupport,
            survival_rule: SurvivalRule::Mushroom,
            ..DEFAULT_BLOCK_BEHAVIOR
        },
        _ => DEFAULT_BLOCK_BEHAVIOR,
    }
}

fn can_place_plant_on(block_id: u8) -> bool {
    matches!(block_id, GRASS_BLOCK_ID | DIRT_BLOCK_ID | FARMLAND_BLOCK_ID)
}

fn has_water_adjacent_to_ground(
    chunk_streamer: &ChunkStreamer,
    x: i32,
    ground_y: i32,
    z: i32,
) -> bool {
    let neighbors = [
        (x - 1, ground_y, z),
        (x + 1, ground_y, z),
        (x, ground_y, z - 1),
        (x, ground_y, z + 1),
    ];
    neighbors.into_iter().any(|(nx, ny, nz)| {
        chunk_streamer
            .block_at_world(nx, ny, nz)
            .is_some_and(|id| id == FLOWING_WATER_BLOCK_ID || id == WATER_BLOCK_ID)
    })
}

fn can_sugar_cane_survive(chunk_streamer: &ChunkStreamer, x: i32, y: i32, z: i32) -> bool {
    let Some(below) = y
        .checked_sub(1)
        .and_then(|by| chunk_streamer.block_at_world(x, by, z))
    else {
        return false;
    };
    if below == SUGAR_CANE_BLOCK_ID {
        return true;
    }
    (below == GRASS_BLOCK_ID || below == DIRT_BLOCK_ID)
        && has_water_adjacent_to_ground(chunk_streamer, x, y - 1, z)
}

fn alpha_material_is_opaque(material: MaterialKind) -> bool {
    !matches!(
        material,
        MaterialKind::Air
            | MaterialKind::Plant
            | MaterialKind::Decoration
            | MaterialKind::SnowLayer
            | MaterialKind::Fire
    )
}

fn alpha_material_is_solid(material: MaterialKind) -> bool {
    !matches!(
        material,
        MaterialKind::Air
            | MaterialKind::Plant
            | MaterialKind::Decoration
            | MaterialKind::SnowLayer
            | MaterialKind::Liquid
            | MaterialKind::Fire
    )
}

fn can_snow_layer_survive(
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    if y <= 0 {
        return false;
    }
    let Some(below) = chunk_streamer.block_at_world(x, y - 1, z) else {
        return false;
    };
    below != AIR_BLOCK_ID && registry.is_collidable(below) && registry.blocks_movement(below)
}

fn can_cactus_survive(
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    if y <= 0 {
        return false;
    }
    let Some(below) = chunk_streamer.block_at_world(x, y - 1, z) else {
        return false;
    };
    if below != CACTUS_BLOCK_ID && below != SAND_BLOCK_ID {
        return false;
    }
    for (nx, ny, nz) in [(x - 1, y, z), (x + 1, y, z), (x, y, z - 1), (x, y, z + 1)] {
        let Some(side_block) = chunk_streamer.block_at_world(nx, ny, nz) else {
            continue;
        };
        if alpha_material_is_solid(registry.material_of(side_block)) {
            return false;
        }
    }
    true
}

fn block_drop_stack(
    block_id: u8,
    can_harvest: bool,
    registry: &BlockRegistry,
    rng: &mut SmallRng,
) -> Option<crate::inventory::ItemStack> {
    if !can_harvest || block_id == AIR_BLOCK_ID || registry.is_liquid(block_id) {
        return None;
    }
    let drop = match block_behavior(block_id).drop_rule {
        DropRule::None => return None,
        DropRule::SelfBlock => crate::inventory::ItemStack::block(block_id, 1),
        DropRule::Block(id) => crate::inventory::ItemStack::block(id, 1),
        DropRule::Item {
            item_id,
            min_count,
            max_count,
        } => {
            let count = if min_count == max_count {
                min_count
            } else {
                rng.gen_range(min_count..=max_count)
            };
            crate::inventory::ItemStack::item(item_id, count)
        }
        DropRule::ChanceBlock {
            block_id,
            chance_denominator,
        } => {
            if chance_denominator == 0 || rng.gen_range(0..chance_denominator) != 0 {
                return None;
            }
            crate::inventory::ItemStack::block(block_id, 1)
        }
        DropRule::ChanceItem {
            item_id,
            chance_denominator,
            fallback_block_id,
        } => {
            if chance_denominator > 0 && rng.gen_range(0..chance_denominator) == 0 {
                crate::inventory::ItemStack::item(item_id, 1)
            } else if let Some(block_id) = fallback_block_id {
                crate::inventory::ItemStack::block(block_id, 1)
            } else {
                return None;
            }
        }
    };
    Some(drop)
}

fn random_tick_lcg_next(random_tick_lcg: &mut i32) -> i32 {
    *random_tick_lcg = random_tick_lcg
        .wrapping_mul(3)
        .wrapping_add(RANDOM_TICK_LCG_INCREMENT);
    *random_tick_lcg
}

fn random_tick_local_coords(random_tick_lcg: &mut i32) -> (i32, i32, i32) {
    let t = random_tick_lcg_next(random_tick_lcg) >> 2;
    let local_x = t & 0xF;
    let local_z = (t >> 8) & 0xF;
    let y = (t >> 16) & 0x7F;
    (local_x, y, local_z)
}

fn break_block_with_drop(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    edit_latency_tracker: &mut EditLatencyTracker,
    x: i32,
    y: i32,
    z: i32,
    block_id: u8,
    rng: &mut SmallRng,
) -> bool {
    let replacement_block = if block_id == ICE_BLOCK_ID
        && y > 0
        && chunk_streamer
            .block_at_world(x, y - 1, z)
            .is_some_and(|below| registry.blocks_movement(below) || registry.is_liquid(below))
    {
        FLOWING_WATER_BLOCK_ID
    } else {
        AIR_BLOCK_ID
    };
    if !chunk_streamer.set_block_at_world(x, y, z, replacement_block) {
        return false;
    }
    refresh_nearby_leaf_decay_metadata(chunk_streamer, registry, x, y, z);
    edit_latency_tracker.record_block_edit(x, z);
    if let Some(drop) = block_drop_stack(block_id, true, registry, rng) {
        crate::entity::spawn_dropped_item_stack(
            ecs_runtime.world_mut(),
            drop,
            DVec3::new(x as f64 + 0.5, y as f64 + 0.5, z as f64 + 0.5),
        );
    }
    true
}

fn update_neighbor_log_proximity_if_matches(
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    neighbor_x: i32,
    neighbor_y: i32,
    neighbor_z: i32,
    proximity: u8,
    updates_this_tick: &mut usize,
) {
    if chunk_streamer.block_at_world(neighbor_x, neighbor_y, neighbor_z) != Some(OAK_LEAVES_BLOCK_ID) {
        return;
    }
    let meta = chunk_streamer
        .block_metadata_at_world(neighbor_x, neighbor_y, neighbor_z)
        .unwrap_or(0);
    if meta == 0 || meta != proximity.saturating_sub(1) {
        return;
    }
    let _ = update_leaves_log_proximity(
        chunk_streamer,
        registry,
        neighbor_x,
        neighbor_y,
        neighbor_z,
        updates_this_tick,
    );
}

fn update_leaves_log_proximity(
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    x: i32,
    y: i32,
    z: i32,
    updates_this_tick: &mut usize,
) -> bool {
    if *updates_this_tick >= 100 {
        return false;
    }
    *updates_this_tick += 1;

    let mut proximity = if y > 0
        && chunk_streamer
            .block_at_world(x, y - 1, z)
            .is_some_and(|id| alpha_material_is_solid(registry.material_of(id)))
    {
        16
    } else {
        0
    };
    let mut current_meta = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
    if current_meta == 0 {
        current_meta = 1;
        let _ = chunk_streamer.set_block_with_metadata_at_world(x, y, z, OAK_LEAVES_BLOCK_ID, 1);
    }
    let sample_neighbor = |nx: i32, ny: i32, nz: i32, cur: u8, streamer: &ChunkStreamer| -> u8 {
        let Some(block_id) = streamer.block_at_world(nx, ny, nz) else {
            return cur;
        };
        if block_id == OAK_LOG_BLOCK_ID {
            return 16;
        }
        if block_id != OAK_LEAVES_BLOCK_ID {
            return cur;
        }
        let meta = streamer.block_metadata_at_world(nx, ny, nz).unwrap_or(0);
        if meta != 0 && meta > cur { meta } else { cur }
    };
    proximity = sample_neighbor(x, y - 1, z, proximity, chunk_streamer);
    proximity = sample_neighbor(x, y, z - 1, proximity, chunk_streamer);
    proximity = sample_neighbor(x, y, z + 1, proximity, chunk_streamer);
    proximity = sample_neighbor(x - 1, y, z, proximity, chunk_streamer);
    proximity = sample_neighbor(x + 1, y, z, proximity, chunk_streamer);

    let mut new_meta = proximity.saturating_sub(1);
    if new_meta < 10 {
        new_meta = 1;
    }
    if new_meta == current_meta {
        return false;
    }
    if !chunk_streamer.set_block_with_metadata_at_world(x, y, z, OAK_LEAVES_BLOCK_ID, new_meta) {
        return false;
    }

    update_neighbor_log_proximity_if_matches(
        chunk_streamer,
        registry,
        x,
        y - 1,
        z,
        current_meta,
        updates_this_tick,
    );
    update_neighbor_log_proximity_if_matches(
        chunk_streamer,
        registry,
        x,
        y + 1,
        z,
        current_meta,
        updates_this_tick,
    );
    update_neighbor_log_proximity_if_matches(
        chunk_streamer,
        registry,
        x,
        y,
        z - 1,
        current_meta,
        updates_this_tick,
    );
    update_neighbor_log_proximity_if_matches(
        chunk_streamer,
        registry,
        x,
        y,
        z + 1,
        current_meta,
        updates_this_tick,
    );
    update_neighbor_log_proximity_if_matches(
        chunk_streamer,
        registry,
        x - 1,
        y,
        z,
        current_meta,
        updates_this_tick,
    );
    update_neighbor_log_proximity_if_matches(
        chunk_streamer,
        registry,
        x + 1,
        y,
        z,
        current_meta,
        updates_this_tick,
    );
    true
}

fn refresh_nearby_leaf_decay_metadata(
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    origin_x: i32,
    origin_y: i32,
    origin_z: i32,
) {
    let mut updates_this_tick = 0_usize;
    for dz in -1..=1 {
        for dy in -1..=1 {
            for dx in -1..=1 {
                let x = origin_x + dx;
                let y = origin_y + dy;
                let z = origin_z + dz;
                if chunk_streamer.block_at_world(x, y, z) == Some(OAK_LEAVES_BLOCK_ID) {
                    let _ = update_leaves_log_proximity(
                        chunk_streamer,
                        registry,
                        x,
                        y,
                        z,
                        &mut updates_this_tick,
                    );
                }
            }
        }
    }
}

fn can_survive_random_tick(
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    behavior: BlockBehavior,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    match behavior.survival_rule {
        SurvivalRule::None => true,
        SurvivalRule::Flower => {
            if y <= 0 {
                return false;
            }
            let bright = chunk_streamer.raw_brightness_at_world(x, y, z).unwrap_or(0) >= 8;
            let sky_access = chunk_streamer.sky_light_at_world(x, y, z).unwrap_or(0) == 15;
            (bright || sky_access)
                && chunk_streamer
                    .block_at_world(x, y - 1, z)
                    .is_some_and(can_place_plant_on)
        }
        SurvivalRule::Mushroom => {
            if y <= 0 {
                return false;
            }
            chunk_streamer
                .raw_brightness_at_world(x, y, z)
                .unwrap_or(u8::MAX)
                <= 13
                && chunk_streamer
                    .block_at_world(x, y - 1, z)
                    .is_some_and(|below| registry.blocks_movement(below))
        }
        SurvivalRule::SugarCane => can_sugar_cane_survive(chunk_streamer, x, y, z),
        SurvivalRule::Cactus => can_cactus_survive(chunk_streamer, registry, x, y, z),
        SurvivalRule::SnowLayer => can_snow_layer_survive(chunk_streamer, registry, x, y, z),
        SurvivalRule::Sapling => {
            if y <= 0 {
                return false;
            }
            let bright = chunk_streamer.raw_brightness_at_world(x, y, z).unwrap_or(0) >= 8;
            let sky_access = chunk_streamer.sky_light_at_world(x, y, z).unwrap_or(0) == 15;
            (bright || sky_access)
                && chunk_streamer
                    .block_at_world(x, y - 1, z)
                    .is_some_and(can_place_plant_on)
        }
    }
}

fn tick_random_block_at(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    edit_latency_tracker: &mut EditLatencyTracker,
    x: i32,
    y: i32,
    z: i32,
    block_id: u8,
    rng: &mut SmallRng,
) -> bool {
    let behavior = block_behavior(block_id);
    if !can_survive_random_tick(chunk_streamer, registry, behavior, x, y, z) {
        let broken = break_block_with_drop(
            ecs_runtime,
            chunk_streamer,
            registry,
            edit_latency_tracker,
            x,
            y,
            z,
            block_id,
            rng,
        );
        if broken {
            apply_post_edit_support_rules(
                ecs_runtime,
                chunk_streamer,
                registry,
                edit_latency_tracker,
                x,
                y,
                z,
            );
        }
        return broken;
    }

    match block_id {
        GRASS_BLOCK_ID => {
            let above_y = y + 1;
            let above_block = chunk_streamer
                .block_at_world(x, above_y, z)
                .unwrap_or(AIR_BLOCK_ID);
            let above_opaque = alpha_material_is_opaque(registry.material_of(above_block));
            let above_brightness = chunk_streamer
                .raw_brightness_at_world(x, above_y, z)
                .unwrap_or(0);
            if above_brightness < 4 && above_opaque {
                if rng.gen_range(0..4) != 0 {
                    return false;
                }
                if chunk_streamer.set_block_at_world(x, y, z, DIRT_BLOCK_ID) {
                    edit_latency_tracker.record_block_edit(x, z);
                    return true;
                }
                return false;
            }
            if above_brightness >= 9 {
                let nx = x + rng.gen_range(-1..=1);
                let ny = y + rng.gen_range(-3..=1);
                let nz = z + rng.gen_range(-1..=1);
                if chunk_streamer.block_at_world(nx, ny, nz) != Some(DIRT_BLOCK_ID) {
                    return false;
                }
                let n_above = chunk_streamer
                    .block_at_world(nx, ny + 1, nz)
                    .unwrap_or(AIR_BLOCK_ID);
                let n_above_opaque = registry.get(n_above).is_some_and(|def| def.opacity >= 255);
                let n_brightness = chunk_streamer
                    .raw_brightness_at_world(nx, ny + 1, nz)
                    .unwrap_or(0);
                if n_brightness >= 4
                    && !n_above_opaque
                    && chunk_streamer.set_block_at_world(nx, ny, nz, GRASS_BLOCK_ID)
                {
                    edit_latency_tracker.record_block_edit(nx, nz);
                    return true;
                }
            }
            false
        }
        OAK_LEAVES_BLOCK_ID => {
            let metadata = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
            if metadata == 0 {
                let mut updates_this_tick = 0_usize;
                update_leaves_log_proximity(chunk_streamer, registry, x, y, z, &mut updates_this_tick)
            } else if metadata == 1 {
                break_block_with_drop(
                    ecs_runtime,
                    chunk_streamer,
                    registry,
                    edit_latency_tracker,
                    x,
                    y,
                    z,
                    block_id,
                    rng,
                )
            } else if rng.gen_range(0..10) == 0 {
                let mut updates_this_tick = 0_usize;
                update_leaves_log_proximity(chunk_streamer, registry, x, y, z, &mut updates_this_tick)
            } else {
                false
            }
        }
        SAPLING_BLOCK_ID => {
            if chunk_streamer.raw_brightness_at_world(x, y + 1, z).unwrap_or(0) < 9 {
                return false;
            }
            if rng.gen_range(0..5) != 0 {
                return false;
            }
            let meta = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
            if meta < 15 {
                chunk_streamer.set_block_with_metadata_at_world(x, y, z, SAPLING_BLOCK_ID, meta + 1)
            } else {
                // TODO(sapling-growth): Hook full Alpha tree feature selection/placement.
                false
            }
        }
        CACTUS_BLOCK_ID => {
            if chunk_streamer.block_at_world(x, y + 1, z) != Some(AIR_BLOCK_ID) {
                return false;
            }
            let mut height = 1_i32;
            while chunk_streamer.block_at_world(x, y - height, z) == Some(CACTUS_BLOCK_ID) {
                height += 1;
            }
            if height >= 3 {
                return false;
            }
            let meta = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
            if meta >= 15 {
                if chunk_streamer.set_block_at_world(x, y + 1, z, CACTUS_BLOCK_ID) {
                    let _ = chunk_streamer.set_block_with_metadata_at_world(
                        x,
                        y,
                        z,
                        CACTUS_BLOCK_ID,
                        0,
                    );
                    edit_latency_tracker.record_block_edit(x, z);
                    true
                } else {
                    false
                }
            } else {
                chunk_streamer.set_block_with_metadata_at_world(
                    x,
                    y,
                    z,
                    CACTUS_BLOCK_ID,
                    meta + 1,
                )
            }
        }
        SUGAR_CANE_BLOCK_ID => {
            if chunk_streamer.block_at_world(x, y + 1, z) != Some(AIR_BLOCK_ID) {
                return false;
            }
            let mut height = 1_i32;
            while chunk_streamer.block_at_world(x, y - height, z) == Some(SUGAR_CANE_BLOCK_ID) {
                height += 1;
            }
            if height >= 3 {
                return false;
            }
            let meta = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
            if meta >= 15 {
                if chunk_streamer.set_block_at_world(x, y + 1, z, SUGAR_CANE_BLOCK_ID) {
                    let _ = chunk_streamer.set_block_with_metadata_at_world(
                        x,
                        y,
                        z,
                        SUGAR_CANE_BLOCK_ID,
                        0,
                    );
                    edit_latency_tracker.record_block_edit(x, z);
                    true
                } else {
                    false
                }
            } else {
                chunk_streamer.set_block_with_metadata_at_world(
                    x,
                    y,
                    z,
                    SUGAR_CANE_BLOCK_ID,
                    meta + 1,
                )
            }
        }
        SNOW_BLOCK_ID | SNOW_LAYER_BLOCK_ID => {
            if chunk_streamer.block_light_at_world(x, y, z).unwrap_or(0) > 11 {
                break_block_with_drop(
                    ecs_runtime,
                    chunk_streamer,
                    registry,
                    edit_latency_tracker,
                    x,
                    y,
                    z,
                    block_id,
                    rng,
                )
            } else {
                false
            }
        }
        ICE_BLOCK_ID => {
            let threshold = 11_i32 - i32::from(registry.opacity_of(ICE_BLOCK_ID));
            if i32::from(chunk_streamer.block_light_at_world(x, y, z).unwrap_or(0)) > threshold {
                if chunk_streamer.set_block_at_world(x, y, z, WATER_BLOCK_ID) {
                    edit_latency_tracker.record_block_edit(x, z);
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
        _ => false,
    }
}

fn tick_random_blocks(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    edit_latency_tracker: &mut EditLatencyTracker,
    center_chunk: ChunkPos,
    random_tick_lcg: &mut i32,
    rng: &mut SmallRng,
) -> bool {
    let mut changed = false;
    for dz in -RANDOM_TICK_CHUNK_RADIUS..=RANDOM_TICK_CHUNK_RADIUS {
        for dx in -RANDOM_TICK_CHUNK_RADIUS..=RANDOM_TICK_CHUNK_RADIUS {
            let chunk_x = center_chunk.x + dx;
            let chunk_z = center_chunk.z + dz;
            let base_x = chunk_x * CHUNK_WIDTH as i32;
            let base_z = chunk_z * CHUNK_DEPTH as i32;
            for _ in 0..RANDOM_TICKS_PER_CHUNK {
                let (local_x, y, local_z) = random_tick_local_coords(random_tick_lcg);
                let world_x = base_x + local_x;
                let world_z = base_z + local_z;
                let Some(block_id) = chunk_streamer.block_at_world(world_x, y, world_z) else {
                    continue;
                };
                if !registry.ticks_randomly_of(block_id) {
                    continue;
                }
                if tick_random_block_at(
                    ecs_runtime,
                    chunk_streamer,
                    registry,
                    edit_latency_tracker,
                    world_x,
                    y,
                    world_z,
                    block_id,
                    rng,
                ) {
                    changed = true;
                }
            }
        }
    }
    changed
}

fn apply_post_edit_support_rules(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    edit_latency_tracker: &mut EditLatencyTracker,
    origin_x: i32,
    origin_y: i32,
    origin_z: i32,
) {
    let mut pending = VecDeque::new();
    let mut visited = HashSet::new();
    for dz in -1..=1 {
        for dy in -1..=1 {
            for dx in -1..=1 {
                pending.push_back((origin_x + dx, origin_y + dy, origin_z + dz));
            }
        }
    }
    while let Some((x, y, z)) = pending.pop_front() {
        if !(0..CHUNK_HEIGHT as i32).contains(&y) || !visited.insert((x, y, z)) {
            continue;
        }
        let Some(block_id) = chunk_streamer.block_at_world(x, y, z) else {
            continue;
        };
        if block_id == AIR_BLOCK_ID {
            continue;
        }

        let behavior = block_behavior(block_id);
        let should_break = match behavior.survival_rule {
            SurvivalRule::None => false,
            SurvivalRule::Flower => {
                !(y > 0
                    && chunk_streamer
                        .block_at_world(x, y - 1, z)
                        .is_some_and(can_place_plant_on))
            }
            SurvivalRule::Mushroom => {
                !(y > 0
                    && chunk_streamer
                        .block_at_world(x, y - 1, z)
                        .is_some_and(|below| registry.blocks_movement(below)))
            }
            SurvivalRule::SugarCane => !can_sugar_cane_survive(chunk_streamer, x, y, z),
            SurvivalRule::Cactus => !can_cactus_survive(chunk_streamer, registry, x, y, z),
            SurvivalRule::SnowLayer => !can_snow_layer_survive(chunk_streamer, registry, x, y, z),
            SurvivalRule::Sapling => {
                !(y > 0
                    && (chunk_streamer.raw_brightness_at_world(x, y, z).unwrap_or(0) >= 8
                        || chunk_streamer.sky_light_at_world(x, y, z).unwrap_or(0) == 15)
                    && chunk_streamer
                        .block_at_world(x, y - 1, z)
                        .is_some_and(can_place_plant_on))
            }
        };
        if !should_break {
            continue;
        }

        if chunk_streamer.set_block_at_world(x, y, z, AIR_BLOCK_ID) {
            edit_latency_tracker.record_block_edit(x, z);
            let seed = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .map_or(0_u64, |d| d.as_nanos() as u64)
                ^ (u64::from(x as u32) << 32)
                ^ (u64::from(y as u32) << 16)
                ^ u64::from(z as u32);
            let mut rng = SmallRng::seed_from_u64(seed);
            if let Some(drop) = block_drop_stack(block_id, true, registry, &mut rng) {
                crate::entity::spawn_dropped_item_stack(
                    ecs_runtime.world_mut(),
                    drop,
                    DVec3::new(x as f64 + 0.5, y as f64 + 0.5, z as f64 + 0.5),
                );
            }
            for (nx, ny, nz) in [
                (x, y + 1, z),
                (x, y - 1, z),
                (x + 1, y, z),
                (x - 1, y, z),
                (x, y, z + 1),
                (x, y, z - 1),
            ] {
                pending.push_back((nx, ny, nz));
            }
        }
    }
}

fn break_block_and_collect_drop(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    block_x: i32,
    block_y: i32,
    block_z: i32,
    edit_latency_tracker: &mut EditLatencyTracker,
    can_harvest: bool,
) -> Option<Option<(crate::inventory::ItemStack, DVec3)>> {
    let broken_block_id = chunk_streamer
        .block_at_world(block_x, block_y, block_z)
        .unwrap_or(AIR_BLOCK_ID);
    if broken_block_id == AIR_BLOCK_ID {
        return None;
    }
    let pos = BlockPos {
        x: block_x,
        y: block_y,
        z: block_z,
    };
    let mut chest_drops = Vec::new();
    {
        let mut containers = ecs_runtime
            .world_mut()
            .resource_mut::<ContainerRuntimeState>();
        if broken_block_id == CHEST_BLOCK_ID {
            if let Some(chest) = containers.remove_chest(pos) {
                for stack in chest.slots().iter().flatten() {
                    chest_drops.push(*stack);
                }
            }
        } else if broken_block_id == FURNACE_BLOCK_ID || broken_block_id == LIT_FURNACE_BLOCK_ID {
            // Alpha parity: furnace inventory is not spilled on remove; block-entity is dropped.
            let _ = containers.remove_furnace(pos);
        }
    }
    let seed = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_or(0_u64, |d| d.as_nanos() as u64)
        ^ (u64::from(block_x as u32) << 32)
        ^ (u64::from(block_y as u32) << 16)
        ^ u64::from(block_z as u32);
    let mut rng = SmallRng::seed_from_u64(seed);
    for mut stack in chest_drops {
        while stack.count > 0 {
            let split = rng.gen_range(10..=30).min(stack.count);
            stack.count -= split;
            let mut drop = stack;
            drop.count = split;
            let ox = rng.gen_range(0.1..0.9);
            let oy = rng.gen_range(0.1..0.9);
            let oz = rng.gen_range(0.1..0.9);
            crate::entity::spawn_dropped_item_stack(
                ecs_runtime.world_mut(),
                drop,
                DVec3::new(
                    block_x as f64 + ox,
                    block_y as f64 + oy,
                    block_z as f64 + oz,
                ),
            );
        }
    }
    let replacement_block = if broken_block_id == ICE_BLOCK_ID
        && block_y > 0
        && chunk_streamer
            .block_at_world(block_x, block_y - 1, block_z)
            .is_some_and(|below| registry.blocks_movement(below) || registry.is_liquid(below))
    {
        FLOWING_WATER_BLOCK_ID
    } else {
        AIR_BLOCK_ID
    };
    if !chunk_streamer.set_block_at_world(block_x, block_y, block_z, replacement_block) {
        return None;
    }
    refresh_nearby_leaf_decay_metadata(chunk_streamer, registry, block_x, block_y, block_z);
    edit_latency_tracker.record_block_edit(block_x, block_z);
    apply_post_edit_support_rules(
        ecs_runtime,
        chunk_streamer,
        registry,
        edit_latency_tracker,
        block_x,
        block_y,
        block_z,
    );
    if !can_harvest {
        return Some(None);
    }
    let drop_stack = block_drop_stack(broken_block_id, can_harvest, registry, &mut rng)?;
    let spawn_pos = DVec3::new(
        block_x as f64 + 0.5,
        block_y as f64 + 0.5,
        block_z as f64 + 0.5,
    );
    Some(Some((drop_stack, spawn_pos)))
}

fn resolve_mining_tool(
    inventory: &PlayerInventoryState,
    tool_registry: &ToolRegistry,
) -> MiningToolKind {
    match inventory.selected_item_key() {
        Some(ItemKey::Tool(id)) if tool_registry.get(id).is_some() => MiningToolKind::Tool(id),
        _ => MiningToolKind::None,
    }
}

fn alpha_tool_mining_speed(
    tool: MiningToolKind,
    block_id: u8,
    tool_registry: &ToolRegistry,
    block_registry: &BlockRegistry,
) -> f32 {
    match tool {
        MiningToolKind::Tool(id) => {
            if let Some(def) = tool_registry.get(id) {
                tool_registry.mining_speed_for_block(def, block_id, block_registry)
            } else {
                1.0
            }
        }
        MiningToolKind::None => 1.0,
    }
}

fn alpha_can_harvest_block(
    tool: MiningToolKind,
    block_id: u8,
    tool_registry: &ToolRegistry,
    block_registry: &BlockRegistry,
) -> bool {
    match tool {
        MiningToolKind::Tool(id) => {
            if let Some(def) = tool_registry.get(id) {
                tool_registry.can_harvest(def, block_id, block_registry)
            } else {
                crate::tool::hand_can_harvest(block_id, block_registry)
            }
        }
        MiningToolKind::None => crate::tool::hand_can_harvest(block_id, block_registry),
    }
}

struct MiningCalc {
    progress_per_tick: f32,
    tool: MiningToolKind,
    can_harvest: bool,
}

fn alpha_block_mining_calc(
    registry: &BlockRegistry,
    block_id: u8,
    inventory: &PlayerInventoryState,
    tool_registry: &ToolRegistry,
    player_on_ground: bool,
    player_in_water: bool,
) -> MiningCalc {
    let mining_time = registry.mining_hardness_of(block_id);
    let tool = resolve_mining_tool(inventory, tool_registry);
    let can_harvest = alpha_can_harvest_block(tool, block_id, tool_registry, registry);

    let progress_per_tick = if mining_time < 0.0 {
        0.0
    } else if mining_time == 0.0 {
        f32::INFINITY
    } else if !can_harvest {
        1.0 / mining_time / 100.0
    } else {
        let mut player_speed = alpha_tool_mining_speed(tool, block_id, tool_registry, registry);
        if player_in_water {
            player_speed /= 5.0;
        }
        if !player_on_ground {
            player_speed /= 5.0;
        }
        player_speed / mining_time / 30.0
    };

    MiningCalc {
        progress_per_tick,
        tool,
        can_harvest,
    }
}

fn try_start_block_mining(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    tool_registry: &ToolRegistry,
    edit_latency_tracker: &mut EditLatencyTracker,
) -> bool {
    let Some(target) = raycast_current_mining_target(ecs_runtime, chunk_streamer, registry) else {
        return false;
    };
    if ecs_runtime.world().resource::<MiningState>().progress != 0.0 {
        return false;
    }

    let Some(snapshot) = ecs_runtime.camera_snapshot() else {
        return false;
    };
    let block_id = chunk_streamer
        .block_at_world(target.x, target.y, target.z)
        .unwrap_or(AIR_BLOCK_ID);
    let calc = {
        let inventory = ecs_runtime.world().resource::<PlayerInventoryState>();
        alpha_block_mining_calc(
            registry,
            block_id,
            inventory,
            tool_registry,
            snapshot.physics.on_ground,
            snapshot.physics.in_water,
        )
    };
    if calc.progress_per_tick >= 1.0 {
        if let Some(drop) = break_block_and_collect_drop(
            ecs_runtime,
            chunk_streamer,
            registry,
            target.x,
            target.y,
            target.z,
            edit_latency_tracker,
            calc.can_harvest,
        ) {
            if let Some((drop_stack, spawn_pos)) = drop {
                crate::entity::spawn_dropped_item_stack(
                    ecs_runtime.world_mut(),
                    drop_stack,
                    spawn_pos,
                );
            }
            apply_post_block_break_effects(ecs_runtime, tool_registry, calc.tool);
            return true;
        }
    }
    false
}

fn tick_block_mining(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    registry: &BlockRegistry,
    tool_registry: &ToolRegistry,
    edit_latency_tracker: &mut EditLatencyTracker,
) -> bool {
    let mut mining_state = *ecs_runtime.world().resource::<MiningState>();
    mining_state.last_progress = mining_state.progress;
    if mining_state.cooldown_ticks > 0 {
        mining_state.cooldown_ticks = mining_state.cooldown_ticks.saturating_sub(1);
        *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
        return false;
    }

    let Some(target) = raycast_current_mining_target(ecs_runtime, chunk_streamer, registry) else {
        mining_state.stop();
        *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
        return false;
    };

    let is_same_target = mining_state.target.is_some_and(|old| old == target);
    if !is_same_target {
        mining_state.retarget(target);
        *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
        return false;
    }

    let Some(snapshot) = ecs_runtime.camera_snapshot() else {
        return false;
    };
    let block_id = chunk_streamer
        .block_at_world(target.x, target.y, target.z)
        .unwrap_or(AIR_BLOCK_ID);
    if block_id == AIR_BLOCK_ID {
        mining_state.stop();
        *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
        return false;
    }

    let calc = {
        let inventory = ecs_runtime.world().resource::<PlayerInventoryState>();
        alpha_block_mining_calc(
            registry,
            block_id,
            inventory,
            tool_registry,
            snapshot.physics.on_ground,
            snapshot.physics.in_water,
        )
    };
    mining_state.progress += calc.progress_per_tick;
    mining_state.ticks += 1.0;
    if mining_state.progress < 1.0 {
        *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
        return false;
    }

    if let Some(drop) = break_block_and_collect_drop(
        ecs_runtime,
        chunk_streamer,
        registry,
        target.x,
        target.y,
        target.z,
        edit_latency_tracker,
        calc.can_harvest,
    ) {
        if let Some((drop_stack, spawn_pos)) = drop {
            crate::entity::spawn_dropped_item_stack(ecs_runtime.world_mut(), drop_stack, spawn_pos);
        }
        *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
        apply_post_block_break_effects(ecs_runtime, tool_registry, calc.tool);
        return true;
    }

    *ecs_runtime.world_mut().resource_mut::<MiningState>() = mining_state;
    false
}

fn tick_container_furnaces(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &mut ChunkStreamer,
    edit_latency_tracker: &mut EditLatencyTracker,
) -> bool {
    let updates = ecs_runtime
        .world_mut()
        .resource_mut::<ContainerRuntimeState>()
        .tick_furnaces();
    let mut changed = false;
    for (pos, tick) in updates {
        changed |= tick.changed;
        if !tick.lit_state_changed {
            continue;
        }
        let Some(current) = chunk_streamer.block_at_world(pos.x, pos.y, pos.z) else {
            continue;
        };
        let target_block = if tick.now_lit {
            if current == FURNACE_BLOCK_ID {
                Some(LIT_FURNACE_BLOCK_ID)
            } else {
                None
            }
        } else if current == LIT_FURNACE_BLOCK_ID {
            Some(FURNACE_BLOCK_ID)
        } else {
            None
        };
        let Some(target_block) = target_block else {
            continue;
        };
        let metadata = chunk_streamer
            .block_metadata_at_world(pos.x, pos.y, pos.z)
            .unwrap_or(0);
        if chunk_streamer.set_block_with_metadata_at_world(
            pos.x,
            pos.y,
            pos.z,
            target_block,
            metadata,
        ) {
            edit_latency_tracker.record_block_edit(pos.x, pos.z);
            changed = true;
        }
    }
    changed
}

fn enqueue_block_interaction_request(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    queue: &mut VecDeque<BlockInteractionRequest>,
) -> bool {
    let allow_liquids = selected_item_allows_liquid_targeting_stub(
        ecs_runtime.world().resource::<PlayerInventoryState>(),
    );
    if raycast_interaction_target(ecs_runtime, chunk_streamer, registry, allow_liquids).is_none() {
        return false;
    }

    let Some((origin, direction)) = current_block_interaction_ray(ecs_runtime) else {
        return false;
    };

    queue.push_back(BlockInteractionRequest { origin, direction });
    true
}

fn selected_item_allows_liquid_targeting_stub(_inventory: &PlayerInventoryState) -> bool {
    // TODO(alpha-bucket): return true only for empty/filled bucket interactions.
    false
}

fn is_raycast_targetable_block(
    registry: &BlockRegistry,
    block_id: u8,
    metadata: u8,
    allow_liquids: bool,
) -> bool {
    if block_id == AIR_BLOCK_ID {
        return false;
    }
    if registry.is_liquid(block_id) {
        return allow_liquids && metadata == 0;
    }
    registry
        .get(block_id)
        .is_some_and(|block| block.can_ray_trace)
}

fn raycast_interaction_target(
    ecs_runtime: &mut EcsRuntime,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    allow_liquids: bool,
) -> Option<BlockRayHit> {
    let (origin, direction) = current_block_interaction_ray(ecs_runtime)?;
    raycast_first_solid_block(
        origin,
        direction,
        BLOCK_INTERACTION_REACH_BLOCKS,
        |x, y, z| {
            let block_id = chunk_streamer.block_at_world(x, y, z)?;
            let metadata = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
            let can_hit = is_raycast_targetable_block(registry, block_id, metadata, allow_liquids);
            if can_hit {
                Some(block_id)
            } else {
                None
            }
        },
    )
}

fn placement_intersects_player(
    ecs_runtime: &mut EcsRuntime,
    registry: &BlockRegistry,
    placing_block_id: u8,
    place_x: i32,
    place_y: i32,
    place_z: i32,
) -> bool {
    if !registry.is_collidable(placing_block_id) {
        return false;
    }
    let Some(snapshot) = ecs_runtime.camera_snapshot() else {
        return false;
    };
    let half_w = snapshot.physics.width * 0.5;
    let player_min_x = snapshot.authoritative.position.x - half_w;
    let player_max_x = snapshot.authoritative.position.x + half_w;
    let player_min_y = snapshot.authoritative.position.y;
    let player_max_y = snapshot.authoritative.position.y + snapshot.physics.height;
    let player_min_z = snapshot.authoritative.position.z - half_w;
    let player_max_z = snapshot.authoritative.position.z + half_w;

    let block_min_x = f64::from(place_x);
    let block_max_x = block_min_x + 1.0;
    let block_min_y = f64::from(place_y);
    let block_max_y = block_min_y + 1.0;
    let block_min_z = f64::from(place_z);
    let block_max_z = block_min_z + 1.0;

    player_min_x < block_max_x
        && player_max_x > block_min_x
        && player_min_y < block_max_y
        && player_max_y > block_min_y
        && player_min_z < block_max_z
        && player_max_z > block_min_z
}

fn can_replace_block_for_placement(registry: &BlockRegistry, target_block_id: u8) -> bool {
    registry.is_water(target_block_id)
        || registry.is_lava(target_block_id)
        || target_block_id == FIRE_BLOCK_ID
        || registry.is_snow_layer(target_block_id)
}

fn alpha_can_place_block_at(
    registry: &BlockRegistry,
    chunk_streamer: &ChunkStreamer,
    block_id: u8,
    place_x: i32,
    place_y: i32,
    place_z: i32,
) -> bool {
    match block_behavior(block_id).placement_rule {
        PlacementRule::Default => true,
        PlacementRule::PlantSoil => {
            if place_y <= 0 {
                return false;
            }
            chunk_streamer
                .block_at_world(place_x, place_y - 1, place_z)
                .is_some_and(can_place_plant_on)
        }
        PlacementRule::SolidSupport => {
            if place_y <= 0 {
                return false;
            }
            chunk_streamer
                .block_at_world(place_x, place_y - 1, place_z)
                .is_some_and(|below| registry.blocks_movement(below))
        }
        PlacementRule::SugarCane => {
            can_sugar_cane_survive(chunk_streamer, place_x, place_y, place_z)
        }
        PlacementRule::Cactus => can_cactus_survive(chunk_streamer, registry, place_x, place_y, place_z),
        PlacementRule::SnowLayer => can_snow_layer_survive(chunk_streamer, registry, place_x, place_y, place_z),
    }
}

fn alpha_is_chest_block(block_id: u8) -> bool {
    block_id == CHEST_BLOCK_ID
}

fn alpha_is_double_chest_at<F>(x: i32, y: i32, z: i32, mut block_lookup: F) -> bool
where
    F: FnMut(i32, i32, i32) -> Option<u8>,
{
    if !block_lookup(x, y, z).is_some_and(alpha_is_chest_block) {
        return false;
    }
    block_lookup(x - 1, y, z).is_some_and(alpha_is_chest_block)
        || block_lookup(x + 1, y, z).is_some_and(alpha_is_chest_block)
        || block_lookup(x, y, z - 1).is_some_and(alpha_is_chest_block)
        || block_lookup(x, y, z + 1).is_some_and(alpha_is_chest_block)
}

fn alpha_can_place_chest_at<F>(x: i32, y: i32, z: i32, mut block_lookup: F) -> bool
where
    F: FnMut(i32, i32, i32) -> Option<u8> + Copy,
{
    let mut adjacent = 0_u8;
    if block_lookup(x - 1, y, z).is_some_and(alpha_is_chest_block) {
        adjacent += 1;
    }
    if block_lookup(x + 1, y, z).is_some_and(alpha_is_chest_block) {
        adjacent += 1;
    }
    if block_lookup(x, y, z - 1).is_some_and(alpha_is_chest_block) {
        adjacent += 1;
    }
    if block_lookup(x, y, z + 1).is_some_and(alpha_is_chest_block) {
        adjacent += 1;
    }
    if adjacent > 1 {
        return false;
    }
    if alpha_is_double_chest_at(x - 1, y, z, block_lookup)
        || alpha_is_double_chest_at(x + 1, y, z, block_lookup)
        || alpha_is_double_chest_at(x, y, z - 1, block_lookup)
        || alpha_is_double_chest_at(x, y, z + 1, block_lookup)
    {
        return false;
    }
    true
}

fn alpha_horizontal_face_from_look(look_direction: DVec3) -> u8 {
    let horizontal = DVec3::new(look_direction.x, 0.0, look_direction.z);
    if horizontal.length_squared() <= f64::EPSILON {
        return 3;
    }
    if horizontal.x.abs() > horizontal.z.abs() {
        if horizontal.x > 0.0 {
            5
        } else {
            4
        }
    } else if horizontal.z > 0.0 {
        3
    } else {
        2
    }
}

fn alpha_opposite_face(face: u8) -> u8 {
    match face {
        2 => 3,
        3 => 2,
        4 => 5,
        5 => 4,
        _ => face,
    }
}

fn alpha_chest_facing_from_solid_neighbors(
    north_solid: bool,
    south_solid: bool,
    west_solid: bool,
    east_solid: bool,
) -> u8 {
    // Alpha ChestBlock.getSprite(WorldView,...): default single chest front is south (face 3),
    // with overrides based on adjacent opaque blocks.
    let mut front_face = 3_u8;
    if north_solid && !south_solid {
        front_face = 3;
    }
    if south_solid && !north_solid {
        front_face = 2;
    }
    if west_solid && !east_solid {
        front_face = 5;
    }
    if east_solid && !west_solid {
        front_face = 4;
    }
    front_face
}

fn alpha_chest_placement_metadata_from_neighbors(
    registry: &BlockRegistry,
    chunk_streamer: &ChunkStreamer,
    x: i32,
    y: i32,
    z: i32,
) -> u8 {
    let north = chunk_streamer
        .block_at_world(x, y, z - 1)
        .is_some_and(|id| registry.is_solid(id));
    let south = chunk_streamer
        .block_at_world(x, y, z + 1)
        .is_some_and(|id| registry.is_solid(id));
    let west = chunk_streamer
        .block_at_world(x - 1, y, z)
        .is_some_and(|id| registry.is_solid(id));
    let east = chunk_streamer
        .block_at_world(x + 1, y, z)
        .is_some_and(|id| registry.is_solid(id));
    alpha_chest_facing_from_solid_neighbors(north, south, west, east)
}

fn alpha_is_solid_block_at(
    registry: &BlockRegistry,
    chunk_streamer: &ChunkStreamer,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    chunk_streamer
        .block_at_world(x, y, z)
        .is_some_and(|id| registry.is_solid(id))
}

fn alpha_resolve_chest_menu_target(
    registry: &BlockRegistry,
    chunk_streamer: &ChunkStreamer,
    chest_pos: BlockPos,
) -> Option<InventoryMenuKind> {
    if alpha_is_solid_block_at(
        registry,
        chunk_streamer,
        chest_pos.x,
        chest_pos.y + 1,
        chest_pos.z,
    ) {
        return None;
    }

    let west = BlockPos {
        x: chest_pos.x - 1,
        y: chest_pos.y,
        z: chest_pos.z,
    };
    let east = BlockPos {
        x: chest_pos.x + 1,
        y: chest_pos.y,
        z: chest_pos.z,
    };
    let north = BlockPos {
        x: chest_pos.x,
        y: chest_pos.y,
        z: chest_pos.z - 1,
    };
    let south = BlockPos {
        x: chest_pos.x,
        y: chest_pos.y,
        z: chest_pos.z + 1,
    };

    for adjacent in [west, east, north, south] {
        if chunk_streamer
            .block_at_world(adjacent.x, adjacent.y, adjacent.z)
            .is_some_and(alpha_is_chest_block)
            && alpha_is_solid_block_at(
                registry,
                chunk_streamer,
                adjacent.x,
                adjacent.y + 1,
                adjacent.z,
            )
        {
            return None;
        }
    }

    if chunk_streamer
        .block_at_world(west.x, west.y, west.z)
        .is_some_and(alpha_is_chest_block)
    {
        return Some(InventoryMenuKind::Chest {
            primary: west,
            secondary: Some(chest_pos),
        });
    }
    if chunk_streamer
        .block_at_world(east.x, east.y, east.z)
        .is_some_and(alpha_is_chest_block)
    {
        return Some(InventoryMenuKind::Chest {
            primary: chest_pos,
            secondary: Some(east),
        });
    }
    if chunk_streamer
        .block_at_world(north.x, north.y, north.z)
        .is_some_and(alpha_is_chest_block)
    {
        return Some(InventoryMenuKind::Chest {
            primary: north,
            secondary: Some(chest_pos),
        });
    }
    if chunk_streamer
        .block_at_world(south.x, south.y, south.z)
        .is_some_and(alpha_is_chest_block)
    {
        return Some(InventoryMenuKind::Chest {
            primary: chest_pos,
            secondary: Some(south),
        });
    }
    Some(InventoryMenuKind::Chest {
        primary: chest_pos,
        secondary: None,
    })
}

fn alpha_placement_metadata_from_look(block_id: u8, look_direction: DVec3) -> u8 {
    match block_id {
        FURNACE_BLOCK_ID | LIT_FURNACE_BLOCK_ID => {
            // Alpha FurnaceBlock.onPlaced stores the front face in metadata (2..5).
            alpha_opposite_face(alpha_horizontal_face_from_look(look_direction))
        }
        PUMPKIN_BLOCK_ID | LIT_PUMPKIN_BLOCK_ID => {
            // Alpha PumpkinBlock.onPlaced stores 0..3 where:
            // 0->north(2), 1->east(5), 2->south(3), 3->west(4).
            match alpha_opposite_face(alpha_horizontal_face_from_look(look_direction)) {
                2 => 0,
                5 => 1,
                3 => 2,
                4 => 3,
                _ => 0,
            }
        }
        _ => 0,
    }
}

fn process_block_interaction_requests(
    ecs_runtime: &mut EcsRuntime,
    registry: &BlockRegistry,
    chunk_streamer: &mut ChunkStreamer,
    queue: &mut VecDeque<BlockInteractionRequest>,
    edit_latency_tracker: &mut EditLatencyTracker,
) -> BlockInteractionEvents {
    let mut events = BlockInteractionEvents::default();
    while let Some(request) = queue.pop_front() {
        if events.open_menu.is_none() {
            events.open_menu = process_single_block_interaction_request(
                ecs_runtime,
                registry,
                chunk_streamer,
                request,
                edit_latency_tracker,
            );
        } else {
            let _ = process_single_block_interaction_request(
                ecs_runtime,
                registry,
                chunk_streamer,
                request,
                edit_latency_tracker,
            );
        }
    }
    events
}

fn process_single_block_interaction_request(
    ecs_runtime: &mut EcsRuntime,
    registry: &BlockRegistry,
    chunk_streamer: &mut ChunkStreamer,
    request: BlockInteractionRequest,
    edit_latency_tracker: &mut EditLatencyTracker,
) -> Option<InventoryMenuKind> {
    let (allow_liquids, selected_block_id) = {
        let inventory = ecs_runtime.world().resource::<PlayerInventoryState>();
        (
            selected_item_allows_liquid_targeting_stub(inventory),
            inventory.selected_block_id(),
        )
    };
    let Some(hit) = raycast_first_solid_block(
        request.origin,
        request.direction,
        BLOCK_INTERACTION_REACH_BLOCKS,
        |x, y, z| {
            let block_id = chunk_streamer.block_at_world(x, y, z)?;
            let metadata = chunk_streamer.block_metadata_at_world(x, y, z).unwrap_or(0);
            let can_hit = is_raycast_targetable_block(registry, block_id, metadata, allow_liquids);
            if can_hit {
                Some(block_id)
            } else {
                None
            }
        },
    ) else {
        return None;
    };

    if hit.normal == [0, 0, 0] {
        return None;
    }
    let target_block_id = chunk_streamer
        .block_at_world(hit.block[0], hit.block[1], hit.block[2])
        .unwrap_or(AIR_BLOCK_ID);

    // Keep place-first semantics for held block stacks; fall back to block-use behavior if place fails.
    if let Some(block_id) = selected_block_id {
        if try_place_selected_block(
            ecs_runtime,
            registry,
            chunk_streamer,
            hit,
            target_block_id,
            block_id,
            request.direction,
            edit_latency_tracker,
        ) {
            return None;
        }
    }

    match target_block_id {
        58 => Some(InventoryMenuKind::CraftingTable), // crafting table
        54 => {
            let chest_pos = BlockPos {
                x: hit.block[0],
                y: hit.block[1],
                z: hit.block[2],
            };
            let Some(menu) = alpha_resolve_chest_menu_target(registry, chunk_streamer, chest_pos)
            else {
                return None;
            };
            if let InventoryMenuKind::Chest { primary, secondary } = menu {
                let mut containers = ecs_runtime
                    .world_mut()
                    .resource_mut::<ContainerRuntimeState>();
                containers.ensure_chest(primary);
                if let Some(secondary) = secondary {
                    containers.ensure_chest(secondary);
                }
            }
            Some(menu)
        }
        61 | 62 => {
            let pos = BlockPos {
                x: hit.block[0],
                y: hit.block[1],
                z: hit.block[2],
            };
            ecs_runtime
                .world_mut()
                .resource_mut::<ContainerRuntimeState>()
                .ensure_furnace(pos);
            Some(InventoryMenuKind::Furnace { pos })
        }
        _ => None,
    }
}

fn try_place_selected_block(
    ecs_runtime: &mut EcsRuntime,
    registry: &BlockRegistry,
    chunk_streamer: &mut ChunkStreamer,
    hit: BlockRayHit,
    target_block_id: u8,
    block_id: u8,
    look_direction: DVec3,
    edit_latency_tracker: &mut EditLatencyTracker,
) -> bool {
    let [x, y, z] = hit.block;
    let (place_x, place_y, place_z) = if registry.is_snow_layer(target_block_id) {
        (x, y, z)
    } else {
        let Some(px) = x.checked_add(hit.normal[0]) else {
            return false;
        };
        let Some(py) = y.checked_add(hit.normal[1]) else {
            return false;
        };
        let Some(pz) = z.checked_add(hit.normal[2]) else {
            return false;
        };
        (px, py, pz)
    };
    if placement_intersects_player(ecs_runtime, registry, block_id, place_x, place_y, place_z) {
        return false;
    }

    if block_id == CHEST_BLOCK_ID
        && !alpha_can_place_chest_at(place_x, place_y, place_z, |qx, qy, qz| {
            chunk_streamer.block_at_world(qx, qy, qz)
        })
    {
        return false;
    }

    let place_target_block = chunk_streamer
        .block_at_world(place_x, place_y, place_z)
        .unwrap_or(AIR_BLOCK_ID);
    let placement_metadata = if block_id == CHEST_BLOCK_ID {
        alpha_chest_placement_metadata_from_neighbors(
            registry,
            chunk_streamer,
            place_x,
            place_y,
            place_z,
        )
    } else {
        alpha_placement_metadata_from_look(block_id, look_direction)
    };
    if (place_target_block == AIR_BLOCK_ID
        || can_replace_block_for_placement(registry, place_target_block))
        && alpha_can_place_block_at(
            registry,
            chunk_streamer,
            block_id,
            place_x,
            place_y,
            place_z,
        )
        && chunk_streamer.set_block_with_metadata_at_world(
            place_x,
            place_y,
            place_z,
            block_id,
            placement_metadata,
        )
    {
        if block_id == CHEST_BLOCK_ID {
            ecs_runtime
                .world_mut()
                .resource_mut::<ContainerRuntimeState>()
                .ensure_chest(BlockPos {
                    x: place_x,
                    y: place_y,
                    z: place_z,
                });
        } else if block_id == FURNACE_BLOCK_ID || block_id == LIT_FURNACE_BLOCK_ID {
            ecs_runtime
                .world_mut()
                .resource_mut::<ContainerRuntimeState>()
                .ensure_furnace(BlockPos {
                    x: place_x,
                    y: place_y,
                    z: place_z,
                });
        }
        let _ = ecs_runtime
            .world_mut()
            .resource_mut::<PlayerInventoryState>()
            .apply(InventoryCommand::ConsumeSelected { amount: 1 });
        edit_latency_tracker.record_block_edit(place_x, place_z);
        apply_post_edit_support_rules(
            ecs_runtime,
            chunk_streamer,
            registry,
            edit_latency_tracker,
            place_x,
            place_y,
            place_z,
        );
        return true;
    }
    false
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

fn is_point_inside_inventory_panel(
    mouse_x: f32,
    mouse_y: f32,
    screen_w: f32,
    screen_h: f32,
    menu: InventoryMenuKind,
) -> bool {
    let layout = inventory_layout_for_menu(screen_w, screen_h, menu);
    let (panel_w, panel_h) = menu_panel_size(menu);
    let mx = mouse_x / layout.scale;
    let my = mouse_y / layout.scale;
    mx >= layout.left && mx < layout.left + panel_w && my >= layout.top && my < layout.top + panel_h
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

fn resolve_bench_config() -> BenchConfig {
    let enabled = parse_env_bool("THINGCRAFT_BENCH").unwrap_or(false);
    let still_secs = parse_env_u32("THINGCRAFT_BENCH_STILL_SECS").unwrap_or(10);
    let turn_secs = parse_env_u32("THINGCRAFT_BENCH_TURN_SECS").unwrap_or(10);
    let move_secs = parse_env_u32("THINGCRAFT_BENCH_MOVE_SECS").unwrap_or(10);
    let turn_pixels_per_sec = std::env::var("THINGCRAFT_BENCH_TURN_PX_PER_SEC")
        .ok()
        .and_then(|v| v.parse::<f64>().ok())
        .unwrap_or(240.0);
    let output_path = std::env::var("THINGCRAFT_BENCH_OUTPUT")
        .ok()
        .map(PathBuf::from)
        .unwrap_or_else(default_bench_output_path);
    BenchConfig {
        enabled,
        still_secs,
        turn_secs,
        move_secs,
        turn_pixels_per_sec,
        output_path,
    }
}

fn default_bench_output_path() -> PathBuf {
    let stamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_or(0, |d| d.as_secs());
    PathBuf::from(format!("docs/reports/benchmarks/bench_{stamp}.csv"))
}

fn screenshot_latest_dir_for_output(output_path: &std::path::Path) -> PathBuf {
    output_path
        .parent()
        .map_or_else(|| PathBuf::from("docs/reports/benchmarks"), PathBuf::from)
}

fn screenshot_run_dir_for_output(output_path: &std::path::Path) -> PathBuf {
    let root = screenshot_latest_dir_for_output(output_path);
    let stem = output_path
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("bench");
    root.join(format!("{stem}_screens"))
}

fn bench_phase_capture_offset(duration_secs: u32) -> f32 {
    if duration_secs <= 1 {
        (duration_secs as f32) * 0.5
    } else {
        duration_secs as f32 - 1.0
    }
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
    camera_pos: Vec3,
    physics: &crate::ecs::PhysicsBody,
    ambient_darkness: u8,
) -> f32 {
    // Alpha Entity.getBrightness samples near eye/body center (not strict camera floor).
    let world_x = camera_pos.x.floor() as i32;
    let sample_y = f64::from(camera_pos.y) - physics.eye_height + physics.height * 0.66;
    let world_y = sample_y.floor() as i32;
    let world_z = camera_pos.z.floor() as i32;
    let sky_default = 15_u8.saturating_sub(ambient_darkness.min(15));
    let light_level = chunk_streamer
        .effective_light_at_world(world_x, world_y, world_z, ambient_darkness)
        .unwrap_or(sky_default);
    alpha_brightness_from_light_level(light_level)
}

fn alpha_liquid_height_loss(metadata: u8) -> f64 {
    let level = if metadata >= 8 { 0 } else { metadata };
    f64::from(level + 1) / 9.0
}

#[derive(Clone, Copy)]
enum EyeLiquid {
    Water,
    Lava,
}

fn alpha_is_eye_submerged_in_material(
    body_pos: DVec3,
    physics: &crate::ecs::PhysicsBody,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    liquid: EyeLiquid,
) -> bool {
    // Alpha uses `entity.y + getEyeHeight()` in isSubmergedIn; player getEyeHeight() is 0.12f.
    // This delays underwater FX until the camera is meaningfully submerged.
    let eye_y = body_pos.y + physics.eye_height - physics.render_eye_height_sneak_offset + 0.12;
    let world_x = body_pos.x.floor() as i32;
    let world_y = eye_y.floor() as i32;
    let world_z = body_pos.z.floor() as i32;
    let Some(block_id) = chunk_streamer.block_at_world(world_x, world_y, world_z) else {
        return false;
    };
    let is_target_liquid = match liquid {
        EyeLiquid::Water => registry.is_water(block_id),
        EyeLiquid::Lava => registry.is_lava(block_id),
    };
    if !is_target_liquid {
        return false;
    }

    let metadata = chunk_streamer
        .block_metadata_at_world(world_x, world_y, world_z)
        .unwrap_or(0);
    let liquid_surface_y =
        (f64::from(world_y) + 1.0) - (alpha_liquid_height_loss(metadata) - 0.111_111_11);
    eye_y < liquid_surface_y
}

fn build_first_person_item_mesh(
    held_item: Option<ItemKey>,
    registry: &BlockRegistry,
    tool_registry: &ToolRegistry,
    equip_progress: f32,
    attack_progress: f32,
    view_yaw: f32,
    view_pitch: f32,
) -> ChunkMesh {
    let Some(item_key) = held_item else {
        return ChunkMesh::default();
    };

    match item_key {
        ItemKey::Tool(id) => {
            return build_first_person_tool_mesh(
                id,
                tool_registry,
                equip_progress,
                attack_progress,
                view_yaw,
                view_pitch,
            );
        }
        ItemKey::Item(id) => {
            if let Some(sprite_index) = crate::inventory::alpha_item_sprite_index(id) {
                return build_first_person_sprite_mesh(
                    sprite_index,
                    equip_progress,
                    attack_progress,
                    view_yaw,
                    view_pitch,
                );
            }
            return ChunkMesh::default();
        }
        ItemKey::Block(block_id) => {
            if !registry.is_defined_block(block_id) {
                return ChunkMesh::default();
            }
            return build_first_person_block_mesh(
                block_id,
                registry,
                equip_progress,
                attack_progress,
            );
        }
    }
}

/// Build a "thick sprite" mesh for a held tool, matching Alpha's
/// `ItemInHandRenderer.render()`.  The sprite gets front/back faces and
/// per-pixel-column edge quads that give it visible 3-D depth.
fn build_first_person_tool_mesh(
    tool_id: u16,
    tool_registry: &ToolRegistry,
    equip_progress: f32,
    attack_progress: f32,
    view_yaw: f32,
    view_pitch: f32,
) -> ChunkMesh {
    let Some(def) = tool_registry.get(tool_id) else {
        return ChunkMesh::default();
    };
    build_first_person_sprite_mesh(
        def.sprite_index,
        equip_progress,
        attack_progress,
        view_yaw,
        view_pitch,
    )
}

fn build_first_person_sprite_mesh(
    sprite_index: u16,
    equip_progress: f32,
    attack_progress: f32,
    view_yaw: f32,
    view_pitch: f32,
) -> ChunkMesh {
    let mut mesh = ChunkMesh::default();

    // Alpha ItemInHandRenderer.render() lines 58-66:
    //   translate(-k, -l, 0)  where k=0, l=0.3
    //   scale(1.5)
    //   rotateY(50)
    //   rotateZ(335)
    //   translate(-0.9375, -0.0625, 0)
    // These are applied inside the common hand transform (after scale(0.4)).
    let common = alpha_first_person_item_transform(equip_progress, attack_progress);
    let model = common
        * Mat4::from_translation(Vec3::new(0.0, -0.3, 0.0))
        * Mat4::from_scale(Vec3::splat(1.5))
        * Mat4::from_rotation_y(50.0_f32.to_radians())
        * Mat4::from_rotation_z(335.0_f32.to_radians())
        * Mat4::from_translation(Vec3::new(-0.9375, -0.0625, 0.0));

    // UV coordinates matching Alpha (lines 53-56).
    // Alpha uses: f = left_u, g = right_u (reversed from usual naming).
    // In Alpha: g = (sprite%16*16 + 0) / 256;  f = (sprite%16*16 + 15.99) / 256
    //           h = (sprite/16*16 + 0) / 256;   i = (sprite/16*16 + 15.99) / 256
    let col = sprite_index % 16;
    let row = sprite_index / 16;
    let u_left = (col as f32 * 16.0) / 256.0; // Alpha 'f' (actually left)
    let u_right = (col as f32 * 16.0 + 15.99) / 256.0; // Alpha 'g' (actually right)
    let v_top = (row as f32 * 16.0) / 256.0; // Alpha 'h'
    let v_bot = (row as f32 * 16.0 + 15.99) / 256.0; // Alpha 'i'

    // Map to Alpha variable names exactly:
    //   f = left U,  g = right U,  h = top V,  i = bottom V
    // The front face uses (g,i) at x=0 and (f,i) at x=j, so the sprite
    // appears horizontally mirrored — matching Alpha behaviour.
    let (f, g, h, i) = (u_left, u_right, v_top, v_bot);

    let n = 0.0625_f32; // thickness = 1/16 block
    let j = 1.0_f32; // width
    let light = [15_u8, 15, 255, 0];

    // Alpha ItemInHandRenderer.renderHand():
    //   glRotatef(playerPitch, 1,0,0); glRotatef(playerYaw, 0,1,0); Lighting.turnOn();
    // The light directions are transformed by those camera rotations, so held
    // item shading shifts as the player looks around.
    let light_rotation = Mat4::from_rotation_x(view_pitch) * Mat4::from_rotation_y(view_yaw);
    // Alpha Lighting.turnOn() uses OpenGL fixed-function lighting with:
    // ambient = 0.4, two diffuse lights at 0.6 each.
    let light0 = light_rotation
        .transform_vector3(Vec3::new(0.2, 1.0, -0.7))
        .normalize_or_zero();
    let light1 = light_rotation
        .transform_vector3(Vec3::new(-0.2, 1.0, 0.7))
        .normalize_or_zero();
    let alpha_lit_tint = |local_normal: Vec3| -> [u8; 4] {
        let n = model.transform_vector3(local_normal).normalize_or_zero();
        let brightness =
            (0.4 + 0.6 * n.dot(light0).max(0.0) + 0.6 * n.dot(light1).max(0.0)).clamp(0.0, 1.0);
        let c = (brightness * 255.0).round() as u8;
        [c, c, c, 255]
    };

    // Helper: push one quad (4 verts + 6 indices) through the model transform.
    let push_quad =
        |mesh: &mut ChunkMesh, corners: [[f32; 3]; 4], uvs: [[f32; 2]; 4], tint_rgba: [u8; 4]| {
            let base = mesh.vertices.len() as u32;
            for idx in 0..4 {
                let p = model.transform_point3(Vec3::from_array(corners[idx]));
                mesh.vertices.push(MeshVertex {
                    position: [p.x, p.y, p.z],
                    uv: uvs[idx],
                    tint_rgba,
                    light_data: light,
                });
            }
            mesh.indices
                .extend_from_slice(&[base, base + 1, base + 2, base, base + 2, base + 3]);
        };

    // Front face (z=0), normal +Z.  Alpha lines 69-73.
    push_quad(
        &mut mesh,
        [
            [0.0, 0.0, 0.0],
            [j, 0.0, 0.0],
            [j, 1.0, 0.0],
            [0.0, 1.0, 0.0],
        ],
        [[g, i], [f, i], [f, h], [g, h]],
        alpha_lit_tint(Vec3::Z),
    );

    // Back face (z=-n), normal -Z.  Alpha lines 76-80.
    push_quad(
        &mut mesh,
        [[0.0, 1.0, -n], [j, 1.0, -n], [j, 0.0, -n], [0.0, 0.0, -n]],
        [[g, h], [f, h], [f, i], [g, i]],
        alpha_lit_tint(-Vec3::Z),
    );

    // Left edge quads (normal -X).  Alpha lines 83-92.
    // 16 thin vertical quads, one per pixel column.
    let bleed = 0.001953125_f32; // 1/512 texel bleed guard
    for col_i in 0..16_u32 {
        let p = col_i as f32 / 16.0;
        let t = g + (f - g) * p - bleed;
        let x = j * p;
        push_quad(
            &mut mesh,
            [[x, 0.0, -n], [x, 0.0, 0.0], [x, 1.0, 0.0], [x, 1.0, -n]],
            [[t, i], [t, i], [t, h], [t, h]],
            alpha_lit_tint(-Vec3::X),
        );
    }

    // Right edge quads (normal +X).  Alpha lines 95-104.
    for col_i in 0..16_u32 {
        let q = col_i as f32 / 16.0;
        let u = g + (f - g) * q - bleed;
        let y = j * q + 0.0625;
        push_quad(
            &mut mesh,
            [[y, 1.0, -n], [y, 1.0, 0.0], [y, 0.0, 0.0], [y, 0.0, -n]],
            [[u, h], [u, h], [u, i], [u, i]],
            alpha_lit_tint(Vec3::X),
        );
    }

    // Top edge quads (normal +Y).  Alpha lines 107-116.
    for row_i in 0..16_u32 {
        let r = row_i as f32 / 16.0;
        let v = i + (h - i) * r - bleed;
        let z = j * r + 0.0625;
        push_quad(
            &mut mesh,
            [[0.0, z, 0.0], [j, z, 0.0], [j, z, -n], [0.0, z, -n]],
            [[g, v], [f, v], [f, v], [g, v]],
            alpha_lit_tint(Vec3::Y),
        );
    }

    // Bottom edge quads (normal -Y).  Alpha lines 119-128.
    for row_i in 0..16_u32 {
        let s = row_i as f32 / 16.0;
        let w = i + (h - i) * s - bleed;
        let aa = j * s;
        push_quad(
            &mut mesh,
            [[j, aa, 0.0], [0.0, aa, 0.0], [0.0, aa, -n], [j, aa, -n]],
            [[f, w], [g, w], [g, w], [f, w]],
            alpha_lit_tint(-Vec3::Y),
        );
    }

    mesh
}

fn build_first_person_block_mesh(
    block_id: u8,
    registry: &BlockRegistry,
    equip_progress: f32,
    attack_progress: f32,
) -> ChunkMesh {
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
    [[u0, v1], [u0, v0], [u1, v0], [u1, v1]]
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

    let submerged_in_water = alpha_is_eye_submerged_in_material(
        transform.position,
        &physics,
        chunk_streamer,
        registry,
        EyeLiquid::Water,
    );
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
    use std::thread;
    use std::time::Duration;

    use glam::DVec3;
    use rand::rngs::SmallRng;
    use rand::SeedableRng;
    use winit::keyboard::KeyCode;

    use super::{
        affected_chunks_for_block_edit, alpha_ambient_darkness, alpha_apply_fog_brightness,
        block_drop_stack, alpha_block_mining_calc, alpha_brightness_from_light_level,
        alpha_can_place_chest_at, can_place_plant_on,
        alpha_chest_facing_from_solid_neighbors, alpha_fog_brightness_target,
        alpha_horizontal_face_from_look, alpha_opposite_face, alpha_placement_metadata_from_look,
        alpha_star_brightness, alpha_sunrise_color, alpha_time_of_day, block_behavior,
        break_block_with_drop,
        can_replace_block_for_placement, has_target_directive, hotbar_slot_for_key,
        is_point_inside_inventory_panel, is_raycast_targetable_block, parse_env_bool,
        parse_env_u32, placement_intersects_player, random_tick_lcg_next,
        raycast_first_solid_block, resolve_axis, tick_random_block_at, tick_random_blocks,
        AdaptiveFluidBudget, BlockRayHit, DropRule, EditLatencyTracker, PlacementRule,
        SurvivalRule, AIR_BLOCK_ID, CHEST_BLOCK_ID, FLOWING_WATER_BLOCK_ID, FURNACE_BLOCK_ID, LIT_FURNACE_BLOCK_ID,
        LIT_PUMPKIN_BLOCK_ID, OAK_LEAVES_BLOCK_ID, OAK_LOG_BLOCK_ID, PUMPKIN_BLOCK_ID,
        RANDOM_TICKS_PER_CHUNK, RANDOM_TICK_CHUNK_RADIUS, SUGAR_CANE_BLOCK_ID,
        WATER_BLOCK_ID, DIRT_BLOCK_ID, GRASS_BLOCK_ID, ICE_BLOCK_ID, CACTUS_BLOCK_ID,
        SAPLING_BLOCK_ID, SNOW_LAYER_BLOCK_ID, SAND_BLOCK_ID, STONE_BLOCK_ID,
    };
    use crate::crafting::CraftingRegistry;
    use crate::ecs::EcsRuntime;
    use crate::gameplay::{
        apply_post_block_break_effects, run_inventory_command_system, InventoryCommandQueue,
        MiningState, MiningToolKind, ALPHA_MINING_COOLDOWN_TICKS,
    };
    use crate::inventory::{
        InventoryCommand, InventoryMenuKind, ItemKey, ItemStack, PlayerInventoryState,
    };
    use crate::streaming::{ChunkStreamer, ResidencyConfig};
    use crate::tool::ToolRegistry;
    use crate::world::{BlockRegistry, ChunkPos, CHUNK_DEPTH, CHUNK_WIDTH};

    fn wait_until_streamer_has_block(
        streamer: &mut ChunkStreamer,
        target: ChunkPos,
        world_x: i32,
        world_y: i32,
        world_z: i32,
    ) {
        let _ = streamer.update_target(target);
        for _ in 0..240 {
            streamer.tick(target);
            if streamer.block_at_world(world_x, world_y, world_z).is_some() {
                return;
            }
            thread::sleep(Duration::from_millis(2));
        }
        panic!("streamer did not load test chunk in time");
    }

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
    fn inventory_panel_bounds_match_gui_layout() {
        // 1920x1080 => GUI scale 4 => panel starts at ((480-176)/2, (270-166)/2) in GUI px.
        // Convert to screen px for the helper input path.
        let scale = 4.0;
        let left_px = 152.0 * scale;
        let top_px = 52.0 * scale;
        assert!(is_point_inside_inventory_panel(
            left_px + 1.0,
            top_px + 1.0,
            1920.0,
            1080.0,
            InventoryMenuKind::Player,
        ));
        assert!(!is_point_inside_inventory_panel(
            left_px - 1.0,
            top_px + 1.0,
            1920.0,
            1080.0,
            InventoryMenuKind::Player,
        ));
        assert!(!is_point_inside_inventory_panel(
            left_px + (176.0 * scale),
            top_px + (166.0 * scale),
            1920.0,
            1080.0,
            InventoryMenuKind::Player,
        ));
    }

    #[test]
    fn alpha_mining_progress_matches_hand_stone_rate() {
        let inv = PlayerInventoryState::alpha_defaults();
        let registry = BlockRegistry::alpha_1_2_6();
        let tool_reg = ToolRegistry::alpha_1_2_6();
        let calc = alpha_block_mining_calc(&registry, 1, &inv, &tool_reg, true, false);
        // Hand cannot harvest stone, so penalty path: 1.0 / 1.5 / 100.0
        assert!((calc.progress_per_tick - (1.0 / 1.5 / 100.0)).abs() < 1e-6);
    }

    #[test]
    fn alpha_mining_progress_applies_water_and_airborne_penalties() {
        let inv = PlayerInventoryState::alpha_defaults();
        let registry = BlockRegistry::alpha_1_2_6();
        let tool_reg = ToolRegistry::alpha_1_2_6();
        let grounded =
            alpha_block_mining_calc(&registry, 3, &inv, &tool_reg, true, false).progress_per_tick;
        let in_water =
            alpha_block_mining_calc(&registry, 3, &inv, &tool_reg, true, true).progress_per_tick;
        let airborne =
            alpha_block_mining_calc(&registry, 3, &inv, &tool_reg, false, false).progress_per_tick;
        assert!((in_water - grounded / 5.0).abs() < 1e-6);
        assert!((airborne - grounded / 5.0).abs() < 1e-6);
    }

    #[test]
    fn inventory_command_system_drains_queue_and_emits_drop_events() {
        let mut ecs = EcsRuntime::new();
        ecs.world_mut()
            .insert_resource(PlayerInventoryState::alpha_defaults());
        ecs.world_mut()
            .insert_resource(CraftingRegistry::alpha_1_2_6());
        ecs.world_mut()
            .insert_resource(InventoryCommandQueue::default());
        ecs.world_mut()
            .resource_mut::<PlayerInventoryState>()
            .cursor = Some(ItemStack::tool(270));
        ecs.world_mut()
            .resource_mut::<InventoryCommandQueue>()
            .pending
            .push_back(InventoryCommand::CloseMenu {
                menu: InventoryMenuKind::Player,
            });

        let events = run_inventory_command_system(&mut ecs, Some(InventoryMenuKind::Player));

        assert!(events.changed);
        assert_eq!(events.dropped_to_world, vec![ItemStack::tool(270)]);
        assert!(ecs
            .world()
            .resource::<InventoryCommandQueue>()
            .pending
            .is_empty());
    }

    #[test]
    fn instant_break_consumes_tool_durability() {
        let mut ecs = EcsRuntime::new();
        ecs.world_mut()
            .insert_resource(PlayerInventoryState::alpha_defaults());
        ecs.world_mut().insert_resource(MiningState::default());
        let _ = ecs
            .world_mut()
            .resource_mut::<PlayerInventoryState>()
            .apply(InventoryCommand::SelectHotbar { index: 4 });
        let tool_reg = ToolRegistry::alpha_1_2_6();
        let before = ecs
            .world()
            .resource::<PlayerInventoryState>()
            .selected_stack()
            .expect("selected tool")
            .metadata;

        apply_post_block_break_effects(&mut ecs, &tool_reg, MiningToolKind::Tool(270));

        let mining_state = *ecs.world().resource::<MiningState>();
        let after = ecs
            .world()
            .resource::<PlayerInventoryState>()
            .selected_stack()
            .expect("selected tool")
            .metadata;
        assert_eq!(mining_state.cooldown_ticks, ALPHA_MINING_COOLDOWN_TICKS);
        assert!(after > before);
    }

    #[test]
    fn non_harvest_break_still_applies_mining_cooldown() {
        let mut ecs = EcsRuntime::new();
        ecs.world_mut()
            .insert_resource(PlayerInventoryState::alpha_defaults());
        ecs.world_mut().insert_resource(MiningState::default());
        let tool_reg = ToolRegistry::alpha_1_2_6();

        apply_post_block_break_effects(&mut ecs, &tool_reg, MiningToolKind::None);

        let mining_state = *ecs.world().resource::<MiningState>();
        assert_eq!(mining_state.cooldown_ticks, ALPHA_MINING_COOLDOWN_TICKS);
    }

    #[test]
    fn interaction_raycast_skips_liquids_without_bucket_mode() {
        let registry = BlockRegistry::alpha_1_2_6();
        // water source
        assert!(!is_raycast_targetable_block(&registry, 9, 0, false));
        // flowing water / non-source
        assert!(!is_raycast_targetable_block(&registry, 8, 4, true));
        // source water is targetable only when allow_liquids is enabled (bucket-like path)
        assert!(is_raycast_targetable_block(&registry, 9, 0, true));
        // regular solid blocks are still targetable
        assert!(is_raycast_targetable_block(&registry, 1, 0, false));
    }

    #[test]
    fn placement_collision_rejects_blocks_inside_player() {
        let mut ecs = EcsRuntime::new();
        let registry = BlockRegistry::alpha_1_2_6();
        assert!(placement_intersects_player(
            &mut ecs, &registry, 1, 0, 72, 0
        ));
        assert!(!placement_intersects_player(
            &mut ecs, &registry, 1, 2, 72, 0
        ));
        // Torches have no collision shape in Alpha canPlace checks.
        assert!(!placement_intersects_player(
            &mut ecs, &registry, 50, 0, 72, 0
        ));
    }

    #[test]
    fn placement_replaces_alpha_replaceable_targets() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert!(can_replace_block_for_placement(&registry, 8)); // flowing water
        assert!(can_replace_block_for_placement(&registry, 9)); // water
        assert!(can_replace_block_for_placement(&registry, 10)); // flowing lava
        assert!(can_replace_block_for_placement(&registry, 11)); // lava
        assert!(can_replace_block_for_placement(&registry, 51)); // fire
        assert!(can_replace_block_for_placement(&registry, 78)); // snow layer
        assert!(!can_replace_block_for_placement(&registry, 1)); // stone
    }

    #[test]
    fn break_drop_rules_match_special_cases() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut rng = SmallRng::seed_from_u64(0xA126);

        let grass = block_drop_stack(2, true, &registry, &mut rng).expect("grass drop");
        assert_eq!(grass, ItemStack::block(3, 1));

        let coal_ore = block_drop_stack(16, true, &registry, &mut rng).expect("coal drop");
        assert_eq!(coal_ore, ItemStack::item(263, 1));

        let diamond_ore =
            block_drop_stack(56, true, &registry, &mut rng).expect("diamond drop");
        assert_eq!(diamond_ore, ItemStack::item(264, 1));

        let cane = block_drop_stack(83, true, &registry, &mut rng).expect("reeds drop");
        assert_eq!(cane, ItemStack::item(338, 1));

        // Leaves never drop themselves in Alpha; they occasionally drop saplings.
        let mut saw_sapling = false;
        for _ in 0..200 {
            if let Some(drop) = block_drop_stack(18, true, &registry, &mut rng) {
                assert_eq!(drop, ItemStack::block(6, 1));
                saw_sapling = true;
            }
        }
        assert!(saw_sapling);
    }

    #[test]
    fn break_drop_rules_cover_gravel_clay_glowstone_and_snow() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut rng = SmallRng::seed_from_u64(0xC1A7_2026);

        let clay = block_drop_stack(82, true, &registry, &mut rng).expect("clay drop");
        assert_eq!(clay, ItemStack::item(337, 4));

        let glow = block_drop_stack(89, true, &registry, &mut rng).expect("glowstone drop");
        assert_eq!(glow, ItemStack::item(348, 1));

        let snow = block_drop_stack(80, true, &registry, &mut rng).expect("snow drop");
        assert_eq!(snow, ItemStack::item(332, 4));

        let snow_layer =
            block_drop_stack(78, true, &registry, &mut rng).expect("snow layer drop");
        assert_eq!(snow_layer, ItemStack::item(332, 1));

        let mut saw_flint = false;
        let mut saw_gravel = false;
        for _ in 0..160 {
            let drop = block_drop_stack(13, true, &registry, &mut rng).expect("gravel drop");
            match drop.item {
                ItemKey::Item(318) => saw_flint = true,
                ItemKey::Block(13) => saw_gravel = true,
                _ => panic!("unexpected gravel drop: {drop:?}"),
            }
        }
        assert!(saw_flint);
        assert!(saw_gravel);
    }

    #[test]
    fn break_drop_rules_cover_stone_glass_bookshelf_spawner_and_ice() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut rng = SmallRng::seed_from_u64(2);

        let stone = block_drop_stack(1, true, &registry, &mut rng).expect("stone drop");
        assert_eq!(stone.item, ItemKey::Block(4));
        assert_eq!(stone.count, 1);

        assert!(block_drop_stack(20, true, &registry, &mut rng).is_none());
        assert!(block_drop_stack(47, true, &registry, &mut rng).is_none());
        assert!(block_drop_stack(52, true, &registry, &mut rng).is_none());
        assert!(block_drop_stack(79, true, &registry, &mut rng).is_none());
    }

    #[test]
    fn breaking_ice_replaces_with_flowing_water_when_blocked_below() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(777, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 40, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();

        let _ = streamer.set_block_at_world(8, 39, 8, 1);
        let _ = streamer.set_block_at_world(8, 40, 8, ICE_BLOCK_ID);
        assert_eq!(streamer.block_at_world(8, 39, 8), Some(1));
        assert_eq!(streamer.block_at_world(8, 40, 8), Some(ICE_BLOCK_ID));
        let mut rng = SmallRng::seed_from_u64(999);
        assert!(break_block_with_drop(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            40,
            8,
            ICE_BLOCK_ID,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 40, 8), Some(FLOWING_WATER_BLOCK_ID));
    }

    #[test]
    fn behavior_table_exposes_expected_special_rules() {
        let reeds = block_behavior(83);
        assert_eq!(reeds.placement_rule, PlacementRule::SugarCane);
        assert_eq!(reeds.survival_rule, SurvivalRule::SugarCane);

        let flower = block_behavior(37);
        assert_eq!(flower.placement_rule, PlacementRule::PlantSoil);
        assert_eq!(flower.survival_rule, SurvivalRule::Flower);

        let coal = block_behavior(16);
        assert_eq!(
            coal.drop_rule,
            DropRule::Item {
                item_id: 263,
                min_count: 1,
                max_count: 1
            }
        );

        let gravel = block_behavior(13);
        assert_eq!(
            gravel.drop_rule,
            DropRule::ChanceItem {
                item_id: 318,
                chance_denominator: 10,
                fallback_block_id: Some(13)
            }
        );

        let cactus = block_behavior(CACTUS_BLOCK_ID);
        assert_eq!(cactus.placement_rule, PlacementRule::Cactus);
        assert_eq!(cactus.survival_rule, SurvivalRule::Cactus);

        let sapling = block_behavior(SAPLING_BLOCK_ID);
        assert_eq!(sapling.placement_rule, PlacementRule::PlantSoil);
        assert_eq!(sapling.survival_rule, SurvivalRule::Sapling);

        let snow_layer = block_behavior(SNOW_LAYER_BLOCK_ID);
        assert_eq!(snow_layer.placement_rule, PlacementRule::SnowLayer);
        assert_eq!(snow_layer.survival_rule, SurvivalRule::SnowLayer);
    }

    #[test]
    fn flower_placement_supports_grass_dirt_and_farmland_only() {
        assert!(can_place_plant_on(2)); // grass
        assert!(can_place_plant_on(3)); // dirt
        assert!(can_place_plant_on(60)); // farmland
        assert!(!can_place_plant_on(1)); // stone
        assert!(!can_place_plant_on(12)); // sand
    }

    #[test]
    fn random_tick_leaves_decay_after_log_support_is_removed() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(123, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 70, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(42);

        for dy in -4..=4 {
            for dz in -4..=4 {
                for dx in -4..=4 {
                    let _ = streamer.set_block_at_world(8 + dx, 70 + dy, 8 + dz, AIR_BLOCK_ID);
                }
            }
        }
        let _ = streamer.set_block_at_world(8, 69, 8, OAK_LOG_BLOCK_ID);
        assert!(streamer.set_block_with_metadata_at_world(8, 70, 8, OAK_LEAVES_BLOCK_ID, 0));

        let _ = tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            70,
            8,
            18,
            &mut rng,
        );
        assert_eq!(streamer.block_at_world(8, 70, 8), Some(OAK_LEAVES_BLOCK_ID));

        let _ = streamer.set_block_at_world(8, 69, 8, AIR_BLOCK_ID);
        let mut decayed = false;
        for _ in 0..128 {
            let _ = tick_random_block_at(
                &mut ecs,
                &mut streamer,
                &registry,
                &mut latency,
                8,
                70,
                8,
                18,
                &mut rng,
            );
            if streamer.block_at_world(8, 70, 8) == Some(AIR_BLOCK_ID) {
                decayed = true;
                break;
            }
        }
        assert!(decayed, "leaf should decay after nearby log support is removed");
        assert_eq!(streamer.block_at_world(8, 70, 8), Some(AIR_BLOCK_ID));
    }

    #[test]
    fn random_tick_grass_underwater_decays_via_alpha_tick_rule() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(321, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 30, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(77);

        assert!(streamer.set_block_at_world(8, 31, 8, WATER_BLOCK_ID));
        assert!(streamer.set_block_at_world(8, 30, 8, GRASS_BLOCK_ID));
        assert_eq!(streamer.block_at_world(8, 30, 8), Some(GRASS_BLOCK_ID));
        assert!(
            matches!(
                streamer.block_at_world(8, 31, 8),
                Some(WATER_BLOCK_ID | FLOWING_WATER_BLOCK_ID)
            ),
            "expected water directly above grass"
        );
        let mut decayed = false;
        for _ in 0..128 {
            let _ = tick_random_block_at(
                &mut ecs,
                &mut streamer,
                &registry,
                &mut latency,
                8,
                30,
                8,
                GRASS_BLOCK_ID,
                &mut rng,
            );
            if streamer.block_at_world(8, 30, 8) == Some(DIRT_BLOCK_ID) {
                decayed = true;
                break;
            }
        }
        assert!(decayed, "grass should eventually decay under opaque-above rule");
        assert_eq!(streamer.block_at_world(8, 30, 8), Some(DIRT_BLOCK_ID));
    }

    #[test]
    fn random_tick_unsupported_sugar_cane_breaks_entire_stack() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(654, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 40, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(88);

        let _ = streamer.set_block_at_world(8, 39, 8, 12);
        let _ = streamer.set_block_at_world(8, 40, 8, SUGAR_CANE_BLOCK_ID);
        let _ = streamer.set_block_at_world(8, 41, 8, SUGAR_CANE_BLOCK_ID);
        let _ = streamer.set_block_at_world(8, 42, 8, SUGAR_CANE_BLOCK_ID);

        assert!(tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            40,
            8,
            SUGAR_CANE_BLOCK_ID,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 40, 8), Some(AIR_BLOCK_ID));
        assert_eq!(streamer.block_at_world(8, 41, 8), Some(AIR_BLOCK_ID));
        assert_eq!(streamer.block_at_world(8, 42, 8), Some(AIR_BLOCK_ID));
    }

    #[test]
    fn random_tick_unsupported_cactus_breaks() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(851, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 40, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(5);

        let _ = streamer.set_block_at_world(8, 39, 8, SAND_BLOCK_ID);
        let _ = streamer.set_block_at_world(8, 40, 8, CACTUS_BLOCK_ID);
        let _ = streamer.set_block_at_world(9, 40, 8, STONE_BLOCK_ID);

        assert!(tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            40,
            8,
            CACTUS_BLOCK_ID,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 40, 8), Some(AIR_BLOCK_ID));
    }

    #[test]
    fn random_tick_cactus_grows_when_metadata_reaches_max() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(852, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 40, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(6);

        for dz in -1..=1 {
            for dy in 0..=2 {
                for dx in -1..=1 {
                    let _ = streamer.set_block_at_world(8 + dx, 39 + dy, 8 + dz, AIR_BLOCK_ID);
                }
            }
        }
        let _ = streamer.set_block_at_world(8, 39, 8, SAND_BLOCK_ID);
        let _ = streamer.set_block_with_metadata_at_world(8, 40, 8, CACTUS_BLOCK_ID, 15);
        let _ = streamer.set_block_at_world(8, 41, 8, AIR_BLOCK_ID);

        assert!(tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            40,
            8,
            CACTUS_BLOCK_ID,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 41, 8), Some(CACTUS_BLOCK_ID));
        assert_eq!(streamer.block_metadata_at_world(8, 40, 8), Some(0));
    }

    #[test]
    fn random_tick_snow_layer_breaks_without_support() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(853, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 40, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(7);

        let _ = streamer.set_block_at_world(8, 39, 8, AIR_BLOCK_ID);
        let _ = streamer.set_block_at_world(8, 40, 8, SNOW_LAYER_BLOCK_ID);

        assert!(tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            40,
            8,
            SNOW_LAYER_BLOCK_ID,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 40, 8), Some(AIR_BLOCK_ID));
    }

    #[test]
    fn random_tick_sapling_advances_growth_metadata() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(854, registry.clone(), ResidencyConfig::default());
        let target = ChunkPos { x: 0, z: 0 };
        wait_until_streamer_has_block(&mut streamer, target, 8, 70, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(8);

        for dz in -1..=1 {
            for dy in 0..=3 {
                for dx in -1..=1 {
                    let _ = streamer.set_block_at_world(8 + dx, 69 + dy, 8 + dz, AIR_BLOCK_ID);
                }
            }
        }
        for y in 71..=127 {
            let _ = streamer.set_block_at_world(8, y, 8, AIR_BLOCK_ID);
        }
        let _ = streamer.set_block_at_world(8, 69, 8, DIRT_BLOCK_ID);
        let _ = streamer.set_block_with_metadata_at_world(8, 70, 8, SAPLING_BLOCK_ID, 0);
        for _ in 0..10 {
            streamer.tick(target);
        }
        assert!(
            streamer.sky_light_at_world(8, 70, 8).unwrap_or(0) == 15
                || streamer.raw_brightness_at_world(8, 70, 8).unwrap_or(0) >= 8,
            "expected sapling location to satisfy PlantBlock survival light"
        );

        let mut advanced = false;
        for _ in 0..64 {
            let _ = tick_random_block_at(
                &mut ecs,
                &mut streamer,
                &registry,
                &mut latency,
                8,
                70,
                8,
                SAPLING_BLOCK_ID,
                &mut rng,
            );
            if streamer.block_metadata_at_world(8, 70, 8).unwrap_or(0) > 0 {
                advanced = true;
                break;
            }
        }
        assert!(advanced, "sapling metadata should advance under bright conditions");
    }

    #[test]
    fn random_tick_ice_melts_under_block_light() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(855, registry.clone(), ResidencyConfig::default());
        let target = ChunkPos { x: 0, z: 0 };
        wait_until_streamer_has_block(&mut streamer, target, 8, 40, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(9);

        let _ = streamer.set_block_at_world(8, 39, 8, STONE_BLOCK_ID);
        let _ = streamer.set_block_at_world(8, 40, 8, ICE_BLOCK_ID);
        for (tx, tz) in [(9, 8), (7, 8), (8, 9), (8, 7)] {
            let _ = streamer.set_block_at_world(tx, 40, tz, 50);
        }
        let mut lit = false;
        for _ in 0..240 {
            streamer.tick(target);
            if streamer.block_light_at_world(8, 40, 8).unwrap_or(0) > 8 {
                lit = true;
                break;
            }
            thread::sleep(Duration::from_millis(2));
        }
        assert!(lit, "expected torch light to reach ice block");

        assert!(tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            40,
            8,
            ICE_BLOCK_ID,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 40, 8), Some(WATER_BLOCK_ID));
    }

    #[test]
    fn random_tick_flower_breaks_in_low_light_closed_space() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(456, registry.clone(), ResidencyConfig::default());
        wait_until_streamer_has_block(&mut streamer, ChunkPos { x: 0, z: 0 }, 8, 20, 8);
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(7);

        for y in 20..=22 {
            for z in 7..=9 {
                for x in 7..=9 {
                    let _ = streamer.set_block_at_world(x, y, z, 1);
                }
            }
        }
        let _ = streamer.set_block_at_world(8, 19, 8, 3);
        let _ = streamer.set_block_at_world(8, 20, 8, 37);
        assert_eq!(streamer.block_at_world(8, 20, 8), Some(37));
        assert!(
            streamer.raw_brightness_at_world(8, 20, 8).unwrap_or(15) < 8,
            "expected low-light chamber"
        );

        assert!(tick_random_block_at(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            8,
            20,
            8,
            37,
            &mut rng,
        ));
        assert_eq!(streamer.block_at_world(8, 20, 8), Some(0));
    }

    #[test]
    fn random_tick_scheduler_consumes_attempt_budget() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut streamer = ChunkStreamer::new(789, registry.clone(), ResidencyConfig::default());
        let mut ecs = EcsRuntime::new();
        let mut latency = EditLatencyTracker::default();
        let mut rng = SmallRng::seed_from_u64(99);
        let mut lcg = 1_i32;

        let attempts =
            ((RANDOM_TICK_CHUNK_RADIUS * 2 + 1) as usize).pow(2) * RANDOM_TICKS_PER_CHUNK;
        let mut expected = lcg;
        for _ in 0..attempts {
            let _ = random_tick_lcg_next(&mut expected);
        }

        let changed = tick_random_blocks(
            &mut ecs,
            &mut streamer,
            &registry,
            &mut latency,
            ChunkPos { x: 0, z: 0 },
            &mut lcg,
            &mut rng,
        );
        assert!(!changed);
        assert_eq!(lcg, expected);
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
    fn alpha_face_mapping_from_look_direction_matches_cardinals() {
        assert_eq!(
            alpha_horizontal_face_from_look(DVec3::new(0.0, 0.0, 1.0)),
            3
        );
        assert_eq!(
            alpha_horizontal_face_from_look(DVec3::new(0.0, 0.0, -1.0)),
            2
        );
        assert_eq!(
            alpha_horizontal_face_from_look(DVec3::new(1.0, 0.0, 0.0)),
            5
        );
        assert_eq!(
            alpha_horizontal_face_from_look(DVec3::new(-1.0, 0.0, 0.0)),
            4
        );
        assert_eq!(alpha_opposite_face(2), 3);
        assert_eq!(alpha_opposite_face(5), 4);
    }

    #[test]
    fn furnace_and_pumpkin_placement_metadata_faces_player() {
        // Looking +Z: block front should be -Z (face 2)
        let look_south = DVec3::new(0.0, 0.0, 1.0);
        assert_eq!(
            alpha_placement_metadata_from_look(FURNACE_BLOCK_ID, look_south),
            2
        );
        assert_eq!(
            alpha_placement_metadata_from_look(LIT_FURNACE_BLOCK_ID, look_south),
            2
        );
        assert_eq!(
            alpha_placement_metadata_from_look(PUMPKIN_BLOCK_ID, look_south),
            0
        );
        assert_eq!(
            alpha_placement_metadata_from_look(LIT_PUMPKIN_BLOCK_ID, look_south),
            0
        );
    }

    #[test]
    fn chest_facing_defaults_to_neighbor_derived_rules() {
        assert_eq!(
            alpha_chest_facing_from_solid_neighbors(false, false, false, false),
            3
        );
        assert_eq!(
            alpha_chest_facing_from_solid_neighbors(true, false, false, false),
            3
        );
        assert_eq!(
            alpha_chest_facing_from_solid_neighbors(false, true, false, false),
            2
        );
        assert_eq!(
            alpha_chest_facing_from_solid_neighbors(false, false, true, false),
            5
        );
        assert_eq!(
            alpha_chest_facing_from_solid_neighbors(false, false, false, true),
            4
        );
    }

    #[test]
    fn chest_placement_rejects_triple_chest_layout() {
        let blocks = vec![((-1, 0, 0), CHEST_BLOCK_ID), ((1, 0, 0), CHEST_BLOCK_ID)];
        let lookup = |x: i32, y: i32, z: i32| {
            blocks
                .iter()
                .find(|((bx, by, bz), _)| *bx == x && *by == y && *bz == z)
                .map(|(_, id)| *id)
        };
        assert!(!alpha_can_place_chest_at(0, 0, 0, lookup));
    }

    #[test]
    fn chest_placement_rejects_attaching_to_existing_double_chest() {
        let blocks = vec![((0, 0, 0), CHEST_BLOCK_ID), ((1, 0, 0), CHEST_BLOCK_ID)];
        let lookup = |x: i32, y: i32, z: i32| {
            blocks
                .iter()
                .find(|((bx, by, bz), _)| *bx == x && *by == y && *bz == z)
                .map(|(_, id)| *id)
        };
        assert!(!alpha_can_place_chest_at(2, 0, 0, lookup));
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

