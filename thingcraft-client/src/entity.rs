use std::f64::consts::TAU;
use std::sync::atomic::{AtomicU64, Ordering};

use bevy_ecs::prelude::*;
use glam::{DVec3, Vec3};
use rand::rngs::SmallRng;
use rand::{Rng, SeedableRng};

use crate::ecs::{PhysicsBody, Player, PlayerVitals, RenderAlpha, Transform64};
use crate::inventory::PlayerInventoryState;
use crate::mesh::{ChunkMesh, MeshVertex};
use crate::streaming::ChunkStreamer;
use crate::world::{BlockRegistry, CHUNK_HEIGHT};

// ---------------------------------------------------------------------------
// Alpha 1.2.6 ItemEntity constants (from decompiled ItemEntity.java)
// ---------------------------------------------------------------------------

const ITEM_GRAVITY: f64 = 0.04;
const ITEM_AIR_DRAG_Y: f64 = 0.98;
const ITEM_AIR_FRICTION_XZ: f64 = 0.98;
/// Default block slipperiness (0.6) * 0.98.
const ITEM_GROUND_FRICTION: f64 = 0.588;
const ITEM_BOUNCE_FACTOR: f64 = -0.5;
const ITEM_WIDTH: f64 = 0.25;
const ITEM_HEIGHT: f64 = 0.25;
const ITEM_SPAWN_VY: f64 = 0.2;
const ITEM_SPAWN_VH_RANGE: f64 = 0.1;
const ITEM_PICKUP_DELAY_TICKS: u32 = 10;
const ITEM_PLAYER_DROP_PICKUP_DELAY_TICKS: u32 = 40;
const ITEM_MAX_AGE_TICKS: u32 = 6000; // 5 minutes at 20 TPS
const ITEM_EJECTION_VEL_MIN: f64 = 0.1;
const ITEM_EJECTION_VEL_MAX: f64 = 0.3;
const ITEM_PLAYER_DROP_THROW_SPEED: f64 = 0.3;
const ITEM_PLAYER_DROP_SPREAD_SPEED: f64 = 0.02;
const ITEM_PLAYER_DROP_Y_BIAS: f64 = 0.1;
/// Player pickup query range from Alpha `PlayerEntity`: shape.grown(1.0, 0.0, 1.0).
const ITEM_PICKUP_RANGE_XZ: f64 = 1.0;
/// Alpha `EntityPickupParticle` lifetime in ticks.
const ITEM_PICKUP_FX_LIFETIME_TICKS: u8 = 3;
/// Alpha client uses offsetY = -0.5f when spawning item pickup particle.
/// In this codebase player `Transform64.position.y` is feet-space, so we apply
/// this relative to eye height to match first-person visual behavior.
const ITEM_PICKUP_FX_OFFSET_Y: f32 = -0.5;

/// Monotonic counter for seeding per-entity RNG (avoids needing `std_rng` feature).
static ENTITY_SEED_COUNTER: AtomicU64 = AtomicU64::new(1);

fn entity_rng() -> SmallRng {
    let seed = ENTITY_SEED_COUNTER.fetch_add(1, Ordering::Relaxed);
    SmallRng::seed_from_u64(seed ^ 0xDEAD_BEEF_CAFE_F00D)
}

/// Atlas tiles per row in terrain.png.
const ATLAS_TILES_PER_ROW: f32 = 16.0;
const ATLAS_TILE_UV: f32 = 1.0 / ATLAS_TILES_PER_ROW;

/// Visual scale of dropped item sprite (world units).
const SPRITE_HALF_SIZE: f32 = 0.125;

// ---------------------------------------------------------------------------
// Components
// ---------------------------------------------------------------------------

/// Marker: this entity is a dropped item.
#[derive(Component, Debug, Clone, Copy)]
pub struct DroppedItem {
    pub stack: crate::inventory::ItemStack,
}

/// Physics body for non-player entities (simpler than player PhysicsBody).
#[derive(Component, Clone, Copy, Debug)]
pub struct EntityPhysics {
    pub width: f64,
    pub height: f64,
    pub velocity: DVec3,
    pub on_ground: bool,
    pub gravity: f64,
    pub air_drag_y: f64,
    pub ground_friction: f64,
    pub air_friction_xz: f64,
    pub bounce_factor: f64,
}

/// Lifecycle tracking.
#[derive(Component, Clone, Copy, Debug)]
pub struct EntityAge {
    pub age_ticks: u32,
    pub max_age_ticks: u32,
}

/// Pickup delay: entity cannot be picked up until this reaches 0.
#[derive(Component, Clone, Copy, Debug)]
pub struct PickupDelay {
    pub ticks_remaining: u32,
}

/// Per-entity random bob offset for visual bobbing animation.
#[derive(Component, Clone, Copy, Debug)]
pub struct BobOffset(pub f32);

/// Interpolated render position for entities.
#[derive(Component, Clone, Copy, Debug, Default)]
pub struct EntityRenderPos {
    pub position: Vec3,
}

/// Short-lived visual pickup effect that lerps item render to the player.
#[derive(Component, Clone, Copy, Debug)]
pub struct EntityPickupFx {
    pub item: crate::inventory::ItemKey,
    pub start_pos: DVec3,
    pub start_age_ticks: u32,
    pub bob_offset: f32,
    pub age_ticks: u8,
    pub lifetime_ticks: u8,
    pub offset_y: f32,
}

// ---------------------------------------------------------------------------
// Spawn
// ---------------------------------------------------------------------------

/// Spawn a dropped block item entity at `position` with Alpha-faithful initial velocity.
pub fn spawn_dropped_item(world: &mut World, block_id: u8, position: DVec3) {
    spawn_dropped_item_stack(
        world,
        crate::inventory::ItemStack::block(block_id, 1),
        position,
    );
}

/// Spawn a dropped item entity (full ItemStack) with Alpha-faithful initial velocity.
pub fn spawn_dropped_item_stack(
    world: &mut World,
    stack: crate::inventory::ItemStack,
    position: DVec3,
) {
    let mut rng = entity_rng();
    let vx = rng.gen_range(-ITEM_SPAWN_VH_RANGE..ITEM_SPAWN_VH_RANGE);
    let vz = rng.gen_range(-ITEM_SPAWN_VH_RANGE..ITEM_SPAWN_VH_RANGE);
    let bob = rng.gen_range(0.0..TAU) as f32;
    spawn_dropped_item_with(
        world,
        stack,
        position,
        DVec3::new(vx, ITEM_SPAWN_VY, vz),
        ITEM_PICKUP_DELAY_TICKS,
        bob,
    );
}

/// Spawn a player-dropped item (full ItemStack) with throw impulse from yaw/pitch.
pub fn spawn_player_dropped_item_stack(
    world: &mut World,
    stack: crate::inventory::ItemStack,
    position: DVec3,
    yaw_radians: f64,
    pitch_radians: f64,
) {
    let mut rng = entity_rng();
    let mut vx = yaw_radians.sin() * pitch_radians.cos() * ITEM_PLAYER_DROP_THROW_SPEED;
    let mut vz = yaw_radians.cos() * pitch_radians.cos() * ITEM_PLAYER_DROP_THROW_SPEED;
    let mut vy = pitch_radians.sin() * ITEM_PLAYER_DROP_THROW_SPEED + ITEM_PLAYER_DROP_Y_BIAS;

    let h = rng.gen_range(0.0..TAU);
    let jitter = ITEM_PLAYER_DROP_SPREAD_SPEED * rng.gen_range(0.0..1.0);
    vx += h.cos() * jitter;
    vy += rng.gen_range(-0.1..0.1);
    vz += h.sin() * jitter;
    let bob = rng.gen_range(0.0..TAU) as f32;

    spawn_dropped_item_with(
        world,
        stack,
        position,
        DVec3::new(vx, vy, vz),
        ITEM_PLAYER_DROP_PICKUP_DELAY_TICKS,
        bob,
    );
}

