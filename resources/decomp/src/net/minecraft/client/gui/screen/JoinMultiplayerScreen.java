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
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

@Environment(value=EnvType.CLIENT)
public class JoinMultiplayerScreen
extends Screen {
    private Screen parent;
    private int ticks = 0;
    private String address = "";

    public JoinMultiplayerScreen(Screen parent) {
        this.parent = parent;
    }

    public void tick() {
        ++this.ticks;
    }

    public void init() {
        this.buttons.clear();
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height / 4 + 96 + 12, "Connect"));
        this.buttons.add(new ButtonWidget(1, this.width / 2 - 100, this.height / 4 + 120 + 12, "Cancel"));
        this.address = this.minecraft.options.lastServer.replaceAll("_", ":");
        ((ButtonWidget)this.buttons.get((int)0)).active = this.address.length() > 0;
    }

    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        if (button.id == 1) {
            this.minecraft.openScreen(this.parent);
        } else if (button.id == 0) {
            this.minecraft.options.lastServer = this.address.replaceAll(":", "_");
            this.minecraft.options.save();
            String[] strings = this.address.split(":");
            this.minecraft.openScreen(new ConnectScreen(this.minecraft, strings[0], strings.length > 1 ? this.parseInt(strings[1], 25565) : 25565));
        }
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        }
        catch (Exception exception) {
            return defaultValue;
        }
    }

    protected void keyPressed(char chr, int key) {
        if (chr == '\u0016') {
            int i;
            String string = Screen.getClipboard();
            if (string == null) {
                string = "";
            }
            if ((i = 32 - this.address.length()) > string.length()) {
                i = string.length();
            }
            if (i > 0) {
                this.address = this.address + string.substring(0, i);
            }
        }
        if (chr == '\r') {
            this.buttonClicked((ButtonWidget)this.buttons.get(0));
        }
        if (key == 14 && this.address.length() > 0) {
            this.address = this.address.substring(0, this.address.length() - 1);
        }
        if (" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb".indexOf(chr) >= 0 && this.address.length() < 32) {
            this.address = this.address + chr;
        }
        ((ButtonWidget)this.buttons.get((int)0)).active = this.address.length() > 0;
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, "Play Multiplayer", this.width / 2, this.height / 4 - 60 + 20, 0xFFFFFF);
        this.drawString(this.textRenderer, "Minecraft Multiplayer is currently not finished, but there", this.width / 2 - 140, this.height / 4 - 60 + 60 + 0, 0xA0A0A0);
        this.drawString(this.textRenderer, "is some buggy early testing going on.", this.width / 2 - 140, this.height / 4 - 60 + 60 + 9, 0xA0A0A0);
        this.drawString(this.textRenderer, "Enter the IP of a server to connect to it:", this.width / 2 - 140, this.height / 4 - 60 + 60 + 36, 0xA0A0A0);
        int i = this.width / 2 - 100;
        int j = this.height / 4 - 10 + 50 + 18;
        int k = 200;
        int l = 20;
        this.fill(i - 1, j - 1, i + k + 1, j + l + 1, -6250336);
        this.fill(i, j, i + k, j + l, -16777216);
        this.drawString(this.textRenderer, this.address + (this.ticks / 6 % 2 == 0 ? "_" : ""), i + 4, j + (l - 8) / 2, 0xE0E0E0);
        super.render(mouseX, mouseY, tickDelta);
    }
}

