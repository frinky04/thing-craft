use std::collections::{HashSet, VecDeque};
use std::f64::consts::FRAC_PI_2;

use bevy_ecs::prelude::*;
use glam::{DVec2, DVec3, Vec2, Vec3};
use winit::keyboard::KeyCode;

#[derive(Component, Debug)]
pub struct Player;

#[derive(Component, Clone, Copy, Debug, PartialEq)]
pub struct Transform64 {
    pub position: DVec3,
    pub prev_position: DVec3,
    pub yaw: f64,
    pub prev_yaw: f64,
    pub pitch: f64,
    pub prev_pitch: f64,
}

impl Transform64 {
    pub fn new(position: DVec3) -> Self {
        Self {
            position,
            prev_position: position,
            yaw: 0.0,
            prev_yaw: 0.0,
            pitch: 0.0,
            prev_pitch: 0.0,
        }
    }
}

#[derive(Component, Clone, Copy, Debug)]
pub struct FlyCamera {
    pub speed: f64,
    pub sensitivity: f64,
    pub fly_mode: bool,
}

#[derive(Component, Clone, Copy, Debug, Default)]
pub struct RenderTransform {
    pub position: Vec3,
    pub yaw: f32,
    pub pitch: f32,
}

/// Physics body for gravity, friction, and AABB collision.
/// `position` in `Transform64` represents the player's feet when physics is active.
#[derive(Component, Clone, Copy, Debug)]
pub struct PhysicsBody {
    pub width: f64,
    pub height: f64,
    pub eye_height: f64,
    pub velocity: DVec3,
    pub on_ground: bool,
    pub fall_distance: f64,
    pub step_height: f64,
}

impl Default for PhysicsBody {
    fn default() -> Self {
        Self {
            width: 0.6,
            height: 1.8,
            eye_height: 1.62,
            velocity: DVec3::ZERO,
            on_ground: false,
            fall_distance: 0.0,
            step_height: 0.5,
        }
    }
}

/// Player health/damage state based on Alpha MobEntity fields.
#[derive(Component, Clone, Copy, Debug)]
pub struct PlayerVitals {
    pub health: i32,
    pub prev_health: i32,
    pub invulnerable_ticks: i32,
    pub invulnerable_timer: i32,
    pub prev_damage_taken: i32,
    pub damaged_time: i32,
    pub damaged_timer: i32,
    pub breath: i32,
    pub breath_capacity: i32,
    pub on_fire_timer: i32,
    pub death_ticks: i32,
    pub dead_ready: bool,
    pub submerged_in_water: bool,
}

impl Default for PlayerVitals {
    fn default() -> Self {
        Self {
            health: 20,
            prev_health: 20,
            invulnerable_ticks: 20,
            invulnerable_timer: 0,
            prev_damage_taken: 0,
            damaged_time: 0,
            damaged_timer: 0,
            breath: 300,
            breath_capacity: 300,
            on_fire_timer: 0,
            death_ticks: 0,
            dead_ready: false,
            submerged_in_water: false,
        }
    }
}

impl PlayerVitals {
    /// Alpha-like damage handling with invulnerability window behavior.
    pub fn apply_damage(&mut self, amount: i32) -> bool {
        if amount <= 0 || self.health <= 0 {
            return false;
        }

        if self.invulnerable_timer > self.invulnerable_ticks / 2 {
            if amount <= self.prev_damage_taken {
                return false;
            }
            self.health -= amount - self.prev_damage_taken;
            self.prev_damage_taken = amount;
        } else {
            self.prev_damage_taken = amount;
            self.prev_health = self.health;
            self.invulnerable_timer = self.invulnerable_ticks;
            self.health -= amount;
            self.damaged_time = 10;
            self.damaged_timer = 10;
        }

        self.health = self.health.max(0);
        true
    }
}

/// Player first-person bobbing state, mirroring Alpha player fields.
#[derive(Component, Clone, Copy, Debug, Default)]
pub struct PlayerWalkBob {
    pub walk_distance: f32,
    pub last_walk_distance: f32,
    pub bob: f32,
    pub last_bob: f32,
    pub tilt: f32,
    pub last_tilt: f32,
}

