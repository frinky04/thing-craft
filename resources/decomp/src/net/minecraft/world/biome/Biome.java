/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.biome;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.MobCategory;
import net.minecraft.entity.mob.animal.ChickenEntity;
import net.minecraft.entity.mob.animal.CowEntity;
import net.minecraft.entity.mob.animal.PigEntity;
import net.minecraft.entity.mob.animal.SheepEntity;
import net.minecraft.entity.mob.monster.CreeperEntity;
import net.minecraft.entity.mob.monster.SkeletonEntity;
import net.minecraft.entity.mob.monster.SpiderEntity;
import net.minecraft.entity.mob.monster.ZombieEntity;
import net.minecraft.world.biome.DesertBiome;
import net.minecraft.world.biome.HellBiome;
import net.minecraft.world.biome.SwampBiome;

public class Biome {
    public static final Biome RAINFOREST = new Biome().setBaseColor(588342).setName("Rainforest").setMutatedColor(2094168);
    public static final Biome SWAMPLAND = new SwampBiome().setBaseColor(522674).setName("Swampland").setMutatedColor(9154376);
    public static final Biome SEASONAL_FOREST = new Biome().setBaseColor(10215459).setName("Seasonal Forest");
    public static final Biome FOREST = new Biome().setBaseColor(353825).setName("Forest").setMutatedColor(5159473);
    public static final Biome SAVANNA = new DesertBiome().setBaseColor(14278691).setName("Savanna");
    public static final Biome SHRUBLAND = new Biome().setBaseColor(10595616).setName("Shrubland");
    public static final Biome TAIGA = new Biome().setBaseColor(3060051).setName("Taiga").setSnowy().setMutatedColor(8107825);
    public static final Biome DESERT = new DesertBiome().setBaseColor(16421912).setName("Desert");
    public static final Biome PLAINS = new DesertBiome().setBaseColor(16767248).setName("Plains");
    public static final Biome ICE_DESERT = new DesertBiome().setBaseColor(16772499).setName("Ice Desert").setSnowy().setMutatedColor(12899129);
    public static final Biome TUNDRA = new Biome().setBaseColor(5762041).setName("Tundra").setSnowy().setMutatedColor(12899129);
    public static final Biome HELL = new HellBiome().setBaseColor(0xFF0000).setName("Hell");
    public String name;
    public int baseColor;
    public byte surfaceBlock;
    public byte subsurfaceBlock;
    public int mutatedColor;
    protected Class[] monsterEntries;
    protected Class[] passiveEntries;
    private static Biome[] BIOME_MAP = new Biome[4096];

    public Biome() {
        this.surfaceBlock = (byte)Block.GRASS.id;
        this.subsurfaceBlock = (byte)Block.DIRT.id;
        this.mutatedColor = 5169201;
        this.monsterEntries = new Class[]{SpiderEntity.class, ZombieEntity.class, SkeletonEntity.class, CreeperEntity.class};
        this.passiveEntries = new Class[]{SheepEntity.class, PigEntity.class, ChickenEntity.class, CowEntity.class};
    }

    public static void init() {
        for (int i = 0; i < 64; ++i) {
            for (int j = 0; j < 64; ++j) {
                Biome.BIOME_MAP[i + j * 64] = Biome.computeBiome((float)i / 63.0f, (float)j / 63.0f);
            }
        }
        Biome.DESERT.surfaceBlock = Biome.DESERT.subsurfaceBlock = (byte)Block.SAND.id;
        Biome.ICE_DESERT.surfaceBlock = Biome.ICE_DESERT.subsurfaceBlock = (byte)Block.SAND.id;
    }

    protected Biome setSnowy() {
        return this;
    }

    protected Biome setName(String name) {
        this.name = name;
        return this;
    }

    protected Biome setMutatedColor(int color) {
        this.mutatedColor = color;
        return this;
    }

    protected Biome setBaseColor(int color) {
        this.baseColor = color;
        return this;
    }

    public static Biome getBiome(double temperature, double downfall) {
        int i = (int)(temperature * 63.0);
        int j = (int)(downfall * 63.0);
        return BIOME_MAP[i + j * 64];
    }

    public static Biome computeBiome(float temperature, float downfall) {
        downfall *= temperature;
        if (temperature < 0.1f) {
            return TUNDRA;
        }
        if (downfall < 0.2f) {
            if (temperature < 0.5f) {
                return TUNDRA;
            }
            if (temperature < 0.95f) {
                return SAVANNA;
            }
            return DESERT;
        }
        if (downfall > 0.5f && temperature < 0.7f) {
            return SWAMPLAND;
        }
        if (temperature < 0.5f) {
            return TAIGA;
        }
        if (temperature < 0.97f) {
            if (downfall < 0.35f) {
                return SHRUBLAND;
            }
            return FOREST;
        }
        if (downfall < 0.45f) {
            return PLAINS;
        }
        if (downfall < 0.9f) {
            return SEASONAL_FOREST;
        }
        return RAINFOREST;
    }

    @Environment(value=EnvType.CLIENT)
    public int getSkyColor(float temperature) {
        if ((temperature /= 3.0f) < -1.0f) {
            temperature = -1.0f;
        }
        if (temperature > 1.0f) {
            temperature = 1.0f;
        }
        return Color.getHSBColor(0.62222224f - temperature * 0.05f, 0.5f + temperature * 0.1f, 1.0f).getRGB();
    }

    public Class[] getSpawnEntries(MobCategory category) {
        if (category == MobCategory.MONSTER) {
            return this.monsterEntries;
        }
        if (category == MobCategory.CREATURE) {
            return this.passiveEntries;
        }
        return null;
    }

    static {
        Biome.init();
    }
}

