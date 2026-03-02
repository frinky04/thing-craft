/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class HandshakePacket
extends Packet {
    public String key;

    public HandshakePacket() {
    }

    public HandshakePacket(String key) {
        this.key = key;
    }

    public void read(DataInputStream input) {
        this.key = input.readUTF();
    }

    public void write(DataOutputStream output) {
        output.writeUTF(this.key);
    }

    public void handle(PacketHandler handler) {
        handler.handleHandshake(this);
    }

    public int getSize() {
        return 4 + this.key.length() + 4;
    }
}

