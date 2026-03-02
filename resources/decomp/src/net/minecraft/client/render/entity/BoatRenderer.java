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
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.entity.BoatModel;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class BoatRenderer
extends EntityRenderer {
    protected Model model;

    public BoatRenderer() {
        this.shadowSize = 0.5f;
        this.model = new BoatModel();
    }

    public void m_43456420(BoatEntity boatEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        GL11.glRotatef((float)(180.0f - g), (float)0.0f, (float)1.0f, (float)0.0f);
        float i = (float)boatEntity.damagedTimer - h;
        float j = (float)boatEntity.damage - h;
        if (j < 0.0f) {
            j = 0.0f;
        }
        if (i > 0.0f) {
            GL11.glRotatef((float)(MathHelper.sin(i) * i * j / 10.0f * (float)boatEntity.damagedSwingDir), (float)1.0f, (float)0.0f, (float)0.0f);
        }
        this.bindTexture("/terrain.png");
        float k = 0.75f;
        GL11.glScalef((float)k, (float)k, (float)k);
        GL11.glScalef((float)(1.0f / k), (float)(1.0f / k), (float)(1.0f / k));
        this.bindTexture("/item/boat.png");
        GL11.glScalef((float)-1.0f, (float)-1.0f, (float)1.0f);
        this.model.render(0.0f, 0.0f, -0.1f, 0.0f, 0.0f, 0.0625f);
        GL11.glPopMatrix();
    }
}

