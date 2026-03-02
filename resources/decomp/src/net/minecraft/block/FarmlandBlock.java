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

public class FarmlandBlock
extends Block {
    protected FarmlandBlock(int id) {
        super(id, Material.GRASS);
        this.sprite = 87;
        this.setTicksRandomly(true);
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.9375f, 1.0f);
        this.setOpacity(255);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return Box.fromPool(x + 0, y + 0, z + 0, x + 1, y + 1, z + 1);
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        if (face == 1 && metadata > 0) {
            return this.sprite - 1;
        }
        if (face == 1) {
            return this.sprite;
        }
        return 2;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(5) == 0) {
            if (this.isNearWater(world, x, y, z)) {
                world.setBlockMetadata(x, y, z, 7);
            } else {
                int i = world.getBlockMetadata(x, y, z);
                if (i > 0) {
                    world.setBlockMetadata(x, y, z, i - 1);
                } else if (!this.isUnderCrop(world, x, y, z)) {
                    world.setBlock(x, y, z, Block.DIRT.id);
                }
            }
        }
    }

    public void onSteppedOn(World world, int x, int y, int z, Entity entity) {
        if (world.random.nextInt(4) == 0) {
            world.setBlock(x, y, z, Block.DIRT.id);
        }
    }

    private boolean isUnderCrop(World world, int x, int y, int z) {
        int i = 0;
        for (int j = x - i; j <= x + i; ++j) {
            for (int k = z - i; k <= z + i; ++k) {
                if (world.getBlock(j, y + 1, k) != Block.WHEAT.id) continue;
                return true;
            }
        }
        return false;
    }

    private boolean isNearWater(World world, int x, int y, int z) {
        for (int i = x - 4; i <= x + 4; ++i) {
            for (int j = y; j <= y + 1; ++j) {
                for (int k = z - 4; k <= z + 4; ++k) {
                    if (world.getMaterial(i, j, k) != Material.WATER) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        super.neighborChanged(world, x, y, z, neighborBlock);
        Material material = world.getMaterial(x, y + 1, z);
        if (material.isSolid()) {
            world.setBlock(x, y, z, Block.DIRT.id);
        }
    }

    public int getDropItem(int metadata, Random random) {
        return Block.DIRT.getDropItem(0, random);
    }
}

