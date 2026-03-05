# World Generation Parity Report: ThingCraft vs Minecraft Alpha 1.2.6

**Date:** 2026-03-05
**Reference:** `resources/decomp/src/net/minecraft/world/gen/chunk/OverworldChunkGenerator.java`
**Implementation:** `thingcraft-client/src/world.rs`

---

## Actionable Checklist

### High Priority (Affects world appearance / feature correctness)

- [ ] **Implement LargeOakTreeFeature** — Alpha has large branching oak trees (10% base chance, 33% in rainforest). ThingCraft has no equivalent; these are replaced with standard oaks.
- [ ] **Remove or gate Birch/Pine tree types behind a flag** — Birch and Pine trees do not exist in Alpha 1.2.6. All trees are either standard oak (`TreeFeature`) or large oak (`LargeOakTreeFeature`). These are custom additions.
- [ ] **Implement forest noise for tree density** — `_forest_noise` is allocated (8 octaves) but unused. Alpha uses `(forestNoise.getValue(x*0.5, z*0.5) / 8.0 + random*4.0 + 4.0) / 3.0` per chunk, added to biome-specific offsets. ThingCraft uses a simplified static lookup per biome.
- [ ] **Fix tree selection to be per-chunk, not per-tree** — In Alpha, the tree feature type (standard vs large oak) is selected ONCE per chunk and used for ALL trees in that chunk. ThingCraft selects tree type per individual tree based on biome.
- [~] **Complete dungeon internals** — Dungeon rooms now place chest blocks (up to 2) and run Alpha loot/spawner RNG rolls, but chest inventory contents and persisted spawner mob type still require block-entity data wiring.
- [ ] **Add Ice Desert biome** — Alpha has an `ICE_DESERT` biome (temperature < 0.5, low downfall) that uses SAND/SAND surface blocks and is snowy. The `BiomeKind` enum does not include this variant.

### Medium Priority (Affects feature distribution / statistical parity)

- [ ] **Unify RNG to single shared JavaRandom per chunk** — Alpha uses a single `java.util.Random` seeded once per chunk (`chunkX * l + chunkZ * m ^ worldSeed`), shared sequentially across ALL features. ThingCraft creates separate `SmallRng` instances per feature with custom XOR constants (`0x5A5A_5A5A`, `0xBEEF_CAFE`, etc.). This means random number streams are completely decorrelated from Alpha — worlds will not be block-for-block identical even if all algorithms match.
- [ ] **Apply +8 coordinate offset for single-chunk features** — Alpha places flowers, mushrooms, sugar cane, pumpkins, cacti, and springs at `chunkOrigin + random(16) + 8` in world coordinates, allowing features to naturally extend into adjacent chunks via the World object. ThingCraft places these at `gen_range(0..16)` in local chunk coordinates, clipping to [0, 15]. This biases feature density toward chunk centers and prevents cross-chunk feature bleed.
- [ ] **Fix plant scatter distribution** — Alpha's `PlantFeature` uses `rand(8) - rand(8)` for X/Z scatter (triangular distribution, range [-7, 7]) and `rand(4) - rand(4)` for Y (range [-3, 3]). ThingCraft uses `gen_range(-8..=8)` / `gen_range(-4..=4)` (uniform distribution, range [-8, 8] / [-4, 4]). The triangular distribution clusters plants more tightly around the base position.
- [ ] **Fix plant base Y coordinate** — Alpha's `PlantFeature.place()` receives a random Y from `nextInt(128)` (0-127). Most scatter attempts fail because they're underground or in air, limiting effective placement. ThingCraft uses `find_surface_y_local()` as the base Y, which is more efficient but places significantly more visible flowers/mushrooms than Alpha.
- [ ] **Fix lava spring height formula** — Alpha: `nextInt(nextInt(nextInt(112) + 8) + 8)` — three levels of nesting with +8 at each level, base range 112. ThingCraft: `gen_range(0..120) + 8` then two levels of `gen_range(0..inner)` — only +8 at the outermost level, base range 120. The distributions differ.
- [ ] **Fix Swampland tree density** — Alpha's `populateChunk` does NOT add a tree density bonus for Swampland (it only adds for Forest, Rainforest, SeasonalForest, Taiga). ThingCraft gives Swampland a base count of 4, making swamps significantly more forested than in Alpha.

