# The Alpha 1.2.6 Feature Checklist (NOT ORDERED)

## Bootstrap Progress (Non-Parity)
- [x] Cross-platform desktop client skeleton (`winit` + `wgpu`) created.
- [x] Fixed 20 TPS simulation loop separated from render frame loop.
- [x] Standalone `bevy_ecs` schedules for input capture, fixed simulation, and render interpolation.
- [x] Command-queue input flow established (network-ready pattern for future multiplayer).
- [x] CI + lint/test baseline added.
- [x] Alpha-derived block registry skeleton added (with explicit Alpha-excluded block IDs).
- [x] Chunk core storage model added (`16x16x128`, nibble light channels, Alpha index layout).
- [x] Deterministic biome climate sampler and overworld startup chunk generator scaffold added.
- [x] CPU chunk meshing scaffold added (face culling + atlas UV generation).
- [x] Reference tracking doc added (`docs/REFERENCES.md`) for copied constants/behaviors.

> Note: Items below track Alpha 1.2.6 gameplay/content parity only.

## World and Generation
- [ ] Dimensions: The Overworld and the Nether.
- [ ] World Limits: X/Z are practically infinite, but the Y height limit is strictly 128 blocks. Water level is at Y=64.
- [ ] Biomes: The "Halloween Update" biome system (2D noise map of Temperature and Humidity).
- [ ] Biome Types: Rainforest, Swampland, Seasonal Forest, Forest, Savanna, Shrubland, Taiga, Desert, Plains, Tundra, Hell.
- [ ] Dynamic Colors: Grass and leaf colors shift dynamically based on biome temperature and humidity values.
- [ ] Terrain: 3D Perlin noise generation.
- [ ] Caves: Standard cave tunnels only (no ravines).
- [ ] Underground Generation: Dirt, gravel, coal, iron, gold, diamond, and redstone veins.
- [ ] Structures: Dungeons with mob spawners.

## Graphics and Lighting
- [ ] Lighting Engine: Strict, blocky, per-block lighting (no Smooth Lighting/Ambient Occlusion).
- [ ] Light Levels: 16 levels of light (0 to 15).
- [ ] Light Spread: Sunlight drops by 1 per block horizontally and vertically. Block light (torches, glowstone) spreads identically.

## Blocks and Items
- [ ] Core Blocks: Stone, Cobblestone, Dirt, Grass, Wood (only Oak native, Birch/Pine data values only), Planks, Leaves, Glass, Sand, Gravel, Bedrock.
- [ ] Mechanics Blocks: TNT, Doors, Ladders, Torches, Signs, Furnaces, Crafting Tables, Chests.
- [ ] Liquids: Flowing and stationary Water and Lava.
- [ ] Exclusions (Ensure these are absent): No Beds, no Tall Grass, no Lapis Lazuli, no Sandstone, no Pistons.
- [ ] Food Mechanics: No Hunger bar (food heals health instantly).

## Entities and Mobs
- [ ] Passive Mobs: Pigs, Sheep, Cows, Chickens.
- [ ] Hostile Mobs: Zombies, Skeletons, Spiders, Creepers, Slimes.
- [ ] Nether Mobs: Ghasts, Zombie Pigmen.
- [ ] Mechanical Entities: Minecarts, Boats, Dropped items.
