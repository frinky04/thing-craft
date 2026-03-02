/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
class StaticCodeBook {
    int dim;
    int entries;
    int[] lengthlist;
    int maptype;
    int q_min;
    int q_delta;
    int q_quant;
    int q_sequencep;
    int[] quantlist;
    static final int VQ_FEXP = 10;
    static final int VQ_FMAN = 21;
    static final int VQ_FEXP_BIAS = 768;

    StaticCodeBook() {
    }

    int pack(Buffer buffer) {
        int i;
        boolean j = false;
        buffer.write(5653314, 24);
        buffer.write(this.dim, 16);
        buffer.write(this.entries, 24);
        for (i = 1; i < this.entries && this.lengthlist[i] >= this.lengthlist[i - 1]; ++i) {
        }
        if (i == this.entries) {
            j = true;
        }
        if (j) {
            int k = 0;
            buffer.write(1, 1);
            buffer.write(this.lengthlist[0] - 1, 5);
            for (i = 1; i < this.entries; ++i) {
                int m = this.lengthlist[i];
                int n = this.lengthlist[i - 1];
                if (m <= n) continue;
                for (int o = n; o < m; ++o) {
                    buffer.write(i - k, Util.ilog(this.entries - k));
                    k = i;
                }
            }
            buffer.write(i - k, Util.ilog(this.entries - k));
        } else {
            buffer.write(0, 1);
            for (i = 0; i < this.entries && this.lengthlist[i] != 0; ++i) {
            }
            if (i == this.entries) {
                buffer.write(0, 1);
                for (i = 0; i < this.entries; ++i) {
                    buffer.write(this.lengthlist[i] - 1, 5);
                }
            } else {
                buffer.write(1, 1);
                for (i = 0; i < this.entries; ++i) {
                    if (this.lengthlist[i] == 0) {
                        buffer.write(0, 1);
                        continue;
                    }
                    buffer.write(1, 1);
                    buffer.write(this.lengthlist[i] - 1, 5);
                }
            }
        }
        buffer.write(this.maptype, 4);
        switch (this.maptype) {
            case 0: {
                break;
            }
            case 1: 
            case 2: {
                if (this.quantlist == null) {
                    return -1;
                }
                buffer.write(this.q_min, 32);
                buffer.write(this.q_delta, 32);
                buffer.write(this.q_quant - 1, 4);
                buffer.write(this.q_sequencep, 1);
                int l = 0;
                switch (this.maptype) {
                    case 1: {
                        l = this.maptype1_quantvals();
                        break;
                    }
                    case 2: {
                        l = this.entries * this.dim;
                    }
                }
                for (i = 0; i < l; ++i) {
                    buffer.write(Math.abs(this.quantlist[i]), this.q_quant);
                }
                break;
            }
            default: {
                return -1;
            }
        }
        return 0;
    }

