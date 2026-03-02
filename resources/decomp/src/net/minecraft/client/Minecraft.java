/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.LWJGLException
 *  org.lwjgl.input.Controllers
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.input.Mouse
 *  org.lwjgl.opengl.Display
 *  org.lwjgl.opengl.DisplayMode
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.util.glu.GLU
 */
package net.minecraft.client;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.client.CrashReportPanel;
import net.minecraft.client.CreativeInteractionManager;
import net.minecraft.client.MinecraftApplet;
import net.minecraft.client.Mouse;
import net.minecraft.client.ParticleManager;
import net.minecraft.client.Screenshot;
import net.minecraft.client.Session;
import net.minecraft.client.TickTimer;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.entity.mob.player.KeyboardInput;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.WorldSaveConflictScreen;
import net.minecraft.client.gui.screen.inventory.menu.SurvivalInventoryScreen;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ItemInHandRenderer;
import net.minecraft.client.render.OpenGlCapabilities;
import net.minecraft.client.render.ProgressRenderError;
import net.minecraft.client.render.ProgressRenderer;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.client.render.platform.MemoryTracker;
import net.minecraft.client.render.texture.ClockSprite;
import net.minecraft.client.render.texture.CompassSprite;
import net.minecraft.client.render.texture.FireSprite;
import net.minecraft.client.render.texture.LavaSideSprite;
import net.minecraft.client.render.texture.LavaSprite;
import net.minecraft.client.render.texture.NetherPortalSprite;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.texture.WaterSideSprite;
import net.minecraft.client.render.texture.WaterSprite;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.render.world.RenderChunk;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.client.resource.ResourceDownloader;
import net.minecraft.client.resource.pack.TexturePacks;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.PortalForcer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.NetherDimension;
import net.minecraft.world.storage.exception.SessionLockException;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

