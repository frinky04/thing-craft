/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

public class GlowstoneBlock
extends Block {
    public GlowstoneBlock(int i, int j, Material material) {
        super(i, j, material);
    }

    public int getDropItem(int metadata, Random random) {
        return Item.GLOWSTONE_DUST.id;
    }
}

