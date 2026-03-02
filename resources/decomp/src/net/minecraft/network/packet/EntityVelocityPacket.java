/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;

public class EntityVelocityPacket
extends Packet {
    public int id;
    public int velocityX;
    public int velocityY;
    public int velocityZ;

    public EntityVelocityPacket() {
    }

    public EntityVelocityPacket(Entity entity) {
        this(entity.networkId, entity.velocityX, entity.velocityY, entity.velocityZ);
    }

    public EntityVelocityPacket(int id, double velocityX, double velocityY, double velocityZ) {
        this.id = id;
        double d = 3.9;
        if (velocityX < -d) {
            velocityX = -d;
        }
        if (velocityY < -d) {
            velocityY = -d;
        }
        if (velocityZ < -d) {
            velocityZ = -d;
        }
        if (velocityX > d) {
            velocityX = d;
        }
        if (velocityY > d) {
            velocityY = d;
        }
        if (velocityZ > d) {
            velocityZ = d;
        }
        this.velocityX = (int)(velocityX * 8000.0);
        this.velocityY = (int)(velocityY * 8000.0);
        this.velocityZ = (int)(velocityZ * 8000.0);
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
        this.velocityX = input.readShort();
        this.velocityY = input.readShort();
        this.velocityZ = input.readShort();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
        output.writeShort(this.velocityX);
        output.writeShort(this.velocityY);
        output.writeShort(this.velocityZ);
    }

    public void handle(PacketHandler handler) {
        handler.handleEntityVelocity(this);
    }

    public int getSize() {
        return 10;
    }
}

