/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.world.chunk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.storage.ChunkStorage;

@Environment(value=EnvType.SERVER)
public class ServerChunkCache
implements ChunkSource {
    private Set chunksToUnload = new HashSet();
    private WorldChunk empty;
    private ChunkSource generator;
    private ChunkStorage storage;
    private Map chunksByPos = new HashMap();
    private List chunks = new ArrayList();
    private ServerWorld world;

    public ServerChunkCache(ServerWorld world, ChunkStorage storage, ChunkSource generator) {
        this.empty = new WorldChunk(world, new byte[32768], 0, 0);
        this.empty.dummy = true;
        this.empty.empty = true;
        this.world = world;
        this.storage = storage;
        this.generator = generator;
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        return this.chunksByPos.containsKey(chunkPos);
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        int i = chunkX * 16 + 8 - this.world.spawnPointX;
        int j = chunkZ * 16 + 8 - this.world.spawnPointZ;
        int k = 128;
        if (i < -k || i > k || j < -k || j > k) {
            this.chunksToUnload.add(new ChunkPos(chunkX, chunkZ));
        }
    }

    public WorldChunk loadChunk(int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        this.chunksToUnload.remove(new ChunkPos(chunkX, chunkZ));
        WorldChunk worldChunk = (WorldChunk)this.chunksByPos.get(chunkPos);
        if (worldChunk == null) {
            worldChunk = this.loadChunkFromStorage(chunkX, chunkZ);
            if (worldChunk == null) {
                worldChunk = this.generator == null ? this.empty : this.generator.getChunk(chunkX, chunkZ);
            }
            this.chunksByPos.put(chunkPos, worldChunk);
            this.chunks.add(worldChunk);
            worldChunk.populateBlockLight();
            if (worldChunk != null) {
                worldChunk.load();
            }
            if (!worldChunk.terrainPopulated && this.hasChunk(chunkX + 1, chunkZ + 1) && this.hasChunk(chunkX, chunkZ + 1) && this.hasChunk(chunkX + 1, chunkZ)) {
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
        return worldChunk;
    }

    public WorldChunk getChunk(int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        WorldChunk worldChunk = (WorldChunk)this.chunksByPos.get(chunkPos);
        if (worldChunk == null) {
            if (this.world.searchingSpawnPoint) {
                return this.loadChunk(chunkX, chunkZ);
            }
            return this.empty;
        }
        return worldChunk;
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
        for (int j = 0; j < this.chunks.size(); ++j) {
            WorldChunk worldChunk = (WorldChunk)this.chunks.get(j);
            if (saveEntities && !worldChunk.empty) {
                this.saveEntities(worldChunk);
            }
            if (!worldChunk.shouldSave(saveEntities)) continue;
            this.saveChunk(worldChunk);
            worldChunk.dirty = false;
            if (++i != 32 || saveEntities) continue;
            return false;
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
        if (!this.world.savingDisabled) {
            for (int i = 0; i < 100; ++i) {
                if (this.chunksToUnload.isEmpty()) continue;
                ChunkPos chunkPos = (ChunkPos)this.chunksToUnload.iterator().next();
                WorldChunk worldChunk = this.getChunk(chunkPos.x, chunkPos.z);
                worldChunk.unload();
                this.saveChunk(worldChunk);
                this.saveEntities(worldChunk);
                this.chunksToUnload.remove(chunkPos);
                this.chunksByPos.remove(chunkPos);
                this.chunks.remove(worldChunk);
            }
            if (this.storage != null) {
                this.storage.tick();
            }
        }
        return this.generator.tick();
    }

    public boolean shouldSave() {
        return !this.world.savingDisabled;
    }

    @Environment(value=EnvType.SERVER)
    public static final class ChunkPos {
        public final int x;
        public final int z;

        public ChunkPos(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public boolean equals(Object object) {
            if (object instanceof ChunkPos) {
                ChunkPos chunkPos = (ChunkPos)object;
                return this.x == chunkPos.x && this.z == chunkPos.z;
            }
            return false;
        }

        public int hashCode() {
            return this.x << 16 ^ this.z;
        }
    }
}

