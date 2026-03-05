use std::fs::{self, File};
use std::io::Write;
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use super::*;

const BENCH_MOVE_ASCEND_BLOCKS: f64 = 20.0;

#[derive(Debug, Clone)]
pub(super) struct BenchConfig {
    pub(super) enabled: bool,
    pub(super) still_secs: u32,
    pub(super) turn_secs: u32,
    pub(super) move_secs: u32,
    pub(super) turn_pixels_per_sec: f64,
    pub(super) output_path: PathBuf,
}

pub(super) struct BenchRuntime {
    pub(super) config: BenchConfig,
    start: Instant,
    output: File,
    last_phase: BenchPhase,
    pub(super) screenshot_run_dir: PathBuf,
    pub(super) screenshot_latest_dir: PathBuf,
    captured_still: bool,
    captured_turn: bool,
    captured_move: bool,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub(super) enum BenchPhase {
    Still,
    Turn,
    Move,
    Done,
}

impl BenchRuntime {
    pub(super) fn new(config: BenchConfig) -> Result<Self> {
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

    pub(super) fn phase(&self) -> BenchPhase {
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

    pub(super) fn apply_controls(&mut self, ecs_runtime: &mut EcsRuntime, frame_delta: Duration) {
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

    pub(super) fn write_sample(
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

    pub(super) fn take_screenshot_request(&mut self) -> Option<Vec<PathBuf>> {
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

pub(super) fn resolve_bench_config() -> BenchConfig {
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

fn screenshot_latest_dir_for_output(output_path: &Path) -> PathBuf {
    output_path
        .parent()
        .map_or_else(|| PathBuf::from("docs/reports/benchmarks"), PathBuf::from)
}

fn screenshot_run_dir_for_output(output_path: &Path) -> PathBuf {
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
