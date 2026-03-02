/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.animal;

import net.minecraft.entity.mob.animal.AnimalEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class PigEntity
extends AnimalEntity {
    public boolean saddled = false;

    public PigEntity(World world) {
        super(world);
        this.texture = "/mob/pig.png";
        this.setSize(0.9f, 0.9f);
        this.saddled = false;
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
        nbt.putBoolean("Saddle", this.saddled);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
        this.saddled = nbt.getBoolean("Saddle");
    }

    protected String getAmbientSound() {
        return "mob.pig";
    }

    protected String getHurtSound() {
        return "mob.pig";
    }

    protected String getDeathSound() {
        return "mob.pigdeath";
    }

    public boolean interact(PlayerEntity player) {
        if (this.saddled) {
            player.startRiding(this);
            return true;
        }
        return false;
    }

    protected int getDropItem() {
        return Item.PORKCHOP.id;
    }
}

