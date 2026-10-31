package com.yb.dragons.client;

import com.yb.dragons.client.model.DemonAnimator;
import com.yb.dragons.client.model.SunSteedAnimator;
import com.yb.dragons.client.model.DragonAnimator;
import com.yb.dragons.client.model.HybridModel;
import com.yb.dragons.client.model.StormAnimator;
import com.yb.dragons.client.model.TortoiseAnimator;
import com.yb.dragons.client.model.TortoiseModel;
import com.yb.dragons.client.render.PlayerDragonRenderer;
import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.AlphaNether;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.AlphaMoon;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.StormSpine;
import com.yb.dragons.dragon.BeamTrace;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.DragonVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Keeps one animator per visible dragon (entities, dragon players and hybrid players) and ticks them with the client. */
public final class ClientAnimators {
    private static final Map<Entity, DragonAnimator> ANIMATORS = new WeakHashMap<>();
    private static final Map<Entity, HybridAnim> HYBRIDS = new WeakHashMap<>();
    private static final Map<Entity, TortoiseAnimator> TORTOISES = new WeakHashMap<>();
    private static final Map<Entity, StormAnimator> SERPENTS = new WeakHashMap<>();
    private static final Map<Entity, DemonAnimator> DEMONS = new WeakHashMap<>();
    private static final Map<Entity, SunSteedAnimator> STEEDS = new WeakHashMap<>();

    /** Wing animation of a hybrid player. */
    public static final class HybridAnim {
        float spread, prevSpread;
        boolean gliding;

        float spread(float partialTick) {
            return Mth.lerp(partialTick, prevSpread, spread);
        }
    }

    private ClientAnimators() {}

    public static DragonAnimator get(Entity entity) {
        return ANIMATORS.computeIfAbsent(entity, e -> new DragonAnimator());
    }

    public static TortoiseAnimator tortoise(Entity entity) {
        return TORTOISES.computeIfAbsent(entity, e -> new TortoiseAnimator());
    }

    public static StormAnimator serpent(Entity entity) {
        return SERPENTS.computeIfAbsent(entity, e -> new StormAnimator());
    }

    public static DemonAnimator demon(Entity entity) {
        return DEMONS.computeIfAbsent(entity, e -> new DemonAnimator());
    }

    public static SunSteedAnimator steed(Entity entity) {
        return STEEDS.computeIfAbsent(entity, e -> new SunSteedAnimator());
    }

