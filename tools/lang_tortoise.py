"""Adds the texts of the alpha of the forest (the Sylvan Tortoise and its Beam of Life) to the language files.
Run from the project folder:  python tools/lang_tortoise.py"""
import json
import os

ROOT = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'lang')

TEXTS = {
    'entity.bdragon.alpha_hitbox': ('Tortue-Sylve', 'Sylvan Tortoise'),
    'hud.bdragon.clouds': ('Nuages', 'Clouds'),
    'hud.bdragon.life_beam': ('Rayon', 'Beam'),
    'message.bdragon.alpha_power.life_beam': ('Rayon de Vie ! (B pour l\'arrêter)', 'Beam of Life! (B to stop it)'),
    'message.bdragon.alpha_power.ender_dragon': ('✦ %s a terrassé l\'Ender Dragon d\'un seul Rayon de Vie !',
                                                   '✦ %s slew the Ender Dragon with a single Beam of Life!'),
    'message.bdragon.tortoise.no_room': ('Pas assez de place : la Tortue-Sylve fait 12 blocs de haut. Va à l\'air libre.',
                                           'Not enough room: the Sylvan Tortoise is 12 blocks high. Go outside.'),
    'message.bdragon.tortoise.stuck': (
        'La Tortue-Sylve n\'a pas la place ici : tu reprends ta forme hybride (H pour redevenir tortue, à l\'air libre).',
        'The Sylvan Tortoise has no room here: you take back your hybrid form (H to become the tortoise again, outside).'),
    'message.bdragon.alpha.look.forest': (
        'Tu es devenu la Tortue-Sylve : 17 × 12 × 12 blocs, avec un petit biome de prairie sur ta carapace. E'
        "n forme hybride, l'écaille alpha brille sur ton front.",
        'You became the Sylvan Tortoise: 17 × 12 × 12 blocks, with a small plains biome on your shell. In hyb'
        'rid form, the alpha scale shines on your forehead.'),
    'message.bdragon.alpha.power.forest': (
        'Ton pouvoir : le Rayon de Vie (6 s). Il fait pousser une forêt là où il touche le sol, soigne tes alliés, '
        'brûle les monstres et terrasse l\'Ender Dragon d\'un coup.',
        'Your power: the Beam of Life (6 s). It grows a forest where it touches the ground, heals your allies, '
        'burns the monsters and slays the Ender Dragon in one shot.'),

    # --- codex --------------------------------------------------------------------------------------------------
    'codex.bdragon.alpha.key.text': (
        'Touche %s (modifiable dans Options > Commandes), en forme dragon seulement. Il se recharge en %s s, même pendant que '
        'tu es en forme hybride (la barre « Alpha » du panneau est alors cachée). Chaque race reçoit son propre pouvoir, '
        'l\'une après l\'autre (la Forêt : le Rayon de Vie, plus bas) ; les autres utilisent pour l\'instant une onde alpha '
        'qui repousse les monstres.',
        'Key %s (can be changed in Options > Controls), in dragon form only. It recharges in %s s, even while you are in hybrid '
        'form (the "Alpha" bar of the panel is hidden then). Each race gets its own power, one after the other (the Forest: '
        'the Beam of Life, below); the others use an alpha wave that pushes the monsters back for now.'),
    'codex.bdragon.alpha.look.text': (
        'L\'alpha est plus grand et son aura brille de la couleur de sa race. En forme hybride, une écaille alpha à contour '
        'd\'or, aux couleurs de sa race, brille sur son front. L\'alpha de la Forêt change complètement : il devient la '
        'Tortue-Sylve.',
        'The alpha is bigger and its aura shines with the colour of its race. In hybrid form, an alpha scale with a golden '
        'rim, in the colours of its race, shines on its forehead. The alpha of the Forest changes completely: it becomes '
        'the Sylvan Tortoise.'),
    'codex.bdragon.alpha.forest': ('L\'alpha de la Forêt : la Tortue-Sylve', 'The alpha of the Forest: the Sylvan Tortoise'),
    'codex.bdragon.alpha.forest.text': (
        'Une tortue alligator géante et volante de %s blocs de long, %s de large et %s de haut. Sa tête a un '
        "bec crochu gris, des yeux jaunes et une barbe de lianes ; sa carapace vert sombre est hérissée d'épi"
        'nes, et son ventre et le dessous de son cou sont en pierre taillée moussue. Sur son dos vit un petit'
        " biome de prairie : deux collines d'herbe fleurie et cinq arbres de hauteurs différentes.",
        'A giant flying alligator snapping turtle %s blocks long, %s wide and %s high. Its head has a hooked '
        'grey beak, yellow eyes and a beard of lianas; its dark green shell bristles with spikes, and its bel'
        'ly and the underside of its neck are mossy stone bricks. A small plains biome lives on its back: two'
        ' grassy hills with flowers and five trees of different heights.'),
    'codex.bdragon.alpha.forest.stats': (
        'Vie +%s cœurs, armure %s, attaque +%s, +%s de vitesse au sol, rien ne la repousse. Elle vole plus lentement que '
        'les autres alphas.',
        'Health +%s hearts, armour %s, attack +%s, +%s ground speed, nothing pushes it back. It flies slower than the '
        'other alphas.'),
    'codex.bdragon.alpha.forest.flight': ('Son vol', 'Its flight'),
    'codex.bdragon.alpha.forest.flight.text': (
        "En avançant : pattes avant tendues vers l'avant, pattes arrière vers l'arrière, comme un cheval qui "
        'saute. Sur place : des nuages se rassemblent et tourbillonnent sous ses quatre pattes ; si elle rest'
        'e immobile 30 secondes (en vol comme au sol, où elle se pose alors sur son ventre), elle rentre lent'
        "ement la tête, les pattes et la queue dans sa carapace (rien qu'un effet visuel) et ressort au moind"
        're mouvement. En descente : les nuages glissent vers le bas et la tortue descend lentement, lourdeme'
        'nt. Quand elle tombe, ses nuages la rattrapent (pas de dégâts) : avancer la fait dériver, reculer fr'
        "eine, s'accroupir la lâche.",
        'Moving forward: front legs stretched forward, hind legs backward, like a jumping horse. Still in the'
        ' air: clouds gather and swirl under its four paws; if it stays still for 30 seconds (in the air or o'
        'n the ground, where it then rests on its belly), it slowly pulls its head, legs and tail into its sh'
        'ell (only a look) and comes out at the slightest movement. Going down: the clouds slide down and the'
        ' tortoise sinks slowly, heavily. When it falls, its clouds catch it (no damage): forward makes it dr'
        'ift, back brakes, sneak drops it.'),
    'codex.bdragon.alpha.forest.beam': ('Le Rayon de Vie', 'The Beam of Life'),
    'codex.bdragon.alpha.forest.beam.text': (
        'Touche %s : un rayon continu de %s s sort de sa gueule, là où tu regardes (%s à nouveau pour l\'arrêter). Là où il '
        'touche un sol naturel, une forêt pousse aussitôt et pour toujours : arbres adultes, herbe haute, fleurs et récoltes '
        'mûres. Il inflige %s dégâts par seconde aux monstres, soigne %s cœurs par seconde aux joueurs et créatures '
        'amies, et terrasse l\'Ender Dragon d\'un seul coup.',
        'Key %s: a continuous beam of %s s comes out of its mouth, where you look (%s again to stop it). Where it touches '
        'natural ground, a forest grows at once and forever: full grown trees, tall grass, flowers and ripe crops. It deals '
        '%s damage per second to the monsters, heals %s hearts per second to the players and friendly creatures, and slays '
        'the Ender Dragon in one shot.'),
    'codex.bdragon.alpha.forest.beam.range': (
        'Sa portée est ta distance d\'affichage : il s\'arrête au bord des chunks chargés pour toi (4 chunks d\'affichage = '
        'jamais plus loin que ces 4 chunks). Il ne touche jamais un chunk non chargé, ni les blocs d\'une construction, ni '
        'la zone protégée du spawn.',
        'Its range is your render distance: it stops at the edge of the chunks loaded for you (4 chunks of render distance '
        '= never further than those 4 chunks). It never touches an unloaded chunk, nor the blocks of a building, nor the '
        'protected spawn area.'),
    'codex.bdragon.alpha.forest.body': ('Son corps', 'Its body'),
    'codex.bdragon.alpha.forest.body.text': (
        'Neuf zones de touche couvrent tout son corps : un coup ou une flèche qui touche la carapace, la forêt, la tête ou '
        'une patte la blesse vraiment. Sa boîte solide est le « tronc » de la tortue (3,6 × 10,8 blocs) : Minecraft ne donne '
        'qu\'une seule boîte carrée à un joueur. Il lui faut 11 blocs libres au-dessus d\'elle pour se transformer.',
        'Nine hit zones cover its whole body: a blow or an arrow that touches the shell, the forest, the head or a leg really '
        'hurts it. Its solid box is the "trunk" of the tortoise (3.6 × 10.8 blocks): Minecraft gives a player a single '
        'square box. It needs 11 free blocks above it to transform.'),

    # --- museum ---------------------------------------------------------------------------------------------------
    'museum.bdragon.panel.alpha.text': (
        'Au stade V : 4 écailles alpha de ta race (lâchées par les dragons de rang V) + 1 étoile du Nether, offertes à '
        'l\'autel = l\'Orbe du pouvoir.\nClic droit avec l\'orbe, posé au sol : l\'éveil de l\'alpha.\nTouche B : pouvoir '
        'alpha (45 s). Forêt : Rayon de Vie.\nBoutons : l\'éveil de ta race, ou la Tortue-Sylve.',
        'At stage V: 4 alpha scales of your race (dropped by the rank V dragons) + 1 Nether Star, offered to the altar = '
        'the Orb of Power.\nRight-click with the orb, on the ground: the awakening of the alpha.\nKey B: alpha power '
        '(45 s). Forest: Beam of Life.\nButtons: the awakening of your race, or the Sylvan Tortoise.'),
    'museum.bdragon.sign.alpha_forest.1': ('Alpha de la', 'Alpha of the'),
    'museum.bdragon.sign.alpha_forest.2': ('FORÊT', 'FOREST'),
    'museum.bdragon.sign.alpha_forest.3': ('Tortue-Sylve', 'Sylvan Tortoise'),
    'museum.bdragon.sign.alpha_forest.4': ('(cinématique)', '(cinematic)'),
}


# lines added at the end of an existing text (only once)
APPEND = {
    'dragon_type.bdragon.forest.powers': ('• Alpha : la Tortue-Sylve et son Rayon de Vie.', '• Alpha: the Sylvan Tortoise and its Beam of Life.'),
}


def main():
    for i, lang in enumerate(('fr_fr', 'en_us')):
        path = os.path.join(ROOT, lang + '.json')
        data = json.load(open(path, encoding='utf-8'))
        for key, texts in TEXTS.items():
            data[key] = texts[i]
        for key, texts in APPEND.items():
            if texts[i] not in data[key]:
                data[key] = data[key] + '\n' + texts[i]
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(dict(sorted(data.items())), f, ensure_ascii=False, indent=2)
            f.write('\n')
        print(lang, len(data), 'keys')


if __name__ == '__main__':
    main()
