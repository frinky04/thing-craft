/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.CodeBook;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.FuncFloor;
import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.InfoMode;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Floor1
extends FuncFloor {
    static final int floor1_rangedb = 140;
    static final int VIF_POSIT = 63;
    private static float[] FLOOR_fromdB_LOOKUP = new float[]{1.0649863E-7f, 1.1341951E-7f, 1.2079015E-7f, 1.2863978E-7f, 1.369995E-7f, 1.459025E-7f, 1.5538409E-7f, 1.6548181E-7f, 1.7623574E-7f, 1.8768856E-7f, 1.998856E-7f, 2.128753E-7f, 2.2670913E-7f, 2.4144197E-7f, 2.5713223E-7f, 2.7384212E-7f, 2.9163792E-7f, 3.1059022E-7f, 3.307741E-7f, 3.5226967E-7f, 3.7516213E-7f, 3.995423E-7f, 4.255068E-7f, 4.5315863E-7f, 4.8260745E-7f, 5.1397E-7f, 5.4737063E-7f, 5.829419E-7f, 6.208247E-7f, 6.611694E-7f, 7.041359E-7f, 7.4989464E-7f, 7.98627E-7f, 8.505263E-7f, 9.057983E-7f, 9.646621E-7f, 1.0273513E-6f, 1.0941144E-6f, 1.1652161E-6f, 1.2409384E-6f, 1.3215816E-6f, 1.4074654E-6f, 1.4989305E-6f, 1.5963394E-6f, 1.7000785E-6f, 1.8105592E-6f, 1.9282195E-6f, 2.053526E-6f, 2.1869757E-6f, 2.3290977E-6f, 2.4804558E-6f, 2.6416496E-6f, 2.813319E-6f, 2.9961443E-6f, 3.1908505E-6f, 3.39821E-6f, 3.619045E-6f, 3.8542307E-6f, 4.1047006E-6f, 4.371447E-6f, 4.6555283E-6f, 4.958071E-6f, 5.280274E-6f, 5.623416E-6f, 5.988857E-6f, 6.3780467E-6f, 6.7925284E-6f, 7.2339453E-6f, 7.704048E-6f, 8.2047E-6f, 8.737888E-6f, 9.305725E-6f, 9.910464E-6f, 1.0554501E-5f, 1.1240392E-5f, 1.1970856E-5f, 1.2748789E-5f, 1.3577278E-5f, 1.4459606E-5f, 1.5399271E-5f, 1.6400005E-5f, 1.7465769E-5f, 1.8600793E-5f, 1.9809577E-5f, 2.1096914E-5f, 2.2467912E-5f, 2.3928002E-5f, 2.5482977E-5f, 2.7139005E-5f, 2.890265E-5f, 3.078091E-5f, 3.2781227E-5f, 3.4911533E-5f, 3.718028E-5f, 3.9596467E-5f, 4.2169668E-5f, 4.491009E-5f, 4.7828602E-5f, 5.0936775E-5f, 5.424693E-5f, 5.7772202E-5f, 6.152657E-5f, 6.552491E-5f, 6.9783084E-5f, 7.4317984E-5f, 7.914758E-5f, 8.429104E-5f, 8.976875E-5f, 9.560242E-5f, 1.0181521E-4f, 1.0843174E-4f, 1.1547824E-4f, 1.2298267E-4f, 1.3097477E-4f, 1.3948625E-4f, 1.4855085E-4f, 1.5820454E-4f, 1.6848555E-4f, 1.7943469E-4f, 1.9109536E-4f, 2.0351382E-4f, 2.167393E-4f, 2.3082423E-4f, 2.4582449E-4f, 2.6179955E-4f, 2.7881275E-4f, 2.9693157E-4f, 3.1622787E-4f, 3.3677815E-4f, 3.5866388E-4f, 3.8197188E-4f, 4.0679457E-4f, 4.3323037E-4f, 4.613841E-4f, 4.913675E-4f, 5.2329927E-4f, 5.573062E-4f, 5.935231E-4f, 6.320936E-4f, 6.731706E-4f, 7.16917E-4f, 7.635063E-4f, 8.1312325E-4f, 8.6596457E-4f, 9.2223985E-4f, 9.821722E-4f, 0.0010459992f, 0.0011139743f, 0.0011863665f, 0.0012634633f, 0.0013455702f, 0.0014330129f, 0.0015261382f, 0.0016253153f, 0.0017309374f, 0.0018434235f, 0.0019632196f, 0.0020908006f, 0.0022266726f, 0.0023713743f, 0.0025254795f, 0.0026895993f, 0.0028643848f, 0.0030505287f, 0.003248769f, 0.0034598925f, 0.0036847359f, 0.0039241905f, 0.0041792067f, 0.004450795f, 0.004740033f, 0.005048067f, 0.0053761187f, 0.005725489f, 0.0060975635f, 0.0064938175f, 0.0069158226f, 0.0073652514f, 0.007843887f, 0.008353627f, 0.008896492f, 0.009474637f, 0.010090352f, 0.01074608f, 0.011444421f, 0.012188144f, 0.012980198f, 0.013823725f, 0.014722068f, 0.015678791f, 0.016697686f, 0.017782796f, 0.018938422f, 0.020169148f, 0.021479854f, 0.022875736f, 0.02436233f, 0.025945531f, 0.027631618f, 0.029427277f, 0.031339627f, 0.03337625f, 0.035545226f, 0.037855156f, 0.0403152f, 0.042935107f, 0.045725275f, 0.048696756f, 0.05186135f, 0.05523159f, 0.05882085f, 0.062643364f, 0.06671428f, 0.07104975f, 0.075666964f, 0.08058423f, 0.08582105f, 0.09139818f, 0.097337745f, 0.1036633f, 0.11039993f, 0.11757434f, 0.12521498f, 0.13335215f, 0.14201812f, 0.15124726f, 0.16107617f, 0.1715438f, 0.18269168f, 0.19456401f, 0.20720787f, 0.22067343f, 0.23501402f, 0.25028655f, 0.26655158f, 0.28387362f, 0.3023213f, 0.32196787f, 0.34289113f, 0.36517414f, 0.3889052f, 0.41417846f, 0.44109413f, 0.4697589f, 0.50028646f, 0.53279793f, 0.5674221f, 0.6042964f, 0.64356697f, 0.6853896f, 0.72993004f, 0.777365f, 0.8278826f, 0.88168305f, 0.9389798f, 1.0f};

    Floor1() {
    }

    void pack(Object object, Buffer buffer) {
        int m;
        InfoFloor1 infoFloor1 = (InfoFloor1)object;
        int i = 0;
        int k = infoFloor1.postlist[1];
        int l = -1;
        buffer.write(infoFloor1.partitions, 5);
        for (m = 0; m < infoFloor1.partitions; ++m) {
            buffer.write(infoFloor1.partitionclass[m], 4);
            if (l >= infoFloor1.partitionclass[m]) continue;
            l = infoFloor1.partitionclass[m];
        }
        for (m = 0; m < l + 1; ++m) {
            buffer.write(infoFloor1.class_dim[m] - 1, 3);
            buffer.write(infoFloor1.class_subs[m], 2);
            if (infoFloor1.class_subs[m] != 0) {
                buffer.write(infoFloor1.class_book[m], 8);
            }
            for (int n = 0; n < 1 << infoFloor1.class_subs[m]; ++n) {
                buffer.write(infoFloor1.class_subbook[m][n] + 1, 8);
            }
        }
        buffer.write(infoFloor1.mult - 1, 2);
        buffer.write(Util.ilog2(k), 4);
        int j = Util.ilog2(k);
        int o = 0;
        for (m = 0; m < infoFloor1.partitions; ++m) {
            i += infoFloor1.class_dim[infoFloor1.partitionclass[m]];
            while (o < i) {
                buffer.write(infoFloor1.postlist[o + 2], j);
                ++o;
            }
        }
    }

    Object unpack(Info info, Buffer buffer) {
        int l;
        int i = 0;
        int j = -1;
        InfoFloor1 infoFloor1 = new InfoFloor1();
        infoFloor1.partitions = buffer.read(5);
        for (l = 0; l < infoFloor1.partitions; ++l) {
            infoFloor1.partitionclass[l] = buffer.read(4);
            if (j >= infoFloor1.partitionclass[l]) continue;
            j = infoFloor1.partitionclass[l];
        }
        for (l = 0; l < j + 1; ++l) {
            infoFloor1.class_dim[l] = buffer.read(3) + 1;
            infoFloor1.class_subs[l] = buffer.read(2);
            if (infoFloor1.class_subs[l] < 0) {
                infoFloor1.free();
                return null;
            }
            if (infoFloor1.class_subs[l] != 0) {
                infoFloor1.class_book[l] = buffer.read(8);
            }
            if (infoFloor1.class_book[l] < 0 || infoFloor1.class_book[l] >= info.books) {
                infoFloor1.free();
                return null;
            }
            for (int m = 0; m < 1 << infoFloor1.class_subs[l]; ++m) {
                infoFloor1.class_subbook[l][m] = buffer.read(8) - 1;
                if (infoFloor1.class_subbook[l][m] >= -1 && infoFloor1.class_subbook[l][m] < info.books) continue;
                infoFloor1.free();
                return null;
            }
        }
        infoFloor1.mult = buffer.read(2) + 1;
        int k = buffer.read(4);
        int n = 0;
        for (l = 0; l < infoFloor1.partitions; ++l) {
            i += infoFloor1.class_dim[infoFloor1.partitionclass[l]];
            while (n < i) {
                infoFloor1.postlist[n + 2] = buffer.read(k);
                int o = infoFloor1.postlist[n + 2];
                if (o < 0 || o >= 1 << k) {
                    infoFloor1.free();
                    return null;
                }
                ++n;
            }
        }
        infoFloor1.postlist[0] = 0;
        infoFloor1.postlist[1] = 1 << k;
        return infoFloor1;
    }

    Object look(DspState dspState, InfoMode infoMode, Object object) {
        int k;
        int j;
        int i = 0;
        int[] is = new int[65];
        InfoFloor1 infoFloor1 = (InfoFloor1)object;
        LookFloor1 lookFloor1 = new LookFloor1();
        lookFloor1.vi = infoFloor1;
        lookFloor1.n = infoFloor1.postlist[1];
        for (j = 0; j < infoFloor1.partitions; ++j) {
            i += infoFloor1.class_dim[infoFloor1.partitionclass[j]];
        }
        lookFloor1.posts = i += 2;
        for (j = 0; j < i; ++j) {
            is[j] = j;
        }
        for (k = 0; k < i - 1; ++k) {
            for (int l = k; l < i; ++l) {
                if (infoFloor1.postlist[is[k]] <= infoFloor1.postlist[is[l]]) continue;
                j = is[l];
                is[l] = is[k];
                is[k] = j;
            }
        }
        for (k = 0; k < i; ++k) {
            lookFloor1.forward_index[k] = is[k];
        }
        for (k = 0; k < i; ++k) {
            lookFloor1.reverse_index[lookFloor1.forward_index[k]] = k;
        }
        for (k = 0; k < i; ++k) {
            lookFloor1.sorted_index[k] = infoFloor1.postlist[lookFloor1.forward_index[k]];
        }
        switch (infoFloor1.mult) {
            case 1: {
                lookFloor1.quant_q = 256;
                break;
            }
            case 2: {
                lookFloor1.quant_q = 128;
                break;
            }
            case 3: {
                lookFloor1.quant_q = 86;
                break;
            }
            case 4: {
                lookFloor1.quant_q = 64;
                break;
            }
            default: {
                lookFloor1.quant_q = -1;
            }
        }
        for (k = 0; k < i - 2; ++k) {
            int m = 0;
            int n = 1;
            int o = 0;
            int p = lookFloor1.n;
            int q = infoFloor1.postlist[k + 2];
            for (int r = 0; r < k + 2; ++r) {
                int s = infoFloor1.postlist[r];
                if (s > o && s < q) {
                    m = r;
                    o = s;
                }
                if (s >= p || s <= q) continue;
                n = r;
                p = s;
            }
            lookFloor1.loneighbor[k] = m;
            lookFloor1.hineighbor[k] = n;
        }
        return lookFloor1;
    }

    void free_info(Object object) {
    }

    void free_look(Object object) {
    }

    void free_state(Object object) {
    }

    int forward(Block block, Object object, float[] fs, float[] gs, Object object2) {
        return 0;
    }

    Object inverse1(Block block, Object object, Object object2) {
        LookFloor1 lookFloor1 = (LookFloor1)object;
        InfoFloor1 infoFloor1 = lookFloor1.vi;
        CodeBook[] codeBooks = block.vd.fullbooks;
        if (block.opb.read(1) == 1) {
            int j;
            int i;
            int[] is;
            Object object3 = null;
            if (object2 instanceof int[]) {
                is = (int[])object2;
            }
            if (is == null || is.length < lookFloor1.posts) {
                is = new int[lookFloor1.posts];
            } else {
                for (i = 0; i < is.length; ++i) {
                    is[i] = 0;
                }
            }
            is[0] = block.opb.read(Util.ilog(lookFloor1.quant_q - 1));
            is[1] = block.opb.read(Util.ilog(lookFloor1.quant_q - 1));
            i = 0;
            int k = 2;
            while (j < infoFloor1.partitions) {
                int l = infoFloor1.partitionclass[j];
                int n = infoFloor1.class_dim[l];
                int p = infoFloor1.class_subs[l];
                int r = 1 << p;
                int t = 0;
                if (p != 0 && (t = codeBooks[infoFloor1.class_book[l]].decode(block.opb)) == -1) {
                    return null;
                }
                for (int u = 0; u < n; ++u) {
                    int v = infoFloor1.class_subbook[l][t & r - 1];
                    t >>>= p;
                    if (v >= 0) {
                        is[k + u] = codeBooks[v].decode(block.opb);
                        if (is[k + u] != -1) continue;
                        return null;
                    }
                    is[k + u] = 0;
                }
                k += n;
                ++j;
            }
            for (j = 2; j < lookFloor1.posts; ++j) {
                int o;
                k = Floor1.render_point(infoFloor1.postlist[lookFloor1.loneighbor[j - 2]], infoFloor1.postlist[lookFloor1.hineighbor[j - 2]], is[lookFloor1.loneighbor[j - 2]], is[lookFloor1.hineighbor[j - 2]], infoFloor1.postlist[j]);
                int m = lookFloor1.quant_q - k;
                int q = (m < (o = k) ? m : o) << 1;
                int s = is[j];
                if (s != 0) {
                    s = s >= q ? (m > o ? (s -= o) : -1 - (s - m)) : ((s & 1) != 0 ? -(s + 1 >>> 1) : (s >>= 1));
                    is[j] = s + k;
                    int n = lookFloor1.loneighbor[j - 2];
                    is[n] = is[n] & Short.MAX_VALUE;
                    int n2 = lookFloor1.hineighbor[j - 2];
                    is[n2] = is[n2] & Short.MAX_VALUE;
                    continue;
                }
                is[j] = k | 0x8000;
            }
            return is;
        }
        return null;
    }

    private static int render_point(int i, int j, int k, int l, int m) {
        int n = (l &= Short.MAX_VALUE) - (k &= Short.MAX_VALUE);
        int o = j - i;
        int p = Math.abs(n);
        int q = p * (m - i);
        int r = q / o;
        if (n < 0) {
            return k - r;
        }
        return k + r;
    }

    int inverse2(Block block, Object object, Object object2, float[] fs) {
        LookFloor1 lookFloor1 = (LookFloor1)object;
        InfoFloor1 infoFloor1 = lookFloor1.vi;
        int i = block.vd.vi.blocksizes[block.mode] / 2;
        if (object2 != null) {
            int n;
            int[] is = (int[])object2;
            int k = 0;
            int l = 0;
            int m = is[0] * infoFloor1.mult;
            for (n = 1; n < lookFloor1.posts; ++n) {
                int o = lookFloor1.forward_index[n];
                int p = is[o] & Short.MAX_VALUE;
                if (p != is[o]) continue;
                k = infoFloor1.postlist[o];
                Floor1.render_line(l, k, m, p *= infoFloor1.mult, fs);
                l = k;
                m = p;
            }
            for (n = k; n < i; ++n) {
                int n2 = n;
                fs[n2] = fs[n2] * fs[n - 1];
            }
            return 1;
        }
        for (int j = 0; j < i; ++j) {
            fs[j] = 0.0f;
        }
        return 0;
    }

    private static void render_line(int i, int j, int k, int l, float[] fs) {
        int m = l - k;
        int n = j - i;
        int o = Math.abs(m);
        int p = m / n;
        int q = m < 0 ? p - 1 : p + 1;
        int r = i;
        int s = k;
        int t = 0;
        o -= Math.abs(p * n);
        int n2 = r;
        fs[n2] = fs[n2] * FLOOR_fromdB_LOOKUP[s];
        while (++r < j) {
            if ((t += o) >= n) {
                t -= n;
                s += q;
            } else {
                s += p;
            }
            int n3 = r;
            fs[n3] = fs[n3] * FLOOR_fromdB_LOOKUP[s];
        }
    }

    @Environment(value=EnvType.CLIENT)
    class EchstateFloor1 {
        int[] codewords;
        float[] curve;
        long frameno;
        long codes;

        EchstateFloor1() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    class InfoFloor1 {
        static final int VIF_POSIT = 63;
        static final int VIF_CLASS = 16;
        static final int VIF_PARTS = 31;
        int partitions;
        int[] partitionclass = new int[31];
        int[] class_dim = new int[16];
        int[] class_subs = new int[16];
        int[] class_book = new int[16];
        int[][] class_subbook = new int[16][];
        int mult;
        int[] postlist = new int[65];
        float maxover;
        float maxunder;
        float maxerr;
        int twofitminsize;
        int twofitminused;
        int twofitweight;
        float twofitatten;
        int unusedminsize;
        int unusedmin_n;
        int n;

        InfoFloor1() {
            for (int i = 0; i < this.class_subbook.length; ++i) {
                this.class_subbook[i] = new int[8];
            }
        }

        void free() {
            this.partitionclass = null;
            this.class_dim = null;
            this.class_subs = null;
            this.class_book = null;
            this.class_subbook = null;
            this.postlist = null;
        }

        Object copy_info() {
            InfoFloor1 infoFloor1 = this;
            InfoFloor1 infoFloor12 = new InfoFloor1();
            infoFloor12.partitions = infoFloor1.partitions;
            System.arraycopy(infoFloor1.partitionclass, 0, infoFloor12.partitionclass, 0, 31);
            System.arraycopy(infoFloor1.class_dim, 0, infoFloor12.class_dim, 0, 16);
            System.arraycopy(infoFloor1.class_subs, 0, infoFloor12.class_subs, 0, 16);
            System.arraycopy(infoFloor1.class_book, 0, infoFloor12.class_book, 0, 16);
            for (int i = 0; i < 16; ++i) {
                System.arraycopy(infoFloor1.class_subbook[i], 0, infoFloor12.class_subbook[i], 0, 8);
            }
            infoFloor12.mult = infoFloor1.mult;
            System.arraycopy(infoFloor1.postlist, 0, infoFloor12.postlist, 0, 65);
            infoFloor12.maxover = infoFloor1.maxover;
            infoFloor12.maxunder = infoFloor1.maxunder;
            infoFloor12.maxerr = infoFloor1.maxerr;
            infoFloor12.twofitminsize = infoFloor1.twofitminsize;
            infoFloor12.twofitminused = infoFloor1.twofitminused;
            infoFloor12.twofitweight = infoFloor1.twofitweight;
            infoFloor12.twofitatten = infoFloor1.twofitatten;
            infoFloor12.unusedminsize = infoFloor1.unusedminsize;
            infoFloor12.unusedmin_n = infoFloor1.unusedmin_n;
            infoFloor12.n = infoFloor1.n;
            return infoFloor12;
        }
    }

    @Environment(value=EnvType.CLIENT)
    class LookFloor1 {
        static final int VIF_POSIT = 63;
        int[] sorted_index = new int[65];
        int[] forward_index = new int[65];
        int[] reverse_index = new int[65];
        int[] hineighbor = new int[63];
        int[] loneighbor = new int[63];
        int posts;
        int n;
        int quant_q;
        InfoFloor1 vi;
        int phrasebits;
        int postbits;
        int frames;

        LookFloor1() {
        }

        void free() {
            this.sorted_index = null;
            this.forward_index = null;
            this.reverse_index = null;
            this.hineighbor = null;
            this.loneighbor = null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    class Lsfit_acc {
        long x0;
        long x1;
        long xa;
        long ya;
        long x2a;
        long y2a;
        long xya;
        long n;
        long an;
        long un;
        long edgey0;
        long edgey1;

        Lsfit_acc() {
        }
    }
}

