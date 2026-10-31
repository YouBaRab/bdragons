"""Builds the Blockbench file of the Sun Steed (alpha of the sunlight), from the sketch of the author
(E:/PROJET_MINECRAFT/alpha/dragon_solaire/croquis_alpha_dragon_soleil.png):

- body and head of a horse, golden with a yellow / orange finish, blue eyes;
- feathered wings (long feathers with orange streaks and yellow tips);
- a dragon / crocodile tail with spikes on top, 4 blocks long in game;
- an angel halo above the head, and a miniature sun (a cube of 4 x 4 x 4 blocks in game) above and behind the head;
- about 12 blocks high in game (to the top of the raised wings).

The model is drawn 4 times smaller than in game (1 block in game = 4 pixels here, MODEL_SCALE = 4), like the other
alphas. The texture is painted here (box UV, 1 texel per pixel). The animations of the file:
- idle: breathing, the tail sways;
- sun_spin: the sun turns very slowly on x and y (always played);
- sun_afk: after 1 min of AFK: the sun spins faster and faster for 10 s, shrinks from the 3rd second, turns to
  stardust at the end, then appears again at once and grows back to its size (the game plays it every minute of AFK);
- walk: the walk of a horse;
- fly: the flight of a pegasus (big wing beats, the legs tucked);
- ult: the power: it rears up on its hind legs, the front legs pawing the air, and throws its sun where it aims;
- sun_rebuild: the sun grows back above its head.

Run from the project folder:
    python tools/build_sun_alpha.py
It writes tools/models/dragon_soleil_alpha.bbmodel.
"""
import base64
import io
import json
import math
import os
import random
import uuid

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'models', 'dragon_soleil_alpha.bbmodel')
TEX_W, TEX_H = 512, 256
RND = random.Random(7)


def uid():
    return str(uuid.UUID(int=RND.getrandbits(128)))


# ---------------------------------------------------------------------------------------------------------------- parts

class Group:
    def __init__(self, name, origin, rotation=(0, 0, 0)):
        self.name, self.origin, self.rotation = name, list(origin), list(rotation)
        self.children = []
        self.uuid = uid()

    def add(self, child):
        self.children.append(child)
        return child


class Cube:
    def __init__(self, name, frm, to, mat, rotation=None, origin=None, uvkey=None, mirror=False):
        self.name, self.frm, self.to, self.mat = name, list(frm), list(to), mat
        self.rotation = list(rotation) if rotation else None
        self.origin = list(origin) if origin else None
        self.uvkey, self.mirror = uvkey, mirror
        self.uuid = uid()
        size = [to[i] - frm[i] for i in range(3)]
        assert all(abs(s - round(s)) < 1e-6 and s > 0 for s in size), (name, size)
        self.size = [int(round(s)) for s in size]


def mirror_x(node, suffix_from='_l', suffix_to='_r'):
    """The mirror of a group (x -> -x): same texture (mirrored), rotations y and z negated."""
    if isinstance(node, Cube):
        c = Cube(node.name.replace(suffix_from, suffix_to), [-node.to[0], node.frm[1], node.frm[2]],
                 [-node.frm[0], node.to[1], node.to[2]], node.mat,
                 [node.rotation[0], -node.rotation[1], -node.rotation[2]] if node.rotation else None,
                 [-node.origin[0], node.origin[1], node.origin[2]] if node.origin else None,
                 node.uvkey or node.uuid, not node.mirror)
        return c
    g = Group(node.name.replace(suffix_from, suffix_to), [-node.origin[0], node.origin[1], node.origin[2]],
              [node.rotation[0], -node.rotation[1], -node.rotation[2]])
    for ch in node.children:
        g.add(mirror_x(ch, suffix_from, suffix_to))
    return g


# ---------------------------------------------------------------------------------------------------------------- model
# Blockbench space: x to the left of the horse, y up, z backwards (the head looks to -z); hooves on y = 0.

