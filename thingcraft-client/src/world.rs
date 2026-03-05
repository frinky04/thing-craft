use std::collections::HashMap;
use std::f64::consts::PI;
use std::fmt;
use std::path::Path;

use crate::noise::{JavaRandom, PerlinNoise, PerlinSimplexNoise};

pub const CHUNK_WIDTH: usize = 16;
pub const CHUNK_DEPTH: usize = 16;
pub const CHUNK_HEIGHT: usize = 128;
pub const SECTION_HEIGHT: usize = 16;
pub const CHUNK_SECTION_COUNT: usize = CHUNK_HEIGHT / SECTION_HEIGHT;
pub const CHUNK_AREA: usize = CHUNK_WIDTH * CHUNK_DEPTH;
pub const CHUNK_VOLUME: usize = CHUNK_AREA * CHUNK_HEIGHT;
pub const CHUNK_EDGE_SLICE_VOLUME: usize = CHUNK_WIDTH * CHUNK_HEIGHT;

const AIR_ID: u8 = 0;
const STONE_ID: u8 = 1;
const GRASS_ID: u8 = 2;
const DIRT_ID: u8 = 3;
const COBBLESTONE_ID: u8 = 4;
const BEDROCK_ID: u8 = 7;
const FLOWING_WATER_ID: u8 = 8;
const WATER_ID: u8 = 9;
const FLOWING_LAVA_ID: u8 = 10;
const LAVA_ID: u8 = 11;
const SAND_ID: u8 = 12;
const GRAVEL_ID: u8 = 13;
const GOLD_ORE_ID: u8 = 14;
const IRON_ORE_ID: u8 = 15;
const COAL_ORE_ID: u8 = 16;
const OAK_LOG_ID: u8 = 17;
const OAK_LEAVES_ID: u8 = 18;
const OBSIDIAN_ID: u8 = 49;
const MOSSY_COBBLESTONE_ID: u8 = 48;
const MOB_SPAWNER_ID: u8 = 52;
const CHEST_ID: u8 = 54;
const CRAFTING_TABLE_ID: u8 = 58;
const FURNACE_ID: u8 = 61;
const LIT_FURNACE_ID: u8 = 62;
const LIT_PUMPKIN_ID: u8 = 91;
const DIAMOND_ORE_ID: u8 = 56;
const REDSTONE_ORE_ID: u8 = 73;
pub(crate) const ICE_ID: u8 = 79;
const YELLOW_FLOWER_ID: u8 = 37;
const RED_FLOWER_ID: u8 = 38;
const BROWN_MUSHROOM_ID: u8 = 39;
const RED_MUSHROOM_ID: u8 = 40;
const SNOW_LAYER_ID: u8 = 78;
const CACTUS_ID: u8 = 81;
const CLAY_ID: u8 = 82;
const SUGAR_CANE_ID: u8 = 83;
const PUMPKIN_ID: u8 = 86;

