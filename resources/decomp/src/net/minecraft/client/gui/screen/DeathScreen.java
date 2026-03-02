/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class DeathScreen
extends Screen {
    public void init() {
        this.buttons.clear();
        this.buttons.add(new ButtonWidget(1, this.width / 2 - 100, this.height / 4 + 72, "Respawn"));
        this.buttons.add(new ButtonWidget(2, this.width / 2 - 100, this.height / 4 + 96, "Title menu"));
        if (this.minecraft.session == null) {
            ((ButtonWidget)this.buttons.get((int)1)).active = false;
        }
    }

    protected void keyPressed(char chr, int key) {
    }

    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 0) {
            // empty if block
        }
        if (button.id == 1) {
            this.minecraft.player.respawn();
            this.minecraft.openScreen(null);
        }
        if (button.id == 2) {
            this.minecraft.setWorld(null);
            this.minecraft.openScreen(new TitleScreen());
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.fillGradient(0, 0, this.width, this.height, 0x60500000, -1602211792);
        GL11.glPushMatrix();
        GL11.glScalef((float)2.0f, (float)2.0f, (float)2.0f);
        this.drawCenteredString(this.textRenderer, "Game over!", this.width / 2 / 2, 30, 0xFFFFFF);
        GL11.glPopMatrix();
        this.drawCenteredString(this.textRenderer, "Score: &e" + this.minecraft.player.getScore(), this.width / 2, 100, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }

    public boolean isPauseScreen() {
        return false;
    }
}

