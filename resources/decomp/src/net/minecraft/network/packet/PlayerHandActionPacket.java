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

public class PlayerHandActionPacket
extends Packet {
    public int x;
    public int y;
    public int z;
    public int face;
    public int action;

    public PlayerHandActionPacket() {
    }

    @Environment(value=EnvType.CLIENT)
    public PlayerHandActionPacket(int action, int x, int y, int z, int face) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    public void read(DataInputStream input) {
        this.action = input.read();
        this.x = input.readInt();
        this.y = input.read();
        this.z = input.readInt();
        this.face = input.read();
    }

    public void write(DataOutputStream output) {
        output.write(this.action);
        output.writeInt(this.x);
        output.write(this.y);
        output.writeInt(this.z);
        output.write(this.face);
    }

    public void handle(PacketHandler handler) {
        handler.handlePlayerHandAction(this);
    }

    public int getSize() {
        return 11;
    }
}

