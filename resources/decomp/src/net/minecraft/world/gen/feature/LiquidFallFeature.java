/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class LiquidFallFeature
extends Feature {
    private int liquid;

    public LiquidFallFeature(int liquid) {
        this.liquid = liquid;
    }

    public boolean place(World world, Random random, int x, int y, int z) {
        if (world.getBlock(x, y + 1, z) != Block.STONE.id) {
            return false;
        }
        if (world.getBlock(x, y - 1, z) != Block.STONE.id) {
            return false;
        }
        if (world.getBlock(x, y, z) != 0 && world.getBlock(x, y, z) != Block.STONE.id) {
            return false;
        }
        int i = 0;
        if (world.getBlock(x - 1, y, z) == Block.STONE.id) {
            ++i;
        }
        if (world.getBlock(x + 1, y, z) == Block.STONE.id) {
            ++i;
        }
        if (world.getBlock(x, y, z - 1) == Block.STONE.id) {
            ++i;
        }
        if (world.getBlock(x, y, z + 1) == Block.STONE.id) {
            ++i;
        }
        int j = 0;
        if (world.getBlock(x - 1, y, z) == 0) {
            ++j;
        }
        if (world.getBlock(x + 1, y, z) == 0) {
            ++j;
        }
        if (world.getBlock(x, y, z - 1) == 0) {
            ++j;
        }
        if (world.getBlock(x, y, z + 1) == 0) {
            ++j;
        }
        if (i == 3 && j == 1) {
            world.setBlock(x, y, z, this.liquid);
            world.doTicksImmediately = true;
            Block.BY_ID[this.liquid].tick(world, x, y, z, random);
            world.doTicksImmediately = false;
        }
        return true;
    }
}

