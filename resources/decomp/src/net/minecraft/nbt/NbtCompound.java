/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.ornithemc.feather.constants.NbtTypes
 */
package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.ornithemc.feather.constants.NbtTypes;

public class NbtCompound
extends NbtElement {
    private Map elements = new HashMap();

    void write(DataOutput output) {
        for (NbtElement nbtElement : this.elements.values()) {
            NbtElement.serialize(nbtElement, output);
        }
        output.writeByte(0);
    }

    /*
     * WARNING - void declaration
     */
    void read(DataInput input) {
        NbtElement nbtElement;
        this.elements.clear();
        while ((nbtElement = NbtElement.deserialize(input)).getType() != 0) {
            void nbtElement2;
            this.elements.put(nbtElement2.getName(), nbtElement2);
        }
    }

    public byte getType() {
        return NbtTypes.COMPOUND;
    }

    public void put(String key, NbtElement element) {
        this.elements.put(key, element.setName(key));
    }

    public void putByte(String key, byte value) {
        this.elements.put(key, new NbtByte(value).setName(key));
    }

    public void putShort(String key, short value) {
        this.elements.put(key, new NbtShort(value).setName(key));
    }

    public void putInt(String key, int value) {
        this.elements.put(key, new NbtInt(value).setName(key));
    }

    public void putLong(String key, long value) {
        this.elements.put(key, new NbtLong(value).setName(key));
    }

    public void putFloat(String key, float value) {
        this.elements.put(key, new NbtFloat(value).setName(key));
    }

    public void putDouble(String key, double value) {
        this.elements.put(key, new NbtDouble(value).setName(key));
    }

    public void putString(String key, String value) {
        this.elements.put(key, new NbtString(value).setName(key));
    }

    public void putByteArray(String key, byte[] value) {
        this.elements.put(key, new NbtByteArray(value).setName(key));
    }

    public void putCompound(String key, NbtCompound nbt) {
        this.elements.put(key, nbt.setName(key));
    }

    public void putBoolean(String key, boolean value) {
        this.putByte(key, value ? (byte)1 : 0);
    }

    public boolean contains(String key) {
        return this.elements.containsKey(key);
    }

    public byte getByte(String key) {
        if (!this.elements.containsKey(key)) {
            return 0;
        }
        return ((NbtByte)this.elements.get((Object)key)).value;
    }

    public short getShort(String key) {
        if (!this.elements.containsKey(key)) {
            return 0;
        }
        return ((NbtShort)this.elements.get((Object)key)).value;
    }

    public int getInt(String key) {
        if (!this.elements.containsKey(key)) {
            return 0;
        }
        return ((NbtInt)this.elements.get((Object)key)).value;
    }

    public long getLong(String key) {
        if (!this.elements.containsKey(key)) {
            return 0L;
        }
        return ((NbtLong)this.elements.get((Object)key)).value;
    }

    public float getFloat(String key) {
        if (!this.elements.containsKey(key)) {
            return 0.0f;
        }
        return ((NbtFloat)this.elements.get((Object)key)).value;
    }

    public double getDouble(String key) {
        if (!this.elements.containsKey(key)) {
            return 0.0;
        }
        return ((NbtDouble)this.elements.get((Object)key)).value;
    }

    public String getString(String key) {
        if (!this.elements.containsKey(key)) {
            return "";
        }
        return ((NbtString)this.elements.get((Object)key)).value;
    }

    public byte[] getByteArray(String key) {
        if (!this.elements.containsKey(key)) {
            return new byte[0];
        }
        return ((NbtByteArray)this.elements.get((Object)key)).value;
    }

    public NbtCompound getCompound(String key) {
        if (!this.elements.containsKey(key)) {
            return new NbtCompound();
        }
        return (NbtCompound)this.elements.get(key);
    }

    public NbtList getList(String key) {
        if (!this.elements.containsKey(key)) {
            return new NbtList();
        }
        return (NbtList)this.elements.get(key);
    }

    public boolean getBoolean(String key) {
        return this.getByte(key) != 0;
    }

    public String toString() {
        return "" + this.elements.size() + " entries";
    }
}

