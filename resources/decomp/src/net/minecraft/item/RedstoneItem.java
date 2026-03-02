/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class RedstoneItem
extends Item {
    public RedstoneItem(int i) {
        super(i);
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        if (face == 0) {
            --y;
        }
        if (face == 1) {
            ++y;
        }
        if (face == 2) {
            --z;
        }
        if (face == 3) {
            ++z;
        }
        if (face == 4) {
            --x;
        }
        if (face == 5) {
            ++x;
        }
        if (world.getBlock(x, y, z) != 0) {
            return false;
        }
        if (Block.REDSTONE_WIRE.canBePlaced(world, x, y, z)) {
            --stack.size;
            world.setBlock(x, y, z, Block.REDSTONE_WIRE.id);
        }
        return true;
    }
}

