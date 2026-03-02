/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.ARBVertexBufferObject
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GLContext
 */
package net.minecraft.client.render.vertex;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.platform.MemoryTracker;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

@Environment(value=EnvType.CLIENT)
public class Tesselator {
    private static boolean TRIANGLE_MODE = true;
    private static boolean USE_VBO = false;
    private ByteBuffer byteBuffer;
    private IntBuffer intBuffer;
    private FloatBuffer floatBuffer;
    private int[] vertexBuffer;
    private int vertexCount = 0;
    private double u;
    private double v;
    private int color;
    private boolean colored = false;
    private boolean textured = false;
    private boolean hasNormals = false;
    private int index = 0;
    private int nextVertexCount = 0;
    private boolean uncolored = false;
    private int drawMode;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private int normals;
    public static final Tesselator INSTANCE = new Tesselator(0x200000);
    private boolean tesselating = false;
    private boolean useVbo = false;
    private IntBuffer vboBuffer;
    private int vbo = 0;
    private int vboCount = 10;
    private int size;

    private Tesselator(int size) {
        this.size = size;
        this.byteBuffer = MemoryTracker.createByteBuffer(size * 4);
        this.intBuffer = this.byteBuffer.asIntBuffer();
        this.floatBuffer = this.byteBuffer.asFloatBuffer();
        this.vertexBuffer = new int[size];
        boolean bl = this.useVbo = USE_VBO && GLContext.getCapabilities().GL_ARB_vertex_buffer_object;
        if (this.useVbo) {
            this.vboBuffer = MemoryTracker.createIntBuffer(this.vboCount);
            ARBVertexBufferObject.glGenBuffersARB((IntBuffer)this.vboBuffer);
        }
    }

    public void end() {
        if (!this.tesselating) {
            throw new IllegalStateException("Not tesselating!");
        }
        this.tesselating = false;
        if (this.vertexCount > 0) {
            this.intBuffer.clear();
            this.intBuffer.put(this.vertexBuffer, 0, this.index);
            this.byteBuffer.position(0);
            this.byteBuffer.limit(this.index * 4);
            if (this.useVbo) {
                this.vbo = (this.vbo + 1) % this.vboCount;
                ARBVertexBufferObject.glBindBufferARB((int)34962, (int)this.vboBuffer.get(this.vbo));
                ARBVertexBufferObject.glBufferDataARB((int)34962, (ByteBuffer)this.byteBuffer, (int)35040);
            }
            if (this.textured) {
                if (this.useVbo) {
                    GL11.glTexCoordPointer((int)2, (int)5126, (int)32, (long)12L);
                } else {
                    this.floatBuffer.position(3);
                    GL11.glTexCoordPointer((int)2, (int)32, (FloatBuffer)this.floatBuffer);
                }
                GL11.glEnableClientState((int)32888);
            }
            if (this.colored) {
                if (this.useVbo) {
                    GL11.glColorPointer((int)4, (int)5121, (int)32, (long)20L);
                } else {
                    this.byteBuffer.position(20);
                    GL11.glColorPointer((int)4, (boolean)true, (int)32, (ByteBuffer)this.byteBuffer);
                }
                GL11.glEnableClientState((int)32886);
            }
            if (this.hasNormals) {
                if (this.useVbo) {
                    GL11.glNormalPointer((int)5120, (int)32, (long)24L);
                } else {
                    this.byteBuffer.position(24);
                    GL11.glNormalPointer((int)32, (ByteBuffer)this.byteBuffer);
                }
                GL11.glEnableClientState((int)32885);
            }
            if (this.useVbo) {
                GL11.glVertexPointer((int)3, (int)5126, (int)32, (long)0L);
            } else {
                this.floatBuffer.position(0);
                GL11.glVertexPointer((int)3, (int)32, (FloatBuffer)this.floatBuffer);
            }
            GL11.glEnableClientState((int)32884);
            if (this.drawMode == 7 && TRIANGLE_MODE) {
                GL11.glDrawArrays((int)4, (int)0, (int)this.vertexCount);
            } else {
                GL11.glDrawArrays((int)this.drawMode, (int)0, (int)this.vertexCount);
            }
            GL11.glDisableClientState((int)32884);
            if (this.textured) {
                GL11.glDisableClientState((int)32888);
            }
            if (this.colored) {
                GL11.glDisableClientState((int)32886);
            }
            if (this.hasNormals) {
                GL11.glDisableClientState((int)32885);
            }
        }
        this.clear();
    }

    private void clear() {
        this.vertexCount = 0;
        this.byteBuffer.clear();
        this.index = 0;
        this.nextVertexCount = 0;
    }

    public void begin() {
        this.begin(7);
    }

