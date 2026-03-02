/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class GlowstoneClusterFeature
extends Feature {
    public boolean place(World world, Random random, int x, int y, int z) {
        if (world.getBlock(x, y, z) != 0) {
            return false;
        }
        if (world.getBlock(x, y + 1, z) != Block.NETHERRACK.id) {
            return false;
        }
        world.setBlock(x, y, z, Block.GLOWSTONE.id);
        for (int i = 0; i < 1500; ++i) {
            int l;
            int k;
            int j = x + random.nextInt(8) - random.nextInt(8);
            if (world.getBlock(j, k = y - random.nextInt(12), l = z + random.nextInt(8) - random.nextInt(8)) != 0) continue;
            int m = 0;
            for (int n = 0; n < 6; ++n) {
                int o = 0;
                if (n == 0) {
                    o = world.getBlock(j - 1, k, l);
                }
                if (n == 1) {
                    o = world.getBlock(j + 1, k, l);
                }
                if (n == 2) {
                    o = world.getBlock(j, k - 1, l);
                }
                if (n == 3) {
                    o = world.getBlock(j, k + 1, l);
                }
                if (n == 4) {
                    o = world.getBlock(j, k, l - 1);
                }
                if (n == 5) {
                    o = world.getBlock(j, k, l + 1);
                }
                if (o != Block.GLOWSTONE.id) continue;
                ++m;
            }
            if (m != true) continue;
            world.setBlock(j, k, l, Block.GLOWSTONE.id);
        }
        return true;
    }
}

