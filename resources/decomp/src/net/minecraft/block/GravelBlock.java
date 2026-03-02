/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.Item;

public class GravelBlock
extends FallingBlock {
    public GravelBlock(int i, int j) {
        super(i, j);
    }

    public int getDropItem(int metadata, Random random) {
        if (random.nextInt(10) == 0) {
            return Item.FLINT.id;
        }
        return this.id;
    }
}

