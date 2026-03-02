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
public class FlameParticle
extends Particle {
    private float initialSize;

    public FlameParticle(World world, double d, double e, double f, double g, double h, double i) {
        super(world, d, e, f, g, h, i);
        this.velocityX = this.velocityX * (double)0.01f + g;
        this.velocityY = this.velocityY * (double)0.01f + h;
        this.velocityZ = this.velocityZ * (double)0.01f + i;
        d += (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.05f);
        e += (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.05f);
        f += (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.05f);
        this.initialSize = this.size;
        this.blue = 1.0f;
        this.green = 1.0f;
        this.red = 1.0f;
        this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2)) + 4;
        this.noClip = true;
        this.sprite = 48;
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = ((float)this.age + tickDelta) / (float)this.lifetime;
        this.size = this.initialSize * (1.0f - f * f * 0.5f);
        super.render(tesselator, tickDelta, dx, dy, dz, forwards, sideways);
    }

    public float getBrightness(float tickDelta) {
        float f = ((float)this.age + tickDelta) / (float)this.lifetime;
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f > 1.0f) {
            f = 1.0f;
        }
        float g = super.getBrightness(tickDelta);
        return g * f + (1.0f - f);
    }

    public void tick() {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= (double)0.96f;
        this.velocityY *= (double)0.96f;
        this.velocityZ *= (double)0.96f;
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
        }
    }
}

