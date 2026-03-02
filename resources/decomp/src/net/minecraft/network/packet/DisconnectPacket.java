/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class DisconnectPacket
extends Packet {
    public String reason;

    public DisconnectPacket() {
    }

    public DisconnectPacket(String reason) {
        this.reason = reason;
    }

    public void read(DataInputStream input) {
        this.reason = input.readUTF();
    }

    public void write(DataOutputStream output) {
        output.writeUTF(this.reason);
    }

    public void handle(PacketHandler handler) {
        handler.handleDisconnect(this);
    }

    public int getSize() {
        return this.reason.length();
    }
}

