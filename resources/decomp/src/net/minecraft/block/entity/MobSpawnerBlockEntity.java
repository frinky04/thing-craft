/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entities;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;

public class MobSpawnerBlockEntity
extends BlockEntity {
    public int delay = 20;
    public String type = "Pig";
    public double rotation;
    public double lastRotation = 0.0;

    public boolean isNearPlayer() {
        return this.world.getNearestPlayer((double)this.x + 0.5, (double)this.y + 0.5, (double)this.z + 0.5, 16.0) != null;
    }

    public void tick() {
        this.lastRotation = this.rotation;
        if (!this.isNearPlayer()) {
            return;
        }
        double d = (float)this.x + this.world.random.nextFloat();
        double e = (float)this.y + this.world.random.nextFloat();
        double f = (float)this.z + this.world.random.nextFloat();
        this.world.addParticle("smoke", d, e, f, 0.0, 0.0, 0.0);
        this.world.addParticle("flame", d, e, f, 0.0, 0.0, 0.0);
        this.rotation += (double)(1000.0f / ((float)this.delay + 200.0f));
        while (this.rotation > 360.0) {
            this.rotation -= 360.0;
            this.lastRotation -= 360.0;
        }
        if (this.delay == -1) {
            this.delay();
        }
        if (this.delay > 0) {
            --this.delay;
            return;
        }
        int i = 4;
        for (int j = 0; j < i; ++j) {
            MobEntity mobEntity = (MobEntity)Entities.createSilently(this.type, this.world);
            if (mobEntity == null) {
                return;
            }
            int k = this.world.getEntitiesOfType(mobEntity.getClass(), Box.fromPool(this.x, this.y, this.z, this.x + 1, this.y + 1, this.z + 1).grown(8.0, 4.0, 8.0)).size();
            if (k >= 6) {
                this.delay();
                return;
            }
            if (mobEntity == null) continue;
            double g = (double)this.x + (this.world.random.nextDouble() - this.world.random.nextDouble()) * 4.0;
            double h = this.y + this.world.random.nextInt(3) - 1;
            double l = (double)this.z + (this.world.random.nextDouble() - this.world.random.nextDouble()) * 4.0;
            mobEntity.setPositionAndAngles(g, h, l, this.world.random.nextFloat() * 360.0f, 0.0f);
            if (!mobEntity.canSpawn()) continue;
            this.world.addEntity(mobEntity);
            for (int m = 0; m < 20; ++m) {
                d = (double)this.x + 0.5 + ((double)this.world.random.nextFloat() - 0.5) * 2.0;
                e = (double)this.y + 0.5 + ((double)this.world.random.nextFloat() - 0.5) * 2.0;
                f = (double)this.z + 0.5 + ((double)this.world.random.nextFloat() - 0.5) * 2.0;
                this.world.addParticle("smoke", d, e, f, 0.0, 0.0, 0.0);
                this.world.addParticle("flame", d, e, f, 0.0, 0.0, 0.0);
            }
            mobEntity.animateSpawn();
            this.delay();
        }
        super.tick();
    }

    private void delay() {
        this.delay = 200 + this.world.random.nextInt(600);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.type = nbt.getString("EntityId");
        this.delay = nbt.getShort("Delay");
    }

    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("EntityId", this.type);
        nbt.putShort("Delay", (short)this.delay);
    }
}

