/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class CreativeInteractionManager
extends ClientPlayerInteractionManager {
    public CreativeInteractionManager(Minecraft minecraft) {
        super(minecraft);
        this.creative = true;
    }

    public void adjustPlayer(PlayerEntity player) {
        for (int i = 0; i < 9; ++i) {
            if (player.inventory.items[i] == null) {
                this.minecraft.player.inventory.items[i] = new ItemStack(((Block)Session.CREATIVE_INVENTORY.get((int)i)).id);
                continue;
            }
            this.minecraft.player.inventory.items[i].size = 1;
        }
    }

    public boolean hasStatusBars() {
        return false;
    }

    public void initWorld(World world) {
        super.initWorld(world);
    }

    public void tick() {
    }
}

