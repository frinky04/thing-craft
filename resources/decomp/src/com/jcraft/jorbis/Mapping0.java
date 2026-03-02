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
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.FuncFloor;
import com.jcraft.jorbis.FuncMapping;
import com.jcraft.jorbis.FuncResidue;
import com.jcraft.jorbis.FuncTime;
import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.InfoMode;
import com.jcraft.jorbis.Mdct;
import com.jcraft.jorbis.PsyLook;
import com.jcraft.jorbis.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Mapping0
extends FuncMapping {
    static int seq = 0;
    float[][] pcmbundle = null;
    int[] zerobundle = null;
    int[] nonzero = null;
    Object[] floormemo = null;

    Mapping0() {
    }

    void free_info(Object object) {
    }

    void free_look(Object object) {
    }

    Object look(DspState dspState, InfoMode infoMode, Object object) {
        Info info = dspState.vi;
        LookMapping0 lookMapping0 = new LookMapping0();
        InfoMapping0 infoMapping0 = lookMapping0.map = (InfoMapping0)object;
        lookMapping0.mode = infoMode;
        lookMapping0.time_look = new Object[infoMapping0.submaps];
        lookMapping0.floor_look = new Object[infoMapping0.submaps];
        lookMapping0.residue_look = new Object[infoMapping0.submaps];
        lookMapping0.time_func = new FuncTime[infoMapping0.submaps];
        lookMapping0.floor_func = new FuncFloor[infoMapping0.submaps];
        lookMapping0.residue_func = new FuncResidue[infoMapping0.submaps];
        for (int i = 0; i < infoMapping0.submaps; ++i) {
            int j = infoMapping0.timesubmap[i];
            int k = infoMapping0.floorsubmap[i];
            int l = infoMapping0.residuesubmap[i];
            lookMapping0.time_func[i] = FuncTime.time_P[info.time_type[j]];
            lookMapping0.time_look[i] = lookMapping0.time_func[i].look(dspState, infoMode, info.time_param[j]);
            lookMapping0.floor_func[i] = FuncFloor.floor_P[info.floor_type[k]];
            lookMapping0.floor_look[i] = lookMapping0.floor_func[i].look(dspState, infoMode, info.floor_param[k]);
            lookMapping0.residue_func[i] = FuncResidue.residue_P[info.residue_type[l]];
            lookMapping0.residue_look[i] = lookMapping0.residue_func[i].look(dspState, infoMode, info.residue_param[l]);
        }
        if (info.psys == 0 || dspState.analysisp != 0) {
            // empty if block
        }
        lookMapping0.ch = info.channels;
        return lookMapping0;
    }

    void pack(Info info, Object object, Buffer buffer) {
        InfoMapping0 infoMapping0 = (InfoMapping0)object;
        if (infoMapping0.submaps > 1) {
            buffer.write(1, 1);
            buffer.write(infoMapping0.submaps - 1, 4);
        } else {
            buffer.write(0, 1);
        }
        if (infoMapping0.coupling_steps > 0) {
            buffer.write(1, 1);
            buffer.write(infoMapping0.coupling_steps - 1, 8);
            for (int i = 0; i < infoMapping0.coupling_steps; ++i) {
                buffer.write(infoMapping0.coupling_mag[i], Util.ilog2(info.channels));
                buffer.write(infoMapping0.coupling_ang[i], Util.ilog2(info.channels));
            }
        } else {
            buffer.write(0, 1);
        }
        buffer.write(0, 2);
        if (infoMapping0.submaps > 1) {
            for (int j = 0; j < info.channels; ++j) {
                buffer.write(infoMapping0.chmuxlist[j], 4);
            }
        }
        for (int k = 0; k < infoMapping0.submaps; ++k) {
            buffer.write(infoMapping0.timesubmap[k], 8);
            buffer.write(infoMapping0.floorsubmap[k], 8);
            buffer.write(infoMapping0.residuesubmap[k], 8);
        }
    }

    Object unpack(Info info, Buffer buffer) {
        InfoMapping0 infoMapping0 = new InfoMapping0();
        infoMapping0.submaps = buffer.read(1) != 0 ? buffer.read(4) + 1 : 1;
        if (buffer.read(1) != 0) {
            infoMapping0.coupling_steps = buffer.read(8) + 1;
            for (int i = 0; i < infoMapping0.coupling_steps; ++i) {
                int l = infoMapping0.coupling_mag[i] = buffer.read(Util.ilog2(info.channels));
                int m = infoMapping0.coupling_ang[i] = buffer.read(Util.ilog2(info.channels));
                if (l >= 0 && m >= 0 && l != m && l < info.channels && m < info.channels) continue;
                infoMapping0.free();
                return null;
            }
        }
        if (buffer.read(2) > 0) {
            infoMapping0.free();
            return null;
        }
        if (infoMapping0.submaps > 1) {
            for (int j = 0; j < info.channels; ++j) {
                infoMapping0.chmuxlist[j] = buffer.read(4);
                if (infoMapping0.chmuxlist[j] < infoMapping0.submaps) continue;
                infoMapping0.free();
                return null;
            }
        }
        for (int k = 0; k < infoMapping0.submaps; ++k) {
            infoMapping0.timesubmap[k] = buffer.read(8);
            if (infoMapping0.timesubmap[k] >= info.times) {
                infoMapping0.free();
                return null;
            }
            infoMapping0.floorsubmap[k] = buffer.read(8);
            if (infoMapping0.floorsubmap[k] >= info.floors) {
                infoMapping0.free();
                return null;
            }
            infoMapping0.residuesubmap[k] = buffer.read(8);
            if (infoMapping0.residuesubmap[k] < info.residues) continue;
            infoMapping0.free();
            return null;
        }
        return infoMapping0;
    }

    synchronized int inverse(Block block, Object object) {
        int j;
        DspState dspState = block.vd;
        Info info = dspState.vi;
        LookMapping0 lookMapping0 = (LookMapping0)object;
        InfoMapping0 infoMapping0 = lookMapping0.map;
        InfoMode infoMode = lookMapping0.mode;
        int i = block.pcmend = info.blocksizes[block.W];
        float[] fs = dspState.window[block.W][block.lW][block.nW][infoMode.windowtype];
        if (this.pcmbundle == null || this.pcmbundle.length < info.channels) {
            this.pcmbundle = new float[info.channels][];
            this.nonzero = new int[info.channels];
            this.zerobundle = new int[info.channels];
            this.floormemo = new Object[info.channels];
        }
        for (j = 0; j < info.channels; ++j) {
            float[] gs = block.pcm[j];
            int l = infoMapping0.chmuxlist[j];
            this.floormemo[j] = lookMapping0.floor_func[l].inverse1(block, lookMapping0.floor_look[l], this.floormemo[j]);
            this.nonzero[j] = this.floormemo[j] != null ? 1 : 0;
            for (int q = 0; q < i / 2; ++q) {
                gs[q] = 0.0f;
            }
        }
        for (j = 0; j < infoMapping0.coupling_steps; ++j) {
            if (this.nonzero[infoMapping0.coupling_mag[j]] == 0 && this.nonzero[infoMapping0.coupling_ang[j]] == 0) continue;
            this.nonzero[infoMapping0.coupling_mag[j]] = 1;
            this.nonzero[infoMapping0.coupling_ang[j]] = 1;
        }
        for (j = 0; j < infoMapping0.submaps; ++j) {
            int k = 0;
            for (int m = 0; m < info.channels; ++m) {
                if (infoMapping0.chmuxlist[m] != j) continue;
                this.zerobundle[k] = this.nonzero[m] != 0 ? 1 : 0;
                this.pcmbundle[k++] = block.pcm[m];
            }
            lookMapping0.residue_func[j].inverse(block, lookMapping0.residue_look[j], this.pcmbundle, this.zerobundle, k);
        }
        for (j = infoMapping0.coupling_steps - 1; j >= 0; --j) {
            float[] hs = block.pcm[infoMapping0.coupling_mag[j]];
            float[] ls = block.pcm[infoMapping0.coupling_ang[j]];
            for (int r = 0; r < i / 2; ++r) {
                float f = hs[r];
                float g = ls[r];
                if (f > 0.0f) {
                    if (g > 0.0f) {
                        hs[r] = f;
                        ls[r] = f - g;
                        continue;
                    }
                    ls[r] = f;
                    hs[r] = f + g;
                    continue;
                }
                if (g > 0.0f) {
                    hs[r] = f;
                    ls[r] = f + g;
                    continue;
                }
                ls[r] = f;
                hs[r] = f - g;
            }
        }
        for (j = 0; j < info.channels; ++j) {
            float[] is = block.pcm[j];
            int n = infoMapping0.chmuxlist[j];
            lookMapping0.floor_func[n].inverse2(block, lookMapping0.floor_look[n], this.floormemo[j], is);
        }
        for (j = 0; j < info.channels; ++j) {
            float[] js = block.pcm[j];
            ((Mdct)dspState.transform[block.W][0]).backward(js, js);
        }
        for (j = 0; j < info.channels; ++j) {
            float[] ks = block.pcm[j];
            if (this.nonzero[j] != 0) {
                for (int o = 0; o < i; ++o) {
                    int n = o;
                    ks[n] = ks[n] * fs[o];
                }
                continue;
            }
            for (int p = 0; p < i; ++p) {
                ks[p] = 0.0f;
            }
        }
        return 0;
    }

    @Environment(value=EnvType.CLIENT)
    class InfoMapping0 {
        int submaps;
        int[] chmuxlist = new int[256];
        int[] timesubmap = new int[16];
        int[] floorsubmap = new int[16];
        int[] residuesubmap = new int[16];
        int[] psysubmap = new int[16];
        int coupling_steps;
        int[] coupling_mag = new int[256];
        int[] coupling_ang = new int[256];

        InfoMapping0() {
        }

        void free() {
            this.chmuxlist = null;
            this.timesubmap = null;
            this.floorsubmap = null;
            this.residuesubmap = null;
            this.psysubmap = null;
            this.coupling_mag = null;
            this.coupling_ang = null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    class LookMapping0 {
        InfoMode mode;
        InfoMapping0 map;
        Object[] time_look;
        Object[] floor_look;
        Object[] floor_state;
        Object[] residue_look;
        PsyLook[] psy_look;
        FuncTime[] time_func;
        FuncFloor[] floor_func;
        FuncResidue[] residue_func;
        int ch;
        float[][] decay;
        int lastframe;

        LookMapping0() {
        }
    }
}

