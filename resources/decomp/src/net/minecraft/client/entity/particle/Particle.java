/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class Particle
extends Entity {
    protected int sprite;
    protected float offsetU;
    protected float offsetV;
    protected int age = 0;
    protected int lifetime = 0;
    protected float size;
    protected float gravity;
    protected float red;
    protected float green;
    protected float blue;
    public static double lerpCameraX;
    public static double lerpCameraY;
    public static double lerpCameraZ;

    public Particle(World world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world);
        this.setSize(0.2f, 0.2f);
        this.eyeHeight = this.height / 2.0f;
        this.setPosition(x, y, z);
        this.blue = 1.0f;
        this.green = 1.0f;
        this.red = 1.0f;
        this.velocityX = velocityX + (double)((float)(Math.random() * 2.0 - 1.0) * 0.4f);
        this.velocityY = velocityY + (double)((float)(Math.random() * 2.0 - 1.0) * 0.4f);
        this.velocityZ = velocityZ + (double)((float)(Math.random() * 2.0 - 1.0) * 0.4f);
        float f = (float)(Math.random() + Math.random() + 1.0) * 0.15f;
        float g = MathHelper.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ);
        this.velocityX = this.velocityX / (double)g * (double)f * (double)0.4f;
        this.velocityY = this.velocityY / (double)g * (double)f * (double)0.4f + (double)0.1f;
        this.velocityZ = this.velocityZ / (double)g * (double)f * (double)0.4f;
        this.offsetU = this.random.nextFloat() * 3.0f;
        this.offsetV = this.random.nextFloat() * 3.0f;
        this.size = (this.random.nextFloat() * 0.5f + 0.5f) * 2.0f;
        this.lifetime = (int)(4.0f / (this.random.nextFloat() * 0.9f + 0.1f));
        this.age = 0;
        this.makesSteps = false;
    }

    public Particle multiplyVelocity(float value) {
        this.velocityX *= (double)value;
        this.velocityY = (this.velocityY - (double)0.1f) * (double)value + (double)0.1f;
        this.velocityZ *= (double)value;
        return this;
    }

    public Particle multiplySize(float value) {
        this.setSize(0.2f * value, 0.2f * value);
        this.size *= value;
        return this;
    }

    public void tick() {
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.velocityY -= 0.04 * (double)this.gravity;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= (double)0.98f;
        this.velocityY *= (double)0.98f;
        this.velocityZ *= (double)0.98f;
        if (this.onGround) {
            this.velocityX *= (double)0.7f;
            this.velocityZ *= (double)0.7f;
        }
    }

    public void render(Tesselator tesselator, float tickDelta, float dx, float dy, float dz, float forwards, float sideways) {
        float f = (float)(this.sprite % 16) / 16.0f;
        float g = f + 0.0624375f;
        float h = (float)(this.sprite / 16) / 16.0f;
        float i = h + 0.0624375f;
        float j = 0.1f * this.size;
        float k = (float)(this.lastX + (this.x - this.lastX) * (double)tickDelta - lerpCameraX);
        float l = (float)(this.lastY + (this.y - this.lastY) * (double)tickDelta - lerpCameraY);
        float m = (float)(this.lastZ + (this.z - this.lastZ) * (double)tickDelta - lerpCameraZ);
        float n = this.getBrightness(tickDelta);
        tesselator.color(this.red * n, this.green * n, this.blue * n);
        tesselator.vertex(k - dx * j - forwards * j, l - dy * j, m - dz * j - sideways * j, f, i);
        tesselator.vertex(k - dx * j + forwards * j, l + dy * j, m - dz * j + sideways * j, f, h);
        tesselator.vertex(k + dx * j + forwards * j, l + dy * j, m + dz * j + sideways * j, g, h);
        tesselator.vertex(k + dx * j - forwards * j, l - dy * j, m + dz * j - sideways * j, g, i);
    }

    public int getAtlasType() {
        return 0;
    }

    public void writeCustomNbt(NbtCompound nbt) {
    }

    public void readCustomNbt(NbtCompound nbt) {
    }
}

