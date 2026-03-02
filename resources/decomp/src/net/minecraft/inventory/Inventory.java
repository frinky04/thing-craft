/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

public interface Inventory {
    public int getSize();

    public ItemStack getItem(int var1);

    @Environment(value=EnvType.CLIENT)
    public ItemStack removeItem(int var1, int var2);

    @Environment(value=EnvType.CLIENT)
    public void setItem(int var1, ItemStack var2);

    @Environment(value=EnvType.CLIENT)
    public String getInventoryName();

    @Environment(value=EnvType.CLIENT)
    public int getMaxStackSize();

    @Environment(value=EnvType.CLIENT)
    public void markDirty();
}