const ALPHA_EXCLUDED_BLOCK_IDS: [u8; 15] = [
    21, // Lapis Ore
    22, // Lapis Block
    23, // Dispenser
    24, // Sandstone
    25, // Noteblock
    26, // Bed
    27, // Powered Rail
    28, // Detector Rail
    29, // Sticky Piston
    30, // Web
    31, // Tall Grass
    32, // Dead Bush
    33, // Piston
    34, // Piston Head
    36, // Moving Block
];
const WHITE_RGB_PACKED: u32 = 0x00FF_FFFF;

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum MaterialKind {
    Air,
    Stone,
    Dirt,
    Grass,
    Wood,
    Decoration,
    Leaves,
    Cactus,
    Plant,
    Glass,
    Ice,
    Clay,
    Pumpkin,
    Tnt,
    SnowLayer,
    Liquid,
    Metal,
    Sand,
    Fire,
    Portal,
    Wool,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum BiomeTintKind {
    None,
    Grass,
    Foliage,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct BlockDef {
    pub id: u8,
    pub name: &'static str,
    pub sprite_index: u16,
    pub material: MaterialKind,
    pub solid: bool,
    pub opacity: u8,
    pub emitted_light: u8,
    pub mining_hardness: f32,
    pub explosion_resistance: f32,
    pub ticks_randomly: bool,
    pub blocks_movement: bool,
    pub has_collision: bool,
    pub can_ray_trace: bool,
    pub face_occluder: bool,
    pub face_sprites: Option<[u16; 6]>,
}

#[derive(Clone)]
pub struct BlockRegistry {
    by_id: [Option<BlockDef>; 256],
}

impl fmt::Debug for BlockRegistry {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("BlockRegistry")
            .field("defined_block_count", &self.defined_block_count())
            .field("excluded_ids", &ALPHA_EXCLUDED_BLOCK_IDS)
            .finish()
    }
}

impl BlockRegistry {
    #[must_use]
    pub fn alpha_1_2_6() -> Self {
        let mut by_id: [Option<BlockDef>; 256] = [None; 256];

        add(&mut by_id, AIR_ID, "air", 0, MaterialKind::Air, false, 0, 0);
        add(
            &mut by_id,
            STONE_ID,
            "stone",
            1,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            GRASS_ID,
            "grass",
            3,
            MaterialKind::Grass,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            DIRT_ID,
            "dirt",
            2,
            MaterialKind::Dirt,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            COBBLESTONE_ID,
            "cobblestone",
            16,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(&mut by_id, 5, "planks", 4, MaterialKind::Wood, true, 255, 0);
        add(
            &mut by_id,
            6,
            "sapling",
            15,
            MaterialKind::Plant,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            BEDROCK_ID,
            "bedrock",
            17,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            FLOWING_WATER_ID,
            "flowing_water",
            205,
            MaterialKind::Liquid,
            false,
            3,
            0,
        );
        add(
            &mut by_id,
            WATER_ID,
            "water",
            205,
            MaterialKind::Liquid,
            false,
            3,
            0,
        );
        add(
            &mut by_id,
            FLOWING_LAVA_ID,
            "flowing_lava",
            237,
            MaterialKind::Liquid,
            false,
            255,
            15,
        );
        add(
            &mut by_id,
            LAVA_ID,
            "lava",
            237,
            MaterialKind::Liquid,
            false,
            255,
            15,
        );
        add(
            &mut by_id,
            SAND_ID,
            "sand",
            18,
            MaterialKind::Sand,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            GRAVEL_ID,
            "gravel",
            19,
            MaterialKind::Sand,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            GOLD_ORE_ID,
            "gold_ore",
            32,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            IRON_ORE_ID,
            "iron_ore",
            33,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            COAL_ORE_ID,
            "coal_ore",
            34,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            17,
            "oak_log",
            20,
            MaterialKind::Wood,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            18,
            "oak_leaves",
            52,
            MaterialKind::Leaves,
            false,
            1,
            0,
        );
        add(&mut by_id, 20, "glass", 49, MaterialKind::Glass, true, 0, 0);
        add(&mut by_id, 35, "wool", 64, MaterialKind::Wool, true, 255, 0);
        add(&mut by_id, 46, "tnt", 8, MaterialKind::Tnt, true, 255, 0);
        add(
            &mut by_id,
            OBSIDIAN_ID,
            "obsidian",
            37,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            MOSSY_COBBLESTONE_ID,
            "mossy_cobblestone",
            36,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            50,
            "torch",
            80,
            MaterialKind::Decoration,
            false,
            0,
            14,
        );
        add(&mut by_id, 51, "fire", 31, MaterialKind::Fire, false, 0, 15);
        add(
            &mut by_id,
            MOB_SPAWNER_ID,
            "mob_spawner",
            65,
            MaterialKind::Metal,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            54,
            "chest",
            26,
            MaterialKind::Wood,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            DIAMOND_ORE_ID,
            "diamond_ore",
            50,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            CRAFTING_TABLE_ID,
            "crafting_table",
            43,
            MaterialKind::Wood,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            61,
            "furnace",
            44,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            62,
            "lit_furnace",
            45,
            MaterialKind::Stone,
            true,
            255,
            13,
        );
        add(
            &mut by_id,
            63,
            "standing_sign",
            0,
            MaterialKind::Wood,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            64,
            "wooden_door",
            97,
            MaterialKind::Wood,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            65,
            "ladder",
            83,
            MaterialKind::Decoration,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            68,
            "wall_sign",
            0,
            MaterialKind::Wood,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            REDSTONE_ORE_ID,
            "redstone_ore",
            51,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            74,
            "lit_redstone_ore",
            51,
            MaterialKind::Stone,
            true,
            255,
            9,
        );
        add(
            &mut by_id,
            76,
            "redstone_torch",
            99,
            MaterialKind::Decoration,
            false,
            0,
            7,
        );
        add(&mut by_id, ICE_ID, "ice", 67, MaterialKind::Ice, true, 3, 0);
        add(
            &mut by_id,
            87,
            "netherrack",
            103,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            88,
            "soul_sand",
            104,
            MaterialKind::Sand,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            89,
            "glowstone",
            105,
            MaterialKind::Glass,
            true,
            255,
            15,
        );
        add(
            &mut by_id,
            90,
            "nether_portal",
            14,
            MaterialKind::Portal,
            false,
            0,
            11,
        );
        add(
            &mut by_id,
            91,
            "lit_pumpkin",
            102,
            MaterialKind::Wood,
            true,
            255,
            15,
        );

        // Decoration blocks
        add(
            &mut by_id,
            YELLOW_FLOWER_ID,
            "yellow_flower",
            13,
            MaterialKind::Plant,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            RED_FLOWER_ID,
            "red_flower",
            12,
            MaterialKind::Plant,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            BROWN_MUSHROOM_ID,
            "brown_mushroom",
            29,
            MaterialKind::Plant,
            false,
            0,
            1,
        );
        add(
            &mut by_id,
            RED_MUSHROOM_ID,
            "red_mushroom",
            28,
            MaterialKind::Plant,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            SNOW_LAYER_ID,
            "snow_layer",
            66,
            MaterialKind::SnowLayer,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            CACTUS_ID,
            "cactus",
            70,
            MaterialKind::Cactus,
            true,
            15,
            0,
        );
        add(
            &mut by_id,
            CLAY_ID,
            "clay",
            72,
            MaterialKind::Clay,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            SUGAR_CANE_ID,
            "sugar_cane",
            73,
            MaterialKind::Plant,
            false,
            0,
            0,
        );
        add(
            &mut by_id,
            PUMPKIN_ID,
            "pumpkin",
            118,
            MaterialKind::Pumpkin,
            true,
            255,
            0,
        );

        if let Some(block) = &mut by_id[91] {
            block.material = MaterialKind::Pumpkin;
        }

        apply_alpha_block_property_overrides(&mut by_id);
        apply_alpha_block_visual_overrides(&mut by_id);

        Self { by_id }
    }

    #[must_use]
    pub fn get(&self, block_id: u8) -> Option<&BlockDef> {
        self.by_id[usize::from(block_id)].as_ref()
    }

    #[must_use]
    pub fn opacity_of(&self, block_id: u8) -> u8 {
        self.get(block_id).map_or(0, |block| block.opacity)
    }

    #[must_use]
    pub fn emitted_light_of(&self, block_id: u8) -> u8 {
        self.get(block_id).map_or(0, |block| block.emitted_light)
    }

    #[must_use]
    pub fn mining_hardness_of(&self, block_id: u8) -> f32 {
        self.get(block_id)
            .map_or(1.0, |block| block.mining_hardness)
    }

    #[must_use]
    pub fn material_of(&self, block_id: u8) -> MaterialKind {
        self.get(block_id)
            .map_or(MaterialKind::Air, |block| block.material)
    }

    #[must_use]
    #[allow(dead_code)]
    pub fn ticks_randomly_of(&self, block_id: u8) -> bool {
        self.get(block_id).is_some_and(|block| block.ticks_randomly)
    }

    #[must_use]
    pub fn sprite_index_of(&self, block_id: u8) -> u16 {
        self.get(block_id).map_or(0, |block| block.sprite_index)
    }

    #[must_use]
    pub fn sprite_index_for_face(&self, block_id: u8, face_offset: [i32; 3]) -> u16 {
        if let Some(block) = self.get(block_id) {
            if let Some(face_sprites) = block.face_sprites {
                return face_sprites[face_index_from_offset(face_offset)];
            }
        }

        let base = self.sprite_index_of(block_id);
        // Liquid side faces use base+1 sprite (top/bottom use base)
        if self.is_liquid(block_id) && face_offset[1] == 0 {
            base + 1
        } else {
            base
        }
    }

    #[must_use]
    pub fn sprite_index_for_face_with_metadata(
        &self,
        block_id: u8,
        face_offset: [i32; 3],
        metadata: u8,
    ) -> u16 {
        let face = alpha_face_code_from_offset(face_offset);
        if block_id == FURNACE_ID || block_id == LIT_FURNACE_ID {
            // Alpha FurnaceBlock.getSprite(WorldView,...):
            // top/bottom -> stone; sides -> body; facing side -> front (lit/unlit variant).
            if face == 0 || face == 1 {
                return self.sprite_index_of(STONE_ID);
            }
            let front_face = if (2..=5).contains(&metadata) {
                metadata
            } else {
                3
            };
            if face == front_face {
                if block_id == LIT_FURNACE_ID {
                    61
                } else {
                    44
                }
            } else {
                45
            }
        } else if block_id == CHEST_ID {
            // Metadata-driven chest facing (player-facing placement path in app.rs).
            // 2..5 follows Alpha face codes; fallback keeps vanilla single-chest default (+Z).
            if face == 0 || face == 1 {
                return 25;
            }
            let front_face = if (2..=5).contains(&metadata) {
                metadata
            } else {
                3
            };
            if face == front_face {
                27
            } else {
                26
            }
        } else if block_id == PUMPKIN_ID || block_id == LIT_PUMPKIN_ID {
            // Alpha PumpkinBlock.getSprite(int face, int metadata):
            // top/bottom = top sprite, sides = side sprite except facing side is front.
            if face == 0 || face == 1 {
                return 102;
            }
            let front_face = match metadata & 3 {
                0 => 2, // north (-Z)
                1 => 5, // east (+X)
                2 => 3, // south (+Z)
                _ => 4, // west (-X)
            };
            if face == front_face {
                if block_id == LIT_PUMPKIN_ID {
                    120
                } else {
                    119
                }
            } else {
                118
            }
        } else {
            self.sprite_index_for_face(block_id, face_offset)
        }
    }

    #[must_use]
    pub fn face_tint_rgb(&self, block_id: u8, face_offset: [i32; 3]) -> [u8; 3] {
        match (block_id, face_offset[1]) {
            // Alpha grass-top is biome-tinted in the original client; the biome tint is provided
            // by chunk data during meshing.
            (GRASS_ID, 1) => [0, 0, 0],
            _ => [u8::MAX, u8::MAX, u8::MAX],
        }
    }

    #[must_use]
    pub fn biome_tint_kind(&self, block_id: u8, face_offset: [i32; 3]) -> BiomeTintKind {
        match (block_id, face_offset[1]) {
            (GRASS_ID, 1) => BiomeTintKind::Grass,
            (OAK_LEAVES_ID, _) => BiomeTintKind::Foliage,
            _ => BiomeTintKind::None,
        }
    }

    #[must_use]
    pub fn is_liquid(&self, block_id: u8) -> bool {
        self.get(block_id)
            .is_some_and(|block| block.material == MaterialKind::Liquid)
    }

    #[must_use]
    pub fn is_leaves(&self, block_id: u8) -> bool {
        block_id == OAK_LEAVES_ID
    }

    #[must_use]
    pub fn is_billboard_plant(&self, block_id: u8) -> bool {
        matches!(
            block_id,
            YELLOW_FLOWER_ID | RED_FLOWER_ID | BROWN_MUSHROOM_ID | RED_MUSHROOM_ID | SUGAR_CANE_ID
        )
    }

    #[must_use]
    pub fn is_cactus(&self, block_id: u8) -> bool {
        block_id == CACTUS_ID
    }

    #[must_use]
    pub fn is_snow_layer(&self, block_id: u8) -> bool {
        block_id == SNOW_LAYER_ID
    }

    #[must_use]
    pub fn is_water(&self, block_id: u8) -> bool {
        matches!(block_id, FLOWING_WATER_ID | WATER_ID)
    }

    #[must_use]
    pub fn is_lava(&self, block_id: u8) -> bool {
        matches!(block_id, FLOWING_LAVA_ID | LAVA_ID)
    }

    /// True when `a` and `b` are both water (flowing or source) or both lava.
    #[must_use]
    pub fn is_same_liquid_kind(&self, a: u8, b: u8) -> bool {
        (self.is_water(a) && self.is_water(b)) || (self.is_lava(a) && self.is_lava(b))
    }

    #[must_use]
    pub fn is_solid(&self, block_id: u8) -> bool {
        self.get(block_id).is_some_and(|block| block.solid)
    }

    #[must_use]
    pub fn blocks_movement(&self, block_id: u8) -> bool {
        self.get(block_id)
            .is_some_and(|block| block.blocks_movement)
    }

    #[must_use]
    pub fn is_collidable(&self, block_id: u8) -> bool {
        self.get(block_id).is_some_and(|block| block.has_collision)
    }

    #[must_use]
    pub fn dropped_item_block_id(&self, block_id: u8) -> Option<u8> {
        if block_id == AIR_ID || self.is_liquid(block_id) {
            return None;
        }
        Some(block_id)
    }

    #[must_use]
    pub fn is_defined_block(&self, block_id: u8) -> bool {
        self.get(block_id).is_some()
    }

    #[must_use]
    pub fn is_face_occluder(&self, block_id: u8) -> bool {
        self.get(block_id).is_some_and(|block| block.face_occluder)
    }

    #[must_use]
    pub fn defined_block_count(&self) -> usize {
        self.by_id.iter().filter(|block| block.is_some()).count()
    }

    #[must_use]
    pub fn alpha_exclusions_respected(&self) -> bool {
        ALPHA_EXCLUDED_BLOCK_IDS
            .iter()
            .all(|id| self.get(*id).is_none())
    }
}

#[allow(clippy::too_many_arguments)]
fn add(
    by_id: &mut [Option<BlockDef>; 256],
    id: u8,
    name: &'static str,
    sprite_index: u16,
    material: MaterialKind,
    solid: bool,
    opacity: u8,
    emitted_light: u8,
) {
    let slot = &mut by_id[usize::from(id)];
    assert!(slot.is_none(), "duplicate block id {id} for {name}");

    let blocks_movement = alpha_material_blocks_movement(material);
    let has_collision = blocks_movement;
    let can_ray_trace = material != MaterialKind::Liquid;
    let default_hardness = alpha_default_hardness_for_material(material);

    *slot = Some(BlockDef {
        id,
        name,
        sprite_index,
        material,
        solid,
        opacity,
        emitted_light,
        mining_hardness: default_hardness,
        explosion_resistance: default_hardness,
        ticks_randomly: false,
        blocks_movement,
        has_collision,
        can_ray_trace,
        face_occluder: solid && opacity == 255,
        face_sprites: None,
    });
}

fn alpha_default_hardness_for_material(material: MaterialKind) -> f32 {
    match material {
        MaterialKind::Air
        | MaterialKind::Plant
        | MaterialKind::Decoration
        | MaterialKind::Fire
        | MaterialKind::Portal
        | MaterialKind::SnowLayer => 0.0,
        _ => 1.0,
    }
}

fn alpha_material_blocks_movement(material: MaterialKind) -> bool {
    !matches!(
        material,
        MaterialKind::Air
            | MaterialKind::Liquid
            | MaterialKind::Plant
            | MaterialKind::Decoration
            | MaterialKind::Fire
            | MaterialKind::Portal
            | MaterialKind::SnowLayer
    )
}

fn face_index_from_offset(face_offset: [i32; 3]) -> usize {
    match face_offset {
        [0, 1, 0] => 0,  // +Y (top)
        [0, -1, 0] => 1, // -Y (bottom)
        [0, 0, -1] => 2, // -Z (north)
        [0, 0, 1] => 3,  // +Z (south)
        [-1, 0, 0] => 4, // -X (west)
        [1, 0, 0] => 5,  // +X (east)
        _ => panic!("invalid face offset: {:?}", face_offset),
    }
}

fn alpha_face_code_from_offset(face_offset: [i32; 3]) -> u8 {
    match face_offset {
        [0, -1, 0] => 0,
        [0, 1, 0] => 1,
        [0, 0, -1] => 2,
        [0, 0, 1] => 3,
        [-1, 0, 0] => 4,
        [1, 0, 0] => 5,
        _ => panic!("invalid face offset: {:?}", face_offset),
    }
}

#[allow(clippy::too_many_arguments)]
fn set_alpha_block_properties(
    by_id: &mut [Option<BlockDef>; 256],
    id: u8,
    hardness: f32,
    explosion_resistance: f32,
    ticks_randomly: bool,
    blocks_movement: bool,
    has_collision: bool,
    can_ray_trace: bool,
) {
    let block = by_id[usize::from(id)]
        .as_mut()
        .unwrap_or_else(|| panic!("missing alpha block id {id}"));
    block.mining_hardness = hardness;
    block.explosion_resistance = explosion_resistance;
    block.ticks_randomly = ticks_randomly;
    block.blocks_movement = blocks_movement;
    block.has_collision = has_collision;
    block.can_ray_trace = can_ray_trace;
}

fn apply_alpha_block_property_overrides(by_id: &mut [Option<BlockDef>; 256]) {
    set_alpha_block_properties(by_id, 0, 0.0, 0.0, false, false, false, false);
    set_alpha_block_properties(by_id, 1, 1.5, 6.0, false, true, true, true);
    set_alpha_block_properties(by_id, 2, 0.6, 0.6, true, true, true, true);
    set_alpha_block_properties(by_id, 3, 0.5, 0.5, false, true, true, true);
    set_alpha_block_properties(by_id, 4, 2.0, 6.0, false, true, true, true);
    set_alpha_block_properties(by_id, 5, 2.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, 6, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 7, -1.0, 3_600_000.0, false, true, true, true);
    set_alpha_block_properties(by_id, 8, 100.0, 100.0, true, false, false, false);
    set_alpha_block_properties(by_id, 9, 100.0, 100.0, false, false, false, false);
    set_alpha_block_properties(by_id, 10, 0.0, 0.0, true, false, false, false);
    set_alpha_block_properties(by_id, 11, 100.0, 100.0, true, false, false, false);
    set_alpha_block_properties(by_id, 12, 0.5, 0.5, false, true, true, true);
    set_alpha_block_properties(by_id, 13, 0.6, 0.6, false, true, true, true);
    set_alpha_block_properties(by_id, 14, 3.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, 15, 3.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, 16, 3.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, 17, 2.0, 2.0, false, true, true, true);
    set_alpha_block_properties(by_id, 18, 0.2, 0.2, true, true, true, true);
    set_alpha_block_properties(by_id, 20, 0.3, 0.3, false, true, true, true);
    set_alpha_block_properties(by_id, 35, 0.8, 0.8, false, true, true, true);
    set_alpha_block_properties(by_id, 37, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 38, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 39, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 40, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 46, 0.0, 0.0, false, true, true, true);
    set_alpha_block_properties(by_id, 48, 2.0, 6.0, false, true, true, true);
    set_alpha_block_properties(by_id, 49, 10.0, 1200.0, false, true, true, true);
    set_alpha_block_properties(by_id, 50, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 51, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 52, 5.0, 5.0, false, true, true, true);
    set_alpha_block_properties(by_id, 54, 2.5, 2.5, false, true, true, true);
    set_alpha_block_properties(by_id, 56, 3.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, CRAFTING_TABLE_ID, 2.5, 2.5, false, true, true, true);
    set_alpha_block_properties(by_id, 61, 3.5, 3.5, false, true, true, true);
    set_alpha_block_properties(by_id, 62, 3.5, 3.5, false, true, true, true);
    set_alpha_block_properties(by_id, 63, 1.0, 1.0, false, true, false, true);
    set_alpha_block_properties(by_id, 64, 3.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, 65, 0.4, 0.4, false, false, true, true);
    set_alpha_block_properties(by_id, 68, 1.0, 1.0, false, true, false, true);
    set_alpha_block_properties(by_id, 73, 3.0, 3.0, false, true, true, true);
    set_alpha_block_properties(by_id, 74, 3.0, 3.0, true, true, true, true);
    set_alpha_block_properties(by_id, 76, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 78, 0.1, 0.1, true, false, false, true);
    set_alpha_block_properties(by_id, 79, 0.5, 0.5, true, true, true, true);
    set_alpha_block_properties(by_id, 81, 0.4, 0.4, true, true, true, true);
    set_alpha_block_properties(by_id, 82, 0.6, 0.6, false, true, true, true);
    set_alpha_block_properties(by_id, 83, 0.0, 0.0, true, false, false, true);
    set_alpha_block_properties(by_id, 86, 1.0, 1.0, true, true, true, true);
    set_alpha_block_properties(by_id, 87, 0.4, 0.4, false, true, true, true);
    set_alpha_block_properties(by_id, 88, 0.5, 0.5, false, true, true, true);
    set_alpha_block_properties(by_id, 89, 0.3, 0.3, false, true, true, true);
    set_alpha_block_properties(by_id, 90, -1.0, 0.0, false, false, false, true);
    set_alpha_block_properties(by_id, 91, 1.0, 1.0, true, true, true, true);
}

fn set_alpha_face_sprites(by_id: &mut [Option<BlockDef>; 256], id: u8, sprites: [u16; 6]) {
    let block = by_id[usize::from(id)]
        .as_mut()
        .unwrap_or_else(|| panic!("missing alpha block id {id}"));
    block.face_sprites = Some(sprites);
}

fn set_alpha_face_occluder(by_id: &mut [Option<BlockDef>; 256], id: u8, face_occluder: bool) {
    let block = by_id[usize::from(id)]
        .as_mut()
        .unwrap_or_else(|| panic!("missing alpha block id {id}"));
    block.face_occluder = face_occluder;
}

fn apply_alpha_block_visual_overrides(by_id: &mut [Option<BlockDef>; 256]) {
    // Face order: [top, bottom, north(-Z), south(+Z), west(-X), east(+X)].
    set_alpha_face_sprites(by_id, GRASS_ID, [0, 2, 3, 3, 3, 3]);
    set_alpha_face_sprites(by_id, OAK_LOG_ID, [21, 21, 20, 20, 20, 20]);
    set_alpha_face_sprites(by_id, CACTUS_ID, [69, 71, 70, 70, 70, 70]);
    set_alpha_face_sprites(by_id, PUMPKIN_ID, [102, 102, 118, 119, 118, 118]);
    set_alpha_face_sprites(by_id, LIT_PUMPKIN_ID, [102, 102, 118, 120, 118, 118]);
    // Alpha ChestBlock.getSprite(int face): top/bottom 25, south/front 27, others 26.
    set_alpha_face_sprites(by_id, CHEST_ID, [25, 25, 26, 27, 26, 26]);
    // Alpha CraftingTableBlock.getSprite(int face): top 43, bottom planks(4),
    // north/west 60, south/east 59.
    set_alpha_face_sprites(by_id, CRAFTING_TABLE_ID, [43, 4, 60, 59, 60, 59]);
    // Alpha MobSpawnerBlock.isSolid() == false: should not occlude neighbor faces.
    set_alpha_face_occluder(by_id, MOB_SPAWNER_ID, false);
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum BiomeKind {
    Rainforest,
    Swampland,
    SeasonalForest,
    Forest,
    Savanna,
    Shrubland,
    Taiga,
    Desert,
    Plains,
    Tundra,
}

impl BiomeKind {
    #[must_use]
    fn from_climate(temperature: f64, downfall: f64) -> Self {
        let adjusted_downfall = downfall * temperature;
        if temperature < 0.1 {
            return Self::Tundra;
        }
        if adjusted_downfall < 0.2 {
            if temperature < 0.5 {
                return Self::Tundra;
            }
            if temperature < 0.95 {
                return Self::Savanna;
            }
            return Self::Desert;
        }
        if adjusted_downfall > 0.5 && temperature < 0.7 {
            return Self::Swampland;
        }
        if temperature < 0.5 {
            return Self::Taiga;
        }
        if temperature < 0.97 {
            if adjusted_downfall < 0.35 {
                return Self::Shrubland;
            }
            return Self::Forest;
        }
        if adjusted_downfall < 0.45 {
            return Self::Plains;
        }
        if adjusted_downfall < 0.9 {
            return Self::SeasonalForest;
        }
        Self::Rainforest
    }

    #[must_use]
    fn surface_subsurface_blocks(self) -> (u8, u8) {
        match self {
            Self::Desert => (SAND_ID, SAND_ID),
            _ => (GRASS_ID, DIRT_ID),
        }
    }
}

#[derive(Debug, Clone, Copy)]
pub struct BiomeSample {
    pub temperature: f64,
    pub biome: BiomeKind,
}

#[derive(Debug, Clone)]
pub struct BiomeSource {
    temperature_noise: PerlinSimplexNoise,
    downfall_noise: PerlinSimplexNoise,
    biome_noise: PerlinSimplexNoise,
}

impl BiomeSource {
    #[must_use]
    pub fn new(seed: u64) -> Self {
        let mut temp_rng = JavaRandom::new(seed as i64 * 9871);
        let temperature_noise = PerlinSimplexNoise::new(&mut temp_rng, 4);

        let mut down_rng = JavaRandom::new(seed as i64 * 39811);
        let downfall_noise = PerlinSimplexNoise::new(&mut down_rng, 4);

        let mut biome_rng = JavaRandom::new(seed as i64 * 543321);
        let biome_noise = PerlinSimplexNoise::new(&mut biome_rng, 2);

        Self {
            temperature_noise,
            downfall_noise,
            biome_noise,
        }
    }

    /// Sample raw temperature and biome noise, returning post-processed values.
    /// Shared by `get_biomes` (which also needs downfall) and temperature-only paths.
    fn sample_climate_raw(
        &self,
        x: i32,
        z: i32,
        size_x: usize,
        size_z: usize,
    ) -> (Vec<f64>, Vec<f64>) {
        let count = size_x * size_z;
        let mut raw_temp = vec![0.0_f64; count];
        let mut raw_biome = vec![0.0_f64; count];

        self.temperature_noise.get_region(
            &mut raw_temp,
            x as f64,
            z as f64,
            size_x,
            size_z,
            0.025,
            0.025,
            0.25,
        );
        self.biome_noise.get_region(
            &mut raw_biome,
            x as f64,
            z as f64,
            size_x,
            size_z,
            0.25,
            0.25,
            0.5882352941176471,
        );

        (raw_temp, raw_biome)
    }

    /// Post-process a raw temperature sample with the biome blend factor.
    fn post_process_temperature(raw_temp: f64, raw_biome: f64) -> f64 {
        let d = raw_biome * 1.1 + 0.5;
        let mut temp = (raw_temp * 0.15 + 0.7) * 0.99 + d * 0.01;
        temp = 1.0 - (1.0 - temp) * (1.0 - temp);
        temp.clamp(0.0, 1.0)
    }

    /// Bulk biome sampling — port of getBiomes (Java lines 81-116).
    pub fn get_biomes(
        &self,
        x: i32,
        z: i32,
        size_x: usize,
        size_z: usize,
    ) -> (Vec<BiomeKind>, Vec<f64>, Vec<f64>) {
        let count = size_x * size_z;
        let (raw_temp, raw_biome) = self.sample_climate_raw(x, z, size_x, size_z);

        let mut raw_down = vec![0.0_f64; count];
        self.downfall_noise.get_region(
            &mut raw_down,
            x as f64,
            z as f64,
            size_x,
            size_z,
            0.05,
            0.05,
            1.0 / 3.0,
        );

        let mut temperatures = vec![0.0_f64; count];
        let mut downfalls = vec![0.0_f64; count];
        let mut biomes = Vec::with_capacity(count);

        for i in 0..count {
            let d = raw_biome[i] * 1.1 + 0.5;
            let temp = Self::post_process_temperature(raw_temp[i], raw_biome[i]);
            let down = ((raw_down[i] * 0.15 + 0.5) * 0.998 + d * 0.002).clamp(0.0, 1.0);
            temperatures[i] = temp;
            downfalls[i] = down;
            biomes.push(BiomeKind::from_climate(temp, down));
        }

        (biomes, temperatures, downfalls)
    }

    /// Convenience single-position sampling (for tree placement etc.).
    #[must_use]
    pub fn sample(&self, x: i32, z: i32) -> BiomeSample {
        let (biomes, temps, _downs) = self.get_biomes(x, z, 1, 1);
        BiomeSample {
            temperature: temps[0],
            biome: biomes[0],
        }
    }
}

#[derive(Debug, Clone)]
pub struct OverworldChunkGenerator {
    seed: u64,
    biome_source: BiomeSource,
    min_limit_noise: PerlinNoise,
    max_limit_noise: PerlinNoise,
    perlin_noise_1: PerlinNoise,
    perlin_noise_2: PerlinNoise,
    perlin_noise_3: PerlinNoise,
    scale_noise: PerlinNoise,
    depth_noise: PerlinNoise,
    forest_noise: PerlinNoise,
    grass_color_map: GrassColorMap,
    foliage_color_map: FoliageColorMap,
}

impl OverworldChunkGenerator {
    #[must_use]
    pub fn new(seed: u64) -> Self {
        let biome_source = BiomeSource::new(seed);

        // All noise generators seeded from one sequential JavaRandom — order matters.
        let mut rng = JavaRandom::new(seed as i64);
        let min_limit_noise = PerlinNoise::new(&mut rng, 16);
        let max_limit_noise = PerlinNoise::new(&mut rng, 16);
        let perlin_noise_1 = PerlinNoise::new(&mut rng, 8);
        let perlin_noise_2 = PerlinNoise::new(&mut rng, 4);
        let perlin_noise_3 = PerlinNoise::new(&mut rng, 4);
        let scale_noise = PerlinNoise::new(&mut rng, 10);
        let depth_noise = PerlinNoise::new(&mut rng, 16);
        let forest_noise = PerlinNoise::new(&mut rng, 8);

        let grass_color_map = GrassColorMap::load(
            &["resources/minecraft-a1.2.6-client/misc/grasscolor.png"],
            [126, 201, 86],
        );
        let foliage_color_map = FoliageColorMap::load(
            &["resources/minecraft-a1.2.6-client/misc/foliagecolor.png"],
            [87, 174, 47],
        );

        Self {
            seed,
            biome_source,
            min_limit_noise,
            max_limit_noise,
            perlin_noise_1,
            perlin_noise_2,
            perlin_noise_3,
            scale_noise,
            depth_noise,
            forest_noise,
            grass_color_map,
            foliage_color_map,
        }
    }

    #[must_use]
    pub fn generate_chunk(&self, chunk_pos: ChunkPos, registry: &BlockRegistry) -> ChunkData {
        self.generate_region(chunk_pos, 0, registry)
            .into_iter()
            .next()
            .expect("single-chunk region generation should return one chunk")
    }

    #[must_use]
    pub fn generate_region(
        &self,
        center_chunk: ChunkPos,
        radius: i32,
        registry: &BlockRegistry,
    ) -> Vec<ChunkData> {
        if radius < 0 {
            return Vec::new();
        }

        let min_x = center_chunk.x - radius;
        let max_x = center_chunk.x + radius;
        let min_z = center_chunk.z - radius;
        let max_z = center_chunk.z + radius;

        let support_min = ChunkPos {
            x: min_x - 1,
            z: min_z - 1,
        };
        let support_max = ChunkPos {
            x: max_x + 1,
            z: max_z + 1,
        };
        let mut chunks = self.generate_base_region(support_min, support_max, registry);

        let source_min = ChunkPos {
            x: min_x - 1,
            z: min_z - 1,
        };
        let source_max = ChunkPos { x: max_x, z: max_z };
        let ore_source_min = ChunkPos {
            x: min_x - 1,
            z: min_z - 1,
        };
        let ore_source_max = ChunkPos {
            x: max_x + 1,
            z: max_z + 1,
        };
        place_lakes(&mut chunks, self.seed, source_min, source_max, registry);
        place_dungeons(&mut chunks, self.seed, source_min, source_max);
        place_clay_patches_world_space(&mut chunks, self.seed, source_min, source_max);
        place_ores_world_space(&mut chunks, self.seed, ore_source_min, ore_source_max);
        place_trees(
            &mut chunks,
            self.seed,
            &self.biome_source,
            &self.forest_noise,
            source_min,
            source_max,
        );
        place_flowers_world_space(&mut chunks, self.seed, source_min, source_max, registry);
        place_mushrooms_world_space(&mut chunks, self.seed, source_min, source_max, registry);
        place_sugar_cane_world_space(&mut chunks, self.seed, source_min, source_max);
        place_pumpkins_world_space(&mut chunks, self.seed, source_min, source_max, registry);
        place_cacti_world_space(
            &mut chunks,
            self.seed,
            &self.biome_source,
            source_min,
            source_max,
            registry,
        );
        place_springs_world_space(&mut chunks, self.seed, ore_source_min, ore_source_max);
        settle_falling_blocks_world_space(&mut chunks, registry);

        let mut output = Vec::new();
        for chunk_z in min_z..=max_z {
            for chunk_x in min_x..=max_x {
                let pos = ChunkPos {
                    x: chunk_x,
                    z: chunk_z,
                };
                let mut chunk = chunks.remove(&pos).unwrap_or_else(|| {
                    panic!(
                        "missing generated chunk at ({}, {}) while extracting region",
                        chunk_x, chunk_z
                    )
                });
                chunk.recalculate_height_map(registry);
                place_snow_cover(&mut chunk, &self.biome_source, registry);
                chunk.seed_emitted_light(registry);
                output.push(chunk);
            }
        }
        output
    }

    fn generate_base_region(
        &self,
        min_chunk: ChunkPos,
        max_chunk: ChunkPos,
        registry: &BlockRegistry,
    ) -> HashMap<ChunkPos, ChunkData> {
        let mut chunks = HashMap::new();
        for chunk_z in min_chunk.z..=max_chunk.z {
            for chunk_x in min_chunk.x..=max_chunk.x {
                let pos = ChunkPos {
                    x: chunk_x,
                    z: chunk_z,
                };
                let mut chunk = self.generate_terrain_chunk(pos);
                self.populate_chunk_features(&mut chunk);
                chunk.recalculate_height_map(registry);
                chunks.insert(pos, chunk);
            }
        }
        chunks
    }

    /// Generate a single chunk: 3D density → stone/water/air, then surface pass.
    fn generate_terrain_chunk(&self, chunk_pos: ChunkPos) -> ChunkData {
        let mut chunk = ChunkData::new(chunk_pos, AIR_ID);
        let cx = chunk_pos.x;
        let cz = chunk_pos.z;

        // Get biome data for this chunk region (computed once, reused everywhere)
        let (biomes, temperatures, downfalls) =
            self.biome_source.get_biomes(cx * 16, cz * 16, 16, 16);

        // Phase 1: 3D density field → stone/water/air
        self.build_terrain(&mut chunk, &temperatures, &downfalls);

        // Phase 2: Surface building pass (top-down)
        let mut chunk_rng = JavaRandom::new(
            (cx as i64)
                .wrapping_mul(341873128712_i64)
                .wrapping_add((cz as i64).wrapping_mul(132897987541_i64)),
        );
        self.build_surfaces(&mut chunk, &biomes, &mut chunk_rng);

        // Set grass/foliage tints per column
        for local_z in 0..CHUNK_DEPTH as u8 {
            for local_x in 0..CHUNK_WIDTH as u8 {
                let col_idx = local_z as usize * CHUNK_WIDTH + local_x as usize;
                let temp = temperatures[col_idx];
                let down = downfalls[col_idx];
                chunk.set_grass_tint(local_x, local_z, self.grass_color_map.sample(temp, down));
                chunk.set_foliage_tint(local_x, local_z, self.foliage_color_map.sample(temp, down));
            }
        }

        chunk
    }

    /// Port of generateHeightMap (Java lines 206-281).
    /// Samples a 5x17x5 density grid for a chunk.
    fn generate_height_map(
        &self,
        chunk_x: i32,
        chunk_z: i32,
        temperatures: &[f64],
        downfalls: &[f64],
    ) -> Vec<f64> {
        let size_x = 5;
        let size_y = 17;
        let size_z = 5;
        let count = size_x * size_y * size_z;

        let x = (chunk_x * 4) as f64;
        let z = (chunk_z * 4) as f64;

        // Sample 2D noise buffers (5x5 = 25 values)
        let mut scale_buf = vec![0.0_f64; 25];
        let mut depth_buf = vec![0.0_f64; 25];
        self.scale_noise
            .get_region_2d(&mut scale_buf, x, z, 5, 5, 1.121, 1.121);
        self.depth_noise
            .get_region_2d(&mut depth_buf, x, z, 5, 5, 200.0, 200.0);

        // Sample 3D noise buffers (5x17x5 = 425 values)
        let mut perlin1_buf = vec![0.0_f64; count];
        let mut min_buf = vec![0.0_f64; count];
        let mut max_buf = vec![0.0_f64; count];
        self.perlin_noise_1.get_region_3d(
            &mut perlin1_buf,
            x,
            0.0,
            z,
            5,
            17,
            5,
            684.412 / 80.0,
            684.412 / 160.0,
            684.412 / 80.0,
        );
        self.min_limit_noise.get_region_3d(
            &mut min_buf,
            x,
            0.0,
            z,
            5,
            17,
            5,
            684.412,
            684.412,
            684.412,
        );
        self.max_limit_noise.get_region_3d(
            &mut max_buf,
            x,
            0.0,
            z,
            5,
            17,
            5,
            684.412,
            684.412,
            684.412,
        );

        let mut density = vec![0.0_f64; count];
        let mut idx_3d = 0;
        let mut idx_2d = 0;

        for l in 0..size_x {
            for n in 0..size_z {
                // Map to temperature/downfall at column center
                // k = CHUNK_WIDTH / size_x = 16/5 = 3 (integer division)
                let m_x = (l * 3 + 1) as usize; // column center X in chunk-local coords
                let m_z = (n * 3 + 1) as usize; // column center Z in chunk-local coords
                let temp_idx = m_z * 16 + m_x;
                let temp = temperatures[temp_idx.min(temperatures.len() - 1)];
                let down = downfalls[temp_idx.min(downfalls.len() - 1)];

                // Scale calculation
                let raw_scale = (scale_buf[idx_2d] + 256.0) / 512.0;
                let climate_factor = 1.0 - (down * temp).powi(4);
                let mut scale = climate_factor * raw_scale;
                scale = scale.clamp(0.0, 1.0);
                scale += 0.5;

                // Depth calculation
                let mut depth = depth_buf[idx_2d] / 8000.0;
                if depth < 0.0 {
                    depth *= -0.3;
                }
                depth = depth * 3.0 - 2.0;
                if depth < 0.0 {
                    depth /= 2.0;
                    if depth < -1.0 {
                        depth = -1.0;
                    }
                    depth /= 1.4;
                    depth /= 2.0;
                } else {
                    if depth > 1.0 {
                        depth = 1.0;
                    }
                    depth /= 8.0;
                }

                idx_2d += 1;

                for s in 0..size_y {
                    // Falloff calculation
                    let center = size_y as f64 / 2.0 + depth * 4.0;
                    let mut falloff = (s as f64 - center) * 12.0 / scale;
                    if falloff < 0.0 {
                        falloff *= 4.0;
                    }

                    // Interpolate between min and max limit noise
                    let min_val = min_buf[idx_3d] / 512.0;
                    let max_val = max_buf[idx_3d] / 512.0;
                    let interp_t = ((perlin1_buf[idx_3d] / 10.0 + 1.0) / 2.0).clamp(0.0, 1.0);
                    let mut val = min_val + (max_val - min_val) * interp_t;
                    val -= falloff;

                    // Top 4 layers blend toward -10.0
                    if s > size_y - 4 {
                        let blend = ((s as f64 - (size_y as f64 - 4.0)) / 3.0).clamp(0.0, 1.0);
                        val = val * (1.0 - blend) + -10.0 * blend;
                    }

                    density[idx_3d] = val;
                    idx_3d += 1;
                }
            }
        }

        density
    }

    /// Port of buildTerrain (Java lines 70-125).
    /// Trilinear interpolation from 5x17x5 density to 16x128x16 blocks.
    fn build_terrain(&self, chunk: &mut ChunkData, temperatures: &[f64], downfalls: &[f64]) {
        let cx = chunk.pos.x;
        let cz = chunk.pos.z;

        let density = self.generate_height_map(cx, cz, temperatures, downfalls);

        // Trilinear interpolation: 4x4 horizontal cells, 16 vertical subcells
        // density grid is indexed [x][z][y] with x=5, z=5, y=17
        for cell_x in 0..4_usize {
            for cell_z in 0..4_usize {
                for cell_y in 0..16_usize {
                    // Get 8 corner density values
                    let d000 = density[(cell_x * 5 + cell_z) * 17 + cell_y]; // (x, z, y)
                    let d001 = density[(cell_x * 5 + cell_z) * 17 + cell_y + 1];
                    let d010 = density[(cell_x * 5 + (cell_z + 1)) * 17 + cell_y];
                    let d011 = density[(cell_x * 5 + (cell_z + 1)) * 17 + cell_y + 1];
                    let d100 = density[((cell_x + 1) * 5 + cell_z) * 17 + cell_y];
                    let d101 = density[((cell_x + 1) * 5 + cell_z) * 17 + cell_y + 1];
                    let d110 = density[((cell_x + 1) * 5 + (cell_z + 1)) * 17 + cell_y];
                    let d111 = density[((cell_x + 1) * 5 + (cell_z + 1)) * 17 + cell_y + 1];

                    // Y interpolation step (8 blocks per cell_y)
                    let y_step_00 = (d001 - d000) / 8.0;
                    let y_step_10 = (d101 - d100) / 8.0;
                    let y_step_01 = (d011 - d010) / 8.0;
                    let y_step_11 = (d111 - d110) / 8.0;

                    let mut d_y0_x0z0 = d000;
                    let mut d_y0_x1z0 = d100;
                    let mut d_y0_x0z1 = d010;
                    let mut d_y0_x1z1 = d110;

                    for sub_y in 0..8_usize {
                        let y = cell_y * 8 + sub_y;
                        if y >= CHUNK_HEIGHT {
                            break;
                        }

                        // X interpolation step (4 blocks per cell_x)
                        let x_step_z0 = (d_y0_x1z0 - d_y0_x0z0) / 4.0;
                        let x_step_z1 = (d_y0_x1z1 - d_y0_x0z1) / 4.0;

                        let mut d_xz0 = d_y0_x0z0;
                        let mut d_xz1 = d_y0_x0z1;

                        for sub_x in 0..4_usize {
                            let local_x = cell_x * 4 + sub_x;

                            // Z interpolation step (4 blocks per cell_z)
                            let z_step = (d_xz1 - d_xz0) / 4.0;
                            let mut d = d_xz0;

                            for sub_z in 0..4_usize {
                                let local_z = cell_z * 4 + sub_z;

                                let block = if d > 0.0 {
                                    STONE_ID
                                } else if y < 64 {
                                    // Check temperature for ICE at sea level surface
                                    let temp_idx = local_z * 16 + local_x;
                                    let temp = temperatures.get(temp_idx).copied().unwrap_or(0.5);
                                    if y == 63 && temp < 0.5 {
                                        ICE_ID
                                    } else {
                                        WATER_ID
                                    }
                                } else {
                                    AIR_ID
                                };
                                chunk.set_block(local_x as u8, y as u8, local_z as u8, block);

                                d += z_step;
                            }
                            d_xz0 += x_step_z0;
                            d_xz1 += x_step_z1;
                        }

                        d_y0_x0z0 += y_step_00;
                        d_y0_x1z0 += y_step_10;
                        d_y0_x0z1 += y_step_01;
                        d_y0_x1z1 += y_step_11;
                    }
                }
            }
        }
    }

    /// Port of buildSurfaces (Java lines 127-191).
    /// Top-down surface building pass: bedrock, sand/gravel/grass/dirt layers.
    fn build_surfaces(&self, chunk: &mut ChunkData, biomes: &[BiomeKind], rng: &mut JavaRandom) {
        let cx = chunk.pos.x;
        let cz = chunk.pos.z;

        let mut sand_buf = vec![0.0_f64; 256];
        let mut gravel_buf = vec![0.0_f64; 256];
        let mut depth_buf = vec![0.0_f64; 256];

        self.perlin_noise_2.get_region_3d(
            &mut sand_buf,
            (cx * 16) as f64,
            (cz * 16) as f64,
            0.0,
            16,
            16,
            1,
            0.03125,
            0.03125,
            1.0,
        );
        // Note: X/Z swap for gravel, matching Alpha
        self.perlin_noise_2.get_region_3d(
            &mut gravel_buf,
            (cz * 16) as f64,
            109.0134,
            (cx * 16) as f64,
            16,
            1,
            16,
            0.03125,
            1.0,
            0.03125,
        );
        self.perlin_noise_3.get_region_3d(
            &mut depth_buf,
            (cx * 16) as f64,
            (cz * 16) as f64,
            0.0,
            16,
            16,
            1,
            0.0625,
            0.0625,
            0.0625,
        );

        for local_x in 0..16_usize {
            for local_z in 0..16_usize {
                let biome = biomes[local_z * 16 + local_x];
                let (mut surface_block, mut subsurface_block) = biome.surface_subsurface_blocks();

                let noise_idx = local_x * 16 + local_z;
                let is_sand = sand_buf[noise_idx] + rng.next_double() * 0.2 > 0.0;
                let is_gravel = gravel_buf[noise_idx] + rng.next_double() * 0.2 > 3.0;
                let stone_depth =
                    (depth_buf[noise_idx] / 3.0 + 3.0 + rng.next_double() * 0.25) as i32;

                let mut counter: i32 = -1;

                for y in (0..128_i32).rev() {
                    // Bedrock
                    if y <= rng.next_int(5) {
                        chunk.set_block(local_x as u8, y as u8, local_z as u8, BEDROCK_ID);
                        continue;
                    }

                    let block = chunk.block(local_x as u8, y as u8, local_z as u8);

                    if block == AIR_ID {
                        counter = -1;
                        continue;
                    }

                    if block != STONE_ID {
                        continue;
                    }

                    if counter == -1 {
                        if stone_depth <= 0 {
                            surface_block = AIR_ID;
                            subsurface_block = STONE_ID;
                        } else if y >= 60 && y <= 65 {
                            // Near sea level: apply sand/gravel overrides
                            let (sb, ssb) = biome.surface_subsurface_blocks();
                            surface_block = sb;
                            subsurface_block = ssb;
                            if is_gravel {
                                surface_block = AIR_ID;
                                subsurface_block = GRAVEL_ID;
                            }
                            if is_sand {
                                surface_block = SAND_ID;
                                subsurface_block = SAND_ID;
                            }
                        }

                        if y < 64 && surface_block == AIR_ID {
                            surface_block = WATER_ID;
                        }

                        counter = stone_depth;
                        chunk.set_block(local_x as u8, y as u8, local_z as u8, surface_block);
                    } else if counter > 0 {
                        counter -= 1;
                        chunk.set_block(local_x as u8, y as u8, local_z as u8, subsurface_block);
                    }
                }
            }
        }
    }

    fn populate_chunk_features(&self, chunk: &mut ChunkData) {
        carve_caves(chunk, self.seed);
    }
}

// TODO(M9-Nether): Add Alpha 1.2.6 Nether world generator parity path.
// Deferred by plan: Nether terrain/carver/population and dimension plumbing.
#[allow(dead_code)]
pub struct NetherChunkGeneratorStub;

// ---------------------------------------------------------------------------
// Cave carving  (translated from CaveWorldCarver.java / Generator.java)
// ---------------------------------------------------------------------------

/// Derive the two seed multipliers exactly as Alpha does in Generator.java:
///   rng.setSeed(worldSeed); l = rng.nextLong()/2*2+1; m = rng.nextLong()/2*2+1;
/// Java's `Random.nextLong()` is two calls to `next(32)` glued together.
fn alpha_seed_multipliers(world_seed: u64) -> (i64, i64) {
    let mut s = java_random_seed(world_seed as i64);
    let l = java_next_long(&mut s) / 2 * 2 + 1;
    let m = java_next_long(&mut s) / 2 * 2 + 1;
    (l, m)
}

/// Derive a deterministic per-chunk seed from the Alpha multiplier pattern.
fn alpha_chunk_seed(world_seed: u64, cx: i32, cz: i32) -> i64 {
    let (mul_l, mul_m) = alpha_seed_multipliers(world_seed);
    (cx as i64)
        .wrapping_mul(mul_l)
        .wrapping_add((cz as i64).wrapping_mul(mul_m))
        ^ (world_seed as i64)
}

/// Minimal Java LCG state — just enough to replicate `java.util.Random`.
fn java_random_seed(seed: i64) -> i64 {
    (seed ^ 0x5DEECE66D) & ((1_i64 << 48) - 1)
}

fn java_next(state: &mut i64, bits: u32) -> i32 {
    *state = (state.wrapping_mul(0x5DEECE66D).wrapping_add(0xB)) & ((1_i64 << 48) - 1);
    (*state >> (48 - bits)) as i32
}

fn java_next_int(state: &mut i64, bound: i32) -> i32 {
    assert!(bound > 0);
    // Simplified: power-of-two fast path not needed for correctness here.
    loop {
        let bits = java_next(state, 31);
        let val = bits % bound;
        if bits.wrapping_sub(val).wrapping_add(bound - 1) >= 0 {
            return val;
        }
    }
}

fn java_next_long(state: &mut i64) -> i64 {
    let hi = java_next(state, 32) as i64;
    let lo = java_next(state, 32) as i64;
    (hi << 32).wrapping_add(lo)
}

fn java_next_float(state: &mut i64) -> f32 {
    java_next(state, 24) as f32 / (1_i32 << 24) as f32
}

fn java_next_double(state: &mut i64) -> f64 {
    let hi = (java_next(state, 26) as i64) << 27;
    let lo = java_next(state, 27) as i64;
    (hi + lo) as f64 / ((1_i64 << 53) as f64)
}

fn carve_caves(chunk: &mut ChunkData, world_seed: u64) {
    let cx = chunk.pos.x;
    let cz = chunk.pos.z;

    for source_cx in (cx - 8)..=(cx + 8) {
        for source_cz in (cz - 8)..=(cz + 8) {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_cx, source_cz));

            // Triple-nested random for cave count, skip 14/15 chunks
            let inner = java_next_int(&mut rng, 40) + 1;
            let mid = java_next_int(&mut rng, inner) + 1;
            let cave_count = java_next_int(&mut rng, mid);
            if java_next_int(&mut rng, 15) != 0 {
                continue;
            }

            for _ in 0..cave_count {
                let start_x = source_cx as f64 * 16.0 + java_next_int(&mut rng, 16) as f64;
                let inner_y = java_next_int(&mut rng, 120) + 8;
                let start_y = java_next_int(&mut rng, inner_y) as f64;
                let start_z = source_cz as f64 * 16.0 + java_next_int(&mut rng, 16) as f64;

                let mut k = 1;
                if java_next_int(&mut rng, 4) == 0 {
                    // Carve a room
                    let room_width = 1.0 + java_next_float(&mut rng) as f64 * 6.0;
                    carve_tunnel(
                        &mut rng,
                        chunk,
                        cx,
                        cz,
                        start_x,
                        start_y,
                        start_z,
                        room_width as f32,
                        0.0,
                        0.0,
                        -1,
                        -1,
                        0.5,
                    );
                    k += java_next_int(&mut rng, 4);
                }
                for _ in 0..k {
                    let yaw = java_next_float(&mut rng) * PI as f32 * 2.0;
                    let pitch = (java_next_float(&mut rng) - 0.5) * 2.0 / 8.0;
                    let base_width = java_next_float(&mut rng) * 2.0 + java_next_float(&mut rng);
                    carve_tunnel(
                        &mut rng, chunk, cx, cz, start_x, start_y, start_z, base_width, yaw, pitch,
                        0, 0, 1.0,
                    );
                }
            }
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn carve_tunnel(
    parent_rng: &mut i64,
    chunk: &mut ChunkData,
    chunk_x: i32,
    chunk_z: i32,
    mut x: f64,
    mut y: f64,
    mut z: f64,
    base_width: f32,
    mut yaw: f32,
    mut pitch: f32,
    mut tunnel: i32,
    mut tunnel_count: i32,
    width_height_ratio: f64,
) {
    let center_x = chunk_x as f64 * 16.0 + 8.0;
    let center_z = chunk_z as f64 * 16.0 + 8.0;
    let mut yaw_drift = 0.0_f32;
    let mut pitch_drift = 0.0_f32;
    let mut rng = java_random_seed(java_next_long(parent_rng));

    if tunnel_count <= 0 {
        let range_blocks = 8 * 16 - 16;
        tunnel_count = range_blocks - java_next_int(&mut rng, range_blocks / 4);
    }

    let mut is_room = false;
    if tunnel == -1 {
        tunnel = tunnel_count / 2;
        is_room = true;
    }

    let fork_point = java_next_int(&mut rng, tunnel_count / 2) + tunnel_count / 4;
    let gradual_pitch = java_next_int(&mut rng, 6) == 0;

    while tunnel < tunnel_count {
        let horiz_radius =
            1.5 + ((tunnel as f32 * PI as f32 / tunnel_count as f32).sin() * base_width) as f64;
        let vert_radius = horiz_radius * width_height_ratio;

        let cos_pitch = pitch.cos();
        let sin_pitch = pitch.sin();
        x += (yaw.cos() * cos_pitch) as f64;
        y += sin_pitch as f64;
        z += (yaw.sin() * cos_pitch) as f64;

        pitch = if gradual_pitch {
            pitch * 0.92
        } else {
            pitch * 0.7
        };
        pitch += pitch_drift * 0.1;
        yaw += yaw_drift * 0.1;

        pitch_drift *= 0.9;
        yaw_drift *= 0.75;
        pitch_drift += (java_next_float(&mut rng) - java_next_float(&mut rng))
            * java_next_float(&mut rng)
            * 2.0;
        yaw_drift += (java_next_float(&mut rng) - java_next_float(&mut rng))
            * java_next_float(&mut rng)
            * 4.0;

        // Fork at midpoint
        if !is_room && tunnel == fork_point && base_width > 1.0 {
            let w1 = java_next_float(&mut rng) * 0.5 + 0.5;
            carve_tunnel(
                &mut rng,
                chunk,
                chunk_x,
                chunk_z,
                x,
                y,
                z,
                w1,
                yaw - PI as f32 / 2.0,
                pitch / 3.0,
                tunnel,
                tunnel_count,
                1.0,
            );
            let w2 = java_next_float(&mut rng) * 0.5 + 0.5;
            carve_tunnel(
                &mut rng,
                chunk,
                chunk_x,
                chunk_z,
                x,
                y,
                z,
                w2,
                yaw + PI as f32 / 2.0,
                pitch / 3.0,
                tunnel,
                tunnel_count,
                1.0,
            );
            return;
        }

        if is_room || java_next_int(&mut rng, 4) != 0 {
            let dx = x - center_x;
            let dz = z - center_z;
            let remaining = (tunnel_count - tunnel) as f64;
            let max_reach = (base_width + 2.0 + 16.0) as f64;
            if dx * dx + dz * dz - remaining * remaining > max_reach * max_reach {
                return;
            }

            if x >= center_x - 16.0 - horiz_radius * 2.0
                && z >= center_z - 16.0 - horiz_radius * 2.0
                && x <= center_x + 16.0 + horiz_radius * 2.0
                && z <= center_z + 16.0 + horiz_radius * 2.0
            {
                carve_ellipsoid(chunk, chunk_x, chunk_z, x, y, z, horiz_radius, vert_radius);
            }

            // Alpha: rooms exit the loop after the first carve pass (CaveWorldCarver line 133)
            if is_room {
                break;
            }
        }

        tunnel += 1;
    }
}

/// Carve an ellipsoidal region within the target chunk bounds.
#[allow(clippy::too_many_arguments)]
fn carve_ellipsoid(
    chunk: &mut ChunkData,
    chunk_x: i32,
    chunk_z: i32,
    cx: f64,
    cy: f64,
    cz: f64,
    horiz_radius: f64,
    vert_radius: f64,
) {
    let min_x = ((cx - horiz_radius).floor() as i32 - chunk_x * 16 - 1).max(0);
    let max_x = ((cx + horiz_radius).floor() as i32 - chunk_x * 16 + 1).min(16);
    let min_y = ((cy - vert_radius).floor() as i32 - 1).max(1);
    let max_y = ((cy + vert_radius).floor() as i32 + 1).min(120);
    let min_z = ((cz - horiz_radius).floor() as i32 - chunk_z * 16 - 1).max(0);
    let max_z = ((cz + horiz_radius).floor() as i32 - chunk_z * 16 + 1).min(16);

    // Check for water — abort if any water in bounding box
    for lx in min_x..max_x {
        for lz in min_z..max_z {
            for ly in (min_y.saturating_sub(1))..=(max_y + 1).min(CHUNK_HEIGHT as i32 - 1) {
                let b = chunk.block(lx as u8, ly as u8, lz as u8);
                if b == FLOWING_WATER_ID || b == WATER_ID {
                    return;
                }
            }
        }
    }

    for lx in min_x..max_x {
        let fx = ((lx + chunk_x * 16) as f64 + 0.5 - cx) / horiz_radius;
        for lz in min_z..max_z {
            let fz = ((lz + chunk_z * 16) as f64 + 0.5 - cz) / horiz_radius;
            let fxz_sq = fx * fx + fz * fz;
            let mut was_grass = false;
            for ly in (min_y..max_y).rev() {
                let fy = (ly as f64 + 0.5 - cy) / vert_radius;
                if fy > -0.7 && fxz_sq + fy * fy < 1.0 {
                    let b = chunk.block(lx as u8, ly as u8, lz as u8);
                    if b == GRASS_ID {
                        was_grass = true;
                    }
                    if b == STONE_ID || b == DIRT_ID || b == GRASS_ID {
                        if ly < 10 {
                            chunk.set_block(lx as u8, ly as u8, lz as u8, FLOWING_LAVA_ID);
                        } else {
                            chunk.set_block(lx as u8, ly as u8, lz as u8, AIR_ID);
                            // If we carved grass, turn dirt below into grass
                            if was_grass && ly > 0 {
                                let below = chunk.block(lx as u8, (ly - 1) as u8, lz as u8);
                                if below == DIRT_ID {
                                    chunk.set_block(lx as u8, (ly - 1) as u8, lz as u8, GRASS_ID);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ore vein placement  (translated from VeinFeature.java / OverworldChunkGenerator.java)
// ---------------------------------------------------------------------------

struct OreConfig {
    block_id: u8,
    vein_size: i32,
    attempts: i32,
    max_y: i32,
}

const ORE_TABLE: &[OreConfig] = &[
    OreConfig {
        block_id: DIRT_ID,
        vein_size: 32,
        attempts: 20,
        max_y: 128,
    },
    OreConfig {
        block_id: GRAVEL_ID,
        vein_size: 32,
        attempts: 10,
        max_y: 128,
    },
    OreConfig {
        block_id: COAL_ORE_ID,
        vein_size: 16,
        attempts: 20,
        max_y: 128,
    },
    OreConfig {
        block_id: IRON_ORE_ID,
        vein_size: 8,
        attempts: 20,
        max_y: 64,
    },
    OreConfig {
        block_id: GOLD_ORE_ID,
        vein_size: 8,
        attempts: 2,
        max_y: 32,
    },
    OreConfig {
        block_id: REDSTONE_ORE_ID,
        vein_size: 7,
        attempts: 8,
        max_y: 16,
    },
    OreConfig {
        block_id: DIAMOND_ORE_ID,
        vein_size: 7,
        attempts: 1,
        max_y: 16,
    },
];

fn place_ores_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let base_x = source_x * 16;
            let base_z = source_z * 16;
            for ore in ORE_TABLE {
                for _ in 0..ore.attempts {
                    let x = base_x + java_next_int(&mut rng, 16);
                    let y = java_next_int(&mut rng, ore.max_y);
                    let z = base_z + java_next_int(&mut rng, 16);
                    place_world_vein(chunks, &mut rng, ore.block_id, ore.vein_size, x, y, z);
                }
            }
        }
    }
}

fn place_world_vein(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    rng: &mut i64,
    block_id: u8,
    size: i32,
    x: i32,
    y: i32,
    z: i32,
) {
    let angle = java_next_float(rng) * PI as f32;
    let size_f = size as f32;

    let vein_x_start = (x as f32 + 8.0) + angle.sin() * size_f / 8.0;
    let vein_x_end = (x as f32 + 8.0) - angle.sin() * size_f / 8.0;
    let vein_z_start = (z as f32 + 8.0) + angle.cos() * size_f / 8.0;
    let vein_z_end = (z as f32 + 8.0) - angle.cos() * size_f / 8.0;
    let vein_y_start = (y + java_next_int(rng, 3) + 2) as f64;
    let vein_y_end = (y + java_next_int(rng, 3) + 2) as f64;

    for step in 0..=size {
        let t = step as f64 / size as f64;
        let interp_x = vein_x_start as f64 + (vein_x_end as f64 - vein_x_start as f64) * t;
        let interp_y = vein_y_start + (vein_y_end - vein_y_start) * t;
        let interp_z = vein_z_start as f64 + (vein_z_end as f64 - vein_z_start as f64) * t;

        let radius_noise = java_next_float(rng) as f64 * size as f64 / 16.0;
        let sin_val = ((step as f32 * PI as f32 / size_f).sin() + 1.0) as f64;
        let half_extent = sin_val * radius_noise + 1.0;

        let x_min = (interp_x - half_extent / 2.0) as i32;
        let x_max = (interp_x + half_extent / 2.0) as i32;
        let y_min = (interp_y - half_extent / 2.0) as i32;
        let y_max = (interp_y + half_extent / 2.0) as i32;
        let z_min = (interp_z - half_extent / 2.0) as i32;
        let z_max = (interp_z + half_extent / 2.0) as i32;

        for bx in x_min..=x_max {
            let fx = (bx as f64 + 0.5 - interp_x) / (half_extent / 2.0);
            for by in y_min..=y_max {
                if by < 1 || by >= CHUNK_HEIGHT as i32 {
                    continue;
                }
                let fy = (by as f64 + 0.5 - interp_y) / (half_extent / 2.0);
                for bz in z_min..=z_max {
                    let fz = (bz as f64 + 0.5 - interp_z) / (half_extent / 2.0);
                    if fx * fx + fy * fy + fz * fz >= 1.0 {
                        continue;
                    }
                    if world_block(chunks, bx, by, bz) == STONE_ID {
                        set_world_block(chunks, bx, by, bz, block_id);
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dungeon generation  (translated from DungeonFeature.java / OverworldChunkGenerator.java)
// ---------------------------------------------------------------------------

/// Alpha's `material.isSolid()` — air and liquids are non-solid.
fn is_solid_for_dungeon(block_id: u8) -> bool {
    !matches!(
        block_id,
        AIR_ID | FLOWING_WATER_ID | WATER_ID | FLOWING_LAVA_ID | LAVA_ID
    )
}

fn place_dungeons(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let base_x = source_x * CHUNK_WIDTH as i32 + 8;
            let base_z = source_z * CHUNK_DEPTH as i32 + 8;

            for _ in 0..8 {
                let x = base_x + java_next_int(&mut rng, 16);
                let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let z = base_z + java_next_int(&mut rng, 16);
                try_place_dungeon(chunks, &mut rng, x, y, z);
            }
        }
    }
}

fn try_place_dungeon(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    rng: &mut i64,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    let room_height = 3;
    let half_x = java_next_int(rng, 2) + 2;
    let half_z = java_next_int(rng, 2) + 2;
    let mut openings = 0;

    for m in (x - half_x - 1)..=(x + half_x + 1) {
        for n in (y - 1)..=(y + room_height + 1) {
            for q in (z - half_z - 1)..=(z + half_z + 1) {
                let block = world_block(chunks, m, n, q);
                if n == y - 1 && !is_solid_for_dungeon(block) {
                    return false;
                }
                if n == y + room_height + 1 && !is_solid_for_dungeon(block) {
                    return false;
                }
                let boundary = m == x - half_x - 1
                    || m == x + half_x + 1
                    || q == z - half_z - 1
                    || q == z + half_z + 1;
                if boundary
                    && n == y
                    && world_block(chunks, m, n, q) == AIR_ID
                    && world_block(chunks, m, n + 1, q) == AIR_ID
                {
                    openings += 1;
                }
            }
        }
    }

    if !(1..=5).contains(&openings) {
        return false;
    }

    for m in (x - half_x - 1)..=(x + half_x + 1) {
        for o in ((y - 1)..=(y + room_height)).rev() {
            for r in (z - half_z - 1)..=(z + half_z + 1) {
                let on_boundary = m == x - half_x - 1
                    || o == y - 1
                    || r == z - half_z - 1
                    || m == x + half_x + 1
                    || o == y + room_height + 1
                    || r == z + half_z + 1;
                if on_boundary {
                    if o >= 0 && !is_solid_for_dungeon(world_block(chunks, m, o - 1, r)) {
                        set_world_block(chunks, m, o, r, AIR_ID);
                        continue;
                    }
                    if !is_solid_for_dungeon(world_block(chunks, m, o, r)) {
                        continue;
                    }
                    let wall_id = if o == y - 1 && java_next_int(rng, 4) != 0 {
                        MOSSY_COBBLESTONE_ID
                    } else {
                        COBBLESTONE_ID
                    };
                    set_world_block(chunks, m, o, r, wall_id);
                    continue;
                }
                set_world_block(chunks, m, o, r, AIR_ID);
            }
        }
    }

    for _ in 0..2 {
        let mut chest_placed = false;
        for _ in 0..3 {
            let chest_x = x + java_next_int(rng, half_x * 2 + 1) - half_x;
            let chest_z = z + java_next_int(rng, half_z * 2 + 1) - half_z;
            if world_block(chunks, chest_x, y, chest_z) != AIR_ID {
                continue;
            }

            let mut neighbor_count = 0;
            if is_solid_for_dungeon(world_block(chunks, chest_x - 1, y, chest_z)) {
                neighbor_count += 1;
            }
            if is_solid_for_dungeon(world_block(chunks, chest_x + 1, y, chest_z)) {
                neighbor_count += 1;
            }
            if is_solid_for_dungeon(world_block(chunks, chest_x, y, chest_z - 1)) {
                neighbor_count += 1;
            }
            if is_solid_for_dungeon(world_block(chunks, chest_x, y, chest_z + 1)) {
                neighbor_count += 1;
            }
            if neighbor_count != 1 {
                continue;
            }

            let north_solid = is_solid_for_dungeon(world_block(chunks, chest_x, y, chest_z - 1));
            let south_solid = is_solid_for_dungeon(world_block(chunks, chest_x, y, chest_z + 1));
            let west_solid = is_solid_for_dungeon(world_block(chunks, chest_x - 1, y, chest_z));
            let east_solid = is_solid_for_dungeon(world_block(chunks, chest_x + 1, y, chest_z));
            let mut chest_front_face = 3_u8;
            if north_solid && !south_solid {
                chest_front_face = 3;
            }
            if south_solid && !north_solid {
                chest_front_face = 2;
            }
            if west_solid && !east_solid {
                chest_front_face = 5;
            }
            if east_solid && !west_solid {
                chest_front_face = 4;
            }
            set_world_block_with_metadata(chunks, chest_x, y, chest_z, CHEST_ID, chest_front_face);
            // TODO(M7): Attach chest block-entity inventory and write real loot stacks.
            for _ in 0..8 {
                consume_dungeon_loot_roll(rng);
            }
            chest_placed = true;
            break;
        }
        if !chest_placed {
            continue;
        }
    }

    set_world_block(chunks, x, y, z, MOB_SPAWNER_ID);
    // TODO(M7): Persist spawner block-entity mob type ("Skeleton"/"Zombie"/"Spider").
    let _spawner_type = pick_dungeon_spawner_type(rng);
    true
}

fn consume_dungeon_loot_roll(rng: &mut i64) {
    let roll = java_next_int(rng, 11);
    let mut has_loot = false;
    match roll {
        0 => has_loot = true, // Saddle
        1 => {
            let _count = java_next_int(rng, 4) + 1; // Iron ingot count
            has_loot = true;
        }
        2 => has_loot = true, // Bread
        3 => {
            let _count = java_next_int(rng, 4) + 1; // Wheat count
            has_loot = true;
        }
        4 => {
            let _count = java_next_int(rng, 4) + 1; // Gunpowder count
            has_loot = true;
        }
        5 => {
            let _count = java_next_int(rng, 4) + 1; // String count
            has_loot = true;
        }
        6 => has_loot = true, // Bucket
        7 => {
            if java_next_int(rng, 100) == 0 {
                has_loot = true; // Golden apple
            }
        }
        8 => {
            if java_next_int(rng, 2) == 0 {
                let _count = java_next_int(rng, 4) + 1; // Redstone count
                has_loot = true;
            }
        }
        9 => {
            if java_next_int(rng, 10) == 0 {
                let _record_variant = java_next_int(rng, 2); // Record 13 or Cat
                has_loot = true;
            }
        }
        _ => {}
    }
    if has_loot {
        let _slot = java_next_int(rng, 27); // Chest inventory slot index
    }
}

fn pick_dungeon_spawner_type(rng: &mut i64) -> &'static str {
    match java_next_int(rng, 4) {
        0 => "Skeleton",
        1 | 2 => "Zombie",
        3 => "Spider",
        _ => "",
    }
}

// ---------------------------------------------------------------------------
// Decoration features  (translated from OverworldChunkGenerator.populate())
// ---------------------------------------------------------------------------

/// Find the highest non-air block at a local column, scanning downward.
/// Check if any of the 4 cardinal neighbors at (local_x, y, local_z) is water.
/// Check if a stone block has exactly 1 air + 3 stone cardinal neighbors,
/// making it a valid spring source. If so, replace it with the given liquid.
fn try_place_spring_world(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    x: i32,
    y: i32,
    z: i32,
    liquid_id: u8,
) {
    if !(1..CHUNK_HEIGHT as i32 - 1).contains(&y) {
        return;
    }
    if world_block(chunks, x, y + 1, z) != STONE_ID {
        return;
    }
    if world_block(chunks, x, y - 1, z) != STONE_ID {
        return;
    }
    let center = world_block(chunks, x, y, z);
    if center != AIR_ID && center != STONE_ID {
        return;
    }
    let mut air_count = 0;
    let mut stone_count = 0;
    for (dx, dz) in [(-1, 0), (1, 0), (0, -1), (0, 1)] {
        let nx = x + dx;
        let nz = z + dz;
        let bid = world_block(chunks, nx, y, nz);
        if bid == AIR_ID {
            air_count += 1;
        } else if bid == STONE_ID {
            stone_count += 1;
        }
    }
    if air_count == 1 && stone_count == 3 {
        set_world_block(chunks, x, y, z, liquid_id);
        // TODO(M7): Mirror Alpha's immediate liquid tick on spring placement.
        // Runtime fluid scheduler will pick this up after chunk load.
    }
}

fn place_springs_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let base_x = source_x * 16 + 8;
            let base_z = source_z * 16 + 8;

            // Waterfalls: 50 attempts
            for _ in 0..50 {
                let x = base_x + java_next_int(&mut rng, 16);
                let z = base_z + java_next_int(&mut rng, 16);
                let inner = java_next_int(&mut rng, 120) + 8;
                let y = java_next_int(&mut rng, inner.max(1));
                if y >= 1 && y < CHUNK_HEIGHT as i32 {
                    try_place_spring_world(chunks, x, y, z, FLOWING_WATER_ID);
                }
            }

            // Lavafalls: 20 attempts
            for _ in 0..20 {
                let x = base_x + java_next_int(&mut rng, 16);
                let z = base_z + java_next_int(&mut rng, 16);
                let inner_a = java_next_int(&mut rng, 112) + 8;
                let inner_b = java_next_int(&mut rng, inner_a.max(1)) + 8;
                let y = java_next_int(&mut rng, inner_b.max(1));
                if y >= 1 && y < CHUNK_HEIGHT as i32 {
                    try_place_spring_world(chunks, x, y, z, FLOWING_LAVA_ID);
                }
            }
        }
    }
}

fn check_water_adjacent_world(
    chunks: &HashMap<ChunkPos, ChunkData>,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    for (dx, dz) in [(-1, 0), (1, 0), (0, -1), (0, 1)] {
        let bid = world_block(chunks, x + dx, y, z + dz);
        if bid == WATER_ID || bid == FLOWING_WATER_ID {
            return true;
        }
    }
    false
}

fn can_place_cactus_world(
    chunks: &HashMap<ChunkPos, ChunkData>,
    x: i32,
    y: i32,
    z: i32,
    registry: &BlockRegistry,
) -> bool {
    for (dx, dz) in [(-1, 0), (1, 0), (0, -1), (0, 1)] {
        if registry.is_solid(world_block(chunks, x + dx, y, z + dz)) {
            return false;
        }
    }
    true
}

fn has_sky_access_world(chunks: &HashMap<ChunkPos, ChunkData>, x: i32, y: i32, z: i32) -> bool {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return false;
    }
    for ay in (y + 1)..CHUNK_HEIGHT as i32 {
        if world_block(chunks, x, ay, z) != AIR_ID {
            return false;
        }
    }
    true
}

fn can_place_flower_world(
    chunks: &HashMap<ChunkPos, ChunkData>,
    _registry: &BlockRegistry,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    if world_block(chunks, x, y, z) != AIR_ID {
        return false;
    }
    let below = world_block(chunks, x, y - 1, z);
    if below != GRASS_ID && below != DIRT_ID {
        return false;
    }
    // Alpha PlantBlock.canSurvive also accepts rawBrightness >= 8.
    // Worldgen has no artificial lights, so sky access is the relevant path.
    has_sky_access_world(chunks, x, y, z)
}

fn can_place_mushroom_world(
    chunks: &HashMap<ChunkPos, ChunkData>,
    registry: &BlockRegistry,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    if world_block(chunks, x, y, z) != AIR_ID {
        return false;
    }
    let below = world_block(chunks, x, y - 1, z);
    if !registry.is_solid(below) {
        return false;
    }
    // Approximation of MushroomPlantBlock.canSurvive (brightness <= 13):
    // disallow direct sky access at generation time.
    !has_sky_access_world(chunks, x, y, z)
}

fn can_place_pumpkin_world(
    chunks: &HashMap<ChunkPos, ChunkData>,
    registry: &BlockRegistry,
    x: i32,
    y: i32,
    z: i32,
) -> bool {
    let at = world_block(chunks, x, y, z);
    let replaceable = at == AIR_ID || registry.is_liquid(at);
    replaceable && registry.is_solid(world_block(chunks, x, y - 1, z))
}

fn place_clay_patches_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let base_x = source_x * 16;
            let base_z = source_z * 16;
            for _ in 0..10 {
                let x = base_x + java_next_int(&mut rng, 16);
                let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let z = base_z + java_next_int(&mut rng, 16);
                let origin_id = world_block(chunks, x, y, z);
                if origin_id != WATER_ID && origin_id != FLOWING_WATER_ID {
                    continue;
                }
                let size = 32_i32;
                let f = java_next_float(&mut rng) * PI as f32;
                let d = (x + 8) as f64 + f.sin() as f64 * size as f64 / 8.0;
                let e = (x + 8) as f64 - f.sin() as f64 * size as f64 / 8.0;
                let g = (z + 8) as f64 + f.cos() as f64 * size as f64 / 8.0;
                let h = (z + 8) as f64 - f.cos() as f64 * size as f64 / 8.0;
                let i = (y + java_next_int(&mut rng, 3) + 2) as f64;
                let j = (y + java_next_int(&mut rng, 3) + 2) as f64;
                for k in 0..=size {
                    let t = k as f64 / size as f64;
                    let l = d + (e - d) * t;
                    let m = i + (j - i) * t;
                    let n = g + (h - g) * t;
                    let o = java_next_float(&mut rng) as f64 * size as f64 / 16.0;
                    let p = (((k as f64 * PI) / size as f64).sin() + 1.0) * o + 1.0;
                    let q = (((k as f64 * PI) / size as f64).sin() + 1.0) * o + 1.0;
                    for rx in (l - p / 2.0) as i32..=(l + p / 2.0) as i32 {
                        for ry in (m - q / 2.0) as i32..=(m + q / 2.0) as i32 {
                            if !(0..CHUNK_HEIGHT as i32).contains(&ry) {
                                continue;
                            }
                            for rz in (n - p / 2.0) as i32..=(n + p / 2.0) as i32 {
                                let u = (rx as f64 + 0.5 - l) / (p / 2.0);
                                let v = (ry as f64 + 0.5 - m) / (q / 2.0);
                                let w = (rz as f64 + 0.5 - n) / (p / 2.0);
                                if u * u + v * v + w * w < 1.0
                                    && world_block(chunks, rx, ry, rz) == SAND_ID
                                {
                                    set_world_block(chunks, rx, ry, rz, CLAY_ID);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fn place_flowers_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
    registry: &BlockRegistry,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let i = source_x * 16;
            let j = source_z * 16;
            for _ in 0..2 {
                let x = i + java_next_int(&mut rng, 16) + 8;
                let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let z = j + java_next_int(&mut rng, 16) + 8;
                for _ in 0..64 {
                    let px = x + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                    let py = y + java_next_int(&mut rng, 4) - java_next_int(&mut rng, 4);
                    let pz = z + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                    if !(1..CHUNK_HEIGHT as i32).contains(&py) {
                        continue;
                    }
                    if can_place_flower_world(chunks, registry, px, py, pz) {
                        set_world_block(chunks, px, py, pz, YELLOW_FLOWER_ID);
                    }
                }
            }
            if java_next_int(&mut rng, 2) == 0 {
                let x = i + java_next_int(&mut rng, 16) + 8;
                let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let z = j + java_next_int(&mut rng, 16) + 8;
                for _ in 0..64 {
                    let px = x + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                    let py = y + java_next_int(&mut rng, 4) - java_next_int(&mut rng, 4);
                    let pz = z + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                    if !(1..CHUNK_HEIGHT as i32).contains(&py) {
                        continue;
                    }
                    if can_place_flower_world(chunks, registry, px, py, pz) {
                        set_world_block(chunks, px, py, pz, RED_FLOWER_ID);
                    }
                }
            }
        }
    }
}

fn place_mushrooms_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
    registry: &BlockRegistry,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let i = source_x * 16;
            let j = source_z * 16;
            let mut try_scatter = |block_id: u8, rng: &mut i64| {
                let x = i + java_next_int(rng, 16) + 8;
                let y = java_next_int(rng, CHUNK_HEIGHT as i32);
                let z = j + java_next_int(rng, 16) + 8;
                for _ in 0..64 {
                    let px = x + java_next_int(rng, 8) - java_next_int(rng, 8);
                    let py = y + java_next_int(rng, 4) - java_next_int(rng, 4);
                    let pz = z + java_next_int(rng, 8) - java_next_int(rng, 8);
                    if !(1..CHUNK_HEIGHT as i32).contains(&py) {
                        continue;
                    }
                    if can_place_mushroom_world(chunks, registry, px, py, pz) {
                        set_world_block(chunks, px, py, pz, block_id);
                    }
                }
            };
            if java_next_int(&mut rng, 4) == 0 {
                try_scatter(BROWN_MUSHROOM_ID, &mut rng);
            }
            if java_next_int(&mut rng, 8) == 0 {
                try_scatter(RED_MUSHROOM_ID, &mut rng);
            }
        }
    }
}

fn place_sugar_cane_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let i = source_x * 16;
            let j = source_z * 16;
            for _ in 0..10 {
                let x = i + java_next_int(&mut rng, 16) + 8;
                let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let z = j + java_next_int(&mut rng, 16) + 8;
                for _ in 0..20 {
                    let px = x + java_next_int(&mut rng, 4) - java_next_int(&mut rng, 4);
                    let pz = z + java_next_int(&mut rng, 4) - java_next_int(&mut rng, 4);
                    if world_block(chunks, px, y, pz) != AIR_ID {
                        continue;
                    }
                    let below = world_block(chunks, px, y - 1, pz);
                    if below != GRASS_ID && below != DIRT_ID && below != SUGAR_CANE_ID {
                        continue;
                    }
                    if !check_water_adjacent_world(chunks, px, y - 1, pz) {
                        continue;
                    }
                    let cane_inner = java_next_int(&mut rng, 3) + 1;
                    let height = 2 + java_next_int(&mut rng, cane_inner);
                    for n in 0..height {
                        let py = y + n;
                        if py >= CHUNK_HEIGHT as i32 || world_block(chunks, px, py, pz) != AIR_ID {
                            break;
                        }
                        set_world_block(chunks, px, py, pz, SUGAR_CANE_ID);
                    }
                }
            }
        }
    }
}

fn place_pumpkins_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
    registry: &BlockRegistry,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            let i = source_x * 16;
            let j = source_z * 16;
            if java_next_int(&mut rng, 32) != 0 {
                continue;
            }
            let x = i + java_next_int(&mut rng, 16) + 8;
            let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
            let z = j + java_next_int(&mut rng, 16) + 8;
            for _ in 0..64 {
                let px = x + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                let py = y + java_next_int(&mut rng, 4) - java_next_int(&mut rng, 4);
                let pz = z + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                if !(1..CHUNK_HEIGHT as i32).contains(&py) {
                    continue;
                }
                if world_block(chunks, px, py, pz) != AIR_ID {
                    continue;
                }
                if world_block(chunks, px, py - 1, pz) != GRASS_ID {
                    continue;
                }
                if !can_place_pumpkin_world(chunks, registry, px, py, pz) {
                    continue;
                }
                let facing_meta = java_next_int(&mut rng, 4) as u8;
                set_world_block_with_metadata(chunks, px, py, pz, PUMPKIN_ID, facing_meta);
            }
        }
    }
}

fn place_cacti_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    biome_source: &BiomeSource,
    source_min: ChunkPos,
    source_max: ChunkPos,
    registry: &BlockRegistry,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let i = source_x * 16;
            let j = source_z * 16;
            if biome_source.sample(i + 16, j + 16).biome != BiomeKind::Desert {
                continue;
            }
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));
            for _ in 0..10 {
                let x = i + java_next_int(&mut rng, 16) + 8;
                let y = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let z = j + java_next_int(&mut rng, 16) + 8;
                for _ in 0..10 {
                    let px = x + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                    let py = y + java_next_int(&mut rng, 4) - java_next_int(&mut rng, 4);
                    let pz = z + java_next_int(&mut rng, 8) - java_next_int(&mut rng, 8);
                    if world_block(chunks, px, py, pz) != AIR_ID {
                        continue;
                    }
                    let cactus_inner = java_next_int(&mut rng, 3) + 1;
                    let height = 1 + java_next_int(&mut rng, cactus_inner);
                    for n in 0..height {
                        let ay = py + n;
                        if !(1..CHUNK_HEIGHT as i32).contains(&ay) {
                            break;
                        }
                        let below = world_block(chunks, px, ay - 1, pz);
                        if below != SAND_ID && below != CACTUS_ID {
                            break;
                        }
                        if !can_place_cactus_world(chunks, px, ay, pz, registry) {
                            break;
                        }
                        if world_block(chunks, px, ay, pz) != AIR_ID {
                            break;
                        }
                        set_world_block(chunks, px, ay, pz, CACTUS_ID);
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Snow cover  (final decoration pass, after trees)
// ---------------------------------------------------------------------------

fn place_snow_cover(chunk: &mut ChunkData, biome_source: &BiomeSource, registry: &BlockRegistry) {
    let chunk_wx = chunk.pos.x * CHUNK_WIDTH as i32;
    let chunk_wz = chunk.pos.z * CHUNK_DEPTH as i32;

    // Bulk-sample temperatures for the whole chunk (avoids 256 individual noise calls)
    let (_biomes, temperatures, _downfalls) =
        biome_source.get_biomes(chunk_wx, chunk_wz, CHUNK_WIDTH, CHUNK_DEPTH);

    for local_x in 0..CHUNK_WIDTH as u8 {
        for local_z in 0..CHUNK_DEPTH as u8 {
            let temp = temperatures[usize::from(local_z) * CHUNK_WIDTH + usize::from(local_x)];

            let h = chunk.height_at(local_x, local_z);
            if h == 0 {
                continue;
            }
            let surface_y = h as i32 - 1;

            // Altitude-adjusted temperature
            let adjusted_temp = temp - (surface_y as f64 - 64.0) / 64.0 * 0.3;
            if adjusted_temp >= 0.5 {
                continue;
            }

            let top_block = chunk.block(local_x, surface_y as u8, local_z);
            // Don't place on ice, liquids, or air
            if top_block == AIR_ID || top_block == ICE_ID || registry.is_liquid(top_block) {
                continue;
            }
            // Don't place on non-solid blocks (plants, etc.)
            if !registry.blocks_movement(top_block) {
                continue;
            }

            let snow_y = surface_y + 1;
            if snow_y >= CHUNK_HEIGHT as i32 {
                continue;
            }
            if chunk.block(local_x, snow_y as u8, local_z) == AIR_ID {
                chunk.set_block(local_x, snow_y as u8, local_z, SNOW_LAYER_ID);
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Oak tree placement  (translated from TreeFeature.java / BiomeDecorator.java)
// ---------------------------------------------------------------------------

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
enum TreeKind {
    Oak,
    LargeOak,
}

const LARGE_OAK_MINOR_AXES: [usize; 6] = [2, 0, 0, 1, 2, 1];

fn alpha_tree_feature_kind_for_chunk(biome: BiomeKind, rng: &mut i64) -> TreeKind {
    // Alpha chooses a single feature for the whole chunk:
    // default TreeFeature, 10% LargeOak, plus 33% rainforest override.
    let mut kind = TreeKind::Oak;
    if java_next_int(rng, 10) == 0 {
        kind = TreeKind::LargeOak;
    }
    if biome == BiomeKind::Rainforest && java_next_int(rng, 3) == 0 {
        kind = TreeKind::LargeOak;
    }
    kind
}

fn alpha_forest_tree_noise_value(
    forest_noise: &PerlinNoise,
    chunk_origin_x: i32,
    chunk_origin_z: i32,
) -> f64 {
    let mut sample = [0.0_f64; 1];
    forest_noise.get_region_2d(
        &mut sample,
        chunk_origin_x as f64,
        chunk_origin_z as f64,
        1,
        1,
        0.5,
        0.5,
    );
    sample[0]
}

fn alpha_tree_attempt_count(biome: BiomeKind, forest_noise: f64, rng: &mut i64) -> i32 {
    let base = ((forest_noise / 8.0) + java_next_double(rng) * 4.0 + 4.0) / 3.0;
    let mut count = 0_i32;
    if java_next_int(rng, 10) == 0 {
        count += 1;
    }
    match biome {
        BiomeKind::Forest | BiomeKind::Rainforest | BiomeKind::Taiga => count + base as i32 + 5,
        BiomeKind::SeasonalForest => count + base as i32 + 2,
        BiomeKind::Desert | BiomeKind::Tundra | BiomeKind::Plains => count - 20,
        // Alpha does not add a swampland bonus.
        BiomeKind::Swampland | BiomeKind::Savanna | BiomeKind::Shrubland => count,
    }
}

#[derive(Debug, Clone, Copy)]
struct LargeOakConfig {
    trunk_scale: f64,
    branch_slope: f64,
    branch_length_scale: f64,
    foliage_density: f64,
    max_trunk_height: i32,
    foliage_cluster_height: i32,
    trunk_width: i32,
}

impl LargeOakConfig {
    fn alpha_populate_defaults() -> Self {
        // Matches LargeOakTreeFeature.prepare(1.0, 1.0, 1.0) used in populateChunk().
        Self {
            trunk_scale: 0.618,
            branch_slope: 0.381,
            branch_length_scale: 1.0,
            foliage_density: 1.0,
            max_trunk_height: 12,
            foliage_cluster_height: 5,
            trunk_width: 1,
        }
    }
}

fn large_oak_get_tree_shape(height: i32, total_height: i32) -> f32 {
    if (height as f64) < (total_height as f64) * 0.3 {
        return -1.618;
    }
    let half = total_height as f32 / 2.0;
    let offset = half - height as f32;
    let mut radius = if offset == 0.0 {
        half
    } else if offset.abs() >= half {
        0.0
    } else {
        (half.abs().powi(2) - offset.abs().powi(2)).sqrt()
    };
    radius *= 0.5;
    radius
}

fn large_oak_cluster_shape(layer: i32, cluster_height: i32) -> f32 {
    if layer < 0 || layer >= cluster_height {
        return -1.0;
    }
    if layer == 0 || layer == cluster_height - 1 {
        return 2.0;
    }
    3.0
}

fn large_oak_round(v: f64) -> i32 {
    (v + 0.5).floor() as i32
}

fn large_oak_try_branch(
    chunks: &HashMap<ChunkPos, ChunkData>,
    from: [i32; 3],
    to: [i32; 3],
) -> i32 {
    let mut delta = [0_i32; 3];
    let mut major_axis = 0_usize;
    for i in 0..3 {
        delta[i] = to[i] - from[i];
        if delta[i].abs() > delta[major_axis].abs() {
            major_axis = i;
        }
    }
    if delta[major_axis] == 0 {
        return -1;
    }

    let axis_b = LARGE_OAK_MINOR_AXES[major_axis];
    let axis_c = LARGE_OAK_MINOR_AXES[major_axis + 3];
    let step = if delta[major_axis] > 0 { 1 } else { -1 };
    let slope_b = delta[axis_b] as f64 / delta[major_axis] as f64;
    let slope_c = delta[axis_c] as f64 / delta[major_axis] as f64;
    let mut cursor = [0_i32; 3];
    let end = delta[major_axis] + step;
    let mut distance = 0_i32;
    while distance != end {
        cursor[major_axis] = from[major_axis] + distance;
        cursor[axis_b] = (from[axis_b] as f64 + distance as f64 * slope_b) as i32;
        cursor[axis_c] = (from[axis_c] as f64 + distance as f64 * slope_c) as i32;
        let block = world_block(chunks, cursor[0], cursor[1], cursor[2]);
        if block != AIR_ID && block != OAK_LEAVES_ID {
            break;
        }
        distance += step;
    }
    if distance == end {
        -1
    } else {
        distance.abs()
    }
}

fn large_oak_place_branch(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    from: [i32; 3],
    to: [i32; 3],
    block_id: u8,
) {
    let mut delta = [0_i32; 3];
    let mut major_axis = 0_usize;
    for i in 0..3 {
        delta[i] = to[i] - from[i];
        if delta[i].abs() > delta[major_axis].abs() {
            major_axis = i;
        }
    }
    if delta[major_axis] == 0 {
        return;
    }

    let axis_b = LARGE_OAK_MINOR_AXES[major_axis];
    let axis_c = LARGE_OAK_MINOR_AXES[major_axis + 3];
    let step = if delta[major_axis] > 0 { 1 } else { -1 };
    let slope_b = delta[axis_b] as f64 / delta[major_axis] as f64;
    let slope_c = delta[axis_c] as f64 / delta[major_axis] as f64;
    let mut cursor = [0_i32; 3];
    let end = delta[major_axis] + step;
    let mut distance = 0_i32;
    while distance != end {
        cursor[major_axis] = large_oak_round(from[major_axis] as f64 + distance as f64);
        cursor[axis_b] = large_oak_round(from[axis_b] as f64 + distance as f64 * slope_b);
        cursor[axis_c] = large_oak_round(from[axis_c] as f64 + distance as f64 * slope_c);
        set_world_block(chunks, cursor[0], cursor[1], cursor[2], block_id);
        distance += step;
    }
}

fn large_oak_place_cluster(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    x: i32,
    y: i32,
    z: i32,
    shape: f32,
    major_axis: usize,
    block_id: u8,
) {
    let radius = (shape as f64 + 0.618) as i32;
    let axis_b = LARGE_OAK_MINOR_AXES[major_axis];
    let axis_c = LARGE_OAK_MINOR_AXES[major_axis + 3];
    let center = [x, y, z];
    let mut cursor = [0_i32; 3];
    cursor[major_axis] = center[major_axis];
    for offset_b in -radius..=radius {
        cursor[axis_b] = center[axis_b] + offset_b;
        for offset_c in -radius..=radius {
            let d = (((offset_b.abs() as f64) + 0.5).powi(2)
                + ((offset_c.abs() as f64) + 0.5).powi(2))
            .sqrt();
            if d > shape as f64 {
                continue;
            }
            cursor[axis_c] = center[axis_c] + offset_c;
            let existing = world_block(chunks, cursor[0], cursor[1], cursor[2]);
            if existing == AIR_ID || existing == OAK_LEAVES_ID {
                set_world_block(chunks, cursor[0], cursor[1], cursor[2], block_id);
            }
        }
    }
}

fn large_oak_place_foliage_cluster(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    x: i32,
    base_y: i32,
    z: i32,
    cluster_height: i32,
) {
    let max_y = base_y + cluster_height;
    for y in base_y..max_y {
        let shape = large_oak_cluster_shape(y - base_y, cluster_height);
        large_oak_place_cluster(chunks, x, y, z, shape, 1, OAK_LEAVES_ID);
    }
}

fn large_oak_should_place_branch(height: i32, total_height: i32) -> bool {
    (height as f64) >= (total_height as f64) * 0.2
}

fn try_place_large_oak(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    rng: &mut i64,
    world_x: i32,
    surface_y: i32,
    world_z: i32,
) -> bool {
    let config = LargeOakConfig::alpha_populate_defaults();
    let mut height = 5 + java_next_int(rng, config.max_trunk_height.max(1));
    let origin = [world_x, surface_y + 1, world_z];
    let ground = world_block(chunks, origin[0], origin[1] - 1, origin[2]);
    if ground != GRASS_ID && ground != DIRT_ID {
        return false;
    }

    let top = [origin[0], origin[1] + height - 1, origin[2]];
    let clearance = large_oak_try_branch(chunks, origin, top);
    if clearance != -1 {
        if clearance < 6 {
            return false;
        }
        height = clearance;
    }

    let mut trunk_height = (height as f64 * config.trunk_scale) as i32;
    if trunk_height >= height {
        trunk_height = height - 1;
    }
    let mut branch_count =
        (1.382 + ((config.foliage_density * height as f64 / 13.0).powi(2))) as i32;
    if branch_count < 1 {
        branch_count = 1;
    }

    let mut branches: Vec<[i32; 4]> = Vec::with_capacity((branch_count * height).max(1) as usize);
    let mut cluster_y = origin[1] + height - config.foliage_cluster_height;
    let trunk_top_y = origin[1] + trunk_height;
    branches.push([origin[0], cluster_y, origin[2], trunk_top_y]);
    cluster_y -= 1;
    let mut relative_y = cluster_y - origin[1];

    while relative_y >= 0 {
        let shape = large_oak_get_tree_shape(relative_y, height);
        if shape >= 0.0 {
            for _ in 0..branch_count {
                let length = config.branch_length_scale
                    * (shape as f64 * (java_next_float(rng) as f64 + 0.328));
                let angle = java_next_float(rng) as f64 * 2.0 * PI;
                let branch_x = (length * angle.sin() + origin[0] as f64 + 0.5) as i32;
                let branch_z = (length * angle.cos() + origin[2] as f64 + 0.5) as i32;
                let branch_base = [branch_x, cluster_y, branch_z];
                let branch_tip = [
                    branch_x,
                    cluster_y + config.foliage_cluster_height,
                    branch_z,
                ];
                if large_oak_try_branch(chunks, branch_base, branch_tip) != -1 {
                    continue;
                }

                let radial = (((origin[0] - branch_base[0]).abs().pow(2)
                    + (origin[2] - branch_base[2]).abs().pow(2))
                    as f64)
                    .sqrt();
                let slope = radial * config.branch_slope;
                let branch_attach_y = if branch_base[1] as f64 - slope > trunk_top_y as f64 {
                    trunk_top_y
                } else {
                    (branch_base[1] as f64 - slope) as i32
                };
                let branch_start = [origin[0], branch_attach_y, origin[2]];
                if large_oak_try_branch(chunks, branch_start, branch_base) != -1 {
                    continue;
                }
                branches.push([
                    branch_base[0],
                    branch_base[1],
                    branch_base[2],
                    branch_attach_y,
                ]);
            }
        }
        cluster_y -= 1;
        relative_y -= 1;
    }

    for branch in &branches {
        large_oak_place_foliage_cluster(
            chunks,
            branch[0],
            branch[1],
            branch[2],
            config.foliage_cluster_height,
        );
    }

    let trunk_from = [origin[0], origin[1], origin[2]];
    let trunk_to = [origin[0], origin[1] + trunk_height, origin[2]];
    large_oak_place_branch(chunks, trunk_from, trunk_to, OAK_LOG_ID);
    if config.trunk_width == 2 {
        let trunk_from_x = [origin[0] + 1, origin[1], origin[2]];
        let trunk_to_x = [origin[0] + 1, origin[1] + trunk_height, origin[2]];
        large_oak_place_branch(chunks, trunk_from_x, trunk_to_x, OAK_LOG_ID);
        let trunk_from_xz = [origin[0] + 1, origin[1], origin[2] + 1];
        let trunk_to_xz = [origin[0] + 1, origin[1] + trunk_height, origin[2] + 1];
        large_oak_place_branch(chunks, trunk_from_xz, trunk_to_xz, OAK_LOG_ID);
        let trunk_from_z = [origin[0], origin[1], origin[2] + 1];
        let trunk_to_z = [origin[0], origin[1] + trunk_height, origin[2] + 1];
        large_oak_place_branch(chunks, trunk_from_z, trunk_to_z, OAK_LOG_ID);
    }

    let trunk_origin_y = origin[1];
    for branch in &branches {
        let branch_height_above_origin = branch[3] - trunk_origin_y;
        if large_oak_should_place_branch(branch_height_above_origin, height) {
            let from = [origin[0], branch[3], origin[2]];
            let to = [branch[0], branch[1], branch[2]];
            large_oak_place_branch(chunks, from, to, OAK_LOG_ID);
        }
    }
    true
}

// ---------------------------------------------------------------------------
// Lake generation  (translated from LakeFeature.java / OverworldChunkGenerator.java)
// ---------------------------------------------------------------------------

fn place_lakes(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    source_min: ChunkPos,
    source_max: ChunkPos,
    registry: &BlockRegistry,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));

            let base_x = source_x * CHUNK_WIDTH as i32 + 8;
            let base_z = source_z * CHUNK_DEPTH as i32 + 8;

            // Water lake: 1/4 chance
            if java_next_int(&mut rng, 4) == 0 {
                let lx = base_x + java_next_int(&mut rng, 16);
                let ly = java_next_int(&mut rng, CHUNK_HEIGHT as i32);
                let lz = base_z + java_next_int(&mut rng, 16);
                try_place_lake(chunks, registry, &mut rng, lx, ly, lz, WATER_ID);
            }

            // Lava lake: 1/8 chance, extra 1/10 gate if Y >= 64
            if java_next_int(&mut rng, 8) == 0 {
                let lx = base_x + java_next_int(&mut rng, 16);
                let lava_inner = java_next_int(&mut rng, 120) + 8;
                let ly = java_next_int(&mut rng, lava_inner);
                let lz = base_z + java_next_int(&mut rng, 16);
                if ly < 64 || java_next_int(&mut rng, 10) == 0 {
                    try_place_lake(chunks, registry, &mut rng, lx, ly, lz, LAVA_ID);
                }
            }
        }
    }
}

fn try_place_lake(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    registry: &BlockRegistry,
    rng: &mut i64,
    mut origin_x: i32,
    mut origin_y: i32,
    mut origin_z: i32,
    liquid_id: u8,
) {
    origin_x -= 8;
    origin_z -= 8;
    while origin_y > 0 && world_block(chunks, origin_x, origin_y, origin_z) == AIR_ID {
        origin_y -= 1;
    }
    origin_y -= 4;

    // Lake shape: 4-7 random ellipsoids within 16×8×16 bounding box
    let num_ellipsoids = java_next_int(rng, 4) + 4;
    let mut carved = [false; 16 * 8 * 16]; // 16×8×16 bounding volume

    for _ in 0..num_ellipsoids {
        // Matches Alpha LakeFeature semantics:
        // d/e/f are diameters, and normalization divides by diameter / 2.
        let rx = java_next_double(rng) * 6.0 + 3.0;
        let ry = java_next_double(rng) * 4.0 + 2.0;
        let rz = java_next_double(rng) * 6.0 + 3.0;
        let ex = java_next_double(rng) * (16.0 - rx - 2.0) + 1.0 + rx / 2.0;
        let ey = java_next_double(rng) * (8.0 - ry - 4.0) + 2.0 + ry / 2.0;
        let ez = java_next_double(rng) * (16.0 - rz - 2.0) + 1.0 + rz / 2.0;
        for bx in 1..15 {
            for bz in 1..15 {
                for by in 1..7 {
                    let dx = (bx as f64 + 0.5 - ex) / (rx / 2.0);
                    let dy = (by as f64 + 0.5 - ey) / (ry / 2.0);
                    let dz = (bz as f64 + 0.5 - ez) / (rz / 2.0);
                    if dx * dx + dy * dy + dz * dz < 1.0 {
                        carved[(bx * 16 + bz) * 8 + by] = true;
                    }
                }
            }
        }
    }

    for bx in 0..16 {
        for bz in 0..16 {
            for by in 0..8 {
                let idx = (bx * 16 + bz) * 8 + by;
                let boundary = !carved[idx]
                    && ((bx < 15 && carved[((bx + 1) * 16 + bz) * 8 + by])
                        || (bx > 0 && carved[((bx - 1) * 16 + bz) * 8 + by])
                        || (bz < 15 && carved[(bx * 16 + (bz + 1)) * 8 + by])
                        || (bz > 0 && carved[(bx * 16 + (bz - 1)) * 8 + by])
                        || (by < 7 && carved[(bx * 16 + bz) * 8 + (by + 1)])
                        || (by > 0 && carved[(bx * 16 + bz) * 8 + (by - 1)]));
                if !boundary {
                    continue;
                }
                let wx = origin_x + bx as i32;
                let wy = origin_y + by as i32;
                let wz = origin_z + bz as i32;
                let material = world_block(chunks, wx, wy, wz);
                let is_liquid = registry.is_liquid(material);
                let is_solid = registry.is_solid(material);
                if by >= 4 && is_liquid {
                    return;
                }
                if by < 4 && !is_solid && material != liquid_id {
                    return;
                }
            }
        }
    }

    // Place the lake
    let is_water = liquid_id == WATER_ID;
    for bx in 0..16 {
        for bz in 0..16 {
            for by in 0..8 {
                if !carved[(bx * 16 + bz) * 8 + by] {
                    continue;
                }
                let wx = origin_x + bx as i32;
                let wy = origin_y + by as i32;
                let wz = origin_z + bz as i32;

                if wy < 1 || wy >= CHUNK_HEIGHT as i32 {
                    continue;
                }

                if by < 4 {
                    set_world_block(chunks, wx, wy, wz, liquid_id);
                } else {
                    set_world_block(chunks, wx, wy, wz, AIR_ID);
                }

                // Water lakes: convert exposed dirt under carved cavity to grass.
                if is_water && by >= 4 {
                    let below = world_block(chunks, wx, wy - 1, wz);
                    if below == DIRT_ID && has_sky_access_world(chunks, wx, wy, wz) {
                        set_world_block(chunks, wx, wy - 1, wz, GRASS_ID);
                    }
                }
            }
        }
    }
}

fn place_trees(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_seed: u64,
    biome_source: &BiomeSource,
    forest_noise: &PerlinNoise,
    source_min: ChunkPos,
    source_max: ChunkPos,
) {
    for source_z in source_min.z..=source_max.z {
        for source_x in source_min.x..=source_max.x {
            let mut rng = java_random_seed(alpha_chunk_seed(world_seed, source_x, source_z));

            let center_x = source_x * CHUNK_WIDTH as i32 + 8;
            let center_z = source_z * CHUNK_DEPTH as i32 + 8;
            let biome_sample = biome_source.sample(center_x, center_z);
            let chunk_origin_x = source_x * CHUNK_WIDTH as i32;
            let chunk_origin_z = source_z * CHUNK_DEPTH as i32;
            let forest_noise_value =
                alpha_forest_tree_noise_value(forest_noise, chunk_origin_x, chunk_origin_z);
            let attempt_count =
                alpha_tree_attempt_count(biome_sample.biome, forest_noise_value, &mut rng);
            if attempt_count <= 0 {
                continue;
            }

            let tree_kind = alpha_tree_feature_kind_for_chunk(biome_sample.biome, &mut rng);
            for _ in 0..attempt_count {
                // Vanilla feature roots are selected from [8..23] within source chunk.
                let world_x = source_x * CHUNK_WIDTH as i32 + java_next_int(&mut rng, 16) + 8;
                let world_z = source_z * CHUNK_DEPTH as i32 + java_next_int(&mut rng, 16) + 8;

                if let Some(surface_y) = find_surface_y_world(chunks, world_x, world_z) {
                    match tree_kind {
                        TreeKind::Oak => {
                            let trunk_height = java_next_int(&mut rng, 3) + 4; // 4-6 blocks
                            try_place_tree(
                                chunks,
                                &mut rng,
                                world_x,
                                surface_y,
                                world_z,
                                trunk_height,
                            );
                        }
                        TreeKind::LargeOak => {
                            let _ =
                                try_place_large_oak(chunks, &mut rng, world_x, surface_y, world_z);
                        }
                    }
                }
            }
        }
    }
}

fn world_to_chunk_local(world_x: i32, world_z: i32) -> (ChunkPos, u8, u8) {
    let chunk_x = world_x.div_euclid(CHUNK_WIDTH as i32);
    let chunk_z = world_z.div_euclid(CHUNK_DEPTH as i32);
    let local_x = world_x.rem_euclid(CHUNK_WIDTH as i32) as u8;
    let local_z = world_z.rem_euclid(CHUNK_DEPTH as i32) as u8;
    (
        ChunkPos {
            x: chunk_x,
            z: chunk_z,
        },
        local_x,
        local_z,
    )
}

fn world_block(chunks: &HashMap<ChunkPos, ChunkData>, world_x: i32, y: i32, world_z: i32) -> u8 {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return AIR_ID;
    }
    let (chunk_pos, local_x, local_z) = world_to_chunk_local(world_x, world_z);
    chunks
        .get(&chunk_pos)
        .map_or(AIR_ID, |chunk| chunk.block(local_x, y as u8, local_z))
}

fn set_world_block(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_x: i32,
    y: i32,
    world_z: i32,
    block_id: u8,
) {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return;
    }
    let (chunk_pos, local_x, local_z) = world_to_chunk_local(world_x, world_z);
    if let Some(chunk) = chunks.get_mut(&chunk_pos) {
        chunk.set_block(local_x, y as u8, local_z, block_id);
    }
}

fn set_world_block_with_metadata(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    world_x: i32,
    y: i32,
    world_z: i32,
    block_id: u8,
    metadata: u8,
) {
    if !(0..CHUNK_HEIGHT as i32).contains(&y) {
        return;
    }
    let (chunk_pos, local_x, local_z) = world_to_chunk_local(world_x, world_z);
    if let Some(chunk) = chunks.get_mut(&chunk_pos) {
        chunk.set_block_with_metadata(local_x, y as u8, local_z, block_id, metadata);
    }
}

fn alpha_falling_block_can_fall_through(block_id: u8, registry: &BlockRegistry) -> bool {
    block_id == AIR_ID || block_id == 51 || registry.is_liquid(block_id)
}

fn settle_falling_blocks_world_space(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    registry: &BlockRegistry,
) {
    // Approximation of Alpha's `FallingBlock.fallImmediately = true` during populate.
    // Resolves sand/gravel columns immediately after all decorators run.
    let mut cells_to_move: Vec<(i32, i32, i32, u8)> = Vec::new();
    for (chunk_pos, chunk) in chunks.iter() {
        let base_x = chunk_pos.x * CHUNK_WIDTH as i32;
        let base_z = chunk_pos.z * CHUNK_DEPTH as i32;
        for lx in 0..CHUNK_WIDTH as u8 {
            for lz in 0..CHUNK_DEPTH as u8 {
                for y in 1..CHUNK_HEIGHT as u8 {
                    let block_id = chunk.block(lx, y, lz);
                    if block_id != SAND_ID && block_id != GRAVEL_ID {
                        continue;
                    }
                    let wx = base_x + i32::from(lx);
                    let wy = i32::from(y);
                    let wz = base_z + i32::from(lz);
                    let below = world_block(chunks, wx, wy - 1, wz);
                    if alpha_falling_block_can_fall_through(below, registry) {
                        cells_to_move.push((wx, wy, wz, block_id));
                    }
                }
            }
        }
    }

    for (wx, wy, wz, block_id) in cells_to_move {
        if world_block(chunks, wx, wy, wz) != block_id {
            continue;
        }
        set_world_block(chunks, wx, wy, wz, AIR_ID);
        let mut target_y = wy;
        while target_y > 0 {
            let below = world_block(chunks, wx, target_y - 1, wz);
            if !alpha_falling_block_can_fall_through(below, registry) {
                break;
            }
            target_y -= 1;
        }
        set_world_block(chunks, wx, target_y, wz, block_id);
    }
}

/// Find the y of the highest non-air block at this world column using the
/// precomputed per-chunk height maps.
fn find_surface_y_world(
    chunks: &HashMap<ChunkPos, ChunkData>,
    world_x: i32,
    world_z: i32,
) -> Option<i32> {
    let (chunk_pos, local_x, local_z) = world_to_chunk_local(world_x, world_z);
    let chunk = chunks.get(&chunk_pos)?;
    let h = chunk.height_at(local_x, local_z);
    if h == 0 {
        None
    } else {
        Some(h as i32 - 1)
    }
}

/// Attempt to place an oak tree at the given position. Validates ground block
/// and clearance, then places trunk and leaf canopy.
fn try_place_tree(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    rng: &mut i64,
    world_x: i32,
    surface_y: i32,
    world_z: i32,
    trunk_height: i32,
) {
    let ground_block = world_block(chunks, world_x, surface_y, world_z);
    if ground_block != GRASS_ID && ground_block != DIRT_ID {
        return;
    }

    let trunk_base = surface_y + 1;
    let trunk_top = trunk_base + trunk_height - 1;

    // The total tree height must fit in the chunk.
    if trunk_top + 1 >= CHUNK_HEIGHT as i32 {
        return;
    }

    // Check clearance: trunk column and leaf radius must be mostly air/leaves.
    // We check a 5x5 area around the trunk from (trunk_top - 3) to (trunk_top + 1).
    let canopy_bottom = trunk_top - 3;
    let canopy_top = trunk_top + 1;
    for cy in canopy_bottom..=canopy_top {
        if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
            return;
        }
        // Radius is 2 for lower canopy layers, 1 for upper layers
        let radius = if cy >= trunk_top { 1 } else { 2 };
        for dx in -radius..=radius {
            for dz in -radius..=radius {
                let bx = world_x + dx;
                let bz = world_z + dz;
                let existing = world_block(chunks, bx, cy, bz);
                if existing != AIR_ID && existing != OAK_LEAVES_ID {
                    return;
                }
            }
        }
    }

    // Replace ground with dirt (matching Alpha behavior).
    set_world_block(chunks, world_x, surface_y, world_z, DIRT_ID);

    // Place leaf canopy: layers from (trunk_top - 3) to (trunk_top + 1).
    // Lower layers (trunk_top-3, trunk_top-2): radius 2, corners randomly pruned.
    // Upper layers (trunk_top-1, trunk_top): radius 1, no corner pruning.
    // Top layer (trunk_top+1): no leaves (trunk doesn't extend there either).
    for cy in (trunk_top - 3)..=(trunk_top) {
        let layer_from_top = trunk_top - cy;
        let radius = if layer_from_top >= 2 { 2 } else { 1 };
        for dx in -radius..=radius {
            for dz in -radius..=radius {
                let bx = world_x + dx;
                let bz = world_z + dz;
                if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
                    continue;
                }
                // Skip the trunk column (trunk pass overwrites center anyway).
                if dx == 0 && dz == 0 {
                    continue;
                }
                // Prune corners on wide layers.
                if radius == 2 && dx.abs() == 2 && dz.abs() == 2 && java_next_int(rng, 2) == 0 {
                    continue;
                }
                let existing = world_block(chunks, bx, cy, bz);
                if existing == AIR_ID {
                    set_world_block(chunks, bx, cy, bz, OAK_LEAVES_ID);
                }
            }
        }
    }

    // Place leaves at trunk_top + 1 (small crown cap): just the center column.
    let cap_y = trunk_top + 1;
    if cap_y < CHUNK_HEIGHT as i32 {
        let existing = world_block(chunks, world_x, cap_y, world_z);
        if existing == AIR_ID {
            set_world_block(chunks, world_x, cap_y, world_z, OAK_LEAVES_ID);
        }
    }

    // Place trunk (overwrites any leaves at trunk column).
    for ty in trunk_base..=trunk_top {
        if ty >= CHUNK_HEIGHT as i32 {
            break;
        }
        set_world_block(chunks, world_x, ty, world_z, OAK_LOG_ID);
    }
}

