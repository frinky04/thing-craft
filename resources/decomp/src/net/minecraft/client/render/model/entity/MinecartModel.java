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
public class MinecartModel
extends Model {
    public ModelPart[] parts = new ModelPart[7];

    public MinecartModel() {
        this.parts[0] = new ModelPart(0, 10);
        this.parts[1] = new ModelPart(0, 0);
        this.parts[2] = new ModelPart(0, 0);
        this.parts[3] = new ModelPart(0, 0);
        this.parts[4] = new ModelPart(0, 0);
        this.parts[5] = new ModelPart(44, 10);
        int i = 20;
        int j = 8;
        int k = 16;
        int l = 4;
        this.parts[0].addBox(-i / 2, -k / 2, -1.0f, i, k, 2, 0.0f);
        this.parts[0].setPos(0.0f, 0 + l, 0.0f);
        this.parts[5].addBox(-i / 2 + 1, -k / 2 + 1, -1.0f, i - 2, k - 2, 1, 0.0f);
        this.parts[5].setPos(0.0f, 0 + l, 0.0f);
        this.parts[1].addBox(-i / 2 + 2, -j - 1, -1.0f, i - 4, j, 2, 0.0f);
        this.parts[1].setPos(-i / 2 + 1, 0 + l, 0.0f);
        this.parts[2].addBox(-i / 2 + 2, -j - 1, -1.0f, i - 4, j, 2, 0.0f);
        this.parts[2].setPos(i / 2 - 1, 0 + l, 0.0f);
        this.parts[3].addBox(-i / 2 + 2, -j - 1, -1.0f, i - 4, j, 2, 0.0f);
        this.parts[3].setPos(0.0f, 0 + l, -k / 2 + 1);
        this.parts[4].addBox(-i / 2 + 2, -j - 1, -1.0f, i - 4, j, 2, 0.0f);
        this.parts[4].setPos(0.0f, 0 + l, k / 2 - 1);
        this.parts[0].rotationX = 1.5707964f;
        this.parts[1].rotationY = 4.712389f;
        this.parts[2].rotationY = 1.5707964f;
        this.parts[3].rotationY = (float)Math.PI;
        this.parts[5].rotationX = -1.5707964f;
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.parts[5].y = 4.0f - bob;
        for (int i = 0; i < 6; ++i) {
            this.parts[i].render(scale);
        }
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
    }
}

