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
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class PortalBlock
extends TransparentBlock {
    public PortalBlock(int id, int sprite) {
        super(id, sprite, Material.PORTAL, false);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    public void updateShape(WorldView world, int x, int y, int z) {
        if (world.getBlock(x - 1, y, z) == this.id || world.getBlock(x + 1, y, z) == this.id) {
            float f = 0.5f;
            float h = 0.125f;
            this.setShape(0.5f - f, 0.0f, 0.5f - h, 0.5f + f, 1.0f, 0.5f + h);
        } else {
            float g = 0.125f;
            float i = 0.5f;
            this.setShape(0.5f - g, 0.0f, 0.5f - i, 0.5f + g, 1.0f, 0.5f + i);
        }
    }

    public boolean isSolid() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    public boolean create(World world, int x, int y, int z) {
        int k;
        int i = 0;
        int j = 0;
        if (world.getBlock(x - 1, y, z) == Block.OBSIDIAN.id || world.getBlock(x + 1, y, z) == Block.OBSIDIAN.id) {
            i = 1;
        }
        if (world.getBlock(x, y, z - 1) == Block.OBSIDIAN.id || world.getBlock(x, y, z + 1) == Block.OBSIDIAN.id) {
            j = 1;
        }
        System.out.println(i + ", " + j);
        if (i == j) {
            return false;
        }
        if (world.getBlock(x - i, y, z - j) == 0) {
            x -= i;
            z -= j;
        }
        for (k = -1; k <= 2; ++k) {
            for (int l = -1; l <= 3; ++l) {
                boolean n;
                boolean bl = n = k == -1 || k == 2 || l == -1 || l == 3;
                if (!(k != -1 && k != 2 || l != -1 && l != 3)) continue;
                int o = world.getBlock(x + i * k, y + l, z + j * k);
                if (!(n ? o != Block.OBSIDIAN.id : o != 0 && o != Block.FIRE.id)) continue;
                return false;
            }
        }
        world.suppressNeighborChangedUpdates = true;
        for (k = 0; k < 2; ++k) {
            for (int m = 0; m < 3; ++m) {
                world.setBlock(x + i * k, y + m, z + j * k, Block.NETHER_PORTAL.id);
            }
        }
        world.suppressNeighborChangedUpdates = false;
        return true;
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        boolean n;
        int l;
        int i = 0;
        int j = 1;
        if (world.getBlock(x - 1, y, z) == this.id || world.getBlock(x + 1, y, z) == this.id) {
            i = 1;
            j = 0;
        }
        int k = y;
        while (world.getBlock(x, k - 1, z) == this.id) {
            --k;
        }
        if (world.getBlock(x, k - 1, z) != Block.OBSIDIAN.id) {
            world.setBlock(x, y, z, 0);
            return;
        }
        for (l = 1; l < 4 && world.getBlock(x, k + l, z) == this.id; ++l) {
        }
        if (l != 3 || world.getBlock(x, k + l, z) != Block.OBSIDIAN.id) {
            world.setBlock(x, y, z, 0);
            return;
        }
        boolean m = world.getBlock(x - 1, y, z) == this.id || world.getBlock(x + 1, y, z) == this.id;
        boolean bl = n = world.getBlock(x, y, z - 1) == this.id || world.getBlock(x, y, z + 1) == this.id;
        if (m && n) {
            world.setBlock(x, y, z, 0);
            return;
        }
        if (!(world.getBlock(x + i, y, z + j) == Block.OBSIDIAN.id && world.getBlock(x - i, y, z - j) == this.id || world.getBlock(x - i, y, z - j) == Block.OBSIDIAN.id && world.getBlock(x + i, y, z + j) == this.id)) {
            world.setBlock(x, y, z, 0);
            return;
        }
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        return true;
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderLayer() {
        return 1;
    }

    public void onEntityCollision(World world, int x, int y, int z, Entity entity) {
        if (world.isMultiplayer) {
            return;
        }
        entity.onPortalCollision();
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(100) == 0) {
            world.playSound((double)x + 0.5, (double)y + 0.5, (double)z + 0.5, "portal.portal", 1.0f, random.nextFloat() * 0.4f + 0.8f);
        }
        for (int i = 0; i < 4; ++i) {
            double d = (float)x + random.nextFloat();
            double e = (float)y + random.nextFloat();
            double f = (float)z + random.nextFloat();
            double g = 0.0;
            double h = 0.0;
            double j = 0.0;
            int k = random.nextInt(2) * 2 - 1;
            g = ((double)random.nextFloat() - 0.5) * 0.5;
            h = ((double)random.nextFloat() - 0.5) * 0.5;
            j = ((double)random.nextFloat() - 0.5) * 0.5;
            if (world.getBlock(x - 1, y, z) == this.id || world.getBlock(x + 1, y, z) == this.id) {
                f = (double)z + 0.5 + 0.25 * (double)k;
                j = random.nextFloat() * 2.0f * (float)k;
            } else {
                d = (double)x + 0.5 + 0.25 * (double)k;
                g = random.nextFloat() * 2.0f * (float)k;
            }
            world.addParticle("portal", d, e, f, g, h, j);
        }
    }
}

