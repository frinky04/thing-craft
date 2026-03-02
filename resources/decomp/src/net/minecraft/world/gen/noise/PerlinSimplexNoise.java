/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.noise;

import java.util.Random;
import net.minecraft.world.gen.noise.Noise;
import net.minecraft.world.gen.noise.SimplexNoise;

public class PerlinSimplexNoise
extends Noise {
    private SimplexNoise[] noiseLevels;
    private int levels;

    public PerlinSimplexNoise(Random random, int levels) {
        this.levels = levels;
        this.noiseLevels = new SimplexNoise[levels];
        for (int i = 0; i < levels; ++i) {
            this.noiseLevels[i] = new SimplexNoise(random);
        }
    }

    public double[] getRegion(double[] values, double x, double y, int sizeX, int sizeY, double scaleX, double scaleY, double scaleExponent) {
        return this.getRegion(values, x, y, sizeX, sizeY, scaleX, scaleY, scaleExponent, 0.5);
    }

    public double[] getRegion(double[] values, double x, double y, int sizeX, int sizeY, double scaleX, double scaleY, double scaleExponentX, double scaleExponentY) {
        scaleX /= 1.5;
        scaleY /= 1.5;
        if (values == null || values.length < sizeX * sizeY) {
            values = new double[sizeX * sizeY];
        } else {
            for (int i = 0; i < values.length; ++i) {
                values[i] = 0.0;
            }
        }
        double d = 1.0;
        double e = 1.0;
        for (int j = 0; j < this.levels; ++j) {
            this.noiseLevels[j].add(values, x, y, sizeX, sizeY, scaleX * e, scaleY * e, 0.55 / d);
            e *= scaleExponentX;
            d *= scaleExponentY;
        }
        return values;
    }
}

