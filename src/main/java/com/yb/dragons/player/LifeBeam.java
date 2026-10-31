package com.yb.dragons.player;

import com.yb.dragons.Config;
import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.BeamTrace;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.AlphaHitbox;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.world.Terraform;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The power of the alpha of the forest (key B, dragon form): the Beam of Life, a continuous beam of
 * 6 seconds fired from the mouth of the Sylvan Tortoise where the player looks.
 * <ul>
 *   <li>Where it touches the ground, a forest grows at once and forever ({@link Terraform}).</li>
 *   <li>It heals the allies it crosses (players, tamed animals, villagers, every creature that is not a
 *       monster), hurts the monsters, and kills the vanilla Ender Dragon in one shot.</li>
 *   <li>Its range is the view distance of the player: it stops at the edge of the chunks loaded for him
 *       (4 chunks of render distance = it never goes past those 4 chunks) and never touches an unloaded chunk.</li>
 * </ul>
 * B again stops it early. The recharge of 45 seconds starts when it is fired.
 */
public final class LifeBeam {
    /** Columns already grown by the current beam of each player (they are not rolled again). */
    private static final Map<UUID, LongSet> GROWN = new HashMap<>();
    /** Where the beam of each player touched the ground at the previous tick (to grow along its sweep). */
    private static final Map<UUID, BlockPos> LAST_HIT = new HashMap<>();
    /** Distance (blocks) between two discs of forest along a fast sweep of the beam, and the most discs per tick. */
    private static final double SWEEP_STEP = 2.5;
    private static final int SWEEP_MAX = 24;

    private LifeBeam() {}

    /** View distance of the player (chunks): his own setting, capped by the server. */
    public static int viewChunks(ServerPlayer player) {
        int server = player.level().getServer().getPlayerList().getViewDistance();
        return Math.max(2, Math.min(player.requestedViewDistance(), Math.max(2, server)));
    }

    /** The chunks loaded for this player right now. */
    public static Terraform.View view(ServerPlayer player) {
        return new Terraform.View(player.level(), player.chunkPosition().x(), player.chunkPosition().z(), viewChunks(player));
    }

    public static Vec3 mouth(Player player) {
        return AlphaForest.mouth(player.position(), player.yBodyRot, AlphaForest.headYaw(player.getYHeadRot(), player.yBodyRot),
                AlphaForest.headPitch(player.getXRot()), 1.0F);
    }

    /** Where the beam of this player stops now (from his eyes, along his look). */
    public static BeamTrace.Result trace(ServerPlayer player, Terraform.View view) {
        return BeamTrace.trace(player.level(), view::hasChunk, player.getEyePosition(), player.getViewVector(1.0F),
                BeamTrace.maxLength(view.radius()));
    }

    // ------------------------------------------------------------------------------------------------------

    /** @return true when the beam started */
    public static boolean start(ServerPlayer player, DragonPlayerData data) {
        if (!data.isTortoise() || data.inRitual()) return false;
        ServerLevel level = player.level();
        data.beamTicks = 0;
        data.beamStart = level.getGameTime();
        data.breathing = false;
        data.markAll();
        GROWN.put(player.getUUID(), new LongOpenHashSet());
        Vec3 m = mouth(player);
        level.playSound(null, m.x, m.y, m.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0F, 1.3F);
        level.playSound(null, m.x, m.y, m.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.life_beam")
                .withStyle(s -> s.withColor(DragonType.FOREST.textColor())), true);
        DragonPlayerManager.refreshAttributes(player, data);
        return true;
    }