    int unpack(Buffer buffer) {
        if (buffer.read(24) != 5653314) {
            this.clear();
            return -1;
        }
        this.dim = buffer.read(16);
        this.entries = buffer.read(24);
        if (this.entries == -1) {
            this.clear();
            return -1;
        }
        switch (buffer.read(1)) {
            case 0: {
                this.lengthlist = new int[this.entries];
                if (buffer.read(1) != 0) {
                    for (int i = 0; i < this.entries; ++i) {
                        if (buffer.read(1) != 0) {
                            int m = buffer.read(5);
                            if (m == -1) {
                                this.clear();
                                return -1;
                            }
                            this.lengthlist[i] = m + 1;
                            continue;
                        }
                        this.lengthlist[i] = 0;
                    }
                } else {
                    for (int j = 0; j < this.entries; ++j) {
                        int n = buffer.read(5);
                        if (n == -1) {
                            this.clear();
                            return -1;
                        }
                        this.lengthlist[j] = n + 1;
                    }
                }
                break;
            }
            case 1: {
                int o = buffer.read(5) + 1;
                this.lengthlist = new int[this.entries];
                int k = 0;
                while (k < this.entries) {
                    int q = buffer.read(Util.ilog(this.entries - k));
                    if (q == -1) {
                        this.clear();
                        return -1;
                    }
                    int r = 0;
                    while (r < q) {
                        this.lengthlist[k] = o;
                        ++r;
                        ++k;
                    }
                    ++o;
                }
                break;
            }
            default: {
                return -1;
            }
        }
        this.maptype = buffer.read(4);
        switch (this.maptype) {
            case 0: {
                break;
            }
            case 1: 
            case 2: {
                this.q_min = buffer.read(32);
                this.q_delta = buffer.read(32);
                this.q_quant = buffer.read(4) + 1;
                this.q_sequencep = buffer.read(1);
                int p = 0;
                switch (this.maptype) {
                    case 1: {
                        p = this.maptype1_quantvals();
                        break;
                    }
                    case 2: {
                        p = this.entries * this.dim;
                    }
                }
                this.quantlist = new int[p];
                for (int l = 0; l < p; ++l) {
                    this.quantlist[l] = buffer.read(this.q_quant);
                }
                if (this.quantlist[p - 1] != -1) break;
                this.clear();
                return -1;
            }
            default: {
                this.clear();
                return -1;
            }
        }
        return 0;
    }

    private int maptype1_quantvals() {
        int i = MathHelper.floor(Math.pow(this.entries, 1.0 / (double)this.dim));
        while (true) {
            int j = 1;
            int k = 1;
            for (int l = 0; l < this.dim; ++l) {
                j *= i;
                k *= i + 1;
            }
            if (j <= this.entries && k > this.entries) {
                return i;
            }
            if (j > this.entries) {
                --i;
                continue;
            }
            ++i;
        }
    }

    void clear() {
    }

    float[] unquantize() {
        if (this.maptype == 1 || this.maptype == 2) {
            float f = StaticCodeBook.float32_unpack(this.q_min);
            float g = StaticCodeBook.float32_unpack(this.q_delta);
            float[] fs = new float[this.entries * this.dim];
            switch (this.maptype) {
                case 1: {
                    int i = this.maptype1_quantvals();
                    for (int j = 0; j < this.entries; ++j) {
                        float h = 0.0f;
                        int m = 1;
                        for (int o = 0; o < this.dim; ++o) {
                            int q = j / m % i;
                            float r = this.quantlist[q];
                            r = Math.abs(r) * g + f + h;
                            if (this.q_sequencep != 0) {
                                h = r;
                            }
                            fs[j * this.dim + o] = r;
                            m *= i;
                        }
                    }
                    break;
                }
                case 2: {
                    for (int k = 0; k < this.entries; ++k) {
                        float l = 0.0f;
                        for (int n = 0; n < this.dim; ++n) {
                            float p = this.quantlist[k * this.dim + n];
                            p = Math.abs(p) * g + f + l;
                            if (this.q_sequencep != 0) {
                                l = p;
                            }
                            fs[k * this.dim + n] = p;
                        }
                    }
                    break;
                }
            }
            return fs;
        }
        return null;
    }

    static long float32_pack(float f) {
        int i = 0;
        if (f < 0.0f) {
            i = Integer.MIN_VALUE;
            f = -f;
        }
        int j = MathHelper.floor(Math.log(f) / Math.log(2.0));
        int k = (int)Math.rint(Math.pow(f, 20 - j));
        j = j + 768 << 21;
        return i | j | k;
    }

    static float float32_unpack(int i) {
        float f = i & 0x1FFFFF;
        float g = (i & 0x7FE00000) >>> 21;
        if ((i & Integer.MIN_VALUE) != 0) {
            f = -f;
        }
        return StaticCodeBook.ldexp(f, (int)g - 20 - 768);
    }

    static float ldexp(float f, int i) {
        return (float)((double)f * Math.pow(2.0, i));
    }
}

