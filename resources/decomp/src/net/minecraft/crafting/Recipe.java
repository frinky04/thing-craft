/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.crafting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public class Recipe {
    private int width;
    private int height;
    private int[] ingredients;
    private ItemStack result;
    public final int resultItem;

    public Recipe(int width, int height, int[] ingredients, ItemStack result) {
        this.resultItem = result.id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
    }

    public boolean matches(int[] inventory) {
        for (int i = 0; i <= 3 - this.width; ++i) {
            for (int j = 0; j <= 3 - this.height; ++j) {
                if (this.matches(inventory, i, j, true)) {
                    return true;
                }
                if (!this.matches(inventory, i, j, false)) continue;
                return true;
            }
        }
        return false;
    }

    private boolean matches(int[] inventory, int x, int y, boolean rotate) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                int k = i - x;
                int l = j - y;
                int m = -1;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    m = rotate ? this.ingredients[this.width - k - 1 + l * this.width] : this.ingredients[k + l * this.width];
                }
                if (inventory[i + j * 3] == m) continue;
                return false;
            }
        }
        return true;
    }

    public ItemStack getResult(int[] inventory) {
        return new ItemStack(this.result.id, this.result.size);
    }

    public int size() {
        return this.width * this.height;
    }
}

