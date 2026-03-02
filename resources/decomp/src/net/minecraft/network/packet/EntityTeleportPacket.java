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
import net.minecraft.util.math.MathHelper;

public class EntityTeleportPacket
extends Packet {
    public int id;
    public int x;
    public int y;
    public int z;
    public byte yaw;
    public byte pitch;

    public EntityTeleportPacket() {
    }

    public EntityTeleportPacket(Entity entity) {
        this.id = entity.networkId;
        this.x = MathHelper.floor(entity.x * 32.0);
        this.y = MathHelper.floor(entity.y * 32.0);
        this.z = MathHelper.floor(entity.z * 32.0);
        this.yaw = (byte)(entity.yaw * 256.0f / 360.0f);
        this.pitch = (byte)(entity.pitch * 256.0f / 360.0f);
    }

    @Environment(value=EnvType.SERVER)
    public EntityTeleportPacket(int id, int x, int y, int z, byte yaw, byte pitch) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
        this.yaw = (byte)input.read();
        this.pitch = (byte)input.read();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
        output.write(this.yaw);
        output.write(this.pitch);
    }

    public void handle(PacketHandler handler) {
        handler.handleEntityTeleport(this);
    }

    public int getSize() {
        return 34;
    }
}

