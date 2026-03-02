/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class EntityAnimationPacket
extends Packet {
    public int id;
    public int action;

    public EntityAnimationPacket() {
    }

    public EntityAnimationPacket(Entity entity, int action) {
        this.id = entity.networkId;
        this.action = action;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.action = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeByte(this.action);
    }

    public void handle(PacketHandler handler) {
        handler.handleEntityAnimation(this);
    }

    public int getSize() {
        return 5;
    }
}

