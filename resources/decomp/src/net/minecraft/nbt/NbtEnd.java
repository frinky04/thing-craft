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

public class NbtEnd
extends NbtElement {
    void read(DataInput input) {
    }

    void write(DataOutput output) {
    }

    public byte getType() {
        return NbtTypes.END;
    }

    public String toString() {
        return "END";
    }
}

