/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.biome.source;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.noise.PerlinSimplexNoise;

public class BiomeSource {
    private PerlinSimplexNoise temperatureNoise;
    private PerlinSimplexNoise downfallNoise;
    private PerlinSimplexNoise biomeNoise;
    public double[] temperatures;
    public double[] downfalls;
    public double[] noises;
    public Biome[] biomesBuffer;

    protected BiomeSource() {
    }

    public BiomeSource(World world) {
        this.temperatureNoise = new PerlinSimplexNoise(new Random(world.seed * 9871L), 4);
        this.downfallNoise = new PerlinSimplexNoise(new Random(world.seed * 39811L), 4);
        this.biomeNoise = new PerlinSimplexNoise(new Random(world.seed * 543321L), 2);
    }

    public Biome getBiome(ChunkPos pos) {
        return this.getBiome(pos.x, pos.z);
    }

    public Biome getBiome(int x, int z) {
        return this.getBiomes(x, z, 1, 1)[0];
    }

    @Environment(value=EnvType.CLIENT)
    public double getTemperature(int x, int z) {
        this.temperatures = this.temperatureNoise.getRegion(this.temperatures, x, z, 1, 1, 0.025f, 0.025f, 0.5);
        return this.temperatures[0];
    }

    public Biome[] getBiomes(int x, int z, int sizeX, int sizeZ) {
        this.biomesBuffer = this.getBiomes(this.biomesBuffer, x, z, sizeX, sizeZ);
        return this.biomesBuffer;
    }

    public double[] getTemperatures(double[] temperatures, int x, int z, int sizeX, int sizeZ) {
        if (temperatures == null || temperatures.length < sizeX * sizeZ) {
            temperatures = new double[sizeX * sizeZ];
        }
        temperatures = this.temperatureNoise.getRegion(temperatures, x, z, sizeX, sizeX, 0.025f, 0.025f, 0.25);
        this.noises = this.biomeNoise.getRegion(this.noises, x, z, sizeX, sizeX, 0.25, 0.25, 0.5882352941176471);
        int i = 0;
        for (int j = 0; j < sizeX; ++j) {
            for (int k = 0; k < sizeZ; ++k) {
                double d = this.noises[i] * 1.1 + 0.5;
                double e = 0.01;
                double f = 1.0 - e;
                double g = (temperatures[i] * 0.15 + 0.7) * f + d * e;
                if ((g = 1.0 - (1.0 - g) * (1.0 - g)) < 0.0) {
                    g = 0.0;
                }
                if (g > 1.0) {
                    g = 1.0;
                }
                temperatures[i] = g;
                ++i;
            }
        }
        return temperatures;
    }

    public Biome[] getBiomes(Biome[] biomes, int x, int z, int sizeX, int sizeZ) {
        if (biomes == null || biomes.length < sizeX * sizeZ) {
            biomes = new Biome[sizeX * sizeZ];
        }
        this.temperatures = this.temperatureNoise.getRegion(this.temperatures, x, z, sizeX, sizeX, 0.025f, 0.025f, 0.25);
        this.downfalls = this.downfallNoise.getRegion(this.downfalls, x, z, sizeX, sizeX, 0.05f, 0.05f, 0.3333333333333333);
        this.noises = this.biomeNoise.getRegion(this.noises, x, z, sizeX, sizeX, 0.25, 0.25, 0.5882352941176471);
        int i = 0;
        for (int j = 0; j < sizeX; ++j) {
            for (int k = 0; k < sizeZ; ++k) {
                double d = this.noises[i] * 1.1 + 0.5;
                double e = 0.01;
                double f = 1.0 - e;
                double g = (this.temperatures[i] * 0.15 + 0.7) * f + d * e;
                e = 0.002;
                f = 1.0 - e;
                double h = (this.downfalls[i] * 0.15 + 0.5) * f + d * e;
                if ((g = 1.0 - (1.0 - g) * (1.0 - g)) < 0.0) {
                    g = 0.0;
                }
                if (h < 0.0) {
                    h = 0.0;
                }
                if (g > 1.0) {
                    g = 1.0;
                }
                if (h > 1.0) {
                    h = 1.0;
                }
                this.temperatures[i] = g;
                this.downfalls[i] = h;
                biomes[i++] = Biome.getBiome(g, h);
            }
        }
        return biomes;
    }
}

