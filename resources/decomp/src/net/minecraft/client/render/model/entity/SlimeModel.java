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

@Environment(value=EnvType.CLIENT)
public class SlimeModel
extends Model {
    ModelPart body;
    ModelPart rightEye;
    ModelPart leftEye;
    ModelPart mouth;

    public SlimeModel(int v) {
        this.body = new ModelPart(0, v);
        this.body.addBox(-4.0f, 16.0f, -4.0f, 8, 8, 8);
        if (v > 0) {
            this.body = new ModelPart(0, v);
            this.body.addBox(-3.0f, 17.0f, -3.0f, 6, 6, 6);
            this.rightEye = new ModelPart(32, 0);
            this.rightEye.addBox(-3.25f, 18.0f, -3.5f, 2, 2, 2);
            this.leftEye = new ModelPart(32, 4);
            this.leftEye.addBox(1.25f, 18.0f, -3.5f, 2, 2, 2);
            this.mouth = new ModelPart(32, 8);
            this.mouth.addBox(0.0f, 21.0f, -3.5f, 1, 1, 1);
        }
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.body.render(scale);
        if (this.rightEye != null) {
            this.rightEye.render(scale);
            this.leftEye.render(scale);
            this.mouth.render(scale);
        }
    }
}

