/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.network.handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MultiplayerInteractionManager;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.entity.mob.player.RemoteClientPlayerEntity;
import net.minecraft.client.entity.particle.EntityPickupParticle;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.world.MultiplayerWorld;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.AddEntityPacket;
import net.minecraft.network.packet.AddItemPacket;
import net.minecraft.network.packet.AddMobPacket;
import net.minecraft.network.packet.AddPlayerPacket;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.BlockUpdatePacket;
import net.minecraft.network.packet.BlocksUpdatePacket;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.DisconnectPacket;
import net.minecraft.network.packet.EntityAnimationPacket;
import net.minecraft.network.packet.EntityEventPacket;
import net.minecraft.network.packet.EntityMovePacket;
import net.minecraft.network.packet.EntityPickupPacket;
import net.minecraft.network.packet.EntityTeleportPacket;
import net.minecraft.network.packet.EntityVelocityPacket;
import net.minecraft.network.packet.ExplosionPacket;
import net.minecraft.network.packet.HandshakePacket;
import net.minecraft.network.packet.ItemPickupPacket;
import net.minecraft.network.packet.LoadWorldChunkPacket;
import net.minecraft.network.packet.LoginPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PlayerHealthPacket;
import net.minecraft.network.packet.PlayerHeldItemPacket;
import net.minecraft.network.packet.PlayerInventoryPacket;
import net.minecraft.network.packet.PlayerMovePacket;
import net.minecraft.network.packet.PlayerRespawnPacket;
import net.minecraft.network.packet.RemoveEntityPacket;
import net.minecraft.network.packet.RideEntityPacket;
import net.minecraft.network.packet.SpawnPointPacket;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.network.packet.WorldTimePacket;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.explosion.Explosion;

