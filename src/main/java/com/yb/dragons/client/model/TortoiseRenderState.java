package com.yb.dragons.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Everything needed to draw the Sylvan Tortoise (alpha of the forest) for one frame. The animation values
 * are interpolated by {@link TortoiseAnimator} before the frame (the deferred renderer poses the model later).
 */
public class TortoiseRenderState extends LivingEntityRenderState {
    /** Size factor (1 = 17 x 12 x 12 blocks): the tortoise grows out of the explosion of the alpha ritual. */
    public float grow = 1;
    /** Alpha ritual: levitation (blocks), white glow (0..1). */
    public float ritualLift;
    public float whiteFlash;

    // --- the 4 states of the tortoise, blended (their sum is 1) -------------------------------------------
    /** Standing or walking on its 4 legs. */
    public float ground = 1;
    /** Flying forward: front legs stretched forward and up, hind legs backward (a jumping horse). */
    public float propel;
    /** Still in the air: the clouds gather and swirl under its 4 paws. */
    public float hover;
    /** Losing height: the clouds slide down, the heavy tortoise sinks slowly. */
    public float descend;

    /** Paddle of the legs in the air (radians) and swirl of the clouds (radians). */
    public float paddle;
    public float swirl;
    /** Body pitch (degrees, nose down positive) and bobbing (blocks). */
    public float bodyPitch;
    public float bob;
    /** Mouth open, 0..1 (Beam of Life, breath, roar). */
    public float jaw;
    /**
     * Withdrawn into its shell, 0..1: after {@link TortoiseAnimator#RETRACT_AFTER} ticks hovering without moving, the
     * head, the legs and the tail slowly go in (only a look).
     */
    public float retract;

    // --- the Beam of Life ------------------------------------------------------------------------------------
    /** 0 = no beam, 1 = full beam (it grows at the start and fades at the end). */
    public float beam;
    /** Where the beam ends, relative to the position of the entity (blocks). */
    @Nullable public Vec3 beamEnd;
    /** The beam touched the ground (a splash of leaves at its end). */
    public boolean beamHit;
}
