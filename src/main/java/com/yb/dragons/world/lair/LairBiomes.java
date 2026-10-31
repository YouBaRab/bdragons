package com.yb.dragons.world.lair;

import com.yb.dragons.dragon.DragonType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The favourite biomes of every race: a nest of a race can only exist in one of its biomes (chosen by the author).
 * Plains, beaches, caves and the central End island never host a nest.
 */
public final class LairBiomes {
    private static final Map<DragonType, List<ResourceKey<Biome>>> BY_TYPE = new EnumMap<>(DragonType.class);
    private static final Map<ResourceKey<Biome>, DragonType> BY_BIOME = new HashMap<>();

    static {
        // the Nether: fire in the crimson and warped forests, the Nether race in the basalt deltas, wither in the soul sand
        put(DragonType.FIRE, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST);
        put(DragonType.NETHER, Biomes.BASALT_DELTAS);
        put(DragonType.WITHER, Biomes.SOUL_SAND_VALLEY);
        // the End: where the End cities are
        put(DragonType.END, Biomes.END_HIGHLANDS);
        // where the polar bears live: the icebergs and the snowy lands
        put(DragonType.ICE, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES);
        // the oceans and the rivers (the nest floats on the water)
        put(DragonType.WATER, Biomes.OCEAN, Biomes.COLD_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN,
                Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.RIVER);
        // every forest but the swamps
        put(DragonType.FOREST, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                Biomes.DARK_FOREST, Biomes.PALE_GARDEN, Biomes.DAPPLED_FOREST, Biomes.TAIGA, Biomes.SNOWY_TAIGA,
                Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.WINDSWEPT_FOREST, Biomes.GROVE,
                Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.CHERRY_GROVE);
        // the swamps
        put(DragonType.MOONLIGHT, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
        // the savannas and the deserts
        put(DragonType.SUNLIGHT, Biomes.DESERT, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
        // the badlands (mesa)
        put(DragonType.TERRA, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        // the high mountains
        put(DragonType.STORM, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES);
    }

    @SafeVarargs
    private static void put(DragonType type, ResourceKey<Biome>... biomes) {
        BY_TYPE.put(type, List.of(biomes));
        for (ResourceKey<Biome> b : biomes) BY_BIOME.put(b, type);
    }

    private LairBiomes() {}

    /** Race whose lairs can be found in this biome, or null when no dragon lives there. */
    @Nullable
    public static DragonType typeFor(Holder<Biome> biome) {
        return biome.unwrapKey().map(BY_BIOME::get).orElse(null);
    }

    /** The biomes of a race (shown in the codex). */
    public static List<ResourceKey<Biome>> biomesOf(DragonType type) {
        return BY_TYPE.getOrDefault(type, List.of());
    }
}
