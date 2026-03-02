/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class FlyingMobEntity
extends MobEntity {
    public FlyingMobEntity(World world) {
        super(world);
    }

    protected void takeFallDamage(float distance) {
    }

    public void moveRelative(float sideways, float forwards) {
        if (this.checkWaterCollisions()) {
            this.updateVelocity(sideways, forwards, 0.02f);
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= (double)0.8f;
            this.velocityY *= (double)0.8f;
            this.velocityZ *= (double)0.8f;
        } else if (this.isInLava()) {
            this.updateVelocity(sideways, forwards, 0.02f);
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= 0.5;
            this.velocityY *= 0.5;
            this.velocityZ *= 0.5;
        } else {
            float f = 0.91f;
            if (this.onGround) {
                f = 0.54600006f;
                int i = this.world.getBlock(MathHelper.floor(this.x), MathHelper.floor(this.shape.minY) - 1, MathHelper.floor(this.z));
                if (i > 0) {
                    f = Block.BY_ID[i].slipperiness * 0.91f;
                }
            }
            float g = 0.16277136f / (f * f * f);
            this.updateVelocity(sideways, forwards, this.onGround ? 0.1f * g : 0.02f);
            f = 0.91f;
            if (this.onGround) {
                f = 0.54600006f;
                int j = this.world.getBlock(MathHelper.floor(this.x), MathHelper.floor(this.shape.minY) - 1, MathHelper.floor(this.z));
                if (j > 0) {
                    f = Block.BY_ID[j].slipperiness * 0.91f;
                }
            }
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= (double)f;
            this.velocityY *= (double)f;
            this.velocityZ *= (double)f;
        }
        this.lastWalkAnimationSpeed = this.walkAnimationSpeed;
        double d = this.x - this.lastX;
        double e = this.z - this.lastZ;
        float h = MathHelper.sqrt(d * d + e * e) * 4.0f;
        if (h > 1.0f) {
            h = 1.0f;
        }
        this.walkAnimationSpeed += (h - this.walkAnimationSpeed) * 0.4f;
        this.walkAnimationProgress += this.walkAnimationSpeed;
    }

    public boolean isClimbing() {
        return false;
    }
}

