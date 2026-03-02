/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PathFinderMobEntity
extends MobEntity {
    private Path path;
    protected Entity walkTarget;
    protected boolean stoppedMoving = false;

    public PathFinderMobEntity(World world) {
        super(world);
    }

    protected void aiTick() {
        Vec3d vec3d2;
        this.stoppedMoving = false;
        float f = 16.0f;
        if (this.walkTarget == null) {
            this.walkTarget = this.findTarget();
            if (this.walkTarget != null) {
                this.path = this.world.findPath(this, this.walkTarget, f);
            }
        } else if (!this.walkTarget.isAlive()) {
            this.walkTarget = null;
        } else {
            float g = this.walkTarget.distanceTo(this);
            if (this.canSee(this.walkTarget)) {
                this.targetInSight(this.walkTarget, g);
            }
        }
        if (!(this.stoppedMoving || this.walkTarget == null || this.path != null && this.random.nextInt(20) != 0)) {
            this.path = this.world.findPath(this, this.walkTarget, f);
        } else if (this.path == null && this.random.nextInt(80) == 0 || this.random.nextInt(80) == 0) {
            boolean i = false;
            int k = -1;
            int m = -1;
            int o = -1;
            float h = -99999.0f;
            for (int p = 0; p < 10; ++p) {
                int s;
                int r;
                int q = MathHelper.floor(this.x + (double)this.random.nextInt(13) - 6.0);
                float u = this.getPathfindingFavor(q, r = MathHelper.floor(this.y + (double)this.random.nextInt(7) - 3.0), s = MathHelper.floor(this.z + (double)this.random.nextInt(13) - 6.0));
                if (!(u > h)) continue;
                h = u;
                k = q;
                m = r;
                o = s;
                i = true;
            }
            if (i) {
                this.path = this.world.findPath(this, k, m, o, 10.0f);
            }
        }
        int j = MathHelper.floor(this.shape.minY);
        boolean l = this.checkWaterCollisions();
        boolean n = this.isInLava();
        this.pitch = 0.0f;
        if (this.path == null || this.random.nextInt(100) == 0) {
            super.aiTick();
            this.path = null;
            return;
        }
        Vec3d vec3d = this.path.getPos(this);
        double d = this.width * 2.0f;
        while (vec3d != null && vec3d.squaredDistanceTo(this.x, vec3d.y, this.z) < d * d) {
            this.path.advance();
            if (this.path.isDone()) {
                Object object = null;
                this.path = null;
                continue;
            }
            vec3d2 = this.path.getPos(this);
        }
        this.jumping = false;
        if (vec3d2 != null) {
            float x;
            double e = vec3d2.x - this.x;
            double t = vec3d2.z - this.z;
            double v = vec3d2.y - (double)j;
            float w = (float)(Math.atan2(t, e) * 180.0 / 3.1415927410125732) - 90.0f;
            this.forwardSpeed = this.walkSpeed;
            for (x = w - this.yaw; x < -180.0f; x += 360.0f) {
            }
            while (x >= 180.0f) {
                x -= 360.0f;
            }
            if (x > 30.0f) {
                x = 30.0f;
            }
            if (x < -30.0f) {
                x = -30.0f;
            }
            this.yaw += x;
            if (this.stoppedMoving && this.walkTarget != null) {
                double y = this.walkTarget.x - this.x;
                double z = this.walkTarget.z - this.z;
                float aa = this.yaw;
                this.yaw = (float)(Math.atan2(z, y) * 180.0 / 3.1415927410125732) - 90.0f;
                x = (aa - this.yaw + 90.0f) * (float)Math.PI / 180.0f;
                this.sidewaysSpeed = -MathHelper.sin(x) * this.forwardSpeed * 1.0f;
                this.forwardSpeed = MathHelper.cos(x) * this.forwardSpeed * 1.0f;
            }
            if (v > 0.0) {
                this.jumping = true;
            }
        }
        if (this.walkTarget != null) {
            this.lookAt(this.walkTarget, 30.0f);
        }
        if (this.collidingHorizontally) {
            this.jumping = true;
        }
        if (this.random.nextFloat() < 0.8f && (l || n)) {
            this.jumping = true;
        }
    }

    protected void targetInSight(Entity target, float distance) {
    }

    protected float getPathfindingFavor(int x, int y, int z) {
        return 0.0f;
    }

    protected Entity findTarget() {
        return null;
    }

    public boolean canSpawn() {
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(this.shape.minY);
        int k = MathHelper.floor(this.z);
        return super.canSpawn() && this.getPathfindingFavor(i, j, k) >= 0.0f;
    }
}

