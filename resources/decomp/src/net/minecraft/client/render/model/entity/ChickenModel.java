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
public class ChickenModel
extends Model {
    public ModelPart head;
    public ModelPart body;
    public ModelPart rightLeg;
    public ModelPart leftLeg;
    public ModelPart rightWing;
    public ModelPart leftWing;
    public ModelPart beak;
    public ModelPart waddle;

    public ChickenModel() {
        int i = 16;
        this.head = new ModelPart(0, 0);
        this.head.addBox(-2.0f, -6.0f, -2.0f, 4, 6, 3, 0.0f);
        this.head.setPos(0.0f, -1 + i, -4.0f);
        this.beak = new ModelPart(14, 0);
        this.beak.addBox(-2.0f, -4.0f, -4.0f, 4, 2, 2, 0.0f);
        this.beak.setPos(0.0f, -1 + i, -4.0f);
        this.waddle = new ModelPart(14, 4);
        this.waddle.addBox(-1.0f, -2.0f, -3.0f, 2, 2, 2, 0.0f);
        this.waddle.setPos(0.0f, -1 + i, -4.0f);
        this.body = new ModelPart(0, 9);
        this.body.addBox(-3.0f, -4.0f, -3.0f, 6, 8, 6, 0.0f);
        this.body.setPos(0.0f, 0 + i, 0.0f);
        this.rightLeg = new ModelPart(26, 0);
        this.rightLeg.addBox(-1.0f, 0.0f, -3.0f, 3, 5, 3);
        this.rightLeg.setPos(-2.0f, 3 + i, 1.0f);
        this.leftLeg = new ModelPart(26, 0);
        this.leftLeg.addBox(-1.0f, 0.0f, -3.0f, 3, 5, 3);
        this.leftLeg.setPos(1.0f, 3 + i, 1.0f);
        this.rightWing = new ModelPart(24, 13);
        this.rightWing.addBox(0.0f, 0.0f, -3.0f, 1, 4, 6);
        this.rightWing.setPos(-4.0f, -3 + i, 0.0f);
        this.leftWing = new ModelPart(24, 13);
        this.leftWing.addBox(-1.0f, 0.0f, -3.0f, 1, 4, 6);
        this.leftWing.setPos(4.0f, -3 + i, 0.0f);
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.head.render(scale);
        this.beak.render(scale);
        this.waddle.render(scale);
        this.body.render(scale);
        this.rightLeg.render(scale);
        this.leftLeg.render(scale);
        this.rightWing.render(scale);
        this.leftWing.render(scale);
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.head.rotationX = -(pitch / 57.295776f);
        this.head.rotationY = yaw / 57.295776f;
        this.beak.rotationX = this.head.rotationX;
        this.beak.rotationY = this.head.rotationY;
        this.waddle.rotationX = this.head.rotationX;
        this.waddle.rotationY = this.head.rotationY;
        this.body.rotationX = 1.5707964f;
        this.rightLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 1.4f * walkAnimationSpeed;
        this.leftLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 1.4f * walkAnimationSpeed;
        this.rightWing.rotationZ = bob;
        this.leftWing.rotationZ = -bob;
    }
}

