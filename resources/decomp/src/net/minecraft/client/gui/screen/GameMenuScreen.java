/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class GameMenuScreen
extends Screen {
    private int saveStep = 0;
    private int ticks = 0;

    public void init() {
        this.saveStep = 0;
        this.buttons.clear();
        this.buttons.add(new ButtonWidget(1, this.width / 2 - 100, this.height / 4 + 48, "Save and quit to title"));
        if (this.minecraft.isMultiplayer()) {
            ((ButtonWidget)this.buttons.get((int)0)).message = "Disconnect";
        }
        this.buttons.add(new ButtonWidget(4, this.width / 2 - 100, this.height / 4 + 24, "Back to game"));
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height / 4 + 96, "Options..."));
    }

    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 0) {
            this.minecraft.openScreen(new OptionsScreen(this, this.minecraft.options));
        }
        if (button.id == 1) {
            if (this.minecraft.isMultiplayer()) {
                this.minecraft.world.disconnect();
            }
            this.minecraft.setWorld(null);
            this.minecraft.openScreen(new TitleScreen());
        }
        if (button.id == 4) {
            this.minecraft.openScreen(null);
            this.minecraft.lockMouse();
        }
    }

    public void tick() {
        super.tick();
        ++this.ticks;
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        boolean i;
        this.renderBackground();
        boolean bl = i = !this.minecraft.world.saveWhilePaused(this.saveStep++);
        if (i || this.ticks < 20) {
            float f = ((float)(this.ticks % 10) + tickDelta) / 10.0f;
            f = MathHelper.sin(f * (float)Math.PI * 2.0f) * 0.2f + 0.8f;
            int j = (int)(255.0f * f);
            this.drawString(this.textRenderer, "Saving level..", 8, this.height - 16, j << 16 | j << 8 | j);
        }
        this.drawCenteredString(this.textRenderer, "Game menu", this.width / 2, 40, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

