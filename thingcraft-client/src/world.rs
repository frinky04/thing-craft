use std::fmt;
use std::path::Path;

use noise::{Fbm, MultiFractal, NoiseFn, OpenSimplex};

pub const CHUNK_WIDTH: usize = 16;
pub const CHUNK_DEPTH: usize = 16;
pub const CHUNK_HEIGHT: usize = 128;
pub const CHUNK_AREA: usize = CHUNK_WIDTH * CHUNK_DEPTH;
pub const CHUNK_VOLUME: usize = CHUNK_AREA * CHUNK_HEIGHT;

const AIR_ID: u8 = 0;
const STONE_ID: u8 = 1;
const GRASS_ID: u8 = 2;
const DIRT_ID: u8 = 3;
const SAND_ID: u8 = 12;
const BEDROCK_ID: u8 = 7;
const WATER_ID: u8 = 9;
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
            4,
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
            8,
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
            10,
            "flowing_lava",
            0,
            MaterialKind::Liquid,
            false,
            255,
            15,
        );
        add(
            &mut by_id,
            11,
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
            13,
            "gravel",
            19,
            MaterialKind::Sand,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            14,
            "gold_ore",
            32,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            15,
            "iron_ore",
            33,
            MaterialKind::Stone,
            true,
            255,
            0,
        );
        add(
            &mut by_id,
            16,
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
            52,
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
            56,
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
            73,
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

    #[cfg(test)]
    #[must_use]
    pub fn block_light(&self, local_x: u8, y: u8, local_z: u8) -> u8 {
        let idx = Self::index(local_x, y, local_z);
        self.block_light.get(idx)
    }

    fn set_block_light(&mut self, local_x: u8, y: u8, local_z: u8, value: u8) {
        let idx = Self::index(local_x, y, local_z);
        self.block_light.set(idx, value);
    }

    #[cfg(test)]
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

    #[cfg(test)]
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
}