#[derive(Clone, Copy, Debug)]
pub enum SimCommandEvent {
    MoveIntent { x: f32, y: f32, z: f32 },
    LookDelta { yaw: f32, pitch: f32 },
    ToggleFly,
    Jump,
}

#[derive(Resource, Debug, Default)]
pub struct SimCommandQueue(pub VecDeque<SimCommandEvent>);

#[derive(Resource, Debug, Default)]
pub struct RawInputState {
    pub pressed: HashSet<KeyCode>,
    pub just_pressed: HashSet<KeyCode>,
    pub mouse_delta: DVec2,
}

#[derive(Resource, Clone, Copy, Debug)]
pub struct FixedTickConfig {
    pub tick_hz: u32,
    pub max_catchup_steps: u32,
}

impl Default for FixedTickConfig {
    fn default() -> Self {
        Self {
            tick_hz: 20,
            max_catchup_steps: 5,
        }
    }
}

#[derive(Resource, Debug, Default)]
pub struct PlayerIntentState {
    movement: Vec3,
    look_delta: Vec2,
    jump_requested: bool,
    toggle_fly_requested: bool,
}

#[derive(Resource, Debug, Clone, Copy, Default)]
pub struct RenderAlpha(pub f32);

#[derive(Resource, Clone, Copy, Debug)]
pub struct FixedDeltaSeconds(pub f64);

impl Default for FixedDeltaSeconds {
    fn default() -> Self {
        Self(1.0 / 20.0)
    }
}

#[derive(Clone, Copy, Debug)]
pub struct CameraSnapshot {
    pub authoritative: Transform64,
    pub interpolated: RenderTransform,
    pub fly_mode: bool,
    pub physics: PhysicsBody,
    pub vitals: PlayerVitals,
    pub walk_bob: PlayerWalkBob,
}

pub struct EcsRuntime {
    world: World,
    input_schedule: Schedule,
    fixed_schedule: Schedule,
    render_schedule: Schedule,
}

impl EcsRuntime {
    pub fn new() -> Self {
        let mut world = World::new();
        world.insert_resource(RawInputState::default());
        world.insert_resource(SimCommandQueue::default());
        world.insert_resource(PlayerIntentState::default());
        world.insert_resource(RenderAlpha::default());
        world.insert_resource(FixedTickConfig::default());
        world.insert_resource(FixedDeltaSeconds::default());

        world.spawn((
            Player,
            Transform64::new(DVec3::new(0.0, 72.0, 0.0)),
            FlyCamera {
                speed: 18.0,
                sensitivity: 0.0025,
                fly_mode: true,
            },
            RenderTransform::default(),
            PhysicsBody::default(),
            PlayerVitals::default(),
            PlayerWalkBob::default(),
        ));

        let mut input_schedule = Schedule::default();
        input_schedule.add_systems(capture_input_system);

        let mut fixed_schedule = Schedule::default();
        fixed_schedule.add_systems((consume_commands_system, apply_player_motion_system).chain());

        let mut render_schedule = Schedule::default();
        render_schedule.add_systems((
            interpolate_render_transform_system,
            crate::entity::interpolate_entity_render_positions,
        ));

        Self {
            world,
            input_schedule,
            fixed_schedule,
            render_schedule,
        }
    }

    pub fn fixed_tick_config(&self) -> FixedTickConfig {
        *self.world.resource::<FixedTickConfig>()
    }

    pub fn handle_key(&mut self, key_code: KeyCode, is_pressed: bool) {
        let mut input = self.world.resource_mut::<RawInputState>();
        if is_pressed {
            if input.pressed.insert(key_code) {
                input.just_pressed.insert(key_code);
            }
        } else {
            input.pressed.remove(&key_code);
        }
    }

    pub fn add_mouse_delta(&mut self, dx: f64, dy: f64) {
        let mut input = self.world.resource_mut::<RawInputState>();
        input.mouse_delta += DVec2::new(dx, dy);
    }

