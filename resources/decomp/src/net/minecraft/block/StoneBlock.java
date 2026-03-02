/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class StoneBlock
extends Block {
    public StoneBlock(int id, int sprite) {
        super(id, sprite, Material.STONE);
    }

    public int getDropItem(int metadata, Random random) {
        return Block.COBBLESTONE.id;
    }
}

