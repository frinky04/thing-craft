/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.crafting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.crafting.CraftingManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class MineralRecipes {
    private Object[][] recipes = new Object[][]{{Block.GOLD_BLOCK, Item.GOLD_INGOT}, {Block.IRON_BLOCK, Item.IRON_INGOT}, {Block.DIAMOND_BLOCK, Item.DIAMOND}};

    public void register(CraftingManager manager) {
        for (int i = 0; i < this.recipes.length; ++i) {
            Block block = (Block)this.recipes[i][0];
            Item item = (Item)this.recipes[i][1];
            manager.registerShaped(new ItemStack(block), "###", "###", "###", Character.valueOf('#'), item);
            manager.registerShaped(new ItemStack(item, 9), "#", Character.valueOf('#'), block);
        }
    }
}

