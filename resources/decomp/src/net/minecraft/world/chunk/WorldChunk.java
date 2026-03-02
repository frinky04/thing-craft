/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleStorage;

public class WorldChunk {
    public static boolean hasSkyLight;
    public byte[] blocks;
    public boolean loaded;
    public World world;
    public ChunkNibbleStorage blockMetadata;
    public ChunkNibbleStorage skyLight;
    public ChunkNibbleStorage blockLight;
    public byte[] heightMap;
    public int lowestHeight;
    public final int chunkX;
    public final int chunkZ;
    public Map blockEntities = new HashMap();
    public List[] entities = new List[8];
    public boolean terrainPopulated = false;
    public boolean dirty = false;
    public boolean empty;
    public boolean dummy = false;
    public boolean lastSaveHadEntities = false;
    public long lastSaveTime = 0L;

    public WorldChunk(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.heightMap = new byte[256];
        for (int i = 0; i < this.entities.length; ++i) {
            this.entities[i] = new ArrayList();
        }
    }

    public WorldChunk(World world, byte[] blocks, int chunkX, int chunkZ) {
        this(world, chunkX, chunkZ);
        this.blocks = blocks;
        this.blockMetadata = new ChunkNibbleStorage(blocks.length);
        this.skyLight = new ChunkNibbleStorage(blocks.length);
        this.blockLight = new ChunkNibbleStorage(blocks.length);
    }

    public boolean isAt(int chunkX, int chunkZ) {
        return chunkX == this.chunkX && chunkZ == this.chunkZ;
    }

    public int getHeight(int localX, int localZ) {
        return this.heightMap[localZ << 4 | localX] & 0xFF;
    }

    public void populateLight() {
    }

