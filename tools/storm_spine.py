"""Python twin of StormSpine.java (the pose of the Tempest Dragon), kept identical to the Java code: used by
storm_preview.py to draw the poses with the real model.

Frame of the serpent: x to its left, y up, z forward, in model pixels (16 px = 1 block before the size factor),
origin at the feet of the player.
Element k (25): 0..3 = the neck (neck4 touches the head, neck1 the chest), 4 = the torso (it carries the arms),
5..19 = segment1..segment15, 20..24 = tail1..tail5. Joint k = front of element k: joint 0 = the joint of the head,
joint 4 = the chest, joint 25 = the tip of the tail.
Angles of an element: yaw (deg, + = going back it turns towards the left) and pitch (deg, + = going back it goes up).

The four states (blended):
  STILL   (coil)    on the ground or in the air: the rearing pose of Rayquaza (Cobblemon): the torso straight up, the
                    neck in an S, the head level, the arms raised, the body spiralling on the ground (on its cloud in
                    the air), breathing slowly like the hovering Gyarados.
  SLITHER           moving on the ground, like the walk of Gyarados: the front raised, humps running back along the
                    body, a small sway.
  FLY               flying, like the flight of Gyarados: the body tilted with the climb, a vertical wave running back
                    along the body (stronger towards the tail) and a slower side wave; it climbs nose up.
  DESCEND (dive)    diving, like Dragonite: straight like an arrow along its path, nose down, the arms swept back.
"""
import math

LENGTHS = [16.0, 19.0, 17.0, 7.0, 37.0] + [7.0, 24.0, 24.0, 7.0, 33.0] * 2 + [7.0, 24.0, 24.0, 7.0, 31.0] \
    + [7.0, 24.0, 21.0, 21.0, 21.0]
WIDTHS = [19.0, 16.0, 16.0, 14.0, 19.0] + [14.0, 16.0, 16.0, 14.0, 19.0] * 3 + [11.0, 12.0, 9.0, 7.0, 4.0]
N = len(LENGTHS)
NECK = 4
TORSO = 4
D = math.pi / 180
TWO_PI = 2 * math.pi
COIL, SLITHER, FLY, DESCEND = 0, 1, 2, 3

HEAD_YAW_MAX, HEAD_PITCH_MIN, HEAD_PITCH_MAX = 70.0, -45.0, 55.0
# part of the turn of the head taken by each piece of the neck (from the head)
NECK_YAW = (0.22, 0.12, 0.05, 0.0)
NECK_PITCH = (0.25, 0.15, 0.05, 0.0)
# height of the chest (joint 4) above the feet in flight (pixels)
ANCHOR_FLY = 52.0
# breathing (still) and the waves: period of the breath (ticks), wavelengths (pixels)
BREATH = 40.0
FLY_WAVE, FLY_SIDE_WAVE, SLITHER_WAVE = 210.0, 420.0, 170.0

# --- the arms: rotations (radians, x y z like ModelPart) of shoulder, upper arm, forearm, hand, and the curl of the
# fingers, for the arm on the -x side of the model; the other arm is mirrored (y and z negated). Per state.
ARM_POSES = (
    # still: out, the forearms forward, claws open
    ((0.0, 0.2, -0.5), (0.0, 0.1, 0.0), (0.0, 0.42, -0.35), (0.0, 0.0, 0.0), 0.35),
    # slither: forward and down, claws ready
    ((0.0, -0.55, -0.45), (0.0, -0.2, 0.15), (0.0, -1.0, -0.2), (0.0, 0.0, 0.0), 0.5),
    # fly: out to the sides, the forearms down, the claws gripping the clouds below
    ((0.0, 0.15, -0.3), (0.0, 0.0, -0.35), (0.0, -0.3, -0.85), (0.0, 0.0, -0.4), 0.9),
    # dive: swept back along the body
    ((0.0, 1.15, -0.15), (0.0, 0.35, 0.0), (0.0, 0.3, 0.0), (0.0, 0.0, 0.0), 0.7),
)
# pivots of the arm chain (Minecraft model pixels, y down): shoulder in the frame of the torso, then each part in
# the frame of the one before (arm on the -x side)
SHOULDER = (-8.0, 0.0, 11.0)
UPPER = (-4.0, 0.0, 0.0)
FOREARM = (-20.0, 0.0, 0.0)
HAND = (-18.0, 0.0, 0.0)
# where the talons hold their cloud, in the frame of the hand
GRIP = (-10.0, 5.0, 0.0)
# the mouth, in the frame of the head
MOUTH = (0.0, 4.0, -44.0)

# arc length from the chest (joint 4) to the middle of each element of the body
ARC = [0.0] * N
for _k in range(TORSO, N):
    ARC[_k] = sum(LENGTHS[TORSO:_k]) + LENGTHS[_k] / 2


