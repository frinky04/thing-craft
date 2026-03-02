/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class BoatItem
extends Item {
    public BoatItem(int i) {
        super(i);
        this.maxStackSize = 1;
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        float p;
        float m;
        float o;
        double q;
        float l;
        float f = 1.0f;
        float g = player.lastPitch + (player.pitch - player.lastPitch) * f;
        float h = player.lastYaw + (player.yaw - player.lastYaw) * f;
        double d = player.lastX + (player.x - player.lastX) * (double)f;
        double e = player.lastY + (player.y - player.lastY) * (double)f + 1.62 - (double)player.eyeHeight;
        double i = player.lastZ + (player.z - player.lastZ) * (double)f;
        Vec3d vec3d = Vec3d.fromPool(d, e, i);
        float j = MathHelper.cos(-h * ((float)Math.PI / 180) - (float)Math.PI);
        float k = MathHelper.sin(-h * ((float)Math.PI / 180) - (float)Math.PI);
        float n = k * (l = -MathHelper.cos(-g * ((float)Math.PI / 180)));
        Vec3d vec3d2 = vec3d.add((double)n * (q = 5.0), (double)(o = (m = MathHelper.sin(-g * ((float)Math.PI / 180)))) * q, (double)(p = j * l) * q);
        HitResult hitResult = world.rayTrace(vec3d, vec3d2, true);
        if (hitResult == null) {
            return stack;
        }
        if (hitResult.type == 0) {
            int r = hitResult.x;
            int s = hitResult.y;
            int t = hitResult.z;
            if (!world.isMultiplayer) {
                world.addEntity(new BoatEntity(world, (float)r + 0.5f, (float)s + 1.5f, (float)t + 0.5f));
            }
            --stack.size;
        }
        return stack;
    }
}

