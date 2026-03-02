/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.ai.pathing;

import net.minecraft.util.math.MathHelper;

public class PathNode {
    public final int x;
    public final int y;
    public final int z;
    public final int hash;
    int heapIndex = -1;
    float distanceFromStart;
    float distanceToTarget;
    float weight;
    PathNode prev;
    public boolean visited = false;

    public PathNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hash = x | y << 10 | z << 20;
    }

    public float distanceTo(PathNode other) {
        float f = other.x - this.x;
        float g = other.y - this.y;
        float h = other.z - this.z;
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public boolean equals(Object object) {
        return ((PathNode)object).hash == this.hash;
    }

    public int hashCode() {
        return this.hash;
    }

    public boolean isInHeap() {
        return this.heapIndex >= 0;
    }

    public String toString() {
        return this.x + ", " + this.y + ", " + this.z;
    }
}

