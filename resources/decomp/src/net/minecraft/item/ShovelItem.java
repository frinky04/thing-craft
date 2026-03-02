/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.item.ToolItem;

public class ShovelItem
extends ToolItem {
    private static Block[] EFFECTIVE_BLOCKS = new Block[]{Block.GRASS, Block.DIRT, Block.SAND, Block.GRAVEL, Block.SNOW_LAYER, Block.SNOW, Block.CLAY};

    public ShovelItem(int id, int tier) {
        super(id, 1, tier, EFFECTIVE_BLOCKS);
    }

    public boolean canMineBlock(Block block) {
        if (block == Block.SNOW_LAYER) {
            return true;
        }
        return block == Block.SNOW;
    }
}

