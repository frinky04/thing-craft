/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.item.ToolItem;

public class AxeItem
extends ToolItem {
    private static Block[] EFFECTIVE_BLOCKS = new Block[]{Block.PLANKS, Block.BOOKSHELF, Block.LOG, Block.CHEST};

    public AxeItem(int id, int tier) {
        super(id, 3, tier, EFFECTIVE_BLOCKS);
    }
}

