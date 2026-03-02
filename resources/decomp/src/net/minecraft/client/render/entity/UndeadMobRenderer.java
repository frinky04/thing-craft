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
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class UndeadMobRenderer
extends MobRenderer {
    protected HumanoidModel model;

    public UndeadMobRenderer(HumanoidModel model, float shadowSize) {
        super(model, shadowSize);
        this.model = model;
    }

    protected void renderMore(MobEntity entity, float tickDelta) {
        ItemStack itemStack = entity.getDisplayItemInHand();
        if (itemStack != null) {
            GL11.glPushMatrix();
            this.model.rightArm.transform(0.0625f);
            GL11.glTranslatef((float)-0.0625f, (float)0.4375f, (float)0.0625f);
            if (itemStack.id < 256 && BlockRenderer.isItem3d(Block.BY_ID[itemStack.id].getRenderType())) {
                float f = 0.5f;
                GL11.glTranslatef((float)0.0f, (float)0.1875f, (float)-0.3125f);
                GL11.glRotatef((float)20.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glScalef((float)(f *= 0.75f), (float)(-f), (float)f);
            } else if (Item.BY_ID[itemStack.id].isHandheld()) {
                float g = 0.625f;
                GL11.glTranslatef((float)0.0f, (float)0.1875f, (float)0.0f);
                GL11.glScalef((float)g, (float)(-g), (float)g);
                GL11.glRotatef((float)-100.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            } else {
                float h = 0.375f;
                GL11.glTranslatef((float)0.25f, (float)0.1875f, (float)-0.1875f);
                GL11.glScalef((float)h, (float)h, (float)h);
                GL11.glRotatef((float)60.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                GL11.glRotatef((float)-90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)20.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            }
            this.dispatcher.itemInHandRenderer.render(itemStack);
            GL11.glPopMatrix();
        }
    }
}

