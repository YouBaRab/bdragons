package com.yb.dragons.item;

import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import com.yb.dragons.world.lair.DragonLair;
import com.yb.dragons.world.lair.LairManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Challenge Horn: blow into it and the dragons answer. It reveals the nearest lair of a wild dragon
 * strong enough for your next evolution (rank of the next stage or more), up to ~6000 blocks away.
 * The lair is then shown on your map (JourneyMap) and in your codex.
 */
public class ChallengeHornItem extends Item {
    /** Search radius in lair regions (512 blocks in the Overworld). */
    private static final int SEARCH_REGIONS = 12;

    public ChallengeHornItem(Properties properties) {
        super(properties);
    }

    /** A wild dragon of rank I or II answers the horn: it comes down from the sky a little in front of the player. */
    private static void summonChallenger(ServerLevel server, ServerPlayer player, int rank) {
        java.util.List<DragonType> races = DragonType.SELECTABLE;
        DragonType type = races.get(server.getRandom().nextInt(races.size()));
        com.yb.dragons.entity.WildDragon dragon = com.yb.dragons.ModRegistry.WILD_DRAGON.get().create(server,
                net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (dragon == null) return;
        Vec3 look = player.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        Vec3 p = player.position().add(look.scale(14)).add(0, 8, 0);
        dragon.snapTo(p.x, p.y, p.z, player.getYRot() + 180, 0);
        dragon.setup(type, rank);
        dragon.setPersistenceRequired();
        dragon.setTarget(player);
        server.addFreshEntity(dragon);
        server.sendParticles(type.breathParticle(), p.x, p.y, p.z, 60, 1.5, 1.5, 1.5, 0.05);
        server.playSound(null, p.x, p.y, p.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5F, 1.4F);
        player.sendSystemMessage(Component.translatable("message.bdragon.horn_summon", type.displayName(), LairManager.roman(rank))
                .withStyle(s -> s.withColor(type.textColor()).withBold(true)));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;

        DragonPlayerData data = DragonPlayerManager.get(sp);
        int rank = 1;
        if (data.isDragon()) rank = data.stage().isMax() ? 5 : Math.max(1, data.stage().next().challengeRank());
        // a legendary dragon who is not an alpha yet hunts the rank V dragons of his own race (their alpha scales)
        DragonType hunt = data.isDragon() && data.stage().isMax() && !data.isAlpha() ? data.type() : null;

        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 2.0F, 0.8F);
        player.getCooldowns().addCooldown(stack, 200);

        // the dragons of rank I and II have no nest: the horn calls one, which comes to fight right away
        if (hunt == null && rank <= 2) {
            summonChallenger(server, sp, rank);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        }

        LairManager manager = LairManager.get(server);
        DragonLair lair = manager.findNearest(server, player.getX(), player.getZ(), rank, hunt != null ? SEARCH_REGIONS + 4 : SEARCH_REGIONS, hunt);
        if (lair == null) {
            sp.sendSystemMessage((hunt != null
                    ? Component.translatable("message.bdragon.horn_nothing_alpha", hunt.displayName())
                    : Component.translatable("message.bdragon.horn_nothing", LairManager.roman(rank))).withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS_SERVER;
        }

        data.addKnownLair(LairManager.knownKey(server, lair.region));
        manager.syncTo(sp, true);
        int distance = (int) Math.sqrt(lair.distanceSqr(player.getX(), player.getZ()));
        Component direction = LairManager.direction(player.getX(), player.getZ(), lair.x, lair.z);
        sp.sendSystemMessage(Component.translatable("message.bdragon.horn_found_1",
                lair.type.displayName(), LairManager.roman(lair.rank)).withStyle(s -> s.withColor(lair.type.textColor()).withBold(true)));
        sp.sendSystemMessage(Component.translatable("message.bdragon.horn_found_2", distance, direction).withStyle(ChatFormatting.YELLOW));
        sp.sendSystemMessage(Component.translatable("message.bdragon.horn_found_3", lair.x, lair.z).withStyle(ChatFormatting.GRAY));

        // a trail of particles pointing at the lair, and the distant growl of the dragon
        Vec3 dir = new Vec3(lair.x - player.getX(), 0, lair.z - player.getZ()).normalize();
        for (int i = 2; i < 14; i++) {
            Vec3 p = player.getEyePosition().add(dir.scale(i));
            server.sendParticles(lair.type.breathParticle(), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
            server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        Vec3 echo = player.position().add(dir.scale(24));
        server.playSound(null, echo.x, player.getY() + 8, echo.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5F, 1.5F - lair.rank * 0.12F);

        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.bdragon.challenge_horn.tooltip_1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("item.bdragon.challenge_horn.tooltip_2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
