# The Alpha 1.2.6 Gameplay Feature Checklist (NOT ORDERED)

This document tracks Alpha 1.2.6 gameplay/content parity items.

## World and Generation
- [ ] Dimensions: The Overworld and the Nether.
- [ ] World Limits: X/Z are practically infinite, but the Y height limit is strictly 128 blocks. Water level is at Y=64.
- [ ] Biomes: The "Halloween Update" biome system (2D noise map of Temperature and Humidity).
- [ ] Biome Types: Rainforest, Swampland, Seasonal Forest, Forest, Savanna, Shrubland, Taiga, Desert, Plains, Tundra, Hell.
- [ ] Dynamic Colors: Grass and leaf colors shift dynamically based on biome temperature and humidity values.
- [ ] Terrain: 3D Perlin noise generation.
- [x] Caves: Standard cave tunnels only (no ravines).
- [x] Underground Generation: Dirt, gravel, coal, iron, gold, diamond, and redstone veins.
- [~] Structures: Dungeon rooms with mob spawner blocks placed (chest inventory and spawner entity config require M7 entity/inventory systems).

## Graphics and Lighting
- [x] Lighting Engine: Async queue propagation and rendered-light sampling are integrated with boundary-aware neighbor relight/remesh invalidation and stale-result dropping.
- [x] Light Levels: 16 levels (0..15) are represented in data + render mapping.
- [x] Light Spread: Sunlight and block light both spread with 1-level attenuation per step, including cross-chunk boundary seeding/propagation.

## Blocks and Items
- [~] Placement Controls: Hotbar slot selection (`1..9`) now drives right-click block placement (torch included); inventory stack/state rules remain pending.
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
