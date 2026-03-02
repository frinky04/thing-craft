/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class LakeFeature
extends Feature {
    private int liquid;

    public LakeFeature(int liquid) {
        this.liquid = liquid;
    }

    public boolean place(World world, Random random, int x, int y, int z) {
        int j;
        x -= 8;
        z -= 8;
        while (y > 0 && world.getBlock(x, y, z) == 0) {
            --y;
        }
        y -= 4;
        boolean[] bls = new boolean[2048];
        int i = random.nextInt(4) + 4;
        for (j = 0; j < i; ++j) {
            double d = random.nextDouble() * 6.0 + 3.0;
            double e = random.nextDouble() * 4.0 + 2.0;
            double f = random.nextDouble() * 6.0 + 3.0;
            double g = random.nextDouble() * (16.0 - d - 2.0) + 1.0 + d / 2.0;
            double h = random.nextDouble() * (8.0 - e - 4.0) + 2.0 + e / 2.0;
            double r = random.nextDouble() * (16.0 - f - 2.0) + 1.0 + f / 2.0;
            for (int s = 1; s < 15; ++s) {
                for (int t = 1; t < 15; ++t) {
                    for (int u = 1; u < 7; ++u) {
                        double v = ((double)s - g) / (d / 2.0);
                        double w = ((double)u - h) / (e / 2.0);
                        double aa = ((double)t - r) / (f / 2.0);
                        double ab = v * v + w * w + aa * aa;
                        if (!(ab < 1.0)) continue;
                        bls[(s * 16 + t) * 8 + u] = true;
                    }
                }
            }
        }
        for (j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int n = 0; n < 8; ++n) {
                    boolean q;
                    boolean bl = q = !bls[(j * 16 + k) * 8 + n] && (j < 15 && bls[((j + 1) * 16 + k) * 8 + n] || j > 0 && bls[((j - 1) * 16 + k) * 8 + n] || k < 15 && bls[(j * 16 + (k + 1)) * 8 + n] || k > 0 && bls[(j * 16 + (k - 1)) * 8 + n] || n < 7 && bls[(j * 16 + k) * 8 + (n + 1)] || n > 0 && bls[(j * 16 + k) * 8 + (n - 1)]);
                    if (!q) continue;
                    Material material = world.getMaterial(x + j, y + n, z + k);
                    if (n >= 4 && material.isLiquid()) {
                        return false;
                    }
                    if (n >= 4 || material.isSolid() || world.getBlock(x + j, y + n, z + k) == this.liquid) continue;
                    return false;
                }
            }
        }
        for (j = 0; j < 16; ++j) {
            for (int l = 0; l < 16; ++l) {
                for (int o = 0; o < 8; ++o) {
                    if (!bls[(j * 16 + l) * 8 + o]) continue;
                    world.setBlock(x + j, y + o, z + l, o >= 4 ? 0 : this.liquid);
                }
            }
        }
        for (j = 0; j < 16; ++j) {
            for (int m = 0; m < 16; ++m) {
                for (int p = 4; p < 8; ++p) {
                    if (!bls[(j * 16 + m) * 8 + p] || world.getBlock(x + j, y + p - 1, z + m) != Block.DIRT.id || world.getLight(LightType.SKY, x + j, y + p, z + m) <= 0) continue;
                    world.setBlock(x + j, y + p - 1, z + m, Block.GRASS.id);
                }
            }
        }
        return true;
    }
}

