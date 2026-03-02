/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.world;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.animal.AnimalEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.network.packet.EntityEventPacket;
import net.minecraft.network.packet.ExplosionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.chunk.ServerChunkCache;
import net.minecraft.util.Int2ObjectHashMap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.explosion.Explosion;

@Environment(value=EnvType.SERVER)
public class ServerWorld
extends World {
    public ServerChunkCache chunkCache;
    public boolean bypassSpawnProtection = false;
    public boolean savingDisabled;
    private MinecraftServer server;
    private Int2ObjectHashMap entitiesById = new Int2ObjectHashMap();

    public ServerWorld(MinecraftServer server, File dir, String saveName, int dimension) {
        super(dir, saveName, new Random().nextLong(), Dimension.fromId(dimension));
        this.server = server;
    }

    public void tick() {
        super.tick();
    }

    public void tickEntity(Entity entity, boolean requireLoaded) {
        if (!this.server.spawnAnimals && entity instanceof AnimalEntity) {
            entity.remove();
        }
        if (entity.rider == null || !(entity.rider instanceof PlayerEntity)) {
            super.tickEntity(entity, requireLoaded);
        }
    }

    public void tickVehicle(Entity vehicle, boolean requireLoaded) {
        super.tickEntity(vehicle, requireLoaded);
    }

    protected ChunkSource createChunkCache(File dir) {
        this.chunkCache = new ServerChunkCache(this, this.dimension.createChunkStorage(dir), this.dimension.createChunkGenerator());
        return this.chunkCache;
    }

    public List getBlockEntities(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        ArrayList<BlockEntity> arrayList = new ArrayList<BlockEntity>();
        for (int i = 0; i < this.blockEntities.size(); ++i) {
            BlockEntity blockEntity = (BlockEntity)this.blockEntities.get(i);
            if (blockEntity.x < minX || blockEntity.y < minY || blockEntity.z < minZ || blockEntity.x >= maxX || blockEntity.y >= maxY || blockEntity.z >= maxZ) continue;
            arrayList.add(blockEntity);
        }
        return arrayList;
    }

    public boolean canModify(PlayerEntity player, int x, int y, int z) {
        int j;
        int i = (int)MathHelper.abs(x - this.spawnPointX);
        if (i > (j = (int)MathHelper.abs(z - this.spawnPointZ))) {
            j = i;
        }
        return j > 16 || this.server.playerManager.isOp(player.name);
    }

    protected void notifyEntityAdded(Entity entity) {
        super.notifyEntityAdded(entity);
        this.entitiesById.put(entity.networkId, entity);
    }

    protected void notifyEntityRemoved(Entity entity) {
        super.notifyEntityRemoved(entity);
        this.entitiesById.remove(entity.networkId);
    }

    public Entity getEntity(int networkId) {
        return (Entity)this.entitiesById.get(networkId);
    }

    public void doEntityEvent(Entity entity, byte event) {
        EntityEventPacket entityEventPacket = new EntityEventPacket(entity.networkId, event);
        this.server.entityMap.sendPacketToAll(entity, entityEventPacket);
    }

    public Explosion explode(Entity source, double x, double y, double z, float power, boolean createFire) {
        Explosion explosion = super.explode(source, x, y, z, power, createFire);
        this.server.playerManager.sendPacket(x, y, z, 64.0, new ExplosionPacket(x, y, z, power, explosion.damagedBlocks));
        return explosion;
    }
}

