/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;
import com.jcraft.jorbis.StaticCodeBook;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class CodeBook {
    int dim;
    int entries;
    StaticCodeBook c = new StaticCodeBook();
    float[] valuelist;
    int[] codelist;
    DecodeAux decode_tree;
    private int[] t = new int[15];

    CodeBook() {
    }

    int encode(int i, Buffer buffer) {
        buffer.write(this.codelist[i], this.c.lengthlist[i]);
        return this.c.lengthlist[i];
    }

    int errorv(float[] fs) {
        int i = this.best(fs, 1);
        for (int j = 0; j < this.dim; ++j) {
            fs[j] = this.valuelist[i * this.dim + j];
        }
        return i;
    }

    int encodev(int i, float[] fs, Buffer buffer) {
        for (int j = 0; j < this.dim; ++j) {
            fs[j] = this.valuelist[i * this.dim + j];
        }
        return this.encode(i, buffer);
    }

    int encodevs(float[] fs, Buffer buffer, int i, int j) {
        int k = this.besterror(fs, i, j);
        return this.encode(k, buffer);
    }

    synchronized int decodevs_add(float[] fs, int i, Buffer buffer, int j) {
        int m;
        int k = j / this.dim;
        if (this.t.length < k) {
            this.t = new int[k];
        }
        for (m = 0; m < k; ++m) {
            int l = this.decode(buffer);
            if (l == -1) {
                return -1;
            }
            this.t[m] = l * this.dim;
        }
        m = 0;
        int o = 0;
        while (m < this.dim) {
            for (int n = 0; n < k; ++n) {
                int n2 = i + o + n;
                fs[n2] = fs[n2] + this.valuelist[this.t[n] + m];
            }
            ++m;
            o += k;
        }
        return 0;
    }

    int decodev_add(float[] fs, int i, Buffer buffer, int j) {
        if (this.dim > 8) {
            int k = 0;
            while (k < j) {
                int o = this.decode(buffer);
                if (o == -1) {
                    return -1;
                }
                int q = o * this.dim;
                int m = 0;
                while (m < this.dim) {
                    int n = i + k++;
                    fs[n] = fs[n] + this.valuelist[q + m++];
                }
            }
        } else {
            int l = 0;
            while (l < j) {
                int p = this.decode(buffer);
                if (p == -1) {
                    return -1;
                }
                int r = p * this.dim;
                int n = 0;
                switch (this.dim) {
                    case 8: {
                        int n2 = i + l++;
                        fs[n2] = fs[n2] + this.valuelist[r + n++];
                    }
                    case 7: {
                        int n3 = i + l++;
                        fs[n3] = fs[n3] + this.valuelist[r + n++];
                    }
                    case 6: {
                        int n4 = i + l++;
                        fs[n4] = fs[n4] + this.valuelist[r + n++];
                    }
                    case 5: {
                        int n5 = i + l++;
                        fs[n5] = fs[n5] + this.valuelist[r + n++];
                    }
                    case 4: {
                        int n6 = i + l++;
                        fs[n6] = fs[n6] + this.valuelist[r + n++];
                    }
                    case 3: {
                        int n7 = i + l++;
                        fs[n7] = fs[n7] + this.valuelist[r + n++];
                    }
                    case 2: {
                        int n8 = i + l++;
                        fs[n8] = fs[n8] + this.valuelist[r + n++];
                    }
                    case 1: {
                        int n9 = i + l++;
                        fs[n9] = fs[n9] + this.valuelist[r + n++];
                    }
                }
            }
        }
        return 0;
    }

    int decodev_set(float[] fs, int i, Buffer buffer, int j) {
        int k = 0;
        while (k < j) {
            int m = this.decode(buffer);
            if (m == -1) {
                return -1;
            }
            int n = m * this.dim;
            int l = 0;
            while (l < this.dim) {
                fs[i + k++] = this.valuelist[n + l++];
            }
        }
        return 0;
    }

    int decodevv_add(float[][] fs, int i, int j, Buffer buffer, int k) {
        int o = 0;
        int l = i / j;
        while (l < (i + k) / j) {
            int n = this.decode(buffer);
            if (n == -1) {
                return -1;
            }
            int p = n * this.dim;
            for (int m = 0; m < this.dim; ++m) {
                float[] fArray = fs[o++];
                int n2 = l++;
                fArray[n2] = fArray[n2] + this.valuelist[p + m];
                if (o != j) continue;
                o = 0;
            }
        }
        return 0;
    }

    int decode(Buffer buffer) {
        int i = 0;
        DecodeAux decodeAux = this.decode_tree;
        int j = buffer.look(decodeAux.tabn);
        if (j >= 0) {
            i = decodeAux.tab[j];
            buffer.adv(decodeAux.tabl[j]);
            if (i <= 0) {
                return -i;
            }
        }
        do {
            switch (buffer.read1()) {
                case 0: {
                    i = decodeAux.ptr0[i];
                    break;
                }
                case 1: {
                    i = decodeAux.ptr1[i];
                    break;
                }
                default: {
                    return -1;
                }
            }
        } while (i > 0);
        return -i;
    }

    /*
     * WARNING - void declaration
     */
    int decodevs(float[] fs, int i, Buffer buffer, int j, int k) {
        int l = this.decode(buffer);
        if (l == -1) {
            return -1;
        }
        switch (k) {
            case -1: {
                void m;
                boolean bl = false;
                int p = 0;
                while (m < this.dim) {
                    fs[i + p] = this.valuelist[l * this.dim + m];
                    ++m;
                    p += j;
                }
                break;
            }
            case 0: {
                void n;
                boolean m = false;
                int q = 0;
                while (n < this.dim) {
                    int n2 = i + q;
                    fs[n2] = fs[n2] + this.valuelist[l * this.dim + n];
                    ++n;
                    q += j;
                }
                break;
            }
            case 1: {
                void o;
                boolean n = false;
                int r = 0;
                while (o < this.dim) {
                    int n3 = i + r;
                    fs[n3] = fs[n3] * this.valuelist[l * this.dim + o];
                    ++o;
                    r += j;
                }
                break;
            }
        }
        return l;
    }

    int best(float[] fs, int i) {
        int j = -1;
        float f = 0.0f;
        int k = 0;
        for (int l = 0; l < this.entries; ++l) {
            if (this.c.lengthlist[l] > 0) {
                float g = CodeBook.dist(this.dim, this.valuelist, k, fs, i);
                if (j == -1 || g < f) {
                    f = g;
                    j = l;
                }
            }
            k += this.dim;
        }
        return j;
    }

    /*
     * WARNING - void declaration
     */
    int besterror(float[] fs, int i, int j) {
        int k = this.best(fs, i);
        switch (j) {
            case 0: {
                void l;
                boolean bl = false;
                int n = 0;
                while (l < this.dim) {
                    int n2 = n;
                    fs[n2] = fs[n2] - this.valuelist[k * this.dim + l];
                    ++l;
                    n += i;
                }
                break;
            }
            case 1: {
                void m;
                boolean l = false;
                int o = 0;
                while (m < this.dim) {
                    float f = this.valuelist[k * this.dim + m];
                    if (f == 0.0f) {
                        fs[o] = 0.0f;
                    } else {
                        int n = o;
                        fs[n] = fs[n] / f;
                    }
                    ++m;
                    o += i;
                }
                break;
            }
        }
        return k;
    }

    void clear() {
    }

    private static float dist(int i, float[] fs, int j, float[] gs, int k) {
        float f = 0.0f;
        for (int l = 0; l < i; ++l) {
            float g = fs[j + l] - gs[l * k];
            f += g * g;
        }
        return f;
    }

    int init_decode(StaticCodeBook staticCodeBook) {
        this.c = staticCodeBook;
        this.entries = staticCodeBook.entries;
        this.dim = staticCodeBook.dim;
        this.valuelist = staticCodeBook.unquantize();
        this.decode_tree = this.make_decode_tree();
        if (this.decode_tree == null) {
            this.clear();
            return -1;
        }
        return 0;
    }

    static int[] make_words(int[] is, int i) {
        int j;
        int[] js = new int[33];
        int[] ks = new int[i];
        for (j = 0; j < i; ++j) {
            int k = is[j];
            if (k <= 0) continue;
            int m = js[k];
            if (k < 32 && m >>> k != 0) {
                return null;
            }
            ks[j] = m;
            int o = k;
            while (o > 0) {
                if ((js[o] & 1) != 0) {
                    if (o == 1) {
                        js[1] = js[1] + 1;
                        break;
                    }
                    js[o] = js[o - 1] << 1;
                    break;
                }
                int n = o--;
                js[n] = js[n] + 1;
            }
            for (o = k + 1; o < 33 && js[o] >>> 1 == m; ++o) {
                m = js[o];
                js[o] = js[o - 1] << 1;
            }
        }
        for (j = 0; j < i; ++j) {
            int l = 0;
            for (int n = 0; n < is[j]; ++n) {
                l <<= 1;
                l |= ks[j] >>> n & 1;
            }
            ks[j] = l;
        }
        return ks;
    }

    DecodeAux make_decode_tree() {
        int j;
        int i = 0;
        DecodeAux decodeAux = new DecodeAux();
        decodeAux.ptr0 = new int[this.entries * 2];
        int[] is = decodeAux.ptr0;
        decodeAux.ptr1 = new int[this.entries * 2];
        int[] js = decodeAux.ptr1;
        int[] ks = CodeBook.make_words(this.c.lengthlist, this.c.entries);
        if (ks == null) {
            return null;
        }
        decodeAux.aux = this.entries * 2;
        for (j = 0; j < this.entries; ++j) {
            int m;
            if (this.c.lengthlist[j] <= 0) continue;
            int k = 0;
            for (m = 0; m < this.c.lengthlist[j] - 1; ++m) {
                int o = ks[j] >>> m & 1;
                if (o == 0) {
                    if (is[k] == 0) {
                        is[k] = ++i;
                    }
                    k = is[k];
                    continue;
                }
                if (js[k] == 0) {
                    js[k] = ++i;
                }
                k = js[k];
            }
            if ((ks[j] >>> m & 1) == 0) {
                is[k] = -j;
                continue;
            }
            js[k] = -j;
        }
        decodeAux.tabn = Util.ilog(this.entries) - 4;
        if (decodeAux.tabn < 5) {
            decodeAux.tabn = 5;
        }
        j = 1 << decodeAux.tabn;
        decodeAux.tab = new int[j];
        decodeAux.tabl = new int[j];
        for (int l = 0; l < j; ++l) {
            int n = 0;
            int p = 0;
            for (p = 0; p < decodeAux.tabn && (n > 0 || p == 0); ++p) {
                n = (l & 1 << p) != 0 ? js[n] : is[n];
            }
            decodeAux.tab[l] = n;
            decodeAux.tabl[l] = p;
        }
        return decodeAux;
    }

    @Environment(value=EnvType.CLIENT)
    class DecodeAux {
        int[] tab;
        int[] tabl;
        int tabn;
        int[] ptr0;
        int[] ptr1;
        int aux;

        DecodeAux() {
        }
    }
}

