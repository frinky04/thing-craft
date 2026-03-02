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

public class WorldTimePacket
extends Packet {
    public long time;

    public WorldTimePacket() {
    }

    @Environment(value=EnvType.SERVER)
    public WorldTimePacket(long l) {
        this.time = l;
    }

    public void read(DataInputStream input) {
        this.time = input.readLong();
    }

    public void write(DataOutputStream output) {
        output.writeLong(this.time);
    }

    public void handle(PacketHandler handler) {
        handler.handleWorldTime(this);
    }

    public int getSize() {
        return 8;
    }
}

