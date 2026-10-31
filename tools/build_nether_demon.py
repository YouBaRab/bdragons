"""Builds tools/models/demon_nether_alpha.bbmodel: the alpha of the Nether, a winged demon on four legs.

Its torso and its digitigrade legs come from the Jersey Devil of the mod "Jersey Devil" (Andre's 5000W Bass-M, MCreator,
Academic Free License 3.0), drawn twice bigger; its head, its wings and its tails are those of the stage V nether dragon
of Bdragon (DragonModel, exported to tools/models/dragon_nether_stage5.json); the rest is ours:
  - it stands on four legs but raised, like a gorilla (the Gorilla of Alex's Mobs): the torso leans forward, the long
    arms reach the ground on their knuckles;
  - the head of the stage V nether dragon (its devil horns, its lower jaw larger and wider than the upper one) with two
    giant lower canines rising in front of the snout and curving back towards it, and two upper canines going down
    outside the lower jaw and curving inwards (they never cross);
  - the wings of the stage V dragon, in the dark colours of the demon, folded along its back at rest;
  - the three tails of the stage V nether dragon with their tridents, on a longer common base;
  - 7 nether warts and crimson and warped fungi on its back;
  - the dark colours of the stage V nether dragon (its texture, dragon/nether/body.png), with cracks of Minecraft lava
    (they glow).

The rest pose of the file is the gorilla stance; the game animates every group from it (DemonModel). The groups
shoulder_* and wing_flap_* carry the swing of the arms and the frame of the flight of the wings.
Needs the Jersey Devil model converted by Blockbench (path below). Run:  python tools/build_nether_demon.py <jd.bbmodel>
"""
import base64
import copy
import io
import json
import math
import os
import random
import sys
import uuid as uuidlib

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'models', 'demon_nether_alpha.bbmodel')
DRAGON = os.path.join(HERE, 'models', 'dragon_nether_stage5.json')
DRAGON_TEX = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'entity', 'dragon', 'nether')
K = 2.0                      # the Jersey Devil drawn twice bigger
F_HEAD = 0.9                 # the head of the stage V dragon, in pixels of the demon
F_WING = 1.3                 # its wings
F_TAIL = 1.0                 # its tails
TAIL_SEGMENTS = 12           # like the dragon: the scale of the pieces goes from 1.5 to 0.3
TAIL_BASE = 6                # the first pieces are the common base, then it splits into three
TAIL_THICK = 0.55            # the nether tails are thin
# the rest pose of the wings: folded along the back (see fold_wings)
WING_FOLD = {'out': 0.30, 'lean': 0.60, 'finger_out': 0.12, 'down': -0.30}
WING_ROOT_X = 8.0             # the wings leave the upper back this far from the spine
WING_ROOT_OUT = 5.0           # and this far out of the back (the bone of the wing is 8 pixels thick)
# the frame of the flight of the wings: level when the body lies in the flight (its rest tilt and the tilt of the flight)
WING_FRAME = 55.0 + 28.0
rng = random.Random(666)


def uid():
    return str(uuidlib.UUID(int=rng.getrandbits(128)))


# ------------------------------------------------------------------------------------------------------------
# Geometry helpers (Blockbench: y up, the front towards -z, rotations Euler ZYX in degrees around the origin)
# ------------------------------------------------------------------------------------------------------------

def rot(r):
    x, y, z = (math.radians(a) for a in (r or (0, 0, 0)))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def about(origin, r):
    m = np.eye(4)
    m[:3, :3] = rot(r)
    o = np.array(origin, float)
    m[:3, 3] = o - m[:3, :3] @ o
    return m


def euler_left(d):
    """Rotation (degrees) that turns -x into the direction d (Rz then Ry)."""
    d = np.array(d, float) / np.linalg.norm(d)
    b = math.degrees(math.asin(max(-1, min(1, d[2]))))
    a = math.degrees(math.atan2(-d[1], -d[0]))
    return [0.0, b, a]


def euler_right(d):
    """Rotation (degrees) that turns +x into the direction d."""
    d = np.array(d, float) / np.linalg.norm(d)
    b = math.degrees(math.asin(max(-1, min(1, -d[2]))))
    a = math.degrees(math.atan2(d[1], d[0]))
    return [0.0, b, a]


def euler_back(d):
    """Rotation (degrees) that turns +z into the direction d (Rx then Ry)."""
    d = np.array(d, float) / np.linalg.norm(d)
    p = math.degrees(math.asin(max(-1, min(1, -d[1]))))
    y = math.degrees(math.atan2(d[0], d[2]))
    return [p, y, 0.0]


def euler_of(r):
    """Euler angles (degrees, Blockbench order: R = Rz Ry Rx) of a rotation matrix."""
    ry = math.asin(max(-1.0, min(1.0, -r[2, 0])))
    rx = math.atan2(r[2, 1], r[2, 2])
    rz = math.atan2(r[1, 0], r[0, 0])
    return [math.degrees(rx), math.degrees(ry), math.degrees(rz)]


def frame(sx, bone, down):
    """Rotation that turns the wing (its bone along sx * x, its membrane hanging along -y) so that the bone points
    along `bone` and the membrane hangs along `down` (made perpendicular to the bone)."""
    t = np.array(bone, float)
    t /= np.linalg.norm(t)
    dn = np.array(down, float)
    dn -= t * (dn @ t)
    dn /= np.linalg.norm(dn)
    xa = t * sx
    ya = -dn
    za = np.cross(xa, ya)
    return np.column_stack([xa, ya, za])


