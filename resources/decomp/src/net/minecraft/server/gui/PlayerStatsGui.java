/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.Timer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;

@Environment(value=EnvType.SERVER)
public class PlayerStatsGui
extends JComponent {
    private int[] memoryUsePercentage = new int[256];
    private int memoryUsage = 0;
    private String[] lines = new String[10];

    public PlayerStatsGui() {
        this.setPreferredSize(new Dimension(256, 196));
        this.setMinimumSize(new Dimension(256, 196));
        this.setMaximumSize(new Dimension(256, 196));
        new Timer(500, new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                PlayerStatsGui.this.update();
            }
        }).start();
        this.setBackground(Color.BLACK);
    }

    private void update() {
        long l = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.gc();
        this.lines[0] = "Memory use: " + l / 1024L / 1024L + " mb (" + Runtime.getRuntime().freeMemory() * 100L / Runtime.getRuntime().maxMemory() + "% free)";
        this.lines[1] = "Threads: " + Connection.READ_THREAD_COUNTER + " + " + Connection.WRITE_THREAD_COUNTER;
        this.memoryUsePercentage[this.memoryUsage++ & 0xFF] = (int)(l * 100L / Runtime.getRuntime().maxMemory());
        this.repaint();
    }

    public void paint(Graphics graphics) {
        int i;
        graphics.setColor(new Color(0xFFFFFF));
        graphics.fillRect(0, 0, 256, 192);
        for (i = 0; i < 256; ++i) {
            int j = this.memoryUsePercentage[i + this.memoryUsage & 0xFF];
            graphics.setColor(new Color(j + 28 << 16));
            graphics.fillRect(i, 100 - j, 1, j);
        }
        graphics.setColor(Color.BLACK);
        for (i = 0; i < this.lines.length; ++i) {
            String string = this.lines[i];
            if (string == null) continue;
            graphics.drawString(string, 32, 116 + i * 16);
        }
    }
}

