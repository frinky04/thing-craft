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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public abstract class LiquidBlock
extends Block {
    protected LiquidBlock(int i, Material material) {
        super(i, (material == Material.LAVA ? 14 : 12) * 16 + 13, material);
        float f = 0.0f;
        float g = 0.0f;
        this.setShape(0.0f + g, 0.0f + f, 0.0f + g, 1.0f + g, 1.0f + f, 1.0f + g);
        this.setTicksRandomly(true);
    }

    public static float getHeightLoss(int state) {
        if (state >= 8) {
            state = 0;
        }
        float f = (float)(state + 1) / 9.0f;
        return f;
    }

    public int getSprite(int face) {
        if (face == 0 || face == 1) {
            return this.sprite;
        }
        return this.sprite + 1;
    }

    protected int getLiquidState(World world, int x, int y, int z) {
        if (world.getMaterial(x, y, z) != this.material) {
            return -1;
        }
        return world.getBlockMetadata(x, y, z);
    }

    protected int getLiquidDepth(WorldView world, int x, int y, int z) {
        if (world.getMaterial(x, y, z) != this.material) {
            return -1;
        }
        int i = world.getBlockMetadata(x, y, z);
        if (i >= 8) {
            i = 0;
        }
        return i;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return false;
    }

    public boolean isSolid() {
        return false;
    }

    public boolean canRayTrace(int metadata, boolean allowLiquids) {
        return allowLiquids && metadata == 0;
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        Material material = world.getMaterial(x, y, z);
        if (material == this.material) {
            return false;
        }
        if (material == Material.ICE) {
            return false;
        }
        if (face == 1) {
            return true;
        }
        return super.shouldRenderFace(world, x, y, z, face);
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 4;
    }

    public int getDropItem(int metadata, Random random) {
        return 0;
    }

    public int getBaseDropCount(Random random) {
        return 0;
    }

    private Vec3d getFlow(WorldView world, int x, int y, int z) {
        int j;
        Vec3d vec3d = Vec3d.fromPool(0.0, 0.0, 0.0);
        int i = this.getLiquidDepth(world, x, y, z);
        for (j = 0; j < 4; ++j) {
            int n;
            int k = x;
            int l = y;
            int m = z;
            if (j == 0) {
                --k;
            }
            if (j == 1) {
                --m;
            }
            if (j == 2) {
                ++k;
            }
            if (j == 3) {
                ++m;
            }
            if ((n = this.getLiquidDepth(world, k, l, m)) < 0) {
                if (world.getMaterial(k, l, m).blocksMovement() || (n = this.getLiquidDepth(world, k, l - 1, m)) < 0) continue;
                int o = n - (i - 8);
                vec3d = vec3d.add((k - x) * o, (l - y) * o, (m - z) * o);
                continue;
            }
            if (n < 0) continue;
            int p = n - i;
            vec3d = vec3d.add((k - x) * p, (l - y) * p, (m - z) * p);
        }
        if (world.getBlockMetadata(x, y, z) >= 8) {
            j = 0;
            if (j != 0 || this.shouldRenderFace(world, x, y, z - 1, 2)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x, y, z + 1, 3)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x - 1, y, z, 4)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x + 1, y, z, 5)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x, y + 1, z - 1, 2)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x, y + 1, z + 1, 3)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x - 1, y + 1, z, 4)) {
                j = 1;
            }
            if (j != 0 || this.shouldRenderFace(world, x + 1, y + 1, z, 5)) {
                j = 1;
            }
            if (j != 0) {
                vec3d = vec3d.normalize().add(0.0, -6.0, 0.0);
            }
        }
        vec3d = vec3d.normalize();
        return vec3d;
    }

    public void applyMaterialDrag(World world, int x, int y, int z, Entity entity, Vec3d velocity) {
        Vec3d vec3d = this.getFlow(world, x, y, z);
        velocity.x += vec3d.x;
        velocity.y += vec3d.y;
        velocity.z += vec3d.z;
    }

    public int getTickRate() {
        if (this.material == Material.WATER) {
            return 5;
        }
        if (this.material == Material.LAVA) {
            return 30;
        }
        return 0;
    }

    @Environment(value=EnvType.CLIENT)
    public float getBrightness(WorldView world, int x, int y, int z) {
        float g;
        float f = world.getBrightness(x, y, z);
        return f > (g = world.getBrightness(x, y + 1, z)) ? f : g;
    }

    public void tick(World world, int x, int y, int z, Random random) {
        super.tick(world, x, y, z, random);
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderLayer() {
        return this.material == Material.WATER ? 1 : 0;
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        int i;
        if (this.material == Material.WATER && random.nextInt(64) == 0 && (i = world.getBlockMetadata(x, y, z)) > 0 && i < 8) {
            world.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, "liquid.water", random.nextFloat() * 0.25f + 0.75f, random.nextFloat() * 1.0f + 0.5f);
        }
        if (this.material == Material.LAVA && world.getMaterial(x, y + 1, z) == Material.AIR && !world.isSolidBlock(x, y + 1, z) && random.nextInt(100) == 0) {
            double d = (float)x + random.nextFloat();
            double e = (double)y + this.maxY;
            double f = (float)z + random.nextFloat();
            world.addParticle("lava", d, e, f, 0.0, 0.0, 0.0);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static double getFlowAngle(WorldView world, int x, int y, int z, Material material) {
        Vec3d vec3d;
        Object object = null;
        if (material == Material.WATER) {
            vec3d = ((LiquidBlock)Block.FLOWING_WATER).getFlow(world, x, y, z);
        }
        if (material == Material.LAVA) {
            vec3d = ((LiquidBlock)Block.FLOWING_LAVA).getFlow(world, x, y, z);
        }
        if (vec3d.x == 0.0 && vec3d.z == 0.0) {
            return -1000.0;
        }
        return Math.atan2(vec3d.z, vec3d.x) - 1.5707963267948966;
    }

    public void onAdded(World world, int x, int y, int z) {
        this.checkBlockCollisions(world, x, y, z);
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
        this.checkBlockCollisions(world, x, y, z);
    }

    private void checkBlockCollisions(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != this.id) {
            return;
        }
        if (this.material == Material.LAVA) {
            boolean i = false;
            if (i || world.getMaterial(x, y, z - 1) == Material.WATER) {
                i = true;
            }
            if (i || world.getMaterial(x, y, z + 1) == Material.WATER) {
                i = true;
            }
            if (i || world.getMaterial(x - 1, y, z) == Material.WATER) {
                i = true;
            }
            if (i || world.getMaterial(x + 1, y, z) == Material.WATER) {
                i = true;
            }
            if (i || world.getMaterial(x, y + 1, z) == Material.WATER) {
                i = true;
            }
            if (i) {
                int j = world.getBlockMetadata(x, y, z);
                if (j == 0) {
                    world.setBlock(x, y, z, Block.OBSIDIAN.id);
                } else if (j <= 4) {
                    world.setBlock(x, y, z, Block.COBBLESTONE.id);
                }
                this.fizz(world, x, y, z);
            }
        }
    }

    protected void fizz(World world, int x, int y, int z) {
        world.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, "random.fizz", 0.5f, 2.6f + (world.random.nextFloat() - world.random.nextFloat()) * 0.8f);
        for (int i = 0; i < 8; ++i) {
            world.addParticle("largesmoke", (double)x + Math.random(), (double)y + 1.2, (double)z + Math.random(), 0.0, 0.0, 0.0);
        }
    }
}

