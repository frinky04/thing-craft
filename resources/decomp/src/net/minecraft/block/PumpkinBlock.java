/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class PumpkinBlock
extends Block {
    private boolean lit;

    protected PumpkinBlock(int id, int sprite, boolean lit) {
        super(id, Material.PUMPKIN);
        this.sprite = sprite;
        this.setTicksRandomly(true);
        this.lit = lit;
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        if (face == 1) {
            return this.sprite;
        }
        if (face == 0) {
            return this.sprite;
        }
        int i = this.sprite + 1 + 16;
        if (this.lit) {
            ++i;
        }
        if (metadata == 0 && face == 2) {
            return i;
        }
        if (metadata == 1 && face == 5) {
            return i;
        }
        if (metadata == 2 && face == 3) {
            return i;
        }
        if (metadata == 3 && face == 4) {
            return i;
        }
        return this.sprite + 16;
    }

    public int getSprite(int face) {
        if (face == 1) {
            return this.sprite;
        }
        if (face == 0) {
            return this.sprite;
        }
        if (face == 3) {
            return this.sprite + 1 + 16;
        }
        return this.sprite + 16;
    }

    public void onAdded(World world, int x, int y, int z) {
        super.onAdded(world, x, y, z);
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        int i = world.getBlock(x, y, z);
        return (i == 0 || Block.BY_ID[i].material.isLiquid()) && world.isSolidBlock(x, y - 1, z);
    }

    public void onPlaced(World world, int x, int y, int z, MobEntity entity) {
        int i = MathHelper.floor((double)(entity.yaw * 4.0f / 360.0f) + 0.5) & 3;
        world.setBlockMetadata(x, y, z, i);
    }
}

