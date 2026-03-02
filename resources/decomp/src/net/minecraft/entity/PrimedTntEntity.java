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

public class PrimedTntEntity
extends Entity {
    public int fuseTimer = 0;

    public PrimedTntEntity(World world) {
        super(world);
        this.blocksBuilding = true;
        this.setSize(0.98f, 0.98f);
        this.eyeHeight = this.height / 2.0f;
    }

    public PrimedTntEntity(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y, z);
        float f = (float)(Math.random() * 3.1415927410125732 * 2.0);
        this.velocityX = -MathHelper.sin(f * (float)Math.PI / 180.0f) * 0.02f;
        this.velocityY = 0.2f;
        this.velocityZ = -MathHelper.cos(f * (float)Math.PI / 180.0f) * 0.02f;
        this.makesSteps = false;
        this.fuseTimer = 80;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    public boolean hasCollision() {
        return !this.removed;
    }

    public void tick() {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.velocityY -= (double)0.04f;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= (double)0.98f;
        this.velocityY *= (double)0.98f;
        this.velocityZ *= (double)0.98f;
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
            this.velocityY *= -0.5;
        }
        if (this.fuseTimer-- <= 0) {
            this.remove();
            this.explode();
        } else {
            this.world.addParticle("smoke", this.x, this.y + 0.5, this.z, 0.0, 0.0, 0.0);
        }
    }

    private void explode() {
        float f = 4.0f;
        this.world.explode(null, this.x, this.y, this.z, f);
    }

    protected void writeCustomNbt(NbtCompound nbt) {
        nbt.putByte("Fuse", (byte)this.fuseTimer);
    }

    protected void readCustomNbt(NbtCompound nbt) {
        this.fuseTimer = nbt.getByte("Fuse");
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }
}

