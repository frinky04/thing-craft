/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.mob.monster;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class SkeletonEntity
extends MonsterEntity {
    private static final ItemStack BOW = new ItemStack(Item.BOW, 1);

    public SkeletonEntity(World world) {
        super(world);
        this.texture = "/mob/skeleton.png";
    }

    protected String getAmbientSound() {
        return "mob.skeleton";
    }

    protected String getHurtSound() {
        return "mob.skeletonhurt";
    }

    protected String getDeathSound() {
        return "mob.skeletonhurt";
    }

    public void mobTick() {
        float f;
        if (this.world.isSunny() && (f = this.getBrightness(1.0f)) > 0.5f && this.world.hasSkyAccess(MathHelper.floor(this.x), MathHelper.floor(this.y), MathHelper.floor(this.z)) && this.random.nextFloat() * 30.0f < (f - 0.4f) * 2.0f) {
            this.onFireTimer = 300;
        }
        super.mobTick();
    }

    protected void targetInSight(Entity target, float distance) {
        if (distance < 10.0f) {
            double d = target.x - this.x;
            double e = target.z - this.z;
            if (this.attackTimer == 0) {
                ArrowEntity arrowEntity = new ArrowEntity(this.world, this);
                arrowEntity.y += (double)1.4f;
                double f = target.y - (double)0.2f - arrowEntity.y;
                float g = MathHelper.sqrt(d * d + e * e) * 0.2f;
                this.world.playSound(this, "random.bow", 1.0f, 1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
                this.world.addEntity(arrowEntity);
                arrowEntity.shoot(d, f + (double)g, e, 0.6f, 12.0f);
                this.attackTimer = 30;
            }
            this.yaw = (float)(Math.atan2(e, d) * 180.0 / 3.1415927410125732) - 90.0f;
            this.stoppedMoving = true;
        }
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    protected int getDropItem() {
        return Item.ARROW.id;
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack getDisplayItemInHand() {
        return BOW;
    }
}

