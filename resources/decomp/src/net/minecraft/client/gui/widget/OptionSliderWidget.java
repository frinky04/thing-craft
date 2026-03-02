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
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class OptionSliderWidget
extends ButtonWidget {
    public float value = 1.0f;
    public boolean dragging = false;
    private int option = 0;

    public OptionSliderWidget(int id, int x, int y, int option, String message, float value) {
        super(id, x, y, 150, 20, message);
        this.option = option;
        this.value = value;
    }

    protected int getYImage(boolean hovered) {
        return 0;
    }

    protected void renderBackground(Minecraft minecraft, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        if (this.dragging) {
            this.value = (float)(mouseX - (this.x + 4)) / (float)(this.width - 8);
            if (this.value < 0.0f) {
                this.value = 0.0f;
            }
            if (this.value > 1.0f) {
                this.value = 1.0f;
            }
            minecraft.options.set(this.option, this.value);
            this.message = minecraft.options.getAsString(this.option);
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.drawTexture(this.x + (int)(this.value * (float)(this.width - 8)), this.y, 0, 66, 4, 20);
        this.drawTexture(this.x + (int)(this.value * (float)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
    }

    public boolean mouseClicked(Minecraft minecraft, int mouseX, int mouseY) {
        if (super.mouseClicked(minecraft, mouseX, mouseY)) {
            this.value = (float)(mouseX - (this.x + 4)) / (float)(this.width - 8);
            if (this.value < 0.0f) {
                this.value = 0.0f;
            }
            if (this.value > 1.0f) {
                this.value = 1.0f;
            }
            minecraft.options.set(this.option, this.value);
            this.message = minecraft.options.getAsString(this.option);
            this.dragging = true;
            return true;
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
}

