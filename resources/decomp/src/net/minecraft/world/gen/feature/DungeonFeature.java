/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;

public class DungeonFeature
extends Feature {
    public boolean place(World world, Random random, int x, int y, int z) {
        int m;
        int i = 3;
        int j = random.nextInt(2) + 2;
        int k = random.nextInt(2) + 2;
        int l = 0;
        for (m = x - j - 1; m <= x + j + 1; ++m) {
            for (int n = y - 1; n <= y + i + 1; ++n) {
                for (int q = z - k - 1; q <= z + k + 1; ++q) {
                    Material material = world.getMaterial(m, n, q);
                    if (n == y - 1 && !material.isSolid()) {
                        return false;
                    }
                    if (n == y + i + 1 && !material.isSolid()) {
                        return false;
                    }
                    if (m != x - j - 1 && m != x + j + 1 && q != z - k - 1 && q != z + k + 1 || n != y || world.getBlock(m, n, q) != 0 || world.getBlock(m, n + 1, q) != 0) continue;
                    ++l;
                }
            }
        }
        if (l < 1 || l > 5) {
            return false;
        }
        for (m = x - j - 1; m <= x + j + 1; ++m) {
            for (int o = y + i; o >= y - 1; --o) {
                for (int r = z - k - 1; r <= z + k + 1; ++r) {
                    if (m == x - j - 1 || o == y - 1 || r == z - k - 1 || m == x + j + 1 || o == y + i + 1 || r == z + k + 1) {
                        if (o >= 0 && !world.getMaterial(m, o - 1, r).isSolid()) {
                            world.setBlock(m, o, r, 0);
                            continue;
                        }
                        if (!world.getMaterial(m, o, r).isSolid()) continue;
                        if (o == y - 1 && random.nextInt(4) != 0) {
                            world.setBlock(m, o, r, Block.MOSSY_COBBLESTONE.id);
                            continue;
                        }
                        world.setBlock(m, o, r, Block.COBBLESTONE.id);
                        continue;
                    }
                    world.setBlock(m, o, r, 0);
                }
            }
        }
        block6: for (m = 0; m < 2; ++m) {
            for (int p = 0; p < 3; ++p) {
                int u;
                int t;
                int s = x + random.nextInt(j * 2 + 1) - j;
                if (world.getBlock(s, t = y, u = z + random.nextInt(k * 2 + 1) - k) != 0) continue;
                int v = 0;
                if (world.getMaterial(s - 1, t, u).isSolid()) {
                    ++v;
                }
                if (world.getMaterial(s + 1, t, u).isSolid()) {
                    ++v;
                }
                if (world.getMaterial(s, t, u - 1).isSolid()) {
                    ++v;
                }
                if (world.getMaterial(s, t, u + 1).isSolid()) {
                    ++v;
                }
                if (v != 1) continue;
                world.setBlock(s, t, u, Block.CHEST.id);
                ChestBlockEntity chestBlockEntity = (ChestBlockEntity)world.getBlockEntity(s, t, u);
                for (int w = 0; w < 8; ++w) {
                    ItemStack itemStack = this.pickLoot(random);
                    if (itemStack == null) continue;
                    chestBlockEntity.setItem(random.nextInt(chestBlockEntity.getSize()), itemStack);
                }
                continue block6;
            }
        }
        world.setBlock(x, y, z, Block.MOB_SPAWNER.id);
        MobSpawnerBlockEntity mobSpawnerBlockEntity = (MobSpawnerBlockEntity)world.getBlockEntity(x, y, z);
        mobSpawnerBlockEntity.type = this.pickDungeonType(random);
        return true;
    }

    private ItemStack pickLoot(Random random) {
        int i = random.nextInt(11);
        if (i == 0) {
            return new ItemStack(Item.SADDLE);
        }
        if (i == 1) {
            return new ItemStack(Item.IRON_INGOT, random.nextInt(4) + 1);
        }
        if (i == 2) {
            return new ItemStack(Item.BREAD);
        }
        if (i == 3) {
            return new ItemStack(Item.WHEAT, random.nextInt(4) + 1);
        }
        if (i == 4) {
            return new ItemStack(Item.GUNPOWDER, random.nextInt(4) + 1);
        }
        if (i == 5) {
            return new ItemStack(Item.STRING, random.nextInt(4) + 1);
        }
        if (i == 6) {
            return new ItemStack(Item.BUCKET);
        }
        if (i == 7 && random.nextInt(100) == 0) {
            return new ItemStack(Item.GOLDEN_APPLE);
        }
        if (i == 8 && random.nextInt(2) == 0) {
            return new ItemStack(Item.REDSTONE, random.nextInt(4) + 1);
        }
        if (i == 9 && random.nextInt(10) == 0) {
            return new ItemStack(Item.BY_ID[Item.RECORD_13.id + random.nextInt(2)]);
        }
        return null;
    }

    private String pickDungeonType(Random random) {
        int i = random.nextInt(4);
        if (i == 0) {
            return "Skeleton";
        }
        if (i == 1) {
            return "Zombie";
        }
        if (i == 2) {
            return "Zombie";
        }
        if (i == 3) {
            return "Spider";
        }
        return "";
    }
}

