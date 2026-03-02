/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.animal;

import net.minecraft.entity.mob.animal.AnimalEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class ChickenEntity
extends AnimalEntity {
    public boolean sheared = false;
    public float flapProgress = 0.0f;
    public float flapSpeed = 0.0f;
    public float lastFlapSpeed;
    public float lastFlapProgress;
    public float flapping = 1.0f;
    public int layEggCooldown;

    public ChickenEntity(World world) {
        super(world);
        this.texture = "/mob/chicken.png";
        this.setSize(0.3f, 0.4f);
        this.health = 4;
        this.layEggCooldown = this.random.nextInt(6000) + 6000;
    }

    public void mobTick() {
        super.mobTick();
        this.lastFlapProgress = this.flapProgress;
        this.lastFlapSpeed = this.flapSpeed;
        this.flapSpeed = (float)((double)this.flapSpeed + (double)(this.onGround ? -1 : 4) * 0.3);
        if (this.flapSpeed < 0.0f) {
            this.flapSpeed = 0.0f;
        }
        if (this.flapSpeed > 1.0f) {
            this.flapSpeed = 1.0f;
        }
        if (!this.onGround && this.flapping < 1.0f) {
            this.flapping = 1.0f;
        }
        this.flapping = (float)((double)this.flapping * 0.9);
        if (!this.onGround && this.velocityY < 0.0) {
            this.velocityY *= 0.6;
        }
        this.flapProgress += this.flapping * 2.0f;
        if (!this.world.isMultiplayer && --this.layEggCooldown <= 0) {
            this.world.playSound(this, "mob.chickenplop", 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
            this.dropItem(Item.EGG.id, 1);
            this.layEggCooldown = this.random.nextInt(6000) + 6000;
        }
    }

    protected void takeFallDamage(float distance) {
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    protected String getAmbientSound() {
        return "mob.chicken";
    }

    protected String getHurtSound() {
        return "mob.chickenhurt";
    }

    protected String getDeathSound() {
        return "mob.chickenhurt";
    }

    protected int getDropItem() {
        return Item.FEATHER.id;
    }
}

