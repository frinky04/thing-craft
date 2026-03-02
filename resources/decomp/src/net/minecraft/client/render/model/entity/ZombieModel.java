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
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class ZombieModel
extends HumanoidModel {
    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        super.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        float f = MathHelper.sin(this.attackAnimationProgress * (float)Math.PI);
        float g = MathHelper.sin((1.0f - (1.0f - this.attackAnimationProgress) * (1.0f - this.attackAnimationProgress)) * (float)Math.PI);
        this.rightArm.rotationZ = 0.0f;
        this.leftArm.rotationZ = 0.0f;
        this.rightArm.rotationY = -(0.1f - f * 0.6f);
        this.leftArm.rotationY = 0.1f - f * 0.6f;
        this.rightArm.rotationX = -1.5707964f;
        this.leftArm.rotationX = -1.5707964f;
        this.rightArm.rotationX -= f * 1.2f - g * 0.4f;
        this.leftArm.rotationX -= f * 1.2f - g * 0.4f;
        this.rightArm.rotationZ += MathHelper.cos(bob * 0.09f) * 0.05f + 0.05f;
        this.leftArm.rotationZ -= MathHelper.cos(bob * 0.09f) * 0.05f + 0.05f;
        this.rightArm.rotationX += MathHelper.sin(bob * 0.067f) * 0.05f;
        this.leftArm.rotationX -= MathHelper.sin(bob * 0.067f) * 0.05f;
    }
}

