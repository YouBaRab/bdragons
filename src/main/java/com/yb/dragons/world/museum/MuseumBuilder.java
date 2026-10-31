package com.yb.dragons.world.museum;

import com.yb.dragons.ModRegistry;
import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonQuests;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.world.lair.DragonLair;
import com.yb.dragons.world.lair.LairBiomes;
import com.yb.dragons.world.lair.LairManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the "Bdragon Museum": a guided tour on a flat world, along an avenue going south from
 * the spawn point. Every step has signs (where to go, what to do) and floating panels, buttons to
 * try everything at once (races, forms, stages, fights) and dragons on display.
 *
 * <pre>
 *  spawn plaza   z   0   welcome, game mode, codex, reset
 *  step 1        z  12   left: the real ritual (altar + chests)   right: one button per race
 *  step 2        z  36   left: the 2 forms, the claw wall         right: the 5 stages and the alpha
 *  step 3        z  62   left: powers (glide tower, pool, targets) right: evolution of the fire dragon
 *  step 4        z 126   the 11 races on pedestals
 *  step 5        z 210   left: lairs and map                      right: arena buttons
 *  arena         z 252   challenge arena (glass dome)
 * </pre>
 *
 * The museum is built automatically in a world created with the "Bdragon Museum" world type
 * (flat, 3 layers), or anywhere with /bdragon musee.
 */
public final class MuseumBuilder {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState PLAZA = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState PATH = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState EDGE = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState HALL = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState DARK = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState WALL = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState BAND = Blocks.GILDED_BLACKSTONE.defaultBlockState();

    private final ServerLevel level;
    private final BlockPos origin;
    private final MuseumData data;

    private MuseumBuilder(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
        this.data = MuseumData.get(level);
    }

    // ------------------------------------------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------------------------------------------

