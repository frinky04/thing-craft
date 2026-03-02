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
import net.minecraft.client.render.model.entity.GhastModel;
import net.minecraft.entity.mob.monster.GhastEntity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class GhastRenderer
extends MobRenderer {
    public GhastRenderer() {
        super(new GhastModel(), 0.5f);
    }

    protected void m_24379219(GhastEntity ghastEntity, float f) {
        GhastEntity ghastEntity2 = ghastEntity;
        float g = ((float)ghastEntity2.lastChargeTicks + (float)(ghastEntity2.chargeTicks - ghastEntity2.lastChargeTicks) * f) / 20.0f;
        if (g < 0.0f) {
            g = 0.0f;
        }
        g = 1.0f / (g * g * g * g * g * 2.0f + 1.0f);
        float h = (8.0f + g) / 2.0f;
        float i = (8.0f + 1.0f / g) / 2.0f;
        GL11.glScalef((float)i, (float)h, (float)i);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }
}

