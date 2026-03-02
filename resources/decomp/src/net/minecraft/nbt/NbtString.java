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

public class NbtString
extends NbtElement {
    public String value;

    public NbtString() {
    }

    public NbtString(String value) {
        this.value = value;
        if (value == null) {
            throw new IllegalArgumentException("Empty string not allowed");
        }
    }

    void write(DataOutput output) {
        output.writeUTF(this.value);
    }

    void read(DataInput input) {
        this.value = input.readUTF();
    }

    public byte getType() {
        return NbtTypes.STRING;
    }

    public String toString() {
        return "" + this.value;
    }
}

