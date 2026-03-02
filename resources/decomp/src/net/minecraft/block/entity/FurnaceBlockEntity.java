/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class FurnaceBlockEntity
extends BlockEntity
implements Inventory {
    private ItemStack[] inventory = new ItemStack[3];
    private int fuelTime = 0;
    private int totalFuelTime = 0;
    private int cookTime = 0;

    public int getSize() {
        return this.inventory.length;
    }

    public ItemStack getItem(int slot) {
        return this.inventory[slot];
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack removeItem(int slot, int amount) {
        if (this.inventory[slot] != null) {
            if (this.inventory[slot].size <= amount) {
                ItemStack itemStack = this.inventory[slot];
                this.inventory[slot] = null;
                return itemStack;
            }
            ItemStack itemStack2 = this.inventory[slot].split(amount);
            if (this.inventory[slot].size == 0) {
                this.inventory[slot] = null;
            }
            return itemStack2;
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public void setItem(int slot, ItemStack item) {
        this.inventory[slot] = item;
        if (item != null && item.size > this.getMaxStackSize()) {
            item.size = this.getMaxStackSize();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public String getInventoryName() {
        return "Chest";
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        NbtList nbtList = nbt.getList("Items");
        this.inventory = new ItemStack[this.getSize()];
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = (NbtCompound)nbtList.get(i);
            byte j = nbtCompound.getByte("Slot");
            if (j < 0 || j >= this.inventory.length) continue;
            this.inventory[j] = new ItemStack(nbtCompound);
        }
        this.fuelTime = nbt.getShort("BurnTime");
        this.cookTime = nbt.getShort("CookTime");
        this.totalFuelTime = this.getFuelTime(this.inventory[1]);
    }

    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putShort("BurnTime", (short)this.fuelTime);
        nbt.putShort("CookTime", (short)this.cookTime);
        NbtList nbtList = new NbtList();
        for (int i = 0; i < this.inventory.length; ++i) {
            if (this.inventory[i] == null) continue;
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)i);
            this.inventory[i].writeNbt(nbtCompound);
            nbtList.addElement(nbtCompound);
        }
        nbt.put("Items", nbtList);
    }

    public int getMaxStackSize() {
        return 64;
    }

    @Environment(value=EnvType.CLIENT)
    public int getCookProgress(int range) {
        return this.cookTime * range / 200;
    }

    @Environment(value=EnvType.CLIENT)
    public int getLitProgress(int range) {
        if (this.totalFuelTime == 0) {
            this.totalFuelTime = 200;
        }
        return this.fuelTime * range / this.totalFuelTime;
    }

    public boolean hasFuel() {
        return this.fuelTime > 0;
    }

    public void tick() {
        boolean i = this.fuelTime > 0;
        boolean j = false;
        if (this.fuelTime > 0) {
            --this.fuelTime;
        }
        if (!this.world.isMultiplayer) {
            if (this.fuelTime == 0 && this.canCook()) {
                this.totalFuelTime = this.fuelTime = this.getFuelTime(this.inventory[1]);
                if (this.fuelTime > 0) {
                    j = true;
                    if (this.inventory[1] != null) {
                        --this.inventory[1].size;
                        if (this.inventory[1].size == 0) {
                            this.inventory[1] = null;
                        }
                    }
                }
            }
            if (this.hasFuel() && this.canCook()) {
                ++this.cookTime;
                if (this.cookTime == 200) {
                    this.cookTime = 0;
                    this.finishCooking();
                    j = true;
                }
            } else {
                this.cookTime = 0;
            }
            if (i != this.fuelTime > 0) {
                j = true;
                FurnaceBlock.updateLitState(this.fuelTime > 0, this.world, this.x, this.y, this.z);
            }
        }
        if (j) {
            this.markDirty();
        }
    }

    private boolean canCook() {
        if (this.inventory[0] == null) {
            return false;
        }
        int i = this.getResult(this.inventory[0].getItem().id);
        if (i < 0) {
            return false;
        }
        if (this.inventory[2] == null) {
            return true;
        }
        if (this.inventory[2].id != i) {
            return false;
        }
        if (this.inventory[2].size < this.getMaxStackSize() && this.inventory[2].size < this.inventory[2].getMaxSize()) {
            return true;
        }
        return this.inventory[2].size < Item.BY_ID[i].getMaxStackSize();
    }

    public void finishCooking() {
        if (!this.canCook()) {
            return;
        }
        int i = this.getResult(this.inventory[0].getItem().id);
        if (this.inventory[2] == null) {
            this.inventory[2] = new ItemStack(i, 1);
        } else if (this.inventory[2].id == i) {
            ++this.inventory[2].size;
        }
        --this.inventory[0].size;
        if (this.inventory[0].size <= 0) {
            this.inventory[0] = null;
        }
    }

    private int getResult(int input) {
        if (input == Block.IRON_ORE.id) {
            return Item.IRON_INGOT.id;
        }
        if (input == Block.GOLD_ORE.id) {
            return Item.GOLD_INGOT.id;
        }
        if (input == Block.DIAMOND_ORE.id) {
            return Item.DIAMOND.id;
        }
        if (input == Block.SAND.id) {
            return Block.GLASS.id;
        }
        if (input == Item.PORKCHOP.id) {
            return Item.COOKED_PORKCHOP.id;
        }
        if (input == Item.FISH.id) {
            return Item.COOKED_FISH.id;
        }
        if (input == Block.COBBLESTONE.id) {
            return Block.STONE.id;
        }
        if (input == Item.CLAY.id) {
            return Item.BRICK.id;
        }
        return -1;
    }

    private int getFuelTime(ItemStack item) {
        if (item == null) {
            return 0;
        }
        int i = item.getItem().id;
        if (i < 256 && Block.BY_ID[i].material == Material.WOOD) {
            return 300;
        }
        if (i == Item.STICK.id) {
            return 100;
        }
        if (i == Item.COAL.id) {
            return 1600;
        }
        if (i == Item.LAVA_BUCKET.id) {
            return 20000;
        }
        return 0;
    }
}