def slope(k):
    """Pitch of an element lying on the ground: its centre line stays at half its thickness."""
    drop = (WIDTHS[min(k + 1, N - 1)] - WIDTHS[k]) / 2
    return math.degrees(math.asin(drop / LENGTHS[k]))


def ramp(k, start, full):
    """0 before element start, 1 from element full."""
    return min(1.0, max(0.0, (k - start) / float(full - start)))


def still_shape():
    """The rearing pose: neck in an S, torso straight up, the body going down and spiralling on the ground."""
    yaw = [0.0] * N
    pitch = [0.0] * N
    pitch[0:4] = [14.0, -28.0, -62.0, -84.0]
    pitch[4] = -80.0
    pitch[5], pitch[6] = -58.0, -26.0
    yaw[6] = 25.0
    r0, spacing = 54.0, 23.0
    turned = 0.0
    heading = 70.0
    for k in range(7, N):
        r = r0 - spacing * turned / 360.0
        step = 2 * math.degrees(math.asin(min(1.0, LENGTHS[k] / (2 * r))))
        heading -= step if k > 7 else step / 2
        turned += step
        yaw[k] = heading
        pitch[k] = slope(k)
    return yaw, pitch


STILL_YAW, STILL_PITCH = still_shape()

# --- still in the air: a tornado. The head and the arms at the centre, the torso going down, then the body winds
# around the chest in a cone that goes down and gets narrower, like a tornado.
ANCHOR_HOVER = 60.0      # height of the chest above the feet
TORNADO_DROP = 80.0      # the body goes down this far, from the bottom of the torso to the tip of the tail
TORNADO_TOP = 60.0       # radius of the widest turn of the cone
TORNADO_GROW = 90.0      # arc length (from the bottom of the torso) over which the body opens to the widest turn
TORNADO_END = 20.0       # radius of the cone at the tip of the tail
TORNADO = [0.0, 0.0, 0.0]  # radius and height (relative to the chest) of the bottom of the torso, total arc


def tornado_radius(s):
    """Radius of the turn at arc length s from the bottom of the torso."""
    r0, total = TORNADO[0], TORNADO[2]
    if s <= TORNADO_GROW:
        return r0 + (TORNADO_TOP - r0) * s / TORNADO_GROW
    return TORNADO_TOP + (TORNADO_END - TORNADO_TOP) * (s - TORNADO_GROW) / (total - TORNADO_GROW)


def tornado_shape():
    yaw = list(STILL_YAW[:4]) + [0.0] * (N - 4)
    pitch = list(STILL_PITCH[:4]) + [0.0] * (N - 4)
    pitch[TORSO] = -75.0
    # the bottom of the torso
    d = direction(yaw[TORSO], pitch[TORSO])
    p = (LENGTHS[TORSO] * d[0], LENGTHS[TORSO] * d[1], LENGTHS[TORSO] * d[2])
    total = sum(LENGTHS[TORSO + 1:])
    TORNADO[0], TORNADO[1], TORNADO[2] = math.hypot(p[0], p[2]), p[1], total
    theta = math.atan2(p[0], p[2])
    rp, s = TORNADO[0], 0.0
    prev = yaw[TORSO]
    for k in range(TORSO + 1, N):
        L = LENGTHS[k]
        s += L
        r1 = tornado_radius(s)
        dy = -TORNADO_DROP * L / total
        c = (r1 * r1 + rp * rp + dy * dy - L * L) / (2 * r1 * rp)
        theta += math.acos(max(-1.0, min(1.0, c)))
        q = (r1 * math.sin(theta), p[1] + dy, r1 * math.cos(theta))
        v = (q[0] - p[0], q[1] - p[1], q[2] - p[2])
        n = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        pitch[k] = math.degrees(math.asin(max(-1.0, min(1.0, v[1] / n))))
        a = math.degrees(math.atan2(v[0], -v[2]))
        # keep the turn continuous (no jump of 360 degrees)
        while a - prev > 180:
            a -= 360
        while a - prev < -180:
            a += 360
        yaw[k] = a
        prev = a
        # the real end of the element (its length may not reach the cone exactly)
        dd = direction(yaw[k], pitch[k])
        p = (p[0] + L * dd[0], p[1] + L * dd[1], p[2] + L * dd[2])
        rp = math.hypot(p[0], p[2])
        theta = math.atan2(p[0], p[2])
    return yaw, pitch


def direction(yaw, pitch):
    y, p = yaw * D, pitch * D
    return (math.sin(y) * math.cos(p), math.sin(p), -math.cos(y) * math.cos(p))


HOVER_YAW, HOVER_PITCH = tornado_shape()