fn spawn_dropped_item_with(
    world: &mut World,
    stack: crate::inventory::ItemStack,
    position: DVec3,
    velocity: DVec3,
    pickup_delay_ticks: u32,
    bob_offset: f32,
) {
    world.spawn((
        DroppedItem { stack },
        Transform64::new(position),
        EntityPhysics {
            width: ITEM_WIDTH,
            height: ITEM_HEIGHT,
            velocity,
            on_ground: false,
            gravity: ITEM_GRAVITY,
            air_drag_y: ITEM_AIR_DRAG_Y,
            ground_friction: ITEM_GROUND_FRICTION,
            air_friction_xz: ITEM_AIR_FRICTION_XZ,
            bounce_factor: ITEM_BOUNCE_FACTOR,
        },
        EntityAge {
            age_ticks: 0,
            max_age_ticks: ITEM_MAX_AGE_TICKS,
        },
        PickupDelay {
            ticks_remaining: pickup_delay_ticks,
        },
        BobOffset(bob_offset),
        EntityRenderPos::default(),
    ));
}

// ---------------------------------------------------------------------------
// Tick: age + pickup delay
// ---------------------------------------------------------------------------

/// Increment ages, decrement pickup delays, despawn expired entities.
pub fn tick_entity_ages(world: &mut World) {
    let mut to_despawn: Vec<Entity> = Vec::new();

    let mut query = world.query::<(Entity, &mut EntityAge, Option<&mut PickupDelay>)>();
    for (entity, mut age, pickup_delay) in query.iter_mut(world) {
        age.age_ticks += 1;
        if age.age_ticks >= age.max_age_ticks {
            to_despawn.push(entity);
        }
        if let Some(mut delay) = pickup_delay {
            delay.ticks_remaining = delay.ticks_remaining.saturating_sub(1);
        }
    }

    for entity in to_despawn {
        world.despawn(entity);
    }

    let mut to_despawn_fx: Vec<Entity> = Vec::new();
    let mut fx_query = world.query::<(Entity, &mut EntityPickupFx)>();
    for (entity, mut fx) in fx_query.iter_mut(world) {
        fx.age_ticks = fx.age_ticks.saturating_add(1);
        if fx.age_ticks >= fx.lifetime_ticks {
            to_despawn_fx.push(entity);
        }
    }
    for entity in to_despawn_fx {
        world.despawn(entity);
    }
}

// ---------------------------------------------------------------------------
// Tick: entity physics
// ---------------------------------------------------------------------------

/// Run simplified physics for all non-player entities.
pub fn tick_entity_physics(
    world: &mut World,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
) {
    let mut scratch = Vec::with_capacity(16);
    let mut rng = entity_rng();

    // Collect entities to process (avoid borrow conflict on World).
    let entities: Vec<Entity> = world
        .query_filtered::<Entity, With<EntityPhysics>>()
        .iter(world)
        .collect();

    for entity in entities {
        let Ok(mut entity_mut) = world.get_entity_mut(entity) else {
            continue;
        };

        let (transform, physics) = {
            let t = *entity_mut.get::<Transform64>().unwrap();
            let p = *entity_mut.get::<EntityPhysics>().unwrap();
            (t, p)
        };

        let mut pos = transform.position;
        let mut vel = physics.velocity;
        // Save prev_position for interpolation.
        entity_mut.get_mut::<Transform64>().unwrap().prev_position = pos;

        // 1. Apply gravity.
        vel.y -= physics.gravity;

        // 2. Solid-block ejection (Alpha: checkBlockCollisions).
        //    If the entity center is inside a solid block, push toward nearest open face.
        let center_bx = pos.x.floor() as i32;
        let center_by = pos.y.floor() as i32;
        let center_bz = pos.z.floor() as i32;
        if center_by >= 0
            && center_by < CHUNK_HEIGHT as i32
            && chunk_streamer
                .block_at_world(center_bx, center_by, center_bz)
                .is_some_and(|id| registry.is_collidable(id))
        {
            // Find nearest open face among 6 cardinal neighbors.
            let directions: [(i32, i32, i32, usize); 6] = [
                (-1, 0, 0, 0), // -X
                (1, 0, 0, 0),  // +X
                (0, -1, 0, 1), // -Y
                (0, 1, 0, 1),  // +Y
                (0, 0, -1, 2), // -Z
                (0, 0, 1, 2),  // +Z
            ];

            let mut best_dist = f64::MAX;
            let mut best_dir: (f64, f64, f64) = (0.0, 0.0, 0.0);
            let cx = pos.x - center_bx as f64;
            let cy = pos.y - center_by as f64;
            let cz = pos.z - center_bz as f64;

            for (dx, dy, dz, _) in &directions {
                let nx = center_bx + dx;
                let ny = center_by + dy;
                let nz = center_bz + dz;
                let neighbor_solid = if ny < 0 || ny >= CHUNK_HEIGHT as i32 {
                    false
                } else {
                    chunk_streamer
                        .block_at_world(nx, ny, nz)
                        .is_some_and(|id| registry.is_collidable(id))
                };
                if !neighbor_solid {
                    // Distance from entity center to the face in that direction.
                    let dist = match (*dx, *dy, *dz) {
                        (-1, 0, 0) => cx,
                        (1, 0, 0) => 1.0 - cx,
                        (0, -1, 0) => cy,
                        (0, 1, 0) => 1.0 - cy,
                        (0, 0, -1) => cz,
                        (0, 0, 1) => 1.0 - cz,
                        _ => f64::MAX,
                    };
                    if dist < best_dist {
                        best_dist = dist;
                        best_dir = (*dx as f64, *dy as f64, *dz as f64);
                    }
                }
            }

            if best_dist < f64::MAX {
                let eject_speed = rng.gen_range(ITEM_EJECTION_VEL_MIN..ITEM_EJECTION_VEL_MAX);
                vel.x += best_dir.0 * eject_speed;
                vel.y += best_dir.1 * eject_speed;
                vel.z += best_dir.2 * eject_speed;
            }
        }

        // 3. AABB sweep collision (Y → X → Z, no step-up).
        let half_w = physics.width / 2.0;
        let height = physics.height;
        let delta = vel;
        let mut resolved_delta = delta;

        // Y axis.
        let expanded_min_y = DVec3::new(
            pos.x - half_w,
            pos.y.min(pos.y + resolved_delta.y),
            pos.z - half_w,
        );
        let expanded_max_y = DVec3::new(
            pos.x + half_w,
            (pos.y + height).max(pos.y + height + resolved_delta.y),
            pos.z + half_w,
        );
        crate::app::collect_solid_block_aabbs(
            &mut scratch,
            chunk_streamer,
            registry,
            expanded_min_y,
            expanded_max_y,
        );
        let original_dy = resolved_delta.y;
        resolved_delta.y =
            crate::app::resolve_axis(pos, half_w, height, resolved_delta.y, &scratch, 1);
        pos.y += resolved_delta.y;

        // X axis.
        let expanded_min_x = DVec3::new(
            pos.x.min(pos.x + resolved_delta.x) - half_w,
            pos.y,
            pos.z - half_w,
        );
        let expanded_max_x = DVec3::new(
            pos.x.max(pos.x + resolved_delta.x) + half_w,
            pos.y + height,
            pos.z + half_w,
        );
        crate::app::collect_solid_block_aabbs(
            &mut scratch,
            chunk_streamer,
            registry,
            expanded_min_x,
            expanded_max_x,
        );
        resolved_delta.x =
            crate::app::resolve_axis(pos, half_w, height, resolved_delta.x, &scratch, 0);
        pos.x += resolved_delta.x;

        // Z axis.
        let expanded_min_z = DVec3::new(
            pos.x - half_w,
            pos.y,
            pos.z.min(pos.z + resolved_delta.z) - half_w,
        );
        let expanded_max_z = DVec3::new(
            pos.x + half_w,
            pos.y + height,
            pos.z.max(pos.z + resolved_delta.z) + half_w,
        );
        crate::app::collect_solid_block_aabbs(
            &mut scratch,
            chunk_streamer,
            registry,
            expanded_min_z,
            expanded_max_z,
        );
        resolved_delta.z =
            crate::app::resolve_axis(pos, half_w, height, resolved_delta.z, &scratch, 2);
        pos.z += resolved_delta.z;

        // 4. Ground detection.
        let on_ground = original_dy < 0.0 && resolved_delta.y != original_dy;

        // 5. Zero blocked velocity components.
        if resolved_delta.x != delta.x {
            vel.x = 0.0;
        }
        if resolved_delta.y != delta.y {
            vel.y = 0.0;
        }
        if resolved_delta.z != delta.z {
            vel.z = 0.0;
        }

        // 6. Bounce on ground.
        if on_ground {
            vel.y *= physics.bounce_factor;
        }

        // 7. Friction / drag.
        let h_friction = if on_ground {
            physics.ground_friction
        } else {
            physics.air_friction_xz
        };
        vel.x *= h_friction;
        vel.y *= physics.air_drag_y;
        vel.z *= h_friction;

        // Write back.
        let Ok(mut entity_mut) = world.get_entity_mut(entity) else {
            continue;
        };
        let mut t = entity_mut.get_mut::<Transform64>().unwrap();
        t.position = pos;
        let mut p = entity_mut.get_mut::<EntityPhysics>().unwrap();
        p.velocity = vel;
        p.on_ground = on_ground;
    }
}

