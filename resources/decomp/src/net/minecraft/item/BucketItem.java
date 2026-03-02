/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.animal.CowEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;

public class BucketItem
extends Item {
    private int liquid;

    public BucketItem(int id, int liquid) {
        super(id);
        this.maxStackSize = 1;
        this.maxDamage = 64;
        this.liquid = liquid;
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
        HitResult hitResult = world.rayTrace(vec3d, vec3d2, this.liquid == 0);
        if (hitResult == null) {
            return stack;
        }
        if (hitResult.type == 0) {
            int r = hitResult.x;
            int s = hitResult.y;
            int t = hitResult.z;
            if (!world.canModify(player, r, s, t)) {
                return stack;
            }
            if (this.liquid == 0) {
                if (world.getMaterial(r, s, t) == Material.WATER && world.getBlockMetadata(r, s, t) == 0) {
                    world.setBlock(r, s, t, 0);
                    return new ItemStack(Item.WATER_BUCKET);
                }
                if (world.getMaterial(r, s, t) == Material.LAVA && world.getBlockMetadata(r, s, t) == 0) {
                    world.setBlock(r, s, t, 0);
                    return new ItemStack(Item.LAVA_BUCKET);
                }
            } else {
                if (this.liquid < 0) {
                    return new ItemStack(Item.BUCKET);
                }
                if (hitResult.face == 0) {
                    --s;
                }
                if (hitResult.face == 1) {
                    ++s;
                }
                if (hitResult.face == 2) {
                    --t;
                }
                if (hitResult.face == 3) {
                    ++t;
                }
                if (hitResult.face == 4) {
                    --r;
                }
                if (hitResult.face == 5) {
                    ++r;
                }
                if (world.getBlock(r, s, t) == 0 || !world.getMaterial(r, s, t).isSolid()) {
                    if (world.dimension.yeetsWater && this.liquid == Block.FLOWING_WATER.id) {
                        world.playSound(d + 0.5, e + 0.5, i + 0.5, "random.fizz", 0.5f, 2.6f + (world.random.nextFloat() - world.random.nextFloat()) * 0.8f);
                        for (int u = 0; u < 8; ++u) {
                            world.addParticle("largesmoke", (double)r + Math.random(), (double)s + Math.random(), (double)t + Math.random(), 0.0, 0.0, 0.0);
                        }
                    } else {
                        world.setBlockWithMetadata(r, s, t, this.liquid, 0);
                    }
                    return new ItemStack(Item.BUCKET);
                }
            }
        } else if (this.liquid == 0 && hitResult.entity instanceof CowEntity) {
            return new ItemStack(Item.MILK_BUCKET);
        }
        return stack;
    }
}