    pub fn run_input(&mut self) {
        self.input_schedule.run(&mut self.world);
    }

    pub fn run_fixed(&mut self, fixed_dt_seconds: f64) {
        self.world.resource_mut::<FixedDeltaSeconds>().0 = fixed_dt_seconds;
        self.fixed_schedule.run(&mut self.world);
    }

    pub fn run_render_prep(&mut self, alpha: f32) {
        self.world.resource_mut::<RenderAlpha>().0 = alpha.clamp(0.0, 1.0);
        self.render_schedule.run(&mut self.world);
    }

    pub fn camera_snapshot(&mut self) -> Option<CameraSnapshot> {
        let mut query = self.world.query_filtered::<(
            &Transform64,
            &RenderTransform,
            &FlyCamera,
            &PhysicsBody,
            &PlayerVitals,
            &PlayerWalkBob,
        ), With<Player>>();
        let (transform, render_transform, fly_camera, physics, vitals, walk_bob) =
            query.iter(&self.world).next()?;

        Some(CameraSnapshot {
            authoritative: *transform,
            interpolated: *render_transform,
            fly_mode: fly_camera.fly_mode,
            physics: *physics,
            vitals: *vitals,
            walk_bob: *walk_bob,
        })
    }

    /// Check if the player is currently in fly mode.
    pub fn is_fly_mode(&mut self) -> bool {
        let mut query = self.world.query_filtered::<&FlyCamera, With<Player>>();
        query
            .iter(&self.world)
            .next()
            .is_none_or(|cam| cam.fly_mode)
    }

    /// Direct access to the player's physics body for collision resolution.
    pub fn player_physics(&mut self) -> Option<(Transform64, PhysicsBody)> {
        let mut query = self
            .world
            .query_filtered::<(&Transform64, &PhysicsBody), With<Player>>();
        let (transform, physics) = query.iter(&self.world).next()?;
        Some((*transform, *physics))
    }

    /// Write back resolved position and physics state after collision.
    pub fn apply_resolved_physics(
        &mut self,
        position: DVec3,
        velocity: DVec3,
        on_ground: bool,
        fall_distance: f64,
        damage_taken: i32,
    ) {
        let mut query = self.world.query_filtered::<(
            &mut Transform64,
            &mut PhysicsBody,
            &mut PlayerVitals,
            &mut PlayerWalkBob,
        ), With<Player>>();
        if let Some((mut transform, mut physics, mut vitals, mut walk_bob)) =
            query.iter_mut(&mut self.world).next()
        {
            // Alpha walkDistance increments from horizontal movement after collision resolution.
            let delta_x = position.x - transform.prev_position.x;
            let delta_z = position.z - transform.prev_position.z;
            walk_bob.last_walk_distance = walk_bob.walk_distance;
            walk_bob.walk_distance += (delta_x * delta_x + delta_z * delta_z).sqrt() as f32 * 0.6;

            transform.position = position;
            // Alpha post-move velocity integration for next tick.
            let mut next_velocity = velocity;
            next_velocity.y -= GRAVITY;
            next_velocity.y *= AIR_DRAG_Y;
            let h_friction = if on_ground {
                GROUND_FRICTION
            } else {
                AIR_FRICTION_H
            };
            next_velocity.x *= h_friction;
            next_velocity.z *= h_friction;
            physics.velocity = next_velocity;
            physics.on_ground = on_ground;
            physics.fall_distance = fall_distance;

            // Alpha bob/tilt smoothing from horizontal/vertical motion.
            walk_bob.last_bob = walk_bob.bob;
            walk_bob.last_tilt = walk_bob.tilt;
            let mut bob_target =
                (next_velocity.x * next_velocity.x + next_velocity.z * next_velocity.z).sqrt()
                    as f32;
            let mut tilt_target = (-next_velocity.y * 0.2).atan() as f32 * 15.0;
            if bob_target > 0.1 {
                bob_target = 0.1;
            }
            if !on_ground || vitals.health <= 0 {
                bob_target = 0.0;
            }
            if on_ground || vitals.health <= 0 {
                tilt_target = 0.0;
            }
            walk_bob.bob += (bob_target - walk_bob.bob) * 0.4;
            walk_bob.tilt += (tilt_target - walk_bob.tilt) * 0.8;

            if damage_taken > 0 {
                let _ = vitals.apply_damage(damage_taken);
            }
        }
    }

