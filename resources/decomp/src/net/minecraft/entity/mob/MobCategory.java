/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob;

import net.minecraft.entity.mob.animal.AnimalEntity;
import net.minecraft.entity.mob.monster.Monster;

public enum MobCategory {
    MONSTER(Monster.class, 100),
    CREATURE(AnimalEntity.class, 20);

    public final Class type;
    public final int cap;

    /*
     * WARNING - void declaration
     */
    private MobCategory() {
        void cap;
        void type;
        this.type = type;
        this.cap = cap;
    }
}

