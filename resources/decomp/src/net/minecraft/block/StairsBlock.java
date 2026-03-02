/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.ArrayList;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class StairsBlock
extends Block {
    private Block baseBlock;

    protected StairsBlock(int id, Block baseBlock) {
        super(id, baseBlock.sprite, baseBlock.material);
        this.baseBlock = baseBlock;
        this.setStrength(baseBlock.miningTime);
        this.setBlastResistance(baseBlock.blastResistance / 3.0f);
        this.setSounds(baseBlock.sounds);
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return super.getCollisionShape(world, x, y, z);
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
        return 10;
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        return super.shouldRenderFace(world, x, y, z, face);
    }

    public void addCollisions(World world, int x, int y, int z, Box shape, ArrayList collisions) {
        int i = world.getBlockMetadata(x, y, z);
        if (i == 0) {
            this.setShape(0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 1.0f);
            super.addCollisions(world, x, y, z, shape, collisions);
            this.setShape(0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
            super.addCollisions(world, x, y, z, shape, collisions);
        } else if (i == 1) {
            this.setShape(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f);
            super.addCollisions(world, x, y, z, shape, collisions);
            this.setShape(0.5f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f);
            super.addCollisions(world, x, y, z, shape, collisions);
        } else if (i == 2) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 0.5f);
            super.addCollisions(world, x, y, z, shape, collisions);
            this.setShape(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f);
            super.addCollisions(world, x, y, z, shape, collisions);
        } else if (i == 3) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f);
            super.addCollisions(world, x, y, z, shape, collisions);
            this.setShape(0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 1.0f);
            super.addCollisions(world, x, y, z, shape, collisions);
        }
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        this.baseBlock.randomDisplayTick(world, x, y, z, random);
    }

    public void startMining(World world, int x, int y, int z, PlayerEntity player) {
        this.baseBlock.startMining(world, x, y, z, player);
    }

    public void onBroken(World world, int x, int y, int z, int metadata) {
        this.baseBlock.onBroken(world, x, y, z, metadata);
    }

    @Environment(value=EnvType.CLIENT)
    public float getBrightness(WorldView world, int x, int y, int z) {
        return this.baseBlock.getBrightness(world, x, y, z);
    }

    public float getBlastResistance(Entity entity) {
        return this.baseBlock.getBlastResistance(entity);
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderLayer() {
        return this.baseBlock.getRenderLayer();
    }

    public int getDropItem(int metadata, Random random) {
        return this.baseBlock.getDropItem(metadata, random);
    }

    public int getBaseDropCount(Random random) {
        return this.baseBlock.getBaseDropCount(random);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        return this.baseBlock.getSprite(face, metadata);
    }

    public int getSprite(int face) {
        return this.baseBlock.getSprite(face);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(WorldView world, int x, int y, int z, int face) {
        return this.baseBlock.getSprite(world, x, y, z, face);
    }

    public int getTickRate() {
        return this.baseBlock.getTickRate();
    }

    @Environment(value=EnvType.CLIENT)
    public Box getOutlineShape(World world, int x, int y, int z) {
        return this.baseBlock.getOutlineShape(world, x, y, z);
    }

    public void applyMaterialDrag(World world, int x, int y, int z, Entity entity, Vec3d velocity) {
        this.baseBlock.applyMaterialDrag(world, x, y, z, entity, velocity);
    }

    public boolean canRayTrace() {
        return this.baseBlock.canRayTrace();
    }

    public boolean canRayTrace(int metadata, boolean allowLiquids) {
        return this.baseBlock.canRayTrace(metadata, allowLiquids);
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        return this.baseBlock.canBePlaced(world, x, y, z);
    }

    public void onAdded(World world, int x, int y, int z) {
        this.neighborChanged(world, x, y, z, 0);
        this.baseBlock.onAdded(world, x, y, z);
    }

    public void onRemoved(World world, int x, int y, int z) {
        this.baseBlock.onRemoved(world, x, y, z);
    }

    public void dropItems(World world, int x, int y, int z, int metadata, float luck) {
        this.baseBlock.dropItems(world, x, y, z, metadata, luck);
    }

    public void dropItems(World world, int x, int y, int z, int metadata) {
        this.baseBlock.dropItems(world, x, y, z, metadata);
    }

    public void onSteppedOn(World world, int x, int y, int z, Entity entity) {
        this.baseBlock.onSteppedOn(world, x, y, z, entity);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        this.baseBlock.tick(world, x, y, z, random);
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        return this.baseBlock.use(world, x, y, z, player);
    }

    public void onExploded(World world, int x, int y, int z) {
        this.baseBlock.onExploded(world, x, y, z);
    }

    public void onPlaced(World world, int x, int y, int z, MobEntity entity) {
        int i = MathHelper.floor((double)(entity.yaw * 4.0f / 360.0f) + 0.5) & 3;
        if (i == 0) {
            world.setBlockMetadata(x, y, z, 2);
        }
        if (i == 1) {
            world.setBlockMetadata(x, y, z, 1);
        }
        if (i == 2) {
            world.setBlockMetadata(x, y, z, 3);
        }
        if (i == 3) {
            world.setBlockMetadata(x, y, z, 0);
        }
    }
}

