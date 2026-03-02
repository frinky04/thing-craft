/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.util.glu.GLU
 */
package net.minecraft.client.gui.screen;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.JoinMultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TexturePacksScreen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

@Environment(value=EnvType.CLIENT)
public class TitleScreen
extends Screen {
    private static final Random RANDOM = new Random();
    String[] logo = new String[]{" *   * * *   * *** *** *** *** *** ***", " ** ** * **  * *   *   * * * * *    * ", " * * * * * * * **  *   **  *** **   * ", " *   * * *  ** *   *   * * * * *    * ", " *   * * *   * *** *** * * * * *    * "};
    private LetterBlock[][] letterBlocks;
    private float ticks = 0.0f;
    private String splashText = "missingno";

    public TitleScreen() {
        try {
            ArrayList<String> arrayList = new ArrayList<String>();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(TitleScreen.class.getResourceAsStream("/title/splashes.txt")));
            String string = "";
            while ((string = bufferedReader.readLine()) != null) {
                if ((string = string.trim()).length() <= 0) continue;
                arrayList.add(string);
            }
            this.splashText = (String)arrayList.get(RANDOM.nextInt(arrayList.size()));
        }
        catch (Exception object) {
            // empty catch block
        }
    }

    public void tick() {
        this.ticks += 1.0f;
        if (this.letterBlocks != null) {
            for (int i = 0; i < this.letterBlocks.length; ++i) {
                for (int j = 0; j < this.letterBlocks[i].length; ++j) {
                    this.letterBlocks[i][j].tick();
                }
            }
        }
    }

    protected void keyPressed(char chr, int key) {
    }

    public void init() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if (calendar.get(2) + 1 == 11 && calendar.get(5) == 9) {
            this.splashText = "Happy birthday, ez!";
        } else if (calendar.get(2) + 1 == 6 && calendar.get(5) == 1) {
            this.splashText = "Happy birthday, Notch!";
        } else if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24) {
            this.splashText = "Merry X-mas!";
        } else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1) {
            this.splashText = "Happy new year!";
        }
        this.buttons.add(new ButtonWidget(1, this.width / 2 - 100, this.height / 4 + 48, "Singleplayer"));
        this.buttons.add(new ButtonWidget(2, this.width / 2 - 100, this.height / 4 + 72, "Multiplayer"));
        this.buttons.add(new ButtonWidget(3, this.width / 2 - 100, this.height / 4 + 96, "Mods and Texture Packs"));
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height / 4 + 120 + 12, "Options..."));
        if (this.minecraft.session == null) {
            ((ButtonWidget)this.buttons.get((int)1)).active = false;
        }
    }

    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 0) {
            this.minecraft.openScreen(new OptionsScreen(this, this.minecraft.options));
        }
        if (button.id == 1) {
            this.minecraft.openScreen(new SelectWorldScreen(this));
        }
        if (button.id == 2) {
            this.minecraft.openScreen(new JoinMultiplayerScreen(this));
        }
        if (button.id == 3) {
            this.minecraft.openScreen(new TexturePacksScreen(this));
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        Tesselator tesselator = Tesselator.INSTANCE;
        this.renderLogo(tickDelta);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/logo.png"));
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        tesselator.color(0xFFFFFF);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)(this.width / 2 + 90), (float)70.0f, (float)0.0f);
        GL11.glRotatef((float)-20.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        float f = 1.8f - MathHelper.abs(MathHelper.sin((float)(System.currentTimeMillis() % 1000L) / 1000.0f * (float)Math.PI * 2.0f) * 0.1f);
        f = f * 100.0f / (float)(this.textRenderer.getWidth(this.splashText) + 32);
        GL11.glScalef((float)f, (float)f, (float)f);
        this.drawCenteredString(this.textRenderer, this.splashText, 0, -8, 0xFFFF00);
        GL11.glPopMatrix();
        this.drawString(this.textRenderer, "Minecraft Alpha v1.2.6", 2, 2, 0x505050);
        String string = "Copyright Mojang Specifications. Do not distribute.";
        this.drawString(this.textRenderer, string, this.width - this.textRenderer.getWidth(string) - 2, this.height - 10, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }

    private void renderLogo(float tickDelta) {
        if (this.letterBlocks == null) {
            this.letterBlocks = new LetterBlock[this.logo[0].length()][this.logo.length];
            for (int i = 0; i < this.letterBlocks.length; ++i) {
                for (int j = 0; j < this.letterBlocks[i].length; ++j) {
                    this.letterBlocks[i][j] = new LetterBlock(i, j);
                }
            }
        }
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        Window window = new Window(this.minecraft.width, this.minecraft.height);
        int k = 120 * window.scale;
        GLU.gluPerspective((float)70.0f, (float)((float)this.minecraft.width / (float)k), (float)0.05f, (float)100.0f);
        GL11.glViewport((int)0, (int)(this.minecraft.height - k), (int)this.minecraft.width, (int)k);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable((int)GL11.GL_CULL_FACE);
        GL11.glCullFace((int)GL11.GL_BACK);
        GL11.glDepthMask((boolean)true);
        BlockRenderer blockRenderer = new BlockRenderer();
        for (int l = 0; l < 3; ++l) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)0.4f, (float)0.6f, (float)-13.0f);
            if (l == 0) {
                GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glTranslatef((float)0.0f, (float)-0.4f, (float)0.0f);
                GL11.glScalef((float)0.98f, (float)1.0f, (float)1.0f);
                GL11.glEnable((int)GL11.GL_BLEND);
                GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            if (l == 1) {
                GL11.glDisable((int)GL11.GL_BLEND);
                GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
            }
            if (l == 2) {
                GL11.glEnable((int)GL11.GL_BLEND);
                GL11.glBlendFunc((int)GL11.GL_SRC_COLOR, (int)GL11.GL_ONE);
            }
            GL11.glScalef((float)1.0f, (float)-1.0f, (float)1.0f);
            GL11.glRotatef((float)15.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glScalef((float)0.89f, (float)1.0f, (float)0.4f);
            GL11.glTranslatef((float)((float)(-this.logo[0].length()) * 0.5f), (float)((float)(-this.logo.length) * 0.5f), (float)0.0f);
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/terrain.png"));
            if (l == 0) {
                GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/title/black.png"));
            }
            for (int m = 0; m < this.logo.length; ++m) {
                for (int n = 0; n < this.logo[m].length(); ++n) {
                    char o = this.logo[m].charAt(n);
                    if (o == ' ') continue;
                    GL11.glPushMatrix();
                    LetterBlock letterBlock = this.letterBlocks[n][m];
                    float f = (float)(letterBlock.lastY + (letterBlock.y - letterBlock.lastY) * (double)tickDelta);
                    float g = 1.0f;
                    float h = 1.0f;
                    float p = 0.0f;
                    if (l == 0) {
                        g = f * 0.04f + 1.0f;
                        h = 1.0f / g;
                        f = 0.0f;
                    }
                    GL11.glTranslatef((float)n, (float)m, (float)f);
                    GL11.glScalef((float)g, (float)g, (float)g);
                    GL11.glRotatef((float)p, (float)0.0f, (float)1.0f, (float)0.0f);
                    blockRenderer.tesselateGuiItem(Block.STONE, h);
                    GL11.glPopMatrix();
                }
            }
            GL11.glPopMatrix();
        }
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glViewport((int)0, (int)0, (int)this.minecraft.width, (int)this.minecraft.height);
        GL11.glEnable((int)GL11.GL_CULL_FACE);
    }

    @Environment(value=EnvType.CLIENT)
    class LetterBlock {
        public double y;
        public double lastY;
        public double deltaY;

        public LetterBlock(int y, int x) {
            this.y = this.lastY = (double)(10 + x) + RANDOM.nextDouble() * 32.0 + (double)y;
        }

        public void tick() {
            this.lastY = this.y;
            if (this.y > 0.0) {
                this.deltaY -= 0.6;
            }
            this.y += this.deltaY;
            this.deltaY *= 0.9;
            if (this.y < 0.0) {
                this.y = 0.0;
                this.deltaY = 0.0;
            }
        }
    }
}

