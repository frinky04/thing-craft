/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.dimension;

import java.io.File;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import net.minecraft.world.chunk.storage.ChunkStorage;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.gen.chunk.NetherChunkGenerator;

public class NetherDimension
extends Dimension {
    public void initBiomeSource() {
        this.biomeSource = new FixedBiomeSource(Biome.HELL, 1.0, 0.0);
        this.unnatural = true;
        this.yeetsWater = true;
        this.noSky = true;
        this.id = -1;
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d getFogColor(float timeOfDay, float tickDelta) {
        return Vec3d.fromPool(0.2f, 0.03f, 0.03f);
    }

    protected void initBrightnessTable() {
        float f = 0.1f;
        for (int i = 0; i <= 15; ++i) {
            float g = 1.0f - (float)i / 15.0f;
            this.brightnessTable[i] = (1.0f - g) / (g * 3.0f + 1.0f) * (1.0f - f) + f;
        }
    }

    public ChunkSource createChunkGenerator() {
        return new NetherChunkGenerator(this.world, this.world.seed);
    }

    public ChunkStorage createChunkStorage(File dir) {
        File file = new File(dir, "DIM-1");
        file.mkdirs();
        return new AlphaChunkStorage(file, true);
    }

    public boolean isValidSpawnPoint(int x, int z) {
        int i = this.world.getSurfaceBlock(x, z);
        if (i == Block.BEDROCK.id) {
            return false;
        }
        if (i == 0) {
            return false;
        }
        return Block.IS_SOLID[i];
    }

    public float getTimeOfDay(long time, float tickDelta) {
        return 0.5f;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean hasSpawnPoint() {
        return false;
    }
}

