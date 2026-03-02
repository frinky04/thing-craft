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
public class FoodRecipes {
    public void register(CraftingManager manager) {
        manager.registerShaped(new ItemStack(Item.MUSHROOM_STEW), "Y", "X", "#", Character.valueOf('X'), Block.BROWN_MUSHROOM, Character.valueOf('Y'), Block.RED_MUSHROOM, Character.valueOf('#'), Item.BOWL);
        manager.registerShaped(new ItemStack(Item.MUSHROOM_STEW), "Y", "X", "#", Character.valueOf('X'), Block.RED_MUSHROOM, Character.valueOf('Y'), Block.BROWN_MUSHROOM, Character.valueOf('#'), Item.BOWL);
    }
}

