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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.MobSpawnerRenderer;
import net.minecraft.client.render.block.entity.SignRenderer;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class BlockEntityRenderDispatcher {
    private Map renderers = new HashMap();
    public static BlockEntityRenderDispatcher INSTANCE = new BlockEntityRenderDispatcher();
    private TextRenderer textRenderer;
    public static double offsetX;
    public static double offsetY;
    public static double offsetZ;
    public TextureManager textureManager;
    public World world;
    public PlayerEntity camera;
    public float cameraYaw;
    public float cameraPitch;
    public double cameraX;
    public double cameraY;
    public double cameraZ;

    private BlockEntityRenderDispatcher() {
        this.renderers.put(SignBlockEntity.class, new SignRenderer());
        this.renderers.put(MobSpawnerBlockEntity.class, new MobSpawnerRenderer());
        for (BlockEntityRenderer blockEntityRenderer : this.renderers.values()) {
            blockEntityRenderer.init(this);
        }
    }

    public BlockEntityRenderer getRenderer(Class type) {
        BlockEntityRenderer blockEntityRenderer = (BlockEntityRenderer)this.renderers.get(type);
        if (blockEntityRenderer == null && type != BlockEntity.class) {
            blockEntityRenderer = this.getRenderer(type.getSuperclass());
            this.renderers.put(type, blockEntityRenderer);
        }
        return blockEntityRenderer;
    }

    public boolean hasRenderer(BlockEntity blockEntity) {
        return this.getRenderer(blockEntity) != null;
    }

    public BlockEntityRenderer getRenderer(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        return this.getRenderer(blockEntity.getClass());
    }

    public void prepare(World world, TextureManager textureManager, TextRenderer textRenderer, PlayerEntity camera, float tickDelta) {
        this.world = world;
        this.textureManager = textureManager;
        this.camera = camera;
        this.textRenderer = textRenderer;
        this.cameraYaw = camera.lastYaw + (camera.yaw - camera.lastYaw) * tickDelta;
        this.cameraPitch = camera.lastPitch + (camera.pitch - camera.lastPitch) * tickDelta;
        this.cameraX = camera.prevX + (camera.x - camera.prevX) * (double)tickDelta;
        this.cameraY = camera.prevY + (camera.y - camera.prevY) * (double)tickDelta;
        this.cameraZ = camera.prevZ + (camera.z - camera.prevZ) * (double)tickDelta;
    }

    public void render(BlockEntity blockEntity, float tickDelta) {
        if (blockEntity.squaredDistanceTo(this.cameraX, this.cameraY, this.cameraZ) < 4096.0) {
            float f = this.world.getBrightness(blockEntity.x, blockEntity.y, blockEntity.z);
            GL11.glColor3f((float)f, (float)f, (float)f);
            this.render(blockEntity, (double)blockEntity.x - offsetX, (double)blockEntity.y - offsetY, (double)blockEntity.z - offsetZ, tickDelta);
        }
    }

    public void render(BlockEntity blockEntity, double dx, double dy, double dz, float tickDelta) {
        BlockEntityRenderer blockEntityRenderer = this.getRenderer(blockEntity);
        if (blockEntityRenderer != null) {
            blockEntityRenderer.render(blockEntity, dx, dy, dz, tickDelta);
        }
    }

    public TextRenderer getTextRenderer() {
        return this.textRenderer;
    }
}

