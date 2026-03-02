/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public class NbtIo {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static NbtCompound readCompressed(InputStream is) {
        DataInputStream dataInputStream = new DataInputStream(new GZIPInputStream(is));
        try {
            NbtCompound nbtCompound = NbtIo.read(dataInputStream);
            return nbtCompound;
        }
        finally {
            dataInputStream.close();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void writeCompressed(NbtCompound nbt, OutputStream os) {
        DataOutputStream dataOutputStream = new DataOutputStream(new GZIPOutputStream(os));
        try {
            NbtIo.write(nbt, dataOutputStream);
        }
        finally {
            dataOutputStream.close();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static NbtCompound readCompressed(byte[] data) {
        DataInputStream dataInputStream = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)));
        try {
            NbtCompound nbtCompound = NbtIo.read(dataInputStream);
            return nbtCompound;
        }
        finally {
            dataInputStream.close();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static byte[] writeCompressed(NbtCompound nbt) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(new GZIPOutputStream(byteArrayOutputStream));
        try {
            NbtIo.write(nbt, dataOutputStream);
        }
        finally {
            dataOutputStream.close();
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static NbtCompound read(DataInput input) {
        NbtElement nbtElement = NbtElement.deserialize(input);
        if (nbtElement instanceof NbtCompound) {
            return (NbtCompound)nbtElement;
        }
        throw new IOException("Root tag must be a named compound tag");
    }

    public static void write(NbtCompound nbt, DataOutput output) {
        NbtElement.serialize(nbt, output);
    }
}

