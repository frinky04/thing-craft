/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class MineralBlock
extends Block {
    public MineralBlock(int id, int sprite) {
        super(id, Material.IRON);
        this.sprite = sprite;
    }

    public int getSprite(int face) {
        return this.sprite - 16;
    }
}

