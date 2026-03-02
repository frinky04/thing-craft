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
import net.minecraft.util.math.MathHelper;

public class Vec3d {
    private static List POOL = new ArrayList();
    private static int poolIndex = 0;
    public double x;
    public double y;
    public double z;

    public static Vec3d of(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    public static void resetPool() {
        poolIndex = 0;
    }

    public static Vec3d fromPool(double x, double y, double z) {
        if (poolIndex >= POOL.size()) {
            POOL.add(Vec3d.of(0.0, 0.0, 0.0));
        }
        return ((Vec3d)POOL.get(poolIndex++)).subtract(x, y, z);
    }

    private Vec3d(double x, double y, double z) {
        if (x == -0.0) {
            x = 0.0;
        }
        if (y == -0.0) {
            y = 0.0;
        }
        if (z == -0.0) {
            z = 0.0;
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private Vec3d subtract(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d subtractFrom(Vec3d vec) {
        return Vec3d.fromPool(vec.x - this.x, vec.y - this.y, vec.z - this.z);
    }

    public Vec3d normalize() {
        double d = MathHelper.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (d < 1.0E-4) {
            return Vec3d.fromPool(0.0, 0.0, 0.0);
        }
        return Vec3d.fromPool(this.x / d, this.y / d, this.z / d);
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d cross(Vec3d vec) {
        return Vec3d.fromPool(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }

    public Vec3d add(double x, double y, double z) {
        return Vec3d.fromPool(this.x + x, this.y + y, this.z + z);
    }

    public double distanceTo(Vec3d vec) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        return MathHelper.sqrt(d * d + e * e + f * f);
    }

    public double squaredDistanceTo(Vec3d vec) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        return d * d + e * e + f * f;
    }

    public double squaredDistanceTo(double x, double y, double z) {
        double d = x - this.x;
        double e = y - this.y;
        double f = z - this.z;
        return d * d + e * e + f * f;
    }

    public double length() {
        return MathHelper.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public Vec3d intermediateWithX(Vec3d vec, double x) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        if (d * d < (double)1.0E-7f) {
            return null;
        }
        double g = (x - this.x) / d;
        if (g < 0.0 || g > 1.0) {
            return null;
        }
        return Vec3d.fromPool(this.x + d * g, this.y + e * g, this.z + f * g);
    }

    public Vec3d intermediateWithY(Vec3d vec, double y) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        if (e * e < (double)1.0E-7f) {
            return null;
        }
        double g = (y - this.y) / e;
        if (g < 0.0 || g > 1.0) {
            return null;
        }
        return Vec3d.fromPool(this.x + d * g, this.y + e * g, this.z + f * g);
    }

    public Vec3d intermediateWithZ(Vec3d vec, double z) {
        double d = vec.x - this.x;
        double e = vec.y - this.y;
        double f = vec.z - this.z;
        if (f * f < (double)1.0E-7f) {
            return null;
        }
        double g = (z - this.z) / f;
        if (g < 0.0 || g > 1.0) {
            return null;
        }
        return Vec3d.fromPool(this.x + d * g, this.y + e * g, this.z + f * g);
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    @Environment(value=EnvType.CLIENT)
    public void rotateX(float angle) {
        float f = MathHelper.cos(angle);
        float g = MathHelper.sin(angle);
        double d = this.x;
        double e = this.y * (double)f + this.z * (double)g;
        double h = this.z * (double)f - this.y * (double)g;
        this.x = d;
        this.y = e;
        this.z = h;
    }

    @Environment(value=EnvType.CLIENT)
    public void rotateY(float angle) {
        float f = MathHelper.cos(angle);
        float g = MathHelper.sin(angle);
        double d = this.x * (double)f + this.z * (double)g;
        double e = this.y;
        double h = this.z * (double)f - this.x * (double)g;
        this.x = d;
        this.y = e;
        this.z = h;
    }
}

