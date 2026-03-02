/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.WorldView;

public class TranslucentBlock
extends Block {
    protected boolean culling;

    protected TranslucentBlock(int id, int sprite, Material material, boolean culling) {
        super(id, sprite, material);
        this.culling = culling;
    }

    public boolean isSolid() {
        return false;
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        int i = world.getBlock(x, y, z);
        if (!this.culling && i == this.id) {
            return false;
        }
        return super.shouldRenderFace(world, x, y, z, face);
    }
}

