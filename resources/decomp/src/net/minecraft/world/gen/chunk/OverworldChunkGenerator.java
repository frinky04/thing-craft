/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.chunk;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.Generator;
import net.minecraft.world.gen.carver.CaveWorldCarver;
import net.minecraft.world.gen.feature.CactusFeature;
import net.minecraft.world.gen.feature.ClayPatchFeature;
import net.minecraft.world.gen.feature.DungeonFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.LakeFeature;
import net.minecraft.world.gen.feature.LargeOakTreeFeature;
import net.minecraft.world.gen.feature.LiquidFallFeature;
import net.minecraft.world.gen.feature.PlantFeature;
import net.minecraft.world.gen.feature.PumpkinPatchFeature;
import net.minecraft.world.gen.feature.SugarcaneFeature;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.VeinFeature;
import net.minecraft.world.gen.noise.PerlinNoise;

public class OverworldChunkGenerator
implements ChunkSource {
    private Random random;
    private PerlinNoise minLimitPerlinNoise;
    private PerlinNoise maxLimitPerlinNoise;
    private PerlinNoise perlinNoise1;
    private PerlinNoise perlinNoise2;
    private PerlinNoise perlinNoise3;
    public PerlinNoise scaleNoise;
    public PerlinNoise depthNoise;
    public PerlinNoise forestNoise;
    private World world;
    private double[] heightMap;
    private double[] sandBuffer = new double[256];
    private double[] gravelBuffer = new double[256];
    private double[] depthBuffer = new double[256];
    private Generator cave = new CaveWorldCarver();
    private Biome[] biomes;
    double[] perlinNoiseBuffer;
    double[] minLimitPerlinNoiseBuffer;
    double[] maxLimitPerlinNoiseBuffer;
    double[] scaleNoiseBuffer;
    double[] depthNoiseBuffer;
    int[][] waterDepths = new int[32][32];
    private double[] temperatures;

    public OverworldChunkGenerator(World world, long seed) {
        this.world = world;
        this.random = new Random(seed);
        this.minLimitPerlinNoise = new PerlinNoise(this.random, 16);
        this.maxLimitPerlinNoise = new PerlinNoise(this.random, 16);
        this.perlinNoise1 = new PerlinNoise(this.random, 8);
        this.perlinNoise2 = new PerlinNoise(this.random, 4);
        this.perlinNoise3 = new PerlinNoise(this.random, 4);
        this.scaleNoise = new PerlinNoise(this.random, 10);
        this.depthNoise = new PerlinNoise(this.random, 16);
        this.forestNoise = new PerlinNoise(this.random, 8);
    }

    public void buildTerrain(int chunkX, int chunkZ, byte[] blocks, Biome[] biomes, double[] temperatures) {
        int i = 4;
        int j = 64;
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
                                double ah = temperatures[(n * 4 + aa) * 16 + (o * 4 + ag)];
                                int ai = 0;
                                if (p * 8 + u < j) {
                                    ai = ah < 0.5 && p * 8 + u >= j - 1 ? Block.ICE.id : Block.WATER.id;
                                }
                                if (ae > 0.0) {
                                    ai = Block.STONE.id;
                                }
                                blocks[ab] = (byte)ai;
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

    public void buildSurfaces(int chunkX, int chunkZ, byte[] blocks, Biome[] biomes) {
        int i = 64;
        double d = 0.03125;
        this.sandBuffer = this.perlinNoise2.getRegion(this.sandBuffer, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, d, d, 1.0);
        this.gravelBuffer = this.perlinNoise2.getRegion(this.gravelBuffer, chunkZ * 16, 109.0134, chunkX * 16, 16, 1, 16, d, 1.0, d);
        this.depthBuffer = this.perlinNoise3.getRegion(this.depthBuffer, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, d * 2.0, d * 2.0, d * 2.0);
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                Biome biome = biomes[j * 16 + k];
                boolean l = this.sandBuffer[j + k * 16] + this.random.nextDouble() * 0.2 > 0.0;
                boolean m = this.gravelBuffer[j + k * 16] + this.random.nextDouble() * 0.2 > 3.0;
                int n = (int)(this.depthBuffer[j + k * 16] / 3.0 + 3.0 + this.random.nextDouble() * 0.25);
                int o = -1;
                byte p = biome.surfaceBlock;
                byte q = biome.subsurfaceBlock;
                for (int r = 127; r >= 0; --r) {
                    int s = (j * 16 + k) * 128 + r;
                    if (r <= 0 + this.random.nextInt(5)) {
                        blocks[s] = (byte)Block.BEDROCK.id;
                        continue;
                    }
                    byte t = blocks[s];
                    if (t == 0) {
                        o = -1;
                        continue;
                    }
                    if (t != Block.STONE.id) continue;
                    if (o == -1) {
                        if (n <= 0) {
                            p = 0;
                            q = (byte)Block.STONE.id;
                        } else if (r >= i - 4 && r <= i + 1) {
                            p = biome.surfaceBlock;
                            q = biome.subsurfaceBlock;
                            if (m) {
                                p = 0;
                            }
                            if (m) {
                                q = (byte)Block.GRAVEL.id;
                            }
                            if (l) {
                                p = (byte)Block.SAND.id;
                            }
                            if (l) {
                                q = (byte)Block.SAND.id;
                            }
                        }
                        if (r < i && p == 0) {
                            p = (byte)Block.WATER.id;
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
        WorldChunk worldChunk = new WorldChunk(this.world, bs, chunkX, chunkZ);
        this.biomes = this.world.getBiomeSource().getBiomes(this.biomes, chunkX * 16, chunkZ * 16, 16, 16);
        double[] ds = this.world.getBiomeSource().temperatures;
        this.buildTerrain(chunkX, chunkZ, bs, this.biomes, ds);
        this.buildSurfaces(chunkX, chunkZ, bs, this.biomes);
        this.cave.place(this, this.world, chunkX, chunkZ, bs);
        worldChunk.populateHeightMap();
        return worldChunk;
    }

    private double[] generateHeightMap(double[] heightMap, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        if (heightMap == null) {
            heightMap = new double[sizeX * sizeY * sizeZ];
        }
        double d = 684.412;
        double e = 684.412;
        double[] ds = this.world.getBiomeSource().temperatures;
        double[] es = this.world.getBiomeSource().downfalls;
        this.scaleNoiseBuffer = this.scaleNoise.getRegion(this.scaleNoiseBuffer, x, z, sizeX, sizeZ, 1.121, 1.121, 0.5);
        this.depthNoiseBuffer = this.depthNoise.getRegion(this.depthNoiseBuffer, x, z, sizeX, sizeZ, 200.0, 200.0, 0.5);
        this.perlinNoiseBuffer = this.perlinNoise1.getRegion(this.perlinNoiseBuffer, x, y, z, sizeX, sizeY, sizeZ, d / 80.0, e / 160.0, d / 80.0);
        this.minLimitPerlinNoiseBuffer = this.minLimitPerlinNoise.getRegion(this.minLimitPerlinNoiseBuffer, x, y, z, sizeX, sizeY, sizeZ, d, e, d);
        this.maxLimitPerlinNoiseBuffer = this.maxLimitPerlinNoise.getRegion(this.maxLimitPerlinNoiseBuffer, x, y, z, sizeX, sizeY, sizeZ, d, e, d);
        int i = 0;
        int j = 0;
        int k = 16 / sizeX;
        for (int l = 0; l < sizeX; ++l) {
            int m = l * k + k / 2;
            for (int n = 0; n < sizeZ; ++n) {
                double q;
                int o = n * k + k / 2;
                double f = ds[m * 16 + o];
                double g = es[m * 16 + o] * f;
                double h = 1.0 - g;
                h *= h;
                h *= h;
                h = 1.0 - h;
                double p = (this.scaleNoiseBuffer[j] + 256.0) / 512.0;
                if ((p *= h) > 1.0) {
                    p = 1.0;
                }
                if ((q = this.depthNoiseBuffer[j] / 8000.0) < 0.0) {
                    q = -q * 0.3;
                }
                if ((q = q * 3.0 - 2.0) < 0.0) {
                    if ((q /= 2.0) < -1.0) {
                        q = -1.0;
                    }
                    q /= 1.4;
                    q /= 2.0;
                    p = 0.0;
                } else {
                    if (q > 1.0) {
                        q = 1.0;
                    }
                    q /= 8.0;
                }
                if (p < 0.0) {
                    p = 0.0;
                }
                p += 0.5;
                q = q * (double)sizeY / 16.0;
                double r = (double)sizeY / 2.0 + q * 4.0;
                ++j;
                for (int s = 0; s < sizeY; ++s) {
                    double t = 0.0;
                    double u = ((double)s - r) * 12.0 / p;
                    if (u < 0.0) {
                        u *= 4.0;
                    }
                    double v = this.minLimitPerlinNoiseBuffer[i] / 512.0;
                    double w = this.maxLimitPerlinNoiseBuffer[i] / 512.0;
                    double aa = (this.perlinNoiseBuffer[i] / 10.0 + 1.0) / 2.0;
                    t = aa < 0.0 ? v : (aa > 1.0 ? w : v + (w - v) * aa);
                    t -= u;
                    if (s > sizeY - 4) {
                        double ab = (float)(s - (sizeY - 4)) / 3.0f;
                        t = t * (1.0 - ab) + -10.0 * ab;
                    }
                    heightMap[i] = t;
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
        int bd;
        int av;
        LargeOakTreeFeature feature;
        int o;
        FallingBlock.fallImmediately = true;
        int i = chunkX * 16;
        int j = chunkZ * 16;
        Biome biome = this.world.getBiomeSource().getBiome(i + 16, j + 16);
        this.random.setSeed(this.world.seed);
        long l = this.random.nextLong() / 2L * 2L + 1L;
        long m = this.random.nextLong() / 2L * 2L + 1L;
        this.random.setSeed((long)chunkX * l + (long)chunkZ * m ^ this.world.seed);
        double d = 0.25;
        if (this.random.nextInt(4) == 0) {
            int k = i + this.random.nextInt(16) + 8;
            int p = this.random.nextInt(128);
            int ab = j + this.random.nextInt(16) + 8;
            new LakeFeature(Block.WATER.id).place(this.world, this.random, k, p, ab);
        }
        if (this.random.nextInt(8) == 0) {
            int n = i + this.random.nextInt(16) + 8;
            int q = this.random.nextInt(this.random.nextInt(120) + 8);
            int ac = j + this.random.nextInt(16) + 8;
            if (q < 64 || this.random.nextInt(10) == 0) {
                new LakeFeature(Block.LAVA.id).place(this.world, this.random, n, q, ac);
            }
        }
        for (o = 0; o < 8; ++o) {
            int r = i + this.random.nextInt(16) + 8;
            int ad = this.random.nextInt(128);
            int am = j + this.random.nextInt(16) + 8;
            new DungeonFeature().place(this.world, this.random, r, ad, am);
        }
        for (o = 0; o < 10; ++o) {
            int s = i + this.random.nextInt(16);
            int ae = this.random.nextInt(128);
            int an = j + this.random.nextInt(16);
            new ClayPatchFeature(32).place(this.world, this.random, s, ae, an);
        }
        for (o = 0; o < 20; ++o) {
            int t = i + this.random.nextInt(16);
            int af = this.random.nextInt(128);
            int ao = j + this.random.nextInt(16);
            new VeinFeature(Block.DIRT.id, 32).place(this.world, this.random, t, af, ao);
        }
        for (o = 0; o < 10; ++o) {
            int u = i + this.random.nextInt(16);
            int ag = this.random.nextInt(128);
            int ap = j + this.random.nextInt(16);
            new VeinFeature(Block.GRAVEL.id, 32).place(this.world, this.random, u, ag, ap);
        }
        for (o = 0; o < 20; ++o) {
            int v = i + this.random.nextInt(16);
            int ah = this.random.nextInt(128);
            int aq = j + this.random.nextInt(16);
            new VeinFeature(Block.COAL_ORE.id, 16).place(this.world, this.random, v, ah, aq);
        }
        for (o = 0; o < 20; ++o) {
            int w = i + this.random.nextInt(16);
            int ai = this.random.nextInt(64);
            int ar = j + this.random.nextInt(16);
            new VeinFeature(Block.IRON_ORE.id, 8).place(this.world, this.random, w, ai, ar);
        }
        for (o = 0; o < 2; ++o) {
            int x = i + this.random.nextInt(16);
            int aj = this.random.nextInt(32);
            int as = j + this.random.nextInt(16);
            new VeinFeature(Block.GOLD_ORE.id, 8).place(this.world, this.random, x, aj, as);
        }
        for (o = 0; o < 8; ++o) {
            int y = i + this.random.nextInt(16);
            int ak = this.random.nextInt(16);
            int at = j + this.random.nextInt(16);
            new VeinFeature(Block.REDSTONE_ORE.id, 7).place(this.world, this.random, y, ak, at);
        }
        for (o = 0; o < 1; ++o) {
            int z = i + this.random.nextInt(16);
            int al = this.random.nextInt(16);
            int au = j + this.random.nextInt(16);
            new VeinFeature(Block.DIAMOND_ORE.id, 7).place(this.world, this.random, z, al, au);
        }
        d = 0.5;
        o = (int)((this.forestNoise.getValue((double)i * d, (double)j * d) / 8.0 + this.random.nextDouble() * 4.0 + 4.0) / 3.0);
        int aa = 0;
        if (this.random.nextInt(10) == 0) {
            ++aa;
        }
        if (biome == Biome.FOREST) {
            aa += o + 5;
        }
        if (biome == Biome.RAINFOREST) {
            aa += o + 5;
        }
        if (biome == Biome.SEASONAL_FOREST) {
            aa += o + 2;
        }
        if (biome == Biome.TAIGA) {
            aa += o + 5;
        }
        if (biome == Biome.DESERT) {
            aa -= 20;
        }
        if (biome == Biome.TUNDRA) {
            aa -= 20;
        }
        if (biome == Biome.PLAINS) {
            aa -= 20;
        }
        TreeFeature treeFeature = new TreeFeature();
        if (this.random.nextInt(10) == 0) {
            feature = new LargeOakTreeFeature();
        }
        if (biome == Biome.RAINFOREST && this.random.nextInt(3) == 0) {
            feature = new LargeOakTreeFeature();
        }
        for (av = 0; av < aa; ++av) {
            int aw = i + this.random.nextInt(16) + 8;
            int be = j + this.random.nextInt(16) + 8;
            ((Feature)feature).prepare(1.0, 1.0, 1.0);
            ((Feature)feature).place(this.world, this.random, aw, this.world.getHeight(aw, be), be);
        }
        for (av = 0; av < 2; ++av) {
            int ax = i + this.random.nextInt(16) + 8;
            int bf = this.random.nextInt(128);
            int bp = j + this.random.nextInt(16) + 8;
            new PlantFeature(Block.YELLOW_FLOWER.id).place(this.world, this.random, ax, bf, bp);
        }
        if (this.random.nextInt(2) == 0) {
            av = i + this.random.nextInt(16) + 8;
            int ay = this.random.nextInt(128);
            int bg = j + this.random.nextInt(16) + 8;
            new PlantFeature(Block.RED_FLOWER.id).place(this.world, this.random, av, ay, bg);
        }
        if (this.random.nextInt(4) == 0) {
            av = i + this.random.nextInt(16) + 8;
            int az = this.random.nextInt(128);
            int bh = j + this.random.nextInt(16) + 8;
            new PlantFeature(Block.BROWN_MUSHROOM.id).place(this.world, this.random, av, az, bh);
        }
        if (this.random.nextInt(8) == 0) {
            av = i + this.random.nextInt(16) + 8;
            int ba = this.random.nextInt(128);
            int bi = j + this.random.nextInt(16) + 8;
            new PlantFeature(Block.RED_MUSHROOM.id).place(this.world, this.random, av, ba, bi);
        }
        for (av = 0; av < 10; ++av) {
            int bb = i + this.random.nextInt(16) + 8;
            int bj = this.random.nextInt(128);
            int bq = j + this.random.nextInt(16) + 8;
            new SugarcaneFeature().place(this.world, this.random, bb, bj, bq);
        }
        if (this.random.nextInt(32) == 0) {
            av = i + this.random.nextInt(16) + 8;
            int bc = this.random.nextInt(128);
            int bk = j + this.random.nextInt(16) + 8;
            new PumpkinPatchFeature().place(this.world, this.random, av, bc, bk);
        }
        av = 0;
        if (biome == Biome.DESERT) {
            av += 10;
        }
        for (bd = 0; bd < av; ++bd) {
            int bl = i + this.random.nextInt(16) + 8;
            int br = this.random.nextInt(128);
            int bv = j + this.random.nextInt(16) + 8;
            new CactusFeature().place(this.world, this.random, bl, br, bv);
        }
        for (bd = 0; bd < 50; ++bd) {
            int bm = i + this.random.nextInt(16) + 8;
            int bs = this.random.nextInt(this.random.nextInt(120) + 8);
            int bw = j + this.random.nextInt(16) + 8;
            new LiquidFallFeature(Block.FLOWING_WATER.id).place(this.world, this.random, bm, bs, bw);
        }
        for (bd = 0; bd < 20; ++bd) {
            int bn = i + this.random.nextInt(16) + 8;
            int bt = this.random.nextInt(this.random.nextInt(this.random.nextInt(112) + 8) + 8);
            int bx = j + this.random.nextInt(16) + 8;
            new LiquidFallFeature(Block.FLOWING_LAVA.id).place(this.world, this.random, bn, bt, bx);
        }
        this.temperatures = this.world.getBiomeSource().getTemperatures(this.temperatures, i + 8, j + 8, 16, 16);
        for (bd = i + 8; bd < i + 8 + 16; ++bd) {
            for (int bo = j + 8; bo < j + 8 + 16; ++bo) {
                int bu = bd - (i + 8);
                int by = bo - (j + 8);
                int bz = this.world.getSurfaceHeight(bd, bo);
                double e = this.temperatures[bu * 16 + by] - (double)(bz - 64) / 64.0 * 0.3;
                if (!(e < 0.5) || bz <= 0 || bz >= 128 || this.world.getBlock(bd, bz, bo) != 0 || !this.world.getMaterial(bd, bz - 1, bo).blocksMovement() || this.world.getMaterial(bd, bz - 1, bo) == Material.ICE) continue;
                this.world.setBlock(bd, bz, bo, Block.SNOW_LAYER.id);
            }
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

