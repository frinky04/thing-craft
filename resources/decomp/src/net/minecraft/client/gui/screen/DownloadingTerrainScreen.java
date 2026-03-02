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
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.packet.PingHostPacket;

@Environment(value=EnvType.CLIENT)
public class DownloadingTerrainScreen
extends Screen {
    private ClientNetworkHandler networkHandler;
    private int ticks = 0;

    public DownloadingTerrainScreen(ClientNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    protected void keyPressed(char chr, int key) {
    }

    public void init() {
        this.buttons.clear();
    }

    public void tick() {
        ++this.ticks;
        if (this.ticks % 20 == 0) {
            this.networkHandler.sendPacket(new PingHostPacket());
        }
        if (this.networkHandler != null) {
            this.networkHandler.tick();
        }
    }

    protected void buttonClicked(ButtonWidget button) {
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.drawBackgroundTexture(0);
        this.drawCenteredString(this.textRenderer, "Downloading terrain", this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }
}

