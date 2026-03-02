/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.handler.ServerLoginNetworkHandler;
import net.minecraft.server.network.handler.ServerPlayNetworkHandler;

@Environment(value=EnvType.SERVER)
public class ConnectionListener {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    private ServerSocket socket;
    private Thread thread;
    public volatile boolean open = false;
    private int connectionCounter = 0;
    private ArrayList pendingConnections = new ArrayList();
    private ArrayList connections = new ArrayList();
    public MinecraftServer server;

    public ConnectionListener(final MinecraftServer server, InetAddress address, int port) {
        this.server = server;
        this.socket = new ServerSocket(port, 0, address);
        this.socket.setPerformancePreferences(0, 2, 1);
        this.open = true;
        this.thread = new Thread("Listen thread"){

            public void run() {
                while (ConnectionListener.this.open) {
                    try {
                        Socket socket = ConnectionListener.this.socket.accept();
                        if (socket == null) continue;
                        ServerLoginNetworkHandler serverLoginNetworkHandler = new ServerLoginNetworkHandler(server, socket, "Connection #" + ConnectionListener.this.connectionCounter++);
                        ConnectionListener.this.addPendingConnection(serverLoginNetworkHandler);
                    }
                    catch (IOException iOException) {
                        iOException.printStackTrace();
                    }
                }
            }
        };
        this.thread.start();
    }

    public void addConnection(ServerPlayNetworkHandler connection) {
        this.connections.add(connection);
    }

    private void addPendingConnection(ServerLoginNetworkHandler connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Got null pendingconnection!");
        }
        this.pendingConnections.add(connection);
    }

    public void tick() {
        int i;
        for (i = 0; i < this.pendingConnections.size(); ++i) {
            ServerLoginNetworkHandler serverLoginNetworkHandler = (ServerLoginNetworkHandler)this.pendingConnections.get(i);
            try {
                serverLoginNetworkHandler.tick();
            }
            catch (Exception exception) {
                serverLoginNetworkHandler.disconnect("Internal server error");
                LOGGER.log(Level.WARNING, "Failed to handle packet: " + exception, exception);
            }
            if (!serverLoginNetworkHandler.disconnected) continue;
            this.pendingConnections.remove(i--);
        }
        for (i = 0; i < this.connections.size(); ++i) {
            ServerPlayNetworkHandler serverPlayNetworkHandler = (ServerPlayNetworkHandler)this.connections.get(i);
            try {
                serverPlayNetworkHandler.tick();
            }
            catch (Exception exception2) {
                LOGGER.log(Level.WARNING, "Failed to handle packet: " + exception2, exception2);
                serverPlayNetworkHandler.disconnect("Internal server error");
            }
            if (!serverPlayNetworkHandler.disconnected) continue;
            this.connections.remove(i--);
        }
    }
}

