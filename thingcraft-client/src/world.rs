use std::f64::consts::PI;
use std::fmt;
use std::path::Path;

use noise::{Fbm, MultiFractal, NoiseFn, OpenSimplex};
use rand::rngs::SmallRng;
use rand::{Rng, SeedableRng};

pub const CHUNK_WIDTH: usize = 16;
pub const CHUNK_DEPTH: usize = 16;
pub const CHUNK_HEIGHT: usize = 128;
pub const CHUNK_AREA: usize = CHUNK_WIDTH * CHUNK_DEPTH;
pub const CHUNK_VOLUME: usize = CHUNK_AREA * CHUNK_HEIGHT;

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
const MOSSY_COBBLESTONE_ID: u8 = 48;
const MOB_SPAWNER_ID: u8 = 52;
const DIAMOND_ORE_ID: u8 = 56;
const REDSTONE_ORE_ID: u8 = 73;
const ICE_ID: u8 = 79;

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
    Plant,
    Glass,
    Liquid,
    Metal,
    Sand,
    Fire,
    Portal,
    Wool,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub struct BlockDef {
    pub id: u8,
    pub name: &'static str,
    pub sprite_index: u16,
    pub material: MaterialKind,
    pub solid: bool,
    pub opacity: u8,
    pub emitted_light: u8,
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
            0,
            MaterialKind::Liquid,
            false,
            3,
            0,
        );
        add(
            &mut by_id,
            WATER_ID,
            "water",
            0,
            MaterialKind::Liquid,
            false,
            3,
            0,
        );
        add(
            &mut by_id,
            FLOWING_LAVA_ID,
            "flowing_lava",
            0,
            MaterialKind::Liquid,
            false,
            255,
            15,
        );
        add(
            &mut by_id,
            LAVA_ID,
            "lava",
            0,
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
            MaterialKind::Plant,
            false,
            1,
            0,
        );
        add(&mut by_id, 20, "glass", 49, MaterialKind::Glass, true, 0, 0);
        add(&mut by_id, 35, "wool", 64, MaterialKind::Wool, true, 255, 0);
        add(&mut by_id, 46, "tnt", 8, MaterialKind::Wood, true, 255, 0);
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
            MaterialKind::Plant,
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
            58,
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
            MaterialKind::Wood,
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
            MaterialKind::Plant,
            false,
            0,
            7,
        );
        add(
            &mut by_id,
            ICE_ID,
            "ice",
            67,
            MaterialKind::Glass,
            true,
            3,
            0,
        );
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
    pub fn sprite_index_of(&self, block_id: u8) -> u16 {
        self.get(block_id).map_or(0, |block| block.sprite_index)
    }

    #[must_use]
    pub fn sprite_index_for_face(&self, block_id: u8, face_offset: [i32; 3]) -> u16 {
        match (block_id, face_offset[1]) {
            (GRASS_ID, 1) => 0,
            (GRASS_ID, -1) => self.sprite_index_of(DIRT_ID),
            (17, 1 | -1) => 21,
            _ => self.sprite_index_of(block_id),
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
    pub fn face_uses_biome_tint(&self, block_id: u8, face_offset: [i32; 3]) -> bool {
        matches!((block_id, face_offset[1]), (GRASS_ID, 1))
    }

    #[must_use]
    pub fn is_defined_block(&self, block_id: u8) -> bool {
        self.get(block_id).is_some()
    }

    #[must_use]
    pub fn is_face_occluder(&self, block_id: u8) -> bool {
        self.get(block_id)
            .is_some_and(|block| block.solid && block.opacity == 255)
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

    *slot = Some(BlockDef {
        id,
        name,
        sprite_index,
        material,
        solid,
        opacity,
        emitted_light,
    });
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
    IceDesert,
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
            if temperature < 0.98 {
                return Self::Desert;
            }
            return Self::IceDesert;
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
            Self::Desert | Self::IceDesert => (SAND_ID, SAND_ID),
            _ => (GRASS_ID, DIRT_ID),
        }
    }
}

#[derive(Debug, Clone, Copy)]
pub struct BiomeSample {
    pub temperature: f64,
    pub downfall: f64,
    pub biome: BiomeKind,
}

#[derive(Debug, Clone)]
pub struct BiomeSource {
    temperature_noise: Fbm<OpenSimplex>,
    downfall_noise: Fbm<OpenSimplex>,
    biome_noise: Fbm<OpenSimplex>,
}

