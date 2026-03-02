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
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class BlockRecipes {
    public void register(CraftingManager manager) {
        manager.registerShaped(new ItemStack(Block.CHEST), "###", "# #", "###", Character.valueOf('#'), Block.PLANKS);
        manager.registerShaped(new ItemStack(Block.FURNACE), "###", "# #", "###", Character.valueOf('#'), Block.COBBLESTONE);
        manager.registerShaped(new ItemStack(Block.CRAFTING_TABLE), "##", "##", Character.valueOf('#'), Block.PLANKS);
    }
}

