/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class MathHelper {
    private static float[] SINE_TABLE = new float[65536];

    public static final float sin(float x) {
        return SINE_TABLE[(int)(x * 10430.378f) & 0xFFFF];
    }

    public static final float cos(float x) {
        return SINE_TABLE[(int)(x * 10430.378f + 16384.0f) & 0xFFFF];
    }

    public static final float sqrt(float x) {
        return (float)Math.sqrt(x);
    }

    public static final float sqrt(double x) {
        return (float)Math.sqrt(x);
    }

    public static int floor(float x) {
        int i = (int)x;
        return x < (float)i ? i - 1 : i;
    }

    public static int floor(double x) {
        int i = (int)x;
        return x < (double)i ? i - 1 : i;
    }

    public static float abs(float x) {
        return x >= 0.0f ? x : -x;
    }

    public static double absMax(double a, double b) {
        if (a < 0.0) {
            a = -a;
        }
        if (b < 0.0) {
            b = -b;
        }
        return a > b ? a : b;
    }

    @Environment(value=EnvType.CLIENT)
    public static int floorDiv(int a, int b) {
        if (a < 0) {
            return -((-a - 1) / b) - 1;
        }
        return a / b;
    }

    static {
        for (int i = 0; i < 65536; ++i) {
            MathHelper.SINE_TABLE[i] = (float)Math.sin((double)i * Math.PI * 2.0 / 65536.0);
        }
    }
}

