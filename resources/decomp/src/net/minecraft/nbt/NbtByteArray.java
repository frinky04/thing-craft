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

public class NbtByteArray
extends NbtElement {
    public byte[] value;

    public NbtByteArray() {
    }

    public NbtByteArray(byte[] value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeInt(this.value.length);
        output.write(this.value);
    }

    void read(DataInput input) {
        int i = input.readInt();
        this.value = new byte[i];
        input.readFully(this.value);
    }

    public byte getType() {
        return NbtTypes.BYTE_ARRAY;
    }

    public String toString() {
        return "[" + this.value.length + " bytes]";
    }
}

