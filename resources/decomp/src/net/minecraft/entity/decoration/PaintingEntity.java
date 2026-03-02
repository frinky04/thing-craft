/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package net.minecraft.entity.decoration;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class PaintingEntity
extends Entity {
    private int ticksSinceValidation = 0;
    public int dir = 0;
    private int blockX;
    private int blockY;
    private int blockZ;
    public Motive motive;

    public PaintingEntity(World world) {
        super(world);
        this.eyeHeight = 0.0f;
        this.setSize(0.5f, 0.5f);
    }

    /*
     * WARNING - void declaration
     */
    public PaintingEntity(World world, int x, int y, int z, int dir) {
        this(world);
        int i;
        void motives;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        ArrayList<void> arrayList = new ArrayList<void>();
        Motive[] motiveArray = Motive.values();
        int n = ((void)motives).length;
        for (int j = 0; j < i; ++j) {
            void motive = motives[j];
            this.motive = motive;
            this.setDirection(dir);
            if (!this.canSurvive()) continue;
            arrayList.add(motive);
        }
        if (arrayList.size() > 0) {
            this.motive = (Motive)((Object)arrayList.get(this.random.nextInt(arrayList.size())));
        }
        this.setDirection(dir);
    }

    public void setDirection(int dir) {
        this.dir = dir;
        this.lastYaw = this.yaw = (float)(dir * 90);
        float f = this.motive.width;
        float g = this.motive.height;
        float h = this.motive.width;
        if (dir == 0 || dir == 2) {
            h = 0.5f;
        } else {
            f = 0.5f;
        }
        f /= 32.0f;
        g /= 32.0f;
        h /= 32.0f;
        float i = (float)this.blockX + 0.5f;
        float j = (float)this.blockY + 0.5f;
        float k = (float)this.blockZ + 0.5f;
        float l = 0.5625f;
        if (dir == 0) {
            k -= l;
        }
        if (dir == 1) {
            i -= l;
        }
        if (dir == 2) {
            k += l;
        }
        if (dir == 3) {
            i += l;
        }
        if (dir == 0) {
            i -= this.getPositionOffset(this.motive.width);
        }
        if (dir == 1) {
            k += this.getPositionOffset(this.motive.width);
        }
        if (dir == 2) {
            i += this.getPositionOffset(this.motive.width);
        }
        if (dir == 3) {
            k -= this.getPositionOffset(this.motive.width);
        }
        this.setPosition(i, j += this.getPositionOffset(this.motive.height), k);
        float m = -0.00625f;
        this.shape.set(i - f - m, j - g - m, k - h - m, i + f + m, j + g + m, k + h + m);
    }

    private float getPositionOffset(int length) {
        if (length == 32) {
            return 0.5f;
        }
        if (length == 64) {
            return 0.5f;
        }
        return 0.0f;
    }

    public void tick() {
        if (this.ticksSinceValidation++ == 100 && !this.canSurvive()) {
            this.ticksSinceValidation = 0;
            this.remove();
            this.world.addEntity(new ItemEntity(this.world, this.x, this.y, this.z, new ItemStack(Item.PAINTING)));
        }
    }

    public boolean canSurvive() {
        if (this.world.getCollisions(this, this.shape).size() > 0) {
            return false;
        }
        int i = this.motive.width / 16;
        int j = this.motive.height / 16;
        int k = this.blockX;
        int l = this.blockY;
        int m = this.blockZ;
        if (this.dir == 0) {
            k = MathHelper.floor(this.x - (double)((float)this.motive.width / 32.0f));
        }
        if (this.dir == 1) {
            m = MathHelper.floor(this.z - (double)((float)this.motive.width / 32.0f));
        }
        if (this.dir == 2) {
            k = MathHelper.floor(this.x - (double)((float)this.motive.width / 32.0f));
        }
        if (this.dir == 3) {
            m = MathHelper.floor(this.z - (double)((float)this.motive.width / 32.0f));
        }
        l = MathHelper.floor(this.y - (double)((float)this.motive.height / 32.0f));
        for (int n = 0; n < i; ++n) {
            for (int o = 0; o < j; ++o) {
                Material material2;
                if (this.dir == 0 || this.dir == 2) {
                    Material material = this.world.getMaterial(k + n, l + o, this.blockZ);
                } else {
                    material2 = this.world.getMaterial(this.blockX, l + o, m + n);
                }
                if (material2.isSolid()) continue;
                return false;
            }
        }
        List list = this.world.getEntities(this, this.shape);
        for (int p = 0; p < list.size(); ++p) {
            if (!(list.get(p) instanceof PaintingEntity)) continue;
            return false;
        }
        return true;
    }

    public boolean hasCollision() {
        return true;
    }

    public boolean takeDamage(Entity source, int amount) {
        this.remove();
        this.markDamaged();
        this.world.addEntity(new ItemEntity(this.world, this.x, this.y, this.z, new ItemStack(Item.PAINTING)));
        return true;
    }

    public void writeCustomNbt(NbtCompound nbt) {
        nbt.putByte("Dir", (byte)this.dir);
        nbt.putString("Motive", this.motive.name);
        nbt.putInt("TileX", this.blockX);
        nbt.putInt("TileY", this.blockY);
        nbt.putInt("TileZ", this.blockZ);
    }

    public void readCustomNbt(NbtCompound nbt) {
        int i;
        this.dir = nbt.getByte("Dir");
        this.blockX = nbt.getInt("TileX");
        this.blockY = nbt.getInt("TileY");
        this.blockZ = nbt.getInt("TileZ");
        String string = nbt.getString("Motive");
        Motive[] motives = Motive.values();
        int n = motives.length;
        for (int j = 0; j < i; ++j) {
            Motive motive = motives[j];
            if (!motive.name.equals(string)) continue;
            this.motive = motive;
        }
        if (this.motive == null) {
            this.motive = Motive.KEBAB;
        }
        this.setDirection(this.dir);
    }

    public static enum Motive {
        KEBAB("Kebab", 16, 16, 0, 0),
        AZTEC("Aztec", 16, 16, 16, 0),
        ALBAN("Alban", 16, 16, 32, 0),
        AZTEC2("Aztec2", 16, 16, 48, 0),
        BOMB("Bomb", 16, 16, 64, 0),
        PLANT("Plant", 16, 16, 80, 0),
        WASTELAND("Wasteland", 16, 16, 96, 0),
        POOL("Pool", 32, 16, 0, 32),
        COURBET("Courbet", 32, 16, 32, 32),
        SEA("Sea", 32, 16, 64, 32),
        SUNSET("Sunset", 32, 16, 96, 32),
        CREEBET("Creebet", 32, 16, 128, 32),
        WANDERER("Wanderer", 16, 32, 0, 64),
        GRAHAM("Graham", 16, 32, 16, 64),
        MATCH("Match", 32, 32, 0, 128),
        BUST("Bust", 32, 32, 32, 128),
        STAGE("Stage", 32, 32, 64, 128),
        VOID("Void", 32, 32, 96, 128),
        SKULL_AND_ROSES("SkullAndRoses", 32, 32, 128, 128),
        FIGHTERS("Fighters", 64, 32, 0, 96),
        POINTER("Pointer", 64, 64, 0, 192),
        PIGSCENE("Pigscene", 64, 64, 64, 192),
        SKELETON("Skeleton", 64, 48, 192, 64),
        DONKEY_KONG("DonkeyKong", 64, 48, 192, 112);

        public final String name;
        public final int width;
        public final int height;
        public final int u;
        public final int v;

        /*
         * WARNING - Possible parameter corruption
         * WARNING - void declaration
         */
        private Motive(int name, int width, int height) {
            void v;
            void u;
            this.name = (String)name;
            this.width = width;
            this.height = height;
            this.u = u;
            this.v = v;
        }
    }
}

