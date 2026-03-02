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
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenu;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class CraftingInventory
implements Inventory {
    private ItemStack[] items;
    private int size;
    private InventoryMenu menu;

    public CraftingInventory(InventoryMenu menu, int width, int height) {
        this.size = width * height;
        this.items = new ItemStack[this.size];
        this.menu = menu;
    }

    public CraftingInventory(InventoryMenu menu, ItemStack[] items) {
        this.size = items.length;
        this.items = items;
        this.menu = menu;
    }

    public int getSize() {
        return this.size;
    }

    public ItemStack getItem(int slot) {
        return this.items[slot];
    }

    public String getInventoryName() {
        return "Crafting";
    }

    public ItemStack removeItem(int slot, int amount) {
        if (this.items[slot] != null) {
            if (this.items[slot].size <= amount) {
                ItemStack itemStack = this.items[slot];
                this.items[slot] = null;
                this.menu.onContentsChanged(this);
                return itemStack;
            }
            ItemStack itemStack2 = this.items[slot].split(amount);
            if (this.items[slot].size == 0) {
                this.items[slot] = null;
            }
            this.menu.onContentsChanged(this);
            return itemStack2;
        }
        return null;
    }

    public void setItem(int slot, ItemStack item) {
        this.items[slot] = item;
        this.menu.onContentsChanged(this);
    }

    public int getMaxStackSize() {
        return 64;
    }

    public void markDirty() {
    }
}