@Environment(value=EnvType.CLIENT)
public abstract class Minecraft
implements Runnable {
    public ClientPlayerInteractionManager interactionManager;
    private boolean fullscreen = false;
    public int width;
    public int height;
    private OpenGlCapabilities openGlCapabilities;
    private TickTimer timer = new TickTimer(20.0f);
    public World world;
    public WorldRenderer worldRenderer;
    public ClientPlayerEntity player;
    public ParticleManager particleManager;
    public Session session = null;
    public String hostAddress;
    public Canvas canvas;
    public boolean appletMode = true;
    public volatile boolean paused = false;
    public TextureManager textureManager;
    public TextRenderer textRenderer;
    public Screen screen = null;
    public ProgressRenderer progressRenderer = new ProgressRenderer(this);
    public GameRenderer gameRenderer = new GameRenderer(this);
    private ResourceDownloader resourceDownloader;
    private int ticks = 0;
    private int attackCooldown = 0;
    private int initWidth;
    private int initHeight;
    public String loadmapUser = null;
    public int loadmapId = 0;
    public GameGui gui;
    public boolean skipGameRender = false;
    public HumanoidModel humanoidModel = new HumanoidModel(0.0f);
    public HitResult crosshairTarget = null;
    public GameOptions options;
    protected MinecraftApplet applet;
    public SoundEngine soundEngine = new SoundEngine();
    public Mouse mouse;
    public TexturePacks texturePacks;
    public File gameDir;
    public static long[] frameTimes = new long[512];
    public static long[] tickTimes = new long[512];
    public static int frameTimeIndex = 0;
    private String startupServerAddress;
    private int startupServerPort;
    private WaterSprite waterSprite = new WaterSprite();
    private LavaSprite lavaSprite = new LavaSprite();
    private static File workingDirectory = null;
    public volatile boolean running = true;
    public String fpsDebugInfo = "";
    boolean screenshotKeyDown = false;
    long timeAfterLastTick = -1L;
    public boolean focused = false;
    private int lastClickTicks = 0;
    public boolean raining = false;
    long lastTickTime = System.currentTimeMillis();
    private int joinPlayerCounter = 0;

    public Minecraft(Component component, Canvas canvas, MinecraftApplet applet, int width, int height, boolean fullscreen) {
        this.initWidth = width;
        this.initHeight = height;
        this.fullscreen = fullscreen;
        this.applet = applet;
        new Thread("Timer hack thread"){
            {
                this.setDaemon(true);
                this.start();
            }

            public void run() {
                while (Minecraft.this.running) {
                    try {
                        Thread.sleep(Integer.MAX_VALUE);
                    }
                    catch (InterruptedException interruptedException) {}
                }
            }
        };
        this.canvas = canvas;
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
    }

    public abstract void handleCrash(CrashReport var1);

    public void setStartupServer(String address, int port) {
        this.startupServerAddress = address;
        this.startupServerPort = port;
    }

    public void init() {
        if (this.canvas != null) {
            Graphics graphics = this.canvas.getGraphics();
            if (graphics != null) {
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, this.width, this.height);
                graphics.dispose();
            }
            Display.setParent((Canvas)this.canvas);
        } else if (this.fullscreen) {
            Display.setFullscreen((boolean)true);
            this.width = Display.getDisplayMode().getWidth();
            this.height = Display.getDisplayMode().getHeight();
            if (this.width <= 0) {
                this.width = 1;
            }
            if (this.height <= 0) {
                this.height = 1;
            }
        } else {
            Display.setDisplayMode((DisplayMode)new DisplayMode(this.width, this.height));
        }
        Display.setTitle((String)"Minecraft Minecraft Alpha v1.2.6");
        try {
            Display.create();
        }
        catch (LWJGLException lWJGLException) {
            lWJGLException.printStackTrace();
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            Display.create();
        }
        EntityRenderDispatcher.INSTANCE.itemInHandRenderer = new ItemInHandRenderer(this);
        this.gameDir = Minecraft.getWorkingDirectory();
        this.options = new GameOptions(this, this.gameDir);
        this.texturePacks = new TexturePacks(this, this.gameDir);
        this.textureManager = new TextureManager(this.texturePacks, this.options);
        this.textRenderer = new TextRenderer(this.options, "/font/default.png", this.textureManager);
        this.renderLoadingScreen();
        Keyboard.create();
        org.lwjgl.input.Mouse.create();
        this.mouse = new Mouse(this.canvas);
        try {
            Controllers.create();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        this.logGlError("Pre startup");
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        GL11.glShadeModel((int)GL11.GL_SMOOTH);
        GL11.glClearDepth((double)1.0);
        GL11.glEnable((int)GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc((int)GL11.GL_LEQUAL);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc((int)GL11.GL_GREATER, (float)0.1f);
        GL11.glCullFace((int)GL11.GL_BACK);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        this.logGlError("Startup");
        this.openGlCapabilities = new OpenGlCapabilities();
        this.soundEngine.load(this.options);
        this.textureManager.addDynamicTexture(this.lavaSprite);
        this.textureManager.addDynamicTexture(this.waterSprite);
        this.textureManager.addDynamicTexture(new NetherPortalSprite());
        this.textureManager.addDynamicTexture(new CompassSprite(this));
        this.textureManager.addDynamicTexture(new ClockSprite(this));
        this.textureManager.addDynamicTexture(new WaterSideSprite());
        this.textureManager.addDynamicTexture(new LavaSideSprite());
        this.textureManager.addDynamicTexture(new FireSprite(0));
        this.textureManager.addDynamicTexture(new FireSprite(1));
        this.worldRenderer = new WorldRenderer(this, this.textureManager);
        GL11.glViewport((int)0, (int)0, (int)this.width, (int)this.height);
        this.particleManager = new ParticleManager(this.world, this.textureManager);
        try {
            this.resourceDownloader = new ResourceDownloader(this.gameDir, this);
            this.resourceDownloader.start();
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.logGlError("Post startup");
        this.gui = new GameGui(this);
        if (this.startupServerAddress != null) {
            this.openScreen(new ConnectScreen(this, this.startupServerAddress, this.startupServerPort));
        } else {
            this.openScreen(new TitleScreen());
        }
    }

    private void renderLoadingScreen() {
        Window window = new Window(this.width, this.height);
        int i = window.getWidth();
        int j = window.getHeight();
        GL11.glClear((int)16640);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)i, (double)j, (double)0.0, (double)1000.0, (double)3000.0);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-2000.0f);
        GL11.glViewport((int)0, (int)0, (int)this.width, (int)this.height);
        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        Tesselator tesselator = Tesselator.INSTANCE;
        GL11.glDisable((int)GL11.GL_LIGHTING);
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
        GL11.glDisable((int)GL11.GL_FOG);
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.textureManager.load("/title/mojang.png"));
        tesselator.begin();
        tesselator.color(0xFFFFFF);
        tesselator.vertex(0.0, this.height, 0.0, 0.0, 0.0);
        tesselator.vertex(this.width, this.height, 0.0, 0.0, 0.0);
        tesselator.vertex(this.width, 0.0, 0.0, 0.0, 0.0);
        tesselator.vertex(0.0, 0.0, 0.0, 0.0, 0.0);
        tesselator.end();
        int k = 256;
        int l = 256;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        tesselator.color(0xFFFFFF);
        this.draw((this.width / 2 - k) / 2, (this.height / 2 - l) / 2, 0, 0, k, l);
        GL11.glDisable((int)GL11.GL_LIGHTING);
        GL11.glDisable((int)GL11.GL_FOG);
        GL11.glEnable((int)GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc((int)GL11.GL_GREATER, (float)0.1f);
        Display.swapBuffers();
    }

    public void draw(int x, int y, int u, int v, int width, int height) {
        float f = 0.00390625f;
        float g = 0.00390625f;
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin();
        tesselator.vertex(x + 0, y + height, 0.0, (float)(u + 0) * f, (float)(v + height) * g);
        tesselator.vertex(x + width, y + height, 0.0, (float)(u + width) * f, (float)(v + height) * g);
        tesselator.vertex(x + width, y + 0, 0.0, (float)(u + width) * f, (float)(v + 0) * g);
        tesselator.vertex(x + 0, y + 0, 0.0, (float)(u + 0) * f, (float)(v + 0) * g);
        tesselator.end();
    }

    public static File getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = Minecraft.getWorkingDirectory("minecraft");
        }
        return workingDirectory;
    }

    public static File getWorkingDirectory(String name) {
        File file5;
        String string = System.getProperty("user.home", ".");
        switch (Minecraft.getOs()) {
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

    public void openScreen(Screen screen) {
        DeathScreen deathScreen;
        if (this.screen instanceof FatalErrorScreen) {
            return;
        }
        if (this.screen != null) {
            this.screen.removed();
        }
        if (screen == null && this.world == null) {
            screen = new TitleScreen();
        } else if (deathScreen == null && this.player.health <= 0) {
            deathScreen = new DeathScreen();
        }
        this.screen = deathScreen;
        if (deathScreen != null) {
            this.unlockMouse();
            Window window = new Window(this.width, this.height);
            int i = window.getWidth();
            int j = window.getHeight();
            deathScreen.init(this, i, j);
            this.skipGameRender = false;
        } else {
            this.lockMouse();
        }
    }

    private void logGlError(String message) {
        int i = GL11.glGetError();
        if (i != 0) {
            String string = GLU.gluErrorString((int)i);
            System.out.println("########## GL ERROR ##########");
            System.out.println("@ " + message);
            System.out.println(i + ": " + string);
            System.exit(0);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void shutdown() {
        if (this.applet != null) {
            this.applet.clearMemory();
        }
        try {
            if (this.resourceDownloader != null) {
                this.resourceDownloader.shutdown();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            System.out.println("Stopping!");
            this.setWorld(null);
            try {
                MemoryTracker.releaseLists();
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.soundEngine.close();
            org.lwjgl.input.Mouse.destroy();
            Keyboard.destroy();
        }
        finally {
            Display.destroy();
        }
        System.gc();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void run() {
        this.running = true;
        try {
            this.init();
        }
        catch (Exception exception) {
            exception.printStackTrace();
            this.handleCrash(new CrashReport("Failed to start game", exception));
            return;
        }
        try {
            long l = System.currentTimeMillis();
            int i = 0;
            while (this.running && (this.applet == null || this.applet.isActive())) {
                Box.resetPool();
                Vec3d.resetPool();
                if (this.canvas == null && Display.isCloseRequested()) {
                    this.stop();
                }
                if (this.paused && this.world != null) {
                    float f = this.timer.partialTick;
                    this.timer.advance();
                    this.timer.partialTick = f;
                } else {
                    this.timer.advance();
                }
                long m = System.nanoTime();
                for (int j = 0; j < this.timer.ticksThisFrame; ++j) {
                    ++this.ticks;
                    try {
                        this.tick();
                        continue;
                    }
                    catch (SessionLockException sessionLockException) {
                        this.world = null;
                        this.setWorld(null);
                        this.openScreen(new WorldSaveConflictScreen());
                    }
                }
                long n = System.nanoTime() - m;
                this.logGlError("Pre render");
                this.soundEngine.update(this.player, this.timer.partialTick);
                GL11.glEnable((int)GL11.GL_TEXTURE_2D);
                if (this.world != null && !this.world.isMultiplayer) {
                    while (this.world.doLightUpdates()) {
                    }
                }
                if (this.world != null && this.world.isMultiplayer) {
                    this.world.doLightUpdates();
                }
                if (this.options.fpsLimit) {
                    Thread.sleep(5L);
                }
                if (!Keyboard.isKeyDown((int)65)) {
                    Display.update();
                }
                if (!this.skipGameRender) {
                    if (this.interactionManager != null) {
                        this.interactionManager.render(this.timer.partialTick);
                    }
                    this.gameRenderer.render(this.timer.partialTick);
                }
                if (!Display.isActive()) {
                    if (this.fullscreen) {
                        this.toggleFullscreen();
                    }
                    Thread.sleep(10L);
                }
                if (Keyboard.isKeyDown((int)61)) {
                    this.renderProfilerChart(n);
                } else {
                    this.timeAfterLastTick = System.nanoTime();
                }
                Thread.yield();
                if (Keyboard.isKeyDown((int)65)) {
                    Display.update();
                }
                this.handleScreenshotKey();
                if (!(this.canvas == null || this.fullscreen || this.canvas.getWidth() == this.width && this.canvas.getHeight() == this.height)) {
                    this.width = this.canvas.getWidth();
                    this.height = this.canvas.getHeight();
                    if (this.width <= 0) {
                        this.width = 1;
                    }
                    if (this.height <= 0) {
                        this.height = 1;
                    }
                    this.resize(this.width, this.height);
                }
                this.logGlError("Post render");
                ++i;
                boolean bl = this.paused = !this.isMultiplayer() && this.screen != null && this.screen.isPauseScreen();
                while (System.currentTimeMillis() >= l + 1000L) {
                    this.fpsDebugInfo = i + " fps, " + RenderChunk.updateCounter + " chunk updates";
                    RenderChunk.updateCounter = 0;
                    l += 1000L;
                    i = 0;
                }
            }
        }
        catch (ProgressRenderError progressRenderError) {
        }
        catch (Throwable throwable) {
            this.world = null;
            throwable.printStackTrace();
            this.handleCrash(new CrashReport("Unexpected error", throwable));
        }
    }

    private void handleScreenshotKey() {
        if (Keyboard.isKeyDown((int)60)) {
            if (!this.screenshotKeyDown) {
                if (Keyboard.isKeyDown((int)59)) {
                    this.gui.addChatMessage(Screenshot.take(workingDirectory, this.width, this.height));
                }
                this.screenshotKeyDown = true;
            }
        } else {
            this.screenshotKeyDown = false;
        }
    }

    private void renderProfilerChart(long tickTime) {
        int j;
        long l = 16666666L;
        if (this.timeAfterLastTick == -1L) {
            this.timeAfterLastTick = System.nanoTime();
        }
        long m = System.nanoTime();
        Minecraft.tickTimes[Minecraft.frameTimeIndex & Minecraft.frameTimes.length - 1] = tickTime;
        Minecraft.frameTimes[Minecraft.frameTimeIndex++ & Minecraft.frameTimes.length - 1] = m - this.timeAfterLastTick;
        this.timeAfterLastTick = m;
        GL11.glClear((int)GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode((int)GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)this.width, (double)this.height, (double)0.0, (double)1000.0, (double)3000.0);
        GL11.glMatrixMode((int)GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-2000.0f);
        GL11.glLineWidth((float)1.0f);
        GL11.glDisable((int)GL11.GL_TEXTURE_2D);
        Tesselator tesselator = Tesselator.INSTANCE;
        tesselator.begin(7);
        int i = (int)(l / 200000L);
        tesselator.color(0x20000000);
        tesselator.vertex(0.0, this.height - i, 0.0);
        tesselator.vertex(0.0, this.height, 0.0);
        tesselator.vertex(frameTimes.length, this.height, 0.0);
        tesselator.vertex(frameTimes.length, this.height - i, 0.0);
        tesselator.color(0x20200000);
        tesselator.vertex(0.0, this.height - i * 2, 0.0);
        tesselator.vertex(0.0, this.height - i, 0.0);
        tesselator.vertex(frameTimes.length, this.height - i, 0.0);
        tesselator.vertex(frameTimes.length, this.height - i * 2, 0.0);
        tesselator.end();
        long n = 0L;
        for (j = 0; j < frameTimes.length; ++j) {
            n += frameTimes[j];
        }
        j = (int)(n / 200000L / (long)frameTimes.length);
        tesselator.begin(7);
        tesselator.color(0x20400000);
        tesselator.vertex(0.0, this.height - j, 0.0);
        tesselator.vertex(0.0, this.height, 0.0);
        tesselator.vertex(frameTimes.length, this.height, 0.0);
        tesselator.vertex(frameTimes.length, this.height - j, 0.0);
        tesselator.end();
        tesselator.begin(1);
        for (int k = 0; k < frameTimes.length; ++k) {
            int o = (k - frameTimeIndex & frameTimes.length - 1) * 255 / frameTimes.length;
            int p = o * o / 255;
            p = p * p / 255;
            int q = p * p / 255;
            q = q * q / 255;
            if (frameTimes[k] > l) {
                tesselator.color(-16777216 + p * 65536);
            } else {
                tesselator.color(-16777216 + p * 256);
            }
            long r = frameTimes[k] / 200000L;
            long s = tickTimes[k] / 200000L;
            tesselator.vertex((float)k + 0.5f, (float)((long)this.height - r) + 0.5f, 0.0);
            tesselator.vertex((float)k + 0.5f, (float)this.height + 0.5f, 0.0);
            tesselator.color(-16777216 + p * 65536 + p * 256 + p * 1);
            tesselator.vertex((float)k + 0.5f, (float)((long)this.height - r) + 0.5f, 0.0);
            tesselator.vertex((float)k + 0.5f, (float)((long)this.height - (r - s)) + 0.5f, 0.0);
        }
        tesselator.end();
        GL11.glEnable((int)GL11.GL_TEXTURE_2D);
    }

    public void stop() {
        this.running = false;
    }

    public void lockMouse() {
        if (!Display.isActive()) {
            return;
        }
        if (this.focused) {
            return;
        }
        this.focused = true;
        this.mouse.lock();
        this.openScreen(null);
        this.lastClickTicks = this.ticks + 10000;
    }

    public void unlockMouse() {
        if (!this.focused) {
            return;
        }
        if (this.player != null) {
            this.player.releaseAllKeys();
        }
        this.focused = false;
        this.mouse.unlock();
    }

    public void pauseGame() {
        if (this.screen != null) {
            return;
        }
        this.openScreen(new GameMenuScreen());
    }

    private void handleMouseDown(int button, boolean holdingAttack) {
        if (this.interactionManager.creative) {
            return;
        }
        if (button == 0 && this.attackCooldown > 0) {
            return;
        }
        if (holdingAttack && this.crosshairTarget != null && this.crosshairTarget.type == 0 && button == 0) {
            int i = this.crosshairTarget.x;
            int j = this.crosshairTarget.y;
            int k = this.crosshairTarget.z;
            this.interactionManager.tickBlockMining(i, j, k, this.crosshairTarget.face);
            this.particleManager.handleBlockMining(i, j, k, this.crosshairTarget.face);
        } else {
            this.interactionManager.stopMiningBlock();
        }
    }

    private void handleMouseClick(int button) {
        ItemStack itemStack;
        if (button == 0 && this.attackCooldown > 0) {
            return;
        }
        if (button == 0) {
            this.player.swingArm();
        }
        boolean i = true;
        if (this.crosshairTarget == null) {
            if (button == 0 && !(this.interactionManager instanceof CreativeInteractionManager)) {
                this.attackCooldown = 10;
            }
        } else if (this.crosshairTarget.type == 1) {
            if (button == 0) {
                this.interactionManager.attackEntity(this.player, this.crosshairTarget.entity);
            }
            if (button == 1) {
                this.interactionManager.interactEntity(this.player, this.crosshairTarget.entity);
            }
        } else if (this.crosshairTarget.type == 0) {
            int j = this.crosshairTarget.x;
            int k = this.crosshairTarget.y;
            int l = this.crosshairTarget.z;
            int m = this.crosshairTarget.face;
            Block block = Block.BY_ID[this.world.getBlock(j, k, l)];
            if (button == 0) {
                this.world.extinguishFire(j, k, l, this.crosshairTarget.face);
                if (block != Block.BEDROCK || this.player.userType >= 100) {
                    this.interactionManager.startMiningBlock(j, k, l, this.crosshairTarget.face);
                }
            } else {
                int n;
                ItemStack itemStack2 = this.player.inventory.getSelectedItem();
                int n2 = n = itemStack2 != null ? itemStack2.size : 0;
                if (this.interactionManager.useBlock(this.player, this.world, itemStack2, j, k, l, m)) {
                    i = false;
                    this.player.swingArm();
                }
                if (itemStack2 == null) {
                    return;
                }
                if (itemStack2.size == 0) {
                    this.player.inventory.items[this.player.inventory.selectedSlot] = null;
                } else if (itemStack2.size != n) {
                    this.gameRenderer.itemInHandRenderer.onBlockUsed();
                }
            }
        }
        if (i && button == 1 && (itemStack = this.player.inventory.getSelectedItem()) != null && this.interactionManager.useItem(this.player, this.world, itemStack)) {
            this.gameRenderer.itemInHandRenderer.onItemUsed();
        }
    }

    public void toggleFullscreen() {
        try {
            this.fullscreen = !this.fullscreen;
            System.out.println("Toggle fullscreen!");
            if (this.fullscreen) {
                Display.setDisplayMode((DisplayMode)Display.getDesktopDisplayMode());
                this.width = Display.getDisplayMode().getWidth();
                this.height = Display.getDisplayMode().getHeight();
                if (this.width <= 0) {
                    this.width = 1;
                }
                if (this.height <= 0) {
                    this.height = 1;
                }
            } else {
                if (this.canvas != null) {
                    this.width = this.canvas.getWidth();
                    this.height = this.canvas.getHeight();
                } else {
                    this.width = this.initWidth;
                    this.height = this.initHeight;
                }
                if (this.width <= 0) {
                    this.width = 1;
                }
                if (this.height <= 0) {
                    this.height = 1;
                }
                Display.setDisplayMode((DisplayMode)new DisplayMode(this.initWidth, this.initHeight));
            }
            this.unlockMouse();
            Display.setFullscreen((boolean)this.fullscreen);
            Display.update();
            Thread.sleep(1000L);
            if (this.fullscreen) {
                this.lockMouse();
            }
            if (this.screen != null) {
                this.unlockMouse();
                this.resize(this.width, this.height);
            }
            System.out.println("Size: " + this.width + ", " + this.height);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void resize(int width, int height) {
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }
        this.width = width;
        this.height = height;
        if (this.screen != null) {
            Window window = new Window(width, height);
            int i = window.getWidth();
            int j = window.getHeight();
            this.screen.init(this, i, j);
        }
    }

    private void handlePickBlock() {
        if (this.crosshairTarget != null) {
            int i = this.world.getBlock(this.crosshairTarget.x, this.crosshairTarget.y, this.crosshairTarget.z);
            if (i == Block.GRASS.id) {
                i = Block.DIRT.id;
            }
            if (i == Block.DOUBLE_STONE_SLAB.id) {
                i = Block.STONE_SLAB.id;
            }
            if (i == Block.BEDROCK.id) {
                i = Block.STONE.id;
            }
            this.player.inventory.selectSlot(i, this.interactionManager instanceof CreativeInteractionManager);
        }
    }

    public void tick() {
        this.gui.tick();
        this.gameRenderer.pick(1.0f);
        if (this.player != null) {
            this.player.beforeTick();
        }
        if (!this.paused && this.world != null) {
            this.interactionManager.tick();
        }
        GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)this.textureManager.load("/terrain.png"));
        if (!this.paused) {
            this.textureManager.tick();
        }
        if (this.screen == null && this.player != null && this.player.health <= 0) {
            this.openScreen(null);
        }
        if (this.screen != null) {
            this.lastClickTicks = this.ticks + 10000;
        }
        if (this.screen != null) {
            this.screen.handleInputs();
            if (this.screen != null) {
                this.screen.tick();
            }
        }
        if (this.screen == null || this.screen.passEvents) {
            while (org.lwjgl.input.Mouse.next()) {
                long l = System.currentTimeMillis() - this.lastTickTime;
                if (l > 200L) continue;
                int j = org.lwjgl.input.Mouse.getEventDWheel();
                if (j != 0) {
                    this.player.inventory.scrollInHotbar(j);
                }
                if (this.screen == null) {
                    if (!this.focused && org.lwjgl.input.Mouse.getEventButtonState()) {
                        this.lockMouse();
                        continue;
                    }
                    if (org.lwjgl.input.Mouse.getEventButton() == 0 && org.lwjgl.input.Mouse.getEventButtonState()) {
                        this.handleMouseClick(0);
                        this.lastClickTicks = this.ticks;
                    }
                    if (org.lwjgl.input.Mouse.getEventButton() == 1 && org.lwjgl.input.Mouse.getEventButtonState()) {
                        this.handleMouseClick(1);
                        this.lastClickTicks = this.ticks;
                    }
                    if (org.lwjgl.input.Mouse.getEventButton() != 2 || !org.lwjgl.input.Mouse.getEventButtonState()) continue;
                    this.handlePickBlock();
                    continue;
                }
                if (this.screen == null) continue;
                this.screen.handleMouse();
            }
            if (this.attackCooldown > 0) {
                --this.attackCooldown;
            }
            while (Keyboard.next()) {
                this.player.handleKeyEvent(Keyboard.getEventKey(), Keyboard.getEventKeyState());
                if (!Keyboard.getEventKeyState()) continue;
                if (Keyboard.getEventKey() == 87) {
                    this.toggleFullscreen();
                    continue;
                }
                if (this.screen != null) {
                    this.screen.handleKeyboard();
                } else {
                    if (Keyboard.getEventKey() == 1) {
                        this.pauseGame();
                    }
                    if (Keyboard.getEventKey() == 31 && Keyboard.isKeyDown((int)61)) {
                        this.forceReload();
                    }
                    if (Keyboard.getEventKey() == 63) {
                        boolean bl = this.options.perspective = !this.options.perspective;
                    }
                    if (Keyboard.getEventKey() == this.options.inventoryKey.keyCode) {
                        this.openScreen(new SurvivalInventoryScreen(this.player.inventory, this.player.inventory.crafting));
                    }
                    if (Keyboard.getEventKey() == this.options.dropKey.keyCode) {
                        this.player.dropItem(this.player.inventory.removeItem(this.player.inventory.selectedSlot, 1), false);
                    }
                    if (this.isMultiplayer() && Keyboard.getEventKey() == this.options.chatKey.keyCode) {
                        this.openScreen(new ChatScreen());
                    }
                }
                for (int i = 0; i < 9; ++i) {
                    if (Keyboard.getEventKey() != 2 + i) continue;
                    this.player.inventory.selectedSlot = i;
                }
                if (Keyboard.getEventKey() != this.options.fogKey.keyCode) continue;
                this.options.set(4, Keyboard.isKeyDown((int)42) || Keyboard.isKeyDown((int)54) ? -1 : 1);
            }
            if (this.screen == null) {
                if (org.lwjgl.input.Mouse.isButtonDown((int)0) && (float)(this.ticks - this.lastClickTicks) >= this.timer.tps / 4.0f && this.focused) {
                    this.handleMouseClick(0);
                    this.lastClickTicks = this.ticks;
                }
                if (org.lwjgl.input.Mouse.isButtonDown((int)1) && (float)(this.ticks - this.lastClickTicks) >= this.timer.tps / 4.0f && this.focused) {
                    this.handleMouseClick(1);
                    this.lastClickTicks = this.ticks;
                }
            }
            this.handleMouseDown(0, this.screen == null && org.lwjgl.input.Mouse.isButtonDown((int)0) && this.focused);
        }
        if (this.world != null) {
            if (this.player != null) {
                ++this.joinPlayerCounter;
                if (this.joinPlayerCounter == 30) {
                    this.joinPlayerCounter = 0;
                    this.world.addEntityAlways(this.player);
                }
            }
            this.world.difficulty = this.options.difficulty;
            if (this.world.isMultiplayer) {
                this.world.difficulty = 3;
            }
            if (!this.paused) {
                this.gameRenderer.tick();
            }
            if (!this.paused) {
                this.worldRenderer.tick();
            }
            if (!this.paused) {
                this.world.tickEntities();
            }
            if (!this.paused || this.isMultiplayer()) {
                this.world.tick();
            }
            if (!this.paused && this.world != null) {
                this.world.doRandomDisplayTicks(MathHelper.floor(this.player.x), MathHelper.floor(this.player.y), MathHelper.floor(this.player.z));
            }
            if (!this.paused) {
                this.particleManager.tick();
            }
        }
        this.lastTickTime = System.currentTimeMillis();
    }

    private void forceReload() {
        System.out.println("FORCING RELOAD!");
        this.soundEngine = new SoundEngine();
        this.soundEngine.load(this.options);
        this.resourceDownloader.forceReload();
    }

    public boolean isMultiplayer() {
        return this.world != null && this.world.isMultiplayer;
    }

    public void startGame(String saveName) {
        this.setWorld(null);
        System.gc();
        World world = new World(new File(Minecraft.getWorkingDirectory(), "saves"), saveName);
        if (world.isNew) {
            this.setWorld(world, "Generating level");
        } else {
            this.setWorld(world, "Loading level");
        }
    }

    public void changeDimension() {
        this.player.dimension = this.player.dimension == -1 ? 0 : -1;
        this.world.removeEntity(this.player);
        this.player.removed = false;
        double d = this.player.x;
        double e = this.player.z;
        double f = 8.0;
        if (this.player.dimension == -1) {
            this.player.setPositionAndAngles(d /= f, this.player.y, e /= f, this.player.yaw, this.player.pitch);
            this.world.tickEntity(this.player, false);
            World world = new World(this.world, new NetherDimension());
            this.setWorld(world, "Entering the Nether", this.player);
        } else {
            this.player.setPositionAndAngles(d *= f, this.player.y, e *= f, this.player.yaw, this.player.pitch);
            this.world.tickEntity(this.player, false);
            World world2 = new World(this.world, new net.minecraft.world.dimension.Dimension());
            this.setWorld(world2, "Leaving the Nether", this.player);
        }
        this.player.world = this.world;
        this.player.setPositionAndAngles(d, this.player.y, e, this.player.yaw, this.player.pitch);
        this.world.tickEntity(this.player, false);
        new PortalForcer().onDimensionChanged(this.world, this.player);
    }

    public void setWorld(World world) {
        this.setWorld(world, "");
    }

    public void setWorld(World world, String message) {
        this.setWorld(world, message, null);
    }

    public void setWorld(World world, String message, PlayerEntity player) {
        this.progressRenderer.progressStart(message);
        this.progressRenderer.progressStage("");
        this.soundEngine.playRecord(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        if (this.world != null) {
            this.world.forceSave(this.progressRenderer);
        }
        this.world = world;
        System.out.println("Player is " + this.player);
        if (world != null) {
            this.interactionManager.initWorld(world);
            if (!this.isMultiplayer()) {
                if (player == null) {
                    this.player = (ClientPlayerEntity)world.getEntityOfSubtype(ClientPlayerEntity.class);
                }
            } else if (this.player != null) {
                this.player.resetPos();
                if (world != null) {
                    world.addEntity(this.player);
                }
            }
            if (!world.isMultiplayer) {
                this.prepareWorld(message);
            }
            System.out.println("Player is now " + this.player);
            if (this.player == null) {
                this.player = (ClientPlayerEntity)this.interactionManager.createPlayer(world);
                this.player.resetPos();
                this.interactionManager.initPlayer(this.player);
            }
            this.player.input = new KeyboardInput(this.options);
            if (this.worldRenderer != null) {
                this.worldRenderer.setWorld(world);
            }
            if (this.particleManager != null) {
                this.particleManager.setWorld(world);
            }
            this.interactionManager.adjustPlayer(this.player);
            if (player != null) {
                world.clearPlayerData();
            }
            world.loadPlayer(this.player);
            if (world.isNew) {
                world.forceSave(this.progressRenderer);
            }
        } else {
            this.player = null;
        }
        System.gc();
        this.lastTickTime = 0L;
    }

    private void prepareWorld(String saveName) {
        this.progressRenderer.progressStart(saveName);
        this.progressRenderer.progressStage("Building terrain");
        int i = 128;
        int j = 0;
        int k = i * 2 / 16 + 1;
        k *= k;
        for (int l = -i; l <= i; l += 16) {
            int m = this.world.spawnPointX;
            int n = this.world.spawnPointZ;
            if (this.player != null) {
                m = (int)this.player.x;
                n = (int)this.player.z;
            }
            for (int o = -i; o <= i; o += 16) {
                this.progressRenderer.progressStagePercentage(j++ * 100 / k);
                this.world.getBlock(m + l, 64, n + o);
                while (this.world.doLightUpdates()) {
                }
            }
        }
        this.progressRenderer.progressStage("Simulating world for a bit");
        k = 2000;
        this.world.prepare();
    }

    public void loadResource(String path, File file) {
        int i = path.indexOf("/");
        String string = path.substring(0, i);
        path = path.substring(i + 1);
        if (string.equalsIgnoreCase("sound")) {
            this.soundEngine.loadSound(path, file);
        } else if (string.equalsIgnoreCase("newsound")) {
            this.soundEngine.loadSound(path, file);
        } else if (string.equalsIgnoreCase("streaming")) {
            this.soundEngine.loadRecord(path, file);
        } else if (string.equalsIgnoreCase("music")) {
            this.soundEngine.loadMusic(path, file);
        } else if (string.equalsIgnoreCase("newmusic")) {
            this.soundEngine.loadMusic(path, file);
        }
    }

    public OpenGlCapabilities getOpenGlCapabilities() {
        return this.openGlCapabilities;
    }

    public String getRenderChunkDebugInfo() {
        return this.worldRenderer.getChunkDebugInfo();
    }

    public String getRenderEntityDebugInfo() {
        return this.worldRenderer.getEntityDebugInfo();
    }

    public String getWorldDebugInfo() {
        return "P: " + this.particleManager.getDebugInfo() + ". T: " + this.world.getDebugInfo();
    }

    public void respawnPlayer() {
        if (!this.world.dimension.hasSpawnPoint()) {
            this.changeDimension();
        }
        this.world.resetSpawnPoint();
        this.world.removeEntities();
        int i = 0;
        if (this.player != null) {
            i = this.player.networkId;
            this.world.removeEntity(this.player);
        }
        this.player = (ClientPlayerEntity)this.interactionManager.createPlayer(this.world);
        this.player.resetPos();
        this.interactionManager.initPlayer(this.player);
        this.world.loadPlayer(this.player);
        this.player.input = new KeyboardInput(this.options);
        this.player.networkId = i;
        this.interactionManager.adjustPlayer(this.player);
        this.prepareWorld("Respawning");
        if (this.screen instanceof DeathScreen) {
            this.openScreen(null);
        }
    }

    public static void start(String username, String sessionId) {
        Minecraft.startAndConnect(username, sessionId, null);
    }

    public static void startAndConnect(String username, String sessionId, String server) {
        boolean i = false;
        String string = username;
        final Frame frame = new Frame("Minecraft");
        Canvas canvas = new Canvas();
        frame.setLayout(new BorderLayout());
        frame.add((Component)canvas, "Center");
        canvas.setPreferredSize(new Dimension(854, 480));
        frame.pack();
        frame.setLocationRelativeTo(null);
        final Minecraft minecraft7 = new Minecraft(frame, canvas, null, 854, 480, i){

            public void handleCrash(CrashReport report) {
                frame.removeAll();
                frame.add((Component)new CrashReportPanel(report), "Center");
                frame.validate();
            }
        };
        final Thread thread = new Thread((Runnable)minecraft7, "Minecraft main thread");
        thread.setPriority(10);
        minecraft7.appletMode = false;
        minecraft7.hostAddress = "www.minecraft.net";
        minecraft7.session = string != null && sessionId != null ? new Session(string, sessionId) : new Session("Player" + System.currentTimeMillis() % 1000L, "");
        if (server != null) {
            String[] strings = server.split(":");
            minecraft7.setStartupServer(strings[0], Integer.parseInt(strings[1]));
        }
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter(){

            public void windowClosing(WindowEvent windowEvent) {
                minecraft7.stop();
                try {
                    thread.join();
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                System.exit(0);
            }
        });
        thread.start();
    }

    public static void main(String[] args) {
        String string = "Player" + System.currentTimeMillis() % 1000L;
        if (args.length > 0) {
            string = args[0];
        }
        String string2 = "-";
        if (args.length > 1) {
            string2 = args[1];
        }
        string = "Player" + System.currentTimeMillis() % 1000L;
        string = "Player524";
        Minecraft.start(string, string2);
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

