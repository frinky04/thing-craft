/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.isom;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.isom.IsomChunkRenderer;
import net.minecraft.isom.IsomRenderChunk;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.ReadOnlyChunkCache;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;

@Environment(value=EnvType.CLIENT)
public class IsomPreviewCanvas
extends Canvas
implements KeyListener,
MouseListener,
MouseMotionListener,
Runnable {
    private int frames = 0;
    private int scale = 2;
    private boolean renderDebugInfo = true;
    private World world;
    private File workingDir;
    private boolean running = true;
    private List chunksToRender = Collections.synchronizedList(new LinkedList());
    private IsomRenderChunk[][] chunks = new IsomRenderChunk[64][64];
    private int offsetX;
    private int offsetZ;
    private int mouseX;
    private int mouseY;

    public File getWorkingDirectory() {
        if (this.workingDir == null) {
            this.workingDir = this.getWorkingDirectory("minecraft");
        }
        return this.workingDir;
    }

    public File getWorkingDirectory(String name) {
        File file5;
        String string = System.getProperty("user.home", ".");
        switch (IsomPreviewCanvas.getOs()) {
            case LINUX: 
            case SOLARIS: {
                File file = new File(string, '.' + name + '/');
                break;
            }
            case WINDOWS: {
                File file;
                String string2 = System.getenv("APPDATA");
                if (string2 != null) {
                    file = new File(string2, "." + name + '/');
                    break;
                }
                File file3 = new File(string, '.' + name + '/');
                break;
            }
            case MACOS: {
                File file4 = new File(string, "Library/Application Support/" + name);
                break;
            }
            default: {
                file5 = new File(string, name + '/');
            }
        }
        if (!file5.exists() && !file5.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + file5);
        }
        return file5;
    }

    private static OS getOs() {
        String string = System.getProperty("os.name").toLowerCase();
        if (string.contains("win")) {
            return OS.WINDOWS;
        }
        if (string.contains("mac")) {
            return OS.MACOS;
        }
        if (string.contains("solaris")) {
            return OS.SOLARIS;
        }
        if (string.contains("sunos")) {
            return OS.SOLARIS;
        }
        if (string.contains("linux")) {
            return OS.LINUX;
        }
        if (string.contains("unix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    public IsomPreviewCanvas() {
        this.workingDir = this.getWorkingDirectory();
        for (int i = 0; i < 64; ++i) {
            for (int j = 0; j < 64; ++j) {
                this.chunks[i][j] = new IsomRenderChunk(null, i, j);
            }
        }
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        this.setFocusable(true);
        this.requestFocus();
        this.setBackground(Color.red);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void openWorld(String saveName) {
        this.offsetZ = 0;
        this.offsetX = 0;
        this.world = new World(new File(this.workingDir, "saves"), saveName){

            protected ChunkSource createChunkCache(File dir) {
                return new ReadOnlyChunkCache(this, new AlphaChunkStorage(dir, false));
            }
        };
        this.world.ambientDarkness = 0;
        List list = this.chunksToRender;
        synchronized (list) {
            try {
                this.chunksToRender.clear();
                for (int i = 0; i < 64; ++i) {
                    for (int k = 0; k < 64; ++k) {
                        this.chunks[i][k].init(this.world, i, k);
                    }
                }
            }
            catch (Throwable throwable) {
                void throwable2;
                throw throwable2;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    private void setAmbientDarkness(int ambientDarkness) {
        List list = this.chunksToRender;
        synchronized (list) {
            try {
                this.world.ambientDarkness = ambientDarkness;
                this.chunksToRender.clear();
                for (int i = 0; i < 64; ++i) {
                    for (int k = 0; k < 64; ++k) {
                        this.chunks[i][k].init(this.world, i, k);
                    }
                }
            }
            catch (Throwable throwable) {
                void throwable2;
                throw throwable2;
            }
        }
    }

    public void start() {
        new Thread(){

            public void run() {
                while (IsomPreviewCanvas.this.running) {
                    IsomPreviewCanvas.this.render();
                    try {
                        Thread.sleep(1L);
                    }
                    catch (Exception exception) {}
                }
            }
        }.start();
        for (int i = 0; i < 8; ++i) {
            new Thread(this).start();
        }
    }

    public void stop() {
        this.running = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    private IsomRenderChunk getChunk(int x, int y) {
        int i = x & 0x3F;
        int j = y & 0x3F;
        IsomRenderChunk isomRenderChunk = this.chunks[i][j];
        if (isomRenderChunk.chunkX == x && isomRenderChunk.chunkZ == y) {
            return isomRenderChunk;
        }
        List list = this.chunksToRender;
        synchronized (list) {
            try {
                this.chunksToRender.remove(isomRenderChunk);
                // ** MonitorExit[list] (shouldn't be in output)
            }
            catch (Throwable throwable) {
                void throwable2;
                // ** MonitorExit[list] (shouldn't be in output)
                throw throwable2;
            }
            isomRenderChunk.init(x, y);
            return isomRenderChunk;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public void run() {
        IsomChunkRenderer isomChunkRenderer = new IsomChunkRenderer();
        while (this.running) {
            IsomRenderChunk isomRenderChunk;
            Object object = null;
            List list = this.chunksToRender;
            synchronized (list) {
                try {
                    if (this.chunksToRender.size() > 0) {
                        isomRenderChunk = (IsomRenderChunk)this.chunksToRender.remove(0);
                    }
                }
                catch (Throwable throwable) {
                    void throwable2;
                    throw throwable2;
                }
            }
            if (isomRenderChunk != null) {
                if (this.frames - isomRenderChunk.lastVisible < 2) {
                    isomChunkRenderer.render(isomRenderChunk);
                    this.repaint();
                } else {
                    isomRenderChunk.toBeRendered = false;
                }
            }
            try {
                Thread.sleep(2L);
            }
            catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

    public void update(Graphics graphics) {
    }

    public void paint(Graphics graphics) {
    }

    public void render() {
        BufferStrategy bufferStrategy = this.getBufferStrategy();
        if (bufferStrategy == null) {
            this.createBufferStrategy(2);
            return;
        }
        this.render((Graphics2D)bufferStrategy.getDrawGraphics());
        bufferStrategy.show();
    }

    public void render(Graphics2D graphics) {
        int o;
        ++this.frames;
        AffineTransform affineTransform = graphics.getTransform();
        graphics.setClip(0, 0, this.getWidth(), this.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.translate(this.getWidth() / 2, this.getHeight() / 2);
        graphics.scale(this.scale, this.scale);
        graphics.translate(this.offsetX, this.offsetZ);
        if (this.world != null) {
            graphics.translate(-(this.world.spawnPointX + this.world.spawnPointZ), -(-this.world.spawnPointX + this.world.spawnPointZ) + 64);
        }
        Rectangle rectangle = graphics.getClipBounds();
        graphics.setColor(new Color(-15724512));
        graphics.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        int i = 16;
        int j = 3;
        int k = rectangle.x / i / 2 - 2 - j;
        int l = (rectangle.x + rectangle.width) / i / 2 + 1 + j;
        int m = rectangle.y / i - 1 - j * 2;
        int n = (rectangle.y + rectangle.height + 16 + 128) / i + 1 + j * 2;
        for (o = m; o <= n; ++o) {
            for (int p = k; p <= l; ++p) {
                int q = p - (o >> 1);
                int r = p + (o + 1 >> 1);
                IsomRenderChunk isomRenderChunk = this.getChunk(q, r);
                isomRenderChunk.lastVisible = this.frames;
                if (!isomRenderChunk.rendered) {
                    if (isomRenderChunk.toBeRendered) continue;
                    isomRenderChunk.toBeRendered = true;
                    this.chunksToRender.add(isomRenderChunk);
                    continue;
                }
                isomRenderChunk.toBeRendered = false;
                if (isomRenderChunk.empty) continue;
                int s = p * i * 2 + (o & 1) * i;
                int t = o * i - 128 - 16;
                graphics.drawImage((Image)isomRenderChunk.image, s, t, null);
            }
        }
        if (this.renderDebugInfo) {
            graphics.setTransform(affineTransform);
            o = this.getHeight() - 32 - 4;
            graphics.setColor(new Color(Integer.MIN_VALUE, true));
            graphics.fillRect(4, this.getHeight() - 32 - 4, this.getWidth() - 8, 32);
            graphics.setColor(Color.WHITE);
            String string = "F1 - F5: load levels   |   0-9: Set time of day   |   Space: return to spawn   |   Double click: zoom   |   Escape: hide this text";
            graphics.drawString(string, this.getWidth() / 2 - graphics.getFontMetrics().stringWidth(string) / 2, o + 20);
        }
        graphics.dispose();
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        int i = mouseEvent.getX() / this.scale;
        int j = mouseEvent.getY() / this.scale;
        this.offsetX += i - this.mouseX;
        this.offsetZ += j - this.mouseY;
        this.mouseX = i;
        this.mouseY = j;
        this.repaint();
    }

    public void mouseMoved(MouseEvent mouseEvent) {
    }

    public void mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            this.scale = 3 - this.scale;
            this.repaint();
        }
    }

    public void mouseEntered(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }

    public void mousePressed(MouseEvent mouseEvent) {
        int i = mouseEvent.getX() / this.scale;
        int j = mouseEvent.getY() / this.scale;
        this.mouseX = i;
        this.mouseY = j;
    }

    public void mouseReleased(MouseEvent mouseEvent) {
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 48) {
            this.setAmbientDarkness(11);
        }
        if (keyEvent.getKeyCode() == 49) {
            this.setAmbientDarkness(10);
        }
        if (keyEvent.getKeyCode() == 50) {
            this.setAmbientDarkness(9);
        }
        if (keyEvent.getKeyCode() == 51) {
            this.setAmbientDarkness(7);
        }
        if (keyEvent.getKeyCode() == 52) {
            this.setAmbientDarkness(6);
        }
        if (keyEvent.getKeyCode() == 53) {
            this.setAmbientDarkness(5);
        }
        if (keyEvent.getKeyCode() == 54) {
            this.setAmbientDarkness(3);
        }
        if (keyEvent.getKeyCode() == 55) {
            this.setAmbientDarkness(2);
        }
        if (keyEvent.getKeyCode() == 56) {
            this.setAmbientDarkness(1);
        }
        if (keyEvent.getKeyCode() == 57) {
            this.setAmbientDarkness(0);
        }
        if (keyEvent.getKeyCode() == 112) {
            this.openWorld("World1");
        }
        if (keyEvent.getKeyCode() == 113) {
            this.openWorld("World2");
        }
        if (keyEvent.getKeyCode() == 114) {
            this.openWorld("World3");
        }
        if (keyEvent.getKeyCode() == 115) {
            this.openWorld("World4");
        }
        if (keyEvent.getKeyCode() == 116) {
            this.openWorld("World5");
        }
        if (keyEvent.getKeyCode() == 32) {
            this.offsetZ = 0;
            this.offsetX = 0;
        }
        if (keyEvent.getKeyCode() == 27) {
            this.renderDebugInfo = !this.renderDebugInfo;
        }
        this.repaint();
    }

    public void keyReleased(KeyEvent keyEvent) {
    }

    public void keyTyped(KeyEvent keyEvent) {
    }

    @Environment(value=EnvType.CLIENT)
    public static enum OS {
        LINUX,
        SOLARIS,
        WINDOWS,
        MACOS,
        UNKNOWN;

    }
}

