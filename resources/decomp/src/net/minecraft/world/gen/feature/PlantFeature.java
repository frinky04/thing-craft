/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class PlantFeature
extends Feature {
    private int flower;

    public PlantFeature(int flower) {
        this.flower = flower;
    }

    public boolean place(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 64; ++i) {
            int l;
            int k;
            int j = x + random.nextInt(8) - random.nextInt(8);
            if (world.getBlock(j, k = y + random.nextInt(4) - random.nextInt(4), l = z + random.nextInt(8) - random.nextInt(8)) != 0 || !((PlantBlock)Block.BY_ID[this.flower]).canSurvive(world, j, k, l)) continue;
            world.setBlockQuietly(j, k, l, this.flower);
        }
        return true;
    }
}