### Low Priority (Minor behavioral differences)

- [ ] **Align Swampland biome behavior** — Alpha has a `SwampBiome` subclass (empty, no overrides). No special surface blocks or decoration logic exists for swampland in Alpha — it uses default GRASS/DIRT and no tree density bonus.
- [ ] **Align mushroom scatter with plant scatter** — Mushrooms in Alpha use `PlantFeature` (same scatter as flowers). ThingCraft has a separate `scatter_mushroom` function that uses `is_solid_for_dungeon` as the ground check instead of checking for specific blocks. Alpha's mushrooms can grow on any surface where `canSurvive()` returns true.

---

## Detailed Comparison

### 1. Noise Generators

| Generator | Alpha Octaves | ThingCraft Octaves | Match |
|-----------|--------------|-------------------|-------|
| `minLimitPerlinNoise` | 16 | 16 | Yes |
| `maxLimitPerlinNoise` | 16 | 16 | Yes |
| `perlinNoise1` | 8 | 8 | Yes |
| `perlinNoise2` | 4 | 4 | Yes |
| `perlinNoise3` | 4 | 4 | Yes |
| `scaleNoise` | 10 | 10 | Yes |
| `depthNoise` | 16 | 16 | Yes |
| `forestNoise` | 8 | 8 (unused) | Partial |

The noise primitives (ImprovedNoise, PerlinNoise, SimplexNoise) and their octave counts are identical. The `forestNoise` generator is constructed for RNG-state parity but its output is not used.

### 2. Terrain Generation (build_terrain / buildTerrain)

The 3D density field algorithm matches Alpha:
- 5x17x5 control point grid with trilinear interpolation
- Same noise scale constants (684.412 for limit noises, 200.0 for depth, etc.)
- Same density falloff formula with temperature/downfall climate factors
- Same top-4-layer blending toward -10.0
- Water level at Y=64, ice when temperature < 0.5

**Status: Matching**

### 3. Surface Building (build_surfaces / buildSurfaces)

Both implementations:
- Use identical sand/gravel noise with the same scale (0.03125) and X/Z swap for gravel
- Apply the same sea-level override logic (Y 60-65)
- Place bedrock at `Y <= random(5)`
- Use depth variation from `perlinNoise3`

**Status: Matching**

### 4. Biome System

| Biome | Alpha | ThingCraft | Notes |
|-------|-------|-----------|-------|
| Rainforest | Yes | Yes | |
| Swampland | Yes (SwampBiome) | Yes | SwampBiome is empty in Alpha |
| SeasonalForest | Yes | Yes | |
| Forest | Yes | Yes | |
| Savanna | Yes (DesertBiome) | Yes | DesertBiome is empty in Alpha |
| Shrubland | Yes | Yes | |
| Taiga | Yes | Yes | |
| Desert | Yes (SAND/SAND) | Yes (SAND/SAND) | |
| Plains | Yes (DesertBiome, GRASS/DIRT) | Yes (GRASS/DIRT) | |
| Ice Desert | Yes (SAND/SAND, snowy) | **Missing** | Not in BiomeKind enum |
| Tundra | Yes | Yes | |

The `from_climate()` / `computeBiome()` thresholds match. BiomeSource noise parameters (seeds, octaves, scales) are identical.

**Status: Missing Ice Desert**

### 5. Cave Generation

Both implementations use the CaveWorldCarver algorithm:
- Same triple-nested random for cave count (`nextInt(nextInt(nextInt(40)+1)+1)`)
- Same 1/15 skip probability
- Same room vs tunnel distinction (25% room chance)
- Same tunnel carving with sine-wave radius modulation
- Same direction smoothing factors (0.92 narrow / 0.7 wide)
- Same lava fill below Y=10
- Same branch probability and perpendicular branching

**Status: Matching**

### 6. Ore Generation

