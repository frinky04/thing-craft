/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server.gui;

import java.util.Vector;
import javax.swing.JList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.mob.player.ServerPlayerEntity;
import net.minecraft.util.Tickable;

@Environment(value=EnvType.SERVER)
public class PlayerListGui
extends JList
implements Tickable {
    private MinecraftServer server;
    private int tick = 0;

    public PlayerListGui(MinecraftServer server) {
        this.server = server;
        server.addTickable(this);
    }

    public void tick() {
        if (this.tick++ % 20 == 0) {
            Vector<String> vector = new Vector<String>();
            for (int i = 0; i < this.server.playerManager.players.size(); ++i) {
                vector.add(((ServerPlayerEntity)this.server.playerManager.players.get((int)i)).name);
            }
            this.setListData(vector);
        }
    }
}

