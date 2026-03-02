/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world.explosion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Explosion {
    public boolean createFire = false;
    private Random random = new Random();
    private World world;
    public double x;
    public double y;
    public double z;
    public Entity source;
    public float power;
    public Set damagedBlocks = new HashSet();

    public Explosion(World world, Entity source, double x, double y, double z, float power) {
        this.world = world;
        this.source = source;
        this.power = power;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void damageEntities() {
        int j;
        float f = this.power;
        int i = 16;
        for (j = 0; j < i; ++j) {
            for (int k = 0; k < i; ++k) {
                for (int m = 0; m < i; ++m) {
                    if (j != 0 && j != i - 1 && k != 0 && k != i - 1 && m != 0 && m != i - 1) continue;
                    double d = (float)j / ((float)i - 1.0f) * 2.0f - 1.0f;
                    double e = (float)k / ((float)i - 1.0f) * 2.0f - 1.0f;
                    double g = (float)m / ((float)i - 1.0f) * 2.0f - 1.0f;
                    double h = Math.sqrt(d * d + e * e + g * g);
                    d /= h;
                    e /= h;
                    g /= h;
                    double w = this.x;
                    double aa = this.y;
                    double ae = this.z;
                    float ag = 0.3f;
                    for (float u = this.power * (0.7f + this.world.random.nextFloat() * 0.6f); u > 0.0f; u -= ag * 0.75f) {
                        int al;
                        int aj;
                        int ai = MathHelper.floor(w);
                        int am = this.world.getBlock(ai, aj = MathHelper.floor(aa), al = MathHelper.floor(ae));
                        if (am > 0) {
                            u -= (Block.BY_ID[am].getBlastResistance(this.source) + 0.3f) * ag;
                        }
                        if (u > 0.0f) {
                            this.damagedBlocks.add(new BlockPos(ai, aj, al));
                        }
                        w += d * (double)ag;
                        aa += e * (double)ag;
                        ae += g * (double)ag;
                    }
                }
            }
        }
        this.power *= 2.0f;
        j = MathHelper.floor(this.x - (double)this.power - 1.0);
        int l = MathHelper.floor(this.x + (double)this.power + 1.0);
        int n = MathHelper.floor(this.y - (double)this.power - 1.0);
        int o = MathHelper.floor(this.y + (double)this.power + 1.0);
        int p = MathHelper.floor(this.z - (double)this.power - 1.0);
        int q = MathHelper.floor(this.z + (double)this.power + 1.0);
        List list = this.world.getEntities(this.source, Box.fromPool(j, n, p, l, o, q));
        Vec3d vec3d = Vec3d.fromPool(this.x, this.y, this.z);
        for (int r = 0; r < list.size(); ++r) {
            Entity entity = (Entity)list.get(r);
            double t = entity.distanceTo(this.x, this.y, this.z) / (double)this.power;
            if (!(t <= 1.0)) continue;
            double x = entity.x - this.x;
            double ab = entity.y - this.y;
            double af = entity.z - this.z;
            double ah = MathHelper.sqrt(x * x + ab * ab + af * af);
            x /= ah;
            ab /= ah;
            af /= ah;
            double ak = this.world.getBlockDensity(vec3d, entity.shape);
            double an = (1.0 - t) * ak;
            entity.takeDamage(this.source, (int)((an * an + an) / 2.0 * 8.0 * (double)this.power + 1.0));
            double ao = an;
            entity.velocityX += x * ao;
            entity.velocityY += ab * ao;
            entity.velocityZ += af * ao;
        }
        this.power = f;
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.damagedBlocks);
        if (this.createFire) {
            for (int s = arrayList.size() - 1; s >= 0; --s) {
                BlockPos blockPos = (BlockPos)arrayList.get(s);
                int v = blockPos.x;
                int y = blockPos.y;
                int z = blockPos.z;
                int ac = this.world.getBlock(v, y, z);
                int ad = this.world.getBlock(v, y - 1, z);
                if (ac != 0 || !Block.IS_SOLID[ad] || this.random.nextInt(3) != 0) continue;
                this.world.setBlock(v, y, z, Block.FIRE.id);
            }
        }
    }

    public void damageBlocks() {
        this.world.playSound(this.x, this.y, this.z, "random.explode", 4.0f, (1.0f + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2f) * 0.7f);
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.damagedBlocks);
        for (int i = arrayList.size() - 1; i >= 0; --i) {
            BlockPos blockPos = (BlockPos)arrayList.get(i);
            int j = blockPos.x;
            int k = blockPos.y;
            int l = blockPos.z;
            int m = this.world.getBlock(j, k, l);
            for (int n = 0; n < 1; ++n) {
                double d = (float)j + this.world.random.nextFloat();
                double e = (float)k + this.world.random.nextFloat();
                double f = (float)l + this.world.random.nextFloat();
                double g = d - this.x;
                double h = e - this.y;
                double o = f - this.z;
                double p = MathHelper.sqrt(g * g + h * h + o * o);
                g /= p;
                h /= p;
                o /= p;
                double q = 0.5 / (p / (double)this.power + 0.1);
                this.world.addParticle("explode", (d + this.x * 1.0) / 2.0, (e + this.y * 1.0) / 2.0, (f + this.z * 1.0) / 2.0, g *= (q *= (double)(this.world.random.nextFloat() * this.world.random.nextFloat() + 0.3f)), h *= q, o *= q);
                this.world.addParticle("smoke", d, e, f, g, h, o);
            }
            if (m <= 0) continue;
            Block.BY_ID[m].dropItems(this.world, j, k, l, this.world.getBlockMetadata(j, k, l), 0.3f);
            this.world.setBlock(j, k, l, 0);
            Block.BY_ID[m].onExploded(this.world, j, k, l);
        }
    }
}

