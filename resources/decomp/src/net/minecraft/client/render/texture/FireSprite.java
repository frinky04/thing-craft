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

@Environment(value=EnvType.CLIENT)
public class FireSprite
extends DynamicTexture {
    protected float[] current = new float[320];
    protected float[] next = new float[320];

    public FireSprite(int i) {
        super(Block.FIRE.sprite + i * 16);
    }

    public void tick() {
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 20; ++j) {
                int l = 18;
                float g = this.current[i + (j + 1) % 20 * 16] * (float)l;
                for (int m = i - 1; m <= i + 1; ++m) {
                    for (int o = j; o <= j + 1; ++o) {
                        int q = m;
                        int s = o;
                        if (q >= 0 && s >= 0 && q < 16 && s < 20) {
                            g += this.current[q + s * 16];
                        }
                        ++l;
                    }
                }
                this.next[i + j * 16] = g / ((float)l * 1.06f);
                if (j < 19) continue;
                this.next[i + j * 16] = (float)(Math.random() * Math.random() * Math.random() * 4.0 + Math.random() * (double)0.1f + (double)0.2f);
            }
        }
        float[] fs = this.next;
        this.next = this.current;
        this.current = fs;
        for (int k = 0; k < 256; ++k) {
            float f = this.current[k] * 1.8f;
            if (f > 1.0f) {
                f = 1.0f;
            }
            if (f < 0.0f) {
                f = 0.0f;
            }
            float h = f;
            int n = (int)(h * 155.0f + 100.0f);
            int p = (int)(h * h * 255.0f);
            int r = (int)(h * h * h * h * h * h * h * h * h * h * 255.0f);
            int t = 255;
            if (h < 0.5f) {
                t = 0;
            }
            h = (h - 0.5f) * 2.0f;
            if (this.anaglyph) {
                int u = (n * 30 + p * 59 + r * 11) / 100;
                int v = (n * 30 + p * 70) / 100;
                int w = (n * 30 + r * 70) / 100;
                n = u;
                p = v;
                r = w;
            }
            this.pixels[k * 4 + 0] = (byte)n;
            this.pixels[k * 4 + 1] = (byte)p;
            this.pixels[k * 4 + 2] = (byte)r;
            this.pixels[k * 4 + 3] = (byte)t;
        }
    }
}

