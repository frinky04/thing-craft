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
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class CactusBlock
extends Block {
    protected CactusBlock(int id, int sprite) {
        super(id, sprite, Material.CACTUS);
        this.setTicksRandomly(true);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (world.getBlock(x, y + 1, z) == 0) {
            int i = 1;
            while (world.getBlock(x, y - i, z) == this.id) {
                ++i;
            }
            if (i < 3) {
                int j = world.getBlockMetadata(x, y, z);
                if (j == 15) {
                    world.setBlock(x, y + 1, z, this.id);
                    world.setBlockMetadata(x, y, z, 0);
                } else {
                    world.setBlockMetadata(x, y, z, j + 1);
                }
            }
        }
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        float f = 0.0625f;
        return Box.fromPool((float)x + f, y, (float)z + f, (float)(x + 1) - f, (float)(y + 1) - f, (float)(z + 1) - f);
    }

    @Environment(value=EnvType.CLIENT)
    public Box getOutlineShape(World world, int x, int y, int z) {
        float f = 0.0625f;
        return Box.fromPool((float)x + f, y, (float)z + f, (float)(x + 1) - f, y + 1, (float)(z + 1) - f);
    }

    public int getSprite(int face) {
        if (face == 1) {
            return this.sprite - 1;
        }
        if (face == 0) {
            return this.sprite + 1;
        }
        return this.sprite;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 13;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        if (!super.canBePlaced(world, x, y, z)) {
            return false;
        }
        return this.canSurvive(world, x, y, z);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (!this.canSurvive(world, x, y, z)) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
    }

    public boolean canSurvive(World world, int x, int y, int z) {
        if (world.getMaterial(x - 1, y, z).isSolid()) {
            return false;
        }
        if (world.getMaterial(x + 1, y, z).isSolid()) {
            return false;
        }
        if (world.getMaterial(x, y, z - 1).isSolid()) {
            return false;
        }
        if (world.getMaterial(x, y, z + 1).isSolid()) {
            return false;
        }
        int i = world.getBlock(x, y - 1, z);
        return i == Block.CACTUS.id || i == Block.SAND.id;
    }

    public void onEntityCollision(World world, int x, int y, int z, Entity entity) {
        entity.takeDamage(null, 1);
    }
}

