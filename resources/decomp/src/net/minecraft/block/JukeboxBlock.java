/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class JukeboxBlock
extends Block {
    protected JukeboxBlock(int id, int sprite) {
        super(id, sprite, Material.WOOD);
    }

    public int getSprite(int face) {
        return this.sprite + (face == 1 ? 1 : 0);
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        int i = world.getBlockMetadata(x, y, z);
        if (i > 0) {
            this.removeRecord(world, x, y, z, i);
            return true;
        }
        return false;
    }

    public void removeRecord(World world, int x, int y, int z, int metadata) {
        world.playRecordMusic(null, x, y, z);
        world.setBlockMetadata(x, y, z, 0);
        int i = Item.RECORD_13.id + metadata - 1;
        float f = 0.7f;
        double d = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
        double e = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.2 + 0.6;
        double g = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
        ItemEntity itemEntity = new ItemEntity(world, (double)x + d, (double)y + e, (double)z + g, new ItemStack(i));
        itemEntity.pickUpDelay = 10;
        world.addEntity(itemEntity);
    }

    public void dropItems(World world, int x, int y, int z, int metadata, float luck) {
        if (world.isMultiplayer) {
            return;
        }
        if (metadata > 0) {
            this.removeRecord(world, x, y, z, metadata);
        }
        super.dropItems(world, x, y, z, metadata, luck);
    }
}

