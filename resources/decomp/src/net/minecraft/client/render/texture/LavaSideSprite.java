/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class LavaSideSprite
extends DynamicTexture {
    protected float[] current = new float[256];
    protected float[] next = new float[256];
    protected float[] heat = new float[256];
    protected float[] heatDelta = new float[256];
    int ticks = 0;

    public LavaSideSprite() {
        super(Block.FLOWING_LAVA.sprite + 1);
        this.replicate = 2;
    }

    public void tick() {
        ++this.ticks;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                float f = 0.0f;
                int l = (int)(MathHelper.sin((float)j * (float)Math.PI * 2.0f / 16.0f) * 1.2f);
                int m = (int)(MathHelper.sin((float)i * (float)Math.PI * 2.0f / 16.0f) * 1.2f);
                for (int o = i - 1; o <= i + 1; ++o) {
                    for (int q = j - 1; q <= j + 1; ++q) {
                        int s = o + l & 0xF;
                        int u = q + m & 0xF;
                        f += this.current[s + u * 16];
                    }
                }
                this.next[i + j * 16] = f / 10.0f + (this.heat[(i + 0 & 0xF) + (j + 0 & 0xF) * 16] + this.heat[(i + 1 & 0xF) + (j + 0 & 0xF) * 16] + this.heat[(i + 1 & 0xF) + (j + 1 & 0xF) * 16] + this.heat[(i + 0 & 0xF) + (j + 1 & 0xF) * 16]) / 4.0f * 0.8f;
                int n = i + j * 16;
                this.heat[n] = this.heat[n] + this.heatDelta[i + j * 16] * 0.01f;
                if (this.heat[i + j * 16] < 0.0f) {
                    this.heat[i + j * 16] = 0.0f;
                }
                int n2 = i + j * 16;
                this.heatDelta[n2] = this.heatDelta[n2] - 0.06f;
                if (!(Math.random() < 0.005)) continue;
                this.heatDelta[i + j * 16] = 1.5f;
            }
        }
        float[] fs = this.next;
        this.next = this.current;
        this.current = fs;
        for (int k = 0; k < 256; ++k) {
            float g = this.current[k - this.ticks / 3 * 16 & 0xFF] * 2.0f;
            if (g > 1.0f) {
                g = 1.0f;
            }
            if (g < 0.0f) {
                g = 0.0f;
            }
            float h = g;
            int n = (int)(h * 100.0f + 155.0f);
            int p = (int)(h * h * 255.0f);
            int r = (int)(h * h * h * h * 128.0f);
            if (this.anaglyph) {
                int t = (n * 30 + p * 59 + r * 11) / 100;
                int v = (n * 30 + p * 70) / 100;
                int w = (n * 30 + r * 70) / 100;
                n = t;
                p = v;
                r = w;
            }
            this.pixels[k * 4 + 0] = (byte)n;
            this.pixels[k * 4 + 1] = (byte)p;
            this.pixels[k * 4 + 2] = (byte)r;
            this.pixels[k * 4 + 3] = -1;
        }
    }
}

