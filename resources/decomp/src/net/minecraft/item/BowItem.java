/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BowItem
extends Item {
    public BowItem(int i) {
        super(i);
        this.maxStackSize = 1;
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        if (player.inventory.removeOne(Item.ARROW.id)) {
            world.playSound(player, "random.bow", 1.0f, 1.0f / (random.nextFloat() * 0.4f + 0.8f));
            if (!world.isMultiplayer) {
                world.addEntity(new ArrowEntity(world, player));
            }
        }
        return stack;
    }
}

