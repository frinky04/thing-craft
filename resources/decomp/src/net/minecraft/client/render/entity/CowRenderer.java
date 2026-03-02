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
import net.minecraft.entity.mob.animal.CowEntity;

@Environment(value=EnvType.CLIENT)
public class CowRenderer
extends MobRenderer {
    public CowRenderer(Model model, float f) {
        super(model, f);
    }

    public void m_36236774(CowEntity cowEntity, double d, double e, double f, float g, float h) {
        super.m_61535805(cowEntity, d, e, f, g, h);
    }
}

