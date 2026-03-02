/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.mob.monster;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.entity.mob.monster.SkeletonEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class CreeperEntity
extends MonsterEntity {
    int fuse;
    int lastFuse;
    int fuseDuration = 30;
    int fuseCooldown = -1;
    int lastFuseCooldown = -1;

    public CreeperEntity(World world) {
        super(world);
        this.texture = "/mob/creeper.png";
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    @Environment(value=EnvType.CLIENT)
    public void doEvent(byte event) {
        super.doEvent(event);
        if (event == 4) {
            if (this.fuse == 0) {
                this.world.playSound(this, "random.fuse", 1.0f, 0.5f);
            }
            this.fuseCooldown = 1;
        }
        if (event == 5) {
            this.fuseCooldown = -1;
        }
    }

    public void tick() {
        this.lastFuse = this.fuse;
        if (this.world.isMultiplayer) {
            this.fuse += this.fuseCooldown;
            if (this.fuse < 0) {
                this.fuse = 0;
            }
            if (this.fuse >= this.fuseDuration) {
                this.fuse = this.fuseDuration;
            }
        }
        super.tick();
    }

    protected void aiTick() {
        if (this.lastFuseCooldown != this.fuseCooldown) {
            this.lastFuseCooldown = this.fuseCooldown;
            if (this.fuseCooldown > 0) {
                this.world.doEntityEvent(this, (byte)4);
            } else {
                this.world.doEntityEvent(this, (byte)5);
            }
        }
        this.lastFuse = this.fuse;
        if (this.world.isMultiplayer) {
            super.aiTick();
        } else {
            if (this.fuse > 0 && this.fuseCooldown < 0) {
                --this.fuse;
            }
            if (this.fuseCooldown >= 0) {
                this.fuseCooldown = 2;
            }
            super.aiTick();
            if (this.fuseCooldown != 1) {
                this.fuseCooldown = -1;
            }
        }
    }

    protected String getHurtSound() {
        return "mob.creeper";
    }

    protected String getDeathSound() {
        return "mob.creeperdeath";
    }

    public void die(Entity killer) {
        super.die(killer);
        if (killer instanceof SkeletonEntity) {
            this.dropItem(Item.RECORD_13.id + this.random.nextInt(2), 1);
        }
    }

    protected void targetInSight(Entity target, float distance) {
        if (this.fuseCooldown <= 0 && distance < 3.0f || this.fuseCooldown > 0 && distance < 7.0f) {
            if (this.fuse == 0) {
                this.world.playSound(this, "random.fuse", 1.0f, 0.5f);
            }
            this.fuseCooldown = 1;
            ++this.fuse;
            if (this.fuse == this.fuseDuration) {
                this.world.explode(this, this.x, this.y, this.z, 3.0f);
                this.remove();
            }
            this.stoppedMoving = true;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public float getFuse(float tickDelta) {
        return ((float)this.lastFuse + (float)(this.fuse - this.lastFuse) * tickDelta) / (float)(this.fuseDuration - 2);
    }

    protected int getDropItem() {
        return Item.GUNPOWDER.id;
    }
}

