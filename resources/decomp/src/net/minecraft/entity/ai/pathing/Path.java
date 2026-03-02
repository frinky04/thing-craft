/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.util.math.Vec3d;

public class Path {
    private final PathNode[] nodes;
    public final int length;
    private int currentIndex;

    public Path(PathNode[] nodes) {
        this.nodes = nodes;
        this.length = nodes.length;
    }

    public void advance() {
        ++this.currentIndex;
    }

    public boolean isDone() {
        return this.currentIndex >= this.nodes.length;
    }

    public Vec3d getPos(Entity target) {
        double d = (double)this.nodes[this.currentIndex].x + (double)((int)(target.width + 1.0f)) * 0.5;
        double e = this.nodes[this.currentIndex].y;
        double f = (double)this.nodes[this.currentIndex].z + (double)((int)(target.width + 1.0f)) * 0.5;
        return Vec3d.fromPool(d, e, f);
    }
}

