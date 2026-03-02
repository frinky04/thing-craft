/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.Polygon;
import net.minecraft.client.render.model.Vertex;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class ModelPart {
    private Vertex[] vertices;
    private Polygon[] polygons;
    private int u;
    private int v;
    public float x;
    public float y;
    public float z;
    public float rotationX;
    public float rotationY;
    public float rotationZ;
    private boolean compiled = false;
    private int glList = 0;
    public boolean flipped = false;
    public boolean visible = true;
    public boolean invisible = false;

    public ModelPart(int u, int v) {
        this.u = u;
        this.v = v;
    }

    public void addBox(float x, float y, float z, int sizeX, int sizeY, int sizeZ) {
        this.addBox(x, y, z, sizeX, sizeY, sizeZ, 0.0f);
    }

    public void addBox(float x, float y, float z, int sizeX, int sizeY, int sizeZ, float increase) {
        this.vertices = new Vertex[8];
        this.polygons = new Polygon[6];
        float f = x + (float)sizeX;
        float g = y + (float)sizeY;
        float h = z + (float)sizeZ;
        x -= increase;
        y -= increase;
        z -= increase;
        f += increase;
        g += increase;
        h += increase;
        if (this.flipped) {
            float i = f;
            f = x;
            x = i;
        }
        Vertex vertex = new Vertex(x, y, z, 0.0f, 0.0f);
        Vertex vertex2 = new Vertex(f, y, z, 0.0f, 8.0f);
        Vertex vertex3 = new Vertex(f, g, z, 8.0f, 8.0f);
        Vertex vertex4 = new Vertex(x, g, z, 8.0f, 0.0f);
        Vertex vertex5 = new Vertex(x, y, h, 0.0f, 0.0f);
        Vertex vertex6 = new Vertex(f, y, h, 0.0f, 8.0f);
        Vertex vertex7 = new Vertex(f, g, h, 8.0f, 8.0f);
        Vertex vertex8 = new Vertex(x, g, h, 8.0f, 0.0f);
        this.vertices[0] = vertex;
        this.vertices[1] = vertex2;
        this.vertices[2] = vertex3;
        this.vertices[3] = vertex4;
        this.vertices[4] = vertex5;
        this.vertices[5] = vertex6;
        this.vertices[6] = vertex7;
        this.vertices[7] = vertex8;
        this.polygons[0] = new Polygon(new Vertex[]{vertex6, vertex2, vertex3, vertex7}, this.u + sizeZ + sizeX, this.v + sizeZ, this.u + sizeZ + sizeX + sizeZ, this.v + sizeZ + sizeY);
        this.polygons[1] = new Polygon(new Vertex[]{vertex, vertex5, vertex8, vertex4}, this.u + 0, this.v + sizeZ, this.u + sizeZ, this.v + sizeZ + sizeY);
        this.polygons[2] = new Polygon(new Vertex[]{vertex6, vertex5, vertex, vertex2}, this.u + sizeZ, this.v + 0, this.u + sizeZ + sizeX, this.v + sizeZ);
        this.polygons[3] = new Polygon(new Vertex[]{vertex3, vertex4, vertex8, vertex7}, this.u + sizeZ + sizeX, this.v + 0, this.u + sizeZ + sizeX + sizeX, this.v + sizeZ);
        this.polygons[4] = new Polygon(new Vertex[]{vertex2, vertex, vertex4, vertex3}, this.u + sizeZ, this.v + sizeZ, this.u + sizeZ + sizeX, this.v + sizeZ + sizeY);
        this.polygons[5] = new Polygon(new Vertex[]{vertex5, vertex6, vertex7, vertex8}, this.u + sizeZ + sizeX + sizeZ, this.v + sizeZ, this.u + sizeZ + sizeX + sizeZ + sizeX, this.v + sizeZ + sizeY);
        if (this.flipped) {
            for (int j = 0; j < this.polygons.length; ++j) {
                this.polygons[j].flip();
            }
        }
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void render(float scale) {
        if (this.invisible) {
            return;
        }
        if (!this.visible) {
            return;
        }
        if (!this.compiled) {
            this.compile(scale);
        }
        if (this.rotationX != 0.0f || this.rotationY != 0.0f || this.rotationZ != 0.0f) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(this.x * scale), (float)(this.y * scale), (float)(this.z * scale));
            if (this.rotationZ != 0.0f) {
                GL11.glRotatef((float)(this.rotationZ * 57.295776f), (float)0.0f, (float)0.0f, (float)1.0f);
            }
            if (this.rotationY != 0.0f) {
                GL11.glRotatef((float)(this.rotationY * 57.295776f), (float)0.0f, (float)1.0f, (float)0.0f);
            }
            if (this.rotationX != 0.0f) {
                GL11.glRotatef((float)(this.rotationX * 57.295776f), (float)1.0f, (float)0.0f, (float)0.0f);
            }
            GL11.glCallList((int)this.glList);
            GL11.glPopMatrix();
        } else if (this.x != 0.0f || this.y != 0.0f || this.z != 0.0f) {
            GL11.glTranslatef((float)(this.x * scale), (float)(this.y * scale), (float)(this.z * scale));
            GL11.glCallList((int)this.glList);
            GL11.glTranslatef((float)(-this.x * scale), (float)(-this.y * scale), (float)(-this.z * scale));
        } else {
            GL11.glCallList((int)this.glList);
        }
    }

    public void transform(float scale) {
        if (this.invisible) {
            return;
        }
        if (!this.visible) {
            return;
        }
        if (!this.compiled) {
            this.compile(scale);
        }
        if (this.rotationX != 0.0f || this.rotationY != 0.0f || this.rotationZ != 0.0f) {
            GL11.glTranslatef((float)(this.x * scale), (float)(this.y * scale), (float)(this.z * scale));
            if (this.rotationZ != 0.0f) {
                GL11.glRotatef((float)(this.rotationZ * 57.295776f), (float)0.0f, (float)0.0f, (float)1.0f);
            }
            if (this.rotationY != 0.0f) {
                GL11.glRotatef((float)(this.rotationY * 57.295776f), (float)0.0f, (float)1.0f, (float)0.0f);
            }
            if (this.rotationX != 0.0f) {
                GL11.glRotatef((float)(this.rotationX * 57.295776f), (float)1.0f, (float)0.0f, (float)0.0f);
            }
        } else if (this.x != 0.0f || this.y != 0.0f || this.z != 0.0f) {
            GL11.glTranslatef((float)(this.x * scale), (float)(this.y * scale), (float)(this.z * scale));
        }
    }

    private void compile(float scale) {
        this.glList = MemoryTracker.getLists(1);
        GL11.glNewList((int)this.glList, (int)GL11.GL_COMPILE);
        Tesselator tesselator = Tesselator.INSTANCE;
        for (int i = 0; i < this.polygons.length; ++i) {
            this.polygons[i].compile(tesselator, scale);
        }
        GL11.glEndList();
        this.compiled = true;
    }
}

