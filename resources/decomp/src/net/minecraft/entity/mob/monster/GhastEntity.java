/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.monster;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.FlyingMobEntity;
import net.minecraft.entity.mob.monster.Monster;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GhastEntity
extends FlyingMobEntity
implements Monster {
    public int collisionCheckCooldown = 0;
    public double floatTargetX;
    public double floatTargetY;
    public double floatTargetZ;
    private Entity target = null;
    private int findTargetTimer = 0;
    public int lastChargeTicks = 0;
    public int chargeTicks = 0;

    public GhastEntity(World world) {
        super(world);
        this.texture = "/mob/ghast.png";
        this.setSize(4.0f, 4.0f);
        this.immuneToFire = true;
    }

    protected void aiTick() {
        if (this.world.difficulty == 0) {
            this.remove();
        }
        this.lastChargeTicks = this.chargeTicks;
        double d = this.floatTargetX - this.x;
        double e = this.floatTargetY - this.y;
        double f = this.floatTargetZ - this.z;
        double g = MathHelper.sqrt(d * d + e * e + f * f);
        if (g < 1.0 || g > 60.0) {
            this.floatTargetX = this.x + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 16.0f);
            this.floatTargetY = this.y + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 16.0f);
            this.floatTargetZ = this.z + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 16.0f);
        }
        if (this.collisionCheckCooldown-- <= 0) {
            this.collisionCheckCooldown += this.random.nextInt(5) + 2;
            if (this.canReachTarget(this.floatTargetX, this.floatTargetY, this.floatTargetZ, g)) {
                this.velocityX += d / g * 0.1;
                this.velocityY += e / g * 0.1;
                this.velocityZ += f / g * 0.1;
            } else {
                this.floatTargetX = this.x;
                this.floatTargetY = this.y;
                this.floatTargetZ = this.z;
            }
        }
        if (this.target != null && this.target.removed) {
            this.target = null;
        }
        if (this.target == null || this.findTargetTimer-- <= 0) {
            this.target = this.world.getNearestPlayer(this, 100.0);
            if (this.target != null) {
                this.findTargetTimer = 20;
            }
        }
        double h = 64.0;
        if (this.target != null && this.target.squaredDistanceTo(this) < h * h) {
            double i = this.target.x - this.x;
            double j = this.target.shape.minY + (double)(this.target.height / 2.0f) - (this.y + (double)(this.height / 2.0f));
            double k = this.target.z - this.z;
            this.bodyYaw = this.yaw = -((float)Math.atan2(i, k)) * 180.0f / (float)Math.PI;
            if (this.canSee(this.target)) {
                if (this.chargeTicks == 10) {
                    this.world.playSound(this, "mob.ghast.charge", this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                }
                ++this.chargeTicks;
                if (this.chargeTicks == 20) {
                    this.world.playSound(this, "mob.ghast.fireball", this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                    FireballEntity fireballEntity = new FireballEntity(this.world, this, i, j, k);
                    double l = 4.0;
                    Vec3d vec3d = this.getLookVector(1.0f);
                    fireballEntity.x = this.x + vec3d.x * l;
                    fireballEntity.y = this.y + (double)(this.height / 2.0f) + 0.5;
                    fireballEntity.z = this.z + vec3d.z * l;
                    this.world.addEntity(fireballEntity);
                    this.chargeTicks = -40;
                }
            } else if (this.chargeTicks > 0) {
                --this.chargeTicks;
            }
        } else {
            this.bodyYaw = this.yaw = -((float)Math.atan2(this.velocityX, this.velocityZ)) * 180.0f / (float)Math.PI;
            if (this.chargeTicks > 0) {
                --this.chargeTicks;
            }
        }
        this.texture = this.chargeTicks > 10 ? "/mob/ghast_fire.png" : "/mob/ghast.png";
    }

    private boolean canReachTarget(double targetX, double targetY, double targetZ, double distance) {
        double d = (this.floatTargetX - this.x) / distance;
        double e = (this.floatTargetY - this.y) / distance;
        double f = (this.floatTargetZ - this.z) / distance;
        Box box = this.shape.copy();
        int i = 1;
        while ((double)i < distance) {
            box.move(d, e, f);
            if (this.world.getCollisions(this, box).size() > 0) {
                return false;
            }
            ++i;
        }
        return true;
    }

    protected String getAmbientSound() {
        return "mob.ghast.moan";
    }

    protected String getHurtSound() {
        return "mob.ghast.scream";
    }

    protected String getDeathSound() {
        return "mob.ghast.death";
    }

    protected int getDropItem() {
        return Item.GUNPOWDER.id;
    }

    protected float getSoundVolume() {
        return 10.0f;
    }

    public boolean canSpawn() {
        return this.random.nextInt(20) == 0 && super.canSpawn() && this.world.difficulty > 0;
    }

    public int maxSpawnedPerChunk() {
        return 1;
    }
}

