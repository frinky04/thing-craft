/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class FoodItem
extends Item {
    private int hungerPoints;

    public FoodItem(int item, int hungerPoints) {
        super(item);
        this.hungerPoints = hungerPoints;
        this.maxStackSize = 1;
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        --stack.size;
        player.heal(this.hungerPoints);
        return stack;
    }
}

