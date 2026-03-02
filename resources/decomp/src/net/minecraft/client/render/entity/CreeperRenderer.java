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
import net.minecraft.client.render.model.entity.CreeperModel;
import net.minecraft.entity.mob.monster.CreeperEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class CreeperRenderer
extends MobRenderer {
    public CreeperRenderer() {
        super(new CreeperModel(), 0.5f);
    }

    protected void m_71846087(CreeperEntity creeperEntity, float f) {
        CreeperEntity creeperEntity2 = creeperEntity;
        float g = creeperEntity2.getFuse(f);
        float h = 1.0f + MathHelper.sin(g * 100.0f) * g * 0.01f;
        if (g < 0.0f) {
            g = 0.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        }
        g *= g;
        g *= g;
        float i = (1.0f + g * 0.4f) * h;
        float j = (1.0f + g * 0.1f) / h;
        GL11.glScalef((float)i, (float)j, (float)i);
    }

    protected int m_95433795(CreeperEntity creeperEntity, float f, float g) {
        CreeperEntity creeperEntity2 = creeperEntity;
        float h = creeperEntity2.getFuse(g);
        if ((int)(h * 10.0f) % 2 == 0) {
            return 0;
        }
        int i = (int)(h * 0.2f * 255.0f);
        if (i < 0) {
            i = 0;
        }
        if (i > 255) {
            i = 255;
        }
        int j = 255;
        int k = 255;
        int l = 255;
        return i << 24 | j << 16 | k << 8 | l;
    }
}

