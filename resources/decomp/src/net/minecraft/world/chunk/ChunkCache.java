/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.chunk;

import java.io.IOException;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.storage.ChunkStorage;

public class ChunkCache
implements ChunkSource {
    private WorldChunk empty;
    private ChunkSource generator;
    private ChunkStorage storage;
    private WorldChunk[] chunks = new WorldChunk[1024];
    private World world;
    int cachedChunkX = -999999999;
    int cachedChunkZ = -999999999;
    private WorldChunk cachedChunk;

    public ChunkCache(World world, ChunkStorage storage, ChunkSource generator) {
        this.empty = new WorldChunk(world, new byte[32768], 0, 0);
        this.empty.dummy = true;
        this.empty.empty = true;
        this.world = world;
        this.storage = storage;
        this.generator = generator;
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        if (chunkX == this.cachedChunkX && chunkZ == this.cachedChunkZ && this.cachedChunk != null) {
            return true;
        }
        int i = chunkX & 0x1F;
        int j = chunkZ & 0x1F;
        int k = i + j * 32;
        return this.chunks[k] != null && (this.chunks[k] == this.empty || this.chunks[k].isAt(chunkX, chunkZ));
    }

    public WorldChunk getChunk(int chunkX, int chunkZ) {
        if (chunkX == this.cachedChunkX && chunkZ == this.cachedChunkZ && this.cachedChunk != null) {
            return this.cachedChunk;
        }
        int i = chunkX & 0x1F;
        int j = chunkZ & 0x1F;
        int k = i + j * 32;
        if (!this.hasChunk(chunkX, chunkZ)) {
            WorldChunk worldChunk;
            if (this.chunks[k] != null) {
                this.chunks[k].unload();
                this.saveChunk(this.chunks[k]);
                this.saveEntities(this.chunks[k]);
            }
            if ((worldChunk = this.loadChunkFromStorage(chunkX, chunkZ)) == null) {
                worldChunk = this.generator == null ? this.empty : this.generator.getChunk(chunkX, chunkZ);
            }
            this.chunks[k] = worldChunk;
            worldChunk.populateBlockLight();
            if (this.chunks[k] != null) {
                this.chunks[k].load();
            }
            if (!this.chunks[k].terrainPopulated && this.hasChunk(chunkX + 1, chunkZ + 1) && this.hasChunk(chunkX, chunkZ + 1) && this.hasChunk(chunkX + 1, chunkZ)) {
                this.populateChunk(this, chunkX, chunkZ);
            }
            if (this.hasChunk(chunkX - 1, chunkZ) && !this.getChunk((int)(chunkX - 1), (int)chunkZ).terrainPopulated && this.hasChunk(chunkX - 1, chunkZ + 1) && this.hasChunk(chunkX, chunkZ + 1) && this.hasChunk(chunkX - 1, chunkZ)) {
                this.populateChunk(this, chunkX - 1, chunkZ);
            }
            if (this.hasChunk(chunkX, chunkZ - 1) && !this.getChunk((int)chunkX, (int)(chunkZ - 1)).terrainPopulated && this.hasChunk(chunkX + 1, chunkZ - 1) && this.hasChunk(chunkX, chunkZ - 1) && this.hasChunk(chunkX + 1, chunkZ)) {
                this.populateChunk(this, chunkX, chunkZ - 1);
            }
            if (this.hasChunk(chunkX - 1, chunkZ - 1) && !this.getChunk((int)(chunkX - 1), (int)(chunkZ - 1)).terrainPopulated && this.hasChunk(chunkX - 1, chunkZ - 1) && this.hasChunk(chunkX, chunkZ - 1) && this.hasChunk(chunkX - 1, chunkZ)) {
                this.populateChunk(this, chunkX - 1, chunkZ - 1);
            }
        }
        this.cachedChunkX = chunkX;
        this.cachedChunkZ = chunkZ;
        this.cachedChunk = this.chunks[k];
        return this.chunks[k];
    }

    private WorldChunk loadChunkFromStorage(int chunkX, int chunkZ) {
        if (this.storage == null) {
            return null;
        }
        try {
            WorldChunk worldChunk = this.storage.loadChunk(this.world, chunkX, chunkZ);
            if (worldChunk != null) {
                worldChunk.lastSaveTime = this.world.ticks;
            }
            return worldChunk;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private void saveEntities(WorldChunk chunk) {
        if (this.storage == null) {
            return;
        }
        try {
            this.storage.saveEntities(this.world, chunk);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void saveChunk(WorldChunk chunk) {
        if (this.storage == null) {
            return;
        }
        try {
            chunk.lastSaveTime = this.world.ticks;
            this.storage.saveChunk(this.world, chunk);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void populateChunk(ChunkSource source, int chunkX, int chunkZ) {
        WorldChunk worldChunk = this.getChunk(chunkX, chunkZ);
        if (!worldChunk.terrainPopulated) {
            worldChunk.terrainPopulated = true;
            if (this.generator != null) {
                this.generator.populateChunk(source, chunkX, chunkZ);
                worldChunk.markDirty();
            }
        }
    }

    public boolean save(boolean saveEntities, ProgressListener listener) {
        int i = 0;
        int j = 0;
        if (listener != null) {
            for (int k = 0; k < this.chunks.length; ++k) {
                if (this.chunks[k] == null || !this.chunks[k].shouldSave(saveEntities)) continue;
                ++j;
            }
        }
        int l = 0;
        for (int m = 0; m < this.chunks.length; ++m) {
            if (this.chunks[m] == null) continue;
            if (saveEntities && !this.chunks[m].empty) {
                this.saveEntities(this.chunks[m]);
            }
            if (!this.chunks[m].shouldSave(saveEntities)) continue;
            this.saveChunk(this.chunks[m]);
            this.chunks[m].dirty = false;
            if (++i == 2 && !saveEntities) {
                return false;
            }
            if (listener == null || ++l % 10 != 0) continue;
            listener.progressStagePercentage(l * 100 / j);
        }
        if (saveEntities) {
            if (this.storage == null) {
                return true;
            }
            this.storage.flush();
        }
        return true;
    }

    public boolean tick() {
        if (this.storage != null) {
            this.storage.tick();
        }
        return this.generator.tick();
    }

    public boolean shouldSave() {
        return true;
    }
}

