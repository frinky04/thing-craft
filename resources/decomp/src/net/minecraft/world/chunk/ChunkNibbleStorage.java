/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.chunk;

public class ChunkNibbleStorage {
    public final byte[] data;

    public ChunkNibbleStorage(int size) {
        this.data = new byte[size >> 1];
    }

    public ChunkNibbleStorage(byte[] data) {
        this.data = data;
    }

    public int get(int x, int y, int z) {
        int i = x << 11 | z << 7 | y;
        int j = i >> 1;
        int k = i & 1;
        if (k == 0) {
            return this.data[j] & 0xF;
        }
        return this.data[j] >> 4 & 0xF;
    }

    public void set(int x, int y, int z, int value) {
        int i = x << 11 | z << 7 | y;
        int j = i >> 1;
        int k = i & 1;
        this.data[j] = k == 0 ? (byte)(this.data[j] & 0xF0 | value & 0xF) : (byte)(this.data[j] & 0xF | (value & 0xF) << 4);
    }

    public boolean hasData() {
        return this.data != null;
    }
}

