/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.biome;

import net.minecraft.entity.mob.monster.GhastEntity;
import net.minecraft.entity.mob.monster.ZombiePigmanEntity;
import net.minecraft.world.biome.Biome;

public class HellBiome
extends Biome {
    public HellBiome() {
        this.monsterEntries = new Class[]{GhastEntity.class, ZombiePigmanEntity.class};
        this.passiveEntries = new Class[0];
    }
}

