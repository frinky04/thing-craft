/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.StoneBlock;

public class ObsidianBlock
extends StoneBlock {
    public ObsidianBlock(int i, int j) {
        super(i, j);
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }

    public int getDropItem(int metadata, Random random) {
        return Block.OBSIDIAN.id;
    }
}