// ---------------------------------------------------------------------------
// Tick: item pickup
// ---------------------------------------------------------------------------

/// Check AABB overlap between player and dropped items. Returns true if any pickup happened.
pub fn check_item_pickup(world: &mut World) -> bool {
    world.resource_scope(|world, mut inventory: Mut<'_, PlayerInventoryState>| {
        // Read player position + physics.
        let mut player_query = world
            .query_filtered::<(&Transform64, &PhysicsBody, Option<&PlayerVitals>), With<Player>>();
        let Some((player_t, player_p, player_vitals)) = player_query
            .iter(world)
            .next()
            .map(|(t, p, v)| (*t, *p, v.copied()))
        else {
            return false;
        };
        if player_vitals.is_some_and(|v| v.health <= 0) {
            return false;
        }

        let p_half_w = player_p.width / 2.0;
        let p_min = DVec3::new(
            player_t.position.x - p_half_w,
            player_t.position.y,
            player_t.position.z - p_half_w,
        );
        let p_max = DVec3::new(
            player_t.position.x + p_half_w,
            player_t.position.y + player_p.height,
            player_t.position.z + p_half_w,
        );

        let expanded_min = DVec3::new(
            p_min.x - ITEM_PICKUP_RANGE_XZ,
            p_min.y,
            p_min.z - ITEM_PICKUP_RANGE_XZ,
        );
        let expanded_max = DVec3::new(
            p_max.x + ITEM_PICKUP_RANGE_XZ,
            p_max.y,
            p_max.z + ITEM_PICKUP_RANGE_XZ,
        );
        // Collect pickup candidates.
        let mut to_despawn: Vec<Entity> = Vec::new();
        let mut to_spawn_fx: Vec<EntityPickupFx> = Vec::new();
        let mut any_pickup = false;

        let mut item_query = world.query::<(
            Entity,
            &DroppedItem,
            &Transform64,
            &PickupDelay,
            &EntityPhysics,
        )>();
        let candidates: Vec<(
            Entity,
            crate::inventory::ItemStack,
            DVec3,
            f64,
            f64,
            u32,
            f32,
        )> = item_query
            .iter(world)
            .filter(|(_, _, _, delay, _)| delay.ticks_remaining == 0)
            .filter_map(|(e, item, t, _, phys)| {
                let age = world.get::<EntityAge>(e)?;
                let bob = world.get::<BobOffset>(e)?;
                Some((
                    e,
                    item.stack,
                    t.position,
                    phys.width,
                    phys.height,
                    age.age_ticks,
                    bob.0,
                ))
            })
            .collect();

        for (entity, item_stack, item_pos, item_w, item_h, age_ticks, bob_offset) in candidates {
            let i_half_w = item_w / 2.0;
            let i_min = DVec3::new(item_pos.x - i_half_w, item_pos.y, item_pos.z - i_half_w);
            let i_max = DVec3::new(
                item_pos.x + i_half_w,
                item_pos.y + item_h,
                item_pos.z + i_half_w,
            );

            let inside_pickup_range = expanded_min.x < i_max.x
                && expanded_max.x > i_min.x
                && expanded_min.y < i_max.y
                && expanded_max.y > i_min.y
                && expanded_min.z < i_max.z
                && expanded_max.z > i_min.z;
            if !inside_pickup_range {
                continue;
            }
            if inventory.try_add_stack(item_stack) {
                to_despawn.push(entity);
                to_spawn_fx.push(EntityPickupFx {
                    item: item_stack.item,
                    start_pos: item_pos,
                    start_age_ticks: age_ticks,
                    bob_offset,
                    age_ticks: 0,
                    lifetime_ticks: ITEM_PICKUP_FX_LIFETIME_TICKS,
                    offset_y: player_p.eye_height as f32 + ITEM_PICKUP_FX_OFFSET_Y,
                });
                any_pickup = true;
            }
        }

        for entity in to_despawn {
            world.despawn(entity);
        }
        for fx in to_spawn_fx {
            world.spawn(fx);
        }
        any_pickup
    })
}

// ---------------------------------------------------------------------------
// Render interpolation system (registered in EcsRuntime render schedule)
// ---------------------------------------------------------------------------

#[allow(clippy::type_complexity)]
pub fn interpolate_entity_render_positions(
    alpha: Res<'_, RenderAlpha>,
    mut query: Query<
        '_,
        '_,
        (&Transform64, &mut EntityRenderPos),
        (With<EntityPhysics>, Without<Player>),
    >,
) {
    let blend = f64::from(alpha.0);
    for (transform, mut render_pos) in &mut query {
        let pos = transform.prev_position.lerp(transform.position, blend);
        render_pos.position = pos.as_vec3();
    }
}

