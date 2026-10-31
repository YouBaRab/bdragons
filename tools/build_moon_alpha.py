"""Builds the Blockbench file of the Moon Dragon (alpha of the moonlight), from the sketch of the author
(E:/PROJET_MINECRAFT/alpha/dragon_lunaire/alpha_lunaire.png) and the body of the Tempest Dragon
(tools/models/dragon_tempete_alpha_v2.bbmodel):

- the long body of the Tempest Dragon, without its horns, its whiskers, its brows, its ears nor its crests;
- four clawed legs: the arms of the Tempest Dragon, and the same legs at the back of the body;
- the head and the neck of an Ender Dragon, with its proportions (a neck of the same width all along, a head 1.6 times
  as wide, a long snout, two small plates on the skull, blue eyes), with a crescent moon standing on its head;
- a ball of moon at the end of the tail (no more fins);
- stars in orbit all around its body (instead of the clouds of the Tempest Dragon), turning slowly around it
  (the animation stars_orbit);
- painted like a well lit night sky: night blue with the stars and the milky way of the photo of the sketch; the moons
  yellow / white, the stars white / blue, the eyes blue;
- the star shaped platform that carries it when the player is away from the keyboard (its own group).

Run from the project folder:
    python tools/build_moon_alpha.py
It writes tools/models/dragon_lune_alpha.bbmodel (the Blockbench changes of that file are lost: change this script, or
keep working in Blockbench afterwards).
"""
import base64
import copy
import io
import json
import math
import os
import random
import uuid

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
STORM = os.path.join(HERE, 'models', 'dragon_tempete_alpha_v2.bbmodel')
OUT = os.path.join(HERE, 'models', 'dragon_lune_alpha.bbmodel')
SKY = os.path.join(HERE, 'models', 'lune_ciel.png')
TEX_W, TEX_H = 1024, 512
RND = random.Random(5)

# the parts of the Tempest Dragon that the Moon Dragon does not have
DROP_CUBES = ('crest', 'back_cloud', 'sourcil_left', 'sourcil_right', 'moustache_left_1', 'moustache_left_2',
              'moustache_left_3', 'moustache_right_1', 'moustache_right_2', 'moustache_right_3', 'cube', 'head_body',
              'upper_jaw', 'lower_jaw', 'nostril_left', 'nostril_right')
DROP_GROUPS = ('horn_left', 'horn_right', 'ear_left', 'ear_right')
# the back legs: a copy of the arms, on this segment
BACK_SEGMENT, BACK_Z = 'segment13', 305.0
ARMS_Z = 46.0


def uid():
    return str(uuid.UUID(int=RND.getrandbits(128)))


def cube(name, frm, to, mat, rotation=None, origin=None):
    e = {'name': name, 'box_uv': True, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0,
         'allow_mirror_modeling': True, 'from': [float(v) for v in frm], 'to': [float(v) for v in to], 'autouv': 0,
         'color': 0, 'origin': list(origin) if origin else [0.0, 0.0, 0.0], 'type': 'cube', 'uuid': uid(), 'mat': mat}
    if rotation:
        e['rotation'] = list(rotation)
    return e


def group(name, origin, rotation=(0, 0, 0)):
    return {'name': name, 'uuid': uid(), 'export': True, 'locked': False, 'scope': 0, 'selected': False,
            'visibility': True, 'origin': list(origin), 'rotation': list(rotation), 'color': 0, 'children': [],
            'reset': False, 'shade': True, 'mirror_uv': False, 'autouv': 0, 'isOpen': False}


