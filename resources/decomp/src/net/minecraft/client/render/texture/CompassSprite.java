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
public class CompassSprite
extends DynamicTexture {
    private Minecraft minecraft;
    private int[] compass = new int[256];
    private double angle;
    private double angleDelta;

    public CompassSprite(Minecraft minecraft) {
        super(Item.COMPASS.getSprite(null));
        this.minecraft = minecraft;
        this.atlas = 1;
        try {
            BufferedImage bufferedImage = ImageIO.read(Minecraft.class.getResource("/gui/items.png"));
            int i = this.sprite % 16 * 16;
            int j = this.sprite / 16 * 16;
            bufferedImage.getRGB(i, j, 16, 16, this.compass, 0, 16);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public void tick() {
        int r;
        double f;
        for (int i = 0; i < 256; ++i) {
            int j = this.compass[i] >> 24 & 0xFF;
            int k = this.compass[i] >> 16 & 0xFF;
            int l = this.compass[i] >> 8 & 0xFF;
            int m = this.compass[i] >> 0 & 0xFF;
            if (this.anaglyph) {
                int n = (k * 30 + l * 59 + m * 11) / 100;
                int o = (k * 30 + l * 70) / 100;
                int q = (k * 30 + m * 70) / 100;
                k = n;
                l = o;
                m = q;
            }
            this.pixels[i * 4 + 0] = (byte)k;
            this.pixels[i * 4 + 1] = (byte)l;
            this.pixels[i * 4 + 2] = (byte)m;
            this.pixels[i * 4 + 3] = (byte)j;
        }
        double d = 0.0;
        if (this.minecraft.world != null && this.minecraft.player != null) {
            double e = (double)this.minecraft.world.spawnPointX - this.minecraft.player.x;
            double g = (double)this.minecraft.world.spawnPointZ - this.minecraft.player.z;
            d = (double)(this.minecraft.player.yaw - 90.0f) * Math.PI / 180.0 - Math.atan2(g, e);
            if (this.minecraft.world.dimension.unnatural) {
                d = Math.random() * 3.1415927410125732 * 2.0;
            }
        }
        for (f = d - this.angle; f < -Math.PI; f += Math.PI * 2) {
        }
        while (f >= Math.PI) {
            f -= Math.PI * 2;
        }
        if (f < -1.0) {
            f = -1.0;
        }
        if (f > 1.0) {
            f = 1.0;
        }
        this.angleDelta += f * 0.1;
        this.angleDelta *= 0.8;
        this.angle += this.angleDelta;
        double h = Math.sin(this.angle);
        double p = Math.cos(this.angle);
        for (r = -4; r <= 4; ++r) {
            int s = (int)(8.5 + p * (double)r * 0.3);
            int u = (int)(7.5 - h * (double)r * 0.3 * 0.5);
            int w = u * 16 + s;
            int y = 100;
            int aa = 100;
            int ac = 100;
            int ae = 255;
            if (this.anaglyph) {
                int ag = (y * 30 + aa * 59 + ac * 11) / 100;
                int ai = (y * 30 + aa * 70) / 100;
                int ak = (y * 30 + ac * 70) / 100;
                y = ag;
                aa = ai;
                ac = ak;
            }
            this.pixels[w * 4 + 0] = (byte)y;
            this.pixels[w * 4 + 1] = (byte)aa;
            this.pixels[w * 4 + 2] = (byte)ac;
            this.pixels[w * 4 + 3] = (byte)ae;
        }
        for (r = -8; r <= 16; ++r) {
            int t = (int)(8.5 + h * (double)r * 0.3);
            int v = (int)(7.5 + p * (double)r * 0.3 * 0.5);
            int x = v * 16 + t;
            int z = r >= 0 ? 255 : 100;
            int ab = r >= 0 ? 20 : 100;
            int ad = r >= 0 ? 20 : 100;
            int af = 255;
            if (this.anaglyph) {
                int ah = (z * 30 + ab * 59 + ad * 11) / 100;
                int aj = (z * 30 + ab * 70) / 100;
                int al = (z * 30 + ad * 70) / 100;
                z = ah;
                ab = aj;
                ad = al;
            }
            this.pixels[x * 4 + 0] = (byte)z;
            this.pixels[x * 4 + 1] = (byte)ab;
            this.pixels[x * 4 + 2] = (byte)ad;
            this.pixels[x * 4 + 3] = (byte)af;
        }
    }
}

