/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screen.world;

import java.io.File;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SurvivalInteractionManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.DeleteWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class SelectWorldScreen
extends Screen {
    protected Screen parent;
    protected String title = "Select world";
    private boolean selected = false;

    public SelectWorldScreen(Screen parent) {
        this.parent = parent;
    }

    public void init() {
        File file = Minecraft.getWorkingDirectory();
        for (int i = 0; i < 5; ++i) {
            NbtCompound nbtCompound = World.getWorldData(file, "World" + (i + 1));
            if (nbtCompound == null) {
                this.buttons.add(new ButtonWidget(i, this.width / 2 - 100, this.height / 6 + 24 * i, "- empty -"));
                continue;
            }
            String string = "World " + (i + 1);
            long l = nbtCompound.getLong("SizeOnDisk");
            string = string + " (" + (float)(l / 1024L * 100L / 1024L) / 100.0f + " MB)";
            this.buttons.add(new ButtonWidget(i, this.width / 2 - 100, this.height / 6 + 24 * i, string));
        }
        this.addButtons();
    }

    protected String getWorldName(int index) {
        File file = Minecraft.getWorkingDirectory();
        return World.getWorldData(file, "World" + index) != null ? "World" + index : null;
    }

    public void addButtons() {
        this.buttons.add(new ButtonWidget(5, this.width / 2 - 100, this.height / 6 + 120 + 12, "Delete world..."));
        this.buttons.add(new ButtonWidget(6, this.width / 2 - 100, this.height / 6 + 168, "Cancel"));
    }

    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        if (button.id < 5) {
            this.selectWorld(button.id + 1);
        } else if (button.id == 5) {
            this.minecraft.openScreen(new DeleteWorldScreen(this));
        } else if (button.id == 6) {
            this.minecraft.openScreen(this.parent);
        }
    }

    public void selectWorld(int id) {
        this.minecraft.openScreen(null);
        if (this.selected) {
            return;
        }
        this.selected = true;
        this.minecraft.interactionManager = new SurvivalInteractionManager(this.minecraft);
        this.minecraft.startGame("World" + id);
        this.minecraft.openScreen(null);
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

