/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.sound;

import java.io.File;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.Sounds;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.codecs.CodecJOrbis;
import paulscode.sound.codecs.CodecMus;
import paulscode.sound.codecs.CodecWav;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

@Environment(value=EnvType.CLIENT)
public class SoundEngine {
    private static SoundSystem system;
    private Sounds sounds = new Sounds();
    private Sounds records = new Sounds();
    private Sounds music = new Sounds();
    private int nextSoundEventIndex = 0;
    private GameOptions options;
    private static boolean started;
    private Random random = new Random();
    private int musicCooldown = this.random.nextInt(12000);

    public void load(GameOptions options) {
        this.records.trimTrailingDigits = false;
        this.options = options;
        if (!(started || options != null && options.soundVolume == 0.0f && options.musicVolume == 0.0f)) {
            this.start();
        }
    }

    private void start() {
        try {
            float f = this.options.soundVolume;
            float g = this.options.musicVolume;
            this.options.soundVolume = 0.0f;
            this.options.musicVolume = 0.0f;
            this.options.save();
            SoundSystemConfig.addLibrary(LibraryLWJGLOpenAL.class);
            SoundSystemConfig.setCodec("ogg", CodecJOrbis.class);
            SoundSystemConfig.setCodec("mus", CodecMus.class);
            SoundSystemConfig.setCodec("wav", CodecWav.class);
            system = new SoundSystem();
            this.options.soundVolume = f;
            this.options.musicVolume = g;
            this.options.save();
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
            System.err.println("error linking with the LibraryJavaSound plug-in");
        }
        started = true;
    }

    public void volumeChanged() {
        if (!(started || this.options.soundVolume == 0.0f && this.options.musicVolume == 0.0f)) {
            this.start();
        }
        if (this.options.musicVolume == 0.0f) {
            system.stop("BgMusic");
        } else {
            system.setVolume("BgMusic", this.options.musicVolume);
        }
    }

    public void close() {
        if (started) {
            system.cleanup();
        }
    }

    public void loadSound(String path, File dir) {
        this.sounds.load(path, dir);
    }

    public void loadRecord(String path, File fil) {
        this.records.load(path, fil);
    }

    public void loadMusic(String path, File file) {
        this.music.load(path, file);
    }

    public void tickMusic() {
        if (!started || this.options.musicVolume == 0.0f) {
            return;
        }
        if (!system.playing("BgMusic") && !system.playing("streaming")) {
            if (this.musicCooldown > 0) {
                --this.musicCooldown;
                return;
            }
            Sound sound = this.music.getRandom();
            if (sound != null) {
                this.musicCooldown = this.random.nextInt(12000) + 12000;
                system.backgroundMusic("BgMusic", sound.url, sound.path, false);
                system.setVolume("BgMusic", this.options.musicVolume);
                system.play("BgMusic");
            }
        }
    }

    public void update(MobEntity listener, float tickDelta) {
        if (!started || this.options.soundVolume == 0.0f) {
            return;
        }
        if (listener == null) {
            return;
        }
        float f = listener.lastYaw + (listener.yaw - listener.lastYaw) * tickDelta;
        double d = listener.lastX + (listener.x - listener.lastX) * (double)tickDelta;
        double e = listener.lastY + (listener.y - listener.lastY) * (double)tickDelta;
        double g = listener.lastZ + (listener.z - listener.lastZ) * (double)tickDelta;
        float h = MathHelper.cos(-f * ((float)Math.PI / 180) - (float)Math.PI);
        float i = MathHelper.sin(-f * ((float)Math.PI / 180) - (float)Math.PI);
        float j = -i;
        float k = 0.0f;
        float l = -h;
        float m = 0.0f;
        float n = 1.0f;
        float o = 0.0f;
        system.setListenerPosition((float)d, (float)e, (float)g);
        system.setListenerOrientation(j, k, l, m, n, o);
    }

    public void playRecord(String name, float x, float y, float z, float volume, float pitch) {
        if (!started || this.options.soundVolume == 0.0f) {
            return;
        }
        String string = "streaming";
        if (system.playing("streaming")) {
            system.stop("streaming");
        }
        if (name == null) {
            return;
        }
        Sound sound = this.records.getRandom(name);
        if (sound != null && volume > 0.0f) {
            if (system.playing("BgMusic")) {
                system.stop("BgMusic");
            }
            float f = 16.0f;
            system.newStreamingSource(true, string, sound.url, sound.path, false, x, y, z, 2, f * 4.0f);
            system.setVolume(string, 0.5f * this.options.soundVolume);
            system.play(string);
        }
    }

    public void play(String id, float x, float y, float z, float volume, float pitch) {
        if (!started || this.options.soundVolume == 0.0f) {
            return;
        }
        Sound sound = this.sounds.getRandom(id);
        if (sound != null && volume > 0.0f) {
            this.nextSoundEventIndex = (this.nextSoundEventIndex + 1) % 256;
            String string = "sound_" + this.nextSoundEventIndex;
            float f = 16.0f;
            if (volume > 1.0f) {
                f *= volume;
            }
            system.newSource(volume > 1.0f, string, sound.url, sound.path, false, x, y, z, 2, f);
            system.setPitch(string, pitch);
            if (volume > 1.0f) {
                volume = 1.0f;
            }
            system.setVolume(string, volume * this.options.soundVolume);
            system.play(string);
        }
    }

    public void play(String id, float volume, float pitch) {
        if (!started || this.options.soundVolume == 0.0f) {
            return;
        }
        Sound sound = this.sounds.getRandom(id);
        if (sound != null) {
            this.nextSoundEventIndex = (this.nextSoundEventIndex + 1) % 256;
            String string = "sound_" + this.nextSoundEventIndex;
            system.newSource(false, string, sound.url, sound.path, false, 0.0f, 0.0f, 0.0f, 0, 0.0f);
            if (volume > 1.0f) {
                volume = 1.0f;
            }
            system.setPitch(string, pitch);
            system.setVolume(string, (volume *= 0.25f) * this.options.soundVolume);
            system.play(string);
        }
    }

    static {
        started = false;
    }
}

