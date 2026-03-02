/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.entity.particle.BlockParticle;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ParticleManager {
    protected World world;
    private List[] particles = new List[4];
    private TextureManager textureManager;
    private Random random = new Random();

    public ParticleManager(World world, TextureManager textureManager) {
        if (world != null) {
            this.world = world;
        }
        this.textureManager = textureManager;
        for (int i = 0; i < 4; ++i) {
            this.particles[i] = new ArrayList();
        }
    }

    public void add(Particle particle) {
        int i = particle.getAtlasType();
        this.particles[i].add(particle);
    }

    public void tick() {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < this.particles[i].size(); ++j) {
                Particle particle = (Particle)this.particles[i].get(j);
                particle.tick();
                if (!particle.removed) continue;
                this.particles[i].remove(j--);
            }
        }
    }

    public void render(Entity camera, float tickDelta) {
        float f = MathHelper.cos(camera.yaw * (float)Math.PI / 180.0f);
        float g = MathHelper.sin(camera.yaw * (float)Math.PI / 180.0f);
        float h = -g * MathHelper.sin(camera.pitch * (float)Math.PI / 180.0f);
        float i = f * MathHelper.sin(camera.pitch * (float)Math.PI / 180.0f);
        float j = MathHelper.cos(camera.pitch * (float)Math.PI / 180.0f);
        Particle.lerpCameraX = camera.prevX + (camera.x - camera.prevX) * (double)tickDelta;
        Particle.lerpCameraY = camera.prevY + (camera.y - camera.prevY) * (double)tickDelta;
        Particle.lerpCameraZ = camera.prevZ + (camera.z - camera.prevZ) * (double)tickDelta;
        for (int k = 0; k < 3; ++k) {
            if (this.particles[k].size() == 0) continue;
            int l = 0;
            if (k == 0) {
                l = this.textureManager.load("/particles.png");
            }
            if (k == 1) {
                l = this.textureManager.load("/terrain.png");
            }
            if (k == 2) {
                l = this.textureManager.load("/gui/items.png");
            }
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)l);
            Tesselator tesselator = Tesselator.INSTANCE;
            tesselator.begin();
            for (int m = 0; m < this.particles[k].size(); ++m) {
                Particle particle = (Particle)this.particles[k].get(m);
                particle.render(tesselator, tickDelta, f, j, g, h, i);
            }
            tesselator.end();
        }
    }

    public void renderLit(Entity camera, float tickDelta) {
        int i = 3;
        if (this.particles[i].size() == 0) {
            return;
        }
        Tesselator tesselator = Tesselator.INSTANCE;
        for (int j = 0; j < this.particles[i].size(); ++j) {
            Particle particle = (Particle)this.particles[i].get(j);
            particle.render(tesselator, tickDelta, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    public void setWorld(World world) {
        this.world = world;
        for (int i = 0; i < 4; ++i) {
            this.particles[i].clear();
        }
    }

    public void handleBlockBreaking(int x, int y, int z) {
        int i = this.world.getBlock(x, y, z);
        if (i == 0) {
            return;
        }
        Block block = Block.BY_ID[i];
        int j = 4;
        for (int k = 0; k < j; ++k) {
            for (int l = 0; l < j; ++l) {
                for (int m = 0; m < j; ++m) {
                    double d = (double)x + ((double)k + 0.5) / (double)j;
                    double e = (double)y + ((double)l + 0.5) / (double)j;
                    double f = (double)z + ((double)m + 0.5) / (double)j;
                    this.add(new BlockParticle(this.world, d, e, f, d - (double)x - 0.5, e - (double)y - 0.5, f - (double)z - 0.5, block).init(x, y, z));
                }
            }
        }
    }

    public void handleBlockMining(int x, int y, int z, int face) {
        int i = this.world.getBlock(x, y, z);
        if (i == 0) {
            return;
        }
        Block block = Block.BY_ID[i];
        float f = 0.1f;
        double d = (double)x + this.random.nextDouble() * (block.maxX - block.minX - (double)(f * 2.0f)) + (double)f + block.minX;
        double e = (double)y + this.random.nextDouble() * (block.maxY - block.minY - (double)(f * 2.0f)) + (double)f + block.minY;
        double g = (double)z + this.random.nextDouble() * (block.maxZ - block.minZ - (double)(f * 2.0f)) + (double)f + block.minZ;
        if (face == 0) {
            e = (double)y + block.minY - (double)f;
        }
        if (face == 1) {
            e = (double)y + block.maxY + (double)f;
        }
        if (face == 2) {
            g = (double)z + block.minZ - (double)f;
        }
        if (face == 3) {
            g = (double)z + block.maxZ + (double)f;
        }
        if (face == 4) {
            d = (double)x + block.minX - (double)f;
        }
        if (face == 5) {
            d = (double)x + block.maxX + (double)f;
        }
        this.add(new BlockParticle(this.world, d, e, g, 0.0, 0.0, 0.0, block).init(x, y, z).multiplyVelocity(0.2f).multiplySize(0.6f));
    }

    public String getDebugInfo() {
        return "" + (this.particles[0].size() + this.particles[1].size() + this.particles[2].size());
    }
}

