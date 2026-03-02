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

public class NbtFloat
extends NbtElement {
    public float value;

    public NbtFloat() {
    }

    public NbtFloat(float value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeFloat(this.value);
    }

    void read(DataInput input) {
        this.value = input.readFloat();
    }

    public byte getType() {
        return NbtTypes.FLOAT;
    }

    public String toString() {
        return "" + this.value;
    }
}

