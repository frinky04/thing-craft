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
class Drft {
    int n;
    float[] trigcache;
    int[] splitcache;
    static int[] ntryh = new int[]{4, 2, 3, 5};
    static float tpi = (float)Math.PI * 2;
    static float hsqt2 = 0.70710677f;
    static float taui = 0.8660254f;
    static float taur = -0.5f;
    static float sqrt2 = 1.4142135f;

    Drft() {
    }

    void backward(float[] fs) {
        if (this.n == 1) {
            return;
        }
        Drft.drftb1(this.n, fs, this.trigcache, this.trigcache, this.n, this.splitcache);
    }

    void init(int i) {
        this.n = i;
        this.trigcache = new float[3 * i];
        this.splitcache = new int[32];
        Drft.fdrffti(i, this.trigcache, this.splitcache);
    }

    void clear() {
        if (this.trigcache != null) {
            this.trigcache = null;
        }
        if (this.splitcache != null) {
            this.splitcache = null;
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    static void drfti1(int i, float[] fs, int j, int[] is) {
        boolean bl = false;
        int p = -1;
        int ah = i;
        int ai = 0;
        int aj = 101;
        while (true) {
            switch (aj) {
                case 101: {
                    int m = ++p < 4 ? ntryh[p] : (m += 2);
                }
                case 104: {
                    int m;
                    int ab = ah / m;
                    int ac = ah - m * ab;
                    if (ac != 0) {
                        aj = 101;
                        break;
                    }
                    is[++ai + 1] = m;
                    ah = ab;
                    if (m != 2) {
                        aj = 107;
                        break;
                    }
                    if (ai == 1) {
                        aj = 107;
                        break;
                    }
                    for (int n = 1; n < ai; ++n) {
                        int v = ai - n + 1;
                        is[v + 1] = is[v];
                    }
                    is[2] = 2;
                }
                case 107: {
                    if (ah != 1) {
                        aj = 104;
                        break;
                    }
                    is[0] = i;
                    is[1] = ai;
                    float g = tpi / (float)i;
                    int z = 0;
                    int af = ai - 1;
                    int s = 1;
                    if (af == 0) {
                        return;
                    }
                    for (int q = 0; q < af; ++q) {
                        int y = is[q + 2];
                        int w = 0;
                        int u = s * y;
                        int ad = i / u;
                        int ae = y - 1;
                        for (p = 0; p < ae; z += ad, ++p) {
                            int o = z;
                            float k = (float)(w += s) * g;
                            float l = 0.0f;
                            for (int x = 2; x < ad; x += 2) {
                                float f = (l += 1.0f) * k;
                                fs[j + o++] = (float)Math.cos(f);
                                fs[j + o++] = (float)Math.sin(f);
                            }
                        }
                        s = u;
                    }
                    return;
                }
            }
        }
    }

    static void fdrffti(int i, float[] fs, int[] is) {
        if (i == 1) {
            return;
        }
        Drft.drfti1(i, fs, i, is);
    }

    static void dradf2(int i, int j, float[] fs, float[] gs, float[] hs, int k) {
        int p;
        int m;
        int n;
        int o = 0;
        int n2 = n = j * i;
        int q = i << 1;
        for (m = 0; m < j; ++m) {
            gs[o << 1] = fs[o] + fs[p];
            gs[(o << 1) + q - 1] = fs[o] - fs[p];
            o += i;
            p += i;
        }
        if (i < 2) {
            return;
        }
        if (i != 2) {
            o = 0;
            p = n2;
            for (m = 0; m < j; ++m) {
                q = p;
                int r = (o << 1) + (i << 1);
                int s = o;
                int t = o + o;
                for (int l = 2; l < i; l += 2) {
                    float g = hs[k + l - 2] * fs[(q += 2) - 1] + hs[k + l - 1] * fs[q];
                    float f = hs[k + l - 2] * fs[q] - hs[k + l - 1] * fs[q - 1];
                    gs[t += 2] = fs[s += 2] + f;
                    gs[r -= 2] = f - fs[s];
                    gs[t - 1] = fs[s - 1] + g;
                    gs[r - 1] = fs[s - 1] - g;
                }
                o += i;
                p += i;
            }
            if (i % 2 == 1) {
                return;
            }
        }
        o = i;
        q = p = o - 1;
        p += n2;
        for (m = 0; m < j; ++m) {
            gs[o] = -fs[p];
            gs[o - 1] = fs[q];
            o += i << 1;
            p += i;
            q += i;
        }
    }

    static void dradf4(int i, int j, float[] fs, float[] gs, float[] hs, int k, float[] is, int l, float[] js, int m) {
        int o;
        int p;
        int q = p = j * i;
        int t = q << 1;
        int r = q + (q << 1);
        int s = 0;
        for (o = 0; o < j; ++o) {
            float ah = fs[q] + fs[r];
            float ak = fs[s] + fs[t];
            int n = s << 2;
            gs[n] = ah + ak;
            gs[(i << 2) + u - 1] = ak - ah;
            gs[(u += i << 1) - true] = fs[s] - fs[t];
            gs[u] = fs[r] - fs[q];
            q += i;
            r += i;
            s += i;
            t += i;
        }
        if (i < 2) {
            return;
        }
        if (i != 2) {
            q = 0;
            for (o = 0; o < j; ++o) {
                r = q;
                t = q << 2;
                int n = i << 1;
                int v = n + t;
                for (int n2 = 2; n2 < i; n2 += 2) {
                    s = r += 2;
                    t += 2;
                    v -= 2;
                    float z = hs[k + n2 - 2] * fs[(s += p) - 1] + hs[k + n2 - 1] * fs[s];
                    float f = hs[k + n2 - 2] * fs[s] - hs[k + n2 - 1] * fs[s - 1];
                    float aa = is[l + n2 - 2] * fs[(s += p) - 1] + is[l + n2 - 1] * fs[s];
                    float g = is[l + n2 - 2] * fs[s] - is[l + n2 - 1] * fs[s - 1];
                    float ab = js[m + n2 - 2] * fs[(s += p) - 1] + js[m + n2 - 1] * fs[s];
                    float h = js[m + n2 - 2] * fs[s] - js[m + n2 - 1] * fs[s - 1];
                    float ai = z + ab;
                    float an = ab - z;
                    float ac = f + h;
                    float ag = f - h;
                    float ae = fs[r] + g;
                    float af = fs[r] - g;
                    float al = fs[r - 1] + aa;
                    float am = fs[r - 1] - aa;
                    gs[t - 1] = ai + al;
                    gs[t] = ac + ae;
                    gs[v - 1] = am - ag;
                    gs[v] = an - af;
                    gs[t + x - 1] = ag + am;
                    gs[t + x] = an + af;
                    gs[v + x - 1] = al - ai;
                    gs[v + x] = ac - ae;
                }
                q += i;
            }
            if ((i & 1) != 0) {
                return;
            }
        }
        q = p + i - 1;
        r = q + (p << 1);
        s = i << 2;
        t = i;
        int w = i << 1;
        int y = i;
        for (o = 0; o < j; ++o) {
            float ad = -hsqt2 * (fs[q] + fs[r]);
            float aj = hsqt2 * (fs[q] - fs[r]);
            gs[t - 1] = aj + fs[y - 1];
            gs[t + w - 1] = fs[y - 1] - aj;
            gs[t] = ad - fs[q + p];
            gs[t + w] = ad + fs[q + p];
            q += i;
            r += i;
            t += s;
            y += i;
        }
    }

    /*
     * Unable to fully structure code
     */
    static void dradfg(int i, int j, int k, int l, float[] fs, float[] gs, float[] hs, float[] is, float[] js, float[] ks, int m) {
        ba = 0;
        var38_12 = 0.0f;
        cw = 0.0f;
        cv = Drft.tpi / (float)j;
        cu = (float)Math.cos(cv);
        cw = (float)Math.sin(cv);
        p = j + 1 >> 1;
        da = j;
        cz = i;
        ct = i - 1 >> 1;
        at = k * i;
        cp = j * i;
        db = 100;
        block7: while (true) {
            switch (db) {
                case 101: {
                    if (i != 1) ** GOTO lbl20
                    db = 119;
                    ** GOTO lbl258
lbl20:
                    // 2 sources

                    for (aq = 0; aq < l; ++aq) {
                        js[aq] = hs[aq];
                    }
                    au = 0;
                    for (y = 1; y < j; ++y) {
                        ba = au += at;
                        for (ad = 0; ad < k; ++ad) {
                            is[ba] = gs[ba];
                            ba += i;
                        }
                    }
                    as = -i;
                    au = 0;
                    if (ct > k) {
                        for (y = 1; y < j; ++y) {
                            as += i;
                            ba = -i + (au += at);
                            for (ae = 0; ae < k; ++ae) {
                                n = as - 1;
                                bb = ba += i;
                                for (q = 2; q < i; q += 2) {
                                    is[(bb += 2) - 1] = ks[m + (n += 2) - 1] * gs[bb - 1] + ks[m + n] * gs[bb];
                                    is[bb] = ks[m + n - 1] * gs[bb] - ks[m + n] * gs[bb - 1];
                                }
                            }
                        }
                    } else {
                        for (y = 1; y < j; ++y) {
                            o = (as += i) - 1;
                            ba = au += at;
                            for (r = 2; r < i; r += 2) {
                                o += 2;
                                bc = ba += 2;
                                for (af = 0; af < k; ++af) {
                                    is[bc - 1] = ks[m + o - 1] * gs[bc - 1] + ks[m + o] * gs[bc];
                                    is[bc] = ks[m + o - 1] * gs[bc] - ks[m + o] * gs[bc - 1];
                                    bc += i;
                                }
                            }
                        }
                    }
                    au = 0;
                    ba = da * at;
                    if (ct < k) {
                        for (y = 1; y < p; ++y) {
                            bd = au += at;
                            bk = ba -= at;
                            for (s = 2; s < i; s += 2) {
                                bs = (bd += 2) - i;
                                bz = (bk += 2) - i;
                                for (ag = 0; ag < k; ++ag) {
                                    gs[(bs += i) - 1] = is[bs - 1] + is[(bz += i) - 1];
                                    gs[bz - 1] = is[bs] - is[bz];
                                    gs[bs] = is[bs] + is[bz];
                                    gs[bz] = is[bz - 1] - is[bs - 1];
                                }
                            }
                        }
                    } else {
                        for (y = 1; y < p; ++y) {
                            be = au += at;
                            bl = ba -= at;
                            for (ah = 0; ah < k; ++ah) {
                                bt = be;
                                ca = bl;
                                for (t = 2; t < i; t += 2) {
                                    gs[(bt += 2) - 1] = is[bt - 1] + is[(ca += 2) - 1];
                                    gs[ca - 1] = is[bt] - is[ca];
                                    gs[bt] = is[bt] + is[ca];
                                    gs[ca] = is[ca - 1] - is[bt - 1];
                                }
                                be += i;
                                bl += i;
                            }
                        }
                    }
                }
                case 119: {
                    for (ar = 0; ar < l; ++ar) {
                        hs[ar] = js[ar];
                    }
                    av = 0;
                    ba = da * l;
                    for (z = 1; z < p; ++z) {
                        bf = (av += at) - i;
                        bm = (ba -= at) - i;
                        for (ai = 0; ai < k; ++ai) {
                            gs[bf += i] = is[bf] + is[bm += i];
                            gs[bm] = is[bm] - is[bf];
                        }
                    }
                    cq = 1.0f;
                    g = 0.0f;
                    av = 0;
                    ba = da * l;
                    bg = (j - 1) * l;
                    for (ao = 1; ao < p; ++ao) {
                        cx = cu * cq - cw * g;
                        g = cu * g + cw * cq;
                        cq = cx;
                        bn = av += l;
                        bu = ba -= l;
                        cb = bg;
                        cf = l;
                        for (ar = 0; ar < l; ++ar) {
                            js[bn++] = hs[ar] + cq * hs[cf++];
                            js[bu++] = g * hs[cb++];
                        }
                        f = cq;
                        cs = g;
                        cr = cq;
                        h = g;
                        bn = l;
                        bu = (da - 1) * l;
                        for (z = 2; z < p; ++z) {
                            bn += l;
                            bu -= l;
                            cy = f * cr - cs * h;
                            h = f * h + cs * cr;
                            cr = cy;
                            cb = av;
                            cf = ba;
                            cj = bn;
                            cm = bu;
                            for (ar = 0; ar < l; ++ar) {
                                v0 = cb++;
                                js[v0] = js[v0] + cr * hs[cj++];
                                v1 = cf++;
                                js[v1] = js[v1] + h * hs[cm++];
                            }
                        }
                    }
                    av = 0;
                    for (z = 1; z < p; ++z) {
                        ba = av += l;
                        ar = 0;
                        while (ar < l) {
                            v2 = ar++;
                            js[v2] = js[v2] + hs[ba++];
                        }
                    }
                    if (i < k) {
                        db = 132;
                    } else {
                        av = 0;
                        ba = 0;
                        for (aj = 0; aj < k; ++aj) {
                            bg = av;
                            bo = ba;
                            for (u = 0; u < i; ++u) {
                                fs[bo++] = is[bg++];
                            }
                            av += i;
                            ba += cp;
                        }
                        db = 135;
                    }
                    ** GOTO lbl258
                }
                case 132: {
                    for (v = 0; v < i; ++v) {
                        aw = v;
                        ba = v;
                        for (ak = 0; ak < k; ++ak) {
                            fs[ba] = is[aw];
                            aw += i;
                            ba += cp;
                        }
                    }
                }
                case 135: {
                    ax = 0;
                    ba = i << 1;
                    bh = 0;
                    bp = da * at;
                    for (aa = 1; aa < p; ++aa) {
                        bv = ax += ba;
                        cc = bh += at;
                        cg = bp -= at;
                        for (al = 0; al < k; ++al) {
                            fs[bv - 1] = is[cc];
                            fs[bv] = is[cg];
                            bv += cp;
                            cc += i;
                            cg += i;
                        }
                    }
                    if (i == 1) {
                        return;
                    }
                    if (ct >= k) ** GOTO lbl206
                    db = 141;
                    ** GOTO lbl258
lbl206:
                    // 1 sources

                    ax = -i;
                    bh = 0;
                    bp = 0;
                    bw = da * at;
                    for (aa = 1; aa < p; ++aa) {
                        cd = ax += ba;
                        ch = bh += ba;
                        ck = bp += at;
                        cn = bw -= at;
                        for (am = 0; am < k; ++am) {
                            for (w = 2; w < i; w += 2) {
                                ap = cz - w;
                                fs[w + ch - 1] = is[w + ck - 1] + is[w + cn - 1];
                                fs[ap + cd - 1] = is[w + ck - 1] - is[w + cn - 1];
                                fs[w + ch] = is[w + ck] + is[w + cn];
                                fs[ap + cd] = is[w + cn] - is[w + ck];
                            }
                            cd += cp;
                            ch += cp;
                            ck += i;
                            cn += i;
                        }
                    }
                    return;
                }
                case 141: {
                    ay = -i;
                    bi = 0;
                    bq = 0;
                    bx = da * at;
                    for (ab = 1; ab < p; ++ab) {
                        ay += ba;
                        bi += ba;
                        bq += at;
                        bx -= at;
                        for (x = 2; x < i; x += 2) {
                            ce = cz + ay - x;
                            ci = x + bi;
                            cl = x + bq;
                            co = x + bx;
                            for (an = 0; an < k; ++an) {
                                fs[ci - 1] = is[cl - 1] + is[co - 1];
                                fs[ce - 1] = is[cl - 1] - is[co - 1];
                                fs[ci] = is[cl] + is[co];
                                fs[ce] = is[co] - is[cl];
                                ce += cp;
                                ci += cp;
                                cl += i;
                                co += i;
                            }
                        }
                    }
                    break block7;
                }
lbl258:
                // 5 sources

                default: {
                    continue block7;
                }
            }
            break;
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    static void drftf1(int i, float[] fs, float[] gs, float[] hs, int[] is) {
        int p = is[1];
        int n = 1;
        int m = i;
        int r = i;
        int k = 0;
        while (true) {
            int t;
            int s;
            int l;
            int q;
            if (k < p) {
                int o = p - k;
                q = is[o + 1];
                l = m / q;
                s = i / m;
                t = s * l;
                r -= (q - 1) * s;
            } else {
                if (n == 1) {
                    return;
                }
                int j = 0;
                while (true) {
                    if (j >= i) {
                        return;
                    }
                    fs[j] = gs[j];
                    ++j;
                }
            }
            n = 1 - n;
            int w = 100;
            block10: while (true) {
                switch (w) {
                    case 100: {
                        if (q != 4) {
                            w = 102;
                            break;
                        }
                        int u = r + s;
                        int v = u + s;
                        if (n != 0) {
                            Drft.dradf4(s, l, gs, fs, hs, r - 1, hs, u - 1, hs, v - 1);
                        } else {
                            Drft.dradf4(s, l, fs, gs, hs, r - 1, hs, u - 1, hs, v - 1);
                        }
                        w = 110;
                        break;
                    }
                    case 102: {
                        if (q != 2) {
                            w = 104;
                            break;
                        }
                        if (n != 0) {
                            w = 103;
                            break;
                        }
                        Drft.dradf2(s, l, fs, gs, hs, r - 1);
                        w = 110;
                        break;
                    }
                    case 103: {
                        Drft.dradf2(s, l, gs, fs, hs, r - 1);
                    }
                    case 104: {
                        if (s == 1) {
                            n = 1 - n;
                        }
                        if (n != 0) {
                            w = 109;
                            break;
                        }
                        Drft.dradfg(s, q, l, t, fs, fs, fs, gs, gs, hs, r - 1);
                        n = 1;
                        w = 110;
                        break;
                    }
                    case 109: {
                        Drft.dradfg(s, q, l, t, gs, gs, gs, fs, fs, hs, r - 1);
                        n = 0;
                    }
                    case 110: {
                        m = l;
                        break block10;
                    }
                }
            }
            ++k;
        }
    }

    /*
     * WARNING - void declaration
     */
    static void dradb2(int i, int j, float[] fs, float[] gs, float[] hs, int k) {
        int m;
        int n = j * i;
        int o = 0;
        int p = 0;
        int q = (i << 1) - 1;
        for (m = 0; m < j; ++m) {
            gs[o] = fs[p] + fs[q + p];
            gs[o + n] = fs[p] - fs[q + p];
            p = (o += i) << 1;
        }
        if (i < 2) {
            return;
        }
        if (i != 2) {
            o = 0;
            p = 0;
            for (m = 0; m < j; ++m) {
                q = o;
                int n2 = p;
                int s = n2 + (i << 1);
                int t = n + o;
                for (int l = 2; l < i; l += 2) {
                    void r;
                    gs[(q += 2) - 1] = fs[(r += 2) - true] + fs[(s -= 2) - 1];
                    float g = fs[r - true] - fs[s - 1];
                    gs[q] = fs[r] - fs[s];
                    float f = fs[r] + fs[s];
                    gs[(t += 2) - 1] = hs[k + l - 2] * g - hs[k + l - 1] * f;
                    gs[t] = hs[k + l - 2] * f + hs[k + l - 1] * g;
                }
                p = (o += i) << 1;
            }
            if (i % 2 == 1) {
                return;
            }
        }
        o = i - 1;
        p = i - 1;
        for (m = 0; m < j; ++m) {
            gs[o] = fs[p] + fs[p];
            gs[o + n] = -(fs[p + 1] + fs[p + 1]);
            o += i;
            p += i << 1;
        }
    }

    static void dradb3(int i, int j, float[] fs, float[] gs, float[] hs, int k, float[] is, int l) {
        int n;
        int o = j * i;
        int p = 0;
        int q = o << 1;
        int r = i << 1;
        int s = i + (i << 1);
        int t = 0;
        for (n = 0; n < j; ++n) {
            float ah = fs[r - 1] + fs[r - 1];
            float ab = fs[t] + taur * ah;
            gs[p] = fs[t] + ah;
            float g = taui * (fs[r] + fs[r]);
            gs[p + o] = ab - g;
            gs[p + q] = ab + g;
            p += i;
            r += s;
            t += s;
        }
        if (i == 1) {
            return;
        }
        p = 0;
        r = i << 1;
        for (n = 0; n < j; ++n) {
            int v = p + (p << 1);
            int u = t = v + r;
            int w = p;
            int n2 = p + o;
            int y = n2 + o;
            for (int m = 2; m < i; m += 2) {
                x += 2;
                y += 2;
                float ai = fs[(t += 2) - 1] + fs[(u -= 2) - 1];
                float ac = fs[(v += 2) - 1] + taur * ai;
                gs[(w += 2) - 1] = fs[v - 1] + ai;
                float ag = fs[t] - fs[u];
                float f = fs[v] + taur * ag;
                gs[w] = fs[v] + ag;
                float ad = taui * (fs[t - 1] - fs[u - 1]);
                float h = taui * (fs[t] + fs[u]);
                float ae = ac - h;
                float af = ac + h;
                float z = f + ad;
                float aa = f - ad;
                gs[x - true] = hs[k + m - 2] * ae - hs[k + m - 1] * z;
                gs[x] = hs[k + m - 2] * z + hs[k + m - 1] * ae;
                gs[y - 1] = is[l + m - 2] * af - is[l + m - 1] * aa;
                gs[y] = is[l + m - 2] * aa + is[l + m - 1] * af;
            }
            p += i;
        }
    }

    /*
     * WARNING - void declaration
     */
    static void dradb4(int i, int j, float[] fs, float[] gs, float[] hs, int k, float[] is, int l, float[] js, int m) {
        int t;
        int o;
        int p = j * i;
        int q = 0;
        int r = i << 2;
        int s = 0;
        int z = i << 1;
        for (o = 0; o < j; ++o) {
            t = s + z;
            int w = q;
            float ar = fs[t - 1] + fs[t - 1];
            float at = fs[t] + fs[t];
            float al = fs[s] - fs[(t += z) - 1];
            float ao = fs[s] + fs[t - 1];
            gs[w] = ao + ar;
            gs[w += p] = al - at;
            gs[w += p] = ao - ar;
            gs[w += p] = al + at;
            q += i;
            s += r;
        }
        if (i < 2) {
            return;
        }
        if (i != 2) {
            q = 0;
            for (o = 0; o < j; ++o) {
                r = q << 2;
                t = s = r + z;
                int x = s + z;
                int aa = q;
                for (int n = 2; n < i; n += 2) {
                    void u;
                    aa += 2;
                    float af = fs[r += 2] + fs[x -= 2];
                    float ah = fs[r] - fs[x];
                    float aj = fs[s += 2] - fs[u -= 2];
                    float au = fs[s] + fs[u];
                    float am = fs[r - 1] - fs[x - 1];
                    float ap = fs[r - 1] + fs[x - 1];
                    float ak = fs[s - 1] - fs[u - true];
                    float as = fs[s - 1] + fs[u - true];
                    gs[aa - 1] = ap + as;
                    float ad = ap - as;
                    gs[aa] = ah + aj;
                    float g = ah - aj;
                    float ac = am - au;
                    float ae = am + au;
                    float f = af + ak;
                    float h = af - ak;
                    int n2 = aa + p;
                    gs[n2 - 1] = hs[k + n - 2] * ac - hs[k + n - 1] * f;
                    gs[ab] = hs[k + n - 2] * f + hs[k + n - 1] * ac;
                    gs[(ab += p) - true] = is[l + n - 2] * ad - is[l + n - 1] * g;
                    gs[ab] = is[l + n - 2] * g + is[l + n - 1] * ad;
                    gs[(ab += p) - true] = js[m + n - 2] * ae - js[m + n - 1] * h;
                    gs[ab] = js[m + n - 2] * h + js[m + n - 1] * ae;
                }
                q += i;
            }
            if (i % 2 == 1) {
                return;
            }
        }
        q = i;
        r = i << 2;
        s = i - 1;
        int v = i + (i << 1);
        for (o = 0; o < j; ++o) {
            int y = s;
            float ag = fs[q] + fs[v];
            float ai = fs[v] - fs[q];
            float an = fs[q - 1] - fs[v - 1];
            float aq = fs[q - 1] + fs[v - 1];
            gs[y] = aq + aq;
            gs[y += p] = sqrt2 * (an - ag);
            gs[y += p] = ai + ai;
            gs[y += p] = -sqrt2 * (an + ag);
            s += i;
            q += r;
            v += r;
        }
    }

    /*
     * Unable to fully structure code
     */
    static void dradbg(int i, int j, int k, int l, float[] fs, float[] gs, float[] hs, float[] is, float[] js, float[] ks, int m) {
        var12_11 = false;
        var19_12 = false;
        dc = 0;
        dl = 0;
        var39_15 = 0.0f;
        dp = 0.0f;
        ds = 0;
        dt = 100;
        block10: while (true) {
            switch (dt) {
                case 100: {
                    dc = j * i;
                    ay = k * i;
                    dn = Drft.tpi / (float)j;
                    dm = (float)Math.cos(dn);
                    dp = (float)Math.sin(dn);
                    dl = i - 1 >>> 1;
                    ds = j;
                    p = j + 1 >>> 1;
                    if (i < k) {
                        dt = 103;
                    } else {
                        az = 0;
                        bi = 0;
                        for (ag = 0; ag < k; ++ag) {
                            br = az;
                            ca = bi;
                            for (q = 0; q < i; ++q) {
                                is[br] = fs[ca];
                                ++br;
                                ++ca;
                            }
                            az += i;
                            bi += dc;
                        }
                        dt = 106;
                    }
                    ** GOTO lbl283
                }
                case 103: {
                    ba = 0;
                    for (r = 0; r < i; ++r) {
                        bj = ba;
                        bs = ba;
                        for (ah = 0; ah < k; ++ah) {
                            is[bj] = fs[bs];
                            bj += i;
                            bs += dc;
                        }
                        ++ba;
                    }
                }
                case 106: {
                    bb = 0;
                    bk = ds * ay;
                    ct = var24_28 = i << 1;
                    for (y = 1; y < p; ++y) {
                        bt = bb += ay;
                        cb = bk -= ay;
                        cn = ci;
                        for (ai = 0; ai < k; ++ai) {
                            is[bt] = fs[cn - true] + fs[cn - true];
                            is[cb] = fs[cn] + fs[cn];
                            bt += i;
                            cb += i;
                            cn += dc;
                        }
                        ci += ct;
                    }
                    if (i == 1) {
                        dt = 116;
                    } else if (dl < k) {
                        dt = 112;
                    } else {
                        bb = 0;
                        bk = ds * ay;
                        ct = 0;
                        for (y = 1; y < p; ++y) {
                            bu = bb += ay;
                            cc = bk -= ay;
                            cw = ct += i << 1;
                            for (aj = 0; aj < k; ++aj) {
                                ci = bu;
                                co = cc;
                                cz = cw;
                                dd = cw;
                                for (s = 2; s < i; s += 2) {
                                    is[(ci += 2) - 1] = fs[(cz += 2) - 1] + fs[(dd -= 2) - 1];
                                    is[(co += 2) - 1] = fs[cz - 1] - fs[dd - 1];
                                    is[ci] = fs[cz] - fs[dd];
                                    is[co] = fs[cz] + fs[dd];
                                }
                                bu += i;
                                cc += i;
                                cw += dc;
                            }
                        }
                        dt = 116;
                    }
                    ** GOTO lbl283
                }
                case 112: {
                    bc = 0;
                    bl = ds * ay;
                    cu = 0;
                    for (z = 1; z < p; ++z) {
                        bv = bc += ay;
                        cd = bl -= ay;
                        cx = cu += i << 1;
                        da = cu;
                        for (t = 2; t < i; t += 2) {
                            cj = bv += 2;
                            cp = cd += 2;
                            de = cx += 2;
                            dg = da -= 2;
                            for (ak = 0; ak < k; ++ak) {
                                is[cj - 1] = fs[de - 1] + fs[dg - 1];
                                is[cp - 1] = fs[de - 1] - fs[dg - 1];
                                is[cj] = fs[de] - fs[dg];
                                is[cp] = fs[de] + fs[dg];
                                cj += i;
                                cp += i;
                                de += dc;
                                dg += dc;
                            }
                        }
                    }
                }
                case 116: {
                    di = 1.0f;
                    g = 0.0f;
                    bd = 0;
                    db = bl = ds * l;
                    bw = (j - 1) * l;
                    for (ar = 1; ar < p; ++ar) {
                        dq = dm * di - dp * g;
                        g = dm * g + dp * di;
                        di = dq;
                        ce = bd += l;
                        ck = bm -= l;
                        cq = 0;
                        cv = l;
                        cy = bw;
                        for (as = 0; as < l; ++as) {
                            hs[ce++] = js[cq++] + di * js[cv++];
                            hs[ck++] = g * js[cy++];
                        }
                        f = di;
                        dk = g;
                        dj = di;
                        h = g;
                        cq = l;
                        cv = db - l;
                        for (aa = 2; aa < p; ++aa) {
                            cq += l;
                            cv -= l;
                            dr = f * dj - dk * h;
                            h = f * h + dk * dj;
                            dj = dr;
                            ce = bd;
                            ck = bm;
                            df = cq;
                            dh = cv;
                            for (as = 0; as < l; ++as) {
                                v0 = ce++;
                                hs[v0] = hs[v0] + dj * js[df++];
                                v1 = ck++;
                                hs[v1] = hs[v1] + h * js[dh++];
                            }
                        }
                    }
                    bd = 0;
                    for (ab = 1; ab < p; ++ab) {
                        bm = bd += l;
                        at = 0;
                        while (at < l) {
                            v2 = at++;
                            js[v2] = js[v2] + js[bm++];
                        }
                    }
                    bd = 0;
                    bm = ds * ay;
                    for (ab = 1; ab < p; ++ab) {
                        bw = bd += ay;
                        cf = bm -= ay;
                        for (al = 0; al < k; ++al) {
                            is[bw] = gs[bw] - gs[cf];
                            is[cf] = gs[bw] + gs[cf];
                            bw += i;
                            cf += i;
                        }
                    }
                    if (i == 1) {
                        dt = 132;
                    } else if (dl < k) {
                        dt = 128;
                    } else {
                        bd = 0;
                        bm = ds * ay;
                        for (ab = 1; ab < p; ++ab) {
                            bw = bd += ay;
                            cg = bm -= ay;
                            for (am = 0; am < k; ++am) {
                                cl = bw;
                                cr = cg;
                                for (u = 2; u < i; u += 2) {
                                    is[(cl += 2) - 1] = gs[cl - 1] - gs[cr += 2];
                                    is[cr - 1] = gs[cl - 1] + gs[cr];
                                    is[cl] = gs[cl] + gs[cr - 1];
                                    is[cr] = gs[cl] - gs[cr - 1];
                                }
                                bw += i;
                                cg += i;
                            }
                        }
                        dt = 132;
                    }
                    ** GOTO lbl283
                }
                case 128: {
                    be = 0;
                    bn = ds * ay;
                    for (ac = 1; ac < p; ++ac) {
                        bx = be += ay;
                        ch = bn -= ay;
                        for (v = 2; v < i; v += 2) {
                            cm = bx += 2;
                            cs = ch += 2;
                            for (an = 0; an < k; ++an) {
                                is[cm - 1] = gs[cm - 1] - gs[cs];
                                is[cs - 1] = gs[cm - 1] + gs[cs];
                                is[cm] = gs[cm] + gs[cs - 1];
                                is[cs] = gs[cm] - gs[cs - 1];
                                cm += i;
                                cs += i;
                            }
                        }
                    }
                }
                case 132: {
                    if (i == 1) {
                        return;
                    }
                    for (au = 0; au < l; ++au) {
                        hs[au] = js[au];
                    }
                    bf = 0;
                    for (ad = 1; ad < j; ++ad) {
                        bo = bf += ay;
                        for (ao = 0; ao < k; ++ao) {
                            gs[bo] = is[bo];
                            bo += i;
                        }
                    }
                    if (dl <= k) ** GOTO lbl250
                    dt = 139;
                    ** GOTO lbl283
lbl250:
                    // 1 sources

                    av = -i - 1;
                    bf = 0;
                    for (ad = 1; ad < j; ++ad) {
                        n = av += i;
                        bp = bf += ay;
                        for (w = 2; w < i; w += 2) {
                            n += 2;
                            by = bp += 2;
                            for (ap = 0; ap < k; ++ap) {
                                gs[by - 1] = ks[m + n - 1] * is[by - 1] - ks[m + n] * is[by];
                                gs[by] = ks[m + n - 1] * is[by] + ks[m + n] * is[by - 1];
                                by += i;
                            }
                        }
                    }
                    return;
                }
                case 139: {
                    aw = -i - 1;
                    bg = 0;
                    for (ae = 1; ae < j; ++ae) {
                        aw += i;
                        bq = bg += ay;
                        for (aq = 0; aq < k; ++aq) {
                            o = aw;
                            bz = bq;
                            for (x = 2; x < i; x += 2) {
                                gs[(bz += 2) - 1] = ks[m + (o += 2) - 1] * is[bz - 1] - ks[m + o] * is[bz];
                                gs[bz] = ks[m + o - 1] * is[bz] + ks[m + o] * is[bz - 1];
                            }
                            bq += i;
                        }
                    }
                    break block10;
                }
lbl283:
                // 10 sources

                default: {
                    continue block10;
                }
            }
            break;
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    static void drftb1(int i, float[] fs, float[] gs, float[] hs, int j, int[] is) {
        int n = 0;
        boolean bl = false;
        boolean bl2 = false;
        int w = 0;
        int p = is[1];
        int o = 0;
        int m = 1;
        int r = 1;
        int l = 0;
        while (true) {
            if (l >= p) {
                if (o == 0) {
                    return;
                }
                int k = 0;
                while (true) {
                    if (k >= i) {
                        return;
                    }
                    fs[k] = gs[k];
                    ++k;
                }
            }
            int x = 100;
            block9: while (true) {
                switch (x) {
                    case 100: {
                        int q = is[l + 2];
                        n = q * m;
                        int v = i / n;
                        w = v * m;
                        if (q != 4) {
                            x = 103;
                            break;
                        }
                        int s = r + v;
                        int u = s + v;
                        if (o != 0) {
                            Drft.dradb4(v, m, gs, fs, hs, j + r - 1, hs, j + s - 1, hs, j + u - 1);
                        } else {
                            Drft.dradb4(v, m, fs, gs, hs, j + r - 1, hs, j + s - 1, hs, j + u - 1);
                        }
                        o = 1 - o;
                        x = 115;
                        break;
                    }
                    case 103: {
                        int v;
                        int q;
                        if (q != 2) {
                            x = 106;
                            break;
                        }
                        if (o != 0) {
                            Drft.dradb2(v, m, gs, fs, hs, j + r - 1);
                        } else {
                            Drft.dradb2(v, m, fs, gs, hs, j + r - 1);
                        }
                        o = 1 - o;
                        x = 115;
                        break;
                    }
                    case 106: {
                        int v;
                        int q;
                        if (q != 3) {
                            x = 109;
                            break;
                        }
                        int t = r + v;
                        if (o != 0) {
                            Drft.dradb3(v, m, gs, fs, hs, j + r - 1, hs, j + t - 1);
                        } else {
                            Drft.dradb3(v, m, fs, gs, hs, j + r - 1, hs, j + t - 1);
                        }
                        o = 1 - o;
                        x = 115;
                        break;
                    }
                    case 109: {
                        int v;
                        int q;
                        if (o != 0) {
                            Drft.dradbg(v, q, m, w, gs, gs, gs, fs, fs, hs, j + r - 1);
                        } else {
                            Drft.dradbg(v, q, m, w, fs, fs, fs, gs, gs, hs, j + r - 1);
                        }
                        if (v == 1) {
                            o = 1 - o;
                        }
                    }
                    case 115: {
                        int v;
                        int q;
                        m = n;
                        r += (q - true) * v;
                        break block9;
                    }
                }
            }
            ++l;
        }
    }
}

