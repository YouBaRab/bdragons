package com.yb.dragons.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Everything needed to draw the Demon of the Nether (alpha of the Nether) for one frame. The animation values are
 * interpolated by {@link DemonAnimator} before the frame (the deferred renderer poses the model later).
 */
public class DemonRenderState extends LivingEntityRenderState {
    /** Size factor (1 = full size): the demon grows out of the explosion of the alpha ritual. */
    public float grow = 1;
    /** Alpha ritual: levitation (blocks), white glow (0..1). */
    public float ritualLift;
    public float whiteFlash;

    // --- the 3 states of the demon, blended (their sum is 1) -------------------------------------------------
    /** On its four limbs, like a gorilla. */
    public float ground = 1;
    /** Flying (moving in the air): the body lies, the wings beat, the legs and the tails trail behind. */
    public float fly;
    /** Still in the air: the body upright, the wings beat, the legs hang. */
    public float hover;

    /** The beat of the wings (radians, steady). */
    public float flap;
    /** The yawn after 30 seconds without moving (0..1, up and down). */
    public float yawn;
    /** The incantation of its Territory (0..1, up and down) and its age (ticks, -1 = none). */
    public float cast;
    public float castAge = -1;
    /** Mouth open, 0..1 (breath, roar). */
    public float jaw;
}
