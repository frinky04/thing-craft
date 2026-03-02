/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.world.World;

public class TntBlock
extends Block {
    public TntBlock(int id, int sprite) {
        super(id, sprite, Material.TNT);
    }

    public int getSprite(int face) {
        if (face == 0) {
            return this.sprite + 2;
        }
        if (face == 1) {
            return this.sprite + 1;
        }
        return this.sprite;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (neighborBlock > 0 && Block.BY_ID[neighborBlock].isSignalSource() && world.hasNeighborSignal(x, y, z)) {
            this.onBroken(world, x, y, z, 0);
            world.setBlock(x, y, z, 0);
        }
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    public void onExploded(World world, int x, int y, int z) {
        PrimedTntEntity primedTntEntity = new PrimedTntEntity(world, (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f);
        primedTntEntity.fuseTimer = world.random.nextInt(primedTntEntity.fuseTimer / 4) + primedTntEntity.fuseTimer / 8;
        world.addEntity(primedTntEntity);
    }

    public void onBroken(World world, int x, int y, int z, int metadata) {
        if (world.isMultiplayer) {
            return;
        }
        PrimedTntEntity primedTntEntity = new PrimedTntEntity(world, (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f);
        world.addEntity(primedTntEntity);
        world.playSound(primedTntEntity, "random.fuse", 1.0f, 1.0f);
    }
}

