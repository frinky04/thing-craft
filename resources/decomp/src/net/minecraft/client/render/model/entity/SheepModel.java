/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.model.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.model.entity.QuadrupedModel;

@Environment(value=EnvType.CLIENT)
public class SheepModel
extends QuadrupedModel {
    public SheepModel() {
        super(12, 0.0f);
        this.head = new ModelPart(0, 0);
        this.head.addBox(-3.0f, -4.0f, -6.0f, 6, 6, 8, 0.0f);
        this.head.setPos(0.0f, 6.0f, -8.0f);
        this.body = new ModelPart(28, 8);
        this.body.addBox(-4.0f, -10.0f, -7.0f, 8, 16, 6, 0.0f);
        this.body.setPos(0.0f, 5.0f, 2.0f);
    }
}