    /// Mutable access to the ECS world for entity operations.
    pub fn world_mut(&mut self) -> &mut World {
        &mut self.world
    }

    #[cfg(test)]
    pub fn queued_commands_len(&self) -> usize {
        self.world.resource::<SimCommandQueue>().0.len()
    }
}

fn capture_input_system(
    mut raw_input: ResMut<'_, RawInputState>,
    mut queue: ResMut<'_, SimCommandQueue>,
) {
    let movement = movement_from_pressed(&raw_input.pressed);
    queue.0.push_back(SimCommandEvent::MoveIntent {
        x: movement.x,
        y: movement.y,
        z: movement.z,
    });

    if raw_input.mouse_delta.length_squared() > 0.0 {
        queue.0.push_back(SimCommandEvent::LookDelta {
            yaw: raw_input.mouse_delta.x as f32,
            pitch: raw_input.mouse_delta.y as f32,
        });
        raw_input.mouse_delta = DVec2::ZERO;
    }

    if raw_input.just_pressed.contains(&KeyCode::KeyF) {
        queue.0.push_back(SimCommandEvent::ToggleFly);
    }

    if raw_input.pressed.contains(&KeyCode::Space) {
        queue.0.push_back(SimCommandEvent::Jump);
    }

    raw_input.just_pressed.clear();
}

fn consume_commands_system(
    mut queue: ResMut<'_, SimCommandQueue>,
    mut intent: ResMut<'_, PlayerIntentState>,
) {
    while let Some(command) = queue.0.pop_front() {
        match command {
            SimCommandEvent::MoveIntent { x, y, z } => {
                intent.movement = Vec3::new(x, y, z);
            }
            SimCommandEvent::LookDelta { yaw, pitch } => {
                intent.look_delta += Vec2::new(yaw, pitch);
            }
            SimCommandEvent::ToggleFly => {
                // Handled by apply_player_motion_system so it can adjust position.
                intent.toggle_fly_requested = true;
            }
            SimCommandEvent::Jump => {
                intent.jump_requested = true;
            }
        }
    }
}

// Alpha physics constants (from Entity.java / MobEntity.java).
const GRAVITY: f64 = 0.08;
const AIR_DRAG_Y: f64 = 0.98;
const GROUND_SLIPPERINESS: f64 = 0.6;
const AIR_FRICTION_H: f64 = 0.91;
const GROUND_FRICTION: f64 = GROUND_SLIPPERINESS * AIR_FRICTION_H; // 0.546
const GROUND_ACCELERATION: f64 = 0.1; // Alpha: 0.1 * (f^3 / f^3), where f = GROUND_FRICTION
const AIR_ACCELERATION: f64 = 0.02;
const JUMP_VELOCITY: f64 = 0.42;

