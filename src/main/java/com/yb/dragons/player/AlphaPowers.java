package com.yb.dragons.player;

import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The power of the alpha (key B by default, dragon form only, recharge of 45 seconds, 3 minutes for the storm, see
 * {@link com.yb.dragons.dragon.DragonAlpha#powerCooldown}).
 *
 * <p>Each race gets its own power, designed one by one with its alpha look:</p>
 * <ul>
 *   <li>forest: the Beam of Life of the Sylvan Tortoise ({@link LifeBeam});</li>
 *   <li>storm: the Storm of the Tempest Dragon ({@link StormCall});</li>
 *   <li>Nether: the Territory of the Demon of the Nether ({@link NetherDomain});</li>
 *   <li>sunlight: the sun of the Sun Steed thrown like a meteorite ({@link SunStrike}).</li>
 * </ul>
 * <p>The other races still use the same temporary power: the alpha wave, a ring of their element that
 * pushes the monsters away.</p>
 */
public final class AlphaPowers {
    private AlphaPowers() {}

    /** @return true when the power was used (the recharge starts) */
    public static boolean use(ServerPlayer player, DragonPlayerData data) {
        DragonType type = data.type();
        if (type == null) return false;
        return switch (type) {
            // the powers of the races come here, one by one
            case FOREST -> LifeBeam.start(player, data);
            case STORM -> StormCall.start(player, data);
            case NETHER -> NetherDomain.start(player, data);
            case SUNLIGHT, MOONLIGHT -> SunStrike.start(player, data);
            default -> alphaWave(player, type);
        };
    }

    /** Monsters (and the mobs that attack this player) around a point: the targets of the powers. */
    static List<LivingEntity> targets(ServerLevel level, ServerPlayer player, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == player || !e.isAlive() || e.isSpectator()) return false;
            if (e instanceof WildDragon d && d.isDisplay()) return false;
            if (e.distanceToSqr(center) > radius * radius) return false;
            return e instanceof Enemy || (e instanceof Mob mob && mob.getTarget() == player);
        });
    }

    /** Temporary power of every alpha: a ring of its element that throws the monsters back. */
    private static boolean alphaWave(ServerPlayer player, DragonType type) {
        ServerLevel level = player.level();
        double x = player.getX(), y = player.getY() + 0.6, z = player.getZ();
        ParticleOptions breath = type.breathParticle();
        for (int ring = 0; ring < 2; ring++) {
            for (int i = 0; i < 64; i++) {
                double a = i / 64.0 * Mth.TWO_PI;
                level.sendParticles(breath, true, false, x, y + ring * 0.8, z, 0, Math.cos(a), 0.03, Math.sin(a), 0.7 + ring * 0.25);
            }
        }
        level.sendParticles(DragonType.flash(type.secondaryColor()), true, true, x, y + 1, z, 1, 0, 0, 0, 0);
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.8F);
        level.playSound(null, x, y, z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.5F, 0.7F);
        for (LivingEntity e : targets(level, player, player.position(), 9)) {
            Vec3 push = e.position().subtract(player.position());
            double dist = Math.max(0.5, push.horizontalDistance());
            double strength = 1.8 * (1 - Math.min(1, dist / 9)) + 0.5;
            e.push(push.x / dist * strength, 0.45, push.z / dist * strength);
            e.needsSync = true;
        }
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.wave").withStyle(ChatFormatting.GRAY), true);
        return true;
    }
}
