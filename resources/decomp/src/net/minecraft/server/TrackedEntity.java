/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.entity.SpawnableEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.network.packet.AddEntityPacket;
import net.minecraft.network.packet.AddItemPacket;
import net.minecraft.network.packet.AddMobPacket;
import net.minecraft.network.packet.AddPlayerPacket;
import net.minecraft.network.packet.EntityAnimationPacket;
import net.minecraft.network.packet.EntityMovePacket;
import net.minecraft.network.packet.EntityTeleportPacket;
import net.minecraft.network.packet.EntityVelocityPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.RemoveEntityPacket;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.SERVER)
public class TrackedEntity {
    public Entity entity;
    public boolean riding = false;
    public boolean burning = false;
    public boolean sneaking = false;
    public int trackingRange;
    public int updateInterval;
    public int lastX;
    public int lastY;
    public int lastZ;
    public int lastYaw;
    public int lastPitch;
    public double velocityX;
    public double velocityY;
    public double velocityZ;
    public int ticks = 0;
    private double x;
    private double y;
    private double z;
    private boolean initialized = false;
    private boolean syncVelocity;
    public boolean moved = false;
    public Set players = new HashSet();

    public TrackedEntity(Entity entity, int trackingRange, int updateInterval, boolean syncVelocity) {
        this.entity = entity;
        this.trackingRange = trackingRange;
        this.updateInterval = updateInterval;
        this.syncVelocity = syncVelocity;
        this.lastX = MathHelper.floor(entity.x * 32.0);
        this.lastY = MathHelper.floor(entity.y * 32.0);
        this.lastZ = MathHelper.floor(entity.z * 32.0);
        this.lastYaw = MathHelper.floor(entity.yaw * 256.0f / 360.0f);
        this.lastPitch = MathHelper.floor(entity.pitch * 256.0f / 360.0f);
    }

    public boolean equals(Object object) {
        if (object instanceof TrackedEntity) {
            return ((TrackedEntity)object).entity.networkId == this.entity.networkId;
        }
        return false;
    }

    public int hashCode() {
        return this.entity.networkId;
    }

    public void tick(List players) {
        this.moved = false;
        if (!this.initialized || this.entity.squaredDistanceTo(this.x, this.y, this.z) > 16.0) {
            this.x = this.entity.x;
            this.y = this.entity.y;
            this.z = this.entity.z;
            this.initialized = true;
            this.moved = true;
            this.updatePlayers(players);
        }
        if (this.ticks++ % this.updateInterval == 0) {
            double g;
            double f;
            double e;
            double d;
            double h;
            EntityMovePacket packet;
            int i = MathHelper.floor(this.entity.x * 32.0);
            int j = MathHelper.floor(this.entity.y * 32.0);
            int k = MathHelper.floor(this.entity.z * 32.0);
            int l = MathHelper.floor(this.entity.yaw * 256.0f / 360.0f);
            int m = MathHelper.floor(this.entity.pitch * 256.0f / 360.0f);
            boolean n = i != this.lastX || j != this.lastY || k != this.lastZ;
            boolean o = l != this.lastYaw || m != this.lastPitch;
            int p = i - this.lastX;
            int q = j - this.lastY;
            int r = k - this.lastZ;
            EntityTeleportPacket object = null;
            if (p < -128 || p >= 128 || q < -128 || q >= 128 || r < -128 || r >= 128) {
                object = new EntityTeleportPacket(this.entity.networkId, i, j, k, (byte)l, (byte)m);
            } else if (n && o) {
                EntityMovePacket.PositionAndAngles entityTeleportPacket = new EntityMovePacket.PositionAndAngles(this.entity.networkId, (byte)p, (byte)q, (byte)r, (byte)l, (byte)m);
            } else if (n) {
                EntityMovePacket.Position positionAndAngles = new EntityMovePacket.Position(this.entity.networkId, (byte)p, (byte)q, (byte)r);
            } else if (o) {
                EntityMovePacket.Angles position = new EntityMovePacket.Angles(this.entity.networkId, (byte)l, (byte)m);
            } else {
                packet = new EntityMovePacket(this.entity.networkId);
            }
            if (this.syncVelocity && ((h = (d = this.entity.velocityX - this.velocityX) * d + (e = this.entity.velocityY - this.velocityY) * e + (f = this.entity.velocityZ - this.velocityZ) * f) > (g = 0.02) * g || h > 0.0 && this.entity.velocityX == 0.0 && this.entity.velocityY == 0.0 && this.entity.velocityZ == 0.0)) {
                this.velocityX = this.entity.velocityX;
                this.velocityY = this.entity.velocityY;
                this.velocityZ = this.entity.velocityZ;
                this.sendPacket(new EntityVelocityPacket(this.entity.networkId, this.velocityX, this.velocityY, this.velocityZ));
            }
            if (packet != null) {
                this.sendPacket(packet);
            }
            if (this.riding && this.entity.vehicle == null) {
                this.riding = false;
                this.sendPacketToAll(new EntityAnimationPacket(this.entity, 101));
            } else if (!this.riding && this.entity.vehicle != null) {
                this.riding = true;
                this.sendPacketToAll(new EntityAnimationPacket(this.entity, 100));
            }
            if (this.entity instanceof MobEntity) {
                if (this.sneaking && !this.entity.isSneaking()) {
                    this.sneaking = false;
                    this.sendPacketToAll(new EntityAnimationPacket(this.entity, 105));
                } else if (!this.sneaking && this.entity.isSneaking()) {
                    this.sneaking = true;
                    this.sendPacketToAll(new EntityAnimationPacket(this.entity, 104));
                }
            }
            if (this.burning && this.entity.onFireTimer <= 0) {
                this.burning = false;
                this.sendPacketToAll(new EntityAnimationPacket(this.entity, 103));
            } else if (!this.burning && this.entity.onFireTimer > 0) {
                this.burning = true;
                this.sendPacketToAll(new EntityAnimationPacket(this.entity, 102));
            }
            this.lastX = i;
            this.lastY = j;
            this.lastZ = k;
            this.lastYaw = l;
            this.lastPitch = m;
        }
        if (this.entity.damaged) {
            this.sendPacketToAll(new EntityVelocityPacket(this.entity));
            this.entity.damaged = false;
        }
    }

