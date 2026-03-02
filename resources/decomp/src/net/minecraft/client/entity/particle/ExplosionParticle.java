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
public class ExplosionParticle
extends Particle {
    public ExplosionParticle(World world, double d, double e, double f, double g, double h, double i) {
        super(world, d, e, f, g, h, i);
        this.velocityX = g + (double)((float)(Math.random() * 2.0 - 1.0) * 0.05f);
        this.velocityY = h + (double)((float)(Math.random() * 2.0 - 1.0) * 0.05f);
        this.velocityZ = i + (double)((float)(Math.random() * 2.0 - 1.0) * 0.05f);
        this.green = this.blue = this.random.nextFloat() * 0.3f + 0.7f;
        this.red = this.blue;
        this.size = this.random.nextFloat() * this.random.nextFloat() * 6.0f + 1.0f;
        this.lifetime = (int)(16.0 / ((double)this.random.nextFloat() * 0.8 + 0.2)) + 2;
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
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
        this.velocityX *= (double)0.9f;
        this.velocityY *= (double)0.9f;
        this.velocityZ *= (double)0.9f;
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
        }
    }
}

