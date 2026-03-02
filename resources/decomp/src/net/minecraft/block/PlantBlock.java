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

public class PlantBlock
extends Block {
    protected PlantBlock(int id, int sprite) {
        super(id, Material.PLANT);
        this.sprite = sprite;
        this.setTicksRandomly(true);
        float f = 0.2f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, f * 3.0f, 0.5f + f);
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        return this.canBePlacedOn(world.getBlock(x, y - 1, z));
    }

    protected boolean canBePlacedOn(int block) {
        return block == Block.GRASS.id || block == Block.DIRT.id || block == Block.FARMLAND.id;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        super.neighborChanged(world, x, y, z, neighborBlock);
        this.canSurviveOrBreak(world, x, y, z);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        this.canSurviveOrBreak(world, x, y, z);
    }

    protected final void canSurviveOrBreak(World world, int x, int y, int z) {
        if (!this.canSurvive(world, x, y, z)) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
    }

    public boolean canSurvive(World world, int x, int y, int z) {
        return (world.getRawBrightness(x, y, z) >= 8 || world.hasSkyAccess(x, y, z)) && this.canBePlacedOn(world.getBlock(x, y - 1, z));
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

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 1;
    }
}