fn apply_player_motion_system(
    fixed_dt: Res<'_, FixedDeltaSeconds>,
    mut intent: ResMut<'_, PlayerIntentState>,
    mut players: Query<
        '_,
        '_,
        (
            &mut Transform64,
            &mut FlyCamera,
            &mut PhysicsBody,
            &mut PlayerVitals,
        ),
        With<Player>,
    >,
) {
    for (mut transform, mut camera, mut physics, mut vitals) in &mut players {
        if vitals.damaged_timer > 0 {
            vitals.damaged_timer -= 1;
        }
        if vitals.invulnerable_timer > 0 {
            vitals.invulnerable_timer -= 1;
        }

        // Handle fly toggle with position adjustment to prevent coordinate-space jump.
        // In fly mode, position = camera (eye). In physics mode, position = feet.
        if intent.toggle_fly_requested {
            camera.fly_mode = !camera.fly_mode;
            if camera.fly_mode {
                // Switching to fly: position was at feet, move up to eye level.
                transform.position.y += physics.eye_height;
            } else {
                // Switching to walk: position was at eye, move down to feet.
                transform.position.y -= physics.eye_height;
                physics.velocity = DVec3::ZERO;
                physics.on_ground = false;
            }
        }

        transform.prev_position = transform.position;
        transform.prev_yaw = transform.yaw;
        transform.prev_pitch = transform.pitch;

        transform.yaw -= f64::from(intent.look_delta.x) * camera.sensitivity;
        transform.pitch = (transform.pitch - f64::from(intent.look_delta.y) * camera.sensitivity)
            .clamp(-FRAC_PI_2 + 0.01, FRAC_PI_2 - 0.01);

        let yaw_sin = transform.yaw.sin();
        let yaw_cos = transform.yaw.cos();
        let pitch_cos = transform.pitch.cos();
        let pitch_sin = transform.pitch.sin();

        let forward =
            DVec3::new(yaw_sin * pitch_cos, pitch_sin, yaw_cos * pitch_cos).normalize_or_zero();
        let flat_forward = DVec3::new(forward.x, 0.0, forward.z).normalize_or_zero();
        let right = flat_forward.cross(DVec3::Y).normalize_or_zero();

        if camera.fly_mode {
            // Fly mode: direct position control (original behavior).
            let mut desired_direction =
                right * f64::from(intent.movement.x) + flat_forward * f64::from(intent.movement.z);
            desired_direction += DVec3::Y * f64::from(intent.movement.y);

            if desired_direction.length_squared() > 0.0 {
                desired_direction = desired_direction.normalize();
            }

            transform.position += desired_direction * camera.speed * fixed_dt.0;
            physics.velocity = DVec3::ZERO;
        } else {
            // Physics mode: apply jump + input acceleration and integrate position.
            // Collision resolution and post-move drag/friction are handled after
            // this system in app.rs via `apply_resolved_physics`.

            // Jump: apply impulse if on_ground and jump requested.
            if intent.jump_requested && physics.on_ground {
                physics.velocity.y = JUMP_VELOCITY;
                physics.on_ground = false;
            }

            // Horizontal input acceleration (Alpha formulas).
            let mut move_dir =
                right * f64::from(intent.movement.x) + flat_forward * f64::from(intent.movement.z);
            if move_dir.length_squared() > 0.0 {
                move_dir = move_dir.normalize();
            }

            let acceleration = if physics.on_ground {
                GROUND_ACCELERATION
            } else {
                AIR_ACCELERATION
            };

            physics.velocity.x += move_dir.x * acceleration;
            physics.velocity.z += move_dir.z * acceleration;

            // Apply current velocity to position first (Alpha Entity.move path).
            // Collision resolution happens in app.rs after this system.
            transform.position += physics.velocity;

        }
    }

    intent.look_delta = Vec2::ZERO;
    intent.jump_requested = false;
    intent.toggle_fly_requested = false;
}

fn interpolate_render_transform_system(
    alpha: Res<'_, RenderAlpha>,
    mut players: Query<
        '_,
        '_,
        (&Transform64, &mut RenderTransform, &FlyCamera, &PhysicsBody),
        With<Player>,
    >,
) {
    for (transform, mut render_transform, fly_camera, physics) in &mut players {
        let blend = f64::from(alpha.0);
        let mut pos = transform.prev_position.lerp(transform.position, blend);

        // In physics mode, position is at feet. Camera goes at eye height.
        if !fly_camera.fly_mode {
            pos.y += physics.eye_height;
        }

        render_transform.position = pos.as_vec3();
        render_transform.yaw =
            (transform.prev_yaw + (transform.yaw - transform.prev_yaw) * blend) as f32;
        render_transform.pitch =
            (transform.prev_pitch + (transform.pitch - transform.prev_pitch) * blend) as f32;
    }
}

