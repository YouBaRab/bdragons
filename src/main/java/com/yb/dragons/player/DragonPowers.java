package com.yb.dragons.player;

import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The elemental powers of each race: passive buffs, immunities, breath effects and reactions.
 * Shared by player dragons and wild dragons so that both fight with the same element.
 */
public final class DragonPowers {
    private DragonPowers() {}

    // ------------------------------------------------------------------------------------------------------
    // Immunities
    // ------------------------------------------------------------------------------------------------------

    public static boolean isImmune(DragonType type, DamageSource source) {
        for (var tag : type.immunities()) {
            if (source.is(tag)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------
    // Passive buffs (called every second on server)
    // ------------------------------------------------------------------------------------------------------

    public static void tickPassive(ServerPlayer player, DragonPlayerData data) {
        DragonType type = data.type();
        if (type == null) return;
        int s = data.stage().number();
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        boolean day = level.isBrightOutside();
        boolean sky = level.canSeeSky(pos.above());

        switch (type) {
            case FIRE -> {
                buff(player, MobEffects.FIRE_RESISTANCE, 0);
                if (s >= 3 && level.dimension() == Level.NETHER) buff(player, MobEffects.STRENGTH, 0);
            }
            case ICE -> {
                if (level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel())) {
                    if (s >= 2) buff(player, MobEffects.SPEED, 0);
                    if (s >= 3) buff(player, MobEffects.RESISTANCE, 0);
                }
            }
            case WATER -> {
                buff(player, MobEffects.WATER_BREATHING, 0);
                if (s >= 2 && player.isInWater()) buff(player, MobEffects.DOLPHINS_GRACE, 0);
                if (s >= 3 && (player.isInWater() || level.isRainingAt(pos.above()))) buff(player, MobEffects.REGENERATION, 0);
            }
            case FOREST -> {
                cure(player, MobEffects.POISON);
                if (day && sky) buff(player, MobEffects.REGENERATION, s >= 4 ? 1 : 0);
                if (s >= 3) buff(player, MobEffects.SATURATION, 0, 1);
            }
            case NETHER -> {
                buff(player, MobEffects.FIRE_RESISTANCE, 0);
                if (s >= 3 && level.dimension() == Level.NETHER) buff(player, MobEffects.STRENGTH, s >= 5 ? 1 : 0);
            }
            case END -> {
                if (s >= 4 && level.dimension() == Level.END) {
                    buff(player, MobEffects.STRENGTH, 0);
                    buff(player, MobEffects.REGENERATION, 0);
                }
            }
            case TERRA -> {
                if (s >= 2) buff(player, MobEffects.HASTE, s >= 4 ? 1 : 0);
                if (s >= 4) buff(player, MobEffects.RESISTANCE, 0);
            }
            case SUNLIGHT -> {
                if (day && sky) {
                    buff(player, MobEffects.REGENERATION, 0);
                    if (s >= 4) burnUndead(player, 6 + s);
                }
            }
            case STORM -> {
                buff(player, MobEffects.SPEED, s >= 3 ? 1 : 0);
                // a real thunderstorm, or the Storm called by the alpha
                if (level.isThundering() || data.storming()) buff(player, MobEffects.STRENGTH, 1);
            }
            case MOONLIGHT -> {
                buffNightVision(player);
                if (!day) {
                    buff(player, MobEffects.SPEED, 0);
                    if (s >= 3) buff(player, MobEffects.STRENGTH, 0);
                    if (s >= 4 && player.isShiftKeyDown()) buff(player, MobEffects.INVISIBILITY, 0);
                }
            }
            case WITHER -> cure(player, MobEffects.WITHER);
            default -> {
            }
        }
    }

    private static void buff(LivingEntity e, Holder<MobEffect> effect, int amplifier) {
        buff(e, effect, amplifier, 60);
    }

    private static void buff(LivingEntity e, Holder<MobEffect> effect, int amplifier, int duration) {
        MobEffectInstance current = e.getEffect(effect);
        if (current != null && current.getAmplifier() > amplifier) return;
        e.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static void buffNightVision(LivingEntity e) {
        // > 10 seconds so that the screen never flickers
        buff(e, MobEffects.NIGHT_VISION, 0, 400);
    }

    private static void cure(LivingEntity e, Holder<MobEffect> effect) {
        if (e.hasEffect(effect)) e.removeEffect(effect);
    }

    private static void burnUndead(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, le -> le.is(EntityTypeTags.UNDEAD))) {
            e.igniteForTicks(60);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Breath (shared by players and wild dragons)
    // ------------------------------------------------------------------------------------------------------

    /**
     * Damages {@code target} with the breath of the given race.
     * @param level 1..5 (stage of a player, rank of a wild dragon)
     */
    public static void breathHit(ServerLevel world, LivingEntity attacker, LivingEntity target, DragonType type, int level, float damage) {
        DamageSource source = attacker instanceof Player p ? world.damageSources().playerAttack(p) : world.damageSources().mobAttack(attacker);
        float dmg = damage;
        boolean night = world.isDarkOutside();
        Vec3 push = target.position().subtract(attacker.position()).normalize();

        switch (type) {
            case FIRE -> {
                if (!target.fireImmune()) target.igniteForTicks(60 + 20 * level);
            }
            case ICE -> {
                target.setTicksFrozen(Math.min(target.getTicksFrozen() + 40, target.getTicksRequiredToFreeze() + 60));
                effect(target, attacker, MobEffects.SLOWNESS, 60, level >= 4 ? 2 : 1);
            }
            case WATER -> {
                target.clearFire();
                if (target.isSensitiveToWater() || target.fireImmune()) dmg *= 2;
                target.push(push.x * 0.4, 0.1, push.z * 0.4);
            }
            case FOREST -> effect(target, attacker, MobEffects.POISON, 80, level >= 4 ? 1 : 0);
            case NETHER -> {
                if (!target.fireImmune()) target.igniteForTicks(60);
                effect(target, attacker, MobEffects.WITHER, 40, 0);
            }
            case END -> effect(target, attacker, MobEffects.LEVITATION, 20, 0);
            case TERRA -> {
                effect(target, attacker, MobEffects.BLINDNESS, 40, 0);
                target.push(push.x * 0.3, 0.05, push.z * 0.3);
            }
            case SUNLIGHT -> {
                effect(target, attacker, MobEffects.GLOWING, 100, 0);
                if (target.is(EntityTypeTags.UNDEAD)) {
                    dmg *= 2;
                    target.igniteForTicks(100);
                }
            }
            case STORM -> {
                if (world.getRandom().nextFloat() < 0.025F * level && world.canSeeSky(target.blockPosition())) {
                    // the lightning is only visual (no fire griefing), its damage is applied here
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) {
                        bolt.snapTo(target.getX(), target.getY(), target.getZ());
                        bolt.setVisualOnly(true);
                        world.addFreshEntity(bolt);
                    }
                    dmg += 3 + level;
                    target.igniteForTicks(40);
                }
            }
            case MOONLIGHT -> {
                effect(target, attacker, MobEffects.WEAKNESS, 80, 0);
                effect(target, attacker, MobEffects.SLOWNESS, 40, 0);
                if (night) dmg *= 1.5F;
            }
            case WITHER -> effect(target, attacker, MobEffects.WITHER, 80, 1);
            default -> {
            }
        }
        // against a wild dragon each hit of the breath counts (no pause between the hits), each one a little weaker
        if (target instanceof com.yb.dragons.entity.WildDragon) {
            target.setInvulnerableTime(0);
            dmg *= 0.6F;
        }
        target.hurtServer(world, source, dmg);
    }

    private static void effect(LivingEntity target, LivingEntity source, Holder<MobEffect> effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, amplifier), source);
    }

