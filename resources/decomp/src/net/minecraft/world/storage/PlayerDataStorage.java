/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.world.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;

@Environment(value=EnvType.SERVER)
public class PlayerDataStorage {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    private File dir;

    public PlayerDataStorage(File dir) {
        this.dir = dir;
        dir.mkdir();
    }

    public void savePlayerData(ServerPlayerEntity player) {
        try {
            NbtCompound nbtCompound = new NbtCompound();
            player.writeNbtWithoutId(nbtCompound);
            File file = new File(this.dir, "_tmp_.dat");
            File file2 = new File(this.dir, player.name + ".dat");
            NbtIo.writeCompressed(nbtCompound, new FileOutputStream(file));
            if (file2.exists()) {
                file2.delete();
            }
            file.renameTo(file2);
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to save player data for " + player.name);
        }
    }

    public void loadPlayerData(ServerPlayerEntity player) {
        try {
            NbtCompound nbtCompound;
            File file = new File(this.dir, player.name + ".dat");
            if (file.exists() && (nbtCompound = NbtIo.readCompressed(new FileInputStream(file))) != null) {
                player.readNbt(nbtCompound);
            }
        }
        catch (Exception exception) {
            LOGGER.warning("Failed to load player data for " + player.name);
        }
    }
}

