/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class SmokeParticle
extends Particle {
    float initialSize;

    public SmokeParticle(World world, double x, double y, double z) {
        this(world, x, y, z, 1.0f);
    }

    public SmokeParticle(World world, double x, double y, double z, float scale) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.velocityX *= (double)0.1f;
        this.velocityY *= (double)0.1f;
        this.velocityZ *= (double)0.1f;
        this.green = this.blue = (float)(Math.random() * (double)0.3f);
        this.red = this.blue;
        this.size *= 0.75f;
        this.size *= scale;
        this.initialSize = this.size;
        this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
        this.lifetime = (int)((float)this.lifetime * scale);
        this.noClip = false;
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = ((float)this.age + tickDelta) / (float)this.lifetime * 32.0f;
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f > 1.0f) {
            f = 1.0f;
        }
        this.size = this.initialSize * f;
        super.render(tesselator, tickDelta, dx, dy, dz, forwards, sideways);
    }

    public void tick() {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.sprite = 7 - this.age * 8 / this.lifetime;
        this.velocityY += 0.004;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        if (this.y == this.lastY) {
            this.velocityX *= 1.1;
            this.velocityZ *= 1.1;
        }
        this.velocityX *= (double)0.96f;
        this.velocityY *= (double)0.96f;
        this.velocityZ *= (double)0.96f;
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
        }
    }
}

