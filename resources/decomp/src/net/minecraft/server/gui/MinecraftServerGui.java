/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.source.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.LogHandler;
import net.minecraft.server.gui.PlayerListGui;
import net.minecraft.server.gui.PlayerStatsGui;

@Environment(value=EnvType.SERVER)
public class MinecraftServerGui
extends JComponent
implements CommandSource {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    private MinecraftServer server;

    public static void create(final MinecraftServer server) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception exception) {
            // empty catch block
        }
        MinecraftServerGui minecraftServerGui = new MinecraftServerGui(server);
        JFrame jFrame = new JFrame("Minecraft server");
        jFrame.add(minecraftServerGui);
        jFrame.pack();
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
        jFrame.addWindowListener(new WindowAdapter(){

            public void windowClosing(WindowEvent e) {
                server.stop();
                while (!server.stopped) {
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
                System.exit(0);
            }
        });
    }

    public MinecraftServerGui(MinecraftServer server) {
        this.server = server;
        this.setPreferredSize(new Dimension(854, 480));
        this.setLayout(new BorderLayout());
        try {
            this.add((Component)this.createLogPanel(), "Center");
            this.add((Component)this.createStatsPanel(), "West");
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private JComponent createStatsPanel() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add((Component)new PlayerStatsGui(), "North");
        jPanel.add((Component)this.createPlaysPanel(), "Center");
        jPanel.setBorder(new TitledBorder(new EtchedBorder(), "Stats"));
        return jPanel;
    }

    private JComponent createPlaysPanel() {
        PlayerListGui playerListGui = new PlayerListGui(this.server);
        JScrollPane jScrollPane = new JScrollPane(playerListGui, 22, 30);
        jScrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Players"));
        return jScrollPane;
    }

    private JComponent createLogPanel() {
        JPanel jPanel = new JPanel(new BorderLayout());
        JTextArea jTextArea = new JTextArea();
        LOGGER.addHandler(new LogHandler(jTextArea));
        JScrollPane jScrollPane = new JScrollPane(jTextArea, 22, 30);
        jTextArea.setEditable(false);
        final JTextField jTextField = new JTextField();
        jTextField.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                String string = jTextField.getText().trim();
                if (string.length() > 0) {
                    MinecraftServerGui.this.server.queueCommand(string, MinecraftServerGui.this);
                }
                jTextField.setText("");
            }
        });
        jTextArea.addFocusListener(new FocusAdapter(){

            public void focusGained(FocusEvent e) {
            }
        });
        jPanel.add((Component)jScrollPane, "Center");
        jPanel.add((Component)jTextField, "South");
        jPanel.setBorder(new TitledBorder(new EtchedBorder(), "Log and chat"));
        return jPanel;
    }

    public void sendMessage(String message) {
        LOGGER.info(message);
    }

    public String getCommandSourceName() {
        return "CONSOLE";
    }
}

