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
import net.minecraft.entity.mob.animal.ChickenEntity;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class ChickenRenderer
extends MobRenderer {
    public ChickenRenderer(Model model, float f) {
        super(model, f);
    }

    public void m_38805144(ChickenEntity chickenEntity, double d, double e, double f, float g, float h) {
        super.m_61535805(chickenEntity, d, e, f, g, h);
    }

    protected float m_83830629(ChickenEntity chickenEntity, float f) {
        float g = chickenEntity.lastFlapProgress + (chickenEntity.flapProgress - chickenEntity.lastFlapProgress) * f;
        float h = chickenEntity.lastFlapSpeed + (chickenEntity.flapSpeed - chickenEntity.lastFlapSpeed) * f;
        return (MathHelper.sin(g) + 1.0f) * h;
    }
}

