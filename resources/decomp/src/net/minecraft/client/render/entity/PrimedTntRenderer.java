/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.PrimedTntEntity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class PrimedTntRenderer
extends EntityRenderer {
    private BlockRenderer blockRenderer = new BlockRenderer();

    public PrimedTntRenderer() {
        this.shadowSize = 0.5f;
    }

    public void m_23395283(PrimedTntEntity primedTntEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        if ((float)primedTntEntity.fuseTimer - h + 1.0f < 10.0f) {
            float i = 1.0f - ((float)primedTntEntity.fuseTimer - h + 1.0f) / 10.0f;
            if (i < 0.0f) {
                i = 0.0f;
            }
            if (i > 1.0f) {
                i = 1.0f;
            }
            i *= i;
            i *= i;
            float k = 1.0f + i * 0.3f;
            GL11.glScalef((float)k, (float)k, (float)k);
        }
        float j = (1.0f - ((float)primedTntEntity.fuseTimer - h + 1.0f) / 100.0f) * 0.8f;
        this.bindTexture("/terrain.png");
        this.blockRenderer.renderAsItem(Block.TNT);
        if (primedTntEntity.fuseTimer / 5 % 2 == 0) {
            GL11.glDisable((int)GL11.GL_TEXTURE_2D);
            GL11.glDisable((int)GL11.GL_LIGHTING);
            GL11.glEnable((int)GL11.GL_BLEND);
            GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_DST_ALPHA);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)j);
            this.blockRenderer.renderAsItem(Block.TNT);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GL11.glDisable((int)GL11.GL_BLEND);
            GL11.glEnable((int)GL11.GL_LIGHTING);
            GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        }
        GL11.glPopMatrix();
    }
}

