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
public class ArmorRecipes {
    private String[][] patterns = new String[][]{{"XXX", "X X"}, {"X X", "XXX", "XXX"}, {"XXX", "X X", "X X"}, {"X X", "X X"}};
    private Object[][] recipes = new Object[][]{{Item.LEATHER, Block.FIRE, Item.IRON_INGOT, Item.DIAMOND, Item.GOLD_INGOT}, {Item.LEATHER_HELMET, Item.CHAINMAIL_HELMET, Item.IRON_HELMET, Item.DIAMOND_HELMET, Item.GOLDEN_HELMET}, {Item.LEATHER_CHESTPLATE, Item.CHAINMAIL_CHESTPLATE, Item.IRON_CHESTPLATE, Item.DIAMOND_CHESTPLATE, Item.GOLDEN_CHESTPLATE}, {Item.LEATHER_LEGGINGS, Item.CHAINMAIL_LEGGINGS, Item.IRON_LEGGINGS, Item.DIAMOND_LEGGINGS, Item.GOLDEN_LEGGINGS}, {Item.LEATHER_BOOTS, Item.CHAINMAIL_BOOTS, Item.IRON_BOOTS, Item.DIAMOND_BOOTS, Item.GOLDEN_BOOTS}};

    public void register(CraftingManager manager) {
        for (int i = 0; i < this.recipes[0].length; ++i) {
            Object object = this.recipes[0][i];
            for (int j = 0; j < this.recipes.length - 1; ++j) {
                Item item = (Item)this.recipes[j + 1][i];
                manager.registerShaped(new ItemStack(item), this.patterns[j], Character.valueOf('X'), object);
            }
        }
    }
}

