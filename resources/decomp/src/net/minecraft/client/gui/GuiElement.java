/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class GuiElement {
    protected float drawOffset = 0.0f;

    protected void fill(int x1, int y1, int x2, int y2, int color) {
        float f = (float)(color >> 24 & 0xFF) / 255.0f;
        float g = (float)(color >> 16 & 0xFF) / 255.0f;
        float h = (float)(color >> 8 & 0xFF) / 255.0f;
        float i = (float)(color & 0xFF) / 255.0f;
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((float)g, (float)h, (float)i, (float)f);
        tesselator.begin();
        tesselator.vertex(x1, y2, 0.0);
        tesselator.vertex(x2, y2, 0.0);
        tesselator.vertex(x2, y1, 0.0);
        tesselator.vertex(x1, y1, 0.0);
        tesselator.end();
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        GL11.glDisable((int)GL11.GL_BLEND);
    }

    protected void fillGradient(int x1, int y1, int x2, int y2, int color1, int color2) {
        float f = (float)(color1 >> 24 & 0xFF) / 255.0f;
        float g = (float)(color1 >> 16 & 0xFF) / 255.0f;
        float h = (float)(color1 >> 8 & 0xFF) / 255.0f;
        float i = (float)(color1 & 0xFF) / 255.0f;
        float j = (float)(color2 >> 24 & 0xFF) / 255.0f;
        float k = (float)(color2 >> 16 & 0xFF) / 255.0f;
        float l = (float)(color2 >> 8 & 0xFF) / 255.0f;
        float m = (float)(color2 & 0xFF) / 255.0f;
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel((int)GL11.GL_SMOOTH);
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.color(g, h, i, f);
        tesselator.vertex(x2, y1, 0.0);
        tesselator.vertex(x1, y1, 0.0);
        tesselator.color(k, l, m, j);
        tesselator.vertex(x1, y2, 0.0);
        tesselator.vertex(x2, y2, 0.0);
        tesselator.end();
        GL11.glShadeModel((int)GL11.GL_FLAT);
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
    }

    public void drawCenteredString(TextRenderer textRenderer, String text, int centerX, int y, int color) {
        textRenderer.drawWithShadow(text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    public void drawString(TextRenderer textRenderer, String text, int x, int y, int color) {
        textRenderer.drawWithShadow(text, x, y, color);
    }

    public void drawTexture(int x, int y, int u, int v, int width, int height) {
        float f = 0.00390625f;
        float g = 0.00390625f;
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(x + 0, y + height, this.drawOffset, (float)(u + 0) * f, (float)(v + height) * g);
        tesselator.vertex(x + width, y + height, this.drawOffset, (float)(u + width) * f, (float)(v + height) * g);
        tesselator.vertex(x + width, y + 0, this.drawOffset, (float)(u + width) * f, (float)(v + 0) * g);
        tesselator.vertex(x + 0, y + 0, this.drawOffset, (float)(u + 0) * f, (float)(v + 0) * g);
        tesselator.end();
    }
}

