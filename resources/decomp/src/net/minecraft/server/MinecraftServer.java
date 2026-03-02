/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.ornithemc.feather.constants.Dimensions
 */
package net.minecraft.server;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.source.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.WorldTimePacket;
import net.minecraft.server.EntityMap;
import net.minecraft.server.PendingCommand;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerLog;
import net.minecraft.server.ServerProperties;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.server.gui.MinecraftServerGui;
import net.minecraft.server.network.ConnectionListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ServerWorldEventListener;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.ornithemc.feather.constants.Dimensions;

@Environment(value=EnvType.SERVER)
public class MinecraftServer
implements CommandSource,
Runnable {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    public static HashMap GIVE_COMMANDS_COOLDOWNS = new HashMap();
    public ConnectionListener connections;
    public ServerProperties properties;
    public ServerWorld world;
    public PlayerManager playerManager;
    private boolean running = true;
    public boolean stopped = false;
    int ticks = 0;
    public String progressType;
    public int progress;
    private List tickables = new ArrayList();
    private List pendingCommands = Collections.synchronizedList(new ArrayList());
    public EntityMap entityMap;
    public boolean onlineMode;
    public boolean spawnAnimals;
    public boolean pvpEnabled;

    public MinecraftServer() {
        new Thread(){
            {
                this.setDaemon(true);
                this.start();
            }

            public void run() {
                while (true) {
                    try {
                        while (true) {
                            Thread.sleep(Integer.MAX_VALUE);
                        }
                    }
                    catch (InterruptedException interruptedException) {
                        continue;
                    }
                    break;
                }
            }
        };
    }

    private boolean init() {
        InetAddress inetAddress;
        Thread thread1 = new Thread(){

            public void run() {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                String string = null;
                try {
                    while (!MinecraftServer.this.stopped && MinecraftServer.this.running && (string = bufferedReader.readLine()) != null) {
                        MinecraftServer.this.queueCommand(string, MinecraftServer.this);
                    }
                }
                catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
        };
        thread1.setDaemon(true);
        thread1.start();
        ServerLog.init();
        LOGGER.info("Starting minecraft server version 0.2.8");
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            LOGGER.warning("**** NOT ENOUGH RAM!");
            LOGGER.warning("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }
        LOGGER.info("Loading properties");
        this.properties = new ServerProperties(new File("server.properties"));
        String string = this.properties.getString("server-ip", "");
        this.onlineMode = this.properties.getBoolean("online-mode", true);
        this.spawnAnimals = this.properties.getBoolean("spawn-animals", true);
        this.pvpEnabled = this.properties.getBoolean("pvp", true);
        Object object = null;
        if (string.length() > 0) {
            inetAddress = InetAddress.getByName(string);
        }
        int i = this.properties.getInt("server-port", 25565);
        LOGGER.info("Starting Minecraft server on " + (string.length() == 0 ? "*" : string) + ":" + i);
        try {
            this.connections = new ConnectionListener(this, inetAddress, i);
        }
        catch (IOException iOException) {
            LOGGER.warning("**** FAILED TO BIND TO PORT!");
            LOGGER.log(Level.WARNING, "The exception was: " + iOException.toString());
            LOGGER.warning("Perhaps a server is already running on that port?");
            return false;
        }
        if (!this.onlineMode) {
            LOGGER.warning("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            LOGGER.warning("The server will make no attempt to authenticate usernames. Beware.");
            LOGGER.warning("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            LOGGER.warning("To change this, set \"online-mode\" to \"true\" in the server.settings file.");
        }
        this.playerManager = new PlayerManager(this);
        this.entityMap = new EntityMap(this);
        String string2 = this.properties.getString("level-name", "world");
        LOGGER.info("Preparing level \"" + string2 + "\"");
        this.loadWorld(string2);
        LOGGER.info("Done! For help, type \"help\" or \"?\"");
        return true;
    }

    private void loadWorld(String saveName) {
        LOGGER.info("Preparing start region");
        this.world = new ServerWorld(this, new File("."), saveName, this.properties.getBoolean("hellworld", false) ? Dimensions.NETHER : Dimensions.OVERWORLD);
        this.world.addEventListener(new ServerWorldEventListener(this));
        this.world.difficulty = this.properties.getBoolean("spawn-monsters", true) ? 1 : 0;
        this.playerManager.setWorld(this.world);
        int i = 10;
        for (int j = -i; j <= i; ++j) {
            this.logProgress("Preparing spawn area", (j + i) * 100 / (i + i + 1));
            for (int k = -i; k <= i; ++k) {
                if (!this.running) {
                    return;
                }
                this.world.chunkCache.loadChunk((this.world.spawnPointX >> 4) + j, (this.world.spawnPointZ >> 4) + k);
            }
        }
        this.clearProgress();
    }

    private void logProgress(String progressType, int progress) {
        this.progressType = progressType;
        this.progress = progress;
        System.out.println(progressType + ": " + progress + "%");
    }

    private void clearProgress() {
        this.progressType = null;
        this.progress = 0;
    }

    private void saveWorlds() {
        LOGGER.info("Saving chunks");
        this.world.save(true, null);
    }

    private void shutdown() {
        LOGGER.info("Stopping server");
        if (this.playerManager != null) {
            this.playerManager.saveAll();
        }
        if (this.world != null) {
            this.saveWorlds();
        }
    }

    public void stop() {
        this.running = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void run() {
        try {
            if (this.init()) {
                long l = System.currentTimeMillis();
                long m = 0L;
                while (this.running) {
                    long n = System.currentTimeMillis();
                    long o = n - l;
                    if (o > 2000L) {
                        LOGGER.warning("Can't keep up! Did the system time change, or is the server overloaded?");
                        o = 2000L;
                    }
                    if (o < 0L) {
                        LOGGER.warning("Time ran backwards! Did the system time change?");
                        o = 0L;
                    }
                    m += o;
                    l = n;
                    while (m > 50L) {
                        m -= 50L;
                        this.tick();
                    }
                    Thread.sleep(1L);
                }
            } else {
                while (this.running) {
                    this.runPendingCommands();
                    try {
                        Thread.sleep(10L);
                    }
                    catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
            LOGGER.log(Level.SEVERE, "Unexpected exception", exception);
            while (this.running) {
                this.runPendingCommands();
                try {
                    Thread.sleep(10L);
                }
                catch (InterruptedException interruptedException2) {
                    interruptedException2.printStackTrace();
                }
            }
        }
        finally {
            this.shutdown();
            this.stopped = true;
            System.exit(0);
        }
    }

    private void tick() {
        int i;
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string : GIVE_COMMANDS_COOLDOWNS.keySet()) {
            int j = (Integer)GIVE_COMMANDS_COOLDOWNS.get(string);
            if (j > 0) {
                GIVE_COMMANDS_COOLDOWNS.put(string, j - 1);
                continue;
            }
            arrayList.add(string);
        }
        for (i = 0; i < arrayList.size(); ++i) {
            GIVE_COMMANDS_COOLDOWNS.remove(arrayList.get(i));
        }
        Box.resetPool();
        Vec3d.resetPool();
        ++this.ticks;
        if (this.ticks % 20 == 0) {
            this.playerManager.sendPacket(new WorldTimePacket(this.world.ticks));
        }
        this.world.tick();
        while (this.world.doLightUpdates()) {
        }
        this.world.tickEntities();
        this.connections.tick();
        this.playerManager.tick();
        this.entityMap.tick();
        for (i = 0; i < this.tickables.size(); ++i) {
            ((Tickable)this.tickables.get(i)).tick();
        }
        try {
            this.runPendingCommands();
        }
        catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unexpected exception while parsing console command", exception);
        }
    }

    public void queueCommand(String command, CommandSource source) {
        this.pendingCommands.add(new PendingCommand(command, source));
    }

    public void runPendingCommands() {
        while (this.pendingCommands.size() > 0) {
            PendingCommand pendingCommand = (PendingCommand)this.pendingCommands.remove(0);
            String string = pendingCommand.command;
            CommandSource commandSource = pendingCommand.source;
            String string2 = commandSource.getCommandSourceName();
            if (string.toLowerCase().startsWith("help") || string.toLowerCase().startsWith("?")) {
                commandSource.sendMessage("To run the server without a gui, start it like this:");
                commandSource.sendMessage("   java -Xmx1024M -Xms1024M -jar minecraft_server.jar nogui");
                commandSource.sendMessage("Console commands:");
                commandSource.sendMessage("   help  or  ?               shows this message");
                commandSource.sendMessage("   kick <player>             removes a player from the server");
                commandSource.sendMessage("   ban <player>              bans a player from the server");
                commandSource.sendMessage("   pardon <player>           pardons a banned player so that they can connect again");
                commandSource.sendMessage("   ban-ip <ip>               bans an IP address from the server");
                commandSource.sendMessage("   pardon-ip <ip>            pardons a banned IP address so that they can connect again");
                commandSource.sendMessage("   op <player>               turns a player into an op");
                commandSource.sendMessage("   deop <player>             removes op status from a player");
                commandSource.sendMessage("   tp <player1> <player2>    moves one player to the same location as another player");
                commandSource.sendMessage("   give <player> <id> [num]  gives a player a resource");
                commandSource.sendMessage("   tell <player> <message>   sends a private message to a player");
                commandSource.sendMessage("   stop                      gracefully stops the server");
                commandSource.sendMessage("   save-all                  forces a server-wide level save");
                commandSource.sendMessage("   save-off                  disables terrain saving (useful for backup scripts)");
                commandSource.sendMessage("   save-on                   re-enables terrain saving");
                commandSource.sendMessage("   list                      lists all currently connected players");
                commandSource.sendMessage("   say <message>             broadcasts a message to all players");
                continue;
            }
            if (string.toLowerCase().startsWith("list")) {
                commandSource.sendMessage("Connected players: " + this.playerManager.listNames());
                continue;
            }
            if (string.toLowerCase().startsWith("stop")) {
                this.sendCommandFeedback(string2, "Stopping the server..");
                this.running = false;
                continue;
            }
            if (string.toLowerCase().startsWith("save-all")) {
                this.sendCommandFeedback(string2, "Forcing save..");
                this.world.save(true, null);
                this.sendCommandFeedback(string2, "Save complete.");
                continue;
            }
            if (string.toLowerCase().startsWith("save-off")) {
                this.sendCommandFeedback(string2, "Disabling level saving..");
                this.world.savingDisabled = true;
                continue;
            }
            if (string.toLowerCase().startsWith("save-on")) {
                this.sendCommandFeedback(string2, "Enabling level saving..");
                this.world.savingDisabled = false;
                continue;
            }
            if (string.toLowerCase().startsWith("op ")) {
                String string3 = string.substring(string.indexOf(" ")).trim();
                this.playerManager.addOp(string3);
                this.sendCommandFeedback(string2, "Opping " + string3);
                this.playerManager.sendMessageToPlayer(string3, "\u00a7eYou are now op!");
                continue;
            }
            if (string.toLowerCase().startsWith("deop ")) {
                String string4 = string.substring(string.indexOf(" ")).trim();
                this.playerManager.removeOp(string4);
                this.playerManager.sendMessageToPlayer(string4, "\u00a7eYou are no longer op!");
                this.sendCommandFeedback(string2, "De-opping " + string4);
                continue;
            }
            if (string.toLowerCase().startsWith("ban-ip ")) {
                String string5 = string.substring(string.indexOf(" ")).trim();
                this.playerManager.banIp(string5);
                this.sendCommandFeedback(string2, "Banning ip " + string5);
                continue;
            }
            if (string.toLowerCase().startsWith("pardon-ip ")) {
                String string6 = string.substring(string.indexOf(" ")).trim();
                this.playerManager.unbanIp(string6);
                this.sendCommandFeedback(string2, "Pardoning ip " + string6);
                continue;
            }
            if (string.toLowerCase().startsWith("ban ")) {
                String string7 = string.substring(string.indexOf(" ")).trim();
                this.playerManager.ban(string7);
                this.sendCommandFeedback(string2, "Banning " + string7);
                ServerPlayerEntity serverPlayerEntity = this.playerManager.get(string7);
                if (serverPlayerEntity == null) continue;
                serverPlayerEntity.networkHandler.disconnect("Banned by admin");
                continue;
            }
            if (string.toLowerCase().startsWith("pardon ")) {
                String string8 = string.substring(string.indexOf(" ")).trim();
                this.playerManager.unban(string8);
                this.sendCommandFeedback(string2, "Pardoning " + string8);
                continue;
            }
            if (string.toLowerCase().startsWith("kick ")) {
                ServerPlayerEntity serverPlayerEntity2;
                String string9 = string.substring(string.indexOf(" ")).trim();
                Object object = null;
                for (int i = 0; i < this.playerManager.players.size(); ++i) {
                    ServerPlayerEntity serverPlayerEntity6 = (ServerPlayerEntity)this.playerManager.players.get(i);
                    if (!serverPlayerEntity6.name.equalsIgnoreCase(string9)) continue;
                    serverPlayerEntity2 = serverPlayerEntity6;
                }
                if (serverPlayerEntity2 != null) {
                    serverPlayerEntity2.networkHandler.disconnect("Kicked by admin");
                    this.sendCommandFeedback(string2, "Kicking " + serverPlayerEntity2.name);
                    continue;
                }
                commandSource.sendMessage("Can't find user " + string9 + ". No kick.");
                continue;
            }
            if (string.toLowerCase().startsWith("tp ")) {
                String[] strings = string.split(" ");
                if (strings.length == 3) {
                    ServerPlayerEntity serverPlayerEntity3 = this.playerManager.get(strings[1]);
                    ServerPlayerEntity serverPlayerEntity4 = this.playerManager.get(strings[2]);
                    if (serverPlayerEntity3 == null) {
                        commandSource.sendMessage("Can't find user " + strings[1] + ". No tp.");
                        continue;
                    }
                    if (serverPlayerEntity4 == null) {
                        commandSource.sendMessage("Can't find user " + strings[2] + ". No tp.");
                        continue;
                    }
                    serverPlayerEntity3.networkHandler.teleport(serverPlayerEntity4.x, serverPlayerEntity4.y, serverPlayerEntity4.z, serverPlayerEntity4.yaw, serverPlayerEntity4.pitch);
                    this.sendCommandFeedback(string2, "Teleporting " + strings[1] + " to " + strings[2] + ".");
                    continue;
                }
                commandSource.sendMessage("Syntax error, please provice a source and a target.");
                continue;
            }
            if (string.toLowerCase().startsWith("give ")) {
                String[] strings2 = string.split(" ");
                if (strings2.length != 3 && strings2.length != 4) {
                    return;
                }
                String string10 = strings2[1];
                ServerPlayerEntity serverPlayerEntity5 = this.playerManager.get(string10);
                if (serverPlayerEntity5 != null) {
                    try {
                        int j = Integer.parseInt(strings2[2]);
                        if (Item.BY_ID[j] != null) {
                            this.sendCommandFeedback(string2, "Giving " + serverPlayerEntity5.name + " some " + j);
                            int k = 1;
                            if (strings2.length > 3) {
                                k = this.parseInt(strings2[3], 1);
                            }
                            if (k < 1) {
                                k = 1;
                            }
                            if (k > 64) {
                                k = 64;
                            }
                            serverPlayerEntity5.dropItem(new ItemStack(j, k));
                            continue;
                        }
                        commandSource.sendMessage("There's no item with id " + j);
                    }
                    catch (NumberFormatException numberFormatException) {
                        commandSource.sendMessage("There's no item with id " + strings2[2]);
                    }
                    continue;
                }
                commandSource.sendMessage("Can't find user " + string10);
                continue;
            }
            if (string.toLowerCase().startsWith("say ")) {
                string = string.substring(string.indexOf(" ")).trim();
                LOGGER.info("[" + string2 + "] " + string);
                this.playerManager.sendPacket(new ChatMessagePacket("\u00a7d[Server] " + string));
                continue;
            }
            if (string.toLowerCase().startsWith("tell ")) {
                String[] strings3 = string.split(" ");
                if (strings3.length < 3) continue;
                string = string.substring(string.indexOf(" ")).trim();
                string = string.substring(string.indexOf(" ")).trim();
                LOGGER.info("[" + string2 + "->" + strings3[1] + "] " + string);
                string = "\u00a77" + string2 + " whispers " + string;
                LOGGER.info(string);
                if (this.playerManager.sendPacket(strings3[1], new ChatMessagePacket(string))) continue;
                commandSource.sendMessage("There's no player by that name online.");
                continue;
            }
            LOGGER.info("Unknown console command. Type \"help\" for help.");
        }
    }

    private void sendCommandFeedback(String player, String message) {
        String string = player + ": " + message;
        this.playerManager.sendMessage("\u00a77(" + string + ")");
        LOGGER.info(string);
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException numberFormatException) {
            return defaultValue;
        }
    }

    public void addTickable(Tickable tickable) {
        this.tickables.add(tickable);
    }

    public static void main(String[] args) {
        try {
            final MinecraftServer minecraftServer = new MinecraftServer();
            if (!(GraphicsEnvironment.isHeadless() || args.length > 0 && args[0].equals("nogui"))) {
                MinecraftServerGui.create(minecraftServer);
            }
            new Thread("Server thread"){

                public void run() {
                    minecraftServer.run();
                }
            }.start();
        }
        catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to start the minecraft server", exception);
        }
    }

    public File getFile(String path) {
        return new File(path);
    }

    public void sendMessage(String message) {
        LOGGER.info(message);
    }

    public String getCommandSourceName() {
        return "CONSOLE";
    }
}

