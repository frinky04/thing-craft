/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.vehicle;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class BoatEntity
extends Entity {
    public int damage = 0;
    public int damagedTimer = 0;
    public int damagedSwingDir = 1;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYaw;
    private double lerpPitch;
    @Environment(value=EnvType.CLIENT)
    private double lerpVelocityX;
    @Environment(value=EnvType.CLIENT)
    private double lerpVelocityY;
    @Environment(value=EnvType.CLIENT)
    private double lerpVelocityZ;

    public BoatEntity(World world) {
        super(world);
        this.blocksBuilding = true;
        this.setSize(1.5f, 0.6f);
        this.eyeHeight = this.height / 2.0f;
        this.makesSteps = false;
    }

    public Box getCollisionAgainstShape(Entity other) {
        return other.shape;
    }

    public Box getCollisionShape() {
        return this.shape;
    }

    public boolean isPushable() {
        return true;
    }

    public BoatEntity(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y + (double)this.eyeHeight, z);
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    public double getMountHeight() {
        return (double)this.height * 0.0 - (double)0.3f;
    }

    public boolean takeDamage(Entity source, int amount) {
        if (this.world.isMultiplayer || this.removed) {
            return true;
        }
        this.damagedSwingDir = -this.damagedSwingDir;
        this.damagedTimer = 10;
        this.damage += amount * 10;
        this.markDamaged();
        if (this.damage > 40) {
            int i;
            for (i = 0; i < 3; ++i) {
                this.dropItem(Block.PLANKS.id, 1, 0.0f);
            }
            for (i = 0; i < 2; ++i) {
                this.dropItem(Item.STICK.id, 1, 0.0f);
            }
            this.remove();
        }
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public void animateDamage() {
        this.damagedSwingDir = -this.damagedSwingDir;
        this.damagedTimer = 10;
        this.damage += this.damage * 10;
    }

    public boolean hasCollision() {
        return !this.removed;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = steps + 4;
        this.velocityX = this.lerpVelocityX;
        this.velocityY = this.lerpVelocityY;
        this.velocityZ = this.lerpVelocityZ;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpVelocity(double velocityX, double velocityY, double velocityZ) {
        this.lerpVelocityX = this.velocityX = velocityX;
        this.lerpVelocityY = this.velocityY = velocityY;
        this.lerpVelocityZ = this.velocityZ = velocityZ;
    }

    public void tick() {
        double ac;
        double o;
        super.tick();
        if (this.damagedTimer > 0) {
            --this.damagedTimer;
        }
        if (this.damage > 0) {
            --this.damage;
        }
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        int i = 5;
        double d = 0.0;
        for (int j = 0; j < i; ++j) {
            double h = this.shape.minY + (this.shape.maxY - this.shape.minY) * (double)(j + 0) / (double)i - 0.125;
            double p = this.shape.minY + (this.shape.maxY - this.shape.minY) * (double)(j + 1) / (double)i - 0.125;
            Box box = Box.fromPool(this.shape.minX, h, this.shape.minZ, this.shape.maxX, p, this.shape.maxZ);
            if (!this.world.containsLiquid(box, Material.WATER)) continue;
            d += 1.0 / (double)i;
        }
        if (this.world.isMultiplayer) {
            if (this.lerpSteps > 0) {
                double t;
                double e = this.x + (this.lerpX - this.x) / (double)this.lerpSteps;
                double m = this.y + (this.lerpY - this.y) / (double)this.lerpSteps;
                double q = this.z + (this.lerpZ - this.z) / (double)this.lerpSteps;
                for (t = this.lerpYaw - (double)this.yaw; t < -180.0; t += 360.0) {
                }
                while (t >= 180.0) {
                    t -= 360.0;
                }
                this.yaw = (float)((double)this.yaw + t / (double)this.lerpSteps);
                this.pitch = (float)((double)this.pitch + (this.lerpPitch - (double)this.pitch) / (double)this.lerpSteps);
                --this.lerpSteps;
                this.setPosition(e, m, q);
                this.setRotation(this.yaw, this.pitch);
            } else {
                double f = this.x + this.velocityX;
                double n = this.y + this.velocityY;
                double r = this.z + this.velocityZ;
                this.setPosition(f, n, r);
                if (this.onGround) {
                    this.velocityX *= 0.5;
                    this.velocityY *= 0.5;
                    this.velocityZ *= 0.5;
                }
                this.velocityX *= (double)0.99f;
                this.velocityY *= (double)0.95f;
                this.velocityZ *= (double)0.99f;
            }
            return;
        }
        double g = d * 2.0 - 1.0;
        this.velocityY += (double)0.04f * g;
        if (this.rider != null) {
            this.velocityX += this.rider.velocityX * 0.2;
            this.velocityZ += this.rider.velocityZ * 0.2;
        }
        if (this.velocityX < -(o = 0.4)) {
            this.velocityX = -o;
        }
        if (this.velocityX > o) {
            this.velocityX = o;
        }
        if (this.velocityZ < -o) {
            this.velocityZ = -o;
        }
        if (this.velocityZ > o) {
            this.velocityZ = o;
        }
        if (this.onGround) {
            this.velocityX *= 0.5;
            this.velocityY *= 0.5;
            this.velocityZ *= 0.5;
        }
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        double s = Math.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
        if (s > 0.15) {
            double u = Math.cos((double)this.yaw * Math.PI / 180.0);
            double x = Math.sin((double)this.yaw * Math.PI / 180.0);
            int z = 0;
            while ((double)z < 1.0 + s * 60.0) {
                double ab = this.random.nextFloat() * 2.0f - 1.0f;
                double ad = (double)(this.random.nextInt(2) * 2 - 1) * 0.7;
                if (this.random.nextBoolean()) {
                    double ae = this.x - u * ab * 0.8 + x * ad;
                    double ah = this.z - x * ab * 0.8 - u * ad;
                    this.world.addParticle("splash", ae, this.y - 0.125, ah, this.velocityX, this.velocityY, this.velocityZ);
                } else {
                    double af = this.x + u + x * ab * 0.7;
                    double ai = this.z + x - u * ab * 0.7;
                    this.world.addParticle("splash", af, this.y - 0.125, ai, this.velocityX, this.velocityY, this.velocityZ);
                }
                ++z;
            }
        }
        if (this.collidingHorizontally && s > 0.15) {
            if (!this.world.isMultiplayer) {
                int v;
                this.remove();
                for (v = 0; v < 3; ++v) {
                    this.dropItem(Block.PLANKS.id, 1, 0.0f);
                }
                for (v = 0; v < 2; ++v) {
                    this.dropItem(Item.STICK.id, 1, 0.0f);
                }
            }
        } else {
            this.velocityX *= (double)0.99f;
            this.velocityY *= (double)0.95f;
            this.velocityZ *= (double)0.99f;
        }
        this.pitch = 0.0f;
        double w = this.yaw;
        double y = this.lastX - this.x;
        double aa = this.lastZ - this.z;
        if (y * y + aa * aa > 0.001) {
            w = (float)(Math.atan2(aa, y) * 180.0 / Math.PI);
        }
        for (ac = w - (double)this.yaw; ac >= 180.0; ac -= 360.0) {
        }
        while (ac < -180.0) {
            ac += 360.0;
        }
        if (ac > 20.0) {
            ac = 20.0;
        }
        if (ac < -20.0) {
            ac = -20.0;
        }
        this.yaw = (float)((double)this.yaw + ac);
        this.setRotation(this.yaw, this.pitch);
        List list = this.world.getEntities(this, this.shape.grown(0.2f, 0.0, 0.2f));
        if (list != null && list.size() > 0) {
            for (int ag = 0; ag < list.size(); ++ag) {
                Entity entity = (Entity)list.get(ag);
                if (entity == this.rider || !entity.isPushable() || !(entity instanceof BoatEntity)) continue;
                entity.push(this);
            }
        }
        if (this.rider != null && this.rider.removed) {
            this.rider = null;
        }
    }

    public void updateRiderPositon() {
        if (this.rider == null) {
            return;
        }
        double d = Math.cos((double)this.yaw * Math.PI / 180.0) * 0.4;
        double e = Math.sin((double)this.yaw * Math.PI / 180.0) * 0.4;
        this.rider.setPosition(this.x + d, this.y + this.getMountHeight() + this.rider.getRideHeight(), this.z + e);
    }

    protected void writeCustomNbt(NbtCompound nbt) {
    }

    protected void readCustomNbt(NbtCompound nbt) {
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }

    public boolean interact(PlayerEntity player) {
        if (this.rider != null && this.rider instanceof PlayerEntity && this.rider != player) {
            return true;
        }
        if (!this.world.isMultiplayer) {
            player.startRiding(this);
        }
        return true;
    }
}

