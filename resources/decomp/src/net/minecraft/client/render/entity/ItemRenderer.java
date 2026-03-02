/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ItemRenderer
extends EntityRenderer {
    private BlockRenderer blockRenderer = new BlockRenderer();
    private Random random = new Random();

    public ItemRenderer() {
        this.shadowSize = 0.15f;
        this.shadowDarkness = 0.75f;
    }

    public void m_23040289(ItemEntity itemEntity, double d, double e, double f, float g, float h) {
        this.random.setSeed(187L);
        ItemStack itemStack = itemEntity.item;
        GL11.glPushMatrix();
        float i = MathHelper.sin(((float)itemEntity.age + h) / 10.0f + itemEntity.bobOffset) * 0.1f + 0.1f;
        float j = (((float)itemEntity.age + h) / 20.0f + itemEntity.bobOffset) * 57.295776f;
        int k = 1;
        if (itemEntity.item.size > 1) {
            k = 2;
        }
        if (itemEntity.item.size > 5) {
            k = 3;
        }
        if (itemEntity.item.size > 20) {
            k = 4;
        }
        GL11.glTranslatef((float)((float)d), (float)((float)e + i), (float)((float)f));
        GL11.glEnable((int)32826);
        if (itemStack.id < 256 && BlockRenderer.isItem3d(Block.BY_ID[itemStack.id].getRenderType())) {
            GL11.glRotatef((float)j, (float)0.0f, (float)1.0f, (float)0.0f);
            this.bindTexture("/terrain.png");
            float l = 0.25f;
            if (!Block.BY_ID[itemStack.id].isCube() && itemStack.id != Block.STONE_SLAB.id) {
                l = 0.5f;
            }
            GL11.glScalef((float)l, (float)l, (float)l);
            for (int n = 0; n < k; ++n) {
                GL11.glPushMatrix();
                if (n > 0) {
                    float o = (this.random.nextFloat() * 2.0f - 1.0f) * 0.2f / l;
                    float q = (this.random.nextFloat() * 2.0f - 1.0f) * 0.2f / l;
                    float s = (this.random.nextFloat() * 2.0f - 1.0f) * 0.2f / l;
                    GL11.glTranslatef((float)o, (float)q, (float)s);
                }
                this.blockRenderer.renderAsItem(Block.BY_ID[itemStack.id]);
                GL11.glPopMatrix();
            }
        } else {
            GL11.glScalef((float)0.5f, (float)0.5f, (float)0.5f);
            int m = itemStack.getSprite();
            if (itemStack.id < 256) {
                this.bindTexture("/terrain.png");
            } else {
                this.bindTexture("/gui/items.png");
            }
            Tesselator tesselator = Tesselator.INSTANCE;
            float p = (float)(m % 16 * 16 + 0) / 256.0f;
            float r = (float)(m % 16 * 16 + 16) / 256.0f;
            float t = (float)(m / 16 * 16 + 0) / 256.0f;
            float u = (float)(m / 16 * 16 + 16) / 256.0f;
            float v = 1.0f;
            float w = 0.5f;
            float x = 0.25f;
            for (int y = 0; y < k; ++y) {
                GL11.glPushMatrix();
                if (y > 0) {
                    float z = (this.random.nextFloat() * 2.0f - 1.0f) * 0.3f;
                    float aa = (this.random.nextFloat() * 2.0f - 1.0f) * 0.3f;
                    float ab = (this.random.nextFloat() * 2.0f - 1.0f) * 0.3f;
                    GL11.glTranslatef((float)z, (float)aa, (float)ab);
                }
                GL11.glRotatef((float)(180.0f - this.dispatcher.cameraYaw), (float)0.0f, (float)1.0f, (float)0.0f);
                tesselator.begin();
                tesselator.normal(0.0f, 1.0f, 0.0f);
                tesselator.vertex(0.0f - w, 0.0f - x, 0.0, p, u);
                tesselator.vertex(v - w, 0.0f - x, 0.0, r, u);
                tesselator.vertex(v - w, 1.0f - x, 0.0, r, t);
                tesselator.vertex(0.0f - w, 1.0f - x, 0.0, p, t);
                tesselator.end();
                GL11.glPopMatrix();
            }
        }
        GL11.glDisable((int)32826);
        GL11.glPopMatrix();
    }

    public void renderGuiItem(TextRenderer textRenderer, TextureManager textureManager, ItemStack item, int x, int y) {
        if (item == null) {
            return;
        }
        if (item.id < 256 && BlockRenderer.isItem3d(Block.BY_ID[item.id].getRenderType())) {
            int i = item.id;
            textureManager.bind(textureManager.load("/terrain.png"));
            Block block = Block.BY_ID[i];
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(x - 2), (float)(y + 3), (float)0.0f);
            GL11.glScalef((float)10.0f, (float)10.0f, (float)10.0f);
            GL11.glTranslatef((float)1.0f, (float)0.5f, (float)8.0f);
            GL11.glRotatef((float)210.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GL11.glScalef((float)1.0f, (float)1.0f, (float)1.0f);
            this.blockRenderer.renderAsItem(block);
            GL11.glPopMatrix();
        } else if (item.getSprite() >= 0) {
            GL11.glDisable((int)GL11.GL_LIGHTING);
            if (item.id < 256) {
                textureManager.bind(textureManager.load("/terrain.png"));
            } else {
                textureManager.bind(textureManager.load("/gui/items.png"));
            }
            this.drawTexture(x, y, item.getSprite() % 16 * 16, item.getSprite() / 16 * 16, 16, 16);
            GL11.glEnable((int)GL11.GL_LIGHTING);
        }
        GL11.glEnable((int)GL11.GL_CULL_FACE);
    }

    public void renderGuiItemDecoration(TextRenderer textRenderer, TextureManager textureManager, ItemStack item, int x, int y) {
        if (item == null) {
            return;
        }
        if (item.size > 1) {
            String string = "" + item.size;
            GL11.glDisable((int)GL11.GL_LIGHTING);
            GL11.glDisable((int)GL11.GL_DEPTH_TEST);
            textRenderer.drawWithShadow(string, x + 19 - 2 - textRenderer.getWidth(string), y + 6 + 3, 0xFFFFFF);
            GL11.glEnable((int)GL11.GL_LIGHTING);
            GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        }
        if (item.metadata > 0) {
            int i = 13 - item.metadata * 13 / item.getMaxDamage();
            int j = 255 - item.metadata * 255 / item.getMaxDamage();
            GL11.glDisable((int)GL11.GL_LIGHTING);
            GL11.glDisable((int)GL11.GL_DEPTH_TEST);
            GL11.glDisable((int)GL11.GL_TEXTURE_2D);
            Tesselator tesselator = Tesselator.INSTANCE;
            int k = 255 - j << 16 | j << 8;
            int l = (255 - j) / 4 << 16 | 0x3F00;
            this.fillRect(tesselator, x + 2, y + 13, 13, 2, 0);
            this.fillRect(tesselator, x + 2, y + 13, 12, 1, l);
            this.fillRect(tesselator, x + 2, y + 13, i, 1, k);
            GL11.glEnable((int)GL11.GL_TEXTURE_2D);
            GL11.glEnable((int)GL11.GL_LIGHTING);
            GL11.glEnable((int)GL11.GL_DEPTH_TEST);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        }
    }

    private void fillRect(Tesselator tesselator, int x, int y, int width, int height, int color) {
        tesselator.begin();
        tesselator.color(color);
        tesselator.vertex(x + 0, y + 0, 0.0);
        tesselator.vertex(x + 0, y + height, 0.0);
        tesselator.vertex(x + width, y + height, 0.0);
        tesselator.vertex(x + width, y + 0, 0.0);
        tesselator.end();
    }

    public void drawTexture(int x, int y, int u, int v, int width, int height) {
        float f = 0.0f;
        float g = 0.00390625f;
        float h = 0.00390625f;
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(x + 0, y + height, f, (float)(u + 0) * g, (float)(v + height) * h);
        tesselator.vertex(x + width, y + height, f, (float)(u + width) * g, (float)(v + height) * h);
        tesselator.vertex(x + width, y + 0, f, (float)(u + width) * g, (float)(v + 0) * h);
        tesselator.vertex(x + 0, y + 0, f, (float)(u + 0) * g, (float)(v + 0) * h);
        tesselator.end();
    }
}

