/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity.mob;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class MobEntity
extends Entity {
    public int invulnerableTicks = 20;
    public float f_26584770;
    public float f_26890866;
    public float f_75186175;
    public float bodyYaw = 0.0f;
    public float lastBodyYaw = 0.0f;
    protected float lastWalkProgress;
    protected float walkProgress;
    protected float totalWalkDistance;
    protected float lastTotalWalkDistance;
    protected boolean f_15736313 = true;
    protected String texture = "/mob/char.png";
    protected boolean f_92491955 = true;
    protected float rotationOffset = 0.0f;
    protected String modelName = null;
    protected float f_94758649 = 1.0f;
    protected int score = 0;
    protected float lastDamageTaken = 0.0f;
    public boolean interpolateOnly = false;
    public float lastAttackAnimationProgress;
    public float attackAnimationProgress;
    public int health = 10;
    public int prevHealth;
    private int ambientSoundTimer;
    public int damagedTimer;
    public int damagedTime;
    public float damagedSwingDir = 0.0f;
    public int deathTicks = 0;
    public int attackTimer = 0;
    public float lastTilt;
    public float tilt;
    protected boolean dead = false;
    public int f_08869654 = -1;
    public float f_92185951 = (float)(Math.random() * (double)0.9f + (double)0.1f);
    public float lastWalkAnimationSpeed;
    public float walkAnimationSpeed;
    public float walkAnimationProgress;
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected double lerpYaw;
    protected double lerpPitch;
    float f_40453667 = 0.0f;
    protected int prevDamageTaken = 0;
    protected int farFromPlayerTicks = 0;
    protected float sidewaysSpeed;
    protected float forwardSpeed;
    protected float rotationSpeed;
    protected boolean jumping = false;
    protected float defaultPitch = 0.0f;
    protected float walkSpeed = 0.7f;
    private Entity lookTarget;
    private int lookTimer = 0;

    public MobEntity(World world) {
        super(world);
        this.blocksBuilding = true;
        this.f_75186175 = (float)(Math.random() + 1.0) * 0.01f;
        this.setPosition(this.x, this.y, this.z);
        this.f_26584770 = (float)Math.random() * 12398.0f;
        this.yaw = (float)(Math.random() * 3.1415927410125732 * 2.0);
        this.f_26890866 = 1.0f;
        this.stepHeight = 0.5f;
    }

    public boolean canSee(Entity entity) {
        return this.world.rayTrace(Vec3d.fromPool(this.x, this.y + (double)this.getEyeHeight(), this.z), Vec3d.fromPool(entity.x, entity.y + (double)entity.getEyeHeight(), entity.z)) == null;
    }

    @Environment(value=EnvType.CLIENT)
    public String getTexture() {
        return this.texture;
    }

    public boolean hasCollision() {
        return !this.removed;
    }

    public boolean isPushable() {
        return !this.removed;
    }

    public float getEyeHeight() {
        return this.height * 0.85f;
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void baseTick() {
        this.lastAttackAnimationProgress = this.attackAnimationProgress;
        super.baseTick();
        if (this.random.nextInt(1000) < this.ambientSoundTimer++) {
            this.ambientSoundTimer = -this.getAmbientSoundInterval();
            String string = this.getAmbientSound();
            if (string != null) {
                this.world.playSound(this, string, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
            }
        }
        if (this.isAlive() && this.isInWall()) {
            this.takeDamage(null, 1);
        }
        if (this.immuneToFire || this.world.isMultiplayer) {
            this.onFireTimer = 0;
        }
        if (this.isAlive() && this.isSubmergedIn(Material.WATER)) {
            --this.breath;
            if (this.breath == -20) {
                this.breath = 0;
                for (int i = 0; i < 8; ++i) {
                    float f = this.random.nextFloat() - this.random.nextFloat();
                    float g = this.random.nextFloat() - this.random.nextFloat();
                    float h = this.random.nextFloat() - this.random.nextFloat();
                    this.world.addParticle("bubble", this.x + (double)f, this.y + (double)g, this.z + (double)h, this.velocityX, this.velocityY, this.velocityZ);
                }
                this.takeDamage(null, 2);
            }
            this.onFireTimer = 0;
        } else {
            this.breath = this.breathCapacity;
        }
        this.lastTilt = this.tilt;
        if (this.attackTimer > 0) {
            --this.attackTimer;
        }
        if (this.damagedTimer > 0) {
            --this.damagedTimer;
        }
        if (this.invulnerableTimer > 0) {
            --this.invulnerableTimer;
        }
        if (this.health <= 0) {
            ++this.deathTicks;
            if (this.deathTicks > 20) {
                this.beforeRemove();
                this.remove();
                for (int j = 0; j < 20; ++j) {
                    double d = this.random.nextGaussian() * 0.02;
                    double e = this.random.nextGaussian() * 0.02;
                    double k = this.random.nextGaussian() * 0.02;
                    this.world.addParticle("explode", this.x + (double)(this.random.nextFloat() * this.width * 2.0f) - (double)this.width, this.y + (double)(this.random.nextFloat() * this.height), this.z + (double)(this.random.nextFloat() * this.width * 2.0f) - (double)this.width, d, e, k);
                }
            }
        }
        this.lastTotalWalkDistance = this.totalWalkDistance;
        this.lastBodyYaw = this.bodyYaw;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
    }

    public void animateSpawn() {
        for (int i = 0; i < 20; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            double g = 10.0;
            this.world.addParticle("explode", this.x + (double)(this.random.nextFloat() * this.width * 2.0f) - (double)this.width - d * g, this.y + (double)(this.random.nextFloat() * this.height) - e * g, this.z + (double)(this.random.nextFloat() * this.width * 2.0f) - (double)this.width - f * g, d, e, f);
        }
    }

    public void rideTick() {
        super.rideTick();
        this.lastWalkProgress = this.walkProgress;
        this.walkProgress = 0.0f;
    }

    @Environment(value=EnvType.CLIENT)
    public void lerpPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        this.eyeHeight = 0.0f;
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = steps;
    }

    public void tick() {
        boolean l;
        float k;
        float j;
        super.tick();
        this.mobTick();
        double d = this.x - this.lastX;
        double e = this.z - this.lastZ;
        float f = MathHelper.sqrt(d * d + e * e);
        float g = this.bodyYaw;
        float h = 0.0f;
        this.lastWalkProgress = this.walkProgress;
        float i = 0.0f;
        if (!(f <= 0.05f)) {
            i = 1.0f;
            h = f * 3.0f;
            g = (float)Math.atan2(e, d) * 180.0f / (float)Math.PI - 90.0f;
        }
        if (this.attackAnimationProgress > 0.0f) {
            g = this.yaw;
        }
        if (!this.onGround) {
            i = 0.0f;
        }
        this.walkProgress += (i - this.walkProgress) * 0.3f;
        for (j = g - this.bodyYaw; j < -180.0f; j += 360.0f) {
        }
        while (j >= 180.0f) {
            j -= 360.0f;
        }
        this.bodyYaw += j * 0.3f;
        for (k = this.yaw - this.bodyYaw; k < -180.0f; k += 360.0f) {
        }
        while (k >= 180.0f) {
            k -= 360.0f;
        }
        boolean bl = l = k < -90.0f || k >= 90.0f;
        if (k < -75.0f) {
            k = -75.0f;
        }
        if (k >= 75.0f) {
            k = 75.0f;
        }
        this.bodyYaw = this.yaw - k;
        if (k * k > 2500.0f) {
            this.bodyYaw += k * 0.2f;
        }
        if (l) {
            h *= -1.0f;
        }
        while (this.yaw - this.lastYaw < -180.0f) {
            this.lastYaw -= 360.0f;
        }
        while (this.yaw - this.lastYaw >= 180.0f) {
            this.lastYaw += 360.0f;
        }
        while (this.bodyYaw - this.lastBodyYaw < -180.0f) {
            this.lastBodyYaw -= 360.0f;
        }
        while (this.bodyYaw - this.lastBodyYaw >= 180.0f) {
            this.lastBodyYaw += 360.0f;
        }
        while (this.pitch - this.lastPitch < -180.0f) {
            this.lastPitch -= 360.0f;
        }
        while (this.pitch - this.lastPitch >= 180.0f) {
            this.lastPitch += 360.0f;
        }
        this.totalWalkDistance += h;
    }

    protected void setSize(float width, float height) {
        super.setSize(width, height);
    }

    public void heal(int amount) {
        if (this.health <= 0) {
            return;
        }
        this.health += amount;
        if (this.health > 20) {
            this.health = 20;
        }
        this.invulnerableTimer = this.invulnerableTicks / 2;
    }

    public boolean takeDamage(Entity source, int amount) {
        if (this.world.isMultiplayer) {
            return false;
        }
        this.farFromPlayerTicks = 0;
        if (this.health <= 0) {
            return false;
        }
        this.walkAnimationSpeed = 1.5f;
        boolean i = true;
        if ((float)this.invulnerableTimer > (float)this.invulnerableTicks / 2.0f) {
            if (amount <= this.prevDamageTaken) {
                return false;
            }
            this.applyDamage(amount - this.prevDamageTaken);
            this.prevDamageTaken = amount;
            i = false;
        } else {
            this.prevDamageTaken = amount;
            this.prevHealth = this.health;
            this.invulnerableTimer = this.invulnerableTicks;
            this.applyDamage(amount);
            this.damagedTime = 10;
            this.damagedTimer = 10;
        }
        this.damagedSwingDir = 0.0f;
        if (i) {
            this.world.doEntityEvent(this, (byte)2);
            this.markDamaged();
            if (source != null) {
                double d = source.x - this.x;
                double e = source.z - this.z;
                while (d * d + e * e < 1.0E-4) {
                    d = (Math.random() - Math.random()) * 0.01;
                    e = (Math.random() - Math.random()) * 0.01;
                }
                this.damagedSwingDir = (float)(Math.atan2(e, d) * 180.0 / 3.1415927410125732) - this.yaw;
                this.applyKnockback(source, amount, d, e);
            } else {
                this.damagedSwingDir = (int)(Math.random() * 2.0) * 180;
            }
        }
        if (this.health <= 0) {
            if (i) {
                this.world.playSound(this, this.getDeathSound(), this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
            }
            this.die(source);
        } else if (i) {
            this.world.playSound(this, this.getHurtSound(), this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
        }
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public void animateDamage() {
        this.damagedTime = 10;
        this.damagedTimer = 10;
        this.damagedSwingDir = 0.0f;
    }

    protected void applyDamage(int amount) {
        this.health -= amount;
    }

    protected float getSoundVolume() {
        return 1.0f;
    }

    protected String getAmbientSound() {
        return null;
    }

    protected String getHurtSound() {
        return "random.hurt";
    }

    protected String getDeathSound() {
        return "random.hurt";
    }

    public void applyKnockback(Entity attacker, int amount, double dx, double dz) {
        float f = MathHelper.sqrt(dx * dx + dz * dz);
        float g = 0.4f;
        this.velocityX /= 2.0;
        this.velocityY /= 2.0;
        this.velocityZ /= 2.0;
        this.velocityX -= dx / (double)f * (double)g;
        this.velocityY += (double)0.4f;
        this.velocityZ -= dz / (double)f * (double)g;
        if (this.velocityY > (double)0.4f) {
            this.velocityY = 0.4f;
        }
    }

    public void die(Entity killer) {
        int i;
        if (this.score > 0 && killer != null) {
            killer.takeKillScore(this, this.score);
        }
        this.dead = true;
        if (!this.world.isMultiplayer && (i = this.getDropItem()) > 0) {
            int j = this.random.nextInt(3);
            for (int k = 0; k < j; ++k) {
                this.dropItem(i, 1);
            }
        }
        this.world.doEntityEvent(this, (byte)3);
    }

    protected int getDropItem() {
        return 0;
    }

    protected void takeFallDamage(float distance) {
        int i = (int)Math.ceil(distance - 3.0f);
        if (i > 0) {
            this.takeDamage(null, i);
            int j = this.world.getBlock(MathHelper.floor(this.x), MathHelper.floor(this.y - (double)0.2f - (double)this.eyeHeight), MathHelper.floor(this.z));
            if (j > 0) {
                Block.Sounds sounds = Block.BY_ID[j].sounds;
                this.world.playSound(this, sounds.getStepping(), sounds.getVolume() * 0.5f, sounds.getPitch() * 0.75f);
            }
        }
    }

    public void moveRelative(float sideways, float forwards) {
        if (this.checkWaterCollisions()) {
            double d = this.y;
            this.updateVelocity(sideways, forwards, 0.02f);
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= (double)0.8f;
            this.velocityY *= (double)0.8f;
            this.velocityZ *= (double)0.8f;
            this.velocityY -= 0.02;
            if (this.collidingHorizontally && this.canMove(this.velocityX, this.velocityY + (double)0.6f - this.y + d, this.velocityZ)) {
                this.velocityY = 0.3f;
            }
        } else if (this.isInLava()) {
            double e = this.y;
            this.updateVelocity(sideways, forwards, 0.02f);
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= 0.5;
            this.velocityY *= 0.5;
            this.velocityZ *= 0.5;
            this.velocityY -= 0.02;
            if (this.collidingHorizontally && this.canMove(this.velocityX, this.velocityY + (double)0.6f - this.y + e, this.velocityZ)) {
                this.velocityY = 0.3f;
            }
        } else {
            float f = 0.91f;
            if (this.onGround) {
                f = 0.54600006f;
                int i = this.world.getBlock(MathHelper.floor(this.x), MathHelper.floor(this.shape.minY) - 1, MathHelper.floor(this.z));
                if (i > 0) {
                    f = Block.BY_ID[i].slipperiness * 0.91f;
                }
            }
            float h = 0.16277136f / (f * f * f);
            this.updateVelocity(sideways, forwards, this.onGround ? 0.1f * h : 0.02f);
            f = 0.91f;
            if (this.onGround) {
                f = 0.54600006f;
                int j = this.world.getBlock(MathHelper.floor(this.x), MathHelper.floor(this.shape.minY) - 1, MathHelper.floor(this.z));
                if (j > 0) {
                    f = Block.BY_ID[j].slipperiness * 0.91f;
                }
            }
            if (this.isClimbing()) {
                this.fallDistance = 0.0f;
                if (this.velocityY < -0.15) {
                    this.velocityY = -0.15;
                }
            }
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            if (this.collidingHorizontally && this.isClimbing()) {
                this.velocityY = 0.2;
            }
            this.velocityY -= 0.08;
            this.velocityY *= (double)0.98f;
            this.velocityX *= (double)f;
            this.velocityZ *= (double)f;
        }
        this.lastWalkAnimationSpeed = this.walkAnimationSpeed;
        double g = this.x - this.lastX;
        double k = this.z - this.lastZ;
        float l = MathHelper.sqrt(g * g + k * k) * 4.0f;
        if (l > 1.0f) {
            l = 1.0f;
        }
        this.walkAnimationSpeed += (l - this.walkAnimationSpeed) * 0.4f;
        this.walkAnimationProgress += this.walkAnimationSpeed;
    }

    public boolean isClimbing() {
        int k;
        int j;
        int i = MathHelper.floor(this.x);
        return this.world.getBlock(i, j = MathHelper.floor(this.shape.minY), k = MathHelper.floor(this.z)) == Block.LADDER.id || this.world.getBlock(i, j + 1, k) == Block.LADDER.id;
    }

    public void writeCustomNbt(NbtCompound nbt) {
        nbt.putShort("Health", (short)this.health);
        nbt.putShort("HurtTime", (short)this.damagedTimer);
        nbt.putShort("DeathTime", (short)this.deathTicks);
        nbt.putShort("AttackTime", (short)this.attackTimer);
    }

    public void readCustomNbt(NbtCompound nbt) {
        this.health = nbt.getShort("Health");
        if (!nbt.contains("Health")) {
            this.health = 10;
        }
        this.damagedTimer = nbt.getShort("HurtTime");
        this.deathTicks = nbt.getShort("DeathTime");
        this.attackTimer = nbt.getShort("AttackTime");
    }

    public boolean isAlive() {
        return !this.removed && this.health > 0;
    }

    public void mobTick() {
        if (this.lerpSteps > 0) {
            double g;
            double d = this.x + (this.lerpX - this.x) / (double)this.lerpSteps;
            double e = this.y + (this.lerpY - this.y) / (double)this.lerpSteps;
            double f = this.z + (this.lerpZ - this.z) / (double)this.lerpSteps;
            for (g = this.lerpYaw - (double)this.yaw; g < -180.0; g += 360.0) {
            }
            while (g >= 180.0) {
                g -= 360.0;
            }
            this.yaw = (float)((double)this.yaw + g / (double)this.lerpSteps);
            this.pitch = (float)((double)this.pitch + (this.lerpPitch - (double)this.pitch) / (double)this.lerpSteps);
            --this.lerpSteps;
            this.setPosition(d, e, f);
            this.setRotation(this.yaw, this.pitch);
        }
        if (this.health <= 0) {
            this.jumping = false;
            this.sidewaysSpeed = 0.0f;
            this.forwardSpeed = 0.0f;
            this.rotationSpeed = 0.0f;
        } else if (!this.interpolateOnly) {
            this.aiTick();
        }
        boolean i = this.checkWaterCollisions();
        boolean j = this.isInLava();
        if (this.jumping) {
            if (i) {
                this.velocityY += (double)0.04f;
            } else if (j) {
                this.velocityY += (double)0.04f;
            } else if (this.onGround) {
                this.jump();
            }
        }
        this.sidewaysSpeed *= 0.98f;
        this.forwardSpeed *= 0.98f;
        this.rotationSpeed *= 0.9f;
        this.moveRelative(this.sidewaysSpeed, this.forwardSpeed);
        List list = this.world.getEntities(this, this.shape.grown(0.2f, 0.0, 0.2f));
        if (list != null && list.size() > 0) {
            for (int k = 0; k < list.size(); ++k) {
                Entity entity = (Entity)list.get(k);
                if (!entity.isPushable()) continue;
                entity.push(this);
            }
        }
    }

    protected void jump() {
        this.velocityY = 0.42f;
    }

    protected void aiTick() {
        ++this.farFromPlayerTicks;
        PlayerEntity playerEntity = this.world.getNearestPlayer(this, -1.0);
        if (playerEntity != null) {
            double d = playerEntity.x - this.x;
            double e = playerEntity.y - this.y;
            double g = playerEntity.z - this.z;
            double h = d * d + e * e + g * g;
            if (h > 16384.0) {
                this.remove();
            }
            if (this.farFromPlayerTicks > 600 && this.random.nextInt(800) == 0) {
                if (h < 1024.0) {
                    this.farFromPlayerTicks = 0;
                } else {
                    this.remove();
                }
            }
        }
        this.sidewaysSpeed = 0.0f;
        this.forwardSpeed = 0.0f;
        float f = 8.0f;
        if (this.random.nextFloat() < 0.02f) {
            playerEntity = this.world.getNearestPlayer(this, f);
            if (playerEntity != null) {
                this.lookTarget = playerEntity;
                this.lookTimer = 10 + this.random.nextInt(20);
            } else {
                this.rotationSpeed = (this.random.nextFloat() - 0.5f) * 20.0f;
            }
        }
        if (this.lookTarget != null) {
            this.lookAt(this.lookTarget, 10.0f);
            if (this.lookTimer-- <= 0 || this.lookTarget.removed || this.lookTarget.squaredDistanceTo(this) > (double)(f * f)) {
                this.lookTarget = null;
            }
        } else {
            if (this.random.nextFloat() < 0.05f) {
                this.rotationSpeed = (this.random.nextFloat() - 0.5f) * 20.0f;
            }
            this.yaw += this.rotationSpeed;
            this.pitch = this.defaultPitch;
        }
        boolean i = this.checkWaterCollisions();
        boolean j = this.isInLava();
        if (i || j) {
            this.jumping = this.random.nextFloat() < 0.8f;
        }
    }

    public void lookAt(Entity target, float maxRotationChange) {
        double f;
        double d = target.x - this.x;
        double g = target.z - this.z;
        if (target instanceof MobEntity) {
            MobEntity mobEntity = (MobEntity)target;
            double e = mobEntity.y + (double)mobEntity.getEyeHeight() - (this.y + (double)this.getEyeHeight());
        } else {
            f = (target.shape.minY + target.shape.maxY) / 2.0 - (this.y + (double)this.getEyeHeight());
        }
        double h = MathHelper.sqrt(d * d + g * g);
        float i = (float)(Math.atan2(g, d) * 180.0 / 3.1415927410125732) - 90.0f;
        float j = (float)(Math.atan2(f, h) * 180.0 / 3.1415927410125732);
        this.pitch = -this.lerpRotation(this.pitch, j, maxRotationChange);
        this.yaw = this.lerpRotation(this.yaw, i, maxRotationChange);
    }

    private float lerpRotation(float from, float to, float maxChange) {
        float f;
        for (f = to - from; f < -180.0f; f += 360.0f) {
        }
        while (f >= 180.0f) {
            f -= 360.0f;
        }
        if (f > maxChange) {
            f = maxChange;
        }
        if (f < -maxChange) {
            f = -maxChange;
        }
        return from + f;
    }

    public void beforeRemove() {
    }

    public boolean canSpawn() {
        return this.world.isUnobstructed(this.shape) && this.world.getCollisions(this, this.shape).size() == 0 && !this.world.containsLiquid(this.shape);
    }

    protected void voidTick() {
        this.takeDamage(null, 4);
    }

    @Environment(value=EnvType.CLIENT)
    public float getAttackAnimationProgress(float tickDelta) {
        float f = this.attackAnimationProgress - this.lastAttackAnimationProgress;
        if (f < 0.0f) {
            f += 1.0f;
        }
        return this.lastAttackAnimationProgress + f * tickDelta;
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d getPosition(float tickDelta) {
        if (tickDelta == 1.0f) {
            return Vec3d.fromPool(this.x, this.y, this.z);
        }
        double d = this.lastX + (this.x - this.lastX) * (double)tickDelta;
        double e = this.lastY + (this.y - this.lastY) * (double)tickDelta;
        double f = this.lastZ + (this.z - this.lastZ) * (double)tickDelta;
        return Vec3d.fromPool(d, e, f);
    }

    public Vec3d getLookVector() {
        return this.getLookVector(1.0f);
    }

    public Vec3d getLookVector(float tickDelta) {
        if (tickDelta == 1.0f) {
            float f = MathHelper.cos(-this.yaw * ((float)Math.PI / 180) - (float)Math.PI);
            float h = MathHelper.sin(-this.yaw * ((float)Math.PI / 180) - (float)Math.PI);
            float j = -MathHelper.cos(-this.pitch * ((float)Math.PI / 180));
            float l = MathHelper.sin(-this.pitch * ((float)Math.PI / 180));
            return Vec3d.fromPool(h * j, l, f * j);
        }
        float g = this.lastPitch + (this.pitch - this.lastPitch) * tickDelta;
        float i = this.lastYaw + (this.yaw - this.lastYaw) * tickDelta;
        float k = MathHelper.cos(-i * ((float)Math.PI / 180) - (float)Math.PI);
        float m = MathHelper.sin(-i * ((float)Math.PI / 180) - (float)Math.PI);
        float n = -MathHelper.cos(-g * ((float)Math.PI / 180));
        float o = MathHelper.sin(-g * ((float)Math.PI / 180));
        return Vec3d.fromPool(m * n, o, k * n);
    }

    @Environment(value=EnvType.CLIENT)
    public HitResult rayTrace(double distance, float tickDelta) {
        Vec3d vec3d = this.getPosition(tickDelta);
        Vec3d vec3d2 = this.getLookVector(tickDelta);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
        return this.world.rayTrace(vec3d, vec3d3);
    }

    public int maxSpawnedPerChunk() {
        return 4;
    }

    @Environment(value=EnvType.CLIENT)
    public ItemStack getDisplayItemInHand() {
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public void doEvent(byte event) {
        if (event == 2) {
            this.walkAnimationSpeed = 1.5f;
            this.invulnerableTimer = this.invulnerableTicks;
            this.damagedTime = 10;
            this.damagedTimer = 10;
            this.damagedSwingDir = 0.0f;
            this.world.playSound(this, this.getHurtSound(), this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
            this.takeDamage(null, 0);
        } else if (event == 3) {
            this.world.playSound(this, this.getDeathSound(), this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
            this.health = 0;
            this.die(null);
        } else {
            super.doEvent(event);
        }
    }
}

