/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.input.Keyboard
 */
package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

@Environment(value=EnvType.CLIENT)
public class ChatScreen
extends Screen {
    private String lastChatMessage = "";
    private int ticks = 0;

    public void init() {
        Keyboard.enableRepeatEvents((boolean)true);
    }

    public void removed() {
        Keyboard.enableRepeatEvents((boolean)false);
    }

    public void tick() {
        ++this.ticks;
    }

    protected void keyPressed(char chr, int key) {
        if (key == 1) {
            this.minecraft.openScreen(null);
            return;
        }
        if (key == 28) {
            String string = this.lastChatMessage.trim();
            if (string.length() > 0) {
                this.minecraft.player.sendChat(this.lastChatMessage.trim());
            }
            this.minecraft.openScreen(null);
            return;
        }
        if (key == 14 && this.lastChatMessage.length() > 0) {
            this.lastChatMessage = this.lastChatMessage.substring(0, this.lastChatMessage.length() - 1);
        }
        if (" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb".indexOf(chr) >= 0 && this.lastChatMessage.length() < 100) {
            this.lastChatMessage = this.lastChatMessage + chr;
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.fill(2, this.height - 14, this.width - 2, this.height - 2, Integer.MIN_VALUE);
        this.drawString(this.textRenderer, "> " + this.lastChatMessage + (this.ticks / 6 % 2 == 0 ? "_" : ""), 4, this.height - 12, 0xE0E0E0);
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && this.minecraft.gui.selectedName != null) {
            if (this.lastChatMessage.length() > 0 && !this.lastChatMessage.endsWith(" ")) {
                this.lastChatMessage = this.lastChatMessage + " ";
            }
            this.lastChatMessage = this.lastChatMessage + this.minecraft.gui.selectedName;
            int i = 100;
            if (this.lastChatMessage.length() > i) {
                this.lastChatMessage = this.lastChatMessage.substring(0, i);
            }
        }
    }
}

