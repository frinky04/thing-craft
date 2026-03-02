/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.entity;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.ItemInHandRenderer;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.entity.ArrowRenderer;
import net.minecraft.client.render.entity.BoatRenderer;
import net.minecraft.client.render.entity.ChickenRenderer;
import net.minecraft.client.render.entity.CowRenderer;
import net.minecraft.client.render.entity.CreeperRenderer;
import net.minecraft.client.render.entity.DefaultRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.FallingBlockRenderer;
import net.minecraft.client.render.entity.FireballRenderer;
import net.minecraft.client.render.entity.FishingBobberRenderer;
import net.minecraft.client.render.entity.GhastRenderer;
import net.minecraft.client.render.entity.GiantRenderer;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.entity.MinecartRenderer;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.entity.PaintingRenderer;
import net.minecraft.client.render.entity.PigRenderer;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.client.render.entity.PrimedTntRenderer;
import net.minecraft.client.render.entity.SheepRenderer;
import net.minecraft.client.render.entity.SlimeRenderer;
import net.minecraft.client.render.entity.SnowballRenderer;
import net.minecraft.client.render.entity.SpiderRenderer;
import net.minecraft.client.render.entity.UndeadMobRenderer;
import net.minecraft.client.render.model.entity.ChickenModel;
import net.minecraft.client.render.model.entity.CowModel;
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.client.render.model.entity.PigModel;
import net.minecraft.client.render.model.entity.SheepFurModel;
import net.minecraft.client.render.model.entity.SheepModel;
import net.minecraft.client.render.model.entity.SkeletonModel;
import net.minecraft.client.render.model.entity.SlimeModel;
import net.minecraft.client.render.model.entity.ZombieModel;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.entity.decoration.PaintingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.animal.ChickenEntity;
import net.minecraft.entity.mob.animal.CowEntity;
import net.minecraft.entity.mob.animal.PigEntity;
import net.minecraft.entity.mob.animal.SheepEntity;
import net.minecraft.entity.mob.monster.CreeperEntity;
import net.minecraft.entity.mob.monster.GhastEntity;
import net.minecraft.entity.mob.monster.GiantEntity;
import net.minecraft.entity.mob.monster.SkeletonEntity;
import net.minecraft.entity.mob.monster.SlimeEntity;
import net.minecraft.entity.mob.monster.SpiderEntity;
import net.minecraft.entity.mob.monster.ZombieEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class EntityRenderDispatcher {
    private Map renderers = new HashMap();
    public static EntityRenderDispatcher INSTANCE = new EntityRenderDispatcher();
    private TextRenderer textRenderer;
    public static double offsetX;
    public static double offsetY;
    public static double offsetZ;
    public TextureManager textureManager;
    public ItemInHandRenderer itemInHandRenderer;
    public World world;
    public PlayerEntity camera;
    public float cameraYaw;
    public float cameraPitch;
    public GameOptions options;
    public double cameraX;
    public double cameraY;
    public double cameraZ;

    private EntityRenderDispatcher() {
        this.renderers.put(SpiderEntity.class, new SpiderRenderer());
        this.renderers.put(PigEntity.class, new PigRenderer(new PigModel(), new PigModel(0.5f), 0.7f));
        this.renderers.put(SheepEntity.class, new SheepRenderer(new SheepModel(), new SheepFurModel(), 0.7f));
        this.renderers.put(CowEntity.class, new CowRenderer(new CowModel(), 0.7f));
        this.renderers.put(ChickenEntity.class, new ChickenRenderer(new ChickenModel(), 0.3f));
        this.renderers.put(CreeperEntity.class, new CreeperRenderer());
        this.renderers.put(SkeletonEntity.class, new UndeadMobRenderer(new SkeletonModel(), 0.5f));
        this.renderers.put(ZombieEntity.class, new UndeadMobRenderer(new ZombieModel(), 0.5f));
        this.renderers.put(SlimeEntity.class, new SlimeRenderer(new SlimeModel(16), new SlimeModel(0), 0.25f));
        this.renderers.put(PlayerEntity.class, new PlayerRenderer());
        this.renderers.put(GiantEntity.class, new GiantRenderer(new ZombieModel(), 0.5f, 6.0f));
        this.renderers.put(GhastEntity.class, new GhastRenderer());
        this.renderers.put(MobEntity.class, new MobRenderer(new HumanoidModel(), 0.5f));
        this.renderers.put(Entity.class, new DefaultRenderer());
        this.renderers.put(PaintingEntity.class, new PaintingRenderer());
        this.renderers.put(ArrowEntity.class, new ArrowRenderer());
        this.renderers.put(SnowballEntity.class, new SnowballRenderer());
        this.renderers.put(FireballEntity.class, new FireballRenderer());
        this.renderers.put(ItemEntity.class, new ItemRenderer());
        this.renderers.put(PrimedTntEntity.class, new PrimedTntRenderer());
        this.renderers.put(FallingBlockEntity.class, new FallingBlockRenderer());
        this.renderers.put(MinecartEntity.class, new MinecartRenderer());
        this.renderers.put(BoatEntity.class, new BoatRenderer());
        this.renderers.put(FishingBobberEntity.class, new FishingBobberRenderer());
        for (EntityRenderer entityRenderer : this.renderers.values()) {
            entityRenderer.init(this);
        }
    }

    public EntityRenderer getRenderer(Class type) {
        EntityRenderer entityRenderer = (EntityRenderer)this.renderers.get(type);
        if (entityRenderer == null && type != Entity.class) {
            entityRenderer = this.getRenderer(type.getSuperclass());
            this.renderers.put(type, entityRenderer);
        }
        return entityRenderer;
    }

    public EntityRenderer getRenderer(Entity entity) {
        return this.getRenderer(entity.getClass());
    }

    public void prepare(World world, TextureManager textureManager, TextRenderer textRenderer, PlayerEntity camera, GameOptions options, float tickDelta) {
        this.world = world;
        this.textureManager = textureManager;
        this.options = options;
        this.camera = camera;
        this.textRenderer = textRenderer;
        this.cameraYaw = camera.lastYaw + (camera.yaw - camera.lastYaw) * tickDelta;
        this.cameraPitch = camera.lastPitch + (camera.pitch - camera.lastPitch) * tickDelta;
        this.cameraX = camera.prevX + (camera.x - camera.prevX) * (double)tickDelta;
        this.cameraY = camera.prevY + (camera.y - camera.prevY) * (double)tickDelta;
        this.cameraZ = camera.prevZ + (camera.z - camera.prevZ) * (double)tickDelta;
    }

    public void render(Entity entity, float tickDelta) {
        double d = entity.prevX + (entity.x - entity.prevX) * (double)tickDelta;
        double e = entity.prevY + (entity.y - entity.prevY) * (double)tickDelta;
        double f = entity.prevZ + (entity.z - entity.prevZ) * (double)tickDelta;
        float g = entity.lastYaw + (entity.yaw - entity.lastYaw) * tickDelta;
        float h = entity.getBrightness(tickDelta);
        GL11.glColor3f((float)h, (float)h, (float)h);
        this.render(entity, d - offsetX, e - offsetY, f - offsetZ, g, tickDelta);
    }

    public void render(Entity entity, double dx, double dy, double dz, float yaw, float tickDelta) {
        EntityRenderer entityRenderer = this.getRenderer(entity);
        if (entityRenderer != null) {
            entityRenderer.render(entity, dx, dy, dz, yaw, tickDelta);
            entityRenderer.postRender(entity, dx, dy, dz, yaw, tickDelta);
        }
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public double squaredDistanceToCamera(double x, double y, double z) {
        double d = x - this.cameraX;
        double e = y - this.cameraY;
        double f = z - this.cameraZ;
        return d * d + e * e + f * f;
    }

    public TextRenderer getTextRenderer() {
        return this.textRenderer;
    }
}

