/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.crafting.ArmorRecipes;
import net.minecraft.crafting.BlockRecipes;
import net.minecraft.crafting.FoodRecipes;
import net.minecraft.crafting.MineralRecipes;
import net.minecraft.crafting.Recipe;
import net.minecraft.crafting.ToolRecipes;
import net.minecraft.crafting.WeaponRecipes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class CraftingManager {
    private static final CraftingManager INSTANCE = new CraftingManager();
    private List recipes = new ArrayList();

    public static final CraftingManager getInstance() {
        return INSTANCE;
    }

    private CraftingManager() {
        new ToolRecipes().register(this);
        new WeaponRecipes().register(this);
        new MineralRecipes().register(this);
        new FoodRecipes().register(this);
        new BlockRecipes().register(this);
        new ArmorRecipes().register(this);
        this.registerShaped(new ItemStack(Item.PAPER, 3), "###", Character.valueOf('#'), Item.REEDS);
        this.registerShaped(new ItemStack(Item.BOOK, 1), "#", "#", "#", Character.valueOf('#'), Item.PAPER);
        this.registerShaped(new ItemStack(Block.FENCE, 2), "###", "###", Character.valueOf('#'), Item.STICK);
        this.registerShaped(new ItemStack(Block.JUKEBOX, 1), "###", "#X#", "###", Character.valueOf('#'), Block.PLANKS, Character.valueOf('X'), Item.DIAMOND);
        this.registerShaped(new ItemStack(Block.BOOKSHELF, 1), "###", "XXX", "###", Character.valueOf('#'), Block.PLANKS, Character.valueOf('X'), Item.BOOK);
        this.registerShaped(new ItemStack(Block.SNOW, 1), "##", "##", Character.valueOf('#'), Item.SNOWBALL);
        this.registerShaped(new ItemStack(Block.CLAY, 1), "##", "##", Character.valueOf('#'), Item.CLAY);
        this.registerShaped(new ItemStack(Block.BRICKS, 1), "##", "##", Character.valueOf('#'), Item.BRICK);
        this.registerShaped(new ItemStack(Block.GLOWSTONE, 1), "###", "###", "###", Character.valueOf('#'), Item.GLOWSTONE_DUST);
        this.registerShaped(new ItemStack(Block.WOOL, 1), "###", "###", "###", Character.valueOf('#'), Item.STRING);
        this.registerShaped(new ItemStack(Block.TNT, 1), "X#X", "#X#", "X#X", Character.valueOf('X'), Item.GUNPOWDER, Character.valueOf('#'), Block.SAND);
        this.registerShaped(new ItemStack(Block.STONE_SLAB, 3), "###", Character.valueOf('#'), Block.COBBLESTONE);
        this.registerShaped(new ItemStack(Block.LADDER, 1), "# #", "###", "# #", Character.valueOf('#'), Item.STICK);
        this.registerShaped(new ItemStack(Item.WOODEN_DOOR, 1), "##", "##", "##", Character.valueOf('#'), Block.PLANKS);
        this.registerShaped(new ItemStack(Item.IRON_DOOR, 1), "##", "##", "##", Character.valueOf('#'), Item.IRON_INGOT);
        this.registerShaped(new ItemStack(Item.SIGN, 1), "###", "###", " X ", Character.valueOf('#'), Block.PLANKS, Character.valueOf('X'), Item.STICK);
        this.registerShaped(new ItemStack(Block.PLANKS, 4), "#", Character.valueOf('#'), Block.LOG);
        this.registerShaped(new ItemStack(Item.STICK, 4), "#", "#", Character.valueOf('#'), Block.PLANKS);
        this.registerShaped(new ItemStack(Block.TORCH, 4), "X", "#", Character.valueOf('X'), Item.COAL, Character.valueOf('#'), Item.STICK);
        this.registerShaped(new ItemStack(Item.BOWL, 4), "# #", " # ", Character.valueOf('#'), Block.PLANKS);
        this.registerShaped(new ItemStack(Block.RAIL, 16), "X X", "X#X", "X X", Character.valueOf('X'), Item.IRON_INGOT, Character.valueOf('#'), Item.STICK);
        this.registerShaped(new ItemStack(Item.MINECART, 1), "# #", "###", Character.valueOf('#'), Item.IRON_INGOT);
        this.registerShaped(new ItemStack(Block.LIT_PUMPKIN, 1), "A", "B", Character.valueOf('A'), Block.PUMPKIN, Character.valueOf('B'), Block.TORCH);
        this.registerShaped(new ItemStack(Item.CHEST_MINECART, 1), "A", "B", Character.valueOf('A'), Block.CHEST, Character.valueOf('B'), Item.MINECART);
        this.registerShaped(new ItemStack(Item.FURNACE_MINECART, 1), "A", "B", Character.valueOf('A'), Block.FURNACE, Character.valueOf('B'), Item.MINECART);
        this.registerShaped(new ItemStack(Item.BOAT, 1), "# #", "###", Character.valueOf('#'), Block.PLANKS);
        this.registerShaped(new ItemStack(Item.BUCKET, 1), "# #", " # ", Character.valueOf('#'), Item.IRON_INGOT);
        this.registerShaped(new ItemStack(Item.FLINT_AND_STEEL, 1), "A ", " B", Character.valueOf('A'), Item.IRON_INGOT, Character.valueOf('B'), Item.FLINT);
        this.registerShaped(new ItemStack(Item.BREAD, 1), "###", Character.valueOf('#'), Item.WHEAT);
        this.registerShaped(new ItemStack(Block.OAK_STAIRS, 4), "#  ", "## ", "###", Character.valueOf('#'), Block.PLANKS);
        this.registerShaped(new ItemStack(Item.FISHING_ROD, 1), "  #", " #X", "# X", Character.valueOf('#'), Item.STICK, Character.valueOf('X'), Item.STRING);
        this.registerShaped(new ItemStack(Block.STONE_STAIRS, 4), "#  ", "## ", "###", Character.valueOf('#'), Block.COBBLESTONE);
        this.registerShaped(new ItemStack(Item.PAINTING, 1), "###", "#X#", "###", Character.valueOf('#'), Item.STICK, Character.valueOf('X'), Block.WOOL);
        this.registerShaped(new ItemStack(Item.GOLDEN_APPLE, 1), "###", "#X#", "###", Character.valueOf('#'), Block.GOLD_BLOCK, Character.valueOf('X'), Item.APPLE);
        this.registerShaped(new ItemStack(Block.LEVER, 1), "X", "#", Character.valueOf('#'), Block.COBBLESTONE, Character.valueOf('X'), Item.STICK);
        this.registerShaped(new ItemStack(Block.REDSTONE_TORCH, 1), "X", "#", Character.valueOf('#'), Item.STICK, Character.valueOf('X'), Item.REDSTONE);
        this.registerShaped(new ItemStack(Item.CLOCK, 1), " # ", "#X#", " # ", Character.valueOf('#'), Item.GOLD_INGOT, Character.valueOf('X'), Item.REDSTONE);
        this.registerShaped(new ItemStack(Item.COMPASS, 1), " # ", "#X#", " # ", Character.valueOf('#'), Item.IRON_INGOT, Character.valueOf('X'), Item.REDSTONE);
        this.registerShaped(new ItemStack(Block.STONE_BUTTON, 1), "#", "#", Character.valueOf('#'), Block.STONE);
        this.registerShaped(new ItemStack(Block.STONE_PRESSURE_PLATE, 1), "###", Character.valueOf('#'), Block.STONE);
        this.registerShaped(new ItemStack(Block.WOODEN_PRESSURE_PLATE, 1), "###", Character.valueOf('#'), Block.PLANKS);
        Collections.sort(this.recipes, new Comparator(){

            public int compare(Recipe recipe, Recipe recipe2) {
                if (recipe2.size() < recipe.size()) {
                    return -1;
                }
                if (recipe2.size() > recipe.size()) {
                    return 1;
                }
                return 0;
            }
        });
        System.out.println(this.recipes.size() + " recipes");
    }

    void registerShaped(ItemStack result, Object ... args) {
        String string = "";
        int i = 0;
        int j = 0;
        int k = 0;
        if (args[i] instanceof String[]) {
            String[] strings = (String[])args[i++];
            for (int l = 0; l < strings.length; ++l) {
                String string3 = strings[l];
                ++k;
                j = string3.length();
                string = string + string3;
            }
        } else {
            while (args[i] instanceof String) {
                String string2 = (String)args[i++];
                ++k;
                j = string2.length();
                string = string + string2;
            }
        }
        HashMap<Character, Integer> hashMap = new HashMap<Character, Integer>();
        while (i < args.length) {
            Character character = (Character)args[i];
            int m = 0;
            if (args[i + 1] instanceof Item) {
                m = ((Item)args[i + 1]).id;
            } else if (args[i + 1] instanceof Block) {
                m = ((Block)args[i + 1]).id;
            }
            hashMap.put(character, m);
            i += 2;
        }
        int[] is = new int[j * k];
        for (int n = 0; n < j * k; ++n) {
            char o = string.charAt(n);
            is[n] = hashMap.containsKey(Character.valueOf(o)) ? (Integer)hashMap.get(Character.valueOf(o)) : -1;
        }
        this.recipes.add(new Recipe(j, k, is, result));
    }

    public ItemStack getResult(int[] inventory) {
        for (int i = 0; i < this.recipes.size(); ++i) {
            Recipe recipe = (Recipe)this.recipes.get(i);
            if (!recipe.matches(inventory)) continue;
            return recipe.getResult(inventory);
        }
        return null;
    }
}

