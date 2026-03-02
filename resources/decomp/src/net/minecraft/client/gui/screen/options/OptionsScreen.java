/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screen.options;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.options.GameOptions;

@Environment(value=EnvType.CLIENT)
public class OptionsScreen
extends Screen {
    private Screen parent;
    protected String title = "Options";
    private GameOptions options;

    public OptionsScreen(Screen parent, GameOptions options) {
        this.parent = parent;
        this.options = options;
    }

    public void init() {
        for (int i = 0; i < this.options.count; ++i) {
            int j = this.options.getInt(i);
            if (j == 0) {
                this.buttons.add(new OptionButtonWidget(i, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), this.options.getAsString(i)));
                continue;
            }
            this.buttons.add(new OptionSliderWidget(i, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), i, this.options.getAsString(i), this.options.getFloat(i)));
        }
        this.buttons.add(new ButtonWidget(100, this.width / 2 - 100, this.height / 6 + 120 + 12, "Controls..."));
        this.buttons.add(new ButtonWidget(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
    }

    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        if (button.id < 100) {
            this.options.set(button.id, 1);
            button.message = this.options.getAsString(button.id);
        }
        if (button.id == 100) {
            this.minecraft.options.save();
            this.minecraft.openScreen(new ControlsOptionsScreen(this, this.options));
        }
        if (button.id == 200) {
            this.minecraft.options.save();
            this.minecraft.openScreen(this.parent);
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

