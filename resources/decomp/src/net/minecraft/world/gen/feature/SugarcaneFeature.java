/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class SugarcaneFeature
extends Feature {
    public boolean place(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 20; ++i) {
            int l;
            int k;
            int j = x + random.nextInt(4) - random.nextInt(4);
            if (world.getBlock(j, k = y, l = z + random.nextInt(4) - random.nextInt(4)) != 0 || world.getMaterial(j - 1, k - 1, l) != Material.WATER && world.getMaterial(j + 1, k - 1, l) != Material.WATER && world.getMaterial(j, k - 1, l - 1) != Material.WATER && world.getMaterial(j, k - 1, l + 1) != Material.WATER) continue;
            int m = 2 + random.nextInt(random.nextInt(3) + 1);
            for (int n = 0; n < m; ++n) {
                if (!Block.REEDS.canSurvive(world, j, k + n, l)) continue;
                world.setBlockQuietly(j, k + n, l, Block.REEDS.id);
            }
        }
        return true;
    }
}

