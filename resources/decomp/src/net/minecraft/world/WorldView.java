/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.world.biome.source.BiomeSource;

public interface WorldView {
    public int getBlock(int var1, int var2, int var3);

    @Environment(value=EnvType.CLIENT)
    public BlockEntity getBlockEntity(int var1, int var2, int var3);

    @Environment(value=EnvType.CLIENT)
    public float getBrightness(int var1, int var2, int var3);

    public int getBlockMetadata(int var1, int var2, int var3);

    public Material getMaterial(int var1, int var2, int var3);

    public boolean isSolidBlock(int var1, int var2, int var3);

    @Environment(value=EnvType.CLIENT)
    public BiomeSource getBiomeSource();
}

