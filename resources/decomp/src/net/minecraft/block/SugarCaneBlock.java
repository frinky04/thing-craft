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
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class SugarCaneBlock
extends Block {
    protected SugarCaneBlock(int id, int sprite) {
        super(id, Material.PLANT);
        this.sprite = sprite;
        float f = 0.375f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, 1.0f, 0.5f + f);
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

    public boolean canBePlaced(World world, int x, int y, int z) {
        int i = world.getBlock(x, y - 1, z);
        if (i == this.id) {
            return true;
        }
        if (i != Block.GRASS.id && i != Block.DIRT.id) {
            return false;
        }
        if (world.getMaterial(x - 1, y - 1, z) == Material.WATER) {
            return true;
        }
        if (world.getMaterial(x + 1, y - 1, z) == Material.WATER) {
            return true;
        }
        if (world.getMaterial(x, y - 1, z - 1) == Material.WATER) {
            return true;
        }
        return world.getMaterial(x, y - 1, z + 1) == Material.WATER;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        this.tryBreak(world, x, y, z);
    }

    protected final void tryBreak(World world, int x, int y, int z) {
        if (!this.canSurvive(world, x, y, z)) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
    }

    public boolean canSurvive(World world, int x, int y, int z) {
        return this.canBePlaced(world, x, y, z);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    public int getDropItem(int metadata, Random random) {
        return Item.REEDS.id;
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 1;
    }
}

