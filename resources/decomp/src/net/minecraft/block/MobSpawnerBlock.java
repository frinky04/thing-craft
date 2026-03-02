/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.material.Material;

public class MobSpawnerBlock
extends BlockWithBlockEntity {
    protected MobSpawnerBlock(int id, int sprite) {
        super(id, sprite, Material.STONE);
    }

    protected BlockEntity createBlockEntity() {
        return new MobSpawnerBlockEntity();
    }

    public int getDropItem(int metadata, Random random) {
        return 0;
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    public boolean isSolid() {
        return false;
    }
}

