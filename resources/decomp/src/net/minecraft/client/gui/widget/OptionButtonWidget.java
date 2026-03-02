/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ButtonWidget;

@Environment(value=EnvType.CLIENT)
public class OptionButtonWidget
extends ButtonWidget {
    public OptionButtonWidget(int i, int j, int k, String string) {
        super(i, j, k, 150, 20, string);
    }
}

