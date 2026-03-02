/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class IceBlock
extends TransparentBlock {
    public IceBlock(int id, int sprite) {
        super(id, sprite, Material.ICE, false);
        this.slipperiness = 0.98f;
        this.setTicksRandomly(true);
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderLayer() {
        return 1;
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        return super.shouldRenderFace(world, x, y, z, 1 - face);
    }

    public void onRemoved(World world, int x, int y, int z) {
        Material material = world.getMaterial(x, y - 1, z);
        if (material.blocksMovement() || material.isLiquid()) {
            world.setBlock(x, y, z, Block.FLOWING_WATER.id);
        }
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (world.getLight(LightType.BLOCK, x, y, z) > 11 - Block.OPACITIES[this.id]) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, Block.WATER.id);
        }
    }
}

