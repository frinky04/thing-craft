/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.Sys
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.screen;

import java.io.File;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.resource.pack.TexturePack;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class TexturePacksScreen
extends Screen {
    protected Screen parent;
    private int lastY = 0;
    private int minY = 32;
    private int maxY = this.height - 55 + 4;
    private int minX = 0;
    private int maxX = this.width;
    private int draggingY = -2;
    private int cooldown = -1;
    private String texturePacksDir = "";

    public TexturePacksScreen(Screen parent) {
        this.parent = parent;
    }

    public void init() {
        this.buttons.add(new OptionButtonWidget(5, this.width / 2 - 154, this.height - 48, "Open texture pack folder"));
        this.buttons.add(new OptionButtonWidget(6, this.width / 2 + 4, this.height - 48, "Done"));
        this.minecraft.texturePacks.reload();
        this.texturePacksDir = new File(this.minecraft.gameDir, "texturepacks").getAbsolutePath();
        this.minY = 32;
        this.maxY = this.height - 58 + 4;
        this.minX = 0;
        this.maxX = this.width;
    }

    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        if (button.id == 5) {
            Sys.openURL((String)("file://" + this.texturePacksDir));
        }
        if (button.id == 6) {
            this.minecraft.textureManager.reload();
            this.minecraft.openScreen(this.parent);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
    }

    protected void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        int m;
        this.renderBackground();
        if (this.cooldown <= 0) {
            this.minecraft.texturePacks.reload();
            this.cooldown += 20;
        }
        List list = this.minecraft.texturePacks.getAvailable();
        if (Mouse.isButtonDown((int)0)) {
            if (this.draggingY == -1) {
                if (mouseY >= this.minY && mouseY <= this.maxY) {
                    int i = this.width / 2 - 110;
                    int k = this.width / 2 + 110;
                    int l = (mouseY - this.minY + this.lastY - 2) / 36;
                    if (mouseX >= i && mouseX <= k && l >= 0 && l < list.size() && this.minecraft.texturePacks.select((TexturePack)list.get(l))) {
                        this.minecraft.textureManager.reload();
                    }
                    this.draggingY = mouseY;
                } else {
                    this.draggingY = -2;
                }
            } else if (this.draggingY >= 0) {
                this.lastY -= mouseY - this.draggingY;
                this.draggingY = mouseY;
            }
        } else {
            if (this.draggingY < 0 || this.draggingY == mouseY) {
                // empty if block
            }
            this.draggingY = -1;
        }
        int j = list.size() * 36 - (this.maxY - this.minY - 4);
        if (j < 0) {
            j /= 2;
        }
        if (this.lastY < 0) {
            this.lastY = 0;
        }
        if (this.lastY > j) {
            this.lastY = j;
        }
        GL11.glDisable((int)GL11.GL_LIGHTING);
        GL11.glDisable((int)GL11.GL_FOG);
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/background.png"));
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        float f = 32.0f;
        tesselator.begin();
        tesselator.color(0x202020);
        tesselator.vertex(this.minX, this.maxY, 0.0, (float)this.minX / f, (float)(this.maxY + this.lastY) / f);
        tesselator.vertex(this.maxX, this.maxY, 0.0, (float)this.maxX / f, (float)(this.maxY + this.lastY) / f);
        tesselator.vertex(this.maxX, this.minY, 0.0, (float)this.maxX / f, (float)(this.minY + this.lastY) / f);
        tesselator.vertex(this.minX, this.minY, 0.0, (float)this.minX / f, (float)(this.minY + this.lastY) / f);
        tesselator.end();
        for (m = 0; m < list.size(); ++m) {
            TexturePack texturePack = (TexturePack)list.get(m);
            int n = this.width / 2 - 92 - 16;
            int o = 36 + m * 36 - this.lastY;
            int p = 32;
            int q = 32;
            if (texturePack == this.minecraft.texturePacks.selected) {
                int r = this.width / 2 - 110;
                int s = this.width / 2 + 110;
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                GL11.glDisable((int)GL11.GL_TEXTURE_2D);
                tesselator.begin();
                tesselator.color(0x808080);
                tesselator.vertex(r, o + p + 2, 0.0, 0.0, 1.0);
                tesselator.vertex(s, o + p + 2, 0.0, 1.0, 1.0);
                tesselator.vertex(s, o - 2, 0.0, 1.0, 0.0);
                tesselator.vertex(r, o - 2, 0.0, 0.0, 0.0);
                tesselator.color(0);
                tesselator.vertex(r + 1, o + p + 1, 0.0, 0.0, 1.0);
                tesselator.vertex(s - 1, o + p + 1, 0.0, 1.0, 1.0);
                tesselator.vertex(s - 1, o - 1, 0.0, 1.0, 0.0);
                tesselator.vertex(r + 1, o - 1, 0.0, 0.0, 0.0);
                tesselator.end();
                GL11.glEnable((int)GL11.GL_TEXTURE_2D);
            }
            texturePack.bindIcon(this.minecraft);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            tesselator.begin();
            tesselator.color(0xFFFFFF);
            tesselator.vertex(n, o + p, 0.0, 0.0, 1.0);
            tesselator.vertex(n + q, o + p, 0.0, 1.0, 1.0);
            tesselator.vertex(n + q, o, 0.0, 1.0, 0.0);
            tesselator.vertex(n, o, 0.0, 0.0, 0.0);
            tesselator.end();
            this.drawString(this.textRenderer, texturePack.name, n + q + 2, o + 1, 0xFFFFFF);
            this.drawString(this.textRenderer, texturePack.descriptionLine1, n + q + 2, o + 12, 0x808080);
            this.drawString(this.textRenderer, texturePack.descriptionLine2, n + q + 2, o + 12 + 10, 0x808080);
        }
        m = 4;
        this.renderHoleBackground(0, this.minY, 255, 255);
        this.renderHoleBackground(this.maxY, this.height, 255, 255);
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glShadeModel((int)GL11.GL_SMOOTH);
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        tesselator.begin();
        tesselator.color(0, 0);
        tesselator.vertex(this.minX, this.minY + m, 0.0, 0.0, 1.0);
        tesselator.vertex(this.maxX, this.minY + m, 0.0, 1.0, 1.0);
        tesselator.color(0, 255);
        tesselator.vertex(this.maxX, this.minY, 0.0, 1.0, 0.0);
        tesselator.vertex(this.minX, this.minY, 0.0, 0.0, 0.0);
        tesselator.end();
        tesselator.begin();
        tesselator.color(0, 255);
        tesselator.vertex(this.minX, this.maxY, 0.0, 0.0, 1.0);
        tesselator.vertex(this.maxX, this.maxY, 0.0, 1.0, 1.0);
        tesselator.color(0, 0);
        tesselator.vertex(this.maxX, this.maxY - m, 0.0, 1.0, 0.0);
        tesselator.vertex(this.minX, this.maxY - m, 0.0, 0.0, 0.0);
        tesselator.end();
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        GL11.glShadeModel((int)GL11.GL_FLAT);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glDisable((int)GL11.GL_BLEND);
        this.drawCenteredString(this.textRenderer, "Select Texture Pack", this.width / 2, 16, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer, "(Place texture pack files here)", this.width / 2 - 77, this.height - 26, 0x808080);
        super.render(mouseX, mouseY, tickDelta);
    }

    public void tick() {
        super.tick();
        --this.cooldown;
    }

    public void renderHoleBackground(int minY, int maxY, int bottomAlpha, int topAlpha) {
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/background.png"));
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        float f = 32.0f;
        tesselator.begin();
        tesselator.color(0x404040, topAlpha);
        tesselator.vertex(0.0, maxY, 0.0, 0.0, (float)maxY / f);
        tesselator.vertex(this.width, maxY, 0.0, (float)this.width / f, (float)maxY / f);
        tesselator.color(0x404040, bottomAlpha);
        tesselator.vertex(this.width, minY, 0.0, (float)this.width / f, (float)minY / f);
        tesselator.vertex(0.0, minY, 0.0, 0.0, (float)minY / f);
        tesselator.end();
    }
}

