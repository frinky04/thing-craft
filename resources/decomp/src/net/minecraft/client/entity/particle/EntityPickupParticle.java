/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.entity.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class EntityPickupParticle
extends Particle {
    private Entity entity;
    private Entity collector;
    private int age = 0;
    private int lifetime = 0;
    private float offsetY;

    public EntityPickupParticle(World world, Entity entity, Entity collector, float offsetY) {
        super(world, entity.x, entity.y, entity.z, entity.velocityX, entity.velocityY, entity.velocityZ);
        this.entity = entity;
        this.collector = collector;
        this.lifetime = 3;
        this.offsetY = offsetY;
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = ((float)this.age + tickDelta) / (float)this.lifetime;
        f *= f;
        double d = this.entity.x;
        double e = this.entity.y;
        double g = this.entity.z;
        double h = this.collector.prevX + (this.collector.x - this.collector.prevX) * (double)tickDelta;
        double i = this.collector.prevY + (this.collector.y - this.collector.prevY) * (double)tickDelta + (double)this.offsetY;
        double j = this.collector.prevZ + (this.collector.z - this.collector.prevZ) * (double)tickDelta;
        double k = d + (h - d) * (double)f;
        double l = e + (i - e) * (double)f;
        double m = g + (j - g) * (double)f;
        int n = MathHelper.floor(k);
        int o = MathHelper.floor(l + (double)(this.eyeHeight / 2.0f));
        int p = MathHelper.floor(m);
        float q = this.world.getBrightness(n, o, p);
        GL11.glColor4f((float)q, (float)q, (float)q, (float)1.0f);
        EntityRenderDispatcher.INSTANCE.render(this.entity, (float)(k -= lerpCameraX), (float)(l -= lerpCameraY), (float)(m -= lerpCameraZ), this.entity.yaw, tickDelta);
    }

    public void tick() {
        ++this.age;
        if (this.age == this.lifetime) {
            this.remove();
        }
    }

    public int getAtlasType() {
        return 3;
    }
}

