/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screen.inventory.menu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuSlot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class CraftingResultSlot
extends InventoryMenuSlot {
    private final Inventory craftingInventory;

    public CraftingResultSlot(InventoryMenuScreen screen, Inventory craftingInventory, Inventory inventory, int id, int x, int y) {
        super(screen, inventory, id, x, y);
        this.craftingInventory = craftingInventory;
    }

    public boolean isItemAllowed(ItemStack item) {
        return false;
    }

    public void onItemRemoved() {
        for (int i = 0; i < this.craftingInventory.getSize(); ++i) {
            if (this.craftingInventory.getItem(i) == null) continue;
            this.craftingInventory.removeItem(i, 1);
        }
    }
}

