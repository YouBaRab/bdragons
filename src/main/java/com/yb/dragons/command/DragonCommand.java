package com.yb.dragons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.player.AlphaRitual;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import com.yb.dragons.world.lair.DragonLair;
import com.yb.dragons.world.lair.LairManager;
import com.yb.dragons.world.museum.MuseumBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.Arrays;

/**
 * /bdragon — admin and test command (permission level 2).
 * <pre>
 * /bdragon info [player]
 * /bdragon transform &lt;type&gt; [player]
 * /bdragon stage &lt;1-5&gt; [player]
 * /bdragon power &lt;amount&gt; [player]
 * /bdragon defeated &lt;rank&gt; [player]
 * /bdragon reset [player]
 * /bdragon summon &lt;type&gt; &lt;rank&gt;
 * /bdragon form &lt;hybrid|dragon&gt;
 * /bdragon lair [player]   (nearest lair of each rank, revealed on the map)
 * /bdragon alpha [player]  (plays the awakening of the alpha: stage V, then the ritual and its cinematic)
 * /bdragon alpha on|off [player]   (becomes / stops being the alpha at once)
 * /bdragon alpha race &lt;type&gt; [player]   (becomes a dragon of this race if needed, then plays the awakening)
 * /bdragon musee           (builds the museum where you stand; alias "museum")
 * </pre>
 * Usable by operators, and by the host of a solo world even when the cheats are off (it is a test
 * command of the mod, the museum must be reachable from any world).
 */
public final class DragonCommand {
    private DragonCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var types = Arrays.stream(DragonType.values()).map(DragonType::id).toList();
        var selectable = DragonType.SELECTABLE.stream().map(DragonType::id).toList();
        var forms = Arrays.stream(DragonForm.values()).map(DragonForm::id).toList();

