/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.BlockUpdatePacket;
import net.minecraft.network.packet.BlocksUpdatePacket;
import net.minecraft.network.packet.LoadWorldChunkPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.util.Long2ObjectHashMap;
import net.minecraft.util.math.ChunkPos;

@Environment(value=EnvType.SERVER)
public class ChunkMap {
    private List players = new ArrayList();
    private Long2ObjectHashMap chunks = new Long2ObjectHashMap();
    private List dirty = new ArrayList();
    private MinecraftServer server;

    public ChunkMap(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        for (int i = 0; i < this.dirty.size(); ++i) {
            ((ChunkHolder)this.dirty.get(i)).sendChanges();
        }
        this.dirty.clear();
    }

    private ChunkHolder getChunk(int chunkX, int chunkZ, boolean add) {
        long l = (long)chunkX + Integer.MAX_VALUE | (long)chunkZ + Integer.MAX_VALUE << 32;
        ChunkHolder chunkHolder = (ChunkHolder)this.chunks.get(l);
        if (chunkHolder == null && add) {
            chunkHolder = new ChunkHolder(chunkX, chunkZ);
            this.chunks.put(l, chunkHolder);
        }
        return chunkHolder;
    }

    public void sendPacket(Packet packet, int x, int y, int z) {
        int i = x >> 4;
        int j = z >> 4;
        ChunkHolder chunkHolder = this.getChunk(i, j, false);
        if (chunkHolder != null) {
            chunkHolder.sendPacket(packet);
        }
    }

    public void onBlockChanged(int x, int y, int z) {
        int i = x >> 4;
        int j = z >> 4;
        ChunkHolder chunkHolder = this.getChunk(i, j, false);
        if (chunkHolder != null) {
            chunkHolder.onBlockChanged(x & 0xF, y, z & 0xF);
        }
    }

    public void addPlayer(ServerPlayerEntity player) {
        int i = (int)player.x >> 4;
        int j = (int)player.z >> 4;
        player.trackedX = player.x;
        player.trackedZ = player.z;
        for (int k = i - 10; k <= i + 10; ++k) {
            for (int l = j - 10; l <= j + 10; ++l) {
                this.getChunk(k, l, true).addPlayer(player);
            }
        }
        this.players.add(player);
    }

    public void removePlayer(ServerPlayerEntity player) {
        int i = (int)player.trackedX >> 4;
        int j = (int)player.trackedZ >> 4;
        for (int k = i - 10; k <= i + 10; ++k) {
            for (int l = j - 10; l <= j + 10; ++l) {
                ChunkHolder chunkHolder = this.getChunk(k, l, false);
                if (chunkHolder == null) continue;
                chunkHolder.removePlayer(player);
            }
        }
        this.players.remove(player);
    }

    private boolean isChunkWithinView(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        int i = chunkX - playerChunkX;
        int j = chunkZ - playerChunkZ;
        if (i < -10 || i > 10) {
            return false;
        }
        return j >= -10 && j <= 10;
    }

    public void movePlayer(ServerPlayerEntity player) {
        int i = (int)player.x >> 4;
        int j = (int)player.z >> 4;
        double d = player.trackedX - player.x;
        double e = player.trackedZ - player.z;
        double f = d * d + e * e;
        if (f < 64.0) {
            return;
        }
        int k = (int)player.trackedX >> 4;
        int l = (int)player.trackedZ >> 4;
        int m = i - k;
        int n = j - l;
        if (m == 0 && n == 0) {
            return;
        }
        for (int o = i - 10; o <= i + 10; ++o) {
            for (int p = j - 10; p <= j + 10; ++p) {
                ChunkHolder chunkHolder;
                if (!this.isChunkWithinView(o, p, k, l)) {
                    this.getChunk(o, p, true).addPlayer(player);
                }
                if (this.isChunkWithinView(o - m, p - n, i, j) || (chunkHolder = this.getChunk(o - m, p - n, false)) == null) continue;
                chunkHolder.removePlayer(player);
            }
        }
        player.trackedX = player.x;
        player.trackedZ = player.z;
    }

    public int getViewDistance() {
        return 144;
    }

    @Environment(value=EnvType.SERVER)
    class ChunkHolder {
        private List players = new ArrayList();
        private int chunkX;
        private int chunkZ;
        private ChunkPos pos;
        private short[] dirtyBlocks = new short[10];
        private int blocksChanged = 0;
        private int dirtyLocalMinX;
        private int dirtyLocalMaxX;
        private int dirtyMinY;
        private int dirtyMaxY;
        private int dirtyLocalMinZ;
        private int dirtyLocalMaxZ;

        public ChunkHolder(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.pos = new ChunkPos(chunkX, chunkZ);
            ((ChunkMap)ChunkMap.this).server.world.chunkCache.loadChunk(chunkX, chunkZ);
        }

        public void addPlayer(ServerPlayerEntity player) {
            if (this.players.contains(player)) {
                throw new IllegalStateException("Failed to add player. " + player + " already is in chunk " + this.chunkX + ", " + this.chunkZ);
            }
            player.allLoadedChunks.add(this.pos);
            player.networkHandler.sendPacket(new LoadWorldChunkPacket(this.pos.x, this.pos.z, true));
            this.players.add(player);
            player.pendingChunks.add(this.pos);
        }

