package com.yb.dragons;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common configuration (config/bdragon-common.toml). Values are read live through the getters. */
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue TERRAIN_EFFECT = BUILDER
            .comment("Legendary dragons (stage 5) transform the ground under their feet (magma for fire, ice for frost...).")
            .define("terrainEffect", true);
    public static final ForgeConfigSpec.IntValue TERRAIN_REVERT_TICKS = BUILDER
            .comment("Ticks before a transformed block goes back to its original state (20 ticks = 1 second).")
            .defineInRange("terrainRevertTicks", 160, 20, 12000);

    public static final ForgeConfigSpec.BooleanValue LAIRS = BUILDER
            .comment("Wild dragons live in lairs placed in the favourite biomes of their race.")
            .define("dragonLairs", true);
    public static final ForgeConfigSpec.IntValue LAIR_RESPAWN_DAYS = BUILDER
            .comment("Minecraft days before a new dragon settles in a lair whose dragon was defeated.")
            .defineInRange("lairRespawnDays", 3, 1, 100);

    public static final ForgeConfigSpec.BooleanValue GIVE_CODEX = BUILDER
            .comment("Give the Dragon Codex to every player the first time he joins the world.")
            .define("giveCodexOnFirstJoin", true);

    public static final ForgeConfigSpec.BooleanValue BREATH_GRIEFING = BUILDER
            .comment("Fire breath can set blocks on fire.")
            .define("breathGriefing", false);

    public static final ForgeConfigSpec.BooleanValue ALPHA_TERRAFORM = BUILDER
            .comment("The Beam of Life of the alpha of the forest grows a forest (trees, grass, flowers, crops) where it touches",
                    "natural ground. This change of the world is permanent.")
            .define("alphaForestTerraform", true);

    public static final ForgeConfigSpec.BooleanValue ALPHA_NETHER_TERRAFORM = BUILDER
            .comment("The Territory of the alpha of the Nether turns the natural ground around it into the Nether (basalt,",
                    "blackstone, netherrack...) and raises sharp peaks up to the height 145. This change of the world is permanent.")
            .define("alphaNetherTerraform", true);

    public static final ForgeConfigSpec.BooleanValue ALPHA_SUN_DESTROY = BUILDER
            .comment("The sun thrown by the alpha of the sunlight removes every block (but the bedrock) in a ball of 10 blocks",
                    "around its impact, leaving a crater. This change of the world is permanent. Spawn protection is respected.")
            .define("alphaSunDestroy", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
