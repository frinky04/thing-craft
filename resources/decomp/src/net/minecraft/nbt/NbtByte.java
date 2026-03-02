/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.ornithemc.feather.constants.NbtTypes
 */
package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import net.minecraft.nbt.NbtElement;
import net.ornithemc.feather.constants.NbtTypes;

public class NbtByte
extends NbtElement {
    public byte value;

    public NbtByte() {
    }

    public NbtByte(byte value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeByte(this.value);
    }

    void read(DataInput input) {
        this.value = input.readByte();
    }

    public byte getType() {
        return NbtTypes.BYTE;
    }

    public String toString() {
        return "" + this.value;
    }
}

