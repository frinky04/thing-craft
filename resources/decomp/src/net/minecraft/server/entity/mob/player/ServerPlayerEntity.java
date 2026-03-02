/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.entity.mob.player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.EntityAnimationPacket;
import net.minecraft.network.packet.EntityPickupPacket;
import net.minecraft.network.packet.ItemPickupPacket;
import net.minecraft.network.packet.PlayerHealthPacket;
import net.minecraft.network.packet.RideEntityPacket;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerPlayerInteractionManager;
import net.minecraft.server.network.handler.ServerPlayNetworkHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

@Environment(value=EnvType.SERVER)
public class ServerPlayerEntity
extends PlayerEntity {
    public ServerPlayNetworkHandler networkHandler;
    public MinecraftServer server;
    public ServerPlayerInteractionManager interactionManager;
    public double trackedX;
    public double trackedZ;
    public List pendingChunks = new LinkedList();
    public Set allLoadedChunks = new HashSet();
    public double lastReceivedY;
    public boolean sneaking = false;
    private int lastHealth = -99999999;
    private int spawnProtectionTicks = 60;

    public ServerPlayerEntity(MinecraftServer server, World world, String name, ServerPlayerInteractionManager interactionManager) {
        super(world);
        int i = world.spawnPointX;
        int j = world.spawnPointZ;
        int k = world.spawnPointY;
        if (!world.dimension.noSky) {
            k = world.getSurfaceHeight(i += this.random.nextInt(20) - 10, j);
            j += this.random.nextInt(20) - 10;
        }
        this.setPositionAndAngles((double)i + 0.5, k, (double)j + 0.5, 0.0f, 0.0f);
        this.server = server;
        this.stepHeight = 0.0f;
        interactionManager.player = this;
        this.name = name;
        this.interactionManager = interactionManager;
        this.eyeHeight = 0.0f;
    }

    public void tick() {
        --this.spawnProtectionTicks;
    }

    public void die(Entity killer) {
        this.inventory.dropAll();
    }

    public boolean takeDamage(Entity source, int amount) {
        if (this.spawnProtectionTicks > 0) {
            return false;
        }
        if (!this.server.pvpEnabled) {
            if (source instanceof PlayerEntity) {
                return false;
            }
            if (source instanceof ArrowEntity) {
                ArrowEntity arrowEntity = (ArrowEntity)source;
                if (arrowEntity.shooter instanceof PlayerEntity) {
                    return false;
                }
            }
        }
        return super.takeDamage(source, amount);
    }

    public void heal(int amount) {
        super.heal(amount);
    }

    public void tickPlayer() {
        ChunkPos chunkPos;
        int i;
        super.tick();
        Object object = null;
        double d = 0.0;
        for (i = 0; i < this.pendingChunks.size(); ++i) {
            ChunkPos chunkPos2 = (ChunkPos)this.pendingChunks.get(i);
            double e = chunkPos2.squaredDistanceToCenter(this);
            if (i != 0 && !(e < d)) continue;
            chunkPos = chunkPos2;
            d = chunkPos2.squaredDistanceToCenter(this);
        }
        if (chunkPos != null) {
            i = 0;
            if (d < 1024.0) {
                i = 1;
            }
            if (this.networkHandler.getBlockDataSendQueueSize() < 2) {
                i = 1;
            }
            if (i != 0) {
                this.pendingChunks.remove(chunkPos);
                this.networkHandler.sendPacket(new WorldChunkPacket(chunkPos.x * 16, 0, chunkPos.z * 16, 16, 128, 16, this.server.world));
                List list = this.server.world.getBlockEntities(chunkPos.x * 16, 0, chunkPos.z * 16, chunkPos.x * 16 + 16, 128, chunkPos.z * 16 + 16);
                for (int j = 0; j < list.size(); ++j) {
                    BlockEntity blockEntity = (BlockEntity)list.get(j);
                    this.networkHandler.sendPacket(new BlockEntityUpdatePacket(blockEntity.x, blockEntity.y, blockEntity.z, blockEntity));
                }
            }
        }
        if (this.health != this.lastHealth) {
            this.networkHandler.sendPacket(new PlayerHealthPacket(this.health));
            this.lastHealth = this.health;
        }
    }

    public void mobTick() {
        this.velocityZ = 0.0;
        this.velocityY = 0.0;
        this.velocityX = 0.0;
        this.jumping = false;
        super.mobTick();
    }

    public void pickUp(Entity item, int count) {
        if (!item.removed) {
            if (item instanceof ItemEntity) {
                this.networkHandler.sendPacket(new ItemPickupPacket(((ItemEntity)item).item, count));
                this.server.entityMap.sendPacket(item, new EntityPickupPacket(item.networkId, this.networkId));
            }
            if (item instanceof ArrowEntity) {
                this.networkHandler.sendPacket(new ItemPickupPacket(new ItemStack(Item.ARROW), 1));
                this.server.entityMap.sendPacket(item, new EntityPickupPacket(item.networkId, this.networkId));
            }
        }
        super.pickUp(item, count);
    }

    public void swingArm() {
        if (!this.swingArm) {
            this.swingArmTicks = -1;
            this.swingArm = true;
            this.server.entityMap.sendPacket(this, new EntityAnimationPacket(this, 1));
        }
    }

    public float getEyeHeight() {
        return 1.62f;
    }

    public void startRiding(Entity entity) {
        super.startRiding(entity);
        this.networkHandler.sendPacket(new RideEntityPacket(this, this.vehicle));
        this.networkHandler.teleport(this.x, this.y, this.z, this.yaw, this.pitch);
    }

    protected void checkFallDamage(double dy, boolean onGround) {
    }

    public void handleFall(double distance, boolean onGround) {
        super.checkFallDamage(distance, onGround);
    }

    public boolean isSneaking() {
        return this.sneaking;
    }
}

