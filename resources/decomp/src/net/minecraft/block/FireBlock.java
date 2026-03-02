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
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class FireBlock
extends Block {
    private int[] flammability = new int[256];
    private int[] burnChance = new int[256];

    protected FireBlock(int id, int sprite) {
        super(id, sprite, Material.FIRE);
        this.setFlammable(Block.PLANKS.id, 5, 20);
        this.setFlammable(Block.LOG.id, 5, 5);
        this.setFlammable(Block.LEAVES.id, 30, 60);
        this.setFlammable(Block.BOOKSHELF.id, 30, 20);
        this.setFlammable(Block.TNT.id, 15, 100);
        this.setFlammable(Block.WOOL.id, 30, 60);
        this.setTicksRandomly(true);
    }

    private void setFlammable(int block, int flammability, int burnChance) {
        this.flammability[block] = flammability;
        this.burnChance[block] = burnChance;
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
        return 3;
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    public int getTickRate() {
        return 10;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        boolean i = world.getBlock(x, y - 1, z) == Block.NETHERRACK.id;
        int j = world.getBlockMetadata(x, y, z);
        if (j < 15) {
            world.setBlockMetadata(x, y, z, j + 1);
            world.scheduleTick(x, y, z, this.id);
        }
        if (!i && !this.hasFlammableNeighbor(world, x, y, z)) {
            if (!world.isSolidBlock(x, y - 1, z) || j > 3) {
                world.setBlock(x, y, z, 0);
            }
            return;
        }
        if (!i && !this.isFlammable(world, x, y - 1, z) && j == 15 && random.nextInt(4) == 0) {
            world.setBlock(x, y, z, 0);
            return;
        }
        if (j % 2 == 0 && j > 2) {
            this.burnNeighbor(world, x + 1, y, z, 300, random);
            this.burnNeighbor(world, x - 1, y, z, 300, random);
            this.burnNeighbor(world, x, y - 1, z, 250, random);
            this.burnNeighbor(world, x, y + 1, z, 250, random);
            this.burnNeighbor(world, x, y, z - 1, 300, random);
            this.burnNeighbor(world, x, y, z + 1, 300, random);
            for (int k = x - 1; k <= x + 1; ++k) {
                for (int l = z - 1; l <= z + 1; ++l) {
                    for (int m = y - 1; m <= y + 4; ++m) {
                        int o;
                        if (k == x && m == y && l == z) continue;
                        int n = 100;
                        if (m > y + 1) {
                            n += (m - (y + 1)) * 100;
                        }
                        if ((o = this.getSpreadChance(world, k, m, l)) <= 0 || random.nextInt(n) > o) continue;
                        world.setBlock(k, m, l, this.id);
                    }
                }
            }
        }
    }

    private void burnNeighbor(World world, int x, int y, int z, int chance, Random random) {
        int i = this.burnChance[world.getBlock(x, y, z)];
        if (random.nextInt(chance) < i) {
            boolean j;
            boolean bl = j = world.getBlock(x, y, z) == Block.TNT.id;
            if (random.nextInt(2) == 0) {
                world.setBlock(x, y, z, this.id);
            } else {
                world.setBlock(x, y, z, 0);
            }
            if (j) {
                Block.TNT.onBroken(world, x, y, z, 0);
            }
        }
    }

    private boolean hasFlammableNeighbor(World world, int x, int y, int z) {
        if (this.isFlammable(world, x + 1, y, z)) {
            return true;
        }
        if (this.isFlammable(world, x - 1, y, z)) {
            return true;
        }
        if (this.isFlammable(world, x, y - 1, z)) {
            return true;
        }
        if (this.isFlammable(world, x, y + 1, z)) {
            return true;
        }
        if (this.isFlammable(world, x, y, z - 1)) {
            return true;
        }
        return this.isFlammable(world, x, y, z + 1);
    }

    private int getSpreadChance(World world, int x, int y, int z) {
        int i = 0;
        if (world.getBlock(x, y, z) != 0) {
            return 0;
        }
        i = this.getFlammability(world, x + 1, y, z, i);
        i = this.getFlammability(world, x - 1, y, z, i);
        i = this.getFlammability(world, x, y - 1, z, i);
        i = this.getFlammability(world, x, y + 1, z, i);
        i = this.getFlammability(world, x, y, z - 1, i);
        i = this.getFlammability(world, x, y, z + 1, i);
        return i;
    }

    public boolean canRayTrace() {
        return false;
    }

    public boolean isFlammable(WorldView world, int x, int y, int z) {
        return this.flammability[world.getBlock(x, y, z)] > 0;
    }

    public int getFlammability(World world, int x, int y, int z, int min) {
        int i = this.flammability[world.getBlock(x, y, z)];
        if (i > min) {
            return i;
        }
        return min;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        return world.isSolidBlock(x, y - 1, z) || this.hasFlammableNeighbor(world, x, y, z);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        if (!world.isSolidBlock(x, y - 1, z) && !this.hasFlammableNeighbor(world, x, y, z)) {
            world.setBlock(x, y, z, 0);
            return;
        }
    }

    public void onAdded(World world, int x, int y, int z) {
        if (world.getBlock(x, y - 1, z) == Block.OBSIDIAN.id && Block.NETHER_PORTAL.create(world, x, y, z)) {
            return;
        }
        if (!world.isSolidBlock(x, y - 1, z) && !this.hasFlammableNeighbor(world, x, y, z)) {
            world.setBlock(x, y, z, 0);
            return;
        }
        world.scheduleTick(x, y, z, this.id);
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        block12: {
            block11: {
                if (random.nextInt(24) == 0) {
                    world.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, "fire.fire", 1.0f + random.nextFloat(), random.nextFloat() * 0.7f + 0.3f);
                }
                if (!world.isSolidBlock(x, y - 1, z) && !Block.FIRE.isFlammable(world, x, y - 1, z)) break block11;
                for (int i = 0; i < 3; ++i) {
                    float f = (float)x + random.nextFloat();
                    float r = (float)y + random.nextFloat() * 0.5f + 0.5f;
                    float aa = (float)z + random.nextFloat();
                    world.addParticle("largesmoke", f, r, aa, 0.0, 0.0, 0.0);
                }
                break block12;
            }
            if (Block.FIRE.isFlammable(world, x - 1, y, z)) {
                for (int j = 0; j < 2; ++j) {
                    float g = (float)x + random.nextFloat() * 0.1f;
                    float s = (float)y + random.nextFloat();
                    float ab = (float)z + random.nextFloat();
                    world.addParticle("largesmoke", g, s, ab, 0.0, 0.0, 0.0);
                }
            }
            if (Block.FIRE.isFlammable(world, x + 1, y, z)) {
                for (int k = 0; k < 2; ++k) {
                    float h = (float)(x + 1) - random.nextFloat() * 0.1f;
                    float t = (float)y + random.nextFloat();
                    float ac = (float)z + random.nextFloat();
                    world.addParticle("largesmoke", h, t, ac, 0.0, 0.0, 0.0);
                }
            }
            if (Block.FIRE.isFlammable(world, x, y, z - 1)) {
                for (int l = 0; l < 2; ++l) {
                    float o = (float)x + random.nextFloat();
                    float u = (float)y + random.nextFloat();
                    float ad = (float)z + random.nextFloat() * 0.1f;
                    world.addParticle("largesmoke", o, u, ad, 0.0, 0.0, 0.0);
                }
            }
            if (Block.FIRE.isFlammable(world, x, y, z + 1)) {
                for (int m = 0; m < 2; ++m) {
                    float p = (float)x + random.nextFloat();
                    float v = (float)y + random.nextFloat();
                    float ae = (float)(z + 1) - random.nextFloat() * 0.1f;
                    world.addParticle("largesmoke", p, v, ae, 0.0, 0.0, 0.0);
                }
            }
            if (!Block.FIRE.isFlammable(world, x, y + 1, z)) break block12;
            for (int n = 0; n < 2; ++n) {
                float q = (float)x + random.nextFloat();
                float w = (float)(y + 1) - random.nextFloat() * 0.1f;
                float af = (float)z + random.nextFloat();
                world.addParticle("largesmoke", q, w, af, 0.0, 0.0, 0.0);
            }
        }
    }
}

