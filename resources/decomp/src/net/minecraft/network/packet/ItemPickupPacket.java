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
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class ItemPickupPacket
extends Packet {
    public int id;
    public int count;
    public int metadata;

    public ItemPickupPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public ItemPickupPacket(ItemStack item, int count) {
        this.id = item.id;
        this.count = count;
        this.metadata = item.metadata;
        if (count == 0) {
            count = 1;
        }
    }

    public void read(DataInputStream input) {
        this.id = input.readShort();
        this.count = input.readByte();
        this.metadata = input.readShort();
    }

    public void write(DataOutputStream output) {
        output.writeShort(this.id);
        output.writeByte(this.count);
        output.writeShort(this.metadata);
    }

    public void handle(PacketHandler handler) {
        handler.handleItemPickup(this);
    }

    public int getSize() {
        return 5;
    }
}

