/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block.material;

import net.minecraft.block.material.AirMaterial;
import net.minecraft.block.material.LiquidMaterial;
import net.minecraft.block.material.PlantMaterial;

public class Material {
    public static final Material AIR = new AirMaterial();
    public static final Material GRASS = new Material();
    public static final Material WOOD = new Material().setFlammable();
    public static final Material STONE = new Material();
    public static final Material IRON = new Material();
    public static final Material WATER = new LiquidMaterial();
    public static final Material LAVA = new LiquidMaterial();
    public static final Material LEAVES = new Material().setFlammable();
    public static final Material PLANT = new PlantMaterial();
    public static final Material SPONGE = new Material();
    public static final Material WOOL = new Material().setFlammable();
    public static final Material FIRE = new AirMaterial();
    public static final Material SAND = new Material();
    public static final Material DECORATION = new PlantMaterial();
    public static final Material GLASS = new Material();
    public static final Material TNT = new Material().setFlammable();
    public static final Material CORAL = new Material();
    public static final Material ICE = new Material();
    public static final Material SNOW_LAYER = new PlantMaterial();
    public static final Material SNOW = new Material();
    public static final Material CACTUS = new Material();
    public static final Material CLAY = new Material();
    public static final Material PUMPKIN = new Material();
    public static final Material PORTAL = new Material();
    private boolean flammable;

    public boolean isLiquid() {
        return false;
    }

    public boolean isSolid() {
        return true;
    }

    public boolean isOpaque() {
        return true;
    }

    public boolean blocksMovement() {
        return true;
    }

    private Material setFlammable() {
        this.flammable = true;
        return this;
    }

    public boolean isFlammable() {
        return this.flammable;
    }
}

