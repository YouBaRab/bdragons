package com.yb.dragons.player;

import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The power of the alpha of the storm (key B, dragon form, recharge of 3 minutes): the Storm.
 * <ul>
 *   <li>The Tempest Dragon roars and calls a storm of one minute around him: dark clouds, rain, and harmless lightning
 *       bolts falling everywhere around him (he is immune to lightning anyway).</li>
 *   <li>{@link AlphaStorm#STRIKES} aimed bolts strike the hostile mobs around him, one after the other. Each one hits a
 *       mob chosen at random, but the mob with the most health has the best chance (the strongest are seen as the
 *       biggest threat), and the mob hit by the previous bolt has little chance to be chosen again. A bolt deals
 *       {@link AlphaStorm#BOLT_DAMAGE}: three of them kill the Ender Dragon.</li>
 * </ul>
 * The storm follows the dragon. It stops when he dies, leaves the world or changes dimension.
 */
public final class StormCall {
    /** The aimed bolts of the current storm of each player. */
    private static final class Strikes {
        int left = AlphaStorm.STRIKES;
        int next = AlphaStorm.FIRST_STRIKE;
        @Nullable UUID last;
    }

    private static final Map<UUID, Strikes> STRIKES = new HashMap<>();

    private StormCall() {}

    /** @return true when the storm started */
    public static boolean start(ServerPlayer player, DragonPlayerData data) {
        if (!data.isSerpent() || data.inRitual() || data.storming()) return false;
        ServerLevel level = player.level();
        data.stormTicks = 0;
        data.stormStart = level.getGameTime();
        data.breathing = false;
        data.markAll();
        STRIKES.put(player.getUUID(), new Strikes());

        // the roar that calls the storm
        Vec3 m = AlphaHitboxes.serpentMouth(player);
        level.playSound(null, m.x, m.y, m.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 5.0F, 0.7F);
        level.playSound(null, m.x, m.y, m.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 4.0F, 0.8F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, m.x, m.y, m.z, 80, 1.2, 1.2, 1.2, 0.6);
        level.sendParticles(DragonType.flash(DragonType.STORM.secondaryColor()), true, true, m.x, m.y, m.z, 1, 0, 0, 0, 0);
        // the first bolts fall around him
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            double a = random.nextDouble() * Mth.TWO_PI, r = 7 + random.nextDouble() * 6;
            ambientBolt(level, player, player.getX() + Math.cos(a) * r, player.getZ() + Math.sin(a) * r);
        }
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.storm", AlphaStorm.STORM_DURATION / 20)
                .withStyle(s -> s.withColor(DragonType.STORM.textColor())), true);
        return true;
    }

    public static void stop(ServerPlayer player, DragonPlayerData data) {
        STRIKES.remove(player.getUUID());
        if (!data.storming()) return;
        data.stormTicks = -1;
        data.stormStart = -1;
        data.markAll();
    }

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        if (!data.storming()) return;
        if (data.type() != DragonType.STORM || !data.isAlpha() || player.isDeadOrDying() || player.isSpectator()) {
            stop(player, data);
            return;
        }
        int t = data.stormTicks;
        ServerLevel level = player.level();
        RandomSource random = level.getRandom();

        // lightning everywhere around him (harmless, about one per second, each one with its thunder)
        if (t >= AlphaStorm.ROAR_TICKS && t < AlphaStorm.STORM_DURATION - 20 && random.nextInt(AlphaStorm.AMBIENT_EVERY) == 0) {
            double a = random.nextDouble() * Mth.TWO_PI, r = 6 + random.nextDouble() * (AlphaStorm.AMBIENT_RADIUS - 6);
            ambientBolt(level, player, player.getX() + Math.cos(a) * r, player.getZ() + Math.sin(a) * r);
        }

        // the aimed bolts, one after the other
        Strikes strikes = STRIKES.computeIfAbsent(player.getUUID(), u -> new Strikes());
        if (strikes.left > 0 && t >= strikes.next) {
            LivingEntity target = chooseTarget(level, player, strikes.last, random);
            if (target != null) {
                strike(level, player, target);
                strikes.left--;
                strikes.last = target.getUUID();
                strikes.next = t + AlphaStorm.STRIKE_INTERVAL;
            } else {
                strikes.next = t + AlphaStorm.STRIKE_RETRY;
            }
        }

        data.stormTicks++;
        if (data.stormTicks >= AlphaStorm.STORM_DURATION) stop(player, data);
    }

    /**
     * A hostile mob around the dragon, chosen at random: the more health it has, the better its chance (up to 3 times
     * the chance of a weak mob), and the mob hit by the previous bolt has little chance to be hit again.
     */
    @Nullable
    static LivingEntity chooseTarget(ServerLevel level, ServerPlayer player, @Nullable UUID last, RandomSource random) {
        List<LivingEntity> targets = AlphaPowers.targets(level, player, player.position(), AlphaStorm.STRIKE_RADIUS);
        if (targets.isEmpty()) return null;
        float strongest = 0;
        for (LivingEntity e : targets) strongest = Math.max(strongest, e.getHealth());
        float[] weights = new float[targets.size()];
        float total = 0;
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity e = targets.get(i);
            float w = 1.0F + 2.0F * (strongest > 0 ? e.getHealth() / strongest : 1.0F);
            if (targets.size() > 1 && e.getUUID().equals(last)) w *= AlphaStorm.SAME_TARGET_WEIGHT;
            weights[i] = w;
            total += w;
        }
        float roll = random.nextFloat() * total;
        for (int i = 0; i < targets.size(); i++) {
            roll -= weights[i];
            if (roll <= 0) return targets.get(i);
        }
        return targets.get(targets.size() - 1);
    }

    /** An aimed bolt: it falls on the mob and deals {@link AlphaStorm#BOLT_DAMAGE} (it is the dragon who hits). */
    private static void strike(ServerLevel level, ServerPlayer player, LivingEntity target) {
        bolt(level, target.getX(), target.getY(), target.getZ());
        DamageSource source = player.damageSources().indirectMagic(player, player);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, target.getX(), target.getY(0.5), target.getZ(), 50, 0.6, 1.0, 0.6, 0.4);
        if (target instanceof EnderDragon dragon) {
            hitEnderDragon(level, player, dragon, source);
            return;
        }
        target.hurtServer(level, source, AlphaStorm.BOLT_DAMAGE);
        if (target.isAlive() && !target.fireImmune()) target.igniteForTicks(80);
    }

    /** The Ender Dragon is hit on its head (its other parts take a quarter of the damage): three bolts kill it. */
    private static void hitEnderDragon(ServerLevel level, ServerPlayer player, EnderDragon dragon, DamageSource source) {
        DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
        if (dragon.isDeadOrDying() || phase.getPhase() == EnderDragonPhase.DYING) return;
        // a perched dragon would skip its death flight: it takes off first when this bolt kills it
        if (phase.isSitting() && dragon.getHealth() <= AlphaStorm.BOLT_DAMAGE) {
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }
        dragon.hurt(level, dragon.head, source, AlphaStorm.BOLT_DAMAGE);
        if (dragon.isDeadOrDying() || dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
            Component msg = Component.translatable("message.bdragon.alpha_power.storm_ender_dragon", player.getDisplayName())
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            for (ServerPlayer p : level.players()) p.sendSystemMessage(msg);
        }
    }

    /** A harmless bolt on the ground at (x, z), near the height of the dragon (never in an unloaded chunk). */
    private static void ambientBolt(ServerLevel level, ServerPlayer player, double x, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        if (!level.hasChunk(bx >> 4, bz >> 4)) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, Mth.floor(player.getY()) + 16, bz);
        for (int i = 0; i < 48; i++) {
            if (!level.getBlockState(pos).isAir()) {
                bolt(level, x, pos.getY() + 1, z);
                return;
            }
            pos.move(0, -1, 0);
        }
    }

    /** A lightning bolt that only shows (and thunders): no fire, no damage, the damage of the aimed bolts is given above. */
    private static void bolt(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }
}
