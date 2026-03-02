/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MinecartItem
extends Item {
    public int type;

    public MinecartItem(int id, int i) {
        super(id);
        this.maxStackSize = 1;
        this.type = i;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        int i = world.getBlock(x, y, z);
        if (i == Block.RAIL.id) {
            if (!world.isMultiplayer) {
                world.addEntity(new MinecartEntity(world, (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, this.type));
            }
            --stack.size;
            return true;
        }
        return false;
    }
}

