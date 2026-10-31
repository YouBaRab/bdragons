"""Paints the palette of the alpha scale (forehead of the alpha hybrids) in the texture of every race.
8 cells of 6x3 texels at (224, 152), 4 per row: dark / mid / light gold, then dark / mid / light / highlight
colour of the race and the ridge. The highlight and the ridge shine a little in the dark (glow.png).
Run from the project folder:  python tools/gen_alpha_mark.py"""
import os
from PIL import Image

ROOT = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'textures', 'entity', 'dragon')
U, V = 224, 152
GOLD = ('6b4a0a', 'c99a2e', 'ffe27a')
# same colours as the alpha scales and orbs (tools/gen_alpha_items.py)
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


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def cells(pal):
    d, m, l, h = (rgb(c) for c in pal)
    return [rgb(GOLD[0]), rgb(GOLD[1]), rgb(GOLD[2]), d, m, l, h, mix(m, l, 0.55)]


def main():
    for race, pal in PALETTES.items():
        body_path = os.path.join(ROOT, race, 'body.png')
        glow_path = os.path.join(ROOT, race, 'glow.png')
        body = Image.open(body_path).convert('RGBA')
        glow = Image.open(glow_path).convert('RGBA')
        for i, c in enumerate(cells(pal)):
            u, v = U + (i % 4) * 6, V + (i // 4) * 3
            for y in range(v, v + 3):
                for x in range(u, u + 6):
                    body.putpixel((x, y), c + (255,))
                    # highlight and ridge: a soft glow; the rest of the scale does not glow
                    glow.putpixel((x, y), (tuple(round(k * 0.45) for k in c) + (255,)) if i in (6, 7) else (0, 0, 0, 0))
        body.save(body_path)
        glow.save(glow_path)
    print('alpha mark palette painted')


if __name__ == '__main__':
    main()
