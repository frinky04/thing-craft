/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.carver;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.Generator;

public class NetherCaveCarver
extends Generator {
    protected void carveRoom(int chunkX, int chunkZ, byte[] blocks, double x, double y, double z) {
        this.carveTunnel(chunkX, chunkZ, blocks, x, y, z, 1.0f + this.random.nextFloat() * 6.0f, 0.0f, 0.0f, -1, -1, 0.5);
    }

    protected void carveTunnel(int chunkX, int chunkZ, byte[] blocks, double x, double y, double z, float baseWidth, float yaw, float pitch, int tunnel, int tunnelCount, double widthHeightRatio) {
        boolean l;
        double d = chunkX * 16 + 8;
        double e = chunkZ * 16 + 8;
        float f = 0.0f;
        float g = 0.0f;
        Random random = new Random(this.random.nextLong());
        if (tunnelCount <= 0) {
            int i = this.range * 16 - 16;
            tunnelCount = i - random.nextInt(i / 4);
        }
        boolean j = false;
        if (tunnel == -1) {
            tunnel = tunnelCount / 2;
            j = true;
        }
        int k = random.nextInt(tunnelCount / 2) + tunnelCount / 4;
        boolean bl = l = random.nextInt(6) == 0;
        while (tunnel < tunnelCount) {
            double h = 1.5 + (double)(MathHelper.sin((float)tunnel * (float)Math.PI / (float)tunnelCount) * baseWidth * 1.0f);
            double m = h * widthHeightRatio;
            float n = MathHelper.cos(pitch);
            float o = MathHelper.sin(pitch);
            x += (double)(MathHelper.cos(yaw) * n);
            y += (double)o;
            z += (double)(MathHelper.sin(yaw) * n);
            pitch = l ? (pitch *= 0.92f) : (pitch *= 0.7f);
            pitch += g * 0.1f;
            yaw += f * 0.1f;
            g *= 0.9f;
            f *= 0.75f;
            g += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0f;
            f += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0f;
            if (!j && tunnel == k && baseWidth > 1.0f) {
                this.carveTunnel(chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5f + 0.5f, yaw - 1.5707964f, pitch / 3.0f, tunnel, tunnelCount, 1.0);
                this.carveTunnel(chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5f + 0.5f, yaw + 1.5707964f, pitch / 3.0f, tunnel, tunnelCount, 1.0);
                return;
            }
            if (j || random.nextInt(4) != 0) {
                double p = x - d;
                double s = z - e;
                double v = tunnelCount - tunnel;
                double ab = baseWidth + 2.0f + 16.0f;
                if (p * p + s * s - v * v > ab * ab) {
                    return;
                }
                if (!(x < d - 16.0 - h * 2.0 || z < e - 16.0 - h * 2.0 || x > d + 16.0 + h * 2.0 || z > e + 16.0 + h * 2.0)) {
                    int ad;
                    int q = MathHelper.floor(x - h) - chunkX * 16 - 1;
                    int r = MathHelper.floor(x + h) - chunkX * 16 + 1;
                    int t = MathHelper.floor(y - m) - 1;
                    int u = MathHelper.floor(y + m) + 1;
                    int w = MathHelper.floor(z - h) - chunkZ * 16 - 1;
                    int aa = MathHelper.floor(z + h) - chunkZ * 16 + 1;
                    if (q < 0) {
                        q = 0;
                    }
                    if (r > 16) {
                        r = 16;
                    }
                    if (t < 1) {
                        t = 1;
                    }
                    if (u > 120) {
                        u = 120;
                    }
                    if (w < 0) {
                        w = 0;
                    }
                    if (aa > 16) {
                        aa = 16;
                    }
                    boolean ac = false;
                    for (ad = q; !ac && ad < r; ++ad) {
                        for (int ae = w; !ac && ae < aa; ++ae) {
                            for (int ag = u + 1; !ac && ag >= t - 1; --ag) {
                                int ah = (ad * 16 + ae) * 128 + ag;
                                if (ag < 0 || ag >= 128) continue;
                                if (blocks[ah] == Block.FLOWING_LAVA.id || blocks[ah] == Block.LAVA.id) {
                                    ac = true;
                                }
                                if (ag == t - 1 || ad == q || ad == r - 1 || ae == w || ae == aa - 1) continue;
                                ag = t;
                            }
                        }
                    }
                    if (!ac) {
                        for (ad = q; ad < r; ++ad) {
                            double af = ((double)(ad + chunkX * 16) + 0.5 - x) / h;
                            for (int ai = w; ai < aa; ++ai) {
                                double aj = ((double)(ai + chunkZ * 16) + 0.5 - z) / h;
                                int ak = (ad * 16 + ai) * 128 + u;
                                for (int al = u - 1; al >= t; --al) {
                                    byte an;
                                    double am = ((double)al + 0.5 - y) / m;
                                    if (am > -0.7 && af * af + am * am + aj * aj < 1.0 && ((an = blocks[ak]) == Block.NETHERRACK.id || an == Block.DIRT.id || an == Block.GRASS.id)) {
                                        blocks[ak] = 0;
                                    }
                                    --ak;
                                }
                            }
                        }
                        if (j) break;
                    }
                }
            }
            ++tunnel;
        }
    }

    protected void place(World world, int startChunkX, int startChunkZ, int chunkX, int chunkZ, byte[] blocks) {
        int i = this.random.nextInt(this.random.nextInt(this.random.nextInt(10) + 1) + 1);
        if (this.random.nextInt(5) != 0) {
            i = 0;
        }
        for (int j = 0; j < i; ++j) {
            double d = startChunkX * 16 + this.random.nextInt(16);
            double e = this.random.nextInt(128);
            double f = startChunkZ * 16 + this.random.nextInt(16);
            int k = 1;
            if (this.random.nextInt(4) == 0) {
                this.carveRoom(chunkX, chunkZ, blocks, d, e, f);
                k += this.random.nextInt(4);
            }
            for (int l = 0; l < k; ++l) {
                float g = this.random.nextFloat() * (float)Math.PI * 2.0f;
                float h = (this.random.nextFloat() - 0.5f) * 2.0f / 8.0f;
                float m = this.random.nextFloat() * 2.0f + this.random.nextFloat();
                this.carveTunnel(chunkX, chunkZ, blocks, d, e, f, m * 2.0f, g, h, 0, 0, 0.5);
            }
        }
    }
}

