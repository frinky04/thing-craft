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
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RedstoneWireBlock
extends Block {
    private boolean shouldSignal = true;

    public RedstoneWireBlock(int id, int sprite) {
        super(id, sprite, Material.DECORATION);
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.0625f, 1.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        return this.sprite + (metadata > 0 ? 16 : 0);
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
        return 5;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        return world.isSolidBlock(x, y - 1, z);
    }

    private void updatePower(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        int j = 0;
        this.shouldSignal = false;
        boolean k = world.hasNeighborSignal(x, y, z);
        this.shouldSignal = true;
        if (k) {
            j = 15;
        } else {
            for (int l = 0; l < 4; ++l) {
                int n = x;
                int p = z;
                if (l == 0) {
                    --n;
                }
                if (l == 1) {
                    ++n;
                }
                if (l == 2) {
                    --p;
                }
                if (l == 3) {
                    ++p;
                }
                j = this.getHighestWirePower(world, n, y, p, j);
                if (world.isSolidBlock(n, y, p) && !world.isSolidBlock(x, y + 1, z)) {
                    j = this.getHighestWirePower(world, n, y + 1, p, j);
                    continue;
                }
                if (world.isSolidBlock(n, y, p)) continue;
                j = this.getHighestWirePower(world, n, y - 1, p, j);
            }
            j = j > 0 ? --j : 0;
        }
        if (i != j) {
            world.setBlockMetadata(x, y, z, j);
            world.notifyRegionChanged(x, y, z, x, y, z);
            if (j > 0) {
                --j;
            }
            for (int m = 0; m < 4; ++m) {
                int s;
                int o = x;
                int q = z;
                int r = y - 1;
                if (m == 0) {
                    --o;
                }
                if (m == 1) {
                    ++o;
                }
                if (m == 2) {
                    --q;
                }
                if (m == 3) {
                    ++q;
                }
                if (world.isSolidBlock(o, y, q)) {
                    r += 2;
                }
                if ((s = this.getHighestWirePower(world, o, y, q, -1)) >= 0 && s != j) {
                    this.updatePower(world, o, y, q);
                }
                if ((s = this.getHighestWirePower(world, o, r, q, -1)) < 0 || s == j) continue;
                this.updatePower(world, o, r, q);
            }
            if (i == 0 || j == 0) {
                world.updateNeighbors(x, y, z, this.id);
                world.updateNeighbors(x - 1, y, z, this.id);
                world.updateNeighbors(x + 1, y, z, this.id);
                world.updateNeighbors(x, y, z - 1, this.id);
                world.updateNeighbors(x, y, z + 1, this.id);
                world.updateNeighbors(x, y - 1, z, this.id);
                world.updateNeighbors(x, y + 1, z, this.id);
            }
        }
    }

    private void updateNeighborsOfWire(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != this.id) {
            return;
        }
        world.updateNeighbors(x, y, z, this.id);
        world.updateNeighbors(x - 1, y, z, this.id);
        world.updateNeighbors(x + 1, y, z, this.id);
        world.updateNeighbors(x, y, z - 1, this.id);
        world.updateNeighbors(x, y, z + 1, this.id);
        world.updateNeighbors(x, y - 1, z, this.id);
        world.updateNeighbors(x, y + 1, z, this.id);
    }

    public void onAdded(World world, int x, int y, int z) {
        super.onAdded(world, x, y, z);
        if (world.isMultiplayer) {
            return;
        }
        this.updatePower(world, x, y, z);
        world.updateNeighbors(x, y + 1, z, this.id);
        world.updateNeighbors(x, y - 1, z, this.id);
        this.updateNeighborsOfWire(world, x - 1, y, z);
        this.updateNeighborsOfWire(world, x + 1, y, z);
        this.updateNeighborsOfWire(world, x, y, z - 1);
        this.updateNeighborsOfWire(world, x, y, z + 1);
        if (world.isSolidBlock(x - 1, y, z)) {
            this.updateNeighborsOfWire(world, x - 1, y + 1, z);
        } else {
            this.updateNeighborsOfWire(world, x - 1, y - 1, z);
        }
        if (world.isSolidBlock(x + 1, y, z)) {
            this.updateNeighborsOfWire(world, x + 1, y + 1, z);
        } else {
            this.updateNeighborsOfWire(world, x + 1, y - 1, z);
        }
        if (world.isSolidBlock(x, y, z - 1)) {
            this.updateNeighborsOfWire(world, x, y + 1, z - 1);
        } else {
            this.updateNeighborsOfWire(world, x, y - 1, z - 1);
        }
        if (world.isSolidBlock(x, y, z + 1)) {
            this.updateNeighborsOfWire(world, x, y + 1, z + 1);
        } else {
            this.updateNeighborsOfWire(world, x, y - 1, z + 1);
        }
    }

    public void onRemoved(World world, int x, int y, int z) {
        super.onRemoved(world, x, y, z);
        if (world.isMultiplayer) {
            return;
        }
        world.updateNeighbors(x, y + 1, z, this.id);
        world.updateNeighbors(x, y - 1, z, this.id);
        this.updatePower(world, x, y, z);
        this.updateNeighborsOfWire(world, x - 1, y, z);
        this.updateNeighborsOfWire(world, x + 1, y, z);
        this.updateNeighborsOfWire(world, x, y, z - 1);
        this.updateNeighborsOfWire(world, x, y, z + 1);
        if (world.isSolidBlock(x - 1, y, z)) {
            this.updateNeighborsOfWire(world, x - 1, y + 1, z);
        } else {
            this.updateNeighborsOfWire(world, x - 1, y - 1, z);
        }
        if (world.isSolidBlock(x + 1, y, z)) {
            this.updateNeighborsOfWire(world, x + 1, y + 1, z);
        } else {
            this.updateNeighborsOfWire(world, x + 1, y - 1, z);
        }
        if (world.isSolidBlock(x, y, z - 1)) {
            this.updateNeighborsOfWire(world, x, y + 1, z - 1);
        } else {
            this.updateNeighborsOfWire(world, x, y - 1, z - 1);
        }
        if (world.isSolidBlock(x, y, z + 1)) {
            this.updateNeighborsOfWire(world, x, y + 1, z + 1);
        } else {
            this.updateNeighborsOfWire(world, x, y - 1, z + 1);
        }
    }

    private int getHighestWirePower(World world, int x, int y, int z, int wirePower) {
        if (world.getBlock(x, y, z) != this.id) {
            return wirePower;
        }
        int i = world.getBlockMetadata(x, y, z);
        if (i > wirePower) {
            return i;
        }
        return wirePower;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (world.isMultiplayer) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        boolean j = this.canBePlaced(world, x, y, z);
        if (!j) {
            this.dropItems(world, x, y, z, i);
            world.setBlock(x, y, z, 0);
        } else {
            this.updatePower(world, x, y, z);
        }
        super.neighborChanged(world, x, y, z, neighborBlock);
    }

    public int getDropItem(int metadata, Random random) {
        return Item.REDSTONE.id;
    }

    public boolean hasDirectSignal(World world, int x, int y, int z, int dir) {
        if (!this.shouldSignal) {
            return false;
        }
        return this.hasSignal(world, x, y, z, dir);
    }

    public boolean hasSignal(WorldView world, int x, int y, int z, int dir) {
        boolean l;
        if (!this.shouldSignal) {
            return false;
        }
        if (world.getBlockMetadata(x, y, z) == 0) {
            return false;
        }
        if (dir == 1) {
            return true;
        }
        boolean i = RedstoneWireBlock.shouldConnectTo(world, x - 1, y, z) || !world.isSolidBlock(x - 1, y, z) && RedstoneWireBlock.shouldConnectTo(world, x - 1, y - 1, z);
        boolean j = RedstoneWireBlock.shouldConnectTo(world, x + 1, y, z) || !world.isSolidBlock(x + 1, y, z) && RedstoneWireBlock.shouldConnectTo(world, x + 1, y - 1, z);
        boolean k = RedstoneWireBlock.shouldConnectTo(world, x, y, z - 1) || !world.isSolidBlock(x, y, z - 1) && RedstoneWireBlock.shouldConnectTo(world, x, y - 1, z - 1);
        boolean bl = l = RedstoneWireBlock.shouldConnectTo(world, x, y, z + 1) || !world.isSolidBlock(x, y, z + 1) && RedstoneWireBlock.shouldConnectTo(world, x, y - 1, z + 1);
        if (!world.isSolidBlock(x, y + 1, z)) {
            if (world.isSolidBlock(x - 1, y, z) && RedstoneWireBlock.shouldConnectTo(world, x - 1, y + 1, z)) {
                i = true;
            }
            if (world.isSolidBlock(x + 1, y, z) && RedstoneWireBlock.shouldConnectTo(world, x + 1, y + 1, z)) {
                j = true;
            }
            if (world.isSolidBlock(x, y, z - 1) && RedstoneWireBlock.shouldConnectTo(world, x, y + 1, z - 1)) {
                k = true;
            }
            if (world.isSolidBlock(x, y, z + 1) && RedstoneWireBlock.shouldConnectTo(world, x, y + 1, z + 1)) {
                l = true;
            }
        }
        if (!(k || j || i || l || dir < 2 || dir > 5)) {
            return true;
        }
        if (dir == 2 && k && !i && !j) {
            return true;
        }
        if (dir == 3 && l && !i && !j) {
            return true;
        }
        if (dir == 4 && i && !k && !l) {
            return true;
        }
        return dir == 5 && j && !k && !l;
    }

    public boolean isSignalSource() {
        return this.shouldSignal;
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) > 0) {
            double d = (double)x + 0.5 + ((double)random.nextFloat() - 0.5) * 0.2;
            double e = (float)y + 0.0625f;
            double f = (double)z + 0.5 + ((double)random.nextFloat() - 0.5) * 0.2;
            world.addParticle("reddust", d, e, f, 0.0, 0.0, 0.0);
        }
    }

    public static boolean shouldConnectTo(WorldView world, int x, int y, int z) {
        int i = world.getBlock(x, y, z);
        if (i == Block.REDSTONE_WIRE.id) {
            return true;
        }
        if (i == 0) {
            return false;
        }
        return Block.BY_ID[i].isSignalSource();
    }
}

