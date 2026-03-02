/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.item.Item;

@Environment(value=EnvType.CLIENT)
public class ClockSprite
extends DynamicTexture {
    private Minecraft minecraft;
    private int[] clock = new int[256];
    private int[] dial = new int[256];
    private double angle;
    private double angleDelta;

    public ClockSprite(Minecraft minecraft) {
        super(Item.CLOCK.getSprite(null));
        this.minecraft = minecraft;
        this.atlas = 1;
        try {
            BufferedImage bufferedImage = ImageIO.read(Minecraft.class.getResource("/gui/items.png"));
            int i = this.sprite % 16 * 16;
            int j = this.sprite / 16 * 16;
            bufferedImage.getRGB(i, j, 16, 16, this.clock, 0, 16);
            bufferedImage = ImageIO.read(Minecraft.class.getResource("/misc/dial.png"));
            bufferedImage.getRGB(0, 0, 16, 16, this.dial, 0, 16);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void tick() {
        double e;
        double d = 0.0;
        if (this.minecraft.world != null && this.minecraft.player != null) {
            float f = this.minecraft.world.getTimeOfDay(1.0f);
            d = -f * (float)Math.PI * 2.0f;
            if (this.minecraft.world.dimension.unnatural) {
                d = Math.random() * 3.1415927410125732 * 2.0;
            }
        }
        for (e = d - this.angle; e < -Math.PI; e += Math.PI * 2) {
        }
        while (e >= Math.PI) {
            e -= Math.PI * 2;
        }
        if (e < -1.0) {
            e = -1.0;
        }
        if (e > 1.0) {
            e = 1.0;
        }
        this.angleDelta += e * 0.1;
        this.angleDelta *= 0.8;
        this.angle += this.angleDelta;
        double g = Math.sin(this.angle);
        double h = Math.cos(this.angle);
        for (int i = 0; i < 256; ++i) {
            int j = this.clock[i] >> 24 & 0xFF;
            int k = this.clock[i] >> 16 & 0xFF;
            int l = this.clock[i] >> 8 & 0xFF;
            int m = this.clock[i] >> 0 & 0xFF;
            if (k == m && l == 0 && m > 0) {
                double n = -((double)(i % 16) / 15.0 - 0.5);
                double q = (double)(i / 16) / 15.0 - 0.5;
                int s = k;
                int t = (int)((n * h + q * g + 0.5) * 16.0);
                int u = (int)((q * h - n * g + 0.5) * 16.0);
                int v = (t & 0xF) + (u & 0xF) * 16;
                j = this.dial[v] >> 24 & 0xFF;
                k = (this.dial[v] >> 16 & 0xFF) * s / 255;
                l = (this.dial[v] >> 8 & 0xFF) * s / 255;
                m = (this.dial[v] >> 0 & 0xFF) * s / 255;
            }
            if (this.anaglyph) {
                int o = (k * 30 + l * 59 + m * 11) / 100;
                int p = (k * 30 + l * 70) / 100;
                int r = (k * 30 + m * 70) / 100;
                k = o;
                l = p;
                m = r;
            }
            this.pixels[i * 4 + 0] = (byte)k;
            this.pixels[i * 4 + 1] = (byte)l;
            this.pixels[i * 4 + 2] = (byte)m;
            this.pixels[i * 4 + 3] = (byte)j;
        }
    }
}

