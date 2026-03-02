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
public class SheepFurModel
extends QuadrupedModel {
    public SheepFurModel() {
        super(12, 0.0f);
        this.head = new ModelPart(0, 0);
        this.head.addBox(-3.0f, -4.0f, -4.0f, 6, 6, 6, 0.6f);
        this.head.setPos(0.0f, 6.0f, -8.0f);
        this.body = new ModelPart(28, 8);
        this.body.addBox(-4.0f, -10.0f, -7.0f, 8, 16, 6, 1.75f);
        this.body.setPos(0.0f, 5.0f, 2.0f);
        float f = 0.5f;
        this.backRightLeg = new ModelPart(0, 16);
        this.backRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.backRightLeg.setPos(-3.0f, 12.0f, 7.0f);
        this.backLeftLeg = new ModelPart(0, 16);
        this.backLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.backLeftLeg.setPos(3.0f, 12.0f, 7.0f);
        this.frontRightLeg = new ModelPart(0, 16);
        this.frontRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.frontRightLeg.setPos(-3.0f, 12.0f, -5.0f);
        this.frontLeftLeg = new ModelPart(0, 16);
        this.frontLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.frontLeftLeg.setPos(3.0f, 12.0f, -5.0f);
    }
}

