package com.yb.dragons.dragon;

import com.yb.dragons.YBDragons;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Every dragon race of the mod (11). Their 256x256 textures come from Even More Dragons (MIT) and
 * Dragon Mounts: Legacy (GPL-3.0), repainted for some races (volcanic fire, log forest, plain
 * sunlight) and extended with the race features (flame, fins, halo, crowns...).
 *
 * Each race keeps a coherent style: its particles, its breath, its passive powers and the
 * ground transformation of the legendary stage (stage 5) all come from the same element.
 */
public enum DragonType {
    // id, primary colour, secondary colour, aura particle, breath particle, stage-5 ground block, immunities, player selectable
    FIRE("fire", 0xE2541B, 0xFFC93C, ParticleTypes.SMALL_FLAME, ParticleTypes.FLAME, Blocks.MAGMA_BLOCK,
            List.of(DamageTypeTags.IS_FIRE), true),
    ICE("ice", 0x9AD8F5, 0xFFFFFF, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, Blocks.SNOW_BLOCK,
            List.of(DamageTypeTags.IS_FREEZING), true),
    WATER("water", 0x2C6BD6, 0x7FE0FF, ParticleTypes.DRIPPING_WATER, ParticleTypes.SPLASH, Blocks.BRAIN_CORAL_BLOCK,
            List.of(DamageTypeTags.IS_DROWNING), true),
    FOREST("forest", 0x3C8D2F, 0xB5E36B, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.HAPPY_VILLAGER, Blocks.MOSS_BLOCK,
            List.of(), true),
    NETHER("nether", 0x8E1B1B, 0xFF6A00, ParticleTypes.CRIMSON_SPORE, ParticleTypes.SOUL_FIRE_FLAME, Blocks.CRIMSON_NYLIUM,
            List.of(DamageTypeTags.IS_FIRE), true),
    END("end", 0x2B1A3D, 0xCE7BFF, ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL, Blocks.END_STONE,
            List.of(DamageTypeTags.IS_FALL), true),
    TERRA("terra", 0x744700, 0xFFFF32, new DustParticleOptions(0xC9A15A, 1.2F), new DustParticleOptions(0xD8B26E, 1.6F), Blocks.BRICKS,
            List.of(), true),
    SUNLIGHT("sunlight", 0xE74600, 0xFFF3B0, ParticleTypes.WAX_ON, ParticleTypes.END_ROD, Blocks.GOLD_BLOCK,
            List.of(), true),
    STORM("storm", 0x414A4C, 0x3D7BFF, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ELECTRIC_SPARK, Blocks.COPPER_BLOCK.weathering().unaffected(),
            List.of(DamageTypeTags.IS_LIGHTNING), true),
    MOONLIGHT("moonlight", 0x06091C, 0xE8F0FF, ParticleTypes.WHITE_ASH, new DustParticleOptions(0xDDE8FF, 1.3F), Blocks.PURPUR_BLOCK,
            List.of(), true),
    WITHER("wither", 0x1A1C1C, 0x6686FF, ParticleTypes.SMOKE, ParticleTypes.SOUL, Blocks.SOUL_SOIL,
            List.of(DamageTypeTags.WITHER_IMMUNE_TO), true);

    public static final List<DragonType> SELECTABLE = java.util.Arrays.stream(values()).filter(t -> t.selectable).toList();

    private final String id;
    private final int color;
    private final int secondaryColor;
    private final ParticleOptions auraParticle;
    private final ParticleOptions breathParticle;
    private final Block groundBlock;
    private final List<TagKey<DamageType>> immunities;
    private final boolean selectable;
    private final Identifier bodyTexture;
    private final Identifier glowTexture;

    DragonType(String id, int color, int secondaryColor, ParticleOptions auraParticle, ParticleOptions breathParticle,
               Block groundBlock, List<TagKey<DamageType>> immunities, boolean selectable) {
        this.id = id;
        this.color = color;
        this.secondaryColor = secondaryColor;
        this.auraParticle = auraParticle;
        this.breathParticle = breathParticle;
        this.groundBlock = groundBlock;
        this.immunities = immunities;
        this.selectable = selectable;
        this.bodyTexture = YBDragons.id("textures/entity/dragon/" + id + "/body.png");
        this.glowTexture = YBDragons.id("textures/entity/dragon/" + id + "/glow.png");
    }

    public String id() { return id; }
    public int color() { return color; }
    public int secondaryColor() { return secondaryColor; }
    public ParticleOptions auraParticle() { return auraParticle; }
    public ParticleOptions breathParticle() { return breathParticle; }
    public Block groundBlock() { return groundBlock; }
    public List<TagKey<DamageType>> immunities() { return immunities; }
    public boolean selectable() { return selectable; }
    public Identifier bodyTexture() { return bodyTexture; }
    public Identifier glowTexture() { return glowTexture; }

    /** "Dragon de Feu" / "Fire Dragon" (translated). */
    public Component displayName() {
        return Component.translatable("dragon_type.bdragon." + id);
    }

    /** Short description of the element, shown in the grimoire. */
    public Component description() {
        return Component.translatable("dragon_type.bdragon." + id + ".desc");
    }

    public int textColor() {
        // very dark races get their bright secondary colour for readable text
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        return (r + g + b) < 200 ? secondaryColor : color;
    }

    /** Darker colour, readable on the parchment of the codex. */
    public int inkColor() {
        int c = textColor();
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        float lum = 0.299F * r + 0.587F * g + 0.114F * b;
        float f = lum > 120 ? 120F / lum : 1F;
        return ((int) (r * f) << 16) | ((int) (g * f) << 8) | (int) (b * f);
    }

    @Nullable
    public static DragonType byId(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        for (DragonType t : values()) if (t.id.equals(lower)) return t;
        // races removed in 1.2: the players and lairs of old worlds get the closest race
        return switch (lower) {
            case "aurora" -> ICE;
            case "sylphid" -> WATER;
            case "enchant" -> END;
            case "zombie" -> WITHER;
            default -> null;
        };
    }

    public static DragonType byOrdinal(int ordinal) {
        DragonType[] v = values();
        return ordinal >= 0 && ordinal < v.length ? v[ordinal] : FIRE;
    }

    public static ParticleOptions flash(int color) {
        return ColorParticleOption.create(ParticleTypes.FLASH, color);
    }
}
