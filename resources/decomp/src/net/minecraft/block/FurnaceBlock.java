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
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class FurnaceBlock
extends BlockWithBlockEntity {
    private final boolean lit;

    protected FurnaceBlock(int id, boolean lit) {
        super(id, Material.STONE);
        this.lit = lit;
        this.sprite = 45;
    }

    public int getDropItem(int metadata, Random random) {
        return Block.FURNACE.id;
    }

    public void onAdded(World world, int x, int y, int z) {
        super.onAdded(world, x, y, z);
        this.updateFacing(world, x, y, z);
    }

    private void updateFacing(World world, int x, int y, int z) {
        int i = world.getBlock(x, y, z - 1);
        int j = world.getBlock(x, y, z + 1);
        int k = world.getBlock(x - 1, y, z);
        int l = world.getBlock(x + 1, y, z);
        int m = 3;
        if (Block.IS_SOLID[i] && !Block.IS_SOLID[j]) {
            m = 3;
        }
        if (Block.IS_SOLID[j] && !Block.IS_SOLID[i]) {
            m = 2;
        }
        if (Block.IS_SOLID[k] && !Block.IS_SOLID[l]) {
            m = 5;
        }
        if (Block.IS_SOLID[l] && !Block.IS_SOLID[k]) {
            m = 4;
        }
        world.setBlockMetadata(x, y, z, m);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(WorldView world, int x, int y, int z, int face) {
        if (face == 1) {
            return Block.STONE.sprite;
        }
        if (face == 0) {
            return Block.STONE.sprite;
        }
        int i = world.getBlockMetadata(x, y, z);
        if (face != i) {
            return this.sprite;
        }
        if (this.lit) {
            return this.sprite + 16;
        }
        return this.sprite - 1;
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (!this.lit) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        float f = (float)x + 0.5f;
        float g = (float)y + 0.0f + random.nextFloat() * 6.0f / 16.0f;
        float h = (float)z + 0.5f;
        float j = 0.52f;
        float k = random.nextFloat() * 0.6f - 0.3f;
        if (i == 4) {
            world.addParticle("smoke", f - j, g, h + k, 0.0, 0.0, 0.0);
            world.addParticle("flame", f - j, g, h + k, 0.0, 0.0, 0.0);
        } else if (i == 5) {
            world.addParticle("smoke", f + j, g, h + k, 0.0, 0.0, 0.0);
            world.addParticle("flame", f + j, g, h + k, 0.0, 0.0, 0.0);
        } else if (i == 2) {
            world.addParticle("smoke", f + k, g, h - j, 0.0, 0.0, 0.0);
            world.addParticle("flame", f + k, g, h - j, 0.0, 0.0, 0.0);
        } else if (i == 3) {
            world.addParticle("smoke", f + k, g, h + j, 0.0, 0.0, 0.0);
            world.addParticle("flame", f + k, g, h + j, 0.0, 0.0, 0.0);
        }
    }

    public int getSprite(int face) {
        if (face == 1) {
            return Block.STONE.id;
        }
        if (face == 0) {
            return Block.STONE.id;
        }
        if (face == 3) {
            return this.sprite - 1;
        }
        return this.sprite;
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        FurnaceBlockEntity furnaceBlockEntity = (FurnaceBlockEntity)world.getBlockEntity(x, y, z);
        player.openFurnaceMenu(furnaceBlockEntity);
        return true;
    }

    public static void updateLitState(boolean lit, World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        BlockEntity blockEntity = world.getBlockEntity(x, y, z);
        if (lit) {
            world.setBlock(x, y, z, Block.LIT_FURNACE.id);
        } else {
            world.setBlock(x, y, z, Block.FURNACE.id);
        }
        world.setBlockMetadata(x, y, z, i);
        world.setBlockEntity(x, y, z, blockEntity);
    }

    protected BlockEntity createBlockEntity() {
        return new FurnaceBlockEntity();
    }

    public void onPlaced(World world, int x, int y, int z, MobEntity entity) {
        int i = MathHelper.floor((double)(entity.yaw * 4.0f / 360.0f) + 0.5) & 3;
        if (i == 0) {
            world.setBlockMetadata(x, y, z, 2);
        }
        if (i == 1) {
            world.setBlockMetadata(x, y, z, 5);
        }
        if (i == 2) {
            world.setBlockMetadata(x, y, z, 3);
        }
        if (i == 3) {
            world.setBlockMetadata(x, y, z, 4);
        }
    }
}

