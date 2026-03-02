/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ChestBlock
extends BlockWithBlockEntity {
    private Random random = new Random();

    protected ChestBlock(int id) {
        super(id, Material.WOOD);
        this.sprite = 26;
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(WorldView world, int x, int y, int z, int face) {
        if (face == 1) {
            return this.sprite - 1;
        }
        if (face == 0) {
            return this.sprite - 1;
        }
        int i = world.getBlock(x, y, z - 1);
        int j = world.getBlock(x, y, z + 1);
        int k = world.getBlock(x - 1, y, z);
        int l = world.getBlock(x + 1, y, z);
        if (i == this.id || j == this.id) {
            if (face == 2 || face == 3) {
                return this.sprite;
            }
            int m = 0;
            if (i == this.id) {
                m = -1;
            }
            int p = world.getBlock(x - 1, y, i == this.id ? z - 1 : z + 1);
            int r = world.getBlock(x + 1, y, i == this.id ? z - 1 : z + 1);
            if (face == 4) {
                m = -1 - m;
            }
            int t = 5;
            if ((Block.IS_SOLID[k] || Block.IS_SOLID[p]) && !Block.IS_SOLID[l] && !Block.IS_SOLID[r]) {
                t = 5;
            }
            if ((Block.IS_SOLID[l] || Block.IS_SOLID[r]) && !Block.IS_SOLID[k] && !Block.IS_SOLID[p]) {
                t = 4;
            }
            return (face == t ? this.sprite + 16 : this.sprite + 32) + m;
        }
        if (k == this.id || l == this.id) {
            if (face == 4 || face == 5) {
                return this.sprite;
            }
            int n = 0;
            if (k == this.id) {
                n = -1;
            }
            int q = world.getBlock(k == this.id ? x - 1 : x + 1, y, z - 1);
            int s = world.getBlock(k == this.id ? x - 1 : x + 1, y, z + 1);
            if (face == 3) {
                n = -1 - n;
            }
            int u = 3;
            if ((Block.IS_SOLID[i] || Block.IS_SOLID[q]) && !Block.IS_SOLID[j] && !Block.IS_SOLID[s]) {
                u = 3;
            }
            if ((Block.IS_SOLID[j] || Block.IS_SOLID[s]) && !Block.IS_SOLID[i] && !Block.IS_SOLID[q]) {
                u = 2;
            }
            return (face == u ? this.sprite + 16 : this.sprite + 32) + n;
        }
        int o = 3;
        if (Block.IS_SOLID[i] && !Block.IS_SOLID[j]) {
            o = 3;
        }
        if (Block.IS_SOLID[j] && !Block.IS_SOLID[i]) {
            o = 2;
        }
        if (Block.IS_SOLID[k] && !Block.IS_SOLID[l]) {
            o = 5;
        }
        if (Block.IS_SOLID[l] && !Block.IS_SOLID[k]) {
            o = 4;
        }
        return face == o ? this.sprite + 1 : this.sprite;
    }

    public int getSprite(int face) {
        if (face == 1) {
            return this.sprite - 1;
        }
        if (face == 0) {
            return this.sprite - 1;
        }
        if (face == 3) {
            return this.sprite + 1;
        }
        return this.sprite;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        int i = 0;
        if (world.getBlock(x - 1, y, z) == this.id) {
            ++i;
        }
        if (world.getBlock(x + 1, y, z) == this.id) {
            ++i;
        }
        if (world.getBlock(x, y, z - 1) == this.id) {
            ++i;
        }
        if (world.getBlock(x, y, z + 1) == this.id) {
            ++i;
        }
        if (i > 1) {
            return false;
        }
        if (this.isDoubleChest(world, x - 1, y, z)) {
            return false;
        }
        if (this.isDoubleChest(world, x + 1, y, z)) {
            return false;
        }
        if (this.isDoubleChest(world, x, y, z - 1)) {
            return false;
        }
        return !this.isDoubleChest(world, x, y, z + 1);
    }

    private boolean isDoubleChest(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != this.id) {
            return false;
        }
        if (world.getBlock(x - 1, y, z) == this.id) {
            return true;
        }
        if (world.getBlock(x + 1, y, z) == this.id) {
            return true;
        }
        if (world.getBlock(x, y, z - 1) == this.id) {
            return true;
        }
        return world.getBlock(x, y, z + 1) == this.id;
    }

    public void onRemoved(World world, int x, int y, int z) {
        ChestBlockEntity chestBlockEntity = (ChestBlockEntity)world.getBlockEntity(x, y, z);
        for (int i = 0; i < chestBlockEntity.getSize(); ++i) {
            ItemStack itemStack = chestBlockEntity.getItem(i);
            if (itemStack == null) continue;
            float f = this.random.nextFloat() * 0.8f + 0.1f;
            float g = this.random.nextFloat() * 0.8f + 0.1f;
            float h = this.random.nextFloat() * 0.8f + 0.1f;
            while (itemStack.size > 0) {
                int j = this.random.nextInt(21) + 10;
                if (j > itemStack.size) {
                    j = itemStack.size;
                }
                itemStack.size -= j;
                ItemEntity itemEntity = new ItemEntity(world, (float)x + f, (float)y + g, (float)z + h, new ItemStack(itemStack.id, j, itemStack.metadata));
                float k = 0.05f;
                itemEntity.velocityX = (float)this.random.nextGaussian() * k;
                itemEntity.velocityY = (float)this.random.nextGaussian() * k + 0.2f;
                itemEntity.velocityZ = (float)this.random.nextGaussian() * k;
                world.addEntity(itemEntity);
            }
        }
        super.onRemoved(world, x, y, z);
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        DoubleInventory object;
        ChestBlockEntity chestBlockEntity = (ChestBlockEntity)world.getBlockEntity(x, y, z);
        if (world.isSolidBlock(x, y + 1, z)) {
            return true;
        }
        if (world.getBlock(x - 1, y, z) == this.id && world.isSolidBlock(x - 1, y + 1, z)) {
            return true;
        }
        if (world.getBlock(x + 1, y, z) == this.id && world.isSolidBlock(x + 1, y + 1, z)) {
            return true;
        }
        if (world.getBlock(x, y, z - 1) == this.id && world.isSolidBlock(x, y + 1, z - 1)) {
            return true;
        }
        if (world.getBlock(x, y, z + 1) == this.id && world.isSolidBlock(x, y + 1, z + 1)) {
            return true;
        }
        if (world.getBlock(x - 1, y, z) == this.id) {
            object = new DoubleInventory("Large chest", (ChestBlockEntity)world.getBlockEntity(x - 1, y, z), chestBlockEntity);
        }
        if (world.getBlock(x + 1, y, z) == this.id) {
            object = new DoubleInventory("Large chest", object, (ChestBlockEntity)world.getBlockEntity(x + 1, y, z));
        }
        if (world.getBlock(x, y, z - 1) == this.id) {
            object = new DoubleInventory("Large chest", (ChestBlockEntity)world.getBlockEntity(x, y, z - 1), object);
        }
        if (world.getBlock(x, y, z + 1) == this.id) {
            object = new DoubleInventory("Large chest", object, (ChestBlockEntity)world.getBlockEntity(x, y, z + 1));
        }
        player.openChestMenu(object);
        return true;
    }

    protected BlockEntity createBlockEntity() {
        return new ChestBlockEntity();
    }
}

