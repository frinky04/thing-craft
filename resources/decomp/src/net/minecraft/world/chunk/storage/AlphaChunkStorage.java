/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.chunk.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleStorage;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.storage.ChunkStorage;

public class AlphaChunkStorage
implements ChunkStorage {
    private File dir;
    private boolean make;

    public AlphaChunkStorage(File dir, boolean make) {
        this.dir = dir;
        this.make = make;
    }

    private File getChunkFile(int chunkX, int chunkZ) {
        String string = "c." + Integer.toString(chunkX, 36) + "." + Integer.toString(chunkZ, 36) + ".dat";
        String string2 = Integer.toString(chunkX & 0x3F, 36);
        String string3 = Integer.toString(chunkZ & 0x3F, 36);
        File file = new File(this.dir, string2);
        if (!file.exists()) {
            if (this.make) {
                file.mkdir();
            } else {
                return null;
            }
        }
        if (!(file = new File(file, string3)).exists()) {
            if (this.make) {
                file.mkdir();
            } else {
                return null;
            }
        }
        if (!(file = new File(file, string)).exists() && !this.make) {
            return null;
        }
        return file;
    }

    public WorldChunk loadChunk(World world, int chunkX, int chunkZ) {
        File file = this.getChunkFile(chunkX, chunkZ);
        if (file != null && file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                NbtCompound nbtCompound = NbtIo.readCompressed(fileInputStream);
                if (!nbtCompound.contains("Level")) {
                    System.out.println("Chunk file at " + chunkX + "," + chunkZ + " is missing level data, skipping");
                    return null;
                }
                if (!nbtCompound.getCompound("Level").contains("Blocks")) {
                    System.out.println("Chunk file at " + chunkX + "," + chunkZ + " is missing block data, skipping");
                    return null;
                }
                WorldChunk worldChunk = AlphaChunkStorage.loadChunkFromNbt(world, nbtCompound.getCompound("Level"));
                if (!worldChunk.isAt(chunkX, chunkZ)) {
                    System.out.println("Chunk file at " + chunkX + "," + chunkZ + " is in the wrong location; relocating. (Expected " + chunkX + ", " + chunkZ + ", got " + worldChunk.chunkX + ", " + worldChunk.chunkZ + ")");
                    nbtCompound.putInt("xPos", chunkX);
                    nbtCompound.putInt("zPos", chunkZ);
                    worldChunk = AlphaChunkStorage.loadChunkFromNbt(world, nbtCompound.getCompound("Level"));
                }
                return worldChunk;
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    public void saveChunk(World world, WorldChunk chunk) {
        world.checkSessionLock();
        File file = this.getChunkFile(chunk.chunkX, chunk.chunkZ);
        if (file.exists()) {
            world.sizeOnDisk -= file.length();
        }
        try {
            File file2 = new File(this.dir, "tmp_chunk.dat");
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            NbtCompound nbtCompound = new NbtCompound();
            NbtCompound nbtCompound2 = new NbtCompound();
            nbtCompound.put("Level", nbtCompound2);
            this.saveChunkToNbt(chunk, world, nbtCompound2);
            NbtIo.writeCompressed(nbtCompound, fileOutputStream);
            fileOutputStream.close();
            if (file.exists()) {
                file.delete();
            }
            file2.renameTo(file);
            world.sizeOnDisk += file.length();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void saveChunkToNbt(WorldChunk chunk, World world, NbtCompound nbt) {
        world.checkSessionLock();
        nbt.putInt("xPos", chunk.chunkX);
        nbt.putInt("zPos", chunk.chunkZ);
        nbt.putLong("LastUpdate", world.ticks);
        nbt.putByteArray("Blocks", chunk.blocks);
        nbt.putByteArray("Data", chunk.blockMetadata.data);
        nbt.putByteArray("SkyLight", chunk.skyLight.data);
        nbt.putByteArray("BlockLight", chunk.blockLight.data);
        nbt.putByteArray("HeightMap", chunk.heightMap);
        nbt.putBoolean("TerrainPopulated", chunk.terrainPopulated);
        chunk.lastSaveHadEntities = false;
        NbtList nbtList = new NbtList();
        for (int i = 0; i < chunk.entities.length; ++i) {
            for (Entity entity : chunk.entities[i]) {
                chunk.lastSaveHadEntities = true;
                NbtCompound nbtCompound = new NbtCompound();
                if (!entity.writeNbt(nbtCompound)) continue;
                nbtList.addElement(nbtCompound);
            }
        }
        nbt.put("Entities", nbtList);
        NbtList nbtList2 = new NbtList();
        for (BlockEntity blockEntity : chunk.blockEntities.values()) {
            NbtCompound nbtCompound2 = new NbtCompound();
            blockEntity.writeNbt(nbtCompound2);
            nbtList2.addElement(nbtCompound2);
        }
        nbt.put("TileEntities", nbtList2);
    }

    public static WorldChunk loadChunkFromNbt(World world, NbtCompound nbt) {
        NbtList nbtList2;
        NbtList nbtList;
        int i = nbt.getInt("xPos");
        int j = nbt.getInt("zPos");
        WorldChunk worldChunk = new WorldChunk(world, i, j);
        worldChunk.blocks = nbt.getByteArray("Blocks");
        worldChunk.blockMetadata = new ChunkNibbleStorage(nbt.getByteArray("Data"));
        worldChunk.skyLight = new ChunkNibbleStorage(nbt.getByteArray("SkyLight"));
        worldChunk.blockLight = new ChunkNibbleStorage(nbt.getByteArray("BlockLight"));
        worldChunk.heightMap = nbt.getByteArray("HeightMap");
        worldChunk.terrainPopulated = nbt.getBoolean("TerrainPopulated");
        if (!worldChunk.blockMetadata.hasData()) {
            worldChunk.blockMetadata = new ChunkNibbleStorage(worldChunk.blocks.length);
        }
        if (worldChunk.heightMap == null || !worldChunk.skyLight.hasData()) {
            worldChunk.heightMap = new byte[256];
            worldChunk.skyLight = new ChunkNibbleStorage(worldChunk.blocks.length);
            worldChunk.populateHeightMap();
        }
        if (!worldChunk.blockLight.hasData()) {
            worldChunk.blockLight = new ChunkNibbleStorage(worldChunk.blocks.length);
            worldChunk.populateLight();
        }
        if ((nbtList = nbt.getList("Entities")) != null) {
            for (int k = 0; k < nbtList.size(); ++k) {
                NbtCompound nbtCompound = (NbtCompound)nbtList.get(k);
                Entity entity = Entities.create(nbtCompound, world);
                worldChunk.lastSaveHadEntities = true;
                if (entity == null) continue;
                worldChunk.addEntity(entity);
            }
        }
        if ((nbtList2 = nbt.getList("TileEntities")) != null) {
            for (int l = 0; l < nbtList2.size(); ++l) {
                NbtCompound nbtCompound2 = (NbtCompound)nbtList2.get(l);
                BlockEntity blockEntity = BlockEntity.fromNbt(nbtCompound2);
                if (blockEntity == null) continue;
                worldChunk.addBlockEntity(blockEntity);
            }
        }
        return worldChunk;
    }

    public void tick() {
    }

    public void flush() {
    }

    public void saveEntities(World world, WorldChunk chunk) {
    }
}

