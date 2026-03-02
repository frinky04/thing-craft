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
import com.jcraft.jorbis.Lpc;
import com.jcraft.jorbis.Lsp;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
class Floor0
extends FuncFloor {
    float[] lsp = null;

    Floor0() {
    }

    void pack(Object object, Buffer buffer) {
        InfoFloor0 infoFloor0 = (InfoFloor0)object;
        buffer.write(infoFloor0.order, 8);
        buffer.write(infoFloor0.rate, 16);
        buffer.write(infoFloor0.barkmap, 16);
        buffer.write(infoFloor0.ampbits, 6);
        buffer.write(infoFloor0.ampdB, 8);
        buffer.write(infoFloor0.numbooks - 1, 4);
        for (int i = 0; i < infoFloor0.numbooks; ++i) {
            buffer.write(infoFloor0.books[i], 8);
        }
    }

    Object unpack(Info info, Buffer buffer) {
        InfoFloor0 infoFloor0 = new InfoFloor0();
        infoFloor0.order = buffer.read(8);
        infoFloor0.rate = buffer.read(16);
        infoFloor0.barkmap = buffer.read(16);
        infoFloor0.ampbits = buffer.read(6);
        infoFloor0.ampdB = buffer.read(8);
        infoFloor0.numbooks = buffer.read(4) + 1;
        if (infoFloor0.order < 1 || infoFloor0.rate < 1 || infoFloor0.barkmap < 1 || infoFloor0.numbooks < 1) {
            return null;
        }
        for (int i = 0; i < infoFloor0.numbooks; ++i) {
            infoFloor0.books[i] = buffer.read(8);
            if (infoFloor0.books[i] >= 0 && infoFloor0.books[i] < info.books) continue;
            return null;
        }
        return infoFloor0;
    }

    Object look(DspState dspState, InfoMode infoMode, Object object) {
        Info info = dspState.vi;
        InfoFloor0 infoFloor0 = (InfoFloor0)object;
        LookFloor0 lookFloor0 = new LookFloor0();
        lookFloor0.m = infoFloor0.order;
        lookFloor0.n = info.blocksizes[infoMode.blockflag] / 2;
        lookFloor0.ln = infoFloor0.barkmap;
        lookFloor0.vi = infoFloor0;
        lookFloor0.lpclook.init(lookFloor0.ln, lookFloor0.m);
        float f = (float)lookFloor0.ln / Floor0.toBARK((float)((double)infoFloor0.rate / 2.0));
        lookFloor0.linearmap = new int[lookFloor0.n];
        for (int i = 0; i < lookFloor0.n; ++i) {
            int j = MathHelper.floor(Floor0.toBARK((float)((double)infoFloor0.rate / 2.0 / (double)lookFloor0.n * (double)i)) * f);
            if (j >= lookFloor0.ln) {
                j = lookFloor0.ln;
            }
            lookFloor0.linearmap[i] = j;
        }
        return lookFloor0;
    }

    static float toBARK(float f) {
        return (float)(13.1 * Math.atan(7.4E-4 * (double)f) + 2.24 * Math.atan((double)(f * f) * 1.85E-8) + 1.0E-4 * (double)f);
    }

    Object state(Object object) {
        EchstateFloor0 echstateFloor0 = new EchstateFloor0();
        InfoFloor0 infoFloor0 = (InfoFloor0)object;
        echstateFloor0.codewords = new int[infoFloor0.order];
        echstateFloor0.curve = new float[infoFloor0.barkmap];
        echstateFloor0.frameno = -1L;
        return echstateFloor0;
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    int inverse(Block block, Object object, float[] fs) {
        LookFloor0 lookFloor0 = (LookFloor0)object;
        InfoFloor0 infoFloor0 = lookFloor0.vi;
        int i = block.opb.read(infoFloor0.ampbits);
        if (i > 0) {
            int j = (1 << infoFloor0.ampbits) - 1;
            float f = (float)i / (float)j * (float)infoFloor0.ampdB;
            int k = block.opb.read(Util.ilog(infoFloor0.numbooks));
            if (k != -1 && k < infoFloor0.numbooks) {
                Floor0 floor0 = this;
                synchronized (floor0) {
                    try {
                        int m;
                        if (this.lsp == null || this.lsp.length < lookFloor0.m) {
                            this.lsp = new float[lookFloor0.m];
                        } else {
                            for (int l = 0; l < lookFloor0.m; ++l) {
                                this.lsp[l] = 0.0f;
                            }
                        }
                        CodeBook codeBook = block.vd.fullbooks[infoFloor0.books[k]];
                        float g = 0.0f;
                        for (m = 0; m < lookFloor0.m; ++m) {
                            fs[m] = 0.0f;
                        }
                        for (m = 0; m < lookFloor0.m; m += codeBook.dim) {
                            if (codeBook.decodevs(this.lsp, m, block.opb, 1, -1) != -1) continue;
                            for (int n = 0; n < lookFloor0.n; ++n) {
                                fs[n] = 0.0f;
                            }
                            // ** MonitorExit[floor0] (shouldn't be in output)
                            return 0;
                        }
                        m = 0;
                        while (m < lookFloor0.m) {
                            for (int o = 0; o < codeBook.dim; ++o) {
                                int n = m++;
                                this.lsp[n] = this.lsp[n] + g;
                            }
                            g = this.lsp[m - 1];
                        }
                        Lsp.lsp_to_curve(fs, lookFloor0.linearmap, lookFloor0.n, lookFloor0.ln, this.lsp, lookFloor0.m, f, infoFloor0.ampdB);
                        // ** MonitorExit[floor0] (shouldn't be in output)
                        return 1;
                    }
                    catch (Throwable throwable) {
                        void throwable2;
                        // ** MonitorExit[floor0] (shouldn't be in output)
                        throw throwable2;
                    }
                }
            }
        }
        return 0;
    }

    Object inverse1(Block block, Object object, Object object2) {
        int i;
        float[] fs;
        LookFloor0 lookFloor0 = (LookFloor0)object;
        InfoFloor0 infoFloor0 = lookFloor0.vi;
        Object object3 = null;
        if (object2 instanceof float[]) {
            fs = (float[])object2;
        }
        if ((i = block.opb.read(infoFloor0.ampbits)) > 0) {
            int j = (1 << infoFloor0.ampbits) - 1;
            float f = (float)i / (float)j * (float)infoFloor0.ampdB;
            int k = block.opb.read(Util.ilog(infoFloor0.numbooks));
            if (k != -1 && k < infoFloor0.numbooks) {
                int m;
                CodeBook codeBook = block.vd.fullbooks[infoFloor0.books[k]];
                float g = 0.0f;
                if (fs == null || fs.length < lookFloor0.m + 1) {
                    fs = new float[lookFloor0.m + 1];
                } else {
                    for (int l = 0; l < fs.length; ++l) {
                        fs[l] = 0.0f;
                    }
                }
                for (m = 0; m < lookFloor0.m; m += codeBook.dim) {
                    if (codeBook.decodev_set(fs, m, block.opb, codeBook.dim) != -1) continue;
                    return null;
                }
                m = 0;
                while (m < lookFloor0.m) {
                    for (int n = 0; n < codeBook.dim; ++n) {
                        int n2 = m++;
                        fs[n2] = fs[n2] + g;
                    }
                    g = fs[m - 1];
                }
                fs[lookFloor0.m] = f;
                return fs;
            }
        }
        return null;
    }

    int inverse2(Block block, Object object, Object object2, float[] fs) {
        LookFloor0 lookFloor0 = (LookFloor0)object;
        InfoFloor0 infoFloor0 = lookFloor0.vi;
        if (object2 != null) {
            float[] gs = (float[])object2;
            float f = gs[lookFloor0.m];
            Lsp.lsp_to_curve(fs, lookFloor0.linearmap, lookFloor0.n, lookFloor0.ln, gs, lookFloor0.m, f, infoFloor0.ampdB);
            return 1;
        }
        for (int i = 0; i < lookFloor0.n; ++i) {
            fs[i] = 0.0f;
        }
        return 0;
    }

    static float fromdB(float f) {
        return (float)Math.exp((double)f * 0.11512925);
    }

    /*
     * WARNING - void declaration
     */
    static void lsp_to_lpc(float[] fs, float[] gs, int i) {
        int k;
        int j;
        int l = i / 2;
        float[] hs = new float[l];
        float[] is = new float[l];
        float[] js = new float[l + 1];
        float[] ks = new float[l + 1];
        float[] ls = new float[l];
        float[] ms = new float[l];
        for (j = 0; j < l; ++j) {
            hs[j] = (float)(-2.0 * Math.cos(fs[j * 2]));
            is[j] = (float)(-2.0 * Math.cos(fs[j * 2 + 1]));
        }
        for (k = 0; k < l; ++k) {
            js[k] = 0.0f;
            ks[k] = 1.0f;
            ls[k] = 0.0f;
            ms[k] = 1.0f;
        }
        ks[k] = 1.0f;
        js[k] = 1.0f;
        for (j = 1; j < i + 1; ++j) {
            void g;
            float f = 0.0f;
            float f2 = 0.0f;
            for (k = 0; k < l; ++k) {
                float h = hs[k] * ks[k] + js[k];
                js[k] = ks[k];
                ks[k] = f2;
                f2 += h;
                h = is[k] * ms[k] + ls[k];
                ls[k] = ms[k];
                ms[k] = g;
                g += h;
            }
            gs[j - 1] = (f2 + ks[k] + g - js[k]) / 2.0f;
            ks[k] = f2;
            js[k] = g;
        }
    }

    static void lpc_to_curve(float[] fs, float[] gs, float f, LookFloor0 lookFloor0, String string, int i) {
        float[] hs = new float[Math.max(lookFloor0.ln * 2, lookFloor0.m * 2 + 2)];
        if (f == 0.0f) {
            for (int j = 0; j < lookFloor0.n; ++j) {
                fs[j] = 0.0f;
            }
            return;
        }
        lookFloor0.lpclook.lpc_to_curve(hs, gs, f);
        for (int k = 0; k < lookFloor0.n; ++k) {
            fs[k] = hs[lookFloor0.linearmap[k]];
        }
    }

    @Environment(value=EnvType.CLIENT)
    class EchstateFloor0 {
        int[] codewords;
        float[] curve;
        long frameno;
        long codes;

        EchstateFloor0() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    class InfoFloor0 {
        int order;
        int rate;
        int barkmap;
        int ampbits;
        int ampdB;
        int numbooks;
        int[] books = new int[16];

        InfoFloor0() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    class LookFloor0 {
        int n;
        int ln;
        int m;
        int[] linearmap;
        InfoFloor0 vi;
        Lpc lpclook = new Lpc();

        LookFloor0() {
        }
    }
}

