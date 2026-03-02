/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.EnvironmentInterface
 *  net.fabricmc.api.EnvironmentInterfaces
 */
package net.minecraft.entity.mob.animal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.api.EnvironmentInterfaces;
import net.minecraft.block.Block;
import net.minecraft.entity.SpawnableEntity;
import net.minecraft.entity.mob.PathFinderMobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@EnvironmentInterfaces(value={@EnvironmentInterface(value=EnvType.SERVER, itf=SpawnableEntity.class)})
public abstract class AnimalEntity
extends PathFinderMobEntity
implements SpawnableEntity {
    public AnimalEntity(World world) {
        super(world);
    }

    protected float getPathfindingFavor(int x, int y, int z) {
        if (this.world.getBlock(x, y - 1, z) == Block.GRASS.id) {
            return 10.0f;
        }
        return this.world.getBrightness(x, y, z) - 0.5f;
    }

    public void writeCustomNbt(NbtCompound nbt) {
        super.writeCustomNbt(nbt);
    }

    public void readCustomNbt(NbtCompound nbt) {
        super.readCustomNbt(nbt);
    }

    public boolean canSpawn() {
        int k;
        int j;
        int i = MathHelper.floor(this.x);
        return this.world.getBlock(i, (j = MathHelper.floor(this.shape.minY)) - 1, k = MathHelper.floor(this.z)) == Block.GRASS.id && this.world.getRawBrightness(i, j, k) > 8 && super.canSpawn();
    }

    public int getAmbientSoundInterval() {
        return 120;
    }
}

