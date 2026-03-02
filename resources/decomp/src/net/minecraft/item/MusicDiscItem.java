/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MusicDiscItem
extends Item {
    private String record;

    protected MusicDiscItem(int id, String record) {
        super(id);
        this.record = record;
        this.maxStackSize = 1;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        if (world.getBlock(x, y, z) == Block.JUKEBOX.id && world.getBlockMetadata(x, y, z) == 0) {
            world.setBlockMetadata(x, y, z, this.id - Item.RECORD_13.id + 1);
            world.playRecordMusic(this.record, x, y, z);
            --stack.size;
            return true;
        }
        return false;
    }
}

