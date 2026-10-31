package com.yb.dragons.dragon;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The spine of the Tempest Dragon (alpha of the storm): the pose of its 25 pieces and of its arms, shared by the model
 * (client) and its hit zones (server).
 *
 * <p>Frame of the serpent: x to its left, y up, z forward, in model pixels (16 pixels = 1 model block =
 * {@link AlphaStorm#MODEL_SCALE} world blocks), origin at the feet of the player. Element k: 0..3 = the neck (neck4
 * touches the head, neck1 the chest), 4 = the torso (it carries the arms), 5..19 = the 15 pieces of the body, 20..24 =
 * the tail. Joint k is the front of element k: joint 0 = the joint of the head, joint 4 = the chest, joint 25 = the tip
 * of the tail.</p>
 *
 * <p>Each element has a yaw (degrees, positive: going back, it turns towards the left of the serpent) and a pitch
 * (degrees, positive: going back, it goes up). The four states of the serpent each give the angles of every element;
 * they are blended (so it uncoils smoothly when it starts to fly) and the joints are found by walking along the body
 * from the chest. The movements follow the long serpents of Cobblemon (only the way they move: the numbers are ours):</p>
 * <ul>
 *   <li>still on the ground ({@link #COIL}): the rearing pose of Rayquaza, the torso straight up, the neck in an S, the
 *       head level, the arms out, the body spiralling on the ground, breathing slowly like the hovering Gyarados;</li>
 *   <li>still in the air ({@link #COIL} in the air): a tornado, the head and the arms at the centre, the body winding down
 *       around the chest in a cone that gets narrower (the wind of the tornado holds it up);</li>
 *   <li>moving on the ground ({@link #SLITHER}): like the walk of Gyarados, the front raised and humps running back along
 *       the body;</li>
 *   <li>flying ({@link #FLY}): like the flight of Gyarados, a vertical wave running back along the body (stronger
 *       towards the tail) and a slower side wave, the whole body tilted with the climb (nose up when it climbs); and the
 *       body follows the path of the head like the snake of the old games ({@link Motion#follow}): the tail goes where
 *       the head went, so the head can meet its own tail in a turn;</li>
 *   <li>diving ({@link #DESCEND}): like Dragonite, straight like an arrow along its path, nose down, the arms swept
 *       back.</li>
 * </ul>
 *
 * <p>The numbers were checked with a drawing of the real model ({@code tools/storm_preview.py}, Python twin
 * {@code tools/storm_spine.py}).</p>
 */
public final class StormSpine {
    public static final int N = 25;
    /** Elements 0..3 are the neck; element 4 is the torso. */
    public static final int NECK = 4, TORSO = 4;
    public static final int COIL = 0, SLITHER = 1, FLY = 2, DESCEND = 3;

    /** Length of each element (pixels): the 4 pieces of the neck, the torso, the 15 pieces of the body, the 5 of the tail. */
    static final float[] LENGTHS = {16, 19, 17, 7, 37, 7, 24, 24, 7, 33, 7, 24, 24, 7, 33, 7, 24, 24, 7, 31, 7, 24, 21, 21, 21};
    /**
     * The same for the Moon Dragon (alpha of the moonlight): the neck of the Ender Dragon is short, its two front pieces
     * have no cubes (their joints are almost at the same place) and the head starts right after the second piece.
     */
    public static final float[] MOON_LENGTHS = {0.5F, 0.5F, 17, 7, 37, 7, 24, 24, 7, 33, 7, 24, 24, 7, 33, 7, 24, 24, 7, 31, 7, 24, 21, 21, 21};
    /** {@link #LENGTHS}, for the other classes. */
    public static final float[] LENGTHS_OF_STORM = LENGTHS;
    /** The whole length of the serpent (pixels). */
    static final float BODY_LENGTH;
    static {
        float l = 0;
        for (float x : LENGTHS) l += x;
        BODY_LENGTH = l;
    }
    /** Thickness of each element (pixels): thin rings between the pieces, a thicker piece every five. */
    public static final float[] WIDTHS = {19, 16, 16, 14, 19, 14, 16, 16, 14, 19, 14, 16, 16, 14, 19, 14, 16, 16, 14, 19, 11, 12, 9, 7, 4};

    /** Limits of the head rotation (degrees): left / right, looking up (negative) and down. */
    public static final float HEAD_YAW_MAX = 70, HEAD_PITCH_MIN = -45, HEAD_PITCH_MAX = 55;
    /** Part of the turn of the head taken by each piece of the neck (from the head). */
    static final float[] NECK_YAW = {0.22F, 0.12F, 0.05F, 0};
    static final float[] NECK_PITCH = {0.25F, 0.15F, 0.05F, 0};
    /** Height of the chest (joint 4) above the feet in flight (pixels). */
    static final float ANCHOR_FLY = 52.0F;
    /** Period of the breath when still (ticks), wavelengths of the waves (pixels). */
    static final float BREATH = 40.0F, FLY_WAVE = 210.0F, FLY_SIDE_WAVE = 420.0F, SLITHER_WAVE = 170.0F;

    /**
     * The arms, in each state: rotations (radians, x y z like ModelPart) of the shoulder, the upper arm, the forearm and
     * the hand, for the arm on the -x side of the model (the other arm is mirrored: y and z negated).
     */
    static final float[][][] ARM_POSES = {
            // still: out, the forearms forward, the claws open
            {{0, 0.2F, -0.5F}, {0, 0.1F, 0}, {0, 0.42F, -0.35F}, {0, 0, 0}},
            // slither: forward and down, claws ready
            {{0, -0.55F, -0.45F}, {0, -0.2F, 0.15F}, {0, -1.0F, -0.2F}, {0, 0, 0}},
            // fly: out to the sides, the forearms down, the claws gripping the clouds below
            {{0, 0.15F, -0.3F}, {0, 0, -0.35F}, {0, -0.3F, -0.85F}, {0, 0, -0.4F}},
            // dive: swept back along the body
            {{0, 1.15F, -0.15F}, {0, 0.35F, 0}, {0, 0.3F, 0}, {0, 0, 0}}};
    /** Curl of the fingers in each state (radians). */
    static final float[] CURL = {0.35F, 0.5F, 0.9F, 0.7F};
    /** Pivots of the arm chain (Minecraft model pixels, y down): shoulder in the frame of the torso, then each part in the frame of the one before (arm on the -x side). */
    static final float[][] ARM_OFFSETS = {{-8, 0, 11}, {-4, 0, 0}, {-20, 0, 0}, {-18, 0, 0}};
    /** Where the talons hold their cloud, in the frame of the hand (arm on the -x side). */
    static final float GRIP_X = -10.0F, GRIP_Y = 5.0F, GRIP_Z = 0.0F;
    /** The mouth, in the frame of the head. */
    static final float MOUTH_Y = 4.0F, MOUTH_Z = -44.0F;

    /** The rearing pose (still on the ground). */
    private static final float[] STILL_YAW = new float[N];
    private static final float[] STILL_PITCH = new float[N];
    /**
     * The tornado (still in the air): the head and the arms at the centre, the torso going down, then the body opens into
     * a wide turn around the chest and winds down in a cone that gets narrower, like a tornado that holds it up.
     */
    private static final float[] HOVER_YAW = new float[N];
    private static final float[] HOVER_PITCH = new float[N];
    /** Height of the chest above the feet when it stays still in the air (pixels). */
    public static final float ANCHOR_HOVER = 60.0F;
    /** The body goes down this far, from the bottom of the torso to the tip of the tail (pixels). */
    static final double TORNADO_DROP = 80.0;
    /** Radius of the widest turn, arc length (from the bottom of the torso) over which the body opens to it, radius at the tip of the tail. */
    static final double TORNADO_TOP = 60.0, TORNADO_GROW = 90.0, TORNADO_END = 20.0;
    /** Radius and height (from the chest) of the bottom of the torso, arc length from there to the tip of the tail. */
    private static double tornadoR0, tornadoY0, tornadoArc;
    /** Arc length from the chest to the middle of each element of the body (pixels). */
    private static final float[] ARC = new float[N];

    static {
        STILL_PITCH[0] = 14.0F;
        STILL_PITCH[1] = -28.0F;
        STILL_PITCH[2] = -62.0F;
        STILL_PITCH[3] = -84.0F;
        STILL_PITCH[4] = -80.0F;
        STILL_PITCH[5] = -58.0F;
        STILL_PITCH[6] = -26.0F;
        STILL_YAW[6] = 25.0F;
        double r0 = 54.0, spacing = 23.0, turned = 0.0, heading = 70.0;
        for (int k = 7; k < N; k++) {
            double r = r0 - spacing * turned / 360.0;
            double step = 2 * Math.toDegrees(Math.asin(Math.min(1.0, LENGTHS[k] / (2 * r))));
            heading -= k > 7 ? step : step / 2;
            turned += step;
            STILL_YAW[k] = (float) heading;
            STILL_PITCH[k] = slope(k);
        }
        for (int k = TORSO; k < N; k++) {
            float s = LENGTHS[k] / 2;
            for (int i = TORSO; i < k; i++) s += LENGTHS[i];
            ARC[k] = s;
        }
        tornadoShape();
    }

    /** Radius of the turn of the tornado at arc length s from the bottom of the torso. */
    private static double tornadoRadius(double s) {
        if (s <= TORNADO_GROW) return tornadoR0 + (TORNADO_TOP - tornadoR0) * s / TORNADO_GROW;
        return TORNADO_TOP + (TORNADO_END - TORNADO_TOP) * (s - TORNADO_GROW) / (tornadoArc - TORNADO_GROW);
    }

    /** Builds the tornado: each piece of the body is a chord of the cone, from the bottom of the torso. */
    private static void tornadoShape() {
        for (int k = 0; k < NECK; k++) {
            HOVER_YAW[k] = STILL_YAW[k];
            HOVER_PITCH[k] = STILL_PITCH[k];
        }
        HOVER_PITCH[TORSO] = -75.0F;
        double[] p = new double[3];
        double[] d = direction(HOVER_YAW[TORSO], HOVER_PITCH[TORSO]);
        for (int i = 0; i < 3; i++) p[i] = LENGTHS[TORSO] * d[i];
        double total = 0;
        for (int k = TORSO + 1; k < N; k++) total += LENGTHS[k];
        tornadoR0 = Math.hypot(p[0], p[2]);
        tornadoY0 = p[1];
        tornadoArc = total;
        double theta = Math.atan2(p[0], p[2]);
        double rp = tornadoR0, s = 0, prev = HOVER_YAW[TORSO];
        for (int k = TORSO + 1; k < N; k++) {
            double len = LENGTHS[k];
            s += len;
            double r1 = tornadoRadius(s);
            double dy = -TORNADO_DROP * len / total;
            double c = (r1 * r1 + rp * rp + dy * dy - len * len) / (2 * r1 * rp);
            theta += Math.acos(Math.max(-1.0, Math.min(1.0, c)));
            double vx = r1 * Math.sin(theta) - p[0], vy = dy, vz = r1 * Math.cos(theta) - p[2];
            double n = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double pitch = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, vy / n))));
            double a = Math.toDegrees(Math.atan2(vx, -vz));
            // keep the turn continuous (no jump of 360 degrees)
            while (a - prev > 180) a -= 360;
            while (a - prev < -180) a += 360;
            HOVER_YAW[k] = (float) a;
            HOVER_PITCH[k] = (float) pitch;
            prev = a;
            // the real end of the piece (its length may not reach the cone exactly)
            double[] e = direction(HOVER_YAW[k], HOVER_PITCH[k]);
            for (int i = 0; i < 3; i++) p[i] += len * e[i];
            rp = Math.hypot(p[0], p[2]);
            theta = Math.atan2(p[0], p[2]);
        }
    }

    /** Direction of a piece (frame of the serpent) from its yaw and pitch (degrees). */
    private static double[] direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new double[] {Math.sin(y) * Math.cos(p), Math.sin(p), -Math.cos(y) * Math.cos(p)};
    }

    /**
     * The cone of the tornado (for its wind): radius (pixels) and height above the feet (pixels), {@code t} from 0 (the
     * widest turn) to 1 (the tip of the tail).
     */
    public static float tornadoConeRadius(float t) {
        return (float) (TORNADO_TOP + (TORNADO_END - TORNADO_TOP) * t);
    }

    public static float tornadoConeHeight(float t) {
        return (float) (ANCHOR_HOVER + tornadoY0 - TORNADO_DROP * (TORNADO_GROW + (tornadoArc - TORNADO_GROW) * t) / tornadoArc);
    }

    private StormSpine() {}

    /** Pitch of an element lying on the ground: its centre line stays at half its thickness. */
    static float slope(int k) {
        float drop = (WIDTHS[Math.min(k + 1, N - 1)] - WIDTHS[k]) / 2;
        return (float) Math.toDegrees(Math.asin(drop / LENGTHS[k]));
    }

    /** 0 before element {@code start}, 1 from element {@code full}. */
    static float ramp(int k, int start, int full) {
        return Math.min(1.0F, Math.max(0.0F, (k - start) / (float) (full - start)));
    }

    /** The joint where the bone of element k has its pivot: the pieces of the neck hang from their back joint (their cubes go forward), the others from their front joint. */
    public static int boneJoint(int k) {
        return k < NECK ? k + 1 : k;
    }

    // ------------------------------------------------------------------------------------------------------
    // The pose
    // ------------------------------------------------------------------------------------------------------

    /** The pose of the whole body for one moment. */
    public static final class Pose {
        /** Angles of each element (degrees). */
        public final float[] yaw = new float[N];
        public final float[] pitch = new float[N];
        /** The joints, in the frame of the serpent (pixels). */
        public final float[] jx = new float[N + 1];
        public final float[] jy = new float[N + 1];
        public final float[] jz = new float[N + 1];
        /** Head rotation (degrees, positive yaw = to the right, positive pitch = looking down). */
        public float headYaw, headPitch;
        /** Rotations of the shoulder, upper arm, forearm and hand (arm on the -x side), curl of the fingers (radians). */
        public final float[][] arm = new float[4][3];
        public float curl;
        /** The length of each element: {@link #LENGTHS} (Tempest Dragon) or {@link #MOON_LENGTHS} (Moon Dragon). */
        public float[] lengths = LENGTHS;

        /**
         * @param coil    still, on the watch            {@code slither} moving on the ground
         * @param fly     flying                         {@code descend} diving (the four weights add up to 1)
         * @param phase   the waves running along the body (radians)
         * @param age     ticks (the breath and the idle motions)
         * @param climb   the flight goes up (degrees, positive: nose up; negative: nose down)
         * @param bend    the body curves into a turn (degrees per piece, positive: turning to the right)
         * @param air     0 on the ground, 1 in the air
         */
        public void solve(float coil, float slither, float fly, float descend, float phase, float age, float headYaw, float headPitch,
                          float climb, float bend, float air) {
            float breath = (float) Math.sin(age * Mth.TWO_PI / BREATH);
            for (int k = 0; k < N; k++) {
                boolean body = k >= TORSO;
                // still: the rearing pose on the ground, the tornado in the air, breathing (the wave of the breath runs
                // down the neck and the torso)
                float cy = STILL_YAW[k] + (HOVER_YAW[k] - STILL_YAW[k]) * air;
                float cp = STILL_PITCH[k] + (HOVER_PITCH[k] - STILL_PITCH[k]) * air;
                if (k <= 5) cp += 3.0F * (float) Math.sin(age * Mth.TWO_PI / BREATH - k * 1.05);
                if (k >= N - 3) cy += (float) Math.sin(age * 0.11 + k) * 18.0F * (k - (N - 4)) / 3.0F;
                // slither: the front raised, humps running back along the body, a small sway
                float sp, sy;
                if (k < 4) {
                    sp = k == 0 ? 10.0F : k == 1 ? -18.0F : k == 2 ? -38.0F : -45.0F;
                    sy = 0;
                } else if (k <= 6) {
                    sp = k == 4 ? -32.0F : k == 5 ? -16.0F : -5.0F;
                    sy = 0;
                } else {
                    float r = ramp(k, 6, 10);
                    sp = slope(k) + 14.0F * r * (float) Math.sin(phase - Mth.TWO_PI * ARC[k] / SLITHER_WAVE);
                    sy = 9.0F * r * (float) Math.sin(0.5 * phase - Mth.TWO_PI * ARC[k] / (2 * SLITHER_WAVE));
                }
                // fly: tilted with the climb, the vertical wave stronger towards the tail, the slower side wave
                float t = body ? (k - TORSO) / (float) (N - 1 - TORSO) : 0;
                float fp, fy, dp, dy;
                if (body) {
                    float wave = (float) Math.sin(phase - Mth.TWO_PI * ARC[k] / FLY_WAVE);
                    float side = (float) Math.sin(0.5 * phase - Mth.TWO_PI * ARC[k] / FLY_SIDE_WAVE);
                    fp = -climb + (4.0F + 18.0F * t) * wave;
                    fy = (2.0F + 12.0F * t) * side;
                    // dive: straight along the path, the waves almost gone
                    dp = -climb + (1.5F + 5.0F * t) * wave;
                    dy = (1.0F + 4.0F * t) * side;
                } else {
                    fp = -climb + (k == 3 ? -18.0F : k == 2 ? -8.0F : k == 1 ? 0 : 6.0F);
                    fy = 0;
                    dp = -climb;
                    dy = 0;
                }
                float curve = bend * Math.max(0, k - TORSO);
                if (k < NECK) {
                    float lookY = headYaw * NECK_YAW[k], lookP = headPitch * NECK_PITCH[k];
                    cy += lookY;
                    sy += lookY;
                    fy += lookY;
                    dy += lookY;
                    cp += lookP;
                    sp += lookP;
                    fp += lookP;
                    dp += lookP;
                    curve = 0;
                }
                yaw[k] = coil * cy + slither * (sy - curve) + fly * (fy - curve) + descend * (dy - curve);
                pitch[k] = coil * cp + slither * sp + fly * fp + descend * dp;
            }
            // the joints, from the chest: the neck goes forward, the body goes back
            jx[TORSO] = 0;
            jy[TORSO] = 0;
            jz[TORSO] = 0;
            for (int k = NECK - 1; k >= 0; k--) {
                jx[k] = jx[k + 1] - lengths[k] * dirX(k);
                jy[k] = jy[k + 1] - lengths[k] * dirY(k);
                jz[k] = jz[k + 1] - lengths[k] * dirZ(k);
            }
            for (int k = TORSO; k < N; k++) {
                jx[k + 1] = jx[k] + lengths[k] * dirX(k);
                jy[k + 1] = jy[k] + lengths[k] * dirY(k);
                jz[k + 1] = jz[k] + lengths[k] * dirZ(k);
            }
            // on the ground (and on its cloud) the lowest piece of the body touches the ground; in flight the chest is at a fixed height
            float low = Float.MAX_VALUE;
            for (int k = TORSO + 1; k < N; k++) low = Math.min(low, Math.min(jy[k], jy[k + 1]) - WIDTHS[k] / 2);
            float lift = coil * ((1 - air) * -low + air * ANCHOR_HOVER) + slither * -low + (fly + descend) * ANCHOR_FLY
                    + air * coil * 1.5F * breath;
            for (int k = 0; k <= N; k++) jy[k] += lift;
            this.headYaw = headYaw;
            this.headPitch = headPitch;
            float[] w = {coil, slither, fly, descend};
            for (int part = 0; part < 4; part++) {
                for (int i = 0; i < 3; i++) {
                    float v = 0;
                    for (int s = 0; s < 4; s++) v += w[s] * ARM_POSES[s][part][i];
                    arm[part][i] = v;
                }
            }
            curl = 0;
            for (int s = 0; s < 4; s++) curl += w[s] * CURL[s];
        }

        /** Direction of element k, from its front joint to its back joint (frame of the serpent). */
        private float dirX(int k) {
            return (float) (Math.sin(Math.toRadians(yaw[k])) * Math.cos(Math.toRadians(pitch[k])));
        }

        private float dirY(int k) {
            return (float) Math.sin(Math.toRadians(pitch[k]));
        }

        private float dirZ(int k) {
            return (float) (-Math.cos(Math.toRadians(yaw[k])) * Math.cos(Math.toRadians(pitch[k])));
        }

        // --- Minecraft model space (pixels, y down, the front towards -z) ------------------------------------------
        public float modelX(int k) {
            return jx[k];
        }

        public float modelY(int k) {
            return 24.0F - jy[k];
        }

        public float modelZ(int k) {
            return -jz[k];
        }

        /**
         * Rotation of a part of an arm.
         * @param part 0 shoulder, 1 upper arm, 2 forearm, 3 hand
         * @param side 0 = the arm on the -x side of the model, 1 = the other one (mirrored)
         */
        public float armX(int part, int side) {
            return arm[part][0];
        }

        public float armY(int part, int side) {
            return side == 0 ? arm[part][1] : -arm[part][1];
        }

        public float armZ(int part, int side) {
            return side == 0 ? arm[part][2] : -arm[part][2];
        }

        /** The curl of the fingers (rotation around z) of an arm. */
        public float fingers(int side) {
            return side == 0 ? -curl : curl;
        }

        /**
         * Where the talons of a hand hold their cloud, in Minecraft model space (pixels).
         * @param side 0 = the arm on the -x side of the model, 1 = the other one
         */
        public void grip(int side, float[] out) {
            float sx = side == 0 ? 1 : -1;
            float[] v = {GRIP_X * sx, GRIP_Y, GRIP_Z};
            for (int part = 3; part >= 0; part--) {
                rotZYX(v, armX(part, side), armY(part, side), armZ(part, side));
                v[0] += ARM_OFFSETS[part][0] * sx;
                v[1] += ARM_OFFSETS[part][1];
                v[2] += ARM_OFFSETS[part][2];
            }
            rotX(v, pitch[TORSO] * Mth.DEG_TO_RAD);
            rotY(v, yaw[TORSO] * Mth.DEG_TO_RAD);
            out[0] = v[0] + modelX(TORSO);
            out[1] = v[1] + modelY(TORSO);
            out[2] = v[2] + modelZ(TORSO);
        }

        /** The mouth, in Minecraft model space (pixels). */
        public void mouth(float[] out) {
            float[] v = {0, MOUTH_Y, MOUTH_Z};
            rotX(v, headPitch * Mth.DEG_TO_RAD);
            rotY(v, headYaw * Mth.DEG_TO_RAD);
            out[0] = v[0] + modelX(0);
            out[1] = v[1] + modelY(0);
            out[2] = v[2] + modelZ(0);
        }

        // --- the frame of the serpent in blocks (x left, y up, z forward, feet at 0), for the server ---------------
        /** Joint k in blocks, at full size. */
        public Vec3 joint(int k) {
            return new Vec3(jx[k] * AlphaStorm.PX, jy[k] * AlphaStorm.PX, jz[k] * AlphaStorm.PX);
        }

        /** The mouth in blocks, at full size. */
        public Vec3 mouthBlocks() {
            float[] m = new float[3];
            mouth(m);
            return fromModel(m);
        }

        /** The cloud of a hand in blocks, at full size. */
        public Vec3 gripBlocks(int side) {
            float[] m = new float[3];
            grip(side, m);
            return fromModel(m);
        }

        private static Vec3 fromModel(float[] m) {
            return new Vec3(m[0] * AlphaStorm.PX, (24.0F - m[1]) * AlphaStorm.PX, -m[2] * AlphaStorm.PX);
        }
    }

    /** Rotation around x like {@code ModelPart} (y towards z for a positive angle). */
    static void rotX(float[] v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float y = v[1] * c - v[2] * s, z = v[1] * s + v[2] * c;
        v[1] = y;
        v[2] = z;
    }

    /** Rotation around y like {@code ModelPart}. */
    static void rotY(float[] v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float x = v[0] * c + v[2] * s, z = -v[0] * s + v[2] * c;
        v[0] = x;
        v[2] = z;
    }

    /** Rotation around z like {@code ModelPart} (x towards y for a positive angle). */
    static void rotZ(float[] v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float x = v[0] * c - v[1] * s, y = v[0] * s + v[1] * c;
        v[0] = x;
        v[1] = y;
    }

    /** The rotation of a {@code ModelPart}: x first, then y, then z. */
    static void rotZYX(float[] v, float x, float y, float z) {
        rotX(v, x);
        rotY(v, y);
        rotZ(v, z);
    }

    // ------------------------------------------------------------------------------------------------------
    // The movement: which state the serpent is in, blended smoothly
    // ------------------------------------------------------------------------------------------------------

    /** The state of one serpent from its movement, ticked with the game (client: the model, server: the hit zones). */
    public static final class Motion {
        /** Horizontal speed (blocks per tick) above which it slithers (ground) or flies (air). */
        public static final double SLITHER_SPEED = 0.06, FLY_SPEED = 0.08;
        /** Vertical speed above which it climbs (flies up even without moving forward). */
        public static final double CLIMB_SPEED = 0.12;
        /** It dives when it goes down faster than this, steeper than {@link #DIVE_SLOPE} (vertical / horizontal). */
        public static final double DIVE_SPEED = -0.15, DIVE_SLOPE = 0.6;
        /** Limits of the tilt of the body (degrees): up to 60 nose up when it climbs, down to 75 nose down when it dives. */
        static final float CLIMB_MAX = 60, FLY_DOWN_MAX = -40, DIVE_MIN = -15, DIVE_MAX = -75;

        private final float[] weights = {1, 0, 0, 0};
        private final float[] prevWeights = {1, 0, 0, 0};
        private float phase, prevPhase, climb, prevClimb, bend, prevBend, air, prevAir;
        private int state = COIL;
        /** The lengths of the elements of this serpent (see {@link Pose#lengths}). */
        public float[] lengths = LENGTHS;

        public static int state(boolean inAir, double horizontalSpeed, double verticalSpeed) {
            if (!inAir) return horizontalSpeed > SLITHER_SPEED ? SLITHER : COIL;
            if (verticalSpeed < DIVE_SPEED && -verticalSpeed > horizontalSpeed * DIVE_SLOPE) return DESCEND;
            return horizontalSpeed > FLY_SPEED || Math.abs(verticalSpeed) > CLIMB_SPEED ? FLY : COIL;
        }

        /**
         * @param inAir flying or falling
         * @param turn  turn of the body this tick (degrees, positive to the right)
         */
        public void tick(boolean inAir, double horizontalSpeed, double verticalSpeed, float turn) {
            state = state(inAir, horizontalSpeed, verticalSpeed);
            System.arraycopy(weights, 0, prevWeights, 0, 4);
            float sum = 0;
            for (int i = 0; i < 4; i++) {
                weights[i] += ((i == state ? 1 : 0) - weights[i]) * 0.15F;
                sum += weights[i];
            }
            for (int i = 0; i < 4; i++) weights[i] /= sum;

            // the waves run back along the body about as fast as the serpent moves (it does not slip); in the air they never stop
            prevPhase = phase;
            float k = weights[SLITHER] * 0.5F + (weights[FLY] + weights[DESCEND]) * 0.3F;
            phase += Math.min(0.6F, 0.03F + 0.13F * weights[FLY] + k * (float) horizontalSpeed);
            if (phase > 1000 * Mth.TWO_PI) {
                phase -= 1000 * Mth.TWO_PI;
                prevPhase -= 1000 * Mth.TWO_PI;
            }

            // the tilt of the body follows its path: nose up when it climbs, nose down when it dives
            prevClimb = climb;
            float path = (float) Math.toDegrees(Math.atan2(verticalSpeed, Math.max(0.05, horizontalSpeed)));
            float climbTarget = state == FLY ? Mth.clamp(path, FLY_DOWN_MAX, CLIMB_MAX)
                    : state == DESCEND ? Mth.clamp(path, DIVE_MAX, DIVE_MIN) : 0;
            climb += (climbTarget - climb) * 0.12F;

            prevBend = bend;
            float bendTarget = Mth.clamp(turn * 0.3F, -4, 4) * (1 - weights[COIL]);
            bend += (bendTarget - bend) * 0.2F;

            prevAir = air;
            air += ((inAir ? 1 : 0) - air) * 0.15F;
        }

        public int state() {
            return state;
        }

        public float weight(int s, float partialTick) {
            return Mth.lerp(partialTick, prevWeights[s], weights[s]);
        }

        public float phase(float partialTick) {
            return Mth.lerp(partialTick, prevPhase, phase);
        }

        public float climb(float partialTick) {
            return Mth.lerp(partialTick, prevClimb, climb);
        }

        public float bend(float partialTick) {
            return Mth.lerp(partialTick, prevBend, bend);
        }

        /** 0 on the ground, 1 in the air (smoothed): the clouds of the serpent. */
        public float air(float partialTick) {
            return Mth.lerp(partialTick, prevAir, air);
        }

        // --- the path of the head: in flight the body follows it, like the snake of the old games ------------------
        /** Points where the head went (world, blocks), the newest first; on the ground and still: the shape of the body. */
        private final java.util.ArrayDeque<double[]> trail = new java.util.ArrayDeque<>();
        private final Pose track = new Pose();
        /** The body follows the path of the head from this weight of flight (flying + diving) on. */
        static final float SNAKE_FROM = 0.02F;
        /** A head further than this from the last point of its path (blocks) was teleported: the path starts again. */
        static final double TRAIL_JUMP = 128.0;

        /**
         * Records where the head is (call it every tick after {@link #tick}, with the position and the body yaw of the
         * player). Not flying, the path is the shape of the body itself, so that when it takes off the body pulls
         * itself through the shape it had.
         */
        public void track(double x, double y, double z, float bodyYaw, float age, float headYaw, float headPitch) {
            track.lengths = lengths;
            solve(track, 1.0F, age, headYaw, headPitch);
            double[] head = toWorld(track, 0, x, y, z, bodyYaw);
            float snake = weights[FLY] + weights[DESCEND];
            if (snake < SNAKE_FROM || trail.isEmpty() || dist(head, trail.peekFirst()) > TRAIL_JUMP) {
                trail.clear();
                for (int k = 0; k <= N; k++) trail.addLast(toWorld(track, k, x, y, z, bodyYaw));
                return;
            }
            if (dist(head, trail.peekFirst()) > 0.02) trail.addFirst(head);
            // keep a path a little longer than the body
            double need = 1.5 * BODY_LENGTH * AlphaStorm.PX, length = 0;
            double[] prev = null;
            int keep = 0;
            for (double[] p : trail) {
                if (prev != null) length += dist(p, prev);
                prev = p;
                keep++;
                if (length > need) break;
            }
            while (trail.size() > Math.max(keep, 2)) trail.removeLast();
        }

        /**
         * In flight, puts the joints of the pose on the path of the head: each piece starts where the one before it
         * ends, on the path (follow the leader). The head and its look stay those of the pose; the body behind goes
         * where the head went, so in a turn the head can meet its own tail.
         * @param snake the weight of the flight (flying + diving) of this frame
         * @param x     position and body yaw of the player for this frame
         */
        public void follow(Pose pose, float snake, double x, double y, double z, float bodyYaw) {
            if (snake < SNAKE_FROM || trail.size() < 2) return;
            float w = Math.min(1.0F, (snake - SNAKE_FROM) / (1 - SNAKE_FROM));
            // the polyline: the head now, then its path
            double[][] path = new double[trail.size() + 1][];
            path[0] = toWorld(pose, 0, x, y, z, bodyYaw);
            int n = 1;
            for (double[] p : trail) path[n++] = p;
            double[] joint = path[0];
            int seg = 0;
            double t = 0;
            double[][] world = new double[N + 1][];
            world[0] = joint;
            for (int k = 0; k < N; k++) {
                double len = pose.lengths[k] * AlphaStorm.PX;
                double[] next = null;
                while (seg < n - 1) {
                    double[] a = path[seg], b = path[seg + 1];
                    double ex = b[0] - a[0], ey = b[1] - a[1], ez = b[2] - a[2];
                    double fx = a[0] - joint[0], fy = a[1] - joint[1], fz = a[2] - joint[2];
                    double qa = ex * ex + ey * ey + ez * ez, qb = 2 * (fx * ex + fy * ey + fz * ez), qc = fx * fx + fy * fy + fz * fz - len * len;
                    double disc = qb * qb - 4 * qa * qc;
                    if (qa > 1.0E-9 && disc >= 0) {
                        double r = (-qb + Math.sqrt(disc)) / (2 * qa);
                        if (r >= t && r <= 1) {
                            t = r;
                            next = new double[] {a[0] + ex * r, a[1] + ey * r, a[2] + ez * r};
                            break;
                        }
                    }
                    seg++;
                    t = 0;
                }
                if (next == null) {
                    // the path is too short: the rest of the body goes on straight
                    double[] a = path[Math.max(0, n - 2)], b = path[n - 1];
                    double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2], l = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (l < 1.0E-6) {
                        dx = joint[0] - (k > 0 ? world[k - 1][0] : joint[0]);
                        dy = joint[1] - (k > 0 ? world[k - 1][1] : joint[1]);
                        dz = joint[2] - (k > 0 ? world[k - 1][2] : joint[2]);
                        l = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    }
                    if (l < 1.0E-6) return;
                    next = new double[] {joint[0] + dx / l * len, joint[1] + dy / l * len, joint[2] + dz / l * len};
                    seg = n - 1;
                }
                world[k + 1] = next;
                joint = next;
            }
            // back to the frame of the serpent (pixels), blended with the pose of the waves
            float r = bodyYaw * Mth.DEG_TO_RAD;
            double sin = Math.sin(r), cos = Math.cos(r);
            for (int k = 1; k <= N; k++) {
                double wx = (world[k][0] - x) / AlphaStorm.PX, wy = (world[k][1] - y) / AlphaStorm.PX, wz = (world[k][2] - z) / AlphaStorm.PX;
                float lx = (float) (wx * cos + wz * sin), lz = (float) (-wx * sin + wz * cos);
                pose.jx[k] += (lx - pose.jx[k]) * w;
                pose.jy[k] += ((float) wy - pose.jy[k]) * w;
                pose.jz[k] += (lz - pose.jz[k]) * w;
            }
            // the angles of the pieces from their joints (front to back)
            for (int k = 0; k < N; k++) {
                float dx = pose.jx[k + 1] - pose.jx[k], dy = pose.jy[k + 1] - pose.jy[k], dz = pose.jz[k + 1] - pose.jz[k];
                float l = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                if (l < 1.0E-4F) continue;
                pose.pitch[k] = (float) Math.toDegrees(Math.asin(Mth.clamp(dy / l, -1, 1)));
                pose.yaw[k] = (float) Math.toDegrees(Math.atan2(dx, -dz));
            }
        }

        /** Joint k of a pose in the world (blocks). */
        private static double[] toWorld(Pose pose, int k, double x, double y, double z, float bodyYaw) {
            float r = bodyYaw * Mth.DEG_TO_RAD;
            double sin = Math.sin(r), cos = Math.cos(r);
            double lx = pose.jx[k] * AlphaStorm.PX, ly = pose.jy[k] * AlphaStorm.PX, lz = pose.jz[k] * AlphaStorm.PX;
            return new double[] {x + lx * cos - lz * sin, y + ly, z + lx * sin + lz * cos};
        }

        private static double dist(double[] a, double[] b) {
            double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Solves the pose for this moment. */
        public void solve(Pose pose, float partialTick, float age, float headYaw, float headPitch) {
            pose.solve(weight(COIL, partialTick), weight(SLITHER, partialTick), weight(FLY, partialTick), weight(DESCEND, partialTick),
                    phase(partialTick), age, clampYaw(headYaw), clampPitch(headPitch), climb(partialTick), bend(partialTick),
                    air(partialTick));
        }
    }

    public static float clampYaw(float yaw) {
        return Mth.clamp(yaw, -HEAD_YAW_MAX, HEAD_YAW_MAX);
    }

    public static float clampPitch(float pitch) {
        return Mth.clamp(pitch, HEAD_PITCH_MIN, HEAD_PITCH_MAX);
    }
}