        public void removePlayer(ServerPlayerEntity player) {
            if (!this.players.contains(player)) {
                new IllegalStateException("Failed to remove player. " + player + " isn't in chunk " + this.chunkX + ", " + this.chunkZ).printStackTrace();
                return;
            }
            this.players.remove(player);
            if (this.players.size() == 0) {
                long l = (long)this.chunkX + Integer.MAX_VALUE | (long)this.chunkZ + Integer.MAX_VALUE << 32;
                ChunkMap.this.chunks.remove(l);
                if (this.blocksChanged > 0) {
                    ChunkMap.this.dirty.remove(this);
                }
                ((ChunkMap)ChunkMap.this).server.world.chunkCache.unloadChunk(this.chunkX, this.chunkZ);
            }
            player.pendingChunks.remove(this.pos);
            if (player.allLoadedChunks.contains(this.pos)) {
                player.networkHandler.sendPacket(new LoadWorldChunkPacket(this.chunkX, this.chunkZ, false));
            }
        }

        public void onBlockChanged(int localX, int y, int localZ) {
            if (this.blocksChanged == 0) {
                ChunkMap.this.dirty.add(this);
                this.dirtyLocalMinX = this.dirtyLocalMaxX = localX;
                this.dirtyMinY = this.dirtyMaxY = y;
                this.dirtyLocalMinZ = this.dirtyLocalMaxZ = localZ;
            }
            if (this.dirtyLocalMinX > localX) {
                this.dirtyLocalMinX = localX;
            }
            if (this.dirtyLocalMaxX < localX) {
                this.dirtyLocalMaxX = localX;
            }
            if (this.dirtyMinY > y) {
                this.dirtyMinY = y;
            }
            if (this.dirtyMaxY < y) {
                this.dirtyMaxY = y;
            }
            if (this.dirtyLocalMinZ > localZ) {
                this.dirtyLocalMinZ = localZ;
            }
            if (this.dirtyLocalMaxZ < localZ) {
                this.dirtyLocalMaxZ = localZ;
            }
            if (this.blocksChanged < 10) {
                short i = (short)(localX << 12 | localZ << 8 | y);
                for (int j = 0; j < this.blocksChanged; ++j) {
                    if (this.dirtyBlocks[j] != i) continue;
                    return;
                }
                this.dirtyBlocks[this.blocksChanged++] = i;
            }
        }

        public void sendPacket(Packet packet) {
            for (int i = 0; i < this.players.size(); ++i) {
                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
                if (!serverPlayerEntity.allLoadedChunks.contains(this.pos)) continue;
                serverPlayerEntity.networkHandler.sendPacket(packet);
            }
        }

        public void sendChanges() {
            if (this.blocksChanged == 0) {
                return;
            }
            if (this.blocksChanged == 1) {
                int i = this.chunkX * 16 + this.dirtyLocalMinX;
                int l = this.dirtyMinY;
                int o = this.chunkZ * 16 + this.dirtyLocalMinZ;
                this.sendPacket(new BlockUpdatePacket(i, l, o, ((ChunkMap)ChunkMap.this).server.world));
                if (Block.HAS_BLOCK_ENTITY[((ChunkMap)ChunkMap.this).server.world.getBlock(i, l, o)]) {
                    this.sendPacket(new BlockEntityUpdatePacket(i, l, o, ((ChunkMap)ChunkMap.this).server.world.getBlockEntity(i, l, o)));
                }
            } else if (this.blocksChanged == 10) {
                this.dirtyMinY = this.dirtyMinY / 2 * 2;
                this.dirtyMaxY = (this.dirtyMaxY / 2 + 1) * 2;
                int j = this.dirtyLocalMinX + this.chunkX * 16;
                int m = this.dirtyMinY;
                int p = this.dirtyLocalMinZ + this.chunkZ * 16;
                int r = this.dirtyLocalMaxX - this.dirtyLocalMinX + 1;
                int t = this.dirtyMaxY - this.dirtyMinY + 2;
                int u = this.dirtyLocalMaxZ - this.dirtyLocalMinZ + 1;
                this.sendPacket(new WorldChunkPacket(j, m, p, r, t, u, ((ChunkMap)ChunkMap.this).server.world));
                List list = ((ChunkMap)ChunkMap.this).server.world.getBlockEntities(j, m, p, j + r, m + t, p + u);
                for (int v = 0; v < list.size(); ++v) {
                    BlockEntity blockEntity = (BlockEntity)list.get(v);
                    this.sendPacket(new BlockEntityUpdatePacket(blockEntity.x, blockEntity.y, blockEntity.z, blockEntity));
                }
            } else {
                this.sendPacket(new BlocksUpdatePacket(this.chunkX, this.chunkZ, this.dirtyBlocks, this.blocksChanged, ((ChunkMap)ChunkMap.this).server.world));
                for (int k = 0; k < this.blocksChanged; ++k) {
                    int n = this.chunkX * 16 + (this.blocksChanged >> 12 & 0xF);
                    int q = this.blocksChanged & 0xFF;
                    int s = this.chunkZ * 16 + (this.blocksChanged >> 8 & 0xF);
                    if (!Block.HAS_BLOCK_ENTITY[((ChunkMap)ChunkMap.this).server.world.getBlock(n, q, s)]) continue;
                    this.sendPacket(new BlockEntityUpdatePacket(n, q, s, ((ChunkMap)ChunkMap.this).server.world.getBlockEntity(n, q, s)));
                }
            }
            this.blocksChanged = 0;
        }
    }
}

