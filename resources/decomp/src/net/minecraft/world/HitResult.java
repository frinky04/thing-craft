/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class HitResult {
    public int type;
    public int x;
    public int y;
    public int z;
    public int face;
    public Vec3d facePos;
    public Entity entity;

    public HitResult(int x, int y, int z, int face, Vec3d facePos) {
        this.type = 0;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.facePos = Vec3d.fromPool(facePos.x, facePos.y, facePos.z);
    }

    public HitResult(Entity entity) {
        this.type = 1;
        this.entity = entity;
        this.facePos = Vec3d.fromPool(entity.x, entity.y, entity.z);
    }
}

