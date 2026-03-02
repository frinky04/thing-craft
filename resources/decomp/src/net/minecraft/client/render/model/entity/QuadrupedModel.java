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
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class QuadrupedModel
extends Model {
    public ModelPart head = new ModelPart(0, 0);
    public ModelPart body;
    public ModelPart backRightLeg;
    public ModelPart backLeftLeg;
    public ModelPart frontRightLeg;
    public ModelPart frontLeftLeg;

    public QuadrupedModel(int pivotPoint, float reduction) {
        this.head.addBox(-4.0f, -4.0f, -8.0f, 8, 8, 8, reduction);
        this.head.setPos(0.0f, 18 - pivotPoint, -6.0f);
        this.body = new ModelPart(28, 8);
        this.body.addBox(-5.0f, -10.0f, -7.0f, 10, 16, 8, reduction);
        this.body.setPos(0.0f, 17 - pivotPoint, 2.0f);
        this.backRightLeg = new ModelPart(0, 16);
        this.backRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, pivotPoint, 4, reduction);
        this.backRightLeg.setPos(-3.0f, 24 - pivotPoint, 7.0f);
        this.backLeftLeg = new ModelPart(0, 16);
        this.backLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, pivotPoint, 4, reduction);
        this.backLeftLeg.setPos(3.0f, 24 - pivotPoint, 7.0f);
        this.frontRightLeg = new ModelPart(0, 16);
        this.frontRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, pivotPoint, 4, reduction);
        this.frontRightLeg.setPos(-3.0f, 24 - pivotPoint, -5.0f);
        this.frontLeftLeg = new ModelPart(0, 16);
        this.frontLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, pivotPoint, 4, reduction);
        this.frontLeftLeg.setPos(3.0f, 24 - pivotPoint, -5.0f);
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.head.render(scale);
        this.body.render(scale);
        this.backRightLeg.render(scale);
        this.backLeftLeg.render(scale);
        this.frontRightLeg.render(scale);
        this.frontLeftLeg.render(scale);
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.head.rotationX = -(pitch / 57.295776f);
        this.head.rotationY = yaw / 57.295776f;
        this.body.rotationX = 1.5707964f;
        this.backRightLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 1.4f * walkAnimationSpeed;
        this.backLeftLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 1.4f * walkAnimationSpeed;
        this.frontRightLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 1.4f * walkAnimationSpeed;
        this.frontLeftLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 1.4f * walkAnimationSpeed;
    }
}