impl BiomeSource {
    #[must_use]
    pub fn new(seed: u64) -> Self {
        let mut temperature_noise =
            Fbm::<OpenSimplex>::new((seed.wrapping_mul(9_871) & 0xFFFF_FFFF) as u32);
        temperature_noise = temperature_noise.set_octaves(4).set_frequency(0.025);

        let mut downfall_noise =
            Fbm::<OpenSimplex>::new((seed.wrapping_mul(39_811) & 0xFFFF_FFFF) as u32);
        downfall_noise = downfall_noise.set_octaves(4).set_frequency(0.05);

        let mut biome_noise =
            Fbm::<OpenSimplex>::new((seed.wrapping_mul(543_321) & 0xFFFF_FFFF) as u32);
        biome_noise = biome_noise.set_octaves(2).set_frequency(0.25);

        Self {
            temperature_noise,
            downfall_noise,
            biome_noise,
        }
    }

    #[must_use]
    pub fn sample(&self, x: i32, z: i32) -> BiomeSample {
        let x_f64 = f64::from(x);
        let z_f64 = f64::from(z);

        let raw_temperature = normalize_noise(self.temperature_noise.get([x_f64, z_f64]));
        let raw_downfall = normalize_noise(self.downfall_noise.get([x_f64, z_f64]));
        let blend_noise = self.biome_noise.get([x_f64, z_f64]) * 1.1 + 0.5;

        let temp_mix_epsilon = 0.01;
        let down_mix_epsilon = 0.002;

        let mut temperature = (raw_temperature * 0.15 + 0.7) * (1.0 - temp_mix_epsilon)
            + blend_noise * temp_mix_epsilon;
        let mut downfall =
            (raw_downfall * 0.15 + 0.5) * (1.0 - down_mix_epsilon) + blend_noise * down_mix_epsilon;

        temperature = 1.0 - (1.0 - temperature) * (1.0 - temperature);
        temperature = temperature.clamp(0.0, 1.0);
        downfall = downfall.clamp(0.0, 1.0);

        let biome = BiomeKind::from_climate(temperature, downfall);
        BiomeSample {
            temperature,
            downfall,
            biome,
        }
    }
}

#[derive(Debug, Clone)]
pub struct OverworldChunkGenerator {
    /// Retained for population passes (caves, ores, dungeons) since noise objects are opaque.
    seed: u64,
    biome_source: BiomeSource,
    terrain_noise: Fbm<OpenSimplex>,
    grass_color_map: GrassColorMap,
}

impl OverworldChunkGenerator {
    #[must_use]
    pub fn new(seed: u64) -> Self {
        let biome_source = BiomeSource::new(seed);
        let mut terrain_noise =
            Fbm::<OpenSimplex>::new((seed.wrapping_mul(341_873_128_712) & 0xFFFF_FFFF) as u32);
        terrain_noise = terrain_noise.set_octaves(8).set_frequency(1.0 / 684.412);
        let grass_color_map = GrassColorMap::load();

        Self {
            seed,
            biome_source,
            terrain_noise,
            grass_color_map,
        }
    }

