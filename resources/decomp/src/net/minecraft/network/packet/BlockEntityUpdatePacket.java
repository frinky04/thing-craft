/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class BlockEntityUpdatePacket
extends Packet {
    public int x;
    public int y;
    public int z;
    public byte[] data;
    public NbtCompound nbt;

    public BlockEntityUpdatePacket() {
        this.shouldDelay = true;
    }

    public BlockEntityUpdatePacket(int x, int y, int z, BlockEntity blockEntity) {
        this.shouldDelay = true;
        this.x = x;
        this.y = y;
        this.z = z;
        this.nbt = new NbtCompound();
        blockEntity.writeNbt(this.nbt);
        try {
            this.data = NbtIo.writeCompressed(this.nbt);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void read(DataInputStream input) {
        this.x = input.readInt();
        this.y = input.readShort();
        this.z = input.readInt();
        int i = input.readShort() & 0xFFFF;
        this.data = new byte[i];
        input.readFully(this.data);
        this.nbt = NbtIo.readCompressed(this.data);
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.x);
        output.writeShort(this.y);
        output.writeInt(this.z);
        output.writeShort((short)this.data.length);
        output.write(this.data);
    }

    public void handle(PacketHandler handler) {
        handler.handleBlockEntityUpdate(this);
    }

    public int getSize() {
        return this.data.length + 2 + 10;
    }
}

