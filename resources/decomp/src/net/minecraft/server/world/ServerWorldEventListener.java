/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldEventListener;

@Environment(value=EnvType.SERVER)
public class ServerWorldEventListener
implements WorldEventListener {
    private MinecraftServer server;

    public ServerWorldEventListener(MinecraftServer server) {
        this.server = server;
    }

    public void addParticle(String type, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void notifyEntityAdded(Entity entity) {
        this.server.entityMap.onEntityAdded(entity);
    }

    public void notifyEntityRemoved(Entity entity) {
        this.server.entityMap.onEntityRemoved(entity);
    }

    public void playSound(String name, double x, double y, double z, float volume, float pitch) {
    }

    public void notifyRegionChanged(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    public void notifyAmbientDarknessChanged() {
    }

    public void notifyBlockChanged(int x, int y, int z) {
        this.server.playerManager.onBlockChanged(x, y, z);
    }

    public void playRecordMusic(String record, int x, int y, int z) {
    }

    public void notifyBlockEntityChanged(int x, int y, int z, BlockEntity blockEntity) {
        this.server.playerManager.onBlockEntityChanged(x, y, z, blockEntity);
    }
}

