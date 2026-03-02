/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.Display
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GLContext
 *  org.lwjgl.util.glu.GLU
 */
package net.minecraft.client.render;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.CreativeInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.entity.particle.RainSplashParticle;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.FrustumCuller;
import net.minecraft.client.render.ItemInHandRenderer;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

@Environment(value=EnvType.CLIENT)
public class GameRenderer {
    private Minecraft minecraft;
    private float renderDistance = 0.0f;
    public ItemInHandRenderer itemInHandRenderer;
    private int ticks;
    private Entity targetEntity = null;
    private long lastActiveTime = System.currentTimeMillis();
    private Random random = new Random();
    volatile int f_71399429 = 0;
    volatile int f_36908558 = 0;
    FloatBuffer colorBuffer = MemoryTracker.createFloatBuffer(16);
    float fogRed;
    float fogGreen;
    float fogBlue;
    private float lastFogBrightness;
    private float fogBrightness;

    public GameRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.itemInHandRenderer = new ItemInHandRenderer(minecraft);
    }

    public void tick() {
        this.lastFogBrightness = this.fogBrightness;
        float f = this.minecraft.world.getBrightness(MathHelper.floor(this.minecraft.player.x), MathHelper.floor(this.minecraft.player.y), MathHelper.floor(this.minecraft.player.z));
        float g = (float)(3 - this.minecraft.options.viewDistance) / 3.0f;
        float h = f * (1.0f - g) + g;
        this.fogBrightness += (h - this.fogBrightness) * 0.1f;
        ++this.ticks;
        this.itemInHandRenderer.tick();
        if (this.minecraft.raining) {
            this.tickRain();
        }
    }

    public void pick(float tickDelta) {
        if (this.minecraft.player == null) {
            return;
        }
        double d = this.minecraft.interactionManager.getReach();
        this.minecraft.crosshairTarget = this.minecraft.player.rayTrace(d, tickDelta);
        double e = d;
        Vec3d vec3d = this.minecraft.player.getPosition(tickDelta);
        if (this.minecraft.crosshairTarget != null) {
            e = this.minecraft.crosshairTarget.facePos.distanceTo(vec3d);
        }
        if (this.minecraft.interactionManager instanceof CreativeInteractionManager) {
            d = 32.0;
            e = 32.0;
        } else {
            if (e > 3.0) {
                e = 3.0;
            }
            d = e;
        }
        Vec3d vec3d2 = this.minecraft.player.getLookVector(tickDelta);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        this.targetEntity = null;
        float f = 1.0f;
        List list = this.minecraft.world.getEntities(this.minecraft.player, this.minecraft.player.shape.expanded(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d).grown(f, f, f));
        double g = 0.0;
        for (int i = 0; i < list.size(); ++i) {
            double j;
            Entity entity = (Entity)list.get(i);
            if (!entity.hasCollision()) continue;
            float h = entity.getPickRadius();
            Box box = entity.shape.grown(h, h, h);
            HitResult hitResult = box.clip(vec3d, vec3d3);
            if (box.contains(vec3d)) {
                if (!(0.0 < g) && g != 0.0) continue;
                this.targetEntity = entity;
                g = 0.0;
                continue;
            }
            if (hitResult == null || !((j = vec3d.distanceTo(hitResult.facePos)) < g) && g != 0.0) continue;
            this.targetEntity = entity;
            g = j;
        }
        if (this.targetEntity != null && !(this.minecraft.interactionManager instanceof CreativeInteractionManager)) {
            this.minecraft.crosshairTarget = new HitResult(this.targetEntity);
        }
    }

    private float getFov(float tickDelta) {
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        float f = 70.0f;
        if (clientPlayerEntity.isSubmergedIn(Material.WATER)) {
            f = 60.0f;
        }
        if (clientPlayerEntity.health <= 0) {
            float g = (float)clientPlayerEntity.deathTicks + tickDelta;
            f /= (1.0f - 500.0f / (g + 500.0f)) * 2.0f + 1.0f;
        }
        return f;
    }

    private void applyHurtCam(float tickDelta) {
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        float f = (float)clientPlayerEntity.damagedTimer - tickDelta;
        if (clientPlayerEntity.health <= 0) {
            float g = (float)clientPlayerEntity.deathTicks + tickDelta;
            GL11.glRotatef((float)(40.0f - 8000.0f / (g + 200.0f)), (float)0.0f, (float)0.0f, (float)1.0f);
        }
        if (f < 0.0f) {
            return;
        }
        f /= (float)clientPlayerEntity.damagedTime;
        f = MathHelper.sin(f * f * f * f * (float)Math.PI);
        float h = clientPlayerEntity.damagedSwingDir;
        GL11.glRotatef((float)(-h), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(-f * 14.0f), (float)0.0f, (float)0.0f, (float)1.0f);
        GL11.glRotatef((float)h, (float)0.0f, (float)1.0f, (float)0.0f);
    }

    private void applyViewBobbing(float tickDelta) {
        if (this.minecraft.options.perspective) {
            return;
        }
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        float f = clientPlayerEntity.walkDistance - clientPlayerEntity.lastWalkDistance;
        float g = clientPlayerEntity.walkDistance + f * tickDelta;
        float h = clientPlayerEntity.lastBob + (clientPlayerEntity.bob - clientPlayerEntity.lastBob) * tickDelta;
        float i = clientPlayerEntity.lastTilt + (clientPlayerEntity.tilt - clientPlayerEntity.lastTilt) * tickDelta;
        GL11.glTranslatef((float)(MathHelper.sin(g * (float)Math.PI) * h * 0.5f), (float)(-Math.abs(MathHelper.cos(g * (float)Math.PI) * h)), (float)0.0f);
        GL11.glRotatef((float)(MathHelper.sin(g * (float)Math.PI) * h * 3.0f), (float)0.0f, (float)0.0f, (float)1.0f);
        GL11.glRotatef((float)(Math.abs(MathHelper.cos(g * (float)Math.PI + 0.2f) * h) * 5.0f), (float)1.0f, (float)0.0f, (float)0.0f);
        GL11.glRotatef((float)i, (float)1.0f, (float)0.0f, (float)0.0f);
    }

    private void transformCamera(float tickDelta) {
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        double d = clientPlayerEntity.lastX + (clientPlayerEntity.x - clientPlayerEntity.lastX) * (double)tickDelta;
        double e = clientPlayerEntity.lastY + (clientPlayerEntity.y - clientPlayerEntity.lastY) * (double)tickDelta;
        double f = clientPlayerEntity.lastZ + (clientPlayerEntity.z - clientPlayerEntity.lastZ) * (double)tickDelta;
        if (this.minecraft.options.perspective) {
            double g = 4.0;
            float h = clientPlayerEntity.yaw;
            float i = clientPlayerEntity.pitch;
            if (Keyboard.isKeyDown((int)59)) {
                i += 180.0f;
                g += 2.0;
            }
            double j = (double)(-MathHelper.sin(h / 180.0f * (float)Math.PI) * MathHelper.cos(i / 180.0f * (float)Math.PI)) * g;
            double k = (double)(MathHelper.cos(h / 180.0f * (float)Math.PI) * MathHelper.cos(i / 180.0f * (float)Math.PI)) * g;
            double l = (double)(-MathHelper.sin(i / 180.0f * (float)Math.PI)) * g;
            for (int m = 0; m < 8; ++m) {
                double q;
                HitResult hitResult;
                float n = (m & 1) * 2 - 1;
                float o = (m >> 1 & 1) * 2 - 1;
                float p = (m >> 2 & 1) * 2 - 1;
                if ((hitResult = this.minecraft.world.rayTrace(Vec3d.fromPool(d + (double)(n *= 0.1f), e + (double)(o *= 0.1f), f + (double)(p *= 0.1f)), Vec3d.fromPool(d - j + (double)n + (double)p, e - l + (double)o, f - k + (double)p))) == null || !((q = hitResult.facePos.distanceTo(Vec3d.fromPool(d, e, f))) < g)) continue;
                g = q;
            }
            if (Keyboard.isKeyDown((int)59)) {
                GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            }
            GL11.glRotatef((float)(clientPlayerEntity.pitch - i), (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)(clientPlayerEntity.yaw - h), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)((float)(-g)));
            GL11.glRotatef((float)(h - clientPlayerEntity.yaw), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)(i - clientPlayerEntity.pitch), (float)1.0f, (float)0.0f, (float)0.0f);
        } else {
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-0.1f);
        }
        GL11.glRotatef((float)(clientPlayerEntity.lastPitch + (clientPlayerEntity.pitch - clientPlayerEntity.lastPitch) * tickDelta), (float)1.0f, (float)0.0f, (float)0.0f);
        GL11.glRotatef((float)(clientPlayerEntity.lastYaw + (clientPlayerEntity.yaw - clientPlayerEntity.lastYaw) * tickDelta + 180.0f), (float)0.0f, (float)1.0f, (float)0.0f);
    }

    private void setupCamera(float tickDelta, int anaglyphRenderPass) {
        float g;
        this.renderDistance = 256 >> this.minecraft.options.viewDistance;
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float f = 0.07f;
        if (this.minecraft.options.anaglyph) {
            GL11.glTranslatef((float)((float)(-(anaglyphRenderPass * 2 - 1)) * f), (float)0.0f, (float)0.0f);
        }
        GLU.gluPerspective((float)this.getFov(tickDelta), (float)((float)this.minecraft.width / (float)this.minecraft.height), (float)0.05f, (float)this.renderDistance);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        if (this.minecraft.options.anaglyph) {
            GL11.glTranslatef((float)((float)(anaglyphRenderPass * 2 - 1) * 0.1f), (float)0.0f, (float)0.0f);
        }
        this.applyHurtCam(tickDelta);
        if (this.minecraft.options.viewBobbing) {
            this.applyViewBobbing(tickDelta);
        }
        if ((g = this.minecraft.player.lastPortalTime + (this.minecraft.player.portalTime - this.minecraft.player.lastPortalTime) * tickDelta) > 0.0f) {
            float h = 5.0f / (g * g + 5.0f) - g * 0.04f;
            h *= h;
            GL11.glRotatef((float)(g * g * 1500.0f), (float)0.0f, (float)1.0f, (float)1.0f);
            GL11.glScalef((float)(1.0f / h), (float)1.0f, (float)1.0f);
            GL11.glRotatef((float)(-g * g * 1500.0f), (float)0.0f, (float)1.0f, (float)1.0f);
        }
        this.transformCamera(tickDelta);
    }

    private void renderItemInHand(float tickDelta, int anaglyphRenderPass) {
        GL11.glLoadIdentity();
        if (this.minecraft.options.anaglyph) {
            GL11.glTranslatef((float)((float)(anaglyphRenderPass * 2 - 1) * 0.1f), (float)0.0f, (float)0.0f);
        }
        GL11.glPushMatrix();
        this.applyHurtCam(tickDelta);
        if (this.minecraft.options.viewBobbing) {
            this.applyViewBobbing(tickDelta);
        }
        if (!this.minecraft.options.perspective && !Keyboard.isKeyDown((int)59)) {
            this.itemInHandRenderer.renderHand(tickDelta);
        }
        GL11.glPopMatrix();
        if (!this.minecraft.options.perspective) {
            this.itemInHandRenderer.renderScreenEffects(tickDelta);
            this.applyHurtCam(tickDelta);
        }
        if (this.minecraft.options.viewBobbing) {
            this.applyViewBobbing(tickDelta);
        }
    }

    public void render(float tickDelta) {
        if (!Display.isActive()) {
            if (System.currentTimeMillis() - this.lastActiveTime > 500L) {
                this.minecraft.pauseGame();
            }
        } else {
            this.lastActiveTime = System.currentTimeMillis();
        }
        if (this.minecraft.focused) {
            this.minecraft.mouse.tick();
            float f = this.minecraft.options.mouseSensitivity * 0.6f + 0.2f;
            float g = f * f * f * 8.0f;
            float h = (float)this.minecraft.mouse.x * g;
            float k = (float)this.minecraft.mouse.y * g;
            int m = 1;
            if (this.minecraft.options.invertMouseY) {
                m = -1;
            }
            this.minecraft.player.updateLocalPlayerCamera(h, k * (float)m);
        }
        if (this.minecraft.skipGameRender) {
            return;
        }
        Window window = new Window(this.minecraft.width, this.minecraft.height);
        int i = window.getWidth();
        int j = window.getHeight();
        int l = Mouse.getX() * i / this.minecraft.width;
        int n = j - Mouse.getY() * j / this.minecraft.height - 1;
        if (this.minecraft.world != null) {
            this.renderWorld(tickDelta);
            if (!Keyboard.isKeyDown((int)59)) {
                this.minecraft.gui.render(tickDelta, this.minecraft.screen != null, l, n);
            }
        } else {
            GL11.glViewport((int)0, (int)0, (int)this.minecraft.width, (int)this.minecraft.height);
            GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
            GL11.glClear((int)16640);
            GL11.glMatrixMode((int)GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            this.setupGuiState();
        }
        if (this.minecraft.screen != null) {
            GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
            this.minecraft.screen.render(l, n, tickDelta);
        }
    }

    public void renderWorld(float tickDelta) {
        this.pick(tickDelta);
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        WorldRenderer worldRenderer = this.minecraft.worldRenderer;
        ParticleManager particleManager = this.minecraft.particleManager;
        double d = clientPlayerEntity.prevX + (clientPlayerEntity.x - clientPlayerEntity.prevX) * (double)tickDelta;
        double e = clientPlayerEntity.prevY + (clientPlayerEntity.y - clientPlayerEntity.prevY) * (double)tickDelta;
        double f = clientPlayerEntity.prevZ + (clientPlayerEntity.z - clientPlayerEntity.prevZ) * (double)tickDelta;
        for (int i = 0; i < 2; ++i) {
            if (this.minecraft.options.anaglyph) {
                if (i == 0) {
                    GL11.glColorMask((boolean)false, (boolean)true, (boolean)true, (boolean)false);
                } else {
                    GL11.glColorMask((boolean)true, (boolean)false, (boolean)false, (boolean)false);
                }
            }
            GL11.glViewport((int)0, (int)0, (int)this.minecraft.width, (int)this.minecraft.height);
            this.setupClearColor(tickDelta);
            GL11.glClear((int)16640);
            GL11.glEnable((int)GL11.GL_CULL_FACE);
            this.setupCamera(tickDelta, i);
            Frustum.getInstance();
            if (this.minecraft.options.viewDistance < 2) {
                this.setupFog(-1);
                worldRenderer.renderSky(tickDelta);
            }
            GL11.glEnable((int)GL11.GL_FOG);
            this.setupFog(1);
            FrustumCuller frustumCuller = new FrustumCuller();
            frustumCuller.prepare(d, e, f);
            this.minecraft.worldRenderer.cullChunks(frustumCuller, tickDelta);
            this.minecraft.worldRenderer.compileChunks(clientPlayerEntity, false);
            this.setupFog(0);
            GL11.glEnable((int)GL11.GL_FOG);
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/terrain.png"));
            Lighting.turnOff();
            worldRenderer.render(clientPlayerEntity, 0, tickDelta);
            Lighting.turnOn();
            worldRenderer.renderEntities(clientPlayerEntity.getPosition(tickDelta), frustumCuller, tickDelta);
            particleManager.renderLit(clientPlayerEntity, tickDelta);
            Lighting.turnOff();
            this.setupFog(0);
            particleManager.render(clientPlayerEntity, tickDelta);
            if (this.minecraft.crosshairTarget != null && clientPlayerEntity.isSubmergedIn(Material.WATER)) {
                GL11.glDisable((int)GL11.GL_ALPHA_TEST);
                worldRenderer.renderMiningProgress(clientPlayerEntity, this.minecraft.crosshairTarget, 0, clientPlayerEntity.inventory.getSelectedItem(), tickDelta);
                worldRenderer.renderBlockOutline(clientPlayerEntity, this.minecraft.crosshairTarget, 0, clientPlayerEntity.inventory.getSelectedItem(), tickDelta);
                GL11.glEnable((int)GL11.GL_ALPHA_TEST);
            }
            GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
            this.setupFog(0);
            GL11.glEnable((int)GL11.GL_BLEND);
            GL11.glDisable((int)GL11.GL_CULL_FACE);
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.minecraft.textureManager.load("/terrain.png"));
            if (this.minecraft.options.fancyGraphics) {
                GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
                int j = worldRenderer.render(clientPlayerEntity, 1, tickDelta);
                GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
                if (this.minecraft.options.anaglyph) {
                    if (i == 0) {
                        GL11.glColorMask((boolean)false, (boolean)true, (boolean)true, (boolean)false);
                    } else {
                        GL11.glColorMask((boolean)true, (boolean)false, (boolean)false, (boolean)false);
                    }
                }
                if (j > 0) {
                    worldRenderer.renderLastChunks(1, tickDelta);
                }
            } else {
                worldRenderer.render(clientPlayerEntity, 1, tickDelta);
            }
            GL11.glDepthMask((boolean)true);
            GL11.glEnable((int)GL11.GL_CULL_FACE);
            GL11.glDisable((int)GL11.GL_BLEND);
            if (this.minecraft.crosshairTarget != null && !clientPlayerEntity.isSubmergedIn(Material.WATER)) {
                GL11.glDisable((int)GL11.GL_ALPHA_TEST);
                worldRenderer.renderMiningProgress(clientPlayerEntity, this.minecraft.crosshairTarget, 0, clientPlayerEntity.inventory.getSelectedItem(), tickDelta);
                worldRenderer.renderBlockOutline(clientPlayerEntity, this.minecraft.crosshairTarget, 0, clientPlayerEntity.inventory.getSelectedItem(), tickDelta);
                GL11.glEnable((int)GL11.GL_ALPHA_TEST);
            }
            GL11.glDisable((int)GL11.GL_FOG);
            if (this.targetEntity != null) {
                // empty if block
            }
            this.setupFog(0);
            GL11.glEnable((int)GL11.GL_FOG);
            worldRenderer.renderClouds(tickDelta);
            GL11.glDisable((int)GL11.GL_FOG);
            this.setupFog(1);
            GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
            this.renderItemInHand(tickDelta, i);
            if (this.minecraft.options.anaglyph) continue;
            return;
        }
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)false);
    }

    private void tickRain() {
        if (!this.minecraft.options.fancyGraphics) {
            return;
        }
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        World world = this.minecraft.world;
        int i = MathHelper.floor(clientPlayerEntity.x);
        int j = MathHelper.floor(clientPlayerEntity.y);
        int k = MathHelper.floor(clientPlayerEntity.z);
        int l = 16;
        for (int m = 0; m < 150; ++m) {
            int n = i + this.random.nextInt(l) - this.random.nextInt(l);
            int o = k + this.random.nextInt(l) - this.random.nextInt(l);
            int p = world.getPrecipitationHeight(n, o);
            int q = world.getBlock(n, p - 1, o);
            if (p > j + l || p < j - l) continue;
            float f = this.random.nextFloat();
            float g = this.random.nextFloat();
            if (q <= 0) continue;
            this.minecraft.particleManager.add(new RainSplashParticle(world, (float)n + f, (double)((float)p + 0.1f) - Block.BY_ID[q].minY, (float)o + g));
        }
    }

    public void setupGuiState() {
        Window window = new Window(this.minecraft.width, this.minecraft.height);
        int i = window.getWidth();
        int j = window.getHeight();
        GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)i, (double)j, (double)0.0, (double)1000.0, (double)3000.0);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-2000.0f);
    }

    private void setupClearColor(float tickDelta) {
        World world = this.minecraft.world;
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        float f = 1.0f / (float)(4 - this.minecraft.options.viewDistance);
        f = 1.0f - (float)Math.pow(f, 0.25);
        Vec3d vec3d = world.getSkyColor(this.minecraft.player, tickDelta);
        float g = (float)vec3d.x;
        float h = (float)vec3d.y;
        float i = (float)vec3d.z;
        Vec3d vec3d2 = world.getFogColor(tickDelta);
        this.fogRed = (float)vec3d2.x;
        this.fogGreen = (float)vec3d2.y;
        this.fogBlue = (float)vec3d2.z;
        this.fogRed += (g - this.fogRed) * f;
        this.fogGreen += (h - this.fogGreen) * f;
        this.fogBlue += (i - this.fogBlue) * f;
        if (clientPlayerEntity.isSubmergedIn(Material.WATER)) {
            this.fogRed = 0.02f;
            this.fogGreen = 0.02f;
            this.fogBlue = 0.2f;
        } else if (clientPlayerEntity.isSubmergedIn(Material.LAVA)) {
            this.fogRed = 0.6f;
            this.fogGreen = 0.1f;
            this.fogBlue = 0.0f;
        }
        float j = this.lastFogBrightness + (this.fogBrightness - this.lastFogBrightness) * tickDelta;
        this.fogRed *= j;
        this.fogGreen *= j;
        this.fogBlue *= j;
        if (this.minecraft.options.anaglyph) {
            float k = (this.fogRed * 30.0f + this.fogGreen * 59.0f + this.fogBlue * 11.0f) / 100.0f;
            float l = (this.fogRed * 30.0f + this.fogGreen * 70.0f) / 100.0f;
            float m = (this.fogRed * 30.0f + this.fogBlue * 70.0f) / 100.0f;
            this.fogRed = k;
            this.fogGreen = l;
            this.fogBlue = m;
        }
        GL11.glClearColor((float)this.fogRed, (float)this.fogGreen, (float)this.fogBlue, (float)0.0f);
    }

    private void setupFog(int type) {
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        GL11.glFog((int)2918, (FloatBuffer)this.updateColorBuffer(this.fogRed, this.fogGreen, this.fogBlue, 1.0f));
        GL11.glNormal3f((float)0.0f, (float)-1.0f, (float)0.0f);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        if (clientPlayerEntity.isSubmergedIn(Material.WATER)) {
            GL11.glFogi((int)2917, (int)2048);
            GL11.glFogf((int)2914, (float)0.1f);
            float f = 0.4f;
            float h = 0.4f;
            float j = 0.9f;
            if (this.minecraft.options.anaglyph) {
                float l = (f * 30.0f + h * 59.0f + j * 11.0f) / 100.0f;
                float n = (f * 30.0f + h * 70.0f) / 100.0f;
                float p = (f * 30.0f + j * 70.0f) / 100.0f;
                f = l;
                h = n;
                j = p;
            }
        } else if (clientPlayerEntity.isSubmergedIn(Material.LAVA)) {
            GL11.glFogi((int)2917, (int)2048);
            GL11.glFogf((int)2914, (float)2.0f);
            float g = 0.4f;
            float i = 0.3f;
            float k = 0.3f;
            if (this.minecraft.options.anaglyph) {
                float m = (g * 30.0f + i * 59.0f + k * 11.0f) / 100.0f;
                float o = (g * 30.0f + i * 70.0f) / 100.0f;
                float q = (g * 30.0f + k * 70.0f) / 100.0f;
                g = m;
                i = o;
                k = q;
            }
        } else {
            GL11.glFogi((int)2917, (int)9729);
            GL11.glFogf((int)2915, (float)(this.renderDistance * 0.25f));
            GL11.glFogf((int)2916, (float)this.renderDistance);
            if (type < 0) {
                GL11.glFogf((int)2915, (float)0.0f);
                GL11.glFogf((int)2916, (float)(this.renderDistance * 0.8f));
            }
            if (GLContext.getCapabilities().GL_NV_fog_distance) {
                GL11.glFogi((int)34138, (int)34139);
            }
            if (this.minecraft.world.dimension.unnatural) {
                GL11.glFogf((int)2915, (float)0.0f);
            }
        }
        GL11.glEnable((int)GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial((int)GL11.GL_FRONT, (int)GL11.GL_AMBIENT);
    }

    private FloatBuffer updateColorBuffer(float f1, float f2, float f3, float f4) {
        this.colorBuffer.clear();
        this.colorBuffer.put(f1).put(f2).put(f3).put(f4);
        this.colorBuffer.flip();
        return this.colorBuffer;
    }
}

