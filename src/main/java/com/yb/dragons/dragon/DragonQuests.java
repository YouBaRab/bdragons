package com.yb.dragons.dragon;

import com.yb.dragons.item.DragonEssenceItem;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The ingredient quests that lead to the transformation ritual.
 * <ol>
 *   <li>Les Ossements Anciens (common)</li>
 *   <li>Le Trésor du Dragon (common)</li>
 *   <li>L'Essence Élémentaire: the first essence offered chooses the race forever</li>
 *   <li>Les Offrandes: offerings specific to the chosen race</li>
 * </ol>
 * Then the ritual is performed by using the altar with an empty hand.
 */
public final class DragonQuests {
    public static final int QUEST_COUNT = 4;
    public static final int ESSENCE_QUEST = 2;
    public static final int OFFERING_QUEST = 3;

    public record Requirement(Predicate<ItemStack> matcher, int count, Item icon, @Nullable String tagName) {
        public static Requirement of(Item item, int count) {
            return new Requirement(s -> s.is(item), count, item, null);
        }

        public static Requirement tag(TagKey<Item> tag, Item icon, int count) {
            return new Requirement(s -> s.is(tag), count, icon, tag.location().getPath());
        }

        public static Requirement essence() {
            return new Requirement(s -> s.getItem() instanceof DragonEssenceItem, 1, Items.NETHER_STAR, "essence");
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && matcher.test(stack);
        }

        public Component name() {
            if ("essence".equals(tagName)) return Component.translatable("quest.bdragon.any_essence");
            if (tagName != null) return Component.translatable("quest.bdragon.tag." + tagName);
            return icon.getName(new ItemStack(icon));
        }
    }

    private static final List<Requirement> QUEST_1 = List.of(
            Requirement.of(Items.BONE, 32),
            Requirement.of(Items.LEATHER, 16),
            Requirement.of(Items.GOLD_INGOT, 8));

    private static final List<Requirement> QUEST_2 = List.of(
            Requirement.of(Items.DIAMOND, 4),
            Requirement.of(Items.EMERALD, 4),
            Requirement.of(Items.GOLD_INGOT, 4),
            Requirement.of(Items.IRON_INGOT, 4));

    private static final List<Requirement> QUEST_3 = List.of(Requirement.essence());

    private static final Map<DragonType, List<Requirement>> OFFERINGS = new EnumMap<>(DragonType.class);

    static {
        OFFERINGS.put(DragonType.FIRE, List.of(Requirement.of(Items.MAGMA_BLOCK, 16), Requirement.of(Items.BLAZE_ROD, 8)));
        OFFERINGS.put(DragonType.ICE, List.of(Requirement.of(Items.PACKED_ICE, 32), Requirement.of(Items.SNOW_BLOCK, 16)));
        OFFERINGS.put(DragonType.WATER, List.of(Requirement.of(Items.NAUTILUS_SHELL, 4), Requirement.of(Items.PRISMARINE_SHARD, 16), Requirement.of(Items.KELP, 32)));
        OFFERINGS.put(DragonType.FOREST, List.of(Requirement.tag(ItemTags.LOGS, Items.OAK_LOG, 32), Requirement.tag(ItemTags.SAPLINGS, Items.OAK_SAPLING, 16), Requirement.of(Items.MOSS_BLOCK, 8)));
        OFFERINGS.put(DragonType.NETHER, List.of(Requirement.of(Items.QUARTZ, 16), Requirement.of(Items.NETHER_WART, 8), Requirement.of(Items.GHAST_TEAR, 4)));
        OFFERINGS.put(DragonType.END, List.of(Requirement.of(Items.END_STONE, 16), Requirement.of(Items.CHORUS_FRUIT, 8), Requirement.of(Items.ENDER_EYE, 4)));
        OFFERINGS.put(DragonType.TERRA, List.of(Requirement.of(Items.BRICKS, 32), Requirement.tag(ItemTags.TERRACOTTA, Items.TERRACOTTA, 16), Requirement.of(Items.RAW_GOLD, 8)));
        OFFERINGS.put(DragonType.SUNLIGHT, List.of(Requirement.of(Items.GOLD_BLOCK, 4), Requirement.of(Items.GLOWSTONE_DUST, 16), Requirement.of(Items.SUNFLOWER, 4)));
        OFFERINGS.put(DragonType.STORM, List.of(Requirement.of(Items.COPPER_BLOCK.weathering().unaffected(), 8), Requirement.of(Items.LIGHTNING_ROD.weathering().unaffected(), 4), Requirement.of(Items.REDSTONE, 16)));
        OFFERINGS.put(DragonType.MOONLIGHT, List.of(Requirement.of(Items.PHANTOM_MEMBRANE, 8), Requirement.of(Items.ENDER_PEARL, 4), Requirement.of(Items.CALCITE, 16)));
        OFFERINGS.put(DragonType.WITHER, List.of(Requirement.of(Items.WITHER_SKELETON_SKULL, 1), Requirement.of(Items.SOUL_SAND, 16), Requirement.of(Items.COAL_BLOCK, 8)));
    }

    private DragonQuests() {}

    /** Requirements of the given quest (the offering quest depends on the chosen race). */
    public static List<Requirement> requirements(int questIndex, @Nullable DragonType chosen) {
        return switch (questIndex) {
            case 0 -> QUEST_1;
            case 1 -> QUEST_2;
            case 2 -> QUEST_3;
            case 3 -> chosen == null ? List.of() : OFFERINGS.getOrDefault(chosen, List.of());
            default -> List.of();
        };
    }

    public static Component title(int questIndex) {
        return Component.translatable("quest.bdragon." + (questIndex + 1) + ".title");
    }

    public static Component lore(int questIndex) {
        return Component.translatable("quest.bdragon." + (questIndex + 1) + ".lore");
    }
}
