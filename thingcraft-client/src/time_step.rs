use std::time::Duration;

#[derive(Debug, Clone)]
pub struct FixedStepClock {
    tick_dt: Duration,
    accumulator: Duration,
    max_catchup_steps: u32,
}

impl FixedStepClock {
    pub fn new(tick_hz: u32, max_catchup_steps: u32) -> Self {
        assert!(tick_hz > 0, "tick_hz must be > 0");
        assert!(max_catchup_steps > 0, "max_catchup_steps must be > 0");

        let tick_dt = Duration::from_secs_f64(1.0 / f64::from(tick_hz));
        Self {
            tick_dt,
            accumulator: Duration::ZERO,
            max_catchup_steps,
        }
    }

    pub fn tick_dt(&self) -> Duration {
        self.tick_dt
    }

    pub fn advance(&mut self, frame_delta: Duration) -> u32 {
        let max_backlog = self.tick_dt.saturating_mul(self.max_catchup_steps);
        self.accumulator = self.accumulator.saturating_add(frame_delta);
        if self.accumulator > max_backlog {
            self.accumulator = max_backlog;
        }

        let mut steps = 0;
        while steps < self.max_catchup_steps && self.accumulator >= self.tick_dt {
            self.accumulator = self.accumulator.saturating_sub(self.tick_dt);
            steps += 1;
        }

        steps
    }

    pub fn alpha(&self) -> f64 {
        let tick = self.tick_dt.as_secs_f64();
        if tick == 0.0 {
            return 0.0;
        }
        (self.accumulator.as_secs_f64() / tick).clamp(0.0, 1.0)
    }
}

#[cfg(test)]
mod tests {
    use super::FixedStepClock;
    use std::time::Duration;

    #[test]
    fn advances_expected_number_of_ticks() {
        let mut clock = FixedStepClock::new(20, 5);
        let ticks = clock.advance(Duration::from_millis(120));

        assert_eq!(ticks, 2);
        assert!((clock.alpha() - 0.4).abs() < 1e-6);
    }

    #[test]
    fn backlog_is_capped_by_max_catchup_steps() {
        let mut clock = FixedStepClock::new(20, 3);
        let ticks = clock.advance(Duration::from_secs(1));

        assert_eq!(ticks, 3);
        assert!(clock.alpha() <= 1.0);
    }
}
