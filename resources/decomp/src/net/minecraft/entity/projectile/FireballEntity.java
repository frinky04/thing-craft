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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class FireballEntity
extends Entity {
    private int blockX = -1;
    private int blockY = -1;
    private int blockZ = -1;
    private int block = 0;
    private boolean inGround = false;
    public int shake = 0;
    private MobEntity shooter;
    private int inBlockTicks;
    private int inAirTicks = 0;
    public double accelerationX;
    public double accelerationY;
    public double accelerationZ;

    public FireballEntity(World world) {
        super(world);
        this.setSize(1.0f, 1.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public boolean shouldRender(double squaredDistanceToCamera) {
        double d = this.shape.getAverageSideLength() * 4.0;
        return squaredDistanceToCamera < (d *= 64.0) * d;
    }

    public FireballEntity(World world, MobEntity shooter, double accelerationX, double accelerationY, double accelerationZ) {
        super(world);
        this.shooter = shooter;
        this.setSize(1.0f, 1.0f);
        this.setPositionAndAngles(shooter.x, shooter.y, shooter.z, shooter.yaw, shooter.pitch);
        this.setPosition(this.x, this.y, this.z);
        this.eyeHeight = 0.0f;
        this.velocityZ = 0.0;
        this.velocityY = 0.0;
        this.velocityX = 0.0;
        double d = MathHelper.sqrt((accelerationX += this.random.nextGaussian() * 0.4) * accelerationX + (accelerationY += this.random.nextGaussian() * 0.4) * accelerationY + (accelerationZ += this.random.nextGaussian() * 0.4) * accelerationZ);
        this.accelerationX = accelerationX / d * 0.1;
        this.accelerationY = accelerationY / d * 0.1;
        this.accelerationZ = accelerationZ / d * 0.1;
    }

    /*
     * Enabled aggressive block sorting
     */
    public void tick() {
        Entity entity;
        block16: {
            super.tick();
            this.onFireTimer = 10;
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
                    break block16;
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
            float h;
            Box box;
            HitResult hitResult2;
            Entity entity2 = (Entity)list.get(j);
            if (!entity2.hasCollision() || entity2 == this.shooter && this.inAirTicks < 25 || (hitResult2 = (box = entity2.shape.grown(h = 0.3f, h, h)).clip(vec3d, vec3d2)) == null || !((e = vec3d.distanceTo(hitResult2.facePos)) < d) && d != 0.0) continue;
            entity = entity2;
            d = e;
        }
        if (entity != null) {
            hitResult = new HitResult(entity);
        }
        if (hitResult != null) {
            if (hitResult.entity == null || hitResult.entity.takeDamage(this.shooter, 0)) {
                // empty if block
            }
            this.world.explode(null, this.x, this.y, this.z, 1.0f, true);
            this.remove();
        }
        this.x += this.velocityX;
        this.y += this.velocityY;
        this.z += this.velocityZ;
        float f = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
        this.yaw = (float)(Math.atan2(this.velocityX, this.velocityZ) * 180.0 / 3.1415927410125732);
        this.pitch = (float)(Math.atan2(this.velocityY, f) * 180.0 / 3.1415927410125732);
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
        float g = 0.95f;
        if (this.checkWaterCollisions()) {
            for (int k = 0; k < 4; ++k) {
                float l = 0.25f;
                this.world.addParticle("bubble", this.x - this.velocityX * (double)l, this.y - this.velocityY * (double)l, this.z - this.velocityZ * (double)l, this.velocityX, this.velocityY, this.velocityZ);
            }
            g = 0.8f;
        }
        this.velocityX += this.accelerationX;
        this.velocityY += this.accelerationY;
        this.velocityZ += this.accelerationZ;
        this.velocityX *= (double)g;
        this.velocityY *= (double)g;
        this.velocityZ *= (double)g;
        this.world.addParticle("smoke", this.x, this.y + 0.5, this.z, 0.0, 0.0, 0.0);
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

    public boolean hasCollision() {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public float getPickRadius() {
        return 1.0f;
    }

    public boolean takeDamage(Entity source, int amount) {
        this.markDamaged();
        if (source != null) {
            Vec3d vec3d = source.getLookVector();
            if (vec3d != null) {
                this.velocityX = vec3d.x;
                this.velocityY = vec3d.y;
                this.velocityZ = vec3d.z;
                this.accelerationX = this.velocityX * 0.1;
                this.accelerationY = this.velocityY * 0.1;
                this.accelerationZ = this.velocityZ * 0.1;
            }
            return true;
        }
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }
}