    public static void clear() {
        ANIMATORS.clear();
        HYBRIDS.clear();
        TORTOISES.clear();
        SERPENTS.clear();
        DEMONS.clear();
        STEEDS.clear();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        RandomSource random = level.getRandom();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof DragonVisual visual && entity instanceof LivingEntity living) {
                boolean flying = !living.onGround();
                DragonAnimator anim = get(entity);
                anim.tick(living, flying, visual.isDragonSitting(), visual.isBreathing(), visual.getRenderScale());
                afterTick(level, living, anim, visual.getDragonType(), visual.getVisualStage(), visual.getRenderScale(), flying, false, random);
            } else if (entity instanceof Player player) {
                ClientDragonData.Info info = ClientDragonData.get(player.getId());
                if (info == null || info.type() == null) continue;
                // the storm called by the alpha of the storm rages around him, whatever his form
                if (info.stormAge(0) >= 0) stormWeather(level, player, info.stormAge(0), random);
                // the acid rain of the Territory of the alpha of the Nether follows him, whatever his form
                if (info.domainAge(0) >= AlphaNether.CAST_TICKS) acidRain(level, player, info.domainAge(0) - AlphaNether.CAST_TICKS, random);
                if (info.isActiveDragon() && PlayerDragonRenderer.drawsTortoise(info, info.ritualAge(0))) {
                    tickTortoise(mc, level, player, info, random);
                } else if (info.isActiveDragon() && PlayerDragonRenderer.drawsSerpent(info, info.ritualAge(0))) {
                    tickSerpent(level, player, info, random);
                } else if (info.isActiveDragon() && PlayerDragonRenderer.drawsDemon(info, info.ritualAge(0))) {
                    tickDemon(level, player, info, random);
                } else if (info.isActiveDragon() && PlayerDragonRenderer.drawsSteed(info, info.ritualAge(0))) {
                    tickSteed(level, player, info, random);
                } else if (info.isActiveDragon() && PlayerDragonRenderer.drawsMoon(info, info.ritualAge(0))) {
                    tickMoon(level, player, info, random);
                } else if (info.isActiveDragon()) {
                    float scale = info.stats().renderScale();
                    boolean flying = player.isFallFlying() || isGliding(player)
                            || (player instanceof LocalPlayer lp ? lp.getAbilities().flying : isAirborne(player, scale));
                    boolean breathing = info.breathing();
                    // awakening of the alpha: the wings beat while it floats, the mouth opens to roar
                    float age = info.ritualAge(0);
                    if (age >= 0) {
                        scale = DragonAlpha.renderScale(age, DragonStats.of(DragonStage.LEGENDARY, false).renderScale(),
                                DragonStats.alpha(info.type()).renderScale());
                        flying = DragonAlpha.floating(age);
                        breathing = DragonAlpha.roaring(age);
                    }
                    DragonAnimator anim = get(entity);
                    anim.tick(player, flying && !player.isInWater(), false, breathing, scale);
                    afterTick(level, player, anim, info.type(), info.stage(), scale, flying, info.isAlpha(), random);
                } else if (info.isHybrid()) {
                    HybridAnim anim = HYBRIDS.computeIfAbsent(entity, e -> new HybridAnim());
                    boolean air = isAirborne(player, 0.6F) && !player.isInWater();
                    anim.gliding = air && isGliding(player);
                    anim.prevSpread = anim.spread;
                    anim.spread += ((air ? 1 : 0) - anim.spread) * 0.2F;
                    aura(level, player, info.type(), info.stage(), 0.45F, info.isAlpha(), random);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // The Sylvan Tortoise (alpha of the forest)
    // ------------------------------------------------------------------------------------------------------

    private static void tickTortoise(Minecraft mc, ClientLevel level, Player player, ClientDragonData.Info info, RandomSource random) {
        TortoiseAnimator anim = tortoise(player);
        boolean inAir = player instanceof LocalPlayer lp ? lp.getAbilities().flying || !lp.onGround() : !player.onGround();
        inAir &= !player.isInWater() && !player.isPassenger();
        boolean beaming = info.beamAge(0) >= 0;
        LocalPlayer local = mc.player;
        int view = mc.options.getEffectiveRenderDistance();
        BeamTrace.ChunkFilter filter = local == null ? null : (cx, cz) -> Math.abs(cx - local.chunkPosition().x()) <= view
                && Math.abs(cz - local.chunkPosition().z()) <= view && level.hasChunk(cx, cz);
        anim.tick(player, inAir, info.breathing() ? 0.7F : 0, beaming, filter, view);
        // the end of the ritual is animated by the renderer
        if (info.ritualAge(0) >= 0 || player.isInvisible()) return;

        float yaw = player.yBodyRot;
        // heavy steps on the ground
        if (!inAir && player.walkAnimation.speed() > 0.15F && anim.step(player.walkAnimation.position())) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.2F, 0.55F, false);
        }
        // the clouds under the paws
        float hover = anim.weight(TortoiseAnimator.Mode.HOVER), descend = anim.weight(TortoiseAnimator.Mode.DESCEND);
        float shell = anim.retract();
        // still for 30 s (in the air or on the ground), it withdraws into its shell (and comes out at the first movement)
        if (anim.shellEvent() != 0) {
            boolean in = anim.shellEvent() > 0;
            level.playLocalSound(player.getX(), player.getY() + 3, player.getZ(), in ? SoundEvents.SHULKER_CLOSE : SoundEvents.SHULKER_OPEN,
                    SoundSource.PLAYERS, 1.5F, in ? 0.5F : 0.6F, false);
        }
        if (hover + descend > 0.3F) {
            for (float[] leg : TortoiseModel.LEGS) {
                if (random.nextFloat() > 0.6F) continue;
                double a = random.nextDouble() * Mth.TWO_PI, r = 0.6 + random.nextDouble() * 1.4;
                // in its shell, the clouds carry the belly
                double out = (leg[0] > 0 ? TortoiseModel.FOOT_OUT : -TortoiseModel.FOOT_OUT) - (leg[0] > 0 ? 0.6 : -0.6) * shell;
                Vec3 local0 = new Vec3(leg[0] + out + Math.cos(a) * r, -0.5 - descend * 1.6 + 2.1 * shell - random.nextDouble() * 0.4,
                        leg[2] + Math.sin(a) * r);
                Vec3 p = player.position().add(AlphaForest.toWorld(local0, yaw));
                // they swirl around the paw (tangent) and sink when the tortoise descends
                Vec3 tangent = AlphaForest.toWorld(new Vec3(-Math.sin(a), 0, Math.cos(a)), yaw).scale(0.06);
                level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, tangent.x, -0.02 - descend * 0.08, tangent.z);
            }
        }
        // the trail of the flight forward
        if (anim.weight(TortoiseAnimator.Mode.PROPEL) > 0.5F && random.nextInt(3) == 0) {
            Vec3 p = player.position().add(AlphaForest.toWorld(new Vec3((random.nextDouble() - 0.5) * 9, 1.5 + random.nextDouble() * 3, -7), yaw));
            level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0, 0);
        }
        // leaves falling from the five trees of the shell
        if (random.nextInt(5) == 0) {
            float[] crown = TortoiseModel.CROWNS[random.nextInt(TortoiseModel.CROWNS.length)];
            double half = crown[3];
            Vec3 p = player.position().add(AlphaForest.toWorld(new Vec3(crown[0] + (random.nextDouble() - 0.5) * 2 * half,
                    crown[1] - random.nextDouble() * 0.3, crown[2] + (random.nextDouble() - 0.5) * 2 * half), yaw));
            level.addParticle(ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF3E6E2E), p.x, p.y, p.z, 0, 0, 0);
        }
        // the Beam of Life: leaves and sparks along it (also seen in first person), a burst where it touches
        Vec3 end = anim.beamEnd();
        if (end != null) {
            Vec3 mouth = TortoiseAnimator.mouth(player);
            Vec3 axis = end.subtract(mouth);
            double length = axis.length();
            int n = (int) Math.min(40, 4 + length / 4);
            for (int i = 0; i < n; i++) {
                Vec3 p = mouth.add(axis.scale(random.nextDouble()));
                level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER, true, false,
                        p.x + (random.nextDouble() - 0.5) * 0.6, p.y + (random.nextDouble() - 0.5) * 0.6, p.z + (random.nextDouble() - 0.5) * 0.6,
                        0, 0, 0);
            }
            if (anim.beamHit()) {
                for (int i = 0; i < 6; i++) {
                    level.addParticle(ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF7CCB3A), true, false,
                            end.x + (random.nextDouble() - 0.5) * 3, end.y + random.nextDouble() * 1.5, end.z + (random.nextDouble() - 0.5) * 3,
                            0, 0.05, 0);
                }
                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, true, false, end.x, end.y + 0.5, end.z, 0, 0, 0);
            }
        }
        aura(level, player, info.type(), info.stage(), info.stats().renderScale(), true, random);
    }

    // ------------------------------------------------------------------------------------------------------
    // The Tempest Dragon (alpha of the storm)
    // ------------------------------------------------------------------------------------------------------

    private static void tickSerpent(ClientLevel level, Player player, ClientDragonData.Info info, RandomSource random) {
        StormAnimator anim = serpent(player);
        boolean inAir = player instanceof LocalPlayer lp ? lp.getAbilities().flying || !lp.onGround() : !player.onGround();
        inAir &= !player.isInWater() && !player.isPassenger();
        float storm = info.stormAge(0);
        boolean roaring = storm >= 0 && storm < AlphaStorm.ROAR_TICKS;
        anim.tick(player, inAir, info.breathing() || roaring ? 0.8F : 0);
        // the end of the ritual is animated by the renderer
        if (info.ritualAge(0) >= 0 || player.isInvisible()) return;

        float yaw = player.yBodyRot;
        StormSpine.Motion motion = anim.motion();
        StormSpine.Pose pose = anim.pose();
        float air = motion.air(1.0F);
        float stream = motion.weight(StormSpine.FLY, 1.0F) + motion.weight(StormSpine.DESCEND, 1.0F);
        float rest = motion.weight(StormSpine.COIL, 1.0F) * air;
        // the talons grip the clouds when it moves in the air: in flight they stream back behind the claws
        if (air - rest > 0.3F) {
            Vec3 back = AlphaForest.toWorld(new Vec3(0, 0, -1), yaw);
            for (int side = 0; side < 2; side++) {
                if (random.nextFloat() > 0.5F + 0.4F * stream) continue;
                Vec3 p = player.position().add(AlphaForest.toWorld(pose.gripBlocks(side), yaw));
                Vec3 v = back.scale(0.05 + 0.25 * stream);
                level.addParticle(ParticleTypes.CLOUD, p.x + (random.nextDouble() - 0.5) * 0.5, p.y - 0.2, p.z + (random.nextDouble() - 0.5) * 0.5,
                        v.x, -0.01, v.z);
            }
        }
        // still in the air: the wind of the tornado that holds it up turns around its body and rises
        if (rest > 0.3F) {
            for (int i = 0; i < 3; i++) {
                float t = random.nextFloat();
                double radius = (StormSpine.tornadoConeRadius(t) + 10 + random.nextFloat() * 16) * AlphaStorm.PX;
                double height = StormSpine.tornadoConeHeight(t) * AlphaStorm.PX + (random.nextDouble() - 0.5);
                double a = random.nextDouble() * Mth.TWO_PI;
                Vec3 p = player.position().add(AlphaForest.toWorld(new Vec3(Math.sin(a) * radius, height, Math.cos(a) * radius), yaw));
                // the same way as the turns of its body
                Vec3 v = AlphaForest.toWorld(new Vec3(Math.cos(a), 0, -Math.sin(a)), yaw).scale(0.35 * rest);
                level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.WHITE_SMOKE : ParticleTypes.CLOUD, p.x, p.y, p.z,
                        v.x, 0.06 + random.nextDouble() * 0.04, v.z);
            }
            if (random.nextInt(5) == 0) {
                float t = random.nextFloat();
                double radius = (StormSpine.tornadoConeRadius(t) + 6) * AlphaStorm.PX;
                double a = random.nextDouble() * Mth.TWO_PI;
                Vec3 p = player.position().add(AlphaForest.toWorld(new Vec3(Math.sin(a) * radius,
                        StormSpine.tornadoConeHeight(t) * AlphaStorm.PX, Math.cos(a) * radius), yaw));
                level.addParticle(ParticleTypes.SMALL_GUST, p.x, p.y, p.z, 0, 0, 0);
            }
            if (random.nextInt(50) == 0) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS,
                        0.9F, 0.6F + random.nextFloat() * 0.2F, false);
            }
        }
        // sparks run along its lightning cracks
        if (random.nextInt(3) == 0) {
            int k = 1 + random.nextInt(StormSpine.N - 1);
            Vec3 j = pose.joint(k);
            Vec3 p = player.position().add(AlphaForest.toWorld(j, yaw));
            double w = StormSpine.WIDTHS[k] * AlphaStorm.PX;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x + (random.nextDouble() - 0.5) * w, p.y + (random.nextDouble() - 0.3) * w,
                    p.z + (random.nextDouble() - 0.5) * w, 0, 0.02, 0);
        }
        // the roar that calls the storm: lightning out of its mouth
        if (roaring) {
            Vec3 m = player.position().add(AlphaForest.toWorld(pose.mouthBlocks(), yaw));
            Vec3 look = player.getViewVector(1.0F);
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, true, false, m.x, m.y, m.z,
                        look.x * 0.6 + (random.nextDouble() - 0.5) * 0.4, look.y * 0.6 + (random.nextDouble() - 0.5) * 0.4,
                        look.z * 0.6 + (random.nextDouble() - 0.5) * 0.4);
            }
        }
        aura(level, player, info.type(), info.stage(), info.stats().renderScale(), true, random);
    }

    // ------------------------------------------------------------------------------------------------------
    // The Demon of the Nether (alpha of the Nether)
    // ------------------------------------------------------------------------------------------------------

    /** A point of the demon given in Blockbench pixels (x to its right... as in the file: x>0 its right, z<0 its front). */
    private static Vec3 demonPoint(Player player, double bx, double by, double bz) {
        double k = AlphaNether.MODEL_SCALE / 16.0;
        return player.position().add(AlphaForest.toWorld(new Vec3(-bx * k, by * k, -bz * k), player.yBodyRot));
    }

    private static void tickDemon(ClientLevel level, Player player, ClientDragonData.Info info, RandomSource random) {
        DemonAnimator anim = demon(player);
        boolean inAir = player instanceof LocalPlayer lp ? lp.getAbilities().flying || !lp.onGround() : !player.onGround();
        inAir &= !player.isInWater() && !player.isPassenger();
        float cast = info.domainAge(0);
        if (cast >= AlphaNether.CAST_TICKS + 25) cast = -1;
        anim.tick(player, inAir, info.breathing() ? 0.8F : 0, cast);
        // the end of the ritual is animated by the renderer
        if (info.ritualAge(0) >= 0 || player.isInvisible()) return;

        // the yawn: a deep growl
        if (anim.yawnStarted()) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS,
                    0.8F, 0.55F, false);
        }
        // lava drips from the cracks of its skin, and the spores of its fungi float off its back
        if (random.nextInt(4) == 0) {
            Vec3 p = demonPoint(player, (random.nextDouble() - 0.5) * 30, 40 + random.nextDouble() * 30, -10 + random.nextDouble() * 34);
            level.addParticle(ParticleTypes.DRIPPING_LAVA, p.x, p.y, p.z, 0, 0, 0);
        }
        if (random.nextInt(6) == 0) {
            Vec3 p = demonPoint(player, (random.nextDouble() - 0.5) * 20, 70 + random.nextDouble() * 10, 10 + random.nextDouble() * 16);
            level.addParticle(random.nextBoolean() ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.WARPED_SPORE, p.x, p.y, p.z, 0, 0, 0);
        }
        // the incantation: fire runs up its raised right hand to the circle above it
        if (cast >= 0 && cast < AlphaNether.CAST_TICKS) {
            float raised = DemonAnimator.cast(cast);
            if (raised > 0.6F) {
                Vec3 hand = demonPoint(player, 24, 130, -4);
                for (int i = 0; i < 2; i++) {
                    level.addParticle(ParticleTypes.FLAME, true, false, hand.x + (random.nextDouble() - 0.5) * 0.4, hand.y,
                            hand.z + (random.nextDouble() - 0.5) * 0.4, 0, 0.12 + random.nextDouble() * 0.08, 0);
                }
            }
            double a = random.nextDouble() * Mth.TWO_PI, r = 3.2 * (0.6 + random.nextDouble() * 0.5);
            level.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, true, false,
                    player.getX() + Math.cos(a) * r, player.getY() + 8.5, player.getZ() + Math.sin(a) * r, 0, -0.02, 0);
            if (cast >= 10 && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.ENCHANT, player.getX() + (random.nextDouble() - 0.5) * 4, player.getY() + 6 + random.nextDouble() * 3,
                        player.getZ() + (random.nextDouble() - 0.5) * 4, 0, 0.3, 0);
            }
        }
        // breath and roar: smoke out of its mouth
        if (info.breathing() && random.nextInt(2) == 0) {
            Vec3 m = demonPoint(player, 0, 70, -44);
            level.addParticle(ParticleTypes.SMOKE, m.x, m.y, m.z, 0, 0.03, 0);
        }
        aura(level, player, info.type(), info.stage(), info.stats().renderScale(), true, random);
    }

    // ------------------------------------------------------------------------------------------------------
    // The Moon Dragon (alpha of the moonlight)
    // ------------------------------------------------------------------------------------------------------

    /** The breeze of the night wind (light blue, faint). */
    private static final DustParticleOptions NIGHT_BREEZE = new DustParticleOptions(0xBFD8FF, 0.8F);

    private static void tickMoon(ClientLevel level, Player player, ClientDragonData.Info info, RandomSource random) {
        StormAnimator anim = serpent(player);
        boolean inAir = player instanceof LocalPlayer lp ? lp.getAbilities().flying || !lp.onGround() : !player.onGround();
        inAir &= !player.isInWater() && !player.isPassenger();
        float roar = info.sunAge(0);
        boolean roaring = roar >= 0 && roar < AlphaMoon.ROAR_TICKS + 6;
        anim.tick(player, inAir, info.breathing() || roaring ? 0.8F : 0, StormSpine.MOON_LENGTHS);
        if (info.ritualAge(0) >= 0 || player.isInvisible()) return;

        float yaw = player.yBodyRot;
        StormSpine.Motion motion = anim.motion();
        StormSpine.Pose pose = anim.pose();
        float air = motion.air(1.0F);
        float stream = motion.weight(StormSpine.FLY, 1.0F) + motion.weight(StormSpine.DESCEND, 1.0F);
        float rest = motion.weight(StormSpine.COIL, 1.0F) * air;
        // moving in the air: no clouds, only a breeze of the night wind along its body
        if (air - rest > 0.3F && random.nextFloat() < 0.6F + 0.4F * stream) {
            int k = StormSpine.TORSO + random.nextInt(StormSpine.N - StormSpine.TORSO);
            Vec3 p = player.position().add(AlphaForest.toWorld(pose.joint(k), yaw));
            Vec3 back = AlphaForest.toWorld(new Vec3((random.nextDouble() - 0.5) * 0.2, 0, -1), yaw).scale(0.08 + 0.3 * stream);
            double w = StormSpine.WIDTHS[k] * AlphaStorm.PX;
            level.addParticle(NIGHT_BREEZE, p.x + (random.nextDouble() - 0.5) * 2 * w, p.y + (random.nextDouble() - 0.5) * 2 * w,
                    p.z + (random.nextDouble() - 0.5) * 2 * w, back.x, 0.005, back.z);
            if (random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.SMALL_GUST, p.x, p.y, p.z, 0, 0, 0);
            }
        }
        // still in the air: it rests on its star platform, which sparkles
        if (rest > 0.3F && random.nextInt(2) == 0) {
            float low = Float.MAX_VALUE;
            float cx = 0, cz = 0;
            for (int k = StormSpine.TORSO; k <= StormSpine.N; k++) {
                low = Math.min(low, pose.jy[k]);
                cx += pose.jx[k];
                cz += pose.jz[k];
            }
            int n = StormSpine.N - StormSpine.TORSO + 1;
            double a = random.nextDouble() * Mth.TWO_PI, r = random.nextDouble() * 60;
            Vec3 c = new Vec3(cx / n + Math.cos(a) * r, low - 12, cz / n + Math.sin(a) * r).scale(AlphaStorm.PX);
            Vec3 p = player.position().add(AlphaForest.toWorld(c, yaw));
            level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, -0.01, 0);
        }
        // its stars twinkle
        if (random.nextInt(4) == 0) {
            int k = StormSpine.TORSO + random.nextInt(StormSpine.N - StormSpine.TORSO);
            Vec3 p = player.position().add(AlphaForest.toWorld(pose.joint(k), yaw));
            double d = (StormSpine.WIDTHS[k] / 2 + 12) * AlphaStorm.PX, a = random.nextDouble() * Mth.TWO_PI;
            Vec3 side = AlphaForest.toWorld(new Vec3(Math.cos(a) * d, Math.sin(a) * d, 0), yaw);
            level.addParticle(ParticleTypes.WAX_OFF, p.x + side.x, p.y + side.y, p.z + side.z, 0, 0, 0);
        }
        aura(level, player, info.type(), info.stage(), info.stats().renderScale(), true, random);
    }

    // ------------------------------------------------------------------------------------------------------
    // The Sun Steed (alpha of the sunlight)
    // ------------------------------------------------------------------------------------------------------

    private static void tickSteed(ClientLevel level, Player player, ClientDragonData.Info info, RandomSource random) {
        SunSteedAnimator anim = steed(player);
        boolean inAir = player instanceof LocalPlayer lp ? lp.getAbilities().flying || !lp.onGround() : isAirborne(player, AlphaSun.SCALE);
        inAir &= !player.isInWater() && !player.isPassenger();
        float sunAge = info.sunAge(0);
        anim.tick(player, inAir, sunAge < 0 && info.ritualAge(0) < 0);
        if (info.ritualAge(0) >= 0 || player.isInvisible()) return;
        Vec3 sun = AlphaSun.sun(player.position(), player.yBodyRot, 0, 1);
        // the sun shines: sparks of light around it
        if (sunAge < 0 && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, sun.x + (random.nextDouble() - 0.5) * 5, sun.y + (random.nextDouble() - 0.5) * 5,
                    sun.z + (random.nextDouble() - 0.5) * 5, 0, 0.01, 0);
        }
        // the implosion when the player is away from the keyboard: the sun turns to stardust
        if (anim.dustNow()) {
            for (int i = 0; i < 70; i++) {
                double vx = random.nextGaussian() * 0.12, vy = random.nextGaussian() * 0.12, vz = random.nextGaussian() * 0.12;
                level.addParticle(i % 3 == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD, true, false, sun.x, sun.y, sun.z, vx, vy, vz);
            }
            for (int i = 0; i < 30; i++) {
                level.addParticle(ParticleTypes.WAX_ON, sun.x + random.nextGaussian(), sun.y + random.nextGaussian(), sun.z + random.nextGaussian(),
                        0, 0, 0);
            }
            level.playLocalSound(sun.x, sun.y, sun.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 0.6F, false);
            level.playLocalSound(sun.x, sun.y, sun.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.5F, 1.2F, false);
        }
        aura(level, player, info.type(), info.stage(), info.stats().renderScale(), true, random);
    }

    /**
     * The acid rain of the Territory of the alpha of the Nether, seen by every client around him: a dark cloud of ash
     * above him and green drops falling on the disc of the territory (the burns come from the server).
     */
    private static void acidRain(ClientLevel level, Player player, float age, RandomSource random) {
        if (player.isInvisible() || age > AlphaNether.RAIN_TICKS) return;
        float strength = Mth.clamp(age / 30F, 0, 1) * Mth.clamp((AlphaNether.RAIN_TICKS - age) / 60F, 0, 1);
        double radius = AlphaNether.RADIUS, top = player.getY() + 14;
        for (int i = 0; i < Math.round(3 * strength); i++) {
            double a = random.nextDouble() * Mth.TWO_PI, r = Math.sqrt(random.nextDouble()) * radius;
            level.addParticle(ParticleTypes.LARGE_SMOKE, true, false, player.getX() + Math.cos(a) * r, top + random.nextDouble() * 3,
                    player.getZ() + Math.sin(a) * r, 0, 0.005, 0);
        }
        for (int i = 0; i < Math.round(26 * strength); i++) {
            double a = random.nextDouble() * Mth.TWO_PI, r = Math.sqrt(random.nextDouble()) * radius;
            double x = player.getX() + Math.cos(a) * r, z = player.getZ() + Math.sin(a) * r;
            double y = top - random.nextDouble() * 12;
            ParticleOptions drop = random.nextInt(3) == 0 ? ParticleTypes.FALLING_SPORE_BLOSSOM : ParticleTypes.ITEM_SLIME;
            level.addParticle(drop, true, false, x, y, z, 0, -0.9, 0);
        }
        if (random.nextInt(4) == 0) {
            double a = random.nextDouble() * Mth.TWO_PI, r = Math.sqrt(random.nextDouble()) * radius;
            level.addParticle(ParticleTypes.ASH, player.getX() + Math.cos(a) * r, player.getY() + random.nextDouble() * 4,
                    player.getZ() + Math.sin(a) * r, 0, 0, 0);
        }
        if ((int) age % 20 == 0 && random.nextInt(2) == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.WEATHER,
                    0.6F * strength + 0.05F, 0.6F, false);
        }
        if (random.nextInt(30) == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.LAVA_POP, SoundSource.WEATHER, 0.5F, 0.8F, false);
        }
    }

    /**
     * The Storm of the alpha of the storm, seen by every client around him: a ceiling of dark clouds above him, the
     * rain falling all around (the lightning bolts and their thunder come from the server).
     */
    private static void stormWeather(ClientLevel level, Player player, float age, RandomSource random) {
        if (player.isInvisible()) return;
        // it gathers during the roar, rages, then thins out in its last seconds
        float strength = Mth.clamp(age / 40F, 0, 1) * Mth.clamp((AlphaStorm.STORM_DURATION - age) / 100F, 0, 1);
        double top = player.getY() + 18;
        for (int i = 0; i < Math.round(5 * strength); i++) {
            double a = random.nextDouble() * Mth.TWO_PI, r = Math.sqrt(random.nextDouble()) * 24;
            level.addParticle(ParticleTypes.LARGE_SMOKE, true, false, player.getX() + Math.cos(a) * r, top + random.nextDouble() * 4,
                    player.getZ() + Math.sin(a) * r, 0, 0.01, 0);
        }
        for (int i = 0; i < Math.round(14 * strength); i++) {
            double a = random.nextDouble() * Mth.TWO_PI, r = Math.sqrt(random.nextDouble()) * 18;
            level.addParticle(ParticleTypes.FALLING_WATER, player.getX() + Math.cos(a) * r, player.getY() + 6 + random.nextDouble() * 10,
                    player.getZ() + Math.sin(a) * r, 0, 0, 0);
        }
        if (random.nextInt(50) == 0) {
            level.playLocalSound(player.getX(), player.getY() + 10, player.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.WEATHER,
                    0.4F * strength + 0.1F, 0.6F, false);
        }
    }

    /** Local player: the real glide state. Other players: falling slowly while moving fast. */
    private static boolean isGliding(Player player) {
        if (player instanceof LocalPlayer) return ClientGlide.isGliding();
        double dy = player.getY() - player.yo;
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo;
        return !player.onGround() && dy < -0.01 && dy > -0.8 && dx * dx + dz * dz > 0.04;
    }

    /** True when the hybrid player should be drawn lying in the air like with an elytra. */
    public static boolean isHybridGliding(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Entity e = mc.level.getEntity(entityId);
        HybridAnim anim = e == null ? null : HYBRIDS.get(e);
        return anim != null && anim.gliding;
    }

    /** Fresh render state of the dragon parts of a hybrid player (one per frame, the renderer is deferred). */
    public static HybridModel.State hybridState(AvatarRenderState avatar, ClientDragonData.Info info, float partialTick) {
        HybridModel.State s = new HybridModel.State();
        s.type = info.type();
        s.stage = info.stage();
        s.ageInTicks = avatar.ageInTicks;
        s.walkPos = avatar.walkAnimationPos;
        s.walkSpeed = avatar.walkAnimationSpeed;
        s.crouching = avatar.isCrouching;
        s.alpha = info.isAlpha();
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(avatar.id);
        HybridAnim anim = e == null ? null : HYBRIDS.get(e);
        if (anim != null) {
            s.wingSpread = anim.spread(partialTick);
            s.gliding = anim.gliding;
        }
        return s;
    }

    private static boolean isAirborne(LivingEntity e, float renderScale) {
        if (e.onGround() || e.isInWater() || e.isPassenger()) return false;
        AABB box = e.getBoundingBox();
        double depth = 1.0 + renderScale;
        return e.level().noCollision(e, new AABB(box.minX, box.minY - depth, box.minZ, box.maxX, box.minY, box.maxZ));
    }

    /** Wing beat sounds and the elemental aura of elder and legendary dragons. */
    private static void afterTick(ClientLevel level, LivingEntity e, DragonAnimator anim, DragonType type, int stage,
                                  float renderScale, boolean flying, boolean alpha, RandomSource random) {
        if (anim.flapped) {
            anim.flapped = false;
            if (flying && stage >= 2) {
                level.playLocalSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL,
                        0.25F + renderScale * 0.6F, 1.4F - renderScale * 0.5F + random.nextFloat() * 0.2F, false);
            }
        }
        aura(level, e, type, stage, renderScale, alpha, random);
        if (type == DragonType.FIRE && stage >= 2) boilingLava(level, e, stage, renderScale, random);
    }

    /** Fire dragon: the lava of its back boils, throwing sparks of lava and wisps of smoke. */
    private static void boilingLava(ClientLevel level, LivingEntity e, int stage, float renderScale, RandomSource random) {
        if (e.isInvisible() || e.isInWater() || random.nextFloat() > 0.05F * stage) return;
        double spread = 0.6 * renderScale + 0.2;
        double x = e.getX() + (random.nextDouble() - 0.5) * spread;
        double y = e.getY() + e.getBbHeight() * 0.85;
        double z = e.getZ() + (random.nextDouble() - 0.5) * spread;
        level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.LAVA : ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
    }

    private static void aura(ClientLevel level, LivingEntity e, DragonType type, int stage, float renderScale, boolean alpha,
                             RandomSource random) {
        if (stage < 4 || e.isInvisible()) return;
        // the alpha: a denser aura, with sparks of the light colour of its race
        if (alpha && random.nextInt(3) == 0) {
            double spread = 1.0 * renderScale + 0.3;
            level.addParticle(new DustParticleOptions(type.secondaryColor(), 1.4F), e.getX() + (random.nextDouble() - 0.5) * spread * 2,
                    e.getY() + random.nextDouble() * (1.8 * renderScale + 0.6), e.getZ() + (random.nextDouble() - 0.5) * spread * 2, 0, 0.02, 0);
        }
        float chance = alpha ? 0.95F : stage >= 5 ? 0.7F : 0.35F;
        if (random.nextFloat() > chance) return;
        double spread = 1.2 * renderScale + 0.4;
        double x = e.getX() + (random.nextDouble() - 0.5) * spread * 2;
        double y = e.getY() + random.nextDouble() * (1.6 * renderScale + 0.5);
        double z = e.getZ() + (random.nextDouble() - 0.5) * spread * 2;
        ParticleOptions particle = type.auraParticle();
        double vy = switch (type) {
            case FIRE, NETHER, SUNLIGHT, END -> 0.03;
            case ICE, MOONLIGHT, TERRA -> -0.02;
            default -> 0.0;
        };
        level.addParticle(particle, x, y, z, 0, vy, 0);
        if (stage >= 5 && (type == DragonType.FIRE || type == DragonType.NETHER) && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.3, z, 0, 0.04, 0);
        }
    }
}
