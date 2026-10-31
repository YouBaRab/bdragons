package com.yb.dragons.client.render;

import com.yb.dragons.client.ClientAnimators;
import com.yb.dragons.client.ClientDragonData;
import com.yb.dragons.client.model.DemonRenderState;
import com.yb.dragons.client.model.DragonAnimator;
import com.yb.dragons.client.model.DragonRenderState;
import com.yb.dragons.client.model.StormRenderState;
import com.yb.dragons.client.model.SunSteedRenderState;
import com.yb.dragons.client.model.TortoiseAnimator;
import com.yb.dragons.client.model.TortoiseRenderState;
import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.AlphaMoon;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderAvatarEvent;
import org.jspecify.annotations.Nullable;

/**
 * The "morph": a player in dragon form is not drawn as a human anymore but with the dragon model
 * of his race and stage (same renderer as the wild dragons). Hybrids keep the human model.
 * The alpha of the forest is drawn as the Sylvan Tortoise ({@link TortoiseRenderer}), the alpha of the storm as the
 * Tempest Dragon ({@link StormRenderer}), the alpha of the Nether as the Demon of the Nether ({@link DemonRenderer}), the
 * alpha of the sunlight as the Sun Steed ({@link SunSteedRenderer}).
 */
public final class PlayerDragonRenderer {
    @Nullable private static DragonEntityRenderer<WildDragon> renderer;
    @Nullable private static TortoiseRenderer tortoise;
    @Nullable private static StormRenderer serpent;
    @Nullable private static DemonRenderer demon;
    @Nullable private static SunSteedRenderer steed;
    @Nullable private static MoonRenderer moon;

    private PlayerDragonRenderer() {}

    public static void setRenderer(DragonEntityRenderer<WildDragon> r) {
        renderer = r;
    }

    public static void setTortoise(TortoiseRenderer r) {
        tortoise = r;
    }

    public static void setSerpent(StormRenderer r) {
        serpent = r;
    }

    public static void setDemon(DemonRenderer r) {
        demon = r;
    }

    public static void setSteed(SunSteedRenderer r) {
        steed = r;
    }

    public static void setMoon(MoonRenderer r) {
        moon = r;
    }

    /** True when this player is drawn as the Moon Dragon now (the alpha of the moonlight, or the end of his awakening ritual). */
    public static boolean drawsMoon(ClientDragonData.Info info, float ritualAge) {
        if (!info.isActiveDragon() || info.type() != DragonType.MOONLIGHT) return false;
        if (ritualAge >= 0) return ritualAge >= DragonAlpha.CLIMAX;
        return info.isMoon();
    }

    /** True when this player is drawn as the Sun Steed now (the alpha of the sunlight, or the end of his awakening ritual). */
    public static boolean drawsSteed(ClientDragonData.Info info, float ritualAge) {
        if (!info.isActiveDragon() || info.type() != DragonType.SUNLIGHT) return false;
        if (ritualAge >= 0) return ritualAge >= DragonAlpha.CLIMAX;
        return info.isSteed();
    }

    /** True when this player is drawn as the Demon of the Nether now (the alpha of the Nether, or the end of his awakening ritual). */
    public static boolean drawsDemon(ClientDragonData.Info info, float ritualAge) {
        if (!info.isActiveDragon() || info.type() != DragonType.NETHER) return false;
        if (ritualAge >= 0) return ritualAge >= DragonAlpha.CLIMAX;
        return info.isDemon();
    }

    /** True when this player is drawn as the tortoise now (the alpha of the forest, or the end of his awakening ritual). */
    public static boolean drawsTortoise(ClientDragonData.Info info, float ritualAge) {
        if (!info.isActiveDragon() || info.type() != DragonType.FOREST) return false;
        if (ritualAge >= 0) return ritualAge >= DragonAlpha.CLIMAX;
        return info.isTortoise();
    }

