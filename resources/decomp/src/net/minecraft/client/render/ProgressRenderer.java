/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.Display
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.ProgressRenderError;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.util.ProgressListener;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ProgressRenderer
implements ProgressListener {
    private String stage = "";
    private Minecraft minecraft;
    private String title = "";
    private long lastTime = System.currentTimeMillis();
    private boolean noAbort = false;

    public ProgressRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void progressStart(String title) {
        this.noAbort = false;
        this.start(title);
    }

    public void progressStartNoAbort(String title) {
        this.noAbort = true;
        this.start(this.title);
    }

    public void start(String title) {
        if (!this.minecraft.running) {
            if (this.noAbort) {
                return;
            }
            throw new ProgressRenderError();
        }
        this.title = title;
        Window window = new Window(this.minecraft.width, this.minecraft.height);
        int i = window.getWidth();
        int j = window.getHeight();
        GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)i, (double)j, (double)0.0, (double)100.0, (double)300.0);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-200.0f);
    }

    public void progressStage(String stage) {
        if (!this.minecraft.running) {
            if (this.noAbort) {
                return;
            }
            throw new ProgressRenderError();
        }
        this.lastTime = 0L;
        this.stage = stage;
        this.progressStagePercentage(-1);
        this.lastTime = 0L;
    }

    public void progressStagePercentage(int percentage) {
        if (!this.minecraft.running) {
            if (this.noAbort) {
                return;
            }
            throw new ProgressRenderError();
        }
        long l = System.currentTimeMillis();
        if (l - this.lastTime < 20L) {
            return;
        }
        this.lastTime = l;
        Window window = new Window(this.minecraft.width, this.minecraft.height);
        int i = window.getWidth();
        int j = window.getHeight();
        GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)i, (double)j, (double)0.0, (double)100.0, (double)300.0);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-200.0f);
        GL11.glClear((int)16640);
        Tesselator tesselator = Tesselator.INSTANCE;
        int k = this.minecraft.textureManager.load("/gui/background.png");
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)k);
        float f = 32.0f;
        tesselator.begin();
        tesselator.color(0x404040);
        tesselator.vertex(0.0, j, 0.0, 0.0, (float)j / f);
        tesselator.vertex(i, j, 0.0, (float)i / f, (float)j / f);
        tesselator.vertex(i, 0.0, 0.0, (float)i / f, 0.0);
        tesselator.vertex(0.0, 0.0, 0.0, 0.0, 0.0);
        tesselator.end();
        if (percentage >= 0) {
            int m = 100;
            int n = 2;
            int o = i / 2 - m / 2;
            int p = j / 2 + 16;
            GL11.glDisable((int)GL11.GL_TEXTURE_2D);
            tesselator.begin();
            tesselator.color(0x808080);
            tesselator.vertex(o, p, 0.0);
            tesselator.vertex(o, p + n, 0.0);
            tesselator.vertex(o + m, p + n, 0.0);
            tesselator.vertex(o + m, p, 0.0);
            tesselator.color(0x80FF80);
            tesselator.vertex(o, p, 0.0);
            tesselator.vertex(o, p + n, 0.0);
            tesselator.vertex(o + percentage, p + n, 0.0);
            tesselator.vertex(o + percentage, p, 0.0);
            tesselator.end();
            GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        }
        this.minecraft.textRenderer.drawWithShadow(this.title, (i - this.minecraft.textRenderer.getWidth(this.title)) / 2, j / 2 - 4 - 16, 0xFFFFFF);
        this.minecraft.textRenderer.drawWithShadow(this.stage, (i - this.minecraft.textRenderer.getWidth(this.stage)) / 2, j / 2 - 4 + 8, 0xFFFFFF);
        Display.update();
        try {
            Thread.yield();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

