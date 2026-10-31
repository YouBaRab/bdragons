package com.yb.dragons.player;

import com.yb.dragons.Config;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.network.ModNetwork;
import com.yb.dragons.world.TerrainEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-side brain of the player dragons: forms, attributes, flight, gliding, powers, evolution and syncing. */
public final class DragonPlayerManager {
    private static final Identifier MOD_HEALTH = YBDragons.id("dragon_health");
    private static final Identifier MOD_ARMOR = YBDragons.id("dragon_armor");
    private static final Identifier MOD_ATTACK = YBDragons.id("dragon_attack");
    private static final Identifier MOD_SPEED = YBDragons.id("dragon_speed");
    private static final Identifier MOD_SCALE = YBDragons.id("dragon_scale");
    private static final Identifier MOD_CAMERA = YBDragons.id("dragon_camera");
    private static final Identifier MOD_BLOCK_REACH = YBDragons.id("dragon_block_reach");
    private static final Identifier MOD_ENTITY_REACH = YBDragons.id("dragon_entity_reach");
    private static final Identifier MOD_STEP = YBDragons.id("dragon_step");
    private static final Identifier MOD_SAFE_FALL = YBDragons.id("dragon_safe_fall");
    private static final Identifier MOD_KNOCKBACK = YBDragons.id("dragon_knockback");
    /** The alpha ritual holds the dragon still. */
    private static final Identifier MOD_RITUAL_SPEED = YBDragons.id("alpha_ritual_speed");
    private static final Identifier MOD_RITUAL_JUMP = YBDragons.id("alpha_ritual_jump");
    /** The tortoise moves slowly while it fires the Beam of Life. */
    private static final Identifier MOD_BEAM_SPEED = YBDragons.id("life_beam_speed");

    public static final float ENERGY_REGEN = DragonStats.ENERGY_REGEN;
    public static final float BREATH_COST = 1.2F;
    public static final float ROAR_COST = 40F;
    public static final int ROAR_COOLDOWN = DragonStats.ROAR_COOLDOWN;
    public static final int MAX_STAMINA = 200;

    private DragonPlayerManager() {}

    public static DragonPlayerData get(ServerPlayer player) {
        return DragonSavedData.get(player.level().getServer()).get(player.getUUID());
    }

    // ------------------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        DragonSavedData saved = DragonSavedData.get(player.level().getServer());
        DragonPlayerData data = saved.get(player.getUUID());
        if (data.inRitual()) AlphaRitual.tick(player, data);
        boolean spectator = player.isSpectator();
        boolean powers = data.hasPowers() && !spectator && !data.inRitual();
        boolean dragon = data.isActiveDragon() && !spectator;

        if (player.tickCount % 20 == 0 || data.publicDirty) {
            applyAttributes(player, data);
        }
        updateFlight(player, data, dragon);
        updateGlide(player, data);
        LifeBeam.tick(player, data);
        StormCall.tick(player, data);
        NetherDomain.tick(player, data);
        SunStrike.tick(player, data);
        AlphaHitboxes.tick(player, data);

        // every recharge counts down whatever the form (the alpha power also recharges in the hybrid form)
        data.tickTimers();

        // a giant alpha (tortoise, serpent) under a ceiling (respawn in a house, teleport into a cave...): back to the
        // hybrid form. A wall at its side is not a problem: it can always move away from it.
        if (player.tickCount % 10 == 5 && (data.isTortoise() || data.isSerpent() || data.isSteed()) && !data.inRitual() && !spectator
                && !AlphaHitboxes.hasRoom(player, data.type())) {
            LifeBeam.stop(player, data);
            data.setForm(DragonForm.HYBRID);
            data.formCooldown = 40;
            applyAttributes(player, data);
            player.sendSystemMessage(Component.translatable(AlphaHitboxes.stuckKey(data.type())).withStyle(ChatFormatting.GOLD));
        }

