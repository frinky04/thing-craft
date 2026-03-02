/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.noise;

import java.util.Random;
import net.minecraft.world.gen.noise.Noise;

public class ImprovedNoise
extends Noise {
    private int[] permutations = new int[512];
    public double offsetX;
    public double offsetY;
    public double offsetZ;

    public ImprovedNoise() {
        this(new Random());
    }

    public ImprovedNoise(Random random) {
        int i;
        this.offsetX = random.nextDouble() * 256.0;
        this.offsetY = random.nextDouble() * 256.0;
        this.offsetZ = random.nextDouble() * 256.0;
        for (i = 0; i < 256; ++i) {
            this.permutations[i] = i;
        }
        for (i = 0; i < 256; ++i) {
            int j = random.nextInt(256 - i) + i;
            int k = this.permutations[i];
            this.permutations[i] = this.permutations[j];
            this.permutations[j] = k;
            this.permutations[i + 256] = this.permutations[i];
        }
    }

    /*
     * WARNING - void declaration
     */
    public double noise(double x, double y, double z) {
        void t;
        void q;
        void s;
        void p;
        double d = x + this.offsetX;
        double e = y + this.offsetY;
        double f = z + this.offsetZ;
        int i = (int)d;
        int j = (int)e;
        int k = (int)f;
        if (d < (double)i) {
            --i;
        }
        if (e < (double)j) {
            --j;
        }
        if (f < (double)k) {
            --k;
        }
        int l = i & 0xFF;
        int m = j & 0xFF;
        int n = k & 0xFF;
        double g = (d -= (double)i) * d * d * (d * (d * 6.0 - 15.0) + 10.0);
        double h = (e -= (double)j) * e * e * (e * (e * 6.0 - 15.0) + 10.0);
        double o = (f -= (double)k) * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        int n2 = this.permutations[l] + m;
        int n3 = this.permutations[p] + n;
        int r = this.permutations[p + true] + n;
        int n4 = this.permutations[l + 1] + m;
        int n5 = this.permutations[s] + n;
        int u = this.permutations[s + true] + n;
        return this.lerp(o, this.lerp(h, this.lerp(g, this.gradientDot(this.permutations[q], d, e, f), this.gradientDot(this.permutations[t], d - 1.0, e, f)), this.lerp(g, this.gradientDot(this.permutations[r], d, e - 1.0, f), this.gradientDot(this.permutations[u], d - 1.0, e - 1.0, f))), this.lerp(h, this.lerp(g, this.gradientDot(this.permutations[q + true], d, e, f - 1.0), this.gradientDot(this.permutations[t + true], d - 1.0, e, f - 1.0)), this.lerp(g, this.gradientDot(this.permutations[r + 1], d, e - 1.0, f - 1.0), this.gradientDot(this.permutations[u + 1], d - 1.0, e - 1.0, f - 1.0))));
    }

    public final double lerp(double delta, double min, double max) {
        return min + delta * (max - min);
    }

    public final double gradientDot(int hash, double x, double z) {
        int i = hash & 0xF;
        double d = (double)(1 - ((i & 8) >> 3)) * x;
        double e = i < 4 ? 0.0 : (i == 12 || i == 14 ? x : z);
        return ((i & 1) == 0 ? d : -d) + ((i & 2) == 0 ? e : -e);
    }

    public final double gradientDot(int hash, double x, double y, double z) {
        double d;
        int i = hash & 0xF;
        double d2 = d = i < 8 ? x : y;
        double e = i < 4 ? y : (i == 12 || i == 14 ? x : z);
        return ((i & 1) == 0 ? d : -d) + ((i & 2) == 0 ? e : -e);
    }

    public double getValue(double x, double y) {
        return this.noise(x, y, 0.0);
    }

    public void add(double[] values, double x, double y, double z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ, double noiseScale) {
        if (sizeY == 1) {
            boolean bl = false;
            boolean bl2 = false;
            boolean bl3 = false;
            int m = 0;
            double d = 0.0;
            double f = 0.0;
            int s = 0;
            double g = 1.0 / noiseScale;
            for (int v = 0; v < sizeX; ++v) {
                double w = (x + (double)v) * scaleX + this.offsetX;
                int ab = (int)w;
                if (w < (double)ab) {
                    --ab;
                }
                int ad = ab & 0xFF;
                double ae = (w -= (double)ab) * w * w * (w * (w * 6.0 - 15.0) + 10.0);
                for (int ag = 0; ag < sizeZ; ++ag) {
                    double ai = (z + (double)ag) * scaleZ + this.offsetZ;
                    int ak = (int)ai;
                    if (ai < (double)ak) {
                        --ak;
                    }
                    int am = ak & 0xFF;
                    double ao = (ai -= (double)ak) * ai * ai * (ai * (ai * 6.0 - 15.0) + 10.0);
                    int i = this.permutations[ad] + 0;
                    int k = this.permutations[i] + am;
                    int l = this.permutations[ad + 1] + 0;
                    m = this.permutations[l] + am;
                    double e = this.lerp(ae, this.gradientDot(this.permutations[k], w, ai), this.gradientDot(this.permutations[m], w - 1.0, 0.0, ai));
                    f = this.lerp(ae, this.gradientDot(this.permutations[k + 1], w, 0.0, ai - 1.0), this.gradientDot(this.permutations[m + 1], w - 1.0, 0.0, ai - 1.0));
                    double aq = this.lerp(ao, e, f);
                    int n = s++;
                    values[n] = values[n] + aq * g;
                }
            }
            return;
        }
        int j = 0;
        double d = 1.0 / noiseScale;
        int n = -1;
        boolean e = false;
        boolean bl = false;
        boolean f = false;
        boolean bl4 = false;
        boolean s = false;
        int u = 0;
        double d2 = 0.0;
        double w = 0.0;
        double ab = 0.0;
        double af = 0.0;
        for (int ah = 0; ah < sizeX; ++ah) {
            double aj = (x + (double)ah) * scaleX + this.offsetX;
            int al = (int)aj;
            if (aj < (double)al) {
                --al;
            }
            int an = al & 0xFF;
            double ap = (aj -= (double)al) * aj * aj * (aj * (aj * 6.0 - 15.0) + 10.0);
            for (int ar = 0; ar < sizeZ; ++ar) {
                double as = (z + (double)ar) * scaleZ + this.offsetZ;
                int at = (int)as;
                if (as < (double)at) {
                    --at;
                }
                int au = at & 0xFF;
                double av = (as -= (double)at) * as * as * (as * (as * 6.0 - 15.0) + 10.0);
                for (int aw = 0; aw < sizeY; ++aw) {
                    double ac;
                    double aa;
                    double h;
                    double ax = (y + (double)aw) * scaleY + this.offsetY;
                    int ay = (int)ax;
                    if (ax < (double)ay) {
                        --ay;
                    }
                    int az = ay & 0xFF;
                    double ba = (ax -= (double)ay) * ax * ax * (ax * (ax * 6.0 - 15.0) + 10.0);
                    if (aw == 0 || az != n) {
                        n = az;
                        int o = this.permutations[an] + az;
                        int p = this.permutations[o] + au;
                        int q = this.permutations[o + 1] + au;
                        int r = this.permutations[an + 1] + az;
                        int t = this.permutations[r] + au;
                        u = this.permutations[r + 1] + au;
                        h = this.lerp(ap, this.gradientDot(this.permutations[p], aj, ax, as), this.gradientDot(this.permutations[t], aj - 1.0, ax, as));
                        aa = this.lerp(ap, this.gradientDot(this.permutations[q], aj, ax - 1.0, as), this.gradientDot(this.permutations[u], aj - 1.0, ax - 1.0, as));
                        ac = this.lerp(ap, this.gradientDot(this.permutations[p + 1], aj, ax, as - 1.0), this.gradientDot(this.permutations[t + 1], aj - 1.0, ax, as - 1.0));
                        af = this.lerp(ap, this.gradientDot(this.permutations[q + 1], aj, ax - 1.0, as - 1.0), this.gradientDot(this.permutations[u + 1], aj - 1.0, ax - 1.0, as - 1.0));
                    }
                    double bb = this.lerp(ba, h, aa);
                    double bc = this.lerp(ba, ac, af);
                    double bd = this.lerp(av, bb, bc);
                    int n2 = j++;
                    values[n2] = values[n2] + bd * d;
                }
            }
        }
    }
}

