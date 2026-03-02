/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class ChatMessagePacket
extends Packet {
    public String message;

    public ChatMessagePacket() {
    }

    public ChatMessagePacket(String message) {
        this.message = message;
    }

    public void read(DataInputStream input) {
        this.message = input.readUTF();
    }

    public void write(DataOutputStream output) {
        output.writeUTF(this.message);
    }

    public void handle(PacketHandler handler) {
        handler.handleChatMessage(this);
    }

    public int getSize() {
        return this.message.length();
    }
}

