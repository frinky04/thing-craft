/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class StoneSlabBlock
extends Block {
    private boolean doubleSlab;

    public StoneSlabBlock(int id, boolean doubleSlab) {
        super(id, 6, Material.STONE);
        this.doubleSlab = doubleSlab;
        if (!doubleSlab) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f);
        }
        this.setOpacity(255);
    }

    public int getSprite(int face) {
        if (face <= 1) {
            return 6;
        }
        return 5;
    }

    public boolean isSolid() {
        return this.doubleSlab;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (this != Block.STONE_SLAB) {
            return;
        }
    }

    public void onAdded(World world, int x, int y, int z) {
        int i;
        if (this != Block.STONE_SLAB) {
            super.onAdded(world, x, y, z);
        }
        if ((i = world.getBlock(x, y - 1, z)) == StoneSlabBlock.STONE_SLAB.id) {
            world.setBlock(x, y, z, 0);
            world.setBlock(x, y - 1, z, Block.DOUBLE_STONE_SLAB.id);
        }
    }

    public int getDropItem(int metadata, Random random) {
        return Block.STONE_SLAB.id;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return this.doubleSlab;
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        if (this != Block.STONE_SLAB) {
            super.shouldRenderFace(world, x, y, z, face);
        }
        if (face == 1) {
            return true;
        }
        if (!super.shouldRenderFace(world, x, y, z, face)) {
            return false;
        }
        if (face == 0) {
            return true;
        }
        return world.getBlock(x, y, z) != this.id;
    }
}

