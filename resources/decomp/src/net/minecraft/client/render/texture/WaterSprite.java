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
public class WaterSprite
extends DynamicTexture {
    protected float[] current = new float[256];
    protected float[] next = new float[256];
    protected float[] heat = new float[256];
    protected float[] heatDelta = new float[256];
    private int ticks = 0;

    public WaterSprite() {
        super(Block.FLOWING_WATER.sprite);
    }

    public void tick() {
        int i;
        ++this.ticks;
        for (i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                float f = 0.0f;
                for (int m = i - 1; m <= i + 1; ++m) {
                    int n = m & 0xF;
                    int p = j & 0xF;
                    f += this.current[n + p * 16];
                }
                this.next[i + j * 16] = f / 3.3f + this.heat[i + j * 16] * 0.8f;
            }
        }
        for (i = 0; i < 16; ++i) {
            for (int k = 0; k < 16; ++k) {
                int n = i + k * 16;
                this.heat[n] = this.heat[n] + this.heatDelta[i + k * 16] * 0.05f;
                if (this.heat[i + k * 16] < 0.0f) {
                    this.heat[i + k * 16] = 0.0f;
                }
                int n2 = i + k * 16;
                this.heatDelta[n2] = this.heatDelta[n2] - 0.1f;
                if (!(Math.random() < 0.05)) continue;
                this.heatDelta[i + k * 16] = 0.5f;
            }
        }
        float[] fs = this.next;
        this.next = this.current;
        this.current = fs;
        for (int l = 0; l < 256; ++l) {
            float g = this.current[l];
            if (g > 1.0f) {
                g = 1.0f;
            }
            if (g < 0.0f) {
                g = 0.0f;
            }
            float h = g * g;
            int o = (int)(32.0f + h * 32.0f);
            int q = (int)(50.0f + h * 64.0f);
            int r = 255;
            int s = (int)(146.0f + h * 50.0f);
            if (this.anaglyph) {
                int t = (o * 30 + q * 59 + r * 11) / 100;
                int u = (o * 30 + q * 70) / 100;
                int v = (o * 30 + r * 70) / 100;
                o = t;
                q = u;
                r = v;
            }
            this.pixels[l * 4 + 0] = (byte)o;
            this.pixels[l * 4 + 1] = (byte)q;
            this.pixels[l * 4 + 2] = (byte)r;
            this.pixels[l * 4 + 3] = (byte)s;
        }
    }
}

