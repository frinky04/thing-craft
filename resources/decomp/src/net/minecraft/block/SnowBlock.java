/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class SnowBlock
extends Block {
    protected SnowBlock(int id, int sprite) {
        super(id, sprite, Material.SNOW);
        this.setTicksRandomly(true);
    }

    public int getDropItem(int metadata, Random random) {
        return Item.SNOWBALL.id;
    }

    public int getBaseDropCount(Random random) {
        return 4;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (world.getLight(LightType.BLOCK, x, y, z) > 11) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
    }
}

