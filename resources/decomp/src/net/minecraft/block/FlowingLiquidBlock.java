/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class FlowingLiquidBlock
extends LiquidBlock {
    int adjacentSources = 0;
    boolean[] spread = new boolean[4];
    int[] distanceToGap = new int[4];

    protected FlowingLiquidBlock(int i, Material material) {
        super(i, material);
    }

    private void convertToSource(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        world.setBlockWithMetadataQuietly(x, y, z, this.id + 1, i);
        world.notifyRegionChanged(x, y, z, x, y, z);
        world.notifyBlockChanged(x, y, z);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        int i = this.getLiquidState(world, x, y, z);
        int j = 1;
        if (this.material == Material.LAVA && !world.dimension.yeetsWater) {
            j = 2;
        }
        boolean k = true;
        if (i > 0) {
            int l = -100;
            this.adjacentSources = 0;
            l = this.getLowestDepth(world, x - 1, y, z, l);
            l = this.getLowestDepth(world, x + 1, y, z, l);
            l = this.getLowestDepth(world, x, y, z - 1, l);
            int m = (l = this.getLowestDepth(world, x, y, z + 1, l)) + j;
            if (m >= 8 || l < 0) {
                m = -1;
            }
            if (this.getLiquidState(world, x, y + 1, z) >= 0) {
                int o = this.getLiquidState(world, x, y + 1, z);
                m = o >= 8 ? o : o + 8;
            }
            if (this.adjacentSources >= 2 && this.material == Material.WATER) {
                if (world.isSolidBlock(x, y - 1, z)) {
                    m = 0;
                } else if (world.getMaterial(x, y - 1, z) == this.material && world.getBlockMetadata(x, y, z) == 0) {
                    m = 0;
                }
            }
            if (this.material == Material.LAVA && i < 8 && m < 8 && m > i && random.nextInt(4) != 0) {
                m = i;
                k = false;
            }
            if (m != i) {
                i = m;
                if (i < 0) {
                    world.setBlock(x, y, z, 0);
                } else {
                    world.setBlockMetadata(x, y, z, i);
                    world.scheduleTick(x, y, z, this.id);
                    world.updateNeighbors(x, y, z, this.id);
                }
            } else if (k) {
                this.convertToSource(world, x, y, z);
            }
        } else {
            this.convertToSource(world, x, y, z);
        }
        if (this.canSpreadTo(world, x, y - 1, z)) {
            if (i >= 8) {
                world.setBlockWithMetadata(x, y - 1, z, this.id, i);
            } else {
                world.setBlockWithMetadata(x, y - 1, z, this.id, i + 8);
            }
        } else if (i >= 0 && (i == 0 || this.isLiquidBlocking(world, x, y - 1, z))) {
            boolean[] bls = this.getSpread(world, x, y, z);
            int n = i + j;
            if (i >= 8) {
                n = 1;
            }
            if (n >= 8) {
                return;
            }
            if (bls[0]) {
                this.spreadTo(world, x - 1, y, z, n);
            }
            if (bls[1]) {
                this.spreadTo(world, x + 1, y, z, n);
            }
            if (bls[2]) {
                this.spreadTo(world, x, y, z - 1, n);
            }
            if (bls[3]) {
                this.spreadTo(world, x, y, z + 1, n);
            }
        }
    }

    private void spreadTo(World world, int x, int y, int z, int depth) {
        if (this.canSpreadTo(world, x, y, z)) {
            int i = world.getBlock(x, y, z);
            if (i > 0) {
                if (this.material == Material.LAVA) {
                    this.fizz(world, x, y, z);
                } else {
                    Block.BY_ID[i].dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
                }
            }
            world.setBlockWithMetadata(x, y, z, this.id, depth);
        }
    }

    private int getDistanceToGap(World world, int x, int y, int z, int distance, int fromDir) {
        int i = 1000;
        for (int j = 0; j < 4; ++j) {
            int n;
            if (j == 0 && fromDir == 1 || j == 1 && fromDir == 0 || j == 2 && fromDir == 3 || j == 3 && fromDir == 2) continue;
            int k = x;
            int l = y;
            int m = z;
            if (j == 0) {
                --k;
            }
            if (j == 1) {
                ++k;
            }
            if (j == 2) {
                --m;
            }
            if (j == 3) {
                ++m;
            }
            if (this.isLiquidBlocking(world, k, l, m) || world.getMaterial(k, l, m) == this.material && world.getBlockMetadata(k, l, m) == 0) continue;
            if (!this.isLiquidBlocking(world, k, l - 1, m)) {
                return distance;
            }
            if (distance >= 4 || (n = this.getDistanceToGap(world, k, l, m, distance + 1, j)) >= i) continue;
            i = n;
        }
        return i;
    }

    private boolean[] getSpread(World world, int x, int y, int z) {
        int k;
        int i;
        for (i = 0; i < 4; ++i) {
            this.distanceToGap[i] = 1000;
            int j = x;
            int l = y;
            int m = z;
            if (i == 0) {
                --j;
            }
            if (i == 1) {
                ++j;
            }
            if (i == 2) {
                --m;
            }
            if (i == 3) {
                ++m;
            }
            if (this.isLiquidBlocking(world, j, l, m) || world.getMaterial(j, l, m) == this.material && world.getBlockMetadata(j, l, m) == 0) continue;
            this.distanceToGap[i] = !this.isLiquidBlocking(world, j, l - 1, m) ? 0 : this.getDistanceToGap(world, j, l, m, 1, i);
        }
        i = this.distanceToGap[0];
        for (k = 1; k < 4; ++k) {
            if (this.distanceToGap[k] >= i) continue;
            i = this.distanceToGap[k];
        }
        for (k = 0; k < 4; ++k) {
            this.spread[k] = this.distanceToGap[k] == i;
        }
        return this.spread;
    }

    private boolean isLiquidBlocking(World world, int x, int y, int z) {
        int i = world.getBlock(x, y, z);
        if (i == Block.WOODEN_DOOR.id || i == Block.IRON_DOOR.id || i == Block.STANDING_SIGN.id || i == Block.LADDER.id || i == Block.REEDS.id) {
            return true;
        }
        if (i == 0) {
            return false;
        }
        Material material = Block.BY_ID[i].material;
        return material.isSolid();
    }

    protected int getLowestDepth(World world, int x, int y, int z, int depth) {
        int i = this.getLiquidState(world, x, y, z);
        if (i < 0) {
            return depth;
        }
        if (i == 0) {
            ++this.adjacentSources;
        }
        if (i >= 8) {
            i = 0;
        }
        return depth < 0 || i < depth ? i : depth;
    }

    private boolean canSpreadTo(World world, int x, int y, int z) {
        Material material = world.getMaterial(x, y, z);
        if (material == this.material) {
            return false;
        }
        if (material == Material.LAVA) {
            return false;
        }
        return !this.isLiquidBlocking(world, x, y, z);
    }

    public void onAdded(World world, int x, int y, int z) {
        super.onAdded(world, x, y, z);
        if (world.getBlock(x, y, z) == this.id) {
            world.scheduleTick(x, y, z, this.id);
        }
    }
}

