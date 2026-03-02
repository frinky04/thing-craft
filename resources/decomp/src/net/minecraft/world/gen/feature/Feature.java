/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.world.World;

public abstract class Feature {
    public abstract boolean place(World var1, Random var2, int var3, int var4, int var5);

    public void prepare(double d0, double d1, double d2) {
    }
}

