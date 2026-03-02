/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.world.World;

public class MushroomPlantBlock
extends PlantBlock {
    protected MushroomPlantBlock(int i, int j) {
        super(i, j);
        float f = 0.2f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, f * 2.0f, 0.5f + f);
    }

    protected boolean canBePlacedOn(int block) {
        return Block.IS_SOLID[block];
    }

    public boolean canSurvive(World world, int x, int y, int z) {
        return world.getRawBrightness(x, y, z) <= 13 && this.canBePlacedOn(world.getBlock(x, y - 1, z));
    }
}

