/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldRegion;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class RenderChunk {
    public World world;
    private int glList = -1;
    private static Tesselator TESSELATOR = Tesselator.INSTANCE;
    public static int updateCounter = 0;
    public int originX;
    public int originY;
    public int originZ;
    public int sizeX;
    public int sizeY;
    public int sizeZ;
    public int renderX;
    public int renderY;
    public int renderZ;
    public int renderOffsetX;
    public int renderOffsetY;
    public int renderOffsetZ;
    public boolean visible = false;
    public boolean[] empty = new boolean[2];
    public int centerX;
    public int centerY;
    public int centerZ;
    public float radius;
    public boolean dirty;
    public Box bounds;
    public int id;
    public boolean occlusionVisible = true;
    public boolean occlusionQueryPending;
    public int occlusionQuery;
    public boolean hasSkyLight;
    private boolean compiled = false;
    public List renderableBlockEntities = new ArrayList();
    private List blockEntitiesToBeRendered;

    public RenderChunk(World world, List blockEntitiesToBeRendered, int originX, int originY, int originZ, int size, int glList) {
        this.world = world;
        this.blockEntitiesToBeRendered = blockEntitiesToBeRendered;
        this.sizeY = this.sizeZ = size;
        this.sizeX = this.sizeZ;
        this.radius = MathHelper.sqrt(this.sizeX * this.sizeX + this.sizeY * this.sizeY + this.sizeZ * this.sizeZ) / 2.0f;
        this.glList = glList;
        this.originX = -999;
        this.setOrigin(originX, originY, originZ);
        this.dirty = false;
    }

    public void setOrigin(int x, int y, int z) {
        if (x == this.originX && y == this.originY && z == this.originZ) {
            return;
        }
        this.reset();
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.centerX = x + this.sizeX / 2;
        this.centerY = y + this.sizeY / 2;
        this.centerZ = z + this.sizeZ / 2;
        this.renderOffsetX = x & 0x3FF;
        this.renderOffsetY = y;
        this.renderOffsetZ = z & 0x3FF;
        this.renderX = x - this.renderOffsetX;
        this.renderY = y - this.renderOffsetY;
        this.renderZ = z - this.renderOffsetZ;
        float f = 2.0f;
        this.bounds = Box.of((float)x - f, (float)y - f, (float)z - f, (float)(x + this.sizeX) + f, (float)(y + this.sizeY) + f, (float)(z + this.sizeZ) + f);
        GL11.glNewList((int)(this.glList + 2), (int)GL11.GL_COMPILE);
        ItemRenderer.renderShapeFlat(Box.fromPool((float)this.renderOffsetX - f, (float)this.renderOffsetY - f, (float)this.renderOffsetZ - f, (float)(this.renderOffsetX + this.sizeX) + f, (float)(this.renderOffsetY + this.sizeY) + f, (float)(this.renderOffsetZ + this.sizeZ) + f));
        GL11.glEndList();
        this.markDirty();
    }

    private void glTranslate() {
        GL11.glTranslatef((float)this.renderOffsetX, (float)this.renderOffsetY, (float)this.renderOffsetZ);
    }

    public void compile() {
        if (!this.dirty) {
            return;
        }
        ++updateCounter;
        int i = this.originX;
        int j = this.originY;
        int k = this.originZ;
        int l = this.originX + this.sizeX;
        int m = this.originY + this.sizeY;
        int n = this.originZ + this.sizeZ;
        for (int o = 0; o < 2; ++o) {
            this.empty[o] = true;
        }
        WorldChunk.hasSkyLight = false;
        HashSet hashSet = new HashSet();
        hashSet.addAll(this.renderableBlockEntities);
        this.renderableBlockEntities.clear();
        int p = 1;
        WorldRegion worldRegion = new WorldRegion(this.world, i - p, j - p, k - p, l + p, m + p, n + p);
        BlockRenderer blockRenderer = new BlockRenderer(worldRegion);
        for (int q = 0; q < 2; ++q) {
            boolean r = false;
            boolean s = false;
            boolean t = false;
            for (int u = j; u < m; ++u) {
                for (int v = k; v < n; ++v) {
                    for (int w = i; w < l; ++w) {
                        Block block;
                        int y;
                        BlockEntity blockEntity;
                        int x = worldRegion.getBlock(w, u, v);
                        if (x <= 0) continue;
                        if (!t) {
                            t = true;
                            GL11.glNewList((int)(this.glList + q), (int)GL11.GL_COMPILE);
                            GL11.glPushMatrix();
                            this.glTranslate();
                            float f = 1.000001f;
                            GL11.glTranslatef((float)((float)(-this.sizeZ) / 2.0f), (float)((float)(-this.sizeY) / 2.0f), (float)((float)(-this.sizeZ) / 2.0f));
                            GL11.glScalef((float)f, (float)f, (float)f);
                            GL11.glTranslatef((float)((float)this.sizeZ / 2.0f), (float)((float)this.sizeY / 2.0f), (float)((float)this.sizeZ / 2.0f));
                            TESSELATOR.begin();
                            TESSELATOR.offset(-this.originX, -this.originY, -this.originZ);
                        }
                        if (q == 0 && Block.HAS_BLOCK_ENTITY[x] && BlockEntityRenderDispatcher.INSTANCE.hasRenderer(blockEntity = worldRegion.getBlockEntity(w, u, v))) {
                            this.renderableBlockEntities.add(blockEntity);
                        }
                        if ((y = (block = Block.BY_ID[x]).getRenderLayer()) != q) {
                            r = true;
                            continue;
                        }
                        if (y != q) continue;
                        s |= blockRenderer.tesselateInWorld(block, w, u, v);
                    }
                }
            }
            if (t) {
                TESSELATOR.end();
                GL11.glPopMatrix();
                GL11.glEndList();
                TESSELATOR.offset(0.0, 0.0, 0.0);
            } else {
                s = false;
            }
            if (s) {
                this.empty[q] = false;
            }
            if (!r) break;
        }
        HashSet hashSet2 = new HashSet();
        hashSet2.addAll(this.renderableBlockEntities);
        hashSet2.removeAll(hashSet);
        this.blockEntitiesToBeRendered.addAll(hashSet2);
        hashSet.removeAll(this.renderableBlockEntities);
        this.blockEntitiesToBeRendered.removeAll(hashSet);
        this.hasSkyLight = WorldChunk.hasSkyLight;
        this.compiled = true;
    }

    public float squaredDistanceToCenter(Entity camera) {
        float f = (float)(camera.x - (double)this.centerX);
        float g = (float)(camera.y - (double)this.centerY);
        float h = (float)(camera.z - (double)this.centerZ);
        return f * f + g * g + h * h;
    }

    public void reset() {
        for (int i = 0; i < 2; ++i) {
            this.empty[i] = true;
        }
        this.visible = false;
        this.compiled = false;
    }

    public void delete() {
        this.reset();
        this.world = null;
    }

    public int getGlList(int layer) {
        if (!this.visible) {
            return -1;
        }
        if (!this.empty[layer]) {
            return this.glList + layer;
        }
        return -1;
    }

    public void cull(Culler culler) {
        this.visible = culler.isVisible(this.bounds);
    }

    public void renderOcclusionTest() {
        GL11.glCallList((int)(this.glList + 2));
    }

    public boolean isEmpty() {
        if (!this.compiled) {
            return false;
        }
        return this.empty[0] && this.empty[1];
    }

    public void markDirty() {
        this.dirty = true;
    }
}

