/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class FishingBobberRenderer
extends EntityRenderer {
    public void m_82069539(FishingBobberEntity fishingBobberEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        GL11.glEnable((int)32826);
        GL11.glScalef((float)0.5f, (float)0.5f, (float)0.5f);
        int i = 1;
        int j = 2;
        this.bindTexture("/particles.png");
        Tesselator tesselator = Tesselator.INSTANCE;
        float k = (float)(i * 8 + 0) / 128.0f;
        float l = (float)(i * 8 + 8) / 128.0f;
        float m = (float)(j * 8 + 0) / 128.0f;
        float n = (float)(j * 8 + 8) / 128.0f;
        float o = 1.0f;
        float p = 0.5f;
        float q = 0.5f;
        GL11.glRotatef((float)(180.0f - this.dispatcher.cameraYaw), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(-this.dispatcher.cameraPitch), (float)1.0f, (float)0.0f, (float)0.0f);
        tesselator.begin();
        tesselator.normal(0.0f, 1.0f, 0.0f);
        tesselator.vertex(0.0f - p, 0.0f - q, 0.0, k, n);
        tesselator.vertex(o - p, 0.0f - q, 0.0, l, n);
        tesselator.vertex(o - p, 1.0f - q, 0.0, l, m);
        tesselator.vertex(0.0f - p, 1.0f - q, 0.0, k, m);
        tesselator.end();
        GL11.glDisable((int)32826);
        GL11.glPopMatrix();
        if (fishingBobberEntity.thrower != null) {
            float r = (fishingBobberEntity.thrower.lastYaw + (fishingBobberEntity.thrower.yaw - fishingBobberEntity.thrower.lastYaw) * h) * (float)Math.PI / 180.0f;
            float s = (fishingBobberEntity.thrower.lastPitch + (fishingBobberEntity.thrower.pitch - fishingBobberEntity.thrower.lastPitch) * h) * (float)Math.PI / 180.0f;
            double t = MathHelper.sin(r);
            double u = MathHelper.cos(r);
            double v = MathHelper.sin(s);
            double w = MathHelper.cos(s);
            double x = fishingBobberEntity.thrower.lastX + (fishingBobberEntity.thrower.x - fishingBobberEntity.thrower.lastX) * (double)h - u * 0.7 - t * 0.5 * w;
            double y = fishingBobberEntity.thrower.lastY + (fishingBobberEntity.thrower.y - fishingBobberEntity.thrower.lastY) * (double)h - v * 0.5;
            double z = fishingBobberEntity.thrower.lastZ + (fishingBobberEntity.thrower.z - fishingBobberEntity.thrower.lastZ) * (double)h - t * 0.7 + u * 0.5 * w;
            if (this.dispatcher.options.perspective) {
                r = (fishingBobberEntity.thrower.lastBodyYaw + (fishingBobberEntity.thrower.bodyYaw - fishingBobberEntity.thrower.lastBodyYaw) * h) * (float)Math.PI / 180.0f;
                t = MathHelper.sin(r);
                u = MathHelper.cos(r);
                x = fishingBobberEntity.thrower.lastX + (fishingBobberEntity.thrower.x - fishingBobberEntity.thrower.lastX) * (double)h - u * 0.35 - t * 0.85;
                y = fishingBobberEntity.thrower.lastY + (fishingBobberEntity.thrower.y - fishingBobberEntity.thrower.lastY) * (double)h - 0.45;
                z = fishingBobberEntity.thrower.lastZ + (fishingBobberEntity.thrower.z - fishingBobberEntity.thrower.lastZ) * (double)h - t * 0.35 + u * 0.85;
            }
            double aa = fishingBobberEntity.lastX + (fishingBobberEntity.x - fishingBobberEntity.lastX) * (double)h;
            double ab = fishingBobberEntity.lastY + (fishingBobberEntity.y - fishingBobberEntity.lastY) * (double)h + 0.25;
            double ac = fishingBobberEntity.lastZ + (fishingBobberEntity.z - fishingBobberEntity.lastZ) * (double)h;
            double ad = (float)(x - aa);
            double ae = (float)(y - ab);
            double af = (float)(z - ac);
            GL11.glDisable((int)GL11.GL_TEXTURE_2D);
            GL11.glDisable((int)GL11.GL_LIGHTING);
            tesselator.begin(3);
            tesselator.color(0);
            int ag = 16;
            for (int ah = 0; ah <= ag; ++ah) {
                float ai = (float)ah / (float)ag;
                tesselator.vertex(d + ad * (double)ai, e + ae * (double)(ai * ai + ai) * 0.5 + 0.25, f + af * (double)ai);
            }
            tesselator.end();
            GL11.glEnable((int)GL11.GL_LIGHTING);
            GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        }
    }
}

