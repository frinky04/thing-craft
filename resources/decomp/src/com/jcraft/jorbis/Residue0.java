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
import com.jcraft.jorbis.FuncResidue;
import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.InfoMode;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Residue0
extends FuncResidue {
    private static int[][][] _01inverse_partword = new int[2][][];
    static int[][] _2inverse_partword = null;

    Residue0() {
    }

    void pack(Object object, Buffer buffer) {
        int j;
        InfoResidue0 infoResidue0 = (InfoResidue0)object;
        int i = 0;
        buffer.write(infoResidue0.begin, 24);
        buffer.write(infoResidue0.end, 24);
        buffer.write(infoResidue0.grouping - 1, 24);
        buffer.write(infoResidue0.partitions - 1, 6);
        buffer.write(infoResidue0.groupbook, 8);
        for (j = 0; j < infoResidue0.partitions; ++j) {
            int k = infoResidue0.secondstages[j];
            if (Util.ilog(k) > 3) {
                buffer.write(k, 3);
                buffer.write(1, 1);
                buffer.write(k >>> 3, 5);
            } else {
                buffer.write(k, 4);
            }
            i += Util.icount(k);
        }
        for (j = 0; j < i; ++j) {
            buffer.write(infoResidue0.booklist[j], 8);
        }
    }

    Object unpack(Info info, Buffer buffer) {
        int j;
        int i = 0;
        InfoResidue0 infoResidue0 = new InfoResidue0();
        infoResidue0.begin = buffer.read(24);
        infoResidue0.end = buffer.read(24);
        infoResidue0.grouping = buffer.read(24) + 1;
        infoResidue0.partitions = buffer.read(6) + 1;
        infoResidue0.groupbook = buffer.read(8);
        for (j = 0; j < infoResidue0.partitions; ++j) {
            int k = buffer.read(3);
            if (buffer.read(1) != 0) {
                k |= buffer.read(5) << 3;
            }
            infoResidue0.secondstages[j] = k;
            i += Util.icount(k);
        }
        for (j = 0; j < i; ++j) {
            infoResidue0.booklist[j] = buffer.read(8);
        }
        if (infoResidue0.groupbook >= info.books) {
            this.free_info(infoResidue0);
            return null;
        }
        for (j = 0; j < i; ++j) {
            if (infoResidue0.booklist[j] < info.books) continue;
            this.free_info(infoResidue0);
            return null;
        }
        return infoResidue0;
    }

    Object look(DspState dspState, InfoMode infoMode, Object object) {
        int l;
        InfoResidue0 infoResidue0 = (InfoResidue0)object;
        LookResidue0 lookResidue0 = new LookResidue0();
        int i = 0;
        int k = 0;
        lookResidue0.info = infoResidue0;
        lookResidue0.map = infoMode.mapping;
        lookResidue0.parts = infoResidue0.partitions;
        lookResidue0.fullbooks = dspState.fullbooks;
        lookResidue0.phrasebook = dspState.fullbooks[infoResidue0.groupbook];
        int j = lookResidue0.phrasebook.dim;
        lookResidue0.partbooks = new int[lookResidue0.parts][];
        for (l = 0; l < lookResidue0.parts; ++l) {
            int m = infoResidue0.secondstages[l];
            int o = Util.ilog(m);
            if (o == 0) continue;
            if (o > k) {
                k = o;
            }
            lookResidue0.partbooks[l] = new int[o];
            for (int q = 0; q < o; ++q) {
                if ((m & 1 << q) == 0) continue;
                lookResidue0.partbooks[l][q] = infoResidue0.booklist[i++];
            }
        }
        lookResidue0.partvals = (int)Math.rint(Math.pow(lookResidue0.parts, j));
        lookResidue0.stages = k;
        lookResidue0.decodemap = new int[lookResidue0.partvals][];
        for (l = 0; l < lookResidue0.partvals; ++l) {
            int n = l;
            int p = lookResidue0.partvals / lookResidue0.parts;
            lookResidue0.decodemap[l] = new int[j];
            for (int r = 0; r < j; ++r) {
                int s = n / p;
                n -= s * p;
                p /= lookResidue0.parts;
                lookResidue0.decodemap[l][r] = s;
            }
        }
        return lookResidue0;
    }

    void free_info(Object object) {
    }

    void free_look(Object object) {
    }

    /*
     * WARNING - void declaration
     */
    static synchronized int _01inverse(Block block, Object object, float[][] fs, int i, int j) {
        int l;
        LookResidue0 lookResidue0 = (LookResidue0)object;
        InfoResidue0 infoResidue0 = lookResidue0.info;
        int p = infoResidue0.grouping;
        int q = lookResidue0.phrasebook.dim;
        int r = infoResidue0.end - infoResidue0.begin;
        int s = r / p;
        int t = (s + q - 1) / q;
        if (_01inverse_partword.length < i) {
            _01inverse_partword = new int[i][][];
        }
        for (l = 0; l < i; ++l) {
            if (_01inverse_partword[l] != null && _01inverse_partword[l].length >= t) continue;
            Residue0._01inverse_partword[l] = new int[t][];
        }
        for (int o = 0; o < lookResidue0.stages; ++o) {
            void k;
            boolean bl = false;
            int n = 0;
            while (k < s) {
                if (o == 0) {
                    for (l = 0; l < i; ++l) {
                        int u = lookResidue0.phrasebook.decode(block.opb);
                        if (u == -1) {
                            return 0;
                        }
                        Residue0._01inverse_partword[l][n] = lookResidue0.decodemap[u];
                        if (_01inverse_partword[l][n] != null) continue;
                        return 0;
                    }
                }
                for (int m = 0; m < q && k < s; ++m, ++k) {
                    for (l = 0; l < i; ++l) {
                        CodeBook codeBook;
                        int v = infoResidue0.begin + k * p;
                        int w = _01inverse_partword[l][n][m];
                        if ((infoResidue0.secondstages[w] & 1 << o) == 0 || (codeBook = lookResidue0.fullbooks[lookResidue0.partbooks[w][o]]) == null || !(j == 0 ? codeBook.decodevs_add(fs[l], v, block.opb, p) == -1 : j == 1 && codeBook.decodev_add(fs[l], v, block.opb, p) == -1)) continue;
                        return 0;
                    }
                }
                ++n;
            }
        }
        return 0;
    }

    /*
     * WARNING - void declaration
     */
    static synchronized int _2inverse(Block block, Object object, float[][] fs, int i) {
        LookResidue0 lookResidue0 = (LookResidue0)object;
        InfoResidue0 infoResidue0 = lookResidue0.info;
        int n = infoResidue0.grouping;
        int o = lookResidue0.phrasebook.dim;
        int p = infoResidue0.end - infoResidue0.begin;
        int q = p / n;
        int r = (q + o - 1) / o;
        if (_2inverse_partword == null || _2inverse_partword.length < r) {
            _2inverse_partword = new int[r][];
        }
        for (int m = 0; m < lookResidue0.stages; ++m) {
            void j;
            boolean bl = false;
            int l = 0;
            while (j < q) {
                if (m == 0) {
                    int s = lookResidue0.phrasebook.decode(block.opb);
                    if (s == -1) {
                        return 0;
                    }
                    Residue0._2inverse_partword[l] = lookResidue0.decodemap[s];
                    if (_2inverse_partword[l] == null) {
                        return 0;
                    }
                }
                for (int k = 0; k < o && j < q; ++k, ++j) {
                    CodeBook codeBook;
                    int t = infoResidue0.begin + j * n;
                    int u = _2inverse_partword[l][k];
                    if ((infoResidue0.secondstages[u] & 1 << m) == 0 || (codeBook = lookResidue0.fullbooks[lookResidue0.partbooks[u][m]]) == null || codeBook.decodevv_add(fs, t, i, block.opb, n) != -1) continue;
                    return 0;
                }
                ++l;
            }
        }
        return 0;
    }

    int inverse(Block block, Object object, float[][] fs, int[] is, int i) {
        int j = 0;
        for (int k = 0; k < i; ++k) {
            if (is[k] == 0) continue;
            fs[j++] = fs[k];
        }
        if (j != 0) {
            return Residue0._01inverse(block, object, fs, j, 0);
        }
        return 0;
    }

    @Environment(value=EnvType.CLIENT)
    class InfoResidue0 {
        int begin;
        int end;
        int grouping;
        int partitions;
        int groupbook;
        int[] secondstages = new int[64];
        int[] booklist = new int[256];
        float[] entmax = new float[64];
        float[] ampmax = new float[64];
        int[] subgrp = new int[64];
        int[] blimit = new int[64];

        InfoResidue0() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    class LookResidue0 {
        InfoResidue0 info;
        int map;
        int parts;
        int stages;
        CodeBook[] fullbooks;
        CodeBook phrasebook;
        int[][] partbooks;
        int partvals;
        int[][] decodemap;
        int postbits;
        int phrasebits;
        int frames;

        LookResidue0() {
        }
    }
}

