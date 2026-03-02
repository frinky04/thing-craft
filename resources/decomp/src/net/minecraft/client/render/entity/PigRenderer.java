/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.Model;
import net.minecraft.entity.mob.animal.PigEntity;

@Environment(value=EnvType.CLIENT)
public class PigRenderer
extends MobRenderer {
    public PigRenderer(Model model, Model saddleModel, float shadowSize) {
        super(model, shadowSize);
        this.setDecorationModel(saddleModel);
    }

    protected boolean m_14716387(PigEntity pigEntity, int i) {
        this.bindTexture("/mob/saddle.png");
        return i == 0 && pigEntity.saddled;
    }
}

