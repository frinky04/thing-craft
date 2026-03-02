/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.network.handler;

import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.source.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.AddItemPacket;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.BlockUpdatePacket;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.DisconnectPacket;
import net.minecraft.network.packet.EntityAnimationPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PingHostPacket;
import net.minecraft.network.packet.PlayerHandActionPacket;
import net.minecraft.network.packet.PlayerHeldItemPacket;
import net.minecraft.network.packet.PlayerInteractEntityPacket;
import net.minecraft.network.packet.PlayerInventoryPacket;
import net.minecraft.network.packet.PlayerMovePacket;
import net.minecraft.network.packet.PlayerRespawnPacket;
import net.minecraft.network.packet.PlayerUsePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.SERVER)
public class ServerPlayNetworkHandler
extends PacketHandler
implements CommandSource {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    public Connection connection;
    public boolean disconnected = false;
    private MinecraftServer server;
    private ServerPlayerEntity player;
    private int ticks = 0;
    private double teleportTargetX;
    private double teleportTargetY;
    private double teleportTargetZ;
    private boolean teleported = true;
    private ItemStack f_33399967 = null;

    public ServerPlayNetworkHandler(MinecraftServer server, Connection connection, ServerPlayerEntity player) {
        this.server = server;
        this.connection = connection;
        connection.setListener(this);
        this.player = player;
        player.networkHandler = this;
    }

    public void tick() {
        this.connection.tick();
        if (this.ticks++ % 20 == 0) {
            this.connection.send(new PingHostPacket());
        }
    }

    public void disconnect(String reason) {
        this.connection.send(new DisconnectPacket(reason));
        this.connection.close();
        this.server.playerManager.sendPacket(new ChatMessagePacket("\u00a7e" + this.player.name + " left the game."));
        this.server.playerManager.remove(this.player);
        this.disconnected = true;
    }

    public void handlePlayerMove(PlayerMovePacket packet) {
        if (!this.teleported) {
            double d = packet.minY - this.teleportTargetY;
            if (packet.x == this.teleportTargetX && d * d < 0.01 && packet.z == this.teleportTargetZ) {
                this.teleported = true;
            }
        }
        if (this.teleported) {
            boolean z;
            if (this.player.vehicle != null) {
                float f = this.player.yaw;
                float g = this.player.pitch;
                this.player.vehicle.updateRiderPositon();
                double h = this.player.x;
                double j = this.player.y;
                double l = this.player.z;
                double n = 0.0;
                double q = 0.0;
                if (packet.hasAngles) {
                    f = packet.yaw;
                    g = packet.pitch;
                }
                if (packet.hasPos && packet.minY == -999.0 && packet.y == -999.0) {
                    n = packet.x;
                    q = packet.z;
                }
                this.player.onGround = packet.onGround;
                this.player.tickPlayer();
                this.player.move(n, 0.0, q);
                this.player.updatePositionAndAngles(h, j, l, f, g);
                this.player.velocityX = n;
                this.player.velocityZ = q;
                if (this.player.vehicle != null) {
                    this.server.world.tickVehicle(this.player.vehicle, true);
                }
                if (this.player.vehicle != null) {
                    this.player.vehicle.updateRiderPositon();
                }
                this.server.playerManager.move(this.player);
                this.teleportTargetX = this.player.x;
                this.teleportTargetY = this.player.y;
                this.teleportTargetZ = this.player.z;
                this.server.world.tickEntity(this.player);
                return;
            }
            double e = this.player.y;
            this.teleportTargetX = this.player.x;
            this.teleportTargetY = this.player.y;
            this.teleportTargetZ = this.player.z;
            double i = this.player.x;
            double k = this.player.y;
            double m = this.player.z;
            float o = this.player.yaw;
            float p = this.player.pitch;
            if (packet.hasPos && packet.minY == -999.0 && packet.y == -999.0) {
                packet.hasPos = false;
            }
            if (packet.hasPos) {
                i = packet.x;
                k = packet.minY;
                m = packet.z;
                double r = packet.y - packet.minY;
                if (r > 1.65 || r < 0.1) {
                    this.disconnect("Illegal stance");
                    LOGGER.warning(this.player.name + " had an illegal stance: " + r);
                }
                this.player.lastReceivedY = packet.y;
            }
            if (packet.hasAngles) {
                o = packet.yaw;
                p = packet.pitch;
            }
            this.player.tickPlayer();
            this.player.eyeHeightSneakOffset = 0.0f;
            this.player.updatePositionAndAngles(this.teleportTargetX, this.teleportTargetY, this.teleportTargetZ, o, p);
            double s = i - this.player.x;
            double t = k - this.player.y;
            double u = m - this.player.z;
            float v = 0.0625f;
            boolean w = this.server.world.getCollisions(this.player, this.player.shape.copy().contract(v, v, v)).size() == 0;
            this.player.move(s, t, u);
            s = i - this.player.x;
            t = k - this.player.y;
            if (t > -0.5 || t < 0.5) {
                t = 0.0;
            }
            u = m - this.player.z;
            double x = s * s + t * t + u * u;
            boolean y = false;
            if (x > 0.0625) {
                y = true;
                LOGGER.warning(this.player.name + " moved wrongly!");
                System.out.println("Got position " + i + ", " + k + ", " + m);
                System.out.println("Expected " + this.player.x + ", " + this.player.y + ", " + this.player.z);
            }
            this.player.updatePositionAndAngles(i, k, m, o, p);
            boolean bl = z = this.server.world.getCollisions(this.player, this.player.shape.copy().contract(v, v, v)).size() == 0;
            if (w && (y || !z)) {
                this.teleport(this.teleportTargetX, this.teleportTargetY, this.teleportTargetZ, o, p);
                return;
            }
            this.player.onGround = packet.onGround;
            this.server.playerManager.move(this.player);
            this.player.handleFall(this.player.y - e, packet.onGround);
        }
    }

    public void teleport(double x, double y, double z, float yaw, float pitch) {
        this.teleported = false;
        this.teleportTargetX = x;
        this.teleportTargetY = y;
        this.teleportTargetZ = z;
        this.player.updatePositionAndAngles(x, y, z, yaw, pitch);
        this.player.networkHandler.sendPacket(new PlayerMovePacket.PositionAndAngles(x, y + (double)1.62f, y, z, yaw, pitch, false));
    }

    public void handlePlayerHandAction(PlayerHandActionPacket packet) {
        double r;
        double h;
        double f;
        double t;
        int p;
        this.player.inventory.items[this.player.inventory.selectedSlot] = this.f_33399967;
        boolean i = this.server.world.bypassSpawnProtection = this.server.playerManager.isOp(this.player.name);
        boolean j = false;
        if (packet.action == 0) {
            j = true;
        }
        if (packet.action == 1) {
            j = true;
        }
        int k = packet.x;
        int l = packet.y;
        int m = packet.z;
        if (j) {
            double d = this.player.x - ((double)k + 0.5);
            double e = this.player.y - ((double)l + 0.5);
            double g = this.player.z - ((double)m + 0.5);
            double q = d * d + e * e + g * g;
            if (q > 36.0) {
                return;
            }
            double s = this.player.y;
            this.player.y = this.player.lastReceivedY;
            this.player.y = s;
        }
        int n = packet.face;
        int o = (int)MathHelper.abs(k - this.server.world.spawnPointX);
        if (o > (p = (int)MathHelper.abs(m - this.server.world.spawnPointZ))) {
            p = o;
        }
        if (packet.action == 0) {
            if (p > 16 || i) {
                this.player.interactionManager.mineBlockInstantly(k, l, m);
            }
        } else if (packet.action == 2) {
            this.player.interactionManager.stopMiningBlock();
        } else if (packet.action == 1) {
            if (p > 16 || i) {
                this.player.interactionManager.finishMiningBlock(k, l, m, n);
            }
        } else if (packet.action == 3 && (t = (f = this.player.x - ((double)k + 0.5)) * f + (h = this.player.y - ((double)l + 0.5)) * h + (r = this.player.z - ((double)m + 0.5)) * r) < 256.0) {
            this.player.networkHandler.sendPacket(new BlockUpdatePacket(k, l, m, this.server.world));
        }
        this.server.world.bypassSpawnProtection = false;
    }

    public void handlePlayerUse(PlayerUsePacket packet) {
        boolean i = this.server.world.bypassSpawnProtection = this.server.playerManager.isOp(this.player.name);
        if (packet.item == 255) {
            ItemStack itemStack = packet.x >= 0 ? new ItemStack(packet.x) : null;
            this.player.interactionManager.useItem(this.player, this.server.world, itemStack);
        } else {
            int o;
            int j = packet.y;
            int k = packet.z;
            int l = packet.face;
            int m = packet.item;
            int n = (int)MathHelper.abs(j - this.server.world.spawnPointX);
            if (n > (o = (int)MathHelper.abs(l - this.server.world.spawnPointZ))) {
                o = n;
            }
            if (o > 16 || i) {
                ItemStack itemStack2 = packet.x >= 0 ? new ItemStack(packet.x) : null;
                this.player.interactionManager.useBlock(this.player, this.server.world, itemStack2, j, k, l, m);
            }
            this.player.networkHandler.sendPacket(new BlockUpdatePacket(j, k, l, this.server.world));
            if (m == 0) {
                --k;
            }
            if (m == 1) {
                ++k;
            }
            if (m == 2) {
                --l;
            }
            if (m == 3) {
                ++l;
            }
            if (m == 4) {
                --j;
            }
            if (m == 5) {
                ++j;
            }
            this.player.networkHandler.sendPacket(new BlockUpdatePacket(j, k, l, this.server.world));
        }
        this.server.world.bypassSpawnProtection = false;
    }

    public void onDisconnect(String reason) {
        LOGGER.info(this.player.name + " lost connection: " + reason);
        this.server.playerManager.sendPacket(new ChatMessagePacket("\u00a7e" + this.player.name + " left the game."));
        this.server.playerManager.remove(this.player);
        this.disconnected = true;
    }

    public void handleUnknownPacket(Packet packet) {
        LOGGER.warning(this.getClass() + " wasn't prepared to deal with a " + packet.getClass());
        this.disconnect("Protocol error, unexpected packet");
    }

    public void sendPacket(Packet packet) {
        this.connection.send(packet);
    }

    public void handlePlayerHeldItem(PlayerHeldItemPacket packet) {
        int i = packet.item;
        this.player.inventory.selectedSlot = this.player.inventory.items.length - 1;
        this.f_33399967 = i == 0 ? null : new ItemStack(i);
        this.player.inventory.items[this.player.inventory.selectedSlot] = this.f_33399967;
        this.server.entityMap.sendPacket(this.player, new PlayerHeldItemPacket(this.player.networkId, i));
    }

    public void handleAddItem(AddItemPacket packet) {
        double d = (double)packet.x / 32.0;
        double e = (double)packet.y / 32.0;
        double f = (double)packet.z / 32.0;
        ItemEntity itemEntity = new ItemEntity(this.server.world, d, e, f, new ItemStack(packet.item, packet.stackSize));
        itemEntity.velocityX = (double)packet.velocityX / 128.0;
        itemEntity.velocityY = (double)packet.velocityY / 128.0;
        itemEntity.velocityZ = (double)packet.velocityZ / 128.0;
        itemEntity.pickUpDelay = 10;
        this.server.world.addEntity(itemEntity);
    }

    public void handleChatMessage(ChatMessagePacket packet) {
        String string = packet.message;
        if (string.length() > 100) {
            this.disconnect("Chat message too long");
            return;
        }
        string = string.trim();
        for (int i = 0; i < string.length(); ++i) {
            if (" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb".indexOf(string.charAt(i)) >= 0) continue;
            this.disconnect("Illegal characters in chat");
            return;
        }
        if (string.startsWith("/")) {
            this.runCommand(string);
        } else {
            string = "<" + this.player.name + "> " + string;
            LOGGER.info(string);
            this.server.playerManager.sendPacket(new ChatMessagePacket(string));
        }
    }

    private void runCommand(String command) {
        if (command.toLowerCase().startsWith("/me ")) {
            command = "* " + this.player.name + " " + command.substring(command.indexOf(" ")).trim();
            LOGGER.info(command);
            this.server.playerManager.sendPacket(new ChatMessagePacket(command));
        } else if (command.toLowerCase().startsWith("/kill")) {
            this.player.takeDamage(null, 1000);
        } else if (command.toLowerCase().startsWith("/tell ")) {
            String[] strings = command.split(" ");
            if (strings.length >= 3) {
                command = command.substring(command.indexOf(" ")).trim();
                command = command.substring(command.indexOf(" ")).trim();
                command = "\u00a77" + this.player.name + " whispers " + command;
                LOGGER.info(command + " to " + strings[1]);
                if (!this.server.playerManager.sendPacket(strings[1], new ChatMessagePacket(command))) {
                    this.sendPacket(new ChatMessagePacket("\u00a7cThere's no player by that name online."));
                }
            }
        } else if (this.server.playerManager.isOp(this.player.name)) {
            String string = command.substring(1);
            LOGGER.info(this.player.name + " issued server command: " + string);
            this.server.queueCommand(string, this);
        } else {
            String string2 = command.substring(1);
            LOGGER.info(this.player.name + " tried command: " + string2);
        }
    }

    public void handleEntityAnimation(EntityAnimationPacket packet) {
        if (packet.action == 1) {
            this.player.swingArm();
        } else if (packet.action == 104) {
            this.player.sneaking = true;
        } else if (packet.action == 105) {
            this.player.sneaking = false;
        }
    }

    public void handleDisconnect(DisconnectPacket packet) {
        this.connection.close("Quitting");
    }

    public int getBlockDataSendQueueSize() {
        return this.connection.getDelayedSendQueueSize();
    }

    public void sendMessage(String message) {
        this.sendPacket(new ChatMessagePacket("\u00a77" + message));
    }

    public String getCommandSourceName() {
        return this.player.name;
    }

    public void handlePlayerInventory(PlayerInventoryPacket packet) {
        if (packet.type == -1) {
            this.player.inventory.items = packet.items;
        }
        if (packet.type == -2) {
            this.player.inventory.crafting = packet.items;
        }
        if (packet.type == -3) {
            this.player.inventory.armor = packet.items;
        }
    }

    public void sendInventory() {
        this.connection.send(new PlayerInventoryPacket(-1, this.player.inventory.items));
        this.connection.send(new PlayerInventoryPacket(-2, this.player.inventory.crafting));
        this.connection.send(new PlayerInventoryPacket(-3, this.player.inventory.armor));
    }

    public void handleBlockEntityUpdate(BlockEntityUpdatePacket packet) {
        if (packet.nbt.getInt("x") != packet.x) {
            return;
        }
        if (packet.nbt.getInt("y") != packet.y) {
            return;
        }
        if (packet.nbt.getInt("z") != packet.z) {
            return;
        }
        BlockEntity blockEntity = this.server.world.getBlockEntity(packet.x, packet.y, packet.z);
        if (blockEntity != null) {
            try {
                blockEntity.readNbt(packet.nbt);
            }
            catch (Exception exception) {
                // empty catch block
            }
            blockEntity.markDirty();
        }
    }

    public void handleInteractEntity(PlayerInteractEntityPacket packet) {
        Entity entity = this.server.world.getEntity(packet.targetId);
        this.player.inventory.items[this.player.inventory.selectedSlot] = this.f_33399967;
        if (entity != null && this.player.canSee(entity)) {
            if (packet.action == 0) {
                this.player.interact(entity);
            } else if (packet.action == 1) {
                this.player.attack(entity);
            }
        }
    }

    public void handlePlayerRespawn(PlayerRespawnPacket packet) {
        if (this.player.health > 0) {
            return;
        }
        this.player = this.server.playerManager.respawn(this.player);
    }
}

