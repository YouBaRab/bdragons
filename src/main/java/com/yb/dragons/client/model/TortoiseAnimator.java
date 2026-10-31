package com.yb.dragons.client.model;

import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.BeamTrace;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animation of one Sylvan Tortoise (client): the blend of its four states (ground, flight forward, hovering,
 * descent), the paddle of its legs, the swirl of its clouds, its mouth, its Beam of Life, and its retreat into
 * its shell when it stays still for 30 seconds. Ticked with the client, interpolated every frame into a
 * {@link TortoiseRenderState}.
 */
public class TortoiseAnimator {
    /** Horizontal speed (blocks per tick) above which a flying tortoise is "flying forward". */
    public static final double PROPEL_SPEED = 0.07;
    /** Vertical speed under which it is "descending". */
    public static final double DESCEND_SPEED = -0.06;
    /** Ticks without moving (hovering or on the ground) before the tortoise withdraws into its shell: 30 seconds. */
    public static final int RETRACT_AFTER = 30 * 20;
    /** Speeds (per tick) of the retreat into the shell (slow, about 1.5 s) and of the way out (quick, half a second). */
    static final float RETRACT_IN = 0.035F, RETRACT_OUT = 0.1F;
    /** Under these speeds (blocks per tick) the tortoise is "not moving". */
    static final double STILL_HORIZONTAL = 0.02, STILL_VERTICAL = 0.03;

    public enum Mode { GROUND, PROPEL, HOVER, DESCEND }

    private final float[] weights = {1, 0, 0, 0};
    private final float[] prevWeights = {1, 0, 0, 0};
    private float paddle, prevPaddle, swirl, prevSwirl, pitch, prevPitch, jaw, prevJaw, beam, prevBeam, retract, prevRetract;
    /** Ticks without moving (the tortoise withdraws into its shell at {@link #RETRACT_AFTER}). */
    private int still;
    /** 1 when it starts to withdraw this tick, -1 when it starts to come out, else 0 (the sounds of the shell). */
    private int shellEvent;
    @Nullable private Vec3 beamEnd, prevBeamEnd;
    private boolean beamHit;
    private Mode mode = Mode.GROUND;
    /** The leg that last touched the ground (for the heavy steps). */
    private int lastStep;

    /** The state the tortoise is in, from its movement (the blend follows it smoothly). */
    public static Mode mode(boolean inAir, double horizontalSpeed, double verticalSpeed) {
        if (!inAir) return Mode.GROUND;
        if (verticalSpeed < DESCEND_SPEED) return Mode.DESCEND;
        if (horizontalSpeed > PROPEL_SPEED) return Mode.PROPEL;
        return Mode.HOVER;
    }

    public Mode mode() {
        return mode;
    }

    public float weight(Mode m) {
        return weights[m.ordinal()];
    }

    /**
     * @param inAir   flying or falling
     * @param mouthOpen 0..1 (breath, roar)
     * @param beaming the Beam of Life is firing
     * @param view    chunks the beam may enter (the client draws it only where the chunks are loaded)
     */
    public void tick(LivingEntity e, boolean inAir, float mouthOpen, boolean beaming, BeamTrace.@Nullable ChunkFilter view, int viewChunks) {
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        double hs = Math.sqrt(vx * vx + vz * vz);
        tick(mode(inAir, hs, vy), hs, vy);
        // still for 30 s, in the air or on the ground: it withdraws into its shell (any movement, the beam or the breath
        // wakes it)
        boolean idle = (mode == Mode.HOVER || mode == Mode.GROUND) && hs < STILL_HORIZONTAL && Math.abs(vy) < STILL_VERTICAL && !beaming && mouthOpen <= 0;
        retract(idle);

        prevJaw = jaw;
        float jawTarget = beaming ? 1 : mouthOpen;
        jaw += Mth.clamp(jawTarget - jaw, -0.2F, 0.25F);
        prevBeam = beam;
        beam = Mth.clamp(beam + (beaming ? 0.2F : -0.25F), 0, 1);

        prevBeamEnd = beamEnd;
        if (beam > 0 && view != null) {
            BeamTrace.Result r = BeamTrace.trace(e.level(), view, e.getEyePosition(), e.getViewVector(1.0F), BeamTrace.maxLength(viewChunks));
            beamEnd = r.end();
            beamHit = r.block() != null;
            if (prevBeamEnd == null) prevBeamEnd = beamEnd;
        } else {
            beamEnd = null;
        }
    }

