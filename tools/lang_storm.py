"""Adds the texts of the alpha of the storm (the Tempest Dragon and its Storm) to the language files.
Run from the project folder:  python tools/lang_storm.py  (it can be run again, nothing is added twice)"""
import json
import os

ROOT = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'lang')

TEXTS = {
    # the hit zones are shared by the giant alphas now
    'entity.bdragon.alpha_hitbox': ('Corps d\'un alpha', 'Alpha body'),
    'hud.bdragon.storm': ('Orage', 'Storm'),
    'message.bdragon.alpha_power.storm': ('L\'Orage gronde ! (%s s)', 'The Storm rages! (%s s)'),
    'message.bdragon.alpha_power.storm_ender_dragon': ('✦ %s a foudroyé l\'Ender Dragon !', '✦ %s struck down the Ender Dragon!'),
    'message.bdragon.serpent.no_room': (
        'Pas assez de place : le Dragon des Tempêtes a besoin de 6 blocs de haut. Va à l\'air libre.',
        'Not enough room: the Tempest Dragon needs 6 blocks of height. Go outside.'),
    'message.bdragon.serpent.stuck': (
        'Le Dragon des Tempêtes n\'a pas la place ici : tu reprends ta forme hybride (H pour redevenir dragon, à l\'air libre).',
        'The Tempest Dragon has no room here: you take back your hybrid form (H to become the dragon again, outside).'),
    'message.bdragon.alpha.look.storm': (
        "Tu es devenu le Dragon des Tempêtes : un long serpent d'orage craquelé d'éclairs. Immobile dans les "
        "airs, une tornade te porte ; en vol, une vague court le long de ton corps et tes serres s'accrochent"
        " aux nuages : tu es l'alpha le plus rapide. En forme hybride, l'écaille alpha brille sur ton front.",
        'You became the Tempest Dragon: a long storm serpent cracked with lightning. Still in the air, a torn'
        'ado holds you up; in flight, a wave runs along your body and your talons grip the clouds: you are th'
        'e fastest alpha. In hybrid form, the alpha scale shines on your forehead.'),
    'message.bdragon.alpha.power.storm': (
        'Ton pouvoir : l\'Orage (1 min, recharge 3 min). Tu rugis, la foudre tombe partout autour de toi et 10 éclairs frappent '
        'les monstres, l\'un après l\'autre (3 suffisent à abattre l\'Ender Dragon). La foudre ne te fait rien.',
        'Your power: the Storm (1 min, recharge 3 min). You roar, lightning falls everywhere around you and 10 bolts strike '
        'the monsters, one after the other (3 are enough to bring down the Ender Dragon). Lightning does nothing to you.'),

    # codex
    'codex.bdragon.alpha.storm': ('L\'alpha de la Tempête : le Dragon des Tempêtes', 'The alpha of the Storm: the Tempest Dragon'),
    'codex.bdragon.alpha.storm.text': (
        "Un long serpent d'orage de 40 blocs, au corps noir comme les nuages d'une tempête, craquelé d'éclair"
        "s bleus et couvert d'écailles, avec des oreilles de chat, des cornes de bouc recourbées, des sourcil"
        "s, des moustaches électriques, une crête sur tout le dos et deux longs bras aux serres d'aigle. Ses "
        'yeux, ses éclairs, ses crêtes, ses moustaches et ses serres brillent dans le noir.',
        'A long storm serpent of 40 blocks, its body black as the clouds of a tempest, cracked with blue ligh'
        'tning and covered in scales, with cat ears, curled goat horns, brows, electric whiskers, a crest all'
        ' along its back and two long arms with eagle talons. Its eyes, its lightning, its crests, its whiske'
        'rs and its talons shine in the dark.'),
    'codex.bdragon.alpha.storm.stats': (
        'Vie +%s cœurs, armure %s, attaque +%s, +%s de vitesse au sol, et il vole %s plus vite que les autres alphas : '
        'c\'est le plus rapide. Rien ne le repousse.',
        'Health +%s hearts, armor %s, attack +%s, +%s ground speed, and it flies %s faster than the other alphas: it is '
        'the fastest. Nothing pushes it back.'),
    'codex.bdragon.alpha.storm.flight': ('Ses mouvements', 'How it moves'),
    'codex.bdragon.alpha.storm.flight.text': (
        "Immobile au sol, il se dresse : le torse droit, le cou en S, la tête à l'horizontale, les bras tendu"
        's et le corps enroulé en spirale ; il respire lentement. Immobile dans les airs, une tornade le port'
        "e : la tête et les bras au centre, son corps s'enroule autour de lui en un cône qui descend et se re"
        "sserre, et le vent tourne autour de lui. Au sol, il avance l'avant dressé, des bosses courant le lon"
        "g de son corps. En vol, une vague court le long de son corps jusqu'au bout de la queue et il s'incli"
        "ne avec sa trajectoire : nez en l'air quand il monte. En piqué, il file droit comme une flèche, nez "
        "en bas, les bras plaqués en arrière. En mouvement, ses serres s'accrochent aux nuages qui le portent"
        '.',
        'Still on the ground, it rears: the torso straight up, the neck in an S, the head level, the arms out'
        ' and the body coiled in a spiral; it breathes slowly. Still in the air, a tornado holds it up: the h'
        'ead and the arms at the centre, its body winds around it in a cone that goes down and narrows, and t'
        'he wind turns around it. On the ground, it moves with its front raised, humps running along its body'
        '. In flight, a wave runs along its body to the tip of its tail and it tilts with its path: nose up w'
        'hen it climbs. Diving, it goes straight like an arrow, nose down, the arms swept back. When it moves'
        ', its talons grip the clouds that carry it.'),
    'codex.bdragon.alpha.storm.power': ('L\'Orage', 'The Storm'),
    'codex.bdragon.alpha.storm.power.text': (
        'Touche %s : il rugit et appelle un orage de %s s autour de lui : nuages noirs, pluie et éclairs de partout. '
        'L\'orage le suit. Recharge : %s min. La foudre ne lui fait rien.',
        'Key %s: it roars and calls a storm of %s s around it: black clouds, rain and lightning everywhere. The storm '
        'follows it. Recharge: %s min. Lightning does nothing to it.'),
    'codex.bdragon.alpha.storm.strikes': (
        '%s éclairs frappent les monstres autour de lui, l\'un après l\'autre (un toutes les %s s, jusqu\'à %s blocs), %s '
        'dégâts chacun : 3 abattent l\'Ender Dragon. Chaque éclair choisit un monstre au hasard, mais celui qui a le plus '
        'de vie a le plus de chances d\'être frappé, et celui qui vient de l\'être a peu de chances de l\'être encore.',
        '%s bolts strike the monsters around it, one after the other (one every %s s, up to %s blocks), %s damage each: '
        '3 bring down the Ender Dragon. Each bolt picks a monster at random, but the one with the most health has the best '
        'chance to be hit, and the one that was just hit has little chance to be hit again.'),
    'codex.bdragon.alpha.storm.body': ('Son corps', 'Its body'),
    'codex.bdragon.alpha.storm.body.text': (
        "Neuf zones de touche suivent sa tête et son corps, qu'il soit dressé, au sol ou en vol : un coup ou "
        'une flèche qui le touche le blesse vraiment. Il lui faut %s blocs libres au-dessus de lui pour se tr'
        'ansformer.',
        'Nine hit zones follow its head and its body, rearing, on the ground or in flight: a blow or an arrow'
        ' that touches it really hurts it. It needs %s free blocks above it to transform.'),

    # museum
    'museum.bdragon.sign.alpha_storm.1': ('Alpha de la', 'Alpha of the'),
    'museum.bdragon.sign.alpha_storm.2': ('TEMPÊTE', 'STORM'),
    'museum.bdragon.sign.alpha_storm.3': ('Dragon-serpent', 'Tempest Dragon'),
    'museum.bdragon.sign.alpha_storm.4': ('(cinématique)', '(cinematic)'),
}

