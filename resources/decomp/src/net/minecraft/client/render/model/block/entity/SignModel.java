/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.model.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.ModelPart;

@Environment(value=EnvType.CLIENT)
public class SignModel {
    public ModelPart board = new ModelPart(0, 0);
    public ModelPart pole;

    public SignModel() {
        this.board.addBox(-12.0f, -14.0f, -1.0f, 24, 12, 2, 0.0f);
        this.pole = new ModelPart(0, 14);
        this.pole.addBox(-1.0f, -2.0f, -1.0f, 2, 14, 2, 0.0f);
    }

    public void render() {
        this.board.render(0.0625f);
        this.pole.render(0.0625f);
    }
}

