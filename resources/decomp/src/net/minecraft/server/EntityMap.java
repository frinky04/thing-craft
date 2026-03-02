/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.entity.SpawnableEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TrackedEntity;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.util.Int2ObjectHashMap;

@Environment(value=EnvType.SERVER)
public class EntityMap {
    private Set entities = new HashSet();
    private Int2ObjectHashMap entitiesById = new Int2ObjectHashMap();
    private MinecraftServer server;
    private int viewDistance;

    public EntityMap(MinecraftServer server) {
        this.server = server;
        this.viewDistance = server.playerManager.getViewDistance();
    }

    public void onEntityAdded(Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            this.startTracking(entity, 512, 2);
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
            for (TrackedEntity trackedEntity : this.entities) {
                if (trackedEntity.entity == serverPlayerEntity) continue;
                trackedEntity.updatePlayer(serverPlayerEntity);
            }
        } else if (entity instanceof FishingBobberEntity) {
            this.startTracking(entity, 64, 5, true);
        } else if (entity instanceof ArrowEntity) {
            this.startTracking(entity, 64, 5, true);
        } else if (entity instanceof SnowballEntity) {
            this.startTracking(entity, 64, 5, true);
        } else if (entity instanceof ItemEntity) {
            this.startTracking(entity, 64, 20, true);
        } else if (entity instanceof MinecartEntity) {
            this.startTracking(entity, 160, 5, true);
        } else if (entity instanceof BoatEntity) {
            this.startTracking(entity, 160, 5, true);
        } else if (entity instanceof SpawnableEntity) {
            this.startTracking(entity, 160, 3);
        } else if (entity instanceof PrimedTntEntity) {
            this.startTracking(entity, 160, 10, true);
        }
    }

    public void startTracking(Entity entity, int trackingRange, int updateInterval) {
        this.startTracking(entity, trackingRange, updateInterval, false);
    }

    public void startTracking(Entity entity, int trackingRange, int updateInterval, boolean syncVelocity) {
        if (trackingRange > this.viewDistance) {
            trackingRange = this.viewDistance;
        }
        if (this.entitiesById.containsKey(entity.networkId)) {
            throw new IllegalStateException("Entity is already tracked!");
        }
        TrackedEntity trackedEntity = new TrackedEntity(entity, trackingRange, updateInterval, syncVelocity);
        this.entities.add(trackedEntity);
        this.entitiesById.put(entity.networkId, trackedEntity);
        trackedEntity.updatePlayers(this.server.world.players);
    }

    public void onEntityRemoved(Entity entity) {
        TrackedEntity trackedEntity;
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
            for (TrackedEntity trackedEntity2 : this.entities) {
                trackedEntity2.removePlayer(serverPlayerEntity);
            }
        }
        if ((trackedEntity = (TrackedEntity)this.entitiesById.remove(entity.networkId)) != null) {
            this.entities.remove(trackedEntity);
            trackedEntity.onRemoved();
        }
    }

    public void tick() {
        ArrayList<ServerPlayerEntity> arrayList = new ArrayList<ServerPlayerEntity>();
        for (TrackedEntity trackedEntity : this.entities) {
            trackedEntity.tick(this.server.world.players);
            if (!trackedEntity.moved || !(trackedEntity.entity instanceof ServerPlayerEntity)) continue;
            arrayList.add((ServerPlayerEntity)trackedEntity.entity);
        }
        for (int i = 0; i < arrayList.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)arrayList.get(i);
            for (TrackedEntity trackedEntity2 : this.entities) {
                if (trackedEntity2.entity == serverPlayerEntity) continue;
                trackedEntity2.updatePlayer(serverPlayerEntity);
            }
        }
    }

    public void sendPacket(Entity entity, Packet packet) {
        TrackedEntity trackedEntity = (TrackedEntity)this.entitiesById.get(entity.networkId);
        if (trackedEntity != null) {
            trackedEntity.sendPacket(packet);
        }
    }

    public void sendPacketToAll(Entity entity, Packet packet) {
        TrackedEntity trackedEntity = (TrackedEntity)this.entitiesById.get(entity.networkId);
        if (trackedEntity != null) {
            trackedEntity.sendPacketToAll(packet);
        }
    }

    public void onPlayerRespawn(ServerPlayerEntity player) {
        for (TrackedEntity trackedEntity : this.entities) {
            trackedEntity.onPlayerRespawn(player);
        }
    }
}

