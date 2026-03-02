/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.world.World;

public class FallingBlock
extends Block {
    public static boolean fallImmediately = false;

    public FallingBlock(int id, int sprite) {
        super(id, sprite, Material.SAND);
    }

    public void onAdded(World world, int x, int y, int z) {
        world.scheduleTick(x, y, z, this.id);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        world.scheduleTick(x, y, z, this.id);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        this.tryFall(world, x, y, z);
    }

    private void tryFall(World world, int x, int y, int z) {
        int i = x;
        int j = y;
        int k = z;
        if (FallingBlock.canFallThrough(world, i, j - 1, k) && j >= 0) {
            FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(world, (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, this.id);
            if (fallImmediately) {
                while (!fallingBlockEntity.removed) {
                    fallingBlockEntity.tick();
                }
            } else {
                world.addEntity(fallingBlockEntity);
            }
        }
    }

    public int getTickRate() {
        return 3;
    }

    public static boolean canFallThrough(World world, int x, int y, int z) {
        int i = world.getBlock(x, y, z);
        if (i == 0) {
            return true;
        }
        if (i == Block.FIRE.id) {
            return true;
        }
        Material material = Block.BY_ID[i].material;
        if (material == Material.WATER) {
            return true;
        }
        return material == Material.LAVA;
    }
}

