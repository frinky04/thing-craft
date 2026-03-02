/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block.material;

import net.minecraft.block.material.Material;

public class LiquidMaterial
extends Material {
    public boolean isLiquid() {
        return true;
    }

    public boolean blocksMovement() {
        return false;
    }

    public boolean isSolid() {
        return false;
    }
}

