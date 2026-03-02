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
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class SnowballRenderer
extends EntityRenderer {
    public void m_79998952(SnowballEntity snowballEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        GL11.glEnable((int)32826);
        GL11.glScalef((float)0.5f, (float)0.5f, (float)0.5f);
        int i = Item.SNOWBALL.getSprite(null);
        this.bindTexture("/gui/items.png");
        Tesselator tesselator = Tesselator.INSTANCE;
        float j = (float)(i % 16 * 16 + 0) / 256.0f;
        float k = (float)(i % 16 * 16 + 16) / 256.0f;
        float l = (float)(i / 16 * 16 + 0) / 256.0f;
        float m = (float)(i / 16 * 16 + 16) / 256.0f;
        float n = 1.0f;
        float o = 0.5f;
        float p = 0.25f;
        GL11.glRotatef((float)(180.0f - this.dispatcher.cameraYaw), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(-this.dispatcher.cameraPitch), (float)1.0f, (float)0.0f, (float)0.0f);
        tesselator.begin();
        tesselator.normal(0.0f, 1.0f, 0.0f);
        tesselator.vertex(0.0f - o, 0.0f - p, 0.0, j, m);
        tesselator.vertex(n - o, 0.0f - p, 0.0, k, m);
        tesselator.vertex(n - o, 1.0f - p, 0.0, k, l);
        tesselator.vertex(0.0f - o, 1.0f - p, 0.0, j, l);
        tesselator.end();
        GL11.glDisable((int)32826);
        GL11.glPopMatrix();
    }
}

