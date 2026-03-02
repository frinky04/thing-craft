/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.item.Item;

public class ArmorItem
extends Item {
    private static final int[] BASE_PROTECTION = new int[]{3, 8, 6, 3};
    private static final int[] BASE_DURABILITY = new int[]{11, 16, 15, 13};
    public final int durability;
    public final int slot;
    public final int protection;
    public final int material;

    public ArmorItem(int id, int durability, int materialId, int slot) {
        super(id);
        this.durability = durability;
        this.slot = slot;
        this.material = materialId;
        this.protection = BASE_PROTECTION[slot];
        this.maxDamage = BASE_DURABILITY[slot] * 3 << durability;
        this.maxStackSize = 1;
    }
}

