"""The Tempest Dragon (alpha of the storm), read from its Blockbench file (dragon_tempete_alpha_v2.bbmodel) and cut
into bones for StormSpine.

In Blockbench the body is a chain of groups, each one the child of the one in front of it (body > neck1 > neck2 >
neck3 > neck4 > head, body > torso > segment1 > ... > segment15 > tail1 > ... > tail5). In the game every piece of the
chain becomes its own bone, child of the root, whose pivot is the joint with the piece in front of it (the origin of
its group): StormSpine places them every frame. The head is a bone on the joint of the head (with its jaw, horns, ears,
whiskers and brows), the torso carries the arms (shoulder > upper arm > forearm > hand > three fingers of two joints).

The cubes keep exactly the geometry, the rotations and the texture offsets of the Blockbench file (box UV), converted
like the Blockbench Java exporter does (x and y flipped, rotations x and y negated). A rotated cube becomes a small part
of its own (named after the cube).

Used by gen_storm_model.py (writes the Java layer) and storm_preview.py (draws the poses).
"""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
BBMODEL = os.path.join(HERE, 'models', 'dragon_tempete_alpha_v2.bbmodel')

# The elements of the spine (StormSpine), from the head to the tip of the tail: the Blockbench groups of the chain.
ELEMENTS = (['neck4', 'neck3', 'neck2', 'neck1', 'torso'] + ['segment%d' % i for i in range(1, 16)]
            + ['tail%d' % i for i in range(1, 6)])
NECK = 4    # elements 0..3 are the neck (element 0 touches the head), element 4 the torso (it carries the arms)
ROOT_ORIGIN = (0, 24, 0)


class Part:
    def __init__(self, name, offset=(0.0, 0.0, 0.0), rot=(0.0, 0.0, 0.0)):
        self.name = name
        self.offset = offset      # pixels, Minecraft model space (y down)
        self.rot = rot            # radians (x, y, z), applied Z then Y then X like ModelPart
        self.cubes = []           # (u, v, x0, y0, z0, w, h, d)
        self.children = []

    def child(self, name):
        for c in self.children:
            if c.name == name:
                return c
        raise KeyError(name)


def conv(v):
    """Blockbench position (y up) -> Minecraft model position (y down, x mirrored)."""
    return (-v[0], -v[1], v[2])


def conv_rot(r):
    if r is None:
        return (0.0, 0.0, 0.0)
    return (-math.radians(r[0]), -math.radians(r[1]), math.radians(r[2]))


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def box(e, origin):
    f, t = e['from'], e['to']
    uv = e.get('uv_offset') or [0, 0]
    return (int(uv[0]), int(uv[1]), -(t[0] - origin[0]), -(t[1] - origin[1]), f[2] - origin[2],
            t[0] - f[0], t[1] - f[1], t[2] - f[2])


def nonzero(r):
    return r is not None and any(abs(x) > 1e-9 for x in r)


class Model:
    def __init__(self, path=BBMODEL):
        bb = json.load(open(path, encoding='utf-8'))
        self.texture = (bb['resolution']['width'], bb['resolution']['height'])
        self.elements = {e['uuid']: e for e in bb['elements']}
        self.groups = {g['uuid']: g for g in bb['groups']}
        self.by_name = {}
        self.tree = {}
        for node in bb['outliner']:
            self._index(node)

    def _index(self, node):
        if isinstance(node, str):
            return
        g = self.groups[node['uuid']]
        self.by_name[g['name']] = g
        self.tree[g['name']] = node['children']
        for c in node['children']:
            self._index(c)

    def fill(self, part, gname):
        """Puts the cubes and the sub groups of the Blockbench group into the part (whose pivot is the group origin)."""
        g = self.by_name[gname]
        origin = g['origin']
        k = 0
        for c in self.tree[gname]:
            if isinstance(c, str):
                e = self.elements[c]
                if e.get('export') is False:
                    continue
                if nonzero(e.get('rotation')):
                    sub_part = Part('r%d' % k, conv(sub(e['origin'], origin)), conv_rot(e['rotation']))
                    k += 1
                    sub_part.cubes.append(box(e, e['origin']))
                    part.children.append(sub_part)
                else:
                    part.cubes.append(box(e, origin))
            else:
                cg = self.groups[c['uuid']]
                part.children.append(self.group(cg['name'], origin, cg['name'].lower()))

    def group(self, gname, parent_origin, name=None):
        g = self.by_name[gname]
        p = Part(name or gname.lower(), conv(sub(g['origin'], parent_origin)), conv_rot(g.get('rotation')))
        self.fill(p, gname)
        return p


