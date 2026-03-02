/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.screen.inventory.menu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class SignEditScreen
extends Screen {
    protected String title = "Edit sign message:";
    private SignBlockEntity sign;
    private int ticks;
    private int row = 0;

    public SignEditScreen(SignBlockEntity sign) {
        this.sign = sign;
    }

    public void init() {
        this.buttons.clear();
        Keyboard.enableRepeatEvents((boolean)true);
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height / 4 + 120, "Done"));
    }

    public void removed() {
        Keyboard.enableRepeatEvents((boolean)false);
    }

    public void tick() {
        ++this.ticks;
    }

    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        if (button.id == 0) {
            this.sign.markDirty();
            this.minecraft.openScreen(null);
        }
    }

    protected void keyPressed(char chr, int key) {
        if (key == 200) {
            this.row = this.row - 1 & 3;
        }
        if (key == 208 || key == 28) {
            this.row = this.row + 1 & 3;
        }
        if (key == 14 && this.sign.lines[this.row].length() > 0) {
            this.sign.lines[this.row] = this.sign.lines[this.row].substring(0, this.sign.lines[this.row].length() - 1);
        }
        if (" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb".indexOf(chr) >= 0 && this.sign.lines[this.row].length() < 15) {
            int n = this.row;
            this.sign.lines[n] = this.sign.lines[n] + chr;
        }
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFF);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)(this.width / 2), (float)(this.height / 2), (float)50.0f);
        float f = 93.75f;
        GL11.glScalef((float)(-f), (float)(-f), (float)(-f));
        GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        Block block = this.sign.getBlock();
        if (block == Block.STANDING_SIGN) {
            float g = (float)(this.sign.getBlockMetadata() * 360) / 16.0f;
            GL11.glRotatef((float)g, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glTranslatef((float)0.0f, (float)0.3125f, (float)0.0f);
        } else {
            int i = this.sign.getBlockMetadata();
            float h = 0.0f;
            if (i == 2) {
                h = 180.0f;
            }
            if (i == 4) {
                h = 90.0f;
            }
            if (i == 5) {
                h = -90.0f;
            }
            GL11.glRotatef((float)h, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glTranslatef((float)0.0f, (float)0.3125f, (float)0.0f);
        }
        if (this.ticks / 6 % 2 == 0) {
            this.sign.currentRow = this.row;
        }
        BlockEntityRenderDispatcher.INSTANCE.render(this.sign, -0.5, -0.75, -0.5, 0.0f);
        this.sign.currentRow = -1;
        GL11.glPopMatrix();
        super.render(mouseX, mouseY, tickDelta);
    }
}

