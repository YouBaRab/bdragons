"""Builds tools/models/dragon_tempete_alpha_v2.bbmodel: the Tempest Dragon with a new anatomy.

The anatomy (only the skeleton and the proportions) follows the long serpent dragons of Cobblemon (Rayquaza: a 4 piece
neck, a chest that carries two long arms held sideways with 3 two-jointed fingers, a body of 15 pieces in a rhythm
"thin ring, long piece, long piece, thin ring, thick piece" repeated 3 times, a tail of 5 tapering pieces). Nothing is
copied from it (no cube, no texture, none of its fins or markings): the sizes are our own, about 1.75 times its
proportions, and the head, the horns, the ears, the whiskers, the crests, the talons and the texture are those of our
Tempest Dragon (dragon_tempete_alpha.bbmodel).

Every piece is a group whose origin is its joint with the piece in front of it, each one the child of the one in front
(a chain, like the bones of a snake): body > neck1 > neck2 > neck3 > neck4 > head and body > torso > segment1 > ... >
segment15 > tail1 > ... > tail5; the arms hang from the torso.

The texture is repainted with the pieces of the old one: the head keeps its pixels, the new pieces are tiled with the
scales of the old body (back and sides) and the ribs of its belly (under), the crests and the talons are scaled from
the old ones.

Run from the project folder:  python tools/build_storm_v2.py
"""
import base64
import copy
import io
import json
import math
import os
import random
import uuid as uuidlib

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OLD = os.path.join(HERE, 'models', 'dragon_tempete_alpha.bbmodel')
NEW = os.path.join(HERE, 'models', 'dragon_tempete_alpha_v2.bbmodel')

Y = 40.75           # height of the spine (same as the old model)
HEAD_JOINT = -24.0  # joint head / neck (same as the old model)
rng = random.Random(2026)


def uid():
    return str(uuidlib.UUID(int=rng.getrandbits(128)))


class Builder:
    def __init__(self):
        self.groups = {}
        self.elements = []
        self.tree = {}        # group uuid -> children (uuids of elements or groups)
        self.roots = []
        self.paint = {}       # element uuid -> how to paint it

    def group(self, name, origin, parent=None, rotation=(0, 0, 0)):
        u = uid()
        self.groups[u] = {'name': name, 'uuid': u, 'export': True, 'locked': False, 'scope': 0, 'selected': False,
                          'visibility': True, 'origin': list(origin), 'rotation': list(rotation), 'color': 0,
                          'children': [], 'reset': False, 'shade': True, 'mirror_uv': False, 'autouv': 0,
                          'isOpen': False}
        self.tree[u] = []
        (self.tree[parent] if parent else self.roots).append(u)
        return u

    def cube(self, parent, name, frm, to, paint, origin=None, rotation=None, color=0):
        u = uid()
        e = {'name': name, 'box_uv': True, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0,
             'allow_mirror_modeling': True, 'from': [round(v, 4) for v in frm], 'to': [round(v, 4) for v in to],
             'autouv': 0, 'color': color, 'origin': list(origin or self.groups[parent]['origin']),
             'uv_offset': [0, 0], 'type': 'cube', 'uuid': u}
        if rotation:
            e['rotation'] = list(rotation)
        self.elements.append(e)
        self.tree[parent].append(u)
        self.paint[u] = paint
        return e

    def box(self, parent, name, cx, cy, z0, w, h, d, paint, color=0):
        """A cube centred on (cx, cy) in x and y, from z0 to z0 + d."""
        return self.cube(parent, name, (cx - w / 2, cy - h / 2, z0), (cx + w / 2, cy + h / 2, z0 + d), paint, color=color)

    def crests(self, parent, z0, length, top, size, count=2):
        """Crests on the back, rotated like the old ones (65 degrees), their base on the top of the piece."""
        for k in range(count):
            zc = z0 + length * (k + 0.5) / count
            c = size
            w = 2.0 if c >= 6 else 1.0
            yc = top + c * 0.2
            self.cube(parent, 'crest', (-w / 2, yc - c / 2, zc - c / 2), (w / 2, yc + c / 2, zc + c / 2),
                      ('crest',), origin=(0, yc, zc), rotation=(65, 0, 0), color=1)


