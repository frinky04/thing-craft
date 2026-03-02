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

public class EntityPickupPacket
extends Packet {
    public int id;
    public int collectorId;

    public EntityPickupPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public EntityPickupPacket(int id, int collectorId) {
        this.id = id;
        this.collectorId = collectorId;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.collectorId = input.readInt();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeInt(this.collectorId);
    }

    public void handle(PacketHandler handler) {
        handler.handleEntityPickup(this);
    }

    public int getSize() {
        return 8;
    }
}

