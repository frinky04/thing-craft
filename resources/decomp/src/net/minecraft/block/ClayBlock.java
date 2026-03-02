/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

public class ClayBlock
extends Block {
    public ClayBlock(int id, int sprite) {
        super(id, sprite, Material.CLAY);
    }

    public int getDropItem(int metadata, Random random) {
        return Item.CLAY.id;
    }

    public int getBaseDropCount(Random random) {
        return 4;
    }
}

