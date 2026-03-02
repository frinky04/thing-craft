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
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.InventorySlot;

@Environment(value=EnvType.CLIENT)
public class InventoryMenuSlot
extends InventorySlot {
    private final InventoryMenuScreen menuScreen;
    public final int x;
    public final int y;

    public InventoryMenuSlot(InventoryMenuScreen menuScreen, Inventory inventory, int id, int x, int y) {
        super(inventory, id);
        this.menuScreen = menuScreen;
        this.x = x;
        this.y = y;
    }

    public boolean mouseClicked(int mouseX, int mouseY) {
        int i = (this.menuScreen.width - this.menuScreen.backgroundWidth) / 2;
        int j = (this.menuScreen.height - this.menuScreen.backgroundHeight) / 2;
        return (mouseX -= i) >= this.x - 1 && mouseX < this.x + 16 + 1 && (mouseY -= j) >= this.y - 1 && mouseY < this.y + 16 + 1;
    }
}

