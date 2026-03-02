/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.MathHelper;

public class AddPlayerPacket
extends Packet {
    public int id;
    public String name;
    public int x;
    public int y;
    public int z;
    public byte yaw;
    public byte pitch;
    public int itemInHand;

    public AddPlayerPacket() {
    }

    public AddPlayerPacket(PlayerEntity player) {
        this.id = player.networkId;
        this.name = player.name;
        this.x = MathHelper.floor(player.x * 32.0);
        this.y = MathHelper.floor(player.y * 32.0);
        this.z = MathHelper.floor(player.z * 32.0);
        this.yaw = (byte)(player.yaw * 256.0f / 360.0f);
        this.pitch = (byte)(player.pitch * 256.0f / 360.0f);
        ItemStack itemStack = player.inventory.getSelectedItem();
        this.itemInHand = itemStack == null ? 0 : itemStack.id;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.name = input.readUTF();
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
        this.yaw = input.readByte();
        this.pitch = input.readByte();
        this.itemInHand = input.readShort();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeUTF(this.name);
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
        output.writeByte(this.yaw);
        output.writeByte(this.pitch);
        output.writeShort(this.itemInHand);
    }

    public void handle(PacketHandler handler) {
        handler.handleAddPlayer(this);
    }

    public int getSize() {
        return 28;
    }
}

