/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobCategory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.monster.SkeletonEntity;
import net.minecraft.entity.mob.monster.SpiderEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class NaturalSpawner {
    private static Set mobSpawningChunks = new HashSet();

    protected static BlockPos getRandomPosInChunk(World world, int chunkX, int chunkZ) {
        int i = chunkX + world.random.nextInt(16);
        int j = world.random.nextInt(128);
        int k = chunkZ + world.random.nextInt(16);
        return new BlockPos(i, j, k);
    }

    /*
     * WARNING - void declaration
     */
    public static final int tick(World world) {
        int i;
        mobSpawningChunks.clear();
        for (i = 0; i < world.players.size(); ++i) {
            PlayerEntity playerEntity = (PlayerEntity)world.players.get(i);
            int k = MathHelper.floor(playerEntity.x / 16.0);
            int l = MathHelper.floor(playerEntity.z / 16.0);
            int m = 8;
            for (int n = -m; n <= m; ++n) {
                for (int o = -m; o <= m; ++o) {
                    mobSpawningChunks.add(new ChunkPos(n + k, o + l));
                }
            }
        }
        i = 0;
        for (int j = 0; j < MobCategory.values().length; ++j) {
            MobCategory mobCategory = MobCategory.values()[j];
            if (world.getEntityCount(mobCategory.type) > mobCategory.cap * mobSpawningChunks.size() / 256) continue;
            block6: for (ChunkPos chunkPos : mobSpawningChunks) {
                Biome biome;
                Class[] classs;
                if (world.random.nextInt(50) != 0 || (classs = (biome = world.getBiomeSource().getBiome(chunkPos)).getSpawnEntries(mobCategory)) == null || classs.length == 0) continue;
                int p = world.random.nextInt(classs.length);
                BlockPos blockPos = NaturalSpawner.getRandomPosInChunk(world, chunkPos.x * 16, chunkPos.z * 16);
                int q = blockPos.x;
                int r = blockPos.y;
                int s = blockPos.z;
                if (world.isSolidBlock(q, r, s) || world.getMaterial(q, r, s) != Material.AIR) continue;
                int t = 0;
                for (int u = 0; u < 3; ++u) {
                    int v = q;
                    int w = r;
                    int x = s;
                    int y = 6;
                    for (int z = 0; z < 4; ++z) {
                        void mobEntity2;
                        float ad;
                        float ab;
                        float aa;
                        float ae;
                        float h;
                        float g;
                        float f;
                        if (!world.isSolidBlock(v += world.random.nextInt(y) - world.random.nextInt(y), (w += world.random.nextInt(1) - world.random.nextInt(1)) - 1, x += world.random.nextInt(y) - world.random.nextInt(y)) || world.isSolidBlock(v, w, x) || world.getMaterial(v, w, x).isLiquid() || world.isSolidBlock(v, w + 1, x) || world.getNearestPlayer(f = (float)v + 0.5f, g = (float)w, h = (float)x + 0.5f, 24.0) != null || (ae = (aa = f - (float)world.spawnPointX) * aa + (ab = g - (float)world.spawnPointY) * ab + (ad = h - (float)world.spawnPointZ) * ad) < 576.0f) continue;
                        try {
                            MobEntity mobEntity = (MobEntity)classs[p].getConstructor(World.class).newInstance(world);
                        }
                        catch (Exception exception) {
                            exception.printStackTrace();
                            return i;
                        }
                        mobEntity2.setPositionAndAngles(f, g, h, world.random.nextFloat() * 360.0f, 0.0f);
                        if (mobEntity2.canSpawn()) {
                            ++t;
                            world.addEntity((Entity)mobEntity2);
                            if (mobEntity2 instanceof SpiderEntity && world.random.nextInt(100) == 0) {
                                SkeletonEntity skeletonEntity = new SkeletonEntity(world);
                                skeletonEntity.setPositionAndAngles(f, g, h, mobEntity2.yaw, 0.0f);
                                world.addEntity(skeletonEntity);
                                skeletonEntity.startRiding((Entity)mobEntity2);
                            }
                            if (t >= mobEntity2.maxSpawnedPerChunk()) continue block6;
                        }
                        i += t;
                    }
                }
            }
        }
        return i;
    }
}

