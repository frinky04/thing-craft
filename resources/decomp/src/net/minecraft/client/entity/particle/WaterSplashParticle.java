/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.RainSplashParticle;
import net.minecraft.world.World;

@Environment(value=EnvType.CLIENT)
public class WaterSplashParticle
extends RainSplashParticle {
    public WaterSplashParticle(World world, double d, double e, double f, double g, double h, double i) {
        super(world, d, e, f);
        this.gravity = 0.04f;
        ++this.sprite;
        if (h == 0.0 && (g != 0.0 || i != 0.0)) {
            this.velocityX = g;
            this.velocityY = h + 0.1;
            this.velocityZ = i;
        }
    }
}