    public void sendPacket(Packet packet) {
        for (ServerPlayerEntity serverPlayerEntity : this.players) {
            serverPlayerEntity.networkHandler.sendPacket(packet);
        }
    }

    public void sendPacketToAll(Packet packet) {
        this.sendPacket(packet);
        if (this.entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity)this.entity).networkHandler.sendPacket(packet);
        }
    }

    public void onRemoved() {
        this.sendPacket(new RemoveEntityPacket(this.entity.networkId));
    }

    public void removePlayer(ServerPlayerEntity player) {
        if (this.players.contains(player)) {
            this.players.remove(player);
        }
    }

    public void updatePlayer(ServerPlayerEntity player) {
        if (player == this.entity) {
            return;
        }
        double d = player.x - (double)(this.lastX / 32);
        double e = player.z - (double)(this.lastZ / 32);
        if (d >= (double)(-this.trackingRange) && d <= (double)this.trackingRange && e >= (double)(-this.trackingRange) && e <= (double)this.trackingRange) {
            if (!this.players.contains(player)) {
                this.players.add(player);
                player.networkHandler.sendPacket(this.createAddEntityPacket());
                if (this.sneaking) {
                    player.networkHandler.sendPacket(new EntityAnimationPacket(this.entity, 104));
                }
                if (this.riding) {
                    player.networkHandler.sendPacket(new EntityAnimationPacket(this.entity, 100));
                }
                if (this.burning) {
                    player.networkHandler.sendPacket(new EntityAnimationPacket(this.entity, 102));
                }
                if (this.syncVelocity) {
                    player.networkHandler.sendPacket(new EntityVelocityPacket(this.entity.networkId, this.entity.velocityX, this.entity.velocityY, this.entity.velocityZ));
                }
            }
        } else if (this.players.contains(player)) {
            this.players.remove(player);
            player.networkHandler.sendPacket(new RemoveEntityPacket(this.entity.networkId));
        }
    }

    public void updatePlayers(List players) {
        for (int i = 0; i < players.size(); ++i) {
            this.updatePlayer((ServerPlayerEntity)players.get(i));
        }
    }

    private Packet createAddEntityPacket() {
        if (this.entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)this.entity;
            AddItemPacket addItemPacket = new AddItemPacket(itemEntity);
            itemEntity.x = (double)addItemPacket.x / 32.0;
            itemEntity.y = (double)addItemPacket.y / 32.0;
            itemEntity.z = (double)addItemPacket.z / 32.0;
            return addItemPacket;
        }
        if (this.entity instanceof ServerPlayerEntity) {
            return new AddPlayerPacket((PlayerEntity)this.entity);
        }
        if (this.entity instanceof MinecartEntity) {
            MinecartEntity minecartEntity = (MinecartEntity)this.entity;
            if (minecartEntity.type == 0) {
                return new AddEntityPacket(this.entity, 10);
            }
            if (minecartEntity.type == 1) {
                return new AddEntityPacket(this.entity, 11);
            }
            if (minecartEntity.type == 2) {
                return new AddEntityPacket(this.entity, 12);
            }
        }
        if (this.entity instanceof BoatEntity) {
            return new AddEntityPacket(this.entity, 1);
        }
        if (this.entity instanceof SpawnableEntity) {
            return new AddMobPacket((MobEntity)this.entity);
        }
        if (this.entity instanceof FishingBobberEntity) {
            return new AddEntityPacket(this.entity, 90);
        }
        if (this.entity instanceof ArrowEntity) {
            return new AddEntityPacket(this.entity, 60);
        }
        if (this.entity instanceof SnowballEntity) {
            return new AddEntityPacket(this.entity, 61);
        }
        if (this.entity instanceof PrimedTntEntity) {
            return new AddEntityPacket(this.entity, 50);
        }
        throw new IllegalArgumentException("Don't know how to add " + this.entity.getClass() + "!");
    }

    public void onPlayerRespawn(ServerPlayerEntity player) {
        if (this.players.contains(player)) {
            this.players.remove(player);
            player.networkHandler.sendPacket(new RemoveEntityPacket(this.entity.networkId));
        }
    }
}

