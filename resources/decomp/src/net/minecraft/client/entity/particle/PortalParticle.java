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
public class PortalParticle
extends Particle {
    private float initialSize;
    private double initialX;
    private double initialY;
    private double initialZ;

    public PortalParticle(World world, double d, double e, double f, double g, double h, double i) {
        super(world, d, e, f, g, h, i);
        this.velocityX = g;
        this.velocityY = h;
        this.velocityZ = i;
        this.initialX = this.x = d;
        this.initialY = this.y = e;
        this.initialZ = this.z = f;
        float j = this.random.nextFloat() * 0.6f + 0.4f;
        this.initialSize = this.size = this.random.nextFloat() * 0.2f + 0.5f;
        this.green = this.blue = 1.0f * j;
        this.red = this.blue;
        this.green *= 0.3f;
        this.red *= 0.9f;
        this.lifetime = (int)(Math.random() * 10.0) + 40;
        this.noClip = true;
        this.sprite = (int)(Math.random() * 8.0);
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = ((float)this.age + tickDelta) / (float)this.lifetime;
        f = 1.0f - f;
        f *= f;
        f = 1.0f - f;
        this.size = this.initialSize * f;
        super.render(tesselator, tickDelta, dx, dy, dz, forwards, sideways);
    }

    public float getBrightness(float tickDelta) {
        float f = super.getBrightness(tickDelta);
        float g = (float)this.age / (float)this.lifetime;
        g *= g;
        g *= g;
        return f * (1.0f - g) + g;
    }

    public void tick() {
        float f;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        float g = f = (float)this.age / (float)this.lifetime;
        f = -f + f * f * 2.0f;
        f = 1.0f - f;
        this.x = this.initialX + this.velocityX * (double)f;
        this.y = this.initialY + this.velocityY * (double)f + (double)(1.0f - g);
        this.z = this.initialZ + this.velocityZ * (double)f;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }
}

