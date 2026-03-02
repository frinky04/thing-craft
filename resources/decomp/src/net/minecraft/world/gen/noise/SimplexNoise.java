/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.noise;

import java.util.Random;

public class SimplexNoise {
    private static int[][] GRADIENTS = new int[][]{{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    private int[] permutations = new int[512];
    public double offsetX;
    public double offsetY;
    public double offsetZ;
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

    public SimplexNoise() {
        this(new Random());
    }

    public SimplexNoise(Random random) {
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

    private static int fastFloor(double x) {
        return x > 0.0 ? (int)x : (int)x - 1;
    }

    private static double dot(int[] gradient, double x, double y) {
        return (double)gradient[0] * x + (double)gradient[1] * y;
    }

    public void add(double[] values, double x, double y, int sizeX, int sizeY, double scaleX, double scaleY, double noiseScale) {
        int i = 0;
        for (int j = 0; j < sizeX; ++j) {
            double d = (x + (double)j) * scaleX + this.offsetX;
            for (int k = 0; k < sizeY; ++k) {
                double n;
                double l;
                double g;
                int ab;
                int z;
                double t;
                double v;
                int q;
                double r;
                double e = (y + (double)k) * scaleY + this.offsetY;
                double o = (d + e) * F2;
                int p = SimplexNoise.fastFloor(d + o);
                double s = (double)p - (r = (double)(p + (q = SimplexNoise.fastFloor(e + o))) * G2);
                double u = d - s;
                if (u > (v = e - (t = (double)q - r))) {
                    boolean w = true;
                    boolean bl = false;
                } else {
                    z = 0;
                    ab = 1;
                }
                double ac = u - (double)z + G2;
                double ad = v - (double)ab + G2;
                double ae = u - 1.0 + 2.0 * G2;
                double af = v - 1.0 + 2.0 * G2;
                int ag = p & 0xFF;
                int ah = q & 0xFF;
                int ai = this.permutations[ag + this.permutations[ah]] % 12;
                int aj = this.permutations[ag + z + this.permutations[ah + ab]] % 12;
                int ak = this.permutations[ag + 1 + this.permutations[ah + 1]] % 12;
                double al = 0.5 - u * u - v * v;
                if (al < 0.0) {
                    double d2 = 0.0;
                } else {
                    al *= al;
                    g = al * al * SimplexNoise.dot(GRADIENTS[ai], u, v);
                }
                double am = 0.5 - ac * ac - ad * ad;
                if (am < 0.0) {
                    double d3 = 0.0;
                } else {
                    am *= am;
                    l = am * am * SimplexNoise.dot(GRADIENTS[aj], ac, ad);
                }
                double an = 0.5 - ae * ae - af * af;
                if (an < 0.0) {
                    double d4 = 0.0;
                } else {
                    an *= an;
                    n = an * an * SimplexNoise.dot(GRADIENTS[ak], ae, af);
                }
                int n2 = i++;
                values[n2] = values[n2] + 70.0 * (g + l + n) * noiseScale;
            }
        }
    }
}