    #[must_use]
    pub fn generate_chunk(&self, chunk_pos: ChunkPos, registry: &BlockRegistry) -> ChunkData {
        let mut chunk = ChunkData::new(chunk_pos, AIR_ID);

        for local_x in 0..CHUNK_WIDTH as u8 {
            for local_z in 0..CHUNK_DEPTH as u8 {
                let world_x = chunk_pos.x * CHUNK_WIDTH as i32 + i32::from(local_x);
                let world_z = chunk_pos.z * CHUNK_DEPTH as i32 + i32::from(local_z);
                let biome_sample = self.biome_source.sample(world_x, world_z);
                let (surface_block, subsurface_block) =
                    biome_sample.biome.surface_subsurface_blocks();
                let grass_tint = self
                    .grass_color_map
                    .sample(biome_sample.temperature, biome_sample.downfall);
                chunk.set_grass_tint(local_x, local_z, grass_tint);

                let terrain_base = self
                    .terrain_noise
                    .get([f64::from(world_x), f64::from(world_z)]);
                let mut surface_y =
                    (64.0 + terrain_base * 18.0 + (biome_sample.temperature - 0.5) * 8.0
                        - (biome_sample.downfall - 0.5) * 4.0)
                        .round() as i32;
                surface_y = surface_y.clamp(1, CHUNK_HEIGHT as i32 - 1);

                for y in 0..=surface_y {
                    let y_u8 = y as u8;
                    let block_id = if y == 0 {
                        BEDROCK_ID
                    } else if y == surface_y {
                        surface_block
                    } else if y >= surface_y - 3 {
                        subsurface_block
                    } else {
                        STONE_ID
                    };
                    chunk.set_block(local_x, y_u8, local_z, block_id);
                }

                let water_level = 64;
                if surface_y < water_level {
                    for y in (surface_y + 1)..=water_level {
                        if y >= CHUNK_HEIGHT as i32 {
                            break;
                        }
                        let y_u8 = y as u8;
                        let fill = if y == water_level && biome_sample.temperature < 0.5 {
                            ICE_ID
                        } else {
                            WATER_ID
                        };
                        chunk.set_block(local_x, y_u8, local_z, fill);
                    }
                }
            }
        }

        carve_caves(&mut chunk, self.seed);
        populate_ores(&mut chunk, self.seed);
        place_dungeon_stubs(&mut chunk, self.seed);
        // Height map needed by place_trees for O(1) surface lookups.
        chunk.recalculate_height_map(registry);
        place_trees(&mut chunk, self.seed, &self.biome_source);
        // Re-run after trees to account for new log/leaf blocks.
        chunk.recalculate_height_map(registry);
        chunk.seed_emitted_light(registry);
        chunk
    }

    #[must_use]
    pub fn generate_region(
        &self,
        center_chunk: ChunkPos,
        radius: i32,
        registry: &BlockRegistry,
    ) -> Vec<ChunkData> {
        let mut chunks = Vec::new();
        for chunk_z in (center_chunk.z - radius)..=(center_chunk.z + radius) {
            for chunk_x in (center_chunk.x - radius)..=(center_chunk.x + radius) {
                chunks.push(self.generate_chunk(
                    ChunkPos {
                        x: chunk_x,
                        z: chunk_z,
                    },
                    registry,
                ));
            }
        }
        chunks
    }
}

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

fn populate_ores(chunk: &mut ChunkData, world_seed: u64) {
    let cx = chunk.pos.x;
    let cz = chunk.pos.z;

    // SmallRng is fine — ore placement doesn't need Java-exact RNG sequences
    let mut rng = SmallRng::seed_from_u64(alpha_chunk_seed(world_seed, cx, cz) as u64);

    let base_x = cx * 16;
    let base_z = cz * 16;

    for ore in ORE_TABLE {
        for _ in 0..ore.attempts {
            let x = base_x + rng.gen_range(0..16);
            let y = rng.gen_range(0..ore.max_y);
            let z = base_z + rng.gen_range(0..16);
            place_vein(chunk, &mut rng, ore.block_id, ore.vein_size, x, y, z);
        }
    }
}

