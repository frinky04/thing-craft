/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.projectile;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class ArrowEntity
extends Entity {
    private int blockX = -1;
    private int blockY = -1;
    private int blockZ = -1;
    private int block = 0;
    private boolean inGround = false;
    public int shake = 0;
    public MobEntity shooter;
    private int inBlockTicks;
    private int inAirTicks = 0;

    public ArrowEntity(World world) {
        super(world);
        this.setSize(0.5f, 0.5f);
    }

    @Environment(value=EnvType.CLIENT)
    public ArrowEntity(World world, double x, double y, double z) {
        super(world);
        this.setSize(0.5f, 0.5f);
        this.setPosition(x, y, z);
        this.eyeHeight = 0.0f;
    }

    public ArrowEntity(World world, MobEntity shooter) {
        super(world);
        this.shooter = shooter;
        this.setSize(0.5f, 0.5f);
        this.setPositionAndAngles(shooter.x, shooter.y + (double)shooter.getEyeHeight(), shooter.z, shooter.yaw, shooter.pitch);
        this.x -= (double)(MathHelper.cos(this.yaw / 180.0f * (float)Math.PI) * 0.16f);
        this.y -= (double)0.1f;
        this.z -= (double)(MathHelper.sin(this.yaw / 180.0f * (float)Math.PI) * 0.16f);
        this.setPosition(this.x, this.y, this.z);
        this.eyeHeight = 0.0f;
        this.velocityX = -MathHelper.sin(this.yaw / 180.0f * (float)Math.PI) * MathHelper.cos(this.pitch / 180.0f * (float)Math.PI);
        this.velocityZ = MathHelper.cos(this.yaw / 180.0f * (float)Math.PI) * MathHelper.cos(this.pitch / 180.0f * (float)Math.PI);
        this.velocityY = -MathHelper.sin(this.pitch / 180.0f * (float)Math.PI);
        this.shoot(this.velocityX, this.velocityY, this.velocityZ, 1.5f, 1.0f);
    }

    public void shoot(double velocityX, double velocityY, double velocityZ, float power, float variance) {
        float f = MathHelper.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        velocityX /= (double)f;
        velocityY /= (double)f;
        velocityZ /= (double)f;
        velocityX += this.random.nextGaussian() * (double)0.0075f * (double)variance;
        velocityY += this.random.nextGaussian() * (double)0.0075f * (double)variance;
        velocityZ += this.random.nextGaussian() * (double)0.0075f * (double)variance;
        this.velocityX = velocityX *= (double)power;
        this.velocityY = velocityY *= (double)power;
        this.velocityZ = velocityZ *= (double)power;
        float g = MathHelper.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        this.lastYaw = this.yaw = (float)(Math.atan2(velocityX, velocityZ) * 180.0 / 3.1415927410125732);
        this.lastPitch = this.pitch = (float)(Math.atan2(velocityY, g) * 180.0 / 3.1415927410125732);
        this.inBlockTicks = 0;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpVelocity(double velocityX, double velocityY, double velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        if (this.lastPitch == 0.0f && this.lastYaw == 0.0f) {
            float f = MathHelper.sqrt(velocityX * velocityX + velocityZ * velocityZ);
            this.lastYaw = this.yaw = (float)(Math.atan2(velocityX, velocityZ) * 180.0 / 3.1415927410125732);
            this.lastPitch = this.pitch = (float)(Math.atan2(velocityY, f) * 180.0 / 3.1415927410125732);
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    public void tick() {
        Entity entity;
        block20: {
            super.tick();
            if (this.lastPitch == 0.0f && this.lastYaw == 0.0f) {
                float f = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
                this.lastYaw = this.yaw = (float)(Math.atan2(this.velocityX, this.velocityZ) * 180.0 / 3.1415927410125732);
                this.lastPitch = this.pitch = (float)(Math.atan2(this.velocityY, f) * 180.0 / 3.1415927410125732);
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
                    break block20;
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
        double d = 0.0;
        for (int j = 0; j < list.size(); ++j) {
            double e;
            float l;
            Box box;
            HitResult hitResult2;
            Entity entity2 = (Entity)list.get(j);
            if (!entity2.hasCollision() || entity2 == this.shooter && this.inAirTicks < 5 || (hitResult2 = (box = entity2.shape.grown(l = 0.3f, l, l)).clip(vec3d, vec3d2)) == null || !((e = vec3d.distanceTo(hitResult2.facePos)) < d) && d != 0.0) continue;
            entity = entity2;
            d = e;
        }
        if (entity != null) {
            hitResult = new HitResult(entity);
        }
        if (hitResult != null) {
            if (hitResult.entity != null) {
                if (hitResult.entity.takeDamage(this.shooter, 4)) {
                    this.world.playSound(this, "random.drr", 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
                    this.remove();
                } else {
                    this.velocityX *= (double)-0.1f;
                    this.velocityY *= (double)-0.1f;
                    this.velocityZ *= (double)-0.1f;
                    this.yaw += 180.0f;
                    this.lastYaw += 180.0f;
                    this.inAirTicks = 0;
                }
            } else {
                this.blockX = hitResult.x;
                this.blockY = hitResult.y;
                this.blockZ = hitResult.z;
                this.block = this.world.getBlock(this.blockX, this.blockY, this.blockZ);
                this.velocityX = (float)(hitResult.facePos.x - this.x);
                this.velocityY = (float)(hitResult.facePos.y - this.y);
                this.velocityZ = (float)(hitResult.facePos.z - this.z);
                float g = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ);
                this.x -= this.velocityX / (double)g * (double)0.05f;
                this.y -= this.velocityY / (double)g * (double)0.05f;
                this.z -= this.velocityZ / (double)g * (double)0.05f;
                this.world.playSound(this, "random.drr", 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
                this.inGround = true;
                this.shake = 7;
            }
        }
        this.x += this.velocityX;
        this.y += this.velocityY;
        this.z += this.velocityZ;
        float h = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
        this.yaw = (float)(Math.atan2(this.velocityX, this.velocityZ) * 180.0 / 3.1415927410125732);
        this.pitch = (float)(Math.atan2(this.velocityY, h) * 180.0 / 3.1415927410125732);
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
        float k = 0.99f;
        float m = 0.03f;
        if (this.checkWaterCollisions()) {
            for (int n = 0; n < 4; ++n) {
                float o = 0.25f;
                this.world.addParticle("bubble", this.x - this.velocityX * (double)o, this.y - this.velocityY * (double)o, this.z - this.velocityZ * (double)o, this.velocityX, this.velocityY, this.velocityZ);
            }
            k = 0.8f;
        }
        this.velocityX *= (double)k;
        this.velocityY *= (double)k;
        this.velocityZ *= (double)k;
        this.velocityY -= (double)m;
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

    public void onPlayerCollision(PlayerEntity player) {
        if (this.world.isMultiplayer) {
            return;
        }
        if (this.inGround && this.shooter == player && this.shake <= 0 && player.inventory.addItem(new ItemStack(Item.ARROW.id, 1))) {
            this.world.playSound(this, "random.pop", 0.2f, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            player.pickUp(this, 1);
            this.remove();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }
}

