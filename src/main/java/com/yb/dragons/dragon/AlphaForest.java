package com.yb.dragons.dragon;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Alpha of the forest: the Sylvan Tortoise ("Tortue-Sylve"), a flying giant alligator snapping turtle carrying
 * a small plains biome on its spiked shell: two grassy hills and five trees (model made in Blockbench after the
 * alligator snapping turtle of Alex's Mobs, with the proportions of Bdragon).
 *
 * <p>Size of the model: 17 blocks long (beak to tail), 12 blocks wide (foot to foot) and 12 blocks high (ground to
 * the top of the highest tree of its back). The solid box of the player is the "trunk" of the
 * tortoise ({@link #SCALE}: 3.6 x 10.8 blocks, a player has only one square box that never turns); the whole
 * volume is covered by the hit zones of {@link com.yb.dragons.entity.AlphaHitbox}.</p>
 *
 * <p>Its power (key B) is the Beam of Life ({@link com.yb.dragons.player.LifeBeam}).</p>
 *
 * <p>Coordinates in this class are in blocks, in the frame of the tortoise: origin at the feet of the player
 * (centre of the shell on the ground), x to its left, y up, z forward.</p>
 */
public final class AlphaForest {
    /** Size of the model (blocks). */
    public static final float LENGTH = 17.0F, WIDTH = 12.0F, HEIGHT = 12.0F;
    /** Front and back of the model (blocks from the centre): the beak at +9.9, the tail at -7.25. */
    public static final float FRONT = 9.9F, BACK = -7.25F;
    /** Scale attribute of the player: box 0.6 x 1.8 -> 3.6 x 10.8 blocks. */
    public static final float SCALE = 6.0F;

    // --- the head rig (shared by the model and the server, so the beam leaves the mouth seen on screen) -----
    /** Pivot of the neck (it turns 40% of the head rotation). */
    public static final float NECK_Y = 4.4F, NECK_Z = 5.0F;
    /** Pivot of the head, from the neck pivot. */
    public static final float HEAD_DY = 0.1F, HEAD_DZ = 1.4F;
    /** The mouth (start of the beam), from the head pivot. */
    public static final float MOUTH_DY = -0.5F, MOUTH_DZ = 3.1F;
    /** Limits of the head rotation (degrees): left / right, looking up (negative) and down. */
    public static final float HEAD_YAW_MAX = 55.0F, HEAD_PITCH_MIN = -40.0F, HEAD_PITCH_MAX = 55.0F;
    public static final float NECK_SHARE = 0.4F;

    // --- the Beam of Life ------------------------------------------------------------------------------------
    /** Duration of the beam (ticks): 6 seconds. */
    public static final int BEAM_DURATION = 6 * 20;
    /** Every {@link #BEAM_HIT_EVERY} ticks: damage to the monsters and healing of the allies along the beam. */
    public static final int BEAM_HIT_EVERY = 4;
    public static final float BEAM_DAMAGE = 4.0F;
    public static final float BEAM_HEAL = 2.0F;
    /** Radius of the terraformed disc where the beam touches the ground. */
    public static final int TERRAFORM_RADIUS = 2;
    /** Speed of the tortoise while it fires (fraction of its normal speed). */
    public static final float BEAM_MOVE_FACTOR = 0.35F;

    private AlphaForest() {}

    /**
     * Size of the tortoise during the alpha ritual: nothing before the explosion (the legendary dragon is drawn),
     * then it grows out of the light in a second, with a small overshoot.
     */
    public static float ritualGrow(float age) {
        if (age < DragonAlpha.CLIMAX) return 0;
        float k = DragonAlpha.phase(age, DragonAlpha.CLIMAX, DragonAlpha.CLIMAX + 24);
        float over = Mth.sin(Mth.clamp((age - DragonAlpha.CLIMAX) / 30F, 0, 1) * Mth.PI) * 0.06F;
        return Mth.lerp(k, 0.22F, 1.0F) * (1 + over);
    }

    /** The full dragon form of the alpha of the forest is the tortoise. */
    public static boolean isTortoise(@Nullable DragonType type, int stage, boolean alpha, DragonForm form) {
        return type == DragonType.FOREST && alpha && stage >= 5 && form == DragonForm.DRAGON;
    }

    /** Head rotation relative to the body, clamped like the model does (degrees). */
    public static float headYaw(float headYawWorld, float bodyYaw) {
        return Mth.clamp(Mth.wrapDegrees(headYawWorld - bodyYaw), -HEAD_YAW_MAX, HEAD_YAW_MAX);
    }

    public static float headPitch(float xRot) {
        return Mth.clamp(xRot, HEAD_PITCH_MIN, HEAD_PITCH_MAX);
    }

    /**
     * World position of the mouth.
     * @param relYaw head yaw relative to the body (degrees, clamped), {@code pitch} the head pitch (degrees)
     * @param scale  size factor of the model (1 = full size)
     */
    public static Vec3 mouth(Vec3 feet, float bodyYaw, float relYaw, float pitch, float scale) {
        // head frame -> neck frame -> tortoise frame, like ModelPart.translateAndRotate (yaw then pitch)
        Vec3 p = rotate(new Vec3(0, MOUTH_DY, MOUTH_DZ), relYaw * (1 - NECK_SHARE), pitch * (1 - NECK_SHARE));
        p = p.add(0, HEAD_DY, HEAD_DZ);
        p = rotate(p, relYaw * NECK_SHARE, pitch * NECK_SHARE);
        p = p.add(0, NECK_Y, NECK_Z).scale(scale);
        return feet.add(toWorld(p, bodyYaw));
    }

    /** A point of the tortoise frame (x left, y up, z forward) turned to the world for the given body yaw. */
    public static Vec3 toWorld(Vec3 local, float bodyYaw) {
        float r = bodyYaw * Mth.DEG_TO_RAD;
        double sin = Mth.sin(r), cos = Mth.cos(r);
        // forward = (-sin, 0, cos); left = (cos, 0, sin)
        return new Vec3(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos);
    }

    /** Yaw (to the left, degrees) then pitch (down, degrees) of a point of a part. */
    private static Vec3 rotate(Vec3 v, float yawDeg, float pitchDeg) {
        float p = pitchDeg * Mth.DEG_TO_RAD;
        // looking down turns the forward axis towards -y
        double y1 = v.y * Mth.cos(p) - v.z * Mth.sin(p);
        double z1 = v.y * Mth.sin(p) + v.z * Mth.cos(p);
        float yaw = yawDeg * Mth.DEG_TO_RAD;
        // turning the head to the right of the body (positive relative yaw, like the vanilla head yaw) moves the
        // forward axis towards -x (the left of the tortoise is +x)
        double x2 = v.x * Mth.cos(yaw) - z1 * Mth.sin(yaw);
        double z2 = v.x * Mth.sin(yaw) + z1 * Mth.cos(yaw);
        return new Vec3(x2, y1, z2);
    }
}
