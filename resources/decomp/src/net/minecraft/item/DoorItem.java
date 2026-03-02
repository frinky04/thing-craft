/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class DoorItem
extends Item {
    private Material material;

    public DoorItem(int id, Material material) {
        super(id);
        this.material = material;
        this.maxDamage = 64;
        this.maxStackSize = 1;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        Block block2;
        if (face != 1) {
            return false;
        }
        ++y;
        if (this.material == Material.WOOD) {
            Block block = Block.WOODEN_DOOR;
        } else {
            block2 = Block.IRON_DOOR;
        }
        if (!block2.canBePlaced(world, x, y, z)) {
            return false;
        }
        int i = MathHelper.floor((double)((player.yaw + 180.0f) * 4.0f / 360.0f) - 0.5) & 3;
        int j = 0;
        int k = 0;
        if (i == 0) {
            k = 1;
        }
        if (i == 1) {
            j = -1;
        }
        if (i == 2) {
            k = -1;
        }
        if (i == 3) {
            j = 1;
        }
        int l = (world.isSolidBlock(x - j, y, z - k) ? 1 : 0) + (world.isSolidBlock(x - j, y + 1, z - k) ? 1 : 0);
        int m = (world.isSolidBlock(x + j, y, z + k) ? 1 : 0) + (world.isSolidBlock(x + j, y + 1, z + k) ? 1 : 0);
        boolean n = world.getBlock(x - j, y, z - k) == block2.id || world.getBlock(x - j, y + 1, z - k) == block2.id;
        boolean o = world.getBlock(x + j, y, z + k) == block2.id || world.getBlock(x + j, y + 1, z + k) == block2.id;
        boolean p = false;
        if (n && !o) {
            p = true;
        } else if (m > l) {
            p = true;
        }
        if (p) {
            i = i - 1 & 3;
            i += 4;
        }
        world.setBlock(x, y, z, block2.id);
        world.setBlockMetadata(x, y, z, i);
        world.setBlock(x, y + 1, z, block2.id);
        world.setBlockMetadata(x, y + 1, z, i + 8);
        --stack.size;
        return true;
    }
}

