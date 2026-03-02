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

public class EntityEventPacket
extends Packet {
    public int id;
    public byte event;

    public EntityEventPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public EntityEventPacket(int id, byte event) {
        this.id = id;
        this.event = event;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.event = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeByte(this.event);
    }

    public void handle(PacketHandler handler) {
        handler.handleEntityEvent(this);
    }

    public int getSize() {
        return 5;
    }
}

