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
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class RideEntityPacket
extends Packet {
    public int id;
    public int mountId;

    public RideEntityPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public RideEntityPacket(Entity entity, Entity mount) {
        this.id = entity.networkId;
        this.mountId = mount != null ? mount.networkId : -1;
    }

    public int getSize() {
        return 8;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.mountId = input.readInt();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeInt(this.mountId);
    }

    public void handle(PacketHandler handler) {
        handler.handleRideEntity(this);
    }
}

