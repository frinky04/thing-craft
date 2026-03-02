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
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class LadderBlock
extends Block {
    protected LadderBlock(int id, int sprite) {
        super(id, sprite, Material.DECORATION);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        float f = 0.125f;
        if (i == 2) {
            this.setShape(0.0f, 0.0f, 1.0f - f, 1.0f, 1.0f, 1.0f);
        }
        if (i == 3) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, f);
        }
        if (i == 4) {
            this.setShape(1.0f - f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        }
        if (i == 5) {
            this.setShape(0.0f, 0.0f, 0.0f, f, 1.0f, 1.0f);
        }
        return super.getCollisionShape(world, x, y, z);
    }

    @Environment(value=EnvType.CLIENT)
    public Box getOutlineShape(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        float f = 0.125f;
        if (i == 2) {
            this.setShape(0.0f, 0.0f, 1.0f - f, 1.0f, 1.0f, 1.0f);
        }
        if (i == 3) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, f);
        }
        if (i == 4) {
            this.setShape(1.0f - f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        }
        if (i == 5) {
            this.setShape(0.0f, 0.0f, 0.0f, f, 1.0f, 1.0f);
        }
        return super.getOutlineShape(world, x, y, z);
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
        return 8;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        if (world.isSolidBlock(x - 1, y, z)) {
            return true;
        }
        if (world.isSolidBlock(x + 1, y, z)) {
            return true;
        }
        if (world.isSolidBlock(x, y, z - 1)) {
            return true;
        }
        return world.isSolidBlock(x, y, z + 1);
    }

    public void updateMetadataOnPlaced(World world, int x, int y, int z, int face) {
        int i = world.getBlockMetadata(x, y, z);
        if ((i == 0 || face == 2) && world.isSolidBlock(x, y, z + 1)) {
            i = 2;
        }
        if ((i == 0 || face == 3) && world.isSolidBlock(x, y, z - 1)) {
            i = 3;
        }
        if ((i == 0 || face == 4) && world.isSolidBlock(x + 1, y, z)) {
            i = 4;
        }
        if ((i == 0 || face == 5) && world.isSolidBlock(x - 1, y, z)) {
            i = 5;
        }
        world.setBlockMetadata(x, y, z, i);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        int i = world.getBlockMetadata(x, y, z);
        boolean j = false;
        if (i == 2 && world.isSolidBlock(x, y, z + 1)) {
            j = true;
        }
        if (i == 3 && world.isSolidBlock(x, y, z - 1)) {
            j = true;
        }
        if (i == 4 && world.isSolidBlock(x + 1, y, z)) {
            j = true;
        }
        if (i == 5 && world.isSolidBlock(x - 1, y, z)) {
            j = true;
        }
        if (!j) {
            this.dropItems(world, x, y, z, i);
            world.setBlock(x, y, z, 0);
        }
        super.neighborChanged(world, x, y, z, neighborBlock);
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }
}

