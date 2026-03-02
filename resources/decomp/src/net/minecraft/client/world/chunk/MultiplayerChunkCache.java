/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.world.chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;

@Environment(value=EnvType.CLIENT)
public class MultiplayerChunkCache
implements ChunkSource {
    private WorldChunk empty;
    private Map chunksByPos = new HashMap();
    private List chunks = new ArrayList();
    private World world;

    public MultiplayerChunkCache(World world) {
        this.empty = new WorldChunk(world, new byte[32768], 0, 0);
        this.empty.dummy = true;
        this.empty.empty = true;
        this.world = world;
    }

    public boolean hasChunk(int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        return this.chunksByPos.containsKey(chunkPos);
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        WorldChunk worldChunk = this.getChunk(chunkX, chunkZ);
        if (!worldChunk.dummy) {
            worldChunk.unload();
        }
        this.chunksByPos.remove(new ChunkPos(chunkX, chunkZ));
        this.chunks.remove(worldChunk);
    }

    public WorldChunk loadChunk(int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        byte[] bs = new byte[32768];
        WorldChunk worldChunk = new WorldChunk(this.world, bs, chunkX, chunkZ);
        Arrays.fill(worldChunk.skyLight.data, (byte)-1);
        this.chunksByPos.put(chunkPos, worldChunk);
        worldChunk.loaded = true;
        return worldChunk;
    }

    public WorldChunk getChunk(int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        WorldChunk worldChunk = (WorldChunk)this.chunksByPos.get(chunkPos);
        if (worldChunk == null) {
            return this.empty;
        }
        return worldChunk;
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

    public void populateChunk(ChunkSource source, int chunkX, int chunkZ) {
    }

    @Environment(value=EnvType.CLIENT)
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

