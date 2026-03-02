/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.world.World;

public class CraftingTableBlock
extends Block {
    protected CraftingTableBlock(int id) {
        super(id, Material.WOOD);
        this.sprite = 59;
    }

    public int getSprite(int face) {
        if (face == 1) {
            return this.sprite - 16;
        }
        if (face == 0) {
            return Block.PLANKS.getSprite(0);
        }
        if (face == 2 || face == 4) {
            return this.sprite + 1;
        }
        return this.sprite;
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        player.openCraftingMenu();
        return true;
    }
}

