/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.material.Material;

public class GlassBlock
extends TransparentBlock {
    public GlassBlock(int i, int j, Material material, boolean bl) {
        super(i, j, material, bl);
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }
}

