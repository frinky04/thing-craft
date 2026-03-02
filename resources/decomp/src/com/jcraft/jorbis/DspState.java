/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.CodeBook;
import com.jcraft.jorbis.FuncMapping;
import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.Mdct;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class DspState {
    static final float M_PI = (float)Math.PI;
    static final int VI_TRANSFORMB = 1;
    static final int VI_WINDOWB = 1;
    int analysisp;
    Info vi;
    int modebits;
    float[][] pcm;
    int pcm_storage;
    int pcm_current;
    int pcm_returned;
    float[] multipliers;
    int envelope_storage;
    int envelope_current;
    int eofflag;
    int lW;
    int W;
    int nW;
    int centerW;
    long granulepos;
    long sequence;
    long glue_bits;
    long time_bits;
    long floor_bits;
    long res_bits;
    float[][][][][] window;
    Object[][] transform = new Object[2][];
    CodeBook[] fullbooks;
    Object[] mode;
    byte[] header;
    byte[] header1;
    byte[] header2;

    public DspState() {
        this.window = new float[2][][][][];
        this.window[0] = new float[2][][][];
        this.window[0][0] = new float[2][][];
        this.window[0][1] = new float[2][][];
        this.window[0][0][0] = new float[2][];
        this.window[0][0][1] = new float[2][];
        this.window[0][1][0] = new float[2][];
        this.window[0][1][1] = new float[2][];
        this.window[1] = new float[2][][][];
        this.window[1][0] = new float[2][][];
        this.window[1][1] = new float[2][][];
        this.window[1][0][0] = new float[2][];
        this.window[1][0][1] = new float[2][];
        this.window[1][1][0] = new float[2][];
        this.window[1][1][1] = new float[2][];
    }

    static float[] window(int i, int j, int k, int l) {
        float[] fs = new float[j];
        switch (i) {
            case 0: {
                int q;
                int m = j / 4 - k / 2;
                int o = j - j / 4 - l / 2;
                for (q = 0; q < k; ++q) {
                    float f = (float)(((double)q + 0.5) / (double)k * 3.1415927410125732 / 2.0);
                    f = (float)Math.sin(f);
                    f *= f;
                    f = (float)((double)f * 1.5707963705062866);
                    fs[q + m] = f = (float)Math.sin(f);
                }
                for (q = m + k; q < o; ++q) {
                    fs[q] = 1.0f;
                }
                for (q = 0; q < l; ++q) {
                    float g = (float)(((double)(l - q) - 0.5) / (double)l * 3.1415927410125732 / 2.0);
                    g = (float)Math.sin(g);
                    g *= g;
                    g = (float)((double)g * 1.5707963705062866);
                    fs[q + o] = g = (float)Math.sin(g);
                }
                break;
            }
            default: {
                return null;
            }
        }
        return fs;
    }

    int init(Info info, boolean bl) {
        int i;
        this.vi = info;
        this.modebits = Util.ilog2(info.modes);
        this.transform[0] = new Object[1];
        this.transform[1] = new Object[1];
        this.transform[0][0] = new Mdct();
        this.transform[1][0] = new Mdct();
        ((Mdct)this.transform[0][0]).init(info.blocksizes[0]);
        ((Mdct)this.transform[1][0]).init(info.blocksizes[1]);
        this.window[0][0][0] = new float[1][];
        this.window[0][0][1] = this.window[0][0][0];
        this.window[0][1][0] = this.window[0][0][0];
        this.window[0][1][1] = this.window[0][0][0];
        this.window[1][0][0] = new float[1][];
        this.window[1][0][1] = new float[1][];
        this.window[1][1][0] = new float[1][];
        this.window[1][1][1] = new float[1][];
        for (i = 0; i < 1; ++i) {
            this.window[0][0][0][i] = DspState.window(i, info.blocksizes[0], info.blocksizes[0] / 2, info.blocksizes[0] / 2);
            this.window[1][0][0][i] = DspState.window(i, info.blocksizes[1], info.blocksizes[0] / 2, info.blocksizes[0] / 2);
            this.window[1][0][1][i] = DspState.window(i, info.blocksizes[1], info.blocksizes[0] / 2, info.blocksizes[1] / 2);
            this.window[1][1][0][i] = DspState.window(i, info.blocksizes[1], info.blocksizes[1] / 2, info.blocksizes[0] / 2);
            this.window[1][1][1][i] = DspState.window(i, info.blocksizes[1], info.blocksizes[1] / 2, info.blocksizes[1] / 2);
        }
        this.fullbooks = new CodeBook[info.books];
        for (i = 0; i < info.books; ++i) {
            this.fullbooks[i] = new CodeBook();
            this.fullbooks[i].init_decode(info.book_param[i]);
        }
        this.pcm_storage = 8192;
        this.pcm = new float[info.channels][];
        for (i = 0; i < info.channels; ++i) {
            this.pcm[i] = new float[this.pcm_storage];
        }
        this.lW = 0;
        this.W = 0;
        this.pcm_current = this.centerW = info.blocksizes[1] / 2;
        this.mode = new Object[info.modes];
        for (i = 0; i < info.modes; ++i) {
            int j = info.mode_param[i].mapping;
            int k = info.map_type[j];
            this.mode[i] = FuncMapping.mapping_P[k].look(this, info.mode_param[i], info.map_param[j]);
        }
        return 0;
    }

    public int synthesis_init(Info info) {
        this.init(info, false);
        this.pcm_returned = this.centerW;
        this.centerW -= info.blocksizes[this.W] / 4 + info.blocksizes[this.lW] / 4;
        this.granulepos = -1L;
        this.sequence = -1L;
        return 0;
    }

    DspState(Info info) {
        this();
        this.init(info, false);
        this.pcm_returned = this.centerW;
        this.centerW -= info.blocksizes[this.W] / 4 + info.blocksizes[this.lW] / 4;
        this.granulepos = -1L;
        this.sequence = -1L;
    }

    public int synthesis_blockin(Block block) {
        if (this.centerW > this.vi.blocksizes[1] / 2 && this.pcm_returned > 8192) {
            int i = this.centerW - this.vi.blocksizes[1] / 2;
            i = this.pcm_returned < i ? this.pcm_returned : i;
            this.pcm_current -= i;
            this.centerW -= i;
            this.pcm_returned -= i;
            if (i != 0) {
                for (int k = 0; k < this.vi.channels; ++k) {
                    System.arraycopy(this.pcm[k], i, this.pcm[k], 0, this.pcm_current);
                }
            }
        }
        this.lW = this.W;
        this.W = block.W;
        this.nW = -1;
        this.glue_bits += (long)block.glue_bits;
        this.time_bits += (long)block.time_bits;
        this.floor_bits += (long)block.floor_bits;
        this.res_bits += (long)block.res_bits;
        if (this.sequence + 1L != block.sequence) {
            this.granulepos = -1L;
        }
        this.sequence = block.sequence;
        int j = this.vi.blocksizes[this.W];
        int l = this.centerW + this.vi.blocksizes[this.lW] / 4 + j / 4;
        int m = l - j / 2;
        int n = m + j;
        int o = 0;
        int p = 0;
        if (n > this.pcm_storage) {
            this.pcm_storage = n + this.vi.blocksizes[1];
            for (int q = 0; q < this.vi.channels; ++q) {
                float[] fs = new float[this.pcm_storage];
                System.arraycopy(this.pcm[q], 0, fs, 0, this.pcm[q].length);
                this.pcm[q] = fs;
            }
        }
        switch (this.W) {
            case 0: {
                o = 0;
                p = this.vi.blocksizes[0] / 2;
                break;
            }
            case 1: {
                o = this.vi.blocksizes[1] / 4 - this.vi.blocksizes[this.lW] / 4;
                p = o + this.vi.blocksizes[this.lW] / 2;
            }
        }
        for (int r = 0; r < this.vi.channels; ++r) {
            int s = m;
            int t = 0;
            for (t = o; t < p; ++t) {
                float[] fArray = this.pcm[r];
                int n2 = s + t;
                fArray[n2] = fArray[n2] + block.pcm[r][t];
            }
            while (t < j) {
                this.pcm[r][s + t] = block.pcm[r][t];
                ++t;
            }
        }
        if (this.granulepos == -1L) {
            this.granulepos = block.granulepos;
        } else {
            this.granulepos += (long)(l - this.centerW);
            if (block.granulepos != -1L && this.granulepos != block.granulepos) {
                if (this.granulepos > block.granulepos && block.eofflag != 0) {
                    l = (int)((long)l - (this.granulepos - block.granulepos));
                }
                this.granulepos = block.granulepos;
            }
        }
        this.centerW = l;
        this.pcm_current = n;
        if (block.eofflag != 0) {
            this.eofflag = 1;
        }
        return 0;
    }

    public int synthesis_pcmout(float[][][] fs, int[] is) {
        if (this.pcm_returned < this.centerW) {
            if (fs != null) {
                for (int i = 0; i < this.vi.channels; ++i) {
                    is[i] = this.pcm_returned;
                }
                fs[0] = this.pcm;
            }
            return this.centerW - this.pcm_returned;
        }
        return 0;
    }

    public int synthesis_read(int i) {
        if (i != 0 && this.pcm_returned + i > this.centerW) {
            return -1;
        }
        this.pcm_returned += i;
        return 0;
    }

    public void clear() {
    }
}

