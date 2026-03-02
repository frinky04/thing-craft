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
import net.minecraft.entity.Entity;

public class ChunkPos {
    public int x;
    public int z;

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int hashCode() {
        return this.x << 8 | this.z;
    }

    public boolean equals(Object object) {
        ChunkPos chunkPos = (ChunkPos)object;
        return chunkPos.x == this.x && chunkPos.z == this.z;
    }

    @Environment(value=EnvType.SERVER)
    public double squaredDistanceToCenter(Entity entity) {
        double d = this.x * 16 + 8;
        double e = this.z * 16 + 8;
        double f = d - entity.x;
        double g = e - entity.z;
        return f * f + g * g;
    }
}