    public static void stop(ServerPlayer player, DragonPlayerData data) {
        if (!data.beaming()) return;
        data.beamTicks = -1;
        data.beamStart = -1;
        data.markAll();
        GROWN.remove(player.getUUID());
        LAST_HIT.remove(player.getUUID());
        player.level().playSound(null, player.getX(), player.getY() + 4, player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
        DragonPlayerManager.refreshAttributes(player, data);
    }

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        if (!data.beaming()) return;
        if (!data.isTortoise() || data.inRitual() || player.isDeadOrDying() || player.isSpectator()) {
            stop(player, data);
            return;
        }
        int t = data.beamTicks;
        ServerLevel level = player.level();
        Terraform.View view = view(player);
        BeamTrace.Result hit = trace(player, view);
        Vec3 mouth = mouth(player);
        Vec3 end = hit.end();

        // --- the forest grows where the beam touches the ground, and along the way it swept since the last tick ---
        BlockPos last = hit.block() == null ? null : LAST_HIT.get(player.getUUID());
        if (hit.block() != null && Config.ALPHA_TERRAFORM.get()) {
            LongSet done = GROWN.computeIfAbsent(player.getUUID(), u -> new LongOpenHashSet());
            int trees = 2;
            if (last != null) {
                double dist = Math.sqrt(last.distSqr(hit.block()));
                int steps = (int) Math.min(SWEEP_MAX, Math.floor(dist / SWEEP_STEP));
                for (int i = 1; i <= steps && trees > 0; i++) {
                    double k = i / (double) (steps + 1);
                    BlockPos p = BlockPos.containing(last.getX() + (hit.block().getX() - last.getX()) * k + 0.5,
                            last.getY() + (hit.block().getY() - last.getY()) * k + 0.5, last.getZ() + (hit.block().getZ() - last.getZ()) * k + 0.5);
                    trees -= Terraform.grow(level, player, view, p, AlphaForest.TERRAFORM_RADIUS, level.getRandom(), done, trees);
                }
            }
            Terraform.grow(level, player, view, hit.block(), AlphaForest.TERRAFORM_RADIUS, level.getRandom(), done, Math.max(1, trees));
        }
        if (hit.block() != null && !hit.water()) LAST_HIT.put(player.getUUID(), hit.block());
        else LAST_HIT.remove(player.getUUID());
        if (hit.block() != null) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, true, false, end.x, end.y + 0.3, end.z, 6, 1.2, 0.4, 1.2, 0);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, true, false, end.x, end.y + 0.5, end.z, 4, 1.0, 0.6, 1.0, 0);
            if (t % 3 == 0) level.sendParticles(ParticleTypes.COMPOSTER, true, false, end.x, end.y + 0.2, end.z, 6, 1.4, 0.3, 1.4, 0);
            if (t % 6 == 0) level.playSound(null, end.x, end.y, end.z, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.5F, 0.8F);
        }
        if (t % 20 == 0) level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 3.0F, 1.6F);

        // --- the creatures crossed by the beam -----------------------------------------------------------------
        if (t % AlphaForest.BEAM_HIT_EVERY == 0) {
            DamageSource source = player.damageSources().indirectMagic(player, player);
            for (LivingEntity e : crossed(level, player, mouth, end)) {
                if (e instanceof EnderDragon dragon) {
                    slay(level, player, dragon, source);
                } else if (isEnemy(e, player)) {
                    e.hurtServer(level, source, AlphaForest.BEAM_DAMAGE);
                } else {
                    e.heal(AlphaForest.BEAM_HEAL);
                    if (t % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0), player);
                    if (t % 8 == 0) level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 1, 0.3, 0.2, 0.3, 0);
                }
            }
        }

        data.beamTicks++;
        if (data.beamTicks >= AlphaForest.BEAM_DURATION) stop(player, data);
    }

    /** The living creatures along the beam (each one once, the parts of the Ender Dragon give the dragon). */
    private static Set<LivingEntity> crossed(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        Set<LivingEntity> result = new LinkedHashSet<>();
        double length = to.distanceTo(from);
        if (length < 0.01) return result;
        Vec3 dir = to.subtract(from).scale(1 / length);
        for (double s = 0; s < length; s += 12) {
            Vec3 a = from.add(dir.scale(s)), b = from.add(dir.scale(Math.min(length, s + 12)));
            AABB box = new AABB(a, b).inflate(2.0);
            for (Entity e : level.getEntities(player, box, e -> e.isAlive() && !e.isSpectator())) {
                Entity target = e;
                if (e instanceof EnderDragonPart part) target = part.parentMob;
                if (e instanceof AlphaHitbox part) target = part.owner();
                if (!(target instanceof LivingEntity living) || target == player || !living.isAlive()) continue;
                if (target instanceof WildDragon d && d.isDisplay()) continue;
                AABB bb = e.getBoundingBox().inflate(0.6);
                if (bb.contains(a) || bb.clip(a, b).isPresent()) result.add(living);
            }
        }
        return result;
    }

    /** Monsters, and every creature that attacks this player. */
    private static boolean isEnemy(LivingEntity e, ServerPlayer player) {
        if (e instanceof Enemy) return true;
        return e instanceof Mob mob && mob.getTarget() == player;
    }

    /** The vanilla Ender Dragon dies at once (with its normal death: the portal, the egg and the experience). */
    private static void slay(ServerLevel level, ServerPlayer player, EnderDragon dragon, DamageSource source) {
        DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
        if (dragon.isDeadOrDying() || phase.getPhase() == EnderDragonPhase.DYING) return;
        // a perched dragon would skip its death flight: it takes off first
        if (phase.isSitting()) dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        dragon.hurt(level, dragon.head, source, dragon.getMaxHealth() * 100.0F);
        Component msg = Component.translatable("message.bdragon.alpha_power.ender_dragon", player.getDisplayName())
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        for (ServerPlayer p : level.players()) p.sendSystemMessage(msg);
    }

    public static void forget(UUID player) {
        GROWN.remove(player);
        LAST_HIT.remove(player);
    }
}