C = np.diag([-1.0, -1.0, 1.0])     # Minecraft model space <-> Blockbench (x and y the other way)


def mc_matrix(pos, rot, scale):
    """The matrix of a Minecraft model part: translate, rotate Z then Y then X (radians), scale."""
    x, y, z = rot
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    m = np.eye(4)
    m[:3, :3] = rz @ ry @ rx @ np.diag(scale)
    m[:3, 3] = pos
    return m


def bake(b, g, part, anchor, f, pre=None, skip=(), own_rot=True, material='dragon'):
    """Puts the visible cubes of an exported part of the dragon (and of its visible children) into the group g: in
    Blockbench space, the origin of the frame `pre` of the part at `anchor`, `f` pixels of the demon for one of the dragon.
    Each cube keeps where it takes its texture in the dragon texture (painted again for its new size)."""
    if not part['visible']:
        return
    m = (pre if pre is not None else np.eye(4)) @ mc_matrix(part['pos'], part['rot'] if own_rot else (0, 0, 0),
                                                           part['scale'] if own_rot else (1, 1, 1))
    if not part.get('skip'):
        for c in part['cubes']:
            lo, hi = np.array(c['from']), np.array(c['to'])
            centre = m @ np.array([*(lo + hi) / 2, 1.0])
            lin = m[:3, :3]
            s = np.linalg.norm(lin, axis=0)
            r = lin / s
            r_bb = C @ r @ C
            size = (hi - lo) * s * f
            cb = np.array(anchor, float) + f * (C @ centre[:3])
            rot_bb = euler_of(r_bb)
            e = b.cube(g, material, cb - size / 2, cb + size / 2, material,
                       origin=list(cb), rotation=rot_bb if any(abs(v) > 1e-6 for v in rot_bb) else None)
            b.source[e['uuid']] = {'uv': c['uv'], 'size': c['size'], 'mirror': c['mirror']}
    for name, child in part['children'].items():
        if name in skip:
            continue
        bake(b, g, child, anchor, f, m, skip, own_rot, material)


class Builder:
    def __init__(self):
        self.groups = {}
        self.tree = {}
        self.parent = {}
        self.roots = []
        self.elements = []
        self.material = {}
        self.source = {}

    def group(self, name, origin, parent=None, rotation=(0, 0, 0)):
        u = uid()
        self.groups[u] = {'name': name, 'uuid': u, 'export': True, 'locked': False, 'scope': 0, 'selected': False,
                          'visibility': True, 'origin': [float(v) for v in origin], 'rotation': [float(v) for v in rotation],
                          'color': 0, 'children': [], 'reset': False, 'shade': True, 'mirror_uv': False, 'autouv': 0,
                          'isOpen': False}
        self.tree[u] = []
        self.parent[u] = parent
        (self.tree[parent] if parent else self.roots).append(u)
        return u

    def cube(self, g, name, frm, to, material, origin=None, rotation=None):
        """A cube; its size is rounded to whole pixels (box UV), keeping its centre."""
        c = [(a + b) / 2 for a, b in zip(frm, to)]
        s = [max(0, round(abs(b - a))) for a, b in zip(frm, to)]
        frm = [c[i] - s[i] / 2 for i in range(3)]
        to = [c[i] + s[i] / 2 for i in range(3)]
        u = uid()
        e = {'name': name, 'box_uv': True, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0,
             'allow_mirror_modeling': True, 'from': [round(v, 4) for v in frm], 'to': [round(v, 4) for v in to],
             'autouv': 0, 'color': 0, 'origin': [round(v, 4) for v in (origin or self.groups[g]['origin'])],
             'uv_offset': [0, 0], 'type': 'cube', 'uuid': u}
        if rotation and any(abs(a) > 1e-9 for a in rotation):
            e['rotation'] = [round(a, 4) for a in rotation]
        self.elements.append(e)
        self.tree[g].append(u)
        self.material[u] = material
        return e

    def world(self, g):
        """World matrix of a group (its rotations and those of its parents)."""
        chain = []
        while g:
            chain.append(g)
            g = self.parent[g]
        m = np.eye(4)
        for x in reversed(chain):
            gr = self.groups[x]
            m = m @ about(gr['origin'], gr['rotation'])
        return m

    def point(self, g, p):
        return (self.world(g) @ np.array([*p, 1.0]))[:3]

    def dir_local(self, g, d):
        """A world direction in the frame of group g (before its own rotation: in the frame of its parent)."""
        par = self.parent[g]
        r = self.world(par)[:3, :3] if par else np.eye(3)
        return r.T @ np.array(d, float)

    def shift(self, dy):
        for gr in self.groups.values():
            gr['origin'][1] += dy
        for e in self.elements:
            for k in ('from', 'to', 'origin'):
                e[k][1] = round(e[k][1] + dy, 4)