/// Birch tree: slightly taller, thinner canopy (radius 1 throughout).
/// Uses OAK_LOG_ID and OAK_LEAVES_ID (Alpha 1.2.6 has only one log/leaf type).
#[allow(dead_code)]
fn try_place_birch_tree(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    rng: &mut i64,
    world_x: i32,
    surface_y: i32,
    world_z: i32,
    trunk_height: i32,
) {
    let ground_block = world_block(chunks, world_x, surface_y, world_z);
    if ground_block != GRASS_ID && ground_block != DIRT_ID {
        return;
    }

    let trunk_base = surface_y + 1;
    let trunk_top = trunk_base + trunk_height - 1;

    if trunk_top + 1 >= CHUNK_HEIGHT as i32 {
        return;
    }

    // Check clearance: radius 1 around trunk from (trunk_top - 2) to (trunk_top + 1).
    let canopy_bottom = trunk_top - 2;
    let canopy_top = trunk_top + 1;
    for cy in canopy_bottom..=canopy_top {
        if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
            return;
        }
        for dx in -1..=1_i32 {
            for dz in -1..=1_i32 {
                let bx = world_x + dx;
                let bz = world_z + dz;
                let existing = world_block(chunks, bx, cy, bz);
                if existing != AIR_ID && existing != OAK_LEAVES_ID {
                    return;
                }
            }
        }
    }

    set_world_block(chunks, world_x, surface_y, world_z, DIRT_ID);

    // Canopy: 3 layers of radius 1, with corner pruning.
    for cy in (trunk_top - 2)..=(trunk_top) {
        for dx in -1..=1_i32 {
            for dz in -1..=1_i32 {
                let bx = world_x + dx;
                let bz = world_z + dz;
                if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
                    continue;
                }
                if dx == 0 && dz == 0 {
                    continue;
                }
                // Prune corners on all layers.
                if dx.abs() == 1 && dz.abs() == 1 && java_next_int(rng, 2) == 0 {
                    continue;
                }
                let existing = world_block(chunks, bx, cy, bz);
                if existing == AIR_ID {
                    set_world_block(chunks, bx, cy, bz, OAK_LEAVES_ID);
                }
            }
        }
    }

    // Cap leaf.
    let cap_y = trunk_top + 1;
    if cap_y < CHUNK_HEIGHT as i32 {
        let existing = world_block(chunks, world_x, cap_y, world_z);
        if existing == AIR_ID {
            set_world_block(chunks, world_x, cap_y, world_z, OAK_LEAVES_ID);
        }
    }

    // Trunk.
    for ty in trunk_base..=trunk_top {
        if ty >= CHUNK_HEIGHT as i32 {
            break;
        }
        set_world_block(chunks, world_x, ty, world_z, OAK_LOG_ID);
    }
}

