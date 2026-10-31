"""Adds the storm clouds of the stage V storm dragon on the back of the Tempest Dragon (alpha of the storm), in its
Blockbench file tools/models/dragon_tempete_alpha_v2.bbmodel (the changes made in Blockbench are kept).

One cloud (four puffs, like the clouds of the stage V dragon) on each long piece of the body, from the torso to the
start of the tail, on one side and then on the other: they sit on the back next to the crests, never on them (the
crests stand on the middle line, the clouds start a little beside it). The puffs are painted with the clouds of the
storm dragon texture (dragon/storm/body.png). Running it again replaces the clouds it made.

Run from the project folder, then the generators:
    python tools/add_storm_clouds.py
    python tools/gen_storm_model.py
    python tools/gen_storm_textures.py
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
BBMODEL = os.path.join(HERE, 'models', 'dragon_tempete_alpha_v2.bbmodel')
CLOUD_SRC = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'entity', 'dragon',
                         'storm', 'body.png')
NAME = 'back_cloud'
# the clouds of the stage V dragon (DragonModel): its puffs, a little bigger here. (x, y, z) of the corner from the
# centre of the cloud (x to the outside), and the size (w, h, d)
PUFFS = [((-1.0, -1.0, -4.0), (8, 5, 8)), ((0.5, 1.5, -3.0), (7, 6, 7)), ((-3.5, -1.5, 1.0), (6, 4, 6)), ((3.0, -1.5, 2.0), (5, 4, 5))]
# the texture of the puffs: one place for each size (the clouds share it), in an empty corner of the texture
UV_AT = (424, 0)
# the crests stand on the middle line, 2 pixels wide: the clouds stay this far from it
CREST_GAP = 2.5
# the pieces that carry a cloud (the long ones), from the torso to the start of the tail
MIN_LENGTH = 24


def face_uv(u, v, w, h, d):
    return {'north': [u + d, v + d, u + d + w, v + d + h], 'east': [u, v + d, u + d, v + d + h],
            'south': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], 'west': [u + d + w, v + d, u + 2 * d + w, v + d + h],
            'up': [u + d + w, v + d, u + d, v], 'down': [u + d + 2 * w, v, u + d + w, v + d]}


def rects(u, v, w, h, d):
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


def main():
    bb = json.load(open(BBMODEL, encoding='utf-8'))
    groups = {g['uuid']: g for g in bb['groups']}
    elements = {e['uuid']: e for e in bb['elements']}

    # the clouds made before are removed (from the elements and from the outliner)
    old = {e['uuid'] for e in bb['elements'] if e['name'] == NAME}
    bb['elements'] = [e for e in bb['elements'] if e['uuid'] not in old]

    def clean(node):
        node['children'] = [c for c in node['children'] if not (isinstance(c, str) and c in old)]
        for c in node['children']:
            if not isinstance(c, str):
                clean(c)
    for n in bb['outliner']:
        if not isinstance(n, str):
            clean(n)

    nodes = {}

    def index(node):
        if isinstance(node, str):
            return
        nodes[groups[node['uuid']]['name']] = node
        for c in node['children']:
            index(c)
    for n in bb['outliner']:
        index(n)

    # the place of each size of puff in the texture
    tw, th = bb['resolution']['width'], bb['resolution']['height']
    tex = np.array(Image.open(io.BytesIO(base64.b64decode(bb['textures'][0]['source'].split(',', 1)[1]))).convert('RGBA'))
    uvs = []
    x, y, row = UV_AT[0], UV_AT[1], 0
    for _, (w, h, d) in PUFFS:
        W, H = 2 * (w + d), d + h
        if x + W > tw:
            x, y, row = UV_AT[0], y + row, 0
        uvs.append((x, y))
        x += W
        row = max(row, H)
    used = np.zeros(tex.shape[:2], bool)
    for e in bb['elements']:
        u, v = e.get('uv_offset', [0, 0])
        w, h, d = (e['to'][i] - e['from'][i] for i in range(3))
        used[int(v):int(v + math.ceil(d + h)), int(u):int(u + math.ceil(2 * (d + w)))] = True
    for (u, v), (_, (w, h, d)) in zip(uvs, PUFFS):
        area = (slice(v, v + d + h), slice(u, u + 2 * (w + d)))
        # (painted already when the clouds are made again)
        if used[area].any() or (not old and tex[area][..., 3].any()):
            raise SystemExit('the texture is not free at %d %d' % (u, v))

    # paint the puffs with the clouds of the stage V storm dragon
    src = np.array(Image.open(CLOUD_SRC).convert('RGBA'))[128:150, 198:232]
    colours = src[src[..., 3] > 200][:, :3]
    rnd = random.Random(11)
    for (u, v), (_, (w, h, d)) in zip(uvs, PUFFS):
        for face, (fx, fy, fw, fh) in rects(u, v, w, h, d).items():
            k = 1.12 if face == 'up' else 0.72 if face == 'down' else 1.0
            for py in range(fy, fy + fh):
                for px in range(fx, fx + fw):
                    c = colours[rnd.randrange(len(colours))].astype(float)
                    # sides: lighter at the top, darker underneath
                    if face not in ('up', 'down'):
                        c *= 1.08 - 0.25 * (py - fy) / max(1, fh - 1)
                    # (kept under the white of the lightning: the clouds do not shine, see gen_storm_textures.py)
                    tex[py, px] = (*np.clip(c * k, 0, 210).astype(int), 255)

    # the clouds, one on each long piece, on one side then the other
    pieces = ['torso'] + ['segment%d' % i for i in range(1, 16)]
    side = 1
    made = 0
    for name in pieces:
        node = nodes[name]
        main = [elements[c] for c in node['children'] if isinstance(c, str) and elements[c]['name'] == name][0]
        (x0, y0, z0), (x1, y1, z1) = main['from'], main['to']
        if z1 - z0 < MIN_LENGTH:
            continue
        half = (x1 - x0) / 2
        top = y1
        # the centre of the cloud: beside the crests, on the curve of the back (a little lower towards the side)
        cx = side * (CREST_GAP + 4.5)
        cy = top - 1.0 - 1.5 * abs(cx) / half
        cz = z0 + (z1 - z0) * (0.35 if side > 0 else 0.6)
        for (ox, oy, oz), (w, h, d) in PUFFS:
            # x towards the outside of the side of the cloud, never on the crests
            if side > 0:
                fx0 = max(cx + ox, CREST_GAP)
            else:
                fx0 = min(cx - ox, -CREST_GAP) - w
            fy0 = cy + oy
            fz0 = cz + oz
            u, v = uvs[PUFFS.index(((ox, oy, oz), (w, h, d)))]
            e = {'name': NAME, 'box_uv': True, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0,
                 'allow_mirror_modeling': True, 'from': [fx0, fy0, fz0], 'to': [fx0 + w, fy0 + h, fz0 + d], 'autouv': 0,
                 'color': 4, 'origin': list(main['origin']), 'uv_offset': [u, v],
                 'faces': {f: {'uv': r, 'texture': 0} for f, r in face_uv(u, v, w, h, d).items()}, 'type': 'cube',
                 'uuid': str(uuid.uuid4())}
            bb['elements'].append(e)
            node['children'].append(e['uuid'])
        made += 1
        side = -side

    out = io.BytesIO()
    Image.fromarray(tex.astype(np.uint8)).save(out, 'PNG')
    bb['textures'][0]['source'] = 'data:image/png;base64,' + base64.b64encode(out.getvalue()).decode()
    with open(BBMODEL, 'w', encoding='utf-8') as f:
        json.dump(bb, f)
    print('%d clouds on the back' % made)


if __name__ == '__main__':
    main()
