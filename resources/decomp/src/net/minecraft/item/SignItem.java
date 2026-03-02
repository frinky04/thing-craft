/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class SignItem
extends Item {
    public SignItem(int i) {
        super(i);
        this.maxDamage = 64;
        this.maxStackSize = 1;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        if (face == 0) {
            return false;
        }
        if (!world.getMaterial(x, y, z).isSolid()) {
            return false;
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
        if (!Block.STANDING_SIGN.canBePlaced(world, x, y, z)) {
            return false;
        }
        if (face == 1) {
            world.setBlockWithMetadata(x, y, z, Block.STANDING_SIGN.id, MathHelper.floor((double)((player.yaw + 180.0f) * 16.0f / 360.0f) + 0.5) & 0xF);
        } else {
            world.setBlockWithMetadata(x, y, z, Block.WALL_SIGN.id, face);
        }
        --stack.size;
        SignBlockEntity signBlockEntity = (SignBlockEntity)world.getBlockEntity(x, y, z);
        if (signBlockEntity != null) {
            player.openSignEditor(signBlockEntity);
        }
        return true;
    }
}

