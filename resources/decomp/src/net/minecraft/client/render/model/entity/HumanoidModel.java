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
public class HumanoidModel
extends Model {
    public ModelPart head = new ModelPart(0, 0);
    public ModelPart hat;
    public ModelPart body;
    public ModelPart rightArm;
    public ModelPart leftArm;
    public ModelPart rightLeg;
    public ModelPart leftLeg;
    public boolean itemInLeftHand = false;
    public boolean itemInRightHand = false;
    public boolean sneaking = false;

    public HumanoidModel() {
        this(0.0f);
    }

    public HumanoidModel(float reduction) {
        this(reduction, 0.0f);
    }

    public HumanoidModel(float reduction, float pivotPoint) {
        this.head.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, reduction);
        this.head.setPos(0.0f, 0.0f + pivotPoint, 0.0f);
        this.hat = new ModelPart(32, 0);
        this.hat.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, reduction + 0.5f);
        this.hat.setPos(0.0f, 0.0f + pivotPoint, 0.0f);
        this.body = new ModelPart(16, 16);
        this.body.addBox(-4.0f, 0.0f, -2.0f, 8, 12, 4, reduction);
        this.body.setPos(0.0f, 0.0f + pivotPoint, 0.0f);
        this.rightArm = new ModelPart(40, 16);
        this.rightArm.addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4, reduction);
        this.rightArm.setPos(-5.0f, 2.0f + pivotPoint, 0.0f);
        this.leftArm = new ModelPart(40, 16);
        this.leftArm.flipped = true;
        this.leftArm.addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4, reduction);
        this.leftArm.setPos(5.0f, 2.0f + pivotPoint, 0.0f);
        this.rightLeg = new ModelPart(0, 16);
        this.rightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4, reduction);
        this.rightLeg.setPos(-2.0f, 12.0f + pivotPoint, 0.0f);
        this.leftLeg = new ModelPart(0, 16);
        this.leftLeg.flipped = true;
        this.leftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4, reduction);
        this.leftLeg.setPos(2.0f, 12.0f + pivotPoint, 0.0f);
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.head.render(scale);
        this.body.render(scale);
        this.rightArm.render(scale);
        this.leftArm.render(scale);
        this.rightLeg.render(scale);
        this.leftLeg.render(scale);
        this.hat.render(scale);
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.head.rotationY = yaw / 57.295776f;
        this.head.rotationX = pitch / 57.295776f;
        this.hat.rotationY = this.head.rotationY;
        this.hat.rotationX = this.head.rotationX;
        this.rightArm.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 2.0f * walkAnimationSpeed * 0.5f;
        this.leftArm.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 2.0f * walkAnimationSpeed * 0.5f;
        this.rightArm.rotationZ = 0.0f;
        this.leftArm.rotationZ = 0.0f;
        this.rightLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f) * 1.4f * walkAnimationSpeed;
        this.leftLeg.rotationX = MathHelper.cos(walkAnimationProgress * 0.6662f + (float)Math.PI) * 1.4f * walkAnimationSpeed;
        this.rightLeg.rotationY = 0.0f;
        this.leftLeg.rotationY = 0.0f;
        if (this.riding) {
            this.rightArm.rotationX += -0.62831855f;
            this.leftArm.rotationX += -0.62831855f;
            this.rightLeg.rotationX = -1.2566371f;
            this.leftLeg.rotationX = -1.2566371f;
            this.rightLeg.rotationY = 0.31415927f;
            this.leftLeg.rotationY = -0.31415927f;
        }
        if (this.itemInLeftHand) {
            this.leftArm.rotationX = this.leftArm.rotationX * 0.5f - 0.31415927f;
        }
        if (this.itemInRightHand) {
            this.rightArm.rotationX = this.rightArm.rotationX * 0.5f - 0.31415927f;
        }
        this.rightArm.rotationY = 0.0f;
        this.leftArm.rotationY = 0.0f;
        if (this.attackAnimationProgress > -9990.0f) {
            float f = this.attackAnimationProgress;
            this.body.rotationY = MathHelper.sin(MathHelper.sqrt(f) * (float)Math.PI * 2.0f) * 0.2f;
            this.rightArm.z = MathHelper.sin(this.body.rotationY) * 5.0f;
            this.rightArm.x = -MathHelper.cos(this.body.rotationY) * 5.0f;
            this.leftArm.z = -MathHelper.sin(this.body.rotationY) * 5.0f;
            this.leftArm.x = MathHelper.cos(this.body.rotationY) * 5.0f;
            this.rightArm.rotationY += this.body.rotationY;
            this.leftArm.rotationY += this.body.rotationY;
            this.leftArm.rotationX += this.body.rotationY;
            f = 1.0f - this.attackAnimationProgress;
            f *= f;
            f *= f;
            f = 1.0f - f;
            float g = MathHelper.sin(f * (float)Math.PI);
            float h = MathHelper.sin(this.attackAnimationProgress * (float)Math.PI) * -(this.head.rotationX - 0.7f) * 0.75f;
            this.rightArm.rotationX = (float)((double)this.rightArm.rotationX - ((double)g * 1.2 + (double)h));
            this.rightArm.rotationY += this.body.rotationY * 2.0f;
            this.rightArm.rotationZ = MathHelper.sin(this.attackAnimationProgress * (float)Math.PI) * -0.4f;
        }
        if (this.sneaking) {
            this.body.rotationX = 0.5f;
            this.rightLeg.rotationX -= 0.0f;
            this.leftLeg.rotationX -= 0.0f;
            this.rightArm.rotationX += 0.4f;
            this.leftArm.rotationX += 0.4f;
            this.rightLeg.z = 4.0f;
            this.leftLeg.z = 4.0f;
            this.rightLeg.y = 9.0f;
            this.leftLeg.y = 9.0f;
            this.head.y = 1.0f;
        } else {
            this.body.rotationX = 0.0f;
            this.rightLeg.z = 0.0f;
            this.leftLeg.z = 0.0f;
            this.rightLeg.y = 12.0f;
            this.leftLeg.y = 12.0f;
            this.head.y = 0.0f;
        }
        this.rightArm.rotationZ += MathHelper.cos(bob * 0.09f) * 0.05f + 0.05f;
        this.leftArm.rotationZ -= MathHelper.cos(bob * 0.09f) * 0.05f + 0.05f;
        this.rightArm.rotationX += MathHelper.sin(bob * 0.067f) * 0.05f;
        this.leftArm.rotationX -= MathHelper.sin(bob * 0.067f) * 0.05f;
    }
}

