/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.world.color;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class FoliageColors {
    private static final int[] colors = new int[65536];

    public static int get(double temperature, double humidity) {
        int i = (int)((1.0 - temperature) * 255.0);
        int j = (int)((1.0 - (humidity *= temperature)) * 255.0);
        return colors[j << 8 | i];
    }

    static {
        try {
            BufferedImage bufferedImage = ImageIO.read(FoliageColors.class.getResource("/misc/foliagecolor.png"));
            bufferedImage.getRGB(0, 0, 256, 256, colors, 0, 256);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}