def fold_wings(b, wings, out, lean, finger_out, down):
    """The rest pose of the wings: folded along the back, like the dragons on the ground (WingFold): the arm goes back
    along the back, the forearm comes forward, the fingers go back again, the membranes hang down and outwards along the
    flanks. The directions are in the world (Blockbench: y up, the front towards -z) for the right wing (+x)."""
    def wing_frame(sx, bone, membrane):
        # the bone of the wing goes along sx * x of its part (the dragon: -x in its space), the membrane along +z
        t = np.array(bone, float)
        t /= np.linalg.norm(t)
        m = np.array(membrane, float)
        m -= t * (m @ t)
        m /= np.linalg.norm(m)
        xa = t * sx
        za = m
        ya = np.cross(za, xa)
        return np.column_stack([xa, ya, za])
    # the back of the demon goes down from the shoulders to the hips (its torso leans forward)
    back = np.array([0.0, down, 1.0])
    for sx, g_arm, g_fore, fingers in wings:
        def put(g, bone, membrane):
            rp = b.world(b.parent[g])[:3, :3]
            b.groups[g]['rotation'] = euler_of(rp.T @ wing_frame(sx, bone, membrane))
        put(g_arm, (sx * out, back[1], back[2]), (sx * lean, -1.0, 0.0))
        put(g_fore, (sx * 0.08, -back[1] * 0.9, -1.0), (sx * lean, -1.0, 0.0))
        for k, g in enumerate(fingers):
            put(g, (sx * (finger_out + 0.02 * k), back[1] - 0.06 * k, 1.0), (sx * (lean + 0.05 * k), -1.0, 0.0))


# ------------------------------------------------------------------------------------------------------------
# The Jersey Devil
# ------------------------------------------------------------------------------------------------------------

class JD:
    def __init__(self, path):
        bb = json.load(open(path, encoding='utf-8'))
        self.els = {e['uuid']: e for e in bb['elements']}
        self.grs = {g['uuid']: g for g in bb['groups']}
        self.nodes = {}

        def walk(n):
            if isinstance(n, str):
                return
            self.nodes[self.grs[n['uuid']]['name']] = n
            for c in n['children']:
                walk(c)
        for n in bb['outliner']:
            walk(n)

    def origin(self, name):
        return [v * K for v in self.grs[self.nodes[name]['uuid']]['origin']]

    def cubes(self, name):
        out = []
        for c in self.nodes[name]['children']:
            if isinstance(c, str):
                e = self.els[c]
                out.append(([v * K for v in e['from']], [v * K for v in e['to']],
                            [v * K for v in e.get('origin', [0, 0, 0])], e.get('rotation')))
        return out


def copy_cubes(b, g, jd, name, material, skip=()):
    for i, (f, t, o, r) in enumerate(jd.cubes(name)):
        if i in skip:
            continue
        m = material(i) if callable(material) else material
        b.cube(g, m, f, t, m, origin=o if r else None, rotation=r)


