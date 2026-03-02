/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity.mob.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import net.minecraft.client.network.handler.ClientNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.network.packet.AddItemPacket;
import net.minecraft.network.packet.ChatMessagePacket;
import net.minecraft.network.packet.EntityAnimationPacket;
import net.minecraft.network.packet.PlayerInventoryPacket;
import net.minecraft.network.packet.PlayerMovePacket;
import net.minecraft.network.packet.PlayerRespawnPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class LocalClientPlayerEntity
extends ClientPlayerEntity {
    private ClientNetworkHandler networkHandler;
    private int ticksSinceSendInventory = 0;
    private double sentX;
    private double sentMinY;
    private double sentY;
    private double sentZ;
    private float sentYaw;
    private float sentPitch;
    private PlayerInventory sentInventory = new PlayerInventory(null);
    private boolean sentOnGround = false;
    private boolean sentSneaking = false;
    private int ticksSinceSendOnGround = 0;

    public LocalClientPlayerEntity(Minecraft minecraft, World world, Session session, ClientNetworkHandler networkHandler) {
        super(minecraft, world, session, 0);
        this.networkHandler = networkHandler;
    }

    public boolean takeDamage(Entity source, int amount) {
        return false;
    }

    public void heal(int amount) {
    }

    public void tick() {
        if (!this.world.isChunkLoaded(MathHelper.floor(this.x), 64, MathHelper.floor(this.z))) {
            return;
        }
        super.tick();
        this.sendMovement();
    }

    public void beforeTick() {
    }

    public void sendMovement() {
        boolean l;
        boolean i;
        if (this.ticksSinceSendInventory++ == 20) {
            this.sendInventory();
            this.ticksSinceSendInventory = 0;
        }
        if ((i = this.isSneaking()) != this.sentSneaking) {
            if (i) {
                this.networkHandler.sendPacket(new EntityAnimationPacket(this, 104));
            } else {
                this.networkHandler.sendPacket(new EntityAnimationPacket(this, 105));
            }
            this.sentSneaking = i;
        }
        double d = this.x - this.sentX;
        double e = this.shape.minY - this.sentMinY;
        double f = this.y - this.sentY;
        double g = this.z - this.sentZ;
        double h = this.yaw - this.sentYaw;
        double j = this.pitch - this.sentPitch;
        boolean k = e != 0.0 || f != 0.0 || d != 0.0 || g != 0.0;
        boolean bl = l = h != 0.0 || j != 0.0;
        if (this.vehicle != null) {
            if (l) {
                this.networkHandler.sendPacket(new PlayerMovePacket.Position(this.velocityX, -999.0, -999.0, this.velocityZ, this.onGround));
            } else {
                this.networkHandler.sendPacket(new PlayerMovePacket.PositionAndAngles(this.velocityX, -999.0, -999.0, this.velocityZ, this.yaw, this.pitch, this.onGround));
            }
            k = false;
        } else if (k && l) {
            this.networkHandler.sendPacket(new PlayerMovePacket.PositionAndAngles(this.x, this.shape.minY, this.y, this.z, this.yaw, this.pitch, this.onGround));
            this.ticksSinceSendOnGround = 0;
        } else if (k) {
            this.networkHandler.sendPacket(new PlayerMovePacket.Position(this.x, this.shape.minY, this.y, this.z, this.onGround));
            this.ticksSinceSendOnGround = 0;
        } else if (l) {
            this.networkHandler.sendPacket(new PlayerMovePacket.Angles(this.yaw, this.pitch, this.onGround));
            this.ticksSinceSendOnGround = 0;
        } else {
            this.networkHandler.sendPacket(new PlayerMovePacket(this.onGround));
            this.ticksSinceSendOnGround = this.sentOnGround != this.onGround || this.ticksSinceSendOnGround > 20 ? 0 : ++this.ticksSinceSendOnGround;
        }
        this.sentOnGround = this.onGround;
        if (k) {
            this.sentX = this.x;
            this.sentMinY = this.shape.minY;
            this.sentY = this.y;
            this.sentZ = this.z;
        }
        if (l) {
            this.sentYaw = this.yaw;
            this.sentPitch = this.pitch;
        }
    }

    private void sendInventory() {
        if (!this.inventory.contentEquals(this.sentInventory)) {
            this.networkHandler.sendPacket(new PlayerInventoryPacket(-1, this.inventory.items));
            this.networkHandler.sendPacket(new PlayerInventoryPacket(-2, this.inventory.crafting));
            this.networkHandler.sendPacket(new PlayerInventoryPacket(-3, this.inventory.armor));
            this.sentInventory = this.inventory.copy();
        }
    }

    protected void spawnItem(ItemEntity item) {
        AddItemPacket addItemPacket = new AddItemPacket(item);
        this.networkHandler.sendPacket(addItemPacket);
        item.x = (double)addItemPacket.x / 32.0;
        item.y = (double)addItemPacket.y / 32.0;
        item.z = (double)addItemPacket.z / 32.0;
        item.velocityX = (double)addItemPacket.velocityX / 128.0;
        item.velocityY = (double)addItemPacket.velocityY / 128.0;
        item.velocityZ = (double)addItemPacket.velocityZ / 128.0;
    }

    public void sendChat(String content) {
        this.networkHandler.sendPacket(new ChatMessagePacket(content));
    }

    public void swingArm() {
        super.swingArm();
        this.networkHandler.sendPacket(new EntityAnimationPacket(this, 1));
    }

    public void respawn() {
        this.sendInventory();
        this.networkHandler.sendPacket(new PlayerRespawnPacket());
    }

    protected void applyDamage(int amount) {
        this.health -= amount;
    }
}

