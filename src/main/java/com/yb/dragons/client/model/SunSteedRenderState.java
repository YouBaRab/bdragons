package com.yb.dragons.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Everything needed to draw the Sun Steed (alpha of the sunlight) for one frame, filled by {@link SunSteedAnimator}
 * (times in seconds, for the animations of {@link SunSteedAnimations}).
 */
public class SunSteedRenderState extends LivingEntityRenderState {
    /** Size factor (1 = full size): the steed grows out of the explosion of the alpha ritual. */
    public float grow = 1;
    /** Alpha ritual: levitation (blocks), white glow (0..1). */
    public float ritualLift;
    public float whiteFlash;

    /** On the ground (1) or in the air (0), blended. */
    public float ground = 1;
    /** How much it walks (0..1) and the time of the walk cycle. */
    public float walk;
    public float walkTime;
    /** The time of the flight cycle. */
    public float flyTime;
    /** Seconds since its power was called (it rears up and throws its sun), -1 = none. */
    public float ultTime = -1;

    /** The slow spin of the sun (degrees around x and y). */
    public float sunSpinX, sunSpinY;
    /** Seconds since the sun started to implode (the player is away from the keyboard), -1 = none. */
    public float afkTime = -1;
    /** Size of the sun: 1 = whole, 0 = thrown (it grows back during the recharge). */
    public float sunSize = 1;
}