# parts of existing texts replaced (only when the old part is still there)
REPLACE = {
    'museum.bdragon.panel.alpha.text': (
        ('Forêt : Rayon de Vie.\nBoutons : l\'éveil de ta race, ou la Tortue-Sylve.',
         'Forêt : Rayon de Vie. Tempête : l\'Orage (3 min).\nBoutons : l\'éveil de ta race, la Tortue-Sylve ou le Dragon des Tempêtes.'),
        ('Forest: Beam of Life.\nButtons: the awakening of your race, or the Sylvan Tortoise.',
         'Forest: Beam of Life. Storm: the Storm (3 min).\nButtons: the awakening of your race, the Sylvan Tortoise or the Tempest Dragon.')),
    'codex.bdragon.alpha.key.text': (
        ('(la Forêt : le Rayon de Vie, plus bas)', '(la Forêt : le Rayon de Vie ; la Tempête : l\'Orage, qui se recharge en 3 min ; plus bas)'),
        ('(the Forest: the Beam of Life, below)', '(the Forest: the Beam of Life; the Storm: the Storm, which recharges in 3 min; below)')),
    'codex.bdragon.alpha.look.text': (
        ('il devient la Tortue-Sylve.', 'il devient la Tortue-Sylve. L\'alpha de la Tempête devient le Dragon des Tempêtes.'),
        ('it becomes the Sylvan Tortoise.', 'it becomes the Sylvan Tortoise. The alpha of the Storm becomes the Tempest Dragon.')),
    'dragon_type.bdragon.storm.powers': (
        ('Force II pendant l\'orage.', 'Force II pendant l\'orage (et pendant son Orage d\'alpha).'),
        ('Strength II during thunderstorms.', 'Strength II during thunderstorms (and during its alpha Storm).')),
}

# lines added at the end of an existing text (only once)
APPEND = {
    'dragon_type.bdragon.storm.powers': ('• Alpha : le Dragon des Tempêtes et son Orage.', '• Alpha: the Tempest Dragon and its Storm.'),
}


def main():
    for i, lang in enumerate(('fr_fr', 'en_us')):
        path = os.path.join(ROOT, lang + '.json')
        data = json.load(open(path, encoding='utf-8'))
        for key, texts in TEXTS.items():
            data[key] = texts[i]
        for key, pairs in REPLACE.items():
            old, new = pairs[i]
            if new not in data[key]:
                data[key] = data[key].replace(old, new)
        for key, texts in APPEND.items():
            if texts[i] not in data[key]:
                data[key] = data[key] + '\n' + texts[i]
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            json.dump(dict(sorted(data.items())), f, ensure_ascii=False, indent=2)
            f.write('\n')
        print(lang, len(data), 'keys')


if __name__ == '__main__':
    main()
