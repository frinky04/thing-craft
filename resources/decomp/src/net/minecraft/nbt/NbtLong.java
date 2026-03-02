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

public class NbtLong
extends NbtElement {
    public long value;

    public NbtLong() {
    }

    public NbtLong(long value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeLong(this.value);
    }

    void read(DataInput input) {
        this.value = input.readLong();
    }

    public byte getType() {
        return NbtTypes.LONG;
    }

    public String toString() {
        return "" + this.value;
    }
}