def build(jd):
    b = Builder()
    root = b.group('demon', (0, 0, 0))

    # --- legs (copied, digitigrade) -----------------------------------------------------------------------------
    legs = {}
    for side, (thigh, shin, foot, toes) in (('left', ('bone15', 'bone16', 'bone17', 'bone18')),
                                            ('right', ('bone35', 'bone36', 'bone37', 'bone38'))):
        g1 = b.group('thigh_' + side, jd.origin(thigh), root, (24, 0, 0))
        copy_cubes(b, g1, jd, thigh, 'skin')
        g2 = b.group('shin_' + side, jd.origin(shin), g1, (-30, 0, 0))
        copy_cubes(b, g2, jd, shin, 'skin')
        g3 = b.group('foot_' + side, jd.origin(foot), g2, (12, 0, 0))
        copy_cubes(b, g3, jd, foot, 'skin')
        g4 = b.group('toes_' + side, jd.origin(toes), g3, (-6, 0, 0))
        copy_cubes(b, g4, jd, toes, 'claw')
        legs[side] = g4

    # --- the torso, leaning forward like a gorilla -------------------------------------------------------------
    hip = (0.0, 56.0, 11.0)
    body = b.group('body', hip, root, (-55, 0, 0))
    copy_cubes(b, body, jd, 'bone2', 'skin')

    # --- neck (Jersey Devil) and head (the stage V nether dragon) ----------------------------------------------------
    dragon = json.load(open(DRAGON, encoding='utf-8'))
    neck = b.group('neck', jd.origin('bone3'), body, (34, 0, 0))
    copy_cubes(b, neck, jd, 'bone3', 'skin')
    joint = np.array(jd.origin('bone4'), float)
    head = b.group('head', joint, neck, (21, 0, 0))
    # the back of the skull of the dragon (6 pixels behind its pivot) on the end of the neck
    anchor = joint + np.array([0.0, 0.0, -6.0 * F_HEAD])

    def at(p):
        """A point of the head of the dragon (Minecraft space of its head) in the demon."""
        return anchor + F_HEAD * (C @ np.array(p, float))
    hp = dict(dragon['head'], pos=[0, 0, 0], rot=[0, 0, 0], scale=[1, 1, 1])
    bake(b, head, hp, anchor, F_HEAD, skip=('jaw',))
    jp = dragon['head']['children']['jaw']
    jaw = b.group('jaw', list(at(jp['pos'])), head)
    # its sabers are replaced by the giant canines of the demon
    bake(b, jaw, jp, anchor, F_HEAD, skip=('sabers', 'teeth'))
    # the jaw of the dragon (in its space): x +-6, y 0..4, z -16..0 from the joint, scaled by its underbite
    js = jp['scale']
    jaw_half, jaw_top, jaw_front = 6 * js[0], jp['pos'][1], jp['pos'][2] - 16 * js[2]
    snout_front = -24 * dragon['head']['children']['snout']['scale'][2]
    # lower canines: straight up in front of the snout, curving back towards it
    # (sx: the side in Blockbench, x < 0 is the left of the demon; the space of the dragon has x the other way)
    for sx, n in ((-1, 'left'), (1, 'right')):
        base = at((-sx * 3.2, jaw_top, jaw_front + 2.2))
        x, y, z = base
        f1 = b.group('fang_low_%s' % n, (x, y, z), jaw)
        b.cube(f1, 'fang', (x - 1.5, y - 1.0, z - 1.5), (x + 1.5, y + 8.0, z + 1.5), 'fang')
        f2 = b.group('fang_low_%s2' % n, (x, y + 8.0, z), f1, (28, 0, 0))
        b.cube(f2, 'fang', (x - 1.0, y + 8.0, z - 1.0), (x + 1.0, y + 13.0, z + 1.0), 'fang')
        f3 = b.group('fang_low_%s3' % n, (x, y + 13.0, z), f2, (32, 0, 0))
        b.cube(f3, 'fang', (x - 0.5, y + 13.0, z - 0.5), (x + 0.5, y + 16.5, z + 0.5), 'fang_tip')
    # the cheeks that carry the upper canines, outside the lower jaw
    for sx in (-1, 1):
        c0, c1 = at((-sx * 4.6, -1.0, -16.0)), at((-sx * 11.2, 2.2, -11.0))
        b.cube(head, 'skin', np.minimum(c0, c1), np.maximum(c0, c1), 'skin')
    # upper canines: down and outwards outside the lower jaw, then curving inwards
    for sx, n in ((-1, 'left'), (1, 'right')):
        x, y, z = at((-sx * 9.6, 1.6, -13.5))
        u1 = b.group('fang_up_%s' % n, (x, y, z), head, (0, 0, sx * 14))
        b.cube(u1, 'fang', (x - 1.5, y - 9.0, z - 1.5), (x + 1.5, y + 1.0, z + 1.5), 'fang')
        u2 = b.group('fang_up_%s2' % n, (x, y - 9.0, z), u1, (0, 0, -sx * 14))
        b.cube(u2, 'fang', (x - 1.0, y - 14.0, z - 1.0), (x + 1.0, y - 9.0, z + 1.0), 'fang')
        u3 = b.group('fang_up_%s3' % n, (x, y - 14.0, z), u2, (0, 0, -sx * 34))
        b.cube(u3, 'fang', (x - 0.5, y - 17.5, z - 0.5), (x + 0.5, y - 14.0, z + 0.5), 'fang_tip')

    # --- the wings of the stage V dragon, from the upper back ---------------------------------------------------------
    # the frame of the flight (wing_flap_*) is level when the body lies in the flight; at rest the wings are folded
    wings = []
    root_y, root_z = jd.origin('bone9')[1], jd.origin('bone9')[2]
    for side, sx, src in (('left', -1, 'left_wing_arm'), ('right', 1, 'right_wing_arm')):
        arm_part = dragon[src]
        shoulder = np.array([sx * WING_ROOT_X, root_y, root_z + WING_ROOT_OUT])
        flap = b.group('wing_flap_' + side, shoulder, body, (WING_FRAME, 0, 0))
        g_arm = b.group('wing_arm_' + side, shoulder, flap)
        # each part of the wing has its pivot on the joint, no rotation (the fold is set below)

        def part_cubes(g, part, origin):
            p = dict(part, pos=[0, 0, 0], rot=[0, 0, 0], scale=[1, 1, 1], children={})
            bake(b, g, p, origin, F_WING, material='wing')
        part_cubes(g_arm, arm_part, shoulder)
        fore_part = arm_part['children'][src.replace('arm', 'forearm')]
        elbow = shoulder + F_WING * (C @ np.array(fore_part['pos'], float))
        g_fore = b.group('wing_forearm_' + side, elbow, g_arm)
        part_cubes(g_fore, fore_part, elbow)
        fingers = []
        for k in range(1, 5):
            fp = fore_part['children'][src.replace('arm', 'finger_%d' % k)]
            wrist = elbow + F_WING * (C @ np.array(fp['pos'], float))
            g_f = b.group('wing_finger_%s%d' % (side, k), wrist, g_fore)
            part_cubes(g_f, fp, wrist)
            fingers.append(g_f)
        wings.append((sx, g_arm, g_fore, fingers))

    # --- arms: long like the arms of a gorilla, the knuckles on the ground --------------------------------------------
    shoulder_y, shoulder_z = 80.3, 9.1
    arms = {}
    for side, sx in (('left', -1), ('right', 1)):
        swing = b.group('shoulder_' + side, (sx * 10.1, shoulder_y, shoulder_z), body)
        a = b.group('arm_' + side, (sx * 10.1, shoulder_y, shoulder_z), swing)
        arms[side] = a

    # --- the tails of the stage V nether dragon: a longer common base, then three tails with their tridents -------
    seg = dragon['tail_0']
    trident = dragon['tail_tip']['children']['trident']
    tail_base = (0.0, 58.0, 16.0)

    def tail_piece(g, i, centre):
        """Piece i of a tail of the dragon (its cube and its scale on top), its centre at `centre`, along +z."""
        sc = 1.5 + (0.3 - 1.5) * (i + 1) / TAIL_SEGMENTS
        part = dict(seg, pos=[0, 0, 0], rot=[0, math.pi, 0], scale=[sc * TAIL_THICK, sc * TAIL_THICK, sc])
        part['children'] = {'middle_scale': seg['children']['middle_scale']}
        bake(b, g, part, centre, F_TAIL)

    def chain(name, parent, start, first, count):
        groups = []
        z = start[2]
        g = parent
        for k in range(count):
            i = first + k
            sc = 1.5 + (0.3 - 1.5) * (i + 1) / TAIL_SEGMENTS
            g = b.group('%s%d' % (name, i + 1), (start[0], start[1], z), g)
            groups.append(g)
            step = (10 * sc - 0.7) * F_TAIL
            tail_piece(g, i, (start[0], start[1], z + step / 2))
            z += step
        return groups, (start[0], start[1], z)
    base_groups, fork = chain('tail_base', body, tail_base, 0, TAIL_BASE)
    tails = []
    for name, splay in (('tail_a', 0.0), ('tail_b', 0.55), ('tail_c', -0.55)):
        groups, end = chain(name, base_groups[-1], fork, TAIL_BASE, TAIL_SEGMENTS - TAIL_BASE)
        # the trident at the end, pointing on along the tail (its -z in the space of the dragon: +z here)
        tp = dict(trident, pos=[0, 0, 0], rot=[0, math.pi, 0], scale=[1.7, 1.7, 1.7])
        bake(b, groups[-1], tp, end, F_TAIL, material='dragon')
        tails.append((groups, splay))

    # --- 7 warts and fungi of the Nether on its back -------------------------------------------------------------
    warts = b.group('warts', hip, body)
    kinds = ['wart', 'crimson', 'warped', 'wart', 'crimson', 'warped', 'wart']
    rng.shuffle(kinds)
    spots = []
    while len(spots) < 7:
        # in the middle of the back: the folded wings lie on its sides
        x, y = rng.uniform(-3.5, 3.5), rng.uniform(57.0, 84.0)
        if any((x - a) ** 2 + (y - c) ** 2 < 25 for a, c in spots):
            continue
        spots.append((x, y))
    for i, ((x, y), kind) in enumerate(zip(spots, kinds)):
        z0 = 16.8
        tilt = (rng.uniform(-12, 12), 0, rng.uniform(-14, 14))
        w = b.group('wart%d' % i, (x, y, z0), warts, (90 + tilt[0], 0, tilt[2]))
        # built upright (+y), turned so that it grows out of the back (+z)
        if kind == 'wart':
            b.cube(w, 'wart', (x - 2.5, y, z0 - 2.5), (x + 2.5, y + 3, z0 + 2.5), 'wart')
            b.cube(w, 'wart', (x - 1.0, y + 3, z0 - 1.5), (x + 2.0, y + 5, z0 + 1.5), 'wart')
            b.cube(w, 'wart', (x - 2.5, y + 2, z0 + 0.5), (x - 0.5, y + 4, z0 + 2.5), 'wart')
        else:
            b.cube(w, kind + '_stem', (x - 1.0, y, z0 - 1.0), (x + 1.0, y + 4, z0 + 1.0), kind + '_stem')
            b.cube(w, kind + '_cap', (x - 3.0, y + 4, z0 - 3.0), (x + 3.0, y + 6, z0 + 3.0), kind + '_cap')
            b.cube(w, kind + '_cap', (x - 1.5, y + 6, z0 - 1.5), (x + 1.5, y + 7, z0 + 1.5), kind + '_cap')

    # ---------------------------------------------------------------------------------------------------------
    # The stance: the tails go back and down, the feet on the ground, the long arms down to the ground
    # ---------------------------------------------------------------------------------------------------------
    # the wings folded along the back (see fold_wings)
    fold_wings(b, wings, **WING_FOLD)
    # the tails: the base leaves the back going down and sags, the three tails part and lift their tridents a little
    # (each piece is turned so that it follows this curve in the world)
    n = len(base_groups)
    for i, g in enumerate(base_groups):
        t = i / (TAIL_SEGMENTS - 1)
        b.groups[g]['rotation'] = euler_back(b.dir_local(g, (0.0, -0.75 + 0.6 * t, 1.0)))
    for groups, splay in tails:
        for k, g in enumerate(groups):
            t = (TAIL_BASE + k) / (TAIL_SEGMENTS - 1)
            u = (k + 1) / len(groups)
            b.groups[g]['rotation'] = euler_back(b.dir_local(g, (splay * (0.4 + 0.8 * u), -0.75 + 0.95 * t, 1.0)))

    # the lowest point of the feet
    els = {e['uuid']: e for e in b.elements}

    def lowest(g):
        m = b.world(g)
        lo = 1e9
        for u in b.tree[g]:
            if u in els:
                e = els[u]
                me = m @ about(e['origin'], e.get('rotation'))
                for xx in (e['from'][0], e['to'][0]):
                    for yy in (e['from'][1], e['to'][1]):
                        for zz in (e['from'][2], e['to'][2]):
                            lo = min(lo, (me @ np.array([xx, yy, zz, 1.0]))[1])
        return lo
    ground = min(lowest(g) for g in legs.values())

    for side, sx in (('left', -1), ('right', 1)):
        a = arms[side]
        s = b.point(a, b.groups[a]['origin'])
        target = np.array([s[0] + sx * 3.0, ground + 5.0, s[2] - 9.0])
        d = target - s
        length = float(np.linalg.norm(d))
        local = b.dir_local(a, d)
        b.groups[a]['rotation'] = euler_left(local) if sx < 0 else euler_right(local)
        ox = b.groups[a]['origin'][0]
        # the arm is built along x from the shoulder: shoulder, upper arm, forearm, hand and four talons
        upper = round((length - 3.0) * 0.5)
        fore = round(length - 3.0 - upper)
        x0 = ox
        b.cube(a, 'shoulder', (x0 + sx * 0.5, shoulder_y - 4, shoulder_z - 4), (x0 + sx * 7.5, shoulder_y + 4, shoulder_z + 4), 'skin')
        b.cube(a, 'upper_arm', (x0 + sx * 3, shoulder_y - 3, shoulder_z - 3), (x0 + sx * (3 + upper), shoulder_y + 3, shoulder_z + 3), 'skin')
        f = b.group('forearm_' + side, (x0 + sx * (3 + upper), shoulder_y, shoulder_z), a)
        b.cube(f, 'forearm', (x0 + sx * (2 + upper), shoulder_y - 2.5, shoulder_z - 2.5),
               (x0 + sx * (3 + upper + fore), shoulder_y + 2.5, shoulder_z + 2.5), 'skin')
        hx = x0 + sx * (3 + upper + fore)
        h = b.group('hand_' + side, (hx, shoulder_y, shoulder_z), f)
        b.cube(h, 'hand', (hx - sx * 1, shoulder_y - 1.5, shoulder_z - 3.5), (hx + sx * 5, shoulder_y + 1.5, shoulder_z + 3.5), 'skin')
        for k, dz in enumerate((-2.6, -0.9, 0.9, 2.6)):
            t = b.group('talon_%s%d' % (side, k), (hx + sx * 5, shoulder_y, shoulder_z + dz), h, (0, 0, -sx * 35))
            b.cube(t, 'talon', (hx + sx * 5 - 0.6, shoulder_y - 0.6, shoulder_z + dz - 0.6),
                   (hx + sx * 9.5 + 0.0, shoulder_y + 0.6, shoulder_z + dz + 0.6), 'claw')
        # the hand points forward and a little down (the knuckles on the ground)
        want = np.array([0.0, -0.35, -1.0])
        lh = b.dir_local(h, want)
        b.groups[h]['rotation'] = euler_left(lh) if sx < 0 else euler_right(lh)

    # everything stands on the ground
    b.shift(-ground)
    return b


