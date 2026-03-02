/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.PlantBlock;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.LargeOakTreeFeature;
import net.minecraft.world.gen.feature.TreeFeature;

public class SaplingBlock
extends PlantBlock {
    protected SaplingBlock(int i, int j) {
        super(i, j);
        float f = 0.4f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, f * 2.0f, 0.5f + f);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        super.tick(world, x, y, z, random);
        if (world.getRawBrightness(x, y + 1, z) >= 9 && random.nextInt(5) == 0) {
            int i = world.getBlockMetadata(x, y, z);
            if (i < 15) {
                world.setBlockMetadata(x, y, z, i + 1);
            } else {
                LargeOakTreeFeature feature;
                world.setBlockQuietly(x, y, z, 0);
                TreeFeature treeFeature = new TreeFeature();
                if (random.nextInt(10) == 0) {
                    feature = new LargeOakTreeFeature();
                }
                if (!((Feature)feature).place(world, random, x, y, z)) {
                    world.setBlockQuietly(x, y, z, this.id);
                }
            }
        }
    }
}

