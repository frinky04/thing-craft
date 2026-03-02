/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.entity.Entities;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.MathHelper;

public class AddMobPacket
extends Packet {
    public int id;
    public byte type;
    public int x;
    public int y;
    public int z;
    public byte yaw;
    public byte pitch;

    public AddMobPacket() {
    }

    public AddMobPacket(MobEntity entity) {
        this.id = entity.networkId;
        this.type = (byte)Entities.getId(entity);
        this.x = MathHelper.floor(entity.x * 32.0);
        this.y = MathHelper.floor(entity.y * 32.0);
        this.z = MathHelper.floor(entity.z * 32.0);
        this.yaw = (byte)(entity.yaw * 256.0f / 360.0f);
        this.pitch = (byte)(entity.pitch * 256.0f / 360.0f);
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.type = input.readByte();
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
        this.yaw = input.readByte();
        this.pitch = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeByte(this.type);
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
        output.writeByte(this.yaw);
        output.writeByte(this.pitch);
    }

    public void handle(PacketHandler handler) {
        handler.handleAddMob(this);
    }

    public int getSize() {
        return 19;
    }
}

