/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.ornithemc.feather.constants.Dimensions
 */
package net.minecraft.world.dimension;

import java.io.File;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import net.minecraft.world.chunk.storage.ChunkStorage;
import net.minecraft.world.dimension.NetherDimension;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.ornithemc.feather.constants.Dimensions;

public class Dimension {
    public World world;
    public BiomeSource biomeSource;
    public boolean unnatural = false;
    public boolean yeetsWater = false;
    public boolean noSky = false;
    public float[] brightnessTable = new float[16];
    public int id = 0;
    private float[] sunriseColor = new float[4];

    public final void init(World world) {
        this.world = world;
        this.initBiomeSource();
        this.initBrightnessTable();
    }

    protected void initBrightnessTable() {
        float f = 0.05f;
        for (int i = 0; i <= 15; ++i) {
            float g = 1.0f - (float)i / 15.0f;
            this.brightnessTable[i] = (1.0f - g) / (g * 3.0f + 1.0f) * (1.0f - f) + f;
        }
    }

    protected void initBiomeSource() {
        this.biomeSource = new BiomeSource(this.world);
    }

    public ChunkSource createChunkGenerator() {
        return new OverworldChunkGenerator(this.world, this.world.seed);
    }

    public ChunkStorage createChunkStorage(File dir) {
        return new AlphaChunkStorage(dir, true);
    }

    public boolean isValidSpawnPoint(int x, int z) {
        int i = this.world.getSurfaceBlock(x, z);
        return i == Block.SAND.id;
    }

    public float getTimeOfDay(long time, float tickDelta) {
        int i = (int)(time % 24000L);
        float f = ((float)i + tickDelta) / 24000.0f - 0.25f;
        if (f < 0.0f) {
            f += 1.0f;
        }
        if (f > 1.0f) {
            f -= 1.0f;
        }
        float g = f;
        f = 1.0f - (float)((Math.cos((double)f * Math.PI) + 1.0) / 2.0);
        f = g + (f - g) / 3.0f;
        return f;
    }

    @Environment(value=EnvType.CLIENT)
    public float[] getSunriseColor(float timeOfDay, float tickDelta) {
        float h;
        float f = 0.4f;
        float g = MathHelper.cos(timeOfDay * (float)Math.PI * 2.0f) - 0.0f;
        if (g >= (h = -0.0f) - f && g <= h + f) {
            float i = (g - h) / f * 0.5f + 0.5f;
            float j = 1.0f - (1.0f - MathHelper.sin(i * (float)Math.PI)) * 0.99f;
            j *= j;
            this.sunriseColor[0] = i * 0.3f + 0.7f;
            this.sunriseColor[1] = i * i * 0.7f + 0.2f;
            this.sunriseColor[2] = i * i * 0.0f + 0.2f;
            this.sunriseColor[3] = j;
            return this.sunriseColor;
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d getFogColor(float timeOfDay, float tickDelta) {
        float f = MathHelper.cos(timeOfDay * (float)Math.PI * 2.0f) * 2.0f + 0.5f;
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f > 1.0f) {
            f = 1.0f;
        }
        float g = 0.7529412f;
        float h = 0.84705883f;
        float i = 1.0f;
        return Vec3d.fromPool(g *= f * 0.94f + 0.06f, h *= f * 0.94f + 0.06f, i *= f * 0.91f + 0.09f);
    }

    @Environment(value=EnvType.CLIENT)
    public boolean hasSpawnPoint() {
        return true;
    }

    public static Dimension fromId(int id) {
        if (id == 0) {
            return new Dimension();
        }
        if (id == Dimensions.NETHER) {
            return new NetherDimension();
        }
        return null;
    }
}

