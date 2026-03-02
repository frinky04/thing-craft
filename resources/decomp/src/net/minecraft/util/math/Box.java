/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.util.math;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;

public class Box {
    private static List POOL = new ArrayList();
    private static int poolIndex = 0;
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    public static Box of(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void resetPool() {
        poolIndex = 0;
    }

    public static Box fromPool(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (poolIndex >= POOL.size()) {
            POOL.add(Box.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        }
        return ((Box)POOL.get(poolIndex++)).set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public Box set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    public Box expanded(double dx, double dy, double dz) {
        double d = this.minX;
        double e = this.minY;
        double f = this.minZ;
        double g = this.maxX;
        double h = this.maxY;
        double i = this.maxZ;
        if (dx < 0.0) {
            d += dx;
        }
        if (dx > 0.0) {
            g += dx;
        }
        if (dy < 0.0) {
            e += dy;
        }
        if (dy > 0.0) {
            h += dy;
        }
        if (dz < 0.0) {
            f += dz;
        }
        if (dz > 0.0) {
            i += dz;
        }
        return Box.fromPool(d, e, f, g, h, i);
    }

    public Box grown(double dx, double dy, double dz) {
        double d = this.minX - dx;
        double e = this.minY - dy;
        double f = this.minZ - dz;
        double g = this.maxX + dx;
        double h = this.maxY + dy;
        double i = this.maxZ + dz;
        return Box.fromPool(d, e, f, g, h, i);
    }

    public Box moved(double dx, double dy, double dz) {
        return Box.fromPool(this.minX + dx, this.minY + dy, this.minZ + dz, this.maxX + dx, this.maxY + dy, this.maxZ + dz);
    }

    public double intersectX(Box box, double limit) {
        double e;
        double d;
        if (box.maxY <= this.minY || box.minY >= this.maxY) {
            return limit;
        }
        if (box.maxZ <= this.minZ || box.minZ >= this.maxZ) {
            return limit;
        }
        if (limit > 0.0 && box.maxX <= this.minX && (d = this.minX - box.maxX) < limit) {
            limit = d;
        }
        if (limit < 0.0 && box.minX >= this.maxX && (e = this.maxX - box.minX) > limit) {
            limit = e;
        }
        return limit;
    }

    public double intersectY(Box box, double limit) {
        double e;
        double d;
        if (box.maxX <= this.minX || box.minX >= this.maxX) {
            return limit;
        }
        if (box.maxZ <= this.minZ || box.minZ >= this.maxZ) {
            return limit;
        }
        if (limit > 0.0 && box.maxY <= this.minY && (d = this.minY - box.maxY) < limit) {
            limit = d;
        }
        if (limit < 0.0 && box.minY >= this.maxY && (e = this.maxY - box.minY) > limit) {
            limit = e;
        }
        return limit;
    }

    public double intersectZ(Box box, double limit) {
        double e;
        double d;
        if (box.maxX <= this.minX || box.minX >= this.maxX) {
            return limit;
        }
        if (box.maxY <= this.minY || box.minY >= this.maxY) {
            return limit;
        }
        if (limit > 0.0 && box.maxZ <= this.minZ && (d = this.minZ - box.maxZ) < limit) {
            limit = d;
        }
        if (limit < 0.0 && box.minZ >= this.maxZ && (e = this.maxZ - box.minZ) > limit) {
            limit = e;
        }
        return limit;
    }

    public boolean intersects(Box box) {
        if (box.maxX <= this.minX || box.minX >= this.maxX) {
            return false;
        }
        if (box.maxY <= this.minY || box.minY >= this.maxY) {
            return false;
        }
        return !(box.maxZ <= this.minZ) && !(box.minZ >= this.maxZ);
    }

    public Box move(double dx, double dy, double dz) {
        this.minX += dx;
        this.minY += dy;
        this.minZ += dz;
        this.maxX += dx;
        this.maxY += dy;
        this.maxZ += dz;
        return this;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean contains(Vec3d vec) {
        if (vec.x <= this.minX || vec.x >= this.maxX) {
            return false;
        }
        if (vec.y <= this.minY || vec.y >= this.maxY) {
            return false;
        }
        return !(vec.z <= this.minZ) && !(vec.z >= this.maxZ);
    }

    @Environment(value=EnvType.CLIENT)
    public double getAverageSideLength() {
        double d = this.maxX - this.minX;
        double e = this.maxY - this.minY;
        double f = this.maxZ - this.minZ;
        return (d + e + f) / 3.0;
    }

    @Environment(value=EnvType.SERVER)
    public Box contract(double dx, double dy, double dz) {
        double d = this.minX;
        double e = this.minY;
        double f = this.minZ;
        double g = this.maxX;
        double h = this.maxY;
        double i = this.maxZ;
        if (dx < 0.0) {
            d -= dx;
        }
        if (dx > 0.0) {
            g -= dx;
        }
        if (dy < 0.0) {
            e -= dy;
        }
        if (dy > 0.0) {
            h -= dy;
        }
        if (dz < 0.0) {
            f -= dz;
        }
        if (dz > 0.0) {
            i -= dz;
        }
        return Box.fromPool(d, e, f, g, h, i);
    }

    public Box copy() {
        return Box.fromPool(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public HitResult clip(Vec3d from, Vec3d to) {
        Vec3d vec3d7;
        Vec3d vec3d = from.intermediateWithX(to, this.minX);
        Vec3d vec3d2 = from.intermediateWithX(to, this.maxX);
        Vec3d vec3d3 = from.intermediateWithY(to, this.minY);
        Vec3d vec3d4 = from.intermediateWithY(to, this.maxY);
        Vec3d vec3d5 = from.intermediateWithZ(to, this.minZ);
        Vec3d vec3d6 = from.intermediateWithZ(to, this.maxZ);
        if (!this.containsYZ(vec3d)) {
            vec3d = null;
        }
        if (!this.containsYZ(vec3d2)) {
            vec3d2 = null;
        }
        if (!this.containsXZ(vec3d3)) {
            vec3d3 = null;
        }
        if (!this.containsXZ(vec3d4)) {
            vec3d4 = null;
        }
        if (!this.containsXY(vec3d5)) {
            vec3d5 = null;
        }
        if (!this.containsXY(vec3d6)) {
            vec3d6 = null;
        }
        Vec3d object = null;
        if (vec3d != null && (object == null || from.squaredDistanceTo(vec3d) < from.squaredDistanceTo(object))) {
            vec3d7 = vec3d;
        }
        if (vec3d2 != null && (vec3d7 == null || from.squaredDistanceTo(vec3d2) < from.squaredDistanceTo(vec3d7))) {
            vec3d7 = vec3d2;
        }
        if (vec3d3 != null && (vec3d7 == null || from.squaredDistanceTo(vec3d3) < from.squaredDistanceTo(vec3d7))) {
            vec3d7 = vec3d3;
        }
        if (vec3d4 != null && (vec3d7 == null || from.squaredDistanceTo(vec3d4) < from.squaredDistanceTo(vec3d7))) {
            vec3d7 = vec3d4;
        }
        if (vec3d5 != null && (vec3d7 == null || from.squaredDistanceTo(vec3d5) < from.squaredDistanceTo(vec3d7))) {
            vec3d7 = vec3d5;
        }
        if (vec3d6 != null && (vec3d7 == null || from.squaredDistanceTo(vec3d6) < from.squaredDistanceTo(vec3d7))) {
            vec3d7 = vec3d6;
        }
        if (vec3d7 == null) {
            return null;
        }
        int i = -1;
        if (vec3d7 == vec3d) {
            i = 4;
        }
        if (vec3d7 == vec3d2) {
            i = 5;
        }
        if (vec3d7 == vec3d3) {
            i = 0;
        }
        if (vec3d7 == vec3d4) {
            i = 1;
        }
        if (vec3d7 == vec3d5) {
            i = 2;
        }
        if (vec3d7 == vec3d6) {
            i = 3;
        }
        return new HitResult(0, 0, 0, i, vec3d7);
    }

    private boolean containsYZ(Vec3d vec) {
        if (vec == null) {
            return false;
        }
        return vec.y >= this.minY && vec.y <= this.maxY && vec.z >= this.minZ && vec.z <= this.maxZ;
    }

    private boolean containsXZ(Vec3d vec) {
        if (vec == null) {
            return false;
        }
        return vec.x >= this.minX && vec.x <= this.maxX && vec.z >= this.minZ && vec.z <= this.maxZ;
    }

    private boolean containsXY(Vec3d vec) {
        if (vec == null) {
            return false;
        }
        return vec.x >= this.minX && vec.x <= this.maxX && vec.y >= this.minY && vec.y <= this.maxY;
    }

    public void set(Box box) {
        this.minX = box.minX;
        this.minY = box.minY;
        this.minZ = box.minZ;
        this.maxX = box.maxX;
        this.maxY = box.maxY;
        this.maxZ = box.maxZ;
    }
}

