/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ItemEntity
extends Entity {
    public ItemStack item;
    private int renderTicks;
    public int age = 0;
    public int pickUpDelay;
    private int health = 5;
    public float bobOffset = (float)(Math.random() * Math.PI * 2.0);

    public ItemEntity(World world, double x, double y, double z, ItemStack item) {
        super(world);
        this.setSize(0.25f, 0.25f);
        this.eyeHeight = this.height / 2.0f;
        this.setPosition(x, y, z);
        this.item = item;
        this.yaw = (float)(Math.random() * 360.0);
        this.velocityX = (float)(Math.random() * (double)0.2f - (double)0.1f);
        this.velocityY = 0.2f;
        this.velocityZ = (float)(Math.random() * (double)0.2f - (double)0.1f);
        this.makesSteps = false;
    }

    public ItemEntity(World world) {
        super(world);
        this.setSize(0.25f, 0.25f);
        this.eyeHeight = this.height / 2.0f;
    }

    public void tick() {
        super.tick();
        if (this.pickUpDelay > 0) {
            --this.pickUpDelay;
        }
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.velocityY -= (double)0.04f;
        if (this.world.getMaterial(MathHelper.floor(this.x), MathHelper.floor(this.y), MathHelper.floor(this.z)) == Material.LAVA) {
            this.velocityY = 0.2f;
            this.velocityX = (this.random.nextFloat() - this.random.nextFloat()) * 0.2f;
            this.velocityZ = (this.random.nextFloat() - this.random.nextFloat()) * 0.2f;
            this.world.playSound(this, "random.fizz", 0.4f, 2.0f + this.random.nextFloat() * 0.4f);
        }
        this.checkBlockCollisions(this.x, this.y, this.z);
        this.checkWaterCollisions();
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        float f = 0.98f;
        if (this.onGround) {
            f = 0.58800006f;
            int i = this.world.getBlock(MathHelper.floor(this.x), MathHelper.floor(this.shape.minY) - 1, MathHelper.floor(this.z));
            if (i > 0) {
                f = Block.BY_ID[i].slipperiness * 0.98f;
            }
        }
        this.velocityX *= (double)f;
        this.velocityY *= (double)0.98f;
        this.velocityZ *= (double)f;
        if (this.onGround) {
            this.velocityY *= -0.5;
        }
        ++this.renderTicks;
        ++this.age;
        if (this.age >= 6000) {
            this.remove();
        }
    }

    public boolean checkWaterCollisions() {
        return this.world.applyLiquidDrag(this.shape, Material.WATER, this);
    }

    private boolean checkBlockCollisions(double x, double y, double z) {
        int i = MathHelper.floor(x);
        int j = MathHelper.floor(y);
        int k = MathHelper.floor(z);
        double d = x - (double)i;
        double e = y - (double)j;
        double f = z - (double)k;
        if (Block.IS_SOLID[this.world.getBlock(i, j, k)]) {
            boolean l = !Block.IS_SOLID[this.world.getBlock(i - 1, j, k)];
            boolean m = !Block.IS_SOLID[this.world.getBlock(i + 1, j, k)];
            boolean n = !Block.IS_SOLID[this.world.getBlock(i, j - 1, k)];
            boolean o = !Block.IS_SOLID[this.world.getBlock(i, j + 1, k)];
            boolean p = !Block.IS_SOLID[this.world.getBlock(i, j, k - 1)];
            boolean q = !Block.IS_SOLID[this.world.getBlock(i, j, k + 1)];
            int r = -1;
            double g = 9999.0;
            if (l && d < g) {
                g = d;
                r = 0;
            }
            if (m && 1.0 - d < g) {
                g = 1.0 - d;
                r = 1;
            }
            if (n && e < g) {
                g = e;
                r = 2;
            }
            if (o && 1.0 - e < g) {
                g = 1.0 - e;
                r = 3;
            }
            if (p && f < g) {
                g = f;
                r = 4;
            }
            if (q && 1.0 - f < g) {
                g = 1.0 - f;
                r = 5;
            }
            float h = this.random.nextFloat() * 0.2f + 0.1f;
            if (r == 0) {
                this.velocityX = -h;
            }
            if (r == 1) {
                this.velocityX = h;
            }
            if (r == 2) {
                this.velocityY = -h;
            }
            if (r == 3) {
                this.velocityY = h;
            }
            if (r == 4) {
                this.velocityZ = -h;
            }
            if (r == 5) {
                this.velocityZ = h;
            }
        }
        return false;
    }

    protected void takeFireDamage(int amount) {
        this.takeDamage(null, amount);
    }

    public boolean takeDamage(Entity source, int amount) {
        this.markDamaged();
        this.health -= amount;
        if (this.health <= 0) {
            this.remove();
        }
        return false;
    }

    public void writeCustomNbt(NbtCompound nbt) {
        nbt.putShort("Health", (byte)this.health);
        nbt.putShort("Age", (short)this.age);
        nbt.putCompound("Item", this.item.writeNbt(new NbtCompound()));
    }

    public void readCustomNbt(NbtCompound nbt) {
        this.health = nbt.getShort("Health") & 0xFF;
        this.age = nbt.getShort("Age");
        NbtCompound nbtCompound = nbt.getCompound("Item");
        this.item = new ItemStack(nbtCompound);
    }

    public void onPlayerCollision(PlayerEntity player) {
        if (this.world.isMultiplayer) {
            return;
        }
        int i = this.item.size;
        if (this.pickUpDelay == 0 && player.inventory.addItem(this.item)) {
            this.world.playSound(this, "random.pop", 0.2f, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
            player.pickUp(this, i);
            this.remove();
        }
    }
}