# ------------------------------------------------------------------------------------------------------------
# Texture (box UV, 1 texel per pixel)
# ------------------------------------------------------------------------------------------------------------

def hexc(h):
    return np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)], float)


RED = [hexc(c) for c in ('#4d1b14', '#6b2519', '#892e24', '#a7372b', '#c54033')]
LAVA = [hexc(c) for c in ('#b8320a', '#e35a0e', '#f6891a', '#fcb33a', '#ffdd6a')]
HORN = [hexc(c) for c in ('#161718', '#1e1c1b', '#282826', '#3a3836', '#4a4846')]
MEMBRANE = [hexc(c) for c in ('#141110', '#1e1a19', '#2a2423', '#3a2a27')]
CLAW = [hexc(c) for c in ('#101011', '#1a1a1b', '#2c2c2e')]
FANG = [hexc(c) for c in ('#8f7f63', '#c4b392', '#e2d7bd', '#f4ecd8')]
EYE = hexc('#f8dd72')
WART = [hexc(c) for c in ('#5a0a0a', '#73100f', '#8f1b17', '#a8261e')]
CRIMSON_CAP = [hexc(c) for c in ('#7a0f12', '#9c1714', '#b72b1c')]
CRIMSON_STEM = [hexc(c) for c in ('#5c2b23', '#8a3d2e', '#c56a43')]
WARPED_CAP = [hexc(c) for c in ('#0e5a55', '#12756c', '#16928a')]
WARPED_STEM = [hexc(c) for c in ('#1e4a4a', '#2a6a64', '#3b8a80')]
METAL = [hexc(c) for c in ('#1c1c1e', '#2e2b2a', '#4a4441')]


