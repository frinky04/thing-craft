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

public class NbtInt
extends NbtElement {
    public int value;

    public NbtInt() {
    }

    public NbtInt(int value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeInt(this.value);
    }

    void read(DataInput input) {
        this.value = input.readInt();
    }

    public byte getType() {
        return NbtTypes.INT;
    }

    public String toString() {
        return "" + this.value;
    }
}