fn movement_from_pressed(keys: &HashSet<KeyCode>) -> Vec3 {
    let mut movement = Vec3::ZERO;

    if keys.contains(&KeyCode::KeyA) {
        movement.x -= 1.0;
    }
    if keys.contains(&KeyCode::KeyD) {
        movement.x += 1.0;
    }
    if keys.contains(&KeyCode::Space) {
        movement.y += 1.0;
    }
    if keys.contains(&KeyCode::ShiftLeft) || keys.contains(&KeyCode::ShiftRight) {
        movement.y -= 1.0;
    }
    if keys.contains(&KeyCode::KeyW) {
        movement.z += 1.0;
    }
    if keys.contains(&KeyCode::KeyS) {
        movement.z -= 1.0;
    }

    movement
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn input_schedule_does_not_mutate_authoritative_transform() {
        let mut runtime = EcsRuntime::new();
        let before = runtime
            .camera_snapshot()
            .expect("expected a player entity")
            .authoritative;

        runtime.handle_key(KeyCode::KeyW, true);
        runtime.add_mouse_delta(10.0, 5.0);
        runtime.run_input();

        let after = runtime
            .camera_snapshot()
            .expect("expected a player entity")
            .authoritative;

        assert_eq!(before, after);
        assert!(runtime.queued_commands_len() > 0);
    }

    #[test]
    fn render_interpolation_blends_previous_and_current_transform() {
        let mut runtime = EcsRuntime::new();

        {
            let mut query = runtime
                .world
                .query_filtered::<&mut Transform64, With<Player>>();
            let mut transform = query
                .iter_mut(&mut runtime.world)
                .next()
                .expect("expected player transform");
            transform.prev_position = DVec3::ZERO;
            transform.position = DVec3::new(10.0, 0.0, 0.0);
            transform.prev_yaw = 0.0;
            transform.yaw = 2.0;
            transform.prev_pitch = 0.0;
            transform.pitch = 1.0;
        }

        runtime.run_render_prep(0.25);

        let snapshot = runtime
            .camera_snapshot()
            .expect("expected camera snapshot after interpolation");
        assert!((snapshot.interpolated.position.x - 2.5).abs() < 1e-6);
        assert!((snapshot.interpolated.yaw - 0.5).abs() < 1e-6);
        assert!((snapshot.interpolated.pitch - 0.25).abs() < 1e-6);
    }

    #[test]
    fn strafing_right_moves_negative_x_at_zero_yaw() {
        let mut runtime = EcsRuntime::new();
        let before = runtime
            .camera_snapshot()
            .expect("expected a player entity")
            .authoritative
            .position;

        runtime.handle_key(KeyCode::KeyD, true);
        runtime.run_input();
        runtime.run_fixed(1.0);

        let after = runtime
            .camera_snapshot()
            .expect("expected a player entity")
            .authoritative
            .position;
        assert!(after.x < before.x);
    }

    #[test]
    fn grounded_forward_tick_applies_pre_friction_displacement() {
        let mut runtime = EcsRuntime::new();
        {
            let mut query = runtime.world.query_filtered::<
                (&mut FlyCamera, &mut PhysicsBody, &mut Transform64),
                With<Player>,
            >();
            let (mut camera, mut physics, mut transform) = query
                .iter_mut(&mut runtime.world)
                .next()
                .expect("expected player");
            camera.fly_mode = false;
            physics.on_ground = true;
            physics.velocity = DVec3::ZERO;
            transform.yaw = 0.0;
            transform.pitch = 0.0;
        }

        let before = runtime
            .camera_snapshot()
            .expect("expected player snapshot")
            .authoritative
            .position;

        runtime.handle_key(KeyCode::KeyW, true);
        runtime.run_input();
        runtime.run_fixed(1.0 / 20.0);

        let after = runtime
            .camera_snapshot()
            .expect("expected player snapshot")
            .authoritative
            .position;
        let dz = after.z - before.z;
        assert!(
            dz > 0.09,
            "expected Alpha-like ~0.1 pre-friction displacement, got {dz}"
        );
    }
}
