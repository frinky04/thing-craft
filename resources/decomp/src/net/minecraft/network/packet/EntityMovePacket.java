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

public class EntityMovePacket
extends Packet {
    public int id;
    public byte dx;
    public byte dy;
    public byte dz;
    public byte yaw;
    public byte pitch;
    public boolean hasAngles = false;

    public EntityMovePacket() {
    }

    @Environment(value=EnvType.SERVER)
    public EntityMovePacket(int id) {
        this.id = id;
    }

    public void read(DataInputStream input) {
        this.id = input.readInt();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.id);
    }

    public void handle(PacketHandler handler) {
        handler.handleEntityMove(this);
    }

    public int getSize() {
        return 4;
    }

    public static class PositionAndAngles
    extends EntityMovePacket {
        public PositionAndAngles() {
            this.hasAngles = true;
        }

        @Environment(value=EnvType.SERVER)
        public PositionAndAngles(int id, byte dx, byte dy, byte dz, byte yaw, byte pitch) {
            super(id);
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.yaw = yaw;
            this.pitch = pitch;
            this.hasAngles = true;
        }

        public void read(DataInputStream input) {
            super.read(input);
            this.dx = input.readByte();
            this.dy = input.readByte();
            this.dz = input.readByte();
            this.yaw = input.readByte();
            this.pitch = input.readByte();
        }

        public void write(DataOutputStream output) {
            super.write(output);
            output.writeByte(this.dx);
            output.writeByte(this.dy);
            output.writeByte(this.dz);
            output.writeByte(this.yaw);
            output.writeByte(this.pitch);
        }

        public int getSize() {
            return 9;
        }
    }

    public static class Angles
    extends EntityMovePacket {
        public Angles() {
            this.hasAngles = true;
        }

        @Environment(value=EnvType.SERVER)
        public Angles(int id, byte yaw, byte pitch) {
            super(id);
            this.yaw = yaw;
            this.pitch = pitch;
            this.hasAngles = true;
        }

        public void read(DataInputStream input) {
            super.read(input);
            this.yaw = input.readByte();
            this.pitch = input.readByte();
        }

        public void write(DataOutputStream output) {
            super.write(output);
            output.writeByte(this.yaw);
            output.writeByte(this.pitch);
        }

        public int getSize() {
            return 6;
        }
    }

    public static class Position
    extends EntityMovePacket {
        public Position() {
        }

        @Environment(value=EnvType.SERVER)
        public Position(int id, byte dx, byte dy, byte dz) {
            super(id);
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        public void read(DataInputStream input) {
            super.read(input);
            this.dx = input.readByte();
            this.dy = input.readByte();
            this.dz = input.readByte();
        }

        public void write(DataOutputStream output) {
            super.write(output);
            output.writeByte(this.dx);
            output.writeByte(this.dy);
            output.writeByte(this.dz);
        }

        public int getSize() {
            return 7;
        }
    }
}

