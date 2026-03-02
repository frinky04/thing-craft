/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.Vertex;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.util.math.Vec3d;

@Environment(value=EnvType.CLIENT)
public class Polygon {
    public Vertex[] vertices;
    public int vertexCount = 0;
    private boolean flipNormal = false;

    public Polygon(Vertex[] vertices) {
        this.vertices = vertices;
        this.vertexCount = vertices.length;
    }

    public Polygon(Vertex[] vertices, int u1, int v1, int u2, int v2) {
        this(vertices);
        float f = 0.0015625f;
        float g = 0.003125f;
        vertices[0] = vertices[0].remap((float)u2 / 64.0f - f, (float)v1 / 32.0f + g);
        vertices[1] = vertices[1].remap((float)u1 / 64.0f + f, (float)v1 / 32.0f + g);
        vertices[2] = vertices[2].remap((float)u1 / 64.0f + f, (float)v2 / 32.0f - g);
        vertices[3] = vertices[3].remap((float)u2 / 64.0f - f, (float)v2 / 32.0f - g);
    }

    public void flip() {
        Vertex[] vertexs = new Vertex[this.vertices.length];
        for (int i = 0; i < this.vertices.length; ++i) {
            vertexs[i] = this.vertices[this.vertices.length - i - 1];
        }
        this.vertices = vertexs;
    }

    public void compile(Tesselator tesselator, float scale) {
        Vec3d vec3d = this.vertices[1].pos.subtractFrom(this.vertices[0].pos);
        Vec3d vec3d2 = this.vertices[1].pos.subtractFrom(this.vertices[2].pos);
        Vec3d vec3d3 = vec3d2.cross(vec3d).normalize();
        tesselator.begin();
        if (this.flipNormal) {
            tesselator.normal(-((float)vec3d3.x), -((float)vec3d3.y), -((float)vec3d3.z));
        } else {
            tesselator.normal((float)vec3d3.x, (float)vec3d3.y, (float)vec3d3.z);
        }
        for (int i = 0; i < 4; ++i) {
            Vertex vertex = this.vertices[i];
            tesselator.vertex((float)vertex.pos.x * scale, (float)vertex.pos.y * scale, (float)vertex.pos.z * scale, vertex.u, vertex.v);
        }
        tesselator.end();
    }
}

