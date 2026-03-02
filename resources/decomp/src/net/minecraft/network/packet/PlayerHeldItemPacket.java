/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class PlayerHeldItemPacket
extends Packet {
    public int id;
    public int item;

    public PlayerHeldItemPacket() {
    }

    public PlayerHeldItemPacket(int id, int item) {
        this.id = id;
        this.item = item;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.item = input.readShort();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeShort(this.item);
    }

    public void handle(PacketHandler handler) {
        handler.handlePlayerHeldItem(this);
    }

    public int getSize() {
        return 6;
    }
}