    // ------------------------------------------------------------------------------------------------------
    // Reactions when a dragon player is hit / hits
    // ------------------------------------------------------------------------------------------------------

    /** The dragon player {@code victim} was hurt by {@code attacker}. */
    public static void onDragonHurt(ServerPlayer victim, DragonPlayerData data, LivingEntity attacker) {
        DragonType type = data.type();
        if (type == null || data.stage().ordinal() < DragonStage.ELDER.ordinal()) return;
        ServerLevel level = victim.level();
        switch (type) {
            case FIRE, NETHER -> attacker.igniteForTicks(80);
            case ICE -> attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), victim);
            case STORM -> {
                attacker.hurtServer(level, level.damageSources().lightningBolt(), 2.0F + data.stage().number());
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY(1.0), attacker.getZ(), 15, 0.4, 0.6, 0.4, 0.2);
            }
            case WITHER -> attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), victim);
            case FOREST -> attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), victim);
            default -> {
            }
        }
    }

    /** The dragon player hit {@code target} for {@code amount}. */
    public static void onDragonAttack(ServerPlayer attacker, DragonPlayerData data, LivingEntity target, float amount) {
        if (data.type() == DragonType.WITHER && data.stage().number() >= 3) {
            attacker.heal(amount * 0.15F);
        }
    }

}
