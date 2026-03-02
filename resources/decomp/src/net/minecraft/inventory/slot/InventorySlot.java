/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.inventory.slot;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class InventorySlot {
    public final int id;
    public final Inventory inventory;

    public InventorySlot(Inventory inventory, int id) {
        this.inventory = inventory;
        this.id = id;
    }

    public void onItemRemoved() {
        this.markDirty();
    }

    public boolean isItemAllowed(ItemStack item) {
        return true;
    }

    public ItemStack getItem() {
        return this.inventory.getItem(this.id);
    }

    public void setItem(ItemStack item) {
        this.inventory.setItem(this.id, item);
        this.markDirty();
    }

    public int getTexture() {
        return -1;
    }

    public void markDirty() {
        this.inventory.markDirty();
    }

    public int getMaxStackSize() {
        return this.inventory.getMaxStackSize();
    }
}

