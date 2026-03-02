/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathFinder;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.LightType;
import net.minecraft.world.LightUpdate;
import net.minecraft.world.NaturalSpawner;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.WorldEventListener;
import net.minecraft.world.WorldRegion;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.NetherDimension;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.storage.exception.SessionLockException;

public class World
implements WorldView {
    public boolean doTicksImmediately = false;
    private List lightUpdates = new ArrayList();
    public List entities = new ArrayList();
    private List entitiesToRemove = new ArrayList();
    private TreeSet scheduledTicksInOrder = new TreeSet();
    private Set scheduledTicks = new HashSet();
    public List blockEntities = new ArrayList();
    public List players = new ArrayList();
    public long ticks = 0L;
    private long cloudColor = 0xFFFFFFL;
    public int ambientDarkness = 0;
    protected int randomTickLCG = new Random().nextInt();
    protected int randomTickLCGIncrement = 1013904223;
    public boolean suppressNeighborChangedUpdates = false;
    private long initTimeMillis = System.currentTimeMillis();
    protected int lastSaveTime = 40;
    public int difficulty;
    public Random random = new Random();
    public int spawnPointX;
    public int spawnPointY;
    public int spawnPointZ;
    public boolean isNew = false;
    public final Dimension dimension;
    protected List eventListeners = new ArrayList();
    private ChunkSource chunkSource;
    public File dir;
    public File saveDir;
    public long seed = 0L;
    private NbtCompound playerData;
    public long sizeOnDisk = 0L;
    public final String saveName;
    public boolean searchingSpawnPoint;
    private ArrayList collisions = new ArrayList();
    private int doLightUpdatesDepth = 0;
    static int updateLightDepth = 0;
    private Set tickingChunks = new HashSet();
    private int ambientSoundCooldown = this.random.nextInt(12000);
    private List entitiesInArea = new ArrayList();
    public boolean isMultiplayer = false;

    @Environment(value=EnvType.CLIENT)
    public static NbtCompound getWorldData(File gameDir, String saveName) {
        File file = new File(gameDir, "saves");
        File file2 = new File(file, saveName);
        if (!file2.exists()) {
            return null;
        }
        File file3 = new File(file2, "level.dat");
        if (file3.exists()) {
            try {
                NbtCompound nbtCompound = NbtIo.readCompressed(new FileInputStream(file3));
                NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
                return nbtCompound2;
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public static void deleteWorld(File gameDir, String saveName) {
        File file = new File(gameDir, "saves");
        File file2 = new File(file, saveName);
        if (!file2.exists()) {
            return;
        }
        World.deleteFilesRecursively(file2.listFiles());
        file2.delete();
    }

    @Environment(value=EnvType.CLIENT)
    private static void deleteFilesRecursively(File[] files) {
        for (int i = 0; i < files.length; ++i) {
            if (files[i].isDirectory()) {
                World.deleteFilesRecursively(files[i].listFiles());
            }
            files[i].delete();
        }
    }

    public BiomeSource getBiomeSource() {
        return this.dimension.biomeSource;
    }

    @Environment(value=EnvType.CLIENT)
    public World(File dir, String saveName) {
        this(dir, saveName, new Random().nextLong());
    }

    @Environment(value=EnvType.CLIENT)
    public World(String saveName, Dimension dimension, long seed) {
        this.saveName = saveName;
        this.seed = seed;
        this.dimension = dimension;
        dimension.init(this);
        this.chunkSource = this.createChunkCache(this.saveDir);
        this.initAmbientDarkness();
    }

    @Environment(value=EnvType.CLIENT)
    public World(World world, Dimension dimension) {
        this.initTimeMillis = world.initTimeMillis;
        this.dir = world.dir;
        this.saveDir = world.saveDir;
        this.saveName = world.saveName;
        this.seed = world.seed;
        this.ticks = world.ticks;
        this.spawnPointX = world.spawnPointX;
        this.spawnPointY = world.spawnPointY;
        this.spawnPointZ = world.spawnPointZ;
        this.sizeOnDisk = world.sizeOnDisk;
        this.dimension = dimension;
        dimension.init(this);
        this.chunkSource = this.createChunkCache(this.saveDir);
        this.initAmbientDarkness();
    }

    @Environment(value=EnvType.CLIENT)
    public World(File dir, String saveName, long seed) {
        this(dir, saveName, seed, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public World(File dir, String saveName, long seed, Dimension dimension) {
        this.dir = dir;
        this.saveName = saveName;
        dir.mkdirs();
        this.saveDir = new File(dir, saveName);
        this.saveDir.mkdirs();
        try {
            File file = new File(this.saveDir, "session.lock");
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file));
            try {
                dataOutputStream.writeLong(this.initTimeMillis);
            }
            finally {
                dataOutputStream.close();
            }
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
            throw new RuntimeException("Failed to check session lock, aborting");
        }
        Dimension dimension2 = new Dimension();
        File file3 = new File(this.saveDir, "level.dat");
        boolean bl = this.isNew = !file3.exists();
        if (file3.exists()) {
            try {
                NbtCompound nbtCompound = NbtIo.readCompressed(new FileInputStream(file3));
                NbtCompound nbtCompound2 = nbtCompound.getCompound("Data");
                this.seed = nbtCompound2.getLong("RandomSeed");
                this.spawnPointX = nbtCompound2.getInt("SpawnX");
                this.spawnPointY = nbtCompound2.getInt("SpawnY");
                this.spawnPointZ = nbtCompound2.getInt("SpawnZ");
                this.ticks = nbtCompound2.getLong("Time");
                this.sizeOnDisk = nbtCompound2.getLong("SizeOnDisk");
                if (nbtCompound2.contains("Player")) {
                    this.playerData = nbtCompound2.getCompound("Player");
                    int j = this.playerData.getInt("Dimension");
                    if (j == -1) {
                        dimension2 = new NetherDimension();
                    }
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        if (dimension != null) {
            dimension2 = dimension;
        }
        boolean i = false;
        if (this.seed == 0L) {
            this.seed = seed;
            i = true;
        }
        this.dimension = dimension2;
        this.dimension.init(this);
        this.chunkSource = this.createChunkCache(this.saveDir);
        if (i) {
            this.searchingSpawnPoint = true;
            this.spawnPointX = 0;
            this.spawnPointY = 64;
            this.spawnPointZ = 0;
            while (!this.dimension.isValidSpawnPoint(this.spawnPointX, this.spawnPointZ)) {
                this.spawnPointX += this.random.nextInt(64) - this.random.nextInt(64);
                this.spawnPointZ += this.random.nextInt(64) - this.random.nextInt(64);
            }
            this.searchingSpawnPoint = false;
        }
        this.initAmbientDarkness();
    }

    protected ChunkSource createChunkCache(File dir) {
        return new ChunkCache(this, this.dimension.createChunkStorage(dir), this.dimension.createChunkGenerator());
    }

    @Environment(value=EnvType.CLIENT)
    public void resetSpawnPoint() {
        if (this.spawnPointY <= 0) {
            this.spawnPointY = 64;
        }
        while (this.getSurfaceBlock(this.spawnPointX, this.spawnPointZ) == 0) {
            this.spawnPointX += this.random.nextInt(8) - this.random.nextInt(8);
            this.spawnPointZ += this.random.nextInt(8) - this.random.nextInt(8);
        }
    }

    public int getSurfaceBlock(int x, int z) {
        int i = 63;
        while (this.getBlock(x, i + 1, z) != 0) {
            ++i;
        }
        return this.getBlock(x, i, z);
    }

    @Environment(value=EnvType.CLIENT)
    public void clearPlayerData() {
    }

    @Environment(value=EnvType.CLIENT)
    public void loadPlayer(PlayerEntity player) {
        try {
            if (this.playerData != null) {
                player.readNbt(this.playerData);
                this.playerData = null;
            }
            this.addEntity(player);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void save(boolean saveEntities, ProgressListener progressListener) {
        if (!this.chunkSource.shouldSave()) {
            return;
        }
        if (progressListener != null) {
            progressListener.progressStartNoAbort("Saving level");
        }
        this.saveData();
        if (progressListener != null) {
            progressListener.progressStage("Saving chunks");
        }
        this.chunkSource.save(saveEntities, progressListener);
    }

    private void saveData() {
        PlayerEntity playerEntity;
        this.checkSessionLock();
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putLong("RandomSeed", this.seed);
        nbtCompound.putInt("SpawnX", this.spawnPointX);
        nbtCompound.putInt("SpawnY", this.spawnPointY);
        nbtCompound.putInt("SpawnZ", this.spawnPointZ);
        nbtCompound.putLong("Time", this.ticks);
        nbtCompound.putLong("SizeOnDisk", this.sizeOnDisk);
        nbtCompound.putLong("LastPlayed", System.currentTimeMillis());
        Object object = null;
        if (this.players.size() > 0) {
            playerEntity = (PlayerEntity)this.players.get(0);
        }
        if (playerEntity != null) {
            NbtCompound nbtCompound2 = new NbtCompound();
            playerEntity.writeNbtWithoutId(nbtCompound2);
            nbtCompound.putCompound("Player", nbtCompound2);
        }
        NbtCompound nbtCompound3 = new NbtCompound();
        nbtCompound3.put("Data", nbtCompound);
        try {
            File file = new File(this.saveDir, "level.dat_new");
            File file2 = new File(this.saveDir, "level.dat_old");
            File file3 = new File(this.saveDir, "level.dat");
            NbtIo.writeCompressed(nbtCompound3, new FileOutputStream(file));
            if (file2.exists()) {
                file2.delete();
            }
            file3.renameTo(file2);
            if (file3.exists()) {
                file3.delete();
            }
            file.renameTo(file3);
            if (file.exists()) {
                file.delete();
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public boolean saveWhilePaused(int step) {
        if (!this.chunkSource.shouldSave()) {
            return true;
        }
        if (step == 0) {
            this.saveData();
        }
        return this.chunkSource.save(false, null);
    }

    public int getBlock(int x, int y, int z) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return 0;
        }
        if (y < 0) {
            return 0;
        }
        if (y >= 128) {
            return 0;
        }
        return this.getChunkAt(x >> 4, z >> 4).getBlockAt(x & 0xF, y, z & 0xF);
    }

    public boolean isChunkLoaded(int x, int y, int z) {
        if (y < 0 || y >= 128) {
            return false;
        }
        return this.isChunkLoadedAt(x >> 4, z >> 4);
    }

    public boolean isAreaLoaded(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY < 0 || minY >= 128) {
            return false;
        }
        minX >>= 4;
        minY >>= 4;
        minZ >>= 4;
        maxX >>= 4;
        maxY >>= 4;
        maxZ >>= 4;
        for (int i = minX; i <= maxX; ++i) {
            for (int j = minZ; j <= maxZ; ++j) {
                if (this.isChunkLoadedAt(i, j)) continue;
                return false;
            }
        }
        return true;
    }

    private boolean isChunkLoadedAt(int chunkX, int chunkZ) {
        return this.chunkSource.hasChunk(chunkX, chunkZ);
    }

    public WorldChunk getChunk(int x, int z) {
        return this.getChunkAt(x >> 4, z >> 4);
    }

    public WorldChunk getChunkAt(int chunkX, int chunkZ) {
        return this.chunkSource.getChunk(chunkX, chunkZ);
    }

    public boolean setBlockWithMetadataQuietly(int x, int y, int z, int block, int metadata) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return false;
        }
        if (y < 0) {
            return false;
        }
        if (y >= 128) {
            return false;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        return worldChunk.setBlockWithMetadataAt(x & 0xF, y, z & 0xF, block, metadata);
    }

    public boolean setBlockQuietly(int x, int y, int z, int block) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return false;
        }
        if (y < 0) {
            return false;
        }
        if (y >= 128) {
            return false;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        return worldChunk.setBlockAt(x & 0xF, y, z & 0xF, block);
    }

    public Material getMaterial(int x, int y, int z) {
        int i = this.getBlock(x, y, z);
        if (i == 0) {
            return Material.AIR;
        }
        return Block.BY_ID[i].material;
    }

    public int getBlockMetadata(int x, int y, int z) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return 0;
        }
        if (y < 0) {
            return 0;
        }
        if (y >= 128) {
            return 0;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        return worldChunk.getBlockMetadataAt(x &= 0xF, y, z &= 0xF);
    }

    public void setBlockMetadata(int x, int y, int z, int metadata) {
        if (this.setBlockMetadataQuietly(x, y, z, metadata)) {
            this.onBlockChanged(x, y, z, this.getBlock(x, y, z));
        }
    }

    public boolean setBlockMetadataQuietly(int x, int y, int z, int metadata) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return false;
        }
        if (y < 0) {
            return false;
        }
        if (y >= 128) {
            return false;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        worldChunk.setBlockMetadataAt(x &= 0xF, y, z &= 0xF, metadata);
        return true;
    }

    public boolean setBlock(int x, int y, int z, int block) {
        if (this.setBlockQuietly(x, y, z, block)) {
            this.onBlockChanged(x, y, z, block);
            return true;
        }
        return false;
    }

    public boolean setBlockWithMetadata(int x, int y, int z, int block, int metadata) {
        if (this.setBlockWithMetadataQuietly(x, y, z, block, metadata)) {
            this.onBlockChanged(x, y, z, block);
            return true;
        }
        return false;
    }

    public void notifyBlockChanged(int x, int y, int z) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).notifyBlockChanged(x, y, z);
        }
    }

    protected void onBlockChanged(int x, int y, int z, int block) {
        this.notifyBlockChanged(x, y, z);
        this.updateNeighbors(x, y, z, block);
    }

    public void onHeightMapChanged(int x, int z, int newHeight, int oldHeight) {
        if (newHeight > oldHeight) {
            int i = oldHeight;
            oldHeight = newHeight;
            newHeight = i;
        }
        this.notifyRegionChanged(x, newHeight, z, x, oldHeight, z);
    }

    public void notifyRegionChanged(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).notifyRegionChanged(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public void updateNeighbors(int x, int y, int z, int block) {
        this.neighborChanged(x - 1, y, z, block);
        this.neighborChanged(x + 1, y, z, block);
        this.neighborChanged(x, y - 1, z, block);
        this.neighborChanged(x, y + 1, z, block);
        this.neighborChanged(x, y, z - 1, block);
        this.neighborChanged(x, y, z + 1, block);
    }

    private void neighborChanged(int x, int y, int z, int neighborBlock) {
        if (this.suppressNeighborChangedUpdates || this.isMultiplayer) {
            return;
        }
        Block block = Block.BY_ID[this.getBlock(x, y, z)];
        if (block != null) {
            block.neighborChanged(this, x, y, z, neighborBlock);
        }
    }

    public boolean hasSkyAccess(int x, int y, int z) {
        return this.getChunkAt(x >> 4, z >> 4).hasSkyAccessAt(x & 0xF, y, z & 0xF);
    }

    public int getRawBrightness(int x, int y, int z) {
        return this.getRawBrightness(x, y, z, true);
    }

    public int getRawBrightness(int x, int y, int z, boolean useNeighborLight) {
        int i;
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return 15;
        }
        if (useNeighborLight && ((i = this.getBlock(x, y, z)) == Block.STONE_SLAB.id || i == Block.FARMLAND.id)) {
            int k = this.getRawBrightness(x, y + 1, z, false);
            int l = this.getRawBrightness(x + 1, y, z, false);
            int m = this.getRawBrightness(x - 1, y, z, false);
            int n = this.getRawBrightness(x, y, z + 1, false);
            int o = this.getRawBrightness(x, y, z - 1, false);
            if (l > k) {
                k = l;
            }
            if (m > k) {
                k = m;
            }
            if (n > k) {
                k = n;
            }
            if (o > k) {
                k = o;
            }
            return k;
        }
        if (y < 0) {
            return 0;
        }
        if (y >= 128) {
            int j = 15 - this.ambientDarkness;
            if (j < 0) {
                j = 0;
            }
            return j;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        return worldChunk.getActualLightAt(x &= 0xF, y, z &= 0xF, this.ambientDarkness);
    }

    public boolean hasSkyLight(int x, int y, int z) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return false;
        }
        if (y < 0) {
            return false;
        }
        if (y >= 128) {
            return true;
        }
        if (!this.isChunkLoadedAt(x >> 4, z >> 4)) {
            return false;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        return worldChunk.hasSkyAccessAt(x &= 0xF, y, z &= 0xF);
    }

    public int getHeight(int x, int z) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return 0;
        }
        if (!this.isChunkLoadedAt(x >> 4, z >> 4)) {
            return 0;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        return worldChunk.getHeight(x & 0xF, z & 0xF);
    }

    public void updateLightIfOtherThan(LightType type, int x, int y, int z, int light) {
        int i;
        if (this.dimension.noSky && type == LightType.SKY) {
            return;
        }
        if (!this.isChunkLoaded(x, y, z)) {
            return;
        }
        if (type == LightType.SKY) {
            if (this.hasSkyLight(x, y, z)) {
                light = 15;
            }
        } else if (type == LightType.BLOCK && Block.LIGHT[i = this.getBlock(x, y, z)] > light) {
            light = Block.LIGHT[i];
        }
        if (this.getLight(type, x, y, z) != light) {
            this.updateLight(type, x, y, z, x, y, z);
        }
    }

    public int getLight(LightType type, int x, int y, int z) {
        if (y < 0 || y >= 128 || x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return type.defaultValue;
        }
        int i = x >> 4;
        int j = z >> 4;
        if (!this.isChunkLoadedAt(i, j)) {
            return 0;
        }
        WorldChunk worldChunk = this.getChunkAt(i, j);
        return worldChunk.getLightAt(type, x & 0xF, y, z & 0xF);
    }

    public void setLight(LightType type, int x, int y, int z, int light) {
        if (x < -32000000 || z < -32000000 || x >= 32000000 || z > 32000000) {
            return;
        }
        if (y < 0) {
            return;
        }
        if (y >= 128) {
            return;
        }
        if (!this.isChunkLoadedAt(x >> 4, z >> 4)) {
            return;
        }
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        worldChunk.setLightAt(type, x & 0xF, y, z & 0xF, light);
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).notifyBlockChanged(x, y, z);
        }
    }

    public float getBrightness(int x, int y, int z) {
        return this.dimension.brightnessTable[this.getRawBrightness(x, y, z)];
    }

    public boolean isSunny() {
        return this.ambientDarkness < 4;
    }

    public HitResult rayTrace(Vec3d from, Vec3d to) {
        return this.rayTrace(from, to, false);
    }

    public HitResult rayTrace(Vec3d from, Vec3d to, boolean allowLiquids) {
        if (Double.isNaN(from.x) || Double.isNaN(from.y) || Double.isNaN(from.z)) {
            return null;
        }
        if (Double.isNaN(to.x) || Double.isNaN(to.y) || Double.isNaN(to.z)) {
            return null;
        }
        int i = MathHelper.floor(to.x);
        int j = MathHelper.floor(to.y);
        int k = MathHelper.floor(to.z);
        int l = MathHelper.floor(from.x);
        int m = MathHelper.floor(from.y);
        int n = MathHelper.floor(from.z);
        int o = 200;
        while (o-- >= 0) {
            HitResult hitResult;
            if (Double.isNaN(from.x) || Double.isNaN(from.y) || Double.isNaN(from.z)) {
                return null;
            }
            if (l == i && m == j && n == k) {
                return null;
            }
            double d = 999.0;
            double e = 999.0;
            double f = 999.0;
            if (i > l) {
                d = (double)l + 1.0;
            }
            if (i < l) {
                d = (double)l + 0.0;
            }
            if (j > m) {
                e = (double)m + 1.0;
            }
            if (j < m) {
                e = (double)m + 0.0;
            }
            if (k > n) {
                f = (double)n + 1.0;
            }
            if (k < n) {
                f = (double)n + 0.0;
            }
            double g = 999.0;
            double h = 999.0;
            double p = 999.0;
            double q = to.x - from.x;
            double r = to.y - from.y;
            double s = to.z - from.z;
            if (d != 999.0) {
                g = (d - from.x) / q;
            }
            if (e != 999.0) {
                h = (e - from.y) / r;
            }
            if (f != 999.0) {
                p = (f - from.z) / s;
            }
            int t = 0;
            if (g < h && g < p) {
                t = i > l ? 4 : 5;
                from.x = d;
                from.y += r * g;
                from.z += s * g;
            } else if (h < p) {
                t = j > m ? 0 : 1;
                from.x += q * h;
                from.y = e;
                from.z += s * h;
            } else {
                t = k > n ? 2 : 3;
                from.x += q * p;
                from.y += r * p;
                from.z = f;
            }
            Vec3d vec3d = Vec3d.fromPool(from.x, from.y, from.z);
            vec3d.x = MathHelper.floor(from.x);
            l = (int)vec3d.x;
            if (t == 5) {
                --l;
                vec3d.x += 1.0;
            }
            vec3d.y = MathHelper.floor(from.y);
            m = (int)vec3d.y;
            if (t == 1) {
                --m;
                vec3d.y += 1.0;
            }
            vec3d.z = MathHelper.floor(from.z);
            n = (int)vec3d.z;
            if (t == 3) {
                --n;
                vec3d.z += 1.0;
            }
            int u = this.getBlock(l, m, n);
            int v = this.getBlockMetadata(l, m, n);
            Block block = Block.BY_ID[u];
            if (u <= 0 || !block.canRayTrace(v, allowLiquids) || (hitResult = block.rayTrace(this, l, m, n, from, to)) == null) continue;
            return hitResult;
        }
        return null;
    }

    public void playSound(Entity source, String sound, float volume, float pitch) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).playSound(sound, source.x, source.y - (double)source.eyeHeight, source.z, volume, pitch);
        }
    }

    public void playSound(double x, double y, double z, String sound, float volume, float pitch) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).playSound(sound, x, y, z, volume, pitch);
        }
    }

    public void playRecordMusic(String record, int x, int y, int z) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).playRecordMusic(record, x, y, z);
        }
    }

    public void addParticle(String type, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).addParticle(type, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    public boolean addEntity(Entity entity) {
        int i = MathHelper.floor(entity.x / 16.0);
        int j = MathHelper.floor(entity.z / 16.0);
        boolean k = false;
        if (entity instanceof PlayerEntity) {
            k = true;
        }
        if (k || this.isChunkLoadedAt(i, j)) {
            if (entity instanceof PlayerEntity) {
                this.players.add((PlayerEntity)entity);
                System.out.println("Player count: " + this.players.size());
            }
            this.getChunkAt(i, j).addEntity(entity);
            this.entities.add(entity);
            this.notifyEntityAdded(entity);
            return true;
        }
        return false;
    }

    protected void notifyEntityAdded(Entity entity) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).notifyEntityAdded(entity);
        }
    }

    protected void notifyEntityRemoved(Entity entity) {
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).notifyEntityRemoved(entity);
        }
    }

    public void removeEntity(Entity entity) {
        entity.remove();
        if (entity instanceof PlayerEntity) {
            this.players.remove((PlayerEntity)entity);
        }
    }

    @Environment(value=EnvType.SERVER)
    public void removeEntityNow(Entity entity) {
        entity.remove();
        if (entity instanceof PlayerEntity) {
            this.players.remove((PlayerEntity)entity);
        }
        int i = entity.chunkX;
        int j = entity.chunkZ;
        if (entity.inChunk && this.isChunkLoadedAt(i, j)) {
            this.getChunkAt(i, j).removeEntity(entity);
        }
        this.entities.remove(entity);
        this.notifyEntityRemoved(entity);
    }

    public void addEventListener(WorldEventListener listener) {
        this.eventListeners.add(listener);
    }

    @Environment(value=EnvType.CLIENT)
    public void removeEventListener(WorldEventListener listener) {
        this.eventListeners.remove(listener);
    }

    public List getCollisions(Entity entity, Box shape) {
        this.collisions.clear();
        int i = MathHelper.floor(shape.minX);
        int j = MathHelper.floor(shape.maxX + 1.0);
        int k = MathHelper.floor(shape.minY);
        int l = MathHelper.floor(shape.maxY + 1.0);
        int m = MathHelper.floor(shape.minZ);
        int n = MathHelper.floor(shape.maxZ + 1.0);
        for (int o = i; o < j; ++o) {
            for (int p = m; p < n; ++p) {
                if (!this.isChunkLoaded(o, 64, p)) continue;
                for (int q = k - 1; q < l; ++q) {
                    Block block = Block.BY_ID[this.getBlock(o, q, p)];
                    if (block == null) continue;
                    block.addCollisions(this, o, q, p, shape, this.collisions);
                }
            }
        }
        double d = 0.25;
        List list = this.getEntities(entity, shape.grown(d, d, d));
        for (int r = 0; r < list.size(); ++r) {
            Box box = ((Entity)list.get(r)).getCollisionShape();
            if (box != null && box.intersects(shape)) {
                this.collisions.add(box);
            }
            if ((box = entity.getCollisionAgainstShape((Entity)list.get(r))) == null || !box.intersects(shape)) continue;
            this.collisions.add(box);
        }
        return this.collisions;
    }

    public int calculateAmbientDarkness(float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        float g = 1.0f - (MathHelper.cos(f * (float)Math.PI * 2.0f) * 2.0f + 0.5f);
        if (g < 0.0f) {
            g = 0.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        }
        return (int)(g * 11.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d getSkyColor(Entity entity, float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        float g = MathHelper.cos(f * (float)Math.PI * 2.0f) * 2.0f + 0.5f;
        if (g < 0.0f) {
            g = 0.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        }
        int i = MathHelper.floor(entity.x);
        int j = MathHelper.floor(entity.z);
        float h = (float)this.getBiomeSource().getTemperature(i, j);
        int k = this.getBiomeSource().getBiome(i, j).getSkyColor(h);
        float l = (float)(k >> 16 & 0xFF) / 255.0f;
        float m = (float)(k >> 8 & 0xFF) / 255.0f;
        float n = (float)(k & 0xFF) / 255.0f;
        return Vec3d.fromPool(l *= g, m *= g, n *= g);
    }

    public float getTimeOfDay(float tickDelta) {
        return this.dimension.getTimeOfDay(this.ticks, tickDelta);
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d getCloudColor(float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        float g = MathHelper.cos(f * (float)Math.PI * 2.0f) * 2.0f + 0.5f;
        if (g < 0.0f) {
            g = 0.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        }
        float h = (float)(this.cloudColor >> 16 & 0xFFL) / 255.0f;
        float i = (float)(this.cloudColor >> 8 & 0xFFL) / 255.0f;
        float j = (float)(this.cloudColor & 0xFFL) / 255.0f;
        return Vec3d.fromPool(h *= g * 0.9f + 0.1f, i *= g * 0.9f + 0.1f, j *= g * 0.85f + 0.15f);
    }

    @Environment(value=EnvType.CLIENT)
    public Vec3d getFogColor(float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        return this.dimension.getFogColor(f, tickDelta);
    }

    public int getSurfaceHeight(int x, int z) {
        int i;
        WorldChunk worldChunk = this.getChunk(x, z);
        for (i = 127; this.getMaterial(x, i, z).blocksMovement() && i > 0; --i) {
        }
        x &= 0xF;
        z &= 0xF;
        while (i > 0) {
            int j = worldChunk.getBlockAt(x, i, z);
            if (j == 0 || !Block.BY_ID[j].material.blocksMovement() && !Block.BY_ID[j].material.isLiquid()) {
                --i;
                continue;
            }
            return i + 1;
        }
        return -1;
    }

    @Environment(value=EnvType.CLIENT)
    public int getPrecipitationHeight(int x, int z) {
        return this.getChunk(x, z).getHeight(x & 0xF, z & 0xF);
    }

    @Environment(value=EnvType.CLIENT)
    public float getStarBrightness(float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        float g = 1.0f - (MathHelper.cos(f * (float)Math.PI * 2.0f) * 2.0f + 0.75f);
        if (g < 0.0f) {
            g = 0.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        }
        return g * g * 0.5f;
    }

    public void scheduleTick(int x, int y, int z, int block) {
        ScheduledTick scheduledTick = new ScheduledTick(x, y, z, block);
        int i = 8;
        if (this.doTicksImmediately) {
            int j;
            if (this.isAreaLoaded(scheduledTick.x - i, scheduledTick.y - i, scheduledTick.z - i, scheduledTick.x + i, scheduledTick.y + i, scheduledTick.z + i) && (j = this.getBlock(scheduledTick.x, scheduledTick.y, scheduledTick.z)) == scheduledTick.block && j > 0) {
                Block.BY_ID[j].tick(this, scheduledTick.x, scheduledTick.y, scheduledTick.z, this.random);
            }
            return;
        }
        if (this.isAreaLoaded(x - i, y - i, z - i, x + i, y + i, z + i)) {
            if (block > 0) {
                scheduledTick.setTime((long)Block.BY_ID[block].getTickRate() + this.ticks);
            }
            if (!this.scheduledTicks.contains(scheduledTick)) {
                this.scheduledTicks.add(scheduledTick);
                this.scheduledTicksInOrder.add(scheduledTick);
            }
        }
    }

    public void tickEntities() {
        int i;
        this.entities.removeAll(this.entitiesToRemove);
        for (i = 0; i < this.entitiesToRemove.size(); ++i) {
            Entity entity = (Entity)this.entitiesToRemove.get(i);
            int j = entity.chunkX;
            int l = entity.chunkZ;
            if (!entity.inChunk || !this.isChunkLoadedAt(j, l)) continue;
            this.getChunkAt(j, l).removeEntity(entity);
        }
        for (i = 0; i < this.entitiesToRemove.size(); ++i) {
            this.notifyEntityRemoved((Entity)this.entitiesToRemove.get(i));
        }
        this.entitiesToRemove.clear();
        for (i = 0; i < this.entities.size(); ++i) {
            Entity entity2 = (Entity)this.entities.get(i);
            if (entity2.vehicle != null) {
                if (!entity2.vehicle.removed && entity2.vehicle.rider == entity2) continue;
                entity2.vehicle.rider = null;
                entity2.vehicle = null;
            }
            if (!entity2.removed) {
                this.tickEntity(entity2);
            }
            if (!entity2.removed) continue;
            int k = entity2.chunkX;
            int m = entity2.chunkZ;
            if (entity2.inChunk && this.isChunkLoadedAt(k, m)) {
                this.getChunkAt(k, m).removeEntity(entity2);
            }
            this.entities.remove(i--);
            this.notifyEntityRemoved(entity2);
        }
        for (i = 0; i < this.blockEntities.size(); ++i) {
            BlockEntity blockEntity = (BlockEntity)this.blockEntities.get(i);
            blockEntity.tick();
        }
    }

    public void tickEntity(Entity entity) {
        this.tickEntity(entity, true);
    }

    public void tickEntity(Entity entity, boolean requireLoaded) {
        int i = MathHelper.floor(entity.x);
        int j = MathHelper.floor(entity.z);
        int k = 16;
        if (!requireLoaded && !this.isAreaLoaded(i - k, 0, j - k, i + k, 128, j + k)) {
            return;
        }
        entity.prevX = entity.x;
        entity.prevY = entity.y;
        entity.prevZ = entity.z;
        entity.lastYaw = entity.yaw;
        entity.lastPitch = entity.pitch;
        if (requireLoaded && entity.inChunk) {
            if (entity.vehicle != null) {
                entity.rideTick();
            } else {
                entity.tick();
            }
        }
        if (Double.isNaN(entity.x) || Double.isInfinite(entity.x)) {
            entity.x = entity.prevX;
        }
        if (Double.isNaN(entity.y) || Double.isInfinite(entity.y)) {
            entity.y = entity.prevY;
        }
        if (Double.isNaN(entity.z) || Double.isInfinite(entity.z)) {
            entity.z = entity.prevZ;
        }
        if (Double.isNaN(entity.pitch) || Double.isInfinite(entity.pitch)) {
            entity.pitch = entity.lastPitch;
        }
        if (Double.isNaN(entity.yaw) || Double.isInfinite(entity.yaw)) {
            entity.yaw = entity.lastYaw;
        }
        int l = MathHelper.floor(entity.x / 16.0);
        int m = MathHelper.floor(entity.y / 16.0);
        int n = MathHelper.floor(entity.z / 16.0);
        if (!entity.inChunk || entity.chunkX != l || entity.chunkY != m || entity.chunkZ != n) {
            if (entity.inChunk && this.isChunkLoadedAt(entity.chunkX, entity.chunkZ)) {
                this.getChunkAt(entity.chunkX, entity.chunkZ).removeEntity(entity, entity.chunkY);
            }
            if (this.isChunkLoadedAt(l, n)) {
                entity.inChunk = true;
                this.getChunkAt(l, n).addEntity(entity);
            } else {
                entity.inChunk = false;
            }
        }
        if (requireLoaded && entity.inChunk && entity.rider != null) {
            if (entity.rider.removed || entity.rider.vehicle != entity) {
                entity.rider.vehicle = null;
                entity.rider = null;
            } else {
                this.tickEntity(entity.rider);
            }
        }
    }

    public boolean isUnobstructed(Box bounds) {
        List list = this.getEntities(null, bounds);
        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity)list.get(i);
            if (entity.removed || !entity.blocksBuilding) continue;
            return false;
        }
        return true;
    }

    public boolean containsLiquid(Box bounds) {
        int i = MathHelper.floor(bounds.minX);
        int j = MathHelper.floor(bounds.maxX + 1.0);
        int k = MathHelper.floor(bounds.minY);
        int l = MathHelper.floor(bounds.maxY + 1.0);
        int m = MathHelper.floor(bounds.minZ);
        int n = MathHelper.floor(bounds.maxZ + 1.0);
        if (bounds.minX < 0.0) {
            --i;
        }
        if (bounds.minY < 0.0) {
            --k;
        }
        if (bounds.minZ < 0.0) {
            --m;
        }
        for (int o = i; o < j; ++o) {
            for (int p = k; p < l; ++p) {
                for (int q = m; q < n; ++q) {
                    Block block = Block.BY_ID[this.getBlock(o, p, q)];
                    if (block == null || !block.material.isLiquid()) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsFireSource(Box bounds) {
        int i = MathHelper.floor(bounds.minX);
        int j = MathHelper.floor(bounds.maxX + 1.0);
        int k = MathHelper.floor(bounds.minY);
        int l = MathHelper.floor(bounds.maxY + 1.0);
        int m = MathHelper.floor(bounds.minZ);
        int n = MathHelper.floor(bounds.maxZ + 1.0);
        for (int o = i; o < j; ++o) {
            for (int p = k; p < l; ++p) {
                for (int q = m; q < n; ++q) {
                    int r = this.getBlock(o, p, q);
                    if (r != Block.FIRE.id && r != Block.FLOWING_LAVA.id && r != Block.LAVA.id) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean applyLiquidDrag(Box bounds, Material material, Entity entity) {
        int i = MathHelper.floor(bounds.minX);
        int j = MathHelper.floor(bounds.maxX + 1.0);
        int k = MathHelper.floor(bounds.minY);
        int l = MathHelper.floor(bounds.maxY + 1.0);
        int m = MathHelper.floor(bounds.minZ);
        int n = MathHelper.floor(bounds.maxZ + 1.0);
        boolean o = false;
        Vec3d vec3d = Vec3d.fromPool(0.0, 0.0, 0.0);
        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    double e;
                    Block block = Block.BY_ID[this.getBlock(p, q, r)];
                    if (block == null || block.material != material || !((double)l >= (e = (double)((float)(q + 1) - LiquidBlock.getHeightLoss(this.getBlockMetadata(p, q, r)))))) continue;
                    o = true;
                    block.applyMaterialDrag(this, p, q, r, entity, vec3d);
                }
            }
        }
        if (vec3d.length() > 0.0) {
            vec3d = vec3d.normalize();
            double d = 0.004;
            entity.velocityX += vec3d.x * d;
            entity.velocityY += vec3d.y * d;
            entity.velocityZ += vec3d.z * d;
        }
        return o;
    }

    public boolean containsMaterial(Box bounds, Material material) {
        int i = MathHelper.floor(bounds.minX);
        int j = MathHelper.floor(bounds.maxX + 1.0);
        int k = MathHelper.floor(bounds.minY);
        int l = MathHelper.floor(bounds.maxY + 1.0);
        int m = MathHelper.floor(bounds.minZ);
        int n = MathHelper.floor(bounds.maxZ + 1.0);
        for (int o = i; o < j; ++o) {
            for (int p = k; p < l; ++p) {
                for (int q = m; q < n; ++q) {
                    Block block = Block.BY_ID[this.getBlock(o, p, q)];
                    if (block == null || block.material != material) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsLiquid(Box bounds, Material material) {
        int i = MathHelper.floor(bounds.minX);
        int j = MathHelper.floor(bounds.maxX + 1.0);
        int k = MathHelper.floor(bounds.minY);
        int l = MathHelper.floor(bounds.maxY + 1.0);
        int m = MathHelper.floor(bounds.minZ);
        int n = MathHelper.floor(bounds.maxZ + 1.0);
        for (int o = i; o < j; ++o) {
            for (int p = k; p < l; ++p) {
                for (int q = m; q < n; ++q) {
                    Block block = Block.BY_ID[this.getBlock(o, p, q)];
                    if (block == null || block.material != material) continue;
                    int r = this.getBlockMetadata(o, p, q);
                    double d = p + 1;
                    if (r < 8) {
                        d = (double)(p + 1) - (double)r / 8.0;
                    }
                    if (!(d >= bounds.minY)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public Explosion explode(Entity source, double x, double y, double z, float power) {
        return this.explode(source, x, y, z, power, false);
    }

    public Explosion explode(Entity source, double x, double y, double z, float power, boolean createFire) {
        Explosion explosion = new Explosion(this, source, x, y, z, power);
        explosion.createFire = createFire;
        explosion.damageEntities();
        explosion.damageBlocks();
        return explosion;
    }

    public float getBlockDensity(Vec3d pos, Box bounds) {
        double d = 1.0 / ((bounds.maxX - bounds.minX) * 2.0 + 1.0);
        double e = 1.0 / ((bounds.maxY - bounds.minY) * 2.0 + 1.0);
        double f = 1.0 / ((bounds.maxZ - bounds.minZ) * 2.0 + 1.0);
        int i = 0;
        int j = 0;
        float g = 0.0f;
        while (g <= 1.0f) {
            float h = 0.0f;
            while (h <= 1.0f) {
                float k = 0.0f;
                while (k <= 1.0f) {
                    double l = bounds.minX + (bounds.maxX - bounds.minX) * (double)g;
                    double m = bounds.minY + (bounds.maxY - bounds.minY) * (double)h;
                    double n = bounds.minZ + (bounds.maxZ - bounds.minZ) * (double)k;
                    if (this.rayTrace(Vec3d.fromPool(l, m, n), pos) == null) {
                        ++i;
                    }
                    ++j;
                    k = (float)((double)k + f);
                }
                h = (float)((double)h + e);
            }
            g = (float)((double)g + d);
        }
        return (float)i / (float)j;
    }

    @Environment(value=EnvType.CLIENT)
    public void extinguishFire(int x, int y, int z, int face) {
        if (face == 0) {
            --y;
        }
        if (face == 1) {
            ++y;
        }
        if (face == 2) {
            --z;
        }
        if (face == 3) {
            ++z;
        }
        if (face == 4) {
            --x;
        }
        if (face == 5) {
            ++x;
        }
        if (this.getBlock(x, y, z) == Block.FIRE.id) {
            this.playSound((float)x + 0.5f, (float)y + 0.5f, (float)z + 0.5f, "random.fizz", 0.5f, 2.6f + (this.random.nextFloat() - this.random.nextFloat()) * 0.8f);
            this.setBlock(x, y, z, 0);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public Entity getEntityOfSubtype(Class type) {
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public String getDebugInfo() {
        return "All: " + this.entities.size();
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        if (worldChunk != null) {
            return worldChunk.getBlockEntityAt(x & 0xF, y, z & 0xF);
        }
        return null;
    }

    public void setBlockEntity(int x, int y, int z, BlockEntity blockEntity) {
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        if (worldChunk != null) {
            worldChunk.setBlockEntityAt(x & 0xF, y, z & 0xF, blockEntity);
        }
    }

    public void removeBlockEntity(int x, int y, int z) {
        WorldChunk worldChunk = this.getChunkAt(x >> 4, z >> 4);
        if (worldChunk != null) {
            worldChunk.removeBlockEntityAt(x & 0xF, y, z & 0xF);
        }
    }

    public boolean isSolidBlock(int x, int y, int z) {
        Block block = Block.BY_ID[this.getBlock(x, y, z)];
        if (block == null) {
            return false;
        }
        return block.isSolid();
    }

    @Environment(value=EnvType.CLIENT)
    public void forceSave(ProgressListener progressListener) {
        this.save(true, progressListener);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean doLightUpdates() {
        if (this.doLightUpdatesDepth >= 50) {
            return false;
        }
        ++this.doLightUpdatesDepth;
        try {
            int i = 5000;
            while (this.lightUpdates.size() > 0) {
                if (--i <= 0) {
                    boolean j = true;
                    return j;
                }
                ((LightUpdate)this.lightUpdates.remove(this.lightUpdates.size() - 1)).run(this);
            }
            boolean k = false;
            return k;
        }
        finally {
            --this.doLightUpdatesDepth;
        }
    }

    public void updateLight(LightType type, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.updateLight(type, minX, minY, minZ, maxX, maxY, maxZ, true);
    }

    public void updateLight(LightType type, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean expand) {
        if (this.dimension.noSky && type == LightType.SKY) {
            return;
        }
        if (++updateLightDepth == 50) {
            --updateLightDepth;
            return;
        }
        int i = (maxX + minX) / 2;
        int j = (maxZ + minZ) / 2;
        if (!this.isChunkLoaded(i, 64, j)) {
            --updateLightDepth;
            return;
        }
        int k = this.lightUpdates.size();
        if (expand) {
            int l = 4;
            if (l > k) {
                l = k;
            }
            for (int m = 0; m < l; ++m) {
                LightUpdate lightUpdate = (LightUpdate)this.lightUpdates.get(this.lightUpdates.size() - m - 1);
                if (lightUpdate.type != type || !lightUpdate.expand(minX, minY, minZ, maxX, maxY, maxZ)) continue;
                --updateLightDepth;
                return;
            }
        }
        this.lightUpdates.add(new LightUpdate(type, minX, minY, minZ, maxX, maxY, maxZ));
        if (this.lightUpdates.size() > 100000) {
            this.lightUpdates.clear();
        }
        --updateLightDepth;
    }

    public void initAmbientDarkness() {
        int i = this.calculateAmbientDarkness(1.0f);
        if (i != this.ambientDarkness) {
            this.ambientDarkness = i;
        }
    }

    public void tick() {
        NaturalSpawner.tick(this);
        this.chunkSource.tick();
        int i = this.calculateAmbientDarkness(1.0f);
        if (i != this.ambientDarkness) {
            this.ambientDarkness = i;
            for (int j = 0; j < this.eventListeners.size(); ++j) {
                ((WorldEventListener)this.eventListeners.get(j)).notifyAmbientDarknessChanged();
            }
        }
        ++this.ticks;
        if (this.ticks % (long)this.lastSaveTime == 0L) {
            this.save(false, null);
        }
        this.doScheduledTicks(false);
        this.tickChunks();
    }

    protected void tickChunks() {
        this.tickingChunks.clear();
        for (int i = 0; i < this.players.size(); ++i) {
            PlayerEntity playerEntity = (PlayerEntity)this.players.get(i);
            int j = MathHelper.floor(playerEntity.x / 16.0);
            int l = MathHelper.floor(playerEntity.z / 16.0);
            int n = 9;
            for (int o = -n; o <= n; ++o) {
                for (int r = -n; r <= n; ++r) {
                    this.tickingChunks.add(new ChunkPos(o + j, r + l));
                }
            }
        }
        if (this.ambientSoundCooldown > 0) {
            --this.ambientSoundCooldown;
        }
        for (ChunkPos chunkPos : this.tickingChunks) {
            int k = chunkPos.x * 16;
            int m = chunkPos.z * 16;
            WorldChunk worldChunk = this.getChunkAt(chunkPos.x, chunkPos.z);
            if (this.ambientSoundCooldown == 0) {
                PlayerEntity playerEntity2;
                this.randomTickLCG = this.randomTickLCG * 3 + this.randomTickLCGIncrement;
                int p = this.randomTickLCG >> 2;
                int s = p & 0xF;
                int u = p >> 8 & 0xF;
                int w = p >> 16 & 0x7F;
                int y = worldChunk.getBlockAt(s, w, u);
                if (y == 0 && this.getRawBrightness(s += k, w, u += m) <= this.random.nextInt(8) && this.getLight(LightType.SKY, s, w, u) <= 0 && (playerEntity2 = this.getNearestPlayer((double)s + 0.5, (double)w + 0.5, (double)u + 0.5, 8.0)) != null && playerEntity2.squaredDistanceTo((double)s + 0.5, (double)w + 0.5, (double)u + 0.5) > 4.0) {
                    this.playSound((double)s + 0.5, (double)w + 0.5, (double)u + 0.5, "ambient.cave.cave", 0.7f, 0.8f + this.random.nextFloat() * 0.2f);
                    this.ambientSoundCooldown = this.random.nextInt(12000) + 6000;
                }
            }
            for (int q = 0; q < 80; ++q) {
                this.randomTickLCG = this.randomTickLCG * 3 + this.randomTickLCGIncrement;
                int t = this.randomTickLCG >> 2;
                int v = t & 0xF;
                int x = t >> 8 & 0xF;
                int z = t >> 16 & 0x7F;
                byte aa = worldChunk.blocks[v << 11 | x << 7 | z];
                if (!Block.TICKS_RANDOMLY[aa]) continue;
                Block.BY_ID[aa].tick(this, v + k, z, x + m, this.random);
            }
        }
    }

    public boolean doScheduledTicks(boolean flush) {
        int i = this.scheduledTicksInOrder.size();
        if (i != this.scheduledTicks.size()) {
            throw new IllegalStateException("TickNextTick list out of synch");
        }
        if (i > 1000) {
            i = 1000;
        }
        for (int j = 0; j < i; ++j) {
            int l;
            ScheduledTick scheduledTick = (ScheduledTick)this.scheduledTicksInOrder.first();
            if (!flush && scheduledTick.time > this.ticks) break;
            this.scheduledTicksInOrder.remove(scheduledTick);
            this.scheduledTicks.remove(scheduledTick);
            int k = 8;
            if (!this.isAreaLoaded(scheduledTick.x - k, scheduledTick.y - k, scheduledTick.z - k, scheduledTick.x + k, scheduledTick.y + k, scheduledTick.z + k) || (l = this.getBlock(scheduledTick.x, scheduledTick.y, scheduledTick.z)) != scheduledTick.block || l <= 0) continue;
            Block.BY_ID[l].tick(this, scheduledTick.x, scheduledTick.y, scheduledTick.z, this.random);
        }
        return this.scheduledTicksInOrder.size() != 0;
    }

    @Environment(value=EnvType.CLIENT)
    public void doRandomDisplayTicks(int x, int y, int z) {
        int i = 16;
        Random random = new Random();
        for (int j = 0; j < 1000; ++j) {
            int m;
            int l;
            int k = x + this.random.nextInt(i) - this.random.nextInt(i);
            int n = this.getBlock(k, l = y + this.random.nextInt(i) - this.random.nextInt(i), m = z + this.random.nextInt(i) - this.random.nextInt(i));
            if (n <= 0) continue;
            Block.BY_ID[n].randomDisplayTick(this, k, l, m, random);
        }
    }

    public List getEntities(Entity exclude, Box bounds) {
        this.entitiesInArea.clear();
        int i = MathHelper.floor((bounds.minX - 2.0) / 16.0);
        int j = MathHelper.floor((bounds.maxX + 2.0) / 16.0);
        int k = MathHelper.floor((bounds.minZ - 2.0) / 16.0);
        int l = MathHelper.floor((bounds.maxZ + 2.0) / 16.0);
        for (int m = i; m <= j; ++m) {
            for (int n = k; n <= l; ++n) {
                if (!this.isChunkLoadedAt(m, n)) continue;
                this.getChunkAt(m, n).getEntities(exclude, bounds, this.entitiesInArea);
            }
        }
        return this.entitiesInArea;
    }

    public List getEntitiesOfType(Class type, Box bounds) {
        int i = MathHelper.floor((bounds.minX - 2.0) / 16.0);
        int j = MathHelper.floor((bounds.maxX + 2.0) / 16.0);
        int k = MathHelper.floor((bounds.minZ - 2.0) / 16.0);
        int l = MathHelper.floor((bounds.maxZ + 2.0) / 16.0);
        ArrayList arrayList = new ArrayList();
        for (int m = i; m <= j; ++m) {
            for (int n = k; n <= l; ++n) {
                if (!this.isChunkLoadedAt(m, n)) continue;
                this.getChunkAt(m, n).getEntitiesOfType(type, bounds, arrayList);
            }
        }
        return arrayList;
    }

    @Environment(value=EnvType.CLIENT)
    public List getEntities() {
        return this.entities;
    }

    public void notifyBlockEntityChanged(int x, int y, int z, BlockEntity blockEntity) {
        if (this.isChunkLoaded(x, y, z)) {
            this.getChunk(x, z).markDirty();
        }
        for (int i = 0; i < this.eventListeners.size(); ++i) {
            ((WorldEventListener)this.eventListeners.get(i)).notifyBlockEntityChanged(x, y, z, blockEntity);
        }
    }

    public int getEntityCount(Class type) {
        int i = 0;
        for (int j = 0; j < this.entities.size(); ++j) {
            Entity entity = (Entity)this.entities.get(j);
            if (!type.isAssignableFrom(entity.getClass())) continue;
            ++i;
        }
        return i;
    }

    public void loadEntities(List entities) {
        this.entities.addAll(entities);
        for (int i = 0; i < entities.size(); ++i) {
            this.notifyEntityAdded((Entity)entities.get(i));
        }
    }

    public void unloadEntities(List entities) {
        this.entitiesToRemove.addAll(entities);
    }

    @Environment(value=EnvType.CLIENT)
    public void prepare() {
        while (this.chunkSource.tick()) {
        }
    }

    public boolean canPlace(int block, int x, int y, int z, boolean skipCollisionChecks) {
        int i = this.getBlock(x, y, z);
        Block block2 = Block.BY_ID[i];
        Block block3 = Block.BY_ID[block];
        Box box = block3.getCollisionShape(this, x, y, z);
        if (skipCollisionChecks) {
            box = null;
        }
        if (box != null && !this.isUnobstructed(box)) {
            return false;
        }
        if (block2 == Block.FLOWING_WATER || block2 == Block.WATER || block2 == Block.FLOWING_LAVA || block2 == Block.LAVA || block2 == Block.FIRE || block2 == Block.SNOW_LAYER) {
            return true;
        }
        return block > 0 && block2 == null && block3.canBePlaced(this, x, y, z);
    }

    public Path findPath(Entity entity, Entity target, float range) {
        int i = MathHelper.floor(entity.x);
        int j = MathHelper.floor(entity.y);
        int k = MathHelper.floor(entity.z);
        int l = (int)(range + 16.0f);
        int m = i - l;
        int n = j - l;
        int o = k - l;
        int p = i + l;
        int q = j + l;
        int r = k + l;
        WorldRegion worldRegion = new WorldRegion(this, m, n, o, p, q, r);
        return new PathFinder(worldRegion).findPath(entity, target, range);
    }

    public Path findPath(Entity entity, int x, int y, int z, float range) {
        int i = MathHelper.floor(entity.x);
        int j = MathHelper.floor(entity.y);
        int k = MathHelper.floor(entity.z);
        int l = (int)(range + 8.0f);
        int m = i - l;
        int n = j - l;
        int o = k - l;
        int p = i + l;
        int q = j + l;
        int r = k + l;
        WorldRegion worldRegion = new WorldRegion(this, m, n, o, p, q, r);
        return new PathFinder(worldRegion).findPath(entity, x, y, z, range);
    }

    public boolean hasDirectSignal(int x, int y, int z, int dir) {
        int i = this.getBlock(x, y, z);
        if (i == 0) {
            return false;
        }
        return Block.BY_ID[i].hasDirectSignal(this, x, y, z, dir);
    }

    public boolean hasDirectNeighborSignal(int x, int y, int z) {
        if (this.hasDirectSignal(x, y - 1, z, 0)) {
            return true;
        }
        if (this.hasDirectSignal(x, y + 1, z, 1)) {
            return true;
        }
        if (this.hasDirectSignal(x, y, z - 1, 2)) {
            return true;
        }
        if (this.hasDirectSignal(x, y, z + 1, 3)) {
            return true;
        }
        if (this.hasDirectSignal(x - 1, y, z, 4)) {
            return true;
        }
        return this.hasDirectSignal(x + 1, y, z, 5);
    }

    public boolean hasSignal(int x, int y, int z, int dir) {
        if (this.isSolidBlock(x, y, z)) {
            return this.hasDirectNeighborSignal(x, y, z);
        }
        int i = this.getBlock(x, y, z);
        if (i == 0) {
            return false;
        }
        return Block.BY_ID[i].hasSignal(this, x, y, z, dir);
    }

    public boolean hasNeighborSignal(int x, int y, int z) {
        if (this.hasSignal(x, y - 1, z, 0)) {
            return true;
        }
        if (this.hasSignal(x, y + 1, z, 1)) {
            return true;
        }
        if (this.hasSignal(x, y, z - 1, 2)) {
            return true;
        }
        if (this.hasSignal(x, y, z + 1, 3)) {
            return true;
        }
        if (this.hasSignal(x - 1, y, z, 4)) {
            return true;
        }
        return this.hasSignal(x + 1, y, z, 5);
    }

    public PlayerEntity getNearestPlayer(Entity entity, double range) {
        return this.getNearestPlayer(entity.x, entity.y, entity.z, range);
    }

    public PlayerEntity getNearestPlayer(double x, double y, double z, double range) {
        PlayerEntity playerEntity;
        double d = -1.0;
        Object object = null;
        for (int i = 0; i < this.players.size(); ++i) {
            PlayerEntity playerEntity2 = (PlayerEntity)this.players.get(i);
            double e = playerEntity2.squaredDistanceTo(x, y, z);
            if (!(range < 0.0) && !(e < range * range) || d != -1.0 && !(e < d)) continue;
            d = e;
            playerEntity = playerEntity2;
        }
        return playerEntity;
    }

    @Environment(value=EnvType.CLIENT)
    public void unpackChunkData(int minX, int minY, int minZ, int widthX, int height, int widthZ, byte[] data) {
        int i = minX >> 4;
        int j = minZ >> 4;
        int k = minX + widthX - 1 >> 4;
        int l = minZ + widthZ - 1 >> 4;
        int m = 0;
        int n = minY;
        int o = minY + height;
        if (n < 0) {
            n = 0;
        }
        if (o > 128) {
            o = 128;
        }
        for (int p = i; p <= k; ++p) {
            int q = minX - p * 16;
            int r = minX + widthX - p * 16;
            if (q < 0) {
                q = 0;
            }
            if (r > 16) {
                r = 16;
            }
            for (int s = j; s <= l; ++s) {
                int t = minZ - s * 16;
                int u = minZ + widthZ - s * 16;
                if (t < 0) {
                    t = 0;
                }
                if (u > 16) {
                    u = 16;
                }
                m = this.getChunkAt(p, s).unpackChunkData(data, q, n, t, r, o, u, m);
                this.notifyRegionChanged(p * 16 + q, n, s * 16 + t, p * 16 + r, o, s * 16 + u);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public void disconnect() {
    }

    @Environment(value=EnvType.SERVER)
    public byte[] packChunkData(int minX, int minY, int minZ, int widthX, int height, int widthZ) {
        byte[] bs = new byte[widthX * height * widthZ * 5 / 2];
        int i = minX >> 4;
        int j = minZ >> 4;
        int k = minX + widthX - 1 >> 4;
        int l = minZ + widthZ - 1 >> 4;
        int m = 0;
        int n = minY;
        int o = minY + height;
        if (n < 0) {
            n = 0;
        }
        if (o > 128) {
            o = 128;
        }
        for (int p = i; p <= k; ++p) {
            int q = minX - p * 16;
            int r = minX + widthX - p * 16;
            if (q < 0) {
                q = 0;
            }
            if (r > 16) {
                r = 16;
            }
            for (int s = j; s <= l; ++s) {
                int t = minZ - s * 16;
                int u = minZ + widthZ - s * 16;
                if (t < 0) {
                    t = 0;
                }
                if (u > 16) {
                    u = 16;
                }
                m = this.getChunkAt(p, s).packChunkData(bs, q, n, t, r, o, u, m);
            }
        }
        return bs;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void checkSessionLock() {
        try {
            File file = new File(this.saveDir, "session.lock");
            DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
            try {
                if (dataInputStream.readLong() != this.initTimeMillis) {
                    throw new SessionLockException("The save is being accessed from another location, aborting");
                }
            }
            finally {
                dataInputStream.close();
            }
        }
        catch (IOException iOException) {
            throw new SessionLockException("Failed to check session lock, aborting");
        }
    }

    @Environment(value=EnvType.CLIENT)
    public void setTime(long time) {
        this.ticks = time;
    }

    @Environment(value=EnvType.CLIENT)
    public void addEntityAlways(Entity entity) {
        int i = MathHelper.floor(entity.x / 16.0);
        int j = MathHelper.floor(entity.z / 16.0);
        int k = 2;
        for (int l = i - k; l <= i + k; ++l) {
            for (int m = j - k; m <= j + k; ++m) {
                this.getChunkAt(l, m);
            }
        }
        if (!this.entities.contains(entity)) {
            this.entities.add(entity);
        }
    }

    public boolean canModify(PlayerEntity player, int x, int y, int z) {
        return true;
    }

    public void doEntityEvent(Entity entity, byte event) {
    }

    @Environment(value=EnvType.CLIENT)
    public void removeEntities() {
        int i;
        this.entities.removeAll(this.entitiesToRemove);
        for (i = 0; i < this.entitiesToRemove.size(); ++i) {
            Entity entity = (Entity)this.entitiesToRemove.get(i);
            int j = entity.chunkX;
            int l = entity.chunkZ;
            if (!entity.inChunk || !this.isChunkLoadedAt(j, l)) continue;
            this.getChunkAt(j, l).removeEntity(entity);
        }
        for (i = 0; i < this.entitiesToRemove.size(); ++i) {
            this.notifyEntityRemoved((Entity)this.entitiesToRemove.get(i));
        }
        this.entitiesToRemove.clear();
        for (i = 0; i < this.entities.size(); ++i) {
            Entity entity2 = (Entity)this.entities.get(i);
            if (entity2.vehicle != null) {
                if (!entity2.vehicle.removed && entity2.vehicle.rider == entity2) continue;
                entity2.vehicle.rider = null;
                entity2.vehicle = null;
            }
            if (!entity2.removed) continue;
            int k = entity2.chunkX;
            int m = entity2.chunkZ;
            if (entity2.inChunk && this.isChunkLoadedAt(k, m)) {
                this.getChunkAt(k, m).removeEntity(entity2);
            }
            this.entities.remove(i--);
            this.notifyEntityRemoved(entity2);
        }
    }
}

