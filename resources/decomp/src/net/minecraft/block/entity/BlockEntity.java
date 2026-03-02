/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block.entity;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class BlockEntity {
    private static Map ID_TO_TYPE = new HashMap();
    private static Map TYPE_TO_ID = new HashMap();
    public World world;
    public int x;
    public int y;
    public int z;

    private static void register(Class type, String id) {
        if (TYPE_TO_ID.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate id: " + id);
        }
        ID_TO_TYPE.put(id, type);
        TYPE_TO_ID.put(type, id);
    }

    public void readNbt(NbtCompound nbt) {
        this.x = nbt.getInt("x");
        this.y = nbt.getInt("y");
        this.z = nbt.getInt("z");
    }

    public void writeNbt(NbtCompound nbt) {
        String string = (String)TYPE_TO_ID.get(this.getClass());
        if (string == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        }
        nbt.putString("id", string);
        nbt.putInt("x", this.x);
        nbt.putInt("y", this.y);
        nbt.putInt("z", this.z);
    }

    public void tick() {
    }

    public static BlockEntity fromNbt(NbtCompound nbt) {
        BlockEntity blockEntity;
        Object object = null;
        try {
            Class class_ = (Class)ID_TO_TYPE.get(nbt.getString("id"));
            if (class_ != null) {
                blockEntity = (BlockEntity)class_.newInstance();
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        if (blockEntity != null) {
            blockEntity.readNbt(nbt);
        } else {
            System.out.println("Skipping TileEntity with id " + nbt.getString("id"));
        }
        return blockEntity;
    }

    @Environment(value=EnvType.CLIENT)
    public int getBlockMetadata() {
        return this.world.getBlockMetadata(this.x, this.y, this.z);
    }

    public void markDirty() {
        this.world.notifyBlockEntityChanged(this.x, this.y, this.z, this);
    }

    @Environment(value=EnvType.CLIENT)
    public double squaredDistanceTo(double x, double y, double z) {
        double d = (double)this.x + 0.5 - x;
        double e = (double)this.y + 0.5 - y;
        double f = (double)this.z + 0.5 - z;
        return d * d + e * e + f * f;
    }

    @Environment(value=EnvType.CLIENT)
    public Block getBlock() {
        return Block.BY_ID[this.world.getBlock(this.x, this.y, this.z)];
    }

    static {
        BlockEntity.register(FurnaceBlockEntity.class, "Furnace");
        BlockEntity.register(ChestBlockEntity.class, "Chest");
        BlockEntity.register(SignBlockEntity.class, "Sign");
        BlockEntity.register(MobSpawnerBlockEntity.class, "MobSpawner");
    }
}

