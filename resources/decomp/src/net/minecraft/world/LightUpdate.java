/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class LightUpdate {
    public final LightType type;
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public LightUpdate(LightType type, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.type = type;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public void run(World world) {
        int i = this.maxX - this.minX + 1;
        int j = this.maxY - this.minY + 1;
        int k = this.maxZ - this.minZ + 1;
        int l = i * j * k;
        if (l > 32768) {
            return;
        }
        for (int m = this.minX; m <= this.maxX; ++m) {
            for (int n = this.minZ; n <= this.maxZ; ++n) {
                if (!world.isChunkLoaded(m, 0, n)) continue;
                for (int o = this.minY; o <= this.maxY; ++o) {
                    if (o < 0 || o >= 128) continue;
                    int p = world.getLight(this.type, m, o, n);
                    int q = 0;
                    int r = world.getBlock(m, o, n);
                    int s = Block.OPACITIES[r];
                    if (s == 0) {
                        s = 1;
                    }
                    int t = 0;
                    if (this.type == LightType.SKY) {
                        if (world.hasSkyLight(m, o, n)) {
                            t = 15;
                        }
                    } else if (this.type == LightType.BLOCK) {
                        t = Block.LIGHT[r];
                    }
                    if (s >= 15 && t == 0) {
                        q = 0;
                    } else {
                        int u = world.getLight(this.type, m - 1, o, n);
                        int w = world.getLight(this.type, m + 1, o, n);
                        int x = world.getLight(this.type, m, o - 1, n);
                        int y = world.getLight(this.type, m, o + 1, n);
                        int z = world.getLight(this.type, m, o, n - 1);
                        int aa = world.getLight(this.type, m, o, n + 1);
                        q = u;
                        if (w > q) {
                            q = w;
                        }
                        if (x > q) {
                            q = x;
                        }
                        if (y > q) {
                            q = y;
                        }
                        if (z > q) {
                            q = z;
                        }
                        if (aa > q) {
                            q = aa;
                        }
                        if ((q -= s) < 0) {
                            q = 0;
                        }
                        if (t > q) {
                            q = t;
                        }
                    }
                    if (p == q) continue;
                    world.setLight(this.type, m, o, n, q);
                    int v = q - 1;
                    if (v < 0) {
                        v = 0;
                    }
                    world.updateLightIfOtherThan(this.type, m - 1, o, n, v);
                    world.updateLightIfOtherThan(this.type, m, o - 1, n, v);
                    world.updateLightIfOtherThan(this.type, m, o, n - 1, v);
                    if (m + 1 >= this.maxX) {
                        world.updateLightIfOtherThan(this.type, m + 1, o, n, v);
                    }
                    if (o + 1 >= this.maxY) {
                        world.updateLightIfOtherThan(this.type, m, o + 1, n, v);
                    }
                    if (n + 1 < this.maxZ) continue;
                    world.updateLightIfOtherThan(this.type, m, o, n + 1, v);
                }
            }
        }
    }

    public boolean expand(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (minX >= this.minX && minY >= this.minY && minZ >= this.minZ && maxX <= this.maxX && maxY <= this.maxY && maxZ <= this.maxZ) {
            return true;
        }
        int i = 1;
        if (minX >= this.minX - i && minY >= this.minY - i && minZ >= this.minZ - i && maxX <= this.maxX + i && maxY <= this.maxY + i && maxZ <= this.maxZ + i) {
            int p;
            int o;
            int n;
            int m;
            int q;
            int j = this.maxX - this.minX;
            int k = this.maxY - this.minY;
            int l = this.maxZ - this.minZ;
            if (minX > this.minX) {
                minX = this.minX;
            }
            if (minY > this.minY) {
                minY = this.minY;
            }
            if (minZ > this.minZ) {
                minZ = this.minZ;
            }
            if (maxX < this.maxX) {
                maxX = this.maxX;
            }
            if (maxY < this.maxY) {
                maxY = this.maxY;
            }
            if (maxZ < this.maxZ) {
                maxZ = this.maxZ;
            }
            if ((q = (m = maxX - minX) * (n = maxY - minY) * (o = maxZ - minZ)) - (p = j * k * l) <= 2) {
                this.minX = minX;
                this.minY = minY;
                this.minZ = minZ;
                this.maxX = maxX;
                this.maxY = maxY;
                this.maxZ = maxZ;
                return true;
            }
        }
        return false;
    }
}

