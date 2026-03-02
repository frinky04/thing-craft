/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.TorchBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RedstoneTorchBlock
extends TorchBlock {
    private boolean lit = false;
    private static List RECENT_TOGGLES = new ArrayList();

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        if (face == 1) {
            return Block.REDSTONE_WIRE.getSprite(face, metadata);
        }
        return super.getSprite(face, metadata);
    }

    private boolean shouldBurnOut(World world, int x, int y, int z, boolean logToggle) {
        if (logToggle) {
            RECENT_TOGGLES.add(new ToggleEntry(x, y, z, world.ticks));
        }
        int i = 0;
        for (int j = 0; j < RECENT_TOGGLES.size(); ++j) {
            ToggleEntry toggleEntry = (ToggleEntry)RECENT_TOGGLES.get(j);
            if (toggleEntry.x != x || toggleEntry.y != y || toggleEntry.z != z || ++i < 8) continue;
            return true;
        }
        return false;
    }

    protected RedstoneTorchBlock(int id, int sprite, boolean lit) {
        super(id, sprite);
        this.lit = lit;
        this.setTicksRandomly(true);
    }

    public int getTickRate() {
        return 2;
    }

    public void onAdded(World world, int x, int y, int z) {
        if (world.getBlockMetadata(x, y, z) == 0) {
            super.onAdded(world, x, y, z);
        }
        if (this.lit) {
            world.updateNeighbors(x, y - 1, z, this.id);
            world.updateNeighbors(x, y + 1, z, this.id);
            world.updateNeighbors(x - 1, y, z, this.id);
            world.updateNeighbors(x + 1, y, z, this.id);
            world.updateNeighbors(x, y, z - 1, this.id);
            world.updateNeighbors(x, y, z + 1, this.id);
        }
    }

    public void onRemoved(World world, int x, int y, int z) {
        if (this.lit) {
            world.updateNeighbors(x, y - 1, z, this.id);
            world.updateNeighbors(x, y + 1, z, this.id);
            world.updateNeighbors(x - 1, y, z, this.id);
            world.updateNeighbors(x + 1, y, z, this.id);
            world.updateNeighbors(x, y, z - 1, this.id);
            world.updateNeighbors(x, y, z + 1, this.id);
        }
    }

    public boolean hasSignal(WorldView world, int x, int y, int z, int dir) {
        if (!this.lit) {
            return false;
        }
        int i = world.getBlockMetadata(x, y, z);
        if (i == 5 && dir == 1) {
            return false;
        }
        if (i == 3 && dir == 3) {
            return false;
        }
        if (i == 4 && dir == 2) {
            return false;
        }
        if (i == 1 && dir == 5) {
            return false;
        }
        return i != 2 || dir != 4;
    }

    private boolean hasNeighborSignal(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        if (i == 5 && world.hasSignal(x, y - 1, z, 0)) {
            return true;
        }
        if (i == 3 && world.hasSignal(x, y, z - 1, 2)) {
            return true;
        }
        if (i == 4 && world.hasSignal(x, y, z + 1, 3)) {
            return true;
        }
        if (i == 1 && world.hasSignal(x - 1, y, z, 4)) {
            return true;
        }
        return i == 2 && world.hasSignal(x + 1, y, z, 5);
    }

    public void tick(World world, int x, int y, int z, Random random) {
        boolean i = this.hasNeighborSignal(world, x, y, z);
        while (RECENT_TOGGLES.size() > 0 && world.ticks - ((ToggleEntry)RedstoneTorchBlock.RECENT_TOGGLES.get((int)0)).time > 100L) {
            RECENT_TOGGLES.remove(0);
        }
        if (this.lit) {
            if (i) {
                world.setBlockWithMetadata(x, y, z, Block.UNLIT_REDSTONE_TORCH.id, world.getBlockMetadata(x, y, z));
                if (this.shouldBurnOut(world, x, y, z, true)) {
                    world.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, "random.fizz", 0.5f, 2.6f + (world.random.nextFloat() - world.random.nextFloat()) * 0.8f);
                    for (int j = 0; j < 5; ++j) {
                        double d = (double)x + random.nextDouble() * 0.6 + 0.2;
                        double e = (double)y + random.nextDouble() * 0.6 + 0.2;
                        double f = (double)z + random.nextDouble() * 0.6 + 0.2;
                        world.addParticle("smoke", d, e, f, 0.0, 0.0, 0.0);
                    }
                }
            }
        } else if (!i && !this.shouldBurnOut(world, x, y, z, false)) {
            world.setBlockWithMetadata(x, y, z, Block.REDSTONE_TORCH.id, world.getBlockMetadata(x, y, z));
        }
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        super.neighborChanged(world, x, y, z, neighborBlock);
        world.scheduleTick(x, y, z, this.id);
    }

    public boolean hasDirectSignal(World world, int x, int y, int z, int dir) {
        if (dir == 0) {
            return this.hasSignal(world, x, y, z, dir);
        }
        return false;
    }

    public int getDropItem(int metadata, Random random) {
        return Block.REDSTONE_TORCH.id;
    }

    public boolean isSignalSource() {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (!this.lit) {
            return;
        }
        int i = world.getBlockMetadata(x, y, z);
        double d = (double)((float)x + 0.5f) + (double)(random.nextFloat() - 0.5f) * 0.2;
        double e = (double)((float)y + 0.7f) + (double)(random.nextFloat() - 0.5f) * 0.2;
        double f = (double)((float)z + 0.5f) + (double)(random.nextFloat() - 0.5f) * 0.2;
        double g = 0.22f;
        double h = 0.27f;
        if (i == 1) {
            world.addParticle("reddust", d - h, e + g, f, 0.0, 0.0, 0.0);
        } else if (i == 2) {
            world.addParticle("reddust", d + h, e + g, f, 0.0, 0.0, 0.0);
        } else if (i == 3) {
            world.addParticle("reddust", d, e + g, f - h, 0.0, 0.0, 0.0);
        } else if (i == 4) {
            world.addParticle("reddust", d, e + g, f + h, 0.0, 0.0, 0.0);
        } else {
            world.addParticle("reddust", d, e, f, 0.0, 0.0, 0.0);
        }
    }

    static class ToggleEntry {
        int x;
        int y;
        int z;
        long time;

        public ToggleEntry(int x, int y, int z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }
}

