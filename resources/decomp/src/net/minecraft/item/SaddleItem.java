/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.animal.PigEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SaddleItem
extends Item {
    public SaddleItem(int i) {
        super(i);
        this.maxStackSize = 1;
        this.maxDamage = 64;
    }

    public void interact(ItemStack stack, MobEntity entity) {
        if (entity instanceof PigEntity) {
            PigEntity pigEntity = (PigEntity)entity;
            if (!pigEntity.saddled) {
                pigEntity.saddled = true;
                --stack.size;
            }
        }
    }

    public void attack(ItemStack stack, MobEntity target) {
        this.interact(stack, target);
    }
}

