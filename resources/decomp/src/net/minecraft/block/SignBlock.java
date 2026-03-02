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
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class SignBlock
extends BlockWithBlockEntity {
    private Class blockEntityType;
    private boolean standing;

    protected SignBlock(int id, Class blockEntityType, boolean standing) {
        super(id, Material.WOOD);
        this.standing = standing;
        this.sprite = 4;
        this.blockEntityType = blockEntityType;
        float f = 0.25f;
        float g = 1.0f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, g, 0.5f + f);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public Box getOutlineShape(World world, int x, int y, int z) {
        this.updateShape(world, x, y, z);
        return super.getOutlineShape(world, x, y, z);
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        if (this.standing) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        float f = 0.28125f;
        float g = 0.78125f;
        float h = 0.0f;
        float j = 1.0f;
        float k = 0.125f;
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        if (i == 2) {
            this.setShape(h, f, 1.0f - k, j, g, 1.0f);
        }
        if (i == 3) {
            this.setShape(h, f, 0.0f, j, g, k);
        }
        if (i == 4) {
            this.setShape(1.0f - k, f, h, 1.0f, g, j);
        }
        if (i == 5) {
            this.setShape(0.0f, f, h, k, g, j);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return -1;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    public boolean isSolid() {
        return false;
    }

    protected BlockEntity createBlockEntity() {
        try {
            return (BlockEntity)this.blockEntityType.newInstance();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public int getDropItem(int metadata, Random random) {
        return Item.SIGN.id;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        boolean i = false;
        if (this.standing) {
            if (!world.getMaterial(x, y - 1, z).isSolid()) {
                i = true;
            }
        } else {
            int j = world.getBlockMetadata(x, y, z);
            i = true;
            if (j == 2 && world.getMaterial(x, y, z + 1).isSolid()) {
                i = false;
            }
            if (j == 3 && world.getMaterial(x, y, z - 1).isSolid()) {
                i = false;
            }
            if (j == 4 && world.getMaterial(x + 1, y, z).isSolid()) {
                i = false;
            }
            if (j == 5 && world.getMaterial(x - 1, y, z).isSolid()) {
                i = false;
            }
        }
        if (i) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
        super.neighborChanged(world, x, y, z, neighborBlock);
    }
}

