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
import net.minecraft.client.render.model.entity.ZombieModel;

@Environment(value=EnvType.CLIENT)
public class SkeletonModel
extends ZombieModel {
    public SkeletonModel() {
        float f = 0.0f;
        this.rightArm = new ModelPart(40, 16);
        this.rightArm.addBox(-1.0f, -2.0f, -1.0f, 2, 12, 2, f);
        this.rightArm.setPos(-5.0f, 2.0f, 0.0f);
        this.leftArm = new ModelPart(40, 16);
        this.leftArm.flipped = true;
        this.leftArm.addBox(-1.0f, -2.0f, -1.0f, 2, 12, 2, f);
        this.leftArm.setPos(5.0f, 2.0f, 0.0f);
        this.rightLeg = new ModelPart(0, 16);
        this.rightLeg.addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2, f);
        this.rightLeg.setPos(-2.0f, 12.0f, 0.0f);
        this.leftLeg = new ModelPart(0, 16);
        this.leftLeg.flipped = true;
        this.leftLeg.addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2, f);
        this.leftLeg.setPos(2.0f, 12.0f, 0.0f);
    }
}