    @Environment(value=EnvType.CLIENT)
    public void populateHeightMapOnly() {
        int i = 127;
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                int l;
                int m = j << 11 | k << 7;
                for (l = 127; l > 0 && Block.OPACITIES[this.blocks[m + l - 1]] == 0; --l) {
                }
                this.heightMap[k << 4 | j] = (byte)l;
                if (l >= i) continue;
                i = l;
            }
        }
        this.lowestHeight = i;
        this.dirty = true;
    }

    public void populateHeightMap() {
        int j;
        int i = 127;
        for (j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                this.heightMap[k << 4 | j] = -128;
                this.updateHeightMap(j, 127, k);
                if ((this.heightMap[k << 4 | j] & 0xFF) >= i) continue;
                i = this.heightMap[k << 4 | j] & 0xFF;
            }
        }
        this.lowestHeight = i;
        for (j = 0; j < 16; ++j) {
            for (int l = 0; l < 16; ++l) {
                this.lightGaps(j, l);
            }
        }
        this.dirty = true;
    }

    public void populateBlockLight() {
        int i = 32;
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                int m;
                int l = j << 11 | k << 7;
                for (m = 0; m < 128; ++m) {
                    int n = Block.LIGHT[this.blocks[l + m]];
                    if (n <= 0) continue;
                    this.blockLight.set(j, m, k, n);
                }
                m = 15;
                int o = i - 2;
                while (o < 128 && m > 0) {
                    byte p = this.blocks[l + ++o];
                    int q = Block.OPACITIES[p];
                    int r = Block.LIGHT[p];
                    if (q == 0) {
                        q = 1;
                    }
                    if (r > (m -= q)) {
                        m = r;
                    }
                    if (m < 0) {
                        m = 0;
                    }
                    this.blockLight.set(j, o, k, m);
                }
            }
        }
        this.world.updateLight(LightType.BLOCK, this.chunkX * 16, i - 1, this.chunkZ * 16, this.chunkX * 16 + 16, i + 1, this.chunkZ * 16 + 16);
        this.dirty = true;
    }

    private void lightGaps(int localX, int localZ) {
        int i = this.getHeight(localX, localZ);
        int j = this.chunkX * 16 + localX;
        int k = this.chunkZ * 16 + localZ;
        this.lightGap(j - 1, k, i);
        this.lightGap(j + 1, k, i);
        this.lightGap(j, k - 1, i);
        this.lightGap(j, k + 1, i);
    }

    private void lightGap(int x, int z, int y) {
        int i = this.world.getHeight(x, z);
        if (i > y) {
            this.world.updateLight(LightType.SKY, x, y, z, x, i, z);
        } else if (i < y) {
            this.world.updateLight(LightType.SKY, x, i, z, x, y, z);
        }
        this.dirty = true;
    }

    private void updateHeightMap(int localX, int y, int localZ) {
        int r;
        int i;
        int j = i = this.heightMap[localZ << 4 | localX] & 0xFF;
        if (y > i) {
            j = y;
        }
        int k = localX << 11 | localZ << 7;
        while (j > 0 && Block.OPACITIES[this.blocks[k + j - 1]] == 0) {
            --j;
        }
        if (j == i) {
            return;
        }
        this.world.onHeightMapChanged(localX, localZ, j, i);
        this.heightMap[localZ << 4 | localX] = (byte)j;
        if (j < this.lowestHeight) {
            this.lowestHeight = j;
        } else {
            int l = 127;
            for (int n = 0; n < 16; ++n) {
                for (int p = 0; p < 16; ++p) {
                    if ((this.heightMap[p << 4 | n] & 0xFF) >= l) continue;
                    l = this.heightMap[p << 4 | n] & 0xFF;
                }
            }
            this.lowestHeight = l;
        }
        int m = this.chunkX * 16 + localX;
        int o = this.chunkZ * 16 + localZ;
        if (j < i) {
            for (int q = j; q < i; ++q) {
                this.skyLight.set(localX, q, localZ, 15);
            }
        } else {
            this.world.updateLight(LightType.SKY, m, i, o, m, j, o);
            for (r = i; r < j; ++r) {
                this.skyLight.set(localX, r, localZ, 0);
            }
        }
        r = 15;
        int s = j;
        while (j > 0 && r > 0) {
            int t;
            if ((t = Block.OPACITIES[this.getBlockAt(localX, --j, localZ)]) == 0) {
                t = 1;
            }
            if ((r -= t) < 0) {
                r = 0;
            }
            this.skyLight.set(localX, j, localZ, r);
        }
        while (j > 0 && Block.OPACITIES[this.getBlockAt(localX, j - 1, localZ)] == 0) {
            --j;
        }
        if (j != s) {
            this.world.updateLight(LightType.SKY, m - 1, j, o - 1, m + 1, s, o + 1);
        }
        this.dirty = true;
    }

    public int getBlockAt(int localX, int y, int localZ) {
        return this.blocks[localX << 11 | localZ << 7 | y];
    }

    public boolean setBlockWithMetadataAt(int localX, int y, int localZ, int block, int metadata) {
        byte i = (byte)block;
        int j = this.heightMap[localZ << 4 | localX] & 0xFF;
        int k = this.blocks[localX << 11 | localZ << 7 | y] & 0xFF;
        if (k == block && this.blockMetadata.get(localX, y, localZ) == metadata) {
            return false;
        }
        int l = this.chunkX * 16 + localX;
        int m = this.chunkZ * 16 + localZ;
        this.blocks[localX << 11 | localZ << 7 | y] = i;
        if (k != 0 && !this.world.isMultiplayer) {
            Block.BY_ID[k].onRemoved(this.world, l, y, m);
        }
        this.blockMetadata.set(localX, y, localZ, metadata);
        if (!this.world.dimension.noSky) {
            if (Block.OPACITIES[i] != 0) {
                if (y >= j) {
                    this.updateHeightMap(localX, y + 1, localZ);
                }
            } else if (y == j - 1) {
                this.updateHeightMap(localX, y, localZ);
            }
            this.world.updateLight(LightType.SKY, l, y, m, l, y, m);
        }
        this.world.updateLight(LightType.BLOCK, l, y, m, l, y, m);
        this.lightGaps(localX, localZ);
        if (block != 0) {
            Block.BY_ID[block].onAdded(this.world, l, y, m);
        }
        this.blockMetadata.set(localX, y, localZ, metadata);
        this.dirty = true;
        return true;
    }

    public boolean setBlockAt(int localX, int y, int localZ, int block) {
        byte i = (byte)block;
        int j = this.heightMap[localZ << 4 | localX] & 0xFF;
        int k = this.blocks[localX << 11 | localZ << 7 | y] & 0xFF;
        if (k == block) {
            return false;
        }
        int l = this.chunkX * 16 + localX;
        int m = this.chunkZ * 16 + localZ;
        this.blocks[localX << 11 | localZ << 7 | y] = i;
        if (k != 0) {
            Block.BY_ID[k].onRemoved(this.world, l, y, m);
        }
        this.blockMetadata.set(localX, y, localZ, 0);
        if (Block.OPACITIES[i] != 0) {
            if (y >= j) {
                this.updateHeightMap(localX, y + 1, localZ);
            }
        } else if (y == j - 1) {
            this.updateHeightMap(localX, y, localZ);
        }
        this.world.updateLight(LightType.SKY, l, y, m, l, y, m);
        this.world.updateLight(LightType.BLOCK, l, y, m, l, y, m);
        this.lightGaps(localX, localZ);
        if (block != 0 && !this.world.isMultiplayer) {
            Block.BY_ID[block].onAdded(this.world, l, y, m);
        }
        this.dirty = true;
        return true;
    }

    public int getBlockMetadataAt(int localX, int y, int localZ) {
        return this.blockMetadata.get(localX, y, localZ);
    }

    public void setBlockMetadataAt(int localX, int y, int localZ, int metadata) {
        this.dirty = true;
        this.blockMetadata.set(localX, y, localZ, metadata);
    }

    public int getLightAt(LightType type, int localX, int y, int localZ) {
        if (type == LightType.SKY) {
            return this.skyLight.get(localX, y, localZ);
        }
        if (type == LightType.BLOCK) {
            return this.blockLight.get(localX, y, localZ);
        }
        return 0;
    }

    public void setLightAt(LightType type, int localX, int y, int localZ, int light) {
        this.dirty = true;
        if (type == LightType.SKY) {
            this.skyLight.set(localX, y, localZ, light);
        } else if (type == LightType.BLOCK) {
            this.blockLight.set(localX, y, localZ, light);
        } else {
            return;
        }
    }

    public int getActualLightAt(int localX, int y, int localZ, int ambientDarkness) {
        int j;
        int i = this.skyLight.get(localX, y, localZ);
        if (i > 0) {
            hasSkyLight = true;
        }
        if ((j = this.blockLight.get(localX, y, localZ)) > (i -= ambientDarkness)) {
            i = j;
        }
        return i;
    }

    public void addEntity(Entity entity) {
        int k;
        if (this.dummy) {
            return;
        }
        this.lastSaveHadEntities = true;
        int i = MathHelper.floor(entity.x / 16.0);
        int j = MathHelper.floor(entity.z / 16.0);
        if (i != this.chunkX || j != this.chunkZ) {
            System.out.println("Wrong location! " + entity);
            Thread.dumpStack();
        }
        if ((k = MathHelper.floor(entity.y / 16.0)) < 0) {
            k = 0;
        }
        if (k >= this.entities.length) {
            k = this.entities.length - 1;
        }
        entity.inChunk = true;
        entity.chunkX = this.chunkX;
        entity.chunkY = k;
        entity.chunkZ = this.chunkZ;
        this.entities[k].add(entity);
    }

    public void removeEntity(Entity entity) {
        this.removeEntity(entity, entity.chunkY);
    }

    public void removeEntity(Entity entity, int chunkY) {
        if (chunkY < 0) {
            chunkY = 0;
        }
        if (chunkY >= this.entities.length) {
            chunkY = this.entities.length - 1;
        }
        this.entities[chunkY].remove(entity);
    }

    public boolean hasSkyAccessAt(int localX, int y, int localZ) {
        return y >= (this.heightMap[localZ << 4 | localX] & 0xFF);
    }

    public BlockEntity getBlockEntityAt(int localX, int y, int localZ) {
        BlockPos blockPos = new BlockPos(localX, y, localZ);
        BlockEntity blockEntity = (BlockEntity)this.blockEntities.get(blockPos);
        if (blockEntity == null) {
            int i = this.getBlockAt(localX, y, localZ);
            if (!Block.HAS_BLOCK_ENTITY[i]) {
                return null;
            }
            BlockWithBlockEntity blockWithBlockEntity = (BlockWithBlockEntity)Block.BY_ID[i];
            blockWithBlockEntity.onAdded(this.world, this.chunkX * 16 + localX, y, this.chunkZ * 16 + localZ);
            blockEntity = (BlockEntity)this.blockEntities.get(blockPos);
        }
        return blockEntity;
    }

    public void addBlockEntity(BlockEntity blockEntity) {
        int i = blockEntity.x - this.chunkX * 16;
        int j = blockEntity.y;
        int k = blockEntity.z - this.chunkZ * 16;
        this.setBlockEntityAt(i, j, k, blockEntity);
    }

    public void setBlockEntityAt(int localX, int y, int localZ, BlockEntity blockEntity) {
        BlockPos blockPos = new BlockPos(localX, y, localZ);
        blockEntity.world = this.world;
        blockEntity.x = this.chunkX * 16 + localX;
        blockEntity.y = y;
        blockEntity.z = this.chunkZ * 16 + localZ;
        if (this.getBlockAt(localX, y, localZ) == 0 || !(Block.BY_ID[this.getBlockAt(localX, y, localZ)] instanceof BlockWithBlockEntity)) {
            System.out.println("Attempted to place a tile entity where there was no entity tile!");
            return;
        }
        if (this.loaded) {
            if (this.blockEntities.get(blockPos) != null) {
                this.world.blockEntities.remove(this.blockEntities.get(blockPos));
            }
            this.world.blockEntities.add(blockEntity);
        }
        this.blockEntities.put(blockPos, blockEntity);
    }

    public void removeBlockEntityAt(int localX, int y, int localZ) {
        BlockPos blockPos = new BlockPos(localX, y, localZ);
        if (this.loaded) {
            this.world.blockEntities.remove(this.blockEntities.remove(blockPos));
        }
    }

    public void load() {
        this.loaded = true;
        this.world.blockEntities.addAll(this.blockEntities.values());
        for (int i = 0; i < this.entities.length; ++i) {
            this.world.loadEntities(this.entities[i]);
        }
    }

    public void unload() {
        this.loaded = false;
        this.world.blockEntities.removeAll(this.blockEntities.values());
        for (int i = 0; i < this.entities.length; ++i) {
            this.world.unloadEntities(this.entities[i]);
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void getEntities(Entity exclude, Box bounds, List entities) {
        int i = MathHelper.floor((bounds.minY - 2.0) / 16.0);
        int j = MathHelper.floor((bounds.maxY + 2.0) / 16.0);
        if (i < 0) {
            i = 0;
        }
        if (j >= this.entities.length) {
            j = this.entities.length - 1;
        }
        for (int k = i; k <= j; ++k) {
            List list = this.entities[k];
            for (int l = 0; l < list.size(); ++l) {
                Entity entity = (Entity)list.get(l);
                if (entity == exclude || !entity.shape.intersects(bounds)) continue;
                entities.add(entity);
            }
        }
    }

    public void getEntitiesOfType(Class type, Box bounds, List entities) {
        int i = MathHelper.floor((bounds.minY - 2.0) / 16.0);
        int j = MathHelper.floor((bounds.maxY + 2.0) / 16.0);
        if (i < 0) {
            i = 0;
        }
        if (j >= this.entities.length) {
            j = this.entities.length - 1;
        }
        for (int k = i; k <= j; ++k) {
            List list = this.entities[k];
            for (int l = 0; l < list.size(); ++l) {
                Entity entity = (Entity)list.get(l);
                if (!type.isAssignableFrom(entity.getClass()) || !entity.shape.intersects(bounds)) continue;
                entities.add(entity);
            }
        }
    }

    public boolean shouldSave(boolean saveEntities) {
        if (this.empty) {
            return false;
        }
        if (this.lastSaveHadEntities && this.world.ticks != this.lastSaveTime) {
            return true;
        }
        return this.dirty;
    }

    @Environment(value=EnvType.CLIENT)
    public int unpackChunkData(byte[] data, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int size) {
        int i;
        for (i = minX; i < maxX; ++i) {
            for (int j = minZ; j < maxZ; ++j) {
                int n = i << 11 | j << 7 | minY;
                int r = maxY - minY;
                System.arraycopy(data, size, this.blocks, n, r);
                size += r;
            }
        }
        this.populateHeightMapOnly();
        for (i = minX; i < maxX; ++i) {
            for (int k = minZ; k < maxZ; ++k) {
                int o = (i << 11 | k << 7 | minY) >> 1;
                int s = (maxY - minY) / 2;
                System.arraycopy(data, size, this.blockMetadata.data, o, s);
                size += s;
            }
        }
        for (i = minX; i < maxX; ++i) {
            for (int l = minZ; l < maxZ; ++l) {
                int p = (i << 11 | l << 7 | minY) >> 1;
                int t = (maxY - minY) / 2;
                System.arraycopy(data, size, this.blockLight.data, p, t);
                size += t;
            }
        }
        for (i = minX; i < maxX; ++i) {
            for (int m = minZ; m < maxZ; ++m) {
                int q = (i << 11 | m << 7 | minY) >> 1;
                int u = (maxY - minY) / 2;
                System.arraycopy(data, size, this.skyLight.data, q, u);
                size += u;
            }
        }
        return size;
    }

    @Environment(value=EnvType.SERVER)
    public int packChunkData(byte[] data, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int size) {
        int i;
        for (i = minX; i < maxX; ++i) {
            for (int j = minZ; j < maxZ; ++j) {
                int n = i << 11 | j << 7 | minY;
                int r = maxY - minY;
                System.arraycopy(this.blocks, n, data, size, r);
                size += r;
            }
        }
        for (i = minX; i < maxX; ++i) {
            for (int k = minZ; k < maxZ; ++k) {
                int o = (i << 11 | k << 7 | minY) >> 1;
                int s = (maxY - minY) / 2;
                System.arraycopy(this.blockMetadata.data, o, data, size, s);
                size += s;
            }
        }
        for (i = minX; i < maxX; ++i) {
            for (int l = minZ; l < maxZ; ++l) {
                int p = (i << 11 | l << 7 | minY) >> 1;
                int t = (maxY - minY) / 2;
                System.arraycopy(this.blockLight.data, p, data, size, t);
                size += t;
            }
        }
        for (i = minX; i < maxX; ++i) {
            for (int m = minZ; m < maxZ; ++m) {
                int q = (i << 11 | m << 7 | minY) >> 1;
                int u = (maxY - minY) / 2;
                System.arraycopy(this.skyLight.data, q, data, size, u);
                size += u;
            }
        }
        return size;
    }

    public Random getRandomForSlime(long seed) {
        return new Random(this.world.seed + (long)(this.chunkX * this.chunkX * 4987142) + (long)(this.chunkX * 5947611) + (long)(this.chunkZ * this.chunkZ) * 4392871L + (long)(this.chunkZ * 389711) ^ seed);
    }
}

