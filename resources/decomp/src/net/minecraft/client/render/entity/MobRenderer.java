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
import net.minecraft.client.render.model.Model;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class MobRenderer
extends EntityRenderer {
    protected Model model;
    protected Model decorationModel;

    public MobRenderer(Model model, float shadowSize) {
        this.model = model;
        this.shadowSize = shadowSize;
    }

    public void setDecorationModel(Model decorationModel) {
        this.decorationModel = decorationModel;
    }

    public void m_61535805(MobEntity mobEntity, double d, double e, double f, float g, float h) {
        GL11.glPushMatrix();
        GL11.glDisable((int)GL11.GL_CULL_FACE);
        this.model.attackAnimationProgress = this.getAttackAnimationProgress(mobEntity, h);
        boolean bl = this.model.riding = mobEntity.vehicle != null || mobEntity.hasVehicle;
        if (this.decorationModel != null) {
            this.decorationModel.riding = this.model.riding;
        }
        try {
            float i = mobEntity.lastBodyYaw + (mobEntity.bodyYaw - mobEntity.lastBodyYaw) * h;
            float j = mobEntity.lastYaw + (mobEntity.yaw - mobEntity.lastYaw) * h;
            float k = mobEntity.lastPitch + (mobEntity.pitch - mobEntity.lastPitch) * h;
            GL11.glTranslatef((float)((float)d), (float)((float)e), (float)((float)f));
            float l = this.getBob(mobEntity, h);
            GL11.glRotatef((float)(180.0f - i), (float)0.0f, (float)1.0f, (float)0.0f);
            if (mobEntity.deathTicks > 0) {
                float m = ((float)mobEntity.deathTicks + h - 1.0f) / 20.0f * 1.6f;
                if ((m = MathHelper.sqrt(m)) > 1.0f) {
                    m = 1.0f;
                }
                GL11.glRotatef((float)(m * this.getDeathYaw(mobEntity)), (float)0.0f, (float)0.0f, (float)1.0f);
            }
            float n = 0.0625f;
            GL11.glEnable((int)32826);
            GL11.glScalef((float)-1.0f, (float)-1.0f, (float)1.0f);
            this.applyScale(mobEntity, h);
            GL11.glTranslatef((float)0.0f, (float)(-24.0f * n - 0.0078125f), (float)0.0f);
            float o = mobEntity.lastWalkAnimationSpeed + (mobEntity.walkAnimationSpeed - mobEntity.lastWalkAnimationSpeed) * h;
            float p = mobEntity.walkAnimationProgress - mobEntity.walkAnimationSpeed * (1.0f - h);
            if (o > 1.0f) {
                o = 1.0f;
            }
            this.bindHttpTexture(mobEntity.skin, mobEntity.getTexture());
            GL11.glEnable((int)GL11.GL_ALPHA_TEST);
            this.model.render(p, o, l, j - i, k, n);
            for (int q = 0; q < 4; ++q) {
                if (!this.bindTexture(mobEntity, q)) continue;
                this.decorationModel.render(p, o, l, j - i, k, n);
                GL11.glDisable((int)GL11.GL_BLEND);
                GL11.glEnable((int)GL11.GL_ALPHA_TEST);
            }
            this.renderMore(mobEntity, h);
            float r = mobEntity.getBrightness(h);
            int s = this.getOverlayColor(mobEntity, r, h);
            if ((s >> 24 & 0xFF) > 0 || mobEntity.damagedTimer > 0 || mobEntity.deathTicks > 0) {
                GL11.glDisable((int)GL11.GL_TEXTURE_2D);
                GL11.glDisable((int)GL11.GL_ALPHA_TEST);
                GL11.glEnable((int)GL11.GL_BLEND);
                GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthFunc((int)GL11.GL_EQUAL);
                if (mobEntity.damagedTimer > 0 || mobEntity.deathTicks > 0) {
                    GL11.glColor4f((float)r, (float)0.0f, (float)0.0f, (float)0.4f);
                    this.model.render(p, o, l, j - i, k, n);
                    for (int t = 0; t < 4; ++t) {
                        if (!this.bindTexture(mobEntity, t)) continue;
                        GL11.glColor4f((float)r, (float)0.0f, (float)0.0f, (float)0.4f);
                        this.decorationModel.render(p, o, l, j - i, k, n);
                    }
                }
                if ((s >> 24 & 0xFF) > 0) {
                    float u = (float)(s >> 16 & 0xFF) / 255.0f;
                    float v = (float)(s >> 8 & 0xFF) / 255.0f;
                    float w = (float)(s & 0xFF) / 255.0f;
                    float x = (float)(s >> 24 & 0xFF) / 255.0f;
                    GL11.glColor4f((float)u, (float)v, (float)w, (float)x);
                    this.model.render(p, o, l, j - i, k, n);
                    for (int y = 0; y < 4; ++y) {
                        if (!this.bindTexture(mobEntity, y)) continue;
                        GL11.glColor4f((float)u, (float)v, (float)w, (float)x);
                        this.decorationModel.render(p, o, l, j - i, k, n);
                    }
                }
                GL11.glDepthFunc((int)GL11.GL_LEQUAL);
                GL11.glDisable((int)GL11.GL_BLEND);
                GL11.glEnable((int)GL11.GL_ALPHA_TEST);
                GL11.glEnable((int)GL11.GL_TEXTURE_2D);
            }
            GL11.glDisable((int)32826);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        GL11.glEnable((int)GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    protected float getAttackAnimationProgress(MobEntity entity, float tickDelta) {
        return entity.getAttackAnimationProgress(tickDelta);
    }

    protected float getBob(MobEntity entity, float tickDelta) {
        return (float)entity.ticks + tickDelta;
    }

    protected void renderMore(MobEntity entity, float tickDelta) {
    }

    protected boolean bindTexture(MobEntity mob, int layer) {
        return false;
    }

    protected float getDeathYaw(MobEntity entity) {
        return 90.0f;
    }

    protected int getOverlayColor(MobEntity entity, float brightness, float timeDelta) {
        return 0;
    }

    protected void applyScale(MobEntity entity, float scale) {
    }
}

