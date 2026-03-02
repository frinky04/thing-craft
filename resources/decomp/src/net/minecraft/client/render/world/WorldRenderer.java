/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.ARBOcclusionQuery
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.world;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.entity.particle.ExplosionParticle;
import net.minecraft.client.entity.particle.FlameParticle;
import net.minecraft.client.entity.particle.ItemParticle;
import net.minecraft.client.entity.particle.LavaParticle;
import net.minecraft.client.entity.particle.PortalParticle;
import net.minecraft.client.entity.particle.RedstoneParticle;
import net.minecraft.client.entity.particle.SmokeParticle;
import net.minecraft.client.entity.particle.WaterBubbleParticle;
import net.minecraft.client.entity.particle.WaterSplashParticle;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.client.render.texture.SkinImageProcessor;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.render.world.ChunkRenderer;
import net.minecraft.client.render.world.DirtyChunkSorter;
import net.minecraft.client.render.world.DistanceChunkSorter;
import net.minecraft.client.render.world.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldEventListener;
import org.lwjgl.opengl.ARBOcclusionQuery;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class WorldRenderer
implements WorldEventListener {
    public List blockEntitiesToBeRendered = new ArrayList();
    private World world;
    private TextureManager textureManager;
    private List dirtyChunks = new ArrayList();
    private RenderChunk[] sortedChunks;
    private RenderChunk[] chunks;
    private int chunkCountX;
    private int chunkCountY;
    private int chunkCountZ;
    private int chunksGlList;
    private Minecraft minecraft;
    private BlockRenderer blockRenderer;
    private IntBuffer occlusionBuffer;
    private boolean occlusion = false;
    private int ticks = 0;
    private int starsGlList;
    private int lightSkyGlList;
    private int darkSkyGlList;
    private int minChunkX;
    private int minChunkY;
    private int minChunkZ;
    private int maxChunkX;
    private int maxChunkY;
    private int maxChunkZ;
    private int lastViewDistance = -1;
    private int entityRenderCooldown = 2;
    private int entityCount;
    private int renderedEntityCount;
    private int culledEntityCount;
    int[] f_82117152 = new int[50000];
    IntBuffer occlusionQueryBuffer = MemoryTracker.createIntBuffer(64);
    private int chunkCount;
    private int invisibleChunkCount;
    private int occludedChunkCount;
    private int compiledChunkCount;
    private int emptyChunkCount;
    private List chunksInCurrentLayer = new ArrayList();
    private ChunkRenderer[] chunkRenderers = new ChunkRenderer[]{new ChunkRenderer(), new ChunkRenderer(), new ChunkRenderer(), new ChunkRenderer()};
    int f_75491149 = 0;
    int f_70644475 = MemoryTracker.getLists(1);
    double prevCameraX = -9999.0;
    double prevCameraY = -9999.0;
    double prevCameraZ = -9999.0;
    public float miningProgress;
    int cullStep = 0;

    public WorldRenderer(Minecraft minecraft, TextureManager textureManager) {
        int l;
        this.minecraft = minecraft;
        this.textureManager = textureManager;
        int i = 64;
        this.chunksGlList = MemoryTracker.getLists(i * i * i * 3);
        this.occlusion = minecraft.getOpenGlCapabilities().glArbOcclusionQuery();
        if (this.occlusion) {
            this.occlusionQueryBuffer.clear();
            this.occlusionBuffer = MemoryTracker.createIntBuffer(i * i * i);
            this.occlusionBuffer.clear();
            this.occlusionBuffer.position(0);
            this.occlusionBuffer.limit(i * i * i);
            ARBOcclusionQuery.glGenQueriesARB((IntBuffer)this.occlusionBuffer);
        }
        this.starsGlList = MemoryTracker.getLists(3);
        GL11.glPushMatrix();
        GL11.glNewList((int)this.starsGlList, (int)GL11.GL_COMPILE);
        this.renderStars();
        GL11.glEndList();
        GL11.glPopMatrix();
        Tesselator tesselator = Tesselator.INSTANCE;
        this.lightSkyGlList = this.starsGlList + 1;
        GL11.glNewList((int)this.lightSkyGlList, (int)GL11.GL_COMPILE);
        int j = 64;
        int k = 256 / j + 2;
        float f = 16.0f;
        for (l = -j * k; l <= j * k; l += j) {
            for (int m = -j * k; m <= j * k; m += j) {
                tesselator.begin();
                tesselator.vertex(l + 0, f, m + 0);
                tesselator.vertex(l + j, f, m + 0);
                tesselator.vertex(l + j, f, m + j);
                tesselator.vertex(l + 0, f, m + j);
                tesselator.end();
            }
        }
        GL11.glEndList();
        this.darkSkyGlList = this.starsGlList + 2;
        GL11.glNewList((int)this.darkSkyGlList, (int)GL11.GL_COMPILE);
        f = -16.0f;
        tesselator.begin();
        for (l = -j * k; l <= j * k; l += j) {
            for (int n = -j * k; n <= j * k; n += j) {
                tesselator.vertex(l + j, f, n + 0);
                tesselator.vertex(l + 0, f, n + 0);
                tesselator.vertex(l + 0, f, n + j);
                tesselator.vertex(l + j, f, n + j);
            }
        }
        tesselator.end();
        GL11.glEndList();
    }

    private void renderStars() {
        Random random = new Random(10842L);
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        for (int i = 0; i < 1500; ++i) {
            double d = random.nextFloat() * 2.0f - 1.0f;
            double e = random.nextFloat() * 2.0f - 1.0f;
            double f = random.nextFloat() * 2.0f - 1.0f;
            double g = 0.25f + random.nextFloat() * 0.25f;
            double h = d * d + e * e + f * f;
            if (!(h < 1.0) || !(h > 0.01)) continue;
            h = 1.0 / Math.sqrt(h);
            double j = (d *= h) * 100.0;
            double k = (e *= h) * 100.0;
            double l = (f *= h) * 100.0;
            double m = Math.atan2(d, f);
            double n = Math.sin(m);
            double o = Math.cos(m);
            double p = Math.atan2(Math.sqrt(d * d + f * f), e);
            double q = Math.sin(p);
            double r = Math.cos(p);
            double s = random.nextDouble() * Math.PI * 2.0;
            double t = Math.sin(s);
            double u = Math.cos(s);
            for (int v = 0; v < 4; ++v) {
                double ab;
                double w = 0.0;
                double x = (double)((v & 2) - 1) * g;
                double y = (double)((v + 1 & 2) - 1) * g;
                double z = w;
                double aa = x * u - y * t;
                double ac = ab = y * u + x * t;
                double ad = aa * q + z * r;
                double ae = z * q - aa * r;
                double af = ae * n - ac * o;
                double ag = ad;
                double ah = ac * n + ae * o;
                tesselator.vertex(j + af, k + ag, l + ah);
            }
        }
        tesselator.end();
    }

    public void setWorld(World world) {
        if (this.world != null) {
            this.world.removeEventListener(this);
        }
        this.prevCameraX = -9999.0;
        this.prevCameraY = -9999.0;
        this.prevCameraZ = -9999.0;
        EntityRenderDispatcher.INSTANCE.setWorld(world);
        this.world = world;
        this.blockRenderer = new BlockRenderer(world);
        if (world != null) {
            world.addEventListener(this);
            this.reload();
        }
    }

    public void reload() {
        int m;
        int j;
        Block.LEAVES.setCulling(this.minecraft.options.fancyGraphics);
        this.lastViewDistance = this.minecraft.options.viewDistance;
        if (this.chunks != null) {
            for (int i = 0; i < this.chunks.length; ++i) {
                this.chunks[i].delete();
            }
        }
        if ((j = 64 << 3 - this.lastViewDistance) > 400) {
            j = 400;
        }
        this.chunkCountX = j / 16 + 1;
        this.chunkCountY = 8;
        this.chunkCountZ = j / 16 + 1;
        this.chunks = new RenderChunk[this.chunkCountX * this.chunkCountY * this.chunkCountZ];
        this.sortedChunks = new RenderChunk[this.chunkCountX * this.chunkCountY * this.chunkCountZ];
        int k = 0;
        int l = 0;
        this.minChunkX = 0;
        this.minChunkY = 0;
        this.minChunkZ = 0;
        this.maxChunkX = this.chunkCountX;
        this.maxChunkY = this.chunkCountY;
        this.maxChunkZ = this.chunkCountZ;
        for (m = 0; m < this.dirtyChunks.size(); ++m) {
            ((RenderChunk)this.dirtyChunks.get((int)m)).dirty = false;
        }
        this.dirtyChunks.clear();
        this.blockEntitiesToBeRendered.clear();
        for (m = 0; m < this.chunkCountX; ++m) {
            for (int n = 0; n < this.chunkCountY; ++n) {
                for (int o = 0; o < this.chunkCountZ; ++o) {
                    this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m] = new RenderChunk(this.world, this.blockEntitiesToBeRendered, m * 16, n * 16, o * 16, 16, this.chunksGlList + k);
                    if (this.occlusion) {
                        this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m].occlusionQuery = this.occlusionBuffer.get(l);
                    }
                    this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m].occlusionQueryPending = false;
                    this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m].occlusionVisible = true;
                    this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m].visible = true;
                    this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m].id = l++;
                    this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m].markDirty();
                    this.sortedChunks[(o * this.chunkCountY + n) * this.chunkCountX + m] = this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m];
                    this.dirtyChunks.add(this.chunks[(o * this.chunkCountY + n) * this.chunkCountX + m]);
                    k += 3;
                }
            }
        }
        if (this.world != null) {
            ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
            this.sortChunks(MathHelper.floor(clientPlayerEntity.x), MathHelper.floor(clientPlayerEntity.y), MathHelper.floor(clientPlayerEntity.z));
            Arrays.sort(this.sortedChunks, new DistanceChunkSorter(clientPlayerEntity));
        }
        this.entityRenderCooldown = 2;
    }

    public void renderEntities(Vec3d cameraPos, Culler culler, float tickDelta) {
        int i;
        if (this.entityRenderCooldown > 0) {
            --this.entityRenderCooldown;
            return;
        }
        BlockEntityRenderDispatcher.INSTANCE.prepare(this.world, this.textureManager, this.minecraft.textRenderer, this.minecraft.player, tickDelta);
        EntityRenderDispatcher.INSTANCE.prepare(this.world, this.textureManager, this.minecraft.textRenderer, this.minecraft.player, this.minecraft.options, tickDelta);
        this.entityCount = 0;
        this.renderedEntityCount = 0;
        this.culledEntityCount = 0;
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        EntityRenderDispatcher.offsetX = clientPlayerEntity.prevX + (clientPlayerEntity.x - clientPlayerEntity.prevX) * (double)tickDelta;
        EntityRenderDispatcher.offsetY = clientPlayerEntity.prevY + (clientPlayerEntity.y - clientPlayerEntity.prevY) * (double)tickDelta;
        EntityRenderDispatcher.offsetZ = clientPlayerEntity.prevZ + (clientPlayerEntity.z - clientPlayerEntity.prevZ) * (double)tickDelta;
        BlockEntityRenderDispatcher.offsetX = clientPlayerEntity.prevX + (clientPlayerEntity.x - clientPlayerEntity.prevX) * (double)tickDelta;
        BlockEntityRenderDispatcher.offsetY = clientPlayerEntity.prevY + (clientPlayerEntity.y - clientPlayerEntity.prevY) * (double)tickDelta;
        BlockEntityRenderDispatcher.offsetZ = clientPlayerEntity.prevZ + (clientPlayerEntity.z - clientPlayerEntity.prevZ) * (double)tickDelta;
        List list = this.world.getEntities();
        this.entityCount = list.size();
        for (i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if (!entity.shouldRender(cameraPos) || !culler.isVisible(entity.shape) || entity == this.minecraft.player && !this.minecraft.options.perspective) continue;
            ++this.renderedEntityCount;
            EntityRenderDispatcher.INSTANCE.render(entity, tickDelta);
        }
        for (i = 0; i < this.blockEntitiesToBeRendered.size(); ++i) {
            BlockEntityRenderDispatcher.INSTANCE.render((BlockEntity)this.blockEntitiesToBeRendered.get(i), tickDelta);
        }
    }

    public String getChunkDebugInfo() {
        return "C: " + this.compiledChunkCount + "/" + this.chunkCount + ". F: " + this.invisibleChunkCount + ", O: " + this.occludedChunkCount + ", E: " + this.emptyChunkCount;
    }

    public String getEntityDebugInfo() {
        return "E: " + this.renderedEntityCount + "/" + this.entityCount + ". B: " + this.culledEntityCount + ", I: " + (this.entityCount - this.culledEntityCount - this.renderedEntityCount);
    }

    private void sortChunks(int cameraX, int cameraY, int cameraZ) {
        cameraX -= 8;
        cameraY -= 8;
        cameraZ -= 8;
        this.minChunkX = Integer.MAX_VALUE;
        this.minChunkY = Integer.MAX_VALUE;
        this.minChunkZ = Integer.MAX_VALUE;
        this.maxChunkX = Integer.MIN_VALUE;
        this.maxChunkY = Integer.MIN_VALUE;
        this.maxChunkZ = Integer.MIN_VALUE;
        int i = this.chunkCountX * 16;
        int j = i / 2;
        for (int k = 0; k < this.chunkCountX; ++k) {
            int l = k * 16;
            int m = l + j - cameraX;
            if (m < 0) {
                m -= i - 1;
            }
            if ((l -= (m /= i) * i) < this.minChunkX) {
                this.minChunkX = l;
            }
            if (l > this.maxChunkX) {
                this.maxChunkX = l;
            }
            for (int n = 0; n < this.chunkCountZ; ++n) {
                int o = n * 16;
                int p = o + j - cameraZ;
                if (p < 0) {
                    p -= i - 1;
                }
                if ((o -= (p /= i) * i) < this.minChunkZ) {
                    this.minChunkZ = o;
                }
                if (o > this.maxChunkZ) {
                    this.maxChunkZ = o;
                }
                for (int q = 0; q < this.chunkCountY; ++q) {
                    int r = q * 16;
                    if (r < this.minChunkY) {
                        this.minChunkY = r;
                    }
                    if (r > this.maxChunkY) {
                        this.maxChunkY = r;
                    }
                    RenderChunk renderChunk = this.chunks[(n * this.chunkCountY + q) * this.chunkCountX + k];
                    boolean s = renderChunk.dirty;
                    renderChunk.setOrigin(l, r, o);
                    if (s || !renderChunk.dirty) continue;
                    this.dirtyChunks.add(renderChunk);
                }
            }
        }
    }

    public int render(PlayerEntity camera, int layer, double tickDelta) {
        if (this.minecraft.options.viewDistance != this.lastViewDistance) {
            this.reload();
        }
        if (layer == 0) {
            this.chunkCount = 0;
            this.invisibleChunkCount = 0;
            this.occludedChunkCount = 0;
            this.compiledChunkCount = 0;
            this.emptyChunkCount = 0;
        }
        double d = camera.prevX + (camera.x - camera.prevX) * tickDelta;
        double e = camera.prevY + (camera.y - camera.prevY) * tickDelta;
        double f = camera.prevZ + (camera.z - camera.prevZ) * tickDelta;
        double g = camera.x - this.prevCameraX;
        double h = camera.y - this.prevCameraY;
        double i = camera.z - this.prevCameraZ;
        if (g * g + h * h + i * i > 16.0) {
            this.prevCameraX = camera.x;
            this.prevCameraY = camera.y;
            this.prevCameraZ = camera.z;
            this.sortChunks(MathHelper.floor(camera.x), MathHelper.floor(camera.y), MathHelper.floor(camera.z));
            Arrays.sort(this.sortedChunks, new DistanceChunkSorter(camera));
        }
        int j = 0;
        if (this.occlusion && !this.minecraft.options.anaglyph && layer == 0) {
            int k = 0;
            int l = 16;
            this.checkOcclusionQueries(k, l);
            for (int m = k; m < l; ++m) {
                this.sortedChunks[m].occlusionVisible = true;
            }
            j += this.renderChunks(k, l, layer, tickDelta);
            do {
                k = l;
                if ((l *= 2) > this.sortedChunks.length) {
                    l = this.sortedChunks.length;
                }
                GL11.glDisable((int)GL11.GL_TEXTURE_2D);
                GL11.glDisable((int)GL11.GL_LIGHTING);
                GL11.glDisable((int)GL11.GL_ALPHA_TEST);
                GL11.glDisable((int)GL11.GL_FOG);
                GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
                GL11.glDepthMask((boolean)false);
                this.checkOcclusionQueries(k, l);
                GL11.glPushMatrix();
                float n = 0.0f;
                float o = 0.0f;
                float p = 0.0f;
                for (int q = k; q < l; ++q) {
                    float r;
                    int s;
                    if (this.sortedChunks[q].isEmpty()) {
                        this.sortedChunks[q].visible = false;
                        continue;
                    }
                    if (!this.sortedChunks[q].visible) {
                        this.sortedChunks[q].occlusionVisible = true;
                    }
                    if (!this.sortedChunks[q].visible || this.sortedChunks[q].occlusionQueryPending || this.ticks % (s = (int)(1.0f + (r = MathHelper.sqrt(this.sortedChunks[q].squaredDistanceToCenter(camera))) / 128.0f)) != q % s) continue;
                    RenderChunk renderChunk = this.sortedChunks[q];
                    float t = (float)((double)renderChunk.renderX - d);
                    float u = (float)((double)renderChunk.renderY - e);
                    float v = (float)((double)renderChunk.renderZ - f);
                    float w = t - n;
                    float x = u - o;
                    float y = v - p;
                    if (w != 0.0f || x != 0.0f || y != 0.0f) {
                        GL11.glTranslatef((float)w, (float)x, (float)y);
                        n += w;
                        o += x;
                        p += y;
                    }
                    ARBOcclusionQuery.glBeginQueryARB((int)ARBOcclusionQuery.GL_SAMPLES_PASSED_ARB, (int)this.sortedChunks[q].occlusionQuery);
                    this.sortedChunks[q].renderOcclusionTest();
                    ARBOcclusionQuery.glEndQueryARB((int)ARBOcclusionQuery.GL_SAMPLES_PASSED_ARB);
                    this.sortedChunks[q].occlusionQueryPending = true;
                }
                GL11.glPopMatrix();
                GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
                GL11.glDepthMask((boolean)true);
                GL11.glEnable((int)GL11.GL_TEXTURE_2D);
                GL11.glEnable((int)GL11.GL_ALPHA_TEST);
                GL11.glEnable((int)GL11.GL_FOG);
                j += this.renderChunks(k, l, layer, tickDelta);
            } while (l < this.sortedChunks.length);
        } else {
            j += this.renderChunks(0, this.sortedChunks.length, layer, tickDelta);
        }
        return j;
    }

    private void checkOcclusionQueries(int from, int to) {
        for (int i = from; i < to; ++i) {
            if (!this.sortedChunks[i].occlusionQueryPending) continue;
            this.occlusionQueryBuffer.clear();
            ARBOcclusionQuery.glGetQueryObjectuARB((int)this.sortedChunks[i].occlusionQuery, (int)ARBOcclusionQuery.GL_QUERY_RESULT_AVAILABLE_ARB, (IntBuffer)this.occlusionQueryBuffer);
            if (this.occlusionQueryBuffer.get(0) == 0) continue;
            this.sortedChunks[i].occlusionQueryPending = false;
            this.occlusionQueryBuffer.clear();
            ARBOcclusionQuery.glGetQueryObjectuARB((int)this.sortedChunks[i].occlusionQuery, (int)ARBOcclusionQuery.GL_QUERY_RESULT_ARB, (IntBuffer)this.occlusionQueryBuffer);
            this.sortedChunks[i].occlusionVisible = this.occlusionQueryBuffer.get(0) != 0;
        }
    }

    private int renderChunks(int from, int to, int layer, double tickDelta) {
        int m;
        this.chunksInCurrentLayer.clear();
        int i = 0;
        for (int j = from; j < to; ++j) {
            int k;
            if (layer == 0) {
                ++this.chunkCount;
                if (this.sortedChunks[j].empty[layer]) {
                    ++this.emptyChunkCount;
                } else if (!this.sortedChunks[j].visible) {
                    ++this.invisibleChunkCount;
                } else if (this.occlusion && !this.sortedChunks[j].occlusionVisible) {
                    ++this.occludedChunkCount;
                } else {
                    ++this.compiledChunkCount;
                }
            }
            if (this.sortedChunks[j].empty[layer] || !this.sortedChunks[j].visible || !this.sortedChunks[j].occlusionVisible || (k = this.sortedChunks[j].getGlList(layer)) < 0) continue;
            this.chunksInCurrentLayer.add(this.sortedChunks[j]);
            ++i;
        }
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        double d = clientPlayerEntity.prevX + (clientPlayerEntity.x - clientPlayerEntity.prevX) * tickDelta;
        double e = clientPlayerEntity.prevY + (clientPlayerEntity.y - clientPlayerEntity.prevY) * tickDelta;
        double f = clientPlayerEntity.prevZ + (clientPlayerEntity.z - clientPlayerEntity.prevZ) * tickDelta;
        int l = 0;
        for (m = 0; m < this.chunkRenderers.length; ++m) {
            this.chunkRenderers[m].clear();
        }
        for (m = 0; m < this.chunksInCurrentLayer.size(); ++m) {
            RenderChunk renderChunk = (RenderChunk)this.chunksInCurrentLayer.get(m);
            int n = -1;
            for (int o = 0; o < l; ++o) {
                if (!this.chunkRenderers[o].isAt(renderChunk.renderX, renderChunk.renderY, renderChunk.renderZ)) continue;
                n = o;
            }
            if (n < 0) {
                n = l++;
                this.chunkRenderers[n].init(renderChunk.renderX, renderChunk.renderY, renderChunk.renderZ, d, e, f);
            }
            this.chunkRenderers[n].addGlList(renderChunk.getGlList(layer));
        }
        this.renderLastChunks(layer, tickDelta);
        return i;
    }

    public void renderLastChunks(int layer, double tickDelta) {
        for (int i = 0; i < this.chunkRenderers.length; ++i) {
            this.chunkRenderers[i].render();
        }
    }

    public void tick() {
        ++this.ticks;
    }

    public void renderSky(float tickDelta) {
        if (this.minecraft.world.dimension.unnatural) {
            return;
        }
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        Vec3d vec3d = this.world.getSkyColor(this.minecraft.player, tickDelta);
        float f = (float)vec3d.x;
        float g = (float)vec3d.y;
        float h = (float)vec3d.z;
        if (this.minecraft.options.anaglyph) {
            float i = (f * 30.0f + g * 59.0f + h * 11.0f) / 100.0f;
            float j = (f * 30.0f + g * 70.0f) / 100.0f;
            float l = (f * 30.0f + h * 70.0f) / 100.0f;
            f = i;
            g = j;
            h = l;
        }
        GL11.glColor3f((float)f, (float)g, (float)h);
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glDepthMask((boolean)false);
        GL11.glEnable((int)GL11.GL_FOG);
        GL11.glColor3f((float)f, (float)g, (float)h);
        GL11.glCallList((int)this.lightSkyGlList);
        GL11.glDisable((int)GL11.GL_FOG);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        float[] fs = this.world.dimension.getSunriseColor(this.world.getTimeOfDay(tickDelta), tickDelta);
        if (fs != null) {
            GL11.glDisable((int)GL11.GL_TEXTURE_2D);
            GL11.glShadeModel((int)GL11.GL_SMOOTH);
            GL11.glPushMatrix();
            GL11.glRotatef((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            float m = this.world.getTimeOfDay(tickDelta);
            GL11.glRotatef((float)(m > 0.5f ? 180.0f : 0.0f), (float)0.0f, (float)0.0f, (float)1.0f);
            tesselator.begin(6);
            tesselator.color(fs[0], fs[1], fs[2], fs[3]);
            tesselator.vertex(0.0, 100.0, 0.0);
            int o = 16;
            tesselator.color(fs[0], fs[1], fs[2], 0.0f);
            for (int q = 0; q <= o; ++q) {
                float s = (float)q * (float)Math.PI * 2.0f / (float)o;
                float u = MathHelper.sin(s);
                float v = MathHelper.cos(s);
                tesselator.vertex(u * 120.0f, v * 120.0f, -v * 40.0f * fs[3]);
            }
            tesselator.end();
            GL11.glPopMatrix();
            GL11.glShadeModel((int)GL11.GL_FLAT);
        }
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc((int)GL11.GL_ONE, (int)GL11.GL_ONE);
        GL11.glPushMatrix();
        float k = 0.0f;
        float n = 0.0f;
        float p = 0.0f;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glTranslatef((float)k, (float)n, (float)p);
        GL11.glRotatef((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GL11.glRotatef((float)(this.world.getTimeOfDay(tickDelta) * 360.0f), (float)1.0f, (float)0.0f, (float)0.0f);
        float r = 30.0f;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.textureManager.load("/terrain/sun.png"));
        tesselator.begin();
        tesselator.vertex(-r, 100.0, -r, 0.0, 0.0);
        tesselator.vertex(r, 100.0, -r, 1.0, 0.0);
        tesselator.vertex(r, 100.0, r, 1.0, 1.0);
        tesselator.vertex(-r, 100.0, r, 0.0, 1.0);
        tesselator.end();
        r = 20.0f;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.textureManager.load("/terrain/moon.png"));
        tesselator.begin();
        tesselator.vertex(-r, -100.0, r, 1.0, 1.0);
        tesselator.vertex(r, -100.0, r, 0.0, 1.0);
        tesselator.vertex(r, -100.0, -r, 0.0, 0.0);
        tesselator.vertex(-r, -100.0, -r, 1.0, 0.0);
        tesselator.end();
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        float t = this.world.getStarBrightness(tickDelta);
        if (t > 0.0f) {
            GL11.glColor4f((float)t, (float)t, (float)t, (float)t);
            GL11.glCallList((int)this.starsGlList);
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glEnable((int)GL11.GL_FOG);
        GL11.glPopMatrix();
        GL11.glColor3f((float)(f * 0.2f + 0.04f), (float)(g * 0.2f + 0.04f), (float)(h * 0.6f + 0.1f));
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        GL11.glCallList((int)this.darkSkyGlList);
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        GL11.glDepthMask((boolean)true);
    }

    public void renderClouds(float tickDelta) {
        if (this.minecraft.world.dimension.unnatural) {
            return;
        }
        if (this.minecraft.options.fancyGraphics) {
            this.renderFancyClouds(tickDelta);
            return;
        }
        GL11.glDisable((int)GL11.GL_CULL_FACE);
        float f = (float)(this.minecraft.player.prevY + (this.minecraft.player.y - this.minecraft.player.prevY) * (double)tickDelta);
        int i = 32;
        int j = 256 / i;
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.textureManager.load("/environment/clouds.png"));
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        Vec3d vec3d = this.world.getCloudColor(tickDelta);
        float g = (float)vec3d.x;
        float h = (float)vec3d.y;
        float k = (float)vec3d.z;
        if (this.minecraft.options.anaglyph) {
            float l = (g * 30.0f + h * 59.0f + k * 11.0f) / 100.0f;
            float n = (g * 30.0f + h * 70.0f) / 100.0f;
            float o = (g * 30.0f + k * 70.0f) / 100.0f;
            g = l;
            h = n;
            k = o;
        }
        float m = 4.8828125E-4f;
        double d = this.minecraft.player.lastX + (this.minecraft.player.x - this.minecraft.player.lastX) * (double)tickDelta + (double)(((float)this.ticks + tickDelta) * 0.03f);
        double e = this.minecraft.player.lastZ + (this.minecraft.player.z - this.minecraft.player.lastZ) * (double)tickDelta;
        int p = MathHelper.floor(d / 2048.0);
        int q = MathHelper.floor(e / 2048.0);
        float r = 120.0f - f + 0.33f;
        float s = (float)((d -= (double)(p * 2048)) * (double)m);
        float t = (float)((e -= (double)(q * 2048)) * (double)m);
        tesselator.begin();
        tesselator.color(g, h, k, 0.8f);
        for (int u = -i * j; u < i * j; u += i) {
            for (int v = -i * j; v < i * j; v += i) {
                tesselator.vertex(u + 0, r, v + i, (float)(u + 0) * m + s, (float)(v + i) * m + t);
                tesselator.vertex(u + i, r, v + i, (float)(u + i) * m + s, (float)(v + i) * m + t);
                tesselator.vertex(u + i, r, v + 0, (float)(u + i) * m + s, (float)(v + 0) * m + t);
                tesselator.vertex(u + 0, r, v + 0, (float)(u + 0) * m + s, (float)(v + 0) * m + t);
            }
        }
        tesselator.end();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glEnable((int)GL11.GL_CULL_FACE);
    }

    public void renderFancyClouds(float tickDelta) {
        GL11.glDisable((int)GL11.GL_CULL_FACE);
        float f = (float)(this.minecraft.player.prevY + (this.minecraft.player.y - this.minecraft.player.prevY) * (double)tickDelta);
        Tesselator tesselator = Tesselator.INSTANCE;
        float g = 12.0f;
        float h = 4.0f;
        double d = (this.minecraft.player.lastX + (this.minecraft.player.x - this.minecraft.player.lastX) * (double)tickDelta + (double)(((float)this.ticks + tickDelta) * 0.03f)) / (double)g;
        double e = (this.minecraft.player.lastZ + (this.minecraft.player.z - this.minecraft.player.lastZ) * (double)tickDelta) / (double)g + (double)0.33f;
        float i = 108.0f - f + 0.33f;
        int j = MathHelper.floor(d / 2048.0);
        int k = MathHelper.floor(e / 2048.0);
        d -= (double)(j * 2048);
        e -= (double)(k * 2048);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.textureManager.load("/environment/clouds.png"));
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        Vec3d vec3d = this.world.getCloudColor(tickDelta);
        float l = (float)vec3d.x;
        float m = (float)vec3d.y;
        float n = (float)vec3d.z;
        if (this.minecraft.options.anaglyph) {
            float o = (l * 30.0f + m * 59.0f + n * 11.0f) / 100.0f;
            float q = (l * 30.0f + m * 70.0f) / 100.0f;
            float s = (l * 30.0f + n * 70.0f) / 100.0f;
            l = o;
            m = q;
            n = s;
        }
        float p = (float)(d * 0.0);
        float r = (float)(e * 0.0);
        float t = 0.00390625f;
        p = (float)MathHelper.floor(d) * t;
        r = (float)MathHelper.floor(e) * t;
        float u = (float)(d - (double)MathHelper.floor(d));
        float v = (float)(e - (double)MathHelper.floor(e));
        int w = 8;
        int x = 3;
        float y = 9.765625E-4f;
        GL11.glScalef((float)g, (float)1.0f, (float)g);
        for (int z = 0; z < 2; ++z) {
            if (z == 0) {
                GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
            } else {
                GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            }
            for (int aa = -x + 1; aa <= x; ++aa) {
                for (int ab = -x + 1; ab <= x; ++ab) {
                    tesselator.begin();
                    float ac = aa * w;
                    float ad = ab * w;
                    float ae = ac - u;
                    float af = ad - v;
                    if (i > -h - 1.0f) {
                        tesselator.color(l * 0.7f, m * 0.7f, n * 0.7f, 0.8f);
                        tesselator.normal(0.0f, -1.0f, 0.0f);
                        tesselator.vertex(ae + 0.0f, i + 0.0f, af + (float)w, (ac + 0.0f) * t + p, (ad + (float)w) * t + r);
                        tesselator.vertex(ae + (float)w, i + 0.0f, af + (float)w, (ac + (float)w) * t + p, (ad + (float)w) * t + r);
                        tesselator.vertex(ae + (float)w, i + 0.0f, af + 0.0f, (ac + (float)w) * t + p, (ad + 0.0f) * t + r);
                        tesselator.vertex(ae + 0.0f, i + 0.0f, af + 0.0f, (ac + 0.0f) * t + p, (ad + 0.0f) * t + r);
                    }
                    if (i <= h + 1.0f) {
                        tesselator.color(l, m, n, 0.8f);
                        tesselator.normal(0.0f, 1.0f, 0.0f);
                        tesselator.vertex(ae + 0.0f, i + h - y, af + (float)w, (ac + 0.0f) * t + p, (ad + (float)w) * t + r);
                        tesselator.vertex(ae + (float)w, i + h - y, af + (float)w, (ac + (float)w) * t + p, (ad + (float)w) * t + r);
                        tesselator.vertex(ae + (float)w, i + h - y, af + 0.0f, (ac + (float)w) * t + p, (ad + 0.0f) * t + r);
                        tesselator.vertex(ae + 0.0f, i + h - y, af + 0.0f, (ac + 0.0f) * t + p, (ad + 0.0f) * t + r);
                    }
                    tesselator.color(l * 0.9f, m * 0.9f, n * 0.9f, 0.8f);
                    if (aa > -1) {
                        tesselator.normal(-1.0f, 0.0f, 0.0f);
                        for (int ag = 0; ag < w; ++ag) {
                            tesselator.vertex(ae + (float)ag + 0.0f, i + 0.0f, af + (float)w, (ac + (float)ag + 0.5f) * t + p, (ad + (float)w) * t + r);
                            tesselator.vertex(ae + (float)ag + 0.0f, i + h, af + (float)w, (ac + (float)ag + 0.5f) * t + p, (ad + (float)w) * t + r);
                            tesselator.vertex(ae + (float)ag + 0.0f, i + h, af + 0.0f, (ac + (float)ag + 0.5f) * t + p, (ad + 0.0f) * t + r);
                            tesselator.vertex(ae + (float)ag + 0.0f, i + 0.0f, af + 0.0f, (ac + (float)ag + 0.5f) * t + p, (ad + 0.0f) * t + r);
                        }
                    }
                    if (aa <= 1) {
                        tesselator.normal(1.0f, 0.0f, 0.0f);
                        for (int ah = 0; ah < w; ++ah) {
                            tesselator.vertex(ae + (float)ah + 1.0f - y, i + 0.0f, af + (float)w, (ac + (float)ah + 0.5f) * t + p, (ad + (float)w) * t + r);
                            tesselator.vertex(ae + (float)ah + 1.0f - y, i + h, af + (float)w, (ac + (float)ah + 0.5f) * t + p, (ad + (float)w) * t + r);
                            tesselator.vertex(ae + (float)ah + 1.0f - y, i + h, af + 0.0f, (ac + (float)ah + 0.5f) * t + p, (ad + 0.0f) * t + r);
                            tesselator.vertex(ae + (float)ah + 1.0f - y, i + 0.0f, af + 0.0f, (ac + (float)ah + 0.5f) * t + p, (ad + 0.0f) * t + r);
                        }
                    }
                    tesselator.color(l * 0.8f, m * 0.8f, n * 0.8f, 0.8f);
                    if (ab > -1) {
                        tesselator.normal(0.0f, 0.0f, -1.0f);
                        for (int ai = 0; ai < w; ++ai) {
                            tesselator.vertex(ae + 0.0f, i + h, af + (float)ai + 0.0f, (ac + 0.0f) * t + p, (ad + (float)ai + 0.5f) * t + r);
                            tesselator.vertex(ae + (float)w, i + h, af + (float)ai + 0.0f, (ac + (float)w) * t + p, (ad + (float)ai + 0.5f) * t + r);
                            tesselator.vertex(ae + (float)w, i + 0.0f, af + (float)ai + 0.0f, (ac + (float)w) * t + p, (ad + (float)ai + 0.5f) * t + r);
                            tesselator.vertex(ae + 0.0f, i + 0.0f, af + (float)ai + 0.0f, (ac + 0.0f) * t + p, (ad + (float)ai + 0.5f) * t + r);
                        }
                    }
                    if (ab <= 1) {
                        tesselator.normal(0.0f, 0.0f, 1.0f);
                        for (int aj = 0; aj < w; ++aj) {
                            tesselator.vertex(ae + 0.0f, i + h, af + (float)aj + 1.0f - y, (ac + 0.0f) * t + p, (ad + (float)aj + 0.5f) * t + r);
                            tesselator.vertex(ae + (float)w, i + h, af + (float)aj + 1.0f - y, (ac + (float)w) * t + p, (ad + (float)aj + 0.5f) * t + r);
                            tesselator.vertex(ae + (float)w, i + 0.0f, af + (float)aj + 1.0f - y, (ac + (float)w) * t + p, (ad + (float)aj + 0.5f) * t + r);
                            tesselator.vertex(ae + 0.0f, i + 0.0f, af + (float)aj + 1.0f - y, (ac + 0.0f) * t + p, (ad + (float)aj + 0.5f) * t + r);
                        }
                    }
                    tesselator.end();
                }
            }
        }
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glEnable((int)GL11.GL_CULL_FACE);
    }

    public boolean compileChunks(PlayerEntity camera, boolean force) {
        Collections.sort(this.dirtyChunks, new DirtyChunkSorter(camera));
        int i = this.dirtyChunks.size() - 1;
        int j = this.dirtyChunks.size();
        for (int k = 0; k < j; ++k) {
            RenderChunk renderChunk = (RenderChunk)this.dirtyChunks.get(i - k);
            if (!force) {
                if (renderChunk.squaredDistanceToCenter(camera) > 1024.0f && (renderChunk.visible ? k >= 3 : k >= 1)) {
                    return false;
                }
            } else if (!renderChunk.visible) continue;
            renderChunk.compile();
            this.dirtyChunks.remove(renderChunk);
            renderChunk.dirty = false;
        }
        return this.dirtyChunks.size() == 0;
    }

    public void renderMiningProgress(PlayerEntity camera, HitResult hit, int i, ItemStack itemInHand, float tickDelta) {
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)((MathHelper.sin((float)System.currentTimeMillis() / 100.0f) * 0.2f + 0.4f) * 0.5f));
        if (i == 0) {
            if (this.miningProgress > 0.0f) {
                GL11.glBlendFunc((int)GL11.GL_DST_COLOR, (int)GL11.GL_SRC_COLOR);
                int j = this.textureManager.load("/terrain.png");
                GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)j);
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)0.5f);
                GL11.glPushMatrix();
                int k = this.world.getBlock(hit.x, hit.y, hit.z);
                Block block = k > 0 ? Block.BY_ID[k] : null;
                GL11.glDisable((int)GL11.GL_ALPHA_TEST);
                GL11.glPolygonOffset((float)-3.0f, (float)-3.0f);
                GL11.glEnable((int)GL11.GL_POLYGON_OFFSET_FILL);
                tesselator.begin();
                double d = camera.prevX + (camera.x - camera.prevX) * (double)tickDelta;
                double e = camera.prevY + (camera.y - camera.prevY) * (double)tickDelta;
                double g = camera.prevZ + (camera.z - camera.prevZ) * (double)tickDelta;
                tesselator.offset(-d, -e, -g);
                tesselator.uncolored();
                if (block == null) {
                    block = Block.STONE;
                }
                this.blockRenderer.renderMiningProgress(block, hit.x, hit.y, hit.z, 240 + (int)(this.miningProgress * 10.0f));
                tesselator.end();
                tesselator.offset(0.0, 0.0, 0.0);
                GL11.glPolygonOffset((float)0.0f, (float)0.0f);
                GL11.glDisable((int)GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glEnable((int)GL11.GL_ALPHA_TEST);
                GL11.glDepthMask((boolean)true);
                GL11.glPopMatrix();
            }
        } else if (itemInHand != null) {
            GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
            float f = MathHelper.sin((float)System.currentTimeMillis() / 100.0f) * 0.2f + 0.8f;
            GL11.glColor4f((float)f, (float)f, (float)f, (float)(MathHelper.sin((float)System.currentTimeMillis() / 200.0f) * 0.2f + 0.5f));
            int l = this.textureManager.load("/terrain.png");
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)l);
            int m = hit.x;
            int n = hit.y;
            int o = hit.z;
            if (hit.face == 0) {
                --n;
            }
            if (hit.face == 1) {
                ++n;
            }
            if (hit.face == 2) {
                --o;
            }
            if (hit.face == 3) {
                ++o;
            }
            if (hit.face == 4) {
                --m;
            }
            if (hit.face == 5) {
                ++m;
            }
        }
        GL11.glDisable((int)GL11.GL_BLEND);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
    }

    public void renderBlockOutline(PlayerEntity camera, HitResult hit, int i, ItemStack itemInHand, float tickDelta) {
        if (i == 0 && hit.type == 0) {
            GL11.glEnable((int)GL11.GL_BLEND);
            GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f((float)0.0f, (float)0.0f, (float)0.0f, (float)0.4f);
            GL11.glLineWidth((float)2.0f);
            GL11.glDisable((int)GL11.GL_TEXTURE_2D);
            GL11.glDepthMask((boolean)false);
            float f = 0.002f;
            int j = this.world.getBlock(hit.x, hit.y, hit.z);
            if (j > 0) {
                Block.BY_ID[j].updateShape(this.world, hit.x, hit.y, hit.z);
                double d = camera.prevX + (camera.x - camera.prevX) * (double)tickDelta;
                double e = camera.prevY + (camera.y - camera.prevY) * (double)tickDelta;
                double g = camera.prevZ + (camera.z - camera.prevZ) * (double)tickDelta;
                this.renderOutline(Block.BY_ID[j].getOutlineShape(this.world, hit.x, hit.y, hit.z).grown(f, f, f).moved(-d, -e, -g));
            }
            GL11.glDepthMask((boolean)true);
            GL11.glEnable((int)GL11.GL_TEXTURE_2D);
            GL11.glDisable((int)GL11.GL_BLEND);
        }
    }

    private void renderOutline(Box shape) {
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin(3);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.end();
        tesselator.begin(3);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.end();
        tesselator.begin(1);
        tesselator.vertex(shape.minX, shape.minY, shape.minZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.minZ);
        tesselator.vertex(shape.maxX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.maxX, shape.maxY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.minY, shape.maxZ);
        tesselator.vertex(shape.minX, shape.maxY, shape.maxZ);
        tesselator.end();
    }

    public void markDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int i = MathHelper.floorDiv(minX, 16);
        int j = MathHelper.floorDiv(minY, 16);
        int k = MathHelper.floorDiv(minZ, 16);
        int l = MathHelper.floorDiv(maxX, 16);
        int m = MathHelper.floorDiv(maxY, 16);
        int n = MathHelper.floorDiv(maxZ, 16);
        for (int o = i; o <= l; ++o) {
            int p = o % this.chunkCountX;
            if (p < 0) {
                p += this.chunkCountX;
            }
            for (int q = j; q <= m; ++q) {
                int r = q % this.chunkCountY;
                if (r < 0) {
                    r += this.chunkCountY;
                }
                for (int s = k; s <= n; ++s) {
                    int t = s % this.chunkCountZ;
                    if (t < 0) {
                        t += this.chunkCountZ;
                    }
                    int u = (t * this.chunkCountY + r) * this.chunkCountX + p;
                    RenderChunk renderChunk = this.chunks[u];
                    if (!renderChunk.dirty) {
                        this.dirtyChunks.add(renderChunk);
                    }
                    renderChunk.markDirty();
                }
            }
        }
    }

    public void notifyBlockChanged(int x, int y, int z) {
        this.markDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    public void notifyRegionChanged(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.markDirty(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);
    }

    public void cullChunks(Culler culler, float tickDelta) {
        for (int i = 0; i < this.chunks.length; ++i) {
            if (this.chunks[i].isEmpty() || this.chunks[i].visible && (i + this.cullStep & 0xF) != 0) continue;
            this.chunks[i].cull(culler);
        }
        ++this.cullStep;
    }

    public void playRecordMusic(String record, int x, int y, int z) {
        if (record != null) {
            this.minecraft.gui.setRecordPlayingOverlay("C418 - " + record);
        }
        this.minecraft.soundEngine.playRecord(record, x, y, z, 1.0f, 1.0f);
    }

    public void playSound(String name, double x, double y, double z, float volume, float pitch) {
        float f = 16.0f;
        if (volume > 1.0f) {
            f *= volume;
        }
        if (this.minecraft.player.squaredDistanceTo(x, y, z) < (double)(f * f)) {
            this.minecraft.soundEngine.play(name, (float)x, (float)y, (float)z, volume, pitch);
        }
    }

    public void addParticle(String type, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        double d = this.minecraft.player.x - x;
        double e = this.minecraft.player.y - y;
        double f = this.minecraft.player.z - z;
        if (d * d + e * e + f * f > 256.0) {
            return;
        }
        if (type == "bubble") {
            this.minecraft.particleManager.add(new WaterBubbleParticle(this.world, x, y, z, velocityX, velocityY, velocityZ));
        } else if (type == "smoke") {
            this.minecraft.particleManager.add(new SmokeParticle(this.world, x, y, z));
        } else if (type == "portal") {
            this.minecraft.particleManager.add(new PortalParticle(this.world, x, y, z, velocityX, velocityY, velocityZ));
        } else if (type == "explode") {
            this.minecraft.particleManager.add(new ExplosionParticle(this.world, x, y, z, velocityX, velocityY, velocityZ));
        } else if (type == "flame") {
            this.minecraft.particleManager.add(new FlameParticle(this.world, x, y, z, velocityX, velocityY, velocityZ));
        } else if (type == "lava") {
            this.minecraft.particleManager.add(new LavaParticle(this.world, x, y, z));
        } else if (type == "splash") {
            this.minecraft.particleManager.add(new WaterSplashParticle(this.world, x, y, z, velocityX, velocityY, velocityZ));
        } else if (type == "largesmoke") {
            this.minecraft.particleManager.add(new SmokeParticle(this.world, x, y, z, 2.5f));
        } else if (type == "reddust") {
            this.minecraft.particleManager.add(new RedstoneParticle(this.world, x, y, z));
        } else if (type == "snowballpoof") {
            this.minecraft.particleManager.add(new ItemParticle(this.world, x, y, z, Item.SNOWBALL));
        } else if (type == "slime") {
            this.minecraft.particleManager.add(new ItemParticle(this.world, x, y, z, Item.SLIMEBALL));
        }
    }

    public void notifyEntityAdded(Entity entity) {
        if (entity.skin != null) {
            this.textureManager.addHttpTexture(entity.skin, new SkinImageProcessor());
        }
    }

    public void notifyEntityRemoved(Entity entity) {
        if (entity.skin != null) {
            this.textureManager.removeHttpTexture(entity.skin);
        }
    }

    public void notifyAmbientDarknessChanged() {
        for (int i = 0; i < this.chunks.length; ++i) {
            if (!this.chunks[i].hasSkyLight) continue;
            if (!this.chunks[i].dirty) {
                this.dirtyChunks.add(this.chunks[i]);
            }
            this.chunks[i].markDirty();
        }
    }

    public void notifyBlockEntityChanged(int x, int y, int z, BlockEntity blockEntity) {
    }
}

