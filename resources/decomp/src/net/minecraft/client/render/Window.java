/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class Window {
    private int width;
    private int height;
    public int scale;

    public Window(int width, int height) {
        this.width = width;
        this.height = height;
        this.scale = 1;
        while (this.width / (this.scale + 1) >= 320 && this.height / (this.scale + 1) >= 240) {
            ++this.scale;
        }
        this.width /= this.scale;
        this.height /= this.scale;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}

