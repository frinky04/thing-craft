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
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public abstract class EntityRenderer {
    protected EntityRenderDispatcher dispatcher;
    private Model model = new HumanoidModel();
    private BlockRenderer blockRenderer = new BlockRenderer();
    protected float shadowSize = 0.0f;
    protected float shadowDarkness = 1.0f;

    public abstract void render(Entity var1, double var2, double var4, double var6, float var8, float var9);

    protected void bindTexture(String path) {
        TextureManager textureManager = this.dispatcher.textureManager;
        textureManager.bind(textureManager.load(path));
    }

    protected void bindHttpTexture(String url, String backup) {
        TextureManager textureManager = this.dispatcher.textureManager;
        textureManager.bind(textureManager.loadHttpTexture(url, backup));
    }

    private void renderOnFire(Entity entity, double dx, double dy, double dz, float tickDelta) {
        GL11.glDisable((int)GL11.GL_LIGHTING);
        int i = Block.FIRE.sprite;
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        float f = (float)j / 256.0f;
        float g = ((float)j + 15.99f) / 256.0f;
        float h = (float)k / 256.0f;
        float l = ((float)k + 15.99f) / 256.0f;
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)dx), (float)((float)dy), (float)((float)dz));
        float m = entity.width * 1.4f;
        GL11.glScalef((float)m, (float)m, (float)m);
        this.bindTexture("/terrain.png");
        Tesselator tesselator = Tesselator.INSTANCE;
        float n = 1.0f;
        float o = 0.5f;
        float p = 0.0f;
        float q = entity.height / entity.width;
        GL11.glRotatef((float)(-this.dispatcher.cameraYaw), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)(-0.4f + (float)((int)q) * 0.02f));
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        tesselator.begin();
        while (q > 0.0f) {
            tesselator.vertex(n - o, 0.0f - p, 0.0, g, l);
            tesselator.vertex(0.0f - o, 0.0f - p, 0.0, f, l);
            tesselator.vertex(0.0f - o, 1.4f - p, 0.0, f, h);
            tesselator.vertex(n - o, 1.4f - p, 0.0, g, h);
            q -= 1.0f;
            p -= 1.0f;
            n *= 0.9f;
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-0.04f);
        }
        tesselator.end();
        GL11.glPopMatrix();
        GL11.glEnable((int)GL11.GL_LIGHTING);
    }

    private void renderShadow(Entity entity, double dx, double dy, double dz, float yaw, float tickDelta) {
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        TextureManager textureManager = this.dispatcher.textureManager;
        textureManager.bind(textureManager.load("%clamp%/misc/shadow.png"));
        World world = this.getWorld();
        GL11.glDepthMask((boolean)false);
        float f = this.shadowSize;
        double d = entity.prevX + (entity.x - entity.prevX) * (double)tickDelta;
        double e = entity.prevY + (entity.y - entity.prevY) * (double)tickDelta + (double)entity.getShadowHeightOffset();
        double g = entity.prevZ + (entity.z - entity.prevZ) * (double)tickDelta;
        int i = MathHelper.floor(d - (double)f);
        int j = MathHelper.floor(d + (double)f);
        int k = MathHelper.floor(e - (double)f);
        int l = MathHelper.floor(e);
        int m = MathHelper.floor(g - (double)f);
        int n = MathHelper.floor(g + (double)f);
        double h = dx - d;
        double o = dy - e;
        double p = dz - g;
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        for (int q = i; q <= j; ++q) {
            for (int r = k; r <= l; ++r) {
                for (int s = m; s <= n; ++s) {
                    int t = world.getBlock(q, r - 1, s);
                    if (t <= 0 || world.getRawBrightness(q, r, s) <= 3) continue;
                    this.renderShadowOnBlock(Block.BY_ID[t], dx, dy + (double)entity.getShadowHeightOffset(), dz, q, r, s, yaw, f, h, o + (double)entity.getShadowHeightOffset(), p);
                }
            }
        }
        tesselator.end();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glDepthMask((boolean)true);
    }

    private World getWorld() {
        return this.dispatcher.world;
    }

    private void renderShadowOnBlock(Block block, double dx, double dy, double dz, int x, int y, int z, float yaw, float shadowSize, double cx, double cy, double cz) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (!block.isCube()) {
            return;
        }
        double d = ((double)yaw - (dy - ((double)y + cy)) / 2.0) * 0.5 * (double)this.getWorld().getBrightness(x, y, z);
        if (d < 0.0) {
            return;
        }
        if (d > 1.0) {
            d = 1.0;
        }
        tesselator.color(1.0f, 1.0f, 1.0f, (float)d);
        double e = (double)x + block.minX + cx;
        double f = (double)x + block.maxX + cx;
        double g = (double)y + block.minY + cy + 0.015625;
        double h = (double)z + block.minZ + cz;
        double i = (double)z + block.maxZ + cz;
        float j = (float)((dx - e) / 2.0 / (double)shadowSize + 0.5);
        float k = (float)((dx - f) / 2.0 / (double)shadowSize + 0.5);
        float l = (float)((dz - h) / 2.0 / (double)shadowSize + 0.5);
        float m = (float)((dz - i) / 2.0 / (double)shadowSize + 0.5);
        tesselator.vertex(e, g, h, j, l);
        tesselator.vertex(e, g, i, j, m);
        tesselator.vertex(f, g, i, k, m);
        tesselator.vertex(f, g, h, k, l);
    }

    public static void renderShape(Box shape, double dx, double dy, double dz) {
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        tesselator.begin();
        tesselator.offset(dx, dy, dz);
        tesselator.normal(0.0f, 0.0f, -1.0f);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.normal(0.0f, 0.0f, 1.0f);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.normal(0.0f, -1.0f, 0.0f);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.normal(0.0f, 1.0f, 0.0f);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.normal(-1.0f, 0.0f, 0.0f);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.normal(1.0f, 0.0f, 0.0f);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.offset(0.0, 0.0, 0.0);
        tesselator.end();
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
    }

    public static void renderShapeFlat(Box shape) {
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.end();
    }

    public void init(EntityRenderDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void postRender(Entity entity, double dx, double dy, double dz, float yaw, float tickDelta) {
        double d;
        float f;
        if (this.dispatcher.options.fancyGraphics && this.shadowSize > 0.0f && (f = (float)((1.0 - (d = this.dispatcher.squaredDistanceToCamera(entity.x, entity.y, entity.z)) / 256.0) * (double)this.shadowDarkness)) > 0.0f) {
            this.renderShadow(entity, dx, dy, dz, f, tickDelta);
        }
        if (entity.onFireTimer > 0 || entity.onFire) {
            this.renderOnFire(entity, dx, dy, dz, tickDelta);
        }
    }

    public TextRenderer getTextRenderer() {
        return this.dispatcher.getTextRenderer();
    }
}

