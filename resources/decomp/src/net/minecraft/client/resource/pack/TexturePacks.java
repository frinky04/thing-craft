/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resource.pack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.pack.BuiltInTexturePack;
import net.minecraft.client.resource.pack.TexturePack;
import net.minecraft.client.resource.pack.ZippedTexturePack;

@Environment(value=EnvType.CLIENT)
public class TexturePacks {
    private List availablePacks = new ArrayList();
    private TexturePack defaultPack = new BuiltInTexturePack();
    public TexturePack selected;
    private Map availablePacksByKey = new HashMap();
    private Minecraft minecraft;
    private File dir;
    private String selectedName;

    public TexturePacks(Minecraft minecraft, File gameDir) {
        this.minecraft = minecraft;
        this.dir = new File(gameDir, "texturepacks");
        if (!this.dir.exists()) {
            this.dir.mkdirs();
        }
        this.selectedName = minecraft.options.skin;
        this.reload();
        this.selected.open();
    }

    public boolean select(TexturePack pack) {
        if (pack == this.selected) {
            return false;
        }
        this.selected.close();
        this.selectedName = pack.name;
        this.selected = pack;
        this.minecraft.options.skin = this.selectedName;
        this.minecraft.options.save();
        this.selected.open();
        return true;
    }

    public void reload() {
        ArrayList<TexturePack> arrayList = new ArrayList<TexturePack>();
        this.selected = null;
        arrayList.add(this.defaultPack);
        if (this.dir.exists() && this.dir.isDirectory()) {
            int i;
            File[] files;
            File[] files2 = files = this.dir.listFiles();
            int n = files2.length;
            for (int j = 0; j < i; ++j) {
                File file = files2[j];
                if (!file.isFile() || !file.getName().toLowerCase().endsWith(".zip")) continue;
                String string = file.getName() + ":" + file.length() + ":" + file.lastModified();
                try {
                    if (!this.availablePacksByKey.containsKey(string)) {
                        ZippedTexturePack zippedTexturePack = new ZippedTexturePack(file);
                        zippedTexturePack.key = string;
                        this.availablePacksByKey.put(string, zippedTexturePack);
                        ((TexturePack)zippedTexturePack).load(this.minecraft);
                    }
                    TexturePack texturePack2 = (TexturePack)this.availablePacksByKey.get(string);
                    if (texturePack2.name.equals(this.selectedName)) {
                        this.selected = texturePack2;
                    }
                    arrayList.add(texturePack2);
                    continue;
                }
                catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
        }
        if (this.selected == null) {
            this.selected = this.defaultPack;
        }
        this.availablePacks.removeAll(arrayList);
        for (TexturePack texturePack : this.availablePacks) {
            texturePack.unload(this.minecraft);
            this.availablePacksByKey.remove(texturePack.key);
        }
        this.availablePacks = arrayList;
    }

    public List getAvailable() {
        return new ArrayList(this.availablePacks);
    }
}

