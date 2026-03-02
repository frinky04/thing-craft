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

@Environment(value=EnvType.CLIENT)
public class ResultInventory
implements Inventory {
    private ItemStack[] items = new ItemStack[1];

    public int getSize() {
        return 1;
    }

    public ItemStack getItem(int slot) {
        return this.items[slot];
    }

    public String getInventoryName() {
        return "Result";
    }

    public ItemStack removeItem(int slot, int amount) {
        if (this.items[slot] != null) {
            ItemStack itemStack = this.items[slot];
            this.items[slot] = null;
            return itemStack;
        }
        return null;
    }

    public void setItem(int slot, ItemStack item) {
        this.items[slot] = item;
    }

    public int getMaxStackSize() {
        return 64;
    }

    public void markDirty() {
    }
}

