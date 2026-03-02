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
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ButtonBlock
extends Block {
    protected ButtonBlock(int id, int sprite) {
        super(id, sprite, Material.DECORATION);
        this.setTicksRandomly(true);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    public int getTickRate() {
        return 20;
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        if (world.isSolidBlock(x - 1, y, z)) {
            return true;
        }
        if (world.isSolidBlock(x + 1, y, z)) {
            return true;
        }
        if (world.isSolidBlock(x, y, z - 1)) {
            return true;
        }
        return world.isSolidBlock(x, y, z + 1);
    }

    public void updateMetadataOnPlaced(World world, int x, int y, int z, int face) {
        int i = world.getBlockMetadata(x, y, z);
        int j = i & 8;
        i &= 7;
        if (face == 2 && world.isSolidBlock(x, y, z + 1)) {
            i = 4;
        }
        if (face == 3 && world.isSolidBlock(x, y, z - 1)) {
            i = 3;
        }
        if (face == 4 && world.isSolidBlock(x + 1, y, z)) {
            i = 2;
        }
        if (face == 5 && world.isSolidBlock(x - 1, y, z)) {
            i = 1;
        }
        world.setBlockMetadata(x, y, z, i + j);
    }

    public void onAdded(World world, int x, int y, int z) {
        if (world.isSolidBlock(x - 1, y, z)) {
            world.setBlockMetadata(x, y, z, 1);
        } else if (world.isSolidBlock(x + 1, y, z)) {
            world.setBlockMetadata(x, y, z, 2);
        } else if (world.isSolidBlock(x, y, z - 1)) {
            world.setBlockMetadata(x, y, z, 3);
        } else if (world.isSolidBlock(x, y, z + 1)) {
            world.setBlockMetadata(x, y, z, 4);
        }
        this.canSurviveOrBreak(world, x, y, z);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (this.canSurviveOrBreak(world, x, y, z)) {
            int i = world.getBlockMetadata(x, y, z) & 7;
            boolean j = false;
            if (!world.isSolidBlock(x - 1, y, z) && i == 1) {
                j = true;
            }
            if (!world.isSolidBlock(x + 1, y, z) && i == 2) {
                j = true;
            }
            if (!world.isSolidBlock(x, y, z - 1) && i == 3) {
                j = true;
            }
            if (!world.isSolidBlock(x, y, z + 1) && i == 4) {
                j = true;
            }
            if (j) {
                this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
                world.setBlock(x, y, z, 0);
            }
        }
    }

    private boolean canSurviveOrBreak(World world, int x, int y, int z) {
        if (!this.canBePlaced(world, x, y, z)) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
            return false;
        }
        return true;
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        int j = i & 7;
        boolean k = (i & 8) > 0;
        float f = 0.375f;
        float g = 0.625f;
        float h = 0.1875f;
        float l = 0.125f;
        if (k) {
            l = 0.0625f;
        }
        if (j == 1) {
            this.setShape(0.0f, f, 0.5f - h, l, g, 0.5f + h);
        } else if (j == 2) {
            this.setShape(1.0f - l, f, 0.5f - h, 1.0f, g, 0.5f + h);
        } else if (j == 3) {
            this.setShape(0.5f - h, f, 0.0f, 0.5f + h, g, l);
        } else if (j == 4) {
            this.setShape(0.5f - h, f, 1.0f - l, 0.5f + h, g, 1.0f);
        }
    }

    public void startMining(World world, int x, int y, int z, PlayerEntity player) {
        this.use(world, x, y, z, player);
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        if (world.isMultiplayer) {
            return true;
        }
        int i = world.getBlockMetadata(x, y, z);
        int j = i & 7;
        int k = 8 - (i & 8);
        if (k == 0) {
            return true;
        }
        world.setBlockMetadata(x, y, z, j + k);
        world.notifyRegionChanged(x, y, z, x, y, z);
        world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "random.click", 0.3f, 0.6f);
        world.updateNeighbors(x, y, z, this.id);
        if (j == 1) {
            world.updateNeighbors(x - 1, y, z, this.id);
        } else if (j == 2) {
            world.updateNeighbors(x + 1, y, z, this.id);
        } else if (j == 3) {
            world.updateNeighbors(x, y, z - 1, this.id);
        } else if (j == 4) {
            world.updateNeighbors(x, y, z + 1, this.id);
        } else {
            world.updateNeighbors(x, y - 1, z, this.id);
        }
        world.scheduleTick(x, y, z, this.id);
        return true;
    }

    public void onRemoved(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        if ((i & 8) > 0) {
            world.updateNeighbors(x, y, z, this.id);
            int j = i & 7;
            if (j == 1) {
                world.updateNeighbors(x - 1, y, z, this.id);
            } else if (j == 2) {
                world.updateNeighbors(x + 1, y, z, this.id);
            } else if (j == 3) {
                world.updateNeighbors(x, y, z - 1, this.id);
            } else if (j == 4) {
                world.updateNeighbors(x, y, z + 1, this.id);
            } else {
                world.updateNeighbors(x, y - 1, z, this.id);
            }
        }
        super.onRemoved(world, x, y, z);
    }

    public boolean hasSignal(WorldView world, int x, int y, int z, int dir) {
        return (world.getBlockMetadata(x, y, z) & 8) > 0;
    }

    public boolean hasDirectSignal(World world, int x, int y, int z, int dir) {
        int i = world.getBlockMetadata(x, y, z);
        if ((i & 8) == 0) {
            return false;
        }
        int j = i & 7;
        if (j == 5 && dir == 1) {
            return true;
        }
        if (j == 4 && dir == 2) {
            return true;
        }
        if (j == 3 && dir == 3) {
            return true;
        }
        if (j == 2 && dir == 4) {
            return true;
        }
        return j == 1 && dir == 5;
    }

    public boolean isSignalSource() {
        return true;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (world.isMultiplayer) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        if ((i & 8) == 0) {
            return;
        }
        world.setBlockMetadata(x, y, z, i & 7);
        world.updateNeighbors(x, y, z, this.id);
        int j = i & 7;
        if (j == 1) {
            world.updateNeighbors(x - 1, y, z, this.id);
        } else if (j == 2) {
            world.updateNeighbors(x + 1, y, z, this.id);
        } else if (j == 3) {
            world.updateNeighbors(x, y, z - 1, this.id);
        } else if (j == 4) {
            world.updateNeighbors(x, y, z + 1, this.id);
        } else {
            world.updateNeighbors(x, y - 1, z, this.id);
        }
        world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "random.click", 0.3f, 0.5f);
        world.notifyRegionChanged(x, y, z, x, y, z);
    }

    @Environment(value=EnvType.CLIENT)
    public void resetShape() {
        float f = 0.1875f;
        float g = 0.125f;
        float h = 0.125f;
        this.setShape(0.5f - f, 0.5f - g, 0.5f - h, 0.5f + f, 0.5f + g, 0.5f + h);
    }
}

