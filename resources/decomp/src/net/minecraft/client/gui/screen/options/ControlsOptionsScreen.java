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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.options.GameOptions;

@Environment(value=EnvType.CLIENT)
public class ControlsOptionsScreen
extends Screen {
    private Screen parent;
    protected String title = "Controls";
    private GameOptions options;
    private int selectedKeyBinding = -1;

    public ControlsOptionsScreen(Screen parent, GameOptions options) {
        this.parent = parent;
        this.options = options;
    }

    public void init() {
        for (int i = 0; i < this.options.keyBindings.length; ++i) {
            this.buttons.add(new OptionButtonWidget(i, this.width / 2 - 155 + i % 2 * 160, this.height / 6 + 24 * (i >> 1), this.options.getBoundKeyName(i)));
        }
        this.buttons.add(new ButtonWidget(200, this.width / 2 - 100, this.height / 6 + 168, "Done"));
    }

    protected void buttonClicked(ButtonWidget button) {
        for (int i = 0; i < this.options.keyBindings.length; ++i) {
            ((ButtonWidget)this.buttons.get((int)i)).message = this.options.getBoundKeyName(i);
        }
        if (button.id == 200) {
            this.minecraft.openScreen(this.parent);
        } else {
            this.selectedKeyBinding = button.id;
            button.message = "> " + this.options.getBoundKeyName(button.id) + " <";
        }
    }

    protected void keyPressed(char chr, int key) {
        if (this.selectedKeyBinding >= 0) {
            this.options.bindKey(this.selectedKeyBinding, key);
            ((ButtonWidget)this.buttons.get((int)this.selectedKeyBinding)).message = this.options.getBoundKeyName(this.selectedKeyBinding);
            this.selectedKeyBinding = -1;
        } else {
            super.keyPressed(chr, key);
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

