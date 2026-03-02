/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatMessage;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class GameGui
extends GuiElement {
    private static ItemRenderer ITEM_RENDERER = new ItemRenderer();
    private List chatMessages = new ArrayList();
    private Random random = new Random();
    private Minecraft minecraft;
    public String selectedName = null;
    private int ticks = 0;
    private String overlayMessage = "";
    private int overlayMessageCooldown = 0;
    public float progress;
    float vignetteBrightness = 1.0f;

    public GameGui(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void render(float tickDelta, boolean screenOpen, int mouseX, int mouseY) {
        boolean k;
        float f;
        Window window = new Window(this.minecraft.width, this.minecraft.height);
        int i = window.getWidth();
        int j = window.getHeight();
        TextRenderer textRenderer = this.minecraft.textRenderer;
        this.minecraft.gameRenderer.setupGuiState();
        GL11.glEnable((int)GL11.GL_BLEND);
        if (this.minecraft.options.fancyGraphics) {
            this.renderVignette(this.minecraft.player.getBrightness(tickDelta), i, j);
        }
        ItemStack itemStack = this.minecraft.player.inventory.getArmor(3);
        if (!this.minecraft.options.perspective && itemStack != null && itemStack.id == Block.PUMPKIN.id) {
            this.renderPumpkinOverlay(i, j);
        }
        if ((f = this.minecraft.player.lastPortalTime + (this.minecraft.player.portalTime - this.minecraft.player.lastPortalTime) * tickDelta) > 0.0f) {
            this.renderPortalOverlay(f, i, j);
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/gui.png"));
        PlayerInventory playerInventory = this.minecraft.player.inventory;
        this.drawOffset = -90.0f;
        this.drawTexture(i / 2 - 91, j - 22, 0, 0, 182, 22);
        this.drawTexture(i / 2 - 91 - 1 + playerInventory.selectedSlot * 20, j - 22 - 1, 0, 22, 24, 22);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/icons.png"));
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_ONE_MINUS_DST_COLOR, (int)GL11.GL_ONE_MINUS_SRC_COLOR);
        this.drawTexture(i / 2 - 7, j / 2 - 7, 0, 0, 16, 16);
        GL11.glDisable((int)GL11.GL_BLEND);
        boolean bl = k = this.minecraft.player.invulnerableTimer / 3 % 2 == 1;
        if (this.minecraft.player.invulnerableTimer < 10) {
            k = false;
        }
        int l = this.minecraft.player.health;
        int m = this.minecraft.player.prevHealth;
        this.random.setSeed(this.ticks * 312871);
        if (this.minecraft.interactionManager.hasStatusBars()) {
            int s;
            int n = this.minecraft.player.getDisplayArmorProtection();
            for (s = 0; s < 10; ++s) {
                int w = j - 32;
                if (n > 0) {
                    int ac = i / 2 + 91 - s * 8 - 9;
                    if (s * 2 + 1 < n) {
                        this.drawTexture(ac, w, 34, 9, 9, 9);
                    }
                    if (s * 2 + 1 == n) {
                        this.drawTexture(ac, w, 25, 9, 9, 9);
                    }
                    if (s * 2 + 1 > n) {
                        this.drawTexture(ac, w, 16, 9, 9, 9);
                    }
                }
                int ad = 0;
                if (k) {
                    ad = 1;
                }
                int af = i / 2 - 91 + s * 8;
                if (l <= 4) {
                    w += this.random.nextInt(2);
                }
                this.drawTexture(af, w, 16 + ad * 9, 0, 9, 9);
                if (k) {
                    if (s * 2 + 1 < m) {
                        this.drawTexture(af, w, 70, 0, 9, 9);
                    }
                    if (s * 2 + 1 == m) {
                        this.drawTexture(af, w, 79, 0, 9, 9);
                    }
                }
                if (s * 2 + 1 < l) {
                    this.drawTexture(af, w, 52, 0, 9, 9);
                }
                if (s * 2 + 1 != l) continue;
                this.drawTexture(af, w, 61, 0, 9, 9);
            }
            if (this.minecraft.player.isSubmergedIn(Material.WATER)) {
                s = (int)Math.ceil((double)(this.minecraft.player.breath - 2) * 10.0 / 300.0);
                int x = (int)Math.ceil((double)this.minecraft.player.breath * 10.0 / 300.0) - s;
                for (int ae = 0; ae < s + x; ++ae) {
                    if (ae < s) {
                        this.drawTexture(i / 2 - 91 + ae * 8, j - 32 - 9, 16, 18, 9, 9);
                        continue;
                    }
                    this.drawTexture(i / 2 - 91 + ae * 8, j - 32 - 9, 25, 18, 9, 9);
                }
            }
        }
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glEnable((int)32826);
        GL11.glPushMatrix();
        GL11.glRotatef((float)180.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        Lighting.turnOn();
        GL11.glPopMatrix();
        for (int o = 0; o < 9; ++o) {
            int t = i / 2 - 90 + o * 20 + 2;
            int y = j - 16 - 3;
            this.renderInventorySlot(o, t, y, tickDelta);
        }
        Lighting.turnOff();
        GL11.glDisable((int)32826);
        if (Keyboard.isKeyDown((int)61)) {
            textRenderer.drawWithShadow("Minecraft Alpha v1.2.6 (" + this.minecraft.fpsDebugInfo + ")", 2, 2, 0xFFFFFF);
            textRenderer.drawWithShadow(this.minecraft.getRenderChunkDebugInfo(), 2, 12, 0xFFFFFF);
            textRenderer.drawWithShadow(this.minecraft.getRenderEntityDebugInfo(), 2, 22, 0xFFFFFF);
            textRenderer.drawWithShadow(this.minecraft.getWorldDebugInfo(), 2, 32, 0xFFFFFF);
            long p = Runtime.getRuntime().maxMemory();
            long z = Runtime.getRuntime().totalMemory();
            long ag = Runtime.getRuntime().freeMemory();
            long ai = z - ag;
            String string = "Used memory: " + ai * 100L / p + "% (" + ai / 1024L / 1024L + "MB) of " + p / 1024L / 1024L + "MB";
            this.drawString(textRenderer, string, i - textRenderer.getWidth(string) - 2, 2, 0xE0E0E0);
            string = "Allocated memory: " + z * 100L / p + "% (" + z / 1024L / 1024L + "MB)";
            this.drawString(textRenderer, string, i - textRenderer.getWidth(string) - 2, 12, 0xE0E0E0);
            this.drawString(textRenderer, "x: " + this.minecraft.player.x, 2, 64, 0xE0E0E0);
            this.drawString(textRenderer, "y: " + this.minecraft.player.y, 2, 72, 0xE0E0E0);
            this.drawString(textRenderer, "z: " + this.minecraft.player.z, 2, 80, 0xE0E0E0);
        } else {
            textRenderer.drawWithShadow("Minecraft Alpha v1.2.6", 2, 2, 0xFFFFFF);
        }
        if (this.overlayMessageCooldown > 0) {
            float g = (float)this.overlayMessageCooldown - tickDelta;
            int u = (int)(g * 256.0f / 20.0f);
            if (u > 255) {
                u = 255;
            }
            if (u > 0) {
                GL11.glPushMatrix();
                GL11.glTranslatef((float)(i / 2), (float)(j - 48), (float)0.0f);
                GL11.glEnable((int)GL11.GL_BLEND);
                GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
                int aa = Color.HSBtoRGB(g / 50.0f, 0.7f, 0.6f) & 0xFFFFFF;
                textRenderer.draw(this.overlayMessage, -textRenderer.getWidth(this.overlayMessage) / 2, -4, aa + (u << 24));
                GL11.glDisable((int)GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        }
        int r = 10;
        boolean v = false;
        if (this.minecraft.screen instanceof ChatScreen) {
            r = 20;
            v = true;
        }
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)0.0f, (float)(j - 48), (float)0.0f);
        for (int ab = 0; ab < this.chatMessages.size() && ab < r; ++ab) {
            if (((ChatMessage)this.chatMessages.get((int)ab)).age >= 200 && !v) continue;
            double d = (double)((ChatMessage)this.chatMessages.get((int)ab)).age / 200.0;
            d = 1.0 - d;
            if ((d *= 10.0) < 0.0) {
                d = 0.0;
            }
            if (d > 1.0) {
                d = 1.0;
            }
            d *= d;
            int ah = (int)(255.0 * d);
            if (v) {
                ah = 255;
            }
            if (ah <= 0) continue;
            int aj = 2;
            int ak = -ab * 9;
            String string2 = ((ChatMessage)this.chatMessages.get((int)ab)).text;
            this.fill(aj, ak - 1, aj + 320, ak + 8, ah / 2 << 24);
            GL11.glEnable((int)GL11.GL_BLEND);
            textRenderer.drawWithShadow(string2, aj, ak, 0xFFFFFF + (ah << 24));
        }
        GL11.glPopMatrix();
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glDisable((int)GL11.GL_BLEND);
    }

    private void renderPumpkinOverlay(int width, int height) {
        GL11.glDisable((int)GL11.GL_DEPTH_TEST);
        GL11.glDepthMask((boolean)false);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("%blur%/misc/pumpkinblur.png"));
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(0.0, height, -90.0, 0.0, 1.0);
        tesselator.vertex(width, height, -90.0, 1.0, 1.0);
        tesselator.vertex(width, 0.0, -90.0, 1.0, 0.0);
        tesselator.vertex(0.0, 0.0, -90.0, 0.0, 0.0);
        tesselator.end();
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private void renderVignette(float brightnessAtEyes, int width, int height) {
        if ((brightnessAtEyes = 1.0f - brightnessAtEyes) < 0.0f) {
            brightnessAtEyes = 0.0f;
        }
        if (brightnessAtEyes > 1.0f) {
            brightnessAtEyes = 1.0f;
        }
        this.vignetteBrightness = (float)((double)this.vignetteBrightness + (double)(brightnessAtEyes - this.vignetteBrightness) * 0.01);
        GL11.glDisable((int)GL11.GL_DEPTH_TEST);
        GL11.glDepthMask((boolean)false);
        GL11.glBlendFunc((int)GL11.GL_ZERO, (int)GL11.GL_ONE_MINUS_SRC_COLOR);
        GL11.glColor4f((float)this.vignetteBrightness, (float)this.vignetteBrightness, (float)this.vignetteBrightness, (float)1.0f);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("%blur%/misc/vignette.png"));
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(0.0, height, -90.0, 0.0, 1.0);
        tesselator.vertex(width, height, -90.0, 1.0, 1.0);
        tesselator.vertex(width, 0.0, -90.0, 1.0, 0.0);
        tesselator.vertex(0.0, 0.0, -90.0, 0.0, 0.0);
        tesselator.end();
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderPortalOverlay(float portalTime, int width, int height) {
        portalTime *= portalTime;
        portalTime *= portalTime;
        portalTime = portalTime * 0.8f + 0.2f;
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glDisable((int)GL11.GL_DEPTH_TEST);
        GL11.glDepthMask((boolean)false);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)portalTime);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/terrain.png"));
        float f = (float)(Block.NETHER_PORTAL.sprite % 16) / 16.0f;
        float g = (float)(Block.NETHER_PORTAL.sprite / 16) / 16.0f;
        float h = (float)(Block.NETHER_PORTAL.sprite % 16 + 1) / 16.0f;
        float i = (float)(Block.NETHER_PORTAL.sprite / 16 + 1) / 16.0f;
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(0.0, height, -90.0, f, i);
        tesselator.vertex(width, height, -90.0, h, i);
        tesselator.vertex(width, 0.0, -90.0, h, g);
        tesselator.vertex(0.0, 0.0, -90.0, f, g);
        tesselator.end();
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private void renderInventorySlot(int slot, int x, int z, float tickDelta) {
        ItemStack itemStack = this.minecraft.player.inventory.items[slot];
        if (itemStack == null) {
            return;
        }
        float f = (float)itemStack.popAnimationTime - tickDelta;
        if (f > 0.0f) {
            GL11.glPushMatrix();
            float g = 1.0f + f / 5.0f;
            GL11.glTranslatef((float)(x + 8), (float)(z + 12), (float)0.0f);
            GL11.glScalef((float)(1.0f / g), (float)((g + 1.0f) / 2.0f), (float)1.0f);
            GL11.glTranslatef((float)(-(x + 8)), (float)(-(z + 12)), (float)0.0f);
        }
        ITEM_RENDERER.renderGuiItem(this.minecraft.textRenderer, this.minecraft.textureManager, itemStack, x, z);
        if (f > 0.0f) {
            GL11.glPopMatrix();
        }
        ITEM_RENDERER.renderGuiItemDecoration(this.minecraft.textRenderer, this.minecraft.textureManager, itemStack, x, z);
    }

    public void tick() {
        if (this.overlayMessageCooldown > 0) {
            --this.overlayMessageCooldown;
        }
        ++this.ticks;
        for (int i = 0; i < this.chatMessages.size(); ++i) {
            ++((ChatMessage)this.chatMessages.get((int)i)).age;
        }
    }

    public void addChatMessage(String text) {
        while (this.minecraft.textRenderer.getWidth(text) > 320) {
            int i;
            for (i = 1; i < text.length() && this.minecraft.textRenderer.getWidth(text.substring(0, i + 1)) <= 320; ++i) {
            }
            this.addChatMessage(text.substring(0, i));
            text = text.substring(i);
        }
        this.chatMessages.add(0, new ChatMessage(text));
        while (this.chatMessages.size() > 50) {
            this.chatMessages.remove(this.chatMessages.size() - 1);
        }
    }

    public void setRecordPlayingOverlay(String record) {
        this.overlayMessage = "Now playing: " + record;
        this.overlayMessageCooldown = 60;
    }
}

