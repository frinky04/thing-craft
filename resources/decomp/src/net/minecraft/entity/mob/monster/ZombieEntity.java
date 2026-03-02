/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.monster;

import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ZombieEntity
extends MonsterEntity {
    public ZombieEntity(World world) {
        super(world);
        this.texture = "/mob/zombie.png";
        this.walkSpeed = 0.5f;
        this.attackDamage = 5;
    }

    public void mobTick() {
        float f;
        if (this.world.isSunny() && (f = this.getBrightness(1.0f)) > 0.5f && this.world.hasSkyAccess(MathHelper.floor(this.x), MathHelper.floor(this.y), MathHelper.floor(this.z)) && this.random.nextFloat() * 30.0f < (f - 0.4f) * 2.0f) {
            this.onFireTimer = 300;
        }
        super.mobTick();
    }

    protected String getAmbientSound() {
        return "mob.zombie";
    }

    protected String getHurtSound() {
        return "mob.zombiehurt";
    }

    protected String getDeathSound() {
        return "mob.zombiedeath";
    }

    protected int getDropItem() {
        return Item.FEATHER.id;
    }
}

