/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.WorldChunk;

public class WorldRegion
implements WorldView {
    private int chunkX;
    private int chunkZ;
    private WorldChunk[][] chunks;
    private World world;

    public WorldRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = world;
        this.chunkX = minX >> 4;
        this.chunkZ = minZ >> 4;
        int i = maxX >> 4;
        int j = maxZ >> 4;
        this.chunks = new WorldChunk[i - this.chunkX + 1][j - this.chunkZ + 1];
        for (int k = this.chunkX; k <= i; ++k) {
            for (int l = this.chunkZ; l <= j; ++l) {
                this.chunks[k - this.chunkX][l - this.chunkZ] = world.getChunkAt(k, l);
            }
        }
    }

    public int getBlock(int x, int y, int z) {
        if (y < 0) {
            return 0;
        }
        if (y >= 128) {
            return 0;
        }
        int i = (x >> 4) - this.chunkX;
        int j = (z >> 4) - this.chunkZ;
        return this.chunks[i][j].getBlockAt(x & 0xF, y, z & 0xF);
    }

    @Environment(value=EnvType.CLIENT)
    public BlockEntity getBlockEntity(int x, int y, int z) {
        int i = (x >> 4) - this.chunkX;
        int j = (z >> 4) - this.chunkZ;
        return this.chunks[i][j].getBlockEntityAt(x & 0xF, y, z & 0xF);
    }

    @Environment(value=EnvType.CLIENT)
    public float getBrightness(int x, int y, int z) {
        return this.world.dimension.brightnessTable[this.getRawBrightness(x, y, z)];
    }

    @Environment(value=EnvType.CLIENT)
    public int getRawBrightness(int x, int y, int z) {
        return this.getRawBrightness(x, y, z, true);
    }

    @Environment(value=EnvType.CLIENT)
    public int getRawBrightness(int x, int y, int z, boolean useNeighborLight) {
        int i;
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return 15;
        }
        if (useNeighborLight && ((i = this.getBlock(x, y, z)) == Block.STONE_SLAB.id || i == Block.FARMLAND.id)) {
            int l = this.getRawBrightness(x, y + 1, z, false);
            int n = this.getRawBrightness(x + 1, y, z, false);
            int o = this.getRawBrightness(x - 1, y, z, false);
            int p = this.getRawBrightness(x, y, z + 1, false);
            int q = this.getRawBrightness(x, y, z - 1, false);
            if (n > l) {
                l = n;
            }
            if (o > l) {
                l = o;
            }
            if (p > l) {
                l = p;
            }
            if (q > l) {
                l = q;
            }
            return l;
        }
        if (y < 0) {
            return 0;
        }
        if (y >= 128) {
            int j = 15 - this.world.ambientDarkness;
            if (j < 0) {
                j = 0;
            }
            return j;
        }
        int k = (x >> 4) - this.chunkX;
        int m = (z >> 4) - this.chunkZ;
        return this.chunks[k][m].getActualLightAt(x & 0xF, y, z & 0xF, this.world.ambientDarkness);
    }

    public int getBlockMetadata(int x, int y, int z) {
        if (y < 0) {
            return 0;
        }
        if (y >= 128) {
            return 0;
        }
        int i = (x >> 4) - this.chunkX;
        int j = (z >> 4) - this.chunkZ;
        return this.chunks[i][j].getBlockMetadataAt(x & 0xF, y, z & 0xF);
    }

    public Material getMaterial(int x, int y, int z) {
        int i = this.getBlock(x, y, z);
        if (i == 0) {
            return Material.AIR;
        }
        return Block.BY_ID[i].material;
    }

    public boolean isSolidBlock(int x, int y, int z) {
        Block block = Block.BY_ID[this.getBlock(x, y, z)];
        if (block == null) {
            return false;
        }
        return block.isSolid();
    }

    @Environment(value=EnvType.CLIENT)
    public BiomeSource getBiomeSource() {
        return this.world.getBiomeSource();
    }
}