def build(old):
    b = Builder()
    body = b.group('body', (0, Y, 35))

    # --- neck: thin ring at the chest, then three pieces up to the head ----------------------------------------
    neck = [('neck1', 7, 14, 14), ('neck2', 17, 16, 16), ('neck3', 19, 16, 16), ('neck4', 16, 18, 19)]
    z = 35.0
    parent = body
    for name, d, w, h in neck:
        g = b.group(name, (0, Y, z), parent)
        b.box(g, name, 0, Y, z - d, w, h, d, ('body',))
        b.crests(g, z - d, d, Y + h / 2, min(0.6 * h, d / 2.6))
        parent = g
        z -= d
    assert abs(z - HEAD_JOINT) < 1e-6, z
    neck4 = parent

    # --- the head of our dragon (kept as it is) ---------------------------------------------------------------
    copy_subtree(b, old, 'head', neck4)

    # --- torso and arms ------------------------------------------------------------------------------------------
    torso = b.group('torso', (0, Y, 35), body)
    b.box(torso, 'torso', 0, Y, 35, 19, 19, 37, ('body',))
    b.crests(torso, 35, 37, Y + 9.5, 9.6)
    arms = b.group('arms', (0, Y, 46), torso)
    for side, sx in (('left', 1), ('right', -1)):
        def X(a, c):
            """x range a..c on the left side, mirrored on the right."""
            return (a, c) if sx > 0 else (-c, -a)
        sh = b.group('arm_' + side, (sx * 8, Y, 46), arms)
        x0, x1 = X(5.5, 12.5)
        b.cube(sh, 'shoulder', (x0, Y - 4.5, 41.5), (x1, Y + 4.5, 50.5), ('body',))
        up = b.group('arm_' + side + '2', (sx * 12, Y, 46), sh)
        x0, x1 = X(12, 31)
        b.cube(up, 'upper_arm', (x0, Y - 2, 44), (x1, Y + 2, 48), ('body',))
        x0, x1 = X(30, 33)
        b.cube(up, 'elbow', (x0, Y - 2.5, 43.5), (x1, Y + 2.5, 48.5), ('body',))
        fo = b.group('arm_' + side + '3', (sx * 32, Y, 46), up)
        x0, x1 = X(32, 50)
        b.cube(fo, 'forearm', (x0, Y - 2.5, 43.5), (x1, Y + 2.5, 48.5), ('body',))
        ha = b.group('hand_' + side, (sx * 50, Y, 46), fo)
        x0, x1 = X(49, 57)
        b.cube(ha, 'hand', (x0, Y - 3.5, 40), (x1, Y + 3.5, 52), ('body',))
        # three fingers (front, middle, back), two joints each, an eagle talon at the end
        for fname, fz, fy in (('front', 41.5, Y - 0.5), ('middle', 46, Y + 0.5), ('back', 50.5, Y - 0.5)):
            f1 = b.group('finger_%s_%s' % (fname, side), (sx * 57, fy, fz), ha)
            x0, x1 = X(57, 61)
            b.cube(f1, 'finger', (x0, fy - 1.75, fz - 1.75), (x1, fy + 1.75, fz + 1.75), ('body',))
            f2 = b.group('finger_%s_%s2' % (fname, side), (sx * 61, fy, fz), f1)
            x0, x1 = X(61, 64)
            b.cube(f2, 'finger', (x0, fy - 1.5, fz - 1.5), (x1, fy + 1.5, fz + 1.5), ('body',))
            x0, x1 = X(64, 67)
            b.cube(f2, 'talon', (x0, fy - 1.5, fz - 0.5), (x1, fy - 0.5, fz + 0.5), ('talon',),
                   origin=(sx * 64, fy - 1, fz), rotation=(0, 0, -30 * sx), color=9)

    # --- body: 3 times "ring, long, long, ring, thick" ---------------------------------------------------------
    rhythm = [(7, 14), (24, 16), (24, 16), (7, 14), (33, 19)]
    pieces = rhythm * 3
    pieces[-1] = (31, 19)
    z = 72.0
    parent = torso
    for i, (d, w) in enumerate(pieces):
        g = b.group('segment%d' % (i + 1), (0, Y, z), parent)
        b.box(g, 'segment%d' % (i + 1), 0, Y, z, w, w, d, ('body',))
        b.crests(g, z, d, Y + w / 2, min(0.6 * w, d / 2.6))
        parent = g
        z += d

    # --- tail: 5 tapering pieces, the fin of our dragon at its end ------------------------------------------------
    tail = [(7, 11), (24, 12), (21, 9), (21, 7), (21, 4)]
    for i, (d, w) in enumerate(tail):
        g = b.group('tail%d' % (i + 1), (0, Y, z), parent)
        b.box(g, 'tail%d' % (i + 1), 0, Y, z, w, w, d, ('body',))
        b.crests(g, z, d, Y + w / 2, max(1.5, min(0.6 * w, d / 2.6)))
        parent = g
        z += d
    end = z
    copy_fin(b, old, parent, end)
    return b, end


