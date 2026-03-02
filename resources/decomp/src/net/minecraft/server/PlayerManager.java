/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PlayerRespawnPacket;
import net.minecraft.server.ChunkMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerPlayerInteractionManager;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.server.network.handler.ServerLoginNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.storage.PlayerDataStorage;

@Environment(value=EnvType.SERVER)
public class PlayerManager {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    public List players = new ArrayList();
    private MinecraftServer server;
    private ChunkMap chunkMap;
    private int maxPlayerCount;
    private Set bans = new HashSet();
    private Set ipBans = new HashSet();
    private Set ops = new HashSet();
    private File bansFile;
    private File ipBansFile;
    private File opsFile;
    private PlayerDataStorage dataStorage;

    public PlayerManager(MinecraftServer server) {
        this.server = server;
        this.bansFile = server.getFile("banned-players.txt");
        this.ipBansFile = server.getFile("banned-ips.txt");
        this.opsFile = server.getFile("ops.txt");
        this.chunkMap = new ChunkMap(server);
        this.maxPlayerCount = server.properties.getInt("max-players", 20);
        this.loadBans();
        this.loadIpBans();
        this.loadOps();
        this.saveBans();
        this.saveIpBans();
        this.saveOps();
    }

    public void setWorld(ServerWorld world) {
        this.dataStorage = new PlayerDataStorage(new File(world.saveDir, "players"));
    }

    public int getViewDistance() {
        return this.chunkMap.getViewDistance();
    }

    public void add(ServerPlayerEntity player) {
        this.players.add(player);
        this.dataStorage.loadPlayerData(player);
        this.server.world.chunkCache.loadChunk((int)player.x >> 4, (int)player.z >> 4);
        while (this.server.world.getCollisions(player, player.shape).size() != 0) {
            player.setPosition(player.x, player.y + 1.0, player.z);
        }
        this.server.world.addEntity(player);
        this.chunkMap.addPlayer(player);
    }

    public void move(ServerPlayerEntity player) {
        this.chunkMap.movePlayer(player);
    }

    public void remove(ServerPlayerEntity player) {
        this.dataStorage.savePlayerData(player);
        this.server.world.removeEntity(player);
        this.players.remove(player);
        this.chunkMap.removePlayer(player);
    }

