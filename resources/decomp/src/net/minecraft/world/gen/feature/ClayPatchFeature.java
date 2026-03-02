/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class ClayPatchFeature
extends Feature {
    private int clay;
    private int size;

    public ClayPatchFeature(int size) {
        this.clay = Block.CLAY.id;
        this.size = size;
    }

    public boolean place(World world, Random random, int x, int y, int z) {
        if (world.getMaterial(x, y, z) != Material.WATER) {
            return false;
        }
        float f = random.nextFloat() * (float)Math.PI;
        double d = (float)(x + 8) + MathHelper.sin(f) * (float)this.size / 8.0f;
        double e = (float)(x + 8) - MathHelper.sin(f) * (float)this.size / 8.0f;
        double g = (float)(z + 8) + MathHelper.cos(f) * (float)this.size / 8.0f;
        double h = (float)(z + 8) - MathHelper.cos(f) * (float)this.size / 8.0f;
        double i = y + random.nextInt(3) + 2;
        double j = y + random.nextInt(3) + 2;
        for (int k = 0; k <= this.size; ++k) {
            double l = d + (e - d) * (double)k / (double)this.size;
            double m = i + (j - i) * (double)k / (double)this.size;
            double n = g + (h - g) * (double)k / (double)this.size;
            double o = random.nextDouble() * (double)this.size / 16.0;
            double p = (double)(MathHelper.sin((float)k * (float)Math.PI / (float)this.size) + 1.0f) * o + 1.0;
            double q = (double)(MathHelper.sin((float)k * (float)Math.PI / (float)this.size) + 1.0f) * o + 1.0;
            for (int r = (int)(l - p / 2.0); r <= (int)(l + p / 2.0); ++r) {
                for (int s = (int)(m - q / 2.0); s <= (int)(m + q / 2.0); ++s) {
                    for (int t = (int)(n - p / 2.0); t <= (int)(n + p / 2.0); ++t) {
                        int aa;
                        double u = ((double)r + 0.5 - l) / (p / 2.0);
                        double v = ((double)s + 0.5 - m) / (q / 2.0);
                        double w = ((double)t + 0.5 - n) / (p / 2.0);
                        if (!(u * u + v * v + w * w < 1.0) || (aa = world.getBlock(r, s, t)) != Block.SAND.id) continue;
                        world.setBlockQuietly(r, s, t, this.clay);
                    }
                }
            }
        }
        return true;
    }
}