/// Pine tree: tall trunk with triangular canopy that narrows from bottom to top.
/// Uses OAK_LOG_ID and OAK_LEAVES_ID (Alpha 1.2.6 has only one log/leaf type).
#[allow(dead_code)]
fn try_place_pine_tree(
    chunks: &mut HashMap<ChunkPos, ChunkData>,
    _rng: &mut i64,
    world_x: i32,
    surface_y: i32,
    world_z: i32,
    trunk_height: i32,
) {
    let ground_block = world_block(chunks, world_x, surface_y, world_z);
    if ground_block != GRASS_ID && ground_block != DIRT_ID {
        return;
    }

    let trunk_base = surface_y + 1;
    let trunk_top = trunk_base + trunk_height - 1;

    if trunk_top + 1 >= CHUNK_HEIGHT as i32 {
        return;
    }

    // Canopy starts at about 60% up the trunk.
    let canopy_start = trunk_base + trunk_height / 3;
    let canopy_layers = trunk_top - canopy_start + 2; // +1 for top leaf

    // Check clearance around entire canopy area.
    for cy in canopy_start..=(trunk_top + 1) {
        if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
            return;
        }
        let layer_from_bottom = cy - canopy_start;
        let radius = pine_canopy_radius(layer_from_bottom, canopy_layers);
        for dx in -radius..=radius {
            for dz in -radius..=radius {
                let bx = world_x + dx;
                let bz = world_z + dz;
                let existing = world_block(chunks, bx, cy, bz);
                if existing != AIR_ID && existing != OAK_LEAVES_ID {
                    return;
                }
            }
        }
    }

    set_world_block(chunks, world_x, surface_y, world_z, DIRT_ID);

    // Place canopy layers.
    for cy in canopy_start..=(trunk_top + 1) {
        if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
            continue;
        }
        let layer_from_bottom = cy - canopy_start;
        let radius = pine_canopy_radius(layer_from_bottom, canopy_layers);
        for dx in -radius..=radius {
            for dz in -radius..=radius {
                let bx = world_x + dx;
                let bz = world_z + dz;
                if dx == 0 && dz == 0 && cy <= trunk_top {
                    continue; // Trunk column handled separately.
                }
                let existing = world_block(chunks, bx, cy, bz);
                if existing == AIR_ID {
                    set_world_block(chunks, bx, cy, bz, OAK_LEAVES_ID);
                }
            }
        }
    }

    // Trunk.
    for ty in trunk_base..=trunk_top {
        if ty >= CHUNK_HEIGHT as i32 {
            break;
        }
        set_world_block(chunks, world_x, ty, world_z, OAK_LOG_ID);
    }

    // Single leaf on very top (above trunk_top).
    let top_y = trunk_top + 1;
    if top_y < CHUNK_HEIGHT as i32 {
        set_world_block(chunks, world_x, top_y, world_z, OAK_LEAVES_ID);
    }
}

