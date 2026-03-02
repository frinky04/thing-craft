/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.world;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.client.world.chunk.MultiplayerChunkCache;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.DisconnectPacket;
import net.minecraft.util.Int2ObjectHashMap;
import net.minecraft.world.World;
import net.minecraft.world.WorldEventListener;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.dimension.Dimension;

@Environment(value=EnvType.CLIENT)
public class MultiplayerWorld
extends World {
    private LinkedList blockResets = new LinkedList();
    private ClientNetworkHandler networkHandler;
    private MultiplayerChunkCache chunkCache;
    private boolean noSendingBlockEntityUpdates = false;
    private Int2ObjectHashMap entitiesByNetworkId = new Int2ObjectHashMap();
    private Set forcedEntities = new HashSet();
    private Set pendingEntities = new HashSet();

    public MultiplayerWorld(ClientNetworkHandler networkHandler, long seed, int dimension) {
        super("MpServer", Dimension.fromId(dimension), seed);
        this.networkHandler = networkHandler;
        this.spawnPointX = 8;
        this.spawnPointY = 64;
        this.spawnPointZ = 8;
    }

    public void tick() {
        int k;
        ++this.ticks;
        int i = this.calculateAmbientDarkness(1.0f);
        if (i != this.ambientDarkness) {
            this.ambientDarkness = i;
            for (int j = 0; j < this.eventListeners.size(); ++j) {
                ((WorldEventListener)this.eventListeners.get(j)).notifyAmbientDarknessChanged();
            }
        }
        for (k = 0; k < 10 && !this.pendingEntities.isEmpty(); ++k) {
            Entity entity = (Entity)this.pendingEntities.iterator().next();
            if (this.entities.contains(entity)) continue;
            this.addEntity(entity);
        }
        this.networkHandler.tick();
        for (k = 0; k < this.blockResets.size(); ++k) {
            BlockReset blockReset = (BlockReset)this.blockResets.get(k);
            if (--blockReset.delay != 0) continue;
            super.setBlockWithMetadataQuietly(blockReset.x, blockReset.y, blockReset.z, blockReset.block, blockReset.metadata);
            super.notifyBlockChanged(blockReset.x, blockReset.y, blockReset.z);
            this.blockResets.remove(k--);
        }
    }

    public void clearBlockResets(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int i = 0; i < this.blockResets.size(); ++i) {
            BlockReset blockReset = (BlockReset)this.blockResets.get(i);
            if (blockReset.x < minX || blockReset.y < minY || blockReset.z < minZ || blockReset.x > maxX || blockReset.y > maxY || blockReset.z > maxZ) continue;
            this.blockResets.remove(i--);
        }
    }

    protected ChunkSource createChunkCache(File dir) {
        this.chunkCache = new MultiplayerChunkCache(this);
        return this.chunkCache;
    }

    public void resetSpawnPoint() {
        this.spawnPointX = 8;
        this.spawnPointY = 64;
        this.spawnPointZ = 8;
    }

    protected void tickChunks() {
    }

    public void scheduleTick(int x, int y, int z, int block) {
    }

    public boolean doScheduledTicks(boolean flush) {
        return false;
    }

    public void updateChunk(int chunkX, int chunkZ, boolean load) {
        if (load) {
            this.chunkCache.loadChunk(chunkX, chunkZ);
        } else {
            this.chunkCache.unloadChunk(chunkX, chunkZ);
        }
        if (!load) {
            this.notifyRegionChanged(chunkX * 16, 0, chunkZ * 16, chunkX * 16 + 15, 128, chunkZ * 16 + 15);
        }
    }

    public boolean addEntity(Entity entity) {
        boolean i = super.addEntity(entity);
        this.forcedEntities.add(entity);
        if (!i) {
            this.pendingEntities.add(entity);
        }
        return i;
    }

    public void removeEntity(Entity entity) {
        super.removeEntity(entity);
        this.forcedEntities.remove(entity);
    }

    protected void notifyEntityAdded(Entity entity) {
        super.notifyEntityAdded(entity);
        if (this.pendingEntities.contains(entity)) {
            this.pendingEntities.remove(entity);
        }
    }

    protected void notifyEntityRemoved(Entity entity) {
        super.notifyEntityRemoved(entity);
        if (this.forcedEntities.contains(entity)) {
            this.pendingEntities.add(entity);
        }
    }

    public void forceEntity(int networkId, Entity entity) {
        Entity entity2 = this.getEntity(networkId);
        if (entity2 != null) {
            this.removeEntity(entity2);
        }
        this.forcedEntities.add(entity);
        entity.networkId = networkId;
        if (!this.addEntity(entity)) {
            this.pendingEntities.add(entity);
        }
        this.entitiesByNetworkId.put(networkId, entity);
    }

    public Entity getEntity(int networkId) {
        return (Entity)this.entitiesByNetworkId.get(networkId);
    }

    public Entity removeEntity(int networkId) {
        Entity entity = (Entity)this.entitiesByNetworkId.remove(networkId);
        if (entity != null) {
            this.forcedEntities.remove(entity);
            this.removeEntity(entity);
        }
        return entity;
    }

    public boolean setBlockMetadataQuietly(int x, int y, int z, int metadata) {
        int i = this.getBlock(x, y, z);
        int j = this.getBlockMetadata(x, y, z);
        if (super.setBlockMetadataQuietly(x, y, z, metadata)) {
            this.blockResets.add(new BlockReset(x, y, z, i, j));
            return true;
        }
        return false;
    }

    public boolean setBlockWithMetadataQuietly(int x, int y, int z, int block, int metadata) {
        int i = this.getBlock(x, y, z);
        int j = this.getBlockMetadata(x, y, z);
        if (super.setBlockWithMetadataQuietly(x, y, z, block, metadata)) {
            this.blockResets.add(new BlockReset(x, y, z, i, j));
            return true;
        }
        return false;
    }

    public boolean setBlockQuietly(int x, int y, int z, int block) {
        int i = this.getBlock(x, y, z);
        int j = this.getBlockMetadata(x, y, z);
        if (super.setBlockQuietly(x, y, z, block)) {
            this.blockResets.add(new BlockReset(x, y, z, i, j));
            return true;
        }
        return false;
    }

    public boolean setBlockWithMetadataFromPacket(int x, int y, int z, int block, int metadata) {
        this.clearBlockResets(x, y, z, x, y, z);
        if (super.setBlockWithMetadataQuietly(x, y, z, block, metadata)) {
            this.onBlockChanged(x, y, z, block);
            return true;
        }
        return false;
    }

    public void notifyBlockEntityChanged(int x, int y, int z, BlockEntity blockEntity) {
        if (this.noSendingBlockEntityUpdates) {
            return;
        }
        this.networkHandler.sendPacket(new BlockEntityUpdatePacket(x, y, z, blockEntity));
    }

    public void disconnect() {
        this.networkHandler.sendPacket(new DisconnectPacket("Quitting"));
    }

    @Environment(value=EnvType.CLIENT)
    class BlockReset {
        int x;
        int y;
        int z;
        int delay;
        int block;
        int metadata;

        public BlockReset(int x, int y, int z, int block, int metadata) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.delay = 80;
            this.block = block;
            this.metadata = metadata;
        }
    }
}

