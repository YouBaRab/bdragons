"""Draws the 16x16 textures of the Alpha Scales and the Orbs of Power (one of each per race) and writes
their item definitions and models. Run from the project folder:  python tools/gen_alpha_items.py"""
import json, math, os
from PIL import Image

ROOT = os.path.join('src', 'main', 'resources', 'assets', 'bdragon')
# dark, mid, light, highlight (+ accent of the orb core)
PALETTES = {
    'fire':      ('5a1406', 'c23a10', 'ff8a2a', 'fff0a0'),
    'ice':       ('2f6f99', '6fb8e6', 'bfeaff', 'ffffff'),
    'water':     ('0b2a5c', '1f63b0', '45c0e6', 'c8fbff'),
    'forest':    ('1d4a17', '3c8d2f', '8fd14f', 'e5ffb0'),
    'nether':    ('3d0808', '8e1b1b', 'e0471e', 'ffb04a'),
    'end':       ('160c22', '4b2a6b', 'a45ee0', 'f0c8ff'),
    'terra':     ('2a1a08', '6e4313', 'a8702c', 'f0d080'),
    'sunlight':  ('7a2a00', 'd86a00', 'ffb030', 'fff3c0'),
    'storm':     ('1c2224', '566166', '5f8dff', 'eef6ff'),
    'moonlight': ('0a0f24', '3a4a7a', 'b8c6e8', 'ffffff'),
    'wither':    ('050606', '1c1f1f', '2f5bd0', '8fe0ff'),
}
GOLD = ('6b4a0a', 'c99a2e', 'ffe27a')
OUTLINE = (18, 14, 20, 255)


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def ramp(pal, t):
    """0 = dark ... 1 = highlight."""
    cols = [rgb(c) for c in pal]
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    return mix(cols[i], cols[i + 1], t - i)


def scale_texture(pal):
    """A shield-shaped scale pointing down, with a golden rim (the mark of the alpha) and a central ridge."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    def inside(x, y):
        # rounded top (y 1..6), then a point at the bottom (y 14)
        cx = 7.5
        if y < 1 or y > 14:
            return False
        if y <= 6:
            r = 6.4
            return (x - cx) ** 2 + ((y - 6.5) * 1.15) ** 2 <= r * r
        half = 6.4 * (14.5 - y) / 8.0
        return abs(x - cx) <= half
    for y in range(16):
        for x in range(16):
            if not inside(x, y):
                continue
            edge = not (inside(x - 1, y) and inside(x + 1, y) and inside(x, y - 1) and inside(x, y + 1))
            if edge:
                px[x, y] = OUTLINE
                continue
            rim = not (inside(x - 2, y) and inside(x + 2, y) and inside(x, y - 2) and inside(x, y + 2))
            if rim:
                g = rgb(GOLD[2]) if (x + y) % 5 == 0 or (x < 8 and y < 7) else rgb(GOLD[1])
                px[x, y] = g
                continue
            # light from the top left
            t = 0.85 - 0.06 * (x - 3) - 0.05 * (y - 2)
            c = ramp(pal, t)
            if x in (7, 8) and 3 <= y <= 12:
                c = ramp(pal, t + (0.25 if x == 7 else -0.2))
            px[x, y] = c
    # sparkle
    px[5, 4] = rgb(pal[3])
    px[4, 5] = rgb(pal[3])
    return im


def orb_texture(pal):
    """A glowing orb in a golden claw: dark outline, radial shading, a swirl of the element and a highlight."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    cx, cy, r = 7.5, 6.8, 6.1
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > r + 0.6:
                continue
            if d > r - 0.4:
                px[x, y] = OUTLINE
                continue
            # radial: bright core, dark rim, light from the top left
            k = 1 - d / r
            light = 0.25 + 0.75 * k + 0.12 * ((cx - x) + (cy - y)) / r
            # swirl of the element inside the orb
            a = math.atan2(y - cy, x - cx)
            swirl = math.sin(a * 2 + d * 1.3)
            if swirl > 0.55 and 1.5 < d < r - 1.2:
                light += 0.28
            px[x, y] = ramp(pal, light)
    # highlight
    for (x, y) in ((4, 3), (5, 3), (4, 4)):
        px[x, y] = (255, 255, 255, 255)
    # golden claw holding the orb (three prongs and a base)
    claw = {(4, 11): 1, (3, 10): 1, (11, 11): 1, (12, 10): 1, (7, 13): 2, (8, 13): 2,
            (5, 13): 1, (6, 13): 1, (9, 13): 1, (10, 13): 1, (6, 14): 0, (7, 14): 0, (8, 14): 0, (9, 14): 0,
            (5, 12): 1, (10, 12): 1, (7, 12): 2, (8, 12): 1}
    for (x, y), shade in claw.items():
        px[x, y] = rgb(GOLD[shade])
    for (x, y) in ((2, 10), (13, 10), (4, 13), (11, 13), (5, 14), (10, 14), (6, 15), (7, 15), (8, 15), (9, 15)):
        if px[x, y][3] == 0:
            px[x, y] = OUTLINE
    return im


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    tex = os.path.join(ROOT, 'textures', 'item')
    for race, pal in PALETTES.items():
        for kind, draw in (('alpha_scale', scale_texture), ('alpha_orb', orb_texture)):
            name = '%s_%s' % (kind, race)
            draw(pal).save(os.path.join(tex, name + '.png'))
            write_json(os.path.join(ROOT, 'items', name + '.json'),
                       {'model': {'type': 'minecraft:model', 'model': 'bdragon:item/' + name}})
            write_json(os.path.join(ROOT, 'models', 'item', name + '.json'),
                       {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'bdragon:item/' + name}})
    print('alpha items written')


if __name__ == '__main__':
    main()
