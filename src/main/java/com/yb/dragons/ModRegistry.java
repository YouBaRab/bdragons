package com.yb.dragons;

import com.yb.dragons.block.DragonAltarBlock;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.AlphaHitbox;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.item.AlphaOrbItem;
import com.yb.dragons.item.AlphaScaleItem;
import com.yb.dragons.item.ChallengeHornItem;
import com.yb.dragons.item.DragonEssenceItem;
import com.yb.dragons.item.DragonGrimoireItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/** All registry objects of the mod. */
public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, YBDragons.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YBDragons.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YBDragons.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YBDragons.MODID);

    // --- blocks -------------------------------------------------------------------------------------------
    public static final RegistryObject<Block> DRAGON_ALTAR = BLOCKS.register("dragon_altar", () -> new DragonAltarBlock(
            BlockBehaviour.Properties.of().setId(BLOCKS.key("dragon_altar"))
                    .mapColor(MapColor.COLOR_BLACK).strength(25.0F, 1200.0F).requiresCorrectToolForDrops()
                    .lightLevel(s -> 10).sound(SoundType.STONE)));

    // --- entities -------------------------------------------------------------------------------------------
    public static final RegistryObject<EntityType<WildDragon>> WILD_DRAGON = ENTITIES.register("wild_dragon",
            () -> EntityType.Builder.<WildDragon>of(WildDragon::new, MobCategory.MONSTER)
                    .sized(2.4F, 1.6F).eyeHeight(1.2F).clientTrackingRange(16).updateInterval(2)
                    .build(ENTITIES.key("wild_dragon")));
    /** Hit zones of the giant alphas (Sylvan Tortoise, Tempest Dragon): invisible, never saved, they follow the player. */
    public static final RegistryObject<EntityType<AlphaHitbox>> ALPHA_HITBOX = ENTITIES.register("alpha_hitbox",
            () -> EntityType.Builder.<AlphaHitbox>of(AlphaHitbox::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).noSave().noSummon().fireImmune().noLootTable().clientTrackingRange(10).updateInterval(1)
                    .build(ENTITIES.key("alpha_hitbox")));

    // --- items ----------------------------------------------------------------------------------------------
    public static final RegistryObject<Item> DRAGON_ALTAR_ITEM = ITEMS.register("dragon_altar",
            () -> new BlockItem(DRAGON_ALTAR.get(), new Item.Properties().setId(ITEMS.key("dragon_altar")).rarity(Rarity.EPIC).useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("dragon_grimoire",
            () -> new DragonGrimoireItem(new Item.Properties().setId(ITEMS.key("dragon_grimoire")).stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DRAGON_SCALE = ITEMS.register("dragon_scale",
            () -> new Item(new Item.Properties().setId(ITEMS.key("dragon_scale")).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CHALLENGE_HORN = ITEMS.register("challenge_horn",
            () -> new ChallengeHornItem(new Item.Properties().setId(ITEMS.key("challenge_horn")).stacksTo(16).rarity(Rarity.RARE)));

    public static final Map<DragonType, RegistryObject<Item>> ESSENCES = new EnumMap<>(DragonType.class);
    /** Alpha Scales (dropped by the rank V dragons) and Orbs of Power (forged at the altar), one of each per race. */
    public static final Map<DragonType, RegistryObject<Item>> ALPHA_SCALES = new EnumMap<>(DragonType.class);
    public static final Map<DragonType, RegistryObject<Item>> ALPHA_ORBS = new EnumMap<>(DragonType.class);

    static {
        for (DragonType type : DragonType.SELECTABLE) {
            String name = "essence_" + type.id();
            ESSENCES.put(type, ITEMS.register(name, () -> new DragonEssenceItem(type,
                    new Item.Properties().setId(ITEMS.key(name)).stacksTo(16).rarity(Rarity.RARE))));
        }
        for (DragonType type : DragonType.SELECTABLE) {
            String scale = "alpha_scale_" + type.id();
            ALPHA_SCALES.put(type, ITEMS.register(scale, () -> new AlphaScaleItem(type,
                    new Item.Properties().setId(ITEMS.key(scale)).stacksTo(16).rarity(Rarity.EPIC).fireResistant())));
            String orb = "alpha_orb_" + type.id();
            ALPHA_ORBS.put(type, ITEMS.register(orb, () -> new AlphaOrbItem(type,
                    new Item.Properties().setId(ITEMS.key(orb)).stacksTo(16).rarity(Rarity.EPIC).fireResistant())));
        }
    }

    // --- creative tab ---------------------------------------------------------------------------------------
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bdragon"))
            .icon(() -> ESSENCES.get(DragonType.FIRE).get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(GRIMOIRE.get());
                output.accept(DRAGON_ALTAR_ITEM.get());
                output.accept(CHALLENGE_HORN.get());
                output.accept(DRAGON_SCALE.get());
                ESSENCES.values().forEach(r -> output.accept(r.get()));
                ALPHA_SCALES.values().forEach(r -> output.accept(r.get()));
                ALPHA_ORBS.values().forEach(r -> output.accept(r.get()));
            }).build());

    private ModRegistry() {}

    public static void register(BusGroup modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        TABS.register(modBus);
    }
}
