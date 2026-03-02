/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class TreeFeature
extends Feature {
    public boolean place(World world, Random random, int x, int y, int z) {
        int m;
        int k;
        int i = random.nextInt(3) + 4;
        boolean j = true;
        if (y < 1 || y + i + 1 > 128) {
            return false;
        }
        for (k = y; k <= y + 1 + i; ++k) {
            int l = 1;
            if (k == y) {
                l = 0;
            }
            if (k >= y + 1 + i - 2) {
                l = 2;
            }
            for (int n = x - l; n <= x + l && j; ++n) {
                for (int q = z - l; q <= z + l && j; ++q) {
                    if (k >= 0 && k < 128) {
                        int s = world.getBlock(n, k, q);
                        if (s == 0 || s == Block.LEAVES.id) continue;
                        j = false;
                        continue;
                    }
                    j = false;
                }
            }
        }
        if (!j) {
            return false;
        }
        k = world.getBlock(x, y - 1, z);
        if (k != Block.GRASS.id && k != Block.DIRT.id || y >= 128 - i - 1) {
            return false;
        }
        world.setBlockQuietly(x, y - 1, z, Block.DIRT.id);
        for (m = y - 3 + i; m <= y + i; ++m) {
            int o = m - (y + i);
            int r = 1 - o / 2;
            for (int t = x - r; t <= x + r; ++t) {
                int u = t - x;
                for (int v = z - r; v <= z + r; ++v) {
                    int w = v - z;
                    if (Math.abs(u) == r && Math.abs(w) == r && (random.nextInt(2) == 0 || o == 0) || Block.IS_SOLID[world.getBlock(t, m, v)]) continue;
                    world.setBlockQuietly(t, m, v, Block.LEAVES.id);
                }
            }
        }
        for (m = 0; m < i; ++m) {
            int p = world.getBlock(x, y + m, z);
            if (p != 0 && p != Block.LEAVES.id) continue;
            world.setBlockQuietly(x, y + m, z, Block.LOG.id);
        }
        return true;
    }
}

