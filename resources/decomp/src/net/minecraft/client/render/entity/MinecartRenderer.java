/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.entity.MinecartModel;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class MinecartRenderer
extends EntityRenderer {
    protected Model model;

    public MinecartRenderer() {
        this.shadowSize = 0.5f;
        this.model = new MinecartModel();
    }

    public void m_72630527(MinecartEntity minecartEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        double i = minecartEntity.prevX + (minecartEntity.x - minecartEntity.prevX) * (double)h;
        double j = minecartEntity.prevY + (minecartEntity.y - minecartEntity.prevY) * (double)h;
        double k = minecartEntity.prevZ + (minecartEntity.z - minecartEntity.prevZ) * (double)h;
        double l = 0.3f;
        Vec3d vec3d = minecartEntity.snapPositionToRail(i, j, k);
        float m = minecartEntity.lastPitch + (minecartEntity.pitch - minecartEntity.lastPitch) * h;
        if (vec3d != null) {
            Vec3d vec3d2 = minecartEntity.snapPositionToRailWithOffset(i, j, k, l);
            Vec3d vec3d3 = minecartEntity.snapPositionToRailWithOffset(i, j, k, -l);
            if (vec3d2 == null) {
                vec3d2 = vec3d;
            }
            if (vec3d3 == null) {
                vec3d3 = vec3d;
            }
            d += vec3d.x - i;
            e += (vec3d2.y + vec3d3.y) / 2.0 - j;
            f += vec3d.z - k;
            Vec3d vec3d4 = vec3d3.add(-vec3d2.x, -vec3d2.y, -vec3d2.z);
            if (vec3d4.length() != 0.0) {
                vec3d4 = vec3d4.normalize();
                g = (float)(Math.atan2(vec3d4.z, vec3d4.x) * 180.0 / Math.PI);
                m = (float)(Math.atan(vec3d4.y) * 73.0);
            }
        }
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        GL11.glRotatef((float)(180.0f - g), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)(-m), (float)0.0f, (float)0.0f, (float)1.0f);
        float n = (float)minecartEntity.damagedTimer - h;
        float o = (float)minecartEntity.damage - h;
        if (o < 0.0f) {
            o = 0.0f;
        }
        if (n > 0.0f) {
            GL11.glRotatef((float)(MathHelper.sin(n) * n * o / 10.0f * (float)minecartEntity.damagedSwingDir), (float)1.0f, (float)0.0f, (float)0.0f);
        }
        if (minecartEntity.type != 0) {
            this.bindTexture("/terrain.png");
            float p = 0.75f;
            GL11.glScalef((float)p, (float)p, (float)p);
            GL11.glTranslatef((float)0.0f, (float)0.3125f, (float)0.0f);
            GL11.glRotatef((float)90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            if (minecartEntity.type == 1) {
                new BlockRenderer().renderAsItem(Block.CHEST);
            } else if (minecartEntity.type == 2) {
                new BlockRenderer().renderAsItem(Block.FURNACE);
            }
            GL11.glRotatef((float)-90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glTranslatef((float)0.0f, (float)-0.3125f, (float)0.0f);
            GL11.glScalef((float)(1.0f / p), (float)(1.0f / p), (float)(1.0f / p));
        }
        this.bindTexture("/item/cart.png");
        GL11.glScalef((float)-1.0f, (float)-1.0f, (float)1.0f);
        this.model.render(0.0f, 0.0f, -0.1f, 0.0f, 0.0f, 0.0625f);
        GL11.glPopMatrix();
    }
}

