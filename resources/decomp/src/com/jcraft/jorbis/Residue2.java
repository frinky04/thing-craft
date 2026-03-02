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
class Residue2
extends Residue0 {
    Residue2() {
    }

    int inverse(Block block, Object object, float[][] fs, int[] is, int i) {
        int j = 0;
        for (j = 0; j < i && is[j] == 0; ++j) {
        }
        if (j == i) {
            return 0;
        }
        return Residue2._2inverse(block, object, fs, i);
    }
}

