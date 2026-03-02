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
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SwordItem
extends Item {
    private int attackDamage;

    public SwordItem(int id, int tier) {
        super(id);
        this.maxStackSize = 1;
        this.maxDamage = 32 << tier;
        if (tier == 3) {
            this.maxDamage *= 4;
        }
        this.attackDamage = 4 + tier * 2;
    }

    public float getMiningSpeed(ItemStack stack, Block block) {
        return 1.5f;
    }

    public void attack(ItemStack stack, MobEntity target) {
        stack.takeDamageAndBreak(1);
    }

    public void mineBlock(ItemStack stack, int x, int y, int z, int face) {
        stack.takeDamageAndBreak(2);
    }

    public int getAttackDamage(Entity target) {
        return this.attackDamage;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isHandheld() {
        return true;
    }
}

