"""Textures of the Tempest Dragon (alpha of the storm), from its texture painted in Blockbench:
  - storm_serpent_glow.png: the parts that shine in the dark (the lightning cracks, the eyes, the crests, the
    whiskers, the nostrils, the talons and the tips of the horns), drawn full bright over the model;
  - storm_clouds.png: the storm clouds it rests on and holds in its talons.
Run from the project folder:  python tools/gen_storm_textures.py
"""
import os
import random

from PIL import Image

ROOT = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'entity', 'alpha')


def is_lightning(r, g, b):
    # the blue of the lightning (the body is grey-black, the belly grey: their blue is close to their red)
    return (b > 150 and b - r > 60) or (r > 180 and g > 220 and b > 220)


def glow():
    src = Image.open(os.path.join(ROOT, 'storm_serpent.png')).convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    n = 0
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = sp[x, y]
            if a > 0 and is_lightning(r, g, b):
                op[x, y] = (r, g, b, 255)
                n += 1
    out.save(os.path.join(ROOT, 'storm_serpent_glow.png'))
    print('glow:', n, 'pixels')


def clouds():
    rnd = random.Random(7)
    w, h = 256, 128
    img = Image.new('RGBA', (w, h))
    p = img.load()
    # soft blobs of grey: storm clouds, a little darker underneath
    base = [[0.0] * w for _ in range(h)]
    for _ in range(520):
        cx, cy, rad = rnd.uniform(0, w), rnd.uniform(0, h), rnd.uniform(3, 10)
        v = rnd.uniform(-1, 1)
        for y in range(max(0, int(cy - rad)), min(h, int(cy + rad) + 1)):
            for x in range(max(0, int(cx - rad)), min(w, int(cx + rad) + 1)):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / rad
                if d < 1:
                    base[y][x] += v * (1 - d) ** 2
    for y in range(h):
        for x in range(w):
            t = max(-1.0, min(1.0, base[y][x]))
            grey = int(196 + 34 * t + rnd.uniform(-6, 6))
            blue = min(255, grey + 10)
            p[x, y] = (grey, grey + 2, blue, int(205 + 30 * t))
    img.save(os.path.join(ROOT, 'storm_clouds.png'))
    print('clouds:', img.size)


if __name__ == '__main__':
    glow()
    clouds()
