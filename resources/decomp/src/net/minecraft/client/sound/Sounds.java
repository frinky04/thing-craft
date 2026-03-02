/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.sound;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.Sound;

@Environment(value=EnvType.CLIENT)
public class Sounds {
    private Random random = new Random();
    private Map soundsByPath = new HashMap();
    private List sounds = new ArrayList();
    public int soundCount = 0;
    public boolean trimTrailingDigits = true;

    public Sound load(String path, File file) {
        try {
            String string = path;
            path = path.substring(0, path.indexOf("."));
            if (this.trimTrailingDigits) {
                while (Character.isDigit(path.charAt(path.length() - 1))) {
                    path = path.substring(0, path.length() - 1);
                }
            }
            if (!this.soundsByPath.containsKey(path = path.replaceAll("/", "."))) {
                this.soundsByPath.put(path, new ArrayList());
            }
            Sound sound = new Sound(string, file.toURI().toURL());
            ((List)this.soundsByPath.get(path)).add(sound);
            this.sounds.add(sound);
            ++this.soundCount;
            return sound;
        }
        catch (MalformedURLException malformedURLException) {
            malformedURLException.printStackTrace();
            throw new RuntimeException(malformedURLException);
        }
    }

    public Sound getRandom(String path) {
        List list = (List)this.soundsByPath.get(path);
        if (list == null) {
            return null;
        }
        return (Sound)list.get(this.random.nextInt(list.size()));
    }

    public Sound getRandom() {
        if (this.sounds.size() == 0) {
            return null;
        }
        return (Sound)this.sounds.get(this.random.nextInt(this.sounds.size()));
    }
}

