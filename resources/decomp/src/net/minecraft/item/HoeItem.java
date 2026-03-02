/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class HoeItem
extends Item {
    public HoeItem(int id, int tier) {
        super(id);
        this.maxStackSize = 1;
        this.maxDamage = 32 << tier;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        int i = world.getBlock(x, y, z);
        Material material = world.getMaterial(x, y + 1, z);
        if (!material.isSolid() && i == Block.GRASS.id || i == Block.DIRT.id) {
            Block block = Block.FARMLAND;
            world.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, block.sounds.getStepping(), (block.sounds.getVolume() + 1.0f) / 2.0f, block.sounds.getPitch() * 0.8f);
            if (world.isMultiplayer) {
                return true;
            }
            world.setBlock(x, y, z, block.id);
            stack.takeDamageAndBreak(1);
            if (world.random.nextInt(8) == 0 && i == Block.GRASS.id) {
                int j = 1;
                for (int k = 0; k < j; ++k) {
                    float f = 0.7f;
                    float g = world.random.nextFloat() * f + (1.0f - f) * 0.5f;
                    float h = 1.2f;
                    float l = world.random.nextFloat() * f + (1.0f - f) * 0.5f;
                    ItemEntity itemEntity = new ItemEntity(world, (float)x + g, (float)y + h, (float)z + l, new ItemStack(Item.WHEAT_SEEDS));
                    itemEntity.pickUpDelay = 10;
                    world.addEntity(itemEntity);
                }
            }
            return true;
        }
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isHandheld() {
        return true;
    }
}

