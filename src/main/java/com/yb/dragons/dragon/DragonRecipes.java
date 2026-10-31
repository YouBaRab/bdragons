package com.yb.dragons.dragon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The crafting recipes of the mod, as shown in the codex. The real recipes are the JSON files in
 * data/bdragon/recipe (generated from the same lists by tools/gen_assets.py).
 */
public final class DragonRecipes {
    /** The 4 elemental ingredients of each essence (+ 1 glass bottle, shapeless). */
    public static final Map<DragonType, List<Item>> ESSENCES = new EnumMap<>(DragonType.class);

    static {
        ESSENCES.put(DragonType.FIRE, List.of(Items.BLAZE_ROD, Items.MAGMA_CREAM, Items.FIRE_CHARGE, Items.COAL_BLOCK));
        ESSENCES.put(DragonType.ICE, List.of(Items.BLUE_ICE, Items.PACKED_ICE, Items.SNOW_BLOCK, Items.SNOWBALL));
        ESSENCES.put(DragonType.WATER, List.of(Items.PRISMARINE_CRYSTALS, Items.PRISMARINE_SHARD, Items.NAUTILUS_SHELL, Items.KELP));
        ESSENCES.put(DragonType.FOREST, List.of(Items.OAK_SAPLING, Items.MOSS_BLOCK, Items.SWEET_BERRIES, Items.APPLE));
        ESSENCES.put(DragonType.NETHER, List.of(Items.NETHERRACK, Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS, Items.QUARTZ));
        ESSENCES.put(DragonType.END, List.of(Items.ENDER_EYE, Items.ENDER_PEARL, Items.OBSIDIAN, Items.CRYING_OBSIDIAN));
        ESSENCES.put(DragonType.TERRA, List.of(Items.BRICKS, Items.TERRACOTTA, Items.CLAY_BALL, Items.RAW_GOLD));
        ESSENCES.put(DragonType.SUNLIGHT, List.of(Items.GLOWSTONE_DUST, Items.SUNFLOWER, Items.GOLD_INGOT, Items.HONEYCOMB));
        ESSENCES.put(DragonType.STORM, List.of(Items.LIGHTNING_ROD.weathering().unaffected(), Items.COPPER_INGOT, Items.REDSTONE, Items.GUNPOWDER));
        ESSENCES.put(DragonType.MOONLIGHT, List.of(Items.PHANTOM_MEMBRANE, Items.GLOW_BERRIES, Items.QUARTZ, Items.CALCITE));
        ESSENCES.put(DragonType.WITHER, List.of(Items.SOUL_SAND, Items.BONE_BLOCK, Items.COAL_BLOCK, Items.NETHER_BRICK));
    }

    /** Shaped recipes, 3 rows of 3 (null = empty slot). */
    public static final Item[] ALTAR = {
            Items.GOLD_INGOT, Items.DIAMOND, Items.GOLD_INGOT,
            Items.BONE_BLOCK, Items.OBSIDIAN, Items.BONE_BLOCK,
            Items.OBSIDIAN, Items.OBSIDIAN, Items.OBSIDIAN};
    public static final Item[] HORN = {
            null, null, Items.BONE,
            null, Items.GOLD_INGOT, null,
            Items.BONE, null, null};
    /** Shapeless recipes. */
    public static final List<Item> CODEX = List.of(Items.BOOK, Items.BONE, Items.GOLD_INGOT);

    private DragonRecipes() {}
}
