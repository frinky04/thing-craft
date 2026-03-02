/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screen.inventory.menu;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.inventory.Inventory;

@Environment(value=EnvType.CLIENT)
public class InventoryMenu {
    protected List f_28417695 = new ArrayList();

    public void close(PlayerEntity player) {
        PlayerInventory playerInventory = player.inventory;
        if (playerInventory.cursorItem != null) {
            player.dropItem(playerInventory.cursorItem);
        }
    }

    public void onContentsChanged(Inventory inventory) {
    }
}

