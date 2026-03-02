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

public class AddEntityPacket
extends Packet {
    public int id;
    public int x;
    public int y;
    public int z;
    public int type;

    public AddEntityPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public AddEntityPacket(Entity entity, int type) {
        this.id = entity.networkId;
        this.x = MathHelper.floor(entity.x * 32.0);
        this.y = MathHelper.floor(entity.y * 32.0);
        this.z = MathHelper.floor(entity.z * 32.0);
        this.type = type;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.type = input.readByte();
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeByte(this.type);
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
    }

    public void handle(PacketHandler handler) {
        handler.handleAddEntity(this);
    }

    public int getSize() {
        return 17;
    }
}

