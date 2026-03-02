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
import net.minecraft.entity.mob.animal.SheepEntity;

@Environment(value=EnvType.CLIENT)
public class SheepRenderer
extends MobRenderer {
    public SheepRenderer(Model model, Model furModel, float shadowSize) {
        super(model, shadowSize);
        this.setDecorationModel(furModel);
    }

    protected boolean m_40314811(SheepEntity sheepEntity, int i) {
        this.bindTexture("/mob/sheep_fur.png");
        return i == 0 && !sheepEntity.sheared;
    }
}