def old_index(old):
    els = {e['uuid']: e for e in old['elements']}
    grs = {g['uuid']: g for g in old['groups']}
    nodes = {}

    def walk(n):
        if isinstance(n, str):
            return
        nodes[grs[n['uuid']]['name']] = n
        for c in n['children']:
            walk(c)
    for n in old['outliner']:
        walk(n)
    return els, grs, nodes


def copy_subtree(b, old, name, parent):
    els, grs, nodes = old_index(old)

    def rec(node, par):
        g = grs[node['uuid']]
        u = b.group(g['name'], g['origin'], par, g.get('rotation') or (0, 0, 0))
        for c in node['children']:
            if isinstance(c, str):
                e = copy.deepcopy(els[c])
                e['uuid'] = uid()
                e.pop('faces', None)
                b.elements.append(e)
                b.tree[u].append(e['uuid'])
                b.paint[e['uuid']] = ('copy', els[c])
            else:
                rec(c, u)
    rec(nodes[name], parent)


def copy_fin(b, old, parent, end):
    """The two plates of the end of the old tail, moved to the end of the new one."""
    els, grs, nodes = old_index(old)
    tail = nodes['Tail']
    cubes = [els[c] for c in tail['children'] if isinstance(c, str)]
    old_end = max(e['to'][2] for e in cubes)
    for e in cubes:
        if not e.get('rotation') or not any(e['rotation']):
            continue
        n = copy.deepcopy(e)
        n['uuid'] = uid()
        n.pop('faces', None)
        dz = end - old_end
        for k in ('from', 'to', 'origin'):
            n[k] = [n[k][0], n[k][1], n[k][2] + dz]
        b.elements.append(n)
        b.tree[parent].append(n['uuid'])
        b.paint[n['uuid']] = ('copy', e)


# ------------------------------------------------------------------------------------------------------------
# Texture
# ------------------------------------------------------------------------------------------------------------

def dims(e):
    return [e['to'][i] - e['from'][i] for i in range(3)]


