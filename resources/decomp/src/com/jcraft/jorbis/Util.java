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
class Util {
    Util() {
    }

    static int ilog(int i) {
        int j = 0;
        while (i != 0) {
            ++j;
            i >>>= 1;
        }
        return j;
    }

    static int ilog2(int i) {
        int j = 0;
        while (i > 1) {
            ++j;
            i >>>= 1;
        }
        return j;
    }

    static int icount(int i) {
        int j = 0;
        while (i != 0) {
            j += i & 1;
            i >>>= 1;
        }
        return j;
    }
}

