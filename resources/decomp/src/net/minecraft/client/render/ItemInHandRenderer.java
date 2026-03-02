/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ItemInHandRenderer {
    private Minecraft minecraft;
    private ItemStack itemInHand = null;
    private float handHeight = 0.0f;
    private float lastHandHeight = 0.0f;
    private BlockRenderer blockRenderer = new BlockRenderer();

    public ItemInHandRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void render(ItemStack item) {
        GL11.glPushMatrix();
        if (item.id < 256 && BlockRenderer.isItem3d(Block.BY_ID[item.id].getRenderType())) {
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/terrain.png"));
            this.blockRenderer.renderAsItem(Block.BY_ID[item.id]);
        } else {
            int o;
            if (item.id < 256) {
                GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/terrain.png"));
            } else {
                GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/gui/items.png"));
            }
            Tesselator tesselator = Tesselator.INSTANCE;
            float f = ((float)(item.getSprite() % 16 * 16) + 0.0f) / 256.0f;
            float g = ((float)(item.getSprite() % 16 * 16) + 15.99f) / 256.0f;
            float h = ((float)(item.getSprite() / 16 * 16) + 0.0f) / 256.0f;
            float i = ((float)(item.getSprite() / 16 * 16) + 15.99f) / 256.0f;
            float j = 1.0f;
            float k = 0.0f;
            float l = 0.3f;
            GL11.glEnable((int)32826);
            GL11.glTranslatef((float)(-k), (float)(-l), (float)0.0f);
            float m = 1.5f;
            GL11.glScalef((float)m, (float)m, (float)m);
            GL11.glRotatef((float)50.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)335.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glTranslatef((float)-0.9375f, (float)-0.0625f, (float)0.0f);
            float n = 0.0625f;
            tesselator.begin();
            tesselator.normal(0.0f, 0.0f, 1.0f);
            tesselator.vertex(0.0, 0.0, 0.0, g, i);
            tesselator.vertex(j, 0.0, 0.0, f, i);
            tesselator.vertex(j, 1.0, 0.0, f, h);
            tesselator.vertex(0.0, 1.0, 0.0, g, h);
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 0.0f, -1.0f);
            tesselator.vertex(0.0, 1.0, 0.0f - n, g, h);
            tesselator.vertex(j, 1.0, 0.0f - n, f, h);
            tesselator.vertex(j, 0.0, 0.0f - n, f, i);
            tesselator.vertex(0.0, 0.0, 0.0f - n, g, i);
            tesselator.end();
            tesselator.begin();
            tesselator.normal(-1.0f, 0.0f, 0.0f);
            for (o = 0; o < 16; ++o) {
                float p = (float)o / 16.0f;
                float t = g + (f - g) * p - 0.001953125f;
                float x = j * p;
                tesselator.vertex(x, 0.0, 0.0f - n, t, i);
                tesselator.vertex(x, 0.0, 0.0, t, i);
                tesselator.vertex(x, 1.0, 0.0, t, h);
                tesselator.vertex(x, 1.0, 0.0f - n, t, h);
            }
            tesselator.end();
            tesselator.begin();
            tesselator.normal(1.0f, 0.0f, 0.0f);
            for (o = 0; o < 16; ++o) {
                float q = (float)o / 16.0f;
                float u = g + (f - g) * q - 0.001953125f;
                float y = j * q + 0.0625f;
                tesselator.vertex(y, 1.0, 0.0f - n, u, h);
                tesselator.vertex(y, 1.0, 0.0, u, h);
                tesselator.vertex(y, 0.0, 0.0, u, i);
                tesselator.vertex(y, 0.0, 0.0f - n, u, i);
            }
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 1.0f, 0.0f);
            for (o = 0; o < 16; ++o) {
                float r = (float)o / 16.0f;
                float v = i + (h - i) * r - 0.001953125f;
                float z = j * r + 0.0625f;
                tesselator.vertex(0.0, z, 0.0, g, v);
                tesselator.vertex(j, z, 0.0, f, v);
                tesselator.vertex(j, z, 0.0f - n, f, v);
                tesselator.vertex(0.0, z, 0.0f - n, g, v);
            }
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, -1.0f, 0.0f);
            for (o = 0; o < 16; ++o) {
                float s = (float)o / 16.0f;
                float w = i + (h - i) * s - 0.001953125f;
                float aa = j * s;
                tesselator.vertex(j, aa, 0.0, f, w);
                tesselator.vertex(0.0, aa, 0.0, g, w);
                tesselator.vertex(0.0, aa, 0.0f - n, g, w);
                tesselator.vertex(j, aa, 0.0f - n, f, w);
            }
            tesselator.end();
            GL11.glDisable((int)32826);
        }
        GL11.glPopMatrix();
    }

    public void renderHand(float tickDelta) {
        float f = this.lastHandHeight + (this.handHeight - this.lastHandHeight) * tickDelta;
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        GL11.glPushMatrix();
        GL11.glRotatef((float)(clientPlayerEntity.lastPitch + (clientPlayerEntity.pitch - clientPlayerEntity.lastPitch) * tickDelta), (float)1.0f, (float)0.0f, (float)0.0f);
        GL11.glRotatef((float)(clientPlayerEntity.lastYaw + (clientPlayerEntity.yaw - clientPlayerEntity.lastYaw) * tickDelta), (float)0.0f, (float)1.0f, (float)0.0f);
        Lighting.turnOn();
        GL11.glPopMatrix();
        float g = this.minecraft.world.getBrightness(MathHelper.floor(clientPlayerEntity.x), MathHelper.floor(clientPlayerEntity.y), MathHelper.floor(clientPlayerEntity.z));
        GL11.glColor4f((float)g, (float)g, (float)g, (float)1.0f);
        ItemStack itemStack = this.itemInHand;
        if (clientPlayerEntity.fishingBobber != null) {
            itemStack = new ItemStack(Item.STICK.id);
        }
        if (itemStack != null) {
            GL11.glPushMatrix();
            float h = 0.8f;
            float j = clientPlayerEntity.getAttackAnimationProgress(tickDelta);
            float l = MathHelper.sin(j * (float)Math.PI);
            float n = MathHelper.sin(MathHelper.sqrt(j) * (float)Math.PI);
            GL11.glTranslatef((float)(-n * 0.4f), (float)(MathHelper.sin(MathHelper.sqrt(j) * (float)Math.PI * 2.0f) * 0.2f), (float)(-l * 0.2f));
            GL11.glTranslatef((float)(0.7f * h), (float)(-0.65f * h - (1.0f - f) * 0.6f), (float)(-0.9f * h));
            GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glEnable((int)32826);
            j = clientPlayerEntity.getAttackAnimationProgress(tickDelta);
            l = MathHelper.sin(j * j * (float)Math.PI);
            n = MathHelper.sin(MathHelper.sqrt(j) * (float)Math.PI);
            GL11.glRotatef((float)(-l * 20.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)(-n * 20.0f), (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glRotatef((float)(-n * 80.0f), (float)1.0f, (float)0.0f, (float)0.0f);
            j = 0.4f;
            GL11.glScalef((float)j, (float)j, (float)j);
            if (itemStack.getItem().shouldRotate()) {
                GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            }
            this.render(itemStack);
            GL11.glPopMatrix();
        } else {
            GL11.glPushMatrix();
            float i = 0.8f;
            float k = clientPlayerEntity.getAttackAnimationProgress(tickDelta);
            float m = MathHelper.sin(k * (float)Math.PI);
            float o = MathHelper.sin(MathHelper.sqrt(k) * (float)Math.PI);
            GL11.glTranslatef((float)(-o * 0.3f), (float)(MathHelper.sin(MathHelper.sqrt(k) * (float)Math.PI * 2.0f) * 0.4f), (float)(-m * 0.4f));
            GL11.glTranslatef((float)(0.8f * i), (float)(-0.75f * i - (1.0f - f) * 0.6f), (float)(-0.9f * i));
            GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glEnable((int)32826);
            k = clientPlayerEntity.getAttackAnimationProgress(tickDelta);
            m = MathHelper.sin(k * k * (float)Math.PI);
            o = MathHelper.sin(MathHelper.sqrt(k) * (float)Math.PI);
            GL11.glRotatef((float)(o * 70.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)(-m * 20.0f), (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.loadHttpTexture(this.minecraft.player.skin, this.minecraft.player.getTexture()));
            GL11.glTranslatef((float)-1.0f, (float)3.6f, (float)3.5f);
            GL11.glRotatef((float)120.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glRotatef((float)200.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)-135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glScalef((float)1.0f, (float)1.0f, (float)1.0f);
            GL11.glTranslatef((float)5.6f, (float)0.0f, (float)0.0f);
            EntityRenderer entityRenderer = EntityRenderDispatcher.INSTANCE.getRenderer(this.minecraft.player);
            PlayerRenderer playerRenderer = (PlayerRenderer)entityRenderer;
            o = 1.0f;
            GL11.glScalef((float)o, (float)o, (float)o);
            playerRenderer.renderRightHand();
            GL11.glPopMatrix();
        }
        GL11.glDisable((int)32826);
        Lighting.turnOff();
    }

    public void renderScreenEffects(float tickDelta) {
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        if (this.minecraft.player.onFireTimer > 0 || this.minecraft.player.onFire) {
            int i = this.minecraft.textureManager.load("/terrain.png");
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)i);
            this.renderOnFireEffect(tickDelta);
        }
        if (this.minecraft.player.isInWall()) {
            int j = MathHelper.floor(this.minecraft.player.x);
            int l = MathHelper.floor(this.minecraft.player.y);
            int m = MathHelper.floor(this.minecraft.player.z);
            int n = this.minecraft.textureManager.load("/terrain.png");
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)n);
            int o = this.minecraft.world.getBlock(j, l, m);
            if (Block.BY_ID[o] != null) {
                this.renderInWallEffect(tickDelta, Block.BY_ID[o].getSprite(2));
            }
        }
        if (this.minecraft.player.isSubmergedIn(Material.WATER)) {
            int k = this.minecraft.textureManager.load("/misc/water.png");
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)k);
            this.renderInWaterEffect(tickDelta);
        }
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
    }

    private void renderInWallEffect(float tickDelta, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        float f = this.minecraft.player.getBrightness(tickDelta);
        f = 0.1f;
        GL11.glColor4f((float)f, (float)f, (float)f, (float)0.5f);
        GL11.glPushMatrix();
        float g = -1.0f;
        float h = 1.0f;
        float i = -1.0f;
        float j = 1.0f;
        float k = -0.5f;
        float l = 0.0078125f;
        float m = (float)(sprite % 16) / 256.0f - l;
        float n = ((float)(sprite % 16) + 15.99f) / 256.0f + l;
        float o = (float)(sprite / 16) / 256.0f - l;
        float p = ((float)(sprite / 16) + 15.99f) / 256.0f + l;
        tesselator.begin();
        tesselator.vertex(g, i, k, n, p);
        tesselator.vertex(h, i, k, m, p);
        tesselator.vertex(h, j, k, m, o);
        tesselator.vertex(g, j, k, n, o);
        tesselator.end();
        GL11.glPopMatrix();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private void renderInWaterEffect(float tickDelta) {
        Tesselator tesselator = Tesselator.INSTANCE;
        float f = this.minecraft.player.getBrightness(tickDelta);
        GL11.glColor4f((float)f, (float)f, (float)f, (float)0.5f);
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        float g = 4.0f;
        float h = -1.0f;
        float i = 1.0f;
        float j = -1.0f;
        float k = 1.0f;
        float l = -0.5f;
        float m = -this.minecraft.player.yaw / 64.0f;
        float n = this.minecraft.player.pitch / 64.0f;
        tesselator.begin();
        tesselator.vertex(h, j, l, g + m, g + n);
        tesselator.vertex(i, j, l, 0.0f + m, g + n);
        tesselator.vertex(i, k, l, 0.0f + m, 0.0f + n);
        tesselator.vertex(h, k, l, g + m, 0.0f + n);
        tesselator.end();
        GL11.glPopMatrix();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_BLEND);
    }

    private void renderOnFireEffect(float tickDelta) {
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)0.9f);
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        float f = 1.0f;
        for (int i = 0; i < 2; ++i) {
            GL11.glPushMatrix();
            int j = Block.FIRE.sprite + i * 16;
            int k = (j & 0xF) << 4;
            int l = j & 0xF0;
            float g = (float)k / 256.0f;
            float h = ((float)k + 15.99f) / 256.0f;
            float m = (float)l / 256.0f;
            float n = ((float)l + 15.99f) / 256.0f;
            float o = (0.0f - f) / 2.0f;
            float p = o + f;
            float q = 0.0f - f / 2.0f;
            float r = q + f;
            float s = -0.5f;
            GL11.glTranslatef((float)((float)(-(i * 2 - 1)) * 0.24f), (float)-0.3f, (float)0.0f);
            GL11.glRotatef((float)((float)(i * 2 - 1) * 10.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            tesselator.begin();
            tesselator.vertex(o, q, s, h, n);
            tesselator.vertex(p, q, s, g, n);
            tesselator.vertex(p, r, s, g, m);
            tesselator.vertex(o, r, s, h, m);
            tesselator.end();
            GL11.glPopMatrix();
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_BLEND);
    }

    public void tick() {
        float f;
        this.lastHandHeight = this.handHeight;
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        ItemStack itemStack = clientPlayerEntity.inventory.getSelectedItem();
        ItemStack itemStack2 = itemStack;
        float g = itemStack2 == this.itemInHand ? 1.0f : 0.0f;
        float h = g - this.handHeight;
        if (h < -(f = 0.4f)) {
            h = -f;
        }
        if (h > f) {
            h = f;
        }
        this.handHeight += h;
        if (this.handHeight < 0.1f) {
            this.itemInHand = itemStack2;
        }
    }

    public void onBlockUsed() {
        this.handHeight = 0.0f;
    }

    public void onItemUsed() {
        this.handHeight = 0.0f;
    }
}

