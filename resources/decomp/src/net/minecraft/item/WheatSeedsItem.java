/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class WheatSeedsItem
extends Item {
    private int plantBlock;

    public WheatSeedsItem(int item, int plantBlock) {
        super(item);
        this.plantBlock = plantBlock;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        if (face != 1) {
            return false;
        }
        int i = world.getBlock(x, y, z);
        if (i == Block.FARMLAND.id) {
            world.setBlock(x, y + 1, z, this.plantBlock);
            --stack.size;
            return true;
        }
        return false;
    }
}