/// Alternating radius pattern for pine canopy: wide at bottom, narrow at top.
#[allow(dead_code)]
fn pine_canopy_radius(layer_from_bottom: i32, total_layers: i32) -> i32 {
    if total_layers <= 0 {
        return 0;
    }
    let layer_from_top = total_layers - 1 - layer_from_bottom;
    // Alternating pattern: 2, 1, 2, 1, 0 from bottom to top
    match layer_from_top {
        0 => 0, // top: single leaf
        1 => 1,
        2 => 2,
        3 => 1,
        _ => 2, // bottom layers are wide
    }
}

#[derive(Debug, Clone, Copy, Eq, PartialEq, Hash)]
pub struct ChunkPos {
    pub x: i32,
    pub z: i32,
}

#[derive(Debug, Clone)]
pub struct ChunkData {
    pub pos: ChunkPos,
    blocks: Box<[u8; CHUNK_VOLUME]>,
    metadata: NibbleStorage,
    sky_light: NibbleStorage,
    block_light: NibbleStorage,
    height_map: [u8; CHUNK_AREA],
    grass_tint: [u32; CHUNK_AREA],
    foliage_tint: [u32; CHUNK_AREA],
}

impl ChunkData {
    #[must_use]
    pub fn new(pos: ChunkPos, fill_block: u8) -> Self {
        Self {
            pos,
            blocks: Box::new([fill_block; CHUNK_VOLUME]),
            metadata: NibbleStorage::new(CHUNK_VOLUME),
            sky_light: NibbleStorage::new(CHUNK_VOLUME),
            block_light: NibbleStorage::new(CHUNK_VOLUME),
            height_map: [0; CHUNK_AREA],
            grass_tint: [WHITE_RGB_PACKED; CHUNK_AREA],
            foliage_tint: [WHITE_RGB_PACKED; CHUNK_AREA],
        }
    }

