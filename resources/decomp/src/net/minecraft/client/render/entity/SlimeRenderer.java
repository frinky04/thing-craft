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
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.Model;
import net.minecraft.entity.mob.monster.SlimeEntity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class SlimeRenderer
extends MobRenderer {
    private Model innerModel;

    public SlimeRenderer(Model model, Model innerModel, float shadowSize) {
        super(model, shadowSize);
        this.innerModel = innerModel;
    }

    protected boolean m_15978682(SlimeEntity slimeEntity, int i) {
        if (i == 0) {
            this.setDecorationModel(this.innerModel);
            GL11.glEnable((int)2977);
            GL11.glEnable((int)GL11.GL_BLEND);
            GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
            return true;
        }
        if (i == 1) {
            GL11.glDisable((int)GL11.GL_BLEND);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        }
        return false;
    }

    protected void m_45002819(SlimeEntity slimeEntity, float f) {
        float g = (slimeEntity.lastStretch + (slimeEntity.stretch - slimeEntity.lastStretch) * f) / ((float)slimeEntity.size * 0.5f + 1.0f);
        float h = 1.0f / (g + 1.0f);
        float i = slimeEntity.size;
        GL11.glScalef((float)(h * i), (float)(1.0f / h * i), (float)(h * i));
    }
}

