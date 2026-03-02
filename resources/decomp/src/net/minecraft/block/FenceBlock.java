/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class FenceBlock
extends Block {
    public FenceBlock(int id, int sprite) {
        super(id, sprite, Material.WOOD);
    }

    public void addCollisions(World world, int x, int y, int z, Box shape, ArrayList collisions) {
        collisions.add(Box.fromPool(x, y, z, x + 1, (double)y + 1.5, z + 1));
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        if (world.getBlock(x, y - 1, z) == this.id) {
            return false;
        }
        if (!world.getMaterial(x, y - 1, z).isSolid()) {
            return false;
        }
        return super.canBePlaced(world, x, y, z);
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
        return 11;
    }
}

