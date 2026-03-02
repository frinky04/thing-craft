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
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class RainSplashParticle
extends Particle {
    public RainSplashParticle(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.velocityX *= (double)0.3f;
        this.velocityY = (float)Math.random() * 0.2f + 0.1f;
        this.velocityZ *= (double)0.3f;
        this.red = 1.0f;
        this.green = 1.0f;
        this.blue = 1.0f;
        this.sprite = 19 + this.random.nextInt(4);
        this.setSize(0.01f, 0.01f);
        this.gravity = 0.06f;
        this.lifetime = (int)(8.0 / (Math.random() * 0.8 + 0.2));
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        super.render(tesselator, tickDelta, dx, dy, dz, forwards, sideways);
    }

    public void tick() {
        double d;
        Material material;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.velocityY -= (double)this.gravity;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= (double)0.98f;
        this.velocityY *= (double)0.98f;
        this.velocityZ *= (double)0.98f;
        if (this.lifetime-- <= 0) {
            this.remove();
        }
        if (this.onGround) {
            if (Math.random() < 0.5) {
                this.remove();
            }
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
        }
        if (((material = this.world.getMaterial(MathHelper.floor(this.x), MathHelper.floor(this.y), MathHelper.floor(this.z))).isLiquid() || material.isSolid()) && this.y < (d = (double)((float)(MathHelper.floor(this.y) + 1) - LiquidBlock.getHeightLoss(this.world.getBlockMetadata(MathHelper.floor(this.x), MathHelper.floor(this.y), MathHelper.floor(this.z)))))) {
            this.remove();
        }
    }
}

