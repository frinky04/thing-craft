# The Alpha 1.2.6 Gameplay Feature Checklist (NOT ORDERED)

This document tracks Alpha 1.2.6 gameplay/content parity items.

## World and Generation
- [~] Dimensions: Overworld generation/streaming is implemented; Nether dimension generation/streaming is still pending.
- [~] World Limits: Y=128 chunk height and sea level behavior at Y=64 are implemented in generation/runtime; full parity validation for practical X/Z behavior across extended travel is still pending.
- [~] Biomes: 2D temperature/downfall biome sampling and biome classification are implemented; final parity tuning/coverage (including dimension-specific behavior) is still pending.
- [~] Biome Types: Overworld biome set is implemented (Rainforest, Swampland, Seasonal Forest, Forest, Savanna, Shrubland, Taiga, Desert, Plains, Tundra, plus Ice Desert variant); Hell/Nether biome parity is pending.
- [x] Dynamic Colors: Grass and leaf colors shift dynamically based on biome temperature and humidity values.
- [~] Terrain: Deterministic terrain generation is implemented (surface shape + biome-dependent top blocks + cave/ore/dungeon/tree population), but strict Alpha-style 3D Perlin density/parity is not fully complete.
- [x] Caves: Standard cave tunnels only (no ravines).
- [x] Underground Generation: Dirt, gravel, coal, iron, gold, diamond, and redstone veins.
- [~] Structures: Dungeon rooms with mob spawner blocks placed (chest inventory and spawner entity config require M7 entity/inventory systems).

## Graphics and Lighting
- [x] Lighting Engine: Async queue propagation and rendered-light sampling are integrated with boundary-aware neighbor relight/remesh invalidation and stale-result dropping.
- [x] Light Levels: 16 levels (0..15) are represented in data + render mapping.
- [x] Light Spread: Sunlight and block light both spread with 1-level attenuation per step, including cross-chunk boundary seeding/propagation.
- [x] Day/Night Cycle: Fixed-tick world time uses Alpha time-of-day and ambient-darkness curves; rendered skylight and fog transition through the cycle while preserving emitted block light.
- [x] Distance Fog: Linear distance fog blending is active, and clear-fog color now uses Alpha-style player-brightness modulation (`fogBrightness` smoothing/interpolation with view-distance bias).
- [~] Sky/Cloud Atmosphere: Alpha fog clear-color mixing, far/normal sky-color pass, and non-fancy moving cloud layer are implemented; sun/moon/stars/sunrise disc rendering is still pending.
- [x] Alpha Test: Terrain shader now discards low-alpha texels (Alpha-style cutout behavior).
- [x] Leaf Graphics Modes: Runtime now supports fancy cutout leaves and fast opaque leaves (via `THINGCRAFT_FANCY_GRAPHICS`), matching Alpha's visual mode split.

## Blocks and Items
- [x] Placement Controls: Hotbar slot selection (`1..9`) drives right-click block placement (torch included), with finite slot stacks (64 max) that consume on successful placement and refill on matching-block pickup from break actions.
- [~] Inventory Model: Runtime supports hotbar slot stack state only; broader Alpha survival inventory/crafting container behavior remains pending.
- [~] Core Blocks: Stone, Cobblestone, Dirt, Grass, Wood, Planks, Leaves, Glass, Sand, Gravel, Bedrock. Oak, birch, and pine tree shapes now generate with log+leaf blocks (Alpha 1.2.6 uses a single log/leaf block type for all variants), and tree population now writes seamlessly across chunk borders (no half-cut border trees).
- [ ] Mechanics Blocks: TNT, Doors, Ladders, Torches, Signs, Furnaces, Crafting Tables, Chests.
- [x] Liquids: Water/lava render with Alpha atlas textures, fixed-tick simulation covers spread (downward + lateral), source/flowing conversion with metadata depth states, lava-water interaction (obsidian/cobblestone outcomes), urgent wake-up on player edits, and adaptive budget control. Water visuals now use Alpha-style per-frame `WaterSprite`/`WaterSideSprite` animation plus flow-angle UV mapping on liquid top faces.
- [ ] Exclusions (Ensure these are absent): No Beds, no Tall Grass, no Lapis Lazuli, no Sandstone, no Pistons.
- [ ] Food Mechanics: No Hunger bar (food heals health instantly).

## Entities and Mobs
- [x] Entity Framework: bevy_ecs entity lifecycle with components for physics, age, pickup delay, and render interpolation. Simplified AABB collision (Alpha ItemEntity constants), solid-block ejection, and age-based despawn.
- [x] Dropped Items: Breaking a block spawns a dropped item entity with Alpha-faithful physics (gravity 0.04, bounce, drag, 10-tick pickup delay, 5-minute despawn). Items render as billboarded terrain atlas sprites with bobbing animation.
- [ ] Passive Mobs: Pigs, Sheep, Cows, Chickens.
- [ ] Hostile Mobs: Zombies, Skeletons, Spiders, Creepers, Slimes.
- [ ] Nether Mobs: Ghasts, Zombie Pigmen.
- [~] Mechanical Entities: Minecarts and Boats pending; dropped items implemented.
