/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BlockItem
extends Item {
    private int block;

    public BlockItem(int i) {
        super(i);
        this.block = i + 256;
        this.setSprite(Block.BY_ID[i + 256].getSprite(2));
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        if (world.getBlock(x, y, z) == Block.SNOW_LAYER.id) {
            face = 0;
        } else {
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
        }
        if (stack.size == 0) {
            return false;
        }
        if (world.canPlace(this.block, x, y, z, false)) {
            Block block = Block.BY_ID[this.block];
            if (world.setBlock(x, y, z, this.block)) {
                Block.BY_ID[this.block].updateMetadataOnPlaced(world, x, y, z, face);
                Block.BY_ID[this.block].onPlaced(world, x, y, z, player);
                world.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, block.sounds.getStepping(), (block.sounds.getVolume() + 1.0f) / 2.0f, block.sounds.getPitch() * 0.8f);
                --stack.size;
            }
        }
        return true;
    }
}

