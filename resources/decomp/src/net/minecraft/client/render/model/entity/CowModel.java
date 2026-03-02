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
public class CowModel
extends QuadrupedModel {
    ModelPart udder;
    ModelPart leftHorn;
    ModelPart rightHorn;

    public CowModel() {
        super(12, 0.0f);
        this.head = new ModelPart(0, 0);
        this.head.addBox(-4.0f, -4.0f, -6.0f, 8, 8, 6, 0.0f);
        this.head.setPos(0.0f, 4.0f, -8.0f);
        this.leftHorn = new ModelPart(22, 0);
        this.leftHorn.addBox(-4.0f, -5.0f, -4.0f, 1, 3, 1, 0.0f);
        this.leftHorn.setPos(0.0f, 3.0f, -7.0f);
        this.rightHorn = new ModelPart(22, 0);
        this.rightHorn.addBox(4.0f, -5.0f, -4.0f, 1, 3, 1, 0.0f);
        this.rightHorn.setPos(0.0f, 3.0f, -7.0f);
        this.udder = new ModelPart(52, 0);
        this.udder.addBox(-2.0f, -3.0f, 0.0f, 4, 6, 2, 0.0f);
        this.udder.setPos(0.0f, 14.0f, 6.0f);
        this.udder.rotationX = 1.5707964f;
        this.body = new ModelPart(18, 4);
        this.body.addBox(-6.0f, -10.0f, -7.0f, 12, 18, 10, 0.0f);
        this.body.setPos(0.0f, 5.0f, 2.0f);
        this.backRightLeg.x -= 1.0f;
        this.backLeftLeg.x += 1.0f;
        this.backRightLeg.z += 0.0f;
        this.backLeftLeg.z += 0.0f;
        this.frontRightLeg.x -= 1.0f;
        this.frontLeftLeg.x += 1.0f;
        this.frontRightLeg.z -= 1.0f;
        this.frontLeftLeg.z -= 1.0f;
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        super.render(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.leftHorn.render(scale);
        this.rightHorn.render(scale);
        this.udder.render(scale);
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        super.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.leftHorn.rotationY = this.head.rotationY;
        this.leftHorn.rotationX = this.head.rotationX;
        this.rightHorn.rotationY = this.head.rotationY;
        this.rightHorn.rotationX = this.head.rotationX;
    }
}

