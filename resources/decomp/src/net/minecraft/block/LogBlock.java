/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class LogBlock
extends Block {
    protected LogBlock(int id) {
        super(id, Material.WOOD);
        this.sprite = 20;
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }

    public int getDropItem(int metadata, Random random) {
        return Block.LOG.id;
    }

    public int getSprite(int face) {
        if (face == 1) {
            return 21;
        }
        if (face == 0) {
            return 21;
        }
        return 20;
    }
}