| Ore | Alpha Attempts | Alpha VeinSize | Alpha MaxY | TC Attempts | TC VeinSize | TC MaxY | Match |
|-----|---------------|---------------|-----------|------------|------------|--------|-------|
| Dirt | 20 | 32 | 128 | 20 | 32 | 128 | Yes |
| Gravel | 10 | 32 | 128 | 10 | 32 | 128 | Yes |
| Coal | 20 | 16 | 128 | 20 | 16 | 128 | Yes |
| Iron | 20 | 8 | 64 | 20 | 8 | 64 | Yes |
| Gold | 2 | 8 | 32 | 2 | 8 | 32 | Yes |
| Redstone | 8 | 7 | 16 | 8 | 7 | 16 | Yes |
| Diamond | 1 | 7 | 16 | 1 | 7 | 16 | Yes |

**Status: Matching**

### 7. Tree Generation

#### Tree Types

| Feature | Alpha | ThingCraft |
|---------|-------|-----------|
| Standard Oak (4-6 blocks) | TreeFeature | Oak (matching) |
| Large Oak (5-16 blocks, branches) | LargeOakTreeFeature | **Missing** |
| Birch (5-6 blocks) | Does not exist | Custom addition |
| Pine (6-10 blocks, conical) | Does not exist | Custom addition |

#### Tree Selection Logic

**Alpha** (per-chunk, lines 396-408):
```
feature = TreeFeature  (default)
if random(10) == 0:  feature = LargeOakTreeFeature
if biome == RAINFOREST && random(3) == 0:  feature = LargeOakTreeFeature
// ALL trees in this chunk use the same feature
```

**ThingCraft** (per-tree, per-biome):
```
Taiga → Pine (100%)
Forest/SeasonalForest → Birch (33%) / Oak (67%)
All others → Oak (100%)
```

#### Tree Density

**Alpha** (noise-based):
```
base = (forestNoise(x*0.5, z*0.5) / 8.0 + random*4.0 + 4.0) / 3.0
count = 0
if random(10)==0: count++
Forest:         count += base + 5
Rainforest:     count += base + 5
SeasonalForest: count += base + 2
Taiga:          count += base + 5
Desert:         count -= 20
Tundra:         count -= 20
Plains:         count -= 20
```

**ThingCraft** (static lookup):
```
Forest/Rainforest/Taiga: 10
SeasonalForest/Swampland: 4    ← Swampland has NO bonus in Alpha
Shrubland/Savanna: 1
Plains: 0
Desert/Tundra: -1
final = base + random(0..10)
```

**Status: Multiple differences (missing large oak, custom birch/pine, simplified density, wrong swampland count)**

### 8. Lake Generation

Both implementations use the multi-ellipsoid carving algorithm:
- Same 1/4 water and 1/8 lava frequencies
- Same ellipsoid shape computation
- Same validation logic
- Same grass-to-dirt conversion for water lakes
- Same Y-bias for lava lakes with 1/10 gate at Y >= 64

**Status: Matching**

### 9. Dungeon Generation

| Component | Alpha | ThingCraft | Match |
|-----------|-------|-----------|-------|
| Room dimensions (2-4 × 3 × 2-4) | Yes | Yes | Yes |
| Wall validation (1-5 openings) | Yes | Yes | Yes |
| Cobblestone/Mossy walls | Yes | Yes | Yes |
| Mob spawner block | Yes | Yes | Yes |
| Spawner mob type | Skeleton/Zombie/Spider | RNG selected, **not persisted** | Partial |
| Chests (up to 2) | Yes | Chest blocks placed, **loot stubbed** | Partial |
| Loot table (11 items) | Yes | **Missing** | No |
| Attempts per chunk | 8 | 8 | Yes |

**Status: Room + chest placement complete; loot/spawner block-entity payloads still incomplete**

### 10. Decoration Features

| Feature | Alpha | ThingCraft | Differences |
|---------|-------|-----------|-------------|
| Yellow flowers | 2 attempts, PlantFeature | 2 attempts, scatter_surface_plant | Base Y, scatter distribution, coord offset |
| Red flowers | 50% chance, 1 attempt | 50% chance, 1 attempt | Same differences as above |
| Brown mushrooms | 25% chance, PlantFeature | 25% chance, scatter_mushroom | Ground check differs |
| Red mushrooms | 12.5% chance, PlantFeature | 12.5% chance, scatter_mushroom | Ground check differs |
| Sugar cane | 10 attempts, SugarcaneFeature | 10×10 nested loops | Structural equivalent |
| Pumpkins | 1/32 chance, PumpkinPatchFeature | 1/32 chance, scatter_surface_plant | Base Y, scatter, coord offset |
| Cacti | 10 per desert chunk, CactusFeature | 10 per desert chunk, nested loops | Structural equivalent |
| Water springs | 50 attempts, LiquidFallFeature | 50 attempts, try_place_spring | Matching |
| Lava springs | 20 attempts, LiquidFallFeature | 20 attempts, try_place_spring | Height formula differs |
| Snow cover | Temperature-based, +8 offset | Temperature-based | Matching |
| Clay patches | 10 attempts, ClayPatchFeature(32) | 10 attempts, place_clay_patches | Matching |

