/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

public class OreBlock
extends Block {
    public OreBlock(int id, int sprite) {
        super(id, sprite, Material.STONE);
    }

    public int getDropItem(int metadata, Random random) {
        if (this.id == Block.COAL_ORE.id) {
            return Item.COAL.id;
        }
        if (this.id == Block.DIAMOND_ORE.id) {
            return Item.DIAMOND.id;
        }
        return this.id;
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }
}

