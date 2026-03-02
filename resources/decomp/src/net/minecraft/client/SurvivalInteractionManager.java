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
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class SurvivalInteractionManager
extends ClientPlayerInteractionManager {
    private int targetBlockX = -1;
    private int targetBlockY = -1;
    private int targetBlockZ = -1;
    private float miningProgress = 0.0f;
    private float lastMiningProgress = 0.0f;
    private float miningTicks = 0.0f;
    private int miningCooldown = 0;

    public SurvivalInteractionManager(Minecraft minecraft) {
        super(minecraft);
    }

    public void initPlayer(PlayerEntity player) {
        player.yaw = -180.0f;
    }

    public boolean finishMiningBlock(int x, int y, int z, int face) {
        int i = this.minecraft.world.getBlock(x, y, z);
        int j = this.minecraft.world.getBlockMetadata(x, y, z);
        boolean k = super.finishMiningBlock(x, y, z, face);
        ItemStack itemStack = this.minecraft.player.getItemInHand();
        boolean l = this.minecraft.player.canMineBlock(Block.BY_ID[i]);
        if (itemStack != null) {
            itemStack.mineBlock(i, x, y, z);
            if (itemStack.size == 0) {
                itemStack.onRemoved(this.minecraft.player);
                this.minecraft.player.clearItemInHand();
            }
        }
        if (k && l) {
            Block.BY_ID[i].afterMinedByPlayer(this.minecraft.world, x, y, z, j);
        }
        return k;
    }

    public void startMiningBlock(int x, int y, int z, int face) {
        int i = this.minecraft.world.getBlock(x, y, z);
        if (i > 0 && this.miningProgress == 0.0f) {
            Block.BY_ID[i].startMining(this.minecraft.world, x, y, z, this.minecraft.player);
        }
        if (i > 0 && Block.BY_ID[i].getMiningSpeed(this.minecraft.player) >= 1.0f) {
            this.finishMiningBlock(x, y, z, face);
        }
    }

    public void stopMiningBlock() {
        this.miningProgress = 0.0f;
        this.miningCooldown = 0;
    }

    public void tickBlockMining(int x, int y, int z, int face) {
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
        this.lastMiningProgress = this.miningProgress;
        this.minecraft.soundEngine.tickMusic();
    }
}