### 11. RNG Architecture

**Alpha:** Single `java.util.Random` instance per chunk, seeded with `(chunkX * l + chunkZ * m) ^ worldSeed`. All features consume from this shared stream sequentially. Feature ordering is critical — skipping a lake roll affects all subsequent feature positions.

**ThingCraft:** Separate `SmallRng` per feature, each seeded with `alpha_chunk_seed(world_seed, cx, cz) ^ FEATURE_CONSTANT`. Features are fully independent. The XOR constants are:

| Feature | XOR Constant |
|---------|-------------|
| Dungeons | `0x5A5A_5A5A` |
| Clay | `0x1111_C1A4` |
| Flowers | `0x2222_F10A` |
| Mushrooms | `0x3333_BEEF` |
| Sugar cane | `0x4444_CA4E` |
| Pumpkins | `0x5555_B0B0` |
| Cacti | `0x6666_CAC7` |
| Springs | `0x7777_5B46` |
| Lakes | `0xAAAA_1A4E` |
| Trees | `0xBEEF_CAFE` |

This design is more modular but produces entirely different random sequences from Alpha.

### 12. Feature Coordinate System

**Alpha:** Features positioned at `chunkOrigin + random(16) + 8` in world coordinates. The +8 offset means features span blocks 8-23 relative to chunk origin, crossing into the adjacent chunk. Alpha writes directly to the `World` object so cross-chunk placement succeeds naturally.

**ThingCraft:** Features positioned at `gen_range(0..16)` in local chunk coordinates (0-15). Scatter positions outside [0, 15] are clipped. This prevents cross-chunk feature placement and biases feature density toward chunk interiors.

### 13. Population Order

**Alpha `populateChunk()`:**
1. Water lakes (1/4)
2. Lava lakes (1/8)
3. Dungeons (×8)
4. Clay patches (×10)
5. Ores (dirt, gravel, coal, iron, gold, redstone, diamond)
6. Trees (biome-dependent count)
7. Yellow flowers (×2)
8. Red flowers (50%)
9. Brown mushrooms (25%)
10. Red mushrooms (12.5%)
11. Sugar cane (×10)
12. Pumpkins (1/32)
13. Cacti (desert ×10)
14. Water springs (×50)
15. Lava springs (×20)
16. Snow cover

**ThingCraft `populate_chunk_features()` + `generate_region()`:**
1. Caves (carve_caves)
2. Clay patches
3. Ores
4. Dungeon generation
5. Height map recalculation
6. Flowers
7. Mushrooms
8. Sugar cane
9. Pumpkins
10. Cacti
11. Springs
*Then in generate_region:*
12. Lakes (cross-chunk)
13. Trees (cross-chunk)
14. Snow cover

Note: In Alpha, caves are carved during `getChunk()` (terrain phase), not `populateChunk()` (decoration phase). Both approaches carve caves before decoration, but the ordering relative to other features differs. Lakes and trees are handled as cross-chunk operations in ThingCraft but single-chunk in Alpha.

---

## Summary

### What matches well:
- Noise primitives and octave configuration
- 3D terrain density algorithm
- Surface building (sand/gravel/bedrock/biome surfaces)
- Biome selection thresholds and climate noise
- Cave carving algorithm
- Ore types, vein sizes, frequencies, and height limits
- Lake shape algorithm
- Dungeon room geometry
- Snow cover logic

### What needs work:
- Tree system (missing large oak, custom birch/pine, wrong density formula)
- Dungeon internals (chests, loot, spawner types)
- RNG architecture (per-feature vs shared stream)
- Feature coordinate offset (+8 cross-chunk placement)
- Plant scatter distribution (uniform vs triangular)
- Missing Ice Desert biome
- Lava spring height formula
