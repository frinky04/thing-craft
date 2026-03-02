/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network;

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
import net.minecraft.network.packet.PlayerHandActionPacket;
import net.minecraft.network.packet.PlayerHealthPacket;
import net.minecraft.network.packet.PlayerHeldItemPacket;
import net.minecraft.network.packet.PlayerInteractEntityPacket;
import net.minecraft.network.packet.PlayerInventoryPacket;
import net.minecraft.network.packet.PlayerMovePacket;
import net.minecraft.network.packet.PlayerRespawnPacket;
import net.minecraft.network.packet.PlayerUsePacket;
import net.minecraft.network.packet.RemoveEntityPacket;
import net.minecraft.network.packet.RideEntityPacket;
import net.minecraft.network.packet.SpawnPointPacket;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.network.packet.WorldTimePacket;

public class PacketHandler {
    public void handleWorldChunk(WorldChunkPacket packet) {
    }

    public void handleUnknownPacket(Packet packet) {
    }

    public void onDisconnect(String reason) {
    }

    public void handleDisconnect(DisconnectPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleLogin(LoginPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerMove(PlayerMovePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleBlocksUpdate(BlocksUpdatePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerHandAction(PlayerHandActionPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleBlockUpdate(BlockUpdatePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleLoadWorldChunk(LoadWorldChunkPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleAddPlayer(AddPlayerPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleEntityMove(EntityMovePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleEntityTeleport(EntityTeleportPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerUse(PlayerUsePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerHeldItem(PlayerHeldItemPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleRemoveEntity(RemoveEntityPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleAddItem(AddItemPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleEntityPickup(EntityPickupPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleChatMessage(ChatMessagePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleItemPickup(ItemPickupPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleAddEntity(AddEntityPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleEntityAnimation(EntityAnimationPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleHandshake(HandshakePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleAddMob(AddMobPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleWorldTime(WorldTimePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerInventory(PlayerInventoryPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleBlockEntityUpdate(BlockEntityUpdatePacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleSpawnPoint(SpawnPointPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleEntityVelocity(EntityVelocityPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleRideEntity(RideEntityPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleInteractEntity(PlayerInteractEntityPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleEntityEvent(EntityEventPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerHealth(PlayerHealthPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handlePlayerRespawn(PlayerRespawnPacket packet) {
        this.handleUnknownPacket(packet);
    }

    public void handleExplosion(ExplosionPacket packet) {
        this.handleUnknownPacket(packet);
    }
}

