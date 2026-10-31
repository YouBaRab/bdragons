package com.yb.dragons.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The animations of the Sun Steed (alpha of the sunlight), written by {@code tools/gen_sun_model.py} from its Blockbench
 * file {@code tools/models/dragon_soleil_alpha.bbmodel}: do not change them here, change the Blockbench file and run the
 * script again.
 *
 * <p>They are played like Blockbench plays them (Modded Entity format): every keyframe value is added to the rest pose
 * of its group (rotations in degrees, positions in pixels, scales as factors); between two keyframes the value is
 * linear, smooth (Catmull-Rom, when one of the two is smooth) or held (step).</p>
 */
public final class SunSteedAnimations {
    /** A channel of one part: kind 0 = rotation, 1 = position, 2 = scale; mode of each key: 0 linear, 1 smooth, 2 step. */
    public record Channel(int bone, int kind, float[] times, float[] values, byte[] modes) {
        /** The value of the channel (x, y, z) at this time (seconds), into out. */
        void sample(float t, float[] out) {
            int n = times.length;
            if (t <= times[0] || n == 1) {
                set(out, 0);
                return;
            }
            if (t >= times[n - 1]) {
                set(out, n - 1);
                return;
            }
            int i = 0;
            while (i < n - 2 && t >= times[i + 1]) i++;
            float k = (t - times[i]) / Math.max(1.0E-6F, times[i + 1] - times[i]);
            if (modes[i] == 2) {
                set(out, i);
                return;
            }
            boolean smooth = modes[i] == 1 || modes[i + 1] == 1;
            for (int c = 0; c < 3; c++) {
                float p1 = values[i * 3 + c], p2 = values[(i + 1) * 3 + c];
                if (smooth) {
                    float p0 = i > 0 ? values[(i - 1) * 3 + c] : p1;
                    float p3 = i + 2 < n ? values[(i + 2) * 3 + c] : p2;
                    out[c] = catmullRom(k, p0, p1, p2, p3);
                } else {
                    out[c] = Mth.lerp(k, p1, p2);
                }
            }
        }

        private void set(float[] out, int i) {
            out[0] = values[i * 3];
            out[1] = values[i * 3 + 1];
            out[2] = values[i * 3 + 2];
        }
    }

    /** An animation: its length (seconds), whether it loops, its channels. */
    public record Anim(float length, boolean loop, Channel... channels) {
        /** The time of the animation (seconds) for a time since its start: wrapped when it loops, held at its end. */
        public float time(float seconds) {
            if (loop) return length <= 0 ? 0 : seconds - length * Mth.floor(seconds / length);
            return Mth.clamp(seconds, 0, length);
        }

        /**
         * Adds this animation at the given time (seconds since its start) to the parts, with a weight (0..1).
         * @param skip a bone index that is not played (-1 = none)
         */
        public void apply(ModelPart[] parts, float seconds, float weight, int skip) {
            if (weight <= 0) return;
            float t = time(seconds);
            float[] v = new float[3];
            for (Channel ch : channels) {
                if (ch.bone == skip) continue;
                ModelPart p = parts[ch.bone];
                if (p == null) continue;
                ch.sample(t, v);
                switch (ch.kind) {
                    // Blockbench -> game: x and y of the rotations and of the positions are the other way
                    case 0 -> {
                        p.xRot -= v[0] * Mth.DEG_TO_RAD * weight;
                        p.yRot -= v[1] * Mth.DEG_TO_RAD * weight;
                        p.zRot += v[2] * Mth.DEG_TO_RAD * weight;
                    }
                    case 1 -> {
                        p.x -= v[0] * weight;
                        p.y -= v[1] * weight;
                        p.z += v[2] * weight;
                    }
                    default -> {
                        p.xScale *= 1 + (v[0] - 1) * weight;
                        p.yScale *= 1 + (v[1] - 1) * weight;
                        p.zScale *= 1 + (v[2] - 1) * weight;
                    }
                }
            }
        }

        /** The value of one channel of one bone at a time (seconds since the start), or null when it has none. */
        public float[] value(int bone, int kind, float seconds) {
            for (Channel ch : channels) {
                if (ch.bone == bone && ch.kind == kind) {
                    float[] v = new float[3];
                    ch.sample(time(seconds), v);
                    return v;
                }
            }
            return null;
        }
    }

    static float catmullRom(float k, float p0, float p1, float p2, float p3) {
        return 0.5F * (2 * p1 + (-p0 + p2) * k + (2 * p0 - 5 * p1 + 4 * p2 - p3) * k * k + (-p0 + 3 * p1 - 3 * p2 + p3) * k * k * k);
    }

    /** The index of a bone in {@link #BONES} from its name (the last part of its path), -1 when it is not animated. */
    public static int bone(String name) {
        for (int i = 0; i < BONES.length; i++) {
            String b = BONES[i];
            if (b.equals(name) || b.endsWith("/" + name)) return i;
        }
        return -1;
    }

