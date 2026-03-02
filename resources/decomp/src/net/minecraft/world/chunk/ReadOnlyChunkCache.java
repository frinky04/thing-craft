/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.chunk;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.storage.ChunkStorage;

@Environment(value=EnvType.CLIENT)
public class ReadOnlyChunkCache
implements ChunkSource {
    private WorldChunk[] chunks = new WorldChunk[256];
    private World world;
    private ChunkStorage storage;
    byte[] blocksForNewChunks = new byte[32768];

    public ReadOnlyChunkCache(World world, ChunkStorage storage) {
        this.world = world;
        this.storage = storage;
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        int i = chunkX & 0xF | (chunkZ & 0xF) * 16;
        return this.chunks[i] != null && this.chunks[i].isAt(chunkX, chunkZ);
    }

    public WorldChunk getChunk(int chunkX, int chunkZ) {
        int i = chunkX & 0xF | (chunkZ & 0xF) * 16;
        try {
            if (!this.hasChunk(chunkX, chunkZ)) {
                WorldChunk worldChunk = this.loadChunkFromStorage(chunkX, chunkZ);
                if (worldChunk == null) {
                    worldChunk = new WorldChunk(this.world, this.blocksForNewChunks, chunkX, chunkZ);
                    worldChunk.dummy = true;
                    worldChunk.empty = true;
                }
                this.chunks[i] = worldChunk;
            }
            return this.chunks[i];
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private synchronized WorldChunk loadChunkFromStorage(int chunkX, int chunkZ) {
        try {
            return this.storage.loadChunk(this.world, chunkX, chunkZ);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
            return null;
        }
    }

    public void populateChunk(ChunkSource source, int chunkX, int chunkZ) {
    }

    public boolean save(boolean saveEntities, ProgressListener listener) {
        return true;
    }

    public boolean tick() {
        return false;
    }

    public boolean shouldSave() {
        return false;
    }
}