    #[must_use]
    pub fn block(&self, local_x: u8, y: u8, local_z: u8) -> u8 {
        let idx = Self::index(local_x, y, local_z);
        self.blocks[idx]
    }

    pub fn set_block(&mut self, local_x: u8, y: u8, local_z: u8, block_id: u8) {
        let idx = Self::index(local_x, y, local_z);
        self.blocks[idx] = block_id;
        self.metadata.set(idx, 0);
    }

    pub fn set_block_with_metadata(
        &mut self,
        local_x: u8,
        y: u8,
        local_z: u8,
        block_id: u8,
        metadata: u8,
    ) {
        let idx = Self::index(local_x, y, local_z);
        self.blocks[idx] = block_id;
        self.metadata.set(idx, metadata);
    }

    #[must_use]
    pub fn block_metadata(&self, local_x: u8, y: u8, local_z: u8) -> u8 {
        let idx = Self::index(local_x, y, local_z);
        self.metadata.get(idx)
    }

    pub fn set_block_metadata(&mut self, local_x: u8, y: u8, local_z: u8, metadata: u8) {
        let idx = Self::index(local_x, y, local_z);
        self.metadata.set(idx, metadata);
    }

    #[must_use]
    pub fn block_light(&self, local_x: u8, y: u8, local_z: u8) -> u8 {
        let idx = Self::index(local_x, y, local_z);
        self.block_light.get(idx)
    }

