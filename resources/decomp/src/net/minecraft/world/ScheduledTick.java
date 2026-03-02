/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world;

public class ScheduledTick
implements Comparable {
    private static long idCounter = 0L;
    public int x;
    public int y;
    public int z;
    public int block;
    public long time;
    private long id = idCounter++;

    public ScheduledTick(int x, int y, int z, int block) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }

    public boolean equals(Object object) {
        if (object instanceof ScheduledTick) {
            ScheduledTick scheduledTick = (ScheduledTick)object;
            return this.x == scheduledTick.x && this.y == scheduledTick.y && this.z == scheduledTick.z && this.block == scheduledTick.block;
        }
        return false;
    }

    public int hashCode() {
        return (this.x * 128 * 1024 + this.z * 128 + this.y) * 256 + this.block;
    }

    public ScheduledTick setTime(long time) {
        this.time = time;
        return this;
    }

    public int compareTo(ScheduledTick scheduledTick) {
        if (this.time < scheduledTick.time) {
            return -1;
        }
        if (this.time > scheduledTick.time) {
            return 1;
        }
        if (this.id < scheduledTick.id) {
            return -1;
        }
        if (this.id > scheduledTick.id) {
            return 1;
        }
        return 0;
    }
}

