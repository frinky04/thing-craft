/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class FlintAndSteelItem
extends Item {
    public FlintAndSteelItem(int i) {
        super(i);
        this.maxStackSize = 1;
        this.maxDamage = 64;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        int i;
        if (face == 0) {
            --y;
        }
        if (face == 1) {
            ++y;
        }
        if (face == 2) {
            --z;
        }
        if (face == 3) {
            ++z;
        }
        if (face == 4) {
            --x;
        }
        if (face == 5) {
            ++x;
        }
        if ((i = world.getBlock(x, y, z)) == 0) {
            world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "fire.ignite", 1.0f, random.nextFloat() * 0.4f + 0.8f);
            world.setBlock(x, y, z, Block.FIRE.id);
        }
        stack.takeDamageAndBreak(1);
        return true;
    }
}

