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

public class LoadWorldChunkPacket
extends Packet {
    public int chunkX;
    public int chunkZ;
    public boolean load;

    public LoadWorldChunkPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public LoadWorldChunkPacket(int chunkX, int chunkZ, boolean load) {
        this.shouldDelay = true;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.load = load;
    }

    public void read(DataInputStream input) {
        this.chunkX = input.readInt();
        this.chunkZ = input.readInt();
        this.load = input.read() != 0;
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.chunkX);
        output.writeInt(this.chunkZ);
        output.write(this.load ? 1 : 0);
    }

    public void handle(PacketHandler handler) {
        handler.handleLoadWorldChunk(this);
    }

    public int getSize() {
        return 9;
    }
}

