/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class FishingRodItem
extends Item {
    public FishingRodItem(int i) {
        super(i);
        this.maxDamage = 64;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isHandheld() {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean shouldRotate() {
        return true;
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        if (player.fishingBobber != null) {
            int i = player.fishingBobber.retrieve();
            stack.takeDamageAndBreak(i);
            player.swingArm();
        } else {
            world.playSound(player, "random.bow", 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
            if (!world.isMultiplayer) {
                world.addEntity(new FishingBobberEntity(world, player));
            }
            player.swingArm();
        }
        return stack;
    }
}

