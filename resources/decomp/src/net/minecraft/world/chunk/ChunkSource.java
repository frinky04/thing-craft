/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.chunk;

import net.minecraft.util.ProgressListener;
import net.minecraft.world.chunk.WorldChunk;

public interface ChunkSource {
    public boolean hasChunk(int var1, int var2);

    public WorldChunk getChunk(int var1, int var2);

    public void populateChunk(ChunkSource var1, int var2, int var3);

    public boolean save(boolean var1, ProgressListener var2);

    public boolean tick();

    public boolean shouldSave();
}

