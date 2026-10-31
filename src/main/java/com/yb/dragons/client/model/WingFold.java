package com.yb.dragons.client.model;

/**
 * The folded pose of the wings of the dragons (on the ground), built so that the wings never go through the body or the
 * legs: the arm runs back along the top of the flank, the forearm comes forward along it, the fingers go back again, and
 * the membranes hang down along the flank, leaning outwards, over the legs. The membranes of the fingers fold up like a
 * fan ({@link #FINGER_WIDTH}) since they are much wider than the body is high.
 *
 * <p>The pose is given by directions in the model space of the right wing (x &lt; 0 side; y down, z towards the tail),
 * turned into the Euler angles of the model parts (Z then Y then X, like the game). The left wing is its mirror.</p>
 */
public final class WingFold {
    /** Width of the membranes of the fingers when folded (1 = spread). */
    public static final float FINGER_WIDTH = 0.3F;
    /** Folded, the shoulder sits this far out of the flank (model pixels) and at this height (the top of the body is 4). */
    public static final float SHOULDER_OUT = 3.5F, SHOULDER_Y = 1.5F;

    /** Rotations (x, y, z) of the arm, the forearm and the 4 fingers, each in the frame of its parent. */
    public final float[] arm, forearm;
    public final float[][] fingers = new float[4][];

    private WingFold(float[] arm, float[] forearm, float[][] fingers) {
        this.arm = arm;
        this.forearm = forearm;
        System.arraycopy(fingers, 0, this.fingers, 0, 4);
    }

    /**
     * The fold of every dragon (the shoulders follow the width of the body, so it also fits the chubby terra dragon and
     * its second pair). Checked numerically for every race, stage, walking and sitting pose: no point of a wing inside the
     * body or a leg.
     */
    public static final WingFold FOLD = build(0.10, 0.55, 0.0);

    /**
     * @param out  how much the arm goes outwards while it runs back (per unit back)
     * @param lean how much the membranes lean outwards (per unit down)
     * @param fingerOut how much the fingers go outwards while they run back
     */
    static WingFold build(double out, double lean, double fingerOut) {
        // arm: back, outwards, a little up; its membrane hangs down and outwards
        double[][] ra = frame(new double[] {-out, -0.10, 1}, new double[] {-lean, 1, 0});
        // forearm: forwards along the flank, a little outwards and down
        double[][] rf = frame(new double[] {-0.06, 0.03, -1}, new double[] {-lean, 1, 0});
        // fingers: back again, fanning down one after the other
        double[][][] rk = new double[4][][];
        for (int i = 0; i < 4; i++) {
            rk[i] = frame(new double[] {-fingerOut - 0.02 * i, 0.05 + 0.07 * i, 1}, new double[] {-lean - 0.05 * i, 1, 0});
        }
        float[][] fingers = new float[4][];
        for (int i = 0; i < 4; i++) fingers[i] = euler(mul(transpose(rf), rk[i]));
        return new WingFold(euler(ra), euler(mul(transpose(ra), rf)), fingers);
    }

    /**
     * The rotation that turns the local axes of a wing part to the world: its bone (local -x) along {@code bone}, its
     * membrane (local +z) towards {@code membrane} (made square to the bone).
     */
    static double[][] frame(double[] bone, double[] membrane) {
        double[] d = norm(bone);
        double[] x = {-d[0], -d[1], -d[2]};
        double k = dot(membrane, d);
        double[] z = norm(new double[] {membrane[0] - k * d[0], membrane[1] - k * d[1], membrane[2] - k * d[2]});
        double[] y = cross(z, x);
        return new double[][] {{x[0], y[0], z[0]}, {x[1], y[1], z[1]}, {x[2], y[2], z[2]}};
    }

    /** Euler angles (x, y, z) of R = Rz * Ry * Rx. */
    static float[] euler(double[][] r) {
        double b = Math.asin(Math.max(-1, Math.min(1, -r[2][0])));
        double a = Math.atan2(r[2][1], r[2][2]);
        double c = Math.atan2(r[1][0], r[0][0]);
        return new float[] {(float) a, (float) b, (float) c};
    }

    static double[][] mul(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j];
        return r;
    }

    static double[][] transpose(double[][] a) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) r[i][j] = a[j][i];
        return r;
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] norm(double[] v) {
        double l = Math.sqrt(dot(v, v));
        return new double[] {v[0] / l, v[1] / l, v[2] / l};
    }
}