def build():
    body = Group('body', (0, 24, 12))           # pivot on the hips (it rears up around them)
    body.add(Cube('torso', (-6, 18, -15), (6, 30, 15), 'gold'))
    body.add(Cube('chest', (-5, 17, -17), (5, 29, -15), 'gold'))
    body.add(Cube('croup', (-5, 29, 3), (5, 31, 14), 'gold'))
    body.add(Cube('withers', (-4, 30, -12), (4, 31, -4), 'gold'))
    body.add(Cube('belly', (-5, 17, -12), (5, 18, 12), 'gold_belly'))

    # legs: thigh > shin (+ hoof)
    def leg(name, x, z, top, thick):
        h = thick / 2
        g = Group(name, (x, top, z))
        g.add(Cube(name, (x - h, 10, z - h), (x + h, min(top, 22), z + h), 'gold'))
        s = g.add(Group(name.replace('leg', 'shin'), (x, 10, z)))
        s.add(Cube('shin', (x - 2, 3, z - 2), (x + 2, 10, z + 2), 'gold'))
        s.add(Cube('hoof', (x - 2.5, 0, z - 2.5), (x + 2.5, 3, z + 2.5), 'hoof'))
        return g
    fl = body.add(leg('leg_fl', 3.5, -11.5, 22, 5))
    body.add(mirror_x(fl, '_fl', '_fr'))
    bl = body.add(leg('leg_bl', 3.5, 11, 24, 5))
    body.add(mirror_x(bl, '_bl', '_br'))

    # neck > head (> ears, eyes, halo), mane; built upright then leaned forward like the horse of the game
    neck = body.add(Group('neck', (0, 28, -12), (-30, 0, 0)))
    neck.add(Cube('neck', (-2.5, 27, -15), (2.5, 42, -8), 'gold'))
    mane = neck.add(Group('mane', (0, 28, -8)))
    mane.add(Cube('mane', (-1, 28, -8), (1, 44, -6), 'mane'))
    for i, y in enumerate((30, 33, 36, 39, 42)):
        mane.add(Cube('mane_lock', (-0.5, y, -7), (0.5, y + 4, -5), 'mane', (40, 0, 0), (0, y, -7)))
    head = neck.add(Group('head', (0, 41, -11.5)))
    head.add(Cube('head', (-3, 38, -17), (3, 44, -9), 'gold'))
    head.add(Cube('muzzle', (-2.5, 38, -23), (2.5, 42, -17), 'muzzle'))
    head.add(Cube('forelock', (-1.5, 43, -15), (1.5, 45, -11), 'mane'))
    ear = Cube('ear_l', (1, 44, -11), (3, 47, -10), 'gold', (0, 0, -12), (2, 44, -10.5))
    head.add(ear)
    head.add(mirror_x(ear))
    eye = Cube('eye_l', (2.5, 40.5, -15.5), (3.5, 41.5, -13.5), 'eye')
    head.add(eye)
    head.add(mirror_x(eye))
    # the halo: 12 small bars on a circle of radius 5, kept level (the neck leans by 30 degrees)
    halo = head.add(Group('halo', (0, 49.5, -13), (30, 0, 0)))
    first = None
    for k in range(12):
        c = Cube('halo', (-1.5, 49, -18.5), (1.5, 50, -17.5), 'halo', (0, k * 30, 0), (0, 49.5, -13),
                 first.uuid if first else None)
        first = first or c
        halo.add(c)

    # the miniature sun, above and behind the head (16 pixels = 4 blocks in game), tilted on a corner (placed by the
    # author in Blockbench); the group turns around its centre
    centre = (1.21752, 65, -8.58671)
    sun = body.add(Group('sun', centre))
    sun.add(Cube('sun', (centre[0] - 8, 57, centre[2] - 8), (centre[0] + 8, 73, centre[2] + 8), 'sun',
                 (-45, -32.5, 27.5), centre))

    # the wings: arm > forearm > hand, with the feathers; spread along +x, then raised and swept back
    wing = body.add(Group('wing_l', (5, 30, -6), (0, -25, 45)))
    wing.add(Cube('arm', (5, 29, -7.5), (15, 31, -4.5), 'gold'))
    fore = wing.add(Group('forearm_l', (15, 30, -6)))
    fore.add(Cube('forearm', (15, 29, -7), (27, 31, -5), 'gold'))
    hand = fore.add(Group('hand_l', (27, 30, -6)))
    hand.add(Cube('hand', (27, 29.5, -6.5), (32, 30.5, -5.5), 'gold'))

    def feather(parent, x, length, angle, y, key):
        """A feather from (x, y, -6) backwards (+z), turned outwards by angle around y: a shaft and a yellow tip."""
        f = Group('feather', (x, y, -6), (0, angle, 0))
        f.add(Cube('feather', (x - 1.5, y - 0.5, -6), (x + 1.5, y + 0.5, -6 + length - 3), 'feather'))
        f.add(Cube('feather_tip', (x - 1.5, y - 0.5, -6 + length - 3), (x + 1.5, y + 0.5, -6 + length), 'feather_tip'))
        parent.add(f)
    for i, (x, length) in enumerate(((7, 12), (10, 13), (13, 15))):
        feather(wing, x, length, 0, 29.6 + 0.4 * (i % 2), 'sec')
    for i, (x, length) in enumerate(((16.5, 16), (19.5, 17), (22.5, 19), (25.5, 20))):
        feather(fore, x, length, 2 + 2 * i, 29.6 + 0.4 * (i % 2), 'sec')
    for i, (x, length, angle) in enumerate(((28, 22, 12), (29, 24, 26), (30, 26, 40), (31, 27, 55), (32, 28, 70))):
        feather(hand, x, length, angle, 29.4 + 0.3 * i, 'prim')
    body.add(mirror_x(wing))

    # the dragon / crocodile tail: 4 segments (16 pixels = 4 blocks in game), spikes on top
    parent, z0 = body, 15
    for i, (length, w, rot) in enumerate(((5, 5, 35), (4, 4, -15), (4, 3, -15), (3, 2, 20))):
        seg = parent.add(Group('tail%d' % (i + 1), (0, 27, z0), (rot, 0, 0)))
        h = w / 2
        seg.add(Cube('tail', (-h, 27 - h, z0), (h, 27 + h, z0 + length), 'scales'))
        seg.add(Cube('spike', (-0.5, 27 + h - 1, z0 + 1), (0.5, 27 + h + 1, z0 + 3), 'spike', (45, 0, 0),
                     (0, 27 + h, z0 + 2)))
        parent, z0 = seg, z0 + length
    parent.add(Cube('tail_tip', (-0.5, 26.5, z0), (0.5, 27.5, z0 + 3), 'spike'))
    return body


