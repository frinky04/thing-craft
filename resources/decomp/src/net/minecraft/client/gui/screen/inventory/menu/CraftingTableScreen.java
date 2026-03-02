/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.screen.inventory.menu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.inventory.menu.CraftingResultSlot;
import net.minecraft.client.gui.screen.inventory.menu.CraftingTableMenu;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuSlot;
import net.minecraft.entity.mob.player.PlayerInventory;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class CraftingTableScreen
extends InventoryMenuScreen {
    public CraftingTableMenu menu = new CraftingTableMenu();

    public CraftingTableScreen(PlayerInventory playerInventory) {
        int i;
        this.menuSlots.add(new CraftingResultSlot(this, this.menu.inventory, this.menu.resultInventory, 0, 124, 35));
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.menuSlots.add(new InventoryMenuSlot(this, this.menu.inventory, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (int k = 0; k < 9; ++k) {
                this.menuSlots.add(new InventoryMenuSlot(this, playerInventory, k + (i + 1) * 9, 8 + k * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.menuSlots.add(new InventoryMenuSlot(this, playerInventory, i, 8 + i * 18, 142));
        }
    }

    public void removed() {
        super.removed();
        this.menu.close(this.minecraft.player);
    }

    protected void renderLabels() {
        this.textRenderer.draw("Crafting", 28, 6, 0x404040);
        this.textRenderer.draw("Inventory", 8, this.backgroundHeight - 96 + 2, 0x404040);
    }

    protected void renderMenuBackground(float tickDelta) {
        int i = this.minecraft.textureManager.load("/gui/crafting.png");
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.minecraft.textureManager.bind(i);
        int j = (this.width - this.backgroundWidth) / 2;
        int k = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(j, k, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}