// ---------------------------------------------------------------------------
// Entity mesh building (called per frame from app.rs)
// ---------------------------------------------------------------------------

/// Scale for dropped 3D block items (Alpha uses 0.25 for cubes).
const BLOCK_ITEM_SCALE: f32 = 0.25;

/// Alpha face-shade multipliers (same as chunk mesh).
const FACE_SHADE_TOP: f32 = 1.0;
const FACE_SHADE_BOTTOM: f32 = 0.5;
const FACE_SHADE_NORTH_SOUTH: f32 = 0.8;
const FACE_SHADE_EAST_WEST: f32 = 0.6;

/// The 6 faces of a unit cube centered at origin (-0.5..+0.5), with per-face
/// atlas index selector, normal offset (for `sprite_index_for_face`), and shade.
/// Each face is 4 vertices in CCW winding order (viewed from outside).
const CUBE_FACES: [CubeFace; 6] = [
    // +Y (top)
    CubeFace {
        verts: [
            [-0.5, 0.5, -0.5],
            [-0.5, 0.5, 0.5],
            [0.5, 0.5, 0.5],
            [0.5, 0.5, -0.5],
        ],
        face_offset: [0, 1, 0],
        shade: FACE_SHADE_TOP,
    },
    // -Y (bottom)
    CubeFace {
        verts: [
            [-0.5, -0.5, 0.5],
            [-0.5, -0.5, -0.5],
            [0.5, -0.5, -0.5],
            [0.5, -0.5, 0.5],
        ],
        face_offset: [0, -1, 0],
        shade: FACE_SHADE_BOTTOM,
    },
    // -Z (north)
    CubeFace {
        verts: [
            [0.5, -0.5, -0.5],
            [-0.5, -0.5, -0.5],
            [-0.5, 0.5, -0.5],
            [0.5, 0.5, -0.5],
        ],
        face_offset: [0, 0, -1],
        shade: FACE_SHADE_NORTH_SOUTH,
    },
    // +Z (south)
    CubeFace {
        verts: [
            [-0.5, -0.5, 0.5],
            [0.5, -0.5, 0.5],
            [0.5, 0.5, 0.5],
            [-0.5, 0.5, 0.5],
        ],
        face_offset: [0, 0, 1],
        shade: FACE_SHADE_NORTH_SOUTH,
    },
    // -X (west)
    CubeFace {
        verts: [
            [-0.5, -0.5, -0.5],
            [-0.5, -0.5, 0.5],
            [-0.5, 0.5, 0.5],
            [-0.5, 0.5, -0.5],
        ],
        face_offset: [-1, 0, 0],
        shade: FACE_SHADE_EAST_WEST,
    },
    // +X (east)
    CubeFace {
        verts: [
            [0.5, -0.5, 0.5],
            [0.5, -0.5, -0.5],
            [0.5, 0.5, -0.5],
            [0.5, 0.5, 0.5],
        ],
        face_offset: [1, 0, 0],
        shade: FACE_SHADE_EAST_WEST,
    },
];

struct CubeFace {
    verts: [[f32; 3]; 4],
    face_offset: [i32; 3],
    shade: f32,
}

/// Two batched meshes: one for terrain-atlas entities (blocks) and one for
/// items-atlas entities (tools). Each mesh uses a different texture binding.
pub struct EntitySpriteMeshes {
    pub terrain: ChunkMesh,
    pub items: ChunkMesh,
}