    public void begin(int drawMode) {
        if (this.tesselating) {
            throw new IllegalStateException("Already tesselating!");
        }
        this.tesselating = true;
        this.clear();
        this.drawMode = drawMode;
        this.hasNormals = false;
        this.colored = false;
        this.textured = false;
        this.uncolored = false;
    }

    public void texture(double u, double v) {
        this.textured = true;
        this.u = u;
        this.v = v;
    }

    public void color(float r, float g, float b) {
        this.color((int)(r * 255.0f), (int)(g * 255.0f), (int)(b * 255.0f));
    }

    public void color(float r, float g, float b, float a) {
        this.color((int)(r * 255.0f), (int)(g * 255.0f), (int)(b * 255.0f), (int)(a * 255.0f));
    }

    public void color(int r, int g, int b) {
        this.color(r, g, b, 255);
    }

    public void color(int r, int g, int b, int a) {
        if (this.uncolored) {
            return;
        }
        if (r > 255) {
            r = 255;
        }
        if (g > 255) {
            g = 255;
        }
        if (b > 255) {
            b = 255;
        }
        if (a > 255) {
            a = 255;
        }
        if (r < 0) {
            r = 0;
        }
        if (g < 0) {
            g = 0;
        }
        if (b < 0) {
            b = 0;
        }
        if (a < 0) {
            a = 0;
        }
        this.colored = true;
        this.color = a << 24 | b << 16 | g << 8 | r;
    }

    public void vertex(double x, double y, double z, double u, double v) {
        this.texture(u, v);
        this.vertex(x, y, z);
    }

    public void vertex(double x, double y, double z) {
        ++this.nextVertexCount;
        if (this.drawMode == 7 && TRIANGLE_MODE && this.nextVertexCount % 4 == 0) {
            for (int i = 0; i < 2; ++i) {
                int j = 8 * (3 - i);
                if (this.textured) {
                    this.vertexBuffer[this.index + 3] = this.vertexBuffer[this.index - j + 3];
                    this.vertexBuffer[this.index + 4] = this.vertexBuffer[this.index - j + 4];
                }
                if (this.colored) {
                    this.vertexBuffer[this.index + 5] = this.vertexBuffer[this.index - j + 5];
                }
                this.vertexBuffer[this.index + 0] = this.vertexBuffer[this.index - j + 0];
                this.vertexBuffer[this.index + 1] = this.vertexBuffer[this.index - j + 1];
                this.vertexBuffer[this.index + 2] = this.vertexBuffer[this.index - j + 2];
                ++this.vertexCount;
                this.index += 8;
            }
        }
        if (this.textured) {
            this.vertexBuffer[this.index + 3] = Float.floatToRawIntBits((float)this.u);
            this.vertexBuffer[this.index + 4] = Float.floatToRawIntBits((float)this.v);
        }
        if (this.colored) {
            this.vertexBuffer[this.index + 5] = this.color;
        }
        if (this.hasNormals) {
            this.vertexBuffer[this.index + 6] = this.normals;
        }
        this.vertexBuffer[this.index + 0] = Float.floatToRawIntBits((float)(x + this.offsetX));
        this.vertexBuffer[this.index + 1] = Float.floatToRawIntBits((float)(y + this.offsetY));
        this.vertexBuffer[this.index + 2] = Float.floatToRawIntBits((float)(z + this.offsetZ));
        this.index += 8;
        ++this.vertexCount;
        if (this.vertexCount % 4 == 0 && this.index >= this.size - 32) {
            this.end();
            this.tesselating = true;
        }
    }

    public void color(int rgb) {
        int i = rgb >> 16 & 0xFF;
        int j = rgb >> 8 & 0xFF;
        int k = rgb & 0xFF;
        this.color(i, j, k);
    }

    public void color(int rgb, int a) {
        int i = rgb >> 16 & 0xFF;
        int j = rgb >> 8 & 0xFF;
        int k = rgb & 0xFF;
        this.color(i, j, k, a);
    }

    public void uncolored() {
        this.uncolored = true;
    }

    public void normal(float x, float y, float z) {
        if (!this.tesselating) {
            System.out.println("But..");
        }
        this.hasNormals = true;
        byte i = (byte)(x * 128.0f);
        byte j = (byte)(y * 127.0f);
        byte k = (byte)(z * 127.0f);
        this.normals = i | j << 8 | k << 16;
    }

    public void offset(double offsetX, double offsetY, double offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public void addOffset(float offsetX, float offsetY, float offsetZ) {
        this.offsetX += (double)offsetX;
        this.offsetY += (double)offsetY;
        this.offsetZ += (double)offsetZ;
    }
}

