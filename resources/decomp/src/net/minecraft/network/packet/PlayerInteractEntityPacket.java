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

public class PlayerInteractEntityPacket
extends Packet {
    public int playerId;
    public int targetId;
    public int action;

    public PlayerInteractEntityPacket() {
    }

    @Environment(value=EnvType.CLIENT)
    public PlayerInteractEntityPacket(int playerId, int targetId, int action) {
        this.playerId = playerId;
        this.targetId = targetId;
        this.action = action;
    }

    public void read(DataInputStream input) {
        this.playerId = input.readInt();
        this.targetId = input.readInt();
        this.action = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.playerId);
        output.writeInt(this.targetId);
        output.writeByte(this.action);
    }

    public void handle(PacketHandler handler) {
        handler.handleInteractEntity(this);
    }

    public int getSize() {
        return 9;
    }
}

