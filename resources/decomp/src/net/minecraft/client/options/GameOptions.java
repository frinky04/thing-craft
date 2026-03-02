/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.input.Keyboard
 */
package net.minecraft.client.options;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import org.lwjgl.input.Keyboard;

@Environment(value=EnvType.CLIENT)
public class GameOptions {
    private static final String[] RENDER_DISTANCE_SETTINGS = new String[]{"FAR", "NORMAL", "SHORT", "TINY"};
    private static final String[] DIFFICULTY_SETTINGS = new String[]{"Peaceful", "Easy", "Normal", "Hard"};
    public float musicVolume = 1.0f;
    public float soundVolume = 1.0f;
    public float mouseSensitivity = 0.5f;
    public boolean invertMouseY = false;
    public int viewDistance = 0;
    public boolean viewBobbing = true;
    public boolean anaglyph = false;
    public boolean fpsLimit = false;
    public boolean fancyGraphics = true;
    public String skin = "Default";
    public KeyBinding forwardKey = new KeyBinding("Forward", 17);
    public KeyBinding leftKey = new KeyBinding("Left", 30);
    public KeyBinding backKey = new KeyBinding("Back", 31);
    public KeyBinding rightKey = new KeyBinding("Right", 32);
    public KeyBinding jumpKey = new KeyBinding("Jump", 57);
    public KeyBinding inventoryKey = new KeyBinding("Inventory", 23);
    public KeyBinding dropKey = new KeyBinding("Drop", 16);
    public KeyBinding chatKey = new KeyBinding("Chat", 20);
    public KeyBinding fogKey = new KeyBinding("Toggle fog", 33);
    public KeyBinding sneakKey = new KeyBinding("Sneak", 42);
    public KeyBinding[] keyBindings = new KeyBinding[]{this.forwardKey, this.leftKey, this.backKey, this.rightKey, this.jumpKey, this.sneakKey, this.dropKey, this.inventoryKey, this.chatKey, this.fogKey};
    protected Minecraft minecraft;
    private File file;
    public int count = 10;
    public int difficulty = 2;
    public boolean perspective = false;
    public String lastServer = "";

    public GameOptions(Minecraft minecraft, File dir) {
        this.minecraft = minecraft;
        this.file = new File(dir, "options.txt");
        this.load();
    }

    public GameOptions() {
    }

    public String getBoundKeyName(int keyBinding) {
        return this.keyBindings[keyBinding].name + ": " + Keyboard.getKeyName((int)this.keyBindings[keyBinding].keyCode);
    }

    public void bindKey(int keyBinding, int keyCode) {
        this.keyBindings[keyBinding].keyCode = keyCode;
        this.save();
    }

    public void set(int option, float value) {
        if (option == 0) {
            this.musicVolume = value;
            this.minecraft.soundEngine.volumeChanged();
        }
        if (option == 1) {
            this.soundVolume = value;
            this.minecraft.soundEngine.volumeChanged();
        }
        if (option == 3) {
            this.mouseSensitivity = value;
        }
    }

    public void set(int option, int value) {
        if (option == 2) {
            boolean bl = this.invertMouseY = !this.invertMouseY;
        }
        if (option == 4) {
            this.viewDistance = this.viewDistance + value & 3;
        }
        if (option == 5) {
            boolean bl = this.viewBobbing = !this.viewBobbing;
        }
        if (option == 6) {
            this.anaglyph = !this.anaglyph;
            this.minecraft.textureManager.reload();
        }
        if (option == 7) {
            boolean bl = this.fpsLimit = !this.fpsLimit;
        }
        if (option == 8) {
            this.difficulty = this.difficulty + value & 3;
        }
        if (option == 9) {
            this.fancyGraphics = !this.fancyGraphics;
            this.minecraft.worldRenderer.reload();
        }
        this.save();
    }

    public int getInt(int option) {
        if (option == 0) {
            return 1;
        }
        if (option == 1) {
            return 1;
        }
        if (option == 3) {
            return 1;
        }
        return 0;
    }

    public float getFloat(int option) {
        if (option == 0) {
            return this.musicVolume;
        }
        if (option == 1) {
            return this.soundVolume;
        }
        if (option == 3) {
            return this.mouseSensitivity;
        }
        return 0.0f;
    }

