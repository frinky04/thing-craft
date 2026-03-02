/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;

@Environment(value=EnvType.CLIENT)
public class Session {
    public static List CREATIVE_INVENTORY = new ArrayList();
    public String username;
    public String id;
    public String password;

    public Session(String username, String id) {
        this.username = username;
        this.id = id;
    }

    static {
        CREATIVE_INVENTORY.add(Block.STONE);
        CREATIVE_INVENTORY.add(Block.COBBLESTONE);
        CREATIVE_INVENTORY.add(Block.BRICKS);
        CREATIVE_INVENTORY.add(Block.DIRT);
        CREATIVE_INVENTORY.add(Block.PLANKS);
        CREATIVE_INVENTORY.add(Block.LOG);
        CREATIVE_INVENTORY.add(Block.LEAVES);
        CREATIVE_INVENTORY.add(Block.TORCH);
        CREATIVE_INVENTORY.add(Block.STONE_SLAB);
        CREATIVE_INVENTORY.add(Block.GLASS);
        CREATIVE_INVENTORY.add(Block.MOSSY_COBBLESTONE);
        CREATIVE_INVENTORY.add(Block.SAPLING);
        CREATIVE_INVENTORY.add(Block.YELLOW_FLOWER);
        CREATIVE_INVENTORY.add(Block.RED_FLOWER);
        CREATIVE_INVENTORY.add(Block.BROWN_MUSHROOM);
        CREATIVE_INVENTORY.add(Block.RED_MUSHROOM);
        CREATIVE_INVENTORY.add(Block.SAND);
        CREATIVE_INVENTORY.add(Block.GRAVEL);
        CREATIVE_INVENTORY.add(Block.SPONGE);
        CREATIVE_INVENTORY.add(Block.WOOL);
        CREATIVE_INVENTORY.add(Block.COAL_ORE);
        CREATIVE_INVENTORY.add(Block.IRON_ORE);
        CREATIVE_INVENTORY.add(Block.GOLD_ORE);
        CREATIVE_INVENTORY.add(Block.IRON_BLOCK);
        CREATIVE_INVENTORY.add(Block.GOLD_BLOCK);
        CREATIVE_INVENTORY.add(Block.BOOKSHELF);
        CREATIVE_INVENTORY.add(Block.TNT);
        CREATIVE_INVENTORY.add(Block.OBSIDIAN);
        System.out.println(CREATIVE_INVENTORY.size());
    }
}

