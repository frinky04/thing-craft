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
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;
import net.minecraft.client.entity.mob.player.Input;
import net.minecraft.client.entity.particle.EntityPickupParticle;
import net.minecraft.client.gui.screen.inventory.menu.ChestScreen;
import net.minecraft.client.gui.screen.inventory.menu.CraftingTableScreen;
import net.minecraft.client.gui.screen.inventory.menu.FurnaceScreen;
import net.minecraft.client.gui.screen.inventory.menu.SignEditScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class ClientPlayerEntity
extends PlayerEntity {
    public Input input;
    private Minecraft minecraft;
    public int portalCooldown = 20;
    private boolean inPortal = false;
    public float portalTime;
    public float lastPortalTime;

    public ClientPlayerEntity(Minecraft minecraft, World world, Session session, int dimension) {
        super(world);
        this.minecraft = minecraft;
        this.dimension = dimension;
        if (session != null && session.username != null && session.username.length() > 0) {
            this.skin = "http://www.minecraft.net/skin/" + session.username + ".png";
            System.out.println("Loading texture " + this.skin);
        }
        this.name = session.username;
    }

    public void aiTick() {
        super.aiTick();
        this.sidewaysSpeed = this.input.movementSideways;
        this.forwardSpeed = this.input.movementForward;
        this.jumping = this.input.jumping;
    }

    public void mobTick() {
        this.lastPortalTime = this.portalTime;
        if (this.inPortal) {
            if (this.portalTime == 0.0f) {
                this.minecraft.soundEngine.play("portal.trigger", 1.0f, this.random.nextFloat() * 0.4f + 0.8f);
            }
            this.portalTime += 0.0125f;
            if (this.portalTime >= 1.0f) {
                this.portalTime = 1.0f;
                this.portalCooldown = 10;
                this.minecraft.soundEngine.play("portal.travel", 1.0f, this.random.nextFloat() * 0.4f + 0.8f);
                this.minecraft.changeDimension();
            }
            this.inPortal = false;
        } else {
            if (this.portalTime > 0.0f) {
                this.portalTime -= 0.05f;
            }
            if (this.portalTime < 0.0f) {
                this.portalTime = 0.0f;
            }
        }
        if (this.portalCooldown > 0) {
            --this.portalCooldown;
        }
        this.input.tick(this);
        if (this.input.sneaking && this.eyeHeightSneakOffset < 0.2f) {
            this.eyeHeightSneakOffset = 0.2f;
        }
        super.mobTick();
    }

    public void releaseAllKeys() {
        this.input.releaseAllKeys();
    }

    public void handleKeyEvent(int key, boolean state) {
        this.input.handleKeyEvent(key, state);
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
        nbt.putInt("Score", this.score);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
        this.score = nbt.getInt("Score");
    }

    public void openChestMenu(Inventory inventory) {
        this.minecraft.openScreen(new ChestScreen(this.inventory, inventory));
    }

    public void openSignEditor(SignBlockEntity sign) {
        this.minecraft.openScreen(new SignEditScreen(sign));
    }

    public void openCraftingMenu() {
        this.minecraft.openScreen(new CraftingTableScreen(this.inventory));
    }

    public void openFurnaceMenu(FurnaceBlockEntity furnace) {
        this.minecraft.openScreen(new FurnaceScreen(this.inventory, furnace));
    }

    public void pickUp(Entity item, int count) {
        this.minecraft.particleManager.add(new EntityPickupParticle(this.minecraft.world, item, this, -0.5f));
    }

    public int getDisplayArmorProtection() {
        return this.inventory.getArmorProtection();
    }

    public void interact(Entity target) {
        if (target.interact(this)) {
            return;
        }
        ItemStack itemStack = this.getItemInHand();
        if (itemStack != null && target instanceof MobEntity) {
            itemStack.interact((MobEntity)target);
            if (itemStack.size <= 0) {
                itemStack.onRemoved(this);
                this.clearItemInHand();
            }
        }
    }

    public void sendChat(String content) {
    }

    public void beforeTick() {
    }

    public boolean isSneaking() {
        return this.input.sneaking;
    }

    public void onPortalCollision() {
        if (this.portalCooldown > 0) {
            this.portalCooldown = 10;
            return;
        }
        this.inPortal = true;
    }

    public void damageTo(int health) {
        int i = this.health - health;
        if (i <= 0) {
            this.health = health;
        } else {
            this.prevDamageTaken = i;
            this.prevHealth = this.health;
            this.invulnerableTimer = this.invulnerableTicks;
            this.applyDamage(i);
            this.damagedTime = 10;
            this.damagedTimer = 10;
        }
    }

    public void respawn() {
        this.minecraft.respawnPlayer();
    }
}

