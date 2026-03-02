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
public class WeaponRecipes {
    private String[][] patterns = new String[][]{{"X", "X", "#"}};
    private Object[][] recipes = new Object[][]{{Block.PLANKS, Block.COBBLESTONE, Item.IRON_INGOT, Item.DIAMOND, Item.GOLD_INGOT}, {Item.WOODEN_SWORD, Item.STONE_SWORD, Item.IRON_SWORD, Item.DIAMOND_SWORD, Item.GOLDEN_SWORD}};

    public void register(CraftingManager manager) {
        for (int i = 0; i < this.recipes[0].length; ++i) {
            Object object = this.recipes[0][i];
            for (int j = 0; j < this.recipes.length - 1; ++j) {
                Item item = (Item)this.recipes[j + 1][i];
                manager.registerShaped(new ItemStack(item), this.patterns[j], Character.valueOf('#'), Item.STICK, Character.valueOf('X'), object);
            }
        }
        manager.registerShaped(new ItemStack(Item.BOW, 1), " #X", "# X", " #X", Character.valueOf('X'), Item.STRING, Character.valueOf('#'), Item.STICK);
        manager.registerShaped(new ItemStack(Item.ARROW, 4), "X", "#", "Y", Character.valueOf('Y'), Item.FEATHER, Character.valueOf('X'), Item.FLINT, Character.valueOf('#'), Item.STICK);
    }
}