class Tree:
    """The groups of the Blockbench file by their place in the outliner (some names repeat, e.g. "crest")."""

    def __init__(self, path=BBMODEL):
        bb = json.load(open(path, encoding='utf-8'))
        self.texture = (bb['resolution']['width'], bb['resolution']['height'])
        self.elements = {e['uuid']: e for e in bb['elements']}
        self.groups = {g['uuid']: g for g in bb['groups']}
        self.nodes = {}
        self.parent = {}

        def walk(node, parent):
            if isinstance(node, str):
                return
            name = self.groups[node['uuid']]['name']
            self.nodes[name] = node
            self.parent[name] = parent
            for c in node['children']:
                walk(c, name)
        for n in bb['outliner']:
            walk(n, None)

    def origin(self, name):
        return self.groups[self.nodes[name]['uuid']]['origin']

    def cubes(self, name):
        return [self.elements[c] for c in self.nodes[name]['children'] if isinstance(c, str)
                and self.elements[c].get('export') is not False]

    def subgroups(self, name):
        return [self.groups[c['uuid']]['name'] for c in self.nodes[name]['children'] if not isinstance(c, str)]

    def part(self, name, parent_origin, with_groups=()):
        """The part of a group: its cubes, its rotated cubes as small parts, and the sub groups asked for."""
        g = self.groups[self.nodes[name]['uuid']]
        origin = g['origin']
        p = Part(name, conv(sub(origin, parent_origin)), conv_rot(g.get('rotation')))
        names = [e['name'] for e in self.cubes(name)]
        seen = {}
        for e in self.cubes(name):
            if nonzero(e.get('rotation')):
                n = e['name']
                if names.count(n) > 1:
                    seen[n] = seen.get(n, -1) + 1
                    n = '%s%d' % (n, seen[n])
                sp = Part(n, conv(sub(e['origin'], origin)), conv_rot(e['rotation']))
                sp.cubes.append(box(e, e['origin']))
                p.children.append(sp)
            else:
                p.cubes.append(box(e, origin))
        for sg in self.subgroups(name):
            if with_groups == 'all' or sg in with_groups:
                p.children.append(self.part(sg, origin, 'all'))
        return p

    def length(self, name):
        """Length (along z) and thickness of the main cube of a piece (the cube named like its group)."""
        e = [c for c in self.cubes(name) if c['name'] == name][0]
        return (e['to'][2] - e['from'][2], max(e['to'][0] - e['from'][0], e['to'][1] - e['from'][1]))


def build(path=BBMODEL):
    """The bones of the serpent, children of the root, at their straight rest pose: head, then the elements."""
    t = Tree(path)
    bones = [t.part('head', ROOT_ORIGIN, 'all')]
    for name in ELEMENTS:
        bones.append(t.part(name, ROOT_ORIGIN, ('arms',) if name == 'torso' else ()))
    return t, bones


def lengths(t):
    return [t.length(n)[0] for n in ELEMENTS]


def widths(t):
    return [t.length(n)[1] for n in ELEMENTS]


if __name__ == '__main__':
    model, bones = build()
    print('lengths', lengths(model))
    print('widths', widths(model))
    def show(p, d=0):
        print('  ' * d + '%s off=%s rot=%s cubes=%d' % (p.name, tuple(round(x, 3) for x in p.offset),
                                                        tuple(round(math.degrees(x), 1) for x in p.rot), len(p.cubes)))
        for c in p.children:
            show(c, d + 1)
    for b in bones:
        show(b)