    /** Counts the still ticks and moves the retreat into the shell. */
    void retract(boolean idle) {
        still = idle ? Math.min(still + 1, RETRACT_AFTER) : 0;
        prevRetract = retract;
        boolean in = still >= RETRACT_AFTER;
        shellEvent = 0;
        if (in && retract == 0) shellEvent = 1;
        else if (!in && retract == 1) shellEvent = -1;
        retract = in ? Math.min(1, retract + RETRACT_IN) : Math.max(0, retract - RETRACT_OUT);
    }

    /** How far it is withdrawn into its shell (0..1). */
    public float retract() {
        return retract;
    }

    /** 1 when the tortoise starts to withdraw into its shell this tick, -1 when it starts to come out, else 0. */
    public int shellEvent() {
        return shellEvent;
    }

    /** The movement part of the tick (also used by the preview tools). */
    public void tick(Mode target, double horizontalSpeed, double verticalSpeed) {
        mode = target;
        System.arraycopy(weights, 0, prevWeights, 0, 4);
        float sum = 0;
        for (int i = 0; i < 4; i++) {
            float goal = i == target.ordinal() ? 1 : 0;
            // the heavy tortoise changes its posture slowly (landing is a little quicker)
            float rate = i == Mode.GROUND.ordinal() && goal > 0 ? 0.2F : 0.12F;
            weights[i] += (goal - weights[i]) * rate;
            sum += weights[i];
        }
        for (int i = 0; i < 4; i++) weights[i] /= sum;

        prevPaddle = paddle;
        paddle += (float) (0.08 + 0.25 * horizontalSpeed + 0.08 * weights[Mode.PROPEL.ordinal()]);
        prevSwirl = swirl;
        swirl += 0.06F + 0.03F * weights[Mode.HOVER.ordinal()] + 0.1F * weights[Mode.DESCEND.ordinal()];
        prevPitch = pitch;
        float pitchTarget = weights[Mode.PROPEL.ordinal()] * (6 + Mth.clamp((float) -verticalSpeed * 35, -10, 10))
                + weights[Mode.DESCEND.ordinal()] * 5;
        pitch += (pitchTarget - pitch) * 0.1F;
    }

    /**
     * Heavy steps: true once each time a foot touches the ground (diagonal gait, two steps per cycle).
     * @param walkPos walk animation position of the entity
     */
    public boolean step(float walkPos) {
        int step = Mth.floor(walkPos * 0.55F / Mth.PI);
        if (step != lastStep) {
            lastStep = step;
            return true;
        }
        return false;
    }

    public boolean beamHit() {
        return beamHit;
    }

    @Nullable
    public Vec3 beamEnd() {
        return beamEnd;
    }

    /** Fills the animation values of the render state (interpolated). */
    public void fill(TortoiseRenderState s, float partialTick) {
        s.ground = Mth.lerp(partialTick, prevWeights[0], weights[0]);
        s.propel = Mth.lerp(partialTick, prevWeights[1], weights[1]);
        s.hover = Mth.lerp(partialTick, prevWeights[2], weights[2]);
        s.descend = Mth.lerp(partialTick, prevWeights[3], weights[3]);
        s.paddle = Mth.lerp(partialTick, prevPaddle, paddle);
        s.swirl = Mth.lerp(partialTick, prevSwirl, swirl);
        s.bodyPitch = Mth.lerp(partialTick, prevPitch, pitch);
        s.jaw = Mth.lerp(partialTick, prevJaw, jaw);
        s.beam = Mth.lerp(partialTick, prevBeam, beam);
        s.retract = Mth.lerp(partialTick, prevRetract, retract);
        float age = s.ageInTicks;
        // the clouds rock the tortoise gently
        s.bob = Mth.sin(age * 0.09F) * 0.18F * s.hover + Mth.sin(age * 0.05F) * 0.08F * s.descend + Mth.sin(age * 0.12F) * 0.05F * s.propel;
        if (beamEnd != null && prevBeamEnd != null && s.beam > 0) {
            Vec3 end = prevBeamEnd.lerp(beamEnd, partialTick);
            s.beamEnd = end.subtract(s.x, s.y, s.z);
            s.beamHit = beamHit;
        } else {
            s.beamEnd = null;
        }
    }

    /** The mouth in the world (approximation without the body pitch, for the particles). */
    public static Vec3 mouth(LivingEntity e) {
        return AlphaForest.mouth(e.position(), e.yBodyRot, AlphaForest.headYaw(e.getYHeadRot(), e.yBodyRot),
                AlphaForest.headPitch(e.getXRot()), 1.0F);
    }
}
