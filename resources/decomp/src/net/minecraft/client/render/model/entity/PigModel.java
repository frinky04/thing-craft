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
import net.minecraft.client.render.model.entity.QuadrupedModel;

@Environment(value=EnvType.CLIENT)
public class PigModel
extends QuadrupedModel {
    public PigModel() {
        super(6, 0.0f);
    }

    public PigModel(float reduction) {
        super(6, reduction);
    }
}

