/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.model.entity;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.Model;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class GhastModel
extends Model {
    ModelPart body;
    ModelPart[] tentacles = new ModelPart[9];

    public GhastModel() {
        int i = -16;
        this.body = new ModelPart(0, 0);
        this.body.addBox(-8.0f, -8.0f, -8.0f, 16, 16, 16);
        this.body.y += (float)(24 + i);
        Random random = new Random(1660L);
        for (int j = 0; j < this.tentacles.length; ++j) {
            this.tentacles[j] = new ModelPart(0, 0);
            float f = (((float)(j % 3) - (float)(j / 3 % 2) * 0.5f + 0.25f) / 2.0f * 2.0f - 1.0f) * 5.0f;
            float g = ((float)(j / 3) / 2.0f * 2.0f - 1.0f) * 5.0f;
            int k = random.nextInt(7) + 8;
            this.tentacles[j].addBox(-1.0f, 0.0f, -1.0f, 2, k, 2);
            this.tentacles[j].x = f;
            this.tentacles[j].z = g;
            this.tentacles[j].y = 31 + i;
        }
    }

    public void setupAnimation(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        for (int i = 0; i < this.tentacles.length; ++i) {
            this.tentacles[i].rotationX = 0.2f * MathHelper.sin(bob * 0.3f + (float)i) + 0.4f;
        }
    }

    public void render(float walkAnimationProgress, float walkAnimationSpeed, float bob, float yaw, float pitch, float scale) {
        this.setupAnimation(walkAnimationProgress, walkAnimationSpeed, bob, yaw, pitch, scale);
        this.body.render(scale);
        for (int i = 0; i < this.tentacles.length; ++i) {
            this.tentacles[i].render(scale);
        }
    }
}