/// Build batched `ChunkMesh`es for all dropped item entities.
///
/// Solid blocks render as 3D mini-cubes (Alpha `renderAsItem` with 0.25 scale)
/// rotated around Y by age. Non-solid items render as camera-facing billboard
/// sprites. Tools render as billboard sprites using items.png atlas.
/// Lighting is sampled from the world at each entity's position.
pub fn build_entity_sprite_mesh(
    world: &mut World,
    camera_yaw: f32,
    registry: &BlockRegistry,
    tool_registry: &crate::tool::ToolRegistry,
    chunk_streamer: &ChunkStreamer,
    ambient_darkness: u8,
    render_alpha: f32,
) -> EntitySpriteMeshes {
    let mut vertices = Vec::new();
    let mut indices = Vec::new();
    let mut items_vertices = Vec::new();
    let mut items_indices = Vec::new();
    let player_interp_pos = world
        .query_filtered::<&Transform64, With<Player>>()
        .iter(world)
        .next()
        .map(|t| t.prev_position.lerp(t.position, f64::from(render_alpha)));

    let mut query = world
        .query_filtered::<(&DroppedItem, &EntityRenderPos, &EntityAge, &BobOffset), With<EntityPhysics>>();

    for (item, render_pos, age, bob) in query.iter(world) {
        // Bob offset: sin((age + partial) / 10.0 + bobOffset) * 0.1 + 0.1
        let age_f = age.age_ticks as f32 + render_alpha;
        let bob_y = (age_f / 10.0 + bob.0).sin() * 0.1 + 0.1;

        let cx = render_pos.position.x;
        let cy = render_pos.position.y + bob_y;
        let cz = render_pos.position.z;

        // Sample world light at entity position (Alpha Entity.getBrightness samples
        // at 66% of entity height; for small items this is ~entity position).
        let world_x = cx.floor() as i32;
        let world_y = (render_pos.position.y as f64 + ITEM_HEIGHT * 0.66).floor() as i32;
        let world_z = cz.floor() as i32;
        let effective_light = chunk_streamer
            .effective_light_at_world(world_x, world_y, world_z, ambient_darkness)
            .unwrap_or(15_u8.saturating_sub(ambient_darkness.min(15)));
        // Split into sky/block for the shader: put effective in both channels so
        // max(block, sky - darkness) = effective regardless of darkness uniform.
        let light_sky = effective_light;
        let light_block = effective_light;

        let tint = [255_u8, 255, 255, 255];

        // Determine block_id for rendering (tools render as billboard placeholder).
        let block_id_opt = match item.stack.item {
            crate::inventory::ItemKey::Block(id) => Some(id),
            crate::inventory::ItemKey::Tool(_) | crate::inventory::ItemKey::Item(_) => None,
        };

        if let Some(block_id) = block_id_opt {
            if registry.is_solid(block_id) {
                // 3D mini-block: Alpha rotates by (age + partial) / 20 * 57.2958 degrees.
                let spin_rad = (age_f / 20.0 + bob.0) * (std::f32::consts::PI / 180.0) * 57.295_78;
                let cos_r = spin_rad.cos();
                let sin_r = spin_rad.sin();

                for face in &CUBE_FACES {
                    let sprite = registry.sprite_index_for_face(block_id, face.face_offset);
                    let u0 = (sprite % 16) as f32 * ATLAS_TILE_UV;
                    let v0 = (sprite / 16) as f32 * ATLAS_TILE_UV;
                    let u1 = u0 + ATLAS_TILE_UV;
                    let v1 = v0 + ATLAS_TILE_UV;

                    let face_shade = (face.shade * 255.0) as u8;
                    let light = [light_sky, light_block, face_shade, 0];

                    let base = vertices.len() as u32;
                    let uvs = [[u0, v1], [u1, v1], [u1, v0], [u0, v0]];

                    for (i, vert) in face.verts.iter().enumerate() {
                        let sx = vert[0] * BLOCK_ITEM_SCALE;
                        let sy = (vert[1] + 0.5) * BLOCK_ITEM_SCALE;
                        let sz = vert[2] * BLOCK_ITEM_SCALE;
                        let rx = sx * cos_r + sz * sin_r;
                        let rz = -sx * sin_r + sz * cos_r;

                        vertices.push(MeshVertex {
                            position: [cx + rx, cy + sy, cz + rz],
                            uv: uvs[i],
                            tint_rgba: tint,
                            light_data: light,
                        });
                    }

                    indices.push(base);
                    indices.push(base + 1);
                    indices.push(base + 2);
                    indices.push(base);
                    indices.push(base + 2);
                    indices.push(base + 3);
                }
                continue;
            }
        }

        {
            // Billboard sprite for non-solid blocks and tools.
            let right_x = -camera_yaw.cos() * SPRITE_HALF_SIZE;
            let right_z = camera_yaw.sin() * SPRITE_HALF_SIZE;

            // Tools use items.png atlas; blocks use terrain.png atlas.
            let (sprite_index, verts, idxs) = match item.stack.item {
                crate::inventory::ItemKey::Block(id) => {
                    (registry.sprite_index_of(id), &mut vertices, &mut indices)
                }
                crate::inventory::ItemKey::Tool(tool_id) => {
                    let si = tool_registry.get(tool_id).map_or(0, |def| def.sprite_index);
                    (si, &mut items_vertices, &mut items_indices)
                }
                crate::inventory::ItemKey::Item(item_id) => {
                    let si = crate::inventory::alpha_item_sprite_index(item_id).unwrap_or(0);
                    (si, &mut items_vertices, &mut items_indices)
                }
            };
            let u0 = (sprite_index % 16) as f32 * ATLAS_TILE_UV;
            let v0 = (sprite_index / 16) as f32 * ATLAS_TILE_UV;
            let u1 = u0 + ATLAS_TILE_UV;
            let v1 = v0 + ATLAS_TILE_UV;

            let light = [light_sky, light_block, 255, 0];
            let base_index = verts.len() as u32;
            let bot_y = cy;
            let top_y = cy + SPRITE_HALF_SIZE * 2.0;

            verts.push(MeshVertex {
                position: [cx - right_x, bot_y, cz - right_z],
                uv: [u0, v1],
                tint_rgba: tint,
                light_data: light,
            });
            verts.push(MeshVertex {
                position: [cx + right_x, bot_y, cz + right_z],
                uv: [u1, v1],
                tint_rgba: tint,
                light_data: light,
            });
            verts.push(MeshVertex {
                position: [cx + right_x, top_y, cz + right_z],
                uv: [u1, v0],
                tint_rgba: tint,
                light_data: light,
            });
            verts.push(MeshVertex {
                position: [cx - right_x, top_y, cz - right_z],
                uv: [u0, v0],
                tint_rgba: tint,
                light_data: light,
            });

            idxs.push(base_index);
            idxs.push(base_index + 1);
            idxs.push(base_index + 2);
            idxs.push(base_index);
            idxs.push(base_index + 2);
            idxs.push(base_index + 3);
        }
    }

    // Alpha-style pickup particle path: entity -> player over 3 ticks with squared progression.
    if let Some(player_pos) = player_interp_pos {
        let mut fx_query = world.query::<&EntityPickupFx>();
        for fx in fx_query.iter(world) {
            let progress = ((f32::from(fx.age_ticks) + render_alpha)
                / f32::from(fx.lifetime_ticks))
            .clamp(0.0, 1.0);
            let progress_sq = progress * progress;
            let target = DVec3::new(
                player_pos.x,
                player_pos.y + f64::from(fx.offset_y),
                player_pos.z,
            );
            let pos = fx.start_pos.lerp(target, f64::from(progress_sq));
            let render_pos = EntityRenderPos {
                position: pos.as_vec3(),
            };
            let age = EntityAge {
                age_ticks: fx.start_age_ticks,
                max_age_ticks: ITEM_MAX_AGE_TICKS,
            };
            let bob = BobOffset(fx.bob_offset);
            match fx.item {
                crate::inventory::ItemKey::Block(block_id) => {
                    append_item_mesh(
                        &mut vertices,
                        &mut indices,
                        block_id,
                        &render_pos,
                        &age,
                        &bob,
                        camera_yaw,
                        registry,
                        chunk_streamer,
                        ambient_darkness,
                        render_alpha,
                    );
                }
                crate::inventory::ItemKey::Tool(tool_id) => {
                    // Render tool pickup FX as billboard into items-atlas mesh.
                    let sprite_index = tool_registry.get(tool_id).map_or(0, |def| def.sprite_index);
                    append_billboard_sprite(
                        &mut items_vertices,
                        &mut items_indices,
                        &render_pos,
                        &age,
                        &bob,
                        camera_yaw,
                        sprite_index,
                        chunk_streamer,
                        ambient_darkness,
                        render_alpha,
                    );
                }
                crate::inventory::ItemKey::Item(item_id) => {
                    let sprite_index =
                        crate::inventory::alpha_item_sprite_index(item_id).unwrap_or(0);
                    append_billboard_sprite(
                        &mut items_vertices,
                        &mut items_indices,
                        &render_pos,
                        &age,
                        &bob,
                        camera_yaw,
                        sprite_index,
                        chunk_streamer,
                        ambient_darkness,
                        render_alpha,
                    );
                }
            }
        }
    }

    EntitySpriteMeshes {
        terrain: ChunkMesh { vertices, indices },
        items: ChunkMesh {
            vertices: items_vertices,
            indices: items_indices,
        },
    }
}

/// Append a billboard sprite quad into vertex/index buffers (used for both
/// terrain-atlas and items-atlas billboard sprites).
#[allow(clippy::too_many_arguments)]
fn append_billboard_sprite(
    vertices: &mut Vec<MeshVertex>,
    indices: &mut Vec<u32>,
    render_pos: &EntityRenderPos,
    age: &EntityAge,
    bob: &BobOffset,
    camera_yaw: f32,
    sprite_index: u16,
    chunk_streamer: &ChunkStreamer,
    ambient_darkness: u8,
    render_alpha: f32,
) {
    let age_f = age.age_ticks as f32 + render_alpha;
    let bob_y = (age_f / 10.0 + bob.0).sin() * 0.1 + 0.1;

    let cx = render_pos.position.x;
    let cy = render_pos.position.y + bob_y;
    let cz = render_pos.position.z;

    let world_x = cx.floor() as i32;
    let world_y = (render_pos.position.y as f64 + ITEM_HEIGHT * 0.66).floor() as i32;
    let world_z = cz.floor() as i32;
    let effective_light = chunk_streamer
        .effective_light_at_world(world_x, world_y, world_z, ambient_darkness)
        .unwrap_or(15_u8.saturating_sub(ambient_darkness.min(15)));

    let right_x = -camera_yaw.cos() * SPRITE_HALF_SIZE;
    let right_z = camera_yaw.sin() * SPRITE_HALF_SIZE;

    let u0 = (sprite_index % 16) as f32 * ATLAS_TILE_UV;
    let v0 = (sprite_index / 16) as f32 * ATLAS_TILE_UV;
    let u1 = u0 + ATLAS_TILE_UV;
    let v1 = v0 + ATLAS_TILE_UV;

    let tint = [255_u8, 255, 255, 255];
    let light = [effective_light, effective_light, 255, 0];
    let base = vertices.len() as u32;
    let bot_y = cy;
    let top_y = cy + SPRITE_HALF_SIZE * 2.0;

    vertices.push(MeshVertex {
        position: [cx - right_x, bot_y, cz - right_z],
        uv: [u0, v1],
        tint_rgba: tint,
        light_data: light,
    });
    vertices.push(MeshVertex {
        position: [cx + right_x, bot_y, cz + right_z],
        uv: [u1, v1],
        tint_rgba: tint,
        light_data: light,
    });
    vertices.push(MeshVertex {
        position: [cx + right_x, top_y, cz + right_z],
        uv: [u1, v0],
        tint_rgba: tint,
        light_data: light,
    });
    vertices.push(MeshVertex {
        position: [cx - right_x, top_y, cz - right_z],
        uv: [u0, v0],
        tint_rgba: tint,
        light_data: light,
    });

    indices.push(base);
    indices.push(base + 1);
    indices.push(base + 2);
    indices.push(base);
    indices.push(base + 2);
    indices.push(base + 3);
}

