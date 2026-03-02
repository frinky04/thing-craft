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
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class WheatBlock
extends PlantBlock {
    protected WheatBlock(int i, int j) {
        super(i, j);
        this.sprite = j;
        this.setTicksRandomly(true);
        float f = 0.5f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, 0.25f, 0.5f + f);
    }

    protected boolean canBePlacedOn(int block) {
        return block == Block.FARMLAND.id;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        float f;
        int i;
        super.tick(world, x, y, z, random);
        if (world.getRawBrightness(x, y + 1, z) >= 9 && (i = world.getBlockMetadata(x, y, z)) < 7 && random.nextInt((int)(100.0f / (f = this.getGrowthSpeed(world, x, y, z)))) == 0) {
            world.setBlockMetadata(x, y, z, ++i);
        }
    }

    private float getGrowthSpeed(World world, int x, int y, int z) {
        float f = 1.0f;
        int i = world.getBlock(x, y, z - 1);
        int j = world.getBlock(x, y, z + 1);
        int k = world.getBlock(x - 1, y, z);
        int l = world.getBlock(x + 1, y, z);
        int m = world.getBlock(x - 1, y, z - 1);
        int n = world.getBlock(x + 1, y, z - 1);
        int o = world.getBlock(x + 1, y, z + 1);
        int p = world.getBlock(x - 1, y, z + 1);
        boolean q = k == this.id || l == this.id;
        boolean r = i == this.id || j == this.id;
        boolean s = m == this.id || n == this.id || o == this.id || p == this.id;
        for (int t = x - 1; t <= x + 1; ++t) {
            for (int u = z - 1; u <= z + 1; ++u) {
                int v = world.getBlock(t, y - 1, u);
                float g = 0.0f;
                if (v == Block.FARMLAND.id) {
                    g = 1.0f;
                    if (world.getBlockMetadata(t, y - 1, u) > 0) {
                        g = 3.0f;
                    }
                }
                if (t != x || u != z) {
                    g /= 4.0f;
                }
                f += g;
            }
        }
        if (s || q && r) {
            f /= 2.0f;
        }
        return f;
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        if (metadata < 0) {
            metadata = 7;
        }
        return this.sprite + metadata;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 6;
    }

    public void onBroken(World world, int x, int y, int z, int metadata) {
        super.onBroken(world, x, y, z, metadata);
        if (!world.isMultiplayer) {
            for (int i = 0; i < 3; ++i) {
                if (world.random.nextInt(15) > metadata) continue;
                float f = 0.7f;
                float g = world.random.nextFloat() * f + (1.0f - f) * 0.5f;
                float h = world.random.nextFloat() * f + (1.0f - f) * 0.5f;
                float j = world.random.nextFloat() * f + (1.0f - f) * 0.5f;
                ItemEntity itemEntity = new ItemEntity(world, (float)x + g, (float)y + h, (float)z + j, new ItemStack(Item.WHEAT_SEEDS));
                itemEntity.pickUpDelay = 10;
                world.addEntity(itemEntity);
            }
        }
    }

    public int getDropItem(int metadata, Random random) {
        if (metadata == 7) {
            return Item.WHEAT.id;
        }
        return -1;
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }
}

