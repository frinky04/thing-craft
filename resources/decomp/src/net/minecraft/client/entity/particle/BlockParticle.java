/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class BlockParticle
extends Particle {
    private Block block;

    public BlockParticle(World world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Block block) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.block = block;
        this.sprite = block.sprite;
        this.gravity = block.gravity;
        this.blue = 0.6f;
        this.green = 0.6f;
        this.red = 0.6f;
        this.size /= 2.0f;
    }

    public BlockParticle init(int dx, int dy, int dz) {
        if (this.block == Block.GRASS) {
            return this;
        }
        int i = this.block.getColor(this.world, dx, dy, dz);
        this.red *= (float)(i >> 16 & 0xFF) / 255.0f;
        this.green *= (float)(i >> 8 & 0xFF) / 255.0f;
        this.blue *= (float)(i & 0xFF) / 255.0f;
        return this;
    }

    public int getAtlasType() {
        return 1;
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = ((float)(this.sprite % 16) + this.offsetU / 4.0f) / 16.0f;
        float g = f + 0.015609375f;
        float h = ((float)(this.sprite / 16) + this.offsetV / 4.0f) / 16.0f;
        float i = h + 0.015609375f;
        float j = 0.1f * this.size;
        float k = (float)(this.lastX + (this.x - this.lastX) * (double)tickDelta - lerpCameraX);
        float l = (float)(this.lastY + (this.y - this.lastY) * (double)tickDelta - lerpCameraY);
        float m = (float)(this.lastZ + (this.z - this.lastZ) * (double)tickDelta - lerpCameraZ);
        float n = this.getBrightness(tickDelta);
        tesselator.color(n * this.red, n * this.green, n * this.blue);
        tesselator.vertex(k - dx * j - forwards * j, l - dy * j, m - dz * j - sideways * j, f, i);
        tesselator.vertex(k - dx * j + forwards * j, l + dy * j, m - dz * j + sideways * j, f, h);
        tesselator.vertex(k + dx * j + forwards * j, l + dy * j, m + dz * j + sideways * j, g, h);
        tesselator.vertex(k + dx * j - forwards * j, l - dy * j, m + dz * j - sideways * j, g, i);
    }
}

