/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.TextRenderer;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ButtonWidget
extends GuiElement {
    protected int width = 200;
    protected int height = 20;
    public int x;
    public int y;
    public String message;
    public int id;
    public boolean active = true;
    public boolean visible = true;

    public ButtonWidget(int id, int x, int y, String message) {
        this(id, x, y, 200, 20, message);
    }

    protected ButtonWidget(int id, int x, int y, int width, int height, String message) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
    }

    protected int getYImage(boolean hovered) {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (hovered) {
            i = 2;
        }
        return i;
    }

    public void render(Minecraft minecraft, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        TextRenderer textRenderer = minecraft.textRenderer;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)minecraft.textureManager.load("/gui/gui.png"));
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        boolean i = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        int j = this.getYImage(i);
        this.drawTexture(this.x, this.y, 0, 46 + j * 20, this.width / 2, this.height);
        this.drawTexture(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + j * 20, this.width / 2, this.height);
        this.renderBackground(minecraft, mouseX, mouseY);
        if (!this.active) {
            this.drawCenteredString(textRenderer, this.message, this.x + this.width / 2, this.y + (this.height - 8) / 2, -6250336);
        } else if (i) {
            this.drawCenteredString(textRenderer, this.message, this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFFFFA0);
        } else {
            this.drawCenteredString(textRenderer, this.message, this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xE0E0E0);
        }
    }

    protected void renderBackground(Minecraft minecraft, int mouseX, int mouseY) {
    }

    public void mouseReleased(int mouseX, int mouseY) {
    }

    public boolean mouseClicked(Minecraft minecraft, int mouseX, int mouseY) {
        return this.active && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }
}