        dispatcher.register(Commands.literal("bdragon")
                .requires(DragonCommand::allowed)
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("transform")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(selectable, b))
                                .executes(c -> transform(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> transform(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("stage")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 5))
                                .executes(c -> stage(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> stage(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("power")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(c -> power(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> power(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("defeated")
                        .then(Commands.argument("rank", IntegerArgumentType.integer(0, 5))
                                .executes(c -> defeated(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> defeated(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("reset")
                        .executes(c -> reset(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reset(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("summon")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(types, b))
                                .then(Commands.argument("rank", IntegerArgumentType.integer(1, 5))
                                        .executes(DragonCommand::summon))))
                .then(Commands.literal("form")
                        .then(Commands.argument("form", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(forms, b))
                                .executes(c -> form(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> form(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("alpha")
                        .executes(c -> alpha(c, c.getSource().getPlayerOrException()))
                        .then(Commands.literal("on")
                                .executes(c -> alphaSet(c, c.getSource().getPlayerOrException(), true))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> alphaSet(c, EntityArgument.getPlayer(c, "player"), true))))
                        .then(Commands.literal("off")
                                .executes(c -> alphaSet(c, c.getSource().getPlayerOrException(), false))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> alphaSet(c, EntityArgument.getPlayer(c, "player"), false))))
                        // tests: the alpha powers without their recharge (until the server stops), or recharged now
                        .then(Commands.literal("cooldown")
                                .then(Commands.literal("off").executes(c -> alphaCooldown(c, false)))
                                .then(Commands.literal("on").executes(c -> alphaCooldown(c, true))))
                        .then(Commands.literal("recharge")
                                .executes(c -> alphaRecharge(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> alphaRecharge(c, EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("race")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(selectable, b))
                                        .executes(c -> alphaRace(c, c.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(c -> alphaRace(c, EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> alpha(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("lair")
                        .executes(c -> lair(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> lair(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("musee")
                        .executes(c -> museum(c, c.getSource().getPlayerOrException()))
                        .then(Commands.literal("mur").executes(DragonCommand::museumWall)))
                .then(Commands.literal("museum")
                        .executes(c -> museum(c, c.getSource().getPlayerOrException()))));
    }

    private static final java.util.function.Predicate<CommandSourceStack> OPERATOR = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    /** Operators, or the owner of a single player world (cheats on or off). */
    private static boolean allowed(CommandSourceStack source) {
        if (OPERATOR.test(source)) return true;
        ServerPlayer player = source.getPlayer();
        return player != null && source.getServer().isSingleplayerOwner(player.nameAndId());
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        String text = d.isDragon()
                ? player.getName().getString() + ": " + d.type().id() + " stage " + d.stage().number() + (d.isAlpha() ? " (alpha)" : "")
                  + ", power " + d.power() + ", defeated rank " + d.maxDefeatedRank() + ", form " + d.form().id()
                : player.getName().getString() + ": human, quest " + (d.questIndex() + 1) + ", chosen "
                  + (d.chosenType() == null ? "-" : d.chosenType().id());
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int transform(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonType type = DragonType.byId(StringArgumentType.getString(c, "type"));
        if (type == null || !type.selectable()) {
            c.getSource().sendFailure(Component.literal("Unknown dragon type"));
            return 0;
        }
        DragonPlayerData d = DragonPlayerManager.get(player);
        d.setChosenType(type);
        d.setQuestIndex(4);
        DragonPlayerManager.transform(player, d, type);
        c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " -> " + type.id()), true);
        return 1;
    }

    private static int stage(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        if (!d.isDragon()) {
            c.getSource().sendFailure(Component.literal("This player is not a dragon"));
            return 0;
        }
        DragonStage stage = DragonStage.byNumber(IntegerArgumentType.getInteger(c, "stage"));
        if (d.power() < stage.powerRequired()) d.setPower(stage.powerRequired());
        if (d.maxDefeatedRank() < stage.challengeRank()) d.setMaxDefeatedRank(stage.challengeRank());
        DragonPlayerManager.evolve(player, d, stage);
        return 1;
    }

    private static int power(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        d.setPower(IntegerArgumentType.getInteger(c, "amount"));
        DragonPlayerManager.tryEvolve(player, d, true);
        c.getSource().sendSuccess(() -> Component.literal("Power = " + d.power()), false);
        return 1;
    }

    private static int defeated(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        d.setMaxDefeatedRank(IntegerArgumentType.getInteger(c, "rank"));
        DragonPlayerManager.tryEvolve(player, d, true);
        c.getSource().sendSuccess(() -> Component.literal("Defeated rank = " + d.maxDefeatedRank()), false);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        d.reset();
        DragonPlayerManager.fullSync(player);
        DragonPlayerManager.resetFlight(player);
        c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + " is human again"), true);
        return 1;
    }

    private static int summon(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        DragonType type = DragonType.byId(StringArgumentType.getString(c, "type"));
        if (type == null) {
            c.getSource().sendFailure(Component.literal("Unknown dragon type"));
            return 0;
        }
        var level = c.getSource().getLevel();
        WildDragon dragon = ModRegistry.WILD_DRAGON.get().create(level, EntitySpawnReason.COMMAND);
        if (dragon == null) return 0;
        var pos = c.getSource().getPosition();
        dragon.snapTo(pos.x, pos.y + 4, pos.z, 0, 0);
        dragon.setup(type, IntegerArgumentType.getInteger(c, "rank"));
        dragon.setPersistenceRequired();
        level.addFreshEntity(dragon);
        return 1;
    }

    private static int form(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        if (!d.isDragon()) {
            c.getSource().sendFailure(Component.literal("This player is not a dragon"));
            return 0;
        }
        d.setForm(DragonForm.byId(StringArgumentType.getString(c, "form")));
        DragonPlayerManager.fullSync(player);
        c.getSource().sendSuccess(() -> Component.literal("Form = " + d.form().id()), false);
        return 1;
    }

    /** Turns the recharge of the alpha powers off (they can be used again and again, even while one is running) or on. */
    private static int alphaCooldown(CommandContext<CommandSourceStack> c, boolean on) {
        DragonAlpha.noCooldown = !on;
        if (!on) {
            for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
                DragonPlayerData d = DragonPlayerManager.get(p);
                if (d.alphaCooldown > 0) {
                    d.alphaCooldown = 0;
                    d.markDirty();
                }
            }
        }
        c.getSource().sendSuccess(() -> Component.literal(on ? "Alpha powers: recharge on" : "Alpha powers: no recharge (until the server stops)"), true);
        return 1;
    }

    /** The alpha power of the player is ready again now. */
    private static int alphaRecharge(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        d.alphaCooldown = 0;
        d.markDirty();
        c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + ": alpha power ready"), false);
        return 1;
    }

    /** Brings the player to the legendary stage if needed, then plays the awakening ritual of the alpha (test). */
    private static int alpha(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        if (!d.isDragon() || d.type() == null) {
            c.getSource().sendFailure(Component.literal("This player is not a dragon"));
            return 0;
        }
        if (d.inRitual()) return 0;
        if (!d.stage().isMax()) {
            DragonStage stage = DragonStage.LEGENDARY;
            if (d.power() < stage.powerRequired()) d.setPower(stage.powerRequired());
            if (d.maxDefeatedRank() < stage.challengeRank()) d.setMaxDefeatedRank(stage.challengeRank());
            DragonPlayerManager.evolve(player, d, stage);
        }
        // the test can be played again by an alpha
        if (d.isAlpha()) d.setAlpha(false);
        if (!AlphaRitual.start(player, d.type())) return 0;
        c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + ": alpha ritual"), false);
        return 1;
    }

    /** The awakening of the alpha of a given race (the museum buttons of the Sylvan Tortoise and of the Tempest Dragon). */
    private static int alphaRace(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        DragonType type = DragonType.byId(StringArgumentType.getString(c, "type"));
        if (type == null || !type.selectable()) {
            c.getSource().sendFailure(Component.literal("Unknown dragon type"));
            return 0;
        }
        DragonPlayerData d = DragonPlayerManager.get(player);
        if (d.inRitual()) return 0;
        if (d.type() != type) {
            d.setChosenType(type);
            d.setQuestIndex(4);
            DragonPlayerManager.transform(player, d, type);
        }
        return alpha(c, player);
    }

    private static int alphaSet(CommandContext<CommandSourceStack> c, ServerPlayer player, boolean alpha) {
        DragonPlayerData d = DragonPlayerManager.get(player);
        if (!d.isDragon()) {
            c.getSource().sendFailure(Component.literal("This player is not a dragon"));
            return 0;
        }
        if (alpha && !d.stage().isMax()) {
            DragonStage stage = DragonStage.LEGENDARY;
            if (d.power() < stage.powerRequired()) d.setPower(stage.powerRequired());
            if (d.maxDefeatedRank() < stage.challengeRank()) d.setMaxDefeatedRank(stage.challengeRank());
            DragonPlayerManager.evolve(player, d, stage);
        }
        AlphaRitual.end(player, d);
        d.setAlpha(alpha);
        DragonPlayerManager.fullSync(player);
        c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + ": alpha = " + d.isAlpha()), true);
        return 1;
    }

    /** Builds the museum where the player stands (it goes south, about 45 x 280 blocks). */
    private static int museum(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        MuseumBuilder.build(player.level(), player.blockPosition());
        c.getSource().sendSuccess(() -> Component.translatable("museum.bdragon.built"), true);
        return 1;
    }

    /** Rebuilds the claw wall of the museum (button of step 2). */
    private static int museumWall(CommandContext<CommandSourceStack> c) {
        var level = c.getSource().getLevel();
        var origin = com.yb.dragons.world.museum.MuseumData.get(level).origin();
        if (origin == null) {
            c.getSource().sendFailure(Component.literal("No museum in this dimension"));
            return 0;
        }
        MuseumBuilder.rebuildClawWall(level, origin);
        return 1;
    }

    /** Reveals the nearest lair of every rank (test helper). */
    private static int lair(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        var level = player.level();
        LairManager manager = LairManager.get(level);
        DragonPlayerData d = DragonPlayerManager.get(player);
        int found = 0;
        for (int rank = 1; rank <= 5; rank++) {
            DragonLair lair = manager.findNearest(level, player.getX(), player.getZ(), rank, 12);
            if (lair == null) continue;
            d.addKnownLair(LairManager.knownKey(level, lair.region));
            int dist = (int) Math.sqrt(lair.distanceSqr(player.getX(), player.getZ()));
            String line = "rank >= " + rank + ": " + lair.type.id() + " rank " + lair.rank + " at " + lair.x + " " + lair.z + " (" + dist + " blocks)";
            c.getSource().sendSuccess(() -> Component.literal(line), false);
            found++;
        }
        manager.syncTo(player, true);
        if (found == 0) c.getSource().sendFailure(Component.literal("No lair within 6000 blocks"));
        return found;
    }
}