    public String getAsString(int option) {
        if (option == 0) {
            return "Music: " + (this.musicVolume > 0.0f ? (int)(this.musicVolume * 100.0f) + "%" : "OFF");
        }
        if (option == 1) {
            return "Sound: " + (this.soundVolume > 0.0f ? (int)(this.soundVolume * 100.0f) + "%" : "OFF");
        }
        if (option == 2) {
            return "Invert mouse: " + (this.invertMouseY ? "ON" : "OFF");
        }
        if (option == 3) {
            if (this.mouseSensitivity == 0.0f) {
                return "Sensitivity: *yawn*";
            }
            if (this.mouseSensitivity == 1.0f) {
                return "Sensitivity: HYPERSPEED!!!";
            }
            return "Sensitivity: " + (int)(this.mouseSensitivity * 200.0f) + "%";
        }
        if (option == 4) {
            return "Render distance: " + RENDER_DISTANCE_SETTINGS[this.viewDistance];
        }
        if (option == 5) {
            return "View bobbing: " + (this.viewBobbing ? "ON" : "OFF");
        }
        if (option == 6) {
            return "3d anaglyph: " + (this.anaglyph ? "ON" : "OFF");
        }
        if (option == 7) {
            return "Limit framerate: " + (this.fpsLimit ? "ON" : "OFF");
        }
        if (option == 8) {
            return "Difficulty: " + DIFFICULTY_SETTINGS[this.difficulty];
        }
        if (option == 9) {
            return "Graphics: " + (this.fancyGraphics ? "FANCY" : "FAST");
        }
        return "";
    }

    public void load() {
        try {
            if (!this.file.exists()) {
                return;
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.file));
            String string = "";
            while ((string = bufferedReader.readLine()) != null) {
                String[] strings = string.split(":");
                if (strings[0].equals("music")) {
                    this.musicVolume = this.parseFloat(strings[1]);
                }
                if (strings[0].equals("sound")) {
                    this.soundVolume = this.parseFloat(strings[1]);
                }
                if (strings[0].equals("mouseSensitivity")) {
                    this.mouseSensitivity = this.parseFloat(strings[1]);
                }
                if (strings[0].equals("invertYMouse")) {
                    this.invertMouseY = strings[1].equals("true");
                }
                if (strings[0].equals("viewDistance")) {
                    this.viewDistance = Integer.parseInt(strings[1]);
                }
                if (strings[0].equals("bobView")) {
                    this.viewBobbing = strings[1].equals("true");
                }
                if (strings[0].equals("anaglyph3d")) {
                    this.anaglyph = strings[1].equals("true");
                }
                if (strings[0].equals("limitFramerate")) {
                    this.fpsLimit = strings[1].equals("true");
                }
                if (strings[0].equals("difficulty")) {
                    this.difficulty = Integer.parseInt(strings[1]);
                }
                if (strings[0].equals("fancyGraphics")) {
                    this.fancyGraphics = strings[1].equals("true");
                }
                if (strings[0].equals("skin")) {
                    this.skin = strings[1];
                }
                if (strings[0].equals("lastServer")) {
                    this.lastServer = strings[1];
                }
                for (int i = 0; i < this.keyBindings.length; ++i) {
                    if (!strings[0].equals("key_" + this.keyBindings[i].name)) continue;
                    this.keyBindings[i].keyCode = Integer.parseInt(strings[1]);
                }
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            System.out.println("Failed to load options");
            exception.printStackTrace();
        }
    }

    private float parseFloat(String s) {
        if (s.equals("true")) {
            return 1.0f;
        }
        if (s.equals("false")) {
            return 0.0f;
        }
        return Float.parseFloat(s);
    }

    public void save() {
        try {
            PrintWriter printWriter = new PrintWriter(new FileWriter(this.file));
            printWriter.println("music:" + this.musicVolume);
            printWriter.println("sound:" + this.soundVolume);
            printWriter.println("invertYMouse:" + this.invertMouseY);
            printWriter.println("mouseSensitivity:" + this.mouseSensitivity);
            printWriter.println("viewDistance:" + this.viewDistance);
            printWriter.println("bobView:" + this.viewBobbing);
            printWriter.println("anaglyph3d:" + this.anaglyph);
            printWriter.println("limitFramerate:" + this.fpsLimit);
            printWriter.println("difficulty:" + this.difficulty);
            printWriter.println("fancyGraphics:" + this.fancyGraphics);
            printWriter.println("skin:" + this.skin);
            printWriter.println("lastServer:" + this.lastServer);
            for (int i = 0; i < this.keyBindings.length; ++i) {
                printWriter.println("key_" + this.keyBindings[i].name + ":" + this.keyBindings[i].keyCode);
            }
            printWriter.close();
        }
        catch (Exception exception) {
            System.out.println("Failed to save options");
            exception.printStackTrace();
        }
    }
}

