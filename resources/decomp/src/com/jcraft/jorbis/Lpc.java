/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jorbis.Drft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Lpc {
    Drft fft = new Drft();
    int ln;
    int m;

    Lpc() {
    }

    static float lpc_from_data(float[] fs, float[] gs, int i, int j) {
        float[] hs = new float[j + 1];
        int m = j + 1;
        while (m-- != 0) {
            float g = 0.0f;
            for (int k = m; k < i; ++k) {
                g += fs[k] * fs[k - m];
            }
            hs[m] = g;
        }
        float f = hs[0];
        for (int l = 0; l < j; ++l) {
            float h = -hs[l + 1];
            if (f == 0.0f) {
                for (int n = 0; n < j; ++n) {
                    gs[n] = 0.0f;
                }
                return 0.0f;
            }
            for (m = 0; m < l; ++m) {
                h -= gs[m] * hs[l - m];
            }
            gs[l] = h /= f;
            for (m = 0; m < l / 2; ++m) {
                float o = gs[m];
                int n = m;
                gs[n] = gs[n] + h * gs[l - 1 - m];
                int n2 = l - 1 - m;
                gs[n2] = gs[n2] + h * o;
            }
            if (l % 2 != 0) {
                int n = m;
                gs[n] = gs[n] + gs[m] * h;
            }
            f = (float)((double)f * (1.0 - (double)(h * h)));
        }
        return f;
    }

    float lpc_from_curve(float[] fs, float[] gs) {
        int j;
        int i = this.ln;
        float[] hs = new float[i + i];
        float f = (float)(0.5 / (double)i);
        for (j = 0; j < i; ++j) {
            hs[j * 2] = fs[j] * f;
            hs[j * 2 + 1] = 0.0f;
        }
        hs[i * 2 - 1] = fs[i - 1] * f;
        this.fft.backward(hs);
        j = 0;
        int k = (i *= 2) / 2;
        while (j < i / 2) {
            float g = hs[j];
            hs[j++] = hs[k];
            hs[k++] = g;
        }
        return Lpc.lpc_from_data(hs, gs, i, this.m);
    }

    void init(int i, int j) {
        this.ln = i;
        this.m = j;
        this.fft.init(i * 2);
    }

    void clear() {
        this.fft.clear();
    }

    static float FAST_HYPOT(float f, float g) {
        return (float)Math.sqrt(f * f + g * g);
    }

    void lpc_to_curve(float[] fs, float[] gs, float f) {
        int i;
        for (i = 0; i < this.ln * 2; ++i) {
            fs[i] = 0.0f;
        }
        if (f == 0.0f) {
            return;
        }
        for (i = 0; i < this.m; ++i) {
            fs[i * 2 + 1] = gs[i] / (4.0f * f);
            fs[i * 2 + 2] = -gs[i] / (4.0f * f);
        }
        this.fft.backward(fs);
        i = this.ln * 2;
        float g = (float)(1.0 / (double)f);
        fs[0] = (float)(1.0 / (double)(fs[0] * 2.0f + g));
        for (int j = 1; j < this.ln; ++j) {
            float h = fs[j] + fs[i - j];
            float k = fs[j] - fs[i - j];
            float l = h + g;
            fs[j] = (float)(1.0 / (double)Lpc.FAST_HYPOT(l, k));
        }
    }
}

