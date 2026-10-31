"""Draws the Tempest Dragon (alpha of the storm) with its real model, its texture and the poses of StormSpine
(Python twin: storm_spine.py), to check the animation and to make the pictures of the codex and of the docs.
Needs numpy and Pillow. Run from the project folder:  python tools/storm_preview.py
  -> src/main/resources/assets/bdragon/textures/gui/codex/alpha_storm.png
  -> docs/apercu_dragon_tempete.png
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import storm_model  # noqa: E402
import storm_spine  # noqa: E402

TEX = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'entity', 'alpha', 'storm_serpent.png')
K = 1.25  # AlphaStorm.MODEL_SCALE


def rot_zyx(x, y, z):
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def mat(offset, rot):
    m = np.eye(4)
    m[:3, :3] = rot_zyx(*rot)
    m[:3, 3] = offset
    return m


def cube_quads(c, tw, th):
    """The 6 faces of a box with the box UV of ModelPart.Cube."""
    u, v, x0, y0, z0, w, h, d = c
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    vs = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    f4, f5, f6, f7 = u, u + d, u + d + w, u + d + w + w
    f8, f9 = u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    polys = [((5, 4, 0, 1), f5, f10, f6, f11), ((2, 3, 7, 6), f6, f11, f7, f10), ((0, 4, 7, 3), f4, f11, f5, f12),
             ((1, 0, 3, 2), f5, f11, f6, f12), ((5, 1, 2, 6), f6, f11, f8, f12), ((4, 5, 6, 7), f8, f11, f9, f12)]
    out = []
    for idx, u1, v1, u2, v2 in polys:
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        out.append(([vs[i] for i in idx], [(a / tw, b / th) for a, b in uvs]))
    return out


class Renderer:
    def __init__(self, size):
        self.tex = np.asarray(Image.open(TEX).convert('RGBA')).astype(np.float32) / 255
        self.w, self.h = size
        self.color = np.zeros((self.h, self.w, 3), np.float32)
        self.alpha = np.zeros((self.h, self.w), np.float32)
        self.depth = np.full((self.h, self.w), np.inf, np.float32)

    def camera(self, eye, target, fov=40):
        eye, target = np.array(eye, float), np.array(target, float)
        f = target - eye
        f /= np.linalg.norm(f)
        r = np.cross(f, np.array([0, 1, 0.0]))
        r /= np.linalg.norm(r)
        self.view = (eye, r, np.cross(r, f), f)
        self.scale = self.h / (2 * math.tan(math.radians(fov) / 2))

    def project(self, p):
        eye, r, u, f = self.view
        q = p - eye
        x, y, z = q @ r, q @ u, q @ f
        return np.stack([self.w / 2 + x / z * self.scale, self.h / 2 - y / z * self.scale, z], -1)

    def quad(self, pts, uvs, light, tint=None):
        pr = self.project(np.array(pts))
        if np.any(pr[:, 2] <= 0.1):
            return
        for tri in ((0, 1, 2), (0, 2, 3)):
            self.tri(pr[list(tri)], np.array([uvs[i] for i in tri]), light, tint)

    def tri(self, pr, uv, light, tint):
        xs, ys = pr[:, 0], pr[:, 1]
        x0, x1 = int(max(0, math.floor(xs.min()))), int(min(self.w - 1, math.ceil(xs.max())))
        y0, y1 = int(max(0, math.floor(ys.min()))), int(min(self.h - 1, math.ceil(ys.max())))
        if x0 > x1 or y0 > y1:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        (ax, ay), (bx, by), (cx, cy) = pr[0, :2], pr[1, :2], pr[2, :2]
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-9:
            return
        l0 = ((by - cy) * (gx - cx) + (cx - bx) * (gy - cy)) / den
        l1 = ((cy - ay) * (gx - cx) + (ax - cx) * (gy - cy)) / den
        l2 = 1 - l0 - l1
        inside = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
        if not inside.any():
            return
        z = 1 / (l0 / pr[0, 2] + l1 / pr[1, 2] + l2 / pr[2, 2])
        w0, w1, w2 = l0 / pr[0, 2] * z, l1 / pr[1, 2] * z, l2 / pr[2, 2] * z
        sub_d = self.depth[y0:y1 + 1, x0:x1 + 1]
        sub_c = self.color[y0:y1 + 1, x0:x1 + 1]
        sub_a = self.alpha[y0:y1 + 1, x0:x1 + 1]
        if tint is not None:
            ok = inside & (z < sub_d)
            a = (ok * tint[3])[..., None]
            sub_c[:] = sub_c * (1 - a) + np.array(tint[:3], np.float32) * light * a
            sub_a[:] = sub_a + a[..., 0] * (1 - sub_a)
            return
        u = w0 * uv[0, 0] + w1 * uv[1, 0] + w2 * uv[2, 0]
        v = w0 * uv[0, 1] + w1 * uv[1, 1] + w2 * uv[2, 1]
        th, tw = self.tex.shape[:2]
        texel = self.tex[np.clip((v * th).astype(int), 0, th - 1), np.clip((u * tw).astype(int), 0, tw - 1)]
        ok = inside & (texel[..., 3] > 0.1) & (z < sub_d)
        sub_d[ok] = z[ok]
        sub_c[ok] = (texel[..., :3] * light)[ok]
        sub_a[ok] = 1

    def image(self, background=None):
        rgb = (np.clip(self.color, 0, 1) * 255).astype(np.uint8)
        a = (np.clip(self.alpha, 0, 1) * 255).astype(np.uint8)
        img = Image.fromarray(np.dstack([rgb, a]), 'RGBA')
        if background is not None:
            bg = Image.new('RGBA', img.size, background)
            bg.alpha_composite(img)
            return bg
        return img


def walk(part, m, out, tw, th, overrides):
    off, rot = overrides.get(id(part), (part.offset, part.rot))
    mp = m @ mat(off, rot)
    for c in part.cubes:
        for pts, uvs in cube_quads(c, tw, th):
            out.append(([(mp @ np.array([*p, 1.0]))[:3] for p in pts], uvs))
    for ch in part.children:
        walk(ch, mp, out, tw, th, overrides)


def to_world():
    """Minecraft model space (pixels, y down, front -z) -> blocks (x left, y up, z forward), feet at 0."""
    k = K / 16.0
    m = np.diag([k, -k, -k, 1.0])
    m[1, 3] = 24 * k
    return m


LIGHT = np.array([0.4, 0.8, 0.45]) / np.linalg.norm([0.4, 0.8, 0.45])


def draw_model(r, bones, pose, tw, th):
    quads = []
    for b in bones:
        walk(b, to_world(), quads, tw, th, storm_spine.overrides(pose, bones))
    for pts, uvs in quads:
        pts = np.array(pts)
        n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
        nl = np.linalg.norm(n)
        light = 0.55 + 0.45 * abs(n @ LIGHT) / nl if nl > 1e-9 else 0.8
        r.quad(pts, uvs, light)


def box(r, center, size, color, light=1.0):
    cx, cy, cz = center
    sx, sy, sz = size[0] / 2, size[1] / 2, size[2] / 2
    v = [(cx + a * sx, cy + b * sy, cz + c * sz) for a in (-1, 1) for b in (-1, 1) for c in (-1, 1)]
    for f in ((0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)):
        shade = light * (1.0 if f in ((2, 3, 7, 6),) else 0.85)
        r.quad(np.array([v[i] for i in f]), [(0, 0)] * 4, shade, tint=color)


def model_to_blocks(p):
    k = K / 16.0
    return (p[0] * k, (24 - p[1]) * k, -p[2] * k)


# same numbers as StormCloudModel.java
GRIP_PUFF = [(12, 8, 12, 0, 0, 0), (8, 7, 8, 6, -1, 2.5), (8, 7, 8, -6, -0.5, 2), (7, 5, 7, 1.5, 2, 7.5), (7, 5, 7, -1.5, -3, -5)]
_BED_UNUSED = [(48, 14, 48, 0, 0, 0), (34, 12, 34, 34, 1, 0), (34, 11, 34, 34, 2, 1.05), (34, 12, 34, 34, 0, 2.1),
            (34, 11, 34, 34, 1.5, 3.15), (34, 12, 34, 34, 0.5, 4.2), (34, 11, 34, 34, 2, 5.25), (24, 9, 24, 55, 3, 0.5),
            (24, 8, 24, 55, 4, 2.6), (24, 9, 24, 55, 3.5, 4.7), (18, 7, 18, 25, 6, 1.6), (18, 7, 18, 25, 6.5, 4.8)]
CLOUD = (0.80, 0.82, 0.86, 0.82)


def draw_clouds(r, pose, coil, air, stream):
    """The puffs of StormCloudModel in the talons (same places, without their slow motion): only when it moves."""
    k = K / 16.0
    hold = air * (1 - coil)
    if hold < 0.03:
        return
    for side in (0, 1):
        g = pose.grip(side)
        for i, (w, h, d, px, py, pz) in enumerate(GRIP_PUFF):
            p = (g[0] + px, g[1] + py, g[2] + pz + stream * (2.0 + i * 2.2))
            box(r, model_to_blocks(p), (w * k * hold, h * k * hold, d * k * hold * (1 + 0.5 * stream)), CLOUD)


def draw_wind(r, coil, air, age):
    """The wind of the tornado that holds it still in the air (the particles of ClientAnimators, drawn as streaks)."""
    k = K / 16.0
    rest = coil * air
    if rest < 0.03:
        return
    import random
    rnd = random.Random(3)
    for i in range(70):
        t = rnd.random()
        radius, height = storm_spine.tornado(t)
        rr = (radius + 10 + rnd.random() * 14) * k
        a = rnd.random() * 2 * math.pi
        y = height * k + (rnd.random() - 0.5) * 1.0
        # a short streak along the turn of the wind
        a2 = a + 0.35
        p0 = (math.sin(a) * rr, y, math.cos(a) * rr)
        p1 = (math.sin(a2) * rr, y + 0.25, math.cos(a2) * rr)
        wv = 0.12
        quad = [(p0[0], p0[1] - wv, p0[2]), (p1[0], p1[1] - wv, p1[2]), (p1[0], p1[1] + wv, p1[2]), (p0[0], p0[1] + wv, p0[2])]
        r.quad(np.array(quad), [(0, 0)] * 4, 1.0, tint=(0.92, 0.95, 1.0, 0.55 * rest))


def scene(size, eye, target, weights, fov=40, ss=2, clouds=True, ground=False, **kw):
    model, bones = storm_model.build()
    tw, th = model.texture
    r = Renderer((size[0] * ss, size[1] * ss))
    r.camera(eye, target, fov)
    if ground:
        for gx in range(-24, 24, 2):
            for gz in range(-34, 14, 2):
                if (gx // 2 + gz // 2) % 2 == 0:
                    r.quad(np.array([(gx, 0, gz), (gx + 2, 0, gz), (gx + 2, 0, gz + 2), (gx, 0, gz + 2)], float), [(0, 0)] * 4,
                           1.0, tint=(0.80, 0.84, 0.80, 1.0))
        r.depth[:] = np.inf
    air = 0.0 if ground else 1.0
    pose = storm_spine.solve(weights, air=air, **kw)
    draw_model(r, bones, pose, tw, th)
    if clouds and air > 0:
        draw_clouds(r, pose, weights[0], air, weights[2] + weights[3])
        draw_wind(r, weights[0], air, kw.get('age', 0))
    img = r.image()
    return img.resize(size, Image.LANCZOS) if ss > 1 else img


def main():
    # the picture of the codex: rearing on its cloud, on the watch
    codex = scene((378, 252), (-15, 4, 15), (0.3, 2.0, 0.5), (1, 0, 0, 0), fov=40, ss=3, age=10, head_yaw=18, head_pitch=6)
    codex.save(os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'gui', 'codex', 'alpha_storm.png'))

    # the sheet of the docs: its 4 states
    w, h = 640, 400
    panels = [
        ('Immobile en l\'air : la tornade (tête et bras au centre, corps en cône)',
         scene((w, h), (16, 5, 16), (0, 1.6, 0), (1, 0, 0, 0), fov=60, age=10, head_yaw=-10, head_pitch=5)),
        ('Au sol : l\'avant dressé, des bosses courent le long du corps',
         scene((w, h), (26, 11, -6), (0, 2, -15), (0, 1, 0, 0), fov=64, ground=True, phase=1.0)),
        ('En vol : une vague court le long du corps (ici il monte)',
         scene((w, h), (30, 10, -12), (0, 8, -13), (0, 0, 1, 0), fov=70, phase=1.0, climb=25)),
        ('Piqué : droit comme une flèche, bras plaqués en arrière',
         scene((w, h), (34, 12, -8), (0, 14, -9), (0, 0, 0, 1), fov=78, phase=2.0, climb=-55)),
    ]
    sheet = Image.new('RGBA', (w * 2, h * 2 + 60), (236, 240, 245, 255))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype('DejaVuSans.ttf', 15)
    except OSError:
        font = None
    d.text((16, 12), 'Bdragon - le Dragon des Tempêtes (alpha de la Tempête), dessiné avec le vrai modèle', fill=(30, 40, 60, 255), font=font)
    for i, (title, img) in enumerate(panels):
        x, y = (i % 2) * w, 40 + (i // 2) * h
        bg = Image.new('RGBA', img.size, (236, 240, 245, 255))
        bg.alpha_composite(img)
        sheet.paste(bg, (x, y))
        d.text((x + 12, y + 8), title, fill=(30, 40, 60, 255), font=font)
    sheet.convert('RGB').save(os.path.join('docs', 'apercu_dragon_tempete.png'))
    print('pictures written')


if __name__ == '__main__':
    main()
