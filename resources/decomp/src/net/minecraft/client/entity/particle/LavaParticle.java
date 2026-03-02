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
public class LavaParticle
extends Particle {
    private float initialSize;

    public LavaParticle(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.velocityX *= (double)0.8f;
        this.velocityY *= (double)0.8f;
        this.velocityZ *= (double)0.8f;
        this.velocityY = this.random.nextFloat() * 0.4f + 0.05f;
        this.blue = 1.0f;
        this.green = 1.0f;
        this.red = 1.0f;
        this.size *= this.random.nextFloat() * 2.0f + 0.2f;
        this.initialSize = this.size;
        this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
        this.noClip = false;
        this.sprite = 49;
    }

    public float getBrightness(float tickDelta) {
        return 1.0f;
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = ((float)this.age + tickDelta) / (float)this.lifetime;
        this.size = this.initialSize * (1.0f - f * f);
        super.render(tesselator, tickDelta, dx, dy, dz, forwards, sideways);
    }

    public void tick() {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        float f = (float)this.age / (float)this.lifetime;
        if (this.random.nextFloat() > f) {
            this.world.addParticle("smoke", this.x, this.y, this.z, this.velocityX, this.velocityY, this.velocityZ);
        }
        this.velocityY -= 0.03;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= (double)0.999f;
        this.velocityY *= (double)0.999f;
        this.velocityZ *= (double)0.999f;
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
        }
    }
}