        if (powers) {
            DragonStage stage = data.stage();
            DragonStats stats = data.stats();
            // energy regeneration
            if (!data.breathing && data.energy < stats.maxEnergy()) {
                data.energy = Math.min(stats.maxEnergy(), data.energy + stats.energyRegen());
            }
            if (data.breathing) {
                BreathHandler.tick(player, data);
            }
            if (player.tickCount % 20 == 0) {
                DragonPowers.tickPassive(player, data);
            }
            if (stage.hasTerrainEffect()) {
                // the feet of the tortoise (and the coils of the serpent) are much bigger
                TerrainEffects.tickPlayer(player, data.type(), data.isTortoise() ? 4 : data.isSerpent() || data.isSteed() ? 3 : dragon ? 2 : 1);
            }
        } else {
            if (data.breathing) data.publicDirty = true;
            data.breathing = false;
        }

        // power gained while the challenge was missing (or given by a command): evolve as soon as possible
        if (data.isDragon() && player.tickCount % 100 == 0) tryEvolve(player, data, false);

        if (data.dirty || data.publicDirty) saved.setDirty();
        // energy & stamina change constantly: refresh the owner's HUD a few times per second
        if (data.dirty || (powers && player.tickCount % 5 == 0)) {
            syncPrivate(player, data);
            data.dirty = false;
        }
        if (data.publicDirty) {
            syncPublic(player, data);
            data.publicDirty = false;
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Attributes, flight and gliding
    // ------------------------------------------------------------------------------------------------------

    /** Applies the stats of the current stage and form now (and the hold of the alpha ritual). */
    public static void refreshAttributes(ServerPlayer player, DragonPlayerData data) {
        applyAttributes(player, data);
    }

    private static void applyAttributes(ServerPlayer player, DragonPlayerData data) {
        if (!data.isDragon() || player.isSpectator()) {
            removeAll(player);
            return;
        }
        DragonForm form = data.form();
        // the hybrid keeps a human sized body: the numbers of every alpha (not those of the tortoise)
        DragonStats s = form == DragonForm.HYBRID ? data.hybridStats() : data.stats();
        DragonStage stage = data.stage();
        set(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, s.speed(form), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (data.beaming()) {
            set(player, Attributes.MOVEMENT_SPEED, MOD_BEAM_SPEED, AlphaForest.BEAM_MOVE_FACTOR - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            remove(player, Attributes.MOVEMENT_SPEED, MOD_BEAM_SPEED);
        }
        // the alpha ritual: the dragon cannot walk nor jump
        if (data.inRitual()) {
            set(player, Attributes.MOVEMENT_SPEED, MOD_RITUAL_SPEED, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            set(player, Attributes.JUMP_STRENGTH, MOD_RITUAL_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else {
            remove(player, Attributes.MOVEMENT_SPEED, MOD_RITUAL_SPEED);
            remove(player, Attributes.JUMP_STRENGTH, MOD_RITUAL_JUMP);
        }
        switch (form) {
            case HYBRID -> {
                float f = form.powerFactor();
                set(player, Attributes.MAX_HEALTH, MOD_HEALTH, Math.round(s.bonusHealth() * f), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.ARMOR, MOD_ARMOR, Math.round(s.bonusArmor() * f), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.ATTACK_DAMAGE, MOD_ATTACK, s.attack(form), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.SCALE, MOD_SCALE, s.hybridScale() - 1.0, AttributeModifier.Operation.ADD_VALUE);
                remove(player, Attributes.CAMERA_DISTANCE, MOD_CAMERA);
                set(player, Attributes.BLOCK_INTERACTION_RANGE, MOD_BLOCK_REACH, s.reach() * 0.5, AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.ENTITY_INTERACTION_RANGE, MOD_ENTITY_REACH, s.reach() * 0.5, AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.STEP_HEIGHT, MOD_STEP, stage.number() >= 2 ? 0.5 : 0.0, AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.SAFE_FALL_DISTANCE, MOD_SAFE_FALL, Math.round(s.safeFall() * f), AttributeModifier.Operation.ADD_VALUE);
                int k = stage.ordinal() + (data.isAlpha() ? 1 : 0);
                double kb = data.type() == DragonType.TERRA ? 0.2 + 0.08 * k : 0.06 * k;
                set(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK, kb, AttributeModifier.Operation.ADD_VALUE);
            }
            case DRAGON -> {
                set(player, Attributes.MAX_HEALTH, MOD_HEALTH, s.bonusHealth(), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.ARMOR, MOD_ARMOR, s.bonusArmor(), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.ATTACK_DAMAGE, MOD_ATTACK, s.attack(form), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.SCALE, MOD_SCALE, s.scaleAttribute() - 1.0, AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.CAMERA_DISTANCE, MOD_CAMERA, s.cameraDistance(), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.BLOCK_INTERACTION_RANGE, MOD_BLOCK_REACH, s.reach(), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.ENTITY_INTERACTION_RANGE, MOD_ENTITY_REACH, s.reach(), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.STEP_HEIGHT, MOD_STEP, s.stepHeight(), AttributeModifier.Operation.ADD_VALUE);
                set(player, Attributes.SAFE_FALL_DISTANCE, MOD_SAFE_FALL, s.safeFall(), AttributeModifier.Operation.ADD_VALUE);
                int k = Math.min(9, stage.ordinal() + (data.isAlpha() ? 1 : 0));
                double kb = data.type() == DragonType.TERRA ? Math.min(1.0, 0.3 + 0.1 * k) : 0.1 * k;
                // nothing pushes a tortoise of 16 blocks, a serpent of 43, nor a steed of 12 blocks high
                if (data.isTortoise() || data.isSerpent() || data.isSteed()) kb = 1.0;
                set(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK, kb, AttributeModifier.Operation.ADD_VALUE);
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void removeAll(ServerPlayer player) {
        remove(player, Attributes.MAX_HEALTH, MOD_HEALTH);
        remove(player, Attributes.ARMOR, MOD_ARMOR);
        remove(player, Attributes.ATTACK_DAMAGE, MOD_ATTACK);
        remove(player, Attributes.MOVEMENT_SPEED, MOD_SPEED);
        remove(player, Attributes.SCALE, MOD_SCALE);
        remove(player, Attributes.CAMERA_DISTANCE, MOD_CAMERA);
        remove(player, Attributes.BLOCK_INTERACTION_RANGE, MOD_BLOCK_REACH);
        remove(player, Attributes.ENTITY_INTERACTION_RANGE, MOD_ENTITY_REACH);
        remove(player, Attributes.STEP_HEIGHT, MOD_STEP);
        remove(player, Attributes.SAFE_FALL_DISTANCE, MOD_SAFE_FALL);
        remove(player, Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK);
        remove(player, Attributes.MOVEMENT_SPEED, MOD_RITUAL_SPEED);
        remove(player, Attributes.JUMP_STRENGTH, MOD_RITUAL_JUMP);
        remove(player, Attributes.MOVEMENT_SPEED, MOD_BEAM_SPEED);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void set(Player player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(id);
        if (current != null && current.amount() == amount && current.operation() == op) return;
        // permanent so that the bonus health is still there when the player data is loaded
        inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static void remove(Player player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null && inst.getModifier(id) != null) inst.removeModifier(id);
    }

    /** Only the full dragon form flies (stage II+). */
    private static void updateFlight(ServerPlayer player, DragonPlayerData data, boolean dragon) {
        if (player.isCreative() || player.isSpectator()) return;
        Abilities abilities = player.getAbilities();
        boolean changed = false;
        DragonStage stage = data.stage();

        boolean allowed = dragon && stage.canFly() && !data.inRitual();
        if (allowed && stage.limitedFlight()) {
            if (abilities.flying) {
                data.flightStamina = Math.max(0, data.flightStamina - 1);
                if (data.flightStamina == 0) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.wings_tired").withStyle(ChatFormatting.GRAY), true);
                }
            } else if (player.onGround()) {
                data.flightStamina = Math.min(MAX_STAMINA, data.flightStamina + 2);
            }
            // tired wings: flying is disabled until half of the stamina is back (the dragon glides meanwhile)
            if (data.flightStamina == 0 || (!abilities.mayfly && data.flightStamina < MAX_STAMINA / 2)) allowed = false;
        } else if (player.onGround() || allowed) {
            data.flightStamina = MAX_STAMINA;
        }

        if (allowed) {
            if (!abilities.mayfly) { abilities.mayfly = true; changed = true; }
            float speed = data.stats().flyingSpeed() * (data.beaming() ? AlphaForest.BEAM_MOVE_FACTOR : 1.0F);
            if (Math.abs(abilities.getFlyingSpeed() - speed) > 1.0E-4F) { abilities.setFlyingSpeed(speed); changed = true; }
        } else if (data.isDragon()) {
            if (abilities.mayfly || abilities.flying) {
                abilities.mayfly = false;
                abilities.flying = false;
                changed = true;
            }
            if (Math.abs(abilities.getFlyingSpeed() - 0.05F) > 1.0E-4F) { abilities.setFlyingSpeed(0.05F); changed = true; }
        }
        if (changed) player.onUpdateAbilities();
    }

    /** True when the player could glide right now (the client decides when to start, like the elytra). */
    public static boolean canGlide(ServerPlayer player, DragonPlayerData data) {
        return data.isDragon() && data.form().hasWings() && !data.inRitual() && !player.isSpectator() && !player.onGround()
                && !player.getAbilities().flying && !player.isInWater() && !player.isInLava() && !player.isPassenger()
                && !player.onClimbable();
    }

    /** While gliding, the dragon never takes fall damage. Sneaking stops the glide. */
    private static void updateGlide(ServerPlayer player, DragonPlayerData data) {
        if (!data.gliding) return;
        if (!canGlide(player, data) || player.isShiftKeyDown()) {
            data.gliding = false;
            data.markDirty();
            return;
        }
        player.resetFallDistance();
    }

    // ------------------------------------------------------------------------------------------------------
    // Actions from the keyboard
    // ------------------------------------------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, int action, boolean pressed) {
        DragonPlayerData data = get(player);
        // nothing can interrupt the awakening of the alpha
        if (data.inRitual()) return;
        switch (action) {
            case ModNetwork.Action.BREATH -> {
                boolean want = pressed && data.hasPowers() && data.energy > 5;
                if (want != data.breathing) {
                    data.breathing = want;
                    data.breathTicks = 0;
                    data.publicDirty = true;
                }
            }
            case ModNetwork.Action.ROAR -> {
                if (!data.hasPowers()) return;
                if (!data.stage().hasRoar()) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.roar_locked").withStyle(ChatFormatting.RED), true);
                    return;
                }
                if (data.roarCooldown > 0) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.roar_cooldown", (data.roarCooldown + 19) / 20).withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                if (data.energy < ROAR_COST) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.no_energy").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                data.energy -= ROAR_COST;
                data.roarCooldown = data.stats().roarCooldown();
                BreathHandler.roar(player, data);
                data.markDirty();
            }
            case ModNetwork.Action.CYCLE_FORM -> {
                if (!data.isDragon() || data.formCooldown > 0) return;
                DragonForm next = data.form().next();
                // the giant alphas need room: 10.8 blocks of free height above the tortoise, 5.9 above the serpent
                if (next == DragonForm.DRAGON && data.isAlpha() && !AlphaHitboxes.hasRoom(player, data.type())) {
                    player.sendSystemMessage(Component.translatable(AlphaHitboxes.noRoomKey(data.type())).withStyle(ChatFormatting.RED), true);
                    data.formCooldown = 20;
                    return;
                }
                LifeBeam.stop(player, data);
                data.setForm(next);
                data.formCooldown = 30;
                data.breathing = false;
                ServerLevel level = player.level();
                DragonType type = data.type();
                level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
                if (type != null) {
                    level.sendParticles(type.breathParticle(), player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.9, 0.6, 0.05);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), switch (next) {
                    case DRAGON -> SoundEvents.ENDER_DRAGON_FLAP;
                    case HYBRID -> SoundEvents.ARMOR_EQUIP_NETHERITE.value();
                }, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendSystemMessage(Component.translatable("message.bdragon.form_" + next.id()).withStyle(ChatFormatting.GOLD), true);
                applyAttributes(player, data);
            }
            case ModNetwork.Action.ALPHA_POWER -> {
                if (!data.isDragon()) return;
                if (!data.isAlpha()) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.locked").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                if (data.form() != DragonForm.DRAGON) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.dragon_only").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                // B again stops the Beam of Life
                if (data.beaming()) {
                    LifeBeam.stop(player, data);
                    return;
                }
                if (data.alphaCooldown > 0) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.cooldown", (data.alphaCooldown + 19) / 20)
                            .withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                // without recharge (tests), the key calls the power again even while it is running
                if (DragonAlpha.noCooldown) {
                    StormCall.stop(player, data);
                    NetherDomain.stop(player, data);
                    SunStrike.stop(player, data);
                }
                if (AlphaPowers.use(player, data)) {
                    data.alphaCooldown = DragonAlpha.noCooldown ? 0 : DragonAlpha.powerCooldown(data.type());
                    data.markDirty();
                }
            }
            case ModNetwork.Action.GLIDE -> {
                boolean want = pressed && canGlide(player, data) && !player.isShiftKeyDown();
                if (want != data.gliding) {
                    data.gliding = want;
                    if (want) player.resetFallDistance();
                    data.markDirty();
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Codex, transformation, power and evolution
    // ------------------------------------------------------------------------------------------------------

    /** The codex is given once to every player, on his first arrival in the world. */
    public static void giveCodexIfNeeded(ServerPlayer player) {
        DragonPlayerData data = get(player);
        if (data.codexGiven() || !Config.GIVE_CODEX.get()) return;
        data.setCodexGiven(true);
        ItemStack codex = new ItemStack(ModRegistry.GRIMOIRE.get());
        if (!player.addItem(codex)) player.spawnAtLocation(player.level(), codex);
        player.sendSystemMessage(Component.translatable("message.bdragon.codex_given_1").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.bdragon.codex_given_2",
                Component.keybind("key.bdragon.grimoire")).withStyle(ChatFormatting.YELLOW));
    }

    /** The ritual: the human becomes a hatchling of the chosen race. */
    public static void transform(ServerPlayer player, DragonPlayerData data, DragonType type) {
        data.setType(type);
        data.setStage(DragonStage.HATCHLING);
        data.setForm(DragonForm.DRAGON);
        // a new dragon starts again from the egg: the alpha is lost
        LifeBeam.stop(player, data);
        StormCall.stop(player, data);
        NetherDomain.stop(player, data);
        SunStrike.stop(player, data);
        data.sunStart = -1;
        data.setAlpha(false);
        data.energy = DragonStage.HATCHLING.maxEnergy();
        ServerLevel level = player.level();
        level.sendParticles(type.breathParticle(), player.getX(), player.getY() + 1, player.getZ(), 120, 1.2, 1.5, 1.2, 0.08);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2F, 1.4F);
        // short title so that it always fits on the screen, details in the subtitle and the chat
        title(player, Component.translatable("title.bdragon.transformed").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                type.displayName().copy().withStyle(s -> s.withColor(type.textColor())));
        player.sendSystemMessage(Component.translatable("message.bdragon.transformed", type.displayName())
                .withStyle(s -> s.withColor(type.textColor()).withBold(true)));
        for (String line : new String[] {"codex", "breath", "form", "fly", "glide"}) {
            player.sendSystemMessage(help(line).withStyle(ChatFormatting.YELLOW));
        }
        tryEvolve(player, data, false);
    }

    private static MutableComponent help(String line) {
        return switch (line) {
            case "codex" -> Component.translatable("message.bdragon.help.codex", Component.keybind("key.bdragon.grimoire"));
            case "breath" -> Component.translatable("message.bdragon.help.breath", Component.keybind("key.bdragon.breath"));
            case "form" -> Component.translatable("message.bdragon.help.form", Component.keybind("key.bdragon.toggle_form"));
            case "fly" -> Component.translatable("message.bdragon.help.fly");
            default -> Component.translatable("message.bdragon.help.glide");
        };
    }

    public static void addPower(ServerPlayer player, int amount) {
        DragonPlayerData data = get(player);
        if (!data.isDragon() || amount <= 0) return;
        data.addPower(amount);
        player.sendSystemMessage(Component.translatable("message.bdragon.power_gain", amount, data.power())
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        tryEvolve(player, data, true);
    }

    public static void onWildDragonDefeated(ServerPlayer player, int rank) {
        DragonPlayerData data = get(player);
        data.incrementDefeated();
        if (rank > data.maxDefeatedRank()) {
            data.setMaxDefeatedRank(rank);
            if (data.isDragon()) {
                player.sendSystemMessage(Component.translatable("message.bdragon.challenge_won", rank).withStyle(ChatFormatting.GOLD));
            }
        }
        addPower(player, DragonStage.powerForRank(rank));
    }

    /** Evolves the player when both the power and the challenge conditions are met. */
    public static void tryEvolve(ServerPlayer player, DragonPlayerData data, boolean notify) {
        if (!data.isDragon()) return;
        while (!data.stage().isMax()) {
            DragonStage next = data.stage().next();
            if (data.power() < next.powerRequired()) return;
            if (data.maxDefeatedRank() < next.challengeRank()) {
                if (notify) {
                    player.sendSystemMessage(Component.translatable("message.bdragon.need_challenge_1").withStyle(ChatFormatting.RED));
                    player.sendSystemMessage(Component.translatable("message.bdragon.need_challenge_2", next.challengeRank())
                            .withStyle(ChatFormatting.RED));
                    player.sendSystemMessage(Component.translatable("message.bdragon.need_challenge_3").withStyle(ChatFormatting.GRAY));
                }
                return;
            }
            evolve(player, data, next);
        }
    }

    public static void evolve(ServerPlayer player, DragonPlayerData data, DragonStage next) {
        data.setStage(next);
        data.energy = data.stats().maxEnergy();
        DragonType type = data.type();
        ServerLevel level = player.level();
        if (type != null) {
            level.sendParticles(type.breathParticle(), player.getX(), player.getY() + 1, player.getZ(), 200, 1.5, 2.0, 1.5, 0.1);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 80, 1.0, 1.5, 1.0, 0.5);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.6F);
        title(player, Component.translatable("title.bdragon.evolved").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("title.bdragon.evolved.sub", next.number(), next.displayName()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("message.bdragon.evolved", next.number(), next.displayName())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.bdragon.evolved_feature", next.featureKey()).withStyle(ChatFormatting.YELLOW));
        player.setHealth(player.getMaxHealth());
    }

    public static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    // ------------------------------------------------------------------------------------------------------
    // Sync
    // ------------------------------------------------------------------------------------------------------

    public static void syncPrivate(ServerPlayer player, DragonPlayerData data) {
        ModNetwork.sendToPlayer(player, new ModNetwork.PrivateSync(data));
    }

    public static void syncPublic(ServerPlayer player, DragonPlayerData data) {
        ModNetwork.sendToTrackingAndSelf(player, ModNetwork.PublicSync.of(player, data));
    }

    /** Called when {@code watcher} starts seeing {@code target}. */
    public static void sendTo(ServerPlayer watcher, ServerPlayer target) {
        ModNetwork.sendToPlayer(watcher, ModNetwork.PublicSync.of(target, get(target)));
    }

    /** Removes the dragon flight (used when an admin resets a player). */
    public static void resetFlight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;
        Abilities abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        abilities.setFlyingSpeed(0.05F);
        player.onUpdateAbilities();
    }

    public static void fullSync(ServerPlayer player) {
        DragonPlayerData data = get(player);
        data.markAll();
        applyAttributes(player, data);
    }
}
