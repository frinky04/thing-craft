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
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenu;
import net.minecraft.crafting.CraftingManager;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.ResultInventory;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class PlayerMenu
extends InventoryMenu {
    public CraftingInventory craftingInventory;
    public Inventory resultInventory = new ResultInventory();

    public PlayerMenu(ItemStack[] items) {
        this.craftingInventory = new CraftingInventory(this, items);
        this.onContentsChanged(this.craftingInventory);
    }

    public void onContentsChanged(Inventory inventory) {
        int[] is = new int[9];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                ItemStack itemStack;
                int k = -1;
                if (i < 2 && j < 2 && (itemStack = this.craftingInventory.getItem(i + j * 2)) != null) {
                    k = itemStack.id;
                }
                is[i + j * 3] = k;
            }
        }
        this.resultInventory.setItem(0, CraftingManager.getInstance().getResult(is));
    }

    public void close(PlayerEntity player) {
        super.close(player);
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = this.craftingInventory.getItem(i);
            if (itemStack == null) continue;
            player.dropItem(itemStack);
        }
    }
}

