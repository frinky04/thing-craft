# Reference Sources (Alpha 1.2.6)

This project follows the rule from `WHAT.md`: steal numbers, not structure.

## Canonical Inputs

- Decompiled source of named classes:
  - `resources/decomp/src/net/minecraft/**`
- Original Alpha-era assets:
  - `resources/minecraft-a1.2.6-client/terrain.png`
  - `resources/minecraft-a1.2.6-client/gui/items.png`
  - `resources/minecraft-a1.2.6-client/gui/icons.png`
  - `resources/minecraft-a1.2.6-client/particles.png`
  - `resources/minecraft-a1.2.6-client/newsound/**`
  - `resources/minecraft-a1.2.6-client/music/**`
- Fallback/extended assets (optional):
  - `resources/default_resource_pack/**`

## Numbers Adopted So Far

- World/chunk dimensions and indexing:
  - `16 x 16 x 128` blocks per chunk
  - block index formula: `(x << 11) | (z << 7) | y`
  - Source: `resources/decomp/src/net/minecraft/world/chunk/WorldChunk.java`
- Overworld sea/water level target:
  - `Y=64`
  - Source: `resources/decomp/src/net/minecraft/world/gen/chunk/OverworldChunkGenerator.java`
- Fixed tick target:
  - `20 TPS`
  - Source: `resources/decomp/src/net/minecraft/client/TickTimer.java`
- Block light scale:
  - light levels `0..15`
  - Source: `resources/decomp/src/net/minecraft/block/Block.java`
- Biome climate seed multipliers:
  - temperature `seed * 9871`
  - downfall `seed * 39811`
  - biome-mix `seed * 543321`
  - Source: `resources/decomp/src/net/minecraft/world/biome/source/BiomeSource.java`
- Terrain noise scale anchor:
  - `684.412` (used as terrain frequency reference)
  - Source: `resources/decomp/src/net/minecraft/world/gen/chunk/OverworldChunkGenerator.java`

## Alpha Exclusions Guarded in Registry

The current block registry explicitly keeps these Alpha-absent IDs undefined:

- 21 `lapis_ore`
- 22 `lapis_block`
- 23 `dispenser`
- 24 `sandstone`
- 25 `noteblock`
- 26 `bed`
- 27 `powered_rail`
- 28 `detector_rail`
- 29 `sticky_piston`
- 30 `web`
- 31 `tallgrass`
- 32 `deadbush`
- 33 `piston`
- 34 `piston_head`
- 36 `moving_block`

Source: `resources/decomp/src/net/minecraft/block/Block.java`
