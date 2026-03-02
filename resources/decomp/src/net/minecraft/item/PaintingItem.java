/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.decoration.PaintingEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PaintingItem
extends Item {
    public PaintingItem(int i) {
        super(i);
        this.maxDamage = 64;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        PaintingEntity paintingEntity;
        if (face == 0) {
            return false;
        }
        if (face == 1) {
            return false;
        }
        int i = 0;
        if (face == 4) {
            i = 1;
        }
        if (face == 3) {
            i = 2;
        }
        if (face == 5) {
            i = 3;
        }
        if ((paintingEntity = new PaintingEntity(world, x, y, z, i)).canSurvive()) {
            world.addEntity(paintingEntity);
            --stack.size;
        }
        return true;
    }
}