    /** World created with the museum world type: build once, at the spawn, and set the world up. */
    public static void onServerStarted(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        MuseumData data = MuseumData.get(overworld);
        if (data.isBuilt() || !isMuseumWorld(overworld)) return;
        // flat world: the grass layer is at minY + 2, the floor of the museum stands on it
        BlockPos origin = new BlockPos(0, overworld.getMinY() + 3, 0);
        build(overworld, origin);
        overworld.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, origin, 0.0F, 0.0F));
        GameRules rules = overworld.getGameRules();
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.SPAWN_MOBS, false, server);
        rules.set(GameRules.SPAWN_MONSTERS, false, server);
        rules.set(GameRules.SPAWN_PHANTOMS, false, server);
        rules.set(GameRules.SPAWN_PATROLS, false, server);
        rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
        rules.set(GameRules.COMMAND_BLOCKS_WORK, true, server);
        rules.set(GameRules.COMMAND_BLOCK_OUTPUT, false, server);
        rules.set(GameRules.KEEP_INVENTORY, true, server);
        rules.set(GameRules.RESPAWN_RADIUS, 0, server);
        // always midday (the 26.x world clock is set through the vanilla command)
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 6000");
        YBDragons.LOGGER.info("Bdragon museum built at the spawn point");
    }

    /** The museum world type: flat, sunflower plains, exactly bedrock + dirt + grass (1 block each). */
    public static boolean isMuseumWorld(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof FlatLevelSource flat)) return false;
        var settings = flat.settings();
        if (!settings.getBiome().is(Biomes.SUNFLOWER_PLAINS)) return false;
        List<FlatLayerInfo> layers = settings.getLayersInfo();
        return layers.size() == 3
                && layers.get(0).getBlockState().is(Blocks.BEDROCK) && layers.get(0).getHeight() == 1
                && layers.get(1).getBlockState().is(Blocks.DIRT) && layers.get(1).getHeight() == 1
                && layers.get(2).getBlockState().is(Blocks.GRASS_BLOCK) && layers.get(2).getHeight() == 1;
    }

    /** Builds the whole museum; {@code origin} is where the visitor stands at the spawn (feet level). */
    public static void build(ServerLevel level, BlockPos origin) {
        MuseumBuilder b = new MuseumBuilder(level, origin);
        b.clear();
        b.plaza();
        b.avenue();
        b.step1();
        b.step2();
        b.step3();
        b.step4();
        b.step5();
        b.arena();
        b.data.setBuilt(origin);
    }

    // ------------------------------------------------------------------------------------------------------
    // Spawn plaza
    // ------------------------------------------------------------------------------------------------------

    private void plaza() {
        fill(-8, -1, -8, 8, -1, 8, PLAZA);
        for (int i = -8; i <= 8; i++) {
            set(i, -1, -8, DARK);
            set(i, -1, 8, DARK);
            set(-8, -1, i, DARK);
            set(8, -1, i, DARK);
        }
        post(-8, -8);
        post(8, -8);
        panel(0, 2.1, 5.5, "welcome", 220);
        standingSign(-2, 0, 7, 8, sign("welcome"));
        standingSign(2, 0, 7, 8, sign("codex"));

        // west kiosk: game mode
        fill(-8, 0, -2, -8, 2, 2, BRICKS);
        button(-8, 1, -1, Direction.EAST, "gamemode survival @p", sign("survival"));
        button(-8, 1, 1, Direction.EAST, "gamemode creative @p", sign("creative"));
        // east kiosk: codex and reset
        fill(8, 0, -2, 8, 2, 2, BRICKS);
        button(8, 1, -1, Direction.WEST, "give @p bdragon:dragon_grimoire", sign("give_codex"));
        button(8, 1, 1, Direction.WEST, "bdragon reset @p", sign("reset"));
    }

    private void avenue() {
        fill(-3, -1, 9, 3, -1, 235, PATH);
        fill(-4, -1, 9, -4, -1, 235, EDGE);
        fill(4, -1, 9, 4, -1, 235, EDGE);
        for (int z = 20; z <= 228; z += 16) {
            post(-4, z);
            post(4, z);
        }
    }

    /** Start of a step: a golden band across the avenue, a floating title and two sign posts. */
    private void stepHeader(int z, String step, String leftSign, String rightSign) {
        fill(-3, -1, z, 3, -1, z, BAND);
        panel(0, 3.2, z + 0.5, step, 200);
        post(-4, z);
        post(4, z);
        wallSign(-4, 1, z - 1, Direction.NORTH, sign(leftSign));
        wallSign(4, 1, z - 1, Direction.NORTH, sign(rightSign));
    }

    // ------------------------------------------------------------------------------------------------------
    // Step 1: becoming a dragon
    // ------------------------------------------------------------------------------------------------------

    private void step1() {
        stepHeader(12, "step1", "step1_left", "step1_right");

        // left: the real ritual
        fill(-17, -1, 13, -5, -1, 32, HALL);
        fill(-17, 0, 13, -17, 3, 32, WALL);
        set(-10, 0, 22, ModRegistry.DRAGON_ALTAR.get().defaultBlockState());
        standingSign(-8, 0, 22, 12, sign("altar"));
        panel(-10.5, 2.4, 22.5, "ritual", 200);

        chest(-16, 15, "q1", stacks(Items.BONE, 64, Items.LEATHER, 32, Items.GOLD_INGOT, 16));
        chest(-16, 17, "q2", stacks(Items.DIAMOND, 8, Items.EMERALD, 8, Items.GOLD_INGOT, 8, Items.IRON_INGOT, 8));
        List<ItemStack> essences = new ArrayList<>();
        for (DragonType t : DragonType.SELECTABLE) essences.add(new ItemStack(ModRegistry.ESSENCES.get(t).get()));
        chest(-16, 19, "q3", essences);
        List<DragonType> all = DragonType.SELECTABLE;
        chest(-16, 21, "q4a", offerings(all.subList(0, 8)));
        chest(-16, 23, "q4b", offerings(all.subList(8, all.size())));
        set(-16, 0, 25, Blocks.CRAFTING_TABLE.defaultBlockState());
        wallSign(-16, 1, 25, Direction.EAST, sign("table"));
        // the forge of the orbs of power: 4 alpha scales of every race and the Nether Stars
        chest(-16, 28, "alpha_chest", alphaOfferings());

        // right: one button per race. Two command blocks never touch (a powered command block would also start its
        // neighbours, and the visitor would become the last race of the row): the buttons go in zigzag, one low,
        // one high, so there is always at least one block between two command blocks.
        fill(5, -1, 13, 17, -1, 32, HALL);
        fill(17, 0, 13, 17, 3, 32, WALL);
        panel(11.5, 2.4, 22.5, "shortcut", 200);
        int z = 14;
        for (DragonType t : DragonType.SELECTABLE) {
            button(17, (z - 14) % 2 == 0 ? 1 : 3, z, Direction.WEST, "bdragon transform " + t.id() + " @p", new Component[] {
                    key("sign.become.1"), name(t, 1), name(t, 2), Component.empty()});
            z++;
        }
        button(17, 1, 30, Direction.WEST, "bdragon reset @p", sign("reset"));
    }

    private static List<ItemStack> stacks(Object... itemsAndCounts) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i + 1 < itemsAndCounts.length; i += 2) list.add(new ItemStack((Item) itemsAndCounts[i], (Integer) itemsAndCounts[i + 1]));
        return list;
    }

    private static List<ItemStack> alphaOfferings() {
        List<ItemStack> list = new ArrayList<>();
        for (DragonType t : DragonType.SELECTABLE) list.add(new ItemStack(ModRegistry.ALPHA_SCALES.get(t).get(), DragonAlpha.SCALES_PER_ORB));
        list.add(new ItemStack(Items.NETHER_STAR, DragonType.SELECTABLE.size()));
        return list;
    }

    private static List<ItemStack> offerings(List<DragonType> types) {
        List<ItemStack> list = new ArrayList<>();
        for (DragonType t : types) {
            for (DragonQuests.Requirement r : DragonQuests.requirements(DragonQuests.OFFERING_QUEST, t)) {
                list.add(new ItemStack(r.icon(), r.count()));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------------------------------------------
    // Step 2: forms and stages
    // ------------------------------------------------------------------------------------------------------

    private void step2() {
        stepHeader(36, "step2", "step2_left", "step2_right");

        fill(-17, -1, 37, -5, -1, 58, HALL);
        fill(-17, 0, 37, -17, 3, 58, WALL);
        panel(-10.5, 2.4, 47.5, "forms", 210);
        button(-17, 1, 41, Direction.EAST, "bdragon form hybrid @p", sign("form_hybrid"));
        button(-17, 1, 44, Direction.EAST, "bdragon form dragon @p", sign("form_dragon"));
        // claws of the hybrid: a wall of blocks to dig with the bare hand (silk touch), and a button to rebuild it
        clawWall();
        standingSign(-8, 0, 50, 12, sign("claws"));
        button(-17, 1, 47, Direction.EAST, "bdragon musee mur", sign("claws_refill"));

        // the right hall goes on to z 61: room for the button of the alpha of the storm
        fill(5, -1, 37, 17, -1, 61, HALL);
        fill(17, 0, 37, 17, 3, 61, WALL);
        panel(11.5, 2.4, 47.5, "stages", 210);
        String[] roman = {"I", "II", "III", "IV", "V"};
        String[] stages = {"hatchling", "juvenile", "adult", "elder", "legendary"};
        for (int s = 1; s <= 5; s++) {
            button(17, 1, 37 + s * 3, Direction.WEST, "bdragon stage " + s + " @p", new Component[] {
                    key("sign.stage.1"), key("sign.stage.2", roman[s - 1]).withStyle(ChatFormatting.GOLD),
                    Component.translatable("dragon_stage.bdragon." + stages[s - 1]), Component.empty()});
        }
        // after the stage V: the alpha (plays the awakening ritual and its cinematic)
        button(17, 1, 55, Direction.WEST, "bdragon alpha @p", sign("alpha"));
        // the alpha of the forest, whatever the race of the visitor: the Sylvan Tortoise
        button(17, 1, 57, Direction.WEST, "bdragon alpha race forest @p", sign("alpha_forest"));
        // the alpha of the storm, whatever the race of the visitor: the Tempest Dragon
        button(17, 1, 59, Direction.WEST, "bdragon alpha race storm @p", sign("alpha_storm"));
        // the alpha of the Nether, whatever the race of the visitor: the Demon of the Nether
        button(17, 1, 61, Direction.WEST, "bdragon alpha race nether @p", sign("alpha_nether"));
        panel(11.5, 2.4, 55.5, "alpha", 210);
    }

    /** Blocks of the claw wall, row by row (bottom first). */
    private static final Block[][] CLAW_WALL = {
            {Blocks.STONE, Blocks.IRON_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE, Blocks.OAK_LOG},
            {Blocks.DIRT, Blocks.GOLD_ORE, Blocks.EMERALD_ORE, Blocks.GLASS, Blocks.GRAVEL},
            {Blocks.OBSIDIAN, Blocks.COAL_ORE, Blocks.LAPIS_ORE, Blocks.ICE, Blocks.BOOKSHELF}};

    private void clawWall() {
        for (int y = 0; y < CLAW_WALL.length; y++) {
            for (int i = 0; i < CLAW_WALL[y].length; i++) set(-15 + i, y, 55, CLAW_WALL[y][i].defaultBlockState());
        }
    }

    /** Rebuilds the claw wall of the museum whose origin is {@code origin} (button of step 2). */
    public static void rebuildClawWall(ServerLevel level, BlockPos origin) {
        new MuseumBuilder(level, origin).clawWall();
    }

    // ------------------------------------------------------------------------------------------------------
    // Step 3: powers (left) and the evolution of the fire dragon (right)
    // ------------------------------------------------------------------------------------------------------

    private void step3() {
        stepHeader(62, "step3", "step3_left", "step3_right");

        // --- left: powers ---
        fill(-20, -1, 63, -5, -1, 122, HALL);
        panel(-8.5, 2.6, 66.5, "powers", 220);

        // glide tower with a ladder on its east face and a railed platform on top
        int top = 30;
        fill(-14, 0, 70, -12, top - 1, 72, WALL);
        for (int y = 0; y <= top; y++) {
            set(-11, y, 71, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, mirror(Direction.EAST)));
        }
        fill(-16, top, 68, -9, top, 74, EDGE);
        set(-11, top, 71, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, mirror(Direction.EAST)));
        set(-12, top, 71, WALL);
        List<BlockPos> rail = new ArrayList<>();
        for (int x = -16; x <= -9; x++) {
            rail.add(at(x, top + 1, 68));
            rail.add(at(x, top + 1, 74));
        }
        for (int zz = 69; zz <= 73; zz++) rail.add(at(-16, top + 1, zz));
        fences(rail);
        standingSign(-10, 0, 74, 12, sign("tower"));
        standingSign(-13, top + 1, 71, 12, sign("tower_top"));

        // pool: an ice dragon of stage V freezes it
        fill(-17, -2, 84, -11, -1, 90, Blocks.WATER.defaultBlockState());
        standingSign(-9, 0, 87, 12, sign("pool"));

        // training targets in a fenced pen
        List<BlockPos> pen = new ArrayList<>();
        for (int x = -18; x <= -11; x++) {
            pen.add(at(x, 0, 99));
            pen.add(at(x, 0, 109));
        }
        for (int zz = 100; zz <= 108; zz++) {
            pen.add(at(-18, 0, zz));
            pen.add(at(-11, 0, zz));
        }
        fences(pen);
        for (int i = 0; i < 3; i++) data.addHusk(ax(-14.5), ay(0), az(101.5 + i * 3));
        standingSign(-9, 0, 102, 12, sign("targets"));
        fill(-9, 0, 105, -9, 2, 107, BRICKS);
        BlockPos husk = at(-14, 0, 104);
        button(-9, 1, 106, Direction.EAST, "summon minecraft:husk " + husk.getX() + " " + husk.getY() + " " + husk.getZ()
                + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}", sign("new_targets"));

        // --- right: evolution of the fire dragon ---
        fill(5, -1, 63, 21, -1, 122, HALL);
        panel(8.5, 2.6, 64.5, "evolution", 210);
        int[] zs = {69, 80, 91, 103, 116};
        for (int r = 1; r <= 5; r++) {
            int size = r >= 4 ? 3 : 2;
            pedestal(13, zs[r - 1], size, r == 5 ? DragonType.FIRE.groundBlock().defaultBlockState() : DARK);
            data.addDragon(ax(13.5), ay(1), az(zs[r - 1] + 0.5), yaw(90.0F), DragonType.FIRE, r);
            standingSign(6, 0, zs[r - 1], 4, sign("evo" + r));
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Step 4: the races
    // ------------------------------------------------------------------------------------------------------

    private void step4() {
        stepHeader(126, "step4", "step4_left", "step4_right");
        fill(-18, -1, 127, -5, -1, 206, HALL);
        fill(5, -1, 127, 18, -1, 206, HALL);
        int i = 0;
        for (DragonType t : DragonType.SELECTABLE) {
            boolean left = i % 2 == 0;
            int z = 132 + (i / 2) * 10;
            int x = left ? -12 : 12;
            pedestal(x, z, 2, t.groundBlock().defaultBlockState());
            data.addDragon(ax(x + 0.5), ay(1), az(z + 0.5), yaw(left ? -90.0F : 90.0F), t, 3);
            standingSign(left ? -6 : 6, 0, z, left ? 12 : 4, new Component[] {
                    name(t, 1).copy().withStyle(s -> s.withColor(t.textColor())), name(t, 2).copy().withStyle(s -> s.withColor(t.textColor())),
                    key("sign.rank", "III"), key("sign.adult")});
            data.addText(ax(x + 0.5), ay(4.6), az(z + 0.5), MuseumEnglish.of(raceText(t)), 190);
            i++;
        }
    }

    private static Component raceText(DragonType t) {
        MutableComponent biomes = Component.empty();
        List<ResourceKey<Biome>> list = LairBiomes.biomesOf(t);
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) biomes.append(", ");
            var id = list.get(i).identifier();
            biomes.append(Component.translatable("biome." + id.getNamespace() + "." + id.getPath()));
        }
        return Component.empty()
                .append(t.displayName().copy().withStyle(s -> s.withColor(t.textColor()).withBold(true)))
                .append("\n")
                .append(Component.translatable("dragon_type.bdragon." + t.id() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                .append("\n")
                .append(Component.translatable("museum.bdragon.panel.race.lairs", biomes).withStyle(ChatFormatting.YELLOW))
                .append("\n")
                .append(Component.translatable("dragon_type.bdragon." + t.id() + ".powers"));
    }

    // ------------------------------------------------------------------------------------------------------
    // Step 5: lairs (left) and the arena buttons (right)
    // ------------------------------------------------------------------------------------------------------

    private void step5() {
        stepHeader(210, "step5", "step5_left", "step5_right");

        // --- left: lairs, demo lairs around the museum (a flat world has no dragon biome) ---
        fill(-18, -1, 211, -5, -1, 232, HALL);
        panel(-11.5, 2.6, 214.5, "lairs", 220);
        LairManager lairs = LairManager.get(level);
        Object[][] demo = {{170, 150, DragonType.TERRA, 2}, {-190, 170, DragonType.ICE, 3}, {40, -230, DragonType.STORM, 4}};
        int z = 218;
        for (Object[] d : demo) {
            DragonType type = (DragonType) d[2];
            int rank = (Integer) d[3];
            DragonLair lair = lairs.addDemoLair(level, origin.getX() + (Integer) d[0], origin.getZ() + (Integer) d[1], type, rank);
            standingSign(-7, 0, z, 12, new Component[] {
                    key("sign.lair.1"),
                    key("sign.lair.2", Component.translatable("museum.bdragon.short." + type.id()), roman(rank)).withStyle(s -> s.withColor(type.textColor())),
                    key("sign.lair.3", lair.x), key("sign.lair.4", lair.z)});
            z += 2;
        }
        standingSign(-7, 0, 226, 12, sign("map"));
        chest(-7, 229, Direction.EAST, null, horns());
        standingSign(-7, 0, 230, 12, sign("horns"));

        // --- right: arena buttons ---
        fill(5, -1, 211, 8, -1, 232, HALL);
        fill(8, 0, 213, 8, 3, 231, WALL);
        BlockPos center = at(0, 0, 252);
        Object[][] fights = {{DragonType.FOREST, 1}, {DragonType.TERRA, 2}, {DragonType.ICE, 3}, {DragonType.STORM, 4}, {DragonType.FIRE, 5}};
        z = 214;
        for (Object[] f : fights) {
            DragonType type = (DragonType) f[0];
            int rank = (Integer) f[1];
            button(8, 1, z, Direction.WEST, "execute positioned " + center.getX() + " " + center.getY() + " " + center.getZ()
                    + " run bdragon summon " + type.id() + " " + rank, new Component[] {
                    key("sign.arena.1"), key("sign.arena.2", roman(rank)).withStyle(ChatFormatting.GOLD),
                    Component.translatable("museum.bdragon.short." + type.id()).withStyle(s -> s.withColor(type.textColor())), Component.empty()});
            z += 3;
        }
        button(8, 1, 229, Direction.WEST, "effect give @p minecraft:instant_health 1 10 true", sign("heal"));
        button(8, 1, 231, Direction.WEST, "kill @e[type=bdragon:wild_dragon,x=" + center.getX() + ",y=" + center.getY() + ",z="
                + center.getZ() + ",distance=..30]", sign("arena_clear"));
        panel(0, 3.2, 233.5, "arena", 220);
    }

    private static List<ItemStack> horns() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModRegistry.CHALLENGE_HORN.get(), 16));
        list.add(new ItemStack(ModRegistry.DRAGON_SCALE.get(), 32));
        return list;
    }

    // ------------------------------------------------------------------------------------------------------
    // Arena: a glass dome, entrance on the north side
    // ------------------------------------------------------------------------------------------------------

    private void arena() {
        int cz = 252, r = 16, h = 14;
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < r - 0.5) set(dx, -1, cz + dz, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                if (d < r + 0.5) set(dx, h, cz + dz, Blocks.GLASS.defaultBlockState());
                if (d >= r - 0.5 && d < r + 0.5) {
                    for (int y = 0; y < h; y++) {
                        boolean door = dz < 0 && Math.abs(dx) <= 1 && y <= 2;
                        set(dx, y, cz + dz, door ? AIR : Blocks.GLASS.defaultBlockState());
                    }
                    set(dx, -1, cz + dz, DARK);
                }
            }
        }
        // small ring in the middle: where the dragons appear
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= 2.5 && d < 3.5) set(dx, -1, cz + dz, BAND);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------

    /*
     * The layout is written with "left" = negative x. Walking south, the left hand is the east (+x),
     * so the local x is mirrored: every position, facing and rotation goes through the helpers below.
     */
    private BlockPos at(int x, int y, int z) {
        return origin.offset(-x, y, z);
    }

    private double ax(double x) { return origin.getX() - x; }

    private static Direction mirror(Direction d) {
        return d.getAxis() == Direction.Axis.X ? d.getOpposite() : d;
    }

    /** Yaw of an entity facing the given local direction (90 = local west). */
    private static float yaw(float localYaw) {
        return -localYaw;
    }
    private double ay(double y) { return origin.getY() + y; }
    private double az(double z) { return origin.getZ() + z; }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(at(x, y, z), state, FLAGS);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, state);
    }

    /** Empties the volume of the museum (only useful when it is built in a normal world). */
    private void clear() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -22; x <= 22; x++) {
            for (int z = -10; z <= 270; z++) {
                for (int y = 0; y <= 33; y++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, AIR, FLAGS);
                }
                pos.set(origin.getX() + x, origin.getY() - 1, origin.getZ() + z);
                if (level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), FLAGS);
            }
        }
    }

    /** A lamp post: two blocks and a lantern. */
    private void post(int x, int z) {
        set(x, 0, z, BRICKS);
        set(x, 1, z, BRICKS);
        set(x, 2, z, Blocks.LANTERN.defaultBlockState());
    }

    /** A square pedestal of (2*half+1) blocks, its centre (3x3) made of {@code top}. */
    private void pedestal(int cx, int cz, int half, BlockState top) {
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                boolean centre = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                set(cx + dx, 0, cz + dz, centre ? top : DARK);
            }
        }
    }

    private void fences(List<BlockPos> positions) {
        for (BlockPos p : positions) {
            BlockState s = Blocks.DARK_OAK_FENCE.defaultBlockState()
                    .setValue(CrossCollisionBlock.NORTH, positions.contains(p.north()))
                    .setValue(CrossCollisionBlock.SOUTH, positions.contains(p.south()))
                    .setValue(CrossCollisionBlock.EAST, positions.contains(p.east()))
                    .setValue(CrossCollisionBlock.WEST, positions.contains(p.west()));
            level.setBlock(p, s, FLAGS);
        }
    }

    /** A command block in a wall, a button on its face and a sign above it. */
    private void button(int wx, int y, int wz, Direction facing, String command, Component[] lines) {
        BlockPos cmd = at(wx, y, wz);
        level.setBlock(cmd, Blocks.COMMAND_BLOCK.defaultBlockState(), FLAGS);
        if (level.getBlockEntity(cmd) instanceof CommandBlockEntity be) {
            be.getCommandBlock().setCommand(command);
            be.setChanged();
        }
        level.setBlock(cmd.relative(mirror(facing)), Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACING, mirror(facing)).setValue(ButtonBlock.FACE, AttachFace.WALL), FLAGS);
        BlockPos behindSign = cmd.above();
        if (level.getBlockState(behindSign).isAir()) level.setBlock(behindSign, BRICKS, FLAGS);
        wallSign(wx + facing.getStepX(), y + 1, wz + facing.getStepZ(), facing, lines);
    }

    private void chest(int x, int z, String signKey, List<ItemStack> items) {
        chest(x, z, Direction.EAST, signKey, items);
    }

    private void chest(int x, int z, Direction facing, String signKey, List<ItemStack> items) {
        BlockPos pos = at(x, 0, z);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, mirror(facing)), FLAGS);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            for (int i = 0; i < items.size() && i < chest.getContainerSize(); i++) chest.setItem(i, items.get(i));
            chest.setChanged();
        }
        if (signKey != null) wallSign(x, 1, z, facing, sign(signKey));
    }

    private void wallSign(int x, int y, int z, Direction facing, Component[] lines) {
        BlockPos pos = at(x, y, z);
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, mirror(facing)), FLAGS);
        writeSign(pos, lines);
    }

    /** @param rotation 0 = facing south, 4 = west, 8 = north, 12 = east */
    private void standingSign(int x, int y, int z, int rotation, Component[] lines) {
        BlockPos pos = at(x, y, z);
        // mirrored rotation: east (12) <-> west (4), north and south unchanged
        level.setBlock(pos, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, (16 - rotation) % 16), FLAGS);
        writeSign(pos, lines);
    }

    private void writeSign(BlockPos pos, Component[] lines) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        List<Component> list = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) list.add(i < lines.length && lines[i] != null ? MuseumEnglish.of(lines[i]) : Component.empty());
        SignText text = new SignText(list, list, DyeColor.WHITE, true);
        sign.setText(text, SignTextSlot.FRONT);
        sign.setText(text, SignTextSlot.BACK);
        sign.setWaxed(true);
    }

    /** The 4 lines of a sign from the language files (museum.bdragon.sign.ID.1 to .4). */
    private static Component[] sign(String id) {
        Component[] lines = new Component[4];
        for (int i = 0; i < 4; i++) lines[i] = Component.translatable("museum.bdragon.sign." + id + "." + (i + 1));
        lines[0] = lines[0].copy().withStyle(ChatFormatting.GOLD);
        return lines;
    }

    private static MutableComponent key(String key, Object... args) {
        return Component.translatable("museum.bdragon." + key, args);
    }

    /** The race name cut in two lines for the signs. */
    private static Component name(DragonType t, int line) {
        return Component.translatable("museum.bdragon.name." + t.id() + "." + line);
    }

    /** A floating text panel: a gold title and a text, always facing the visitor. */
    private void panel(double x, double y, double z, String id, int width) {
        Component text = Component.empty()
                .append(Component.translatable("museum.bdragon.panel." + id + ".title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append("\n")
                .append(Component.translatable("museum.bdragon.panel." + id + ".text"));
        data.addText(ax(x), ay(y), az(z), MuseumEnglish.of(text), width);
    }

    private static String roman(int n) {
        return LairManager.roman(n);
    }
}
