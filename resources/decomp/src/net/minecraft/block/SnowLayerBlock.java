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
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class SnowLayerBlock
extends Block {
    protected SnowLayerBlock(int id, int sprite) {
        super(id, sprite, Material.SNOW_LAYER);
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.125f, 1.0f);
        this.setTicksRandomly(true);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        int i = world.getBlock(x, y - 1, z);
        if (i == 0 || !Block.BY_ID[i].isSolid()) {
            return false;
        }
        return world.getMaterial(x, y - 1, z).blocksMovement();
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        this.canBePlacedOrBreak(world, x, y, z);
    }

    private boolean canBePlacedOrBreak(World world, int x, int y, int z) {
        if (!this.canBePlaced(world, x, y, z)) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
            return false;
        }
        return true;
    }

    public void afterMinedByPlayer(World world, int x, int y, int z, int metadata) {
        int i = Item.SNOWBALL.id;
        float f = 0.7f;
        double d = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
        double e = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
        double g = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
        ItemEntity itemEntity = new ItemEntity(world, (double)x + d, (double)y + e, (double)z + g, new ItemStack(i));
        itemEntity.pickUpDelay = 10;
        world.addEntity(itemEntity);
        world.setBlock(x, y, z, 0);
    }

    public int getDropItem(int metadata, Random random) {
        return Item.SNOWBALL.id;
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (world.getLight(LightType.BLOCK, x, y, z) > 11) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        Material material = world.getMaterial(x, y, z);
        if (face == 1) {
            return true;
        }
        if (material == this.material) {
            return false;
        }
        return super.shouldRenderFace(world, x, y, z, face);
    }
}

