"""Paints the texture of the Sylvan Tortoise (alpha of the forest) and its glow texture.

The layout follows the UV_* constants of TortoiseModel.java (texture of 256 x 256 units, painted at 2 texels
per unit: 512 x 512). Every box of the model has the usual Minecraft layout from its corner (u, v):
top (u+d, v), bottom (u+d+w, v), then the row of the sides at v+d: right (u), front (u+d), left (u+d+w),
back (u+2d+w).

Run from the project folder:  python tools/gen_tortoise.py"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'entity', 'alpha')
S = 2           # texels per unit
SIZE = 256 * S

rng = np.random.default_rng(20261025)


def rgb(h):
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


SHELL = [rgb(c) for c in ('132611', '24451f', '35652b', '4f8638', '86b552')]
GOLD = [rgb(c) for c in ('6b4a0a', 'c99a2e', 'ffe27a')]
STONE = [rgb(c) for c in ('2b2a26', '4a4944', '66655c', '858376', 'a09d8e')]
MOSS = [rgb(c) for c in ('1f4416', '2f5f1f', '4a8a2c', '6fb043', 'a8d860')]
SKIN = [rgb(c) for c in ('1f2a17', '34482c', '4e6a3e', '6d8a55', '93aa74')]
BEAK = [rgb(c) for c in ('3a3624', '5c5638', '8a8256', 'b8ad7a')]
CLOUD = [rgb(c) for c in ('c4cfdd', 'e1e8f2', 'f4f7fd', 'ffffff')]
FLOWERS = [rgb(c) for c in ('e04040', 'f0e060', '70a0ff', 'ffffff', 'f08ad0')]

tex = np.zeros((SIZE, SIZE, 4), dtype=float)
glow = np.zeros((SIZE, SIZE, 4), dtype=float)


def ramp(pal, t):
    """Colours along a palette, t in 0..1 (array)."""
    t = np.clip(np.asarray(t, dtype=float), 0, 0.999) * (len(pal) - 1)
    i = t.astype(int)
    f = (t - i)[..., None]
    p = np.stack(pal)
    return p[i] * (1 - f) + p[np.minimum(i + 1, len(pal) - 1)] * f


def faces(u, v, w, h, d):
    """Rectangles (in texels) of the faces of a box: name -> (x0, y0, width, height)."""
    return {
        'top': ((u + d) * S, v * S, w * S, d * S),
        'bottom': ((u + d + w) * S, v * S, w * S, d * S),
        'right': (u * S, (v + d) * S, d * S, h * S),
        'front': ((u + d) * S, (v + d) * S, w * S, h * S),
        'left': ((u + d + w) * S, (v + d) * S, d * S, h * S),
        'back': ((u + 2 * d + w) * S, (v + d) * S, w * S, h * S),
    }


def place(rect, colors, alpha=None, target=None):
    x0, y0, w, h = (int(round(k)) for k in rect)
    t = tex if target is None else target
    t[y0:y0 + h, x0:x0 + w, :3] = np.clip(colors[:h, :w], 0, 255)
    t[y0:y0 + h, x0:x0 + w, 3] = 255 if alpha is None else alpha[:h, :w]


def voronoi(w, h, cols, rows, jitter=0.35, stagger=False):
    """Cells on a jittered grid: index of the cell, distance to the nearest seed and to the seam."""
    seeds = []
    for r in range(rows):
        for c in range(cols):
            off = 0.5 if stagger and r % 2 else 0.0
            sx = (c + 0.5 + off + rng.uniform(-jitter, jitter)) * w / cols
            sy = (r + 0.5 + rng.uniform(-jitter, jitter)) * h / rows
            seeds.append((sx, sy))
    # extra seeds around the borders so the cells at the edges stay closed
    seeds = np.array(seeds)
    ys, xs = np.mgrid[0:h, 0:w] + 0.5
    d = np.sqrt((xs[..., None] - seeds[:, 0]) ** 2 + (ys[..., None] - seeds[:, 1]) ** 2)
    order = np.argsort(d, axis=-1)
    idx = order[..., 0]
    d1 = np.take_along_axis(d, order[..., :1], -1)[..., 0]
    d2 = np.take_along_axis(d, order[..., 1:2], -1)[..., 0]
    return idx, d1, d2 - d1


def plates(w, h, cols, rows, pal, seam_pal, lo=0.35, hi=0.8, rings=False, bevel=True, stagger=False, seam=1.6):
    idx, d1, edge = voronoi(w, h, cols, rows, stagger=stagger)
    tint = rng.uniform(-0.08, 0.08, idx.max() + 1)[idx]
    noise = rng.uniform(-0.06, 0.06, (h, w))
    t = lo + (hi - lo) * np.clip(edge / (max(w / cols, h / rows) * 0.5), 0, 1) ** 0.6 + tint + noise
    if rings:
        t += 0.07 * np.cos(edge * 1.25)
    if bevel:
        ys = np.mgrid[0:h, 0:w][0]
        t += 0.06 * (0.5 - ys / max(1, h))
    col = ramp(pal, t)
    seam_mask = edge < seam
    col[seam_mask] = ramp(seam_pal, rng.uniform(0.1, 0.45, seam_mask.sum()))
    return col


def stone(w, h, moss_rows=0):
    col = plates(w, h, max(1, w // 10), max(1, h // 8), STONE, STONE[:2], lo=0.3, hi=0.75, seam=1.0)
    if moss_rows:
        col[:moss_rows] = ramp(MOSS, rng.uniform(0.25, 0.75, (min(moss_rows, h), w)))
    return col


def moss(w, h, flowers=True):
    t = rng.uniform(0.25, 0.8, (h, w))
    # darker and lighter patches
    ys, xs = np.mgrid[0:h, 0:w]
    t += 0.12 * np.sin(xs * 0.21 + np.sin(ys * 0.17) * 2) * np.cos(ys * 0.13)
    col = ramp(MOSS, t)
    if flowers:
        n = int(w * h * 0.012)
        for _ in range(n):
            x, y = rng.integers(0, w), rng.integers(0, h)
            col[y, x] = FLOWERS[rng.integers(0, len(FLOWERS))]
        blades = rng.random((h, w)) < 0.05
        col[blades] = MOSS[4]
    return col


def skin(w, h, light=False):
    """Small rough scales (low contrast: they must read as skin, not as cobblestone)."""
    col = plates(w, h, max(1, w // 4), max(1, h // 4), SKIN[1:] if light else SKIN, SKIN[1:3], lo=0.4, hi=0.8, stagger=True, seam=0.6)
    return col


def scutes_side(w, h, cells, gold_rows=0):
    """Side band of the shell: a row of plates with a bevel (and the golden line of the rim)."""
    col = plates(w, h, cells, 1, SHELL, SHELL[:2], lo=0.42, hi=0.9, rings=True, seam=1.4)
    ys = np.mgrid[0:h, 0:w][0]
    col *= (1.08 - 0.25 * ys / max(1, h))[..., None]
    col[h - 1:] = SHELL[0]
    if gold_rows:
        gt = np.linspace(1.0, 0.45, gold_rows)[:, None] + rng.uniform(-0.08, 0.08, (gold_rows, w))
        col[:gold_rows] = ramp(GOLD, gt)
    return col


def paint_box(u, v, w, h, d, side=None, top=None, bottom=None, front=None, back=None):
    f = faces(u, v, w, h, d)
    for name, fn in (('top', top), ('bottom', bottom), ('right', side), ('left', side),
                     ('front', front or side), ('back', back or side)):
        if fn is None:
            continue
        x0, y0, fw, fh = (int(k) for k in f[name])
        place(f[name], fn(fw, fh))
    return f


def main():
    # --- belly: the rocky plastron (bottom face), stone on its sides -----------------------------------------
    paint_box(0, 0, 36, 4, 39, side=lambda w, h: stone(w, h), top=lambda w, h: np.full((h, w, 3), SHELL[0]),
              bottom=lambda w, h: plates(w, h, 2, 5, STONE, [MOSS[0], MOSS[1], STONE[0]], lo=0.3, hi=0.85, seam=2.2))
    # rocks (and the mossy rocks of the sides): 40 x 14 from (150, 0)
    place((150 * S, 0, 40 * S, 14 * S), stone(40 * S, 14 * S, moss_rows=4))
    # clouds: 40 x 16 from (190, 0), soft and translucent
    ch, cw = 16 * S, 40 * S
    ys = np.mgrid[0:ch, 0:cw][0]
    ct = 0.95 - 0.55 * (ys % 24) / 24 + rng.uniform(-0.08, 0.08, (ch, cw))
    place((190 * S, 0, cw, ch), ramp(CLOUD, ct), alpha=rng.uniform(205, 240, (ch, cw)))
    # claws, eyes, bees, wings
    place((230 * S, 0, 8 * S, 4 * S), ramp([rgb('e0d6b8'), rgb('b0a585')], np.mgrid[0:8, 0:16][0] / 8))
    eye = np.tile(rgb('d8a01a'), (8, 16, 1))
    eye[2:6, 4:8] = rgb('1a1205')
    eye[2:6, 12:16] = rgb('1a1205')
    place((238 * S, 0, 16, 8), eye)
    bee = np.tile(rgb('f2c02a'), (8, 16, 1))
    bee[:, 4:8] = rgb('2a2010')
    bee[:, 12:16] = rgb('2a2010')
    place((230 * S, 4 * S, 16, 8), bee)
    place((238 * S, 4 * S, 16, 8), np.tile(rgb('e8f4ff'), (8, 16, 1)))
    # twigs, leaves, lianas
    place((230 * S, 8 * S, 16, 12), ramp([rgb('3a2412'), rgb('5a3a1e'), rgb('7a5530')], rng.uniform(0.2, 0.9, (12, 16))))
    place((238 * S, 8 * S, 24, 12), ramp([rgb('245a16'), rgb('3f8f2a'), rgb('6fbf3f'), rgb('a0e060')], rng.uniform(0.15, 0.9, (12, 24))))
    place((250 * S, 8 * S, 8, 12), ramp([rgb('1f4f16'), rgb('2f6f20'), rgb('4f9a30')], rng.uniform(0.15, 0.9, (12, 8))))

    # --- rim of the shell: marginal scutes, a golden line on top ----------------------------------------------
    paint_box(0, 43, 40, 6, 43, side=lambda w, h: scutes_side(w, h, max(2, w // 16), gold_rows=2),
              top=lambda w, h: np.full((h, w, 3), SHELL[1]),
              bottom=lambda w, h: ramp(SHELL, rng.uniform(0.05, 0.3, (h, w))))
    # --- the shell: hexagonal scutes on the top, rows of plates on the sides -----------------------------------
    paint_box(0, 92, 36, 5, 39, top=lambda w, h: plates(w, h, 5, 6, SHELL, SHELL[:2], lo=0.4, hi=0.92, rings=True, stagger=True),
              bottom=lambda w, h: np.full((h, w, 3), SHELL[0]))
    paint_box(0, 92, 30, 4, 33, side=lambda w, h: scutes_side(w, h, max(2, w // 22)))
    paint_box(0, 92, 36, 5, 39, side=lambda w, h: scutes_side(w, h, max(2, w // 24)))
    # --- moss of the back (and the crown of moss of the head, the tuft of the tail) ----------------------------
    place((0, 136 * S, 120 * S, 33 * S), moss(120 * S, 33 * S))
    paint_box(0, 136, 28, 2, 31, side=lambda w, h: moss(w, h, flowers=False) * (1 - 0.25 * np.mgrid[0:h, 0:w][0][..., None] / max(1, h)),
              top=lambda w, h: moss(w, h))
    # --- head: skull (12 x 9 x 11), beak, hook, brows, jaw -------------------------------------------------------
    paint_box(166, 43, 12, 9, 11, side=lambda w, h: skin(w, h), top=lambda w, h: skin(w, h), bottom=lambda w, h: skin(w, h, light=True))
    bw, bh = 22 * S, 6 * S
    place((212 * S, 43 * S, bw, bh), ramp(BEAK, 0.15 + 0.7 * (np.mgrid[0:bh, 0:bw][1] % 11) / 11 + rng.uniform(-0.05, 0.05, (bh, bw))))
    place((234 * S, 43 * S, 10 * S, 2 * S), np.tile(BEAK[3], (2 * S, 10 * S, 1)))
    place((212 * S, 49 * S, 16 * S, 5 * S), stone(16 * S, 5 * S))
    paint_box(166, 63, 11, 2, 10, side=lambda w, h: skin(w, h, light=True), top=lambda w, h: skin(w, h), bottom=lambda w, h: skin(w, h, light=True))
    place((208 * S, 63 * S, 18 * S, 3 * S), ramp(BEAK, rng.uniform(0.25, 0.7, (3 * S, 18 * S))))
    # --- skin of the neck, legs and tail; soles of the feet ------------------------------------------------------
    place((166 * S, 75 * S, 40 * S, 17 * S), skin(40 * S, 17 * S))
    foot = skin(42 * S, 12 * S)
    foot[:20] = ramp(SKIN, rng.uniform(0.05, 0.4, (20, 42 * S)))
    place((166 * S, 92 * S, 42 * S, 12 * S), foot)

    # --- glow: eyes, runes in the seams of the shell, a few shining spores in the moss ---------------------------
    eyes = np.tile(rgb('b5ff6b'), (8, 16, 1))
    eyes[2:6, 4:8] = rgb('f6ffd0')
    eyes[2:6, 12:16] = rgb('f6ffd0')
    place((238 * S, 0, 16, 8), eyes, target=glow)
    f = faces(0, 92, 36, 5, 39)
    for name in ('right', 'front', 'left', 'back'):
        x0, y0, w, h = (int(k) for k in f[name])
        for x in range(9, w, 24):
            for y in range(2, h - 2):
                if (x // 24 + y) % 4 != 0:
                    glow[y0 + y, x0 + x, :3] = rgb('5fd03a') * 0.8
                    glow[y0 + y, x0 + x, 3] = 255
    for _ in range(60):
        x = rng.integers(0, 120 * S)
        y = rng.integers(136 * S, 169 * S)
        glow[y, x, :3] = rgb('d8ff8a') * 0.7
        glow[y, x, 3] = 255

    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(OUT, 'forest_tortoise.png'))
    Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(OUT, 'forest_tortoise_glow.png'))
    print('tortoise textures painted')


if __name__ == '__main__':
    main()
