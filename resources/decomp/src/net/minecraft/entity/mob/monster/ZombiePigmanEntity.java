/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.mob.monster;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.ZombieEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class ZombiePigmanEntity
extends ZombieEntity {
    private int angerValue = 0;
    private int angerSoundDelay = 0;
    private static final ItemStack GOLDEN_SWORD = new ItemStack(Item.GOLDEN_SWORD, 1);

    public ZombiePigmanEntity(World world) {
        super(world);
        this.texture = "/mob/pigzombie.png";
        this.walkSpeed = 0.5f;
        this.attackDamage = 5;
        this.immuneToFire = true;
    }

    public void tick() {
        float f = this.walkSpeed = this.walkTarget != null ? 0.95f : 0.5f;
        if (this.angerSoundDelay > 0 && --this.angerSoundDelay == 0) {
            this.world.playSound(this, "mob.zombiepig.zpigangry", this.getSoundVolume() * 2.0f, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) * 1.8f);
        }
        super.tick();
    }

    public boolean canSpawn() {
        return this.world.difficulty > 0 && this.world.isUnobstructed(this.shape) && this.world.getCollisions(this, this.shape).size() == 0 && !this.world.containsLiquid(this.shape);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
        nbt.putShort("Anger", (short)this.angerValue);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
        this.angerValue = nbt.getShort("Anger");
    }

    protected Entity findTarget() {
        if (this.angerValue == 0) {
            return null;
        }
        return super.findTarget();
    }

    public void mobTick() {
        super.mobTick();
    }

    public boolean takeDamage(Entity source, int amount) {
        if (source instanceof PlayerEntity) {
            List list = this.world.getEntities(this, this.shape.grown(32.0, 32.0, 32.0));
            for (int i = 0; i < list.size(); ++i) {
                Entity entity = (Entity)list.get(i);
                if (!(entity instanceof ZombiePigmanEntity)) continue;
                ZombiePigmanEntity zombiePigmanEntity = (ZombiePigmanEntity)entity;
                zombiePigmanEntity.getAngryTo(source);
            }
            this.getAngryTo(source);
        }
        return super.takeDamage(source, amount);
    }

    private void getAngryTo(Entity target) {
        this.walkTarget = target;
        this.angerValue = 400 + this.random.nextInt(400);
        this.angerSoundDelay = this.random.nextInt(40);
    }

    protected String getAmbientSound() {
        return "mob.zombiepig.zpig";
    }

    protected String getHurtSound() {
        return "mob.zombiepig.zpighurt";
    }

    protected String getDeathSound() {
        return "mob.zombiepig.zpigdeath";
    }

    protected int getDropItem() {
        return Item.COOKED_PORKCHOP.id;
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack getDisplayItemInHand() {
        return GOLDEN_SWORD;
    }
}

