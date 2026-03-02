/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class PortalForcer {
    private Random random = new Random();

    public void onDimensionChanged(World world, Entity entity) {
        if (this.findNetherPortal(world, entity)) {
            return;
        }
        this.generateNetherPortal(world, entity);
        this.findNetherPortal(world, entity);
    }

    public boolean findNetherPortal(World world, Entity entity) {
        int o;
        int i = 128;
        double d = -1.0;
        int j = 0;
        int k = 0;
        int l = 0;
        int m = MathHelper.floor(entity.x);
        int n = MathHelper.floor(entity.z);
        for (o = m - i; o <= m + i; ++o) {
            double e = (double)o + 0.5 - entity.x;
            for (int r = n - i; r <= n + i; ++r) {
                double g = (double)r + 0.5 - entity.z;
                for (int s = 127; s >= 0; --s) {
                    if (world.getBlock(o, s, r) != Block.NETHER_PORTAL.id) continue;
                    while (world.getBlock(o, s - 1, r) == Block.NETHER_PORTAL.id) {
                        --s;
                    }
                    double t = (double)s + 0.5 - entity.y;
                    double v = e * e + t * t + g * g;
                    if (!(d < 0.0) && !(v < d)) continue;
                    d = v;
                    j = o;
                    k = s;
                    l = r;
                }
            }
        }
        if (d >= 0.0) {
            o = j;
            int p = k;
            int q = l;
            double f = (double)o + 0.5;
            double h = (double)p + 0.5;
            double u = (double)q + 0.5;
            if (world.getBlock(o - 1, p, q) == Block.NETHER_PORTAL.id) {
                f -= 0.5;
            }
            if (world.getBlock(o + 1, p, q) == Block.NETHER_PORTAL.id) {
                f += 0.5;
            }
            if (world.getBlock(o, p, q - 1) == Block.NETHER_PORTAL.id) {
                u -= 0.5;
            }
            if (world.getBlock(o, p, q + 1) == Block.NETHER_PORTAL.id) {
                u += 0.5;
            }
            System.out.println("Teleporting to " + f + ", " + h + ", " + u);
            entity.setPositionAndAngles(f, h, u, entity.yaw, 0.0f);
            entity.velocityZ = 0.0;
            entity.velocityY = 0.0;
            entity.velocityX = 0.0;
            return true;
        }
        return false;
    }

    public boolean generateNetherPortal(World world, Entity entity) {
        int r;
        int i = 16;
        double d = -1.0;
        int j = MathHelper.floor(entity.x);
        int k = MathHelper.floor(entity.y);
        int l = MathHelper.floor(entity.z);
        int m = j;
        int n = k;
        int o = l;
        int p = 0;
        int q = this.random.nextInt(4);
        for (r = j - i; r <= j + i; ++r) {
            double e = (double)r + 0.5 - entity.x;
            for (int u = l - i; u <= l + i; ++u) {
                double g = (double)u + 0.5 - entity.z;
                block2: for (int z = 127; z >= 0; --z) {
                    if (world.getBlock(r, z, u) != 0) continue;
                    while (z > 0 && world.getBlock(r, z - 1, u) == 0) {
                        --z;
                    }
                    for (int ad = q; ad < q + 4; ++ad) {
                        int ah = ad % 2;
                        int am = 1 - ah;
                        if (ad % 4 >= 2) {
                            ah = -ah;
                            am = -am;
                        }
                        for (int ar = 0; ar < 3; ++ar) {
                            for (int ay = 0; ay < 4; ++ay) {
                                for (int bd = -1; bd < 4; ++bd) {
                                    int bj = r + (ay - 1) * ah + ar * am;
                                    int bl = z + bd;
                                    int bn = u + (ay - 1) * am - ar * ah;
                                    if (bd < 0 && !world.getMaterial(bj, bl, bn).isSolid() || bd >= 0 && world.getBlock(bj, bl, bn) != 0) continue block2;
                                }
                            }
                        }
                        double as = (double)z + 0.5 - entity.y;
                        double be = e * e + as * as + g * g;
                        if (!(d < 0.0) && !(be < d)) continue;
                        d = be;
                        m = r;
                        n = z;
                        o = u;
                        p = ad % 4;
                    }
                }
            }
        }
        if (d < 0.0) {
            for (r = j - i; r <= j + i; ++r) {
                double f = (double)r + 0.5 - entity.x;
                for (int v = l - i; v <= l + i; ++v) {
                    double h = (double)v + 0.5 - entity.z;
                    block10: for (int aa = 127; aa >= 0; --aa) {
                        if (world.getBlock(r, aa, v) != 0) continue;
                        while (world.getBlock(r, aa - 1, v) == 0) {
                            --aa;
                        }
                        for (int ae = q; ae < q + 2; ++ae) {
                            int ai = ae % 2;
                            int an = 1 - ai;
                            for (int at = 0; at < 4; ++at) {
                                for (int az = -1; az < 4; ++az) {
                                    int bf = r + (at - 1) * ai;
                                    int bk = aa + az;
                                    int bm = v + (at - 1) * an;
                                    if (az < 0 && !world.getMaterial(bf, bk, bm).isSolid() || az >= 0 && world.getBlock(bf, bk, bm) != 0) continue block10;
                                }
                            }
                            double au = (double)aa + 0.5 - entity.y;
                            double bg = f * f + au * au + h * h;
                            if (!(d < 0.0) && !(bg < d)) continue;
                            d = bg;
                            m = r;
                            n = aa;
                            o = v;
                            p = ae % 2;
                        }
                    }
                }
            }
        }
        r = p;
        int s = m;
        int t = n;
        int w = o;
        int x = r % 2;
        int y = 1 - x;
        if (r % 4 >= 2) {
            x = -x;
            y = -y;
        }
        if (d < 0.0) {
            if (n < 70) {
                n = 70;
            }
            if (n > 118) {
                n = 118;
            }
            t = n;
            for (int ab = -1; ab <= 1; ++ab) {
                for (int af = 1; af < 3; ++af) {
                    for (int aj = -1; aj < 3; ++aj) {
                        int ao = s + (af - 1) * x + ab * y;
                        int av = t + aj;
                        int ba = w + (af - 1) * y - ab * x;
                        boolean bh = aj < 0;
                        world.setBlock(ao, av, ba, bh ? Block.OBSIDIAN.id : 0);
                    }
                }
            }
        }
        for (int ac = 0; ac < 4; ++ac) {
            int ag;
            world.suppressNeighborChangedUpdates = true;
            for (ag = 0; ag < 4; ++ag) {
                for (int ak = -1; ak < 4; ++ak) {
                    int ap = s + (ag - 1) * x;
                    int aw = t + ak;
                    int bb = w + (ag - 1) * y;
                    boolean bi = ag == 0 || ag == 3 || ak == -1 || ak == 3;
                    world.setBlock(ap, aw, bb, bi ? Block.OBSIDIAN.id : Block.NETHER_PORTAL.id);
                }
            }
            world.suppressNeighborChangedUpdates = false;
            for (ag = 0; ag < 4; ++ag) {
                for (int al = -1; al < 4; ++al) {
                    int aq = s + (ag - 1) * x;
                    int ax = t + al;
                    int bc = w + (ag - 1) * y;
                    world.updateNeighbors(aq, ax, bc, world.getBlock(aq, ax, bc));
                }
            }
        }
        return true;
    }
}

