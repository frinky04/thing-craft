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
public class SpiderModel
extends Model {
    public ModelPart head;
    public ModelPart neck;
    public ModelPart body;
    public ModelPart backRightLeg;
    public ModelPart backLeftLeg;
    public ModelPart backMiddleRightLeg;
    public ModelPart backMiddleLeftLeg;
    public ModelPart frontMiddleRightLeg;
    public ModelPart frontMiddleLeftLeg;
    public ModelPart frontRightLeg;
    public ModelPart frontLeftLeg;

    public SpiderModel() {
        float f = 0.0f;
        int i = 15;
        this.head = new ModelPart(32, 4);
        this.head.addBox(-4.0f, -4.0f, -8.0f, 8, 8, 8, f);
        this.head.setPos(0.0f, 0 + i, -3.0f);
        this.neck = new ModelPart(0, 0);
        this.neck.addBox(-3.0f, -3.0f, -3.0f, 6, 6, 6, f);
        this.neck.setPos(0.0f, i, 0.0f);
        this.body = new ModelPart(0, 12);
        this.body.addBox(-5.0f, -4.0f, -6.0f, 10, 8, 12, f);
        this.body.setPos(0.0f, 0 + i, 9.0f);
        this.backRightLeg = new ModelPart(18, 0);
        this.backRightLeg.addBox(-15.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.backRightLeg.setPos(-4.0f, 0 + i, 2.0f);
        this.backLeftLeg = new ModelPart(18, 0);
        this.backLeftLeg.addBox(-1.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.backLeftLeg.setPos(4.0f, 0 + i, 2.0f);
        this.backMiddleRightLeg = new ModelPart(18, 0);
        this.backMiddleRightLeg.addBox(-15.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.backMiddleRightLeg.setPos(-4.0f, 0 + i, 1.0f);
        this.backMiddleLeftLeg = new ModelPart(18, 0);
        this.backMiddleLeftLeg.addBox(-1.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.backMiddleLeftLeg.setPos(4.0f, 0 + i, 1.0f);
        this.frontMiddleRightLeg = new ModelPart(18, 0);
        this.frontMiddleRightLeg.addBox(-15.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.frontMiddleRightLeg.setPos(-4.0f, 0 + i, 0.0f);
        this.frontMiddleLeftLeg = new ModelPart(18, 0);
        this.frontMiddleLeftLeg.addBox(-1.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.frontMiddleLeftLeg.setPos(4.0f, 0 + i, 0.0f);
        this.frontRightLeg = new ModelPart(18, 0);
        this.frontRightLeg.addBox(-15.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.frontRightLeg.setPos(-4.0f, 0 + i, -1.0f);
        this.frontLeftLeg = new ModelPart(18, 0);
        this.frontLeftLeg.addBox(-1.0f, -1.0f, -1.0f, 16, 2, 2, f);
        this.frontLeftLeg.setPos(4.0f, 0 + i, -1.0f);
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.head.render(scale);
        this.neck.render(scale);
        this.body.render(scale);
        this.backRightLeg.render(scale);
        this.backLeftLeg.render(scale);
        this.backMiddleRightLeg.render(scale);
        this.backMiddleLeftLeg.render(scale);
        this.frontMiddleRightLeg.render(scale);
        this.frontMiddleLeftLeg.render(scale);
        this.frontRightLeg.render(scale);
        this.frontLeftLeg.render(scale);
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.head.rotationY = yaw / 57.295776f;
        this.head.rotationX = pitch / 57.295776f;
        float f = 0.7853982f;
        this.backRightLeg.rotationZ = -f;
        this.backLeftLeg.rotationZ = f;
        this.backMiddleRightLeg.rotationZ = -f * 0.74f;
        this.backMiddleLeftLeg.rotationZ = f * 0.74f;
        this.frontMiddleRightLeg.rotationZ = -f * 0.74f;
        this.frontMiddleLeftLeg.rotationZ = f * 0.74f;
        this.frontRightLeg.rotationZ = -f;
        this.frontLeftLeg.rotationZ = f;
        float g = -0.0f;
        float h = 0.3926991f;
        this.backRightLeg.rotationY = h * 2.0f + g;
        this.backLeftLeg.rotationY = -h * 2.0f - g;
        this.backMiddleRightLeg.rotationY = h * 1.0f + g;
        this.backMiddleLeftLeg.rotationY = -h * 1.0f - g;
        this.frontMiddleRightLeg.rotationY = -h * 1.0f + g;
        this.frontMiddleLeftLeg.rotationY = h * 1.0f - g;
        this.frontRightLeg.rotationY = -h * 2.0f + g;
        this.frontLeftLeg.rotationY = h * 2.0f - g;
        float i = -(MathHelper.cos(walkAnimationProgress * 0.6662f * 2.0f + 0.0f) * 0.4f) * walkAnimationSpeed;
        float j = -(MathHelper.cos(walkAnimationProgress * 0.6662f * 2.0f + (float)Math.PI) * 0.4f) * walkAnimationSpeed;
        float k = -(MathHelper.cos(walkAnimationProgress * 0.6662f * 2.0f + 1.5707964f) * 0.4f) * walkAnimationSpeed;
        float l = -(MathHelper.cos(walkAnimationProgress * 0.6662f * 2.0f + 4.712389f) * 0.4f) * walkAnimationSpeed;
        float m = Math.abs(MathHelper.sin(walkAnimationProgress * 0.6662f + 0.0f) * 0.4f) * walkAnimationSpeed;
        float n = Math.abs(MathHelper.sin(walkAnimationProgress * 0.6662f + (float)Math.PI) * 0.4f) * walkAnimationSpeed;
        float o = Math.abs(MathHelper.sin(walkAnimationProgress * 0.6662f + 1.5707964f) * 0.4f) * walkAnimationSpeed;
        float p = Math.abs(MathHelper.sin(walkAnimationProgress * 0.6662f + 4.712389f) * 0.4f) * walkAnimationSpeed;
        this.backRightLeg.rotationY += i;
        this.backLeftLeg.rotationY += -i;
        this.backMiddleRightLeg.rotationY += j;
        this.backMiddleLeftLeg.rotationY += -j;
        this.frontMiddleRightLeg.rotationY += k;
        this.frontMiddleLeftLeg.rotationY += -k;
        this.frontRightLeg.rotationY += l;
        this.frontLeftLeg.rotationY += -l;
        this.backRightLeg.rotationZ += m;
        this.backLeftLeg.rotationZ += -m;
        this.backMiddleRightLeg.rotationZ += n;
        this.backMiddleLeftLeg.rotationZ += -n;
        this.frontMiddleRightLeg.rotationZ += o;
        this.frontMiddleLeftLeg.rotationZ += -o;
        this.frontRightLeg.rotationZ += p;
        this.frontLeftLeg.rotationZ += -p;
    }
}

