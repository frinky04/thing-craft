/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.decoration.PaintingEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class PaintingRenderer
extends EntityRenderer {
    private Random random = new Random();

    public void m_13244935(PaintingEntity paintingEntity, double d, double e, double f, float g, float h) {
        this.random.setSeed(187L);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        GL11.glRotatef((float)g, (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glEnable((int)32826);
        this.bindTexture("/art/kz.png");
        PaintingEntity.Motive motive = paintingEntity.motive;
        float i = 0.0625f;
        GL11.glScalef((float)i, (float)i, (float)i);
        this.renderPainting(paintingEntity, motive.width, motive.height, motive.u, motive.v);
        GL11.glDisable((int)32826);
        GL11.glPopMatrix();
    }

    private void renderPainting(PaintingEntity painting, int width, int height, int u, int v) {
        float f = (float)(-width) / 2.0f;
        float g = (float)(-height) / 2.0f;
        float h = -0.5f;
        float i = 0.5f;
        for (int j = 0; j < width / 16; ++j) {
            for (int k = 0; k < height / 16; ++k) {
                float l = f + (float)((j + 1) * 16);
                float m = f + (float)(j * 16);
                float n = g + (float)((k + 1) * 16);
                float o = g + (float)(k * 16);
                this.applyBrightness(painting, (l + m) / 2.0f, (n + o) / 2.0f);
                float p = (float)(u + width - j * 16) / 256.0f;
                float q = (float)(u + width - (j + 1) * 16) / 256.0f;
                float r = (float)(v + height - k * 16) / 256.0f;
                float s = (float)(v + height - (k + 1) * 16) / 256.0f;
                float t = 0.75f;
                float w = 0.8125f;
                float x = 0.0f;
                float y = 0.0625f;
                float z = 0.75f;
                float aa = 0.8125f;
                float ab = 0.001953125f;
                float ac = 0.001953125f;
                float ad = 0.7519531f;
                float ae = 0.7519531f;
                float af = 0.0f;
                float ag = 0.0625f;
                Tesselator tesselator = Tesselator.INSTANCE;
                tesselator.begin();
                tesselator.normal(0.0f, 0.0f, -1.0f);
                tesselator.vertex(l, o, h, q, r);
                tesselator.vertex(m, o, h, p, r);
                tesselator.vertex(m, n, h, p, s);
                tesselator.vertex(l, n, h, q, s);
                tesselator.normal(0.0f, 0.0f, 1.0f);
                tesselator.vertex(l, n, i, t, x);
                tesselator.vertex(m, n, i, w, x);
                tesselator.vertex(m, o, i, w, y);
                tesselator.vertex(l, o, i, t, y);
                tesselator.normal(0.0f, -1.0f, 0.0f);
                tesselator.vertex(l, n, h, z, ab);
                tesselator.vertex(m, n, h, aa, ab);
                tesselator.vertex(m, n, i, aa, ac);
                tesselator.vertex(l, n, i, z, ac);
                tesselator.normal(0.0f, 1.0f, 0.0f);
                tesselator.vertex(l, o, i, z, ab);
                tesselator.vertex(m, o, i, aa, ab);
                tesselator.vertex(m, o, h, aa, ac);
                tesselator.vertex(l, o, h, z, ac);
                tesselator.normal(-1.0f, 0.0f, 0.0f);
                tesselator.vertex(l, n, i, ae, af);
                tesselator.vertex(l, o, i, ae, ag);
                tesselator.vertex(l, o, h, ad, ag);
                tesselator.vertex(l, n, h, ad, af);
                tesselator.normal(1.0f, 0.0f, 0.0f);
                tesselator.vertex(m, n, h, ae, af);
                tesselator.vertex(m, o, h, ae, ag);
                tesselator.vertex(m, o, i, ad, ag);
                tesselator.vertex(m, n, i, ad, af);
                tesselator.end();
            }
        }
    }

    private void applyBrightness(PaintingEntity painting, float u, float v) {
        int i = MathHelper.floor(painting.x);
        int j = MathHelper.floor(painting.y + (double)(v / 16.0f));
        int k = MathHelper.floor(painting.z);
        if (painting.dir == 0) {
            i = MathHelper.floor(painting.x + (double)(u / 16.0f));
        }
        if (painting.dir == 1) {
            k = MathHelper.floor(painting.z - (double)(u / 16.0f));
        }
        if (painting.dir == 2) {
            i = MathHelper.floor(painting.x - (double)(u / 16.0f));
        }
        if (painting.dir == 3) {
            k = MathHelper.floor(painting.z + (double)(u / 16.0f));
        }
        float f = this.dispatcher.world.getBrightness(i, j, k);
        GL11.glColor3f((float)f, (float)f, (float)f);
    }
}

