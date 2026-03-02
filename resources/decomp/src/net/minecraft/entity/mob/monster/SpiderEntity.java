/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.monster;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class SpiderEntity
extends MonsterEntity {
    public SpiderEntity(World world) {
        super(world);
        this.texture = "/mob/spider.png";
        this.setSize(1.4f, 0.9f);
        this.walkSpeed = 0.8f;
    }

    public double getMountHeight() {
        return (double)this.height * 0.75 - 0.5;
    }

    protected Entity findTarget() {
        float f = this.getBrightness(1.0f);
        if (f < 0.5f) {
            double d = 16.0;
            return this.world.getNearestPlayer(this, d);
        }
        return null;
    }

    protected String getAmbientSound() {
        return "mob.spider";
    }

    protected String getHurtSound() {
        return "mob.spider";
    }

    protected String getDeathSound() {
        return "mob.spiderdeath";
    }

    protected void targetInSight(Entity target, float distance) {
        float f = this.getBrightness(1.0f);
        if (f > 0.5f && this.random.nextInt(100) == 0) {
            this.walkTarget = null;
            return;
        }
        if (distance > 2.0f && distance < 6.0f && this.random.nextInt(10) == 0) {
            if (this.onGround) {
                double d = target.x - this.x;
                double e = target.z - this.z;
                float g = MathHelper.sqrt(d * d + e * e);
                this.velocityX = d / (double)g * 0.5 * (double)0.8f + this.velocityX * (double)0.2f;
                this.velocityZ = e / (double)g * 0.5 * (double)0.8f + this.velocityZ * (double)0.2f;
                this.velocityY = 0.4f;
            }
        } else {
            super.targetInSight(target, distance);
        }
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    protected int getDropItem() {
        return Item.STRING.id;
    }
}

