/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.monster;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PathFinderMobEntity;
import net.minecraft.entity.mob.monster.Monster;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class MonsterEntity
extends PathFinderMobEntity
implements Monster {
    protected int attackDamage = 2;

    public MonsterEntity(World world) {
        super(world);
        this.health = 20;
    }

    public void mobTick() {
        float f = this.getBrightness(1.0f);
        if (f > 0.5f) {
            this.farFromPlayerTicks += 2;
        }
        super.mobTick();
    }

    public void tick() {
        super.tick();
        if (this.world.difficulty == 0) {
            this.remove();
        }
    }

    protected Entity findTarget() {
        PlayerEntity playerEntity = this.world.getNearestPlayer(this, 16.0);
        if (playerEntity != null && this.canSee(playerEntity)) {
            return playerEntity;
        }
        return null;
    }

    public boolean takeDamage(Entity source, int amount) {
        if (super.takeDamage(source, amount)) {
            if (this.rider == source || this.vehicle == source) {
                return true;
            }
            if (source != this) {
                this.walkTarget = source;
            }
            return true;
        }
        return false;
    }

    protected void targetInSight(Entity target, float distance) {
        if ((double)distance < 2.5 && target.shape.maxY > this.shape.minY && target.shape.minY < this.shape.maxY) {
            this.attackTimer = 20;
            target.takeDamage(this, this.attackDamage);
        }
    }

    protected float getPathfindingFavor(int x, int y, int z) {
        return 0.5f - this.world.getBrightness(x, y, z);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    public boolean canSpawn() {
        int k;
        int j;
        int i = MathHelper.floor(this.x);
        if (this.world.getLight(LightType.SKY, i, j = MathHelper.floor(this.shape.minY), k = MathHelper.floor(this.z)) > this.random.nextInt(32)) {
            return false;
        }
        int l = this.world.getRawBrightness(i, j, k);
        return l <= this.random.nextInt(8) && super.canSpawn();
    }
}

