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
import net.minecraft.client.world.color.GrassColors;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class GrassBlock
extends Block {
    protected GrassBlock(int id) {
        super(id, Material.GRASS);
        this.sprite = 3;
        this.setTicksRandomly(true);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(WorldView world, int x, int y, int z, int face) {
        if (face == 1) {
            return 0;
        }
        if (face == 0) {
            return 2;
        }
        Material material = world.getMaterial(x, y + 1, z);
        if (material == Material.SNOW_LAYER || material == Material.SNOW) {
            return 68;
        }
        return 3;
    }

    @Environment(value=EnvType.CLIENT)
    public int getColor(WorldView world, int x, int y, int z) {
        world.getBiomeSource().getBiomes(x, z, 1, 1);
        double d = world.getBiomeSource().temperatures[0];
        double e = world.getBiomeSource().downfalls[0];
        return GrassColors.getColor(d, e);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        int k;
        int j;
        int i;
        if (world.getRawBrightness(x, y + 1, z) < 4 && world.getMaterial(x, y + 1, z).isOpaque()) {
            if (random.nextInt(4) != 0) {
                return;
            }
            world.setBlock(x, y, z, Block.DIRT.id);
        } else if (world.getRawBrightness(x, y + 1, z) >= 9 && world.getBlock(i = x + random.nextInt(3) - 1, j = y + random.nextInt(5) - 3, k = z + random.nextInt(3) - 1) == Block.DIRT.id && world.getRawBrightness(i, j + 1, k) >= 4 && !world.getMaterial(i, j + 1, k).isOpaque()) {
            world.setBlock(i, j, k, Block.GRASS.id);
        }
    }

    public int getDropItem(int metadata, Random random) {
        return Block.DIRT.getDropItem(0, random);
    }
}

