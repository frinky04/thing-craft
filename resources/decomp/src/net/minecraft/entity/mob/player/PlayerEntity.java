/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.mob.player;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FishingBobberEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class PlayerEntity
extends MobEntity {
    public PlayerInventory inventory = new PlayerInventory(this);
    public byte userType = 0;
    public int score = 0;
    public float lastBob;
    public float bob;
    public boolean swingArm = false;
    public int swingArmTicks = 0;
    public String name;
    public int dimension;
    private int damageSpill = 0;
    public FishingBobberEntity fishingBobber = null;

    public PlayerEntity(World world) {
        super(world);
        this.eyeHeight = 1.62f;
        this.setPositionAndAngles((double)world.spawnPointX + 0.5, world.spawnPointY + 1, (double)world.spawnPointZ + 0.5, 0.0f, 0.0f);
        this.health = 20;
        this.modelName = "humanoid";
        this.rotationOffset = 180.0f;
        this.safeOnFireTime = 20;
        this.texture = "/mob/char.png";
    }

    public void rideTick() {
        super.rideTick();
        this.lastBob = this.bob;
        this.bob = 0.0f;
    }

    @Environment(value=EnvType.CLIENT)
    public void resetPos() {
        this.eyeHeight = 1.62f;
        this.setSize(0.6f, 1.8f);
        super.resetPos();
        this.health = 20;
        this.deathTicks = 0;
    }

    protected void aiTick() {
        if (this.swingArm) {
            ++this.swingArmTicks;
            if (this.swingArmTicks == 8) {
                this.swingArmTicks = 0;
                this.swingArm = false;
            }
        } else {
            this.swingArmTicks = 0;
        }
        this.attackAnimationProgress = (float)this.swingArmTicks / 8.0f;
    }

    public void mobTick() {
        List list;
        if (this.world.difficulty == 0 && this.health < 20 && this.ticks % 20 * 4 == 0) {
            this.heal(1);
        }
        this.inventory.tick();
        this.lastBob = this.bob;
        super.mobTick();
        float f = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
        float g = (float)Math.atan(-this.velocityY * (double)0.2f) * 15.0f;
        if (f > 0.1f) {
            f = 0.1f;
        }
        if (!this.onGround || this.health <= 0) {
            f = 0.0f;
        }
        if (this.onGround || this.health <= 0) {
            g = 0.0f;
        }
        this.bob += (f - this.bob) * 0.4f;
        this.tilt += (g - this.tilt) * 0.8f;
        if (this.health > 0 && (list = this.world.getEntities(this, this.shape.grown(1.0, 0.0, 1.0))) != null) {
            for (int i = 0; i < list.size(); ++i) {
                this.onEntityCollision((Entity)list.get(i));
            }
        }
    }

    private void onEntityCollision(Entity entity) {
        entity.onPlayerCollision(this);
    }

    @Environment(value=EnvType.CLIENT)
    public int getScore() {
        return this.score;
    }

    public void die(Entity killer) {
        super.die(killer);
        this.setSize(0.2f, 0.2f);
        this.setPosition(this.x, this.y, this.z);
        this.velocityY = 0.1f;
        if (this.name.equals("Notch")) {
            this.dropItem(new ItemStack(Item.APPLE, 1), true);
        }
        this.inventory.dropAll();
        if (killer != null) {
            this.velocityX = -MathHelper.cos((this.damagedSwingDir + this.yaw) * (float)Math.PI / 180.0f) * 0.1f;
            this.velocityZ = -MathHelper.sin((this.damagedSwingDir + this.yaw) * (float)Math.PI / 180.0f) * 0.1f;
        } else {
            this.velocityZ = 0.0;
            this.velocityX = 0.0;
        }
        this.eyeHeight = 0.1f;
    }

    public void takeKillScore(Entity victim, int score) {
        this.score += score;
    }

    public void dropItem(ItemStack item) {
        this.dropItem(item, false);
    }

    public void dropItem(ItemStack item, boolean dead) {
        if (item == null) {
            return;
        }
        ItemEntity itemEntity = new ItemEntity(this.world, this.x, this.y - (double)0.3f + (double)this.getEyeHeight(), this.z, item);
        itemEntity.pickUpDelay = 40;
        float f = 0.1f;
        if (dead) {
            float g = this.random.nextFloat() * 0.5f;
            float i = this.random.nextFloat() * (float)Math.PI * 2.0f;
            itemEntity.velocityX = -MathHelper.sin(i) * g;
            itemEntity.velocityZ = MathHelper.cos(i) * g;
            itemEntity.velocityY = 0.2f;
        } else {
            f = 0.3f;
            itemEntity.velocityX = -MathHelper.sin(this.yaw / 180.0f * (float)Math.PI) * MathHelper.cos(this.pitch / 180.0f * (float)Math.PI) * f;
            itemEntity.velocityZ = MathHelper.cos(this.yaw / 180.0f * (float)Math.PI) * MathHelper.cos(this.pitch / 180.0f * (float)Math.PI) * f;
            itemEntity.velocityY = -MathHelper.sin(this.pitch / 180.0f * (float)Math.PI) * f + 0.1f;
            f = 0.02f;
            float h = this.random.nextFloat() * (float)Math.PI * 2.0f;
            itemEntity.velocityX += Math.cos(h) * (double)(f *= this.random.nextFloat());
            itemEntity.velocityY += (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1f);
            itemEntity.velocityZ += Math.sin(h) * (double)f;
        }
        this.spawnItem(itemEntity);
    }

    protected void spawnItem(ItemEntity item) {
        this.world.addEntity(item);
    }

    public float getMiningSpeed(Block block) {
        float f = this.inventory.getMiningSpeed(block);
        if (this.isSubmergedIn(Material.WATER)) {
            f /= 5.0f;
        }
        if (!this.onGround) {
            f /= 5.0f;
        }
        return f;
    }

    public boolean canMineBlock(Block block) {
        return this.inventory.canMineBlock(block);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
        NbtList nbtList = nbt.getList("Inventory");
        this.inventory.readNbt(nbtList);
        this.dimension = nbt.getInt("Dimension");
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
        nbt.put("Inventory", this.inventory.writeNbt(new NbtList()));
        nbt.putInt("Dimension", this.dimension);
    }

    public void openChestMenu(Inventory inventory) {
    }

    public void openCraftingMenu() {
    }

    public void pickUp(Entity item, int count) {
    }

    public float getEyeHeight() {
        return 0.12f;
    }

    public boolean takeDamage(Entity source, int amount) {
        this.farFromPlayerTicks = 0;
        if (this.health <= 0) {
            return false;
        }
        if (source instanceof MonsterEntity || source instanceof ArrowEntity) {
            if (this.world.difficulty == 0) {
                amount = 0;
            }
            if (this.world.difficulty == 1) {
                amount = amount / 3 + 1;
            }
            if (this.world.difficulty == 3) {
                amount = amount * 3 / 2;
            }
        }
        if (amount == 0) {
            return false;
        }
        return super.takeDamage(source, amount);
    }

    protected void applyDamage(int amount) {
        int i = 25 - this.inventory.getArmorProtection();
        int j = amount * i + this.damageSpill;
        this.inventory.damageArmor(amount);
        amount = j / 25;
        this.damageSpill = j % 25;
        super.applyDamage(amount);
    }

    public void openFurnaceMenu(FurnaceBlockEntity furnace) {
    }

    public void openSignEditor(SignBlockEntity sign) {
    }

    public void interact(Entity target) {
        target.interact(this);
    }

    public ItemStack getItemInHand() {
        return this.inventory.getSelectedItem();
    }

    public void clearItemInHand() {
        this.inventory.setItem(this.inventory.selectedSlot, null);
    }

    public double getRideHeight() {
        return this.eyeHeight - 0.5f;
    }

    public void swingArm() {
        this.swingArmTicks = -1;
        this.swingArm = true;
    }

    public void attack(Entity target) {
        int i = this.inventory.getAttackDamage(target);
        if (i > 0) {
            target.takeDamage(this, i);
            ItemStack itemStack = this.getItemInHand();
            if (itemStack != null && target instanceof MobEntity) {
                itemStack.attack((MobEntity)target);
                if (itemStack.size <= 0) {
                    itemStack.onRemoved(this);
                    this.clearItemInHand();
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public void respawn() {
    }
}

