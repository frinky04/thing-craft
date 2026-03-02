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

public class PlayerHealthPacket
extends Packet {
    public int health;

    public PlayerHealthPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public PlayerHealthPacket(int health) {
        this.health = health;
    }

    public void read(DataInputStream input) {
        this.health = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeByte(this.health);
    }

    public void handle(PacketHandler handler) {
        handler.handlePlayerHealth(this);
    }

    public int getSize() {
        return 1;
    }
}

