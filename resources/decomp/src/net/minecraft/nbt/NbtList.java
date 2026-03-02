/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.ornithemc.feather.constants.NbtTypes
 */
package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtElement;
import net.ornithemc.feather.constants.NbtTypes;

public class NbtList
extends NbtElement {
    private List elements = new ArrayList();
    private byte type;

    void write(DataOutput output) {
        this.type = this.elements.size() > 0 ? ((NbtElement)this.elements.get(0)).getType() : (byte)1;
        output.writeByte(this.type);
        output.writeInt(this.elements.size());
        for (int i = 0; i < this.elements.size(); ++i) {
            ((NbtElement)this.elements.get(i)).write(output);
        }
    }

    void read(DataInput input) {
        this.type = input.readByte();
        int i = input.readInt();
        this.elements = new ArrayList();
        for (int j = 0; j < i; ++j) {
            NbtElement nbtElement = NbtElement.create(this.type);
            nbtElement.read(input);
            this.elements.add(nbtElement);
        }
    }

    public byte getType() {
        return NbtTypes.LIST;
    }

    public String toString() {
        return "" + this.elements.size() + " entries of type " + NbtElement.getName(this.type);
    }

    public void addElement(NbtElement element) {
        this.type = element.getType();
        this.elements.add(element);
    }

    public NbtElement get(int index) {
        return (NbtElement)this.elements.get(index);
    }

    public int size() {
        return this.elements.size();
    }
}

