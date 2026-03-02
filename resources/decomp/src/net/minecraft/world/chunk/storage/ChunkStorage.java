/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.chunk.storage;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public interface ChunkStorage {
    public WorldChunk loadChunk(World var1, int var2, int var3);

    public void saveChunk(World var1, WorldChunk var2);

    public void saveEntities(World var1, WorldChunk var2);

    public void tick();

    public void flush();
}

