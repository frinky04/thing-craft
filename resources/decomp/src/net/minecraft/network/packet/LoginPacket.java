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

public class LoginPacket
extends Packet {
    public int gameVersion;
    public String username;
    public String password;
    public long seed;
    public byte dimension;

    public LoginPacket() {
    }

    @Environment(value=EnvType.CLIENT)
    public LoginPacket(String username, String password, int gameVersion) {
        this.username = username;
        this.password = password;
        this.gameVersion = gameVersion;
    }

    @Environment(value=EnvType.SERVER)
    public LoginPacket(String username, String password, int id, long seed, byte dimensionId) {
        this.username = username;
        this.password = password;
        this.gameVersion = id;
        this.seed = seed;
        this.dimension = dimensionId;
    }

    public void read(DataInputStream input) {
        this.gameVersion = input.readInt();
        this.username = input.readUTF();
        this.password = input.readUTF();
        this.seed = input.readLong();
        this.dimension = input.readByte();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.gameVersion);
        output.writeUTF(this.username);
        output.writeUTF(this.password);
        output.writeLong(this.seed);
        output.writeByte(this.dimension);
    }

    public void handle(PacketHandler handler) {
        handler.handleLogin(this);
    }

    public int getSize() {
        return 4 + this.username.length() + this.password.length() + 4 + 5;
    }
}