    /** True when this player is drawn as the Tempest Dragon now (the alpha of the storm, or the end of his awakening ritual). */
    public static boolean drawsSerpent(ClientDragonData.Info info, float ritualAge) {
        if (!info.isActiveDragon() || info.type() != DragonType.STORM) return false;
        if (ritualAge >= 0) return ritualAge >= DragonAlpha.CLIMAX;
        return info.isSerpent();
    }

    /**
     * The hybrid glides exactly like a player with an elytra: lying in the air in the direction of the look, rolled in
     * the turns, the arms and the legs still (the game only does this for a real elytra, so its values are set here:
     * the same rules as the game for the roll and for the swing of the limbs).
     */
    static void glideLikeElytra(AvatarRenderState avatar) {
        avatar.isFallFlying = true;
        avatar.fallFlyingTimeInTicks = Math.max(avatar.fallFlyingTimeInTicks, 20);
        // the limbs do not swing (with an elytra they only swing at very low speed)
        avatar.walkAnimationSpeed = 0;
        avatar.speedValue = Math.max(avatar.speedValue, 1.0F);
        avatar.isCrouching = false;
        var level = Minecraft.getInstance().level;
        Entity entity = level == null ? null : level.getEntity(avatar.id);
        if (entity == null) return;
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 look = entity.getViewVector(partialTick);
        Vec3 move = entity.getDeltaMovement();
        if (entity.xo != entity.getX() || entity.zo != entity.getZ()) {
            move = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        }
        double speed = move.lengthSqr() / 0.2;
        avatar.speedValue = Math.max(1.0F, (float) (speed * speed * speed));
        if (move.horizontalDistanceSqr() > 1.0E-5 && look.horizontalDistanceSqr() > 1.0E-5) {
            avatar.shouldApplyFlyingYRot = true;
            double dot = move.horizontal().normalize().dot(look.horizontal().normalize());
            double cross = move.x * look.z - move.z * look.x;
            avatar.flyingYRot = (float) (Math.signum(cross) * Math.acos(Math.min(1.0, Math.abs(dot))));
        } else {
            avatar.shouldApplyFlyingYRot = false;
            avatar.flyingYRot = 0;
        }
    }

