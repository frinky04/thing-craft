/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.platform;

import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class Lighting {
    private static FloatBuffer BUFFER = MemoryTracker.createFloatBuffer(16);

    public static void turnOff() {
        GL11.glDisable((int)GL11.GL_LIGHTING);
        GL11.glDisable((int)GL11.GL_LIGHT0);
        GL11.glDisable((int)GL11.GL_LIGHT1);
        GL11.glDisable((int)GL11.GL_COLOR_MATERIAL);
    }

    public static void turnOn() {
        GL11.glEnable((int)GL11.GL_LIGHTING);
        GL11.glEnable((int)GL11.GL_LIGHT0);
        GL11.glEnable((int)GL11.GL_LIGHT1);
        GL11.glEnable((int)GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial((int)GL11.GL_FRONT_AND_BACK, (int)GL11.GL_AMBIENT_AND_DIFFUSE);
        float f = 0.4f;
        float g = 0.6f;
        float h = 0.0f;
        Vec3d vec3d = Vec3d.fromPool(0.2f, 1.0, -0.7f).normalize();
        GL11.glLight((int)GL11.GL_LIGHT0, (int)GL11.GL_POSITION, (FloatBuffer)Lighting.getBuffer(vec3d.x, vec3d.y, vec3d.z, 0.0));
        GL11.glLight((int)GL11.GL_LIGHT0, (int)GL11.GL_DIFFUSE, (FloatBuffer)Lighting.getBuffer(g, g, g, 1.0f));
        GL11.glLight((int)GL11.GL_LIGHT0, (int)GL11.GL_AMBIENT, (FloatBuffer)Lighting.getBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        GL11.glLight((int)GL11.GL_LIGHT0, (int)GL11.GL_SPECULAR, (FloatBuffer)Lighting.getBuffer(h, h, h, 1.0f));
        vec3d = Vec3d.fromPool(-0.2f, 1.0, 0.7f).normalize();
        GL11.glLight((int)GL11.GL_LIGHT1, (int)GL11.GL_POSITION, (FloatBuffer)Lighting.getBuffer(vec3d.x, vec3d.y, vec3d.z, 0.0));
        GL11.glLight((int)GL11.GL_LIGHT1, (int)GL11.GL_DIFFUSE, (FloatBuffer)Lighting.getBuffer(g, g, g, 1.0f));
        GL11.glLight((int)GL11.GL_LIGHT1, (int)GL11.GL_AMBIENT, (FloatBuffer)Lighting.getBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        GL11.glLight((int)GL11.GL_LIGHT1, (int)GL11.GL_SPECULAR, (FloatBuffer)Lighting.getBuffer(h, h, h, 1.0f));
        GL11.glShadeModel((int)GL11.GL_FLAT);
        GL11.glLightModel((int)GL11.GL_LIGHT_MODEL_AMBIENT, (FloatBuffer)Lighting.getBuffer(f, f, f, 1.0f));
    }

    private static FloatBuffer getBuffer(double r, double g, double b, double a) {
        return Lighting.getBuffer((float)r, (float)g, (float)b, (float)a);
    }

    private static FloatBuffer getBuffer(float r, float g, float b, float a) {
        BUFFER.clear();
        BUFFER.put(r).put(g).put(b).put(a);
        BUFFER.flip();
        return BUFFER;
    }
}

