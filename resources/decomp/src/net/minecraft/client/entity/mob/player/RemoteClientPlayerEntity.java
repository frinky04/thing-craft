/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity.mob.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class RemoteClientPlayerEntity
extends PlayerEntity {
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYaw;
    private double lerpPitch;
    float f_58709213 = 0.0f;

    public RemoteClientPlayerEntity(World world, String name) {
        super(world);
        this.name = name;
        this.eyeHeight = 0.0f;
        this.stepHeight = 0.0f;
        if (name != null && name.length() > 0) {
            this.skin = "http://www.minecraft.net/skin/" + name + ".png";
            System.out.println("Loading texture " + this.skin);
        }
        this.noClip = true;
        this.viewDistanceScaling = 10.0;
    }

    public boolean takeDamage(Entity source, int amount) {
        return true;
    }

    public void lerpPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        this.eyeHeight = 0.0f;
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = steps;
    }

    public void tick() {
        super.tick();
        this.lastWalkAnimationSpeed = this.walkAnimationSpeed;
        double d = this.x - this.lastX;
        double e = this.z - this.lastZ;
        float f = MathHelper.sqrt(d * d + e * e) * 4.0f;
        if (f > 1.0f) {
            f = 1.0f;
        }
        this.walkAnimationSpeed += (f - this.walkAnimationSpeed) * 0.4f;
        this.walkAnimationProgress += this.walkAnimationSpeed;
    }

    public float getShadowHeightOffset() {
        return 0.0f;
    }

    public void mobTick() {
        super.aiTick();
        if (this.lerpSteps > 0) {
            double i;
            double d = this.x + (this.lerpX - this.x) / (double)this.lerpSteps;
            double e = this.y + (this.lerpY - this.y) / (double)this.lerpSteps;
            double h = this.z + (this.lerpZ - this.z) / (double)this.lerpSteps;
            for (i = this.lerpYaw - (double)this.yaw; i < -180.0; i += 360.0) {
            }
            while (i >= 180.0) {
                i -= 360.0;
            }
            this.yaw = (float)((double)this.yaw + i / (double)this.lerpSteps);
            this.pitch = (float)((double)this.pitch + (this.lerpPitch - (double)this.pitch) / (double)this.lerpSteps);
            --this.lerpSteps;
            this.setPosition(d, e, h);
            this.setRotation(this.yaw, this.pitch);
        }
        this.lastBob = this.bob;
        float f = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
        float g = (float)Math.atan(-this.velocityY * (double)0.2f) * 15.0f;
        if (f > 0.1f) {
            f = 0.1f;
        }
        if (!this.onGround || this.health <= 0) {
            f = 0.0f;
        }
        if (this.onGround || this.health <= 0) {
            g = 0.0f;
        }
        this.bob += (f - this.bob) * 0.4f;
        this.tilt += (g - this.tilt) * 0.8f;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }
}

