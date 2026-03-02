/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class DoubleInventory
implements Inventory {
    private String name;
    private Inventory inventory1;
    private Inventory inventory2;

    public DoubleInventory(String name, Inventory inventory1, Inventory inventory2) {
        this.name = name;
        this.inventory1 = inventory1;
        this.inventory2 = inventory2;
    }

    public int getSize() {
        return this.inventory1.getSize() + this.inventory2.getSize();
    }

    @Environment(value=EnvType.CLIENT)
    public String getInventoryName() {
        return this.name;
    }

    public ItemStack getItem(int slot) {
        if (slot >= this.inventory1.getSize()) {
            return this.inventory2.getItem(slot - this.inventory1.getSize());
        }
        return this.inventory1.getItem(slot);
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= this.inventory1.getSize()) {
            return this.inventory2.removeItem(slot - this.inventory1.getSize(), amount);
        }
        return this.inventory1.removeItem(slot, amount);
    }

    @Environment(value=EnvType.CLIENT)
    public void setItem(int slot, ItemStack item) {
        if (slot >= this.inventory1.getSize()) {
            this.inventory2.setItem(slot - this.inventory1.getSize(), item);
        } else {
            this.inventory1.setItem(slot, item);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public int getMaxStackSize() {
        return this.inventory1.getMaxStackSize();
    }

    @Environment(value=EnvType.CLIENT)
    public void markDirty() {
        this.inventory1.markDirty();
        this.inventory2.markDirty();
    }
}

