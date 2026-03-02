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
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.inventory.menu.CraftingResultSlot;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuSlot;
import net.minecraft.client.gui.screen.inventory.menu.PlayerMenu;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class SurvivalInventoryScreen
extends InventoryMenuScreen {
    private PlayerMenu menu;
    private float mouseX;
    private float mouseY;

    public SurvivalInventoryScreen(Inventory inventory, ItemStack[] craftingSlots) {
        int i;
        this.passEvents = true;
        this.menu = new PlayerMenu(craftingSlots);
        this.menuSlots.add(new CraftingResultSlot(this, this.menu.craftingInventory, this.menu.resultInventory, 0, 144, 36));
        for (i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
                this.menuSlots.add(new InventoryMenuSlot(this, this.menu.craftingInventory, j + i * 2, 88 + j * 18, 26 + i * 18));
            }
        }
        for (i = 0; i < 4; ++i) {
            final int k = i;
            this.menuSlots.add(new InventoryMenuSlot(this, inventory, inventory.getSize() - 1 - i, 8, 8 + i * 18){

                public int getMaxStackSize() {
                    return 1;
                }

                public boolean isItemAllowed(ItemStack item) {
                    if (item.getItem() instanceof ArmorItem) {
                        return ((ArmorItem)item.getItem()).slot == k;
                    }
                    System.out.println(item.getItem().id + ", " + k);
                    if (item.getItem().id == Block.PUMPKIN.id) {
                        return k == 0;
                    }
                    return false;
                }

                public int getTexture() {
                    return 15 + k * 16;
                }
            });
        }
        for (i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.menuSlots.add(new InventoryMenuSlot(this, inventory, l + (i + 1) * 9, 8 + l * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.menuSlots.add(new InventoryMenuSlot(this, inventory, i, 8 + i * 18, 142));
        }
    }

    protected void renderLabels() {
        this.textRenderer.draw("Crafting", 86, 16, 0x404040);
    }

    public void render(int mouseX, int mouseY, float tickDelta) {
        super.render(mouseX, mouseY, tickDelta);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    protected void renderMenuBackground(float tickDelta) {
        int i = this.minecraft.textureManager.load("/gui/inventory.png");
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.minecraft.textureManager.bind(i);
        int j = (this.width - this.backgroundWidth) / 2;
        int k = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(j, k, 0, 0, this.backgroundWidth, this.backgroundHeight);
        GL11.glEnable((int)32826);
        GL11.glEnable((int)GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)(j + 51), (float)(k + 75), (float)50.0f);
        float f = 30.0f;
        GL11.glScalef((float)(-f), (float)f, (float)f);
        GL11.glRotatef((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        float g = this.minecraft.player.bodyYaw;
        float h = this.minecraft.player.yaw;
        float l = this.minecraft.player.pitch;
        float m = (float)(j + 51) - this.mouseX;
        float n = (float)(k + 75 - 50) - this.mouseY;
        GL11.glRotatef((float)135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        Lighting.turnOn();
        GL11.glRotatef((float)-135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(-((float)Math.atan(n / 40.0f)) * 20.0f), (float)1.0f, (float)0.0f, (float)0.0f);
        this.minecraft.player.bodyYaw = (float)Math.atan(m / 40.0f) * 20.0f;
        this.minecraft.player.yaw = (float)Math.atan(m / 40.0f) * 40.0f;
        this.minecraft.player.pitch = -((float)Math.atan(n / 40.0f)) * 20.0f;
        GL11.glTranslatef((float)0.0f, (float)this.minecraft.player.eyeHeight, (float)0.0f);
        EntityRenderDispatcher.INSTANCE.render(this.minecraft.player, 0.0, 0.0, 0.0, 0.0f, 1.0f);
        this.minecraft.player.bodyYaw = g;
        this.minecraft.player.yaw = h;
        this.minecraft.player.pitch = l;
        GL11.glPopMatrix();
        Lighting.turnOff();
        GL11.glDisable((int)32826);
    }
}