def tornado(t):
    """Radius and height (above the feet) of the cone of the tornado, t from 0 (widest turn) to 1 (tip of the tail)."""
    return TORNADO_TOP + (TORNADO_END - TORNADO_TOP) * t, ANCHOR_HOVER + TORNADO[1] - TORNADO_DROP * (
        TORNADO_GROW + (TORNADO[2] - TORNADO_GROW) * t) / TORNADO[2]


def direction(yaw, pitch):
    y, p = yaw * D, pitch * D
    return (math.sin(y) * math.cos(p), math.sin(p), -math.cos(y) * math.cos(p))


def rx(v, a):
    c, s = math.cos(a), math.sin(a)
    return (v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c)


def ry(v, a):
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c)


def rz(v, a):
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2])


def rzyx(v, r):
    """Like ModelPart: rotation x first, then y, then z."""
    return rz(ry(rx(v, r[0]), r[1]), r[2])


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def mirror(r, side):
    """The rotation of the arm on the side: side 0 = -x (as written), side 1 = +x (y and z negated)."""
    return r if side == 0 else (r[0], -r[1], -r[2])


class Pose:
    def __init__(self):
        self.yaw = [0.0] * N
        self.pitch = [0.0] * N
        self.joints = [(0.0, 0.0, 0.0)] * (N + 1)
        self.arm = [[0.0] * 3 for _ in range(4)]
        self.curl = 0.0
        self.head_yaw = self.head_pitch = 0.0

    def solve(self, coil, slither, fly, desc, phase, age, head_yaw, head_pitch, climb, bend, air):
        yaw, pitch = self.yaw, self.pitch
        swing = fly + desc
        breath = math.sin(age * TWO_PI / BREATH)
        for k in range(N):
            body = k >= TORSO
            # still: the rearing pose on the ground, the tornado in the air, breathing (the wave of the breath runs
            # down the neck and the torso)
            cy = STILL_YAW[k] + (HOVER_YAW[k] - STILL_YAW[k]) * air
            cp = STILL_PITCH[k] + (HOVER_PITCH[k] - STILL_PITCH[k]) * air
            if k <= 5:
                cp += 3.0 * math.sin(age * TWO_PI / BREATH - k * 1.05)
            if k >= N - 3:
                cy += math.sin(age * 0.11 + k) * 18.0 * (k - (N - 4)) / 3.0
            # slither: the front raised, humps running back along the body, a small sway
            if k < 4:
                sp = (10.0, -18.0, -38.0, -45.0)[k]
                sy = 0.0
            elif k <= 6:
                sp = (-32.0, -16.0, -5.0)[k - 4]
                sy = 0.0
            else:
                r = ramp(k, 6, 10)
                sp = slope(k) + 14.0 * r * math.sin(phase - TWO_PI * ARC[k] / SLITHER_WAVE)
                sy = 9.0 * r * math.sin(0.5 * phase - TWO_PI * ARC[k] / (2 * SLITHER_WAVE))
            # fly: tilted with the climb, the vertical wave stronger towards the tail, the slower side wave
            t = (k - TORSO) / float(N - 1 - TORSO) if body else 0.0
            if body:
                fp = -climb + (4.0 + 18.0 * t) * math.sin(phase - TWO_PI * ARC[k] / FLY_WAVE)
                fy = (2.0 + 12.0 * t) * math.sin(0.5 * phase - TWO_PI * ARC[k] / FLY_SIDE_WAVE)
            else:
                fp = -climb + (-18.0, -8.0, 0.0, 6.0)[3 - k]
                fy = 0.0
            # dive: straight along the path, the waves almost gone
            if body:
                dp = -climb + (1.5 + 5.0 * t) * math.sin(phase - TWO_PI * ARC[k] / FLY_WAVE)
                dy = (1.0 + 4.0 * t) * math.sin(0.5 * phase - TWO_PI * ARC[k] / FLY_SIDE_WAVE)
            else:
                dp = -climb
                dy = 0.0
            curve = bend * max(0, k - TORSO)
            if k < NECK:
                look_y = head_yaw * NECK_YAW[k]
                look_p = head_pitch * NECK_PITCH[k]
                cy, sy, fy, dy = cy + look_y, sy + look_y, fy + look_y, dy + look_y
                cp, sp, fp, dp = cp + look_p, sp + look_p, fp + look_p, dp + look_p
                curve = 0.0
            yaw[k] = coil * cy + slither * (sy - curve) + fly * (fy - curve) + desc * (dy - curve)
            pitch[k] = coil * cp + slither * sp + fly * fp + desc * dp
        # the joints, from the chest: the neck goes forward, the body goes back
        j = [(0.0, 0.0, 0.0)] * (N + 1)
        for k in range(NECK - 1, -1, -1):
            d = direction(yaw[k], pitch[k])
            p = j[k + 1]
            j[k] = (p[0] - LENGTHS[k] * d[0], p[1] - LENGTHS[k] * d[1], p[2] - LENGTHS[k] * d[2])
        for k in range(TORSO, N):
            d = direction(yaw[k], pitch[k])
            p = j[k]
            j[k + 1] = (p[0] + LENGTHS[k] * d[0], p[1] + LENGTHS[k] * d[1], p[2] + LENGTHS[k] * d[2])
        # on the ground (and on its cloud), the lowest piece of the body touches the ground; in flight, the chest is
        # at a fixed height
        low = 1e9
        for k in range(TORSO + 1, N):
            low = min(low, min(j[k][1], j[k + 1][1]) - WIDTHS[k] / 2)
        lift = coil * ((1 - air) * -low + air * ANCHOR_HOVER) + slither * -low + swing * ANCHOR_FLY + air * coil * 1.5 * breath
        self.joints = [(p[0], p[1] + lift, p[2]) for p in j]
        self.head_yaw, self.head_pitch = head_yaw, head_pitch
        w = (coil, slither, fly, desc)
        for part in range(4):
            self.arm[part] = [sum(w[s] * ARM_POSES[s][part][i] for s in range(4)) for i in range(3)]
        self.curl = sum(w[s] * ARM_POSES[s][4] for s in range(4))

    # --- Minecraft model space (pixels, y down, front -z) --------------------------------------------------------
    def model_joint(self, k):
        j = self.joints[k]
        return (j[0], 24.0 - j[1], -j[2])

    @staticmethod
    def bone_joint(k):
        """The joint where the bone of element k has its pivot: the neck pieces hang from their back joint (their
        cubes go forward), the pieces of the body from their front joint (their cubes go back)."""
        return k + 1 if k < NECK else k

    def arm_rot(self, part, side):
        return mirror(self.arm[part], side)

    def grip(self, side):
        """Where the talons of a hand hold their cloud, in Minecraft model space (side 0: x < 0, side 1: x > 0)."""
        sx = 1 if side == 0 else -1
        v = (GRIP[0] * sx, GRIP[1], GRIP[2])
        for part, off in ((3, HAND), (2, FOREARM), (1, UPPER), (0, SHOULDER)):
            v = rzyx(v, self.arm_rot(part, side))
            v = add(v, (off[0] * sx, off[1], off[2]))
        v = rx(v, self.pitch[TORSO] * D)
        v = ry(v, self.yaw[TORSO] * D)
        return add(v, self.model_joint(TORSO))

    def mouth(self):
        v = rx(MOUTH, self.head_pitch * D)
        v = ry(v, self.head_yaw * D)
        return add(v, self.model_joint(0))


