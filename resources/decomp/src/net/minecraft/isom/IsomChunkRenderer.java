/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.isom;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.isom.IsomRenderChunk;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

@Environment(value=EnvType.CLIENT)
public class IsomChunkRenderer {
    private float[] colors = new float[768];
    private int[] pixels = new int[5120];
    private int[] height = new int[5120];
    private int[] waterHeight = new int[5120];
    private int[] waterBrightness = new int[5120];
    private int[] depth = new int[34];
    private int[] sprites = new int[768];

    public IsomChunkRenderer() {
        try {
            BufferedImage bufferedImage = ImageIO.read(IsomChunkRenderer.class.getResource("/terrain.png"));
            int[] is = new int[65536];
            bufferedImage.getRGB(0, 0, 256, 256, is, 0, 256);
            for (int j = 0; j < 256; ++j) {
                int k = 0;
                int l = 0;
                int m = 0;
                int n = j % 16 * 16;
                int o = j / 16 * 16;
                int p = 0;
                for (int q = 0; q < 16; ++q) {
                    for (int r = 0; r < 16; ++r) {
                        int s = is[r + n + (q + o) * 256];
                        int t = s >> 24 & 0xFF;
                        if (t <= 128) continue;
                        k += s >> 16 & 0xFF;
                        l += s >> 8 & 0xFF;
                        m += s & 0xFF;
                        ++p;
                    }
                    if (p == 0) {
                        ++p;
                    }
                    this.colors[j * 3 + 0] = k / p;
                    this.colors[j * 3 + 1] = l / p;
                    this.colors[j * 3 + 2] = m / p;
                }
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
        for (int i = 0; i < 256; ++i) {
            if (Block.BY_ID[i] == null) continue;
            this.sprites[i * 3 + 0] = Block.BY_ID[i].getSprite(1);
            this.sprites[i * 3 + 1] = Block.BY_ID[i].getSprite(2);
            this.sprites[i * 3 + 2] = Block.BY_ID[i].getSprite(3);
        }
    }

    public void render(IsomRenderChunk chunk) {
        World world = chunk.world;
        if (world == null) {
            chunk.empty = true;
            chunk.rendered = true;
            return;
        }
        int i = chunk.chunkX * 16;
        int j = chunk.chunkZ * 16;
        int k = i + 16;
        int l = j + 16;
        WorldChunk worldChunk = world.getChunkAt(chunk.chunkX, chunk.chunkZ);
        if (worldChunk.dummy) {
            chunk.empty = true;
            chunk.rendered = true;
            return;
        }
        chunk.empty = false;
        Arrays.fill(this.height, 0);
        Arrays.fill(this.waterHeight, 0);
        Arrays.fill(this.depth, 160);
        for (int m = l - 1; m >= j; --m) {
            for (int n = k - 1; n >= i; --n) {
                int o = n - i;
                int p = m - j;
                int q = o + p;
                boolean r = true;
                for (int s = 0; s < 128; ++s) {
                    int t = p - o - s + 160 - 16;
                    if (t >= this.depth[q] && t >= this.depth[q + 1]) continue;
                    Block block = Block.BY_ID[world.getBlock(n, s, m)];
                    if (block == null) {
                        r = false;
                        continue;
                    }
                    if (block.material == Material.WATER) {
                        int u = world.getBlock(n, s + 1, m);
                        if (u != 0 && Block.BY_ID[u].material == Material.WATER) continue;
                        float g = (float)s / 127.0f * 0.6f + 0.4f;
                        float h = world.getBrightness(n, s + 1, m) * g;
                        if (t < 0 || t >= 160) continue;
                        int z = q + t * 32;
                        if (q >= 0 && q <= 32 && this.waterHeight[z] <= s) {
                            this.waterHeight[z] = s;
                            this.waterBrightness[z] = (int)(h * 127.0f);
                        }
                        if (q >= -1 && q <= 31 && this.waterHeight[z + 1] <= s) {
                            this.waterHeight[z + 1] = s;
                            this.waterBrightness[z + 1] = (int)(h * 127.0f);
                        }
                        r = false;
                        continue;
                    }
                    if (r) {
                        if (t < this.depth[q]) {
                            this.depth[q] = t;
                        }
                        if (t < this.depth[q + 1]) {
                            this.depth[q + 1] = t;
                        }
                    }
                    float f = (float)s / 127.0f * 0.6f + 0.4f;
                    if (t >= 0 && t < 160) {
                        int v = q + t * 32;
                        int x = this.sprites[block.id * 3 + 0];
                        float aa = (world.getBrightness(n, s + 1, m) * 0.8f + 0.2f) * f;
                        int ac = x;
                        if (q >= 0) {
                            float ae = aa;
                            if (this.height[v] <= s) {
                                this.height[v] = s;
                                this.pixels[v] = 0xFF000000 | (int)(this.colors[ac * 3 + 0] * ae) << 16 | (int)(this.colors[ac * 3 + 1] * ae) << 8 | (int)(this.colors[ac * 3 + 2] * ae);
                            }
                        }
                        if (q < 31) {
                            float af = aa * 0.9f;
                            if (this.height[v + 1] <= s) {
                                this.height[v + 1] = s;
                                this.pixels[v + 1] = 0xFF000000 | (int)(this.colors[ac * 3 + 0] * af) << 16 | (int)(this.colors[ac * 3 + 1] * af) << 8 | (int)(this.colors[ac * 3 + 2] * af);
                            }
                        }
                    }
                    if (t < -1 || t >= 159) continue;
                    int w = q + (t + 1) * 32;
                    int y = this.sprites[block.id * 3 + 1];
                    float ab = world.getBrightness(n - 1, s, m) * 0.8f + 0.2f;
                    int ad = this.sprites[block.id * 3 + 2];
                    float ag = world.getBrightness(n, s, m + 1) * 0.8f + 0.2f;
                    if (q >= 0) {
                        float ah = ab * f * 0.6f;
                        if (this.height[w] <= s - 1) {
                            this.height[w] = s - 1;
                            this.pixels[w] = 0xFF000000 | (int)(this.colors[y * 3 + 0] * ah) << 16 | (int)(this.colors[y * 3 + 1] * ah) << 8 | (int)(this.colors[y * 3 + 2] * ah);
                        }
                    }
                    if (q >= 31) continue;
                    float ai = ag * 0.9f * f * 0.4f;
                    if (this.height[w + 1] > s - 1) continue;
                    this.height[w + 1] = s - 1;
                    this.pixels[w + 1] = 0xFF000000 | (int)(this.colors[ad * 3 + 0] * ai) << 16 | (int)(this.colors[ad * 3 + 1] * ai) << 8 | (int)(this.colors[ad * 3 + 2] * ai);
                }
            }
        }
        this.postProcess();
        if (chunk.image == null) {
            chunk.image = new BufferedImage(32, 160, 2);
        }
        chunk.image.setRGB(0, 0, 32, 160, this.pixels, 0, 32);
        chunk.rendered = true;
    }

    private void postProcess() {
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 160; ++j) {
                int k = i + j * 32;
                if (this.height[k] == 0) {
                    this.pixels[k] = 0;
                }
                if (this.waterHeight[k] <= this.height[k]) continue;
                int l = this.pixels[k] >> 24 & 0xFF;
                this.pixels[k] = ((this.pixels[k] & 0xFEFEFE) >> 1) + this.waterBrightness[k];
                if (l < 128) {
                    this.pixels[k] = Integer.MIN_VALUE + this.waterBrightness[k] * 2;
                    continue;
                }
                int n = k;
                this.pixels[n] = this.pixels[n] | 0xFF000000;
            }
        }
    }
}

