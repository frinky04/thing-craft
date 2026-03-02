/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.options;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class KeyBinding {
    public String name;
    public int keyCode;

    public KeyBinding(String name, int keyCode) {
        this.name = name;
        this.keyCode = keyCode;
    }
}

