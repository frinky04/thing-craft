/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen;

import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;

public class Generator {
    protected int range = 8;
    protected Random random = new Random();

    public void place(ChunkSource source, World world, int chunkX, int chunkZ, byte[] blocks) {
        int i = this.range;
        this.random.setSeed(world.seed);
        long l = this.random.nextLong() / 2L * 2L + 1L;
        long m = this.random.nextLong() / 2L * 2L + 1L;
        for (int j = chunkX - i; j <= chunkX + i; ++j) {
            for (int k = chunkZ - i; k <= chunkZ + i; ++k) {
                this.random.setSeed((long)j * l + (long)k * m ^ world.seed);
                this.place(world, j, k, chunkX, chunkZ, blocks);
            }
        }
    }

    protected void place(World world, int startChunkX, int startChunkZ, int chunkX, int chunkZ, byte[] blocks) {
    }
}