#[allow(clippy::too_many_arguments)]
fn append_item_mesh(
    vertices: &mut Vec<MeshVertex>,
    indices: &mut Vec<u32>,
    block_id: u8,
    render_pos: &EntityRenderPos,
    age: &EntityAge,
    bob: &BobOffset,
    camera_yaw: f32,
    registry: &BlockRegistry,
    chunk_streamer: &ChunkStreamer,
    ambient_darkness: u8,
    render_alpha: f32,
) {
    // Bob offset: sin((age + partial) / 10.0 + bobOffset) * 0.1 + 0.1
    let age_f = age.age_ticks as f32 + render_alpha;
    let bob_y = (age_f / 10.0 + bob.0).sin() * 0.1 + 0.1;

    let cx = render_pos.position.x;
    let cy = render_pos.position.y + bob_y;
    let cz = render_pos.position.z;

    // Sample world light at entity position (Alpha Entity.getBrightness samples
    // at 66% of entity height; for small items this is ~entity position).
    let world_x = cx.floor() as i32;
    let world_y = (render_pos.position.y as f64 + ITEM_HEIGHT * 0.66).floor() as i32;
    let world_z = cz.floor() as i32;
    let effective_light = chunk_streamer
        .effective_light_at_world(world_x, world_y, world_z, ambient_darkness)
        .unwrap_or(15_u8.saturating_sub(ambient_darkness.min(15)));
    // Split into sky/block for the shader: put effective in both channels so
    // max(block, sky - darkness) = effective regardless of darkness uniform.
    let light_sky = effective_light;
    let light_block = effective_light;

    let tint = [255_u8, 255, 255, 255];

    if registry.is_solid(block_id) {
        // 3D mini-block: Alpha rotates by (age + partial) / 20 * 57.2958 degrees.
        let spin_rad = (age_f / 20.0 + bob.0) * (std::f32::consts::PI / 180.0) * 57.295_78;
        let cos_r = spin_rad.cos();
        let sin_r = spin_rad.sin();

        for face in &CUBE_FACES {
            let sprite = registry.sprite_index_for_face(block_id, face.face_offset);
            let u0 = (sprite % 16) as f32 * ATLAS_TILE_UV;
            let v0 = (sprite / 16) as f32 * ATLAS_TILE_UV;
            let u1 = u0 + ATLAS_TILE_UV;
            let v1 = v0 + ATLAS_TILE_UV;

            let face_shade = (face.shade * 255.0) as u8;
            let light = [light_sky, light_block, face_shade, 0];

            let base = vertices.len() as u32;
            let uvs = [[u0, v1], [u1, v1], [u1, v0], [u0, v0]];

            for (i, vert) in face.verts.iter().enumerate() {
                // Scale then shift so bottom face sits at Y=0 (Alpha does
                // glTranslate(-0.5,-0.5,-0.5) then glScale(0.25) so the
                // cube occupies 0..0.25 in Y, not -0.125..+0.125).
                let sx = vert[0] * BLOCK_ITEM_SCALE;
                let sy = (vert[1] + 0.5) * BLOCK_ITEM_SCALE;
                let sz = vert[2] * BLOCK_ITEM_SCALE;
                let rx = sx * cos_r + sz * sin_r;
                let rz = -sx * sin_r + sz * cos_r;

                vertices.push(MeshVertex {
                    position: [cx + rx, cy + sy, cz + rz],
                    uv: uvs[i],
                    tint_rgba: tint,
                    light_data: light,
                });
            }

            indices.push(base);
            indices.push(base + 1);
            indices.push(base + 2);
            indices.push(base);
            indices.push(base + 2);
            indices.push(base + 3);
        }
    } else {
        // Billboard sprite for non-solid items (torches, flowers, etc.).
        let right_x = -camera_yaw.cos() * SPRITE_HALF_SIZE;
        let right_z = camera_yaw.sin() * SPRITE_HALF_SIZE;

        let sprite_index = registry.sprite_index_of(block_id);
        let u0 = (sprite_index % 16) as f32 * ATLAS_TILE_UV;
        let v0 = (sprite_index / 16) as f32 * ATLAS_TILE_UV;
        let u1 = u0 + ATLAS_TILE_UV;
        let v1 = v0 + ATLAS_TILE_UV;

        let light = [light_sky, light_block, 255, 0];
        let base_index = vertices.len() as u32;
        let bot_y = cy;
        let top_y = cy + SPRITE_HALF_SIZE * 2.0;

        vertices.push(MeshVertex {
            position: [cx - right_x, bot_y, cz - right_z],
            uv: [u0, v1],
            tint_rgba: tint,
            light_data: light,
        });
        vertices.push(MeshVertex {
            position: [cx + right_x, bot_y, cz + right_z],
            uv: [u1, v1],
            tint_rgba: tint,
            light_data: light,
        });
        vertices.push(MeshVertex {
            position: [cx + right_x, top_y, cz + right_z],
            uv: [u1, v0],
            tint_rgba: tint,
            light_data: light,
        });
        vertices.push(MeshVertex {
            position: [cx - right_x, top_y, cz - right_z],
            uv: [u0, v0],
            tint_rgba: tint,
            light_data: light,
        });

        indices.push(base_index);
        indices.push(base_index + 1);
        indices.push(base_index + 2);
        indices.push(base_index);
        indices.push(base_index + 2);
        indices.push(base_index + 3);
    }
}

// ---------------------------------------------------------------------------
// Entity shadow mesh building
// ---------------------------------------------------------------------------

/// Alpha `ItemRenderer` shadow size (half-extent in blocks).
const SHADOW_SIZE: f32 = 0.15;
/// Alpha `ItemRenderer` shadow base darkness.
const SHADOW_DARKNESS: f32 = 0.75;

/// Alpha brightness curve: converts effective light level (0–15) to 0.05–1.0.
fn alpha_brightness(light_level: u8) -> f32 {
    let g = 1.0 - f32::from(light_level.min(15)) / 15.0;
    ((1.0 - g) / (g * 3.0 + 1.0)) * 0.95 + 0.05
}

