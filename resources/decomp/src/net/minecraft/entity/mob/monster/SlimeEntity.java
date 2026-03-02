/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.mob.monster;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.monster.Monster;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class SlimeEntity
extends MobEntity
implements Monster {
    public float stretch;
    public float lastStretch;
    private int jumpTimer = 0;
    public int size = 1;

    public SlimeEntity(World world) {
        super(world);
        this.texture = "/mob/slime.png";
        this.size = 1 << this.random.nextInt(3);
        this.eyeHeight = 0.0f;
        this.jumpTimer = this.random.nextInt(20) + 10;
        this.setSize(this.size);
    }

    public void setSize(int size) {
        this.size = size;
        this.setSize(0.6f * (float)size, 0.6f * (float)size);
        this.health = size * size;
        this.setPosition(this.x, this.y, this.z);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
        nbt.putInt("Size", this.size - 1);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
        this.size = nbt.getInt("Size") + 1;
    }

    public void tick() {
        this.lastStretch = this.stretch;
        boolean i = this.onGround;
        super.tick();
        if (this.onGround && !i) {
            for (int j = 0; j < this.size * 8; ++j) {
                float f = this.random.nextFloat() * (float)Math.PI * 2.0f;
                float g = this.random.nextFloat() * 0.5f + 0.5f;
                float h = MathHelper.sin(f) * (float)this.size * 0.5f * g;
                float k = MathHelper.cos(f) * (float)this.size * 0.5f * g;
                this.world.addParticle("slime", this.x + (double)h, this.shape.minY, this.z + (double)k, 0.0, 0.0, 0.0);
            }
            if (this.size > 2) {
                this.world.playSound(this, "mob.slime", this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) / 0.8f);
            }
            this.stretch = -0.5f;
        }
        this.stretch *= 0.6f;
    }

    protected void aiTick() {
        PlayerEntity playerEntity = this.world.getNearestPlayer(this, 16.0);
        if (playerEntity != null) {
            this.lookAt(playerEntity, 10.0f);
        }
        if (this.onGround && this.jumpTimer-- <= 0) {
            this.jumpTimer = this.random.nextInt(20) + 10;
            if (playerEntity != null) {
                this.jumpTimer /= 3;
            }
            this.jumping = true;
            if (this.size > 1) {
                this.world.playSound(this, "mob.slime", this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) * 0.8f);
            }
            this.stretch = 1.0f;
            this.sidewaysSpeed = 1.0f - this.random.nextFloat() * 2.0f;
            this.forwardSpeed = 1 * this.size;
        } else {
            this.jumping = false;
            if (this.onGround) {
                this.forwardSpeed = 0.0f;
                this.sidewaysSpeed = 0.0f;
            }
        }
    }

    public void remove() {
        if (this.size > 1 && this.health == 0) {
            for (int i = 0; i < 4; ++i) {
                float f = ((float)(i % 2) - 0.5f) * (float)this.size / 4.0f;
                float g = ((float)(i / 2) - 0.5f) * (float)this.size / 4.0f;
                SlimeEntity slimeEntity = new SlimeEntity(this.world);
                slimeEntity.setSize(this.size / 2);
                slimeEntity.setPositionAndAngles(this.x + (double)f, this.y + 0.5, this.z + (double)g, this.random.nextFloat() * 360.0f, 0.0f);
                this.world.addEntity(slimeEntity);
            }
        }
        super.remove();
    }

    public void onPlayerCollision(PlayerEntity player) {
        if (this.size > 1 && this.canSee(player) && (double)this.distanceTo(player) < 0.6 * (double)this.size && player.takeDamage(this, this.size)) {
            this.world.playSound(this, "mob.slimeattack", 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
        }
    }

    protected String getHurtSound() {
        return "mob.slime";
    }

    protected String getDeathSound() {
        return "mob.slime";
    }

    protected int getDropItem() {
        if (this.size == 1) {
            return Item.SLIMEBALL.id;
        }
        return 0;
    }

    public boolean canSpawn() {
        WorldChunk worldChunk = this.world.getChunk(MathHelper.floor(this.x), MathHelper.floor(this.z));
        return (this.size == 1 || this.world.difficulty > 0) && this.random.nextInt(10) == 0 && worldChunk.getRandomForSlime(987234911L).nextInt(10) == 0 && this.y < 16.0;
    }

    protected float getSoundVolume() {
        return 0.6f;
    }
}

