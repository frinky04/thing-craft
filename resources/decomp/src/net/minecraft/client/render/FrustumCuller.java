/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.FrustumData;
import net.minecraft.util.math.Box;

@Environment(value=EnvType.CLIENT)
public class FrustumCuller
implements Culler {
    private FrustumData frustum = Frustum.getInstance();
    private double offsetX;
    private double offsetY;
    private double offsetZ;

    public void prepare(double offsetX, double offsetY, double offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public boolean intersectsFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.frustum.intersects(minX - this.offsetX, minY - this.offsetY, minZ - this.offsetZ, maxX - this.offsetX, maxY - this.offsetY, maxZ - this.offsetZ);
    }

    public boolean isVisible(Box shape) {
        return this.intersectsFrustum(shape.minX, shape.minY, shape.minZ, shape.maxX, shape.maxY, shape.maxZ);
    }
}

