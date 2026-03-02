/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class FallingBlockRenderer
extends EntityRenderer {
    private BlockRenderer blockRenderer = new BlockRenderer();

    public FallingBlockRenderer() {
        this.shadowSize = 0.5f;
    }

    public void m_86140670(FallingBlockEntity fallingBlockEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
        this.bindTexture("/terrain.png");
        Block block = Block.BY_ID[fallingBlockEntity.block];
        World world = fallingBlockEntity.getWorld();
        GL11.glDisable((int)GL11.GL_LIGHTING);
        this.blockRenderer.tesselateFallingBlock(block, world, MathHelper.floor(fallingBlockEntity.x), MathHelper.floor(fallingBlockEntity.y), MathHelper.floor(fallingBlockEntity.z));
        GL11.glEnable((int)GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
}