fn place_vein(
    chunk: &mut ChunkData,
    rng: &mut SmallRng,
    block_id: u8,
    size: i32,
    x: i32,
    y: i32,
    z: i32,
) {
    let angle: f32 = rng.gen::<f32>() * PI as f32;
    let size_f = size as f32;

    let vein_x_start = (x as f32 + 8.0) + angle.sin() * size_f / 8.0;
    let vein_x_end = (x as f32 + 8.0) - angle.sin() * size_f / 8.0;
    let vein_z_start = (z as f32 + 8.0) + angle.cos() * size_f / 8.0;
    let vein_z_end = (z as f32 + 8.0) - angle.cos() * size_f / 8.0;
    let vein_y_start = (y + rng.gen_range(0..3) + 2) as f64;
    let vein_y_end = (y + rng.gen_range(0..3) + 2) as f64;

    let chunk_base_x = chunk.pos.x * 16;
    let chunk_base_z = chunk.pos.z * 16;

    for step in 0..=size {
        let t = step as f64 / size as f64;
        let interp_x = vein_x_start as f64 + (vein_x_end as f64 - vein_x_start as f64) * t;
        let interp_y = vein_y_start + (vein_y_end - vein_y_start) * t;
        let interp_z = vein_z_start as f64 + (vein_z_end as f64 - vein_z_start as f64) * t;

        let radius_noise = rng.gen::<f64>() * size as f64 / 16.0;
        let sin_val = ((step as f32 * PI as f32 / size_f).sin() + 1.0) as f64;
        // Alpha uses identical horizontal and vertical radii (spherical cross-section)
        let half_extent = sin_val * radius_noise + 1.0;

        // Java (int) cast truncates toward zero, not floor — matters for negative coords
        let x_min = (interp_x - half_extent / 2.0) as i32;
        let x_max = (interp_x + half_extent / 2.0) as i32;
        let y_min = (interp_y - half_extent / 2.0) as i32;
        let y_max = (interp_y + half_extent / 2.0) as i32;
        let z_min = (interp_z - half_extent / 2.0) as i32;
        let z_max = (interp_z + half_extent / 2.0) as i32;

        for bx in x_min..=x_max {
            let local_x = bx - chunk_base_x;
            if !(0..16).contains(&local_x) {
                continue;
            }
            let fx = (bx as f64 + 0.5 - interp_x) / (half_extent / 2.0);
            for by in y_min..=y_max {
                if by < 1 || by >= CHUNK_HEIGHT as i32 {
                    continue;
                }
                let fy = (by as f64 + 0.5 - interp_y) / (half_extent / 2.0);
                for bz in z_min..=z_max {
                    let local_z = bz - chunk_base_z;
                    if !(0..16).contains(&local_z) {
                        continue;
                    }
                    let fz = (bz as f64 + 0.5 - interp_z) / (half_extent / 2.0);
                    if fx * fx + fy * fy + fz * fz < 1.0
                        && chunk.block(local_x as u8, by as u8, local_z as u8) == STONE_ID
                    {
                        chunk.set_block(local_x as u8, by as u8, local_z as u8, block_id);
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dungeon stubs  (translated from DungeonFeature.java / OverworldChunkGenerator.java)
// ---------------------------------------------------------------------------

/// Alpha's `material.isSolid()` — air and liquids are non-solid.
fn is_solid_for_dungeon(block_id: u8) -> bool {
    !matches!(
        block_id,
        AIR_ID | FLOWING_WATER_ID | WATER_ID | FLOWING_LAVA_ID | LAVA_ID
    )
}

fn place_dungeon_stubs(chunk: &mut ChunkData, world_seed: u64) {
    let cx = chunk.pos.x;
    let cz = chunk.pos.z;

    // SmallRng is fine — dungeon placement doesn't need Java-exact RNG sequences.
    // XOR with a constant to decorrelate from ore placement (same base seed).
    let mut rng =
        SmallRng::seed_from_u64((alpha_chunk_seed(world_seed, cx, cz) ^ 0x5A5A_5A5A) as u64);

    // 8 attempts per chunk (matching Alpha)
    for _ in 0..8 {
        let local_x = rng.gen_range(0..16_i32);
        let y = rng.gen_range(1..CHUNK_HEIGHT as i32 - 4);
        let local_z = rng.gen_range(0..16_i32);

        let half_x: i32 = rng.gen_range(2..=4);
        let half_z: i32 = rng.gen_range(2..=4);
        let room_height = 3;

        // Bounds check — room must fit within chunk
        if local_x - half_x - 1 < 0
            || local_x + half_x + 1 >= 16
            || local_z - half_z - 1 < 0
            || local_z + half_z + 1 >= 16
            || y + room_height + 1 >= CHUNK_HEIGHT as i32
        {
            continue;
        }

        // Validate: floor and ceiling are solid (Alpha rejects air and liquids)
        let mut solid_envelope = true;
        'envelope: for vx in (local_x - half_x - 1)..=(local_x + half_x + 1) {
            for vz in (local_z - half_z - 1)..=(local_z + half_z + 1) {
                let floor_b = chunk.block(vx as u8, (y - 1) as u8, vz as u8);
                let ceil_b = chunk.block(vx as u8, (y + room_height + 1) as u8, vz as u8);
                if !is_solid_for_dungeon(floor_b) || !is_solid_for_dungeon(ceil_b) {
                    solid_envelope = false;
                    break 'envelope;
                }
            }
        }
        if !solid_envelope {
            continue;
        }

        // Count wall openings (adjacent air on walls at floor level)
        let mut openings = 0;
        for vx in (local_x - half_x - 1)..=(local_x + half_x + 1) {
            for vz in (local_z - half_z - 1)..=(local_z + half_z + 1) {
                let on_wall = vx == local_x - half_x - 1
                    || vx == local_x + half_x + 1
                    || vz == local_z - half_z - 1
                    || vz == local_z + half_z + 1;
                if on_wall {
                    let b = chunk.block(vx as u8, y as u8, vz as u8);
                    let b_above = chunk.block(vx as u8, (y + 1) as u8, vz as u8);
                    if b == AIR_ID && b_above == AIR_ID {
                        openings += 1;
                    }
                }
            }
        }
        if !(1..=5).contains(&openings) {
            continue;
        }

        // Carve the room and place walls
        for vx in (local_x - half_x - 1)..=(local_x + half_x + 1) {
            for vy in (y - 1)..=(y + room_height + 1) {
                for vz in (local_z - half_z - 1)..=(local_z + half_z + 1) {
                    let on_boundary = vx == local_x - half_x - 1
                        || vx == local_x + half_x + 1
                        || vy == y - 1
                        || vy == y + room_height + 1
                        || vz == local_z - half_z - 1
                        || vz == local_z + half_z + 1;

                    if on_boundary {
                        // Walls/floor/ceiling: cobblestone or mossy (only replace solid blocks)
                        let existing = chunk.block(vx as u8, vy as u8, vz as u8);
                        if is_solid_for_dungeon(existing) {
                            let wall_block = if vy == y - 1 && rng.gen_range(0..4) != 0 {
                                MOSSY_COBBLESTONE_ID
                            } else {
                                COBBLESTONE_ID
                            };
                            chunk.set_block(vx as u8, vy as u8, vz as u8, wall_block);
                        }
                    } else {
                        // Interior: clear to air
                        chunk.set_block(vx as u8, vy as u8, vz as u8, AIR_ID);
                    }
                }
            }
        }

        // Place mob spawner at center
        chunk.set_block(local_x as u8, y as u8, local_z as u8, MOB_SPAWNER_ID);
    }
}

// ---------------------------------------------------------------------------
// Oak tree placement  (translated from TreeFeature.java / BiomeDecorator.java)
// ---------------------------------------------------------------------------

/// Returns the number of trees to attempt for the given biome.
/// Alpha's BiomeDecorator applies: `treesPerChunk` which defaults to 0, then
/// Forest/Rainforest/Taiga add +5, SeasonalForest +2, and Desert/Tundra/Plains
/// effectively get near-zero (the decorator has `treesPerChunk + offset`, where
/// offset is drawn from `rng.gen_range(0..10)`, and the decorator only places
/// a tree if the random roll < treesPerChunk, so low-tree biomes still get
/// occasional strays).
fn trees_per_chunk(biome: BiomeKind) -> i32 {
    match biome {
        BiomeKind::Forest | BiomeKind::Rainforest | BiomeKind::Taiga => 10,
        BiomeKind::SeasonalForest | BiomeKind::Swampland => 4,
        BiomeKind::Shrubland | BiomeKind::Savanna => 1,
        BiomeKind::Plains => 0,
        BiomeKind::Desert | BiomeKind::IceDesert | BiomeKind::Tundra => -1,
    }
}

fn place_trees(chunk: &mut ChunkData, world_seed: u64, biome_source: &BiomeSource) {
    let cx = chunk.pos.x;
    let cz = chunk.pos.z;

    // XOR with a constant to decorrelate from dungeon/ore placement.
    let mut rng =
        SmallRng::seed_from_u64((alpha_chunk_seed(world_seed, cx, cz) ^ 0xBEEF_CAFE) as u64);

    // Sample biome at chunk center to determine tree density.
    let center_x = cx * CHUNK_WIDTH as i32 + 8;
    let center_z = cz * CHUNK_DEPTH as i32 + 8;
    let biome_sample = biome_source.sample(center_x, center_z);
    let base_count = trees_per_chunk(biome_sample.biome);

    // Alpha adds a random 0..10 offset, then attempts that many trees.
    let attempt_count = base_count + rng.gen_range(0..10_i32);
    if attempt_count <= 0 {
        return;
    }

    for _ in 0..attempt_count {
        let local_x = rng.gen_range(0..CHUNK_WIDTH as i32);
        let local_z = rng.gen_range(0..CHUNK_DEPTH as i32);
        let trunk_height = rng.gen_range(0..3_i32) + 4; // 4-6 blocks

        if let Some(surface_y) = find_surface_y(chunk, local_x as u8, local_z as u8) {
            try_place_tree(chunk, &mut rng, local_x, surface_y, local_z, trunk_height);
        }
    }
}

/// Find the y of the highest non-air block at this column using the
/// precomputed height map. Returns the y of the topmost solid block.
fn find_surface_y(chunk: &ChunkData, local_x: u8, local_z: u8) -> Option<i32> {
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
    chunk: &mut ChunkData,
    rng: &mut SmallRng,
    local_x: i32,
    surface_y: i32,
    local_z: i32,
    trunk_height: i32,
) {
    let ground_block = chunk.block(local_x as u8, surface_y as u8, local_z as u8);
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
                let bx = local_x + dx;
                let bz = local_z + dz;
                if !(0..CHUNK_WIDTH as i32).contains(&bx) || !(0..CHUNK_DEPTH as i32).contains(&bz) {
                    // Out-of-chunk blocks: skip (acceptable clipping for single-chunk gen).
                    continue;
                }
                let existing = chunk.block(bx as u8, cy as u8, bz as u8);
                if existing != AIR_ID && existing != OAK_LEAVES_ID {
                    return;
                }
            }
        }
    }

    // Replace ground with dirt (matching Alpha behavior).
    chunk.set_block(local_x as u8, surface_y as u8, local_z as u8, DIRT_ID);

    // Place leaf canopy: layers from (trunk_top - 3) to (trunk_top + 1).
    // Lower layers (trunk_top-3, trunk_top-2): radius 2, corners randomly pruned.
    // Upper layers (trunk_top-1, trunk_top): radius 1, no corner pruning.
    // Top layer (trunk_top+1): no leaves (trunk doesn't extend there either).
    for cy in (trunk_top - 3)..=(trunk_top) {
        let layer_from_top = trunk_top - cy;
        let radius = if layer_from_top >= 2 { 2 } else { 1 };
        for dx in -radius..=radius {
            for dz in -radius..=radius {
                let bx = local_x + dx;
                let bz = local_z + dz;
                if !(0..CHUNK_WIDTH as i32).contains(&bx) || !(0..CHUNK_DEPTH as i32).contains(&bz) {
                    continue;
                }
                if cy < 0 || cy >= CHUNK_HEIGHT as i32 {
                    continue;
                }
                // Skip the trunk column (trunk pass overwrites center anyway).
                if dx == 0 && dz == 0 {
                    continue;
                }
                // Prune corners on wide layers.
                if radius == 2
                    && dx.abs() == 2
                    && dz.abs() == 2
                    && rng.gen_range(0..2_i32) == 0
                {
                    continue;
                }
                let existing = chunk.block(bx as u8, cy as u8, bz as u8);
                if existing == AIR_ID {
                    chunk.set_block(bx as u8, cy as u8, bz as u8, OAK_LEAVES_ID);
                }
            }
        }
    }

    // Place leaves at trunk_top + 1 (small crown cap): just the center column.
    let cap_y = trunk_top + 1;
    if cap_y < CHUNK_HEIGHT as i32
        && (0..CHUNK_WIDTH as i32).contains(&local_x)
        && (0..CHUNK_DEPTH as i32).contains(&local_z)
    {
        let existing = chunk.block(local_x as u8, cap_y as u8, local_z as u8);
        if existing == AIR_ID {
            chunk.set_block(local_x as u8, cap_y as u8, local_z as u8, OAK_LEAVES_ID);
        }
    }

    // Place trunk (overwrites any leaves at trunk column).
    for ty in trunk_base..=trunk_top {
        if ty >= CHUNK_HEIGHT as i32 {
            break;
        }
        chunk.set_block(local_x as u8, ty as u8, local_z as u8, OAK_LOG_ID);
    }
}

fn normalize_noise(value: f64) -> f64 {
    ((value + 1.0) * 0.5).clamp(0.0, 1.0)
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
    sky_light: NibbleStorage,
    block_light: NibbleStorage,
    height_map: [u8; CHUNK_AREA],
    grass_tint: [u32; CHUNK_AREA],
}

impl ChunkData {
    #[must_use]
    pub fn new(pos: ChunkPos, fill_block: u8) -> Self {
        Self {
            pos,
            blocks: Box::new([fill_block; CHUNK_VOLUME]),
            sky_light: NibbleStorage::new(CHUNK_VOLUME),
            block_light: NibbleStorage::new(CHUNK_VOLUME),
            height_map: [0; CHUNK_AREA],
            grass_tint: [WHITE_RGB_PACKED; CHUNK_AREA],
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
        self.height_map[usize::from(local_z) * CHUNK_WIDTH + usize::from(local_x)] = height;

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
        self.height_map[usize::from(local_z) * CHUNK_WIDTH + usize::from(local_x)]
    }

    #[must_use]
    pub fn grass_tint_at(&self, local_x: u8, local_z: u8) -> [u8; 3] {
        let packed = self.grass_tint[usize::from(local_z) * CHUNK_WIDTH + usize::from(local_x)];
        unpack_rgb(packed)
    }

    fn set_grass_tint(&mut self, local_x: u8, local_z: u8, tint: [u8; 3]) {
        let index = usize::from(local_z) * CHUNK_WIDTH + usize::from(local_x);
        self.grass_tint[index] = pack_rgb(tint);
    }

    #[must_use]
    pub fn index(local_x: u8, y: u8, local_z: u8) -> usize {
        assert!(usize::from(local_x) < CHUNK_WIDTH, "local_x out of range");
        assert!(usize::from(local_z) < CHUNK_DEPTH, "local_z out of range");
        assert!(usize::from(y) < CHUNK_HEIGHT, "y out of range");
        (usize::from(local_x) << 11) | (usize::from(local_z) << 7) | usize::from(y)
    }
}

#[derive(Debug, Clone)]
struct GrassColorMap {
    rgb: Box<[u32; 256 * 256]>,
}

impl GrassColorMap {
    fn load() -> Self {
        let fallback = || Self {
            rgb: Box::new([pack_rgb([126, 201, 86]); 256 * 256]),
        };

        let candidates = [
            Path::new("resources/minecraft-a1.2.6-client/misc/grasscolor.png").to_path_buf(),
            Path::new("../resources/minecraft-a1.2.6-client/misc/grasscolor.png").to_path_buf(),
        ];

        for candidate in candidates {
            let bytes = match std::fs::read(candidate) {
                Ok(bytes) => bytes,
                Err(_) => continue,
            };
            let image = match image::load_from_memory_with_format(&bytes, image::ImageFormat::Png) {
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

        fallback()
    }

    fn sample(&self, temperature: f64, downfall: f64) -> [u8; 3] {
        // Alpha uses temperature plus humidity*temperature to index misc/grasscolor.png.
        let temp = temperature.clamp(0.0, 1.0);
        let humid = (downfall * temp).clamp(0.0, 1.0);
        let x = ((1.0 - temp) * 255.0).round() as usize;
        let y = ((1.0 - humid) * 255.0).round() as usize;
        unpack_rgb(self.rgb[(y << 8) | x])
    }
}

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
    fn grass_top_face_marks_biome_tint_usage() {
        let registry = BlockRegistry::alpha_1_2_6();
        assert_eq!(registry.face_tint_rgb(GRASS_ID, [0, 1, 0]), [0, 0, 0]);
        assert!(registry.face_uses_biome_tint(GRASS_ID, [0, 1, 0]));
        assert_eq!(
            registry.face_tint_rgb(STONE_ID, [0, 1, 0]),
            [u8::MAX, u8::MAX, u8::MAX]
        );
        assert!(!registry.face_uses_biome_tint(STONE_ID, [0, 1, 0]));
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
        assert_eq!(BiomeKind::from_climate(0.99, 0.05), BiomeKind::IceDesert);
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
                                    below == DIRT_ID || below == OAK_LOG_ID,
                                    "block below trunk at ({lx},{y},{lz}) is {below}, expected dirt or log"
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
    fn dungeon_stub_places_cobblestone() {
        let registry = BlockRegistry::alpha_1_2_6();
        let gen = OverworldChunkGenerator::new(314159);
        let mut found_cobble = false;
        let mut found_spawner = false;
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
                            if found_cobble && found_spawner {
                                break 'outer;
                            }
                        }
                    }
                }
            }
        }
        assert!(
            found_cobble,
            "no cobblestone found — dungeon stubs not placing walls"
        );
        assert!(
            found_spawner,
            "no mob spawner found — dungeon stubs not placing spawners"
        );
    }
}
