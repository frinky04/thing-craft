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
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class RedstoneOreBlock
extends Block {
    private boolean lit;

    public RedstoneOreBlock(int id, int sprite, boolean lit) {
        super(id, sprite, Material.STONE);
        if (lit) {
            this.setTicksRandomly(true);
        }
        this.lit = lit;
    }

    public int getTickRate() {
        return 30;
    }

    public void startMining(World world, int x, int y, int z, PlayerEntity player) {
        this.interact(world, x, y, z);
        super.startMining(world, x, y, z, player);
    }

    public void onSteppedOn(World world, int x, int y, int z, Entity entity) {
        this.interact(world, x, y, z);
        super.onSteppedOn(world, x, y, z, entity);
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        this.interact(world, x, y, z);
        return super.use(world, x, y, z, player);
    }

    private void interact(World world, int x, int y, int z) {
        this.addParticles(world, x, y, z);
        if (this.id == Block.REDSTONE_ORE.id) {
            world.setBlock(x, y, z, Block.LIT_REDSTONE_ORE.id);
        }
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (this.id == Block.LIT_REDSTONE_ORE.id) {
            world.setBlock(x, y, z, Block.REDSTONE_ORE.id);
        }
    }

    public int getDropItem(int metadata, Random random) {
        return Item.REDSTONE.id;
    }

    public int getBaseDropCount(Random random) {
        return 4 + random.nextInt(2);
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (this.lit) {
            this.addParticles(world, x, y, z);
        }
    }

    private void addParticles(World world, int x, int y, int z) {
        Random random = world.random;
        double d = 0.0625;
        for (int i = 0; i < 6; ++i) {
            double e = (float)x + random.nextFloat();
            double f = (float)y + random.nextFloat();
            double g = (float)z + random.nextFloat();
            if (i == 0 && !world.isSolidBlock(x, y + 1, z)) {
                f = (double)(y + 1) + d;
            }
            if (i == 1 && !world.isSolidBlock(x, y - 1, z)) {
                f = (double)(y + 0) - d;
            }
            if (i == 2 && !world.isSolidBlock(x, y, z + 1)) {
                g = (double)(z + 1) + d;
            }
            if (i == 3 && !world.isSolidBlock(x, y, z - 1)) {
                g = (double)(z + 0) - d;
            }
            if (i == 4 && !world.isSolidBlock(x + 1, y, z)) {
                e = (double)(x + 1) + d;
            }
            if (i == 5 && !world.isSolidBlock(x - 1, y, z)) {
                e = (double)(x + 0) - d;
            }
            if (!(e < (double)x || e > (double)(x + 1) || f < 0.0 || f > (double)(y + 1) || g < (double)z) && !(g > (double)(z + 1))) continue;
            world.addParticle("reddust", e, f, g, 0.0, 0.0, 0.0);
        }
    }
}

