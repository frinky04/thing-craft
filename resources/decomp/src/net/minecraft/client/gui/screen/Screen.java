/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.screen;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class Screen
extends GuiElement {
    protected Minecraft minecraft;
    public int width;
    public int height;
    protected List buttons = new ArrayList();
    public boolean passEvents = false;
    protected TextRenderer textRenderer;
    private ButtonWidget lastClickedButton = null;

    public void render(int mouseX, int mouseY, float tickDelta) {
        for (int i = 0; i < this.buttons.size(); ++i) {
            ButtonWidget buttonWidget = (ButtonWidget)this.buttons.get(i);
            buttonWidget.render(this.minecraft, mouseX, mouseY);
        }
    }

    protected void keyPressed(char chr, int key) {
        if (key == 1) {
            this.minecraft.openScreen(null);
            this.minecraft.lockMouse();
        }
    }

    public static String getClipboard() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String string = (String)transferable.getTransferData(DataFlavor.stringFlavor);
                return string;
            }
        }
        catch (Exception object) {
            // empty catch block
        }
        return null;
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < this.buttons.size(); ++i) {
                ButtonWidget buttonWidget = (ButtonWidget)this.buttons.get(i);
                if (!buttonWidget.mouseClicked(this.minecraft, mouseX, mouseY)) continue;
                this.lastClickedButton = buttonWidget;
                this.minecraft.soundEngine.play("random.click", 1.0f, 1.0f);
                this.buttonClicked(buttonWidget);
            }
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (this.lastClickedButton != null && button == 0) {
            this.lastClickedButton.mouseReleased(mouseX, mouseY);
            this.lastClickedButton = null;
        }
    }

    protected void buttonClicked(ButtonWidget button) {
    }

    public void init(Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.textRenderer = minecraft.textRenderer;
        this.width = width;
        this.height = height;
        this.buttons.clear();
        this.init();
    }

    public void init() {
    }

    public void handleInputs() {
        while (Mouse.next()) {
            this.handleMouse();
        }
        while (Keyboard.next()) {
            this.handleKeyboard();
        }
    }

    public void handleMouse() {
        if (Mouse.getEventButtonState()) {
            int i = Mouse.getEventX() * this.width / this.minecraft.width;
            int k = this.height - Mouse.getEventY() * this.height / this.minecraft.height - 1;
            this.mouseClicked(i, k, Mouse.getEventButton());
        } else {
            int j = Mouse.getEventX() * this.width / this.minecraft.width;
            int l = this.height - Mouse.getEventY() * this.height / this.minecraft.height - 1;
            this.mouseReleased(j, l, Mouse.getEventButton());
        }
    }

    public void handleKeyboard() {
        if (Keyboard.getEventKeyState()) {
            if (Keyboard.getEventKey() == 87) {
                this.minecraft.toggleFullscreen();
                return;
            }
            this.keyPressed(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        }
    }

    public void tick() {
    }

    public void removed() {
    }

    public void renderBackground() {
        this.renderBackground(0);
    }

    public void renderBackground(int offset) {
        if (this.minecraft.world != null) {
            this.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        } else {
            this.drawBackgroundTexture(offset);
        }
    }

    public void drawBackgroundTexture(int offset) {
        GL11.glDisable((int)GL11.GL_LIGHTING);
        GL11.glDisable((int)GL11.GL_FOG);
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/background.png"));
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        float f = 32.0f;
        tesselator.begin();
        tesselator.color(0x404040);
        tesselator.vertex(0.0, this.height, 0.0, 0.0, (float)this.height / f + (float)offset);
        tesselator.vertex(this.width, this.height, 0.0, (float)this.width / f, (float)this.height / f + (float)offset);
        tesselator.vertex(this.width, 0.0, 0.0, (float)this.width / f, 0 + offset);
        tesselator.vertex(0.0, 0.0, 0.0, 0.0, 0 + offset);
        tesselator.end();
    }

    public boolean isPauseScreen() {
        return true;
    }

    public void confirmResult(boolean result, int id) {
    }
}

