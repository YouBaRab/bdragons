package com.yb.dragons.client.model;

import com.yb.dragons.dragon.AlphaNether;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Animation of one Demon of the Nether (client): the blend of its three states (ground, flight, hovering), the steady
 * beat of its wings, its yawn after 30 seconds without moving and the incantation of its Territory. Ticked with the
 * client, interpolated every frame into a {@link DemonRenderState}.
 */
public class DemonAnimator {
    /** Horizontal speed (blocks per tick) above which a flying demon is "flying", vertical speed above which it climbs or dives. */
    public static final double FLY_SPEED = 0.08, CLIMB_SPEED = 0.12;
    /** Under these speeds (blocks per tick) the demon is "not moving". */
    static final double STILL_HORIZONTAL = 0.02, STILL_VERTICAL = 0.03;
    /** The wings beat at a steady pace (radians per tick): a beat every second and a half. */
    static final float FLAP_SPEED = 0.21F;

    public enum Mode { GROUND, FLY, HOVER }

    private final float[] weights = {1, 0, 0};
    private final float[] prevWeights = {1, 0, 0};
    private float flap, prevFlap, jaw, prevJaw;
    private Mode mode = Mode.GROUND;
    /** Ticks without moving, and the age of the yawn (-1 = no yawn). */
    private int still;
    private int yawnAge = -1;
    private float castAge = -1, prevCastAge = -1;

    public static Mode mode(boolean inAir, double horizontalSpeed, double verticalSpeed) {
        if (!inAir) return Mode.GROUND;
        return horizontalSpeed > FLY_SPEED || Math.abs(verticalSpeed) > CLIMB_SPEED ? Mode.FLY : Mode.HOVER;
    }

    public Mode mode() {
        return mode;
    }

    public float weight(Mode m) {
        return weights[m.ordinal()];
    }

    /**
     * @param inAir     flying or falling
     * @param mouthOpen 0..1 (breath, roar)
     * @param castAge   ticks since the Territory was called (-1 = none)
     */
    public void tick(LivingEntity e, boolean inAir, float mouthOpen, float castAge) {
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        double hs = Math.sqrt(vx * vx + vz * vz);
        mode = mode(inAir, hs, vy);
        System.arraycopy(weights, 0, prevWeights, 0, 3);
        float sum = 0;
        for (int i = 0; i < 3; i++) {
            weights[i] += ((i == mode.ordinal() ? 1 : 0) - weights[i]) * 0.15F;
            sum += weights[i];
        }
        for (int i = 0; i < 3; i++) weights[i] /= sum;

        // the wings beat at a steady pace in the air, and stop folded on the ground
        prevFlap = flap;
        float air = weights[1] + weights[2];
        flap += FLAP_SPEED * Math.min(1, air * 1.5F);
        if (flap > 1000 * Mth.TWO_PI) {
            flap -= 1000 * Mth.TWO_PI;
            prevFlap -= 1000 * Mth.TWO_PI;
        }
        prevJaw = jaw;
        jaw += Mth.clamp(mouthOpen - jaw, -0.2F, 0.25F);
        prevCastAge = this.castAge;
        this.castAge = castAge;

        // still for 30 seconds (on the ground or in the air): it yawns, then again every 30 seconds
        boolean idle = mode != Mode.FLY && hs < STILL_HORIZONTAL && Math.abs(vy) < STILL_VERTICAL && mouthOpen <= 0 && castAge < 0;
        still = idle ? still + 1 : 0;
        if (yawnAge >= 0) {
            yawnAge = !idle || yawnAge >= AlphaNether.YAWN_TICKS ? -1 : yawnAge + 1;
        } else if (still >= AlphaNether.YAWN_AFTER) {
            yawnAge = 0;
            still = 0;
        }
    }

    /** The yawn starts this tick (its sound). */
    public boolean yawnStarted() {
        return yawnAge == 0;
    }

    /** 0..1: the yawn opens the mouth for 70 ticks, up and down smoothly. */
    private float yawn(float partialTick) {
        if (yawnAge < 0) return 0;
        float t = Mth.clamp((yawnAge + partialTick) / AlphaNether.YAWN_TICKS, 0, 1);
        // a slow opening, held, then a quick closing
        float open = Mth.clamp(t / 0.35F, 0, 1), close = Mth.clamp((1 - t) / 0.2F, 0, 1);
        float k = Math.min(open, close);
        return k * k * (3 - 2 * k);
    }

    /** Fills the animation values of the render state (interpolated). */
    public void fill(DemonRenderState s, float partialTick) {
        s.ground = Mth.lerp(partialTick, prevWeights[0], weights[0]);
        s.fly = Mth.lerp(partialTick, prevWeights[1], weights[1]);
        s.hover = Mth.lerp(partialTick, prevWeights[2], weights[2]);
        s.flap = Mth.lerp(partialTick, prevFlap, flap);
        s.jaw = Mth.lerp(partialTick, prevJaw, jaw);
        s.yawn = yawn(partialTick);
        s.castAge = castAge < 0 ? -1 : prevCastAge < 0 ? castAge : Mth.lerp(partialTick, prevCastAge, castAge);
        s.cast = cast(s.castAge);
    }

    /** 0..1: the pose of the incantation, raised in half a second, held, and released at its end. */
    public static float cast(float age) {
        if (age < 0) return 0;
        float in = Mth.clamp(age / 10F, 0, 1), out = Mth.clamp((AlphaNether.CAST_TICKS + 10 - age) / 15F, 0, 1);
        float k = Math.min(in, out);
        return k * k * (3 - 2 * k);
    }
}
