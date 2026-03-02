/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class BlockRenderer {
    private WorldView world;
    private int forcedSprite = -1;
    private boolean renderFlipped = false;
    private boolean renderFaceAlways = false;

    public BlockRenderer(WorldView world) {
        this.world = world;
    }

    public BlockRenderer() {
    }

    public void renderMiningProgress(Block block, int x, int y, int z, int sprite) {
        this.forcedSprite = sprite;
        this.tesselateInWorld(block, x, y, z);
        this.forcedSprite = -1;
    }

    public boolean tesselateInWorld(Block block, int x, int y, int z) {
        int i = block.getRenderType();
        block.updateShape(this.world, x, y, z);
        if (i == 0) {
            return this.tesselateBlock(block, x, y, z);
        }
        if (i == 4) {
            return this.tesselateLiquid(block, x, y, z);
        }
        if (i == 13) {
            return this.tesselateCactus(block, x, y, z);
        }
        if (i == 1) {
            return this.tesselateCross(block, x, y, z);
        }
        if (i == 6) {
            return this.tesselatePlant(block, x, y, z);
        }
        if (i == 2) {
            return this.tesselateTorch(block, x, y, z);
        }
        if (i == 3) {
            return this.tesselateFire(block, x, y, z);
        }
        if (i == 5) {
            return this.tesselateRedstoneWire(block, x, y, z);
        }
        if (i == 8) {
            return this.tesselateLadder(block, x, y, z);
        }
        if (i == 7) {
            return this.tesselateDoor(block, x, y, z);
        }
        if (i == 9) {
            return this.tesselateRail(block, x, y, z);
        }
        if (i == 10) {
            return this.tesselateStairs(block, x, y, z);
        }
        if (i == 11) {
            return this.tesselateFence(block, x, y, z);
        }
        if (i == 12) {
            return this.tesselateLever(block, x, y, z);
        }
        return false;
    }

    public boolean tesselateTorch(Block block, int x, int y, int z) {
        int i = this.world.getBlockMetadata(x, y, z);
        Tesselator tesselator = Tesselator.INSTANCE;
        float f = block.getBrightness(this.world, x, y, z);
        if (Block.LIGHT[block.id] > 0) {
            f = 1.0f;
        }
        tesselator.color(f, f, f);
        double d = 0.4f;
        double e = 0.5 - d;
        double g = 0.2f;
        if (i == 1) {
            this.tesselateTorch(block, (double)x - e, (double)y + g, z, -d, 0.0);
        } else if (i == 2) {
            this.tesselateTorch(block, (double)x + e, (double)y + g, z, d, 0.0);
        } else if (i == 3) {
            this.tesselateTorch(block, x, (double)y + g, (double)z - e, 0.0, -d);
        } else if (i == 4) {
            this.tesselateTorch(block, x, (double)y + g, (double)z + e, 0.0, d);
        } else {
            this.tesselateTorch(block, x, y, z, 0.0, 0.0);
        }
        return true;
    }

    public boolean tesselateLever(Block block, int x, int y, int z) {
        boolean l;
        int i = this.world.getBlockMetadata(x, y, z);
        int j = i & 7;
        boolean k = (i & 8) > 0;
        Tesselator tesselator = Tesselator.INSTANCE;
        boolean bl = l = this.forcedSprite >= 0;
        if (!l) {
            this.forcedSprite = Block.COBBLESTONE.sprite;
        }
        float f = 0.25f;
        float g = 0.1875f;
        float h = 0.1875f;
        if (j == 5) {
            block.setShape(0.5f - g, 0.0f, 0.5f - f, 0.5f + g, h, 0.5f + f);
        } else if (j == 6) {
            block.setShape(0.5f - f, 0.0f, 0.5f - g, 0.5f + f, h, 0.5f + g);
        } else if (j == 4) {
            block.setShape(0.5f - g, 0.5f - f, 1.0f - h, 0.5f + g, 0.5f + f, 1.0f);
        } else if (j == 3) {
            block.setShape(0.5f - g, 0.5f - f, 0.0f, 0.5f + g, 0.5f + f, h);
        } else if (j == 2) {
            block.setShape(1.0f - h, 0.5f - f, 0.5f - g, 1.0f, 0.5f + f, 0.5f + g);
        } else if (j == 1) {
            block.setShape(0.0f, 0.5f - f, 0.5f - g, h, 0.5f + f, 0.5f + g);
        }
        this.tesselateBlock(block, x, y, z);
        if (!l) {
            this.forcedSprite = -1;
        }
        float m = block.getBrightness(this.world, x, y, z);
        if (Block.LIGHT[block.id] > 0) {
            m = 1.0f;
        }
        tesselator.color(m, m, m);
        int n = block.getSprite(0);
        if (this.forcedSprite >= 0) {
            n = this.forcedSprite;
        }
        int o = (n & 0xF) << 4;
        int p = n & 0xF0;
        float q = (float)o / 256.0f;
        float r = ((float)o + 15.99f) / 256.0f;
        float s = (float)p / 256.0f;
        float t = ((float)p + 15.99f) / 256.0f;
        Vec3d[] vec3ds = new Vec3d[8];
        float u = 0.0625f;
        float v = 0.0625f;
        float w = 0.625f;
        vec3ds[0] = Vec3d.fromPool(-u, 0.0, -v);
        vec3ds[1] = Vec3d.fromPool(u, 0.0, -v);
        vec3ds[2] = Vec3d.fromPool(u, 0.0, v);
        vec3ds[3] = Vec3d.fromPool(-u, 0.0, v);
        vec3ds[4] = Vec3d.fromPool(-u, w, -v);
        vec3ds[5] = Vec3d.fromPool(u, w, -v);
        vec3ds[6] = Vec3d.fromPool(u, w, v);
        vec3ds[7] = Vec3d.fromPool(-u, w, v);
        for (int aa = 0; aa < 8; ++aa) {
            if (k) {
                vec3ds[aa].z -= 0.0625;
                vec3ds[aa].rotateX(0.69813174f);
            } else {
                vec3ds[aa].z += 0.0625;
                vec3ds[aa].rotateX(-0.69813174f);
            }
            if (j == 6) {
                vec3ds[aa].rotateY(1.5707964f);
            }
            if (j < 5) {
                vec3ds[aa].y -= 0.375;
                vec3ds[aa].rotateX(1.5707964f);
                if (j == 4) {
                    vec3ds[aa].rotateY(0.0f);
                }
                if (j == 3) {
                    vec3ds[aa].rotateY((float)Math.PI);
                }
                if (j == 2) {
                    vec3ds[aa].rotateY(1.5707964f);
                }
                if (j == 1) {
                    vec3ds[aa].rotateY(-1.5707964f);
                }
                vec3ds[aa].x += (double)x + 0.5;
                vec3ds[aa].y += (double)((float)y + 0.5f);
                vec3ds[aa].z += (double)z + 0.5;
                continue;
            }
            vec3ds[aa].x += (double)x + 0.5;
            vec3ds[aa].y += (double)((float)y + 0.125f);
            vec3ds[aa].z += (double)z + 0.5;
        }
        Object aa = null;
        Object var26_27 = null;
        Object var27_28 = null;
        Object object4 = null;
        for (int ab = 0; ab < 6; ++ab) {
            Vec3d vec3d4;
            Vec3d vec3d3;
            Vec3d vec3d2;
            Vec3d vec3d;
            if (ab == 0) {
                q = (float)(o + 7) / 256.0f;
                r = ((float)(o + 9) - 0.01f) / 256.0f;
                s = (float)(p + 6) / 256.0f;
                t = ((float)(p + 8) - 0.01f) / 256.0f;
            } else if (ab == 2) {
                q = (float)(o + 7) / 256.0f;
                r = ((float)(o + 9) - 0.01f) / 256.0f;
                s = (float)(p + 6) / 256.0f;
                t = ((float)(p + 16) - 0.01f) / 256.0f;
            }
            if (ab == 0) {
                vec3d = vec3ds[0];
                vec3d2 = vec3ds[1];
                vec3d3 = vec3ds[2];
                vec3d4 = vec3ds[3];
            } else if (ab == 1) {
                vec3d = vec3ds[7];
                vec3d2 = vec3ds[6];
                vec3d3 = vec3ds[5];
                vec3d4 = vec3ds[4];
            } else if (ab == 2) {
                vec3d = vec3ds[1];
                vec3d2 = vec3ds[0];
                vec3d3 = vec3ds[4];
                vec3d4 = vec3ds[5];
            } else if (ab == 3) {
                vec3d = vec3ds[2];
                vec3d2 = vec3ds[1];
                vec3d3 = vec3ds[5];
                vec3d4 = vec3ds[6];
            } else if (ab == 4) {
                vec3d = vec3ds[3];
                vec3d2 = vec3ds[2];
                vec3d3 = vec3ds[6];
                vec3d4 = vec3ds[7];
            } else if (ab == 5) {
                vec3d = vec3ds[0];
                vec3d2 = vec3ds[3];
                vec3d3 = vec3ds[7];
                vec3d4 = vec3ds[4];
            }
            tesselator.vertex(vec3d.x, vec3d.y, vec3d.z, q, t);
            tesselator.vertex(vec3d2.x, vec3d2.y, vec3d2.z, r, t);
            tesselator.vertex(vec3d3.x, vec3d3.y, vec3d3.z, r, s);
            tesselator.vertex(vec3d4.x, vec3d4.y, vec3d4.z, q, s);
        }
        return true;
    }

    public boolean tesselateFire(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = block.getSprite(0);
        if (this.forcedSprite >= 0) {
            i = this.forcedSprite;
        }
        float f = block.getBrightness(this.world, x, y, z);
        tesselator.color(f, f, f);
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        double d = (float)j / 256.0f;
        double e = ((float)j + 15.99f) / 256.0f;
        double g = (float)k / 256.0f;
        double h = ((float)k + 15.99f) / 256.0f;
        float l = 1.4f;
        if (this.world.isSolidBlock(x, y - 1, z) || Block.FIRE.isFlammable(this.world, x, y - 1, z)) {
            double m = (double)x + 0.5 + 0.2;
            double p = (double)x + 0.5 - 0.2;
            double s = (double)z + 0.5 + 0.2;
            double u = (double)z + 0.5 - 0.2;
            double w = (double)x + 0.5 - 0.3;
            double ab = (double)x + 0.5 + 0.3;
            double ad = (double)z + 0.5 - 0.3;
            double af = (double)z + 0.5 + 0.3;
            tesselator.vertex(w, (float)y + l, z + 1, e, g);
            tesselator.vertex(m, y + 0, z + 1, e, h);
            tesselator.vertex(m, y + 0, z + 0, d, h);
            tesselator.vertex(w, (float)y + l, z + 0, d, g);
            tesselator.vertex(ab, (float)y + l, z + 0, e, g);
            tesselator.vertex(p, y + 0, z + 0, e, h);
            tesselator.vertex(p, y + 0, z + 1, d, h);
            tesselator.vertex(ab, (float)y + l, z + 1, d, g);
            d = (float)j / 256.0f;
            e = ((float)j + 15.99f) / 256.0f;
            g = (float)(k + 16) / 256.0f;
            h = ((float)k + 15.99f + 16.0f) / 256.0f;
            tesselator.vertex(x + 1, (float)y + l, af, e, g);
            tesselator.vertex(x + 1, y + 0, u, e, h);
            tesselator.vertex(x + 0, y + 0, u, d, h);
            tesselator.vertex(x + 0, (float)y + l, af, d, g);
            tesselator.vertex(x + 0, (float)y + l, ad, e, g);
            tesselator.vertex(x + 0, y + 0, s, e, h);
            tesselator.vertex(x + 1, y + 0, s, d, h);
            tesselator.vertex(x + 1, (float)y + l, ad, d, g);
            m = (double)x + 0.5 - 0.5;
            p = (double)x + 0.5 + 0.5;
            s = (double)z + 0.5 - 0.5;
            u = (double)z + 0.5 + 0.5;
            w = (double)x + 0.5 - 0.4;
            ab = (double)x + 0.5 + 0.4;
            ad = (double)z + 0.5 - 0.4;
            af = (double)z + 0.5 + 0.4;
            tesselator.vertex(w, (float)y + l, z + 0, d, g);
            tesselator.vertex(m, y + 0, z + 0, d, h);
            tesselator.vertex(m, y + 0, z + 1, e, h);
            tesselator.vertex(w, (float)y + l, z + 1, e, g);
            tesselator.vertex(ab, (float)y + l, z + 1, d, g);
            tesselator.vertex(p, y + 0, z + 1, d, h);
            tesselator.vertex(p, y + 0, z + 0, e, h);
            tesselator.vertex(ab, (float)y + l, z + 0, e, g);
            d = (float)j / 256.0f;
            e = ((float)j + 15.99f) / 256.0f;
            g = (float)k / 256.0f;
            h = ((float)k + 15.99f) / 256.0f;
            tesselator.vertex(x + 0, (float)y + l, af, d, g);
            tesselator.vertex(x + 0, y + 0, u, d, h);
            tesselator.vertex(x + 1, y + 0, u, e, h);
            tesselator.vertex(x + 1, (float)y + l, af, e, g);
            tesselator.vertex(x + 1, (float)y + l, ad, d, g);
            tesselator.vertex(x + 1, y + 0, s, d, h);
            tesselator.vertex(x + 0, y + 0, s, e, h);
            tesselator.vertex(x + 0, (float)y + l, ad, e, g);
        } else {
            float n = 0.2f;
            float o = 0.0625f;
            if ((x + y + z & 1) == 1) {
                d = (float)j / 256.0f;
                e = ((float)j + 15.99f) / 256.0f;
                g = (float)(k + 16) / 256.0f;
                h = ((float)k + 15.99f + 16.0f) / 256.0f;
            }
            if ((x / 2 + y / 2 + z / 2 & 1) == 1) {
                double q = e;
                e = d;
                d = q;
            }
            if (Block.FIRE.isFlammable(this.world, x - 1, y, z)) {
                tesselator.vertex((float)x + n, (float)y + l + o, z + 1, e, g);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 1, e, h);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 0, d, h);
                tesselator.vertex((float)x + n, (float)y + l + o, z + 0, d, g);
                tesselator.vertex((float)x + n, (float)y + l + o, z + 0, d, g);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 0, d, h);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 1, e, h);
                tesselator.vertex((float)x + n, (float)y + l + o, z + 1, e, g);
            }
            if (Block.FIRE.isFlammable(this.world, x + 1, y, z)) {
                tesselator.vertex((float)(x + 1) - n, (float)y + l + o, z + 0, d, g);
                tesselator.vertex(x + 1 - 0, (float)(y + 0) + o, z + 0, d, h);
                tesselator.vertex(x + 1 - 0, (float)(y + 0) + o, z + 1, e, h);
                tesselator.vertex((float)(x + 1) - n, (float)y + l + o, z + 1, e, g);
                tesselator.vertex((float)(x + 1) - n, (float)y + l + o, z + 1, e, g);
                tesselator.vertex(x + 1 - 0, (float)(y + 0) + o, z + 1, e, h);
                tesselator.vertex(x + 1 - 0, (float)(y + 0) + o, z + 0, d, h);
                tesselator.vertex((float)(x + 1) - n, (float)y + l + o, z + 0, d, g);
            }
            if (Block.FIRE.isFlammable(this.world, x, y, z - 1)) {
                tesselator.vertex(x + 0, (float)y + l + o, (float)z + n, e, g);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 0, e, h);
                tesselator.vertex(x + 1, (float)(y + 0) + o, z + 0, d, h);
                tesselator.vertex(x + 1, (float)y + l + o, (float)z + n, d, g);
                tesselator.vertex(x + 1, (float)y + l + o, (float)z + n, d, g);
                tesselator.vertex(x + 1, (float)(y + 0) + o, z + 0, d, h);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 0, e, h);
                tesselator.vertex(x + 0, (float)y + l + o, (float)z + n, e, g);
            }
            if (Block.FIRE.isFlammable(this.world, x, y, z + 1)) {
                tesselator.vertex(x + 1, (float)y + l + o, (float)(z + 1) - n, d, g);
                tesselator.vertex(x + 1, (float)(y + 0) + o, z + 1 - 0, d, h);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 1 - 0, e, h);
                tesselator.vertex(x + 0, (float)y + l + o, (float)(z + 1) - n, e, g);
                tesselator.vertex(x + 0, (float)y + l + o, (float)(z + 1) - n, e, g);
                tesselator.vertex(x + 0, (float)(y + 0) + o, z + 1 - 0, e, h);
                tesselator.vertex(x + 1, (float)(y + 0) + o, z + 1 - 0, d, h);
                tesselator.vertex(x + 1, (float)y + l + o, (float)(z + 1) - n, d, g);
            }
            if (Block.FIRE.isFlammable(this.world, x, y + 1, z)) {
                double r = (double)x + 0.5 + 0.5;
                double t = (double)x + 0.5 - 0.5;
                double v = (double)z + 0.5 + 0.5;
                double aa = (double)z + 0.5 - 0.5;
                double ac = (double)x + 0.5 - 0.5;
                double ae = (double)x + 0.5 + 0.5;
                double ag = (double)z + 0.5 - 0.5;
                double ah = (double)z + 0.5 + 0.5;
                d = (float)j / 256.0f;
                e = ((float)j + 15.99f) / 256.0f;
                g = (float)k / 256.0f;
                h = ((float)k + 15.99f) / 256.0f;
                l = -0.2f;
                if ((x + ++y + z & 1) == 0) {
                    tesselator.vertex(ac, (float)y + l, z + 0, e, g);
                    tesselator.vertex(r, y + 0, z + 0, e, h);
                    tesselator.vertex(r, y + 0, z + 1, d, h);
                    tesselator.vertex(ac, (float)y + l, z + 1, d, g);
                    d = (float)j / 256.0f;
                    e = ((float)j + 15.99f) / 256.0f;
                    g = (float)(k + 16) / 256.0f;
                    h = ((float)k + 15.99f + 16.0f) / 256.0f;
                    tesselator.vertex(ae, (float)y + l, z + 1, e, g);
                    tesselator.vertex(t, y + 0, z + 1, e, h);
                    tesselator.vertex(t, y + 0, z + 0, d, h);
                    tesselator.vertex(ae, (float)y + l, z + 0, d, g);
                } else {
                    tesselator.vertex(x + 0, (float)y + l, ah, e, g);
                    tesselator.vertex(x + 0, y + 0, aa, e, h);
                    tesselator.vertex(x + 1, y + 0, aa, d, h);
                    tesselator.vertex(x + 1, (float)y + l, ah, d, g);
                    d = (float)j / 256.0f;
                    e = ((float)j + 15.99f) / 256.0f;
                    g = (float)(k + 16) / 256.0f;
                    h = ((float)k + 15.99f + 16.0f) / 256.0f;
                    tesselator.vertex(x + 1, (float)y + l, ag, e, g);
                    tesselator.vertex(x + 1, y + 0, v, e, h);
                    tesselator.vertex(x + 0, y + 0, v, d, h);
                    tesselator.vertex(x + 0, (float)y + l, ag, d, g);
                }
            }
        }
        return true;
    }

    public boolean tesselateRedstoneWire(Block block, int x, int y, int z) {
        boolean q;
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = block.getSprite(1, this.world.getBlockMetadata(x, y, z));
        if (this.forcedSprite >= 0) {
            i = this.forcedSprite;
        }
        float f = block.getBrightness(this.world, x, y, z);
        tesselator.color(f, f, f);
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        double d = (float)j / 256.0f;
        double e = ((float)j + 15.99f) / 256.0f;
        double g = (float)k / 256.0f;
        double h = ((float)k + 15.99f) / 256.0f;
        float l = 0.0f;
        float m = 0.03125f;
        boolean n = RedstoneWireBlock.shouldConnectTo(this.world, x - 1, y, z) || !this.world.isSolidBlock(x - 1, y, z) && RedstoneWireBlock.shouldConnectTo(this.world, x - 1, y - 1, z);
        boolean o = RedstoneWireBlock.shouldConnectTo(this.world, x + 1, y, z) || !this.world.isSolidBlock(x + 1, y, z) && RedstoneWireBlock.shouldConnectTo(this.world, x + 1, y - 1, z);
        boolean p = RedstoneWireBlock.shouldConnectTo(this.world, x, y, z - 1) || !this.world.isSolidBlock(x, y, z - 1) && RedstoneWireBlock.shouldConnectTo(this.world, x, y - 1, z - 1);
        boolean bl = q = RedstoneWireBlock.shouldConnectTo(this.world, x, y, z + 1) || !this.world.isSolidBlock(x, y, z + 1) && RedstoneWireBlock.shouldConnectTo(this.world, x, y - 1, z + 1);
        if (!this.world.isSolidBlock(x, y + 1, z)) {
            if (this.world.isSolidBlock(x - 1, y, z) && RedstoneWireBlock.shouldConnectTo(this.world, x - 1, y + 1, z)) {
                n = true;
            }
            if (this.world.isSolidBlock(x + 1, y, z) && RedstoneWireBlock.shouldConnectTo(this.world, x + 1, y + 1, z)) {
                o = true;
            }
            if (this.world.isSolidBlock(x, y, z - 1) && RedstoneWireBlock.shouldConnectTo(this.world, x, y + 1, z - 1)) {
                p = true;
            }
            if (this.world.isSolidBlock(x, y, z + 1) && RedstoneWireBlock.shouldConnectTo(this.world, x, y + 1, z + 1)) {
                q = true;
            }
        }
        float r = 0.3125f;
        float s = x + 0;
        float t = x + 1;
        float u = z + 0;
        float v = z + 1;
        int w = 0;
        if ((n || o) && !p && !q) {
            w = 1;
        }
        if ((p || q) && !o && !n) {
            w = 2;
        }
        if (w != 0) {
            d = (float)(j + 16) / 256.0f;
            e = ((float)(j + 16) + 15.99f) / 256.0f;
            g = (float)k / 256.0f;
            h = ((float)k + 15.99f) / 256.0f;
        }
        if (w == 0) {
            if (o || p || q || n) {
                if (!n) {
                    s += r;
                }
                if (!n) {
                    d += (double)(r / 16.0f);
                }
                if (!o) {
                    t -= r;
                }
                if (!o) {
                    e -= (double)(r / 16.0f);
                }
                if (!p) {
                    u += r;
                }
                if (!p) {
                    g += (double)(r / 16.0f);
                }
                if (!q) {
                    v -= r;
                }
                if (!q) {
                    h -= (double)(r / 16.0f);
                }
            }
            tesselator.vertex(t + l, (float)y + m, v + l, e, h);
            tesselator.vertex(t + l, (float)y + m, u - l, e, g);
            tesselator.vertex(s - l, (float)y + m, u - l, d, g);
            tesselator.vertex(s - l, (float)y + m, v + l, d, h);
        }
        if (w == 1) {
            tesselator.vertex(t + l, (float)y + m, v + l, e, h);
            tesselator.vertex(t + l, (float)y + m, u - l, e, g);
            tesselator.vertex(s - l, (float)y + m, u - l, d, g);
            tesselator.vertex(s - l, (float)y + m, v + l, d, h);
        }
        if (w == 2) {
            tesselator.vertex(t + l, (float)y + m, v + l, e, h);
            tesselator.vertex(t + l, (float)y + m, u - l, d, h);
            tesselator.vertex(s - l, (float)y + m, u - l, d, g);
            tesselator.vertex(s - l, (float)y + m, v + l, e, g);
        }
        d = (float)(j + 16) / 256.0f;
        e = ((float)(j + 16) + 15.99f) / 256.0f;
        g = (float)k / 256.0f;
        h = ((float)k + 15.99f) / 256.0f;
        if (!this.world.isSolidBlock(x, y + 1, z)) {
            if (this.world.isSolidBlock(x - 1, y, z) && this.world.getBlock(x - 1, y + 1, z) == Block.REDSTONE_WIRE.id) {
                tesselator.vertex((float)x + m, (float)(y + 1) + l, (float)(z + 1) + l, e, g);
                tesselator.vertex((float)x + m, (float)(y + 0) - l, (float)(z + 1) + l, d, g);
                tesselator.vertex((float)x + m, (float)(y + 0) - l, (float)(z + 0) - l, d, h);
                tesselator.vertex((float)x + m, (float)(y + 1) + l, (float)(z + 0) - l, e, h);
            }
            if (this.world.isSolidBlock(x + 1, y, z) && this.world.getBlock(x + 1, y + 1, z) == Block.REDSTONE_WIRE.id) {
                tesselator.vertex((float)(x + 1) - m, (float)(y + 0) - l, (float)(z + 1) + l, d, h);
                tesselator.vertex((float)(x + 1) - m, (float)(y + 1) + l, (float)(z + 1) + l, e, h);
                tesselator.vertex((float)(x + 1) - m, (float)(y + 1) + l, (float)(z + 0) - l, e, g);
                tesselator.vertex((float)(x + 1) - m, (float)(y + 0) - l, (float)(z + 0) - l, d, g);
            }
            if (this.world.isSolidBlock(x, y, z - 1) && this.world.getBlock(x, y + 1, z - 1) == Block.REDSTONE_WIRE.id) {
                tesselator.vertex((float)(x + 1) + l, (float)(y + 0) - l, (float)z + m, d, h);
                tesselator.vertex((float)(x + 1) + l, (float)(y + 1) + l, (float)z + m, e, h);
                tesselator.vertex((float)(x + 0) - l, (float)(y + 1) + l, (float)z + m, e, g);
                tesselator.vertex((float)(x + 0) - l, (float)(y + 0) - l, (float)z + m, d, g);
            }
            if (this.world.isSolidBlock(x, y, z + 1) && this.world.getBlock(x, y + 1, z + 1) == Block.REDSTONE_WIRE.id) {
                tesselator.vertex((float)(x + 1) + l, (float)(y + 1) + l, (float)(z + 1) - m, e, g);
                tesselator.vertex((float)(x + 1) + l, (float)(y + 0) - l, (float)(z + 1) - m, d, g);
                tesselator.vertex((float)(x + 0) - l, (float)(y + 0) - l, (float)(z + 1) - m, d, h);
                tesselator.vertex((float)(x + 0) - l, (float)(y + 1) + l, (float)(z + 1) - m, e, h);
            }
        }
        return true;
    }

    public boolean tesselateRail(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = this.world.getBlockMetadata(x, y, z);
        int j = block.getSprite(0, i);
        if (this.forcedSprite >= 0) {
            j = this.forcedSprite;
        }
        float f = block.getBrightness(this.world, x, y, z);
        tesselator.color(f, f, f);
        int k = (j & 0xF) << 4;
        int l = j & 0xF0;
        double d = (float)k / 256.0f;
        double e = ((float)k + 15.99f) / 256.0f;
        double g = (float)l / 256.0f;
        double h = ((float)l + 15.99f) / 256.0f;
        float m = 0.0625f;
        float n = x + 1;
        float o = x + 1;
        float p = x + 0;
        float q = x + 0;
        float r = z + 0;
        float s = z + 1;
        float t = z + 1;
        float u = z + 0;
        float v = (float)y + m;
        float w = (float)y + m;
        float aa = (float)y + m;
        float ab = (float)y + m;
        if (i == 1 || i == 2 || i == 3 || i == 7) {
            n = q = (float)(x + 1);
            o = p = (float)(x + 0);
            r = s = (float)(z + 1);
            t = u = (float)(z + 0);
        } else if (i == 8) {
            n = o = (float)(x + 0);
            p = q = (float)(x + 1);
            r = u = (float)(z + 1);
            s = t = (float)(z + 0);
        } else if (i == 9) {
            n = q = (float)(x + 0);
            o = p = (float)(x + 1);
            r = s = (float)(z + 0);
            t = u = (float)(z + 1);
        }
        if (i == 2 || i == 4) {
            v += 1.0f;
            ab += 1.0f;
        } else if (i == 3 || i == 5) {
            w += 1.0f;
            aa += 1.0f;
        }
        tesselator.vertex(n, v, r, e, g);
        tesselator.vertex(o, w, s, e, h);
        tesselator.vertex(p, aa, t, d, h);
        tesselator.vertex(q, ab, u, d, g);
        tesselator.vertex(q, ab, u, d, g);
        tesselator.vertex(p, aa, t, d, h);
        tesselator.vertex(o, w, s, e, h);
        tesselator.vertex(n, v, r, e, g);
        return true;
    }

    public boolean tesselateLadder(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = block.getSprite(0);
        if (this.forcedSprite >= 0) {
            i = this.forcedSprite;
        }
        float f = block.getBrightness(this.world, x, y, z);
        tesselator.color(f, f, f);
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        double d = (float)j / 256.0f;
        double e = ((float)j + 15.99f) / 256.0f;
        double g = (float)k / 256.0f;
        double h = ((float)k + 15.99f) / 256.0f;
        int l = this.world.getBlockMetadata(x, y, z);
        float m = 0.0f;
        float n = 0.05f;
        if (l == 5) {
            tesselator.vertex((float)x + n, (float)(y + 1) + m, (float)(z + 1) + m, d, g);
            tesselator.vertex((float)x + n, (float)(y + 0) - m, (float)(z + 1) + m, d, h);
            tesselator.vertex((float)x + n, (float)(y + 0) - m, (float)(z + 0) - m, e, h);
            tesselator.vertex((float)x + n, (float)(y + 1) + m, (float)(z + 0) - m, e, g);
        }
        if (l == 4) {
            tesselator.vertex((float)(x + 1) - n, (float)(y + 0) - m, (float)(z + 1) + m, e, h);
            tesselator.vertex((float)(x + 1) - n, (float)(y + 1) + m, (float)(z + 1) + m, e, g);
            tesselator.vertex((float)(x + 1) - n, (float)(y + 1) + m, (float)(z + 0) - m, d, g);
            tesselator.vertex((float)(x + 1) - n, (float)(y + 0) - m, (float)(z + 0) - m, d, h);
        }
        if (l == 3) {
            tesselator.vertex((float)(x + 1) + m, (float)(y + 0) - m, (float)z + n, e, h);
            tesselator.vertex((float)(x + 1) + m, (float)(y + 1) + m, (float)z + n, e, g);
            tesselator.vertex((float)(x + 0) - m, (float)(y + 1) + m, (float)z + n, d, g);
            tesselator.vertex((float)(x + 0) - m, (float)(y + 0) - m, (float)z + n, d, h);
        }
        if (l == 2) {
            tesselator.vertex((float)(x + 1) + m, (float)(y + 1) + m, (float)(z + 1) - n, d, g);
            tesselator.vertex((float)(x + 1) + m, (float)(y + 0) - m, (float)(z + 1) - n, d, h);
            tesselator.vertex((float)(x + 0) - m, (float)(y + 0) - m, (float)(z + 1) - n, e, h);
            tesselator.vertex((float)(x + 0) - m, (float)(y + 1) + m, (float)(z + 1) - n, e, g);
        }
        return true;
    }

    public boolean tesselateCross(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        float f = block.getBrightness(this.world, x, y, z);
        tesselator.color(f, f, f);
        this.tesselateCross(block, this.world.getBlockMetadata(x, y, z), x, y, z);
        return true;
    }

    public boolean tesselatePlant(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        float f = block.getBrightness(this.world, x, y, z);
        tesselator.color(f, f, f);
        this.tesselatePlant(block, this.world.getBlockMetadata(x, y, z), x, (float)y - 0.0625f, z);
        return true;
    }

    public void tesselateTorch(Block block, double x, double y, double z, double dx, double dz) {
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = block.getSprite(0);
        if (this.forcedSprite >= 0) {
            i = this.forcedSprite;
        }
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        float f = (float)j / 256.0f;
        float g = ((float)j + 15.99f) / 256.0f;
        float h = (float)k / 256.0f;
        float l = ((float)k + 15.99f) / 256.0f;
        double d = (double)f + 0.02734375;
        double e = (double)h + 0.0234375;
        double m = (double)f + 0.03515625;
        double n = (double)h + 0.03125;
        double o = (x += 0.5) - 0.5;
        double p = x + 0.5;
        double q = (z += 0.5) - 0.5;
        double r = z + 0.5;
        double s = 0.0625;
        double t = 0.625;
        tesselator.vertex(x + dx * (1.0 - t) - s, y + t, z + dz * (1.0 - t) - s, d, e);
        tesselator.vertex(x + dx * (1.0 - t) - s, y + t, z + dz * (1.0 - t) + s, d, n);
        tesselator.vertex(x + dx * (1.0 - t) + s, y + t, z + dz * (1.0 - t) + s, m, n);
        tesselator.vertex(x + dx * (1.0 - t) + s, y + t, z + dz * (1.0 - t) - s, m, e);
        tesselator.vertex(x - s, y + 1.0, q, f, h);
        tesselator.vertex(x - s + dx, y + 0.0, q + dz, f, l);
        tesselator.vertex(x - s + dx, y + 0.0, r + dz, g, l);
        tesselator.vertex(x - s, y + 1.0, r, g, h);
        tesselator.vertex(x + s, y + 1.0, r, f, h);
        tesselator.vertex(x + dx + s, y + 0.0, r + dz, f, l);
        tesselator.vertex(x + dx + s, y + 0.0, q + dz, g, l);
        tesselator.vertex(x + s, y + 1.0, q, g, h);
        tesselator.vertex(o, y + 1.0, z + s, f, h);
        tesselator.vertex(o + dx, y + 0.0, z + s + dz, f, l);
        tesselator.vertex(p + dx, y + 0.0, z + s + dz, g, l);
        tesselator.vertex(p, y + 1.0, z + s, g, h);
        tesselator.vertex(p, y + 1.0, z - s, f, h);
        tesselator.vertex(p + dx, y + 0.0, z - s + dz, f, l);
        tesselator.vertex(o + dx, y + 0.0, z - s + dz, g, l);
        tesselator.vertex(o, y + 1.0, z - s, g, h);
    }

    public void tesselateCross(Block block, int metadata, double x, double y, double z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = block.getSprite(0, metadata);
        if (this.forcedSprite >= 0) {
            i = this.forcedSprite;
        }
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        double d = (float)j / 256.0f;
        double e = ((float)j + 15.99f) / 256.0f;
        double f = (float)k / 256.0f;
        double g = ((float)k + 15.99f) / 256.0f;
        double h = x + 0.5 - (double)0.45f;
        double l = x + 0.5 + (double)0.45f;
        double m = z + 0.5 - (double)0.45f;
        double n = z + 0.5 + (double)0.45f;
        tesselator.vertex(h, y + 1.0, m, d, f);
        tesselator.vertex(h, y + 0.0, m, d, g);
        tesselator.vertex(l, y + 0.0, n, e, g);
        tesselator.vertex(l, y + 1.0, n, e, f);
        tesselator.vertex(l, y + 1.0, n, d, f);
        tesselator.vertex(l, y + 0.0, n, d, g);
        tesselator.vertex(h, y + 0.0, m, e, g);
        tesselator.vertex(h, y + 1.0, m, e, f);
        tesselator.vertex(h, y + 1.0, n, d, f);
        tesselator.vertex(h, y + 0.0, n, d, g);
        tesselator.vertex(l, y + 0.0, m, e, g);
        tesselator.vertex(l, y + 1.0, m, e, f);
        tesselator.vertex(l, y + 1.0, m, d, f);
        tesselator.vertex(l, y + 0.0, m, d, g);
        tesselator.vertex(h, y + 0.0, n, e, g);
        tesselator.vertex(h, y + 1.0, n, e, f);
    }

    public void tesselatePlant(Block block, int metadata, double x, double y, double z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        int i = block.getSprite(0, metadata);
        if (this.forcedSprite >= 0) {
            i = this.forcedSprite;
        }
        int j = (i & 0xF) << 4;
        int k = i & 0xF0;
        double d = (float)j / 256.0f;
        double e = ((float)j + 15.99f) / 256.0f;
        double f = (float)k / 256.0f;
        double g = ((float)k + 15.99f) / 256.0f;
        double h = x + 0.5 - 0.25;
        double l = x + 0.5 + 0.25;
        double m = z + 0.5 - 0.5;
        double n = z + 0.5 + 0.5;
        tesselator.vertex(h, y + 1.0, m, d, f);
        tesselator.vertex(h, y + 0.0, m, d, g);
        tesselator.vertex(h, y + 0.0, n, e, g);
        tesselator.vertex(h, y + 1.0, n, e, f);
        tesselator.vertex(h, y + 1.0, n, d, f);
        tesselator.vertex(h, y + 0.0, n, d, g);
        tesselator.vertex(h, y + 0.0, m, e, g);
        tesselator.vertex(h, y + 1.0, m, e, f);
        tesselator.vertex(l, y + 1.0, n, d, f);
        tesselator.vertex(l, y + 0.0, n, d, g);
        tesselator.vertex(l, y + 0.0, m, e, g);
        tesselator.vertex(l, y + 1.0, m, e, f);
        tesselator.vertex(l, y + 1.0, m, d, f);
        tesselator.vertex(l, y + 0.0, m, d, g);
        tesselator.vertex(l, y + 0.0, n, e, g);
        tesselator.vertex(l, y + 1.0, n, e, f);
        h = x + 0.5 - 0.5;
        l = x + 0.5 + 0.5;
        m = z + 0.5 - 0.25;
        n = z + 0.5 + 0.25;
        tesselator.vertex(h, y + 1.0, m, d, f);
        tesselator.vertex(h, y + 0.0, m, d, g);
        tesselator.vertex(l, y + 0.0, m, e, g);
        tesselator.vertex(l, y + 1.0, m, e, f);
        tesselator.vertex(l, y + 1.0, m, d, f);
        tesselator.vertex(l, y + 0.0, m, d, g);
        tesselator.vertex(h, y + 0.0, m, e, g);
        tesselator.vertex(h, y + 1.0, m, e, f);
        tesselator.vertex(l, y + 1.0, n, d, f);
        tesselator.vertex(l, y + 0.0, n, d, g);
        tesselator.vertex(h, y + 0.0, n, e, g);
        tesselator.vertex(h, y + 1.0, n, e, f);
        tesselator.vertex(h, y + 1.0, n, d, f);
        tesselator.vertex(h, y + 0.0, n, d, g);
        tesselator.vertex(l, y + 0.0, n, e, g);
        tesselator.vertex(l, y + 1.0, n, e, f);
    }

    public boolean tesselateLiquid(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        boolean i = block.shouldRenderFace(this.world, x, y + 1, z, 1);
        boolean j = block.shouldRenderFace(this.world, x, y - 1, z, 0);
        boolean[] bls = new boolean[]{block.shouldRenderFace(this.world, x, y, z - 1, 2), block.shouldRenderFace(this.world, x, y, z + 1, 3), block.shouldRenderFace(this.world, x - 1, y, z, 4), block.shouldRenderFace(this.world, x + 1, y, z, 5)};
        if (!(i || j || bls[0] || bls[1] || bls[2] || bls[3])) {
            return false;
        }
        boolean k = false;
        float f = 0.5f;
        float g = 1.0f;
        float h = 0.8f;
        float l = 0.6f;
        double d = 0.0;
        double e = 1.0;
        Material material = block.material;
        int m = this.world.getBlockMetadata(x, y, z);
        float n = this.getLiquidHeight(x, y, z, material);
        float o = this.getLiquidHeight(x, y, z + 1, material);
        float p = this.getLiquidHeight(x + 1, y, z + 1, material);
        float q = this.getLiquidHeight(x + 1, y, z, material);
        if (this.renderFaceAlways || i) {
            k = true;
            int r = block.getSprite(1, m);
            float u = (float)LiquidBlock.getFlowAngle(this.world, x, y, z, material);
            if (u > -999.0f) {
                r = block.getSprite(2, m);
            }
            int w = (r & 0xF) << 4;
            int ab = r & 0xF0;
            double ad = ((double)w + 8.0) / 256.0;
            double ag = ((double)ab + 8.0) / 256.0;
            if (u < -999.0f) {
                u = 0.0f;
            } else {
                ad = (float)(w + 16) / 256.0f;
                ag = (float)(ab + 16) / 256.0f;
            }
            float am = MathHelper.sin(u) * 8.0f / 256.0f;
            float ar = MathHelper.cos(u) * 8.0f / 256.0f;
            float aw = block.getBrightness(this.world, x, y, z);
            tesselator.color(g * aw, g * aw, g * aw);
            tesselator.vertex(x + 0, (float)y + n, z + 0, ad - (double)ar - (double)am, ag - (double)ar + (double)am);
            tesselator.vertex(x + 0, (float)y + o, z + 1, ad - (double)ar + (double)am, ag + (double)ar + (double)am);
            tesselator.vertex(x + 1, (float)y + p, z + 1, ad + (double)ar + (double)am, ag + (double)ar - (double)am);
            tesselator.vertex(x + 1, (float)y + q, z + 0, ad + (double)ar - (double)am, ag - (double)ar - (double)am);
        }
        if (this.renderFaceAlways || j) {
            float s = block.getBrightness(this.world, x, y - 1, z);
            tesselator.color(f * s, f * s, f * s);
            this.tesselateBottomFace(block, x, y, z, block.getSprite(0));
            k = true;
        }
        for (int t = 0; t < 4; ++t) {
            float bi;
            float ba;
            float be;
            float av;
            float aq;
            float al;
            int v = x;
            int aa = y;
            int ac = z;
            if (t == 0) {
                --ac;
            }
            if (t == 1) {
                ++ac;
            }
            if (t == 2) {
                --v;
            }
            if (t == 3) {
                ++v;
            }
            int ae = block.getSprite(t + 2, m);
            int af = (ae & 0xF) << 4;
            int ah = ae & 0xF0;
            if (!this.renderFaceAlways && !bls[t]) continue;
            if (t == 0) {
                float ai = n;
                float an = q;
                float as = x;
                float bb = x + 1;
                float ax = z;
                float f2 = z;
            } else if (t == 1) {
                float aj = p;
                float ao = o;
                float at = x + 1;
                float bc = x;
                float ay = z + 1;
                float bf = z + 1;
            } else if (t == 2) {
                float ak = o;
                float ap = n;
                float au = x;
                float bd = x;
                float az = z + 1;
                float bg = z;
            } else {
                al = q;
                aq = p;
                av = x + 1;
                be = x + 1;
                ba = z;
                bi = z + 1;
            }
            k = true;
            double bj = (float)(af + 0) / 256.0f;
            double bk = ((double)(af + 16) - 0.01) / 256.0;
            double bl = ((float)ah + (1.0f - al) * 16.0f) / 256.0f;
            double bm = ((float)ah + (1.0f - aq) * 16.0f) / 256.0f;
            double bn = ((double)(ah + 16) - 0.01) / 256.0;
            float bo = block.getBrightness(this.world, v, aa, ac);
            bo = t < 2 ? (bo *= h) : (bo *= l);
            tesselator.color(g * bo, g * bo, g * bo);
            tesselator.vertex(av, (float)y + al, ba, bj, bl);
            tesselator.vertex(be, (float)y + aq, bi, bk, bm);
            tesselator.vertex(be, y + 0, bi, bk, bn);
            tesselator.vertex(av, y + 0, ba, bj, bn);
        }
        block.minY = d;
        block.maxY = e;
        return k;
    }

    private float getLiquidHeight(int x, int y, int z, Material liquid) {
        int i = 0;
        float f = 0.0f;
        for (int j = 0; j < 4; ++j) {
            int k = x - (j & 1);
            int l = y;
            int m = z - (j >> 1 & 1);
            if (this.world.getMaterial(k, l + 1, m) == liquid) {
                return 1.0f;
            }
            Material material = this.world.getMaterial(k, l, m);
            if (material == liquid) {
                int n = this.world.getBlockMetadata(k, l, m);
                if (n >= 8 || n == 0) {
                    f += LiquidBlock.getHeightLoss(n) * 10.0f;
                    i += 10;
                }
                f += LiquidBlock.getHeightLoss(n);
                ++i;
                continue;
            }
            if (material.isSolid()) continue;
            f += 1.0f;
            ++i;
        }
        return 1.0f - f / (float)i;
    }

    public void tesselateFallingBlock(Block block, World world, int x, int y, int z) {
        float f = 0.5f;
        float g = 1.0f;
        float h = 0.8f;
        float i = 0.6f;
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        float j = block.getBrightness(world, x, y, z);
        float k = block.getBrightness(world, x, y - 1, z);
        if (k < j) {
            k = j;
        }
        tesselator.color(f * k, f * k, f * k);
        this.tesselateBottomFace(block, -0.5, -0.5, -0.5, block.getSprite(0));
        k = block.getBrightness(world, x, y + 1, z);
        if (k < j) {
            k = j;
        }
        tesselator.color(g * k, g * k, g * k);
        this.tesselateTopFace(block, -0.5, -0.5, -0.5, block.getSprite(1));
        k = block.getBrightness(world, x, y, z - 1);
        if (k < j) {
            k = j;
        }
        tesselator.color(h * k, h * k, h * k);
        this.tesselateNorthFace(block, -0.5, -0.5, -0.5, block.getSprite(2));
        k = block.getBrightness(world, x, y, z + 1);
        if (k < j) {
            k = j;
        }
        tesselator.color(h * k, h * k, h * k);
        this.tesselateSouthFace(block, -0.5, -0.5, -0.5, block.getSprite(3));
        k = block.getBrightness(world, x - 1, y, z);
        if (k < j) {
            k = j;
        }
        tesselator.color(i * k, i * k, i * k);
        this.tesselateWestFace(block, -0.5, -0.5, -0.5, block.getSprite(4));
        k = block.getBrightness(world, x + 1, y, z);
        if (k < j) {
            k = j;
        }
        tesselator.color(i * k, i * k, i * k);
        this.tesselateEastFace(block, -0.5, -0.5, -0.5, block.getSprite(5));
        tesselator.end();
    }

    public boolean tesselateBlock(Block block, int x, int y, int z) {
        int i = block.getColor(this.world, x, y, z);
        float f = (float)(i >> 16 & 0xFF) / 255.0f;
        float g = (float)(i >> 8 & 0xFF) / 255.0f;
        float h = (float)(i & 0xFF) / 255.0f;
        return this.tesselateWithoutAmbientOcclusion(block, x, y, z, f, g, h);
    }

    public boolean tesselateWithoutAmbientOcclusion(Block block, int x, int y, int z, float r, float g, float b) {
        Tesselator tesselator = Tesselator.INSTANCE;
        boolean i = false;
        float f = 0.5f;
        float h = 1.0f;
        float j = 0.8f;
        float k = 0.6f;
        float l = h * r;
        float m = h * g;
        float n = h * b;
        if (block == Block.GRASS) {
            b = 1.0f;
            g = 1.0f;
            r = 1.0f;
        }
        float o = f * r;
        float p = j * r;
        float q = k * r;
        float s = f * g;
        float t = j * g;
        float u = k * g;
        float v = f * b;
        float w = j * b;
        float aa = k * b;
        float ab = block.getBrightness(this.world, x, y, z);
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y - 1, z, 0)) {
            float ac = block.getBrightness(this.world, x, y - 1, z);
            tesselator.color(o * ac, s * ac, v * ac);
            this.tesselateBottomFace(block, x, y, z, block.getSprite(this.world, x, y, z, 0));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y + 1, z, 1)) {
            float ad = block.getBrightness(this.world, x, y + 1, z);
            if (block.maxY != 1.0 && !block.material.isLiquid()) {
                ad = ab;
            }
            tesselator.color(l * ad, m * ad, n * ad);
            this.tesselateTopFace(block, x, y, z, block.getSprite(this.world, x, y, z, 1));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y, z - 1, 2)) {
            float ae = block.getBrightness(this.world, x, y, z - 1);
            if (block.minZ > 0.0) {
                ae = ab;
            }
            tesselator.color(p * ae, t * ae, w * ae);
            this.tesselateNorthFace(block, x, y, z, block.getSprite(this.world, x, y, z, 2));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y, z + 1, 3)) {
            float af = block.getBrightness(this.world, x, y, z + 1);
            if (block.maxZ < 1.0) {
                af = ab;
            }
            tesselator.color(p * af, t * af, w * af);
            this.tesselateSouthFace(block, x, y, z, block.getSprite(this.world, x, y, z, 3));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x - 1, y, z, 4)) {
            float ag = block.getBrightness(this.world, x - 1, y, z);
            if (block.minX > 0.0) {
                ag = ab;
            }
            tesselator.color(q * ag, u * ag, aa * ag);
            this.tesselateWestFace(block, x, y, z, block.getSprite(this.world, x, y, z, 4));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x + 1, y, z, 5)) {
            float ah = block.getBrightness(this.world, x + 1, y, z);
            if (block.maxX < 1.0) {
                ah = ab;
            }
            tesselator.color(q * ah, u * ah, aa * ah);
            this.tesselateEastFace(block, x, y, z, block.getSprite(this.world, x, y, z, 5));
            i = true;
        }
        return i;
    }

    public boolean tesselateCactus(Block block, int x, int y, int z) {
        int i = block.getColor(this.world, x, y, z);
        float f = (float)(i >> 16 & 0xFF) / 255.0f;
        float g = (float)(i >> 8 & 0xFF) / 255.0f;
        float h = (float)(i & 0xFF) / 255.0f;
        return this.tesselateCactus(block, x, y, z, f, g, h);
    }

    public boolean tesselateCactus(Block block, int x, int y, int z, float r, float g, float b) {
        Tesselator tesselator = Tesselator.INSTANCE;
        boolean i = false;
        float f = 0.5f;
        float h = 1.0f;
        float j = 0.8f;
        float k = 0.6f;
        float l = f * r;
        float m = h * r;
        float n = j * r;
        float o = k * r;
        float p = f * g;
        float q = h * g;
        float s = j * g;
        float t = k * g;
        float u = f * b;
        float v = h * b;
        float w = j * b;
        float aa = k * b;
        float ab = 0.0625f;
        float ac = block.getBrightness(this.world, x, y, z);
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y - 1, z, 0)) {
            float ad = block.getBrightness(this.world, x, y - 1, z);
            tesselator.color(l * ad, p * ad, u * ad);
            this.tesselateBottomFace(block, x, y, z, block.getSprite(this.world, x, y, z, 0));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y + 1, z, 1)) {
            float ae = block.getBrightness(this.world, x, y + 1, z);
            if (block.maxY != 1.0 && !block.material.isLiquid()) {
                ae = ac;
            }
            tesselator.color(m * ae, q * ae, v * ae);
            this.tesselateTopFace(block, x, y, z, block.getSprite(this.world, x, y, z, 1));
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y, z - 1, 2)) {
            float af = block.getBrightness(this.world, x, y, z - 1);
            if (block.minZ > 0.0) {
                af = ac;
            }
            tesselator.color(n * af, s * af, w * af);
            tesselator.addOffset(0.0f, 0.0f, ab);
            this.tesselateNorthFace(block, x, y, z, block.getSprite(this.world, x, y, z, 2));
            tesselator.addOffset(0.0f, 0.0f, -ab);
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x, y, z + 1, 3)) {
            float ag = block.getBrightness(this.world, x, y, z + 1);
            if (block.maxZ < 1.0) {
                ag = ac;
            }
            tesselator.color(n * ag, s * ag, w * ag);
            tesselator.addOffset(0.0f, 0.0f, -ab);
            this.tesselateSouthFace(block, x, y, z, block.getSprite(this.world, x, y, z, 3));
            tesselator.addOffset(0.0f, 0.0f, ab);
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x - 1, y, z, 4)) {
            float ah = block.getBrightness(this.world, x - 1, y, z);
            if (block.minX > 0.0) {
                ah = ac;
            }
            tesselator.color(o * ah, t * ah, aa * ah);
            tesselator.addOffset(ab, 0.0f, 0.0f);
            this.tesselateWestFace(block, x, y, z, block.getSprite(this.world, x, y, z, 4));
            tesselator.addOffset(-ab, 0.0f, 0.0f);
            i = true;
        }
        if (this.renderFaceAlways || block.shouldRenderFace(this.world, x + 1, y, z, 5)) {
            float ai = block.getBrightness(this.world, x + 1, y, z);
            if (block.maxX < 1.0) {
                ai = ac;
            }
            tesselator.color(o * ai, t * ai, aa * ai);
            tesselator.addOffset(-ab, 0.0f, 0.0f);
            this.tesselateEastFace(block, x, y, z, block.getSprite(this.world, x, y, z, 5));
            tesselator.addOffset(ab, 0.0f, 0.0f);
            i = true;
        }
        return i;
    }

    public boolean tesselateFence(Block block, int x, int y, int z) {
        float t;
        boolean o;
        boolean i = false;
        float f = 0.375f;
        float g = 0.625f;
        block.setShape(f, 0.0f, f, g, 1.0f, g);
        this.tesselateBlock(block, x, y, z);
        boolean j = false;
        boolean k = false;
        if (this.world.getBlock(x - 1, y, z) == block.id || this.world.getBlock(x + 1, y, z) == block.id) {
            j = true;
        }
        if (this.world.getBlock(x, y, z - 1) == block.id || this.world.getBlock(x, y, z + 1) == block.id) {
            k = true;
        }
        boolean l = this.world.getBlock(x - 1, y, z) == block.id;
        boolean m = this.world.getBlock(x + 1, y, z) == block.id;
        boolean n = this.world.getBlock(x, y, z - 1) == block.id;
        boolean bl = o = this.world.getBlock(x, y, z + 1) == block.id;
        if (!j && !k) {
            j = true;
        }
        f = 0.4375f;
        g = 0.5625f;
        float h = 0.75f;
        float p = 0.9375f;
        float q = l ? 0.0f : f;
        float r = m ? 1.0f : g;
        float s = n ? 0.0f : f;
        float f2 = t = o ? 1.0f : g;
        if (j) {
            block.setShape(q, h, f, r, p, g);
            this.tesselateBlock(block, x, y, z);
        }
        if (k) {
            block.setShape(f, h, s, g, p, t);
            this.tesselateBlock(block, x, y, z);
        }
        h = 0.375f;
        p = 0.5625f;
        if (j) {
            block.setShape(q, h, f, r, p, g);
            this.tesselateBlock(block, x, y, z);
        }
        if (k) {
            block.setShape(f, h, s, g, p, t);
            this.tesselateBlock(block, x, y, z);
        }
        block.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        return i;
    }

    public boolean tesselateStairs(Block block, int x, int y, int z) {
        boolean i = false;
        int j = this.world.getBlockMetadata(x, y, z);
        if (j == 0) {
            block.setShape(0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 1.0f);
            this.tesselateBlock(block, x, y, z);
            block.setShape(0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
            this.tesselateBlock(block, x, y, z);
        } else if (j == 1) {
            block.setShape(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f);
            this.tesselateBlock(block, x, y, z);
            block.setShape(0.5f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f);
            this.tesselateBlock(block, x, y, z);
        } else if (j == 2) {
            block.setShape(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 0.5f);
            this.tesselateBlock(block, x, y, z);
            block.setShape(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f);
            this.tesselateBlock(block, x, y, z);
        } else if (j == 3) {
            block.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f);
            this.tesselateBlock(block, x, y, z);
            block.setShape(0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 1.0f);
            this.tesselateBlock(block, x, y, z);
        }
        block.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        return i;
    }

    public boolean tesselateDoor(Block block, int x, int y, int z) {
        Tesselator tesselator = Tesselator.INSTANCE;
        DoorBlock doorBlock = (DoorBlock)block;
        boolean i = false;
        float f = 0.5f;
        float g = 1.0f;
        float h = 0.8f;
        float j = 0.6f;
        float k = block.getBrightness(this.world, x, y, z);
        float l = block.getBrightness(this.world, x, y - 1, z);
        if (doorBlock.minY > 0.0) {
            l = k;
        }
        if (Block.LIGHT[block.id] > 0) {
            l = 1.0f;
        }
        tesselator.color(f * l, f * l, f * l);
        this.tesselateBottomFace(block, x, y, z, block.getSprite(this.world, x, y, z, 0));
        i = true;
        l = block.getBrightness(this.world, x, y + 1, z);
        if (doorBlock.maxY < 1.0) {
            l = k;
        }
        if (Block.LIGHT[block.id] > 0) {
            l = 1.0f;
        }
        tesselator.color(g * l, g * l, g * l);
        this.tesselateTopFace(block, x, y, z, block.getSprite(this.world, x, y, z, 1));
        i = true;
        l = block.getBrightness(this.world, x, y, z - 1);
        if (doorBlock.minZ > 0.0) {
            l = k;
        }
        if (Block.LIGHT[block.id] > 0) {
            l = 1.0f;
        }
        tesselator.color(h * l, h * l, h * l);
        int m = block.getSprite(this.world, x, y, z, 2);
        if (m < 0) {
            this.renderFlipped = true;
            m = -m;
        }
        this.tesselateNorthFace(block, x, y, z, m);
        i = true;
        this.renderFlipped = false;
        l = block.getBrightness(this.world, x, y, z + 1);
        if (doorBlock.maxZ < 1.0) {
            l = k;
        }
        if (Block.LIGHT[block.id] > 0) {
            l = 1.0f;
        }
        tesselator.color(h * l, h * l, h * l);
        m = block.getSprite(this.world, x, y, z, 3);
        if (m < 0) {
            this.renderFlipped = true;
            m = -m;
        }
        this.tesselateSouthFace(block, x, y, z, m);
        i = true;
        this.renderFlipped = false;
        l = block.getBrightness(this.world, x - 1, y, z);
        if (doorBlock.minX > 0.0) {
            l = k;
        }
        if (Block.LIGHT[block.id] > 0) {
            l = 1.0f;
        }
        tesselator.color(j * l, j * l, j * l);
        m = block.getSprite(this.world, x, y, z, 4);
        if (m < 0) {
            this.renderFlipped = true;
            m = -m;
        }
        this.tesselateWestFace(block, x, y, z, m);
        i = true;
        this.renderFlipped = false;
        l = block.getBrightness(this.world, x + 1, y, z);
        if (doorBlock.maxX < 1.0) {
            l = k;
        }
        if (Block.LIGHT[block.id] > 0) {
            l = 1.0f;
        }
        tesselator.color(j * l, j * l, j * l);
        m = block.getSprite(this.world, x, y, z, 5);
        if (m < 0) {
            this.renderFlipped = true;
            m = -m;
        }
        this.tesselateEastFace(block, x, y, z, m);
        i = true;
        this.renderFlipped = false;
        return i;
    }

    public void tesselateBottomFace(Block block, double x, double y, double z, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (this.forcedSprite >= 0) {
            sprite = this.forcedSprite;
        }
        int i = (sprite & 0xF) << 4;
        int j = sprite & 0xF0;
        double d = ((double)i + block.minX * 16.0) / 256.0;
        double e = ((double)i + block.maxX * 16.0 - 0.01) / 256.0;
        double f = ((double)j + block.minZ * 16.0) / 256.0;
        double g = ((double)j + block.maxZ * 16.0 - 0.01) / 256.0;
        if (block.minX < 0.0 || block.maxX > 1.0) {
            d = ((float)i + 0.0f) / 256.0f;
            e = ((float)i + 15.99f) / 256.0f;
        }
        if (block.minZ < 0.0 || block.maxZ > 1.0) {
            f = ((float)j + 0.0f) / 256.0f;
            g = ((float)j + 15.99f) / 256.0f;
        }
        double h = x + block.minX;
        double k = x + block.maxX;
        double l = y + block.minY;
        double m = z + block.minZ;
        double n = z + block.maxZ;
        tesselator.vertex(h, l, n, d, g);
        tesselator.vertex(h, l, m, d, f);
        tesselator.vertex(k, l, m, e, f);
        tesselator.vertex(k, l, n, e, g);
    }

    public void tesselateTopFace(Block block, double x, double y, double z, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (this.forcedSprite >= 0) {
            sprite = this.forcedSprite;
        }
        int i = (sprite & 0xF) << 4;
        int j = sprite & 0xF0;
        double d = ((double)i + block.minX * 16.0) / 256.0;
        double e = ((double)i + block.maxX * 16.0 - 0.01) / 256.0;
        double f = ((double)j + block.minZ * 16.0) / 256.0;
        double g = ((double)j + block.maxZ * 16.0 - 0.01) / 256.0;
        if (block.minX < 0.0 || block.maxX > 1.0) {
            d = ((float)i + 0.0f) / 256.0f;
            e = ((float)i + 15.99f) / 256.0f;
        }
        if (block.minZ < 0.0 || block.maxZ > 1.0) {
            f = ((float)j + 0.0f) / 256.0f;
            g = ((float)j + 15.99f) / 256.0f;
        }
        double h = x + block.minX;
        double k = x + block.maxX;
        double l = y + block.maxY;
        double m = z + block.minZ;
        double n = z + block.maxZ;
        tesselator.vertex(k, l, n, e, g);
        tesselator.vertex(k, l, m, e, f);
        tesselator.vertex(h, l, m, d, f);
        tesselator.vertex(h, l, n, d, g);
    }

    public void tesselateNorthFace(Block block, double x, double y, double z, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (this.forcedSprite >= 0) {
            sprite = this.forcedSprite;
        }
        int i = (sprite & 0xF) << 4;
        int j = sprite & 0xF0;
        double d = ((double)i + block.minX * 16.0) / 256.0;
        double e = ((double)i + block.maxX * 16.0 - 0.01) / 256.0;
        double f = ((double)j + block.minY * 16.0) / 256.0;
        double g = ((double)j + block.maxY * 16.0 - 0.01) / 256.0;
        if (this.renderFlipped) {
            double h = d;
            d = e;
            e = h;
        }
        if (block.minX < 0.0 || block.maxX > 1.0) {
            d = ((float)i + 0.0f) / 256.0f;
            e = ((float)i + 15.99f) / 256.0f;
        }
        if (block.minY < 0.0 || block.maxY > 1.0) {
            f = ((float)j + 0.0f) / 256.0f;
            g = ((float)j + 15.99f) / 256.0f;
        }
        double k = x + block.minX;
        double l = x + block.maxX;
        double m = y + block.minY;
        double n = y + block.maxY;
        double o = z + block.minZ;
        tesselator.vertex(k, n, o, e, f);
        tesselator.vertex(l, n, o, d, f);
        tesselator.vertex(l, m, o, d, g);
        tesselator.vertex(k, m, o, e, g);
    }

    public void tesselateSouthFace(Block block, double x, double y, double z, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (this.forcedSprite >= 0) {
            sprite = this.forcedSprite;
        }
        int i = (sprite & 0xF) << 4;
        int j = sprite & 0xF0;
        double d = ((double)i + block.minX * 16.0) / 256.0;
        double e = ((double)i + block.maxX * 16.0 - 0.01) / 256.0;
        double f = ((double)j + block.minY * 16.0) / 256.0;
        double g = ((double)j + block.maxY * 16.0 - 0.01) / 256.0;
        if (this.renderFlipped) {
            double h = d;
            d = e;
            e = h;
        }
        if (block.minX < 0.0 || block.maxX > 1.0) {
            d = ((float)i + 0.0f) / 256.0f;
            e = ((float)i + 15.99f) / 256.0f;
        }
        if (block.minY < 0.0 || block.maxY > 1.0) {
            f = ((float)j + 0.0f) / 256.0f;
            g = ((float)j + 15.99f) / 256.0f;
        }
        double k = x + block.minX;
        double l = x + block.maxX;
        double m = y + block.minY;
        double n = y + block.maxY;
        double o = z + block.maxZ;
        tesselator.vertex(k, n, o, d, f);
        tesselator.vertex(k, m, o, d, g);
        tesselator.vertex(l, m, o, e, g);
        tesselator.vertex(l, n, o, e, f);
    }

    public void tesselateWestFace(Block block, double x, double y, double z, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (this.forcedSprite >= 0) {
            sprite = this.forcedSprite;
        }
        int i = (sprite & 0xF) << 4;
        int j = sprite & 0xF0;
        double d = ((double)i + block.minZ * 16.0) / 256.0;
        double e = ((double)i + block.maxZ * 16.0 - 0.01) / 256.0;
        double f = ((double)j + block.minY * 16.0) / 256.0;
        double g = ((double)j + block.maxY * 16.0 - 0.01) / 256.0;
        if (this.renderFlipped) {
            double h = d;
            d = e;
            e = h;
        }
        if (block.minZ < 0.0 || block.maxZ > 1.0) {
            d = ((float)i + 0.0f) / 256.0f;
            e = ((float)i + 15.99f) / 256.0f;
        }
        if (block.minY < 0.0 || block.maxY > 1.0) {
            f = ((float)j + 0.0f) / 256.0f;
            g = ((float)j + 15.99f) / 256.0f;
        }
        double k = x + block.minX;
        double l = y + block.minY;
        double m = y + block.maxY;
        double n = z + block.minZ;
        double o = z + block.maxZ;
        tesselator.vertex(k, m, o, e, f);
        tesselator.vertex(k, m, n, d, f);
        tesselator.vertex(k, l, n, d, g);
        tesselator.vertex(k, l, o, e, g);
    }

    public void tesselateEastFace(Block block, double x, double y, double z, int sprite) {
        Tesselator tesselator = Tesselator.INSTANCE;
        if (this.forcedSprite >= 0) {
            sprite = this.forcedSprite;
        }
        int i = (sprite & 0xF) << 4;
        int j = sprite & 0xF0;
        double d = ((double)i + block.minZ * 16.0) / 256.0;
        double e = ((double)i + block.maxZ * 16.0 - 0.01) / 256.0;
        double f = ((double)j + block.minY * 16.0) / 256.0;
        double g = ((double)j + block.maxY * 16.0 - 0.01) / 256.0;
        if (this.renderFlipped) {
            double h = d;
            d = e;
            e = h;
        }
        if (block.minZ < 0.0 || block.maxZ > 1.0) {
            d = ((float)i + 0.0f) / 256.0f;
            e = ((float)i + 15.99f) / 256.0f;
        }
        if (block.minY < 0.0 || block.maxY > 1.0) {
            f = ((float)j + 0.0f) / 256.0f;
            g = ((float)j + 15.99f) / 256.0f;
        }
        double k = x + block.maxX;
        double l = y + block.minY;
        double m = y + block.maxY;
        double n = z + block.minZ;
        double o = z + block.maxZ;
        tesselator.vertex(k, l, o, d, g);
        tesselator.vertex(k, l, n, e, g);
        tesselator.vertex(k, m, n, e, f);
        tesselator.vertex(k, m, o, d, f);
    }

    public void tesselateGuiItem(Block block, float tickDelta) {
        int i = block.getRenderType();
        Tesselator tesselator = Tesselator.INSTANCE;
        if (i == 0) {
            block.resetShape();
            GL11.glTranslatef((float)-0.5f, (float)-0.5f, (float)-0.5f);
            float f = 0.5f;
            float g = 1.0f;
            float h = 0.8f;
            float j = 0.6f;
            tesselator.begin();
            tesselator.color(g, g, g, tickDelta);
            this.tesselateBottomFace(block, 0.0, 0.0, 0.0, block.getSprite(0));
            tesselator.color(f, f, f, tickDelta);
            this.tesselateTopFace(block, 0.0, 0.0, 0.0, block.getSprite(1));
            tesselator.color(h, h, h, tickDelta);
            this.tesselateNorthFace(block, 0.0, 0.0, 0.0, block.getSprite(2));
            this.tesselateSouthFace(block, 0.0, 0.0, 0.0, block.getSprite(3));
            tesselator.color(j, j, j, tickDelta);
            this.tesselateWestFace(block, 0.0, 0.0, 0.0, block.getSprite(4));
            this.tesselateEastFace(block, 0.0, 0.0, 0.0, block.getSprite(5));
            tesselator.end();
            GL11.glTranslatef((float)0.5f, (float)0.5f, (float)0.5f);
        }
    }

    public void renderAsItem(Block block) {
        int i = -1;
        Tesselator tesselator = Tesselator.INSTANCE;
        int j = block.getRenderType();
        if (j == 0) {
            block.resetShape();
            GL11.glTranslatef((float)-0.5f, (float)-0.5f, (float)-0.5f);
            tesselator.begin();
            tesselator.normal(0.0f, -1.0f, 0.0f);
            this.tesselateBottomFace(block, 0.0, 0.0, 0.0, block.getSprite(0));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 1.0f, 0.0f);
            this.tesselateTopFace(block, 0.0, 0.0, 0.0, block.getSprite(1));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 0.0f, -1.0f);
            this.tesselateNorthFace(block, 0.0, 0.0, 0.0, block.getSprite(2));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 0.0f, 1.0f);
            this.tesselateSouthFace(block, 0.0, 0.0, 0.0, block.getSprite(3));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(-1.0f, 0.0f, 0.0f);
            this.tesselateWestFace(block, 0.0, 0.0, 0.0, block.getSprite(4));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(1.0f, 0.0f, 0.0f);
            this.tesselateEastFace(block, 0.0, 0.0, 0.0, block.getSprite(5));
            tesselator.end();
            GL11.glTranslatef((float)0.5f, (float)0.5f, (float)0.5f);
        } else if (j == 1) {
            tesselator.begin();
            tesselator.normal(0.0f, -1.0f, 0.0f);
            this.tesselateCross(block, i, -0.5, -0.5, -0.5);
            tesselator.end();
        } else if (j == 13) {
            block.resetShape();
            GL11.glTranslatef((float)-0.5f, (float)-0.5f, (float)-0.5f);
            float f = 0.0625f;
            tesselator.begin();
            tesselator.normal(0.0f, -1.0f, 0.0f);
            this.tesselateBottomFace(block, 0.0, 0.0, 0.0, block.getSprite(0));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 1.0f, 0.0f);
            this.tesselateTopFace(block, 0.0, 0.0, 0.0, block.getSprite(1));
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 0.0f, -1.0f);
            tesselator.addOffset(0.0f, 0.0f, f);
            this.tesselateNorthFace(block, 0.0, 0.0, 0.0, block.getSprite(2));
            tesselator.addOffset(0.0f, 0.0f, -f);
            tesselator.end();
            tesselator.begin();
            tesselator.normal(0.0f, 0.0f, 1.0f);
            tesselator.addOffset(0.0f, 0.0f, -f);
            this.tesselateSouthFace(block, 0.0, 0.0, 0.0, block.getSprite(3));
            tesselator.addOffset(0.0f, 0.0f, f);
            tesselator.end();
            tesselator.begin();
            tesselator.normal(-1.0f, 0.0f, 0.0f);
            tesselator.addOffset(f, 0.0f, 0.0f);
            this.tesselateWestFace(block, 0.0, 0.0, 0.0, block.getSprite(4));
            tesselator.addOffset(-f, 0.0f, 0.0f);
            tesselator.end();
            tesselator.begin();
            tesselator.normal(1.0f, 0.0f, 0.0f);
            tesselator.addOffset(-f, 0.0f, 0.0f);
            this.tesselateEastFace(block, 0.0, 0.0, 0.0, block.getSprite(5));
            tesselator.addOffset(f, 0.0f, 0.0f);
            tesselator.end();
            GL11.glTranslatef((float)0.5f, (float)0.5f, (float)0.5f);
        } else if (j == 6) {
            tesselator.begin();
            tesselator.normal(0.0f, -1.0f, 0.0f);
            this.tesselatePlant(block, i, -0.5, -0.5, -0.5);
            tesselator.end();
        } else if (j == 2) {
            tesselator.begin();
            tesselator.normal(0.0f, -1.0f, 0.0f);
            this.tesselateTorch(block, -0.5, -0.5, -0.5, 0.0, 0.0);
            tesselator.end();
        } else if (j == 10) {
            for (int k = 0; k < 2; ++k) {
                if (k == 0) {
                    block.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f);
                }
                if (k == 1) {
                    block.setShape(0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 1.0f);
                }
                GL11.glTranslatef((float)-0.5f, (float)-0.5f, (float)-0.5f);
                tesselator.begin();
                tesselator.normal(0.0f, -1.0f, 0.0f);
                this.tesselateBottomFace(block, 0.0, 0.0, 0.0, block.getSprite(0));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(0.0f, 1.0f, 0.0f);
                this.tesselateTopFace(block, 0.0, 0.0, 0.0, block.getSprite(1));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(0.0f, 0.0f, -1.0f);
                this.tesselateNorthFace(block, 0.0, 0.0, 0.0, block.getSprite(2));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(0.0f, 0.0f, 1.0f);
                this.tesselateSouthFace(block, 0.0, 0.0, 0.0, block.getSprite(3));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(-1.0f, 0.0f, 0.0f);
                this.tesselateWestFace(block, 0.0, 0.0, 0.0, block.getSprite(4));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(1.0f, 0.0f, 0.0f);
                this.tesselateEastFace(block, 0.0, 0.0, 0.0, block.getSprite(5));
                tesselator.end();
                GL11.glTranslatef((float)0.5f, (float)0.5f, (float)0.5f);
            }
        } else if (j == 11) {
            for (int l = 0; l < 4; ++l) {
                float g = 0.125f;
                if (l == 0) {
                    block.setShape(0.5f - g, 0.0f, 0.0f, 0.5f + g, 1.0f, g * 2.0f);
                }
                if (l == 1) {
                    block.setShape(0.5f - g, 0.0f, 1.0f - g * 2.0f, 0.5f + g, 1.0f, 1.0f);
                }
                g = 0.0625f;
                if (l == 2) {
                    block.setShape(0.5f - g, 1.0f - g * 3.0f, -g * 2.0f, 0.5f + g, 1.0f - g, 1.0f + g * 2.0f);
                }
                if (l == 3) {
                    block.setShape(0.5f - g, 0.5f - g * 3.0f, -g * 2.0f, 0.5f + g, 0.5f - g, 1.0f + g * 2.0f);
                }
                GL11.glTranslatef((float)-0.5f, (float)-0.5f, (float)-0.5f);
                tesselator.begin();
                tesselator.normal(0.0f, -1.0f, 0.0f);
                this.tesselateBottomFace(block, 0.0, 0.0, 0.0, block.getSprite(0));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(0.0f, 1.0f, 0.0f);
                this.tesselateTopFace(block, 0.0, 0.0, 0.0, block.getSprite(1));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(0.0f, 0.0f, -1.0f);
                this.tesselateNorthFace(block, 0.0, 0.0, 0.0, block.getSprite(2));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(0.0f, 0.0f, 1.0f);
                this.tesselateSouthFace(block, 0.0, 0.0, 0.0, block.getSprite(3));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(-1.0f, 0.0f, 0.0f);
                this.tesselateWestFace(block, 0.0, 0.0, 0.0, block.getSprite(4));
                tesselator.end();
                tesselator.begin();
                tesselator.normal(1.0f, 0.0f, 0.0f);
                this.tesselateEastFace(block, 0.0, 0.0, 0.0, block.getSprite(5));
                tesselator.end();
                GL11.glTranslatef((float)0.5f, (float)0.5f, (float)0.5f);
            }
            block.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public static boolean isItem3d(int type) {
        if (type == 0) {
            return true;
        }
        if (type == 13) {
            return true;
        }
        if (type == 10) {
            return true;
        }
        return type == 11;
    }
}

