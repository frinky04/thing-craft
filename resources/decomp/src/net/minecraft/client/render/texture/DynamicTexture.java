/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL11
 */
package net.minecraft.client.render.texture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.texture.TextureManager;
import org.lwjgl.opengl.GL11;

@Environment(value=EnvType.CLIENT)
public class DynamicTexture {
    public byte[] pixels = new byte[1024];
    public int sprite;
    public boolean anaglyph = false;
    public int copyTo = 0;
    public int replicate = 1;
    public int atlas = 0;

    public DynamicTexture(int sprite) {
        this.sprite = sprite;
    }

    public void tick() {
    }

    public void bind(TextureManager textureManager) {
        if (this.atlas == 0) {
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)textureManager.load("/terrain.png"));
        } else if (this.atlas == 1) {
            GL11.glBindTexture((int)GL11.GL_TEXTURE_2D, (int)textureManager.load("/gui/items.png"));
        }
    }
}

