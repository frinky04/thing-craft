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
import net.minecraft.entity.mob.player.PlayerEntity;

@Environment(value=EnvType.CLIENT)
public class Input {
    public float movementSideways = 0.0f;
    public float movementForward = 0.0f;
    public boolean wasJumping = false;
    public boolean jumping = false;
    public boolean sneaking = false;

    public void tick(PlayerEntity player) {
    }

    public void releaseAllKeys() {
    }

    public void handleKeyEvent(int key, boolean state) {
    }
}

