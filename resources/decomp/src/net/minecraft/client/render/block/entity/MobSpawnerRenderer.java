/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.block.entity;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class MobSpawnerRenderer
extends BlockEntityRenderer {
    private Map models = new HashMap();

    public void m_41904860(MobSpawnerBlockEntity x, double d, double e, double f, float g) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float)((float)d + 0.5f), (float)((float)e), (float)((float)f + 0.5f));
        Entity entity = (Entity)this.models.get(x.type);
        if (entity == null) {
            entity = Entities.createSilently(x.type, null);
            this.models.put(x.type, entity);
        }
        if (entity != null) {
            entity.setWorld(x.world);
            float h = 0.4375f;
            GL11.glTranslatef((float)0.0f, (float)0.4f, (float)0.0f);
            GL11.glRotatef((float)((float)(x.lastRotation + (x.rotation - x.lastRotation) * (double)g) * 10.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)-30.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glTranslatef((float)0.0f, (float)-0.4f, (float)0.0f);
            GL11.glScalef((float)h, (float)h, (float)h);
            entity.setPositionAndAngles(d, e, f, 0.0f, 0.0f);
            EntityRenderDispatcher.INSTANCE.render(entity, 0.0, 0.0, 0.0, 0.0f, g);
        }
        GL11.glPopMatrix();
    }
}

