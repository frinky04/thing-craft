/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.item;

import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BoatItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.DoorItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.FoodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.item.PaintingItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.PlaceableItem;
import net.minecraft.item.RedstoneItem;
import net.minecraft.item.SaddleItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SignItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.StewItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.WheatSeedsItem;
import net.minecraft.world.World;

public class Item {
    protected static Random random = new Random();
    public static Item[] BY_ID = new Item[32000];
    public static Item IRON_SHOVEL = new ShovelItem(0, 2).setSprite(82);
    public static Item IRON_PICKAXE = new PickaxeItem(1, 2).setSprite(98);
    public static Item IRON_AXE = new AxeItem(2, 2).setSprite(114);
    public static Item FLINT_AND_STEEL = new FlintAndSteelItem(3).setSprite(5);
    public static Item APPLE = new FoodItem(4, 4).setSprite(10);
    public static Item BOW = new BowItem(5).setSprite(21);
    public static Item ARROW = new Item(6).setSprite(37);
    public static Item COAL = new Item(7).setSprite(7);
    public static Item DIAMOND = new Item(8).setSprite(55);
    public static Item IRON_INGOT = new Item(9).setSprite(23);
    public static Item GOLD_INGOT = new Item(10).setSprite(39);
    public static Item IRON_SWORD = new SwordItem(11, 2).setSprite(66);
    public static Item WOODEN_SWORD = new SwordItem(12, 0).setSprite(64);
    public static Item WOODEN_SHOVEL = new ShovelItem(13, 0).setSprite(80);
    public static Item WOODEN_PICKAXE = new PickaxeItem(14, 0).setSprite(96);
    public static Item WOODEN_AXE = new AxeItem(15, 0).setSprite(112);
    public static Item STONE_SWORD = new SwordItem(16, 1).setSprite(65);
    public static Item STONE_SHOVEL = new ShovelItem(17, 1).setSprite(81);
    public static Item STONE_PICKAXE = new PickaxeItem(18, 1).setSprite(97);
    public static Item STONE_AXE = new AxeItem(19, 1).setSprite(113);
    public static Item DIAMOND_SWORD = new SwordItem(20, 3).setSprite(67);
    public static Item DIAMOND_SHOVEL = new ShovelItem(21, 3).setSprite(83);
    public static Item DIAMOND_PICKAXE = new PickaxeItem(22, 3).setSprite(99);
    public static Item DIAMOND_AXE = new AxeItem(23, 3).setSprite(115);
    public static Item STICK = new Item(24).setSprite(53).setHandheld();
    public static Item BOWL = new Item(25).setSprite(71);
    public static Item MUSHROOM_STEW = new StewItem(26, 10).setSprite(72);
    public static Item GOLDEN_SWORD = new SwordItem(27, 0).setSprite(68);
    public static Item GOLDEN_SHOVEL = new ShovelItem(28, 0).setSprite(84);
    public static Item GOLDEN_PICKAXE = new PickaxeItem(29, 0).setSprite(100);
    public static Item GOLDEN_AXE = new AxeItem(30, 0).setSprite(116);
    public static Item STRING = new Item(31).setSprite(8);
    public static Item FEATHER = new Item(32).setSprite(24);
    public static Item GUNPOWDER = new Item(33).setSprite(40);
    public static Item WOODEN_HOE = new HoeItem(34, 0).setSprite(128);
    public static Item STONE_HOE = new HoeItem(35, 1).setSprite(129);
    public static Item IRON_HOE = new HoeItem(36, 2).setSprite(130);
    public static Item DIAMOND_HOE = new HoeItem(37, 3).setSprite(131);
    public static Item GOLDEN_HOE = new HoeItem(38, 1).setSprite(132);
    public static Item WHEAT_SEEDS = new WheatSeedsItem(39, Block.WHEAT.id).setSprite(9);
    public static Item WHEAT = new Item(40).setSprite(25);
    public static Item BREAD = new FoodItem(41, 5).setSprite(41);
    public static Item LEATHER_HELMET = new ArmorItem(42, 0, 0, 0).setSprite(0);
    public static Item LEATHER_CHESTPLATE = new ArmorItem(43, 0, 0, 1).setSprite(16);
    public static Item LEATHER_LEGGINGS = new ArmorItem(44, 0, 0, 2).setSprite(32);
    public static Item LEATHER_BOOTS = new ArmorItem(45, 0, 0, 3).setSprite(48);
    public static Item CHAINMAIL_HELMET = new ArmorItem(46, 1, 1, 0).setSprite(1);
    public static Item CHAINMAIL_CHESTPLATE = new ArmorItem(47, 1, 1, 1).setSprite(17);
    public static Item CHAINMAIL_LEGGINGS = new ArmorItem(48, 1, 1, 2).setSprite(33);
    public static Item CHAINMAIL_BOOTS = new ArmorItem(49, 1, 1, 3).setSprite(49);
    public static Item IRON_HELMET = new ArmorItem(50, 2, 2, 0).setSprite(2);
    public static Item IRON_CHESTPLATE = new ArmorItem(51, 2, 2, 1).setSprite(18);
    public static Item IRON_LEGGINGS = new ArmorItem(52, 2, 2, 2).setSprite(34);
    public static Item IRON_BOOTS = new ArmorItem(53, 2, 2, 3).setSprite(50);
    public static Item DIAMOND_HELMET = new ArmorItem(54, 3, 3, 0).setSprite(3);
    public static Item DIAMOND_CHESTPLATE = new ArmorItem(55, 3, 3, 1).setSprite(19);
    public static Item DIAMOND_LEGGINGS = new ArmorItem(56, 3, 3, 2).setSprite(35);
    public static Item DIAMOND_BOOTS = new ArmorItem(57, 3, 3, 3).setSprite(51);
    public static Item GOLDEN_HELMET = new ArmorItem(58, 1, 4, 0).setSprite(4);
    public static Item GOLDEN_CHESTPLATE = new ArmorItem(59, 1, 4, 1).setSprite(20);
    public static Item GOLDEN_LEGGINGS = new ArmorItem(60, 1, 4, 2).setSprite(36);
    public static Item GOLDEN_BOOTS = new ArmorItem(61, 1, 4, 3).setSprite(52);
    public static Item FLINT = new Item(62).setSprite(6);
    public static Item PORKCHOP = new FoodItem(63, 3).setSprite(87);
    public static Item COOKED_PORKCHOP = new FoodItem(64, 8).setSprite(88);
    public static Item PAINTING = new PaintingItem(65).setSprite(26);
    public static Item GOLDEN_APPLE = new FoodItem(66, 42).setSprite(11);
    public static Item SIGN = new SignItem(67).setSprite(42);
    public static Item WOODEN_DOOR = new DoorItem(68, Material.WOOD).setSprite(43);
    public static Item BUCKET = new BucketItem(69, 0).setSprite(74);
    public static Item WATER_BUCKET = new BucketItem(70, Block.FLOWING_WATER.id).setSprite(75);
    public static Item LAVA_BUCKET = new BucketItem(71, Block.FLOWING_LAVA.id).setSprite(76);
    public static Item MINECART = new MinecartItem(72, 0).setSprite(135);
    public static Item SADDLE = new SaddleItem(73).setSprite(104);
    public static Item IRON_DOOR = new DoorItem(74, Material.IRON).setSprite(44);
    public static Item REDSTONE = new RedstoneItem(75).setSprite(56);
    public static Item SNOWBALL = new SnowballItem(76).setSprite(14);
    public static Item BOAT = new BoatItem(77).setSprite(136);
    public static Item LEATHER = new Item(78).setSprite(103);
    public static Item MILK_BUCKET = new BucketItem(79, -1).setSprite(77);
    public static Item BRICK = new Item(80).setSprite(22);
    public static Item CLAY = new Item(81).setSprite(57);
    public static Item REEDS = new PlaceableItem(82, Block.REEDS).setSprite(27);
    public static Item PAPER = new Item(83).setSprite(58);
    public static Item BOOK = new Item(84).setSprite(59);
    public static Item SLIMEBALL = new Item(85).setSprite(30);
    public static Item CHEST_MINECART = new MinecartItem(86, 1).setSprite(151);
    public static Item FURNACE_MINECART = new MinecartItem(87, 2).setSprite(167);
    public static Item EGG = new Item(88).setSprite(12);
    public static Item COMPASS = new Item(89).setSprite(54);
    public static Item FISHING_ROD = new FishingRodItem(90).setSprite(69);
    public static Item CLOCK = new Item(91).setSprite(70);
    public static Item GLOWSTONE_DUST = new Item(92).setSprite(73);
    public static Item FISH = new FoodItem(93, 2).setSprite(89);
    public static Item COOKED_FISH = new FoodItem(94, 5).setSprite(90);
    public static Item RECORD_13 = new MusicDiscItem(2000, "13").setSprite(240);
    public static Item RECORD_CAT = new MusicDiscItem(2001, "cat").setSprite(241);
    public final int id;
    protected int maxStackSize = 64;
    protected int maxDamage = 32;
    protected int sprite;
    protected boolean handheld = false;

