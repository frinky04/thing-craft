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
public class CreeperModel
extends Model {
    public ModelPart head;
    public ModelPart hat;
    public ModelPart body;
    public ModelPart rightBackLeg;
    public ModelPart leftBackleg;
    public ModelPart rightFrontLeg;
    public ModelPart leftFrontLeg;

    public CreeperModel() {
        float f = 0.0f;
        int i = 4;
        this.head = new ModelPart(0, 0);
        this.head.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, f);
        this.head.setPos(0.0f, i, 0.0f);
        this.hat = new ModelPart(32, 0);
        this.hat.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, f + 0.5f);
        this.hat.setPos(0.0f, i, 0.0f);
        this.body = new ModelPart(16, 16);
        this.body.addBox(-4.0f, 0.0f, -2.0f, 8, 12, 4, f);
        this.body.setPos(0.0f, i, 0.0f);
        this.rightBackLeg = new ModelPart(0, 16);
        this.rightBackLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.rightBackLeg.setPos(-2.0f, 12 + i, 4.0f);
        this.leftBackleg = new ModelPart(0, 16);
        this.leftBackleg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.leftBackleg.setPos(2.0f, 12 + i, 4.0f);
        this.rightFrontLeg = new ModelPart(0, 16);
        this.rightFrontLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.rightFrontLeg.setPos(-2.0f, 12 + i, -4.0f);
        this.leftFrontLeg = new ModelPart(0, 16);
        this.leftFrontLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, f);
        this.leftFrontLeg.setPos(2.0f, 12 + i, -4.0f);
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.head.render(scale);
        this.body.render(scale);
        this.rightBackLeg.render(scale);
        this.leftBackleg.render(scale);
        this.rightFrontLeg.render(scale);
        this.leftFrontLeg.render(scale);
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.head.rotationY = yaw / 57.295776f;
        this.head.rotationX = pitch / 57.295776f;
        this.rightBackLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 1.4f * walkAnimationSpeed;
        this.leftBackleg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 1.4f * walkAnimationSpeed;
        this.rightFrontLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 1.4f * walkAnimationSpeed;
        this.leftFrontLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 1.4f * walkAnimationSpeed;
    }
}

