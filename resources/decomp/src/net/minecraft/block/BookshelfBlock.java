/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BookshelfBlock
extends Block {
    public BookshelfBlock(int id, int sprite) {
        super(id, sprite, Material.WOOD);
    }

    public int getSprite(int face) {
        if (face <= 1) {
            return 4;
        }
        return this.sprite;
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }
}

