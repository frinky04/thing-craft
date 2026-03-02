/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;

public interface WorldEventListener {
    public void notifyBlockChanged(int var1, int var2, int var3);

    public void notifyRegionChanged(int var1, int var2, int var3, int var4, int var5, int var6);

    public void playSound(String var1, double var2, double var4, double var6, float var8, float var9);

    public void addParticle(String var1, double var2, double var4, double var6, double var8, double var10, double var12);

    public void notifyEntityAdded(Entity var1);

    public void notifyEntityRemoved(Entity var1);

    public void notifyAmbientDarknessChanged();

    public void playRecordMusic(String var1, int var2, int var3, int var4);

    public void notifyBlockEntityChanged(int var1, int var2, int var3, BlockEntity var4);
}

