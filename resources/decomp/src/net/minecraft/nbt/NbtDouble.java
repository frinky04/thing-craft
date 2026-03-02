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

public class NbtDouble
extends NbtElement {
    public double value;

    public NbtDouble() {
    }

    public NbtDouble(double value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeDouble(this.value);
    }

    void read(DataInput input) {
        this.value = input.readDouble();
    }

    public byte getType() {
        return NbtTypes.DOUBLE;
    }

    public String toString() {
        return "" + this.value;
    }
}

