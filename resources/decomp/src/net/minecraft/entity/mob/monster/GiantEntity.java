/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.monster;

import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.world.World;

public class GiantEntity
extends MonsterEntity {
    public GiantEntity(World world) {
        super(world);
        this.texture = "/mob/zombie.png";
        this.walkSpeed = 0.5f;
        this.attackDamage = 50;
        this.health *= 10;
        this.eyeHeight *= 6.0f;
        this.setSize(this.width * 6.0f, this.height * 6.0f);
    }

    protected float getPathfindingFavor(int x, int y, int z) {
        return this.world.getBrightness(x, y, z) - 0.5f;
    }
}

