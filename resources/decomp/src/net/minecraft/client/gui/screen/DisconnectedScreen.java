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
import net.minecraft.client.gui.widget.ButtonWidget;

@Environment(value=EnvType.CLIENT)
public class DisconnectedScreen
extends Screen {
    private String title;
    private String reason;

    public DisconnectedScreen(String title, String reason) {
        this.title = title;
        this.reason = reason;
    }

    public void tick() {
    }

    protected void keyPressed(char chr, int key) {
    }

    public void init() {
        this.buttons.clear();
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height / 4 + 120 + 12, "Back to title screen"));
    }

    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 0) {
            this.minecraft.openScreen(new TitleScreen());
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer, this.reason, this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