def face_rects(u, v, w, h, d):
    def R(a, b_, c, e):
        x0, y0 = int(math.floor(a)), int(math.floor(b_))
        return (x0, y0, max(1, int(math.ceil(c)) - x0), max(1, int(math.ceil(e)) - y0))
    return {'up': R(u + d, v, u + d + w, v + d), 'down': R(u + d + w, v, u + d + 2 * w, v + d),
            'east': R(u, v + d, u + d, v + d + h), 'north': R(u + d, v + d, u + d + w, v + d + h),
            'west': R(u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': R(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def dims(e):
    return [e['to'][i] - e['from'][i] for i in range(3)]


def pack(elements, tw):
    items = []
    for e in elements:
        w, h, d = dims(e)
        items.append((e, int(math.ceil(2 * (d + w))) + 1, int(math.ceil(d + h)) + 1))
    items.sort(key=lambda it: (-it[2], -it[1]))
    x = y = row = 0
    for e, W, H in items:
        if x + W > tw:
            x, y, row = 0, y + row, 0
        e['uv_offset'] = [x, y]
        x += W
        row = max(row, H)
    return y + row


DARK = [hexc(c) for c in ('#251313', '#301918', '#3c201f', '#482625')]


def paint(b):
    tw = 1024
    used = pack(b.elements, tw)
    dtex = np.array(Image.open(os.path.join(DRAGON_TEX, 'body.png')).convert('RGBA')).astype(float)
    dglow = np.array(Image.open(os.path.join(DRAGON_TEX, 'glow.png')).convert('RGBA')).astype(float)
    dh, dw = dtex.shape[:2]
    th = 64
    while th < used:
        th *= 2
    img = np.zeros((th, tw, 4), float)
    glow = np.zeros((th, tw, 4), float)
    nr = np.random.default_rng(13)

    def put(x, y, c, a=255):
        if 0 <= x < tw and 0 <= y < th:
            img[y, x, :3] = np.clip(c, 0, 255)
            img[y, x, 3] = a

    def lit(x, y, c):
        put(x, y, c)
        if 0 <= x < tw and 0 <= y < th:
            glow[y, x, :3] = np.clip(c, 0, 255)
            glow[y, x, 3] = 255

    def ramp(pal, t):
        t = min(0.999, max(0.0, t)) * (len(pal) - 1)
        i = int(t)
        f = t - i
        return pal[i] * (1 - f) + pal[min(i + 1, len(pal) - 1)] * f

    def fill(R, pal, lo=0.2, hi=0.8, speck=0.0, speck_pal=None):
        x0, y0, w, h = R
        for yy in range(h):
            for xx in range(w):
                t = lo + (hi - lo) * nr.random()
                if (xx + yy) % 3 == 0:
                    t *= 0.9
                c = ramp(pal, t)
                if speck and nr.random() < speck:
                    c = ramp(speck_pal, nr.random())
                put(x0 + xx, y0 + yy, c)

    def cracks(R, n):
        """Lava cracks: short glowing random walks."""
        x0, y0, w, h = R
        if w < 3 or h < 3:
            return
        for _ in range(n):
            x, y = nr.integers(0, w), nr.integers(0, h)
            for _ in range(int(nr.integers(4, 4 + (w + h) // 2))):
                lit(x0 + x, y0 + y, ramp(LAVA, 0.5 + 0.5 * nr.random()))
                x = int(min(w - 1, max(0, x + nr.integers(-1, 2))))
                y = int(min(h - 1, max(0, y + nr.integers(-1, 2))))

    def lava_patch(R):
        """A patch of the lava of Minecraft: orange with yellow swirls (it glows)."""
        x0, y0, w, h = R
        if w < 4 or h < 4:
            return
        pw, ph = max(3, w // 2), max(3, h // 2)
        px, py = int(nr.integers(0, w - pw + 1)), int(nr.integers(0, h - ph + 1))
        cx, cy = px + pw / 2, py + ph / 2
        for yy in range(py, py + ph):
            for xx in range(px, px + pw):
                r = ((xx - cx) / (pw / 2)) ** 2 + ((yy - cy) / (ph / 2)) ** 2
                if r > 1.0 + 0.3 * nr.random():
                    continue
                t = 0.35 + 0.4 * math.sin(xx * 1.3 + yy * 0.7) ** 2 + 0.25 * nr.random()
                lit(x0 + xx, y0 + yy, ramp(LAVA, t))

    def resample(e, rects, src):
        """The faces of a cube of the dragon, painted again for the size of this cube (nearest texel)."""
        srects = face_rects(src['uv'][0], src['uv'][1], *src['size'])
        for f, (x0, y0, ww, hh) in rects.items():
            sf = f
            if src['mirror'] and f in ('east', 'west'):
                sf = 'west' if f == 'east' else 'east'
            sx0, sy0, sw, sh = srects[sf]
            for yy in range(hh):
                for xx in range(ww):
                    fx = (xx + 0.5) / ww
                    if src['mirror']:
                        fx = 1 - fx
                    u = int(min(dw - 1, max(0, sx0 + fx * sw)))
                    v = int(min(dh - 1, max(0, sy0 + (yy + 0.5) / hh * sh)))
                    c = dtex[v, u]
                    if c[3] == 0:
                        continue
                    put(x0 + xx, y0 + yy, c[:3], c[3])
                    if dglow[v, u, 3] > 0 and 0 <= x0 + xx < tw and 0 <= y0 + yy < th:
                        glow[y0 + yy, x0 + xx] = dglow[v, u]

    def scales(R):
        """The dark scales of the stage V nether dragon (the flank of its body), from a random place."""
        x0, y0, ww, hh = R
        ox, oy = int(nr.integers(0, 64)), int(nr.integers(0, 24))
        for yy in range(hh):
            for xx in range(ww):
                c = dtex[64 + (yy + oy) % 24, (xx + ox) % 64]
                put(x0 + xx, y0 + yy, c[:3])

    for e in b.elements:
        w, h, d = dims(e)
        rects = face_rects(e['uv_offset'][0], e['uv_offset'][1], w, h, d)
        m = b.material[e['uuid']]
        if m == 'dragon':
            resample(e, rects, b.source[e['uuid']])
            continue
        for f, R in rects.items():
            if m == 'wing':
                if min(w, h, d) == 0:
                    fill(R, MEMBRANE, 0.1, 0.7)
                    x0, y0, ww, hh = R
                    for k in range(max(2, ww // 9)):
                        x = int(nr.integers(0, max(1, ww)))
                        for yy in range(hh):
                            x = int(min(ww - 1, max(0, x + nr.integers(-1, 2))))
                            put(x0 + x, y0 + yy, ramp(DARK, 0.3 + 0.4 * nr.random()))
                else:
                    fill(R, DARK, 0.0, 0.8)
            elif m in ('skin', 'jaw'):
                scales(R)
                # thin cracks of lava (they glow)
                cracks(R, max(1, (R[2] * R[3]) // 140))
            elif m == 'horn':
                x0, y0, ww, hh = R
                for yy in range(hh):
                    for xx in range(ww):
                        put(x0 + xx, y0 + yy, ramp(HORN, (0.3 if yy % 2 else 0.65) + 0.2 * nr.random()))
            elif m == 'membrane':
                fill(R, MEMBRANE, 0.1, 0.7)
                x0, y0, ww, hh = R
                for k in range(max(2, ww // 9)):
                    x = int(nr.integers(0, max(1, ww)))
                    for yy in range(hh):
                        x = int(min(ww - 1, max(0, x + nr.integers(-1, 2))))
                        put(x0 + x, y0 + yy, ramp(RED, 0.2 + 0.3 * nr.random()))
            elif m == 'wingbone':
                fill(R, RED, 0.05, 0.45)
            elif m == 'claw':
                fill(R, CLAW, 0.0, 1.0)
            elif m == 'metal':
                fill(R, METAL, 0.0, 1.0)
            elif m in ('fang', 'fang_tip'):
                x0, y0, ww, hh = R
                for yy in range(hh):
                    for xx in range(ww):
                        put(x0 + xx, y0 + yy, ramp(FANG, 0.45 + 0.5 * nr.random() if m == 'fang' else 0.75 + 0.25 * nr.random()))
            elif m == 'eye':
                x0, y0, ww, hh = R
                for yy in range(hh):
                    for xx in range(ww):
                        lit(x0 + xx, y0 + yy, EYE * (0.85 + 0.15 * nr.random()))
            elif m == 'wart':
                fill(R, WART, 0.0, 1.0, speck=0.12, speck_pal=[hexc('#c0392b'), hexc('#e05a3a')])
            elif m == 'crimson_cap':
                fill(R, CRIMSON_CAP, 0.0, 1.0, speck=0.14, speck_pal=[hexc('#f2c230'), hexc('#ffd84a')])
            elif m == 'crimson_stem':
                fill(R, CRIMSON_STEM, 0.0, 1.0)
            elif m == 'warped_cap':
                fill(R, WARPED_CAP, 0.0, 1.0, speck=0.14, speck_pal=[hexc('#f08a24'), hexc('#ffad3a')])
            elif m == 'warped_stem':
                fill(R, WARPED_STEM, 0.0, 1.0)
    return img, glow, tw, th


def main():
    jd_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'models', 'jersey_devil_ref.bbmodel')
    jd = JD(jd_path)
    b = build(jd)
    img, glow, tw, th = paint(b)
    tex = Image.fromarray(img.astype(np.uint8), 'RGBA')
    buf = io.BytesIO()
    tex.save(buf, 'PNG')
    for e in b.elements:
        w, h, d = dims(e)
        u, v = e['uv_offset']
        e['faces'] = {f: {'uv': [r[0], r[1], r[0] + r[2], r[1] + r[3]], 'texture': 0}
                      for f, r in face_rects(u, v, w, h, d).items()}

    def outline(u):
        return {'uuid': u, 'isOpen': False, 'children': [c if c not in b.groups else outline(c) for c in b.tree[u]]}

    model = {
        'meta': {'format_version': '5.0', 'model_format': 'modded_entity', 'box_uv': True},
        'name': 'demon_nether_alpha', 'model_identifier': 'demon_nether_alpha',
        'modded_entity_entity_class': '', 'modded_entity_version': '1.17', 'modded_entity_flip_y': True,
        'visible_box': [1, 1, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'timeline_setups': [], 'unhandled_root_fields': {},
        'resolution': {'width': tw, 'height': th},
        'elements': b.elements, 'groups': list(b.groups.values()), 'outliner': [outline(u) for u in b.roots],
        'textures': [{'path': '', 'name': 'demon_nether', 'folder': '', 'namespace': '', 'id': '0', 'width': tw,
                      'height': th, 'uv_width': tw, 'uv_height': th, 'particle': False, 'use_as_default': False,
                      'layers_enabled': False, 'render_mode': 'default', 'render_sides': 'auto', 'wrap_mode': 'limited',
                      'pbr_channel': 'color', 'visible': True, 'internal': True, 'saved': False, 'uuid': uid(),
                      'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}],
    }
    json.dump(model, open(OUT, 'w', encoding='utf-8'))
    Image.fromarray(glow.astype(np.uint8), 'RGBA').save(os.path.join(HERE, 'models', 'demon_nether_alpha_glow.png'))
    print('%d cubes, %d groups, texture %dx%d' % (len(b.elements), len(b.groups), tw, th))


if __name__ == '__main__':
    main()
