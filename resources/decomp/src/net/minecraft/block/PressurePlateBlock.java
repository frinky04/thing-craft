/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class PressurePlateBlock
extends Block {
    private ActivationRule rule;

    protected PressurePlateBlock(int id, int sprite, ActivationRule rule) {
        super(id, sprite, Material.STONE);
        this.rule = rule;
        this.setTicksRandomly(true);
        float f = 0.0625f;
        this.setShape(f, 0.0f, f, 1.0f - f, 0.03125f, 1.0f - f);
    }

    public int getTickRate() {
        return 20;
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

    public boolean canBePlaced(World world, int x, int y, int z) {
        return world.isSolidBlock(x, y - 1, z);
    }

    public void onAdded(World world, int x, int y, int z) {
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        boolean i = false;
        if (!world.isSolidBlock(x, y - 1, z)) {
            i = true;
        }
        if (i) {
            this.dropItems(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.setBlock(x, y, z, 0);
        }
    }

    public void tick(World world, int x, int y, int z, Random random) {
        if (world.isMultiplayer) {
            return;
        }
        if (world.getBlockMetadata(x, y, z) == 0) {
            return;
        }
        this.updateOutputState(world, x, y, z);
    }

    public void onEntityCollision(World world, int x, int y, int z, Entity entity) {
        if (world.isMultiplayer) {
            return;
        }
        if (world.getBlockMetadata(x, y, z) == 1) {
            return;
        }
        this.updateOutputState(world, x, y, z);
    }

    private void updateOutputState(World world, int x, int y, int z) {
        List list;
        boolean i = world.getBlockMetadata(x, y, z) == 1;
        boolean j = false;
        float f = 0.125f;
        Object object = null;
        if (this.rule == ActivationRule.EVERYTHING) {
            list = world.getEntities(null, Box.fromPool((float)x + f, y, (float)z + f, (float)(x + 1) - f, (double)y + 0.25, (float)(z + 1) - f));
        }
        if (this.rule == ActivationRule.MOBS) {
            list = world.getEntitiesOfType(MobEntity.class, Box.fromPool((float)x + f, y, (float)z + f, (float)(x + 1) - f, (double)y + 0.25, (float)(z + 1) - f));
        }
        if (this.rule == ActivationRule.PLAYERS) {
            list = world.getEntitiesOfType(PlayerEntity.class, Box.fromPool((float)x + f, y, (float)z + f, (float)(x + 1) - f, (double)y + 0.25, (float)(z + 1) - f));
        }
        if (list.size() > 0) {
            j = true;
        }
        if (j && !i) {
            world.setBlockMetadata(x, y, z, 1);
            world.updateNeighbors(x, y, z, this.id);
            world.updateNeighbors(x, y - 1, z, this.id);
            world.notifyRegionChanged(x, y, z, x, y, z);
            world.playSound((double)x + 0.5, (double)y + 0.1, (double)z + 0.5, "random.click", 0.3f, 0.6f);
        }
        if (!j && i) {
            world.setBlockMetadata(x, y, z, 0);
            world.updateNeighbors(x, y, z, this.id);
            world.updateNeighbors(x, y - 1, z, this.id);
            world.notifyRegionChanged(x, y, z, x, y, z);
            world.playSound((double)x + 0.5, (double)y + 0.1, (double)z + 0.5, "random.click", 0.3f, 0.5f);
        }
        if (j) {
            world.scheduleTick(x, y, z, this.id);
        }
    }

    public void onRemoved(World world, int x, int y, int z) {
        int i = world.getBlockMetadata(x, y, z);
        if (i > 0) {
            world.updateNeighbors(x, y, z, this.id);
            world.updateNeighbors(x, y - 1, z, this.id);
        }
        super.onRemoved(world, x, y, z);
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        boolean i = world.getBlockMetadata(x, y, z) == 1;
        float f = 0.0625f;
        if (i) {
            this.setShape(f, 0.0f, f, 1.0f - f, 0.03125f, 1.0f - f);
        } else {
            this.setShape(f, 0.0f, f, 1.0f - f, 0.0625f, 1.0f - f);
        }
    }

    public boolean hasSignal(WorldView world, int x, int y, int z, int dir) {
        return world.getBlockMetadata(x, y, z) > 0;
    }

    public boolean hasDirectSignal(World world, int x, int y, int z, int dir) {
        if (world.getBlockMetadata(x, y, z) == 0) {
            return false;
        }
        return dir == 1;
    }

    public boolean isSignalSource() {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public void resetShape() {
        float f = 0.5f;
        float g = 0.125f;
        float h = 0.5f;
        this.setShape(0.5f - f, 0.5f - g, 0.5f - h, 0.5f + f, 0.5f + g, 0.5f + h);
    }

    public static enum ActivationRule {
        EVERYTHING,
        MOBS,
        PLAYERS;

    }
}

