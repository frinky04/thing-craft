/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.texture;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.texture.HttpImageProcessor;

@Environment(value=EnvType.CLIENT)
public class SkinImageProcessor
implements HttpImageProcessor {
    private int[] data;
    private int width;
    private int height;

    public BufferedImage process(BufferedImage image) {
        int j;
        if (image == null) {
            return null;
        }
        this.width = 64;
        this.height = 32;
        BufferedImage bufferedImage = new BufferedImage(this.width, this.height, 2);
        Graphics graphics = bufferedImage.getGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        this.data = ((DataBufferInt)bufferedImage.getRaster().getDataBuffer()).getData();
        this.setOpaque(0, 0, 32, 16);
        this.setTransparent(32, 0, 64, 32);
        this.setOpaque(0, 16, 64, 32);
        boolean i = false;
        for (j = 32; j < 64; ++j) {
            for (int k = 0; k < 16; ++k) {
                int m = this.data[j + k * 64];
                if ((m >> 24 & 0xFF) >= 128) continue;
                i = true;
            }
        }
        if (!i) {
            for (j = 32; j < 64; ++j) {
                for (int l = 0; l < 16; ++l) {
                    int n = this.data[j + l * 64];
                    if ((n >> 24 & 0xFF) >= 128) continue;
                    i = true;
                }
            }
        }
        return bufferedImage;
    }

    private void setTransparent(int u1, int v1, int u2, int v2) {
        if (this.hasTransperancy(u1, v1, u2, v2)) {
            return;
        }
        for (int i = u1; i < u2; ++i) {
            for (int j = v1; j < v2; ++j) {
                int n = i + j * this.width;
                this.data[n] = this.data[n] & 0xFFFFFF;
            }
        }
    }

    private void setOpaque(int u1, int v1, int u2, int v2) {
        for (int i = u1; i < u2; ++i) {
            for (int j = v1; j < v2; ++j) {
                int n = i + j * this.width;
                this.data[n] = this.data[n] | 0xFF000000;
            }
        }
    }

    private boolean hasTransperancy(int u1, int v1, int u2, int v2) {
        for (int i = u1; i < u2; ++i) {
            for (int j = v1; j < v2; ++j) {
                int k = this.data[i + j * this.width];
                if ((k >> 24 & 0xFF) >= 128) continue;
                return true;
            }
        }
        return false;
    }
}

