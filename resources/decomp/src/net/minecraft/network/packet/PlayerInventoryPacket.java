/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class PlayerInventoryPacket
extends Packet {
    public int type;
    public ItemStack[] items;

    public PlayerInventoryPacket() {
    }

    public PlayerInventoryPacket(int type, ItemStack[] items) {
        this.type = type;
        this.items = new ItemStack[items.length];
        for (int i = 0; i < this.items.length; ++i) {
            this.items[i] = items[i] == null ? null : items[i].copy();
        }
    }

    public void read(DataInputStream input) {
        this.type = input.readInt();
        int i = input.readShort();
        this.items = new ItemStack[i];
        for (int j = 0; j < i; ++j) {
            short k = input.readShort();
            if (k < 0) continue;
            byte l = input.readByte();
            short m = input.readShort();
            this.items[j] = new ItemStack(k, l, m);
        }
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.type);
        output.writeShort(this.items.length);
        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null) {
                output.writeShort(-1);
                continue;
            }
            output.writeShort((short)this.items[i].id);
            output.writeByte((byte)this.items[i].size);
            output.writeShort((short)this.items[i].metadata);
        }
    }

    public void handle(PacketHandler handler) {
        handler.handlePlayerInventory(this);
    }

    public int getSize() {
        return 6 + this.items.length * 5;
    }
}

