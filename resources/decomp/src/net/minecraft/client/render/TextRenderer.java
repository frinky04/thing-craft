/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class TextRenderer {
    private int[] characterWidths = new int[256];
    public int boundTexture = 0;
    private int boundPage;
    private IntBuffer pageBuffer = MemoryTracker.createIntBuffer(1024);

    /*
     * WARNING - void declaration
     */
    public TextRenderer(GameOptions options, String fontPath, TextureManager textureManager) {
        int m;
        void bufferedImage2;
        try {
            BufferedImage bufferedImage = ImageIO.read(TextureManager.class.getResourceAsStream(fontPath));
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
        int i = bufferedImage2.getWidth();
        int j = bufferedImage2.getHeight();
        int[] is = new int[i * j];
        bufferedImage2.getRGB(0, 0, i, j, is, 0, i);
        for (int k = 0; k < 256; ++k) {
            int q;
            int l = k % 16;
            int n = k / 16;
            for (q = 7; q >= 0; --q) {
                int t = l * 8 + q;
                boolean v = true;
                for (int x = 0; x < 8 && v; ++x) {
                    int z = (n * 8 + x) * i;
                    int ab = is[t + z] & 0xFF;
                    if (ab <= 0) continue;
                    v = false;
                }
                if (!v) break;
            }
            if (k == 32) {
                q = 2;
            }
            this.characterWidths[k] = q + 2;
        }
        this.boundTexture = textureManager.load((BufferedImage)bufferedImage2);
        this.boundPage = MemoryTracker.getLists(288);
        Tesselator tesselator = Tesselator.INSTANCE;
        for (m = 0; m < 256; ++m) {
            GL11.glNewList((int)(this.boundPage + m), (int)GL11.GL_COMPILE);
            tesselator.begin();
            int o = m % 16 * 8;
            int r = m / 16 * 8;
            float f = 7.99f;
            float g = 0.0f;
            float h = 0.0f;
            tesselator.vertex(0.0, 0.0f + f, 0.0, (float)o / 128.0f + g, ((float)r + f) / 128.0f + h);
            tesselator.vertex(0.0f + f, 0.0f + f, 0.0, ((float)o + f) / 128.0f + g, ((float)r + f) / 128.0f + h);
            tesselator.vertex(0.0f + f, 0.0, 0.0, ((float)o + f) / 128.0f + g, (float)r / 128.0f + h);
            tesselator.vertex(0.0, 0.0, 0.0, (float)o / 128.0f + g, (float)r / 128.0f + h);
            tesselator.end();
            GL11.glTranslatef((float)this.characterWidths[m], (float)0.0f, (float)0.0f);
            GL11.glEndList();
        }
        for (m = 0; m < 32; ++m) {
            boolean y;
            int p = (m >> 3 & 1) * 85;
            int s = (m >> 2 & 1) * 170 + p;
            int u = (m >> 1 & 1) * 170 + p;
            int w = (m >> 0 & 1) * 170 + p;
            if (m == 6) {
                s += 85;
            }
            boolean bl = y = m >= 16;
            if (options.anaglyph) {
                int aa = (s * 30 + u * 59 + w * 11) / 100;
                int ac = (s * 30 + u * 70) / 100;
                int ad = (s * 30 + w * 70) / 100;
                s = aa;
                u = ac;
                w = ad;
            }
            if (y) {
                s /= 4;
                u /= 4;
                w /= 4;
            }
            GL11.glNewList((int)(this.boundPage + 256 + m), (int)GL11.GL_COMPILE);
            GL11.glColor3f((float)((float)s / 255.0f), (float)((float)u / 255.0f), (float)((float)w / 255.0f));
            GL11.glEndList();
        }
    }

    public void drawWithShadow(String text, int x, int y, int color) {
        this.drawLayer(text, x + 1, y + 1, color, true);
        this.draw(text, x, y, color);
    }

    public void draw(String text, int x, int y, int color) {
        this.drawLayer(text, x, y, color, false);
    }

    public void drawLayer(String text, int x, int y, int color, boolean shadow) {
        if (text == null) {
            return;
        }
        if (shadow) {
            int i = color & 0xFF000000;
            color = (color & 0xFCFCFC) >> 2;
            color += i;
        }
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.boundTexture);
        float f = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float h = (float)(color & 0xFF) / 255.0f;
        float m = (float)(color >> 24 & 0xFF) / 255.0f;
        if (m == 0.0f) {
            m = 1.0f;
        }
        GL11.glColor4f((float)f, (float)g, (float)h, (float)m);
        this.pageBuffer.clear();
        GL11.glPushMatrix();
        GL11.glTranslatef((float)x, (float)y, (float)0.0f);
        for (int j = 0; j < text.length(); ++j) {
            while (text.charAt(j) == '\u00a7' && text.length() > j + 1) {
                int k = "0123456789abcdef".indexOf(text.toLowerCase().charAt(j + 1));
                if (k < 0 || k > 15) {
                    k = 15;
                }
                this.pageBuffer.put(this.boundPage + 256 + k + (shadow ? 16 : 0));
                if (this.pageBuffer.remaining() == 0) {
                    this.pageBuffer.flip();
                    GL11.glCallLists((IntBuffer)this.pageBuffer);
                    this.pageBuffer.clear();
                }
                j += 2;
            }
            int l = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb".indexOf(text.charAt(j));
            if (l >= 0) {
                this.pageBuffer.put(this.boundPage + l + 32);
            }
            if (this.pageBuffer.remaining() != 0) continue;
            this.pageBuffer.flip();
            GL11.glCallLists((IntBuffer)this.pageBuffer);
            this.pageBuffer.clear();
        }
        this.pageBuffer.flip();
        GL11.glCallLists((IntBuffer)this.pageBuffer);
        GL11.glPopMatrix();
    }

    public int getWidth(String text) {
        if (text == null) {
            return 0;
        }
        int i = 0;
        for (int j = 0; j < text.length(); ++j) {
            if (text.charAt(j) == '\u00a7') {
                ++j;
                continue;
            }
            int k = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb".indexOf(text.charAt(j));
            if (k < 0) continue;
            i += this.characterWidths[k + 32];
        }
        return i;
    }
}

