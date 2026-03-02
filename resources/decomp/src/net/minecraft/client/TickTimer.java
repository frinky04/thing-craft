/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class TickTimer {
    public float tps;
    private double timeSec;
    public int ticksThisFrame;
    public float partialTick;
    public float tpsScale = 1.0f;
    public float tickDelta = 0.0f;
    private long lastTickTime;
    private long cumTickTime;
    private double tickTimeCorrection = 1.0;

    public TickTimer(float tps) {
        this.tps = tps;
        this.lastTickTime = System.currentTimeMillis();
        this.cumTickTime = System.nanoTime() / 1000000L;
    }

    public void advance() {
        long l = System.currentTimeMillis();
        long m = l - this.lastTickTime;
        long n = System.nanoTime() / 1000000L;
        if (m > 1000L) {
            long o = n - this.cumTickTime;
            double e = (double)m / (double)o;
            this.tickTimeCorrection += (e - this.tickTimeCorrection) * (double)0.2f;
            this.lastTickTime = l;
            this.cumTickTime = n;
        }
        if (m < 0L) {
            this.lastTickTime = l;
            this.cumTickTime = n;
        }
        double d = (double)n / 1000.0;
        double f = (d - this.timeSec) * this.tickTimeCorrection;
        this.timeSec = d;
        if (f < 0.0) {
            f = 0.0;
        }
        if (f > 1.0) {
            f = 1.0;
        }
        this.tickDelta = (float)((double)this.tickDelta + f * (double)this.tpsScale * (double)this.tps);
        this.ticksThisFrame = (int)this.tickDelta;
        this.tickDelta -= (float)this.ticksThisFrame;
        if (this.ticksThisFrame > 10) {
            this.ticksThisFrame = 10;
        }
        this.partialTick = this.tickDelta;
    }
}

