/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.network.handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.Random;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.DisconnectPacket;
import net.minecraft.network.packet.HandshakePacket;
import net.minecraft.network.packet.LoginPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.SpawnPointPacket;
import net.minecraft.network.packet.WorldTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.server.network.handler.ServerPlayNetworkHandler;

@Environment(value=EnvType.SERVER)
public class ServerLoginNetworkHandler
extends PacketHandler {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    private static Random RANDOM = new Random();
    public Connection connection;
    public boolean disconnected = false;
    private MinecraftServer server;
    private int ticks = 0;
    private String playerName = null;
    private LoginPacket packet = null;
    private String key = "";

    public ServerLoginNetworkHandler(MinecraftServer server, Socket socket, String name) {
        this.server = server;
        this.connection = new Connection(socket, name, this);
    }

    public void tick() {
        if (this.packet != null) {
            this.acceptLogin(this.packet);
            this.packet = null;
        }
        if (this.ticks++ == 600) {
            this.disconnect("Took too long to log in");
        } else {
            this.connection.tick();
        }
    }

    public void disconnect(String reason) {
        try {
            LOGGER.info("Disconnecting " + this.getConnectionInfo() + ": " + reason);
            this.connection.send(new DisconnectPacket(reason));
            this.connection.close();
            this.disconnected = true;
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void handleHandshake(HandshakePacket packet) {
        if (this.server.onlineMode) {
            this.key = Long.toHexString(RANDOM.nextLong());
            this.connection.send(new HandshakePacket(this.key));
        } else {
            this.connection.send(new HandshakePacket("-"));
        }
    }

    public void handleLogin(final LoginPacket packet) {
        this.playerName = packet.username;
        if (packet.gameVersion != 6) {
            if (packet.gameVersion > 6) {
                this.disconnect("Outdated server!");
            } else {
                this.disconnect("Outdated client!");
            }
            return;
        }
        if (!this.server.onlineMode) {
            this.acceptLogin(packet);
        } else {
            new Thread(){

                public void run() {
                    try {
                        String string = ServerLoginNetworkHandler.this.key;
                        URL uRL = new URL("http://www.minecraft.net/game/checkserver.jsp?user=" + packet.username + "&serverId=" + string);
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uRL.openStream()));
                        String string2 = bufferedReader.readLine();
                        bufferedReader.close();
                        System.out.println("THE REPLY IS " + string2);
                        if (string2.equals("YES")) {
                            ServerLoginNetworkHandler.this.packet = packet;
                        } else {
                            ServerLoginNetworkHandler.this.disconnect("Failed to verify username!");
                        }
                    }
                    catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }.start();
        }
    }

    public void acceptLogin(LoginPacket packet) {
        ServerPlayerEntity serverPlayerEntity = this.server.playerManager.createForLogin(this, packet.username, packet.password);
        if (serverPlayerEntity != null) {
            LOGGER.info(this.getConnectionInfo() + " logged in with entity id " + serverPlayerEntity.networkId);
            ServerPlayNetworkHandler serverPlayNetworkHandler = new ServerPlayNetworkHandler(this.server, this.connection, serverPlayerEntity);
            serverPlayNetworkHandler.sendPacket(new LoginPacket("", "", serverPlayerEntity.networkId, this.server.world.seed, (byte)this.server.world.dimension.id));
            serverPlayNetworkHandler.sendPacket(new SpawnPointPacket(this.server.world.spawnPointX, this.server.world.spawnPointY, this.server.world.spawnPointZ));
            this.server.playerManager.sendPacket(new ChatMessagePacket("\u00a7e" + serverPlayerEntity.name + " joined the game."));
            this.server.playerManager.add(serverPlayerEntity);
            serverPlayNetworkHandler.teleport(serverPlayerEntity.x, serverPlayerEntity.y, serverPlayerEntity.z, serverPlayerEntity.yaw, serverPlayerEntity.pitch);
            serverPlayNetworkHandler.sendInventory();
            this.server.connections.addConnection(serverPlayNetworkHandler);
            serverPlayNetworkHandler.sendPacket(new WorldTimePacket(this.server.world.ticks));
        }
        this.disconnected = true;
    }

    public void onDisconnect(String reason) {
        LOGGER.info(this.getConnectionInfo() + " lost connection");
        this.disconnected = true;
    }

    public void handleUnknownPacket(Packet packet) {
        this.disconnect("Protocol error");
    }

    public String getConnectionInfo() {
        if (this.playerName != null) {
            return this.playerName + " [" + this.connection.getAddress().toString() + "]";
        }
        return this.connection.getAddress().toString();
    }
}

