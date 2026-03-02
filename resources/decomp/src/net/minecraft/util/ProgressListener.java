/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.util;

public interface ProgressListener {
    public void progressStartNoAbort(String var1);

    public void progressStage(String var1);

    public void progressStagePercentage(int var1);
}

