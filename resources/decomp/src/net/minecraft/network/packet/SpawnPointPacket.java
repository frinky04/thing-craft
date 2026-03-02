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

public class SpawnPointPacket
extends Packet {
    public int x;
    public int y;
    public int z;

    public SpawnPointPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public SpawnPointPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void read(DataInputStream input) {
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
    }

    public void handle(PacketHandler handler) {
        handler.handleSpawnPoint(this);
    }

    public int getSize() {
        return 12;
    }
}