def still_center():
    """Centre of the spiral of the still pose (Minecraft model pixels): the cloud it rests on in the air."""
    p = Pose()
    p.solve(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    xs = [p.model_joint(k)[0] for k in range(8, N + 1)]
    zs = [p.model_joint(k)[2] for k in range(8, N + 1)]
    return sum(xs) / len(xs), sum(zs) / len(zs)


def overrides(p, bones):
    """Offsets and rotations of the bones of storm_model.build() for this pose."""
    import storm_model
    out = {}
    names = {b.name: b for b in bones}
    for k, name in enumerate(storm_model.ELEMENTS):
        out[id(names[name])] = (p.model_joint(p.bone_joint(k)), (p.pitch[k] * D, p.yaw[k] * D, 0.0))
    out[id(names['head'])] = (p.model_joint(0), (p.head_pitch * D, p.head_yaw * D, 0.0))

    def find(part, name):
        if part.name == name:
            return part
        for c in part.children:
            f = find(c, name)
            if f:
                return f
        return None
    torso = names['torso']
    for side, sname in ((0, 'left'), (1, 'right')):
        for part, n in enumerate(('arm_%s' % sname, 'arm_%s2' % sname, 'arm_%s3' % sname, 'hand_%s' % sname)):
            b = find(torso, n)
            out[id(b)] = (b.offset, p.arm_rot(part, side))
        hand = find(torso, 'hand_%s' % sname)
        for f in ('front', 'middle', 'back'):
            for j, suffix in enumerate(('', '2')):
                b = find(hand, 'finger_%s_%s%s' % (f, sname, suffix))
                c = p.curl * (-1 if side == 0 else 1)
                out[id(b)] = (b.offset, (0.0, 0.0, c))
    return out


def solve(w, phase=0.0, age=0.0, head_yaw=0.0, head_pitch=0.0, climb=0.0, bend=0.0, air=0.0):
    p = Pose()
    p.solve(*w, phase, age, head_yaw, head_pitch, climb, bend, air)
    return p
