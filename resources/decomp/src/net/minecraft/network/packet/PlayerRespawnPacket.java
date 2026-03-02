/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class PlayerRespawnPacket
extends Packet {
    public void handle(PacketHandler handler) {
        handler.handlePlayerRespawn(this);
    }

    public void read(DataInputStream input) {
    }

    public void write(DataOutputStream output) {
    }

    public int getSize() {
        return 0;
    }
}

