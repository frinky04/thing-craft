/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public abstract class BlockWithBlockEntity
extends Block {
    protected BlockWithBlockEntity(int i, Material material) {
        super(i, material);
        BlockWithBlockEntity.HAS_BLOCK_ENTITY[i] = true;
    }

    protected BlockWithBlockEntity(int i, int j, Material material) {
        super(i, j, material);
    }

    public void onAdded(World world, int x, int y, int z) {
        super.onAdded(world, x, y, z);
        world.setBlockEntity(x, y, z, this.createBlockEntity());
    }

    public void onRemoved(World world, int x, int y, int z) {
        super.onRemoved(world, x, y, z);
        world.removeBlockEntity(x, y, z);
    }

    protected abstract BlockEntity createBlockEntity();
}

