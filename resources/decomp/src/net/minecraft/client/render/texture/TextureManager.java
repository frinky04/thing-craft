/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.texture;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.client.render.texture.HttpImageProcessor;
import net.minecraft.client.render.texture.HttpTexture;
import net.minecraft.client.resource.pack.TexturePack;
import net.minecraft.client.resource.pack.TexturePacks;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class TextureManager {
    public static boolean MIPMAP = false;
    private HashMap textureIds = new HashMap();
    private HashMap textureImages = new HashMap();
    private IntBuffer idBuffer = MemoryTracker.createIntBuffer(1);
    private ByteBuffer imageBuffer = MemoryTracker.createByteBuffer(0x100000);
    private List dynamicTextures = new ArrayList();
    private Map httpTextures = new HashMap();
    private GameOptions options;
    private boolean clamp = false;
    private boolean blur = false;
    private TexturePacks texturePacks;

    public TextureManager(TexturePacks texturePacks, GameOptions options) {
        this.texturePacks = texturePacks;
        this.options = options;
    }

    public int load(String path) {
        TexturePack texturePack = this.texturePacks.selected;
        Integer integer = (Integer)this.textureIds.get(path);
        if (integer != null) {
            return integer;
        }
        try {
            this.idBuffer.clear();
            MemoryTracker.genTextures(this.idBuffer);
            int i = this.idBuffer.get(0);
            if (path.startsWith("##")) {
                this.load(this.rescale(this.readImage(texturePack.getResource(path.substring(2)))), i);
            } else if (path.startsWith("%clamp%")) {
                this.clamp = true;
                this.load(this.readImage(texturePack.getResource(path.substring(7))), i);
                this.clamp = false;
            } else if (path.startsWith("%blur%")) {
                this.blur = true;
                this.load(this.readImage(texturePack.getResource(path.substring(6))), i);
                this.blur = false;
            } else {
                this.load(this.readImage(texturePack.getResource(path)), i);
            }
            this.textureIds.put(path, i);
            return i;
        }
        catch (IOException iOException) {
            throw new RuntimeException("!!");
        }
    }

    private BufferedImage rescale(BufferedImage image) {
        int i = image.getWidth() / 16;
        BufferedImage bufferedImage = new BufferedImage(16, image.getHeight() * i, 2);
        Graphics graphics = bufferedImage.getGraphics();
        for (int j = 0; j < i; ++j) {
            graphics.drawImage(image, -j * 16, j * image.getHeight(), null);
        }
        graphics.dispose();
        return bufferedImage;
    }

    public int load(BufferedImage image) {
        this.idBuffer.clear();
        MemoryTracker.genTextures(this.idBuffer);
        int i = this.idBuffer.get(0);
        this.load(image, i);
        this.textureImages.put(i, image);
        return i;
    }

    public void load(BufferedImage image, int id) {
        int k;
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)id);
        if (MIPMAP) {
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_MIN_FILTER, (int)GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_MAG_FILTER, (int)GL11.GL_LINEAR);
        } else {
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_MIN_FILTER, (int)GL11.GL_NEAREST);
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_MAG_FILTER, (int)GL11.GL_NEAREST);
        }
        if (this.blur) {
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_MIN_FILTER, (int)GL11.GL_LINEAR);
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_MAG_FILTER, (int)GL11.GL_LINEAR);
        }
        if (this.clamp) {
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_WRAP_S, (int)GL11.GL_CLAMP);
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_WRAP_T, (int)GL11.GL_CLAMP);
        } else {
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_WRAP_S, (int)GL11.GL_REPEAT);
            GL11.glTexParameteri((int)GL11.GL_TEXTURE_2D, (int)GL11.GL_TEXTURE_WRAP_T, (int)GL11.GL_REPEAT);
        }
        int i = image.getWidth();
        int j = image.getHeight();
        int[] is = new int[i * j];
        byte[] bs = new byte[i * j * 4];
        image.getRGB(0, 0, i, j, is, 0, i);
        for (k = 0; k < is.length; ++k) {
            int l = is[k] >> 24 & 0xFF;
            int n = is[k] >> 16 & 0xFF;
            int p = is[k] >> 8 & 0xFF;
            int r = is[k] & 0xFF;
            if (this.options != null && this.options.anaglyph) {
                int t = (n * 30 + p * 59 + r * 11) / 100;
                int v = (n * 30 + p * 70) / 100;
                int x = (n * 30 + r * 70) / 100;
                n = t;
                p = v;
                r = x;
            }
            bs[k * 4 + 0] = (byte)n;
            bs[k * 4 + 1] = (byte)p;
            bs[k * 4 + 2] = (byte)r;
            bs[k * 4 + 3] = (byte)l;
        }
        this.imageBuffer.clear();
        this.imageBuffer.put(bs);
        this.imageBuffer.position(0).limit(bs.length);
        GL11.glTexImage2D((int)3553, (int)0, (int)6408, (int)i, (int)j, (int)0, (int)6408, (int)5121, (ByteBuffer)this.imageBuffer);
        if (MIPMAP) {
            for (k = 1; k <= 4; ++k) {
                int m = i >> k - 1;
                int o = i >> k;
                int q = j >> k;
                for (int s = 0; s < o; ++s) {
                    for (int u = 0; u < q; ++u) {
                        int w = this.imageBuffer.getInt((s * 2 + 0 + (u * 2 + 0) * m) * 4);
                        int y = this.imageBuffer.getInt((s * 2 + 1 + (u * 2 + 0) * m) * 4);
                        int z = this.imageBuffer.getInt((s * 2 + 1 + (u * 2 + 1) * m) * 4);
                        int aa = this.imageBuffer.getInt((s * 2 + 0 + (u * 2 + 1) * m) * 4);
                        int ab = this.crispBlend(this.crispBlend(w, y), this.crispBlend(z, aa));
                        this.imageBuffer.putInt((s + u * o) * 4, ab);
                    }
                }
                GL11.glTexImage2D((int)3553, (int)k, (int)6408, (int)o, (int)q, (int)0, (int)6408, (int)5121, (ByteBuffer)this.imageBuffer);
            }
        }
    }

    public void release(int id) {
        this.textureImages.remove(id);
        this.idBuffer.clear();
        this.idBuffer.put(id);
        this.idBuffer.flip();
        GL11.glDeleteTextures((IntBuffer)this.idBuffer);
    }

    public int loadHttpTexture(String url, String backup) {
        HttpTexture httpTexture = (HttpTexture)this.httpTextures.get(url);
        if (httpTexture != null && httpTexture.image != null && !httpTexture.uploaded) {
            if (httpTexture.id < 0) {
                httpTexture.id = this.load(httpTexture.image);
            } else {
                this.load(httpTexture.image, httpTexture.id);
            }
            httpTexture.uploaded = true;
        }
        if (httpTexture == null || httpTexture.id < 0) {
            return this.load(backup);
        }
        return httpTexture.id;
    }

    public HttpTexture addHttpTexture(String url, HttpImageProcessor processor) {
        HttpTexture httpTexture = (HttpTexture)this.httpTextures.get(url);
        if (httpTexture == null) {
            this.httpTextures.put(url, new HttpTexture(url, processor));
        } else {
            ++httpTexture.count;
        }
        return httpTexture;
    }

    public void removeHttpTexture(String url) {
        HttpTexture httpTexture = (HttpTexture)this.httpTextures.get(url);
        if (httpTexture != null) {
            --httpTexture.count;
            if (httpTexture.count == 0) {
                if (httpTexture.id >= 0) {
                    this.release(httpTexture.id);
                }
                this.httpTextures.remove(url);
            }
        }
    }

    public void addDynamicTexture(DynamicTexture texture) {
        this.dynamicTextures.add(texture);
        texture.tick();
    }

    public void tick() {
        int i;
        for (i = 0; i < this.dynamicTextures.size(); ++i) {
            DynamicTexture dynamicTexture = (DynamicTexture)this.dynamicTextures.get(i);
            dynamicTexture.anaglyph = this.options.anaglyph;
            dynamicTexture.tick();
            this.imageBuffer.clear();
            this.imageBuffer.put(dynamicTexture.pixels);
            this.imageBuffer.position(0).limit(dynamicTexture.pixels.length);
            dynamicTexture.bind(this);
            for (int j = 0; j < dynamicTexture.replicate; ++j) {
                for (int l = 0; l < dynamicTexture.replicate; ++l) {
                    GL11.glTexSubImage2D((int)3553, (int)0, (int)(dynamicTexture.sprite % 16 * 16 + j * 16), (int)(dynamicTexture.sprite / 16 * 16 + l * 16), (int)16, (int)16, (int)6408, (int)5121, (ByteBuffer)this.imageBuffer);
                    if (!MIPMAP) continue;
                    for (int n = 1; n <= 4; ++n) {
                        int p = 16 >> n - 1;
                        int r = 16 >> n;
                        for (int t = 0; t < r; ++t) {
                            for (int v = 0; v < r; ++v) {
                                int x = this.imageBuffer.getInt((t * 2 + 0 + (v * 2 + 0) * p) * 4);
                                int z = this.imageBuffer.getInt((t * 2 + 1 + (v * 2 + 0) * p) * 4);
                                int ab = this.imageBuffer.getInt((t * 2 + 1 + (v * 2 + 1) * p) * 4);
                                int ad = this.imageBuffer.getInt((t * 2 + 0 + (v * 2 + 1) * p) * 4);
                                int ae = this.smoothBlend(this.smoothBlend(x, z), this.smoothBlend(ab, ad));
                                this.imageBuffer.putInt((t + v * r) * 4, ae);
                            }
                        }
                        GL11.glTexSubImage2D((int)3553, (int)n, (int)(dynamicTexture.sprite % 16 * r), (int)(dynamicTexture.sprite / 16 * r), (int)r, (int)r, (int)6408, (int)5121, (ByteBuffer)this.imageBuffer);
                    }
                }
            }
        }
        for (i = 0; i < this.dynamicTextures.size(); ++i) {
            DynamicTexture dynamicTexture2 = (DynamicTexture)this.dynamicTextures.get(i);
            if (dynamicTexture2.copyTo <= 0) continue;
            this.imageBuffer.clear();
            this.imageBuffer.put(dynamicTexture2.pixels);
            this.imageBuffer.position(0).limit(dynamicTexture2.pixels.length);
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)dynamicTexture2.copyTo);
            GL11.glTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)16, (int)16, (int)6408, (int)5121, (ByteBuffer)this.imageBuffer);
            if (!MIPMAP) continue;
            for (int k = 1; k <= 4; ++k) {
                int m = 16 >> k - 1;
                int o = 16 >> k;
                for (int q = 0; q < o; ++q) {
                    for (int s = 0; s < o; ++s) {
                        int u = this.imageBuffer.getInt((q * 2 + 0 + (s * 2 + 0) * m) * 4);
                        int w = this.imageBuffer.getInt((q * 2 + 1 + (s * 2 + 0) * m) * 4);
                        int y = this.imageBuffer.getInt((q * 2 + 1 + (s * 2 + 1) * m) * 4);
                        int aa = this.imageBuffer.getInt((q * 2 + 0 + (s * 2 + 1) * m) * 4);
                        int ac = this.smoothBlend(this.smoothBlend(u, w), this.smoothBlend(y, aa));
                        this.imageBuffer.putInt((q + s * o) * 4, ac);
                    }
                }
                GL11.glTexSubImage2D((int)3553, (int)k, (int)0, (int)0, (int)o, (int)o, (int)6408, (int)5121, (ByteBuffer)this.imageBuffer);
            }
        }
    }

    private int smoothBlend(int color1, int color2) {
        int i = (color1 & 0xFF000000) >> 24 & 0xFF;
        int j = (color2 & 0xFF000000) >> 24 & 0xFF;
        return (i + j >> 1 << 24) + ((color1 & 0xFEFEFE) + (color2 & 0xFEFEFE) >> 1);
    }

    private int crispBlend(int color1, int color2) {
        int i = (color1 & 0xFF000000) >> 24 & 0xFF;
        int j = (color2 & 0xFF000000) >> 24 & 0xFF;
        int k = 255;
        if (i + j == 0) {
            i = 1;
            j = 1;
            k = 0;
        }
        int l = (color1 >> 16 & 0xFF) * i;
        int m = (color1 >> 8 & 0xFF) * i;
        int n = (color1 & 0xFF) * i;
        int o = (color2 >> 16 & 0xFF) * j;
        int p = (color2 >> 8 & 0xFF) * j;
        int q = (color2 & 0xFF) * j;
        int r = (l + o) / (i + j);
        int s = (m + p) / (i + j);
        int t = (n + q) / (i + j);
        return k << 24 | r << 16 | s << 8 | t;
    }

    public void reload() {
        BufferedImage bufferedImage;
        TexturePack texturePack = this.texturePacks.selected;
        Iterator<Object> iterator = this.textureImages.keySet().iterator();
        while (iterator.hasNext()) {
            int i = (Integer)iterator.next();
            bufferedImage = (BufferedImage)this.textureImages.get(i);
            this.load(bufferedImage, i);
        }
        for (HttpTexture httpTexture : this.httpTextures.values()) {
            httpTexture.uploaded = false;
        }
        for (String string : this.textureIds.keySet()) {
            try {
                BufferedImage bufferedImage5;
                if (string.startsWith("##")) {
                    bufferedImage = this.rescale(this.readImage(texturePack.getResource(string.substring(2))));
                } else if (string.startsWith("%clamp%")) {
                    this.clamp = true;
                    BufferedImage bufferedImage2 = this.readImage(texturePack.getResource(string.substring(7)));
                } else if (string.startsWith("%blur%")) {
                    this.blur = true;
                    BufferedImage bufferedImage3 = this.readImage(texturePack.getResource(string.substring(6)));
                } else {
                    bufferedImage5 = this.readImage(texturePack.getResource(string));
                }
                int j = (Integer)this.textureIds.get(string);
                this.load(bufferedImage5, j);
                this.blur = false;
                this.clamp = false;
            }
            catch (IOException iOException) {
                iOException.printStackTrace();
            }
        }
    }

    private BufferedImage readImage(InputStream is) {
        BufferedImage bufferedImage = ImageIO.read(is);
        is.close();
        return bufferedImage;
    }

    public void bind(int id) {
        if (id < 0) {
            return;
        }
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)id);
    }
}

