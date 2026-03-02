/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screen;

import java.net.ConnectException;
import java.net.UnknownHostException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.network.packet.HandshakePacket;

@Environment(value=EnvType.CLIENT)
public class ConnectScreen
extends Screen {
    private ClientNetworkHandler networkHandler;
    private boolean aborted = false;

    public ConnectScreen(final Minecraft minecraft, final String address, final int port) {
        minecraft.setWorld(null);
        new Thread(){

            public void run() {
                try {
                    ConnectScreen.this.networkHandler = new ClientNetworkHandler(minecraft, address, port);
                    if (ConnectScreen.this.aborted) {
                        return;
                    }
                    ConnectScreen.this.networkHandler.sendPacket(new HandshakePacket(minecraft.session.username));
                }
                catch (UnknownHostException unknownHostException) {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }
                    minecraft.openScreen(new DisconnectedScreen("Failed to connect to the server", "Unknown host '" + address + "'"));
                }
                catch (ConnectException connectException) {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }
                    minecraft.openScreen(new DisconnectedScreen("Failed to connect to the server", connectException.getMessage()));
                }
                catch (Exception exception) {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }
                    exception.printStackTrace();
                    minecraft.openScreen(new DisconnectedScreen("Failed to connect to the server", exception.toString()));
                }
            }
        }.start();
    }

    public void tick() {
        if (this.networkHandler != null) {
            this.networkHandler.tick();
        }
    }

    protected void keyPressed(char chr, int key) {
    }

    public void init() {
        this.buttons.clear();
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height / 4 + 120 + 12, "Cancel"));
    }

    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 0) {
            this.aborted = true;
            if (this.networkHandler != null) {
                this.networkHandler.disconnect();
            }
            this.minecraft.openScreen(new TitleScreen());
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        if (this.networkHandler == null) {
            this.drawCenteredString(this.textRenderer, "Connecting to the server...", this.width / 2, this.height / 2 - 50, 0xFFFFFF);
            this.drawCenteredString(this.textRenderer, "", this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        } else {
            this.drawCenteredString(this.textRenderer, "Logging in...", this.width / 2, this.height / 2 - 50, 0xFFFFFF);
            this.drawCenteredString(this.textRenderer, this.networkHandler.message, this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        }
        super.render(mouseX, mouseY, tickDelta);
    }
}

