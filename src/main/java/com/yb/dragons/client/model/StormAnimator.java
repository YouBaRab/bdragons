package com.yb.dragons.client.model;

import com.yb.dragons.dragon.StormSpine;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Animation of one Tempest Dragon (client): the blend of its four states (still, slithering, flying, diving),
 * the wave of its body and its mouth. Ticked with the client, interpolated every frame into a {@link StormRenderState}.
 */
public class StormAnimator {
    private final StormSpine.Motion motion = new StormSpine.Motion();
    private float jaw, prevJaw;
    /** The same pose as the model, solved each tick for the particles (clouds of the talons, the mouth). */
    private final StormSpine.Pose pose = new StormSpine.Pose();

    /**
     * @param inAir     flying or falling
     * @param mouthOpen 0..1 (breath, roar)
     */
    public void tick(LivingEntity e, boolean inAir, float mouthOpen) {
        tick(e, inAir, mouthOpen, StormSpine.LENGTHS_OF_STORM);
    }

    /** @param lengths the lengths of the elements of this serpent (the Moon Dragon has a short neck) */
    public void tick(LivingEntity e, boolean inAir, float mouthOpen, float[] lengths) {
        motion.lengths = lengths;
        pose.lengths = lengths;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        float turn = Mth.wrapDegrees(e.yBodyRot - e.yBodyRotO);
        motion.tick(inAir, Math.sqrt(vx * vx + vz * vz), vy, turn);
        prevJaw = jaw;
        jaw += Mth.clamp(mouthOpen - jaw, -0.2F, 0.25F);
        float headYaw = Mth.wrapDegrees(e.getYHeadRot() - e.yBodyRot);
        motion.track(e.getX(), e.getY(), e.getZ(), e.yBodyRot, e.tickCount, headYaw, e.getXRot());
        motion.solve(pose, 1.0F, e.tickCount, headYaw, e.getXRot());
        motion.follow(pose, motion.weight(StormSpine.FLY, 1.0F) + motion.weight(StormSpine.DESCEND, 1.0F), e.getX(), e.getY(), e.getZ(), e.yBodyRot);
    }

    public StormSpine.Motion motion() {
        return motion;
    }

    /** The pose of the last tick. */
    public StormSpine.Pose pose() {
        return pose;
    }

    /** Fills the animation values of the render state (interpolated). */
    public void fill(StormRenderState s, float partialTick) {
        s.coil = motion.weight(StormSpine.COIL, partialTick);
        s.slither = motion.weight(StormSpine.SLITHER, partialTick);
        s.fly = motion.weight(StormSpine.FLY, partialTick);
        s.descend = motion.weight(StormSpine.DESCEND, partialTick);
        s.air = motion.air(partialTick);
        s.phase = motion.phase(partialTick);
        s.climb = motion.climb(partialTick);
        s.bend = motion.bend(partialTick);
        s.jaw = Mth.lerp(partialTick, prevJaw, jaw);
        s.motion = motion;
    }
}