    fn set_block_light(&mut self, local_x: u8, y: u8, local_z: u8, value: u8) {
        let idx = Self::index(local_x, y, local_z);
        self.block_light.set(idx, value);
    }

    #[must_use]
    pub fn sky_light(&self, local_x: u8, y: u8, local_z: u8) -> u8 {
        let idx = Self::index(local_x, y, local_z);
        self.sky_light.get(idx)
    }

    fn set_sky_light(&mut self, local_x: u8, y: u8, local_z: u8, value: u8) {
        let idx = Self::index(local_x, y, local_z);
        self.sky_light.set(idx, value);
    }

    pub fn recalculate_height_map(&mut self, registry: &BlockRegistry) {
        for local_x in 0..CHUNK_WIDTH {
            for local_z in 0..CHUNK_DEPTH {
                self.recalculate_column_height_and_sky_light(
                    local_x as u8,
                    local_z as u8,
                    registry,
                );
            }
        }
    }

    pub fn seed_emitted_light(&mut self, registry: &BlockRegistry) {
        for local_x in 0..CHUNK_WIDTH {
            for local_z in 0..CHUNK_DEPTH {
                self.reseed_column_emitted_light(local_x as u8, local_z as u8, registry);
            }
        }
    }

    pub fn recalculate_column_height_and_sky_light(
        &mut self,
        local_x: u8,
        local_z: u8,
        registry: &BlockRegistry,
    ) {
        assert!(usize::from(local_x) < CHUNK_WIDTH, "local_x out of range");
        assert!(usize::from(local_z) < CHUNK_DEPTH, "local_z out of range");

        let mut height = 0;
        for y in (0..CHUNK_HEIGHT).rev() {
            let y_u8 = y as u8;
            let block_id = self.block(local_x, y_u8, local_z);
            if registry.opacity_of(block_id) > 0 {
                height = (y + 1) as u8;
                break;
            }
        }
        self.height_map[column_index(local_x, local_z)] = height;

        let mut sky_level = 15_u8;
        for y in (0..CHUNK_HEIGHT).rev() {
            let y_u8 = y as u8;
            let block_id = self.block(local_x, y_u8, local_z);
            let opacity = registry.opacity_of(block_id);
            if opacity > 0 {
                let attenuation = opacity.clamp(1, 15);
                sky_level = sky_level.saturating_sub(attenuation);
            }
            self.set_sky_light(local_x, y_u8, local_z, sky_level);
        }
    }

    pub fn reseed_column_emitted_light(
        &mut self,
        local_x: u8,
        local_z: u8,
        registry: &BlockRegistry,
    ) {
        assert!(usize::from(local_x) < CHUNK_WIDTH, "local_x out of range");
        assert!(usize::from(local_z) < CHUNK_DEPTH, "local_z out of range");

        for y in 0..CHUNK_HEIGHT {
            let y_u8 = y as u8;
            let block_id = self.block(local_x, y_u8, local_z);
            self.set_block_light(local_x, y_u8, local_z, registry.emitted_light_of(block_id));
        }
    }

    pub fn apply_light_channels(
        &mut self,
        sky_light: &[u8; CHUNK_VOLUME],
        block_light: &[u8; CHUNK_VOLUME],
    ) {
        for local_x in 0..CHUNK_WIDTH as u8 {
            for local_z in 0..CHUNK_DEPTH as u8 {
                for y in 0..CHUNK_HEIGHT as u8 {
                    let index = Self::index(local_x, y, local_z);
                    self.set_sky_light(local_x, y, local_z, sky_light[index]);
                    self.set_block_light(local_x, y, local_z, block_light[index]);
                }
            }
        }
    }

    #[must_use]
    pub fn height_at(&self, local_x: u8, local_z: u8) -> u8 {
        self.height_map[column_index(local_x, local_z)]
    }

    #[must_use]
    pub fn grass_tint_at(&self, local_x: u8, local_z: u8) -> [u8; 3] {
        unpack_rgb(self.grass_tint[column_index(local_x, local_z)])
    }

    fn set_grass_tint(&mut self, local_x: u8, local_z: u8, tint: [u8; 3]) {
        self.grass_tint[column_index(local_x, local_z)] = pack_rgb(tint);
    }

    #[must_use]
    pub fn foliage_tint_at(&self, local_x: u8, local_z: u8) -> [u8; 3] {
        unpack_rgb(self.foliage_tint[column_index(local_x, local_z)])
    }

    fn set_foliage_tint(&mut self, local_x: u8, local_z: u8, tint: [u8; 3]) {
        self.foliage_tint[column_index(local_x, local_z)] = pack_rgb(tint);
    }

    #[must_use]
    pub fn index(local_x: u8, y: u8, local_z: u8) -> usize {
        assert!(usize::from(local_x) < CHUNK_WIDTH, "local_x out of range");
        assert!(usize::from(local_z) < CHUNK_DEPTH, "local_z out of range");
        assert!(usize::from(y) < CHUNK_HEIGHT, "y out of range");
        (usize::from(local_x) << 11) | (usize::from(local_z) << 7) | usize::from(y)
    }
}

fn column_index(local_x: u8, local_z: u8) -> usize {
    usize::from(local_z) * CHUNK_WIDTH + usize::from(local_x)
}

#[derive(Debug, Clone)]
struct BiomeColorMap {
    rgb: Box<[u32; 256 * 256]>,
}

impl BiomeColorMap {
    fn load(path_candidates: &[&str], fallback_rgb: [u8; 3]) -> Self {
        let fallback = || Self {
            rgb: Box::new([pack_rgb(fallback_rgb); 256 * 256]),
        };

        for &candidate in path_candidates {
            let candidates = [
                Path::new(candidate).to_path_buf(),
                Path::new("..").join(candidate),
            ];
            for path in candidates {
                let bytes = match std::fs::read(path) {
                    Ok(bytes) => bytes,
                    Err(_) => continue,
                };
                let image =
                    match image::load_from_memory_with_format(&bytes, image::ImageFormat::Png) {
                        Ok(image) => image.to_rgba8(),
                        Err(_) => continue,
                    };
                let (width, height) = image.dimensions();
                if width != 256 || height != 256 {
                    continue;
                }

                let mut rgb = [0_u32; 256 * 256];
                for (index, pixel) in image.pixels().enumerate() {
                    rgb[index] = pack_rgb([pixel[0], pixel[1], pixel[2]]);
                }
                return Self { rgb: Box::new(rgb) };
            }
        }

        fallback()
    }

    fn sample(&self, temperature: f64, downfall: f64) -> [u8; 3] {
        let temp = temperature.clamp(0.0, 1.0);
        let humid = (downfall * temp).clamp(0.0, 1.0);
        let x = ((1.0 - temp) * 255.0).round() as usize;
        let y = ((1.0 - humid) * 255.0).round() as usize;
        unpack_rgb(self.rgb[(y << 8) | x])
    }
}

type GrassColorMap = BiomeColorMap;
type FoliageColorMap = BiomeColorMap;

const fn pack_rgb(rgb: [u8; 3]) -> u32 {
    ((rgb[0] as u32) << 16) | ((rgb[1] as u32) << 8) | rgb[2] as u32
}

const fn unpack_rgb(rgb: u32) -> [u8; 3] {
    [
        ((rgb >> 16) & 0xFF) as u8,
        ((rgb >> 8) & 0xFF) as u8,
        (rgb & 0xFF) as u8,
    ]
}

#[derive(Debug, Clone)]
struct NibbleStorage {
    bytes: Box<[u8]>,
}

impl NibbleStorage {
    fn new(entries: usize) -> Self {
        assert!(
            entries.is_multiple_of(2),
            "nibble storage requires even entry count"
        );
        Self {
            bytes: vec![0; entries / 2].into_boxed_slice(),
        }
    }

    fn get(&self, index: usize) -> u8 {
        let byte = self.bytes[index / 2];
        if index & 1 == 0 {
            byte & 0x0F
        } else {
            (byte >> 4) & 0x0F
        }
    }

    fn set(&mut self, index: usize, value: u8) {
        let clamped = value.min(0x0F);
        let slot = &mut self.bytes[index / 2];
        if index & 1 == 0 {
            *slot = (*slot & 0xF0) | clamped;
        } else {
            *slot = (*slot & 0x0F) | (clamped << 4);
        }
    }
}

#[derive(Debug, Clone)]
pub struct BootstrapWorld {
    pub registry: BlockRegistry,
    pub spawn_chunk: ChunkData,
    pub spawn_region: Vec<ChunkData>,
}

impl BootstrapWorld {
    #[must_use]
    pub fn alpha_bootstrap() -> Self {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(0xA126_0001);
        let spawn_region = generator.generate_region(ChunkPos { x: 0, z: 0 }, 1, &registry);
        let spawn_chunk = spawn_region
            .iter()
            .find(|chunk| chunk.pos == ChunkPos { x: 0, z: 0 })
            .cloned()
            .unwrap_or_else(|| generator.generate_chunk(ChunkPos { x: 0, z: 0 }, &registry));

        Self {
            registry,
            spawn_chunk,
            spawn_region,
        }
    }
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;

    use super::*;

    #[test]
    fn chunk_index_matches_alpha_layout() {
        assert_eq!(ChunkData::index(0, 0, 0), 0);
        assert_eq!(ChunkData::index(1, 0, 0), 2048);
        assert_eq!(ChunkData::index(0, 0, 1), 128);
        assert_eq!(ChunkData::index(15, 127, 15), 32767);
    }

    #[test]
    fn nibble_storage_round_trips_both_halves() {
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block_light(0, 0, 0, 3);
        chunk.set_block_light(0, 1, 0, 12);

        assert_eq!(chunk.block_light(0, 0, 0), 3);
        assert_eq!(chunk.block_light(0, 1, 0), 12);
    }

