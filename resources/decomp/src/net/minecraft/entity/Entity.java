/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity;

import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entities;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class Entity {
    private static int nextNetworkId = 0;
    public int networkId = nextNetworkId++;
    public double viewDistanceScaling = 1.0;
    public boolean blocksBuilding = false;
    public Entity rider;
    public Entity vehicle;
    public World world;
    public double lastX;
    public double lastY;
    public double lastZ;
    public double x;
    public double y;
    public double z;
    public double velocityX;
    public double velocityY;
    public double velocityZ;
    public float yaw;
    public float pitch;
    public float lastYaw;
    public float lastPitch;
    public final Box shape = Box.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    public boolean onGround = false;
    public boolean collidingHorizontally;
    public boolean collidingVertically;
    public boolean colliding = false;
    public boolean damaged = false;
    public boolean immovable = true;
    public boolean removed = false;
    public float eyeHeight = 0.0f;
    public float width = 0.6f;
    public float height = 1.8f;
    public float lastWalkDistance = 0.0f;
    public float walkDistance = 0.0f;
    protected boolean makesSteps = true;
    protected float fallDistance = 0.0f;
    private int blocksWalkedOn = 1;
    public double prevX;
    public double prevY;
    public double prevZ;
    public float eyeHeightSneakOffset = 0.0f;
    public float stepHeight = 0.0f;
    public boolean noClip = false;
    public float pushSpeedReduction = 0.0f;
    public boolean f_09841856 = false;
    protected Random random = new Random();
    public int ticks = 0;
    public int safeOnFireTime = 1;
    public int onFireTimer = 0;
    protected int breathCapacity = 300;
    protected boolean inWater = false;
    public int invulnerableTimer = 0;
    public int breath = 300;
    private boolean firstTick = true;
    @Environment(value=EnvType.CLIENT)
    public String skin;
    protected boolean immuneToFire = false;
    private double ridingEntityPitchDelta;
    private double ridingEntityYawDelta;
    public boolean inChunk = false;
    public int chunkX;
    public int chunkY;
    public int chunkZ;
    @Environment(value=EnvType.CLIENT)
    public int lastKnownX;
    @Environment(value=EnvType.CLIENT)
    public int lastKnownY;
    @Environment(value=EnvType.CLIENT)
    public int lastKnownZ;
    @Environment(value=EnvType.CLIENT)
    public boolean hasVehicle;
    @Environment(value=EnvType.CLIENT)
    public boolean onFire;
    @Environment(value=EnvType.CLIENT)
    public boolean sneaking;

    public Entity(World world) {
        this.world = world;
        this.setPosition(0.0, 0.0, 0.0);
    }

    public boolean equals(Object object) {
        if (object instanceof Entity) {
            return ((Entity)object).networkId == this.networkId;
        }
        return false;
    }

    public int hashCode() {
        return this.networkId;
    }

    @Environment(value=EnvType.CLIENT)
    protected void resetPos() {
        if (this.world == null) {
            return;
        }
        while (this.y > 0.0) {
            this.setPosition(this.x, this.y, this.z);
            if (this.world.getCollisions(this, this.shape).size() == 0) break;
            this.y += 1.0;
        }
        this.velocityZ = 0.0;
        this.velocityY = 0.0;
        this.velocityX = 0.0;
        this.pitch = 0.0f;
    }

    public void remove() {
        this.removed = true;
    }

    protected void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    protected void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        float f = this.width / 2.0f;
        float g = this.height;
        this.shape.set(x - (double)f, y - (double)this.eyeHeight + (double)this.eyeHeightSneakOffset, z - (double)f, x + (double)f, y - (double)this.eyeHeight + (double)this.eyeHeightSneakOffset + (double)g, z + (double)f);
    }

    @Environment(value=EnvType.CLIENT)
    public void updateLocalPlayerCamera(float yaw, float pitch) {
        float f = this.pitch;
        float g = this.yaw;
        this.yaw = (float)((double)this.yaw + (double)yaw * 0.15);
        this.pitch = (float)((double)this.pitch - (double)pitch * 0.15);
        if (this.pitch < -90.0f) {
            this.pitch = -90.0f;
        }
        if (this.pitch > 90.0f) {
            this.pitch = 90.0f;
        }
        this.lastPitch += this.pitch - f;
        this.lastYaw += this.yaw - g;
    }

    public void tick() {
        this.baseTick();
    }

    public void baseTick() {
        if (this.vehicle != null && this.vehicle.removed) {
            this.vehicle = null;
        }
        ++this.ticks;
        this.lastWalkDistance = this.walkDistance;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.lastPitch = this.pitch;
        this.lastYaw = this.yaw;
        if (this.checkWaterCollisions()) {
            if (!this.inWater && !this.firstTick) {
                float f = MathHelper.sqrt(this.velocityX * this.velocityX * (double)0.2f + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ * (double)0.2f) * 0.2f;
                if (f > 1.0f) {
                    f = 1.0f;
                }
                this.world.playSound(this, "random.splash", f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.4f);
                float g = MathHelper.floor(this.shape.minY);
                int i = 0;
                while ((float)i < 1.0f + this.width * 20.0f) {
                    float h = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    float k = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    this.world.addParticle("bubble", this.x + (double)h, g + 1.0f, this.z + (double)k, this.velocityX, this.velocityY - (double)(this.random.nextFloat() * 0.2f), this.velocityZ);
                    ++i;
                }
                i = 0;
                while ((float)i < 1.0f + this.width * 20.0f) {
                    float j = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    float l = (this.random.nextFloat() * 2.0f - 1.0f) * this.width;
                    this.world.addParticle("splash", this.x + (double)j, g + 1.0f, this.z + (double)l, this.velocityX, this.velocityY, this.velocityZ);
                    ++i;
                }
            }
            this.fallDistance = 0.0f;
            this.inWater = true;
            this.onFireTimer = 0;
        } else {
            this.inWater = false;
        }
        if (this.world.isMultiplayer) {
            this.onFireTimer = 0;
        } else if (this.onFireTimer > 0) {
            if (this.immuneToFire) {
                this.onFireTimer -= 4;
                if (this.onFireTimer < 0) {
                    this.onFireTimer = 0;
                }
            } else {
                if (this.onFireTimer % 20 == 0) {
                    this.takeDamage(null, 1);
                }
                --this.onFireTimer;
            }
        }
        if (this.isInLava()) {
            this.takeLavaDamage();
        }
        if (this.y < -64.0) {
            this.voidTick();
        }
        this.firstTick = false;
    }

    protected void takeLavaDamage() {
        if (!this.immuneToFire) {
            this.takeDamage(null, 4);
            this.onFireTimer = 600;
        }
    }

    protected void voidTick() {
        this.remove();
    }

    public boolean canMove(double dx, double dy, double dz) {
        Box box = this.shape.moved(dx, dy, dz);
        List list = this.world.getCollisions(this, box);
        if (list.size() > 0) {
            return false;
        }
        return !this.world.containsLiquid(box);
    }

    public void move(double dx, double dy, double dz) {
        int l;
        int k;
        boolean i;
        if (this.noClip) {
            this.shape.move(dx, dy, dz);
            this.x = (this.shape.minX + this.shape.maxX) / 2.0;
            this.y = this.shape.minY + (double)this.eyeHeight - (double)this.eyeHeightSneakOffset;
            this.z = (this.shape.minZ + this.shape.maxZ) / 2.0;
            return;
        }
        double d = this.x;
        double e = this.z;
        double f = dx;
        double g = dy;
        double h = dz;
        Box box = this.shape.copy();
        boolean bl = i = this.onGround && this.isSneaking();
        if (i) {
            double j = 0.05;
            while (dx != 0.0 && this.world.getCollisions(this, this.shape.moved(dx, -1.0, 0.0)).size() == 0) {
                dx = dx < j && dx >= -j ? 0.0 : (dx > 0.0 ? (dx -= j) : (dx += j));
                f = dx;
            }
            while (dz != 0.0 && this.world.getCollisions(this, this.shape.moved(0.0, -1.0, dz)).size() == 0) {
                dz = dz < j && dz >= -j ? 0.0 : (dz > 0.0 ? (dz -= j) : (dz += j));
                h = dz;
            }
        }
        List list = this.world.getCollisions(this, this.shape.expanded(dx, dy, dz));
        for (k = 0; k < list.size(); ++k) {
            dy = ((Box)list.get(k)).intersectY(this.shape, dy);
        }
        this.shape.move(0.0, dy, 0.0);
        if (!this.immovable && g != dy) {
            dz = 0.0;
            dy = 0.0;
            dx = 0.0;
        }
        k = this.onGround || g != dy && g < 0.0 ? 1 : 0;
        for (l = 0; l < list.size(); ++l) {
            dx = ((Box)list.get(l)).intersectX(this.shape, dx);
        }
        this.shape.move(dx, 0.0, 0.0);
        if (!this.immovable && f != dx) {
            dz = 0.0;
            dy = 0.0;
            dx = 0.0;
        }
        for (l = 0; l < list.size(); ++l) {
            dz = ((Box)list.get(l)).intersectZ(this.shape, dz);
        }
        this.shape.move(0.0, 0.0, dz);
        if (!this.immovable && h != dz) {
            dz = 0.0;
            dy = 0.0;
            dx = 0.0;
        }
        if (this.stepHeight > 0.0f && k != 0 && this.eyeHeightSneakOffset < 0.05f && (f != dx || h != dz)) {
            int x;
            double m = dx;
            double o = dy;
            double q = dz;
            dx = f;
            dy = this.stepHeight;
            dz = h;
            Box box2 = this.shape.copy();
            this.shape.set(box);
            list = this.world.getCollisions(this, this.shape.expanded(dx, dy, dz));
            for (x = 0; x < list.size(); ++x) {
                dy = ((Box)list.get(x)).intersectY(this.shape, dy);
            }
            this.shape.move(0.0, dy, 0.0);
            if (!this.immovable && g != dy) {
                dz = 0.0;
                dy = 0.0;
                dx = 0.0;
            }
            for (x = 0; x < list.size(); ++x) {
                dx = ((Box)list.get(x)).intersectX(this.shape, dx);
            }
            this.shape.move(dx, 0.0, 0.0);
            if (!this.immovable && f != dx) {
                dz = 0.0;
                dy = 0.0;
                dx = 0.0;
            }
            for (x = 0; x < list.size(); ++x) {
                dz = ((Box)list.get(x)).intersectZ(this.shape, dz);
            }
            this.shape.move(0.0, 0.0, dz);
            if (!this.immovable && h != dz) {
                dz = 0.0;
                dy = 0.0;
                dx = 0.0;
            }
            if (m * m + q * q >= dx * dx + dz * dz) {
                dx = m;
                dy = o;
                dz = q;
                this.shape.set(box2);
            } else {
                this.eyeHeightSneakOffset = (float)((double)this.eyeHeightSneakOffset + 0.5);
            }
        }
        this.x = (this.shape.minX + this.shape.maxX) / 2.0;
        this.y = this.shape.minY + (double)this.eyeHeight - (double)this.eyeHeightSneakOffset;
        this.z = (this.shape.minZ + this.shape.maxZ) / 2.0;
        this.collidingHorizontally = f != dx || h != dz;
        this.collidingVertically = g != dy;
        this.onGround = g != dy && g < 0.0;
        this.colliding = this.collidingHorizontally || this.collidingVertically;
        this.checkFallDamage(dy, this.onGround);
        if (f != dx) {
            this.velocityX = 0.0;
        }
        if (g != dy) {
            this.velocityY = 0.0;
        }
        if (h != dz) {
            this.velocityZ = 0.0;
        }
        double n = this.x - d;
        double p = this.z - e;
        if (this.makesSteps && !i) {
            this.walkDistance = (float)((double)this.walkDistance + (double)MathHelper.sqrt(n * n + p * p) * 0.6);
            int r = MathHelper.floor(this.x);
            int t = MathHelper.floor(this.y - (double)0.2f - (double)this.eyeHeight);
            int v = MathHelper.floor(this.z);
            int y = this.world.getBlock(r, t, v);
            if (this.walkDistance > (float)this.blocksWalkedOn && y > 0) {
                ++this.blocksWalkedOn;
                Block.Sounds sounds = Block.BY_ID[y].sounds;
                if (this.world.getBlock(r, t + 1, v) == Block.SNOW_LAYER.id) {
                    sounds = Block.SNOW_LAYER.sounds;
                    this.world.playSound(this, sounds.getStepping(), sounds.getVolume() * 0.15f, sounds.getPitch());
                } else if (!Block.BY_ID[y].material.isLiquid()) {
                    this.world.playSound(this, sounds.getStepping(), sounds.getVolume() * 0.15f, sounds.getPitch());
                }
                Block.BY_ID[y].onSteppedOn(this.world, r, t, v, this);
            }
        }
        int s = MathHelper.floor(this.shape.minX);
        int u = MathHelper.floor(this.shape.minY);
        int w = MathHelper.floor(this.shape.minZ);
        int z = MathHelper.floor(this.shape.maxX);
        int aa = MathHelper.floor(this.shape.maxY);
        int ab = MathHelper.floor(this.shape.maxZ);
        for (int ac = s; ac <= z; ++ac) {
            for (int ad = u; ad <= aa; ++ad) {
                for (int ae = w; ae <= ab; ++ae) {
                    int af = this.world.getBlock(ac, ad, ae);
                    if (af <= 0) continue;
                    Block.BY_ID[af].onEntityCollision(this.world, ac, ad, ae, this);
                }
            }
        }
        this.eyeHeightSneakOffset *= 0.4f;
        s = this.checkWaterCollisions() ? 1 : 0;
        if (this.world.containsFireSource(this.shape)) {
            this.takeFireDamage(1);
            if (s == 0) {
                ++this.onFireTimer;
                if (this.onFireTimer == 0) {
                    this.onFireTimer = 300;
                }
            }
        } else if (this.onFireTimer <= 0) {
            this.onFireTimer = -this.safeOnFireTime;
        }
        if (s != 0 && this.onFireTimer > 0) {
            this.world.playSound(this, "random.fizz", 0.7f, 1.6f + (this.random.nextFloat() - this.random.nextFloat()) * 0.4f);
            this.onFireTimer = -this.safeOnFireTime;
        }
    }

    protected void checkFallDamage(double dy, boolean onGround) {
        if (onGround) {
            if (this.fallDistance > 0.0f) {
                this.takeFallDamage(this.fallDistance);
                this.fallDistance = 0.0f;
            }
        } else if (dy < 0.0) {
            this.fallDistance = (float)((double)this.fallDistance - dy);
        }
    }

    public boolean isSneaking() {
        return false;
    }

    public Box getCollisionShape() {
        return null;
    }

    protected void takeFireDamage(int amount) {
        if (!this.immuneToFire) {
            this.takeDamage(null, amount);
        }
    }

    protected void takeFallDamage(float distance) {
    }

    public boolean checkWaterCollisions() {
        return this.world.applyLiquidDrag(this.shape.grown(0.0, -0.4f, 0.0), Material.WATER, this);
    }

    public boolean isSubmergedIn(Material liquid) {
        int k;
        int j;
        double d = this.y + (double)this.getEyeHeight();
        int i = MathHelper.floor(this.x);
        int l = this.world.getBlock(i, j = MathHelper.floor(MathHelper.floor(d)), k = MathHelper.floor(this.z));
        if (l != 0 && Block.BY_ID[l].material == liquid) {
            float f = LiquidBlock.getHeightLoss(this.world.getBlockMetadata(i, j, k)) - 0.11111111f;
            float g = (float)(j + 1) - f;
            return d < (double)g;
        }
        return false;
    }

    public float getEyeHeight() {
        return 0.0f;
    }

    public boolean isInLava() {
        return this.world.containsMaterial(this.shape.grown(0.0, -0.4f, 0.0), Material.LAVA);
    }

    public void updateVelocity(float sideways, float forwards, float scale) {
        float f = MathHelper.sqrt(sideways * sideways + forwards * forwards);
        if (f < 0.01f) {
            return;
        }
        if (f < 1.0f) {
            f = 1.0f;
        }
        f = scale / f;
        float g = MathHelper.sin(this.yaw * (float)Math.PI / 180.0f);
        float h = MathHelper.cos(this.yaw * (float)Math.PI / 180.0f);
        this.velocityX += (double)((sideways *= f) * h - (forwards *= f) * g);
        this.velocityZ += (double)(forwards * h + sideways * g);
    }

    public float getBrightness(float tickDelta) {
        int i = MathHelper.floor(this.x);
        double d = (this.shape.maxY - this.shape.minY) * 0.66;
        int j = MathHelper.floor(this.y - (double)this.eyeHeight + d);
        int k = MathHelper.floor(this.z);
        return this.world.getBrightness(i, j, k);
    }

    @Environment(value=EnvType.CLIENT)
    public void setWorld(World world) {
        this.world = world;
    }

    public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.lastX = this.x = x;
        this.lastY = this.y = y;
        this.lastZ = this.z = z;
        this.lastYaw = this.yaw = yaw;
        this.lastPitch = this.pitch = pitch;
        this.eyeHeightSneakOffset = 0.0f;
        double d = this.lastYaw - yaw;
        if (d < -180.0) {
            this.lastYaw += 360.0f;
        }
        if (d >= 180.0) {
            this.lastYaw -= 360.0f;
        }
        this.setPosition(this.x, this.y, this.z);
        this.setRotation(yaw, pitch);
    }

    public void setPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.lastX = this.x = x;
        this.lastY = this.y = y + (double)this.eyeHeight;
        this.lastZ = this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.setPosition(this.x, this.y, this.z);
    }

    public float distanceTo(Entity entity) {
        float f = (float)(this.x - entity.x);
        float g = (float)(this.y - entity.y);
        float h = (float)(this.z - entity.z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public double squaredDistanceTo(double x, double y, double z) {
        double d = this.x - x;
        double e = this.y - y;
        double f = this.z - z;
        return d * d + e * e + f * f;
    }

    public double distanceTo(double x, double y, double z) {
        double d = this.x - x;
        double e = this.y - y;
        double f = this.z - z;
        return MathHelper.sqrt(d * d + e * e + f * f);
    }

    public double squaredDistanceTo(Entity entity) {
        double d = this.x - entity.x;
        double e = this.y - entity.y;
        double f = this.z - entity.z;
        return d * d + e * e + f * f;
    }

    public void onPlayerCollision(PlayerEntity player) {
    }

    public void push(Entity entity) {
        if (entity.rider == this || entity.vehicle == this) {
            return;
        }
        double d = entity.x - this.x;
        double e = entity.z - this.z;
        double f = MathHelper.absMax(d, e);
        if (f >= (double)0.01f) {
            f = MathHelper.sqrt(f);
            d /= f;
            e /= f;
            double g = 1.0 / f;
            if (g > 1.0) {
                g = 1.0;
            }
            d *= g;
            e *= g;
            d *= (double)0.05f;
            e *= (double)0.05f;
            this.addVelocity(-(d *= (double)(1.0f - this.pushSpeedReduction)), 0.0, -(e *= (double)(1.0f - this.pushSpeedReduction)));
            entity.addVelocity(d, 0.0, e);
        }
    }

    public void addVelocity(double dx, double dy, double dz) {
        this.velocityX += dx;
        this.velocityY += dy;
        this.velocityZ += dz;
    }

    protected void markDamaged() {
        this.damaged = true;
    }

    public boolean takeDamage(Entity source, int amount) {
        this.markDamaged();
        return false;
    }

    public boolean hasCollision() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    public void takeKillScore(Entity victim, int score) {
    }

    @Environment(value=EnvType.CLIENT)
    public boolean shouldRender(Vec3d cameraPos) {
        double d = this.x - cameraPos.x;
        double e = this.y - cameraPos.y;
        double f = this.z - cameraPos.z;
        double g = d * d + e * e + f * f;
        return this.shouldRender(g);
    }

    @Environment(value=EnvType.CLIENT)
    public boolean shouldRender(double squaredDistanceToCamera) {
        double d = this.shape.getAverageSideLength();
        return squaredDistanceToCamera < (d *= 64.0 * this.viewDistanceScaling) * d;
    }

    @Environment(value=EnvType.CLIENT)
    public String getTexture() {
        return null;
    }

    public boolean writeNbt(NbtCompound nbt) {
        String string = this.getTypeId();
        if (this.removed || string == null) {
            return false;
        }
        nbt.putString("id", string);
        this.writeNbtWithoutId(nbt);
        return true;
    }

    public void writeNbtWithoutId(NbtCompound nbt) {
        nbt.put("Pos", this.toNbtList(this.x, this.y, this.z));
        nbt.put("Motion", this.toNbtList(this.velocityX, this.velocityY, this.velocityZ));
        nbt.put("Rotation", this.toNbtList(this.yaw, this.pitch));
        nbt.putFloat("FallDistance", this.fallDistance);
        nbt.putShort("Fire", (short)this.onFireTimer);
        nbt.putShort("Air", (short)this.breath);
        nbt.putBoolean("OnGround", this.onGround);
        this.writeCustomNbt(nbt);
    }

    public void readNbt(NbtCompound nbt) {
        NbtList nbtList = nbt.getList("Pos");
        NbtList nbtList2 = nbt.getList("Motion");
        NbtList nbtList3 = nbt.getList("Rotation");
        this.setPosition(0.0, 0.0, 0.0);
        this.velocityX = ((NbtDouble)nbtList2.get((int)0)).value;
        this.velocityY = ((NbtDouble)nbtList2.get((int)1)).value;
        this.velocityZ = ((NbtDouble)nbtList2.get((int)2)).value;
        this.prevX = this.x = ((NbtDouble)nbtList.get((int)0)).value;
        this.lastX = this.x;
        this.prevY = this.y = ((NbtDouble)nbtList.get((int)1)).value;
        this.lastY = this.y;
        this.prevZ = this.z = ((NbtDouble)nbtList.get((int)2)).value;
        this.lastZ = this.z;
        this.lastYaw = this.yaw = ((NbtFloat)nbtList3.get((int)0)).value;
        this.lastPitch = this.pitch = ((NbtFloat)nbtList3.get((int)1)).value;
        this.fallDistance = nbt.getFloat("FallDistance");
        this.onFireTimer = nbt.getShort("Fire");
        this.breath = nbt.getShort("Air");
        this.onGround = nbt.getBoolean("OnGround");
        this.setPosition(this.x, this.y, this.z);
        this.readCustomNbt(nbt);
    }

    protected final String getTypeId() {
        return Entities.getKey(this);
    }

    protected abstract void readCustomNbt(NbtCompound var1);

    protected abstract void writeCustomNbt(NbtCompound var1);

    protected NbtList toNbtList(double ... values) {
        int i;
        NbtList nbtList = new NbtList();
        double[] ds = values;
        int n = ds.length;
        for (int j = 0; j < i; ++j) {
            double d = ds[j];
            nbtList.addElement(new NbtDouble(d));
        }
        return nbtList;
    }

    protected NbtList toNbtList(float ... values) {
        int i;
        NbtList nbtList = new NbtList();
        float[] fs = values;
        int n = fs.length;
        for (int j = 0; j < i; ++j) {
            float f = fs[j];
            nbtList.addElement(new NbtFloat(f));
        }
        return nbtList;
    }

    @Environment(value=EnvType.CLIENT)
    public float getShadowHeightOffset() {
        return this.height / 2.0f;
    }

    public ItemEntity dropItem(int item, int amount) {
        return this.dropItem(item, amount, 0.0f);
    }

    public ItemEntity dropItem(int item, int amount, float offsetY) {
        ItemEntity itemEntity = new ItemEntity(this.world, this.x, this.y + (double)offsetY, this.z, new ItemStack(item, amount));
        itemEntity.pickUpDelay = 10;
        this.world.addEntity(itemEntity);
        return itemEntity;
    }

    public boolean isAlive() {
        return !this.removed;
    }

    public boolean isInWall() {
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(this.y + (double)this.getEyeHeight());
        int k = MathHelper.floor(this.z);
        return this.world.isSolidBlock(i, j, k);
    }

    public boolean interact(PlayerEntity player) {
        return false;
    }

    public Box getCollisionAgainstShape(Entity other) {
        return null;
    }

    public void rideTick() {
        if (this.vehicle.removed) {
            this.vehicle = null;
            return;
        }
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
        this.tick();
        this.vehicle.updateRiderPositon();
        this.ridingEntityYawDelta += (double)(this.vehicle.yaw - this.vehicle.lastYaw);
        this.ridingEntityPitchDelta += (double)(this.vehicle.pitch - this.vehicle.lastPitch);
        while (this.ridingEntityYawDelta >= 180.0) {
            this.ridingEntityYawDelta -= 360.0;
        }
        while (this.ridingEntityYawDelta < -180.0) {
            this.ridingEntityYawDelta += 360.0;
        }
        while (this.ridingEntityPitchDelta >= 180.0) {
            this.ridingEntityPitchDelta -= 360.0;
        }
        while (this.ridingEntityPitchDelta < -180.0) {
            this.ridingEntityPitchDelta += 360.0;
        }
        double d = this.ridingEntityYawDelta * 0.5;
        double e = this.ridingEntityPitchDelta * 0.5;
        float f = 10.0f;
        if (d > (double)f) {
            d = f;
        }
        if (d < (double)(-f)) {
            d = -f;
        }
        if (e > (double)f) {
            e = f;
        }
        if (e < (double)(-f)) {
            e = -f;
        }
        this.ridingEntityYawDelta -= d;
        this.ridingEntityPitchDelta -= e;
        this.yaw = (float)((double)this.yaw + d);
        this.pitch = (float)((double)this.pitch + e);
    }

    public void updateRiderPositon() {
        this.rider.setPosition(this.x, this.y + this.getMountHeight() + this.rider.getRideHeight(), this.z);
    }

    public double getRideHeight() {
        return this.eyeHeight;
    }

    public double getMountHeight() {
        return (double)this.height * 0.75;
    }

    public void startRiding(Entity entity) {
        this.ridingEntityPitchDelta = 0.0;
        this.ridingEntityYawDelta = 0.0;
        if (entity == null) {
            if (this.vehicle != null) {
                this.setPositionAndAngles(this.vehicle.x, this.vehicle.shape.minY + (double)this.vehicle.height, this.vehicle.z, this.yaw, this.pitch);
                this.vehicle.rider = null;
            }
            this.vehicle = null;
            return;
        }
        if (this.vehicle == entity) {
            this.vehicle.rider = null;
            this.vehicle = null;
            this.setPositionAndAngles(entity.x, entity.shape.minY + (double)entity.height, entity.z, this.yaw, this.pitch);
            return;
        }
        if (this.vehicle != null) {
            this.vehicle.rider = null;
        }
        if (entity.rider != null) {
            entity.rider.vehicle = null;
        }
        this.vehicle = entity;
        entity.rider = this;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        this.setPosition(x, y, z);
        this.setRotation(yaw, pitch);
    }

    @Environment(value=EnvType.CLIENT)
    public float getPickRadius() {
        return 0.1f;
    }

    public Vec3d getLookVector() {
        return null;
    }

    public void onPortalCollision() {
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpVelocity(double velocityX, double velocityY, double velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    @Environment(value=EnvType.CLIENT)
    public void doEvent(byte event) {
    }

    @Environment(value=EnvType.CLIENT)
    public void animateDamage() {
    }
}