    /** @return true to cancel the vanilla player rendering */
    public static boolean onRenderAvatar(RenderAvatarEvent.Pre event) {
        if (renderer == null) return false;
        AvatarRenderState avatar = event.getState();
        ClientDragonData.Info info = ClientDragonData.get(avatar.id);
        if (info == null || info.type() == null) return false;
        if (info.isHybrid()) {
            // the hybrid is drawn as a human (+ HybridLayer); when gliding it lies in the air like with an elytra
            if (ClientAnimators.isHybridGliding(avatar.id)) glideLikeElytra(avatar);
            return false;
        }
        if (!info.isActiveDragon()) return false;
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        Entity entity = level.getEntity(avatar.id);
        if (entity == null) return false;

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float age = info.ritualAge(partialTick);
        if (tortoise != null && drawsTortoise(info, age)) {
            TortoiseRenderState state = new TortoiseRenderState();
            copy(avatar, state);
            ClientAnimators.tortoise(entity).fill(state, partialTick);
            if (age >= 0) ritualTortoise(state, info, age);
            tortoise.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
            return true;
        }
        if (serpent != null && drawsSerpent(info, age)) {
            StormRenderState state = new StormRenderState();
            copy(avatar, state);
            ClientAnimators.serpent(entity).fill(state, partialTick);
            if (age >= 0) {
                ritualSerpent(state, info, age);
            } else {
                // the roar that calls the storm
                float storm = info.stormAge(partialTick);
                if (storm >= 0 && storm < AlphaStorm.ROAR_TICKS) state.jaw = Math.max(state.jaw, 1.0F);
            }
            serpent.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
            return true;
        }
        if (moon != null && drawsMoon(info, age)) {
            StormRenderState state = new StormRenderState();
            copy(avatar, state);
            ClientAnimators.serpent(entity).fill(state, partialTick);
            if (age >= 0) {
                ritualSerpent(state, info, age);
            } else {
                // the roar that brings the night, before the moon is thrown
                float roar = info.sunAge(partialTick);
                if (roar >= 0 && roar < AlphaMoon.ROAR_TICKS + 6) state.jaw = Math.max(state.jaw, 1.0F);
            }
            moon.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
            return true;
        }
        if (steed != null && drawsSteed(info, age)) {
            SunSteedRenderState state = new SunSteedRenderState();
            copy(avatar, state);
            ClientAnimators.steed(entity).fill(state, partialTick);
            float sunAge = info.sunAge(partialTick);
            state.sunSize = AlphaSun.sunSize(sunAge);
            state.ultTime = sunAge >= 0 && sunAge < AlphaSun.ULT_TICKS ? sunAge / 20F : -1;
            if (age >= 0) ritualSteed(state, info, age);
            steed.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
            return true;
        }
        if (demon != null && drawsDemon(info, age)) {
            DemonRenderState state = new DemonRenderState();
            copy(avatar, state);
            ClientAnimators.demon(entity).fill(state, partialTick);
            if (age >= 0) ritualDemon(state, info, age);
            demon.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
            return true;
        }

        DragonAnimator animator = ClientAnimators.get(entity);
        DragonRenderState state = new DragonRenderState();
        copy(avatar, state);
        state.type = info.type();
        state.stage = info.stage();
        state.renderScale = info.stats().renderScale();
        state.breathing = info.breathing();
        state.sitting = false;
        state.partialTick = partialTick;
        state.animator = animator;
        state.modelPitch = animator.getModelPitch(partialTick);
        state.modelOffsetY = animator.getModelOffsetY();

        // awakening of the alpha: the dragon faces one way, rises, rears up, glows white and grows
        if (age >= 0) {
            state.renderScale = DragonAlpha.renderScale(age, DragonStats.of(DragonStage.LEGENDARY, false).renderScale(),
                    DragonStats.alpha(info.type()).renderScale());
            state.ritualLift = DragonAlpha.lift(age);
            state.whiteFlash = DragonAlpha.flash(age);
            state.modelPitch += DragonAlpha.pitch(age);
            state.bodyRot = info.ritualYaw();
            state.yRot = 0;
            // it looks at the orb above its head, then roars at the sky
            state.xRot = ritualHeadPitch(age);
            state.breathing = DragonAlpha.roaring(age);
        }

        renderer.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
        return true;
    }

    private static float ritualHeadPitch(float age) {
        return age < DragonAlpha.ORB_BREAK - 8 ? -35 * DragonAlpha.phase(age, 0, 25)
                : DragonAlpha.roaring(age) ? -25 : -10 * (1 - DragonAlpha.phase(age, DragonAlpha.ROAR_END, DragonAlpha.LAND_END));
    }

    /** The end of the ritual of the alpha of the forest: the tortoise grows out of the light, floats on its clouds, roars and lands. */
    static void ritualTortoise(TortoiseRenderState state, ClientDragonData.Info info, float age) {
        state.grow = AlphaForest.ritualGrow(age);
        state.ritualLift = DragonAlpha.lift(age);
        state.whiteFlash = DragonAlpha.flash(age);
        state.bodyPitch = DragonAlpha.pitch(age) * 0.35F;
        state.bodyRot = info.ritualYaw();
        state.yRot = 0;
        state.xRot = ritualHeadPitch(age);
        state.jaw = DragonAlpha.roaring(age) ? 1 : 0;
        float land = DragonAlpha.phase(age, DragonAlpha.LAND_START, DragonAlpha.LAND_END);
        state.ground = land;
        state.hover = 1 - land;
        state.propel = 0;
        state.descend = 0;
        state.beam = 0;
        state.beamEnd = null;
    }