/// Build a batched `ChunkMesh` of shadow quads for all dropped item entities.
///
/// Each shadow is a flat quad projected onto the highest solid surface below
/// the entity, matching Alpha 1.2.6 `EntityRenderer` shadow rendering.
pub fn build_entity_shadow_mesh(
    world: &mut World,
    chunk_streamer: &ChunkStreamer,
    registry: &BlockRegistry,
    ambient_darkness: u8,
    camera_pos: [f32; 3],
    render_alpha: f32,
) -> ChunkMesh {
    let mut vertices = Vec::new();
    let mut indices = Vec::new();

    let cam = Vec3::from(camera_pos);

    let mut query =
        world.query_filtered::<(&EntityRenderPos, &EntityAge, &BobOffset), With<EntityPhysics>>();

    for (render_pos, age, bob) in query.iter(world) {
        // Replicate the bob offset used by the sprite mesh so shadow lines up.
        let age_f = age.age_ticks as f32 + render_alpha;
        let bob_y = (age_f / 10.0 + bob.0).sin() * 0.1 + 0.1;

        let ex = render_pos.position.x;
        let ey = render_pos.position.y + bob_y;
        let ez = render_pos.position.z;

        // Scan downward to find the highest solid block surface (max 16 blocks).
        let start_by = (ey - 0.01).floor() as i32;
        let mut ground_block_y: Option<i32> = None;
        for by in (start_by.saturating_sub(16)..=start_by).rev() {
            if by < 0 || by >= CHUNK_HEIGHT as i32 {
                continue;
            }
            let bx = ex.floor() as i32;
            let bz = ez.floor() as i32;
            if chunk_streamer
                .block_at_world(bx, by, bz)
                .is_some_and(|id| registry.is_collidable(id))
            {
                ground_block_y = Some(by);
                break;
            }
        }

        let Some(solid_y) = ground_block_y else {
            continue;
        };

        // Shadow quad Y: top of solid block + z-fighting offset.
        let ground_y = solid_y as f32 + 1.0 + 1.0 / 64.0;

        // Distance fade: baseFade = (1 - sqDist/256) * shadowDarkness.
        let dx = ex - cam.x;
        let dy = ey - cam.y;
        let dz = ez - cam.z;
        let sq_dist = dx * dx + dy * dy + dz * dz;
        let dist_fade = (1.0 - sq_dist / 256.0).clamp(0.0, 1.0);

        // Height fade: reduces when entity is high above ground.
        let height_above = ey - ground_y;
        let height_fade = (1.0 - height_above / 2.0).clamp(0.0, 1.0);

        // Ground brightness from world light at shadow position.
        let light_x = ex.floor() as i32;
        let light_y = (ground_y - 1.0 / 64.0).floor() as i32 + 1; // block above solid
        let light_z = ez.floor() as i32;
        let effective_light = chunk_streamer
            .effective_light_at_world(light_x, light_y, light_z, ambient_darkness)
            .unwrap_or(0);
        let ground_brightness = alpha_brightness(effective_light);

        // Final opacity: Alpha formula.
        let opacity = dist_fade * SHADOW_DARKNESS * 0.5 * height_fade * ground_brightness;
        if opacity <= 0.001 {
            continue;
        }
        let alpha_byte = (opacity.clamp(0.0, 1.0) * 255.0) as u8;

        // Emit flat quad centered on entity XZ at ground_y.
        let base = vertices.len() as u32;
        let x0 = ex - SHADOW_SIZE;
        let x1 = ex + SHADOW_SIZE;
        let z0 = ez - SHADOW_SIZE;
        let z1 = ez + SHADOW_SIZE;

        let tint = [255_u8, 255, 255, alpha_byte];
        let light = [0_u8, 0, 0, 0]; // unused by shadow shader

        vertices.push(MeshVertex {
            position: [x0, ground_y, z0],
            uv: [0.0, 0.0],
            tint_rgba: tint,
            light_data: light,
        });
        vertices.push(MeshVertex {
            position: [x1, ground_y, z0],
            uv: [1.0, 0.0],
            tint_rgba: tint,
            light_data: light,
        });
        vertices.push(MeshVertex {
            position: [x1, ground_y, z1],
            uv: [1.0, 1.0],
            tint_rgba: tint,
            light_data: light,
        });
        vertices.push(MeshVertex {
            position: [x0, ground_y, z1],
            uv: [0.0, 1.0],
            tint_rgba: tint,
            light_data: light,
        });

        indices.push(base);
        indices.push(base + 1);
        indices.push(base + 2);
        indices.push(base);
        indices.push(base + 2);
        indices.push(base + 3);
    }

    ChunkMesh { vertices, indices }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ecs::Player;

    fn spawn_test_player(world: &mut World, position: DVec3) {
        world.spawn((Player, Transform64::new(position), PhysicsBody::default()));
    }

    #[test]
    fn spawn_creates_entity_with_all_components() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_dropped_item(&mut world, 1, DVec3::new(5.0, 65.0, 5.0));

        let mut query = world.query::<(
            &DroppedItem,
            &Transform64,
            &EntityPhysics,
            &EntityAge,
            &PickupDelay,
            &BobOffset,
            &EntityRenderPos,
        )>();
        let results: Vec<_> = query.iter(&world).collect();
        assert_eq!(results.len(), 1);

        let (item, transform, physics, age, delay, _bob, _rp) = results[0];
        assert_eq!(item.stack.item, crate::inventory::ItemKey::Block(1));
        assert_eq!(item.stack.count, 1);
        assert_eq!(transform.position, DVec3::new(5.0, 65.0, 5.0));
        assert!((physics.gravity - ITEM_GRAVITY).abs() < 1e-10);
        assert_eq!(age.age_ticks, 0);
        assert_eq!(age.max_age_ticks, ITEM_MAX_AGE_TICKS);
        assert_eq!(delay.ticks_remaining, ITEM_PICKUP_DELAY_TICKS);
    }

    #[test]
    fn age_increments_and_despawns() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_dropped_item(&mut world, 1, DVec3::ZERO);

        // Set age near max to test despawn.
        {
            let mut query = world.query::<&mut EntityAge>();
            for mut age in query.iter_mut(&mut world) {
                age.age_ticks = ITEM_MAX_AGE_TICKS - 1;
            }
        }

        tick_entity_ages(&mut world);

        // Entity should be despawned.
        let count = world.query::<&DroppedItem>().iter(&world).count();
        assert_eq!(count, 0);
    }

    #[test]
    fn pickup_delay_decrements() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_dropped_item(&mut world, 1, DVec3::ZERO);

        tick_entity_ages(&mut world);

        let mut query = world.query::<&PickupDelay>();
        let delay = query.iter(&world).next().unwrap();
        assert_eq!(delay.ticks_remaining, ITEM_PICKUP_DELAY_TICKS - 1);
    }

    #[test]
    fn player_drop_uses_longer_pickup_delay() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_player_dropped_item_stack(
            &mut world,
            crate::inventory::ItemStack::block(1, 1),
            DVec3::ZERO,
            0.0,
            0.0,
        );

        let mut query = world.query::<&PickupDelay>();
        let delay = query.iter(&world).next().unwrap();
        assert_eq!(delay.ticks_remaining, ITEM_PLAYER_DROP_PICKUP_DELAY_TICKS);
    }

    #[test]
    fn player_drop_throw_uses_forward_look_direction_convention() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        // yaw = +90deg should throw toward +X in ThingCraft camera convention.
        spawn_player_dropped_item_stack(
            &mut world,
            crate::inventory::ItemStack::block(1, 1),
            DVec3::ZERO,
            std::f64::consts::FRAC_PI_2,
            0.0,
        );

        let mut query = world.query::<&EntityPhysics>();
        let physics = query.iter(&world).next().unwrap();
        assert!(physics.velocity.x > 0.0);
    }

    #[test]
    fn player_drop_throw_respects_pitch_sign() {
        let mut up_world = World::new();
        up_world.insert_resource(RenderAlpha(0.0));
        spawn_player_dropped_item_stack(
            &mut up_world,
            crate::inventory::ItemStack::block(1, 1),
            DVec3::ZERO,
            0.0,
            std::f64::consts::FRAC_PI_4,
        );
        let vy_up = up_world
            .query::<&EntityPhysics>()
            .iter(&up_world)
            .next()
            .unwrap()
            .velocity
            .y;

        let mut down_world = World::new();
        down_world.insert_resource(RenderAlpha(0.0));
        spawn_player_dropped_item_stack(
            &mut down_world,
            crate::inventory::ItemStack::block(1, 1),
            DVec3::ZERO,
            0.0,
            -std::f64::consts::FRAC_PI_4,
        );
        let vy_down = down_world
            .query::<&EntityPhysics>()
            .iter(&down_world)
            .next()
            .unwrap()
            .velocity
            .y;

        assert!(
            vy_up > vy_down,
            "looking up should produce higher drop arc than looking down"
        );
    }

    #[test]
    fn solid_block_mesh_produces_cube_geometry() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        // Spawn 3 solid-block items (stone = id 1).
        for i in 0..3 {
            spawn_dropped_item(&mut world, 1, DVec3::new(i as f64, 65.0, 0.0));
        }
        for mut rp in world.query::<&mut EntityRenderPos>().iter_mut(&mut world) {
            rp.position = Vec3::new(1.0, 65.0, 0.0);
        }

        let registry = BlockRegistry::alpha_1_2_6();
        let streamer = ChunkStreamer::new(
            42,
            BlockRegistry::alpha_1_2_6(),
            crate::streaming::ResidencyConfig::default(),
        );
        let tool_reg = crate::tool::ToolRegistry::alpha_1_2_6();
        let mesh =
            build_entity_sprite_mesh(&mut world, 0.0, &registry, &tool_reg, &streamer, 0, 0.0);

        // 6 faces * 4 verts = 24 per solid block, 6 faces * 6 indices = 36
        assert_eq!(mesh.terrain.vertices.len(), 3 * 24);
        assert_eq!(mesh.terrain.indices.len(), 3 * 36);
    }

    #[test]
    fn non_solid_block_mesh_produces_billboard() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        // Spawn a torch (id 50, non-solid).
        spawn_dropped_item(&mut world, 50, DVec3::new(0.0, 65.0, 0.0));
        for mut rp in world.query::<&mut EntityRenderPos>().iter_mut(&mut world) {
            rp.position = Vec3::new(1.0, 65.0, 0.0);
        }

        let registry = BlockRegistry::alpha_1_2_6();
        let streamer = ChunkStreamer::new(
            42,
            BlockRegistry::alpha_1_2_6(),
            crate::streaming::ResidencyConfig::default(),
        );
        let tool_reg = crate::tool::ToolRegistry::alpha_1_2_6();
        let mesh =
            build_entity_sprite_mesh(&mut world, 0.0, &registry, &tool_reg, &streamer, 0, 0.0);

        // Billboard: 4 verts, 6 indices
        assert_eq!(mesh.terrain.vertices.len(), 4);
        assert_eq!(mesh.terrain.indices.len(), 6);
    }

    #[test]
    fn pickup_collects_in_alpha_expanded_range_and_spawns_fx() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_test_player(&mut world, DVec3::new(0.0, 64.0, 0.0));
        // Just outside player AABB, but inside Alpha expanded pickup range.
        spawn_dropped_item(&mut world, 3, DVec3::new(1.20, 64.2, 0.0));
        for mut delay in world.query::<&mut PickupDelay>().iter_mut(&mut world) {
            delay.ticks_remaining = 0;
        }

        let mut inventory = PlayerInventoryState::alpha_defaults();
        // Ensure stack is mergeable so pickup succeeds.
        let _ = inventory.apply(crate::inventory::InventoryCommand::SelectHotbar { index: 0 });
        world.insert_resource(inventory);
        let picked = check_item_pickup(&mut world);
        assert!(picked);
        assert_eq!(world.query::<&DroppedItem>().iter(&world).count(), 0);
        assert_eq!(world.query::<&EntityPickupFx>().iter(&world).count(), 1);
    }

    #[test]
    fn pickup_fx_expires_after_lifetime() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_test_player(&mut world, DVec3::new(0.0, 64.0, 0.0));
        spawn_dropped_item(&mut world, 3, DVec3::new(1.20, 64.2, 0.0));
        for mut delay in world.query::<&mut PickupDelay>().iter_mut(&mut world) {
            delay.ticks_remaining = 0;
        }
        world.insert_resource(PlayerInventoryState::alpha_defaults());
        assert!(check_item_pickup(&mut world));
        assert_eq!(world.query::<&EntityPickupFx>().iter(&world).count(), 1);

        for _ in 0..ITEM_PICKUP_FX_LIFETIME_TICKS {
            tick_entity_ages(&mut world);
        }
        assert_eq!(world.query::<&EntityPickupFx>().iter(&world).count(), 0);
    }

    #[test]
    fn drop_pickup_preserves_tool_metadata() {
        let mut world = World::new();
        world.insert_resource(RenderAlpha(0.0));
        spawn_test_player(&mut world, DVec3::new(0.0, 64.0, 0.0));

        let mut dropped = crate::inventory::ItemStack::tool(270);
        dropped.metadata = 7;
        spawn_player_dropped_item_stack(&mut world, dropped, DVec3::new(0.0, 64.2, 0.0), 0.0, 0.0);
        for mut delay in world.query::<&mut PickupDelay>().iter_mut(&mut world) {
            delay.ticks_remaining = 0;
        }

        world.insert_resource(PlayerInventoryState::alpha_defaults());
        assert!(check_item_pickup(&mut world));

        let inv = world.resource::<PlayerInventoryState>();
        let found = inv
            .hotbar_stack(0)
            .into_iter()
            .chain(inv.hotbar_stack(1))
            .chain(inv.hotbar_stack(2))
            .chain(inv.hotbar_stack(3))
            .chain(inv.hotbar_stack(4))
            .chain(inv.hotbar_stack(5))
            .chain(inv.hotbar_stack(6))
            .chain(inv.hotbar_stack(7))
            .chain(inv.hotbar_stack(8))
            .chain(inv.main_stacks().iter().copied().flatten())
            .find(|s| s.item == crate::inventory::ItemKey::Tool(270) && s.metadata == 7);
        assert!(found.is_some(), "picked tool metadata should be preserved");
    }
}
