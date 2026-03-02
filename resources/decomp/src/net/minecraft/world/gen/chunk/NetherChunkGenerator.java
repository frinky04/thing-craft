/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.chunk;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.Generator;
import net.minecraft.world.gen.carver.NetherCaveCarver;
import net.minecraft.world.gen.feature.FirePatchFeature;
import net.minecraft.world.gen.feature.GlowstoneClusterFeature;
import net.minecraft.world.gen.feature.NetherLiquidFallFeature;
import net.minecraft.world.gen.feature.PlantFeature;
import net.minecraft.world.gen.feature.RareGlowstoneClusterFeature;
import net.minecraft.world.gen.noise.PerlinNoise;

public class NetherChunkGenerator
implements ChunkSource {
    private Random random;
    private PerlinNoise minLimitPerlinNoise;
    private PerlinNoise maxLimitPerlinNoise;
    private PerlinNoise perlinNoise1;
    private PerlinNoise perlinNoise2;
    private PerlinNoise perlinNoise3;
    public PerlinNoise scaleNoise;
    public PerlinNoise depthNoise;
    private World world;
    private double[] heightMap;
    private double[] sandBuffer = new double[256];
    private double[] gravelBuffer = new double[256];
    private double[] depthBuffer = new double[256];
    private Generator cave = new NetherCaveCarver();
    double[] perlinNoiseBuffer;
    double[] minLimitPerlinNoiseBuffer;
    double[] maxLimitPerlinNoiseBuffer;
    double[] scaleNoiseBuffer;
    double[] depthNoiseBuffer;

    public NetherChunkGenerator(World world, long seed) {
        this.world = world;
        this.random = new Random(seed);
        this.minLimitPerlinNoise = new PerlinNoise(this.random, 16);
        this.maxLimitPerlinNoise = new PerlinNoise(this.random, 16);
        this.perlinNoise1 = new PerlinNoise(this.random, 8);
        this.perlinNoise2 = new PerlinNoise(this.random, 4);
        this.perlinNoise3 = new PerlinNoise(this.random, 4);
        this.scaleNoise = new PerlinNoise(this.random, 10);
        this.depthNoise = new PerlinNoise(this.random, 16);
    }

    public void buildTerrain(int chunkX, int chunkZ, byte[] blocks) {
        int i = 4;
        int j = 32;
        int k = i + 1;
        int l = 17;
        int m = i + 1;
        this.heightMap = this.generateHeightMap(this.heightMap, chunkX * i, 0, chunkZ * i, k, l, m);
        for (int n = 0; n < i; ++n) {
            for (int o = 0; o < i; ++o) {
                for (int p = 0; p < 16; ++p) {
                    double d = 0.125;
                    double e = this.heightMap[((n + 0) * m + (o + 0)) * l + (p + 0)];
                    double f = this.heightMap[((n + 0) * m + (o + 1)) * l + (p + 0)];
                    double g = this.heightMap[((n + 1) * m + (o + 0)) * l + (p + 0)];
                    double h = this.heightMap[((n + 1) * m + (o + 1)) * l + (p + 0)];
                    double q = (this.heightMap[((n + 0) * m + (o + 0)) * l + (p + 1)] - e) * d;
                    double r = (this.heightMap[((n + 0) * m + (o + 1)) * l + (p + 1)] - f) * d;
                    double s = (this.heightMap[((n + 1) * m + (o + 0)) * l + (p + 1)] - g) * d;
                    double t = (this.heightMap[((n + 1) * m + (o + 1)) * l + (p + 1)] - h) * d;
                    for (int u = 0; u < 8; ++u) {
                        double v = 0.25;
                        double w = e;
                        double x = f;
                        double y = (g - e) * v;
                        double z = (h - f) * v;
                        for (int aa = 0; aa < 4; ++aa) {
                            int ab = aa + n * 4 << 11 | 0 + o * 4 << 7 | p * 8 + u;
                            int ac = 128;
                            double ad = 0.25;
                            double ae = w;
                            double af = (x - w) * ad;
                            for (int ag = 0; ag < 4; ++ag) {
                                int ah = 0;
                                if (p * 8 + u < j) {
                                    ah = Block.LAVA.id;
                                }
                                if (ae > 0.0) {
                                    ah = Block.NETHERRACK.id;
                                }
                                blocks[ab] = (byte)ah;
                                ab += ac;
                                ae += af;
                            }
                            w += y;
                            x += z;
                        }
                        e += q;
                        f += r;
                        g += s;
                        h += t;
                    }
                }
            }
        }
    }

    public void buildSurfaces(int chunkX, int chunkZ, byte[] blocks) {
        int i = 64;
        double d = 0.03125;
        this.sandBuffer = this.perlinNoise2.getRegion(this.sandBuffer, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, d, d, 1.0);
        this.gravelBuffer = this.perlinNoise2.getRegion(this.gravelBuffer, chunkZ * 16, 109.0134, chunkX * 16, 16, 1, 16, d, 1.0, d);
        this.depthBuffer = this.perlinNoise3.getRegion(this.depthBuffer, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, d * 2.0, d * 2.0, d * 2.0);
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                boolean l = this.sandBuffer[j + k * 16] + this.random.nextDouble() * 0.2 > 0.0;
                boolean m = this.gravelBuffer[j + k * 16] + this.random.nextDouble() * 0.2 > 0.0;
                int n = (int)(this.depthBuffer[j + k * 16] / 3.0 + 3.0 + this.random.nextDouble() * 0.25);
                int o = -1;
                byte p = (byte)Block.NETHERRACK.id;
                byte q = (byte)Block.NETHERRACK.id;
                for (int r = 127; r >= 0; --r) {
                    int s = (j * 16 + k) * 128 + r;
                    if (r >= 127 - this.random.nextInt(5)) {
                        blocks[s] = (byte)Block.BEDROCK.id;
                        continue;
                    }
                    if (r <= 0 + this.random.nextInt(5)) {
                        blocks[s] = (byte)Block.BEDROCK.id;
                        continue;
                    }
                    byte t = blocks[s];
                    if (t == 0) {
                        o = -1;
                        continue;
                    }
                    if (t != Block.NETHERRACK.id) continue;
                    if (o == -1) {
                        if (n <= 0) {
                            p = 0;
                            q = (byte)Block.NETHERRACK.id;
                        } else if (r >= i - 4 && r <= i + 1) {
                            p = (byte)Block.NETHERRACK.id;
                            q = (byte)Block.NETHERRACK.id;
                            if (m) {
                                p = (byte)Block.GRAVEL.id;
                            }
                            if (m) {
                                q = (byte)Block.NETHERRACK.id;
                            }
                            if (l) {
                                p = (byte)Block.SOUL_SAND.id;
                            }
                            if (l) {
                                q = (byte)Block.SOUL_SAND.id;
                            }
                        }
                        if (r < i && p == 0) {
                            p = (byte)Block.LAVA.id;
                        }
                        o = n;
                        if (r >= i - 1) {
                            blocks[s] = p;
                            continue;
                        }
                        blocks[s] = q;
                        continue;
                    }
                    if (o <= 0) continue;
                    --o;
                    blocks[s] = q;
                }
            }
        }
    }

    public WorldChunk getChunk(int chunkX, int chunkZ) {
        this.random.setSeed((long)chunkX * 341873128712L + (long)chunkZ * 132897987541L);
        byte[] bs = new byte[32768];
        this.buildTerrain(chunkX, chunkZ, bs);
        this.buildSurfaces(chunkX, chunkZ, bs);
        this.cave.place(this, this.world, chunkX, chunkZ, bs);
        WorldChunk worldChunk = new WorldChunk(this.world, bs, chunkX, chunkZ);
        worldChunk.populateHeightMap();
        worldChunk.populateBlockLight();
        return worldChunk;
    }

    private double[] generateHeightMap(double[] heightMap, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        int k;
        if (heightMap == null) {
            heightMap = new double[sizeX * sizeY * sizeZ];
        }
        double d = 684.412;
        double e = 2053.236;
        this.scaleNoiseBuffer = this.scaleNoise.getRegion(this.scaleNoiseBuffer, x, y, z, sizeX, 1, sizeZ, 1.0, 0.0, 1.0);
        this.depthNoiseBuffer = this.depthNoise.getRegion(this.depthNoiseBuffer, x, y, z, sizeX, 1, sizeZ, 100.0, 0.0, 100.0);
        this.perlinNoiseBuffer = this.perlinNoise1.getRegion(this.perlinNoiseBuffer, x, y, z, sizeX, sizeY, sizeZ, d / 80.0, e / 60.0, d / 80.0);
        this.minLimitPerlinNoiseBuffer = this.minLimitPerlinNoise.getRegion(this.minLimitPerlinNoiseBuffer, x, y, z, sizeX, sizeY, sizeZ, d, e, d);
        this.maxLimitPerlinNoiseBuffer = this.maxLimitPerlinNoise.getRegion(this.maxLimitPerlinNoiseBuffer, x, y, z, sizeX, sizeY, sizeZ, d, e, d);
        int i = 0;
        int j = 0;
        double[] ds = new double[sizeY];
        for (k = 0; k < sizeY; ++k) {
            ds[k] = Math.cos((double)k * Math.PI * 6.0 / (double)sizeY) * 2.0;
            double f = k;
            if (k > sizeY / 2) {
                f = sizeY - 1 - k;
            }
            if (!(f < 4.0)) continue;
            f = 4.0 - f;
            int n = k;
            ds[n] = ds[n] - f * f * f * 10.0;
        }
        for (k = 0; k < sizeX; ++k) {
            for (int l = 0; l < sizeZ; ++l) {
                double g = (this.scaleNoiseBuffer[j] + 256.0) / 512.0;
                if (g > 1.0) {
                    g = 1.0;
                }
                double h = 0.0;
                double m = this.depthNoiseBuffer[j] / 8000.0;
                if (m < 0.0) {
                    m = -m;
                }
                if ((m = m * 3.0 - 3.0) < 0.0) {
                    if ((m /= 2.0) < -1.0) {
                        m = -1.0;
                    }
                    m /= 1.4;
                    m /= 2.0;
                    g = 0.0;
                } else {
                    if (m > 1.0) {
                        m = 1.0;
                    }
                    m /= 6.0;
                }
                g += 0.5;
                m = m * (double)sizeY / 16.0;
                ++j;
                for (int n = 0; n < sizeY; ++n) {
                    double o = 0.0;
                    double p = ds[n];
                    double q = this.minLimitPerlinNoiseBuffer[i] / 512.0;
                    double r = this.maxLimitPerlinNoiseBuffer[i] / 512.0;
                    double s = (this.perlinNoiseBuffer[i] / 10.0 + 1.0) / 2.0;
                    o = s < 0.0 ? q : (s > 1.0 ? r : q + (r - q) * s);
                    o -= p;
                    if (n > sizeY - 4) {
                        double t = (float)(n - (sizeY - 4)) / 3.0f;
                        o = o * (1.0 - t) + -10.0 * t;
                    }
                    if ((double)n < h) {
                        double u = (h - (double)n) / 4.0;
                        if (u < 0.0) {
                            u = 0.0;
                        }
                        if (u > 1.0) {
                            u = 1.0;
                        }
                        o = o * (1.0 - u) + -10.0 * u;
                    }
                    heightMap[i] = o;
                    ++i;
                }
            }
        }
        return heightMap;
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        return true;
    }

    public void populateChunk(ChunkSource source, int chunkX, int chunkZ) {
        int m;
        int k;
        FallingBlock.fallImmediately = true;
        int i = chunkX * 16;
        int j = chunkZ * 16;
        for (k = 0; k < 8; ++k) {
            int l = i + this.random.nextInt(16) + 8;
            int n = this.random.nextInt(120) + 4;
            int t = j + this.random.nextInt(16) + 8;
            new NetherLiquidFallFeature(Block.FLOWING_LAVA.id).place(this.world, this.random, l, n, t);
        }
        k = this.random.nextInt(this.random.nextInt(10) + 1) + 1;
        for (m = 0; m < k; ++m) {
            int o = i + this.random.nextInt(16) + 8;
            int u = this.random.nextInt(120) + 4;
            int z = j + this.random.nextInt(16) + 8;
            new FirePatchFeature().place(this.world, this.random, o, u, z);
        }
        k = this.random.nextInt(this.random.nextInt(10) + 1);
        for (m = 0; m < k; ++m) {
            int p = i + this.random.nextInt(16) + 8;
            int v = this.random.nextInt(120) + 4;
            int aa = j + this.random.nextInt(16) + 8;
            new RareGlowstoneClusterFeature().place(this.world, this.random, p, v, aa);
        }
        for (m = 0; m < 10; ++m) {
            int q = i + this.random.nextInt(16) + 8;
            int w = this.random.nextInt(128);
            int ab = j + this.random.nextInt(16) + 8;
            new GlowstoneClusterFeature().place(this.world, this.random, q, w, ab);
        }
        if (this.random.nextInt(1) == 0) {
            m = i + this.random.nextInt(16) + 8;
            int r = this.random.nextInt(128);
            int x = j + this.random.nextInt(16) + 8;
            new PlantFeature(Block.BROWN_MUSHROOM.id).place(this.world, this.random, m, r, x);
        }
        if (this.random.nextInt(1) == 0) {
            m = i + this.random.nextInt(16) + 8;
            int s = this.random.nextInt(128);
            int y = j + this.random.nextInt(16) + 8;
            new PlantFeature(Block.RED_MUSHROOM.id).place(this.world, this.random, m, s, y);
        }
        FallingBlock.fallImmediately = false;
    }

    public boolean save(boolean saveEntities, ProgressListener listener) {
        return true;
    }

    public boolean tick() {
        return false;
    }

    public boolean shouldSave() {
        return true;
    }
}