    public ServerPlayerEntity createForLogin(ServerLoginNetworkHandler networkHandler, String name, String password) {
        if (this.bans.contains(name.trim().toLowerCase())) {
            networkHandler.disconnect("You are banned from this server!");
            return null;
        }
        String string = networkHandler.connection.getAddress().toString();
        string = string.substring(string.indexOf("/") + 1);
        if (this.ipBans.contains(string = string.substring(0, string.indexOf(":")))) {
            networkHandler.disconnect("Your IP address is banned from this server!");
            return null;
        }
        if (this.players.size() >= this.maxPlayerCount) {
            networkHandler.disconnect("The server is full!");
            return null;
        }
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
            if (!serverPlayerEntity.name.equalsIgnoreCase(name)) continue;
            serverPlayerEntity.networkHandler.disconnect("You logged in from another location");
        }
        return new ServerPlayerEntity(this.server, this.server.world, name, new ServerPlayerInteractionManager(this.server.world));
    }

    public ServerPlayerEntity respawn(ServerPlayerEntity player) {
        this.server.entityMap.onPlayerRespawn(player);
        this.server.entityMap.onEntityRemoved(player);
        this.chunkMap.removePlayer(player);
        this.players.remove(player);
        this.server.world.removeEntityNow(player);
        ServerPlayerEntity serverPlayerEntity = new ServerPlayerEntity(this.server, this.server.world, player.name, new ServerPlayerInteractionManager(this.server.world));
        serverPlayerEntity.networkId = player.networkId;
        serverPlayerEntity.networkHandler = player.networkHandler;
        this.server.world.chunkCache.loadChunk((int)serverPlayerEntity.x >> 4, (int)serverPlayerEntity.z >> 4);
        while (this.server.world.getCollisions(serverPlayerEntity, serverPlayerEntity.shape).size() != 0) {
            serverPlayerEntity.setPosition(serverPlayerEntity.x, serverPlayerEntity.y + 1.0, serverPlayerEntity.z);
        }
        serverPlayerEntity.networkHandler.sendPacket(new PlayerRespawnPacket());
        serverPlayerEntity.networkHandler.teleport(serverPlayerEntity.x, serverPlayerEntity.y, serverPlayerEntity.z, serverPlayerEntity.yaw, serverPlayerEntity.pitch);
        this.chunkMap.addPlayer(serverPlayerEntity);
        this.server.world.addEntity(serverPlayerEntity);
        this.players.add(serverPlayerEntity);
        return serverPlayerEntity;
    }

    public void tick() {
        this.chunkMap.tick();
    }

    public void onBlockChanged(int x, int y, int z) {
        this.chunkMap.onBlockChanged(x, y, z);
    }

    public void sendPacket(Packet packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
            serverPlayerEntity.networkHandler.sendPacket(packet);
        }
    }

    public String listNames() {
        String string = "";
        for (int i = 0; i < this.players.size(); ++i) {
            if (i > 0) {
                string = string + ", ";
            }
            string = string + ((ServerPlayerEntity)this.players.get((int)i)).name;
        }
        return string;
    }

    public void ban(String name) {
        this.bans.add(name.toLowerCase());
        this.saveBans();
    }

    public void unban(String name) {
        this.bans.remove(name.toLowerCase());
        this.saveBans();
    }

    private void loadBans() {
        try {
            this.bans.clear();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.bansFile));
            String string = "";
            while ((string = bufferedReader.readLine()) != null) {
                this.bans.add(string.trim().toLowerCase());
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to load ban list: " + exception);
        }
    }

    private void saveBans() {
        try {
            PrintWriter printWriter = new PrintWriter(new FileWriter(this.bansFile, false));
            for (String string : this.bans) {
                printWriter.println(string);
            }
            printWriter.close();
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to save ban list: " + exception);
        }
    }

    public void banIp(String ip) {
        this.ipBans.add(ip.toLowerCase());
        this.saveIpBans();
    }

    public void unbanIp(String ip) {
        this.ipBans.remove(ip.toLowerCase());
        this.saveIpBans();
    }

    private void loadIpBans() {
        try {
            this.ipBans.clear();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.ipBansFile));
            String string = "";
            while ((string = bufferedReader.readLine()) != null) {
                this.ipBans.add(string.trim().toLowerCase());
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to load ip ban list: " + exception);
        }
    }

    private void saveIpBans() {
        try {
            PrintWriter printWriter = new PrintWriter(new FileWriter(this.ipBansFile, false));
            for (String string : this.ipBans) {
                printWriter.println(string);
            }
            printWriter.close();
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to save ip ban list: " + exception);
        }
    }

    public void addOp(String name) {
        this.ops.add(name.toLowerCase());
        this.saveOps();
    }

    public void removeOp(String name) {
        this.ops.remove(name.toLowerCase());
        this.saveOps();
    }

    private void loadOps() {
        try {
            this.ops.clear();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.opsFile));
            String string = "";
            while ((string = bufferedReader.readLine()) != null) {
                this.ops.add(string.trim().toLowerCase());
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to load ip ban list: " + exception);
        }
    }

    private void saveOps() {
        try {
            PrintWriter printWriter = new PrintWriter(new FileWriter(this.opsFile, false));
            for (String string : this.ops) {
                printWriter.println(string);
            }
            printWriter.close();
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to save ip ban list: " + exception);
        }
    }

    public boolean isOp(String name) {
        return this.ops.contains(name.trim().toLowerCase());
    }

    public ServerPlayerEntity get(String name) {
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
            if (!serverPlayerEntity.name.equalsIgnoreCase(name)) continue;
            return serverPlayerEntity;
        }
        return null;
    }

    public void sendMessageToPlayer(String name, String message) {
        ServerPlayerEntity serverPlayerEntity = this.get(name);
        if (serverPlayerEntity != null) {
            serverPlayerEntity.networkHandler.sendPacket(new ChatMessagePacket(message));
        }
    }

    public void sendPacket(double x, double y, double z, double range, Packet packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
            double d = x - serverPlayerEntity.x;
            double e = y - serverPlayerEntity.y;
            double f = z - serverPlayerEntity.z;
            if (!(d * d + e * e + f * f < range * range)) continue;
            serverPlayerEntity.networkHandler.sendPacket(packet);
        }
    }

    public void sendMessage(String message) {
        ChatMessagePacket chatMessagePacket = new ChatMessagePacket(message);
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(i);
            if (!this.isOp(serverPlayerEntity.name)) continue;
            serverPlayerEntity.networkHandler.sendPacket(chatMessagePacket);
        }
    }

    public boolean sendPacket(String player, Packet packet) {
        ServerPlayerEntity serverPlayerEntity = this.get(player);
        if (serverPlayerEntity != null) {
            serverPlayerEntity.networkHandler.sendPacket(packet);
            return true;
        }
        return false;
    }

    public void onBlockEntityChanged(int x, int y, int z, BlockEntity blockEntity) {
        this.chunkMap.sendPacket(new BlockEntityUpdatePacket(x, y, z, blockEntity), x, y, z);
    }

    public void saveAll() {
        for (int i = 0; i < this.players.size(); ++i) {
            this.dataStorage.savePlayerData((ServerPlayerEntity)this.players.get(i));
        }
    }
}

