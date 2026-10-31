package com.yb.dragons.client.model;

import com.yb.dragons.dragon.StormSpine;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Everything needed to draw the Tempest Dragon (alpha of the storm) for one frame. The movement values are
 * interpolated by {@link StormAnimator}; the pose of the whole body is solved once ({@link #solve()}) before the
 * frame and read by the model and by its clouds (the deferred renderer poses the models later).
 */
public class StormRenderState extends LivingEntityRenderState {
    /** Size factor (1 = full size): the serpent grows out of the explosion of the alpha ritual. */
    public float grow = 1;
    /** Alpha ritual: levitation (blocks), white glow (0..1). */
    public float ritualLift;
    public float whiteFlash;

    // --- the 4 states of the serpent, blended (their sum is 1) -------------------------------------------
    /** Still (on the ground or in the air): rearing, the neck in an S, the arms raised, the body spiralling. */
    public float coil = 1;
    /** Moving on the ground: the front raised, humps running back along the body. */
    public float slither;
    /** Flying (forward, climbing or going down gently): the waves of the body, tilted with its path. */
    public float fly;
    /** Diving: straight along its path, nose down, the arms swept back. */
    public float descend;
    /** 0 on the ground, 1 in the air: the clouds it rests on and that its talons hold. */
    public float air;

    /** The waves along the body (radians), the tilt of the flight (degrees, nose up positive), the curve into a turn (degrees per piece). */
    public float phase;
    public float climb;
    public float bend;
    /** Mouth open, 0..1 (breath, roar). */
    public float jaw;

    public final StormSpine.Pose pose = new StormSpine.Pose();
    /** The movement of this serpent: in flight its body follows the path of its head (null during the ritual). */
    public StormSpine.Motion motion;

    /** Solves the pose from the values of this frame (head: {@link #yRot} and {@link #xRot}). */
    public void solve() {
        pose.solve(coil, slither, fly, descend, phase, ageInTicks, StormSpine.clampYaw(yRot), StormSpine.clampPitch(xRot), climb, bend, air);
        if (motion != null) motion.follow(pose, fly + descend, x, y, z, bodyRot);
    }
}
