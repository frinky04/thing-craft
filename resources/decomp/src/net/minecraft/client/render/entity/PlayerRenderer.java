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
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class PlayerRenderer
extends MobRenderer {
    private HumanoidModel player;
    private HumanoidModel armor1;
    private HumanoidModel armor2;
    private static final String[] ARMOR_VARIANTS = new String[]{"cloth", "chain", "iron", "diamond", "gold"};

    public PlayerRenderer() {
        super(new HumanoidModel(0.0f), 0.5f);
        this.player = (HumanoidModel)this.model;
        this.armor1 = new HumanoidModel(1.0f);
        this.armor2 = new HumanoidModel(0.5f);
    }

    protected boolean m_21812092(PlayerEntity playerEntity, int i) {
        Item item;
        ItemStack itemStack = playerEntity.inventory.getArmor(3 - i);
        if (itemStack != null && (item = itemStack.getItem()) instanceof ArmorItem) {
            ArmorItem armorItem = (ArmorItem)item;
            this.bindTexture("/armor/" + ARMOR_VARIANTS[armorItem.material] + "_" + (i == 2 ? 2 : 1) + ".png");
            HumanoidModel humanoidModel = i == 2 ? this.armor2 : this.armor1;
            humanoidModel.head.visible = i == 0;
            humanoidModel.hat.visible = i == 0;
            humanoidModel.body.visible = i == 1 || i == 2;
            humanoidModel.rightArm.visible = i == 1;
            humanoidModel.leftArm.visible = i == 1;
            humanoidModel.rightLeg.visible = i == 2 || i == 3;
            humanoidModel.leftLeg.visible = i == 2 || i == 3;
            this.setDecorationModel(humanoidModel);
            return true;
        }
        return false;
    }

    public void m_69546567(PlayerEntity playerEntity, double d, double e, double f, float g, float h) {
        float m;
        ItemStack itemStack = playerEntity.inventory.getSelectedItem();
        this.player.itemInRightHand = itemStack != null;
        this.armor2.itemInRightHand = this.player.itemInRightHand;
        this.armor1.itemInRightHand = this.player.itemInRightHand;
        this.armor2.sneaking = this.player.sneaking = playerEntity.isSneaking();
        this.armor1.sneaking = this.player.sneaking;
        double i = e - (double)playerEntity.eyeHeight;
        if (playerEntity.sneaking) {
            i -= 0.125;
        }
        super.m_61535805(playerEntity, d, i, f, g, h);
        this.player.sneaking = false;
        this.armor2.sneaking = false;
        this.armor1.sneaking = false;
        this.player.itemInRightHand = false;
        this.armor2.itemInRightHand = false;
        this.armor1.itemInRightHand = false;
        float j = 1.6f;
        float k = 0.016666668f * j;
        float l = playerEntity.distanceTo(this.dispatcher.camera);
        float f2 = m = playerEntity.isSneaking() ? 32.0f : 64.0f;
        if (l < m) {
            k = (float)((double)k * (Math.sqrt(l) / 2.0));
            TextRenderer textRenderer = this.getTextRenderer();
            GL11.glPushMatrix();
            GL11.glTranslatef((float)((float)d + 0.0f), (float)((float)e + 2.3f), (float)((float)f));
            GL11.glNormal3f((float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)(-this.dispatcher.cameraYaw), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)this.dispatcher.cameraPitch, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glScalef((float)(-k), (float)(-k), (float)k);
            String string = playerEntity.name;
            GL11.glDisable((int)GL11.GL_LIGHTING);
            if (!playerEntity.isSneaking()) {
                GL11.glDepthMask((boolean)false);
                GL11.glDisable((int)GL11.GL_DEPTH_TEST);
                GL11.glEnable((int)GL11.GL_BLEND);
                GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
                Tesselator tesselator = Tesselator.INSTANCE;
                GL11.glDisable((int)GL11.GL_TEXTURE_2D);
                tesselator.begin();
                int n = textRenderer.getWidth(string) / 2;
                tesselator.color(0.0f, 0.0f, 0.0f, 0.25f);
                tesselator.vertex(-n - 1, -1.0, 0.0);
                tesselator.vertex(-n - 1, 8.0, 0.0);
                tesselator.vertex(n + 1, 8.0, 0.0);
                tesselator.vertex(n + 1, -1.0, 0.0);
                tesselator.end();
                GL11.glEnable((int)GL11.GL_TEXTURE_2D);
                textRenderer.draw(string, -textRenderer.getWidth(string) / 2, 0, 0x20FFFFFF);
                GL11.glEnable((int)GL11.GL_DEPTH_TEST);
                GL11.glDepthMask((boolean)true);
                textRenderer.draw(string, -textRenderer.getWidth(string) / 2, 0, -1);
                GL11.glEnable((int)GL11.GL_LIGHTING);
                GL11.glDisable((int)GL11.GL_BLEND);
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                GL11.glPopMatrix();
            } else {
                GL11.glTranslatef((float)0.0f, (float)(0.25f / k), (float)0.0f);
                GL11.glDepthMask((boolean)false);
                GL11.glEnable((int)GL11.GL_BLEND);
                GL11.glBlendFunc((int)GL11.GL_SRC_ALPHA, (int)GL11.GL_ONE_MINUS_SRC_ALPHA);
                Tesselator tesselator2 = Tesselator.INSTANCE;
                GL11.glDisable((int)GL11.GL_TEXTURE_2D);
                tesselator2.begin();
                int o = textRenderer.getWidth(string) / 2;
                tesselator2.color(0.0f, 0.0f, 0.0f, 0.25f);
                tesselator2.vertex(-o - 1, -1.0, 0.0);
                tesselator2.vertex(-o - 1, 8.0, 0.0);
                tesselator2.vertex(o + 1, 8.0, 0.0);
                tesselator2.vertex(o + 1, -1.0, 0.0);
                tesselator2.end();
                GL11.glEnable((int)GL11.GL_TEXTURE_2D);
                GL11.glDepthMask((boolean)true);
                textRenderer.draw(string, -textRenderer.getWidth(string) / 2, 0, 0x20FFFFFF);
                GL11.glEnable((int)GL11.GL_LIGHTING);
                GL11.glDisable((int)GL11.GL_BLEND);
                GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                GL11.glPopMatrix();
            }
        }
    }

    protected void m_52625690(PlayerEntity playerEntity, float f) {
        ItemStack itemStack2;
        ItemStack itemStack = playerEntity.inventory.getArmor(3);
        if (itemStack != null && itemStack.getItem().id < 256) {
            GL11.glPushMatrix();
            this.player.head.transform(0.0625f);
            if (BlockRenderer.isItem3d(Block.BY_ID[itemStack.id].getRenderType())) {
                float g = 0.625f;
                GL11.glTranslatef((float)0.0f, (float)-0.25f, (float)0.0f);
                GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glScalef((float)g, (float)(-g), (float)g);
            }
            this.dispatcher.itemInHandRenderer.render(itemStack);
            GL11.glPopMatrix();
        }
        if ((itemStack2 = playerEntity.inventory.getSelectedItem()) != null) {
            GL11.glPushMatrix();
            this.player.rightArm.transform(0.0625f);
            GL11.glTranslatef((float)-0.0625f, (float)0.4375f, (float)0.0625f);
            if (playerEntity.fishingBobber != null) {
                itemStack2 = new ItemStack(Item.STICK.id);
            }
            if (itemStack2.id < 256 && BlockRenderer.isItem3d(Block.BY_ID[itemStack2.id].getRenderType())) {
                float h = 0.5f;
                GL11.glTranslatef((float)0.0f, (float)0.1875f, (float)-0.3125f);
                GL11.glRotatef((float)20.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glScalef((float)(h *= 0.75f), (float)(-h), (float)h);
            } else if (Item.BY_ID[itemStack2.id].isHandheld()) {
                float i = 0.625f;
                if (Item.BY_ID[itemStack2.id].shouldRotate()) {
                    GL11.glRotatef((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                    GL11.glTranslatef((float)0.0f, (float)-0.125f, (float)0.0f);
                }
                GL11.glTranslatef((float)0.0f, (float)0.1875f, (float)0.0f);
                GL11.glScalef((float)i, (float)(-i), (float)i);
                GL11.glRotatef((float)-100.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            } else {
                float j = 0.375f;
                GL11.glTranslatef((float)0.25f, (float)0.1875f, (float)-0.1875f);
                GL11.glScalef((float)j, (float)j, (float)j);
                GL11.glRotatef((float)60.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                GL11.glRotatef((float)-90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)20.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            }
            this.dispatcher.itemInHandRenderer.render(itemStack2);
            GL11.glPopMatrix();
        }
    }

    protected void m_81022230(PlayerEntity playerEntity, float f) {
        float g = 0.9375f;
        GL11.glScalef((float)g, (float)g, (float)g);
    }

    public void renderRightHand() {
        this.player.attackAnimationProgress = 0.0f;
        this.player.setupAnimation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        this.player.rightArm.render(0.0625f);
    }
}

