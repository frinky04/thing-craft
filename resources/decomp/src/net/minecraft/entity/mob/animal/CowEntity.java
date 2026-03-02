/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.animal;

import net.minecraft.entity.mob.animal.AnimalEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class CowEntity
extends AnimalEntity {
    public boolean f_12501010 = false;

    public CowEntity(World world) {
        super(world);
        this.texture = "/mob/cow.png";
        this.setSize(0.9f, 1.3f);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    protected String getAmbientSound() {
        return "mob.cow";
    }

    protected String getHurtSound() {
        return "mob.cowhurt";
    }

    protected String getDeathSound() {
        return "mob.cowhurt";
    }

    protected float getSoundVolume() {
        return 0.4f;
    }

    protected int getDropItem() {
        return Item.LEATHER.id;
    }

    public boolean interact(PlayerEntity player) {
        ItemStack itemStack = player.inventory.getSelectedItem();
        if (itemStack != null && itemStack.id == Item.BUCKET.id) {
            player.inventory.setItem(player.inventory.selectedSlot, new ItemStack(Item.MILK_BUCKET));
            return true;
        }
        return false;
    }
}

