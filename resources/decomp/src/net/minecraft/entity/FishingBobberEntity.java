/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class FishingBobberEntity
extends Entity {
    private int blockX = -1;
    private int blockY = -1;
    private int blockZ = -1;
    private int block = 0;
    private boolean inGround = false;
    public int shake = 0;
    public PlayerEntity thrower;
    private int inBlockTicks;
    private int inAirTicks = 0;
    private int fishTravelTimer = 0;
    public Entity hookedEntity = null;
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

    public FishingBobberEntity(World world) {
        super(world);
        this.setSize(0.25f, 0.25f);
    }

    @Environment(value=EnvType.CLIENT)
    public boolean shouldRender(double squaredDistanceToCamera) {
        double d = this.shape.getAverageSideLength() * 4.0;
        return squaredDistanceToCamera < (d *= 64.0) * d;
    }

    @Environment(value=EnvType.CLIENT)
    public FishingBobberEntity(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y, z);
    }

    public FishingBobberEntity(World world, PlayerEntity thrower) {
        super(world);
        this.thrower = thrower;
        this.thrower.fishingBobber = this;
        this.setSize(0.25f, 0.25f);
        this.setPositionAndAngles(thrower.x, thrower.y + 1.62 - (double)thrower.eyeHeight, thrower.z, thrower.yaw, thrower.pitch);
        this.x -= (double)(MathHelper.cos(this.yaw / 180.0f * (float)Math.PI) * 0.16f);
        this.y -= (double)0.1f;
        this.z -= (double)(MathHelper.sin(this.yaw / 180.0f * (float)Math.PI) * 0.16f);
        this.setPosition(this.x, this.y, this.z);
        this.eyeHeight = 0.0f;
        float f = 0.4f;
        this.velocityX = -MathHelper.sin(this.yaw / 180.0f * (float)Math.PI) * MathHelper.cos(this.pitch / 180.0f * (float)Math.PI) * f;
        this.velocityZ = MathHelper.cos(this.yaw / 180.0f * (float)Math.PI) * MathHelper.cos(this.pitch / 180.0f * (float)Math.PI) * f;
        this.velocityY = -MathHelper.sin(this.pitch / 180.0f * (float)Math.PI) * f;
        this.thrown(this.velocityX, this.velocityY, this.velocityZ, 1.5f, 1.0f);
    }

    public void thrown(double velocityX, double velocityY, double velocityZ, float scale, float min) {
        float f = MathHelper.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        velocityX /= (double)f;
        velocityY /= (double)f;
        velocityZ /= (double)f;
        velocityX += this.random.nextGaussian() * (double)0.0075f * (double)min;
        velocityY += this.random.nextGaussian() * (double)0.0075f * (double)min;
        velocityZ += this.random.nextGaussian() * (double)0.0075f * (double)min;
        this.velocityX = velocityX *= (double)scale;
        this.velocityY = velocityY *= (double)scale;
        this.velocityZ = velocityZ *= (double)scale;
        float g = MathHelper.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        this.lastYaw = this.yaw = (float)(Math.atan2(velocityX, velocityZ) * 180.0 / 3.1415927410125732);
        this.lastPitch = this.pitch = (float)(Math.atan2(velocityY, g) * 180.0 / 3.1415927410125732);
        this.inBlockTicks = 0;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = steps;
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

    /*
     * Enabled aggressive block sorting
     */
    public void tick() {
        Entity entity;
        block35: {
            super.tick();
            if (this.lerpSteps > 0) {
                double h;
                double d = this.x + (this.lerpX - this.x) / (double)this.lerpSteps;
                double e = this.y + (this.lerpY - this.y) / (double)this.lerpSteps;
                double f = this.z + (this.lerpZ - this.z) / (double)this.lerpSteps;
                for (h = this.lerpYaw - (double)this.yaw; h < -180.0; h += 360.0) {
                }
                while (true) {
                    if (!(h >= 180.0)) {
                        this.yaw = (float)((double)this.yaw + h / (double)this.lerpSteps);
                        this.pitch = (float)((double)this.pitch + (this.lerpPitch - (double)this.pitch) / (double)this.lerpSteps);
                        --this.lerpSteps;
                        this.setPosition(d, e, f);
                        this.setRotation(this.yaw, this.pitch);
                        return;
                    }
                    h -= 360.0;
                }
            }
            if (!this.world.isMultiplayer) {
                ItemStack itemStack = this.thrower.getItemInHand();
                if (this.thrower.removed || !this.thrower.isAlive() || itemStack == null || itemStack.getItem() != Item.FISHING_ROD || this.squaredDistanceTo(this.thrower) > 1024.0) {
                    this.remove();
                    this.thrower.fishingBobber = null;
                    return;
                }
                if (this.hookedEntity != null) {
                    if (!this.hookedEntity.removed) {
                        this.x = this.hookedEntity.x;
                        this.y = this.hookedEntity.shape.minY + (double)this.hookedEntity.height * 0.8;
                        this.z = this.hookedEntity.z;
                        return;
                    }
                    this.hookedEntity = null;
                }
            }
            if (this.shake > 0) {
                --this.shake;
            }
            if (this.inGround) {
                int i = this.world.getBlock(this.blockX, this.blockY, this.blockZ);
                if (i != this.block) {
                    this.inGround = false;
                    this.velocityX *= (double)(this.random.nextFloat() * 0.2f);
                    this.velocityY *= (double)(this.random.nextFloat() * 0.2f);
                    this.velocityZ *= (double)(this.random.nextFloat() * 0.2f);
                    this.inBlockTicks = 0;
                    this.inAirTicks = 0;
                    break block35;
                } else {
                    ++this.inBlockTicks;
                    if (this.inBlockTicks == 1200) {
                        this.remove();
                    }
                    return;
                }
            }
            ++this.inAirTicks;
        }
        Vec3d vec3d = Vec3d.fromPool(this.x, this.y, this.z);
        Vec3d vec3d2 = Vec3d.fromPool(this.x + this.velocityX, this.y + this.velocityY, this.z + this.velocityZ);
        HitResult hitResult = this.world.rayTrace(vec3d, vec3d2);
        vec3d = Vec3d.fromPool(this.x, this.y, this.z);
        vec3d2 = Vec3d.fromPool(this.x + this.velocityX, this.y + this.velocityY, this.z + this.velocityZ);
        if (hitResult != null) {
            vec3d2 = Vec3d.fromPool(hitResult.facePos.x, hitResult.facePos.y, hitResult.facePos.z);
        }
        Object object = null;
        List list = this.world.getEntities(this, this.shape.expanded(this.velocityX, this.velocityY, this.velocityZ).grown(1.0, 1.0, 1.0));
        double g = 0.0;
        for (int j = 0; j < list.size(); ++j) {
            double p;
            float m;
            Box box;
            HitResult hitResult2;
            Entity entity2 = (Entity)list.get(j);
            if (!entity2.hasCollision() || entity2 == this.thrower && this.inAirTicks < 5 || (hitResult2 = (box = entity2.shape.grown(m = 0.3f, m, m)).clip(vec3d, vec3d2)) == null || !((p = vec3d.distanceTo(hitResult2.facePos)) < g) && g != 0.0) continue;
            entity = entity2;
            g = p;
        }
        if (entity != null) {
            hitResult = new HitResult(entity);
        }
        if (hitResult != null) {
            if (hitResult.entity != null) {
                if (hitResult.entity.takeDamage(this.thrower, 0)) {
                    this.hookedEntity = hitResult.entity;
                }
            } else {
                this.inGround = true;
            }
        }
        if (this.inGround) {
            return;
        }
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        float k = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
        this.yaw = (float)(Math.atan2(this.velocityX, this.velocityZ) * 180.0 / 3.1415927410125732);
        this.pitch = (float)(Math.atan2(this.velocityY, k) * 180.0 / 3.1415927410125732);
        while (this.pitch - this.lastPitch < -180.0f) {
            this.lastPitch -= 360.0f;
        }
        while (this.pitch - this.lastPitch >= 180.0f) {
            this.lastPitch += 360.0f;
        }
        while (this.yaw - this.lastYaw < -180.0f) {
            this.lastYaw -= 360.0f;
        }
        while (this.yaw - this.lastYaw >= 180.0f) {
            this.lastYaw += 360.0f;
        }
        this.pitch = this.lastPitch + (this.pitch - this.lastPitch) * 0.2f;
        this.yaw = this.lastYaw + (this.yaw - this.lastYaw) * 0.2f;
        float l = 0.92f;
        if (this.onGround || this.collidingHorizontally) {
            l = 0.5f;
        }
        int n = 5;
        double o = 0.0;
        for (int q = 0; q < n; ++q) {
            double t = this.shape.minY + (this.shape.maxY - this.shape.minY) * (double)(q + 0) / (double)n - 0.125 + 0.125;
            double x = this.shape.minY + (this.shape.maxY - this.shape.minY) * (double)(q + 1) / (double)n - 0.125 + 0.125;
            Box box2 = Box.fromPool(this.shape.minX, t, this.shape.minZ, this.shape.maxX, x, this.shape.maxZ);
            if (!this.world.containsLiquid(box2, Material.WATER)) continue;
            o += 1.0 / (double)n;
        }
        if (o > 0.0) {
            if (this.fishTravelTimer > 0) {
                --this.fishTravelTimer;
            } else if (this.random.nextInt(500) == 0) {
                this.fishTravelTimer = this.random.nextInt(30) + 10;
                this.velocityY -= (double)0.2f;
                this.world.playSound(this, "random.splash", 0.25f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.4f);
                float r = MathHelper.floor(this.shape.minY);
                int u = 0;
                while ((float)u < 1.0f + this.width * 20.0f) {
                    float v = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    float y = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    this.world.addParticle("bubble", this.x + (double)v, r + 1.0f, this.z + (double)y, this.velocityX, this.velocityY - (double)(this.random.nextFloat() * 0.2f), this.velocityZ);
                    ++u;
                }
                u = 0;
                while ((float)u < 1.0f + this.width * 20.0f) {
                    float w = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    float z = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    this.world.addParticle("splash", this.x + (double)w, r + 1.0f, this.z + (double)z, this.velocityX, this.velocityY, this.velocityZ);
                    ++u;
                }
            }
        }
        if (this.fishTravelTimer > 0) {
            this.velocityY -= (double)(this.random.nextFloat() * this.random.nextFloat() * this.random.nextFloat()) * 0.2;
        }
        double s = o * 2.0 - 1.0;
        this.velocityY += (double)0.04f * s;
        if (o > 0.0) {
            l = (float)((double)l * 0.9);
            this.velocityY *= 0.8;
        }
        this.velocityX *= (double)l;
        this.velocityY *= (double)l;
        this.velocityZ *= (double)l;
        this.setPosition(this.x, this.y, this.z);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        nbt.putShort("xTile", (short)this.blockX);
        nbt.putShort("yTile", (short)this.blockY);
        nbt.putShort("zTile", (short)this.blockZ);
        nbt.putByte("inTile", (byte)this.block);
        nbt.putByte("shake", (byte)this.shake);
        nbt.putByte("inGround", (byte)(this.inGround ? 1 : 0));
    }

    public void readCustomNbt(NbtCompound nbt) {
        this.blockX = nbt.getShort("xTile");
        this.blockY = nbt.getShort("yTile");
        this.blockZ = nbt.getShort("zTile");
        this.block = nbt.getByte("inTile") & 0xFF;
        this.shake = nbt.getByte("shake") & 0xFF;
        this.inGround = nbt.getByte("inGround") == 1;
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }

    public int retrieve() {
        int i = 0;
        if (this.hookedEntity != null) {
            double d = this.thrower.x - this.x;
            double f = this.thrower.y - this.y;
            double h = this.thrower.z - this.z;
            double k = MathHelper.sqrt(d * d + f * f + h * h);
            double m = 0.1;
            this.hookedEntity.velocityX += d * m;
            this.hookedEntity.velocityY += f * m + (double)MathHelper.sqrt(k) * 0.08;
            this.hookedEntity.velocityZ += h * m;
            i = 3;
        } else if (this.fishTravelTimer > 0) {
            ItemEntity itemEntity = new ItemEntity(this.world, this.x, this.y, this.z, new ItemStack(Item.FISH.id));
            double e = this.thrower.x - this.x;
            double g = this.thrower.y - this.y;
            double j = this.thrower.z - this.z;
            double l = MathHelper.sqrt(e * e + g * g + j * j);
            double n = 0.1;
            itemEntity.velocityX = e * n;
            itemEntity.velocityY = g * n + (double)MathHelper.sqrt(l) * 0.08;
            itemEntity.velocityZ = j * n;
            this.world.addEntity(itemEntity);
            i = 1;
        }
        if (this.inGround) {
            i = 2;
        }
        this.remove();
        this.thrower.fishingBobber = null;
        return i;
    }
}

