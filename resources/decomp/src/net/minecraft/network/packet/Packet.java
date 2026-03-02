/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.PacketHandler;
import net.minecraft.network.packet.AddEntityPacket;
import net.minecraft.network.packet.AddItemPacket;
import net.minecraft.network.packet.AddMobPacket;
import net.minecraft.network.packet.AddPlayerPacket;
import net.minecraft.network.packet.BlockEntityUpdatePacket;
import net.minecraft.network.packet.BlockUpdatePacket;
import net.minecraft.network.packet.BlocksUpdatePacket;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.DisconnectPacket;
import net.minecraft.network.packet.EntityAnimationPacket;
import net.minecraft.network.packet.EntityEventPacket;
import net.minecraft.network.packet.EntityMovePacket;
import net.minecraft.network.packet.EntityPickupPacket;
import net.minecraft.network.packet.EntityTeleportPacket;
import net.minecraft.network.packet.EntityVelocityPacket;
import net.minecraft.network.packet.ExplosionPacket;
import net.minecraft.network.packet.HandshakePacket;
import net.minecraft.network.packet.ItemPickupPacket;
import net.minecraft.network.packet.LoadWorldChunkPacket;
import net.minecraft.network.packet.LoginPacket;
import net.minecraft.network.packet.PingHostPacket;
import net.minecraft.network.packet.PlayerHandActionPacket;
import net.minecraft.network.packet.PlayerHealthPacket;
import net.minecraft.network.packet.PlayerHeldItemPacket;
import net.minecraft.network.packet.PlayerInteractEntityPacket;
import net.minecraft.network.packet.PlayerInventoryPacket;
import net.minecraft.network.packet.PlayerMovePacket;
import net.minecraft.network.packet.PlayerRespawnPacket;
import net.minecraft.network.packet.PlayerUsePacket;
import net.minecraft.network.packet.RemoveEntityPacket;
import net.minecraft.network.packet.RideEntityPacket;
import net.minecraft.network.packet.SpawnPointPacket;
import net.minecraft.network.packet.WorldChunkPacket;
import net.minecraft.network.packet.WorldTimePacket;

public abstract class Packet {
    private static Map ID_TO_TYPE = new HashMap();
    private static Map TYPE_TO_ID = new HashMap();
    public boolean shouldDelay = false;

    static void register(int id, Class type) {
        if (ID_TO_TYPE.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate packet id:" + id);
        }
        if (TYPE_TO_ID.containsKey(type)) {
            throw new IllegalArgumentException("Duplicate packet class:" + type);
        }
        ID_TO_TYPE.put(id, type);
        TYPE_TO_ID.put(type, id);
    }

    public static Packet create(int id) {
        try {
            Class class_ = (Class)ID_TO_TYPE.get(id);
            if (class_ == null) {
                return null;
            }
            return (Packet)class_.newInstance();
        }
        catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("Skipping packet with id " + id);
            return null;
        }
    }

    public final int getId() {
        return (Integer)TYPE_TO_ID.get(this.getClass());
    }

    public static Packet deserialize(DataInputStream input) {
        int i = input.read();
        if (i == -1) {
            return null;
        }
        Packet packet = Packet.create(i);
        if (packet == null) {
            throw new IOException("Bad packet id " + i);
        }
        packet.read(input);
        return packet;
    }

    public static void serialize(Packet packet, DataOutputStream output) {
        output.write(packet.getId());
        packet.write(output);
    }

    public abstract void read(DataInputStream var1);

    public abstract void write(DataOutputStream var1);

    public abstract void handle(PacketHandler var1);

    public abstract int getSize();

    static {
        Packet.register(0, PingHostPacket.class);
        Packet.register(1, LoginPacket.class);
        Packet.register(2, HandshakePacket.class);
        Packet.register(3, ChatMessagePacket.class);
        Packet.register(4, WorldTimePacket.class);
        Packet.register(5, PlayerInventoryPacket.class);
        Packet.register(6, SpawnPointPacket.class);
        Packet.register(7, PlayerInteractEntityPacket.class);
        Packet.register(8, PlayerHealthPacket.class);
        Packet.register(9, PlayerRespawnPacket.class);
        Packet.register(10, PlayerMovePacket.class);
        Packet.register(11, PlayerMovePacket.Position.class);
        Packet.register(12, PlayerMovePacket.Angles.class);
        Packet.register(13, PlayerMovePacket.PositionAndAngles.class);
        Packet.register(14, PlayerHandActionPacket.class);
        Packet.register(15, PlayerUsePacket.class);
        Packet.register(16, PlayerHeldItemPacket.class);
        Packet.register(17, ItemPickupPacket.class);
        Packet.register(18, EntityAnimationPacket.class);
        Packet.register(20, AddPlayerPacket.class);
        Packet.register(21, AddItemPacket.class);
        Packet.register(22, EntityPickupPacket.class);
        Packet.register(23, AddEntityPacket.class);
        Packet.register(24, AddMobPacket.class);
        Packet.register(28, EntityVelocityPacket.class);
        Packet.register(29, RemoveEntityPacket.class);
        Packet.register(30, EntityMovePacket.class);
        Packet.register(31, EntityMovePacket.Position.class);
        Packet.register(32, EntityMovePacket.Angles.class);
        Packet.register(33, EntityMovePacket.PositionAndAngles.class);
        Packet.register(34, EntityTeleportPacket.class);
        Packet.register(38, EntityEventPacket.class);
        Packet.register(39, RideEntityPacket.class);
        Packet.register(50, LoadWorldChunkPacket.class);
        Packet.register(51, WorldChunkPacket.class);
        Packet.register(52, BlocksUpdatePacket.class);
        Packet.register(53, BlockUpdatePacket.class);
        Packet.register(59, BlockEntityUpdatePacket.class);
        Packet.register(60, ExplosionPacket.class);
        Packet.register(255, DisconnectPacket.class);
    }
}