    /** The animated parts (paths from the root of the model). */
    public static final String[] BONES = {"body", "body/neck", "body/tail1", "body/tail1/tail2/tail3", "body/wing_l", "body/neck/head/halo", "body/wing_r", "body/leg_fl", "body/leg_br", "body/leg_fr", "body/leg_bl", "body/leg_fl/shin_fl", "body/leg_br/shin_br", "body/leg_fr/shin_fr", "body/leg_bl/shin_bl", "body/wing_l/forearm_l", "body/wing_l/forearm_l/hand_l", "body/tail1/tail2", "body/wing_r/forearm_r", "body/wing_r/forearm_r/hand_r", "body/sun"};

    public static final Anim IDLE = new Anim(4F, true,
            new Channel(0, 1, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(1, 0, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(2, 0, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, -8.0F, 0.0F, 0.0F, 8.0F, 0.0F, 0.0F, -8.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(3, 0, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, 10.0F, 0.0F, 0.0F, -10.0F, 0.0F, 0.0F, 10.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(4, 0, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(5, 1, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(6, 0, new float[] {0.0F, 2.0F, 4.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}));

    public static final Anim WALK = new Anim(1F, true,
            new Channel(7, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 18.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(8, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 18.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(9, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {-18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -18.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(10, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {-18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -18.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(11, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {-30.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -10.0F, 0.0F, 0.0F, -30.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(12, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(13, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {0.0F, 0.0F, 0.0F, -10.0F, 0.0F, 0.0F, -30.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(14, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {25.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(1, 0, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {-4.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(0, 1, new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F},
                    new float[] {0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(2, 0, new float[] {0.0F, 0.5F, 1.0F},
                    new float[] {0.0F, -6.0F, 0.0F, 0.0F, 6.0F, 0.0F, 0.0F, -6.0F, 0.0F},
                    new byte[] {1, 1, 1}));

    public static final Anim FLY = new Anim(0.9F, true,
            new Channel(4, 0, new float[] {0.0F, 0.3F, 0.6F, 0.9F},
                    new float[] {0.0F, 0.0F, 8.0F, 0.0F, 10.0F, -80.0F, 0.0F, 0.0F, -35.0F, 0.0F, 0.0F, 8.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(15, 0, new float[] {0.0F, 0.3F, 0.6F, 0.9F},
                    new float[] {0.0F, 0.0F, -15.0F, 0.0F, 0.0F, 15.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, -15.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(16, 0, new float[] {0.0F, 0.35F, 0.65F, 0.9F},
                    new float[] {0.0F, 0.0F, -20.0F, 0.0F, 0.0F, 20.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, -20.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(0, 1, new float[] {0.0F, 0.35F, 0.9F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(1, 0, new float[] {0.0F, 0.3F, 0.9F},
                    new float[] {-8.0F, 0.0F, 0.0F, -14.0F, 0.0F, 0.0F, -8.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(7, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {45.0F, 0.0F, 0.0F, 60.0F, 0.0F, 0.0F, 45.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(9, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {60.0F, 0.0F, 0.0F, 45.0F, 0.0F, 0.0F, 60.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(11, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {-80.0F, 0.0F, 0.0F, -95.0F, 0.0F, 0.0F, -80.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(13, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {-95.0F, 0.0F, 0.0F, -80.0F, 0.0F, 0.0F, -95.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(10, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {-40.0F, 0.0F, 0.0F, -50.0F, 0.0F, 0.0F, -40.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(8, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {-50.0F, 0.0F, 0.0F, -40.0F, 0.0F, 0.0F, -50.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(14, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {25.0F, 0.0F, 0.0F, 35.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(12, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {35.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 35.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(2, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {-25.0F, 0.0F, 0.0F, -32.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(17, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {5.0F, 0.0F, 0.0F, 12.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(3, 0, new float[] {0.0F, 0.45F, 0.9F},
                    new float[] {8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1}),
            new Channel(6, 0, new float[] {0.0F, 0.3F, 0.6F, 0.9F},
                    new float[] {0.0F, 0.0F, -8.0F, 0.0F, -10.0F, 80.0F, 0.0F, 0.0F, 35.0F, 0.0F, 0.0F, -8.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(18, 0, new float[] {0.0F, 0.3F, 0.6F, 0.9F},
                    new float[] {0.0F, 0.0F, 15.0F, 0.0F, 0.0F, -15.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, 15.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(19, 0, new float[] {0.0F, 0.35F, 0.65F, 0.9F},
                    new float[] {0.0F, 0.0F, 20.0F, 0.0F, 0.0F, -20.0F, 0.0F, 0.0F, -10.0F, 0.0F, 0.0F, 20.0F},
                    new byte[] {1, 1, 1, 1}));

    public static final Anim ULT = new Anim(3.0F, false,
            new Channel(0, 0, new float[] {0.0F, 0.35F, 1.3F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 45.0F, 0.0F, 0.0F, 49.0F, 0.0F, 0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(10, 0, new float[] {0.0F, 0.35F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, -40.0F, 0.0F, 0.0F, -40.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(14, 0, new float[] {0.0F, 0.35F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(1, 0, new float[] {0.0F, 0.35F, 0.7F, 1.0F, 1.4F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -15.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -14.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1}),
            new Channel(2, 0, new float[] {0.0F, 0.35F, 1.2F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, -30.0F, 0.0F, 0.0F, -30.0F, 12.0F, 0.0F, -30.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(4, 0, new float[] {0.0F, 0.35F, 0.6F, 0.85F, 1.1F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 8.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 8.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1}),
            new Channel(7, 0, new float[] {0.0F, 0.35F, 0.6F, 0.85F, 1.1F, 1.35F, 1.6F, 1.85F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1}),
            new Channel(11, 0, new float[] {0.0F, 0.35F, 0.6F, 0.85F, 1.1F, 1.35F, 1.6F, 1.85F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1}),
            new Channel(9, 0, new float[] {0.0F, 0.35F, 0.6F, 0.85F, 1.1F, 1.35F, 1.6F, 1.85F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 75.0F, 0.0F, 0.0F, 25.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1}),
            new Channel(13, 0, new float[] {0.0F, 0.35F, 0.6F, 0.85F, 1.1F, 1.35F, 1.6F, 1.85F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, -85.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1}),
            new Channel(8, 0, new float[] {0.0F, 0.35F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, -40.0F, 0.0F, 0.0F, -40.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(12, 0, new float[] {0.0F, 0.35F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1}),
            new Channel(6, 0, new float[] {0.0F, 0.35F, 0.6F, 0.85F, 1.1F, 2.2F, 2.7F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -8.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -8.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F},
                    new byte[] {1, 1, 1, 1, 1, 1, 1}),
            new Channel(20, 0, new float[] {0.0F, 1.5F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 540.0F, 0.0F},
                    new byte[] {0, 0}),
            new Channel(20, 1, new float[] {0.0F, 0.9F, 1.05F, 1.25F, 1.5F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 14.142F, -16.971F, 0.0F, -11.314F, -45.255F, 0.0F, -84.853F, -56.569F},
                    new byte[] {1, 1, 1, 1, 1}),
            new Channel(20, 2, new float[] {0.0F, 0.45F, 0.6F, 0.75F, 0.9F, 1.45F, 1.5F, 3.0F},
                    new float[] {1.0F, 1.0F, 1.0F, 1.02F, 1.02F, 1.02F, 1.0F, 1.0F, 1.0F, 1.03F, 1.03F, 1.03F, 1.02F, 1.02F, 1.02F, 0.7F, 0.7F, 0.7F, 0.01F, 0.01F, 0.01F, 0.01F, 0.01F, 0.01F},
                    new byte[] {1, 1, 1, 1, 1, 1, 2, 2}));

    public static final Anim SUN_SPIN = new Anim(180F, true,
            new Channel(20, 0, new float[] {0.0F, 180.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 720.0F, 1080.0F, 0.0F},
                    new byte[] {0, 0}));

    public static final Anim SUN_AFK = new Anim(11.5F, false,
            new Channel(20, 0, new float[] {0.0F, 0.125F, 0.25F, 0.375F, 0.5F, 0.625F, 0.75F, 0.875F, 1.0F, 1.125F, 1.25F, 1.375F, 1.5F, 1.625F, 1.75F, 1.875F, 2.0F, 2.125F, 2.25F, 2.375F, 2.5F, 2.625F, 2.75F, 2.875F, 3.0F, 3.125F, 3.25F, 3.375F, 3.5F, 3.625F, 3.75F, 3.875F, 4.0F, 4.125F, 4.25F, 4.375F, 4.5F, 4.625F, 4.75F, 4.875F, 5.0F, 5.125F, 5.25F, 5.375F, 5.5F, 5.625F, 5.75F, 5.875F, 6.0F, 6.125F, 6.25F, 6.375F, 6.5F, 6.625F, 6.75F, 6.875F, 7.0F, 7.125F, 7.25F, 7.375F, 7.5F, 7.625F, 7.75F, 7.875F, 8.0F, 8.125F, 8.25F, 8.375F, 8.5F, 8.625F, 8.75F, 8.875F, 9.0F, 9.125F, 9.25F, 9.375F, 9.5F, 9.625F, 9.75F, 9.875F, 10.0F, 11.5F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.5F, 0.76F, 0.0F, 1.05F, 1.57F, 0.0F, 1.67F, 2.51F, 0.0F, 2.41F, 3.62F, 0.0F, 3.31F, 4.96F, 0.0F, 4.4F, 6.59F, 0.0F, 5.72F, 8.58F, 0.0F, 7.32F, 10.98F, 0.0F, 9.23F, 13.85F, 0.0F, 11.49F, 17.24F, 0.0F, 14.15F, 21.22F, 0.0F, 17.23F, 25.85F, 0.0F, 20.78F, 31.17F, 0.0F, 24.84F, 37.26F, 0.0F, 29.45F, 44.17F, 0.0F, 34.64F, 51.96F, 0.0F, 40.46F, 60.69F, 0.0F, 46.94F, 70.41F, 0.0F, 54.12F, 81.19F, 0.0F, 62.05F, 93.08F, 0.0F, 70.76F, 106.14F, 0.0F, 80.29F, 120.43F, 0.0F, 90.67F, 136.01F, 0.0F, 101.96F, 152.94F, 0.0F, 114.18F, 171.28F, 0.0F, 127.38F, 191.08F, 0.0F, 141.6F, 212.4F, 0.0F, 156.87F, 235.31F, 0.0F, 173.24F, 259.85F, 0.0F, 190.73F, 286.1F, 0.0F, 209.4F, 314.1F, 0.0F, 229.28F, 343.92F, 0.0F, 250.41F, 375.62F, 0.0F, 272.83F, 409.25F, 0.0F, 296.58F, 444.87F, 0.0F, 321.69F, 482.54F, 0.0F, 348.21F, 522.32F, 0.0F, 376.18F, 564.27F, 0.0F, 405.63F, 608.44F, 0.0F, 436.6F, 654.9F, 0.0F, 469.14F, 703.71F, 0.0F, 503.28F, 754.91F, 0.0F, 539.05F, 808.58F, 0.0F, 576.51F, 864.77F, 0.0F, 615.69F, 923.53F, 0.0F, 656.62F, 984.93F, 0.0F, 699.35F, 1049.03F, 0.0F, 743.92F, 1115.88F, 0.0F, 790.36F, 1185.55F, 0.0F, 838.72F, 1258.08F, 0.0F, 889.03F, 1333.55F, 0.0F, 941.33F, 1412.0F, 0.0F, 995.67F, 1493.5F, 0.0F, 1052.07F, 1578.1F, 0.0F, 1110.58F, 1665.87F, 0.0F, 1171.24F, 1756.86F, 0.0F, 1234.09F, 1851.14F, 0.0F, 1299.17F, 1948.75F, 0.0F, 1366.51F, 2049.76F, 0.0F, 1436.15F, 2154.23F, 0.0F, 1508.14F, 2262.21F, 0.0F, 1582.51F, 2373.77F, 0.0F, 1659.31F, 2488.96F, 0.0F, 1738.56F, 2607.84F, 0.0F, 1820.32F, 2730.48F, 0.0F, 1904.61F, 2856.92F, 0.0F, 1991.48F, 2987.23F, 0.0F, 2080.97F, 3121.46F, 0.0F, 2173.12F, 3259.68F, 0.0F, 2267.96F, 3401.94F, 0.0F, 2365.53F, 3548.3F, 0.0F, 2465.88F, 3698.82F, 0.0F, 2569.04F, 3853.57F, 0.0F, 2675.06F, 4012.58F, 0.0F, 2783.96F, 4175.94F, 0.0F, 2895.79F, 4343.69F, 0.0F, 3010.59F, 4515.89F, 0.0F, 3128.4F, 4692.61F, 0.0F, 3249.26F, 4873.89F, 0.0F, 3373.2F, 5059.8F, 0.0F, 3379.2F, 5068.8F, 0.0F},
                    new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
            new Channel(20, 2, new float[] {0.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F, 10.02F, 10.15F, 10.5F, 11.0F, 11.5F},
                    new float[] {1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.983F, 0.983F, 0.983F, 0.931F, 0.931F, 0.931F, 0.844F, 0.844F, 0.844F, 0.722F, 0.722F, 0.722F, 0.566F, 0.566F, 0.566F, 0.376F, 0.376F, 0.376F, 0.15F, 0.15F, 0.15F, 0.01F, 0.01F, 0.01F, 0.2F, 0.2F, 0.2F, 0.75F, 0.75F, 0.75F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F},
                    new byte[] {0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1}));

    public static final Anim SUN_REBUILD = new Anim(2.0F, false,
            new Channel(20, 0, new float[] {0.0F, 2.0F},
                    new float[] {0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F},
                    new byte[] {0, 0}),
            new Channel(20, 2, new float[] {0.0F, 2.0F},
                    new float[] {0.01F, 0.01F, 0.01F, 1.0F, 1.0F, 1.0F},
                    new byte[] {1, 1}));

    private SunSteedAnimations() {}
}
