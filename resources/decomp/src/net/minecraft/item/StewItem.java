/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class StewItem
extends FoodItem {
    public StewItem(int id, int hungerPoints) {
        super(id, hungerPoints);
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        super.startUsing(stack, world, player);
        return new ItemStack(Item.BOWL);
    }
}

