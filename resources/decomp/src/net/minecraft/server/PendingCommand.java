/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.source.CommandSource;

@Environment(value=EnvType.SERVER)
public class PendingCommand {
    public final String command;
    public final CommandSource source;

    public PendingCommand(String command, CommandSource source) {
        this.command = command;
        this.source = source;
    }
}

