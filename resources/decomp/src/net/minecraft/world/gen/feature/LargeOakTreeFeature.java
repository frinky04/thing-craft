/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class LargeOakTreeFeature
extends Feature {
    static final byte[] MINOR_AXES = new byte[]{2, 0, 0, 1, 2, 1};
    Random random = new Random();
    World world;
    int[] origin = new int[]{0, 0, 0};
    int height = 0;
    int trunkHeight;
    double trunkScale = 0.618;
    double branchDensity = 1.0;
    double branchSlope = 0.381;
    double branchLengthScale = 1.0;
    double foliageDensity = 1.0;
    int trunkWidth = 1;
    int maxTrunkHeight = 12;
    int foliageClusterHeight = 4;
    int[][] branches;

    void makeBranches() {
        int i;
        this.trunkHeight = (int)((double)this.height * this.trunkScale);
        if (this.trunkHeight >= this.height) {
            this.trunkHeight = this.height - 1;
        }
        if ((i = (int)(1.382 + Math.pow(this.foliageDensity * (double)this.height / 13.0, 2.0))) < 1) {
            i = 1;
        }
        int[][] is = new int[i * this.height][4];
        int j = this.origin[1] + this.height - this.foliageClusterHeight;
        int k = 1;
        int l = this.origin[1] + this.trunkHeight;
        int m = j - this.origin[1];
        is[0][0] = this.origin[0];
        is[0][1] = j--;
        is[0][2] = this.origin[2];
        is[0][3] = l;
        while (m >= 0) {
            float f = this.getTreeShape(m);
            if (f < 0.0f) {
                --j;
                --m;
                continue;
            }
            double d = 0.5;
            for (int n = 0; n < i; ++n) {
                int[] ks;
                int p;
                double g;
                double e = this.branchLengthScale * ((double)f * ((double)this.random.nextFloat() + 0.328));
                int o = (int)(e * Math.sin(g = (double)this.random.nextFloat() * 2.0 * 3.14159) + (double)this.origin[0] + d);
                int[] js = new int[]{o, j, p = (int)(e * Math.cos(g) + (double)this.origin[2] + d)};
                if (this.tryBranch(js, ks = new int[]{o, j + this.foliageClusterHeight, p}) != -1) continue;
                int[] ls = new int[]{this.origin[0], this.origin[1], this.origin[2]};
                double h = Math.sqrt(Math.pow(Math.abs(this.origin[0] - js[0]), 2.0) + Math.pow(Math.abs(this.origin[2] - js[2]), 2.0));
                double q = h * this.branchSlope;
                ls[1] = (double)js[1] - q > (double)l ? l : (int)((double)js[1] - q);
                if (this.tryBranch(ls, js) != -1) continue;
                is[k][0] = o;
                is[k][1] = j;
                is[k][2] = p;
                is[k][3] = ls[1];
                ++k;
            }
            --j;
            --m;
        }
        this.branches = new int[k][4];
        System.arraycopy(is, 0, this.branches, 0, k);
    }

    void placeCluster(int x, int y, int z, float shape, byte majorAxis, int clusterBlock) {
        int i = (int)((double)shape + 0.618);
        byte j = MINOR_AXES[majorAxis];
        byte k = MINOR_AXES[majorAxis + 3];
        int[] is = new int[]{x, y, z};
        int[] js = new int[]{0, 0, 0};
        int m = -i;
        js[majorAxis] = is[majorAxis];
        for (int l = -i; l <= i; ++l) {
            js[j] = is[j] + l;
            m = -i;
            while (m <= i) {
                double d = Math.sqrt(Math.pow((double)Math.abs(l) + 0.5, 2.0) + Math.pow((double)Math.abs(m) + 0.5, 2.0));
                if (d > (double)shape) {
                    ++m;
                    continue;
                }
                js[k] = is[k] + m;
                int n = this.world.getBlock(js[0], js[1], js[2]);
                if (n != 0 && n != 18) {
                    ++m;
                    continue;
                }
                this.world.setBlockQuietly(js[0], js[1], js[2], clusterBlock);
                ++m;
            }
        }
    }

    float getTreeShape(int height) {
        if ((double)height < (double)this.height * 0.3) {
            return -1.618f;
        }
        float f = (float)this.height / 2.0f;
        float g = (float)this.height / 2.0f - (float)height;
        if (g == 0.0f) {
            float f2 = f;
        } else if (Math.abs(g) >= f) {
            float h = 0.0f;
        } else {
            float j = (float)Math.sqrt(Math.pow(Math.abs(f), 2.0) - Math.pow(Math.abs(g), 2.0));
        }
        return j *= 0.5f;
    }

    float getClusterShape(int layer) {
        if (layer < 0 || layer >= this.foliageClusterHeight) {
            return -1.0f;
        }
        if (layer == 0 || layer == this.foliageClusterHeight - 1) {
            return 2.0f;
        }
        return 3.0f;
    }

    void placeFoliageCluster(int x, int baseY, int z) {
        int j = baseY + this.foliageClusterHeight;
        for (int i = baseY; i < j; ++i) {
            float f = this.getClusterShape(i - baseY);
            this.placeCluster(x, i, z, f, (byte)1, 18);
        }
    }

    void placeBranch(int[] from, int[] to, int log) {
        int n;
        int[] is = new int[]{0, 0, 0};
        int j = 0;
        for (int i = 0; i < 3; i = (int)((byte)(i + 1))) {
            is[i] = to[i] - from[i];
            if (Math.abs(is[i]) <= Math.abs(is[j])) continue;
            j = i;
        }
        if (is[j] == 0) {
            return;
        }
        byte k = MINOR_AXES[j];
        byte l = MINOR_AXES[j + 3];
        if (is[j] > 0) {
            boolean bl = true;
        } else {
            n = -1;
        }
        double d = (double)is[k] / (double)is[j];
        double e = (double)is[l] / (double)is[j];
        int[] js = new int[]{0, 0, 0};
        int p = is[j] + n;
        for (int o = 0; o != p; o += n) {
            js[j] = MathHelper.floor((double)(from[j] + o) + 0.5);
            js[k] = MathHelper.floor((double)from[k] + (double)o * d + 0.5);
            js[l] = MathHelper.floor((double)from[l] + (double)o * e + 0.5);
            this.world.setBlockQuietly(js[0], js[1], js[2], log);
        }
    }

    void placeFoliage() {
        int j = this.branches.length;
        for (int i = 0; i < j; ++i) {
            int k = this.branches[i][0];
            int l = this.branches[i][1];
            int m = this.branches[i][2];
            this.placeFoliageCluster(k, l, m);
        }
    }

    boolean shouldPlaceBranch(int height) {
        return !((double)height < (double)this.height * 0.2);
    }

    void placeTrunk() {
        int i = this.origin[0];
        int j = this.origin[1];
        int k = this.origin[1] + this.trunkHeight;
        int l = this.origin[2];
        int[] is = new int[]{i, j, l};
        int[] js = new int[]{i, k, l};
        this.placeBranch(is, js, 17);
        if (this.trunkWidth == 2) {
            is[0] = is[0] + 1;
            js[0] = js[0] + 1;
            this.placeBranch(is, js, 17);
            is[2] = is[2] + 1;
            js[2] = js[2] + 1;
            this.placeBranch(is, js, 17);
            is[0] = is[0] + -1;
            js[0] = js[0] + -1;
            this.placeBranch(is, js, 17);
        }
    }

    void placeBranches() {
        int j = this.branches.length;
        int[] is = new int[]{this.origin[0], this.origin[1], this.origin[2]};
        for (int i = 0; i < j; ++i) {
            int[] js = this.branches[i];
            int[] ks = new int[]{js[0], js[1], js[2]};
            is[1] = js[3];
            int k = is[1] - this.origin[1];
            if (!this.shouldPlaceBranch(k)) continue;
            this.placeBranch(is, ks, 17);
        }
    }

    int tryBranch(int[] from, int[] to) {
        int o;
        int n;
        int[] is = new int[]{0, 0, 0};
        int j = 0;
        for (int i = 0; i < 3; i = (int)((byte)(i + 1))) {
            is[i] = to[i] - from[i];
            if (Math.abs(is[i]) <= Math.abs(is[j])) continue;
            j = i;
        }
        if (is[j] == 0) {
            return -1;
        }
        byte k = MINOR_AXES[j];
        byte l = MINOR_AXES[j + 3];
        if (is[j] > 0) {
            boolean bl = true;
        } else {
            n = -1;
        }
        double d = (double)is[k] / (double)is[j];
        double e = (double)is[l] / (double)is[j];
        int[] js = new int[]{0, 0, 0};
        int p = is[j] + n;
        for (o = 0; o != p; o += n) {
            js[j] = from[j] + o;
            js[k] = (int)((double)from[k] + (double)o * d);
            js[l] = (int)((double)from[l] + (double)o * e);
            int q = this.world.getBlock(js[0], js[1], js[2]);
            if (q != 0 && q != 18) break;
        }
        if (o == p) {
            return -1;
        }
        return Math.abs(o);
    }

    boolean canPlace() {
        int[] is = new int[]{this.origin[0], this.origin[1], this.origin[2]};
        int[] js = new int[]{this.origin[0], this.origin[1] + this.height - 1, this.origin[2]};
        int i = this.world.getBlock(this.origin[0], this.origin[1] - 1, this.origin[2]);
        if (i != 2 && i != 3) {
            return false;
        }
        int j = this.tryBranch(is, js);
        if (j == -1) {
            return true;
        }
        if (j < 6) {
            return false;
        }
        this.height = j;
        return true;
    }

    public void prepare(double d0, double d1, double d2) {
        this.maxTrunkHeight = (int)(d0 * 12.0);
        if (d0 > 0.5) {
            this.foliageClusterHeight = 5;
        }
        this.branchLengthScale = d1;
        this.foliageDensity = d2;
    }

    public boolean place(World world, Random random, int x, int y, int z) {
        this.world = world;
        long l = random.nextLong();
        this.random.setSeed(l);
        this.origin[0] = x;
        this.origin[1] = y;
        this.origin[2] = z;
        if (this.height == 0) {
            this.height = 5 + this.random.nextInt(this.maxTrunkHeight);
        }
        if (!this.canPlace()) {
            return false;
        }
        this.makeBranches();
        this.placeFoliage();
        this.placeTrunk();
        this.placeBranches();
        return true;
    }
}

