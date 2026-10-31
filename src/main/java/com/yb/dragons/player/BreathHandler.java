package com.yb.dragons.player;

import com.yb.dragons.Config;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Breath (hold the key) and roar (stage 3+) of the player dragons. */
public final class BreathHandler {
    private BreathHandler() {}

    /** Where the breath leaves the mouth: in front of the eyes, further for big dragons. */
    public static Vec3 mouth(LivingEntity entity, float renderScale) {
        Vec3 look = entity.getLookAngle();
        return entity.getEyePosition().add(look.scale(0.4 + renderScale * 1.6));
    }

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        DragonType type = data.type();
        if (type == null) return;
        DragonStage stage = data.stage();
        var stats = data.stats();
        data.energy -= DragonPlayerManager.BREATH_COST;
        if (data.energy <= 0) {
            data.energy = 0;
            data.breathing = false;
            data.publicDirty = true;
            return;
        }
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        // the hybrid breathes from its human mouth, with a shorter and weaker breath
        Vec3 origin = data.isTortoise() ? LifeBeam.mouth(player).add(look.scale(0.5))
                : data.isSerpent() ? AlphaHitboxes.serpentMouth(player).add(look.scale(0.5))
                : data.isSteed() ? AlphaSun.mouth(player.position(), player.yBodyRot).add(look.scale(0.5))
                : data.isActiveDragon() ? mouth(player, stats.renderScale()) : player.getEyePosition().add(look.scale(0.5));
        int range = Math.max(3, Math.round(stats.breathRange() * data.form().rangeFactor()));
        float damage = stats.breathDamage() * data.powerFactor();

        spawnBreathParticles(level, type, origin, look, range, data.breathTicks, level.getRandom());

        if (data.breathTicks % 10 == 0) {
            level.playSound(null, origin.x, origin.y, origin.z, breathSound(type), SoundSource.PLAYERS, 0.7F,
                    0.8F + level.getRandom().nextFloat() * 0.4F);
        }

        if (data.breathTicks % 4 == 0) {
            for (LivingEntity target : targetsInCone(level, player, origin, look, range, 0.42)) {
                DragonPowers.breathHit(level, player, target, type, stage.number(), damage);
            }
            if (type == DragonType.FIRE && Config.BREATH_GRIEFING.get()) {
                igniteBlock(level, player, origin, look, range);
            }
        }
        data.breathTicks++;
    }

    public static net.minecraft.sounds.SoundEvent breathSound(DragonType type) {
        return switch (type) {
            case FIRE, NETHER -> SoundEvents.BLAZE_SHOOT;
            case ICE, MOONLIGHT -> SoundEvents.POWDER_SNOW_STEP;
            case WATER -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
            case STORM -> SoundEvents.TRIDENT_THUNDER.value();
            case SUNLIGHT -> SoundEvents.AMETHYST_BLOCK_CHIME;
            default -> SoundEvents.ENDER_DRAGON_SHOOT;
        };
    }

    /** Spawns a cone of particles going in the {@code look} direction. */
    public static void spawnBreathParticles(ServerLevel level, DragonType type, Vec3 origin, Vec3 look, int range,
                                            int tick, RandomSource random) {
        ParticleOptions particle = type.breathParticle();
        double speed = 0.25 + range * 0.06;
        for (int i = 0; i < 6; i++) {
            Vec3 dir = look.add((random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.35,
                    (random.nextDouble() - 0.5) * 0.35).normalize();
            // count = 0 -> the "spread" values are used as a velocity
            level.sendParticles(particle, origin.x, origin.y, origin.z, 0, dir.x, dir.y, dir.z, speed);
        }
        // a few extra particles along the beam for long breaths
        for (int i = 0; i < 2; i++) {
            double d = random.nextDouble() * range;
            Vec3 p = origin.add(look.scale(d));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.3, 0.3, 0.3, 0.01);
        }
        if (type == DragonType.FIRE || type == DragonType.NETHER) {
            level.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /** Living entities inside the breath cone, visible from the mouth. */
    public static List<LivingEntity> targetsInCone(ServerLevel level, LivingEntity attacker, Vec3 origin, Vec3 look, double range, double spread) {
        AABB box = new AABB(origin, origin.add(look.scale(range))).inflate(range * spread + 1.0);
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == attacker || !e.isAlive() || e.isSpectator()) return false;
            if (e instanceof com.yb.dragons.entity.WildDragon d && d.isDisplay()) return false;
            if (attacker.isPassengerOfSameVehicle(e)) return false;
            Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
            double dist = to.length();
            if (dist > range + e.getBbWidth()) return false;
            double cos = to.normalize().dot(look);
            if (cos < 1.0 - spread && dist > 1.5) return false;
            return hasLineOfSight(level, attacker, origin, e);
        });
    }

    private static boolean hasLineOfSight(ServerLevel level, Entity attacker, Vec3 from, Entity target) {
        Vec3 to = target.getBoundingBox().getCenter();
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void igniteBlock(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 look, int range) {
        Vec3 end = origin.add(look.scale(range));
        BlockHitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
        if (level.isEmptyBlock(firePos)) {
            BlockState fire = BaseFireBlock.getState(level, firePos);
            if (fire.canSurvive(level, firePos)) level.setBlockAndUpdate(firePos, fire);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Roar
    // ------------------------------------------------------------------------------------------------------

    public static void roar(ServerPlayer player, DragonPlayerData data) {
        DragonType type = data.type();
        if (type == null) return;
        ServerLevel level = player.level();
        int s = data.stage().number();
        float factor = data.powerFactor();
        // the alpha roars like a dragon of a sixth stage
        double radius = (5 + (s + (data.isAlpha() ? 1 : 0)) * 2) * (0.5 + factor * 0.5);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 1.3F - s * 0.1F);
        // ring of particles
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Mth.TWO_PI;
            level.sendParticles(type.breathParticle(), player.getX(), player.getY() + 0.3, player.getZ(), 0,
                    Math.cos(a), 0.05, Math.sin(a), 0.5 + radius * 0.05);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);

        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            if (e instanceof com.yb.dragons.entity.WildDragon d && d.isDisplay()) continue;
            if (e instanceof Player other && !player.canHarmPlayer(other)) continue;
            Vec3 push = e.position().subtract(player.position()).normalize();
            double strength = (1.0 + s * 0.25) * factor;
            e.push(push.x * strength, 0.4 + s * 0.05, push.z * strength);
            e.needsSync = true;
            DragonPowers.breathHit(level, player, e, type, s, data.stats().breathDamage() * factor);
        }
    }

}
