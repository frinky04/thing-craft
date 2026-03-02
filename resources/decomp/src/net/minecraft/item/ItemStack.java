/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public final class ItemStack {
    public int size = 0;
    public int popAnimationTime;
    public int id;
    public int metadata;

    public ItemStack(Block block) {
        this(block, 1);
    }

    public ItemStack(Block block, int size) {
        this(block.id, size);
    }

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int size) {
        this(item.id, size);
    }

    public ItemStack(int id) {
        this(id, 1);
    }

    public ItemStack(int id, int size) {
        this.id = id;
        this.size = size;
    }

    public ItemStack(int id, int size, int metadata) {
        this.id = id;
        this.size = size;
        this.metadata = metadata;
    }

    public ItemStack(NbtCompound nbt) {
        this.readNbt(nbt);
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack split(int amount) {
        this.size -= amount;
        return new ItemStack(this.id, amount, this.metadata);
    }

    public Item getItem() {
        return Item.BY_ID[this.id];
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite() {
        return this.getItem().getSprite(this);
    }

    public boolean useOn(PlayerEntity player, World world, int x, int y, int z, int face) {
        return this.getItem().useOn(this, player, world, x, y, z, face);
    }

    public float getMiningSpeed(Block block) {
        return this.getItem().getMiningSpeed(this, block);
    }

    public ItemStack startUsing(World world, PlayerEntity player) {
        return this.getItem().startUsing(this, world, player);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putShort("id", (short)this.id);
        nbt.putByte("Count", (byte)this.size);
        nbt.putShort("Damage", (short)this.metadata);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        this.id = nbt.getShort("id");
        this.size = nbt.getByte("Count");
        this.metadata = nbt.getShort("Damage");
    }

    public int getMaxSize() {
        return this.getItem().getMaxStackSize();
    }

    public int getMaxDamage() {
        return Item.BY_ID[this.id].getMaxDamage();
    }

    public void takeDamageAndBreak(int amount) {
        this.metadata += amount;
        if (this.metadata > this.getMaxDamage()) {
            --this.size;
            if (this.size < 0) {
                this.size = 0;
            }
            this.metadata = 0;
        }
    }

    public void attack(MobEntity entity) {
        Item.BY_ID[this.id].attack(this, entity);
    }

    public void mineBlock(int x, int y, int z, int face) {
        Item.BY_ID[this.id].mineBlock(this, x, y, z, face);
    }

    public int getAttackDamage(Entity target) {
        return Item.BY_ID[this.id].getAttackDamage(target);
    }

    public boolean canMineBlock(Block block) {
        return Item.BY_ID[this.id].canMineBlock(block);
    }

    public void onRemoved(PlayerEntity player) {
    }

    @Environment(value=EnvType.CLIENT)
    public void interact(MobEntity entity) {
        Item.BY_ID[this.id].interact(this, entity);
    }

    public ItemStack copy() {
        return new ItemStack(this.id, this.size, this.metadata);
    }
}