def face_rects(u, v, w, h, d):
    """Box UV faces (Blockbench layout), as (x, y, width, height) with integer edges."""
    def R(a, b_, c, e):
        x0, y0 = int(math.floor(a)), int(math.floor(b_))
        return (x0, y0, max(1, int(math.ceil(c)) - x0), max(1, int(math.ceil(e)) - y0))
    return {'up': R(u + d, v, u + d + w, v + d), 'down': R(u + d + w, v, u + d + 2 * w, v + d),
            'east': R(u, v + d, u + d, v + d + h), 'north': R(u + d, v + d, u + d + w, v + d + h),
            'west': R(u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': R(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


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


def paint(b, old_img, old):
    els, _, _ = old_index(old)
    tw = 512
    used = pack(b.elements, tw)
    th = 256
    while th < used:
        th *= 2
    img = Image.new('RGBA', (tw, th), (0, 0, 0, 0))
    src = old_img.convert('RGBA')

    # tiles of the old body: bodyPart_0 (16 x 16 x 32, uv 0,0)
    o = face_rects(0, 0, 16, 16, 32)
    tiles = {k: src.crop((r[0], r[1], r[0] + r[2], r[1] + r[3])) for k, r in o.items()}
    crest_src = [e for e in old['elements'] if e['name'] == 'crest_a'][0]
    talon_src = [e for e in old['elements'] if e['name'] == 'talon_middle'][0]

    def tile(dst, rect, t, flip=False):
        x0, y0, w, h = rect
        if flip:
            t = t.transpose(Image.Transpose.ROTATE_90)
        ox, oy = rng.randrange(t.width), rng.randrange(t.height)
        for yy in range(h):
            for xx in range(w):
                dst.putpixel((x0 + xx, y0 + yy), t.getpixel(((xx + ox) % t.width, (yy + oy) % t.height)))

    def scaled(dst, rect, se, face):
        w, h, d = dims(se)
        r = face_rects(se['uv_offset'][0], se['uv_offset'][1], w, h, d)[face]
        part = src.crop((r[0], r[1], r[0] + r[2], r[1] + r[3])).resize((rect[2], rect[3]), Image.NEAREST)
        dst.paste(part, rect[:2])

    for e in b.elements:
        w, h, d = dims(e)
        rects = face_rects(e['uv_offset'][0], e['uv_offset'][1], w, h, d)
        kind = b.paint[e['uuid']]
        if kind[0] == 'copy':
            se = kind[1]
            sw, sh, sd = dims(se)
            srects = face_rects(se['uv_offset'][0], se['uv_offset'][1], sw, sh, sd) if se.get('uv_offset') else \
                face_rects(0, 0, sw, sh, sd)
            for f, r in rects.items():
                s = srects[f]
                img.paste(src.crop((s[0], s[1], s[0] + s[2], s[1] + s[3])).resize((r[2], r[3]), Image.NEAREST), r[:2])
        elif kind[0] == 'crest':
            for f, r in rects.items():
                scaled(img, r, crest_src, f)
        elif kind[0] == 'talon':
            for f, r in rects.items():
                scaled(img, r, talon_src, f)
        else:
            for f, r in rects.items():
                if f == 'up':
                    tile(img, r, tiles['up'])
                elif f == 'down':
                    tile(img, r, tiles['down'])
                elif f in ('east', 'west'):
                    tile(img, r, tiles['east'])
                else:
                    tile(img, r, tiles['north'])
    return img, tw, th


def main():
    old = json.load(open(OLD, encoding='utf-8'))
    old_img = Image.open(io.BytesIO(base64.b64decode(old['textures'][0]['source'].split(',', 1)[1])))
    b, end = build(old)
    img, tw, th = paint(b, old_img, old)
    buf = io.BytesIO()
    img.save(buf, 'PNG')

    def outline(u):
        return {'uuid': u, 'isOpen': False, 'children': [c if c not in b.groups else outline(c) for c in b.tree[u]]}

    new = {k: copy.deepcopy(old[k]) for k in old if k not in ('elements', 'groups', 'outliner', 'textures')}
    new['name'] = 'dragon_tempete_alpha_v2'
    new['model_identifier'] = 'dragon_tempete_alpha_v2'
    new['resolution'] = {'width': tw, 'height': th}
    new['elements'] = b.elements
    new['groups'] = list(b.groups.values())
    new['outliner'] = [outline(u) for u in b.roots]
    tex = {k: v for k, v in old['textures'][0].items()}
    tex.update({'name': 'dragon_tempete_v2', 'uuid': uid(), 'width': tw, 'height': th, 'uv_width': tw, 'uv_height': th,
                'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()})
    new['textures'] = [tex]
    for e in b.elements:
        w, h, d = dims(e)
        u, v = e['uv_offset']
        rects = face_rects(u, v, w, h, d)
        e['faces'] = {f: {'uv': [r[0], r[1], r[0] + r[2], r[1] + r[3]], 'texture': 0} for f, r in rects.items()}
    json.dump(new, open(NEW, 'w', encoding='utf-8'))
    img.save(os.path.join(HERE, 'models', 'dragon_tempete_alpha_v2.png'))
    print('%d cubes, %d groups, texture %dx%d, tail end z=%.1f (head front z=-68): length %.0f px'
          % (len(b.elements), len(b.groups), tw, th, end, end + 68))


if __name__ == '__main__':
    main()
