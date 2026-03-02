/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Mdct {
    int n;
    int log2n;
    float[] trig;
    int[] bitrev;
    float scale;
    float[] _x = new float[1024];
    float[] _w = new float[1024];

    Mdct() {
    }

    void init(int i) {
        int p;
        this.bitrev = new int[i / 4];
        this.trig = new float[i + i / 4];
        this.log2n = (int)Math.rint(Math.log(i) / Math.log(2.0));
        this.n = i;
        int j = 0;
        int k = 1;
        int l = j + i / 2;
        int m = l + 1;
        int n = l + i / 2;
        int o = n + 1;
        for (p = 0; p < i / 4; ++p) {
            this.trig[j + p * 2] = (float)Math.cos(Math.PI / (double)i * (double)(4 * p));
            this.trig[k + p * 2] = (float)(-Math.sin(Math.PI / (double)i * (double)(4 * p)));
            this.trig[l + p * 2] = (float)Math.cos(Math.PI / (double)(2 * i) * (double)(2 * p + 1));
            this.trig[m + p * 2] = (float)Math.sin(Math.PI / (double)(2 * i) * (double)(2 * p + 1));
        }
        for (p = 0; p < i / 8; ++p) {
            this.trig[n + p * 2] = (float)Math.cos(Math.PI / (double)i * (double)(4 * p + 2));
            this.trig[o + p * 2] = (float)(-Math.sin(Math.PI / (double)i * (double)(4 * p + 2)));
        }
        p = (1 << this.log2n - 1) - 1;
        int q = 1 << this.log2n - 2;
        for (int r = 0; r < i / 8; ++r) {
            int s = 0;
            int t = 0;
            while (q >>> t != 0) {
                if ((q >>> t & r) != 0) {
                    s |= 1 << t;
                }
                ++t;
            }
            this.bitrev[r * 2] = ~s & p;
            this.bitrev[r * 2 + 1] = s;
        }
        this.scale = 4.0f / (float)i;
    }

    void clear() {
    }

    void forward(float[] fs, float[] gs) {
    }

    /*
     * WARNING - void declaration
     */
    synchronized void backward(float[] fs, float[] gs) {
        void q;
        int o;
        if (this._x.length < this.n / 2) {
            this._x = new float[this.n / 2];
        }
        if (this._w.length < this.n / 2) {
            this._w = new float[this.n / 2];
        }
        float[] hs = this._x;
        float[] is = this._w;
        int i = this.n >>> 1;
        int j = this.n >>> 2;
        int k = this.n >>> 3;
        int l = 1;
        int m = 0;
        int n = i;
        for (o = 0; o < k; ++o) {
            hs[m++] = -fs[l + 2] * this.trig[(n -= 2) + 1] - fs[l] * this.trig[n];
            hs[m++] = fs[l] * this.trig[n + 1] - fs[l + 2] * this.trig[n];
            l += 4;
        }
        l = i - 4;
        for (o = 0; o < k; ++o) {
            hs[m++] = fs[l] * this.trig[(n -= 2) + 1] + fs[l + 2] * this.trig[n];
            hs[m++] = fs[l] * this.trig[n] - fs[l + 2] * this.trig[n + 1];
            l -= 4;
        }
        float[] js = this.mdct_kernel(hs, is, this.n, i, j, k);
        m = 0;
        n = i;
        o = j;
        int p = o - 1;
        int n2 = j + i;
        void r = q - true;
        for (int s = 0; s < j; ++s) {
            float f = js[m] * this.trig[n + 1] - js[m + 1] * this.trig[n];
            float g = -(js[m] * this.trig[n] + js[m + 1] * this.trig[n + 1]);
            gs[o] = -f;
            gs[p] = f;
            gs[q] = g;
            gs[r] = g;
            ++o;
            --p;
            ++q;
            --r;
            m += 2;
            n += 2;
        }
    }

    private float[] mdct_kernel(float[] fs, float[] gs, int i, int j, int k, int l) {
        int q;
        int m = k;
        int n = 0;
        int o = k;
        int p = j;
        for (q = 0; q < k; ++q) {
            float f = fs[m] - fs[n];
            gs[o + q] = fs[m++] + fs[n++];
            float g = fs[m] - fs[n];
            gs[q++] = f * this.trig[p -= 4] + g * this.trig[p + 1];
            gs[q] = g * this.trig[p] - f * this.trig[p + 1];
            gs[o + q] = fs[m++] + fs[n++];
        }
        for (q = 0; q < this.log2n - 3; ++q) {
            int r = i >>> q + 2;
            int t = 1 << q + 3;
            int v = j - 2;
            p = 0;
            for (int y = 0; y < r >>> 2; ++y) {
                int aa = v;
                o = aa - (r >> 1);
                float h = this.trig[p];
                float af = this.trig[p + 1];
                v -= 2;
                ++r;
                for (int aj = 0; aj < 2 << q; ++aj) {
                    float ah = gs[aa] - gs[o];
                    fs[aa] = gs[aa] + gs[o];
                    float ad = gs[++aa] - gs[++o];
                    fs[aa] = gs[aa] + gs[o];
                    fs[o] = ad * h - ah * af;
                    fs[o - 1] = ah * h + ad * af;
                    aa -= r;
                    o -= r;
                }
                --r;
                p += t;
            }
            float[] hs = gs;
            gs = fs;
            fs = hs;
        }
        q = i;
        int s = 0;
        int u = 0;
        int w = j - 1;
        for (int x = 0; x < l; ++x) {
            int z = this.bitrev[s++];
            int ab = this.bitrev[s++];
            float ac = gs[z] - gs[ab + 1];
            float ae = gs[z - 1] + gs[ab];
            float ag = gs[z] + gs[ab + 1];
            float ai = gs[z - 1] - gs[ab];
            float ak = ac * this.trig[q];
            float al = ae * this.trig[q++];
            float am = ac * this.trig[q];
            float an = ae * this.trig[q++];
            fs[u++] = (ag + am + al) * 0.5f;
            fs[w--] = (-ai + an - ak) * 0.5f;
            fs[u++] = (ai + an - ak) * 0.5f;
            fs[w--] = (ag - am - al) * 0.5f;
        }
        return fs;
    }
}

