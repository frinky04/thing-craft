/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.texture.TextureManager;

@Environment(value=EnvType.CLIENT)
public abstract class BlockEntityRenderer {
    protected BlockEntityRenderDispatcher dispatcher;

    public abstract void render(BlockEntity var1, double var2, double var4, double var6, float var8);

    protected void bindTexture(String path) {
        TextureManager textureManager = this.dispatcher.textureManager;
        textureManager.bind(textureManager.load(path));
    }

    public void init(BlockEntityRenderDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public TextRenderer getTextRenderer() {
        return this.dispatcher.getTextRenderer();
    }
}

