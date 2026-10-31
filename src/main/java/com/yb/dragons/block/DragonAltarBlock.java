package com.yb.dragons.block;

import com.yb.dragons.ModRegistry;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonQuests;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.item.AlphaScaleItem;
import com.yb.dragons.item.DragonEssenceItem;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * The Dragon Altar. Offer the quest ingredients by using them on the altar, then use the altar
 * with an empty hand to perform the transformation ritual.
 *
 * <p>For the dragons, the altar is also the forge of the Orbs of Power: 4 Alpha Scales of a race and
 * a Nether Star offered here become the orb of that race (sneak + empty hand takes the offerings back).</p>
 */
public class DragonAltarBlock extends Block {
    public DragonAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        DragonPlayerData data = DragonPlayerManager.get(sp);
        if (data.isDragon() && (stack.getItem() instanceof AlphaScaleItem || stack.is(Items.NETHER_STAR))) {
            return forge(sp, server, pos, data, stack);
        }
        if (data.isDragon() || data.questsDone()) {
            return useWithoutItem(state, level, pos, player, hit);
        }
        int quest = data.questIndex();
        List<DragonQuests.Requirement> reqs = DragonQuests.requirements(quest, data.chosenType());

        // quest 3: the first essence chooses the race
        if (quest == DragonQuests.ESSENCE_QUEST) {
            if (stack.getItem() instanceof DragonEssenceItem essence) {
                DragonType type = essence.getDragonType();
                data.setChosenType(type);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                server.sendParticles(type.breathParticle(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.05);
                server.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 0.8F);
                sp.sendSystemMessage(Component.translatable("message.bdragon.type_chosen", type.displayName())
                        .withStyle(s -> s.withColor(type.textColor()).withBold(true)));
                completeQuest(sp, server, pos, data);
                return InteractionResult.SUCCESS_SERVER;
            }
            sp.sendSystemMessage(Component.translatable("message.bdragon.need_essence").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        for (int i = 0; i < reqs.size(); i++) {
            DragonQuests.Requirement req = reqs.get(i);
            int have = data.questProgress()[i];
            if (have >= req.count() || !req.matches(stack)) continue;
            int give = Math.min(req.count() - have, stack.getCount());
            data.setQuestProgress(i, have + give);
            if (!player.getAbilities().instabuild) stack.shrink(give);
            server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.5);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            sp.sendSystemMessage(Component.translatable("message.bdragon.offered", give, req.name(), have + give, req.count())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            if (isComplete(data, reqs)) completeQuest(sp, server, pos, data);
            return InteractionResult.SUCCESS_SERVER;
        }
        sp.sendSystemMessage(Component.translatable("message.bdragon.not_needed").withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.FAIL;
    }

    // ------------------------------------------------------------------------------------------------------
    // Forge of the orbs of power
    // ------------------------------------------------------------------------------------------------------

    private static InteractionResult forge(ServerPlayer player, ServerLevel level, BlockPos pos, DragonPlayerData data, ItemStack stack) {
        boolean creative = player.getAbilities().instabuild;
        DragonType race;
        if (stack.getItem() instanceof AlphaScaleItem scale) {
            race = scale.getDragonType();
            int have = data.forgeScales(race);
            if (have >= DragonAlpha.SCALES_PER_ORB) {
                player.sendSystemMessage(Component.translatable("message.bdragon.forge.scales_full", race.displayName()).withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            int give = Math.min(DragonAlpha.SCALES_PER_ORB - have, stack.getCount());
            data.setForgeScales(race, have + give);
            if (!creative) stack.shrink(give);
        } else {
            if (data.forgeStars() >= 1) {
                player.sendSystemMessage(Component.translatable("message.bdragon.forge.star_full").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            data.setForgeStars(1);
            if (!creative) stack.shrink(1);
            race = forgeRace(data);
        }
        level.sendParticles(race.breathParticle(), pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 25, 0.3, 0.4, 0.3, 0.03);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.5);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.7F);

        DragonType ready = readyRace(data);
        if (ready != null) {
            forgeOrb(player, level, pos, data, ready);
        } else {
            player.sendSystemMessage(forgeStatus(data, race), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The race whose scales are in the forge (the player's own race first). */
    private static DragonType forgeRace(DragonPlayerData data) {
        DragonType own = data.type();
        if (own != null && data.forgeScales(own) > 0) return own;
        DragonType best = own == null ? DragonType.FIRE : own;
        int most = 0;
        for (DragonType t : DragonType.SELECTABLE) {
            if (data.forgeScales(t) > most) {
                most = data.forgeScales(t);
                best = t;
            }
        }
        return best;
    }

    /** A race with its 4 scales, when the Nether Star is there too. */
    @org.jspecify.annotations.Nullable
    private static DragonType readyRace(DragonPlayerData data) {
        if (data.forgeStars() < 1) return null;
        DragonType own = data.type();
        if (own != null && data.forgeScales(own) >= DragonAlpha.SCALES_PER_ORB) return own;
        for (DragonType t : DragonType.SELECTABLE) {
            if (data.forgeScales(t) >= DragonAlpha.SCALES_PER_ORB) return t;
        }
        return null;
    }

    private static Component forgeStatus(DragonPlayerData data, DragonType race) {
        return Component.translatable("message.bdragon.forge.status", race.displayName(), data.forgeScales(race), DragonAlpha.SCALES_PER_ORB,
                data.forgeStars()).withStyle(s -> s.withColor(race.textColor()));
    }

    /** The orb is born above the altar. */
    private static void forgeOrb(ServerPlayer player, ServerLevel level, BlockPos pos, DragonPlayerData data, DragonType race) {
        data.setForgeScales(race, 0);
        data.setForgeStars(0);
        double x = pos.getX() + 0.5, y = pos.getY() + 1.4, z = pos.getZ() + 0.5;
        ItemEntity orb = new ItemEntity(level, x, y, z, new ItemStack(ModRegistry.ALPHA_ORBS.get(race).get()), 0, 0.25, 0);
        orb.setGlowingTag(true);
        orb.setPickUpDelay(20);
        level.addFreshEntity(orb);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 60, 0.3, 0.5, 0.3, 0.4);
        level.sendParticles(race.breathParticle(), x, y, z, 80, 0.4, 0.6, 0.4, 0.12);
        level.sendParticles(DragonType.flash(race.color()), x, y, z, 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.4F, 1.0F);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.6F);
        player.sendSystemMessage(Component.translatable("message.bdragon.forge.done", race.displayName())
                .withStyle(s -> s.withColor(race.textColor()).withBold(true)));
        if (race == data.type() && data.stage().isMax() && !data.isAlpha()) {
            player.sendSystemMessage(Component.translatable("message.bdragon.forge.use").withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Sneak + empty hand: the offerings of the forge come back. */
    private static void takeBackForge(ServerPlayer player, ServerLevel level, BlockPos pos, DragonPlayerData data) {
        for (DragonType t : DragonType.SELECTABLE) {
            int n = data.forgeScales(t);
            if (n > 0) give(player, new ItemStack(ModRegistry.ALPHA_SCALES.get(t).get(), n));
        }
        if (data.forgeStars() > 0) give(player, new ItemStack(Items.NETHER_STAR, data.forgeStars()));
        data.clearForge();
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
        player.sendSystemMessage(Component.translatable("message.bdragon.forge.taken").withStyle(ChatFormatting.GRAY), true);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.spawnAtLocation(player.level(), stack);
    }

    private static boolean isComplete(DragonPlayerData data, List<DragonQuests.Requirement> reqs) {
        for (int i = 0; i < reqs.size(); i++) {
            if (data.questProgress()[i] < reqs.get(i).count()) return false;
        }
        return true;
    }

    private static void completeQuest(ServerPlayer player, ServerLevel level, BlockPos pos, DragonPlayerData data) {
        int done = data.questIndex();
        data.setQuestIndex(done + 1);
        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.3);
        player.sendSystemMessage(Component.translatable("message.bdragon.quest_complete", DragonQuests.title(done))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (data.questsDone()) {
            player.sendSystemMessage(Component.translatable("message.bdragon.ritual_ready").withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.translatable("message.bdragon.next_quest", DragonQuests.title(data.questIndex()))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        DragonPlayerData data = DragonPlayerManager.get(sp);

        if (data.isDragon()) {
            DragonType type = data.type();
            DragonStage stage = data.stage();
            if (player.isShiftKeyDown() && data.hasForgeOfferings()) {
                takeBackForge(sp, server, pos, data);
                return InteractionResult.SUCCESS_SERVER;
            }
            Component rank = data.isAlpha() ? Component.translatable("dragon_stage.bdragon.alpha") : stage.displayName();
            sp.sendSystemMessage(Component.literal("✦ ").append(type.displayName()).append(" — ").append(rank)
                    .withStyle(s -> s.withColor(type.textColor()).withBold(true)));
            if (data.isAlpha()) {
                sp.sendSystemMessage(Component.translatable("message.bdragon.max_stage").withStyle(ChatFormatting.GOLD));
            } else if (stage.isMax()) {
                sp.sendSystemMessage(Component.translatable("message.bdragon.alpha.hint_1").withStyle(ChatFormatting.GOLD));
                sp.sendSystemMessage(Component.translatable("message.bdragon.alpha.hint_2", DragonAlpha.SCALES_PER_ORB, type.displayName())
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                DragonStage next = stage.next();
                boolean powerOk = data.power() >= next.powerRequired();
                boolean challengeOk = data.maxDefeatedRank() >= next.challengeRank();
                sp.sendSystemMessage(Component.translatable("message.bdragon.next_stage_1", next.number(), next.displayName())
                        .withStyle(ChatFormatting.YELLOW));
                sp.sendSystemMessage(Component.translatable("message.bdragon.next_stage_2", data.power(), next.powerRequired())
                        .withStyle(powerOk ? ChatFormatting.GREEN : ChatFormatting.WHITE));
                sp.sendSystemMessage(Component.translatable("message.bdragon.next_stage_3", next.challengeRank())
                        .withStyle(challengeOk ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            }
            if (data.hasForgeOfferings()) {
                sp.sendSystemMessage(forgeStatus(data, forgeRace(data)));
                sp.sendSystemMessage(Component.translatable("message.bdragon.forge.take_hint").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        if (data.questsDone() && data.chosenType() != null) {
            performRitual(sp, server, pos, data);
            return InteractionResult.SUCCESS_SERVER;
        }

        // show the current quest
        int quest = data.questIndex();
        sp.sendSystemMessage(Component.translatable("message.bdragon.quest_header", quest + 1, DragonQuests.QUEST_COUNT,
                DragonQuests.title(quest)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        sp.sendSystemMessage(DragonQuests.lore(quest).copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        List<DragonQuests.Requirement> reqs = DragonQuests.requirements(quest, data.chosenType());
        for (int i = 0; i < reqs.size(); i++) {
            DragonQuests.Requirement r = reqs.get(i);
            int have = data.questProgress()[i];
            sp.sendSystemMessage(Component.literal(have >= r.count() ? " ✔ " : " ✘ ").append(r.name())
                    .append(" : " + Math.min(have, r.count()) + "/" + r.count())
                    .withStyle(have >= r.count() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void performRitual(ServerPlayer player, ServerLevel level, BlockPos pos, DragonPlayerData data) {
        DragonType type = data.chosenType();
        if (type == null) return;
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3, 0.5, 0.5, 0.5, 0);
        level.sendParticles(type.breathParticle(), pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 150, 1.5, 1.5, 1.5, 0.15);
        DragonPlayerManager.transform(player, data, type);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // runes flying into the altar, like an enchanting table
        for (int i = 0; i < 3; i++) {
            double dx = (random.nextDouble() - 0.5) * 3.0;
            double dz = (random.nextDouble() - 0.5) * 3.0;
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, dx, -0.6 - random.nextDouble(), dz);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
