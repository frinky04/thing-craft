/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.LWJGLException
 *  org.lwjgl.input.Cursor
 *  org.lwjgl.input.Mouse
 */
package net.minecraft.client;

import java.awt.Component;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.platform.MemoryTracker;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;

@Environment(value=EnvType.CLIENT)
public class Mouse {
    private Component component;
    private Cursor cursor;
    public int x;
    public int y;
    private int f_87086150 = 10;

    public Mouse(Component component) {
        this.component = component;
        IntBuffer intBuffer = MemoryTracker.createIntBuffer(1);
        intBuffer.put(0);
        intBuffer.flip();
        IntBuffer intBuffer2 = MemoryTracker.createIntBuffer(1024);
        try {
            this.cursor = new Cursor(32, 32, 16, 16, 1, intBuffer2, intBuffer);
        }
        catch (LWJGLException lWJGLException) {
            lWJGLException.printStackTrace();
        }
    }

    public void lock() {
        org.lwjgl.input.Mouse.setGrabbed((boolean)true);
        this.x = 0;
        this.y = 0;
    }

    public void unlock() {
        org.lwjgl.input.Mouse.setCursorPosition((int)(this.component.getWidth() / 2), (int)(this.component.getHeight() / 2));
        org.lwjgl.input.Mouse.setGrabbed((boolean)false);
    }

    public void tick() {
        this.x = org.lwjgl.input.Mouse.getDX();
        this.y = org.lwjgl.input.Mouse.getDY();
    }
}

