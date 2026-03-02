/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.block;

import java.util.ArrayList;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BookshelfBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ClayBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DirtBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowingLiquidBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.GlowstoneBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.GravelBlock;
import net.minecraft.block.IceBlock;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LiquidSourceBlock;
import net.minecraft.block.LogBlock;
import net.minecraft.block.MineralBlock;
import net.minecraft.block.MobSpawnerBlock;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.block.NetherrackBlock;
import net.minecraft.block.ObsidianBlock;
import net.minecraft.block.OreBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.PortalBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.SnowLayerBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.block.SpongeBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StoneBlock;
import net.minecraft.block.StoneSlabBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.WheatBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class Block {
    public static final Sounds DEFAULT_SOUNDS = new Sounds("stone", 1.0f, 1.0f);
    public static final Sounds WOOD_SOUNDS = new Sounds("wood", 1.0f, 1.0f);
    public static final Sounds GRAVEL_SOUNDS = new Sounds("gravel", 1.0f, 1.0f);
    public static final Sounds GRASS_SOUNDS = new Sounds("grass", 1.0f, 1.0f);
    public static final Sounds STONE_SOUNDS = new Sounds("stone", 1.0f, 1.0f);
    public static final Sounds METAL_SOUNDS = new Sounds("stone", 1.0f, 1.5f);
    public static final Sounds GLASS_SOUNDS = new Sounds("stone", 1.0f, 1.0f){

        @Environment(value=EnvType.CLIENT)
        public String getBreaking() {
            return "random.glass";
        }
    };
    public static final Sounds CLOTH_SOUNDS = new Sounds("cloth", 1.0f, 1.0f);
    public static final Sounds SAND_SOUNDS = new Sounds("sand", 1.0f, 1.0f){

        @Environment(value=EnvType.CLIENT)
        public String getBreaking() {
            return "step.gravel";
        }
    };
    public static final Block[] BY_ID = new Block[256];
    public static final boolean[] TICKS_RANDOMLY = new boolean[256];
    public static final boolean[] IS_SOLID = new boolean[256];
    public static final boolean[] HAS_BLOCK_ENTITY = new boolean[256];
    public static final int[] OPACITIES = new int[256];
    public static final boolean[] IS_TRANSLUCENT = new boolean[256];
    public static final int[] LIGHT = new int[256];
    public static final Block STONE = new StoneBlock(1, 1).setStrength(1.5f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final GrassBlock GRASS = (GrassBlock)new GrassBlock(2).setStrength(0.6f).setSounds(GRASS_SOUNDS);
    public static final Block DIRT = new DirtBlock(3, 2).setStrength(0.5f).setSounds(GRAVEL_SOUNDS);
    public static final Block COBBLESTONE = new Block(4, 16, Material.STONE).setStrength(2.0f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final Block PLANKS = new Block(5, 4, Material.WOOD).setStrength(2.0f).setBlastResistance(5.0f).setSounds(WOOD_SOUNDS);
    public static final Block SAPLING = new SaplingBlock(6, 15).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final Block BEDROCK = new Block(7, 17, Material.STONE).setStrength(-1.0f).setBlastResistance(6000000.0f).setSounds(STONE_SOUNDS);
    public static final Block FLOWING_WATER = new FlowingLiquidBlock(8, Material.WATER).setStrength(100.0f).setOpacity(3);
    public static final Block WATER = new LiquidSourceBlock(9, Material.WATER).setStrength(100.0f).setOpacity(3);
    public static final Block FLOWING_LAVA = new FlowingLiquidBlock(10, Material.LAVA).setStrength(0.0f).setLight(1.0f).setOpacity(255);
    public static final Block LAVA = new LiquidSourceBlock(11, Material.LAVA).setStrength(100.0f).setLight(1.0f).setOpacity(255);
    public static final Block SAND = new FallingBlock(12, 18).setStrength(0.5f).setSounds(SAND_SOUNDS);
    public static final Block GRAVEL = new GravelBlock(13, 19).setStrength(0.6f).setSounds(GRAVEL_SOUNDS);
    public static final Block GOLD_ORE = new OreBlock(14, 32).setStrength(3.0f).setBlastResistance(5.0f).setSounds(STONE_SOUNDS);
    public static final Block IRON_ORE = new OreBlock(15, 33).setStrength(3.0f).setBlastResistance(5.0f).setSounds(STONE_SOUNDS);
    public static final Block COAL_ORE = new OreBlock(16, 34).setStrength(3.0f).setBlastResistance(5.0f).setSounds(STONE_SOUNDS);
    public static final Block LOG = new LogBlock(17).setStrength(2.0f).setSounds(WOOD_SOUNDS);
    public static final LeavesBlock LEAVES = (LeavesBlock)new LeavesBlock(18, 52).setStrength(0.2f).setOpacity(1).setSounds(GRASS_SOUNDS);
    public static final Block SPONGE = new SpongeBlock(19).setStrength(0.6f).setSounds(GRASS_SOUNDS);
    public static final Block GLASS = new GlassBlock(20, 49, Material.GLASS, false).setStrength(0.3f).setSounds(GLASS_SOUNDS);
    public static final Block LAPIS_ORE = null;
    public static final Block LAPIS_BLOCK = null;
    public static final Block DISPENSER = null;
    public static final Block SANDSTONE = null;
    public static final Block NOTEBLOCK = null;
    public static final Block BED = null;
    public static final Block POWERED_RAIL = null;
    public static final Block DETECTOR_RAIL = null;
    public static final Block STICKY_PISTON = null;
    public static final Block WEB = null;
    public static final Block TALLGRASS = null;
    public static final Block DEADBUSH = null;
    public static final Block PISTON = null;
    public static final Block PISTON_HEAD = null;
    public static final Block WOOL = new Block(35, 64, Material.WOOL).setStrength(0.8f).setSounds(CLOTH_SOUNDS);
    public static final Block MOVING_BLOCK = null;
    public static final PlantBlock YELLOW_FLOWER = (PlantBlock)new PlantBlock(37, 13).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final PlantBlock RED_FLOWER = (PlantBlock)new PlantBlock(38, 12).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final PlantBlock BROWN_MUSHROOM = (PlantBlock)new MushroomPlantBlock(39, 29).setStrength(0.0f).setSounds(GRASS_SOUNDS).setLight(0.125f);
    public static final PlantBlock RED_MUSHROOM = (PlantBlock)new MushroomPlantBlock(40, 28).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final Block GOLD_BLOCK = new MineralBlock(41, 39).setStrength(3.0f).setBlastResistance(10.0f).setSounds(METAL_SOUNDS);
    public static final Block IRON_BLOCK = new MineralBlock(42, 38).setStrength(5.0f).setBlastResistance(10.0f).setSounds(METAL_SOUNDS);
    public static final Block DOUBLE_STONE_SLAB = new StoneSlabBlock(43, true).setStrength(2.0f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final Block STONE_SLAB = new StoneSlabBlock(44, false).setStrength(2.0f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final Block BRICKS = new Block(45, 7, Material.STONE).setStrength(2.0f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final Block TNT = new TntBlock(46, 8).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final Block BOOKSHELF = new BookshelfBlock(47, 35).setStrength(1.5f).setSounds(WOOD_SOUNDS);
    public static final Block MOSSY_COBBLESTONE = new Block(48, 36, Material.STONE).setStrength(2.0f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final Block OBSIDIAN = new ObsidianBlock(49, 37).setStrength(10.0f).setBlastResistance(2000.0f).setSounds(STONE_SOUNDS);
    public static final Block TORCH = new TorchBlock(50, 80).setStrength(0.0f).setLight(0.9375f).setSounds(WOOD_SOUNDS);
    public static final FireBlock FIRE = (FireBlock)new FireBlock(51, 31).setStrength(0.0f).setLight(1.0f).setSounds(WOOD_SOUNDS);
    public static final Block MOB_SPAWNER = new MobSpawnerBlock(52, 65).setStrength(5.0f).setSounds(METAL_SOUNDS);
    public static final Block OAK_STAIRS = new StairsBlock(53, PLANKS);
    public static final Block CHEST = new ChestBlock(54).setStrength(2.5f).setSounds(WOOD_SOUNDS);
    public static final Block REDSTONE_WIRE = new RedstoneWireBlock(55, 84).setStrength(0.0f).setSounds(DEFAULT_SOUNDS);
    public static final Block DIAMOND_ORE = new OreBlock(56, 50).setStrength(3.0f).setBlastResistance(5.0f).setSounds(STONE_SOUNDS);
    public static final Block DIAMOND_BLOCK = new MineralBlock(57, 40).setStrength(5.0f).setBlastResistance(10.0f).setSounds(METAL_SOUNDS);
    public static final Block CRAFTING_TABLE = new CraftingTableBlock(58).setStrength(2.5f).setSounds(WOOD_SOUNDS);
    public static final Block WHEAT = new WheatBlock(59, 88).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final Block FARMLAND = new FarmlandBlock(60).setStrength(0.6f).setSounds(GRAVEL_SOUNDS);
    public static final Block FURNACE = new FurnaceBlock(61, false).setStrength(3.5f).setSounds(STONE_SOUNDS);
    public static final Block LIT_FURNACE = new FurnaceBlock(62, true).setStrength(3.5f).setSounds(STONE_SOUNDS).setLight(0.875f);
    public static final Block STANDING_SIGN = new SignBlock(63, SignBlockEntity.class, true).setStrength(1.0f).setSounds(WOOD_SOUNDS);
    public static final Block WOODEN_DOOR = new DoorBlock(64, Material.WOOD).setStrength(3.0f).setSounds(WOOD_SOUNDS);
    public static final Block LADDER = new LadderBlock(65, 83).setStrength(0.4f).setSounds(WOOD_SOUNDS);
    public static final Block RAIL = new RailBlock(66, 128).setStrength(0.7f).setSounds(METAL_SOUNDS);
    public static final Block STONE_STAIRS = new StairsBlock(67, COBBLESTONE);
    public static final Block WALL_SIGN = new SignBlock(68, SignBlockEntity.class, false).setStrength(1.0f).setSounds(WOOD_SOUNDS);
    public static final Block LEVER = new LeverBlock(69, 96).setStrength(0.5f).setSounds(WOOD_SOUNDS);
    public static final Block STONE_PRESSURE_PLATE = new PressurePlateBlock(70, Block.STONE.sprite, PressurePlateBlock.ActivationRule.MOBS).setStrength(0.5f).setSounds(STONE_SOUNDS);
    public static final Block IRON_DOOR = new DoorBlock(71, Material.IRON).setStrength(5.0f).setSounds(METAL_SOUNDS);
    public static final Block WOODEN_PRESSURE_PLATE = new PressurePlateBlock(72, Block.PLANKS.sprite, PressurePlateBlock.ActivationRule.EVERYTHING).setStrength(0.5f).setSounds(WOOD_SOUNDS);
    public static final Block REDSTONE_ORE = new RedstoneOreBlock(73, 51, false).setStrength(3.0f).setBlastResistance(5.0f).setSounds(STONE_SOUNDS);
    public static final Block LIT_REDSTONE_ORE = new RedstoneOreBlock(74, 51, true).setLight(0.625f).setStrength(3.0f).setBlastResistance(5.0f).setSounds(STONE_SOUNDS);
    public static final Block UNLIT_REDSTONE_TORCH = new RedstoneTorchBlock(75, 115, false).setStrength(0.0f).setSounds(WOOD_SOUNDS);
    public static final Block REDSTONE_TORCH = new RedstoneTorchBlock(76, 99, true).setStrength(0.0f).setLight(0.5f).setSounds(WOOD_SOUNDS);
    public static final Block STONE_BUTTON = new ButtonBlock(77, Block.STONE.sprite).setStrength(0.5f).setSounds(STONE_SOUNDS);
    public static final Block SNOW_LAYER = new SnowLayerBlock(78, 66).setStrength(0.1f).setSounds(CLOTH_SOUNDS);
    public static final Block ICE = new IceBlock(79, 67).setStrength(0.5f).setOpacity(3).setSounds(GLASS_SOUNDS);
    public static final Block SNOW = new SnowBlock(80, 66).setStrength(0.2f).setSounds(CLOTH_SOUNDS);
    public static final Block CACTUS = new CactusBlock(81, 70).setStrength(0.4f).setSounds(CLOTH_SOUNDS);
    public static final Block CLAY = new ClayBlock(82, 72).setStrength(0.6f).setSounds(GRAVEL_SOUNDS);
    public static final Block REEDS = new SugarCaneBlock(83, 73).setStrength(0.0f).setSounds(GRASS_SOUNDS);
    public static final Block JUKEBOX = new JukeboxBlock(84, 74).setStrength(2.0f).setBlastResistance(10.0f).setSounds(STONE_SOUNDS);
    public static final Block FENCE = new FenceBlock(85, 4).setStrength(2.0f).setBlastResistance(5.0f).setSounds(WOOD_SOUNDS);
    public static final Block PUMPKIN = new PumpkinBlock(86, 102, false).setStrength(1.0f).setSounds(WOOD_SOUNDS);
    public static final Block NETHERRACK = new NetherrackBlock(87, 103).setStrength(0.4f).setSounds(STONE_SOUNDS);
    public static final Block SOUL_SAND = new SoulSandBlock(88, 104).setStrength(0.5f).setSounds(SAND_SOUNDS);
    public static final Block GLOWSTONE = new GlowstoneBlock(89, 105, Material.GLASS).setStrength(0.3f).setSounds(GLASS_SOUNDS).setLight(1.0f);
    public static final PortalBlock NETHER_PORTAL = (PortalBlock)new PortalBlock(90, 14).setStrength(-1.0f).setSounds(GLASS_SOUNDS).setLight(0.75f);
    public static final Block LIT_PUMPKIN = new PumpkinBlock(91, 102, true).setStrength(1.0f).setSounds(WOOD_SOUNDS).setLight(1.0f);
    public int sprite;
    public final int id;
    protected float miningTime;
    protected float blastResistance;
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;
    public Sounds sounds = DEFAULT_SOUNDS;
    public float gravity = 1.0f;
    public final Material material;
    public float slipperiness = 0.6f;

    protected Block(int id, Material material) {
        if (BY_ID[id] != null) {
            throw new IllegalArgumentException("Slot " + id + " is already occupied by " + BY_ID[id] + " when adding " + this);
        }
        this.material = material;
        Block.BY_ID[id] = this;
        this.id = id;
        this.setShape(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        Block.IS_SOLID[id] = this.isSolid();
        Block.OPACITIES[id] = this.isSolid() ? 255 : 0;
        Block.IS_TRANSLUCENT[id] = this.isTranslucent();
        Block.HAS_BLOCK_ENTITY[id] = false;
    }

    protected Block(int id, int sprite, Material material) {
        this(id, material);
        this.sprite = sprite;
    }

    protected Block setSounds(Sounds sounds) {
        this.sounds = sounds;
        return this;
    }

    protected Block setOpacity(int opacity) {
        Block.OPACITIES[this.id] = opacity;
        return this;
    }

    protected Block setLight(float light) {
        Block.LIGHT[this.id] = (int)(15.0f * light);
        return this;
    }

    protected Block setBlastResistance(float blastResistance) {
        this.blastResistance = blastResistance * 3.0f;
        return this;
    }

    private boolean isTranslucent() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isCube() {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderType() {
        return 0;
    }

    protected Block setStrength(float strength) {
        this.miningTime = strength;
        if (this.blastResistance < strength * 5.0f) {
            this.blastResistance = strength * 5.0f;
        }
        return this;
    }

    protected void setTicksRandomly(boolean ticksRandomly) {
        Block.TICKS_RANDOMLY[this.id] = ticksRandomly;
    }

    public void setShape(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Environment(value=EnvType.CLIENT)
    public float getBrightness(WorldView world, int x, int y, int z) {
        return world.getBrightness(x, y, z);
    }

    public boolean shouldRenderFace(WorldView world, int x, int y, int z, int face) {
        if (face == 0 && this.minY > 0.0) {
            return true;
        }
        if (face == 1 && this.maxY < 1.0) {
            return true;
        }
        if (face == 2 && this.minZ > 0.0) {
            return true;
        }
        if (face == 3 && this.maxZ < 1.0) {
            return true;
        }
        if (face == 4 && this.minX > 0.0) {
            return true;
        }
        if (face == 5 && this.maxX < 1.0) {
            return true;
        }
        return !world.isSolidBlock(x, y, z);
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(WorldView world, int x, int y, int z, int face) {
        return this.getSprite(face, world.getBlockMetadata(x, y, z));
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(int face, int metadata) {
        return this.getSprite(face);
    }

    public int getSprite(int face) {
        return this.sprite;
    }

    @Environment(value=EnvType.CLIENT)
    public Box getOutlineShape(World world, int x, int y, int z) {
        return Box.fromPool((double)x + this.minX, (double)y + this.minY, (double)z + this.minZ, (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
    }

    public void addCollisions(World world, int x, int y, int z, Box shape, ArrayList collisions) {
        Box box = this.getCollisionShape(world, x, y, z);
        if (box != null && shape.intersects(box)) {
            collisions.add(box);
        }
    }

    public Box getCollisionShape(World world, int x, int y, int z) {
        return Box.fromPool((double)x + this.minX, (double)y + this.minY, (double)z + this.minZ, (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
    }

    public boolean isSolid() {
        return true;
    }

    public boolean canRayTrace(int metadata, boolean allowLiquids) {
        return this.canRayTrace();
    }

    public boolean canRayTrace() {
        return true;
    }

    public void tick(World world, int x, int y, int z, Random random) {
    }

    @Environment(value=EnvType.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
    }

    public void onBroken(World world, int x, int y, int z, int metadata) {
    }

    public void neighborChanged(World world, int x, int y, int z, int neighborBlock) {
    }

    public int getTickRate() {
        return 10;
    }

    public void onAdded(World world, int x, int y, int z) {
    }

    public void onRemoved(World world, int x, int y, int z) {
    }

    public int getBaseDropCount(Random random) {
        return 1;
    }

    public int getDropItem(int metadata, Random random) {
        return this.id;
    }

    public float getMiningSpeed(PlayerEntity player) {
        if (this.miningTime < 0.0f) {
            return 0.0f;
        }
        if (!player.canMineBlock(this)) {
            return 1.0f / this.miningTime / 100.0f;
        }
        return player.getMiningSpeed(this) / this.miningTime / 30.0f;
    }

    public void dropItems(World world, int x, int y, int z, int metadata) {
        this.dropItems(world, x, y, z, metadata, 1.0f);
    }

    public void dropItems(World world, int x, int y, int z, int metadata, float luck) {
        if (world.isMultiplayer) {
            return;
        }
        int i = this.getBaseDropCount(world.random);
        for (int j = 0; j < i; ++j) {
            int k;
            if (world.random.nextFloat() > luck || (k = this.getDropItem(metadata, world.random)) <= 0) continue;
            float f = 0.7f;
            double d = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
            double e = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
            double g = (double)(world.random.nextFloat() * f) + (double)(1.0f - f) * 0.5;
            ItemEntity itemEntity = new ItemEntity(world, (double)x + d, (double)y + e, (double)z + g, new ItemStack(k));
            itemEntity.pickUpDelay = 10;
            world.addEntity(itemEntity);
        }
    }

    public float getBlastResistance(Entity entity) {
        return this.blastResistance / 5.0f;
    }

    public HitResult rayTrace(World world, int x, int y, int z, Vec3d from, Vec3d to) {
        Vec3d vec3d7;
        this.updateShape(world, x, y, z);
        from = from.add(-x, -y, -z);
        to = to.add(-x, -y, -z);
        Vec3d vec3d = from.intermediateWithX(to, this.minX);
        Vec3d vec3d2 = from.intermediateWithX(to, this.maxX);
        Vec3d vec3d3 = from.intermediateWithY(to, this.minY);
        Vec3d vec3d4 = from.intermediateWithY(to, this.maxY);
        Vec3d vec3d5 = from.intermediateWithZ(to, this.minZ);
        Vec3d vec3d6 = from.intermediateWithZ(to, this.maxZ);
        if (!this.containsX(vec3d)) {
            vec3d = null;
        }
        if (!this.containsX(vec3d2)) {
            vec3d2 = null;
        }
        if (!this.containsY(vec3d3)) {
            vec3d3 = null;
        }
        if (!this.containsY(vec3d4)) {
            vec3d4 = null;
        }
        if (!this.containsZ(vec3d5)) {
            vec3d5 = null;
        }
        if (!this.containsZ(vec3d6)) {
            vec3d6 = null;
        }
        Vec3d object = null;
        if (vec3d != null && (object == null || from.distanceTo(vec3d) < from.distanceTo(object))) {
            vec3d7 = vec3d;
        }
        if (vec3d2 != null && (vec3d7 == null || from.distanceTo(vec3d2) < from.distanceTo(vec3d7))) {
            vec3d7 = vec3d2;
        }
        if (vec3d3 != null && (vec3d7 == null || from.distanceTo(vec3d3) < from.distanceTo(vec3d7))) {
            vec3d7 = vec3d3;
        }
        if (vec3d4 != null && (vec3d7 == null || from.distanceTo(vec3d4) < from.distanceTo(vec3d7))) {
            vec3d7 = vec3d4;
        }
        if (vec3d5 != null && (vec3d7 == null || from.distanceTo(vec3d5) < from.distanceTo(vec3d7))) {
            vec3d7 = vec3d5;
        }
        if (vec3d6 != null && (vec3d7 == null || from.distanceTo(vec3d6) < from.distanceTo(vec3d7))) {
            vec3d7 = vec3d6;
        }
        if (vec3d7 == null) {
            return null;
        }
        int i = -1;
        if (vec3d7 == vec3d) {
            i = 4;
        }
        if (vec3d7 == vec3d2) {
            i = 5;
        }
        if (vec3d7 == vec3d3) {
            i = 0;
        }
        if (vec3d7 == vec3d4) {
            i = 1;
        }
        if (vec3d7 == vec3d5) {
            i = 2;
        }
        if (vec3d7 == vec3d6) {
            i = 3;
        }
        return new HitResult(x, y, z, i, vec3d7.add(x, y, z));
    }

    private boolean containsX(Vec3d pos) {
        if (pos == null) {
            return false;
        }
        return pos.y >= this.minY && pos.y <= this.maxY && pos.z >= this.minZ && pos.z <= this.maxZ;
    }

    private boolean containsY(Vec3d pos) {
        if (pos == null) {
            return false;
        }
        return pos.x >= this.minX && pos.x <= this.maxX && pos.z >= this.minZ && pos.z <= this.maxZ;
    }

    private boolean containsZ(Vec3d pos) {
        if (pos == null) {
            return false;
        }
        return pos.x >= this.minX && pos.x <= this.maxX && pos.y >= this.minY && pos.y <= this.maxY;
    }

    public void onExploded(World world, int x, int y, int z) {
    }

    @Environment(value=EnvType.CLIENT)
    public int getRenderLayer() {
        return 0;
    }

    public boolean canBePlaced(World world, int x, int y, int z) {
        int i = world.getBlock(x, y, z);
        return i == 0 || Block.BY_ID[i].material.isLiquid();
    }

    public boolean use(World world, int x, int y, int z, PlayerEntity player) {
        return false;
    }

    public void onSteppedOn(World world, int x, int y, int z, Entity entity) {
    }

    public void updateMetadataOnPlaced(World world, int x, int y, int z, int face) {
    }

    public void startMining(World world, int x, int y, int z, PlayerEntity player) {
    }

    public void applyMaterialDrag(World world, int x, int y, int z, Entity entity, Vec3d velocity) {
    }

    public void updateShape(WorldView world, int x, int y, int z) {
    }

    @Environment(value=EnvType.CLIENT)
    public int getColor(WorldView world, int x, int y, int z) {
        return 0xFFFFFF;
    }

    public boolean hasSignal(WorldView world, int x, int y, int z, int dir) {
        return false;
    }

    public boolean isSignalSource() {
        return false;
    }

    public void onEntityCollision(World world, int x, int y, int z, Entity entity) {
    }

    public boolean hasDirectSignal(World world, int x, int y, int z, int dir) {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public void resetShape() {
    }

    public void afterMinedByPlayer(World world, int x, int y, int z, int metadata) {
        this.dropItems(world, x, y, z, metadata);
    }

    public boolean canSurvive(World world, int x, int y, int z) {
        return true;
    }

    public void onPlaced(World world, int x, int y, int z, MobEntity entity) {
    }

    static {
        for (int i = 0; i < 256; ++i) {
            if (BY_ID[i] == null) continue;
            Item.BY_ID[i] = new BlockItem(i - 256);
        }
    }

    public static class Sounds {
        public final String key;
        public final float volume;
        public final float pitch;

        public Sounds(String key, float volume, float pitch) {
            this.key = key;
            this.volume = volume;
            this.pitch = pitch;
        }

        public float getVolume() {
            return this.volume;
        }

        public float getPitch() {
            return this.pitch;
        }

        @Environment(value=EnvType.CLIENT)
        public String getBreaking() {
            return "step." + this.key;
        }

        public String getStepping() {
            return "step." + this.key;
        }
    }
}

