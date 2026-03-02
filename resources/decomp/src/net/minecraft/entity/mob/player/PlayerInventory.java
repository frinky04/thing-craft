/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.mob.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class PlayerInventory
implements Inventory {
    public ItemStack[] items = new ItemStack[37];
    public ItemStack[] armor = new ItemStack[4];
    public ItemStack[] crafting = new ItemStack[4];
    public int selectedSlot = 0;
    private PlayerEntity player;
    @Environment(value=EnvType.CLIENT)
    public ItemStack cursorItem;
    public boolean dirty = false;

    public PlayerInventory(PlayerEntity player) {
        this.player = player;
    }

    public ItemStack getSelectedItem() {
        return this.items[this.selectedSlot];
    }

    private int getSlot(int item) {
        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null || this.items[i].id != item) continue;
            return i;
        }
        return -1;
    }

    private int getSlotWithSpace(int item) {
        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null || this.items[i].id != item || this.items[i].size >= this.items[i].getMaxSize() || this.items[i].size >= this.getMaxStackSize()) continue;
            return i;
        }
        return -1;
    }

    private int getEmptySlot() {
        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] != null) continue;
            return i;
        }
        return -1;
    }

    @Environment(value=EnvType.CLIENT)
    public void selectSlot(int block, boolean creative) {
        int i = this.getSlot(block);
        if (i >= 0 && i < 9) {
            this.selectedSlot = i;
            return;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public void scrollInHotbar(int amount) {
        if (amount > 0) {
            amount = 1;
        }
        if (amount < 0) {
            amount = -1;
        }
        this.selectedSlot -= amount;
        while (this.selectedSlot < 0) {
            this.selectedSlot += 9;
        }
        while (this.selectedSlot >= 9) {
            this.selectedSlot -= 9;
        }
    }

    private int addItemStack(int item, int amount) {
        int j;
        int i = this.getSlotWithSpace(item);
        if (i < 0) {
            i = this.getEmptySlot();
        }
        if (i < 0) {
            return amount;
        }
        if (this.items[i] == null) {
            this.items[i] = new ItemStack(item, 0);
        }
        if ((j = amount) > this.items[i].getMaxSize() - this.items[i].size) {
            j = this.items[i].getMaxSize() - this.items[i].size;
        }
        if (j > this.getMaxStackSize() - this.items[i].size) {
            j = this.getMaxStackSize() - this.items[i].size;
        }
        if (j == 0) {
            return amount;
        }
        this.items[i].size += j;
        this.items[i].popAnimationTime = 5;
        return amount -= j;
    }

    public void tick() {
        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null || this.items[i].popAnimationTime <= 0) continue;
            --this.items[i].popAnimationTime;
        }
    }

    public boolean removeOne(int item) {
        int i = this.getSlot(item);
        if (i < 0) {
            return false;
        }
        if (--this.items[i].size <= 0) {
            this.items[i] = null;
        }
        return true;
    }

    public boolean addItem(ItemStack item) {
        int i;
        if (item.metadata == 0) {
            item.size = this.addItemStack(item.id, item.size);
            if (item.size == 0) {
                return true;
            }
        }
        if ((i = this.getEmptySlot()) >= 0) {
            this.items[i] = item;
            this.items[i].popAnimationTime = 5;
            return true;
        }
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack removeItem(int slot, int amount) {
        ItemStack[] itemStacks = this.items;
        if (slot >= this.items.length) {
            itemStacks = this.armor;
            slot -= this.items.length;
        }
        if (itemStacks[slot] != null) {
            if (itemStacks[slot].size <= amount) {
                ItemStack itemStack = itemStacks[slot];
                itemStacks[slot] = null;
                return itemStack;
            }
            ItemStack itemStack2 = itemStacks[slot].split(amount);
            if (itemStacks[slot].size == 0) {
                itemStacks[slot] = null;
            }
            return itemStack2;
        }
        return null;
    }

    public void setItem(int slot, ItemStack item) {
        ItemStack[] itemStacks = this.items;
        if (slot >= itemStacks.length) {
            slot -= itemStacks.length;
            itemStacks = this.armor;
        }
        if (slot >= itemStacks.length) {
            slot -= itemStacks.length;
            itemStacks = this.crafting;
        }
        itemStacks[slot] = item;
    }

    public float getMiningSpeed(Block block) {
        float f = 1.0f;
        if (this.items[this.selectedSlot] != null) {
            f *= this.items[this.selectedSlot].getMiningSpeed(block);
        }
        return f;
    }

    public NbtList writeNbt(NbtList list) {
        int i;
        for (i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null) continue;
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)i);
            this.items[i].writeNbt(nbtCompound);
            list.addElement(nbtCompound);
        }
        for (i = 0; i < this.armor.length; ++i) {
            if (this.armor[i] == null) continue;
            NbtCompound nbtCompound2 = new NbtCompound();
            nbtCompound2.putByte("Slot", (byte)(i + 100));
            this.armor[i].writeNbt(nbtCompound2);
            list.addElement(nbtCompound2);
        }
        for (i = 0; i < this.crafting.length; ++i) {
            if (this.crafting[i] == null) continue;
            NbtCompound nbtCompound3 = new NbtCompound();
            nbtCompound3.putByte("Slot", (byte)(i + 80));
            this.crafting[i].writeNbt(nbtCompound3);
            list.addElement(nbtCompound3);
        }
        return list;
    }

    public void readNbt(NbtList list) {
        this.items = new ItemStack[36];
        this.armor = new ItemStack[4];
        this.crafting = new ItemStack[4];
        for (int i = 0; i < list.size(); ++i) {
            NbtCompound nbtCompound = (NbtCompound)list.get(i);
            int j = nbtCompound.getByte("Slot") & 0xFF;
            if (j >= 0 && j < this.items.length) {
                this.items[j] = new ItemStack(nbtCompound);
            }
            if (j >= 80 && j < this.crafting.length + 80) {
                this.crafting[j - 80] = new ItemStack(nbtCompound);
            }
            if (j < 100 || j >= this.armor.length + 100) continue;
            this.armor[j - 100] = new ItemStack(nbtCompound);
        }
    }

    public int getSize() {
        return this.items.length + 4;
    }

    public ItemStack getItem(int slot) {
        ItemStack[] itemStacks = this.items;
        if (slot >= itemStacks.length) {
            slot -= itemStacks.length;
            itemStacks = this.armor;
        }
        if (slot >= itemStacks.length) {
            slot -= itemStacks.length;
            itemStacks = this.crafting;
        }
        return itemStacks[slot];
    }

    @Environment(value=EnvType.CLIENT)
    public String getInventoryName() {
        return "Inventory";
    }

    public int getMaxStackSize() {
        return 64;
    }

    public int getAttackDamage(Entity target) {
        ItemStack itemStack = this.getItem(this.selectedSlot);
        if (itemStack != null) {
            return itemStack.getAttackDamage(target);
        }
        return 1;
    }

    public boolean canMineBlock(Block block) {
        if (block.material != Material.STONE && block.material != Material.IRON && block.material != Material.SNOW && block.material != Material.SNOW_LAYER) {
            return true;
        }
        ItemStack itemStack = this.getItem(this.selectedSlot);
        if (itemStack != null) {
            return itemStack.canMineBlock(block);
        }
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack getArmor(int slot) {
        return this.armor[slot];
    }

    public int getArmorProtection() {
        int i = 0;
        int j = 0;
        int k = 0;
        for (int l = 0; l < this.armor.length; ++l) {
            if (this.armor[l] == null || !(this.armor[l].getItem() instanceof ArmorItem)) continue;
            int m = this.armor[l].getMaxDamage();
            int n = this.armor[l].metadata;
            int o = m - n;
            j += o;
            k += m;
            int p = ((ArmorItem)this.armor[l].getItem()).protection;
            i += p;
        }
        if (k == 0) {
            return 0;
        }
        return (i - 1) * j / k + 1;
    }

    public void damageArmor(int damage) {
        for (int i = 0; i < this.armor.length; ++i) {
            if (this.armor[i] == null || !(this.armor[i].getItem() instanceof ArmorItem)) continue;
            this.armor[i].takeDamageAndBreak(damage);
            if (this.armor[i].size != 0) continue;
            this.armor[i].onRemoved(this.player);
            this.armor[i] = null;
        }
    }

    public void dropAll() {
        int i;
        for (i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null) continue;
            this.player.dropItem(this.items[i], true);
            this.items[i] = null;
        }
        for (i = 0; i < this.armor.length; ++i) {
            if (this.armor[i] == null) continue;
            this.player.dropItem(this.armor[i], true);
            this.armor[i] = null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public void markDirty() {
        this.dirty = true;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean contentEquals(PlayerInventory other) {
        int i;
        for (i = 0; i < this.items.length; ++i) {
            if (this.itemsEqual(other.items[i], this.items[i])) continue;
            return false;
        }
        for (i = 0; i < this.armor.length; ++i) {
            if (this.itemsEqual(other.armor[i], this.armor[i])) continue;
            return false;
        }
        for (i = 0; i < this.crafting.length; ++i) {
            if (this.itemsEqual(other.crafting[i], this.crafting[i])) continue;
            return false;
        }
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    private boolean itemsEqual(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        if (item1 == null || item2 == null) {
            return false;
        }
        return item1.id == item2.id && item1.size == item2.size && item1.metadata == item2.metadata;
    }

    @Environment(value=EnvType.CLIENT)
    public PlayerInventory copy() {
        int i;
        PlayerInventory playerInventory = new PlayerInventory(null);
        for (i = 0; i < this.items.length; ++i) {
            playerInventory.items[i] = this.items[i] != null ? this.items[i].copy() : null;
        }
        for (i = 0; i < this.armor.length; ++i) {
            playerInventory.armor[i] = this.armor[i] != null ? this.armor[i].copy() : null;
        }
        for (i = 0; i < this.crafting.length; ++i) {
            playerInventory.crafting[i] = this.crafting[i] != null ? this.crafting[i].copy() : null;
        }
        return playerInventory;
    }
}

