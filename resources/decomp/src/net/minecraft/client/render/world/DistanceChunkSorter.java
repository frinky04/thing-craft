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
import net.minecraft.entity.Entity;

@Environment(value=EnvType.CLIENT)
public class DistanceChunkSorter
implements Comparator {
    private Entity camera;

    public DistanceChunkSorter(Entity camera) {
        this.camera = camera;
    }

    public int compare(RenderChunk renderChunk, RenderChunk renderChunk2) {
        return renderChunk.squaredDistanceToCenter(this.camera) < renderChunk2.squaredDistanceToCenter(this.camera) ? -1 : 1;
    }
}

