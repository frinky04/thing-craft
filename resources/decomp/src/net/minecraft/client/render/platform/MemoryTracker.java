/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.platform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class MemoryTracker {
    private static List LIST_RANGES = new ArrayList();
    private static List LISTS = new ArrayList();

    public static synchronized int getLists(int s) {
        int i = GL11.glGenLists((int)s);
        LIST_RANGES.add(i);
        LIST_RANGES.add(s);
        return i;
    }

    public static synchronized void genTextures(IntBuffer lists) {
        GL11.glGenTextures((IntBuffer)lists);
        for (int i = lists.position(); i < lists.limit(); ++i) {
            LISTS.add(lists.get(i));
        }
    }

    public static synchronized void releaseLists() {
        for (int i = 0; i < LIST_RANGES.size(); i += 2) {
            GL11.glDeleteLists((int)((Integer)LIST_RANGES.get(i)), (int)((Integer)LIST_RANGES.get(i + 1)));
        }
        IntBuffer intBuffer = MemoryTracker.createIntBuffer(LISTS.size());
        intBuffer.flip();
        GL11.glDeleteTextures((IntBuffer)intBuffer);
        for (int j = 0; j < LISTS.size(); ++j) {
            intBuffer.put((Integer)LISTS.get(j));
        }
        intBuffer.flip();
        GL11.glDeleteTextures((IntBuffer)intBuffer);
        LIST_RANGES.clear();
        LISTS.clear();
    }

    public static synchronized ByteBuffer createByteBuffer(int capacity) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        return byteBuffer;
    }

    public static IntBuffer createIntBuffer(int capacity) {
        return MemoryTracker.createByteBuffer(capacity << 2).asIntBuffer();
    }

    public static FloatBuffer createFloatBuffer(int capacity) {
        return MemoryTracker.createByteBuffer(capacity << 2).asFloatBuffer();
    }
}

