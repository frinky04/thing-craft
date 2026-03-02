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
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class ClientPlayerInteractionManager {
    protected final Minecraft minecraft;
    public boolean creative = false;

    public ClientPlayerInteractionManager(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void initWorld(World world) {
    }

    public void startMiningBlock(int x, int y, int z, int face) {
        this.finishMiningBlock(x, y, z, face);
    }

    public boolean finishMiningBlock(int x, int y, int z, int face) {
        this.minecraft.particleManager.handleBlockBreaking(x, y, z);
        World world = this.minecraft.world;
        Block block = Block.BY_ID[world.getBlock(x, y, z)];
        int i = world.getBlockMetadata(x, y, z);
        boolean j = world.setBlock(x, y, z, 0);
        if (block != null && j) {
            this.minecraft.soundEngine.play(block.sounds.getBreaking(), (float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, (block.sounds.getVolume() + 1.0f) / 2.0f, block.sounds.getPitch() * 0.8f);
            block.onBroken(world, x, y, z, i);
        }
        return j;
    }

    public void tickBlockMining(int x, int y, int z, int face) {
    }

    public void stopMiningBlock() {
    }

    public void render(float tickDelta) {
    }

    public float getReach() {
        return 5.0f;
    }

    public boolean useItem(PlayerEntity player, World world, ItemStack itemInHand) {
        int i = itemInHand.size;
        ItemStack itemStack = itemInHand.startUsing(world, player);
        if (itemStack != itemInHand || itemStack != null && itemStack.size != i) {
            player.inventory.items[player.inventory.selectedSlot] = itemStack;
            if (itemStack.size == 0) {
                player.inventory.items[player.inventory.selectedSlot] = null;
            }
            return true;
        }
        return false;
    }

    public void initPlayer(PlayerEntity player) {
    }

    public void tick() {
    }

    public boolean hasStatusBars() {
        return true;
    }

    public void adjustPlayer(PlayerEntity player) {
    }

    public boolean useBlock(PlayerEntity player, World world, ItemStack itemInHand, int x, int y, int z, int face) {
        int i = world.getBlock(x, y, z);
        if (i > 0 && Block.BY_ID[i].use(world, x, y, z, player)) {
            return true;
        }
        if (itemInHand == null) {
            return false;
        }
        return itemInHand.useOn(player, world, x, y, z, face);
    }

    public PlayerEntity createPlayer(World world) {
        return new ClientPlayerEntity(this.minecraft, world, this.minecraft.session, world.dimension.id);
    }

    public void interactEntity(PlayerEntity player, Entity target) {
        player.interact(target);
    }

    public void attackEntity(PlayerEntity player, Entity target) {
        player.attack(target);
    }
}

