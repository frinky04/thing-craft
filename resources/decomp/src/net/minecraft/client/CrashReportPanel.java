/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.Sys
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.crash.CrashReport;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class CrashReportPanel
extends Panel {
    public CrashReportPanel(CrashReport report) {
        this.setBackground(new Color(3028036));
        this.setLayout(new BorderLayout());
        StringWriter stringWriter = new StringWriter();
        report.exception.printStackTrace(new PrintWriter(stringWriter));
        String string = stringWriter.toString();
        String string2 = "";
        String string3 = "";
        try {
            string3 = string3 + "Generated " + new SimpleDateFormat().format(new Date()) + "\n";
            string3 = string3 + "\n";
            string3 = string3 + "Minecraft: Minecraft Alpha v1.2.6\n";
            string3 = string3 + "OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version") + "\n";
            string3 = string3 + "Java: " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor") + "\n";
            string3 = string3 + "VM: " + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor") + "\n";
            string3 = string3 + "LWJGL: " + Sys.getVersion() + "\n";
            string2 = GL11.glGetString((int)7936);
            string3 = string3 + "OpenGL: " + GL11.glGetString((int)7937) + " version " + GL11.glGetString((int)7938) + ", " + GL11.glGetString((int)7936) + "\n";
        }
        catch (Throwable throwable) {
            string3 = string3 + "[failed to get system properties (" + throwable + ")]\n";
        }
        string3 = string3 + "\n";
        string3 = string3 + string;
        String string4 = "";
        string4 = string4 + "\n";
        string4 = string4 + "\n";
        if (string.contains("Pixel format not accelerated")) {
            string4 = string4 + "      Bad video card drivers!      \n";
            string4 = string4 + "      -----------------------      \n";
            string4 = string4 + "\n";
            string4 = string4 + "Minecraft was unable to start because it failed to find an accelerated OpenGL mode.\n";
            string4 = string4 + "This can usually be fixed by updating the video card drivers.\n";
            if (string2.toLowerCase().contains("nvidia")) {
                string4 = string4 + "\n";
                string4 = string4 + "You might be able to find drivers for your video card here:\n";
                string4 = string4 + "  http://www.nvidia.com/\n";
            } else if (string2.toLowerCase().contains("ati")) {
                string4 = string4 + "\n";
                string4 = string4 + "You might be able to find drivers for your video card here:\n";
                string4 = string4 + "  http://www.amd.com/\n";
            }
        } else {
            string4 = string4 + "      Minecraft has crashed!      \n";
            string4 = string4 + "      ----------------------      \n";
            string4 = string4 + "\n";
            string4 = string4 + "Minecraft has stopped running because it encountered a problem.\n";
            string4 = string4 + "\n";
            string4 = string4 + "If you wish to report this, please copy this entire text and email it to support@mojang.com.\n";
            string4 = string4 + "Please include a description of what you did when the error occured.\n";
        }
        string4 = string4 + "\n";
        string4 = string4 + "\n";
        string4 = string4 + "\n";
        string4 = string4 + "--- BEGIN ERROR REPORT " + Integer.toHexString(string4.hashCode()) + " --------\n";
        string4 = string4 + string3;
        string4 = string4 + "--- END ERROR REPORT " + Integer.toHexString(string4.hashCode()) + " ----------\n";
        string4 = string4 + "\n";
        string4 = string4 + "\n";
        TextArea textArea = new TextArea(string4, 0, 0, 1);
        textArea.setFont(new Font("Monospaced", 0, 12));
        this.add((Component)new LogoCanvas(), "North");
        this.add((Component)new BorderCanvas(80), "East");
        this.add((Component)new BorderCanvas(80), "West");
        this.add((Component)new BorderCanvas(100), "South");
        this.add((Component)textArea, "Center");
    }

    @Environment(value=EnvType.CLIENT)
    static class LogoCanvas
    extends Canvas {
        private BufferedImage logo;

        public LogoCanvas() {
            try {
                this.logo = ImageIO.read(CrashReportPanel.class.getResource("/gui/logo.png"));
            }
            catch (IOException iOException) {
                // empty catch block
            }
            int i = 100;
            this.setPreferredSize(new Dimension(i, i));
            this.setMinimumSize(new Dimension(i, i));
        }

        public void paint(Graphics graphics) {
            super.paint(graphics);
            graphics.drawImage(this.logo, this.getWidth() / 2 - this.logo.getWidth() / 2, 32, null);
        }
    }

    @Environment(value=EnvType.CLIENT)
    static class BorderCanvas
    extends Canvas {
        public BorderCanvas(int size) {
            this.setPreferredSize(new Dimension(size, size));
            this.setMinimumSize(new Dimension(size, size));
        }
    }
}

