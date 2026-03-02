/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.mob.player.LocalClientPlayerEntity;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.PlayerHandActionPacket;
import net.minecraft.network.packet.PlayerHeldItemPacket;
import net.minecraft.network.packet.PlayerInteractEntityPacket;
import net.minecraft.network.packet.PlayerUsePacket;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class MultiplayerInteractionManager
extends ClientPlayerInteractionManager {
    private int targetBlockX = -1;
    private int targetBlockY = -1;
    private int targetBlockZ = -1;
    private float miningProgress = 0.0f;
    private float lastMiningProgress = 0.0f;
    private float miningTicks = 0.0f;
    private int miningCooldown = 0;
    private boolean isMiningBlock = false;
    private ClientNetworkHandler networkHandler;
    private int selectedHotbarSlot = 0;

    public MultiplayerInteractionManager(Minecraft minecraft, ClientNetworkHandler networkHandler) {
        super(minecraft);
        this.networkHandler = networkHandler;
    }

    public void initPlayer(PlayerEntity player) {
        player.yaw = -180.0f;
    }

    public boolean finishMiningBlock(int x, int y, int z, int face) {
        this.networkHandler.sendPacket(new PlayerHandActionPacket(3, x, y, z, face));
        int i = this.minecraft.world.getBlock(x, y, z);
        int j = this.minecraft.world.getBlockMetadata(x, y, z);
        boolean k = super.finishMiningBlock(x, y, z, face);
        ItemStack itemStack = this.minecraft.player.getItemInHand();
        if (itemStack != null) {
            itemStack.mineBlock(i, x, y, z);
            if (itemStack.size == 0) {
                itemStack.onRemoved(this.minecraft.player);
                this.minecraft.player.clearItemInHand();
            }
        }
        return k;
    }

    public void startMiningBlock(int x, int y, int z, int face) {
        this.isMiningBlock = true;
        this.networkHandler.sendPacket(new PlayerHandActionPacket(0, x, y, z, face));
        int i = this.minecraft.world.getBlock(x, y, z);
        if (i > 0 && this.miningProgress == 0.0f) {
            Block.BY_ID[i].startMining(this.minecraft.world, x, y, z, this.minecraft.player);
        }
        if (i > 0 && Block.BY_ID[i].getMiningSpeed(this.minecraft.player) >= 1.0f) {
            this.finishMiningBlock(x, y, z, face);
        }
    }

    public void stopMiningBlock() {
        if (!this.isMiningBlock) {
            return;
        }
        this.isMiningBlock = false;
        this.networkHandler.sendPacket(new PlayerHandActionPacket(2, 0, 0, 0, 0));
        this.miningProgress = 0.0f;
        this.miningCooldown = 0;
    }

    public void tickBlockMining(int x, int y, int z, int face) {
        this.isMiningBlock = true;
        this.sendSelectedHotbarSlot();
        this.networkHandler.sendPacket(new PlayerHandActionPacket(1, x, y, z, face));
        if (this.miningCooldown > 0) {
            --this.miningCooldown;
            return;
        }
        if (x == this.targetBlockX && y == this.targetBlockY && z == this.targetBlockZ) {
            int i = this.minecraft.world.getBlock(x, y, z);
            if (i == 0) {
                return;
            }
            Block block = Block.BY_ID[i];
            this.miningProgress += block.getMiningSpeed(this.minecraft.player);
            if (this.miningTicks % 4.0f == 0.0f && block != null) {
                this.minecraft.soundEngine.play(block.sounds.getStepping(), (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, (block.sounds.getVolume() + 1.0f) / 8.0f, block.sounds.getPitch() * 0.5f);
            }
            this.miningTicks += 1.0f;
            if (this.miningProgress >= 1.0f) {
                this.finishMiningBlock(x, y, z, face);
                this.miningProgress = 0.0f;
                this.lastMiningProgress = 0.0f;
                this.miningTicks = 0.0f;
                this.miningCooldown = 5;
            }
        } else {
            this.miningProgress = 0.0f;
            this.lastMiningProgress = 0.0f;
            this.miningTicks = 0.0f;
            this.targetBlockX = x;
            this.targetBlockY = y;
            this.targetBlockZ = z;
        }
    }

    public void render(float tickDelta) {
        if (this.miningProgress <= 0.0f) {
            this.minecraft.gui.progress = 0.0f;
            this.minecraft.worldRenderer.miningProgress = 0.0f;
        } else {
            float f;
            this.minecraft.gui.progress = f = this.lastMiningProgress + (this.miningProgress - this.lastMiningProgress) * tickDelta;
            this.minecraft.worldRenderer.miningProgress = f;
        }
    }

    public float getReach() {
        return 4.0f;
    }

    public void initWorld(World world) {
        super.initWorld(world);
    }

    public void tick() {
        this.sendSelectedHotbarSlot();
        this.lastMiningProgress = this.miningProgress;
        this.minecraft.soundEngine.tickMusic();
    }

    private void sendSelectedHotbarSlot() {
        ItemStack itemStack = this.minecraft.player.inventory.getSelectedItem();
        int i = 0;
        if (itemStack != null) {
            i = itemStack.id;
        }
        if (i != this.selectedHotbarSlot) {
            this.selectedHotbarSlot = i;
            this.networkHandler.sendPacket(new PlayerHeldItemPacket(0, this.selectedHotbarSlot));
        }
    }

    public boolean useBlock(PlayerEntity player, World world, ItemStack itemInHand, int x, int y, int z, int face) {
        this.sendSelectedHotbarSlot();
        this.networkHandler.sendPacket(new PlayerUsePacket(itemInHand != null ? itemInHand.id : -1, x, y, z, face));
        return super.useBlock(player, world, itemInHand, x, y, z, face);
    }

    public boolean useItem(PlayerEntity player, World world, ItemStack itemInHand) {
        this.sendSelectedHotbarSlot();
        this.networkHandler.sendPacket(new PlayerUsePacket(itemInHand != null ? itemInHand.id : -1, -1, -1, -1, 255));
        return super.useItem(player, world, itemInHand);
    }

    public PlayerEntity createPlayer(World world) {
        return new LocalClientPlayerEntity(this.minecraft, world, this.minecraft.session, this.networkHandler);
    }

    public void attackEntity(PlayerEntity player, Entity target) {
        this.sendSelectedHotbarSlot();
        this.networkHandler.sendPacket(new PlayerInteractEntityPacket(player.networkId, target.networkId, 1));
        player.attack(target);
    }

    public void interactEntity(PlayerEntity player, Entity target) {
        this.sendSelectedHotbarSlot();
        this.networkHandler.sendPacket(new PlayerInteractEntityPacket(player.networkId, target.networkId, 0));
        player.interact(target);
    }
}

