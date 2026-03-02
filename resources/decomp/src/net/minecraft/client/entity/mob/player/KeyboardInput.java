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
import net.minecraft.client.entity.mob.player.Input;
import net.minecraft.client.options.GameOptions;
import net.minecraft.entity.mob.player.PlayerEntity;

@Environment(value=EnvType.CLIENT)
public class KeyboardInput
extends Input {
    private boolean[] inputKeys = new boolean[10];
    private GameOptions options;

    public KeyboardInput(GameOptions options) {
        this.options = options;
    }

    public void handleKeyEvent(int key, boolean state) {
        int i = -1;
        if (key == this.options.forwardKey.keyCode) {
            i = 0;
        }
        if (key == this.options.backKey.keyCode) {
            i = 1;
        }
        if (key == this.options.leftKey.keyCode) {
            i = 2;
        }
        if (key == this.options.rightKey.keyCode) {
            i = 3;
        }
        if (key == this.options.jumpKey.keyCode) {
            i = 4;
        }
        if (key == this.options.sneakKey.keyCode) {
            i = 5;
        }
        if (i >= 0) {
            this.inputKeys[i] = state;
        }
    }

    public void releaseAllKeys() {
        for (int i = 0; i < 10; ++i) {
            this.inputKeys[i] = false;
        }
    }

    public void tick(PlayerEntity player) {
        this.movementSideways = 0.0f;
        this.movementForward = 0.0f;
        if (this.inputKeys[0]) {
            this.movementForward += 1.0f;
        }
        if (this.inputKeys[1]) {
            this.movementForward -= 1.0f;
        }
        if (this.inputKeys[2]) {
            this.movementSideways += 1.0f;
        }
        if (this.inputKeys[3]) {
            this.movementSideways -= 1.0f;
        }
        this.jumping = this.inputKeys[4];
        this.sneaking = this.inputKeys[5];
        if (this.sneaking) {
            this.movementSideways = (float)((double)this.movementSideways * 0.3);
            this.movementForward = (float)((double)this.movementForward * 0.3);
        }
    }
}

