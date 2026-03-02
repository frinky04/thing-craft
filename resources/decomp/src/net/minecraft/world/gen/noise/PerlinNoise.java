/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.noise;

import java.util.Random;
import net.minecraft.world.gen.noise.ImprovedNoise;
import net.minecraft.world.gen.noise.Noise;

public class PerlinNoise
extends Noise {
    private ImprovedNoise[] noiseLevels;
    private int levels;

    public PerlinNoise(Random random, int levels) {
        this.levels = levels;
        this.noiseLevels = new ImprovedNoise[levels];
        for (int i = 0; i < levels; ++i) {
            this.noiseLevels[i] = new ImprovedNoise(random);
        }
    }

    public double getValue(double x, double y) {
        double d = 0.0;
        double e = 1.0;
        for (int i = 0; i < this.levels; ++i) {
            d += this.noiseLevels[i].getValue(x * e, y * e) / e;
            e /= 2.0;
        }
        return d;
    }

    public double[] getRegion(double[] values, double x, double y, double z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ) {
        if (values == null) {
            values = new double[sizeX * sizeY * sizeZ];
        } else {
            for (int i = 0; i < values.length; ++i) {
                values[i] = 0.0;
            }
        }
        double d = 1.0;
        for (int j = 0; j < this.levels; ++j) {
            this.noiseLevels[j].add(values, x, y, z, sizeX, sizeY, sizeZ, scaleX * d, scaleY * d, scaleZ * d, d);
            d /= 2.0;
        }
        return values;
    }

    public double[] getRegion(double[] values, int x, int z, int sizeX, int sizeZ, double scaleX, double scaleZ, double d) {
        return this.getRegion(values, x, 10.0, z, sizeX, 1, sizeZ, scaleX, 1.0, scaleZ);
    }
}

