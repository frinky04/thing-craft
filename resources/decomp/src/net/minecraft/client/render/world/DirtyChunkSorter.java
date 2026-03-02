/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render.world;

import java.util.Comparator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.world.RenderChunk;
import net.minecraft.entity.mob.player.PlayerEntity;

@Environment(value=EnvType.CLIENT)
public class DirtyChunkSorter
implements Comparator {
    private PlayerEntity camera;

    public DirtyChunkSorter(PlayerEntity camera) {
        this.camera = camera;
    }

    public int compare(RenderChunk renderChunk, RenderChunk renderChunk2) {
        double e;
        boolean i = renderChunk.visible;
        boolean j = renderChunk2.visible;
        if (i && !j) {
            return 1;
        }
        if (j && !i) {
            return -1;
        }
        double d = renderChunk.squaredDistanceToCenter(this.camera);
        if (d < (e = (double)renderChunk2.squaredDistanceToCenter(this.camera))) {
            return 1;
        }
        if (d > e) {
            return -1;
        }
        return renderChunk.id < renderChunk2.id ? 1 : -1;
    }
}

