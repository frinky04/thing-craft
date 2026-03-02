/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.SERVER)
public class ServerProperties {
    public static Logger LOGGER = Logger.getLogger("Minecraft");
    private Properties properties = new Properties();
    private File file;

    public ServerProperties(File file) {
        this.file = file;
        if (file.exists()) {
            try {
                this.properties.load(new FileInputStream(file));
            }
            catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to load " + file, exception);
                this.generate();
            }
        } else {
            LOGGER.log(Level.WARNING, file + " does not exist");
            this.generate();
        }
    }

    public void generate() {
        LOGGER.log(Level.INFO, "Generating new properties file");
        this.save();
    }

    public void save() {
        try {
            this.properties.store(new FileOutputStream(this.file), "Minecraft server properties");
        }
        catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to save " + this.file, exception);
            this.generate();
        }
    }

    public String getString(String key, String defaultValue) {
        if (!this.properties.containsKey(key)) {
            this.properties.setProperty(key, defaultValue);
            this.save();
        }
        return this.properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(this.getString(key, "" + defaultValue));
        }
        catch (Exception exception) {
            this.properties.setProperty(key, "" + defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(this.getString(key, "" + defaultValue));
        }
        catch (Exception exception) {
            this.properties.setProperty(key, "" + defaultValue);
            return defaultValue;
        }
    }
}

