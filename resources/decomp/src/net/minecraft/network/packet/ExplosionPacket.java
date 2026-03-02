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
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;

public class ExplosionPacket
extends Packet {
    public double x;
    public double y;
    public double z;
    public float power;
    public Set damagedBlocks;

    public ExplosionPacket() {
    }

    @Environment(value=EnvType.SERVER)
    public ExplosionPacket(double x, double y, double z, float power, Set damagedBlocks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.damagedBlocks = new HashSet(damagedBlocks);
    }

    public void read(DataInputStream input) {
        this.x = input.readDouble();
        this.y = input.readDouble();
        this.z = input.readDouble();
        this.power = input.readFloat();
        int i = input.readInt();
        this.damagedBlocks = new HashSet();
        int j = (int)this.x;
        int k = (int)this.y;
        int l = (int)this.z;
        for (int m = 0; m < i; ++m) {
            int n = input.readByte() + j;
            int o = input.readByte() + k;
            int p = input.readByte() + l;
            this.damagedBlocks.add(new BlockPos(n, o, p));
        }
    }

    public void write(DataOutputStream output) {
        output.writeDouble(this.x);
        output.writeDouble(this.y);
        output.writeDouble(this.z);
        output.writeFloat(this.power);
        output.writeInt(this.damagedBlocks.size());
        int i = (int)this.x;
        int j = (int)this.y;
        int k = (int)this.z;
        for (BlockPos blockPos : this.damagedBlocks) {
            int l = blockPos.x - i;
            int m = blockPos.y - j;
            int n = blockPos.z - k;
            output.writeByte(l);
            output.writeByte(m);
            output.writeByte(n);
        }
    }

    public void handle(PacketHandler handler) {
        handler.handleExplosion(this);
    }

    public int getSize() {
        return 32 + this.damagedBlocks.size() * 3;
    }
}

