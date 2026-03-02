/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class FallingBlockEntity
extends Entity {
    public int block;
    public int fallingTicks = 0;

    public FallingBlockEntity(World world) {
        super(world);
    }

    public FallingBlockEntity(World world, float x, float y, float z, int block) {
        super(world);
        this.block = block;
        this.blocksBuilding = true;
        this.setSize(0.98f, 0.98f);
        this.eyeHeight = this.height / 2.0f;
        this.setPosition(x, y, z);
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
        this.makesSteps = false;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    public boolean hasCollision() {
        return !this.removed;
    }

    public void tick() {
        if (this.block == 0) {
            this.remove();
            return;
        }
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        ++this.fallingTicks;
        this.velocityY -= (double)0.04f;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= (double)0.98f;
        this.velocityY *= (double)0.98f;
        this.velocityZ *= (double)0.98f;
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(this.y);
        int k = MathHelper.floor(this.z);
        if (this.world.getBlock(i, j, k) == this.block) {
            this.world.setBlock(i, j, k, 0);
        }
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
            this.velocityY *= -0.5;
            this.remove();
            if (!this.world.canPlace(this.block, i, j, k, true) || !this.world.setBlock(i, j, k, this.block)) {
                this.dropItem(this.block, 1);
            }
        } else if (this.fallingTicks > 100) {
            this.dropItem(this.block, 1);
            this.remove();
        }
    }

    protected void writeCustomNbt(NbtCompound nbt) {
        nbt.putByte("Tile", (byte)this.block);
    }

    protected void readCustomNbt(NbtCompound nbt) {
        this.block = nbt.getByte("Tile") & 0xFF;
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }

    @Environment(value=EnvType.CLIENT)
    public World getWorld() {
        return this.world;
    }
}