@Environment(value=EnvType.CLIENT)
public class ClientNetworkHandler
extends PacketHandler {
    private boolean disconnected = false;
    private Connection connection;
    public String message;
    private Minecraft minecraft;
    private MultiplayerWorld world;
    private boolean started = false;
    Random random = new Random();

    public ClientNetworkHandler(Minecraft minecraft, String address, int port) {
        this.minecraft = minecraft;
        Socket socket = new Socket(InetAddress.getByName(address), port);
        this.connection = new Connection(socket, "Client", this);
    }

    public void tick() {
        if (this.disconnected) {
            return;
        }
        this.connection.tick();
    }

    public void handleLogin(LoginPacket packet) {
        this.minecraft.interactionManager = new MultiplayerInteractionManager(this.minecraft, this);
        this.world = new MultiplayerWorld(this, packet.seed, packet.dimension);
        this.world.isMultiplayer = true;
        this.minecraft.setWorld(this.world);
        this.minecraft.openScreen(new DownloadingTerrainScreen(this));
        this.minecraft.player.networkId = packet.gameVersion;
        System.out.println("clientEntityId: " + packet.gameVersion);
    }

    public void handleAddItem(AddItemPacket packet) {
        double d = (double)packet.x / 32.0;
        double e = (double)packet.y / 32.0;
        double f = (double)packet.z / 32.0;
        ItemEntity itemEntity = new ItemEntity(this.world, d, e, f, new ItemStack(packet.item, packet.stackSize));
        itemEntity.velocityX = (double)packet.velocityX / 128.0;
        itemEntity.velocityY = (double)packet.velocityY / 128.0;
        itemEntity.velocityZ = (double)packet.velocityZ / 128.0;
        itemEntity.lastKnownX = packet.x;
        itemEntity.lastKnownY = packet.y;
        itemEntity.lastKnownZ = packet.z;
        this.world.forceEntity(packet.id, itemEntity);
    }

    public void handleAddEntity(AddEntityPacket packet) {
        Entity entity;
        MinecartEntity minecartEntity;
        double d = (double)packet.x / 32.0;
        double e = (double)packet.y / 32.0;
        double f = (double)packet.z / 32.0;
        Object object = null;
        if (packet.type == 10) {
            minecartEntity = new MinecartEntity(this.world, d, e, f, 0);
        }
        if (packet.type == 11) {
            minecartEntity = new MinecartEntity(this.world, d, e, f, 1);
        }
        if (packet.type == 12) {
            minecartEntity = new MinecartEntity(this.world, d, e, f, 2);
        }
        if (packet.type == 90) {
            entity = new FishingBobberEntity(this.world, d, e, f);
        }
        if (packet.type == 60) {
            entity = new ArrowEntity(this.world, d, e, f);
        }
        if (packet.type == 61) {
            entity = new SnowballEntity(this.world, d, e, f);
        }
        if (packet.type == 1) {
            entity = new BoatEntity(this.world, d, e, f);
        }
        if (packet.type == 50) {
            entity = new PrimedTntEntity(this.world, d, e, f);
        }
        if (entity != null) {
            entity.lastKnownX = packet.x;
            entity.lastKnownY = packet.y;
            entity.lastKnownZ = packet.z;
            entity.yaw = 0.0f;
            entity.pitch = 0.0f;
            entity.networkId = packet.id;
            this.world.forceEntity(packet.id, entity);
        }
    }

    public void handleEntityVelocity(EntityVelocityPacket packet) {
        Entity entity = this.getEntity(packet.id);
        if (entity == null) {
            return;
        }
        entity.lerpVelocity((double)packet.velocityX / 8000.0, (double)packet.velocityY / 8000.0, (double)packet.velocityZ / 8000.0);
    }

    public void handleAddPlayer(AddPlayerPacket packet) {
        double d = (double)packet.x / 32.0;
        double e = (double)packet.y / 32.0;
        double f = (double)packet.z / 32.0;
        float g = (float)(packet.yaw * 360) / 256.0f;
        float h = (float)(packet.pitch * 360) / 256.0f;
        RemoteClientPlayerEntity remoteClientPlayerEntity = new RemoteClientPlayerEntity(this.minecraft.world, packet.name);
        remoteClientPlayerEntity.lastKnownX = packet.x;
        remoteClientPlayerEntity.lastKnownY = packet.y;
        remoteClientPlayerEntity.lastKnownZ = packet.z;
        int i = packet.itemInHand;
        remoteClientPlayerEntity.inventory.items[remoteClientPlayerEntity.inventory.selectedSlot] = i == 0 ? null : new ItemStack(i);
        remoteClientPlayerEntity.updatePositionAndAngles(d, e, f, g, h);
        this.world.forceEntity(packet.id, remoteClientPlayerEntity);
    }

    public void handleEntityTeleport(EntityTeleportPacket packet) {
        Entity entity = this.getEntity(packet.id);
        if (entity == null) {
            return;
        }
        entity.lastKnownX = packet.x;
        entity.lastKnownY = packet.y;
        entity.lastKnownZ = packet.z;
        double d = (double)entity.lastKnownX / 32.0;
        double e = (double)entity.lastKnownY / 32.0 + 0.015625;
        double f = (double)entity.lastKnownZ / 32.0;
        float g = (float)(packet.yaw * 360) / 256.0f;
        float h = (float)(packet.pitch * 360) / 256.0f;
        entity.lerpPositionAndAngles(d, e, f, g, h, 3);
    }

    public void handleEntityMove(EntityMovePacket packet) {
        Entity entity = this.getEntity(packet.id);
        if (entity == null) {
            return;
        }
        entity.lastKnownX += packet.dx;
        entity.lastKnownY += packet.dy;
        entity.lastKnownZ += packet.dz;
        double d = (double)entity.lastKnownX / 32.0;
        double e = (double)entity.lastKnownY / 32.0 + 0.015625;
        double f = (double)entity.lastKnownZ / 32.0;
        float g = packet.hasAngles ? (float)(packet.yaw * 360) / 256.0f : entity.yaw;
        float h = packet.hasAngles ? (float)(packet.pitch * 360) / 256.0f : entity.pitch;
        entity.lerpPositionAndAngles(d, e, f, g, h, 3);
    }

    public void handleRemoveEntity(RemoveEntityPacket packet) {
        this.world.removeEntity(packet.id);
    }

    public void handlePlayerMove(PlayerMovePacket packet) {
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        double d = clientPlayerEntity.x;
        double e = clientPlayerEntity.y;
        double f = clientPlayerEntity.z;
        float g = clientPlayerEntity.yaw;
        float h = clientPlayerEntity.pitch;
        if (packet.hasPos) {
            d = packet.x;
            e = packet.minY;
            f = packet.z;
        }
        if (packet.hasAngles) {
            g = packet.yaw;
            h = packet.pitch;
        }
        clientPlayerEntity.eyeHeightSneakOffset = 0.0f;
        clientPlayerEntity.velocityZ = 0.0;
        clientPlayerEntity.velocityY = 0.0;
        clientPlayerEntity.velocityX = 0.0;
        clientPlayerEntity.updatePositionAndAngles(d, e, f, g, h);
        packet.x = clientPlayerEntity.x;
        packet.minY = clientPlayerEntity.shape.minY;
        packet.z = clientPlayerEntity.z;
        packet.y = clientPlayerEntity.y;
        this.connection.send(packet);
        if (!this.started) {
            this.minecraft.player.lastX = this.minecraft.player.x;
            this.minecraft.player.lastY = this.minecraft.player.y;
            this.minecraft.player.lastZ = this.minecraft.player.z;
            this.started = true;
            this.minecraft.openScreen(null);
        }
    }

    public void handleLoadWorldChunk(LoadWorldChunkPacket packet) {
        this.world.updateChunk(packet.chunkX, packet.chunkZ, packet.load);
    }

    public void handleBlocksUpdate(BlocksUpdatePacket packet) {
        WorldChunk worldChunk = this.world.getChunkAt(packet.chunkX, packet.chunkZ);
        int i = packet.chunkX * 16;
        int j = packet.chunkZ * 16;
        for (int k = 0; k < packet.blockChangeCount; ++k) {
            short l = packet.positions[k];
            int m = packet.blockIds[k] & 0xFF;
            byte n = packet.blockMetadata[k];
            int o = l >> 12 & 0xF;
            int p = l >> 8 & 0xF;
            int q = l & 0xFF;
            worldChunk.setBlockWithMetadataAt(o, q, p, m, n);
            this.world.clearBlockResets(o + i, q, p + j, o + i, q, p + j);
            this.world.notifyRegionChanged(o + i, q, p + j, o + i, q, p + j);
        }
    }

    public void handleWorldChunk(WorldChunkPacket packet) {
        this.world.clearBlockResets(packet.x, packet.y, packet.z, packet.x + packet.sizeX - 1, packet.y + packet.sizeY - 1, packet.z + packet.sizeZ - 1);
        this.world.unpackChunkData(packet.x, packet.y, packet.z, packet.sizeX, packet.sizeY, packet.sizeZ, packet.data);
    }

    public void handleBlockUpdate(BlockUpdatePacket packet) {
        this.world.setBlockWithMetadataFromPacket(packet.x, packet.y, packet.z, packet.block, packet.metadata);
    }

    public void handleDisconnect(DisconnectPacket packet) {
        this.connection.close("Got kicked");
        this.disconnected = true;
        this.minecraft.setWorld(null);
        this.minecraft.openScreen(new DisconnectedScreen("Disconnected by server", packet.reason));
    }

    public void onDisconnect(String reason) {
        if (this.disconnected) {
            return;
        }
        this.disconnected = true;
        this.minecraft.setWorld(null);
        this.minecraft.openScreen(new DisconnectedScreen("Connection lost", reason));
    }

    public void sendPacket(Packet packet) {
        if (this.disconnected) {
            return;
        }
        this.connection.send(packet);
    }

    public void handleEntityPickup(EntityPickupPacket packet) {
        Entity entity = this.getEntity(packet.id);
        MobEntity mobEntity = (MobEntity)this.getEntity(packet.collectorId);
        if (mobEntity == null) {
            mobEntity = this.minecraft.player;
        }
        if (entity != null) {
            this.world.playSound(entity, "random.pop", 0.2f, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            this.minecraft.particleManager.add(new EntityPickupParticle(this.minecraft.world, entity, mobEntity, -0.5f));
            this.world.removeEntity(packet.id);
        }
    }

    public void handlePlayerHeldItem(PlayerHeldItemPacket packet) {
        Entity entity = this.getEntity(packet.id);
        if (entity == null) {
            return;
        }
        PlayerEntity playerEntity = (PlayerEntity)entity;
        int i = packet.item;
        playerEntity.inventory.items[playerEntity.inventory.selectedSlot] = i == 0 ? null : new ItemStack(i);
    }

    public void handleChatMessage(ChatMessagePacket packet) {
        this.minecraft.gui.addChatMessage(packet.message);
    }

    public void handleEntityAnimation(EntityAnimationPacket packet) {
        Entity entity = this.getEntity(packet.id);
        if (entity == null) {
            return;
        }
        if (packet.action == 1) {
            PlayerEntity playerEntity = (PlayerEntity)entity;
            playerEntity.swingArm();
        } else if (packet.action == 100) {
            entity.hasVehicle = true;
        } else if (packet.action == 101) {
            entity.hasVehicle = false;
        } else if (packet.action == 102) {
            entity.onFire = true;
        } else if (packet.action == 103) {
            entity.onFire = false;
        } else if (packet.action == 104) {
            entity.sneaking = true;
        } else if (packet.action == 105) {
            entity.sneaking = false;
        } else if (packet.action == 2) {
            entity.animateDamage();
        }
    }

    public void handleItemPickup(ItemPickupPacket packet) {
        this.minecraft.player.inventory.addItem(new ItemStack(packet.id, packet.count, packet.metadata));
    }

    public void handleHandshake(HandshakePacket packet) {
        if (packet.key.equals("-")) {
            this.sendPacket(new LoginPacket(this.minecraft.session.username, "Password", 6));
        } else {
            try {
                URL uRL = new URL("http://www.minecraft.net/game/joinserver.jsp?user=" + this.minecraft.session.username + "&sessionId=" + this.minecraft.session.id + "&serverId=" + packet.key);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uRL.openStream()));
                String string = bufferedReader.readLine();
                bufferedReader.close();
                if (string.equalsIgnoreCase("ok")) {
                    this.sendPacket(new LoginPacket(this.minecraft.session.username, "Password", 6));
                } else {
                    this.connection.close("Failed to login: " + string);
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
                this.connection.close("Internal client error: " + exception.toString());
            }
        }
    }

    public void disconnect() {
        this.disconnected = true;
        this.connection.close("Closed");
    }

    public void handleAddMob(AddMobPacket packet) {
        double d = (double)packet.x / 32.0;
        double e = (double)packet.y / 32.0;
        double f = (double)packet.z / 32.0;
        float g = (float)(packet.yaw * 360) / 256.0f;
        float h = (float)(packet.pitch * 360) / 256.0f;
        MobEntity mobEntity = (MobEntity)Entities.create(packet.type, this.minecraft.world);
        mobEntity.lastKnownX = packet.x;
        mobEntity.lastKnownY = packet.y;
        mobEntity.lastKnownZ = packet.z;
        mobEntity.networkId = packet.id;
        mobEntity.updatePositionAndAngles(d, e, f, g, h);
        mobEntity.interpolateOnly = true;
        this.world.forceEntity(packet.id, mobEntity);
    }

    public void handleWorldTime(WorldTimePacket packet) {
        this.minecraft.world.setTime(packet.time);
    }

    public void handlePlayerInventory(PlayerInventoryPacket packet) {
        ClientPlayerEntity clientPlayerEntity = this.minecraft.player;
        if (packet.type == -1) {
            clientPlayerEntity.inventory.items = packet.items;
        }
        if (packet.type == -2) {
            clientPlayerEntity.inventory.crafting = packet.items;
        }
        if (packet.type == -3) {
            clientPlayerEntity.inventory.armor = packet.items;
        }
    }

    public void handleBlockEntityUpdate(BlockEntityUpdatePacket packet) {
        if (packet.nbt.getInt("x") != packet.x) {
            return;
        }
        if (packet.nbt.getInt("y") != packet.y) {
            return;
        }
        if (packet.nbt.getInt("z") != packet.z) {
            return;
        }
        BlockEntity blockEntity = this.world.getBlockEntity(packet.x, packet.y, packet.z);
        if (blockEntity != null) {
            try {
                blockEntity.readNbt(packet.nbt);
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.world.notifyRegionChanged(packet.x, packet.y, packet.z, packet.x, packet.y, packet.z);
        }
    }

    public void handleSpawnPoint(SpawnPointPacket packet) {
        this.world.spawnPointX = packet.x;
        this.world.spawnPointY = packet.y;
        this.world.spawnPointZ = packet.z;
    }

    public void handleRideEntity(RideEntityPacket packet) {
        Entity entity = this.getEntity(packet.id);
        Entity entity2 = this.getEntity(packet.mountId);
        if (packet.id == this.minecraft.player.networkId) {
            entity = this.minecraft.player;
        }
        if (entity == null) {
            return;
        }
        entity.startRiding(entity2);
    }

    public void handleEntityEvent(EntityEventPacket packet) {
        Entity entity = this.getEntity(packet.id);
        if (entity != null) {
            entity.doEvent(packet.event);
        }
    }

    private Entity getEntity(int networkId) {
        if (networkId == this.minecraft.player.networkId) {
            return this.minecraft.player;
        }
        return this.world.getEntity(networkId);
    }

    public void handlePlayerHealth(PlayerHealthPacket packet) {
        this.minecraft.player.damageTo(packet.health);
    }

    public void handlePlayerRespawn(PlayerRespawnPacket packet) {
        this.minecraft.respawnPlayer();
    }

    public void handleExplosion(ExplosionPacket packet) {
        Explosion explosion = new Explosion(this.minecraft.world, null, packet.x, packet.y, packet.z, packet.power);
        explosion.damagedBlocks = packet.damagedBlocks;
        explosion.damageBlocks();
    }
}

