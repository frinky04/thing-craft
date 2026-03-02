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

public class PlayerMovePacket
extends Packet {
    public double x;
    public double minY;
    public double z;
    public double y;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean hasPos;
    public boolean hasAngles;

    public PlayerMovePacket() {
    }

    @Environment(value=EnvType.CLIENT)
    public PlayerMovePacket(boolean onGround) {
        this.onGround = onGround;
    }

    public void handle(PacketHandler handler) {
        handler.handlePlayerMove(this);
    }

    public void read(DataInputStream input) {
        this.onGround = input.read() != 0;
    }

    public void write(DataOutputStream output) {
        output.write(this.onGround ? 1 : 0);
    }

    public int getSize() {
        return 1;
    }

    public static class PositionAndAngles
    extends PlayerMovePacket {
        public PositionAndAngles() {
            this.hasAngles = true;
            this.hasPos = true;
        }

        public PositionAndAngles(double x, double minY, double y, double z, float yaw, float pitch, boolean onGround) {
            this.x = x;
            this.minY = minY;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.hasAngles = true;
            this.hasPos = true;
        }

        public void read(DataInputStream input) {
            this.x = input.readDouble();
            this.minY = input.readDouble();
            this.y = input.readDouble();
            this.z = input.readDouble();
            this.yaw = input.readFloat();
            this.pitch = input.readFloat();
            super.read(input);
        }

        public void write(DataOutputStream output) {
            output.writeDouble(this.x);
            output.writeDouble(this.minY);
            output.writeDouble(this.y);
            output.writeDouble(this.z);
            output.writeFloat(this.yaw);
            output.writeFloat(this.pitch);
            super.write(output);
        }

        public int getSize() {
            return 41;
        }
    }

    public static class Angles
    extends PlayerMovePacket {
        public Angles() {
            this.hasAngles = true;
        }

        @Environment(value=EnvType.CLIENT)
        public Angles(float yaw, float pitch, boolean onGround) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
            this.hasAngles = true;
        }

        public void read(DataInputStream input) {
            this.yaw = input.readFloat();
            this.pitch = input.readFloat();
            super.read(input);
        }

        public void write(DataOutputStream output) {
            output.writeFloat(this.yaw);
            output.writeFloat(this.pitch);
            super.write(output);
        }

        public int getSize() {
            return 9;
        }
    }

    public static class Position
    extends PlayerMovePacket {
        public Position() {
            this.hasPos = true;
        }

        @Environment(value=EnvType.CLIENT)
        public Position(double x, double minY, double y, double z, boolean onGround) {
            this.x = x;
            this.minY = minY;
            this.y = y;
            this.z = z;
            this.onGround = onGround;
            this.hasPos = true;
        }

        public void read(DataInputStream input) {
            this.x = input.readDouble();
            this.minY = input.readDouble();
            this.y = input.readDouble();
            this.z = input.readDouble();
            super.read(input);
        }

        public void write(DataOutputStream output) {
            output.writeDouble(this.x);
            output.writeDouble(this.minY);
            output.writeDouble(this.y);
            output.writeDouble(this.z);
            super.write(output);
        }

        public int getSize() {
            return 33;
        }
    }
}

