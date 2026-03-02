/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class SnowballItem
extends Item {
    public SnowballItem(int i) {
        super(i);
        this.maxStackSize = 16;
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        --stack.size;
        world.playSound(player, "random.bow", 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
        if (!world.isMultiplayer) {
            world.addEntity(new SnowballEntity(world, player));
        }
        return stack;
    }
}

