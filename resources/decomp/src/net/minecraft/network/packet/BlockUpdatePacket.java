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
import net.minecraft.world.World;

public class BlockUpdatePacket
extends Packet {
    public int x;
    public int y;
    public int z;
    public int block;
    public int metadata;

    public BlockUpdatePacket() {
        this.shouldDelay = true;
    }

    @Environment(value=EnvType.SERVER)
    public BlockUpdatePacket(int x, int y, int z, World world) {
        this.shouldDelay = true;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = world.getBlock(x, y, z);
        this.metadata = world.getBlockMetadata(x, y, z);
    }

    public void read(DataInputStream input) {
        this.x = input.readInt();
        this.y = input.read();
        this.z = input.readInt();
        this.block = input.read();
        this.metadata = input.read();
    }

    public void write(DataOutputStream output) {
        output.writeInt(this.x);
        output.write(this.y);
        output.writeInt(this.z);
        output.write(this.block);
        output.write(this.metadata);
    }

    public void handle(PacketHandler handler) {
        handler.handleBlockUpdate(this);
    }

    public int getSize() {
        return 11;
    }
}

