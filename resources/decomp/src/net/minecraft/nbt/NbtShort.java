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

public class NbtShort
extends NbtElement {
    public short value;

    public NbtShort() {
    }

    public NbtShort(short value) {
        this.value = value;
    }

    void write(DataOutput output) {
        output.writeShort(this.value);
    }

    void read(DataInput input) {
        this.value = input.readShort();
    }

    public byte getType() {
        return NbtTypes.SHORT;
    }

    public String toString() {
        return "" + this.value;
    }
}

