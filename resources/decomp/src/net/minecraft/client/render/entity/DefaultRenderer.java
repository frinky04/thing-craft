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
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class DefaultRenderer
extends EntityRenderer {
    public void render(Entity entity, double dx, double dy, double dz, float yaw, float tickDelta) {
        GL11.glPushMatrix();
        DefaultRenderer.renderShape(entity.shape, dx - entity.prevX, dy - entity.prevY, dz - entity.prevZ);
        GL11.glPopMatrix();
    }
}

