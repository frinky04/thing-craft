/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.util.crash;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class CrashReport {
    public final String description;
    public final Throwable exception;

    public CrashReport(String description, Throwable exception) {
        this.description = description;
        this.exception = exception;
    }
}

