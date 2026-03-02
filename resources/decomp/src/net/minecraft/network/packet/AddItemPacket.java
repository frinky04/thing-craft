/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.MathHelper;

public class AddItemPacket
extends Packet {
    public int id;
    public int x;
    public int y;
    public int z;
    public byte velocityX;
    public byte velocityY;
    public byte velocityZ;
    public int item;
    public int stackSize;

    public AddItemPacket() {
    }

    public AddItemPacket(ItemEntity item) {
        this.id = item.networkId;
        this.item = item.item.id;
        this.stackSize = item.item.size;
        this.x = MathHelper.floor(item.x * 32.0);
        this.y = MathHelper.floor(item.y * 32.0);
        this.z = MathHelper.floor(item.z * 32.0);
        this.velocityX = (byte)(item.velocityX * 128.0);
        this.velocityY = (byte)(item.velocityY * 128.0);
        this.velocityZ = (byte)(item.velocityZ * 128.0);
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.item = input.readShort();
        this.stackSize = input.readByte();
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
        this.velocityX = input.readByte();
        this.velocityY = input.readByte();
        this.velocityZ = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeShort(this.item);
        output.writeByte(this.stackSize);
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
        output.writeByte(this.velocityX);
        output.writeByte(this.velocityY);
        output.writeByte(this.velocityZ);
    }

    public void handle(PacketHandler handler) {
        handler.handleAddItem(this);
    }

    public int getSize() {
        return 22;
    }
}

