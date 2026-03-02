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
import net.minecraft.client.render.model.entity.SpiderModel;
import net.minecraft.entity.mob.monster.SpiderEntity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class SpiderRenderer
extends MobRenderer {
    public SpiderRenderer() {
        super(new SpiderModel(), 1.0f);
        this.setDecorationModel(new SpiderModel());
    }

    protected float m_92916927(SpiderEntity spiderEntity) {
        return 180.0f;
    }

    protected boolean m_62713151(SpiderEntity spiderEntity, int i) {
        if (i != 0) {
            return false;
        }
        if (i != 0) {
            return false;
        }
        this.bindTexture("/mob/spider_eyes.png");
        float f = (1.0f - spiderEntity.getBrightness(1.0f)) * 0.5f;
        GL11.glEnable((int)GL11.GL_BLEND);
        GL11.glDisable((int)GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)f);
        return true;
    }
}

