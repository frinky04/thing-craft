/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.entity;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.entity.decoration.PaintingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.animal.ChickenEntity;
import net.minecraft.entity.mob.animal.CowEntity;
import net.minecraft.entity.mob.animal.PigEntity;
import net.minecraft.entity.mob.animal.SheepEntity;
import net.minecraft.entity.mob.monster.CreeperEntity;
import net.minecraft.entity.mob.monster.GhastEntity;
import net.minecraft.entity.mob.monster.GiantEntity;
import net.minecraft.entity.mob.monster.MonsterEntity;
import net.minecraft.entity.mob.monster.SkeletonEntity;
import net.minecraft.entity.mob.monster.SlimeEntity;
import net.minecraft.entity.mob.monster.SpiderEntity;
import net.minecraft.entity.mob.monster.ZombieEntity;
import net.minecraft.entity.mob.monster.ZombiePigmanEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class Entities {
    private static Map KEY_TO_TYPE = new HashMap();
    private static Map TYPE_TO_KEY = new HashMap();
    private static Map ID_TO_TYPE = new HashMap();
    private static Map TYPE_TO_ID = new HashMap();

    private static void register(Class type, String key, int id) {
        KEY_TO_TYPE.put(key, type);
        TYPE_TO_KEY.put(type, key);
        ID_TO_TYPE.put(id, type);
        TYPE_TO_ID.put(type, id);
    }

    public static Entity createSilently(String key, World world) {
        Entity entity;
        Object object = null;
        try {
            Class class_ = (Class)KEY_TO_TYPE.get(key);
            if (class_ != null) {
                entity = (Entity)class_.getConstructor(World.class).newInstance(world);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        return entity;
    }

    public static Entity create(NbtCompound nbt, World world) {
        Entity entity;
        Object object = null;
        try {
            Class class_ = (Class)KEY_TO_TYPE.get(nbt.getString("id"));
            if (class_ != null) {
                entity = (Entity)class_.getConstructor(World.class).newInstance(world);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        if (entity != null) {
            entity.readNbt(nbt);
        } else {
            System.out.println("Skipping Entity with id " + nbt.getString("id"));
        }
        return entity;
    }

    @Environment(value=EnvType.CLIENT)
    public static Entity create(int id, World world) {
        Entity entity;
        Object object = null;
        try {
            Class class_ = (Class)ID_TO_TYPE.get(id);
            if (class_ != null) {
                entity = (Entity)class_.getConstructor(World.class).newInstance(world);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        if (entity == null) {
            System.out.println("Skipping Entity with id " + id);
        }
        return entity;
    }

    public static int getId(Entity entity) {
        return (Integer)TYPE_TO_ID.get(entity.getClass());
    }

    public static String getKey(Entity entity) {
        return (String)TYPE_TO_KEY.get(entity.getClass());
    }

    static {
        Entities.register(ArrowEntity.class, "Arrow", 10);
        Entities.register(SnowballEntity.class, "Snowball", 11);
        Entities.register(ItemEntity.class, "Item", 1);
        Entities.register(PaintingEntity.class, "Painting", 9);
        Entities.register(MobEntity.class, "Mob", 48);
        Entities.register(MonsterEntity.class, "Monster", 49);
        Entities.register(CreeperEntity.class, "Creeper", 50);
        Entities.register(SkeletonEntity.class, "Skeleton", 51);
        Entities.register(SpiderEntity.class, "Spider", 52);
        Entities.register(GiantEntity.class, "Giant", 53);
        Entities.register(ZombieEntity.class, "Zombie", 54);
        Entities.register(SlimeEntity.class, "Slime", 55);
        Entities.register(GhastEntity.class, "Ghast", 56);
        Entities.register(ZombiePigmanEntity.class, "PigZombie", 57);
        Entities.register(PigEntity.class, "Pig", 90);
        Entities.register(SheepEntity.class, "Sheep", 91);
        Entities.register(CowEntity.class, "Cow", 92);
        Entities.register(ChickenEntity.class, "Chicken", 93);
        Entities.register(PrimedTntEntity.class, "PrimedTnt", 20);
        Entities.register(FallingBlockEntity.class, "FallingSand", 21);
        Entities.register(MinecartEntity.class, "Minecart", 40);
        Entities.register(BoatEntity.class, "Boat", 41);
    }
}

