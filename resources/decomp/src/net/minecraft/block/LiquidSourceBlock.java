/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class LiquidSourceBlock
extends LiquidBlock {
    protected LiquidSourceBlock(int i, Material material) {
        super(i, material);
        this.setTicksRandomly(false);
        if (material == Material.LAVA) {
            this.setTicksRandomly(true);
        }
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        super.neighborChanged(world, x, y, z, neighborBlock);
        if (world.getBlock(x, y, z) == this.id) {
            this.convertToFlowing(world, x, y, z);
        }
    }

    private void convertToFlowing(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        world.suppressNeighborChangedUpdates = true;
        world.setBlockWithMetadataQuietly(x, y, z, this.id - 1, i);
        world.notifyRegionChanged(x, y, z, x, y, z);
        world.scheduleTick(x, y, z, this.id - 1);
        world.suppressNeighborChangedUpdates = false;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (this.material == Material.LAVA) {
            int i = random.nextInt(3);
            for (int j = 0; j < i; ++j) {
                int k = world.getBlock(x += random.nextInt(3) - 1, ++y, z += random.nextInt(3) - 1);
                if (k == 0) {
                    if (!this.isFlammable(world, x - 1, y, z) && !this.isFlammable(world, x + 1, y, z) && !this.isFlammable(world, x, y, z - 1) && !this.isFlammable(world, x, y, z + 1) && !this.isFlammable(world, x, y - 1, z) && !this.isFlammable(world, x, y + 1, z)) continue;
                    world.setBlock(x, y, z, Block.FIRE.id);
                    return;
                }
                if (!Block.BY_ID[k].material.blocksMovement()) continue;
                return;
            }
        }
    }

    private boolean isFlammable(World world, int x, int y, int z) {
        return world.getMaterial(x, y, z).isFlammable();
    }
}

