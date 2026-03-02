/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.gui.screen.inventory.menu;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuSlot;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.InventorySlot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public abstract class InventoryMenuScreen
extends Screen {
    private static ItemRenderer itemRenderer = new ItemRenderer();
    protected int backgroundWidth = 176;
    protected int backgroundHeight = 166;
    protected List menuSlots = new ArrayList();

    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.renderMenuBackground(tickDelta);
        GL11.glPushMatrix();
        GL11.glRotatef((float)180.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        Lighting.turnOn();
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glTranslatef((float)i, (float)j, (float)0.0f);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glEnable((int)32826);
        for (int k = 0; k < this.menuSlots.size(); ++k) {
            InventoryMenuSlot inventoryMenuSlot = (InventoryMenuSlot)this.menuSlots.get(k);
            this.renderSlot(inventoryMenuSlot);
            if (!inventoryMenuSlot.mouseClicked(mouseX, mouseY)) continue;
            GL11.glDisable((int)GL11.GL_LIGHTING);
            GL11.glDisable((int)GL11.GL_DEPTH_TEST);
            int l = inventoryMenuSlot.x;
            int m = inventoryMenuSlot.y;
            this.fillGradient(l, m, l + 16, m + 16, -2130706433, -2130706433);
            GL11.glEnable((int)GL11.GL_LIGHTING);
            GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        }
        PlayerInventory playerInventory = this.minecraft.player.inventory;
        if (playerInventory.cursorItem != null) {
            GL11.glTranslatef((float)0.0f, (float)0.0f, (float)32.0f);
            itemRenderer.renderGuiItem(this.textRenderer, this.minecraft.textureManager, playerInventory.cursorItem, mouseX - i - 8, mouseY - j - 8);
            itemRenderer.renderGuiItemDecoration(this.textRenderer, this.minecraft.textureManager, playerInventory.cursorItem, mouseX - i - 8, mouseY - j - 8);
        }
        GL11.glDisable((int)32826);
        Lighting.turnOff();
        GL11.glDisable((int)GL11.GL_LIGHTING);
        GL11.glDisable((int)GL11.GL_DEPTH_TEST);
        this.renderLabels();
        GL11.glEnable((int)GL11.GL_LIGHTING);
        GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    protected void renderLabels() {
    }

    protected abstract void renderMenuBackground(float var1);

    private void renderSlot(InventoryMenuSlot slot) {
        int l;
        Inventory inventory = slot.inventory;
        int i = slot.id;
        int j = slot.x;
        int k = slot.y;
        ItemStack itemStack = inventory.getItem(i);
        if (itemStack == null && (l = slot.getTexture()) >= 0) {
            GL11.glDisable((int)GL11.GL_LIGHTING);
            this.minecraft.textureManager.bind(this.minecraft.textureManager.load("/gui/items.png"));
            this.drawTexture(j, k, l % 16 * 16, l / 16 * 16, 16, 16);
            GL11.glEnable((int)GL11.GL_LIGHTING);
            return;
        }
        itemRenderer.renderGuiItem(this.textRenderer, this.minecraft.textureManager, itemStack, j, k);
        itemRenderer.renderGuiItemDecoration(this.textRenderer, this.minecraft.textureManager, itemStack, j, k);
    }

    private InventorySlot getSlot(int x, int y) {
        for (int i = 0; i < this.menuSlots.size(); ++i) {
            InventoryMenuSlot inventoryMenuSlot = (InventoryMenuSlot)this.menuSlots.get(i);
            if (!inventoryMenuSlot.mouseClicked(x, y)) continue;
            return inventoryMenuSlot;
        }
        return null;
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 || button == 1) {
            InventorySlot inventorySlot = this.getSlot(mouseX, mouseY);
            PlayerInventory playerInventory = this.minecraft.player.inventory;
            if (inventorySlot != null) {
                ItemStack itemStack = inventorySlot.getItem();
                if (itemStack != null || playerInventory.cursorItem != null) {
                    if (itemStack != null && playerInventory.cursorItem == null) {
                        int j = button == 0 ? itemStack.size : (itemStack.size + 1) / 2;
                        playerInventory.cursorItem = inventorySlot.inventory.removeItem(inventorySlot.id, j);
                        if (itemStack.size == 0) {
                            inventorySlot.setItem(null);
                        }
                        inventorySlot.onItemRemoved();
                    } else if (itemStack == null && playerInventory.cursorItem != null && inventorySlot.isItemAllowed(playerInventory.cursorItem)) {
                        int k;
                        int n = k = button == 0 ? playerInventory.cursorItem.size : 1;
                        if (k > inventorySlot.getMaxStackSize()) {
                            k = inventorySlot.getMaxStackSize();
                        }
                        inventorySlot.setItem(playerInventory.cursorItem.split(k));
                        if (playerInventory.cursorItem.size == 0) {
                            playerInventory.cursorItem = null;
                        }
                    } else if (itemStack != null && playerInventory.cursorItem != null) {
                        int n;
                        if (inventorySlot.isItemAllowed(playerInventory.cursorItem)) {
                            if (itemStack.id != playerInventory.cursorItem.id) {
                                if (playerInventory.cursorItem.size <= inventorySlot.getMaxStackSize()) {
                                    ItemStack itemStack2 = itemStack;
                                    inventorySlot.setItem(playerInventory.cursorItem);
                                    playerInventory.cursorItem = itemStack2;
                                }
                            } else if (itemStack.id == playerInventory.cursorItem.id) {
                                if (button == 0) {
                                    int l = playerInventory.cursorItem.size;
                                    if (l > inventorySlot.getMaxStackSize() - itemStack.size) {
                                        l = inventorySlot.getMaxStackSize() - itemStack.size;
                                    }
                                    if (l > playerInventory.cursorItem.getMaxSize() - itemStack.size) {
                                        l = playerInventory.cursorItem.getMaxSize() - itemStack.size;
                                    }
                                    playerInventory.cursorItem.split(l);
                                    if (playerInventory.cursorItem.size == 0) {
                                        playerInventory.cursorItem = null;
                                    }
                                    itemStack.size += l;
                                } else if (button == 1) {
                                    int m = 1;
                                    if (m > inventorySlot.getMaxStackSize() - itemStack.size) {
                                        m = inventorySlot.getMaxStackSize() - itemStack.size;
                                    }
                                    if (m > playerInventory.cursorItem.getMaxSize() - itemStack.size) {
                                        m = playerInventory.cursorItem.getMaxSize() - itemStack.size;
                                    }
                                    playerInventory.cursorItem.split(m);
                                    if (playerInventory.cursorItem.size == 0) {
                                        playerInventory.cursorItem = null;
                                    }
                                    itemStack.size += m;
                                }
                            }
                        } else if (itemStack.id == playerInventory.cursorItem.id && playerInventory.cursorItem.getMaxSize() > 1 && (n = itemStack.size) > 0 && n + playerInventory.cursorItem.size <= playerInventory.cursorItem.getMaxSize()) {
                            playerInventory.cursorItem.size += n;
                            itemStack.split(n);
                            if (itemStack.size == 0) {
                                inventorySlot.setItem(null);
                            }
                            inventorySlot.onItemRemoved();
                        }
                    }
                }
                inventorySlot.markDirty();
            } else if (playerInventory.cursorItem != null) {
                int i = (this.width - this.backgroundWidth) / 2;
                int o = (this.height - this.backgroundHeight) / 2;
                if (mouseX < i || mouseY < o || mouseX >= i + this.backgroundWidth || mouseY >= o + this.backgroundWidth) {
                    ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
                    if (button == 0) {
                        clientPlayerEntity.dropItem(playerInventory.cursorItem);
                        playerInventory.cursorItem = null;
                    }
                    if (button == 1) {
                        clientPlayerEntity.dropItem(playerInventory.cursorItem.split(1));
                        if (playerInventory.cursorItem.size == 0) {
                            playerInventory.cursorItem = null;
                        }
                    }
                }
            }
        }
    }

    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            // empty if block
        }
    }

    protected void keyPressed(char chr, int key) {
        if (key == 1 || key == this.minecraft.options.inventoryKey.keyCode) {
            this.minecraft.openScreen(null);
        }
    }

    public void removed() {
        if (this.minecraft.player == null) {
            return;
        }
        PlayerInventory playerInventory = this.minecraft.player.inventory;
        if (playerInventory.cursorItem != null) {
            this.minecraft.player.dropItem(playerInventory.cursorItem);
            playerInventory.cursorItem = null;
        }
    }

    public boolean isPauseScreen() {
        return false;
    }
}

