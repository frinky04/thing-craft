# The Alpha 1.2.6 Gameplay Feature Checklist (NOT ORDERED)

This document tracks Alpha 1.2.6 gameplay/content parity items.

## World and Generation
- [~] Dimensions: Overworld generation/streaming is implemented; Nether dimension generation/streaming is still pending.
- [~] World Limits: Y=128 chunk height and sea level behavior at Y=64 are implemented in generation/runtime; full parity validation for practical X/Z behavior across extended travel is still pending.
- [x] Biomes: Alpha-exact `PerlinSimplexNoise` biome sampling with bulk region queries, quadratic temperature transform, and climate-to-biome classification matching Alpha's `computeBiome` logic.
- [~] Biome Types: Overworld biome set is implemented (Rainforest, Swampland, Seasonal Forest, Forest, Savanna, Shrubland, Taiga, Desert, Plains, Tundra); IceDesert removed (Alpha never produces it). Hell/Nether biome parity is pending.
- [x] Dynamic Colors: Grass and leaf colors shift dynamically based on biome temperature and humidity values.
- [~] Terrain: Alpha-exact 3D density terrain generation is implemented (7 `PerlinNoise` generators with correct seeding order, 5x17x5 density grid with trilinear interpolation, climate-modulated scale/depth shaping, top-down `buildSurfaces` pass with bedrock/sand/gravel layers). Surface lake population is re-enabled; broader parity validation for all decoration feature rates/distribution is still pending.
- [x] Caves: Standard cave tunnels only (no ravines).
- [x] Underground Generation: Dirt, gravel, coal, iron, gold, diamond, and redstone veins.
- [~] Structures: Dungeon rooms with mob spawner blocks placed (chest inventory and spawner entity config require M7 entity/inventory systems).

## Graphics and Lighting
- [x] Lighting Engine: Async queue propagation and rendered-light sampling are integrated with boundary-aware neighbor relight/remesh invalidation and stale-result dropping.
- [x] Light Levels: 16 levels (0..15) are represented in data + render mapping.
- [x] Light Spread: Sunlight and block light both spread with 1-level attenuation per step, including cross-chunk boundary seeding/propagation.
- [x] Day/Night Cycle: Fixed-tick world time uses Alpha time-of-day and ambient-darkness curves; rendered skylight and fog transition through the cycle while preserving emitted block light.
- [x] Distance Fog: Linear distance fog blending is active, and clear-fog color now uses Alpha-style player-brightness modulation (`fogBrightness` smoothing/interpolation with view-distance bias).
- [~] Sky/Cloud Atmosphere: Alpha fog clear-color mixing, far/normal sky-color pass, sun/moon + deterministic starfield rendering, and fancy 3D block cloud volume rendering are implemented; sunrise disc rendering is still pending.
- [x] Alpha Test: Terrain shader now discards low-alpha texels (Alpha-style cutout behavior).
- [x] Leaf Graphics Modes: Runtime now supports fancy cutout leaves and fast opaque leaves (via `THINGCRAFT_FANCY_GRAPHICS`), matching Alpha's visual mode split.

## Blocks and Items
- [x] Placement Controls: Hotbar slot selection (`1..9`) drives right-click block placement (torch included), with finite slot stacks (64 max) that consume on successful placement and refill on matching-block pickup from break actions.
- [~] Inventory Model: Runtime now uses an Alpha-style player inventory domain (27 main + 9 hotbar + 4 armor stubs) with cursor stack state, authoritative command-applied click semantics (left/right split/merge/swap, outside drop), and an interactive inventory panel (`E`) rendered from `inventory.png`; crafting grid/result logic remains pending.
- [~] Core Blocks: Stone, Cobblestone, Dirt, Grass, Wood, Planks, Leaves, Glass, Sand, Gravel, Bedrock. Oak, birch, and pine tree shapes now generate with log+leaf blocks (Alpha 1.2.6 uses a single log/leaf block type for all variants), and tree population now writes seamlessly across chunk borders (no half-cut border trees).
- [ ] Mechanics Blocks: TNT, Doors, Ladders, Torches, Signs, Furnaces, Crafting Tables, Chests.
- [x] Liquids: Water/lava render with Alpha atlas textures, fixed-tick simulation covers spread (downward + lateral), source/flowing conversion with metadata depth states, lava-water interaction (obsidian/cobblestone outcomes), urgent wake-up on player edits, and adaptive budget control. Water displacement now emits dropped block items (lava fizz/no-drop behavior preserved), leaves now block liquid spread, and water/lava no longer drop as break items. Water visuals now use Alpha-style per-frame `WaterSprite`/`WaterSideSprite` animation plus flow-angle UV mapping on liquid top faces.
- [ ] Exclusions (Ensure these are absent): No Beds, no Tall Grass, no Lapis Lazuli, no Sandstone, no Pistons.
- [ ] Food Mechanics: No Hunger bar (food heals health instantly).

## Entities and Mobs
- [x] Entity Framework: bevy_ecs entity lifecycle with components for physics, age, pickup delay, and render interpolation. Simplified AABB collision (Alpha ItemEntity constants), solid-block ejection, and age-based despawn.
- [x] Dropped Items: Breaking a block spawns a dropped item entity with Alpha-faithful physics (gravity 0.04, bounce, drag, 10-tick pickup delay, 5-minute despawn). Items render as billboarded terrain atlas sprites with bobbing animation.
- [~] Player Survival Loop: Core vitals state is now active (20 HP hearts, invulnerability damage window, hurt timer, fall-damage application, drowning breath drain/damage, fire/lava damage-over-time, manual respawn after death timer, Alpha-style water/lava locomotion constants including swim-up and wall-surface assist, Alpha-style crouch/sneak locomotion with edge-safe movement, and Alpha-style first-person hand/item animation with equip + swing timing), and dead-state flow now gates player control/input and unlocks mouse until respawn; full parity (mob combat/knockback, death screen buttons/text UX, food-based healing) is still pending.
- [~] In-Game HUD: Alpha-style textured HUD rendering is now active (`gui.png` + `icons.png` + terrain item sprites) with real hotbar art, crosshair, selected-slot frame, heart bar flash/jitter behavior, underwater air bubbles, and block-style hotbar item icon treatment (solid blocks rendered as mini 3-face icons). Inventory-screen rendering is now integrated (`inventory.png`, slot hover, cursor-held item render), while stack-count text and rotating player preview are still pending.
- [ ] Passive Mobs: Pigs, Sheep, Cows, Chickens.
- [ ] Hostile Mobs: Zombies, Skeletons, Spiders, Creepers, Slimes.
- [ ] Nether Mobs: Ghasts, Zombie Pigmen.
- [~] Mechanical Entities: Minecarts and Boats pending; dropped items implemented.
