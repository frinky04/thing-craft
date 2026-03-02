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
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class DoorBlock
extends Block {
    protected DoorBlock(int i, Material material) {
        super(i, material);
        this.sprite = 97;
        if (material == Material.IRON) {
            ++this.sprite;
        }
        float f = 0.5f;
        float g = 1.0f;
        this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, g, 0.5f + f);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        if (face == 0 || face == 1) {
            return this.sprite;
        }
        int i = this.getFacing(metadata);
        if ((i == 0 || i == 2) ^ face <= 3) {
            return this.sprite;
        }
        int j = i / 2 + (face & 1 ^ i);
        int k = this.sprite - (metadata & 8) * 2;
        if (((j += (metadata & 4) / 4) & 1) != 0) {
            k = -k;
        }
        return k;
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 7;
    }

    @Environment(value=EnvType.CLIENT)
    public Box getOutlineShape(World world, int x, int y, int z) {
        this.updateShape(world, x, y, z);
        return super.getOutlineShape(world, x, y, z);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        this.updateShape(world, x, y, z);
        return super.getCollisionShape(world, x, y, z);
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        this.updateShape(this.getFacing(world.getBlockMetadata(x, y, z)));
    }

    public void updateShape(int facing) {
        float f = 0.1875f;
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 1.0f);
        if (facing == 0) {
            this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, f);
        }
        if (facing == 1) {
            this.setShape(1.0f - f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        }
        if (facing == 2) {
            this.setShape(0.0f, 0.0f, 1.0f - f, 1.0f, 1.0f, 1.0f);
        }
        if (facing == 3) {
            this.setShape(0.0f, 0.0f, 0.0f, f, 1.0f, 1.0f);
        }
    }

    public void startMining(World world, int x, int y, int z, PlayerEntity player) {
        this.use(world, x, y, z, player);
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        if (this.material == Material.IRON) {
            return true;
        }
        int i = world.getBlockMetadata(x, y, z);
        if ((i & 8) != 0) {
            if (world.getBlock(x, y - 1, z) == this.id) {
                this.use(world, x, y - 1, z, player);
            }
            return true;
        }
        if (world.getBlock(x, y + 1, z) == this.id) {
            world.setBlockMetadata(x, y + 1, z, (i ^ 4) + 8);
        }
        world.setBlockMetadata(x, y, z, i ^ 4);
        world.notifyRegionChanged(x, y - 1, z, x, y, z);
        if (Math.random() < 0.5) {
            world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "random.door_open", 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
        } else {
            world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "random.door_close", 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
        }
        return true;
    }

    public void updateOpenState(World world, int x, int y, int z, boolean open) {
        boolean j;
        int i = world.getBlockMetadata(x, y, z);
        if ((i & 8) != 0) {
            if (world.getBlock(x, y - 1, z) == this.id) {
                this.updateOpenState(world, x, y - 1, z, open);
            }
            return;
        }
        boolean bl = j = (world.getBlockMetadata(x, y, z) & 4) > 0;
        if (j == open) {
            return;
        }
        if (world.getBlock(x, y + 1, z) == this.id) {
            world.setBlockMetadata(x, y + 1, z, (i ^ 4) + 8);
        }
        world.setBlockMetadata(x, y, z, i ^ 4);
        world.notifyRegionChanged(x, y - 1, z, x, y, z);
        if (Math.random() < 0.5) {
            world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "random.door_open", 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
        } else {
            world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "random.door_close", 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
        }
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        int i = world.getBlockMetadata(x, y, z);
        if ((i & 8) != 0) {
            if (world.getBlock(x, y - 1, z) != this.id) {
                world.setBlock(x, y, z, 0);
            }
            if (neighborBlock > 0 && Block.BY_ID[neighborBlock].isSignalSource()) {
                this.neighborChanged(world, x, y - 1, z, neighborBlock);
            }
        } else {
            boolean j = false;
            if (world.getBlock(x, y + 1, z) != this.id) {
                world.setBlock(x, y, z, 0);
                j = true;
            }
            if (!world.isSolidBlock(x, y - 1, z)) {
                world.setBlock(x, y, z, 0);
                j = true;
                if (world.getBlock(x, y + 1, z) == this.id) {
                    world.setBlock(x, y + 1, z, 0);
                }
            }
            if (j) {
                this.dropItems(world, x, y, z, i);
            } else if (neighborBlock > 0 && Block.BY_ID[neighborBlock].isSignalSource()) {
                boolean k = world.hasNeighborSignal(x, y, z) || world.hasNeighborSignal(x, y + 1, z);
                this.updateOpenState(world, x, y, z, k);
            }
        }
    }

    public int getDropItem(int metadata, Random random) {
        if ((metadata & 8) != 0) {
            return 0;
        }
        if (this.material == Material.IRON) {
            return Item.IRON_DOOR.id;
        }
        return Item.WOODEN_DOOR.id;
    }

    public HitResult rayTrace(World world, int x, int y, int z, Vec3d from, Vec3d to) {
        this.updateShape(world, x, y, z);
        return super.rayTrace(world, x, y, z, from, to);
    }

    public int getFacing(int metadata) {
        if ((metadata & 4) == 0) {
            return metadata - 1 & 3;
        }
        return metadata & 3;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        if (y >= 127) {
            return false;
        }
        return world.isSolidBlock(x, y - 1, z) && super.canBePlaced(world, x, y, z) && super.canBePlaced(world, x, y + 1, z);
    }
}

