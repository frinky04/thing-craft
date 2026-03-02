/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.vehicle;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MinecartEntity
extends Entity
implements Inventory {
    private ItemStack[] inventory = new ItemStack[36];
    public int damage = 0;
    public int damagedTimer = 0;
    public int damagedSwingDir = 1;
    private boolean flipped = false;
    public int type;
    public int fuel;
    public double pushX;
    public double pushZ;
    private static final int[][][] ADJACENT_RAIL_POSITIONS_BY_SHAPE = new int[][][]{new int[][]{{0, 0, -1}, {0, 0, 1}}, new int[][]{{-1, 0, 0}, {1, 0, 0}}, new int[][]{{-1, -1, 0}, {1, 0, 0}}, new int[][]{{-1, 0, 0}, {1, -1, 0}}, new int[][]{{0, 0, -1}, {0, -1, 1}}, new int[][]{{0, -1, -1}, {0, 0, 1}}, new int[][]{{0, 0, 1}, {1, 0, 0}}, new int[][]{{0, 0, 1}, {-1, 0, 0}}, new int[][]{{0, 0, -1}, {-1, 0, 0}}, new int[][]{{0, 0, -1}, {1, 0, 0}}};
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYaw;
    private double lerpPitch;
    @Environment(value=EnvType.CLIENT)
    private double lerpVelocityX;
    @Environment(value=EnvType.CLIENT)
    private double lerpVelocityY;
    @Environment(value=EnvType.CLIENT)
    private double lerpVelocityZ;

    public MinecartEntity(World world) {
        super(world);
        this.blocksBuilding = true;
        this.setSize(0.98f, 0.7f);
        this.eyeHeight = this.height / 2.0f;
        this.makesSteps = false;
    }

    public Box getCollisionAgainstShape(Entity other) {
        return other.shape;
    }

    public Box getCollisionShape() {
        return null;
    }

    public boolean isPushable() {
        return true;
    }

    public MinecartEntity(World world, double x, double y, double z, int type) {
        this(world);
        this.setPosition(x, y + (double)this.eyeHeight, z);
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.type = type;
    }

    public double getMountHeight() {
        return (double)this.height * 0.0 - (double)0.3f;
    }

    public boolean takeDamage(Entity source, int amount) {
        if (this.world.isMultiplayer || this.removed) {
            return true;
        }
        this.damagedSwingDir = -this.damagedSwingDir;
        this.damagedTimer = 10;
        this.markDamaged();
        this.damage += amount * 10;
        if (this.damage > 40) {
            this.dropItem(Item.MINECART.id, 1, 0.0f);
            if (this.type == 1) {
                this.dropItem(Block.CHEST.id, 1, 0.0f);
            } else if (this.type == 2) {
                this.dropItem(Block.FURNACE.id, 1, 0.0f);
            }
            this.remove();
        }
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public void animateDamage() {
        System.out.println("Animating hurt");
        this.damagedSwingDir = -this.damagedSwingDir;
        this.damagedTimer = 10;
        this.damage += this.damage * 10;
    }

    public boolean hasCollision() {
        return !this.removed;
    }

    public void remove() {
        for (int i = 0; i < this.getSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack == null) continue;
            float f = this.random.nextFloat() * 0.8f + 0.1f;
            float g = this.random.nextFloat() * 0.8f + 0.1f;
            float h = this.random.nextFloat() * 0.8f + 0.1f;
            while (itemStack.size > 0) {
                int j = this.random.nextInt(21) + 10;
                if (j > itemStack.size) {
                    j = itemStack.size;
                }
                itemStack.size -= j;
                ItemEntity itemEntity = new ItemEntity(this.world, this.x + (double)f, this.y + (double)g, this.z + (double)h, new ItemStack(itemStack.id, j, itemStack.metadata));
                float k = 0.05f;
                itemEntity.velocityX = (float)this.random.nextGaussian() * k;
                itemEntity.velocityY = (float)this.random.nextGaussian() * k + 0.2f;
                itemEntity.velocityZ = (float)this.random.nextGaussian() * k;
                this.world.addEntity(itemEntity);
            }
        }
        super.remove();
    }

    public void tick() {
        double r;
        int k;
        int j;
        if (this.damagedTimer > 0) {
            --this.damagedTimer;
        }
        if (this.damage > 0) {
            --this.damage;
        }
        if (this.world.isMultiplayer && this.lerpSteps > 0) {
            if (this.lerpSteps > 0) {
                double h;
                double d = this.x + (this.lerpX - this.x) / (double)this.lerpSteps;
                double e = this.y + (this.lerpY - this.y) / (double)this.lerpSteps;
                double g = this.z + (this.lerpZ - this.z) / (double)this.lerpSteps;
                for (h = this.lerpYaw - (double)this.yaw; h < -180.0; h += 360.0) {
                }
                while (h >= 180.0) {
                    h -= 360.0;
                }
                this.yaw = (float)((double)this.yaw + h / (double)this.lerpSteps);
                this.pitch = (float)((double)this.pitch + (this.lerpPitch - (double)this.pitch) / (double)this.lerpSteps);
                --this.lerpSteps;
                this.setPosition(d, e, g);
                this.setRotation(this.yaw, this.pitch);
            } else {
                this.setPosition(this.x, this.y, this.z);
                this.setRotation(this.yaw, this.pitch);
            }
            return;
        }
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.velocityY -= (double)0.04f;
        int i = MathHelper.floor(this.x);
        if (this.world.getBlock(i, (j = MathHelper.floor(this.y)) - 1, k = MathHelper.floor(this.z)) == Block.RAIL.id) {
            --j;
        }
        double f = 0.4;
        boolean l = false;
        double m = 0.0078125;
        if (this.world.getBlock(i, j, k) == Block.RAIL.id) {
            double am;
            Vec3d vec3d = this.snapPositionToRail(this.x, this.y, this.z);
            int o = this.world.getBlockMetadata(i, j, k);
            this.y = j;
            if (o >= 2 && o <= 5) {
                this.y = j + 1;
            }
            if (o == 2) {
                this.velocityX -= m;
            }
            if (o == 3) {
                this.velocityX += m;
            }
            if (o == 4) {
                this.velocityZ += m;
            }
            if (o == 5) {
                this.velocityZ -= m;
            }
            int[][] is = ADJACENT_RAIL_POSITIONS_BY_SHAPE[o];
            double q = is[1][0] - is[0][0];
            double s = is[1][2] - is[0][2];
            double t = Math.sqrt(q * q + s * s);
            double v = this.velocityX * q + this.velocityZ * s;
            if (v < 0.0) {
                q = -q;
                s = -s;
            }
            double w = Math.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
            this.velocityX = w * q / t;
            this.velocityZ = w * s / t;
            double x = 0.0;
            double y = (double)i + 0.5 + (double)is[0][0] * 0.5;
            double z = (double)k + 0.5 + (double)is[0][2] * 0.5;
            double aa = (double)i + 0.5 + (double)is[1][0] * 0.5;
            double ab = (double)k + 0.5 + (double)is[1][2] * 0.5;
            q = aa - y;
            s = ab - z;
            if (q == 0.0) {
                this.x = (double)i + 0.5;
                x = this.z - (double)k;
            } else if (s == 0.0) {
                this.z = (double)k + 0.5;
                x = this.x - (double)i;
            } else {
                double ag;
                double ac = this.x - y;
                double ae = this.z - z;
                x = ag = (ac * q + ae * s) * 2.0;
            }
            this.x = y + q * x;
            this.z = z + s * x;
            this.setPosition(this.x, this.y + (double)this.eyeHeight, this.z);
            double ad = this.velocityX;
            double af = this.velocityZ;
            if (this.rider != null) {
                ad *= 0.75;
                af *= 0.75;
            }
            if (ad < -f) {
                ad = -f;
            }
            if (ad > f) {
                ad = f;
            }
            if (af < -f) {
                af = -f;
            }
            if (af > f) {
                af = f;
            }
            this.move(ad, 0.0, af);
            if (is[0][1] != 0 && MathHelper.floor(this.x) - i == is[0][0] && MathHelper.floor(this.z) - k == is[0][2]) {
                this.setPosition(this.x, this.y + (double)is[0][1], this.z);
            } else if (is[1][1] != 0 && MathHelper.floor(this.x) - i == is[1][0] && MathHelper.floor(this.z) - k == is[1][2]) {
                this.setPosition(this.x, this.y + (double)is[1][1], this.z);
            }
            if (this.rider != null) {
                this.velocityX *= (double)0.997f;
                this.velocityY *= 0.0;
                this.velocityZ *= (double)0.997f;
            } else {
                if (this.type == 2) {
                    double ah = MathHelper.sqrt(this.pushX * this.pushX + this.pushZ * this.pushZ);
                    if (ah > 0.01) {
                        l = true;
                        this.pushX /= ah;
                        this.pushZ /= ah;
                        double ak = 0.04;
                        this.velocityX *= (double)0.8f;
                        this.velocityY *= 0.0;
                        this.velocityZ *= (double)0.8f;
                        this.velocityX += this.pushX * ak;
                        this.velocityZ += this.pushZ * ak;
                    } else {
                        this.velocityX *= (double)0.9f;
                        this.velocityY *= 0.0;
                        this.velocityZ *= (double)0.9f;
                    }
                }
                this.velocityX *= (double)0.96f;
                this.velocityY *= 0.0;
                this.velocityZ *= (double)0.96f;
            }
            Vec3d vec3d2 = this.snapPositionToRail(this.x, this.y, this.z);
            if (vec3d2 != null && vec3d != null) {
                double ai = (vec3d.y - vec3d2.y) * 0.05;
                w = Math.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
                if (w > 0.0) {
                    this.velocityX = this.velocityX / w * (w + ai);
                    this.velocityZ = this.velocityZ / w * (w + ai);
                }
                this.setPosition(this.x, vec3d2.y, this.z);
            }
            int aj = MathHelper.floor(this.x);
            int al = MathHelper.floor(this.z);
            if (aj != i || al != k) {
                w = Math.sqrt(this.velocityX * this.velocityX + this.velocityZ * this.velocityZ);
                this.velocityX = w * (double)(aj - i);
                this.velocityZ = w * (double)(al - k);
            }
            if (this.type == 2 && (am = (double)MathHelper.sqrt(this.pushX * this.pushX + this.pushZ * this.pushZ)) > 0.01 && this.velocityX * this.velocityX + this.velocityZ * this.velocityZ > 0.001) {
                this.pushX /= am;
                this.pushZ /= am;
                if (this.pushX * this.velocityX + this.pushZ * this.velocityZ < 0.0) {
                    this.pushX = 0.0;
                    this.pushZ = 0.0;
                } else {
                    this.pushX = this.velocityX;
                    this.pushZ = this.velocityZ;
                }
            }
        } else {
            if (this.velocityX < -f) {
                this.velocityX = -f;
            }
            if (this.velocityX > f) {
                this.velocityX = f;
            }
            if (this.velocityZ < -f) {
                this.velocityZ = -f;
            }
            if (this.velocityZ > f) {
                this.velocityZ = f;
            }
            if (this.onGround) {
                this.velocityX *= 0.5;
                this.velocityY *= 0.5;
                this.velocityZ *= 0.5;
            }
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            if (!this.onGround) {
                this.velocityX *= (double)0.95f;
                this.velocityY *= (double)0.95f;
                this.velocityZ *= (double)0.95f;
            }
        }
        this.pitch = 0.0f;
        double n = this.lastX - this.x;
        double p = this.lastZ - this.z;
        if (n * n + p * p > 0.001) {
            this.yaw = (float)(Math.atan2(p, n) * 180.0 / Math.PI);
            if (this.flipped) {
                this.yaw += 180.0f;
            }
        }
        for (r = (double)(this.yaw - this.lastYaw); r >= 180.0; r -= 360.0) {
        }
        while (r < -180.0) {
            r += 360.0;
        }
        if (r < -170.0 || r >= 170.0) {
            this.yaw += 180.0f;
            this.flipped = !this.flipped;
        }
        this.setRotation(this.yaw, this.pitch);
        List list = this.world.getEntities(this, this.shape.grown(0.2f, 0.0, 0.2f));
        if (list != null && list.size() > 0) {
            for (int u = 0; u < list.size(); ++u) {
                Entity entity = (Entity)list.get(u);
                if (entity == this.rider || !entity.isPushable() || !(entity instanceof MinecartEntity)) continue;
                entity.push(this);
            }
        }
        if (this.rider != null && this.rider.removed) {
            this.rider = null;
        }
        if (l && this.random.nextInt(4) == 0) {
            --this.fuel;
            if (this.fuel < 0) {
                this.pushZ = 0.0;
                this.pushX = 0.0;
            }
            this.world.addParticle("largesmoke", this.x, this.y + 0.8, this.z, 0.0, 0.0, 0.0);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d snapPositionToRailWithOffset(double x, double y, double z, double offset) {
        int k;
        int j;
        int i = MathHelper.floor(x);
        if (this.world.getBlock(i, (j = MathHelper.floor(y)) - 1, k = MathHelper.floor(z)) == Block.RAIL.id) {
            --j;
        }
        if (this.world.getBlock(i, j, k) == Block.RAIL.id) {
            int l = this.world.getBlockMetadata(i, j, k);
            y = j;
            if (l >= 2 && l <= 5) {
                y = j + 1;
            }
            int[][] is = ADJACENT_RAIL_POSITIONS_BY_SHAPE[l];
            double d = is[1][0] - is[0][0];
            double e = is[1][2] - is[0][2];
            double f = Math.sqrt(d * d + e * e);
            if (is[0][1] != 0 && MathHelper.floor(x += (d /= f) * offset) - i == is[0][0] && MathHelper.floor(z += (e /= f) * offset) - k == is[0][2]) {
                y += (double)is[0][1];
            } else if (is[1][1] != 0 && MathHelper.floor(x) - i == is[1][0] && MathHelper.floor(z) - k == is[1][2]) {
                y += (double)is[1][1];
            }
            return this.snapPositionToRail(x, y, z);
        }
        return null;
    }

    public Vec3d snapPositionToRail(double x, double y, double z) {
        int k;
        int j;
        int i = MathHelper.floor(x);
        if (this.world.getBlock(i, (j = MathHelper.floor(y)) - 1, k = MathHelper.floor(z)) == Block.RAIL.id) {
            --j;
        }
        if (this.world.getBlock(i, j, k) == Block.RAIL.id) {
            int l = this.world.getBlockMetadata(i, j, k);
            y = j;
            if (l >= 2 && l <= 5) {
                y = j + 1;
            }
            int[][] is = ADJACENT_RAIL_POSITIONS_BY_SHAPE[l];
            double d = 0.0;
            double e = (double)i + 0.5 + (double)is[0][0] * 0.5;
            double f = (double)j + 0.5 + (double)is[0][1] * 0.5;
            double g = (double)k + 0.5 + (double)is[0][2] * 0.5;
            double h = (double)i + 0.5 + (double)is[1][0] * 0.5;
            double m = (double)j + 0.5 + (double)is[1][1] * 0.5;
            double n = (double)k + 0.5 + (double)is[1][2] * 0.5;
            double o = h - e;
            double p = (m - f) * 2.0;
            double q = n - g;
            if (o == 0.0) {
                x = (double)i + 0.5;
                d = z - (double)k;
            } else if (q == 0.0) {
                z = (double)k + 0.5;
                d = x - (double)i;
            } else {
                double t;
                double r = x - e;
                double s = z - g;
                d = t = (r * o + s * q) * 2.0;
            }
            x = e + o * d;
            y = f + p * d;
            z = g + q * d;
            if (p < 0.0) {
                y += 1.0;
            }
            if (p > 0.0) {
                y += 0.5;
            }
            return Vec3d.fromPool(x, y, z);
        }
        return null;
    }

    protected void writeCustomNbt(NbtCompound nbt) {
        nbt.putInt("Type", this.type);
        if (this.type == 2) {
            nbt.putDouble("PushX", this.pushX);
            nbt.putDouble("PushZ", this.pushZ);
            nbt.putShort("Fuel", (short)this.fuel);
        } else if (this.type == 1) {
            NbtList nbtList = new NbtList();
            for (int i = 0; i < this.inventory.length; ++i) {
                if (this.inventory[i] == null) continue;
                NbtCompound nbtCompound = new NbtCompound();
                nbtCompound.putByte("Slot", (byte)i);
                this.inventory[i].writeNbt(nbtCompound);
                nbtList.addElement(nbtCompound);
            }
            nbt.put("Items", nbtList);
        }
    }

    protected void readCustomNbt(NbtCompound nbt) {
        this.type = nbt.getInt("Type");
        if (this.type == 2) {
            this.pushX = nbt.getDouble("PushX");
            this.pushZ = nbt.getDouble("PushZ");
            this.fuel = nbt.getShort("Fuel");
        } else if (this.type == 1) {
            NbtList nbtList = nbt.getList("Items");
            this.inventory = new ItemStack[this.getSize()];
            for (int i = 0; i < nbtList.size(); ++i) {
                NbtCompound nbtCompound = (NbtCompound)nbtList.get(i);
                int j = nbtCompound.getByte("Slot") & 0xFF;
                if (j < 0 || j >= this.inventory.length) continue;
                this.inventory[j] = new ItemStack(nbtCompound);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return 0.0f;
    }

    public void push(Entity entity) {
        double e;
        double d;
        double f;
        if (this.world.isMultiplayer) {
            return;
        }
        if (entity == this.rider) {
            return;
        }
        if (entity instanceof MobEntity && !(entity instanceof PlayerEntity) && this.type == 0 && this.velocityX * this.velocityX + this.velocityZ * this.velocityZ > 0.01 && this.rider == null && entity.vehicle == null) {
            entity.startRiding(this);
        }
        if ((f = (d = entity.x - this.x) * d + (e = entity.z - this.z) * e) >= (double)1.0E-4f) {
            f = MathHelper.sqrt(f);
            d /= f;
            e /= f;
            double g = 1.0 / f;
            if (g > 1.0) {
                g = 1.0;
            }
            d *= g;
            e *= g;
            d *= (double)0.1f;
            e *= (double)0.1f;
            d *= (double)(1.0f - this.pushSpeedReduction);
            e *= (double)(1.0f - this.pushSpeedReduction);
            d *= 0.5;
            e *= 0.5;
            if (entity instanceof MinecartEntity) {
                double h = entity.velocityX + this.velocityX;
                double i = entity.velocityZ + this.velocityZ;
                if (((MinecartEntity)entity).type == 2 && this.type != 2) {
                    this.velocityX *= (double)0.2f;
                    this.velocityZ *= (double)0.2f;
                    this.addVelocity(entity.velocityX - d, 0.0, entity.velocityZ - e);
                    entity.velocityX *= (double)0.7f;
                    entity.velocityZ *= (double)0.7f;
                } else if (((MinecartEntity)entity).type != 2 && this.type == 2) {
                    entity.velocityX *= (double)0.2f;
                    entity.velocityZ *= (double)0.2f;
                    entity.addVelocity(this.velocityX + d, 0.0, this.velocityZ + e);
                    this.velocityX *= (double)0.7f;
                    this.velocityZ *= (double)0.7f;
                } else {
                    this.velocityX *= (double)0.2f;
                    this.velocityZ *= (double)0.2f;
                    this.addVelocity((h /= 2.0) - d, 0.0, (i /= 2.0) - e);
                    entity.velocityX *= (double)0.2f;
                    entity.velocityZ *= (double)0.2f;
                    entity.addVelocity(h + d, 0.0, i + e);
                }
            } else {
                this.addVelocity(-d, 0.0, -e);
                entity.addVelocity(d / 4.0, 0.0, e / 4.0);
            }
        }
    }

    public int getSize() {
        return 27;
    }

    public ItemStack getItem(int slot) {
        return this.inventory[slot];
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack removeItem(int slot, int amount) {
        if (this.inventory[slot] != null) {
            if (this.inventory[slot].size <= amount) {
                ItemStack itemStack = this.inventory[slot];
                this.inventory[slot] = null;
                return itemStack;
            }
            ItemStack itemStack2 = this.inventory[slot].split(amount);
            if (this.inventory[slot].size == 0) {
                this.inventory[slot] = null;
            }
            return itemStack2;
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public void setItem(int slot, ItemStack item) {
        this.inventory[slot] = item;
        if (item != null && item.size > this.getMaxStackSize()) {
            item.size = this.getMaxStackSize();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public String getInventoryName() {
        return "Minecart";
    }

    @Environment(value=EnvType.CLIENT)
    public int getMaxStackSize() {
        return 64;
    }

    @Environment(value=EnvType.CLIENT)
    public void markDirty() {
    }

    public boolean interact(PlayerEntity player) {
        if (this.type == 0) {
            if (this.rider != null && this.rider instanceof PlayerEntity && this.rider != player) {
                return true;
            }
            if (!this.world.isMultiplayer) {
                player.startRiding(this);
            }
        } else if (this.type == 1) {
            player.openChestMenu(this);
        } else if (this.type == 2) {
            ItemStack itemStack = player.inventory.getSelectedItem();
            if (itemStack != null && itemStack.id == Item.COAL.id) {
                if (--itemStack.size == 0) {
                    player.inventory.setItem(player.inventory.selectedSlot, null);
                }
                this.fuel += 1200;
            }
            this.pushX = this.x - player.x;
            this.pushZ = this.z - player.z;
        }
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = steps + 2;
        this.velocityX = this.lerpVelocityX;
        this.velocityY = this.lerpVelocityY;
        this.velocityZ = this.lerpVelocityZ;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpVelocity(double velocityX, double velocityY, double velocityZ) {
        this.lerpVelocityX = this.velocityX = velocityX;
        this.lerpVelocityY = this.velocityY = velocityY;
        this.lerpVelocityZ = this.velocityZ = velocityZ;
    }
}

