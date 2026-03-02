/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.texture;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class NetherPortalSprite
extends DynamicTexture {
    private int ticks = 0;
    private byte[][] frames = new byte[32][1024];

    public NetherPortalSprite() {
        super(Block.NETHER_PORTAL.sprite);
        Random random = new Random(100L);
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    int l;
                    float f = 0.0f;
                    for (l = 0; l < 2; ++l) {
                        float g = l * 8;
                        float h = l * 8;
                        float o = ((float)j - g) / 16.0f * 2.0f;
                        float q = ((float)k - h) / 16.0f * 2.0f;
                        if (o < -1.0f) {
                            o += 2.0f;
                        }
                        if (o >= 1.0f) {
                            o -= 2.0f;
                        }
                        if (q < -1.0f) {
                            q += 2.0f;
                        }
                        if (q >= 1.0f) {
                            q -= 2.0f;
                        }
                        float s = o * o + q * q;
                        float t = (float)Math.atan2(q, o) + ((float)i / 32.0f * (float)Math.PI * 2.0f - s * 10.0f + (float)(l * 2)) * (float)(l * 2 - 1);
                        t = (MathHelper.sin(t) + 1.0f) / 2.0f;
                        f += (t /= s + 1.0f) * 0.5f;
                    }
                    l = (int)((f += random.nextFloat() * 0.1f) * 100.0f + 155.0f);
                    int m = (int)(f * f * 200.0f + 55.0f);
                    int n = (int)(f * f * f * f * 255.0f);
                    int p = (int)(f * 100.0f + 155.0f);
                    int r = k * 16 + j;
                    this.frames[i][r * 4 + 0] = (byte)m;
                    this.frames[i][r * 4 + 1] = (byte)n;
                    this.frames[i][r * 4 + 2] = (byte)l;
                    this.frames[i][r * 4 + 3] = (byte)p;
                }
            }
        }
    }

    public void tick() {
        ++this.ticks;
        byte[] bs = this.frames[this.ticks & 0x1F];
        for (int i = 0; i < 256; ++i) {
            int j = bs[i * 4 + 0] & 0xFF;
            int k = bs[i * 4 + 1] & 0xFF;
            int l = bs[i * 4 + 2] & 0xFF;
            int m = bs[i * 4 + 3] & 0xFF;
            if (this.anaglyph) {
                int n = (j * 30 + k * 59 + l * 11) / 100;
                int o = (j * 30 + k * 70) / 100;
                int p = (j * 30 + l * 70) / 100;
                j = n;
                k = o;
                l = p;
            }
            this.pixels[i * 4 + 0] = (byte)j;
            this.pixels[i * 4 + 1] = (byte)k;
            this.pixels[i * 4 + 2] = (byte)l;
            this.pixels[i * 4 + 3] = (byte)m;
        }
    }
}

