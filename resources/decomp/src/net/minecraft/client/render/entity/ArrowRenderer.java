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
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ArrowRenderer
extends EntityRenderer {
    public void m_11289555(ArrowEntity arrowEntity, double d, double e, double f, float g, float h) {
        this.bindTexture("/item/arrows.png");
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        GL11.glRotatef((float)(arrowEntity.lastYaw + (arrowEntity.yaw - arrowEntity.lastYaw) * h - 90.0f), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(arrowEntity.lastPitch + (arrowEntity.pitch - arrowEntity.lastPitch) * h), (float)0.0f, (float)0.0f, (float)1.0f);
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = 0;
        float j = 0.0f;
        float k = 0.5f;
        float l = (float)(0 + i * 10) / 32.0f;
        float m = (float)(5 + i * 10) / 32.0f;
        float n = 0.0f;
        float o = 0.15625f;
        float p = (float)(5 + i * 10) / 32.0f;
        float q = (float)(10 + i * 10) / 32.0f;
        float r = 0.05625f;
        GL11.glEnable((int)32826);
        float s = (float)arrowEntity.shake - h;
        if (s > 0.0f) {
            float t = -MathHelper.sin(s * 3.0f) * s;
            GL11.glRotatef((float)t, (float)0.0f, (float)0.0f, (float)1.0f);
        }
        GL11.glRotatef((float)45.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        GL11.glScalef((float)r, (float)r, (float)r);
        GL11.glTranslatef((float)-4.0f, (float)0.0f, (float)0.0f);
        GL11.glNormal3f((float)r, (float)0.0f, (float)0.0f);
        tesselator.begin();
        tesselator.vertex(-7.0, -2.0, -2.0, n, p);
        tesselator.vertex(-7.0, -2.0, 2.0, o, p);
        tesselator.vertex(-7.0, 2.0, 2.0, o, q);
        tesselator.vertex(-7.0, 2.0, -2.0, n, q);
        tesselator.end();
        GL11.glNormal3f((float)(-r), (float)0.0f, (float)0.0f);
        tesselator.begin();
        tesselator.vertex(-7.0, 2.0, -2.0, n, p);
        tesselator.vertex(-7.0, 2.0, 2.0, o, p);
        tesselator.vertex(-7.0, -2.0, 2.0, o, q);
        tesselator.vertex(-7.0, -2.0, -2.0, n, q);
        tesselator.end();
        for (int u = 0; u < 4; ++u) {
            GL11.glRotatef((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glNormal3f((float)0.0f, (float)0.0f, (float)r);
            tesselator.begin();
            tesselator.vertex(-8.0, -2.0, 0.0, j, l);
            tesselator.vertex(8.0, -2.0, 0.0, k, l);
            tesselator.vertex(8.0, 2.0, 0.0, k, m);
            tesselator.vertex(-8.0, 2.0, 0.0, j, m);
            tesselator.end();
        }
        GL11.glDisable((int)32826);
        GL11.glPopMatrix();
    }
}

