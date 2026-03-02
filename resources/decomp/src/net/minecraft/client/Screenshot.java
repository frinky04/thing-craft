/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class Screenshot {
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static ByteBuffer pixels;
    private static byte[] colorBuffer;
    private static int[] pixelBuffer;

    /*
     * WARNING - void declaration
     */
    public static String take(File gameDir, int width, int height) {
        try {
            void file2;
            File file;
            File file3 = new File(gameDir, "screenshots");
            file3.mkdir();
            if (pixels == null || pixels.capacity() < width * height * 3) {
                pixels = BufferUtils.createByteBuffer((int)(width * height * 3));
                colorBuffer = new byte[width * height * 3];
                pixelBuffer = new int[width * height];
            }
            GL11.glPixelStorei((int)3333, (int)1);
            GL11.glPixelStorei((int)3317, (int)1);
            pixels.clear();
            GL11.glReadPixels((int)0, (int)0, (int)width, (int)height, (int)6407, (int)5121, (ByteBuffer)pixels);
            pixels.clear();
            String string = "" + DATE_FORMAT.format(new Date());
            int i = 1;
            while ((file = new File(file3, string + (i == 1 ? "" : "_" + i) + ".png")).exists()) {
                ++i;
            }
            pixels.get(colorBuffer);
            for (int j = 0; j < width; ++j) {
                for (int k = 0; k < height; ++k) {
                    int p;
                    int l = j + (height - k - 1) * width;
                    int m = colorBuffer[l * 3 + 0] & 0xFF;
                    int n = colorBuffer[l * 3 + 1] & 0xFF;
                    int o = colorBuffer[l * 3 + 2] & 0xFF;
                    Screenshot.pixelBuffer[j + k * width] = p = 0xFF000000 | m << 16 | n << 8 | o;
                }
            }
            BufferedImage bufferedImage = new BufferedImage(width, height, 1);
            bufferedImage.setRGB(0, 0, width, height, pixelBuffer, 0, width);
            ImageIO.write((RenderedImage)bufferedImage, "png", (File)file2);
            return "Saved screenshot as " + file2.getName();
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return "Failed to save: " + exception;
        }
    }
}

