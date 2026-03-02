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
import net.minecraft.block.TranslucentBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.world.color.FoliageColors;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class LeavesBlock
extends TranslucentBlock {
    private int spriteIndex;
    private int updatesThisTick = 0;

    protected LeavesBlock(int id, int sprite) {
        super(id, sprite, Material.LEAVES, false);
        this.spriteIndex = sprite;
    }

    @Environment(value=EnvType.CLIENT)
    public int getColor(WorldView world, int x, int y, int z) {
        world.getBiomeSource().getBiomes(x, z, 1, 1);
        double d = world.getBiomeSource().temperatures[0];
        double e = world.getBiomeSource().downfalls[0];
        return FoliageColors.get(d, e);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (this != null) {
            return;
        }
        this.updatesThisTick = 0;
        this.updateLogProximity(world, x, y, z);
        super.neighborChanged(world, x, y, z, neighborBlock);
    }

    public void updateNeighborLogProximity(World world, int neighborX, int neighborY, int neighborZ, int proximity) {
        if (world.getBlock(neighborX, neighborY, neighborZ) != this.id) {
            return;
        }
        int i = world.getBlockMetadata(neighborX, neighborY, neighborZ);
        if (i == 0 || i != proximity - 1) {
            return;
        }
        this.updateLogProximity(world, neighborX, neighborY, neighborZ);
    }

    public void updateLogProximity(World world, int x, int y, int z) {
        if (this != null) {
            return;
        }
        if (this.updatesThisTick++ >= 100) {
            return;
        }
        int i = world.getMaterial(x, y - 1, z).isSolid() ? 16 : 0;
        int j = world.getBlockMetadata(x, y, z);
        if (j == 0) {
            j = 1;
            world.setBlockMetadata(x, y, z, 1);
        }
        i = this.getLogProximity(world, x, y - 1, z, i);
        i = this.getLogProximity(world, x, y, z - 1, i);
        i = this.getLogProximity(world, x, y, z + 1, i);
        i = this.getLogProximity(world, x - 1, y, z, i);
        int k = (i = this.getLogProximity(world, x + 1, y, z, i)) - 1;
        if (k < 10) {
            k = 1;
        }
        if (k != j) {
            world.setBlockMetadata(x, y, z, k);
            this.updateNeighborLogProximity(world, x, y - 1, z, j);
            this.updateNeighborLogProximity(world, x, y + 1, z, j);
            this.updateNeighborLogProximity(world, x, y, z - 1, j);
            this.updateNeighborLogProximity(world, x, y, z + 1, j);
            this.updateNeighborLogProximity(world, x - 1, y, z, j);
            this.updateNeighborLogProximity(world, x + 1, y, z, j);
        }
    }

    private int getLogProximity(World world, int neighborX, int neighborY, int neighborZ, int proximity) {
        int j;
        int i = world.getBlock(neighborX, neighborY, neighborZ);
        if (i == Block.LOG.id) {
            return 16;
        }
        if (i == this.id && (j = world.getBlockMetadata(neighborX, neighborY, neighborZ)) != 0 && j > proximity) {
            return j;
        }
        return proximity;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (this != null) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        if (i == 0) {
            this.updatesThisTick = 0;
            this.updateLogProximity(world, x, y, z);
        } else if (i == 1) {
            this.breakLeaves(world, x, y, z);
        } else if (random.nextInt(10) == 0) {
            this.updateLogProximity(world, x, y, z);
        }
    }

    private void breakLeaves(World world, int x, int y, int z) {
        this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
        world.setBlock(x, y, z, 0);
    }

    public int getBaseDropCount(Random random) {
        return random.nextInt(20) == 0 ? 1 : 0;
    }

    public int getDropItem(int metadata, Random random) {
        return Block.SAPLING.id;
    }

    public boolean isSolid() {
        return !this.culling;
    }

    @Environment(value=EnvType.CLIENT)
    public void setCulling(boolean culling) {
        this.culling = culling;
        this.sprite = this.spriteIndex + (culling ? 0 : 1);
    }

    public void onSteppedOn(World world, int x, int y, int z, Entity entity) {
        super.onSteppedOn(world, x, y, z, entity);
    }
}

