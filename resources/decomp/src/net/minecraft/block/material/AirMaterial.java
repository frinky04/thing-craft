/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block.material;

import net.minecraft.block.material.Material;

public class AirMaterial
extends Material {
    public boolean isSolid() {
        return false;
    }

    public boolean isOpaque() {
        return false;
    }

    public boolean blocksMovement() {
        return false;
    }
}