# ---------------------------------------------------------------------------------------------------------------- texture

def face_uv(u, v, w, h, d):
    return {'north': [u + d, v + d, u + d + w, v + d + h], 'east': [u, v + d, u + d, v + d + h],
            'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], 'west': [u + d + w, v + d, u + 2 * d + w, v + d + h],
            'up': [u + d + w, v + d, u + d, v], 'down': [u + d + 2 * w, v, u + d + w, v + d]}


def rects(u, v, w, h, d):
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


def hexc(s):
    return np.array([int(s[i:i + 2], 16) for i in (1, 3, 5)], float)


GOLD_TOP, GOLD, GOLD_LOW, ORANGE = hexc('#FFE27A'), hexc('#F2BE3A'), hexc('#E99A2C'), hexc('#E0701C')


def mix(a, b, t):
    return a + (b - a) * t


def paint_face(tex, mat, face, x, y, w, h, along):
    """Paints one face. along: the texture axis that runs along the length of the cube (0 = u, 1 = v) for feathers."""
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)              # 0 at the top of a side face
            n = RND.uniform(-1, 1)
            if mat in ('gold', 'gold_belly', 'muzzle'):
                if face == 'up':
                    c = mix(GOLD_TOP, GOLD, 0.3 + 0.2 * n)
                elif face == 'down':
                    c = GOLD_LOW * 0.92
                else:
                    c = mix(GOLD, GOLD_LOW, t) if t < 0.75 else mix(GOLD_LOW, ORANGE, (t - 0.75) * 2)
                if mat == 'gold_belly':
                    c = mix(c, GOLD_LOW, 0.4)
                c = c * (1 + 0.05 * n)
                if RND.random() < 0.01:
                    c = hexc('#FFEB9A')          # sparkles of the gold
            elif mat == 'hoof':
                c = mix(hexc('#B8742A'), hexc('#7E4A16'), t) * (1 + 0.05 * n)
            elif mat == 'mane':
                # flames: orange at the root, yellow then white at the end
                c = mix(ORANGE, hexc('#FFD23A'), 0.5 + 0.5 * math.sin(i * 1.7 + j * 0.6)) * (1 + 0.06 * n)
            elif mat == 'eye':
                c = hexc('#2F7BFF') if (i + j) % 3 else hexc('#A8D8FF')
            elif mat == 'halo':
                c = mix(hexc('#FFF7C4'), hexc('#FFE15A'), 0.5 + 0.5 * n)
            elif mat == 'sun':
                cx, cy = (w - 1) / 2, (h - 1) / 2
                r = math.hypot((i - cx) / max(1, cx), (j - cy) / max(1, cy)) / math.sqrt(2)
                r = min(1, r + 0.08 * n)
                c = (mix(hexc('#FFFFFF'), hexc('#FFF27A'), r / 0.35) if r < 0.35 else
                     mix(hexc('#FFF27A'), hexc('#FFB52E'), (r - 0.35) / 0.35) if r < 0.7 else
                     mix(hexc('#FFB52E'), hexc('#FF7A12'), (r - 0.7) / 0.3))
                if RND.random() < 0.04:
                    c = hexc('#FFFFFF')
            elif mat in ('feather', 'feather_tip'):
                k = i if along == 1 else j       # across the feather
                if mat == 'feather':
                    c = mix(hexc('#FFF0B8'), hexc('#F6D778'), 0.5 + 0.4 * n)
                    if face in ('up', 'down') and k % 3 == 1 and RND.random() < 0.55:
                        c = hexc('#F08A1C')       # the orange streaks along the feather
                else:
                    c = mix(hexc('#FFE82E'), hexc('#FFF59A'), 0.5 + 0.5 * n)
                    if RND.random() < 0.18:
                        c = hexc('#5A3C12')       # the jagged dark edge of the tips
            elif mat == 'scales':
                c = mix(GOLD, ORANGE, 0.35 + 0.3 * t) if face != 'down' else mix(GOLD_TOP, GOLD, 0.5)
                if (i + 2 * (j // 2)) % 4 == 0 or j % 2 == 0 and i % 4 == 2:
                    c = c * 0.8                    # the scales
                c = c * (1 + 0.04 * n)
            elif mat == 'spike':
                c = mix(ORANGE, hexc('#B4541A'), t) * (1 + 0.05 * n)
            else:
                c = np.array([255, 0, 255.0])
            tex[y + j, x + i] = (*np.clip(c, 0, 255).astype(int), 255)


def all_cubes(node, out):
    if isinstance(node, Cube):
        out.append(node)
    else:
        for c in node.children:
            all_cubes(c, out)
    return out


def layout(cubes):
    """Places the box UV of every cube (shelves); mirrored cubes share the place of their original."""
    places, x, y, row = {}, 0, 0, 0
    for c in sorted(cubes, key=lambda c: -(c.size[2] + c.size[1])):
        key = c.uvkey or c.uuid
        if key in places:
            continue
        w, h, d = c.size
        W, H = 2 * (w + d), d + h
        if x + W > TEX_W:
            x, y, row = 0, y + row, 0
        places[key] = (x, y)
        x += W
        row = max(row, H)
    assert y + row <= TEX_H, 'texture too small: %d' % (y + row)
    return places


# ---------------------------------------------------------------------------------------------------------------- animations
# Written in the space of the groups (rotation x > 0: the top goes back, the front goes up; z > 0: the left wing goes
# up). In the Modded Entity format, Blockbench adds the keyframes to the rotations of the groups as they are.

def kf(channel, t, v, interp='catmullrom'):
    return {'channel': channel, 'data_points': [{'x': round(v[0], 3), 'y': round(v[1], 3), 'z': round(v[2], 3)}],
            'uuid': uid(), 'time': round(t, 4), 'color': -1, 'interpolation': interp}


def mirror_keys(keys):
    """The keyframes of the right side from those of the left one (rotations y and z negated, position x)."""
    out = []
    for ch, t, v, *rest in keys:
        if ch == 'rotation':
            v = (v[0], -v[1], -v[2])
        elif ch == 'position':
            v = (-v[0], v[1], v[2])
        out.append((ch, t, v, *rest))
    return out


def animations(groups):
    def anim(name, length, loop, tracks):
        animators = {}
        for gname, keys in tracks.items():
            g = groups[gname]
            animators[g.uuid] = {'name': gname, 'type': 'bone',
                                 'keyframes': [kf(ch, t, v, *rest) for ch, t, v, *rest in keys]}
        return {'uuid': uid(), 'name': 'animation.sun_alpha.' + name, 'loop': 'loop' if loop else 'once',
                'override': False, 'length': length, 'snapping': 24, 'selected': False, 'anim_time_update': '',
                'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators}

    def cyc(channel, length, values, axis=None):
        """Evenly spaced keyframes of one loop, the last one equal to the first."""
        n = len(values)
        return [(channel, length * i / n, values[i]) for i in range(n)] + [(channel, length, values[0])]

    def rx(*a):
        return [(x, 0, 0) for x in a]

    def rz(*a):
        return [(0, 0, z) for z in a]

    out = []
    # idle: breathing, the head and the tail move a little
    t = {'body': cyc('position', 4, [(0, 0, 0), (0, -0.4, 0)]),
         'neck': cyc('rotation', 4, rx(0, -3)),
         'tail1': cyc('rotation', 4, [(0, -8, 0), (0, 8, 0)]),
         'tail3': cyc('rotation', 4, [(0, 10, 0), (0, -10, 0)]),
         'wing_l': cyc('rotation', 4, rz(0, 3)),
         'halo': cyc('position', 4, [(0, 0, 0), (0, 0.5, 0)])}
    t['wing_r'] = mirror_keys(t['wing_l'])
    out.append(anim('idle', 4, True, t))

    # walk: diagonal pairs of legs, the head nods
    swing = [18, 0, -18, 0]
    t = {'leg_fl': cyc('rotation', 1, rx(*swing)), 'leg_br': cyc('rotation', 1, rx(*swing)),
         'leg_fr': cyc('rotation', 1, rx(*[-s for s in swing])), 'leg_bl': cyc('rotation', 1, rx(*[-s for s in swing])),
         'shin_fl': cyc('rotation', 1, rx(-30, 0, 0, -10)), 'shin_br': cyc('rotation', 1, rx(0, 0, 25, 10)),
         'shin_fr': cyc('rotation', 1, rx(0, -10, -30, 0)), 'shin_bl': cyc('rotation', 1, rx(25, 10, 0, 0)),
         'neck': cyc('rotation', 1, rx(-4, 2, -4, 2)),
         'body': cyc('position', 1, [(0, 0.5, 0), (0, 0, 0), (0, 0.5, 0), (0, 0, 0)]),
         'tail1': cyc('rotation', 1, [(0, -6, 0), (0, 6, 0)])}
    out.append(anim('walk', 1, True, t))

    # fly: the flight of a pegasus, 0.9 s per wing beat: big beats, front legs folded, hind legs stretched back
    T = 0.9
    t = {'wing_l': [('rotation', 0, (0, 0, 8)), ('rotation', 0.3, (0, 10, -80)), ('rotation', 0.6, (0, 0, -35)),
                    ('rotation', T, (0, 0, 8))],
         'forearm_l': [('rotation', 0, (0, 0, -15)), ('rotation', 0.3, (0, 0, 15)), ('rotation', 0.6, (0, 0, 25)),
                       ('rotation', T, (0, 0, -15))],
         'hand_l': [('rotation', 0, (0, 0, -20)), ('rotation', 0.35, (0, 0, 20)), ('rotation', 0.65, (0, 0, 10)),
                    ('rotation', T, (0, 0, -20))],
         'body': [('position', 0, (0, 0, 0)), ('position', 0.35, (0, 1.5, 0)), ('position', T, (0, 0, 0))],
         'neck': [('rotation', 0, (-8, 0, 0)), ('rotation', 0.3, (-14, 0, 0)), ('rotation', T, (-8, 0, 0))],
         'leg_fl': cyc('rotation', T, rx(45, 60)), 'leg_fr': cyc('rotation', T, rx(60, 45)),
         'shin_fl': cyc('rotation', T, rx(-80, -95)), 'shin_fr': cyc('rotation', T, rx(-95, -80)),
         'leg_bl': cyc('rotation', T, rx(-40, -50)), 'leg_br': cyc('rotation', T, rx(-50, -40)),
         'shin_bl': cyc('rotation', T, rx(25, 35)), 'shin_br': cyc('rotation', T, rx(35, 25)),
         'tail1': cyc('rotation', T, rx(-25, -32)),
         'tail2': cyc('rotation', T, [(5, 0, 0), (12, 0, 0)]),
         'tail3': cyc('rotation', T, [(8, 0, 0), (0, 0, 0)])}
    for g in ('wing', 'forearm', 'hand'):
        t[g + '_r'] = mirror_keys(t[g + '_l'])
    out.append(anim('fly', T, True, t))

    # ult: rears up on the hind legs (like a horse throwing off its rider), the front legs paw the air, and the sun
    # leaves its place above the head and flies to the aimed point (0.9 s to 1.5 s); the sun then grows back
    # (sun_rebuild). The real flight of the sun is done in game (the projectile).
    up = 45
    hold = [0.35, 2.2]
    paw = []
    for i in range(7):
        paw.append(0.35 + i * 0.25)
    t = {'body': [('rotation', 0, (0, 0, 0)), ('rotation', 0.35, (up, 0, 0)), ('rotation', 1.3, (up + 4, 0, 0)),
                  ('rotation', 2.2, (up, 0, 0)), ('rotation', 2.7, (0, 0, 0))],
         'leg_bl': [('rotation', 0, (0, 0, 0)), ('rotation', 0.35, (-up + 5, 0, 0)), ('rotation', 2.2, (-up + 5, 0, 0)),
                    ('rotation', 2.7, (0, 0, 0))],
         'shin_bl': [('rotation', 0, (0, 0, 0)), ('rotation', 0.35, (10, 0, 0)), ('rotation', 2.2, (10, 0, 0)),
                     ('rotation', 2.7, (0, 0, 0))],
         'neck': [('rotation', 0, (0, 0, 0)), ('rotation', 0.35, (-5, 0, 0)), ('rotation', 0.7, (-15, 0, 0)),
                  ('rotation', 1.0, (-5, 0, 0)), ('rotation', 1.4, (-14, 0, 0)), ('rotation', 2.2, (-5, 0, 0)),
                  ('rotation', 2.7, (0, 0, 0))],
         'tail1': [('rotation', 0, (0, 0, 0)), ('rotation', 0.35, (-30, 0, 0)), ('rotation', 1.2, (-30, 12, 0)),
                   ('rotation', 2.2, (-30, -12, 0)), ('rotation', 2.7, (0, 0, 0))],
         'wing_l': [('rotation', 0, (0, 0, 0)), ('rotation', 0.35, (0, 0, 8)), ('rotation', 0.6, (0, 0, 2)),
                    ('rotation', 0.85, (0, 0, 8)), ('rotation', 1.1, (0, 0, 2)), ('rotation', 2.2, (0, 0, 8)),
                    ('rotation', 2.7, (0, 0, 0))]}
    # the front legs paw the air one after the other (thigh forward and up, the shin folded)
    for side, phase in (('fl', 0), ('fr', 1)):
        thigh, shin = [('rotation', 0, (0, 0, 0))], [('rotation', 0, (0, 0, 0))]
        for i, k in enumerate(paw):
            high = (i + phase) % 2 == 0
            thigh.append(('rotation', k, (75 if high else 25, 0, 0)))
            shin.append(('rotation', k, (-85 if high else -25, 0, 0)))
        thigh.append(('rotation', 2.7, (0, 0, 0)))
        shin.append(('rotation', 2.7, (0, 0, 0)))
        t['leg_' + side], t['shin_' + side] = thigh, shin
    t['leg_br'], t['shin_br'] = mirror_keys(t['leg_bl']), mirror_keys(t['shin_bl'])
    t['wing_r'] = mirror_keys(t['wing_l'])
    # the sun: charges (pulses), then flies forward and down to the aimed point, in the frame of the reared body
    a = math.radians(up)

    def body_frame(dy, dz):
        """A displacement of the world (up, back) in the frame of the body reared by `up` degrees."""
        return (0, dy * math.cos(a) + dz * math.sin(a), -dy * math.sin(a) + dz * math.cos(a))
    # (it rises over the head first, then dives forward)
    t['sun'] = [('position', 0, (0, 0, 0)), ('position', 0.9, (0, 0, 0)), ('position', 1.05, body_frame(22, -2)),
                ('position', 1.25, body_frame(24, -40)), ('position', 1.5, body_frame(-20, -100)),
                ('scale', 0, (1, 1, 1)), ('scale', 0.45, (1.02, 1.02, 1.02)), ('scale', 0.6, (1, 1, 1)),
                ('scale', 0.75, (1.03, 1.03, 1.03)), ('scale', 0.9, (1.02, 1.02, 1.02)), ('scale', 1.45, (0.7, 0.7, 0.7)),
                ('scale', 1.5, (0.01, 0.01, 0.01), 'step'), ('scale', 3.0, (0.01, 0.01, 0.01), 'step'),
                ('rotation', 0, (0, 0, 0), 'linear'), ('rotation', 1.5, (0, 540, 0), 'linear')]
    out.append(anim('ult', 3.0, False, t))

    # sun_spin: one turn in 60 s on y and in 90 s on x (the loop lasts 180 s: 3 turns on y, 2 on x)
    out.append(anim('sun_spin', 180, True, {'sun': [('rotation', 0, (0, 0, 0), 'linear'),
                                                    ('rotation', 180, (720, 1080, 0), 'linear')]}))

    # sun_afk: the speed of the spin grows from the slow one (6 deg/s on y, 4 on x) to 250 times more in 10 s; from
    # the 3rd second the sun shrinks, faster and faster; at 10 s it turns to stardust (the particles are made in
    # game), appears again right away (0.15 s) and grows back to its size in 1 s, spinning slowly again
    keys = []
    ay = ax = 0.0
    step = 0.125
    t0 = 0.0
    while t0 < 10 - 1e-9:
        keys.append(('rotation', t0, (round(ax, 2), round(ay, 2), 0), 'linear'))
        # (the speed at the middle of the step: wy = 6 + 1500 (t/10)^2, wx = 4 + 1000 (t/10)^2)
        m = (t0 + step / 2) / 10
        ay += (6 + 1500 * m * m) * step
        ax += (4 + 1000 * m * m) * step
        t0 += step
    keys.append(('rotation', 10, (round(ax, 2), round(ay, 2), 0), 'linear'))
    keys.append(('rotation', 11.5, (round(ax + 4 * 1.5, 2), round(ay + 6 * 1.5, 2), 0), 'linear'))
    shrink = [('scale', 0, (1, 1, 1), 'linear'), ('scale', 3, (1, 1, 1), 'linear')]
    for k in range(1, 8):
        tt = 3 + k
        f = 1 - 0.85 * (k / 7) ** 2
        shrink.append(('scale', tt, (f, f, f)))
    shrink += [('scale', 10.02, (0.01, 0.01, 0.01), 'step'), ('scale', 10.15, (0.2, 0.2, 0.2)),
               ('scale', 10.5, (0.75, 0.75, 0.75)), ('scale', 11.0, (1, 1, 1)), ('scale', 11.5, (1, 1, 1))]
    out.append(anim('sun_afk', 11.5, False, {'sun': keys + shrink}))

    # sun_rebuild: the sun grows back above the head (the game spreads it over the 45 s of recharge)
    t = {'sun': [('scale', 0, (0.01, 0.01, 0.01)), ('scale', 2.0, (1, 1, 1)),
                 ('rotation', 0, (0, 0, 0), 'linear'), ('rotation', 2.0, (0, 360, 0), 'linear')]}
    out.append(anim('sun_rebuild', 2.0, False, t))
    return out


# ---------------------------------------------------------------------------------------------------------------- file

def main():
    root = build()
    cubes = all_cubes(root, [])
    places = layout(cubes)
    tex = np.zeros((TEX_H, TEX_W, 4), np.uint8)
    painted = set()
    elements = []
    for c in cubes:
        key = c.uvkey or c.uuid
        u, v = places[key]
        w, h, d = c.size
        if key not in painted:
            painted.add(key)
            for face, (x, y, fw, fh) in rects(u, v, w, h, d).items():
                paint_face(tex, c.mat, face, x, y, fw, fh, 1 if face in ('up', 'down') else 0)
        e = {'name': c.name, 'box_uv': True, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0,
             'allow_mirror_modeling': True, 'from': c.frm, 'to': c.to, 'autouv': 0, 'color': 0,
             'origin': c.origin or [0, 0, 0], 'uv_offset': [u, v], 'mirror_uv': c.mirror,
             'faces': {f: {'uv': r, 'texture': 0} for f, r in face_uv(u, v, w, h, d).items()}, 'type': 'cube',
             'uuid': c.uuid}
        if c.rotation:
            e['rotation'] = c.rotation
        elements.append(e)

    groups, by_name = [], {}

    def outline(node):
        if isinstance(node, Cube):
            return node.uuid
        groups.append({'name': node.name, 'uuid': node.uuid, 'export': True, 'locked': False, 'scope': 0,
                       'selected': False, 'visibility': True, 'origin': node.origin, 'rotation': node.rotation,
                       'color': 0, 'children': [], 'reset': False, 'shade': True, 'mirror_uv': False, 'autouv': 0,
                       'isOpen': False})
        by_name.setdefault(node.name, node)
        return {'uuid': node.uuid, 'isOpen': node.name == 'body', 'children': [outline(c) for c in node.children]}
    tree = outline(root)

    buf = io.BytesIO()
    Image.fromarray(tex).save(buf, 'PNG')
    texture = {'path': '', 'name': 'sun_alpha', 'folder': '', 'namespace': '', 'id': '0', 'width': TEX_W,
               'height': TEX_H, 'uv_width': TEX_W, 'uv_height': TEX_H, 'particle': False, 'use_as_default': False,
               'layers_enabled': False, 'render_mode': 'default', 'render_sides': 'auto', 'wrap_mode': 'limited',
               'pbr_channel': 'color', 'visible': True, 'internal': True, 'saved': False, 'uuid': uid(),
               'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}
    bb = {'meta': {'format_version': '5.0', 'model_format': 'modded_entity', 'box_uv': True},
          'name': 'dragon_soleil_alpha', 'model_identifier': 'dragon_soleil_alpha', 'modded_entity_entity_class': '',
          'modded_entity_version': '1.17', 'modded_entity_flip_y': True, 'visible_box': [1, 1, 0],
          'variable_placeholders': '', 'variable_placeholder_buttons': [], 'timeline_setups': [],
          'unhandled_root_fields': {}, 'resolution': {'width': TEX_W, 'height': TEX_H}, 'elements': elements,
          'groups': groups, 'outliner': [tree], 'textures': [texture], 'animations': animations(by_name)}
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(bb, f)
    print('%d cubes, %d groups, %d animations -> %s' % (len(elements), len(groups), len(bb['animations']), OUT))


if __name__ == '__main__':
    main()
