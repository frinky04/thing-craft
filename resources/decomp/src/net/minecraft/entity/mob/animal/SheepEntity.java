/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.animal;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.animal.AnimalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class SheepEntity
extends AnimalEntity {
    public boolean sheared = false;

    public SheepEntity(World world) {
        super(world);
        this.texture = "/mob/sheep.png";
        this.setSize(0.9f, 1.3f);
    }

    public boolean takeDamage(Entity source, int amount) {
        if (!this.world.isMultiplayer && !this.sheared && source instanceof MobEntity) {
            this.sheared = true;
            int i = 1 + this.random.nextInt(3);
            for (int j = 0; j < i; ++j) {
                ItemEntity itemEntity = this.dropItem(Block.WOOL.id, 1, 1.0f);
                itemEntity.velocityY += (double)(this.random.nextFloat() * 0.05f);
                itemEntity.velocityX += (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1f);
                itemEntity.velocityZ += (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1f);
            }
        }
        return super.takeDamage(source, amount);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
        nbt.putBoolean("Sheared", this.sheared);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
        this.sheared = nbt.getBoolean("Sheared");
    }

    protected String getAmbientSound() {
        return "mob.sheep";
    }

    protected String getHurtSound() {
        return "mob.sheep";
    }

    protected String getDeathSound() {
        return "mob.sheep";
    }
}

