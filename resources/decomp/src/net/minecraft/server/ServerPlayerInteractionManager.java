/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Environment(value=EnvType.SERVER)
public class ServerPlayerInteractionManager {
    private World world;
    public PlayerEntity player;
    private float f_65209988;
    private float miningProgress = 0.0f;
    private int miningCooldown = 0;
    private float miningTime = 0.0f;
    private int targetX;
    private int targetY;
    private int targetZ;

    public ServerPlayerInteractionManager(World world) {
        this.world = world;
    }

    public void mineBlockInstantly(int x, int y, int z) {
        int i = this.world.getBlock(x, y, z);
        if (i > 0 && this.miningProgress == 0.0f) {
            Block.BY_ID[i].startMining(this.world, x, y, z, this.player);
        }
        if (i > 0 && Block.BY_ID[i].getMiningSpeed(this.player) >= 1.0f) {
            this.tryMineBlock(x, y, z);
        }
    }

    public void stopMiningBlock() {
        this.miningProgress = 0.0f;
        this.miningCooldown = 0;
    }

    public void finishMiningBlock(int x, int y, int z, int face) {
        if (this.miningCooldown > 0) {
            --this.miningCooldown;
            return;
        }
        if (x == this.targetX && y == this.targetY && z == this.targetZ) {
            int i = this.world.getBlock(x, y, z);
            if (i == 0) {
                return;
            }
            Block block = Block.BY_ID[i];
            this.miningProgress += block.getMiningSpeed(this.player);
            this.miningTime += 1.0f;
            if (this.miningProgress >= 1.0f) {
                this.tryMineBlock(x, y, z);
                this.miningProgress = 0.0f;
                this.f_65209988 = 0.0f;
                this.miningTime = 0.0f;
                this.miningCooldown = 5;
            }
        } else {
            this.miningProgress = 0.0f;
            this.f_65209988 = 0.0f;
            this.miningTime = 0.0f;
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
        }
    }

    public boolean mineBlock(int x, int y, int z) {
        Block block = Block.BY_ID[this.world.getBlock(x, y, z)];
        int i = this.world.getBlockMetadata(x, y, z);
        boolean j = this.world.setBlock(x, y, z, 0);
        if (block != null && j) {
            block.onBroken(this.world, x, y, z, i);
        }
        return j;
    }

    public boolean tryMineBlock(int x, int y, int z) {
        int i = this.world.getBlock(x, y, z);
        int j = this.world.getBlockMetadata(x, y, z);
        boolean k = this.mineBlock(x, y, z);
        ItemStack itemStack = this.player.getItemInHand();
        if (itemStack != null) {
            itemStack.mineBlock(i, x, y, z);
            if (itemStack.size == 0) {
                itemStack.onRemoved(this.player);
                this.player.clearItemInHand();
            }
        }
        if (k && this.player.canMineBlock(Block.BY_ID[i])) {
            Block.BY_ID[i].afterMinedByPlayer(this.world, x, y, z, j);
        }
        return k;
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
}

