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
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;

public class WorldChunkPacket
extends Packet {
    public int x;
    public int y;
    public int z;
    public int sizeX;
    public int sizeY;
    public int sizeZ;
    public byte[] data;
    private int compressedSize;

    public WorldChunkPacket() {
        this.shouldDelay = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Environment(value=EnvType.SERVER)
    public WorldChunkPacket(int minX, int minY, int minZ, int widthX, int height, int widthZ, World world) {
        this.shouldDelay = true;
        this.x = minX;
        this.y = minY;
        this.z = minZ;
        this.sizeX = widthX;
        this.sizeY = height;
        this.sizeZ = widthZ;
        byte[] bs = world.packChunkData(minX, minY, minZ, widthX, height, widthZ);
        Deflater deflater = new Deflater(1);
        try {
            deflater.setInput(bs);
            deflater.finish();
            this.data = new byte[widthX * height * widthZ * 5 / 2];
            this.compressedSize = deflater.deflate(this.data);
        }
        finally {
            deflater.end();
        }
    }

    public void read(DataInputStream input) {
        this.x = input.readInt();
        this.y = input.readShort();
        this.z = input.readInt();
        this.sizeX = input.read() + 1;
        this.sizeY = input.read() + 1;
        this.sizeZ = input.read() + 1;
        int i = input.readInt();
        byte[] bs = new byte[i];
        input.readFully(bs);
        this.data = new byte[this.sizeX * this.sizeY * this.sizeZ * 5 / 2];
        Inflater inflater = new Inflater();
        inflater.setInput(bs);
        try {
            inflater.inflate(this.data);
        }
        catch (DataFormatException dataFormatException) {
            throw new IOException("Bad compressed data format");
        }
        finally {
            inflater.end();
        }
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.x);
        output.writeShort(this.y);
        output.writeInt(this.z);
        output.write(this.sizeX - 1);
        output.write(this.sizeY - 1);
        output.write(this.sizeZ - 1);
        output.writeInt(this.compressedSize);
        output.write(this.data, 0, this.compressedSize);
    }

    public void handle(PacketHandler handler) {
        handler.handleWorldChunk(this);
    }

    public int getSize() {
        return 17 + this.compressedSize;
    }
}