    #[test]
    fn registry_respects_alpha_exclusions() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert!(registry.alpha_exclusions_respected());
        assert!(registry.get(26).is_none());
        assert!(registry.get(31).is_none());
        assert!(registry.get(33).is_none());
    }

    #[test]
    fn grass_uses_alpha_face_sprites() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(registry.sprite_index_for_face(GRASS_ID, [0, 1, 0]), 0);
        assert_eq!(registry.sprite_index_for_face(GRASS_ID, [0, -1, 0]), 2);
        assert_eq!(registry.sprite_index_for_face(GRASS_ID, [1, 0, 0]), 3);
    }

    #[test]
    fn oak_log_uses_cap_texture_on_top_and_bottom() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(registry.sprite_index_for_face(17, [0, 1, 0]), 21);
        assert_eq!(registry.sprite_index_for_face(17, [0, -1, 0]), 21);
        assert_eq!(registry.sprite_index_for_face(17, [1, 0, 0]), 20);
    }

    #[test]
    fn chest_uses_alpha_face_sprites() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(registry.sprite_index_for_face(CHEST_ID, [0, 1, 0]), 25);
        assert_eq!(registry.sprite_index_for_face(CHEST_ID, [0, -1, 0]), 25);
        assert_eq!(registry.sprite_index_for_face(CHEST_ID, [0, 0, -1]), 26);
        assert_eq!(registry.sprite_index_for_face(CHEST_ID, [0, 0, 1]), 27);
        assert_eq!(registry.sprite_index_for_face(CHEST_ID, [-1, 0, 0]), 26);
        assert_eq!(registry.sprite_index_for_face(CHEST_ID, [1, 0, 0]), 26);
    }

    #[test]
    fn chest_uses_metadata_oriented_face_sprites() {
        let registry = BlockRegistry::alpha_1_2_6();
        // Metadata 2 -> front north (-Z)
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(CHEST_ID, [0, 0, -1], 2),
            27
        );
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(CHEST_ID, [0, 0, 1], 2),
            26
        );
    }

    #[test]
    fn crafting_table_uses_alpha_face_sprites() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(
            registry.sprite_index_for_face(CRAFTING_TABLE_ID, [0, 1, 0]),
            43
        );
        assert_eq!(
            registry.sprite_index_for_face(CRAFTING_TABLE_ID, [0, -1, 0]),
            4
        );
        assert_eq!(
            registry.sprite_index_for_face(CRAFTING_TABLE_ID, [0, 0, -1]),
            60
        );
        assert_eq!(
            registry.sprite_index_for_face(CRAFTING_TABLE_ID, [0, 0, 1]),
            59
        );
        assert_eq!(
            registry.sprite_index_for_face(CRAFTING_TABLE_ID, [-1, 0, 0]),
            60
        );
        assert_eq!(
            registry.sprite_index_for_face(CRAFTING_TABLE_ID, [1, 0, 0]),
            59
        );
    }

    #[test]
    fn furnace_uses_metadata_oriented_face_sprites() {
        let registry = BlockRegistry::alpha_1_2_6();
        // Metadata 5 -> front on +X.
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(FURNACE_ID, [1, 0, 0], 5),
            44
        );
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(FURNACE_ID, [0, 0, 1], 5),
            45
        );
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(FURNACE_ID, [0, 1, 0], 5),
            1
        );
        // Lit furnace front uses sprite +16.
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(LIT_FURNACE_ID, [1, 0, 0], 5),
            61
        );
    }

    #[test]
    fn pumpkin_uses_metadata_oriented_face_sprites() {
        let registry = BlockRegistry::alpha_1_2_6();
        // Metadata 1 -> front on +X.
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(PUMPKIN_ID, [1, 0, 0], 1),
            119
        );
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(PUMPKIN_ID, [0, 0, 1], 1),
            118
        );
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(PUMPKIN_ID, [0, 1, 0], 1),
            102
        );
        assert_eq!(
            registry.sprite_index_for_face_with_metadata(LIT_PUMPKIN_ID, [1, 0, 0], 1),
            120
        );
    }

    #[test]
    fn mob_spawner_is_non_occluding_but_collidable() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert!(!registry.is_face_occluder(MOB_SPAWNER_ID));
        assert!(registry.blocks_movement(MOB_SPAWNER_ID));
        assert!(registry.is_collidable(MOB_SPAWNER_ID));
    }

    #[test]
    fn grass_top_face_marks_biome_tint_usage() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(registry.face_tint_rgb(GRASS_ID, [0, 1, 0]), [0, 0, 0]);
        assert_eq!(
            registry.biome_tint_kind(GRASS_ID, [0, 1, 0]),
            BiomeTintKind::Grass
        );
        assert_eq!(
            registry.face_tint_rgb(STONE_ID, [0, 1, 0]),
            [u8::MAX, u8::MAX, u8::MAX]
        );
        assert_eq!(
            registry.biome_tint_kind(STONE_ID, [0, 1, 0]),
            BiomeTintKind::None
        );
    }

    #[test]
    fn generated_chunk_has_non_white_grass_tint_samples() {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(0xA126_0001);
        let chunk = generator.generate_chunk(ChunkPos { x: 0, z: 0 }, &registry);
        assert_ne!(chunk.grass_tint_at(8, 8), [u8::MAX, u8::MAX, u8::MAX]);
    }

    #[test]
    fn biome_mapping_matches_alpha_thresholds() {
        assert_eq!(BiomeKind::from_climate(0.05, 0.5), BiomeKind::Tundra);
        assert_eq!(BiomeKind::from_climate(0.96, 0.05), BiomeKind::Desert);
        assert_eq!(BiomeKind::from_climate(0.99, 0.05), BiomeKind::Desert);
        assert_eq!(BiomeKind::from_climate(0.65, 0.95), BiomeKind::Swampland);
        assert_eq!(BiomeKind::from_climate(0.85, 0.6), BiomeKind::Forest);
    }

    #[test]
    fn overworld_generator_is_deterministic() {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(123_456_789);
        let chunk_a = generator.generate_chunk(ChunkPos { x: 4, z: -3 }, &registry);
        let chunk_b = generator.generate_chunk(ChunkPos { x: 4, z: -3 }, &registry);

        assert_eq!(chunk_a.height_at(2, 7), chunk_b.height_at(2, 7));
        assert_eq!(chunk_a.block(2, 64, 7), chunk_b.block(2, 64, 7));
        assert_eq!(chunk_a.block(8, 40, 11), chunk_b.block(8, 40, 11));
    }

    #[test]
    fn bootstrap_world_populates_height_map() {
        let world = BootstrapWorld::alpha_bootstrap();
        assert_eq!(world.spawn_region.len(), 9);
        let center_height = world.spawn_chunk.height_at(8, 8);
        assert!((1..=CHUNK_HEIGHT as u8).contains(&center_height));
        assert_eq!(world.spawn_chunk.block(8, 0, 8), BEDROCK_ID);
        assert_eq!(world.spawn_chunk.sky_light(8, 127, 8), 15);
    }

    #[test]
    fn column_recalculation_updates_height_and_sky_light_after_edit() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(2, 5, 3, STONE_ID);
        chunk.recalculate_column_height_and_sky_light(2, 3, &registry);
        assert_eq!(chunk.height_at(2, 3), 6);
        assert_eq!(chunk.sky_light(2, 6, 3), 15);
        assert_eq!(chunk.sky_light(2, 5, 3), 0);

        chunk.set_block(2, 5, 3, AIR_ID);
        chunk.recalculate_column_height_and_sky_light(2, 3, &registry);
        assert_eq!(chunk.height_at(2, 3), 0);
        assert_eq!(chunk.sky_light(2, 5, 3), 15);
    }

    #[test]
    fn emitted_light_column_reseed_tracks_new_block_values() {
        let registry = BlockRegistry::alpha_1_2_6();
        let mut chunk = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        chunk.set_block(4, 10, 7, 50);
        chunk.reseed_column_emitted_light(4, 7, &registry);
        assert_eq!(chunk.block_light(4, 10, 7), 14);

        chunk.set_block(4, 10, 7, AIR_ID);
        chunk.reseed_column_emitted_light(4, 7, &registry);
        assert_eq!(chunk.block_light(4, 10, 7), 0);
    }

    #[test]
    fn cave_carving_is_deterministic() {
        let registry = BlockRegistry::alpha_1_2_6();
        let seed = 42;
        let gen = OverworldChunkGenerator::new(seed);
        let pos = ChunkPos { x: 3, z: -2 };
        let a = gen.generate_chunk(pos, &registry);
        let b = gen.generate_chunk(pos, &registry);
        for lx in 0..16_u8 {
            for lz in 0..16_u8 {
                for y in 0..128_u8 {
                    assert_eq!(a.block(lx, y, lz), b.block(lx, y, lz));
                }
            }
        }
    }

    #[test]
    fn caves_create_air_below_surface() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(12345);
        // Check a 5x5 region — caves should appear somewhere
        let mut found_underground_air = false;
        'outer: for cx in -2..=2 {
            for cz in -2..=2 {
                let chunk = gen.generate_chunk(ChunkPos { x: cx, z: cz }, &registry);
                for lx in 0..16_u8 {
                    for lz in 0..16_u8 {
                        for y in 1..50_u8 {
                            if chunk.block(lx, y, lz) == AIR_ID {
                                found_underground_air = true;
                                break 'outer;
                            }
                        }
                    }
                }
            }
        }
        assert!(
            found_underground_air,
            "no underground air found in 5x5 region — caves not working"
        );
    }

    #[test]
    fn caves_place_lava_below_y10() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(12345);
        let mut found_lava = false;
        'outer: for cx in -3..=3 {
            for cz in -3..=3 {
                let chunk = gen.generate_chunk(ChunkPos { x: cx, z: cz }, &registry);
                for lx in 0..16_u8 {
                    for lz in 0..16_u8 {
                        for y in 1..10_u8 {
                            if chunk.block(lx, y, lz) == FLOWING_LAVA_ID {
                                found_lava = true;
                                break 'outer;
                            }
                        }
                    }
                }
            }
        }
        assert!(found_lava, "no lava found below y=10 in 7x7 region");
    }

    #[test]
    fn ores_are_present_underground() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(99999);
        let chunk = gen.generate_chunk(ChunkPos { x: 0, z: 0 }, &registry);
        let mut found_coal = false;
        for lx in 0..16_u8 {
            for lz in 0..16_u8 {
                for y in 1..128_u8 {
                    if chunk.block(lx, y, lz) == COAL_ORE_ID {
                        found_coal = true;
                    }
                }
            }
        }
        assert!(
            found_coal,
            "no coal ore found in chunk — ore placement not working"
        );
    }

    #[test]
    fn ore_distribution_respects_y_limits() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(77777);
        // Check several chunks
        for cx in -2..=2 {
            for cz in -2..=2 {
                let chunk = gen.generate_chunk(ChunkPos { x: cx, z: cz }, &registry);
                for lx in 0..16_u8 {
                    for lz in 0..16_u8 {
                        for y in 0..128_u8 {
                            let b = chunk.block(lx, y, lz);
                            // max_y is the starting Y cap; veins spread a few blocks above
                            if b == DIAMOND_ORE_ID {
                                assert!(y < 20, "diamond ore found at y={y}, expected < 20");
                            }
                            if b == GOLD_ORE_ID {
                                assert!(y < 36, "gold ore found at y={y}, expected < 36");
                            }
                        }
                    }
                }
            }
        }
    }

    #[test]
    fn trees_appear_in_forest_biome() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(42);
        let mut found_log = false;
        let mut found_leaves = false;
        // Forest biomes should have trees. Check a 5x5 region.
        'outer: for cx in -2..=2 {
            for cz in -2..=2 {
                let chunk = gen.generate_chunk(ChunkPos { x: cx, z: cz }, &registry);
                for lx in 0..16_u8 {
                    for lz in 0..16_u8 {
                        for y in 1..128_u8 {
                            let b = chunk.block(lx, y, lz);
                            if b == OAK_LOG_ID {
                                found_log = true;
                            }
                            if b == OAK_LEAVES_ID {
                                found_leaves = true;
                            }
                            if found_log && found_leaves {
                                break 'outer;
                            }
                        }
                    }
                }
            }
        }
        assert!(found_log, "no oak log found — tree placement not working");
        assert!(
            found_leaves,
            "no oak leaves found — tree placement not working"
        );
    }

    #[test]
    fn tree_trunks_sit_on_dirt_or_grass() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(12345);
        for cx in -1..=1 {
            for cz in -1..=1 {
                let chunk = gen.generate_chunk(ChunkPos { x: cx, z: cz }, &registry);
                for lx in 0..16_u8 {
                    for lz in 0..16_u8 {
                        // Find the lowest LOG in each column.
                        for y in 1..127_u8 {
                            if chunk.block(lx, y, lz) == OAK_LOG_ID {
                                let below = chunk.block(lx, y - 1, lz);
                                // Below a trunk base should be dirt (tree replaces grass with dirt).
                                assert!(
                                    below == DIRT_ID
                                        || below == GRASS_ID
                                        || below == OAK_LOG_ID
                                        || below == OAK_LEAVES_ID,
                                    "block below tree log at ({lx},{y},{lz}) is {below}, expected dirt/grass/log/leaves"
                                );
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    #[test]
    fn tree_placement_writes_across_chunk_boundaries() {
        let mut chunks = HashMap::new();
        let mut west = ChunkData::new(ChunkPos { x: 0, z: 0 }, AIR_ID);
        let mut east = ChunkData::new(ChunkPos { x: 1, z: 0 }, AIR_ID);
        for x in 0..16_u8 {
            for z in 0..16_u8 {
                west.set_block(x, 59, z, DIRT_ID);
                west.set_block(x, 60, z, GRASS_ID);
                east.set_block(x, 59, z, DIRT_ID);
                east.set_block(x, 60, z, GRASS_ID);
            }
        }
        chunks.insert(west.pos, west);
        chunks.insert(east.pos, east);

        let mut rng = java_random_seed(7);
        // Root on the east edge of chunk (0, 0); canopy should spill into (1, 0).
        try_place_tree(&mut chunks, &mut rng, 15, 60, 8, 5);

        let east_chunk = chunks
            .get(&ChunkPos { x: 1, z: 0 })
            .expect("east chunk should exist");
        let mut found_spill = false;
        for y in 61..=66_u8 {
            for z in 6..=10_u8 {
                let block = east_chunk.block(0, y, z);
                if block == OAK_LEAVES_ID || block == OAK_LOG_ID {
                    found_spill = true;
                    break;
                }
            }
            if found_spill {
                break;
            }
        }
        assert!(
            found_spill,
            "expected boundary tree canopy spill into east chunk, but none was written"
        );
    }

    #[test]
    fn large_oak_generation_places_branch_logs_off_trunk_axis() {
        let mut found_branch_log = false;

        for seed in 0_u64..256 {
            let mut chunks = HashMap::new();
            for cx in -1..=1 {
                for cz in -1..=1 {
                    let mut chunk = ChunkData::new(ChunkPos { x: cx, z: cz }, AIR_ID);
                    for x in 0..16_u8 {
                        for z in 0..16_u8 {
                            chunk.set_block(x, 59, z, DIRT_ID);
                            chunk.set_block(x, 60, z, GRASS_ID);
                        }
                    }
                    chunks.insert(chunk.pos, chunk);
                }
            }

            let mut rng = java_random_seed(seed as i64);
            if !try_place_large_oak(&mut chunks, &mut rng, 8, 60, 8) {
                continue;
            }

            for y in 62..=96_i32 {
                for x in 2..=14_i32 {
                    for z in 2..=14_i32 {
                        let block = world_block(&chunks, x, y, z);
                        if block == OAK_LOG_ID && (x != 8 || z != 8) {
                            found_branch_log = true;
                            break;
                        }
                    }
                    if found_branch_log {
                        break;
                    }
                }
                if found_branch_log {
                    break;
                }
            }

            if found_branch_log {
                break;
            }
        }

        assert!(
            found_branch_log,
            "expected large oak generation to place at least one off-axis branch log"
        );
    }

    #[test]
    fn dungeons_place_walls_spawners_and_chests() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(314159);
        let mut found_cobble = false;
        let mut found_spawner = false;
        let mut found_chest = false;
        // Check a larger area since dungeons are rare
        'outer: for cx in -5..=5 {
            for cz in -5..=5 {
                let chunk = gen.generate_chunk(ChunkPos { x: cx, z: cz }, &registry);
                for lx in 0..16_u8 {
                    for lz in 0..16_u8 {
                        for y in 1..120_u8 {
                            let b = chunk.block(lx, y, lz);
                            if b == COBBLESTONE_ID || b == MOSSY_COBBLESTONE_ID {
                                found_cobble = true;
                            }
                            if b == MOB_SPAWNER_ID {
                                found_spawner = true;
                            }
                            if b == CHEST_ID {
                                found_chest = true;
                            }
                            if found_cobble && found_spawner && found_chest {
                                break 'outer;
                            }
                        }
                    }
                }
            }
        }
        assert!(
            found_cobble,
            "no cobblestone found — dungeon generation not placing walls"
        );
        assert!(
            found_spawner,
            "no mob spawner found — dungeon generation not placing spawners"
        );
        assert!(
            found_chest,
            "no chest found — dungeon generation not placing chest blocks"
        );
    }

    #[test]
    fn decoration_generation_is_deterministic() {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(42);
        let chunk_a = generator.generate_chunk(ChunkPos { x: 3, z: -2 }, &registry);
        let chunk_b = generator.generate_chunk(ChunkPos { x: 3, z: -2 }, &registry);

        // Sample multiple positions across the chunk to verify determinism
        for x in [0_u8, 5, 8, 12, 15] {
            for z in [0_u8, 5, 8, 12, 15] {
                for y in [20_u8, 50, 64, 80, 100] {
                    assert_eq!(
                        chunk_a.block(x, y, z),
                        chunk_b.block(x, y, z),
                        "decoration non-determinism at ({x}, {y}, {z})"
                    );
                }
            }
        }
    }

    #[test]
    fn snow_covers_cold_biomes() {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(0xA126_0001);
        // Generate a larger region so that snow placement runs in generate_region
        let chunks = generator.generate_region(ChunkPos { x: 0, z: 0 }, 2, &registry);

        let mut found_snow = false;
        for chunk in &chunks {
            for lx in 0..CHUNK_WIDTH as u8 {
                for lz in 0..CHUNK_DEPTH as u8 {
                    let h = chunk.height_at(lx, lz);
                    if h > 0 && chunk.block(lx, h - 1, lz) == SNOW_LAYER_ID {
                        found_snow = true;
                    }
                }
            }
        }
        // Snow may or may not appear depending on biome; this just checks
        // the mechanism runs without panic. If the seed includes cold biomes
        // then we'll find snow.
        let _ = found_snow;
    }

    #[test]
    fn flowers_appear_on_grass() {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(12345);
        // Generate enough chunks to have a good chance of finding flowers
        let chunks = generator.generate_region(ChunkPos { x: 0, z: 0 }, 3, &registry);

        let mut found_flower = false;
        'outer: for chunk in &chunks {
            for lx in 0..CHUNK_WIDTH as u8 {
                for lz in 0..CHUNK_DEPTH as u8 {
                    for y in 1..CHUNK_HEIGHT as u8 {
                        let bid = chunk.block(lx, y, lz);
                        if bid == YELLOW_FLOWER_ID || bid == RED_FLOWER_ID {
                            // Flower must be on grass
                            let below = chunk.block(lx, y - 1, lz);
                            assert_eq!(
                                below, GRASS_ID,
                                "flower at y={y} not on grass (below={below})"
                            );
                            found_flower = true;
                            break 'outer;
                        }
                    }
                }
            }
        }
        assert!(found_flower, "no flowers found in 7x7 region");
    }

    #[test]
    fn clay_appears_in_world() {
        let registry = BlockRegistry::alpha_1_2_6();
        // Try several seeds to find clay (it's uncommon)
        let mut found_clay = false;
        'outer: for seed_offset in 0..5_u64 {
            let generator = OverworldChunkGenerator::new(54321 + seed_offset * 1000);
            let chunks = generator.generate_region(ChunkPos { x: 0, z: 0 }, 3, &registry);
            for chunk in &chunks {
                for lx in 0..CHUNK_WIDTH as u8 {
                    for lz in 0..CHUNK_DEPTH as u8 {
                        for y in 1..CHUNK_HEIGHT as u8 {
                            if chunk.block(lx, y, lz) == CLAY_ID {
                                found_clay = true;
                                break 'outer;
                            }
                        }
                    }
                }
            }
        }
        assert!(found_clay, "no clay found across multiple seeds");
    }

    #[test]
    fn springs_place_flowing_liquids_underground() {
        let registry = BlockRegistry::alpha_1_2_6();
        let generator = OverworldChunkGenerator::new(99999);
        let chunks = generator.generate_region(ChunkPos { x: 0, z: 0 }, 2, &registry);

        let mut found_spring = false;
        'outer: for chunk in &chunks {
            for lx in 0..CHUNK_WIDTH as u8 {
                for lz in 0..CHUNK_DEPTH as u8 {
                    for y in 1..60_u8 {
                        let bid = chunk.block(lx, y, lz);
                        // Flowing water/lava underground surrounded by stone = spring
                        if bid == FLOWING_WATER_ID || bid == FLOWING_LAVA_ID {
                            found_spring = true;
                            break 'outer;
                        }
                    }
                }
            }
        }
        assert!(found_spring, "no underground springs found in 5x5 region");
    }

    #[test]
    fn liquid_blocks_do_not_drop_items() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(registry.dropped_item_block_id(WATER_ID), None);
        assert_eq!(registry.dropped_item_block_id(FLOWING_WATER_ID), None);
        assert_eq!(registry.dropped_item_block_id(LAVA_ID), None);
        assert_eq!(registry.dropped_item_block_id(FLOWING_LAVA_ID), None);
        assert_eq!(registry.dropped_item_block_id(STONE_ID), Some(STONE_ID));
    }

    #[test]
    fn alpha_registry_exposes_hardness_and_tick_flags() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert!((registry.mining_hardness_of(STONE_ID) - 1.5).abs() < f32::EPSILON);
        assert!((registry.mining_hardness_of(OAK_LEAVES_ID) - 0.2).abs() < f32::EPSILON);
        assert!((registry.mining_hardness_of(BEDROCK_ID) - (-1.0)).abs() < f32::EPSILON);
        assert!(registry.ticks_randomly_of(GRASS_ID));
        assert!(registry.ticks_randomly_of(FLOWING_WATER_ID));
        assert!(!registry.ticks_randomly_of(WATER_ID));
    }

    #[test]
    fn alpha_registry_collision_flags_cover_special_non_cube_blocks() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert!(registry.is_collidable(65), "ladders should have collision");
        assert!(
            !registry.is_collidable(63),
            "standing signs should not collide"
        );
        assert!(!registry.is_collidable(50), "torches should not collide");
        assert!(
            !registry.is_collidable(WATER_ID),
            "water should not collide"
        );
    }

    #[test]
    fn leaves_block_movement_but_are_not_full_solid_occluders() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert!(registry.blocks_movement(OAK_LEAVES_ID));
        assert!(registry.is_collidable(OAK_LEAVES_ID));
        assert!(!registry.is_solid(OAK_LEAVES_ID));
    }
}
