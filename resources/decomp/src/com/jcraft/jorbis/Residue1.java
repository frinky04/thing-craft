/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.jcraft.jorbis;

import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Residue0;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
class Residue1
extends Residue0 {
    Residue1() {
    }

    int inverse(Block block, Object object, float[][] fs, int[] is, int i) {
        int j = 0;
        for (int k = 0; k < i; ++k) {
            if (is[k] == 0) continue;
            fs[j++] = fs[k];
        }
        if (j != 0) {
            return Residue1._01inverse(block, object, fs, j, 1);
        }
        return 0;
    }
}