def main():
    bb = json.load(open(STORM, encoding='utf-8'))
    elements = {e['uuid']: e for e in bb['elements']}
    groups = {g['uuid']: g for g in bb['groups']}
    keep_el, keep_gr = [], []
    nodes = {}

    # --- the body of the Tempest Dragon, cleaned -------------------------------------------------------------------
    def clean(node):
        g = groups[node['uuid']]
        if g['name'] in DROP_GROUPS:
            return None
        out = {'uuid': node['uuid'], 'isOpen': False, 'children': []}
        nodes[g['name']] = out
        keep_gr.append(g)
        for c in node['children']:
            if isinstance(c, str):
                e = elements[c]
                if e['name'] in DROP_CUBES:
                    continue
                e['mat'] = 'talon' if e['name'] == 'talon' else 'sky'
                # the neck of the Ender Dragon has the same width all along (10, here x 1.5)
                if e['name'] in ('neck2', 'neck3', 'neck4'):
                    e['from'][0], e['to'][0] = -7.5, 7.5
                    e['from'][1], e['to'][1] = 33.25, 48.25
                keep_el.append(e)
                out['children'].append(c)
            else:
                k = clean(c)
                if k:
                    out['children'].append(k)
        return out

    tree = [clean(n) for n in bb['outliner'] if not isinstance(n, str)]
    new_groups = []

    def add_group(parent, g):
        new_groups.append(g)
        node = {'uuid': g['uuid'], 'isOpen': False, 'children': []}
        nodes[g['name']] = node
        parent['children'].append(node)
        return node

    def add(parent, e):
        keep_el.append(e)
        parent['children'].append(e['uuid'])

    # --- the head of an Ender Dragon, smaller ----------------------------------------------------------------------
    head = nodes['head']
    # the proportions of the Ender Dragon: its neck is 10 wide and its head 16 (x 1.6), its snout 12 x 5 x 16 and its
    # jaw 12 x 4 x 16; here the neck is 15 wide, so everything is 1.5 times the Ender Dragon
    head_scale = 1.5
    def hb(x0, y0, z0, x1, y1, z1):
        k = head_scale
        return (x0 * k, 40.75 + y0 * k, -24 + z0 * k), (x1 * k, 40.75 + y1 * k, -24 + z1 * k)
    add(head, cube('head_body', *hb(-8, -8, -16, 8, 8, 0), 'sky'))
    add(head, cube('upper_jaw', *hb(-6, -4, -32, 6, 1, -16), 'sky'))
    add(head, cube('nostril_left', *hb(3, 1, -30, 5, 3, -26), 'sky'))
    add(head, cube('nostril_right', *hb(-5, 1, -30, -3, 3, -26), 'sky'))
    add(head, cube('plate_left', *hb(3, 8, -12, 5, 12, -6), 'sky'))
    add(head, cube('plate_right', *hb(-5, 8, -12, -3, 12, -6), 'sky'))
    add(head, cube('eye_left', *hb(3, 2, -16.4, 6.4, 4, -15.8), 'eye'))
    add(head, cube('eye_right', *hb(-6.4, 2, -16.4, -3, 4, -15.8), 'eye'))
    jaw = nodes['jaw']
    groups[jaw['uuid']]['origin'] = [0, 40.75 - 4 * head_scale, -24 - 16 * head_scale]
    add(jaw, cube('lower_jaw', *hb(-6, -8, -32, 6, -4, -16), 'sky'))

    # the crescent moon standing on its head (in the plane of the body), its horns towards the back and the sky
    moon = add_group(head, group('crescent', (0, 68, -36)))
    cy, cz, radius = 68.0, -36.0, 9.5
    n = 11
    for i in range(n):
        a0, a1 = math.radians(120 + i * 230 / n), math.radians(120 + (i + 1) * 230 / n)
        am = (a0 + a1) / 2
        t = 1 - abs(i - (n - 1) / 2) / ((n - 1) / 2)     # 1 in the middle, 0 at the horns
        thick = 1 + round(4.5 * t)
        length = math.ceil(2 * radius * math.sin((a1 - a0) / 2)) + 1
        pz, py = cz + radius * math.cos(am), cy + radius * math.sin(am)
        # the long side of the cube along the tangent of the circle
        dz, dy = -math.sin(am), math.cos(am)
        rx = math.degrees(math.atan2(-dy, dz))
        add(moon, cube('crescent', (-1.5, py - thick / 2, pz - length / 2), (1.5, py + thick / 2, pz + length / 2), 'moon',
                       (round(rx, 3), 0, 0), (0, py, pz)))

    # --- the back legs: the arms again, on the back of the body -----------------------------------------------------
    arms = nodes['arms']
    back = nodes[BACK_SEGMENT]
    shift = BACK_Z - ARMS_Z

    def copy_node(node, parent):
        g = copy.deepcopy(groups[node['uuid']])
        g['uuid'] = uid()
        g['name'] = (g['name'].replace('arms', 'legs').replace('arm_', 'leg_').replace('hand_', 'foot_')
                     .replace('finger_', 'toe_'))
        g['origin'] = [g['origin'][0], g['origin'][1], g['origin'][2] + shift]
        out = add_group(parent, g)
        for c in node['children']:
            if isinstance(c, str):
                e = copy.deepcopy(elements[c])
                e['uuid'] = uid()
                e['from'][2] += shift
                e['to'][2] += shift
                e['origin'] = [e['origin'][0], e['origin'][1], e['origin'][2] + shift]
                e['mat'] = 'talon' if e['name'] == 'talon' else 'sky'
                add(out, e)
            else:
                copy_node(c, out)
    copy_node(arms, back)

    # --- the ball of moon at the end of the tail ------------------------------------------------------------------
    tail5 = nodes['tail5']
    c = (0.0, 40.75, 456.0)
    for (w, h, d) in ((14, 10, 10), (10, 14, 10), (10, 10, 14), (12, 12, 12)):
        add(tail5, cube('moon_ball', (c[0] - w / 2, c[1] - h / 2, c[2] - d / 2), (c[0] + w / 2, c[1] + h / 2, c[2] + d / 2), 'moon'))

    # --- stars on the back, where the clouds of the Tempest Dragon were ---------------------------------------------
    def star(parent, cy, cz, size, mat, name='star', flat=False):
        """A star of five points: flat on the ground (flat) or standing in the plane of the body."""
        k = size / 7.0
        if flat:
            add(parent, cube(name, (-2.5 * k, cy - 1, cz - 2.5 * k), (2.5 * k, cy + 1, cz + 2.5 * k), mat))
        else:
            add(parent, cube(name, (-1, cy - 2.5 * k, cz - 2.5 * k), (1, cy + 2.5 * k, cz + 2.5 * k), mat))
        for p in range(5):
            a = math.radians(90 + p * 72)
            for (r0, r1, t) in ((1.5, 4.5, 3), (4.5, 7.5, 1)):
                r0k, r1k, tk = r0 * k, r1 * k, max(1, round(t * k))
                rm = (r0k + r1k) / 2
                length = math.ceil(r1k - r0k)
                if flat:
                    px, pz = rm * math.cos(a), cz + rm * math.sin(a)
                    ry = math.degrees(math.atan2(math.cos(a), math.sin(a)))
                    add(parent, cube(name, (px - tk / 2, cy - 1, pz - length / 2), (px + tk / 2, cy + 1, pz + length / 2), mat,
                                     (0, round(ry, 3), 0), (px, cy, pz)))
                else:
                    pz, py = cz + rm * math.cos(a), cy + rm * math.sin(a)
                    rx = math.degrees(math.atan2(-math.sin(a), math.cos(a)))
                    add(parent, cube(name, (-1, py - tk / 2, pz - length / 2), (1, py + tk / 2, pz + length / 2), mat,
                                     (round(rx, 3), 0, 0), (0, py, pz)))

    # stars in orbit all around the body (not a crest): each one in its own group turning around the axis of the body
    pieces = []
    for name, node in nodes.items():
        if not (name == 'torso' or name.startswith('segment') or name.startswith('tail')):
            continue
        main = [elements[c] for c in node['children'] if isinstance(c, str) and c in elements and elements[c]['name'] == name]
        if main:
            pieces.append((node, main[0]))
    orbit = []
    rs = random.Random(21)
    z = 40.0
    k = 0
    while z < 440:
        z += rs.uniform(12, 22)
        # not where the legs turn
        if abs(z - ARMS_Z) < 14 or abs(z - BACK_Z) < 14:
            continue
        node, m = next(((nd, mm) for nd, mm in pieces if mm['from'][2] <= z <= mm['to'][2]), (None, None))
        if node is None:
            continue
        half = (m['to'][1] - m['from'][1]) / 2
        cy = (m['to'][1] + m['from'][1]) / 2
        k += 1
        sgroup = group('star%d' % k, (0, cy, z), (0, 0, round(rs.uniform(0, 360), 1)))
        sg = add_group(node, sgroup)
        dist = half + rs.uniform(9, 16)
        star(sg, cy + dist, z, rs.choice((4.5, 5.5, 7)), 'star')
        orbit.append((sgroup, rs.choice((1, -1)) * rs.choice((360, 360, 720))))

    # --- the star platform that carries it (away from the keyboard) -------------------------------------------------
    plat = group('star_platform', (0, 8, 200))
    new_groups.append(plat)
    pnode = {'uuid': plat['uuid'], 'isOpen': False, 'children': []}
    tree.append(pnode)
    star(pnode, 8, 200, 56, 'platform', 'platform', flat=True)

    # --- texture ------------------------------------------------------------------------------------------------
    for e in keep_el:
        e.pop('faces', None)
    tex = np.zeros((TEX_H, TEX_W, 4), np.uint8)
    sky = np.array(Image.open(SKY).convert('RGB')).astype(float)
    places_uv = {}
    x = y = row = 0

    def size(e):
        return [e['to'][i] - e['from'][i] for i in range(3)]

    # identical decorations share their place in the texture
    def key(e):
        s = size(e)
        return (e['mat'], round(s[0], 3), round(s[1], 3), round(s[2], 3)) if e['mat'] in ('star', 'moon', 'platform', 'talon') else e['uuid']

    order = sorted(keep_el, key=lambda e: -(math.ceil(size(e)[2]) + math.ceil(size(e)[1])))
    for e in order:
        k = key(e)
        if k in places_uv:
            continue
        w, h, d = (math.ceil(v) for v in size(e))
        W, H = 2 * (w + d), d + h
        if x + W > TEX_W:
            x, y, row = 0, y + row, 0
        places_uv[k] = (x, y)
        x += W
        row = max(row, H)
    assert y + row <= TEX_H, 'texture too small: %d' % (y + row)
    painted = set()
    for e in keep_el:
        k = key(e)
        u, v = places_uv[k]
        w, h, d = size(e)
        e['uv_offset'] = [u, v]
        e['faces'] = {f: {'uv': r, 'texture': 0} for f, r in face_uv(u, v, w, h, d).items()}
        if k in painted:
            continue
        painted.add(k)
        W, H = 2 * (math.ceil(w) + math.ceil(d)), math.ceil(d) + math.ceil(h)
        paint(tex, sky, e['mat'], u, v, W, H, math.ceil(h), math.ceil(d))
    for e in keep_el:
        e.pop('mat', None)

    buf = io.BytesIO()
    Image.fromarray(tex).save(buf, 'PNG')
    bb['textures'] = [{**bb['textures'][0], 'name': 'moon_dragon', 'uuid': uid(), 'width': TEX_W, 'height': TEX_H,
                       'uv_width': TEX_W, 'uv_height': TEX_H,
                       'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}]
    bb['resolution'] = {'width': TEX_W, 'height': TEX_H}
    bb['name'] = bb['model_identifier'] = 'dragon_lune_alpha'
    bb['elements'] = keep_el
    bb['groups'] = keep_gr + new_groups
    bb['outliner'] = tree
    # the orbit of the stars: one turn (or two) in 60 seconds around the body, some one way, some the other
    animators = {}
    for g, turn in orbit:
        animators[g['uuid']] = {'name': g['name'], 'type': 'bone', 'keyframes': [
            {'channel': 'rotation', 'data_points': [{'x': 0, 'y': 0, 'z': 0}], 'uuid': uid(), 'time': 0, 'color': -1, 'interpolation': 'linear'},
            {'channel': 'rotation', 'data_points': [{'x': 0, 'y': 0, 'z': turn}], 'uuid': uid(), 'time': 60, 'color': -1, 'interpolation': 'linear'}]}
    bb['animations'] = [{'uuid': uid(), 'name': 'animation.moon_alpha.stars_orbit', 'loop': 'loop', 'override': False, 'length': 60,
                         'snapping': 24, 'selected': False, 'anim_time_update': '', 'blend_weight': '', 'start_delay': '',
                         'loop_delay': '', 'animators': animators}]
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(bb, f)
    print('%d cubes, %d groups -> %s' % (len(keep_el), len(bb['groups']), OUT))


def face_uv(u, v, w, h, d):
    return {'north': [u + d, v + d, u + d + w, v + d + h], 'east': [u, v + d, u + d, v + d + h],
            'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], 'west': [u + d + w, v + d, u + 2 * d + w, v + d + h],
            'up': [u + d + w, v + d, u + d, v], 'down': [u + d + 2 * w, v, u + d + w, v + d]}


def mix(a, b, t):
    return a + (b - a) * t


def hexc(s):
    return np.array([int(s[i:i + 2], 16) for i in (1, 3, 5)], float)


def paint(tex, sky, mat, u, v, W, H, h, d):
    """Paints the whole box of one cube (its 6 faces) in the texture."""
    for j in range(H):
        for i in range(W):
            n = RND.uniform(-1, 1)
            if mat == 'sky':
                # a patch of the night sky of the photo, brightened and turned towards the night blue
                sy, sx = (v + j) * 7 % sky.shape[0], (u + i) * 5 % sky.shape[1]
                src = sky[sy, sx]
                lum = src.mean() / 255
                # (the bright band of the milky way only gives some stars, the rest stays night blue)
                if lum > 0.62 and RND.random() < 0.15:
                    # a star of the photo
                    c = mix(hexc('#BFD8FF'), hexc('#FFFFFF'), min(1, (lum - 0.62) / 0.3))
                else:
                    # the night blue, lit by the colours of the sky (a touch of the purple of the milky way)
                    base = mix(hexc('#0F1D52'), hexc('#3B55B8'), min(1, lum / 0.45))
                    c = base * 0.8 + np.minimum(src, 115) * 0.25
                if RND.random() < 0.012:
                    c = hexc('#F2F6FF')
            elif mat == 'moon':
                c = hexc('#FFF6C8') if n > -0.55 else hexc('#D9CF98')
                if RND.random() < 0.06:
                    c = hexc('#C8BE8A')      # the craters
            elif mat in ('star', 'platform'):
                c = hexc('#FFFFFF') if n > 0.2 else hexc('#BFD8FF')
                if mat == 'platform' and RND.random() < 0.15:
                    c = hexc('#8FB6FF')
            elif mat == 'eye':
                c = hexc('#2F7BFF') if (i + j) % 3 else hexc('#A8D8FF')
            elif mat == 'talon':
                c = hexc('#C9D6F0') * (1 + 0.05 * n)
            else:
                c = np.array([255, 0, 255.0])
            tex[v + j, u + i] = (*np.clip(c, 0, 255).astype(int), 255)


if __name__ == '__main__':
    main()
