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
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuSlot;
import net.minecraft.inventory.Inventory;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ChestScreen
extends InventoryMenuScreen {
    private Inventory playerInventory;
    private Inventory inventory;
    private int rows = 0;

    public ChestScreen(Inventory playerInventory, Inventory inventory) {
        int l;
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.passEvents = false;
        int i = 222;
        int j = i - 108;
        this.rows = inventory.getSize() / 9;
        this.backgroundHeight = j + this.rows * 18;
        int k = (this.rows - 4) * 18;
        for (l = 0; l < this.rows; ++l) {
            for (int m = 0; m < 9; ++m) {
                this.menuSlots.add(new InventoryMenuSlot(this, inventory, m + l * 9, 8 + m * 18, 18 + l * 18));
            }
        }
        for (l = 0; l < 3; ++l) {
            for (int n = 0; n < 9; ++n) {
                this.menuSlots.add(new InventoryMenuSlot(this, playerInventory, n + (l + 1) * 9, 8 + n * 18, 103 + l * 18 + k));
            }
        }
        for (l = 0; l < 9; ++l) {
            this.menuSlots.add(new InventoryMenuSlot(this, playerInventory, l, 8 + l * 18, 161 + k));
        }
    }

    protected void renderLabels() {
        this.textRenderer.draw(this.inventory.getInventoryName(), 8, 6, 0x404040);
        this.textRenderer.draw(this.playerInventory.getInventoryName(), 8, this.backgroundHeight - 96 + 2, 0x404040);
    }

    protected void renderMenuBackground(float tickDelta) {
        int i = this.minecraft.textureManager.load("/gui/container.png");
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.minecraft.textureManager.bind(i);
        int j = (this.width - this.backgroundWidth) / 2;
        int k = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(j, k, 0, 0, this.backgroundWidth, this.rows * 18 + 17);
        this.drawTexture(j, k + this.rows * 18 + 17, 0, 126, this.backgroundWidth, 96);
    }
}