    /** The end of the ritual of the alpha of the storm: the serpent grows out of the light, held up by its tornado, roars and lands. */
    static void ritualSerpent(StormRenderState state, ClientDragonData.Info info, float age) {
        state.grow = AlphaForest.ritualGrow(age);
        state.ritualLift = DragonAlpha.lift(age);
        state.whiteFlash = DragonAlpha.flash(age);
        state.bodyRot = info.ritualYaw();
        state.yRot = 0;
        state.xRot = ritualHeadPitch(age);
        state.jaw = DragonAlpha.roaring(age) ? 1 : 0;
        state.coil = 1;
        state.slither = 0;
        state.fly = 0;
        state.descend = 0;
        state.bend = 0;
        state.climb = 0;
        // its tornado holds it up while it floats; it lands rearing
        state.air = 1 - DragonAlpha.phase(age, DragonAlpha.LAND_START, DragonAlpha.LAND_END);
    }

    /** The end of the ritual of the alpha of the Nether: the demon grows out of the light, floats on its wings, roars and lands. */
    static void ritualDemon(DemonRenderState state, ClientDragonData.Info info, float age) {
        state.grow = AlphaForest.ritualGrow(age);
        state.ritualLift = DragonAlpha.lift(age);
        state.whiteFlash = DragonAlpha.flash(age);
        state.bodyRot = info.ritualYaw();
        state.yRot = 0;
        state.xRot = ritualHeadPitch(age);
        state.jaw = DragonAlpha.roaring(age) ? 1 : 0;
        state.yawn = 0;
        state.cast = 0;
        state.castAge = -1;
        float land = DragonAlpha.phase(age, DragonAlpha.LAND_START, DragonAlpha.LAND_END);
        state.ground = land;
        state.hover = 1 - land;
        state.fly = 0;
    }

    /** The end of the ritual of the alpha of the sunlight: the steed grows out of the light, floats on its wings and lands. */
    static void ritualSteed(SunSteedRenderState state, ClientDragonData.Info info, float age) {
        state.grow = AlphaForest.ritualGrow(age);
        state.ritualLift = DragonAlpha.lift(age);
        state.whiteFlash = DragonAlpha.flash(age);
        state.bodyRot = info.ritualYaw();
        state.yRot = 0;
        state.xRot = ritualHeadPitch(age);
        state.ground = DragonAlpha.phase(age, DragonAlpha.LAND_START, DragonAlpha.LAND_END);
        state.walk = 0;
        state.ultTime = -1;
        state.afkTime = -1;
        state.sunSize = 1;
    }

    private static void copy(AvatarRenderState from, LivingEntityRenderState to) {
        to.entityType = from.entityType;
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.ageInTicks = from.ageInTicks;
        to.boundingBoxWidth = from.boundingBoxWidth;
        to.boundingBoxHeight = from.boundingBoxHeight;
        to.eyeHeight = from.eyeHeight;
        to.distanceToCameraSq = from.distanceToCameraSq;
        to.isInvisible = from.isInvisible;
        to.isDiscrete = from.isDiscrete;
        to.displayFireAnimation = from.displayFireAnimation;
        to.lightCoords = from.lightCoords;
        to.outlineColor = from.outlineColor;
        to.nameTag = from.nameTag;
        to.scoreText = from.scoreText;
        to.nameTagAttachment = from.nameTagAttachment;
        to.leashStates = from.leashStates;
        to.bodyRot = from.bodyRot;
        to.yRot = from.yRot;
        to.xRot = from.xRot;
        to.deathTime = from.deathTime;
        to.walkAnimationPos = from.walkAnimationPos;
        to.walkAnimationSpeed = from.walkAnimationSpeed;
        to.scale = from.scale;
        to.ageScale = from.ageScale;
        to.isUpsideDown = from.isUpsideDown;
        to.isFullyFrozen = from.isFullyFrozen;
        to.isInWater = from.isInWater;
        to.hasRedOverlay = from.hasRedOverlay;
        to.isInvisibleToPlayer = from.isInvisibleToPlayer;
        // a dragon never lies down nor swims like a human
        to.pose = from.pose == Pose.SLEEPING ? Pose.SLEEPING : Pose.STANDING;
        to.bedOrientation = from.bedOrientation;
    }
}
