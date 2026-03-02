/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.biome.source;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;

public class FixedBiomeSource
extends BiomeSource {
    private Biome biome;
    private double temperature;
    private double downfall;

    public FixedBiomeSource(Biome biome, double temperature, double downfall) {
        this.biome = biome;
        this.temperature = temperature;
        this.downfall = downfall;
    }

    public Biome getBiome(ChunkPos pos) {
        return this.biome;
    }

    public Biome getBiome(int x, int z) {
        return this.biome;
    }

    @Environment(value=EnvType.CLIENT)
    public double getTemperature(int x, int z) {
        return this.temperature;
    }

    public Biome[] getBiomes(int x, int z, int sizeX, int sizeZ) {
        this.biomesBuffer = this.getBiomes(this.biomesBuffer, x, z, sizeX, sizeZ);
        return this.biomesBuffer;
    }

    public double[] getTemperatures(double[] temperatures, int x, int z, int sizeX, int sizeZ) {
        if (temperatures == null || temperatures.length < sizeX * sizeZ) {
            temperatures = new double[sizeX * sizeZ];
        }
        Arrays.fill(temperatures, 0, sizeX * sizeZ, this.temperature);
        return temperatures;
    }

    public Biome[] getBiomes(Biome[] biomes, int x, int z, int sizeX, int sizeZ) {
        if (biomes == null || biomes.length < sizeX * sizeZ) {
            biomes = new Biome[sizeX * sizeZ];
            this.temperatures = new double[sizeX * sizeZ];
            this.downfalls = new double[sizeX * sizeZ];
        }
        Arrays.fill(biomes, 0, sizeX * sizeZ, this.biome);
        Arrays.fill(this.downfalls, 0, sizeX * sizeZ, this.downfall);
        Arrays.fill(this.temperatures, 0, sizeX * sizeZ, this.temperature);
        return biomes;
    }
}

