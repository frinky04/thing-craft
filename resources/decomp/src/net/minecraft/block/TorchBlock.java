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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class TorchBlock
extends Block {
    protected TorchBlock(int id, int sprite) {
        super(id, sprite, Material.DECORATION);
        this.setTicksRandomly(true);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
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
        return 2;
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
        if (world.isSolidBlock(x, y, z + 1)) {
            return true;
        }
        return world.isSolidBlock(x, y - 1, z);
    }

    public void updateMetadataOnPlaced(World world, int x, int y, int z, int face) {
        int i = world.getBlockMetadata(x, y, z);
        if (face == 1 && world.isSolidBlock(x, y - 1, z)) {
            i = 5;
        }
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
        world.setBlockMetadata(x, y, z, i);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        super.tick(world, x, y, z, random);
        if (world.getBlockMetadata(x, y, z) == 0) {
            this.onAdded(world, x, y, z);
        }
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
        } else if (world.isSolidBlock(x, y - 1, z)) {
            world.setBlockMetadata(x, y, z, 5);
        }
        this.canSurviveOrBreak(world, x, y, z);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (this.canSurviveOrBreak(world, x, y, z)) {
            int i = world.getBlockMetadata(x, y, z);
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
            if (!world.isSolidBlock(x, y - 1, z) && i == 5) {
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

    public HitResult rayTrace(World world, int x, int y, int z, Vec3d from, Vec3d to) {
        int i = world.getBlockMetadata(x, y, z) & 7;
        float f = 0.15f;
        if (i == 1) {
            this.setShape(0.0f, 0.2f, 0.5f - f, f * 2.0f, 0.8f, 0.5f + f);
        } else if (i == 2) {
            this.setShape(1.0f - f * 2.0f, 0.2f, 0.5f - f, 1.0f, 0.8f, 0.5f + f);
        } else if (i == 3) {
            this.setShape(0.5f - f, 0.2f, 0.0f, 0.5f + f, 0.8f, f * 2.0f);
        } else if (i == 4) {
            this.setShape(0.5f - f, 0.2f, 1.0f - f * 2.0f, 0.5f + f, 0.8f, 1.0f);
        } else {
            f = 0.1f;
            this.setShape(0.5f - f, 0.0f, 0.5f - f, 0.5f + f, 0.6f, 0.5f + f);
        }
        return super.rayTrace(world, x, y, z, from, to);
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        int i = world.getBlockMetadata(x, y, z);
        double d = (float)x + 0.5f;
        double e = (float)y + 0.7f;
        double f = (float)z + 0.5f;
        double g = 0.22f;
        double h = 0.27f;
        if (i == 1) {
            world.addParticle("smoke", d - h, e + g, f, 0.0, 0.0, 0.0);
            world.addParticle("flame", d - h, e + g, f, 0.0, 0.0, 0.0);
        } else if (i == 2) {
            world.addParticle("smoke", d + h, e + g, f, 0.0, 0.0, 0.0);
            world.addParticle("flame", d + h, e + g, f, 0.0, 0.0, 0.0);
        } else if (i == 3) {
            world.addParticle("smoke", d, e + g, f - h, 0.0, 0.0, 0.0);
            world.addParticle("flame", d, e + g, f - h, 0.0, 0.0, 0.0);
        } else if (i == 4) {
            world.addParticle("smoke", d, e + g, f + h, 0.0, 0.0, 0.0);
            world.addParticle("flame", d, e + g, f + h, 0.0, 0.0, 0.0);
        } else {
            world.addParticle("smoke", d, e, f, 0.0, 0.0, 0.0);
            world.addParticle("flame", d, e, f, 0.0, 0.0, 0.0);
        }
    }
}

