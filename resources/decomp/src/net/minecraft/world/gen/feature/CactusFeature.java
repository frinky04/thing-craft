/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class CactusFeature
extends Feature {
    public boolean place(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 10; ++i) {
            int l;
            int k;
            int j = x + random.nextInt(8) - random.nextInt(8);
            if (world.getBlock(j, k = y + random.nextInt(4) - random.nextInt(4), l = z + random.nextInt(8) - random.nextInt(8)) != 0) continue;
            int m = 1 + random.nextInt(random.nextInt(3) + 1);
            for (int n = 0; n < m; ++n) {
                if (!Block.CACTUS.canSurvive(world, j, k + n, l)) continue;
                world.setBlockQuietly(j, k + n, l, Block.CACTUS.id);
            }
        }
        return true;
    }
}

