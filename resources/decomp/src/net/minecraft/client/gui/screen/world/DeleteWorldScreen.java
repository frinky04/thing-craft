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
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class DeleteWorldScreen
extends SelectWorldScreen {
    public DeleteWorldScreen(Screen screen) {
        super(screen);
        this.title = "Delete world";
    }

    public void addButtons() {
        this.buttons.add(new ButtonWidget(6, this.width / 2 - 100, this.height / 6 + 168, "Cancel"));
    }

    public void selectWorld(int id) {
        String string = this.getWorldName(id);
        if (string != null) {
            this.minecraft.openScreen(new ConfirmScreen(this, "Are you sure you want to delete this world?", "'" + string + "' will be lost forever!", id));
        }
    }

    public void confirmResult(boolean result, int id) {
        if (result) {
            File file = Minecraft.getWorkingDirectory();
            World.deleteWorld(file, this.getWorldName(id));
        }
        this.minecraft.openScreen(this.parent);
    }
}