    protected Item(int id) {
        this.id = 256 + id;
        if (BY_ID[256 + id] != null) {
            System.out.println("CONFLICT @ " + id);
        }
        Item.BY_ID[256 + id] = this;
    }

    public Item setSprite(int sprite) {
        this.sprite = sprite;
        return this;
    }

    @Environment(value=EnvType.CLIENT)
    public int getSprite(ItemStack stack) {
        return this.sprite;
    }

    public boolean useOn(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int face) {
        return false;
    }

    public float getMiningSpeed(ItemStack stack, Block block) {
        return 1.0f;
    }

    public ItemStack startUsing(ItemStack stack, World world, PlayerEntity player) {
        return stack;
    }

    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    public int getMaxDamage() {
        return this.maxDamage;
    }

    public void attack(ItemStack stack, MobEntity target) {
    }

    public void mineBlock(ItemStack stack, int x, int y, int z, int face) {
    }

    public int getAttackDamage(Entity target) {
        return 1;
    }

    public boolean canMineBlock(Block block) {
        return false;
    }

    public void interact(ItemStack stack, MobEntity entity) {
    }

    public Item setHandheld() {
        this.handheld = true;
        return this;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean isHandheld() {
        return this.handheld;
    }

    @Environment(value=EnvType.CLIENT)
    public boolean shouldRotate() {
        return false;
    }
}

