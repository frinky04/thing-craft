/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class SpongeBlock
extends Block {
    protected SpongeBlock(int id) {
        super(id, Material.SPONGE);
        this.sprite = 48;
    }

    public void onAdded(World world, int x, int y, int z) {
        int i = 2;
        for (int j = x - i; j <= x + i; ++j) {
            for (int k = y - i; k <= y + i; ++k) {
                for (int l = z - i; l <= z + i; ++l) {
                    if (world.getMaterial(j, k, l) != Material.WATER) continue;
                }
            }
        }
    }

    public void onRemoved(World world, int x, int y, int z) {
        int i = 2;
        for (int j = x - i; j <= x + i; ++j) {
            for (int k = y - i; k <= y + i; ++k) {
                for (int l = z - i; l <= z + i; ++l) {
                    world.updateNeighbors(j, k, l, world.getBlock(j, k, l));
                }
            }
        }
    }
}

