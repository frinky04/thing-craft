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

public class PlayerUsePacket
extends Packet {
    public int x;
    public int y;
    public int z;
    public int face;
    public int item;

    public PlayerUsePacket() {
    }

    @Environment(value=EnvType.CLIENT)
    public PlayerUsePacket(int x, int y, int z, int face, int item) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.item = item;
    }

    public void read(DataInputStream input) {
        this.x = input.readShort();
        this.y = input.readInt();
        this.z = input.read();
        this.face = input.readInt();
        this.item = input.read();
    }

    public void write(DataOutputStream output) {
        output.writeShort(this.x);
        output.writeInt(this.y);
        output.write(this.z);
        output.writeInt(this.face);
        output.write(this.item);
    }

    public void handle(PacketHandler handler) {
        handler.handlePlayerUse(this);
    }

    public int getSize() {
        return 12;
    }
}

