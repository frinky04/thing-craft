/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ToolItem;

public class PickaxeItem
extends ToolItem {
    private static Block[] EFFECTIVE_BLOCKS = new Block[]{Block.COBBLESTONE, Block.DOUBLE_STONE_SLAB, Block.STONE_SLAB, Block.STONE, Block.MOSSY_COBBLESTONE, Block.IRON_ORE, Block.IRON_BLOCK, Block.COAL_ORE, Block.GOLD_BLOCK, Block.GOLD_ORE, Block.DIAMOND_ORE, Block.DIAMOND_BLOCK, Block.ICE, Block.NETHERRACK};
    private int tier;

    public PickaxeItem(int id, int tier) {
        super(id, 2, tier, EFFECTIVE_BLOCKS);
        this.tier = tier;
    }

    public boolean canMineBlock(Block block) {
        if (block == Block.OBSIDIAN) {
            return this.tier == 3;
        }
        if (block == Block.DIAMOND_BLOCK || block == Block.DIAMOND_ORE) {
            return this.tier >= 2;
        }
        if (block == Block.GOLD_BLOCK || block == Block.GOLD_ORE) {
            return this.tier >= 2;
        }
        if (block == Block.IRON_BLOCK || block == Block.IRON_ORE) {
            return this.tier >= 1;
        }
        if (block == Block.REDSTONE_ORE || block == Block.LIT_REDSTONE_ORE) {
            return this.tier >= 2;
        }
        if (block.material == Material.STONE) {
            return true;
        }
        return block.material == Material.IRON;
    }
}

