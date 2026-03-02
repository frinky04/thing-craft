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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;

@Environment(value=EnvType.CLIENT)
public class ConfirmScreen
extends Screen {
    private Screen listener;
    private String title;
    private String description;
    private int id;

    public ConfirmScreen(Screen parent, String title, String description, int id) {
        this.listener = parent;
        this.title = title;
        this.description = description;
        this.id = id;
    }

    public void init() {
        this.buttons.add(new OptionButtonWidget(0, this.width / 2 - 155 + 0, this.height / 6 + 96, "Yes"));
        this.buttons.add(new OptionButtonWidget(1, this.width / 2 - 155 + 160, this.height / 6 + 96, "No"));
    }

    protected void buttonClicked(ButtonWidget button) {
        this.listener.confirmResult(button.id == 0, this.id);
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 70, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer, this.description, this.width / 2, 90, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

