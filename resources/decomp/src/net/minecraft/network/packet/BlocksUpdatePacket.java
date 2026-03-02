/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class BlocksUpdatePacket
extends Packet {
    public int chunkX;
    public int chunkZ;
    public short[] positions;
    public byte[] blockIds;
    public byte[] blockMetadata;
    public int blockChangeCount;

    public BlocksUpdatePacket() {
        this.shouldDelay = true;
    }

    @Environment(value=EnvType.SERVER)
    public BlocksUpdatePacket(int chunkX, int chunkZ, short[] positions, int blockChangeCount, World world) {
        this.shouldDelay = true;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blockChangeCount = blockChangeCount;
        this.positions = new short[blockChangeCount];
        this.blockIds = new byte[blockChangeCount];
        this.blockMetadata = new byte[blockChangeCount];
        WorldChunk worldChunk = world.getChunkAt(chunkX, chunkZ);
        for (int i = 0; i < blockChangeCount; ++i) {
            int j = positions[i] >> 12 & 0xF;
            int k = positions[i] >> 8 & 0xF;
            int l = positions[i] & 0xFF;
            this.positions[i] = positions[i];
            this.blockIds[i] = (byte)worldChunk.getBlockAt(j, l, k);
            this.blockMetadata[i] = (byte)worldChunk.getBlockMetadataAt(j, l, k);
        }
    }

    public void read(DataInputStream input) {
        this.chunkX = input.readInt();
        this.chunkZ = input.readInt();
        this.blockChangeCount = input.readShort() & 0xFFFF;
        this.positions = new short[this.blockChangeCount];
        this.blockIds = new byte[this.blockChangeCount];
        this.blockMetadata = new byte[this.blockChangeCount];
        for (int i = 0; i < this.blockChangeCount; ++i) {
            this.positions[i] = input.readShort();
        }
        input.readFully(this.blockIds);
        input.readFully(this.blockMetadata);
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.chunkX);
        output.writeInt(this.chunkZ);
        output.writeShort((short)this.blockChangeCount);
        for (int i = 0; i < this.blockChangeCount; ++i) {
            output.writeShort(this.positions[i]);
        }
        output.write(this.blockIds);
        output.write(this.blockMetadata);
    }

    public void handle(PacketHandler handler) {
        handler.handleBlocksUpdate(this);
    }

    public int getSize() {
        return 10 + this.blockChangeCount * 4;
    }
}

