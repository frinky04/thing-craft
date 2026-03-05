use super::*;

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub(super) enum DropRule {
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
pub(super) enum PlacementRule {
    Default,
    PlantSoil,
    SolidSupport,
    SugarCane,
    Cactus,
    SnowLayer,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub(super) enum SurvivalRule {
    None,
    Flower,
    Mushroom,
    SugarCane,
    Cactus,
    SnowLayer,
    Sapling,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub(super) struct BlockBehavior {
    pub(super) drop_rule: DropRule,
    pub(super) placement_rule: PlacementRule,
    pub(super) survival_rule: SurvivalRule,
}

pub(super) const DEFAULT_BLOCK_BEHAVIOR: BlockBehavior = BlockBehavior {
    drop_rule: DropRule::SelfBlock,
    placement_rule: PlacementRule::Default,
    survival_rule: SurvivalRule::None,
};

pub(super) fn block_behavior(block_id: u8) -> BlockBehavior {
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
        GLASS_BLOCK_ID | BOOKSHELF_BLOCK_ID | MOB_SPAWNER_BLOCK_ID | ICE_BLOCK_ID => {
            BlockBehavior {
                drop_rule: DropRule::None,
                ..DEFAULT_BLOCK_BEHAVIOR
            }
        }
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

pub(super) fn can_place_plant_on(block_id: u8) -> bool {
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

pub(super) fn can_sugar_cane_survive(
    chunk_streamer: &ChunkStreamer,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
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

pub(super) fn can_snow_layer_survive(
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

pub(super) fn can_cactus_survive(
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

pub(super) fn block_drop_stack(
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

pub(super) fn random_tick_lcg_next(random_tick_lcg: &mut i32) -> i32 {
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

pub(super) fn break_block_with_drop(
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
    if chunk_streamer.block_at_world(neighbor_x, neighbor_y, neighbor_z)
        != Some(OAK_LEAVES_BLOCK_ID)
    {
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
        if meta != 0 && meta > cur {
            meta
        } else {
            cur
        }
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

pub(super) fn tick_random_block_at(
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
                update_leaves_log_proximity(
                    chunk_streamer,
                    registry,
                    x,
                    y,
                    z,
                    &mut updates_this_tick,
                )
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
                update_leaves_log_proximity(
                    chunk_streamer,
                    registry,
                    x,
                    y,
                    z,
                    &mut updates_this_tick,
                )
            } else {
                false
            }
        }
        SAPLING_BLOCK_ID => {
            if chunk_streamer
                .raw_brightness_at_world(x, y + 1, z)
                .unwrap_or(0)
                < 9
            {
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
                chunk_streamer.set_block_with_metadata_at_world(x, y, z, CACTUS_BLOCK_ID, meta + 1)
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

pub(super) fn tick_random_blocks(
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

pub(super) fn apply_post_edit_support_rules(
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

pub(super) fn break_block_and_collect_drop(
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
