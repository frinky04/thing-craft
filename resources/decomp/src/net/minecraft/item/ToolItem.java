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

public class ToolItem
extends Item {
    private Block[] effectiveBlocks;
    private float miningSpeed = 4.0f;
    private int attackDamage;
    protected int tier;

    public ToolItem(int id, int attackDamage, int tier, Block[] effectiveBlocks) {
        super(id);
        this.tier = tier;
        this.effectiveBlocks = effectiveBlocks;
        this.maxStackSize = 1;
        this.maxDamage = 32 << tier;
        if (tier == 3) {
            this.maxDamage *= 4;
        }
        this.miningSpeed = (tier + 1) * 2;
        this.attackDamage = attackDamage + tier;
    }

    public float getMiningSpeed(ItemStack stack, Block block) {
        for (int i = 0; i < this.effectiveBlocks.length; ++i) {
            if (this.effectiveBlocks[i] != block) continue;
            return this.miningSpeed;
        }
        return 1.0f;
    }

    public void attack(ItemStack stack, MobEntity target) {
        stack.takeDamageAndBreak(2);
    }

    public void mineBlock(ItemStack stack, int x, int y, int z, int face) {
        stack.takeDamageAndBreak(1);
    }

    public int getAttackDamage(Entity target) {
        return this.attackDamage;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isHandheld() {
        return true;
    }
}

