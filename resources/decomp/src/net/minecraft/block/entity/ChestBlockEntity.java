/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class ChestBlockEntity
extends BlockEntity
implements Inventory {
    private ItemStack[] inventory = new ItemStack[36];

    public int getSize() {
        return 27;
    }

    public ItemStack getItem(int slot) {
        return this.inventory[slot];
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack removeItem(int slot, int amount) {
        if (this.inventory[slot] != null) {
            if (this.inventory[slot].size <= amount) {
                ItemStack itemStack = this.inventory[slot];
                this.inventory[slot] = null;
                this.markDirty();
                return itemStack;
            }
            ItemStack itemStack2 = this.inventory[slot].split(amount);
            if (this.inventory[slot].size == 0) {
                this.inventory[slot] = null;
            }
            this.markDirty();
            return itemStack2;
        }
        return null;
    }

    public void setItem(int slot, ItemStack item) {
        this.inventory[slot] = item;
        if (item != null && item.size > this.getMaxStackSize()) {
            item.size = this.getMaxStackSize();
        }
        this.markDirty();
    }

    @Environment(value=EnvType.CLIENT)
    public String getInventoryName() {
        return "Chest";
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        NbtList nbtList = nbt.getList("Items");
        this.inventory = new ItemStack[this.getSize()];
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = (NbtCompound)nbtList.get(i);
            int j = nbtCompound.getByte("Slot") & 0xFF;
            if (j < 0 || j >= this.inventory.length) continue;
            this.inventory[j] = new ItemStack(nbtCompound);
        }
    }

    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList nbtList = new NbtList();
        for (int i = 0; i < this.inventory.length; ++i) {
            if (this.inventory[i] == null) continue;
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)i);
            this.inventory[i].writeNbt(nbtCompound);
            nbtList.addElement(nbtCompound);
        }
        nbt.put("Items", nbtList);
    }

    public int getMaxStackSize() {
        return 64;
    }
}

