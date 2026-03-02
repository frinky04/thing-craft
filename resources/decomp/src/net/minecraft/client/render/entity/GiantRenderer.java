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
import net.minecraft.entity.mob.monster.GiantEntity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class GiantRenderer
extends MobRenderer {
    private float scale;

    public GiantRenderer(Model model, float shadowSize, float scale) {
        super(model, shadowSize * scale);
        this.scale = scale;
    }

    protected void m_48077585(GiantEntity giantEntity, float f) {
        GL11.glScalef((float)this.scale, (float)this.scale, (float)this.scale);
    }
}

