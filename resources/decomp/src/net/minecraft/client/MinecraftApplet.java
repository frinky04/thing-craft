/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CrashReportPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;
import net.minecraft.util.crash.CrashReport;

@Environment(value=EnvType.CLIENT)
public class MinecraftApplet
extends Applet {
    private Canvas canvas;
    private Minecraft minecraft;
    private Thread thread = null;

    public void init() {
        this.canvas = new Canvas(){

            public synchronized void addNotify() {
                super.addNotify();
                MinecraftApplet.this.startThread();
            }

            public synchronized void removeNotify() {
                MinecraftApplet.this.stopThread();
                super.removeNotify();
            }
        };
        boolean i = false;
        if (this.getParameter("fullscreen") != null) {
            i = this.getParameter("fullscreen").equalsIgnoreCase("true");
        }
        this.minecraft = new Minecraft(this, this.canvas, this, this.getWidth(), this.getHeight(), i){

            public void handleCrash(CrashReport report) {
                MinecraftApplet.this.removeAll();
                MinecraftApplet.this.setLayout(new BorderLayout());
                MinecraftApplet.this.add((Component)new CrashReportPanel(report), "Center");
                MinecraftApplet.this.validate();
            }
        };
        this.minecraft.hostAddress = this.getDocumentBase().getHost();
        if (this.getDocumentBase().getPort() > 0) {
            this.minecraft.hostAddress = this.minecraft.hostAddress + ":" + this.getDocumentBase().getPort();
        }
        if (this.getParameter("username") != null && this.getParameter("sessionid") != null) {
            this.minecraft.session = new Session(this.getParameter("username"), this.getParameter("sessionid"));
            System.out.println("Setting user: " + this.minecraft.session.username + ", " + this.minecraft.session.id);
            if (this.getParameter("mppass") != null) {
                this.minecraft.session.password = this.getParameter("mppass");
            }
        } else {
            this.minecraft.session = new Session("Player", "");
        }
        if (this.getParameter("loadmap_user") != null && this.getParameter("loadmap_id") != null) {
            this.minecraft.loadmapUser = this.getParameter("loadmap_user");
            this.minecraft.loadmapId = Integer.parseInt(this.getParameter("loadmap_id"));
        } else if (this.getParameter("server") != null && this.getParameter("port") != null) {
            this.minecraft.setStartupServer(this.getParameter("server"), Integer.parseInt(this.getParameter("port")));
        }
        this.minecraft.appletMode = true;
        this.setLayout(new BorderLayout());
        this.add((Component)this.canvas, "Center");
        this.canvas.setFocusable(true);
        this.validate();
    }

    public void startThread() {
        if (this.thread != null) {
            return;
        }
        this.thread = new Thread((Runnable)this.minecraft, "Minecraft main thread");
        this.thread.start();
    }

    public void start() {
        if (this.minecraft != null) {
            this.minecraft.paused = false;
        }
    }

    public void stop() {
        if (this.minecraft != null) {
            this.minecraft.paused = true;
        }
    }

    public void destroy() {
        this.stopThread();
    }

    public void stopThread() {
        if (this.thread == null) {
            return;
        }
        this.minecraft.stop();
        try {
            this.thread.join(10000L);
        }
        catch (InterruptedException interruptedException) {
            try {
                this.minecraft.shutdown();
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        this.thread = null;
    }

    public void clearMemory() {
        this.canvas = null;
        this.minecraft = null;
        this.thread = null;
        try {
            this.removeAll();
            this.validate();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

