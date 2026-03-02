/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jorbis.Lookup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Lsp {
    static final float M_PI = (float)Math.PI;

    Lsp() {
    }

    static void lsp_to_curve(float[] fs, int[] is, int i, int j, float[] gs, int k, float f, float g) {
        int l;
        float h = (float)Math.PI / (float)j;
        for (l = 0; l < k; ++l) {
            gs[l] = Lookup.coslook(gs[l]);
        }
        int m = k / 2 * 2;
        l = 0;
        while (l < i) {
            int r;
            int n = is[l];
            float o = 0.70710677f;
            float p = 0.70710677f;
            float q = Lookup.coslook(h * (float)n);
            for (r = 0; r < m; r += 2) {
                p *= gs[r] - q;
                o *= gs[r + 1] - q;
            }
            if ((k & 1) != 0) {
                p *= gs[k - 1] - q;
                p *= p;
                o *= o * (1.0f - q * q);
            } else {
                p *= p * (1.0f + q);
                o *= o * (1.0f - q);
            }
            p = o + p;
            r = Float.floatToIntBits(p);
            int s = Integer.MAX_VALUE & r;
            int t = 0;
            if (s < 2139095040 && s != 0) {
                if (s < 0x800000) {
                    p = (float)((double)p * 3.3554432E7);
                    r = Float.floatToIntBits(p);
                    s = Integer.MAX_VALUE & r;
                    t = -25;
                }
                t += (s >>> 23) - 126;
                r = r & 0x807FFFFF | 0x3F000000;
                p = Float.intBitsToFloat(r);
            }
            p = Lookup.fromdBlook(f * Lookup.invsqlook(p) * Lookup.invsq2explook(t + k) - g);
            do {
                int n2 = l++;
                fs[n2] = fs[n2] * p;
            } while (l < i && is[l] == n);
        }
    }
}

