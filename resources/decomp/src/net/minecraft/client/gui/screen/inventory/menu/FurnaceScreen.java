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
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuSlot;
import net.minecraft.entity.mob.player.PlayerInventory;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class FurnaceScreen
extends InventoryMenuScreen {
    private FurnaceBlockEntity furnace;

    public FurnaceScreen(PlayerInventory playerInventory, FurnaceBlockEntity furnace) {
        int i;
        this.furnace = furnace;
        this.menuSlots.add(new InventoryMenuSlot(this, furnace, 0, 56, 17));
        this.menuSlots.add(new InventoryMenuSlot(this, furnace, 1, 56, 53));
        this.menuSlots.add(new InventoryMenuSlot(this, furnace, 2, 116, 35));
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.menuSlots.add(new InventoryMenuSlot(this, playerInventory, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.menuSlots.add(new InventoryMenuSlot(this, playerInventory, i, 8 + i * 18, 142));
        }
    }

    protected void renderLabels() {
        this.textRenderer.draw("Furnace", 60, 6, 0x404040);
        this.textRenderer.draw("Inventory", 8, this.backgroundHeight - 96 + 2, 0x404040);
    }

    protected void renderMenuBackground(float tickDelta) {
        int i = this.minecraft.textureManager.load("/gui/furnace.png");
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.minecraft.textureManager.bind(i);
        int j = (this.width - this.backgroundWidth) / 2;
        int k = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(j, k, 0, 0, this.backgroundWidth, this.backgroundHeight);
        if (this.furnace.hasFuel()) {
            int l = this.furnace.getLitProgress(12);
            this.drawTexture(j + 56, k + 36 + 12 - l, 176, 12 - l, 14, l + 2);
        }
        int m = this.furnace.getCookProgress(24);
        this.drawTexture(j + 79, k + 34, 176, 14, m + 1, 16);
    }
}

