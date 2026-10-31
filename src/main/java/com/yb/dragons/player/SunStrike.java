package com.yb.dragons.player;

import com.yb.dragons.Config;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The power of the alpha of the sunlight (key B, dragon form, recharge of 45 seconds): the Sun Steed throws its sun
 * like a meteorite; the Moon Dragon (alpha of the moonlight) roars, the night falls, and it throws a moon from its mouth
 * the same way (same damage, same explosion). At night (when the players could sleep in a bed) the day rises at once, like the command
 * {@code /time set day}.
 * <ol>
 *   <li>It rears up on its hind legs, like a horse throwing off its rider, its front legs pawing the air
 *       ({@link AlphaSun#ULT_TICKS}, the animation of the client).</li>
 *   <li>At {@link AlphaSun#LAUNCH_TICKS} its sun leaves its place above its head and flies straight to the point the
 *       player aims at ({@link AlphaSun#SUN_SPEED} blocks per tick, up to {@link AlphaSun#RANGE} blocks). It explodes at
 *       the first thing on its way: a block or a creature.</li>
 *   <li>The explosion: a white ball of light grows where the sun fell, up to {@link AlphaSun#RADIUS} blocks, in
 *       {@link AlphaSun#EXPLODE_TICKS} ticks. Everything inside disappears but the bedrock (and the blocks that cannot
 *       be broken), leaving a crater of 10 blocks deep; the creatures inside are hurt like by a huge TNT (the steed
 *       and its pets are spared). Spawn protection is respected, and the destruction of the blocks can be turned off
 *       in the configuration.</li>
 *   <li>The sun grows back above its head during the recharge (the client draws it from the start time).</li>
 * </ol>
 */
public final class SunStrike {
    /** The thrown sun of a player: its way, then its explosion. */
    private static final class Shot {
        /** A moon thrown by the Moon Dragon (from its mouth), or the sun of the Sun Steed. */
        boolean moon;
        Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
        int flight = 1;
        /** Ticks since the impact (-1 = still flying). */
        int exploded = -1;
        Vec3 centre = Vec3.ZERO;
        double doneRadius = -1;
        final Set<Integer> hurt = new HashSet<>();
    }

    private static final Map<UUID, Shot> SHOTS = new HashMap<>();
    /** The colours of the sun. */
    private static final DustParticleOptions SUN_CORE = new DustParticleOptions(0xFFE36A, 4.0F);
    private static final DustParticleOptions SUN_EDGE = new DustParticleOptions(0xFF8A1E, 3.0F);
    private static final DustParticleOptions LIGHT = new DustParticleOptions(0xFFFFFF, 4.0F);
    /** The colours of the moon. */
    private static final DustParticleOptions MOON_CORE = new DustParticleOptions(0xFFF6C8, 4.0F);
    private static final DustParticleOptions MOON_EDGE = new DustParticleOptions(0xBFD8FF, 3.0F);

    private SunStrike() {}

    /** @return true when the power started */
    public static boolean start(ServerPlayer player, DragonPlayerData data) {
        boolean moon = data.isMoon();
        if (!(data.isSteed() || moon) || data.inRitual() || data.striking()) return false;
        ServerLevel level = player.level();
        data.sunTicks = 0;
        data.sunStart = level.getGameTime();
        data.breathing = false;
        data.markAll();
        Shot shot = new Shot();
        shot.moon = moon;
        SHOTS.put(player.getUUID(), shot);
        if (moon) {
            // the Moon Dragon roars: the day gives way to the night, like the command /time set night
            bringNight(level);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 5.0F, 0.6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 3.0F, 0.5F);
            player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.moon")
                    .withStyle(s -> s.withColor(0xBFD8FF)), true);
            return true;
        }
        // the night (when the players could sleep in a bed) gives way to the day, like the command /time set day
        bringDay(level);
        // it rears up and neighs
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HORSE_ANGRY, SoundSource.PLAYERS, 4.0F, 0.55F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.sun")
                .withStyle(s -> s.withColor(DragonType.SUNLIGHT.textColor())), true);
        return true;
    }

    /** At night (when the players could sleep in a bed), the day rises at once: the command "/time set day". */
    private static void bringDay(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld.isBrightOutside()) return;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set day");
        Component day = Component.translatable("message.bdragon.alpha_power.sun.day").withStyle(s -> s.withColor(DragonType.SUNLIGHT.textColor()));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(day);
    }

    /** By day, the night falls at once: the command "/time set night". */
    private static void bringNight(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.overworld();
        if (!overworld.isBrightOutside()) return;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set night");
        Component night = Component.translatable("message.bdragon.alpha_power.moon.night").withStyle(s -> s.withColor(0xBFD8FF));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(night);
    }

    /** Where the sun (or the moon) is before it is thrown: above the head of the steed, or in the mouth of the Moon Dragon. */
    private static Vec3 origin(ServerPlayer player, Shot shot, float rear) {
        if (shot.moon) return AlphaHitboxes.serpentMouth(player);
        return AlphaSun.sun(player.position(), player.yBodyRot, rear, 1);
    }

    /** Stops the power (death, other dimension...): the thrown sun disappears. Its sun still grows back. */
    public static void stop(ServerPlayer player, DragonPlayerData data) {
        SHOTS.remove(player.getUUID());
        if (!data.striking()) return;
        data.sunTicks = -1;
        data.markAll();
    }

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        if (!data.striking()) return;
        Shot shot = SHOTS.get(player.getUUID());
        DragonType race = shot == null ? null : shot.moon ? DragonType.MOONLIGHT : DragonType.SUNLIGHT;
        if (shot == null || data.type() != race || !data.isAlpha() || player.isDeadOrDying() || player.isSpectator()) {
            stop(player, data);
            return;
        }
        ServerLevel level = player.level();
        RandomSource random = level.getRandom();
        int t = data.sunTicks;

        if (t < AlphaSun.LAUNCH_TICKS) {
            // the sun gathers its light while the steed rears up
            float rear = AlphaSun.LAUNCH_REAR * Mth.clamp(t / 7F, 0, 1);
            Vec3 s = origin(player, shot, rear);
            if (t % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, true, true, s.x, s.y, s.z, 6, 2.5, 2.5, 2.5, 0.08);
                level.sendParticles(shot.moon ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, true, true, s.x, s.y, s.z, 4, 2.0, 2.0, 2.0, 0.02);
            }
            // the moon forms in its mouth
            if (shot.moon && t > AlphaSun.LAUNCH_TICKS / 2) {
                level.sendParticles(MOON_CORE, true, true, s.x, s.y, s.z, 12, 0.5, 0.5, 0.5, 0);
            }
        } else if (t == AlphaSun.LAUNCH_TICKS) {
            launch(level, player, shot);
        } else if (shot.exploded < 0) {
            fly(level, player, shot, t - AlphaSun.LAUNCH_TICKS, random);
        } else {
            explode(level, player, shot, random);
            shot.exploded++;
            if (shot.exploded > AlphaSun.EXPLODE_TICKS) {
                stop(player, data);
                return;
            }
        }
        data.sunTicks++;
    }

    // ------------------------------------------------------------------------------------------------------
    // The throw and the flight
    // ------------------------------------------------------------------------------------------------------

    /** The sun leaves its place: the point aimed at (the first block or creature in the look), and its way there. */
    private static void launch(ServerLevel level, ServerPlayer player, Shot shot) {
        Vec3 from = origin(player, shot, AlphaSun.LAUNCH_REAR);
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getViewVector(1.0F).scale(AlphaSun.RANGE));
        Vec3 aim = firstHit(level, player, eye, far);
        if (aim == null) aim = far;
        // the sun flies from its place: it stops at the first thing on its own way
        Vec3 to = firstHit(level, player, from, aim);
        shot.from = from;
        shot.to = to == null ? aim : to;
        shot.flight = Math.max(1, Mth.ceil(shot.from.distanceTo(shot.to) / AlphaSun.SUN_SPEED));
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 4.0F, 0.5F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 4.0F, 0.6F);
    }

    /** The sun on its way; it explodes at the end, or earlier on a creature that came in its way. */
    private static void fly(ServerLevel level, ServerPlayer player, Shot shot, int f, RandomSource random) {
        Vec3 prev = position(shot, f - 1), pos = position(shot, f);
        Vec3 hit = firstCreature(level, player, prev, pos);
        if (hit != null) pos = hit;
        // the ball of fire, and its trail
        level.sendParticles(shot.moon ? MOON_CORE : SUN_CORE, true, true, pos.x, pos.y, pos.z, 30, 0.9, 0.9, 0.9, 0);
        level.sendParticles(shot.moon ? MOON_EDGE : SUN_EDGE, true, true, pos.x, pos.y, pos.z, 16, 1.4, 1.4, 1.4, 0);
        level.sendParticles(ParticleTypes.END_ROD, true, true, pos.x, pos.y, pos.z, 6, 1.2, 1.2, 1.2, 0.05);
        for (int i = 1; i <= 3; i++) {
            Vec3 p = prev.lerp(pos, i / 3.0);
            level.sendParticles(shot.moon ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, true, true, p.x, p.y, p.z, 4, 0.6, 0.6, 0.6, 0.02);
        }
        if (hit != null || f >= shot.flight) {
            shot.exploded = 0;
            shot.centre = pos;
        }
    }

    private static Vec3 position(Shot shot, int f) {
        return shot.from.lerp(shot.to, Mth.clamp(f / (double) shot.flight, 0, 1));
    }

    /** The first block or creature between two points (null = none). */
    @Nullable
    private static Vec3 firstHit(ServerLevel level, ServerPlayer player, Vec3 a, Vec3 b) {
        BlockHitResult block = level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? b : block.getLocation();
        Vec3 creature = firstCreature(level, player, a, end);
        if (creature != null) return creature;
        return block.getType() == HitResult.Type.MISS ? null : end;
    }

    /** The first creature (not the steed) touched by the sun between two points (null = none). */
    @Nullable
    private static Vec3 firstCreature(ServerLevel level, ServerPlayer player, Vec3 a, Vec3 b) {
        AABB box = new AABB(a, b).inflate(2.0);
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> !spared(player, e))) {
            Optional<Vec3> p = e.getBoundingBox().inflate(1.5).clip(a, b);
            if (p.isEmpty()) continue;
            double d = p.get().distanceToSqr(a);
            if (d < bestDist) {
                bestDist = d;
                best = p.get();
            }
        }
        return best;
    }

    /** The steed itself, its pets, the dead and the statues of the museum are never hurt. */
    private static boolean spared(ServerPlayer player, LivingEntity e) {
        if (e == player || !e.isAlive() || e.isSpectator()) return true;
        if (e instanceof WildDragon d && d.isDisplay()) return true;
        return e instanceof OwnableEntity pet && pet.getOwner() == player;
    }

    // ------------------------------------------------------------------------------------------------------
    // The explosion
    // ------------------------------------------------------------------------------------------------------

    /** The white ball grows; everything inside disappears but the bedrock, the creatures inside are hurt. */
    private static void explode(ServerLevel level, ServerPlayer player, Shot shot, RandomSource random) {
        int e = shot.exploded;
        Vec3 c = shot.centre;
        float k = Mth.clamp(e / (float) AlphaSun.EXPLODE_TICKS, 0, 1);
        double radius = AlphaSun.RADIUS * (1 - (1 - k) * (1 - k));
        if (e == 0) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 8.0F, 0.6F);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 6.0F, 0.7F);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 6.0F, 0.5F);
        }
        // the ball of white light
        double r = Math.max(0.8, radius);
        level.sendParticles(LIGHT, true, true, c.x, c.y, c.z, 40 + (int) (22 * r), r * 0.42, r * 0.42, r * 0.42, 0);
        if (e % 4 == 0) level.sendParticles(DragonType.flash(0xFFFFFF), true, true, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 36; i++) {
            // its edge
            double u = random.nextDouble() * 2 - 1, a = random.nextDouble() * Mth.TWO_PI, s = Math.sqrt(1 - u * u);
            level.sendParticles(ParticleTypes.END_ROD, true, true, c.x + Math.cos(a) * s * r, c.y + u * r, c.z + Math.sin(a) * s * r,
                    1, 0, 0, 0, 0);
        }
        if (Config.ALPHA_SUN_DESTROY.get()) clear(level, player, shot, radius);
        hurt(level, player, shot, radius);
        shot.doneRadius = radius;
    }

    /** Removes the blocks between the last radius and this one (all but the bedrock and the unbreakable blocks). */
    private static void clear(ServerLevel level, ServerPlayer player, Shot shot, double radius) {
        Vec3 c = shot.centre;
        int n = Mth.ceil(radius);
        double inner = shot.doneRadius < 0 ? -1 : shot.doneRadius * shot.doneRadius, outer = radius * radius;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int cx = Mth.floor(c.x), cy = Mth.floor(c.y), cz = Mth.floor(c.z);
        for (int dx = -n; dx <= n; dx++) {
            for (int dy = -n; dy <= n; dy++) {
                for (int dz = -n; dz <= n; dz++) {
                    double x = cx + dx + 0.5 - c.x, y = cy + dy + 0.5 - c.y, z = cz + dz + 0.5 - c.z;
                    double d = x * x + y * y + z * z;
                    if (d > outer || d <= inner) continue;
                    p.set(cx + dx, cy + dy, cz + dz);
                    if (p.getY() <= level.getMinY() || p.getY() >= level.getMaxY()) continue;
                    if (!level.isLoaded(p) || !level.getWorldBorder().isWithinBounds(p) || !level.mayInteract(player, p)) continue;
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.is(Blocks.BEDROCK) || s.getDestroySpeed(level, p) < 0) continue;
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** The creatures reached by the ball are hurt once, more near its centre, and thrown away. */
    private static void hurt(ServerLevel level, ServerPlayer player, Shot shot, double radius) {
        Vec3 c = shot.centre;
        AABB box = new AABB(c, c).inflate(radius + 2);
        DamageSource source = level.damageSources().explosion(player, player);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> !spared(player, e))) {
            if (shot.hurt.contains(e.getId())) continue;
            Vec3 p = e.getBoundingBox().getCenter();
            double d = p.distanceTo(c) - e.getBbWidth() / 2;
            if (d > radius) continue;
            shot.hurt.add(e.getId());
            float k = (float) Mth.clamp(d / AlphaSun.RADIUS, 0, 1);
            e.hurtServer(level, source, Mth.lerp(k, AlphaSun.DAMAGE_CENTRE, AlphaSun.DAMAGE_EDGE));
            Vec3 push = p.subtract(c);
            double len = Math.max(0.5, push.length());
            double strength = 2.5 * (1 - k) + 0.6;
            e.push(push.x / len * strength, 0.6 + 0.4 * (1 - k), push.z / len * strength);
            e.needsSync = true;
        }
    }
}
