/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.block.entity.SignModel;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class SignRenderer
extends BlockEntityRenderer {
    private SignModel model = new SignModel();

    public void m_07188493(SignBlockEntity signBlockEntity, double d, double e, double f, float g) {
        Block block = signBlockEntity.getBlock();
        GL11.glPushMatrix();
        float h = 0.6666667f;
        if (block == Block.STANDING_SIGN) {
            GL11.glTranslatef((float)((float)d + 0.5f), (float)((float)e + 0.75f * h), (float)((float)f + 0.5f));
            float i = (float)(signBlockEntity.getBlockMetadata() * 360) / 16.0f;
            GL11.glRotatef((float)(-i), (float)0.0f, (float)1.0f, (float)0.0f);
            this.model.pole.visible = true;
        } else {
            int j = signBlockEntity.getBlockMetadata();
            float k = 0.0f;
            if (j == 2) {
                k = 180.0f;
            }
            if (j == 4) {
                k = 90.0f;
            }
            if (j == 5) {
                k = -90.0f;
            }
            GL11.glTranslatef((float)((float)d + 0.5f), (float)((float)e + 0.75f * h), (float)((float)f + 0.5f));
            GL11.glRotatef((float)(-k), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glTranslatef((float)0.0f, (float)-0.3125f, (float)-0.4375f);
            this.model.pole.visible = false;
        }
        this.bindTexture("/item/sign.png");
        GL11.glPushMatrix();
        GL11.glScalef((float)h, (float)(-h), (float)(-h));
        this.model.render();
        GL11.glPopMatrix();
        TextRenderer textRenderer = this.getTextRenderer();
        float l = 0.016666668f * h;
        GL11.glTranslatef((float)0.0f, (float)(0.5f * h), (float)(0.07f * h));
        GL11.glScalef((float)l, (float)(-l), (float)l);
        GL11.glNormal3f((float)0.0f, (float)0.0f, (float)(-1.0f * l));
        GL11.glDepthMask((boolean)false);
        int m = 0;
        for (int n = 0; n < signBlockEntity.lines.length; ++n) {
            String string = signBlockEntity.lines[n];
            if (n == signBlockEntity.currentRow) {
                string = "> " + string + " <";
                textRenderer.draw(string, -textRenderer.getWidth(string) / 2, n * 10 - signBlockEntity.lines.length * 5, m);
                continue;
            }
            textRenderer.draw(string, -textRenderer.getWidth(string) / 2, n * 10 - signBlockEntity.lines.length * 5, m);
        }
        GL11.glDepthMask((boolean)true);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glPopMatrix();
    }
}

