"""Adds the texts of the Alpha (v1.4.0; the texts of the alpha power and of the forehead scale were
added in 1.4.1 directly in the JSON files) to the language files (keeps them sorted).
Run from the project folder:  python tools/lang_alpha.py"""
import json, os

LANG = os.path.join('src', 'main', 'resources', 'assets', 'bdragon', 'lang')
RACES = ['fire', 'ice', 'water', 'forest', 'nether', 'end', 'terra', 'sunlight', 'storm', 'moonlight', 'wither']

FR = {
    # --- items ---
    'item.bdragon.alpha_scale.tooltip_1': "Lâchée par un %s de rang V.",
    'item.bdragon.alpha_scale.tooltip_2': "%s écailles + 1 étoile du Nether à l'autel = l'orbe.",
    'item.bdragon.alpha_orb.tooltip_1': "Éveille l'alpha d'un %s légendaire.",
    'item.bdragon.alpha_orb.tooltip_2': "Clic droit, posé au sol : pour toujours.",
    # --- stage, HUD ---
    'dragon_stage.bdragon.alpha': "Alpha",
    'hud.bdragon.alpha_form': "Alpha · %s",
    'hud.bdragon.alpha': "Puissance de l'alpha",
    # --- the orb ---
    'message.bdragon.orb.not_dragon': "Seul un dragon peut absorber cette orbe.",
    'message.bdragon.orb.wrong_race': "Cette orbe est celle du %s.",
    'message.bdragon.orb.already': "Tu es déjà l'alpha de ta race.",
    'message.bdragon.orb.need_legendary': "Il faut être un %s (stade V).",
    'message.bdragon.orb.ground': "Pose-toi au sol pour l'éveil de l'alpha.",
    'message.bdragon.orb.start': "L'Orbe du pouvoir s'élève de tes griffes…",
    'message.bdragon.alpha.done': "✦ Tu es devenu un %s Alpha !",
    'message.bdragon.alpha.bonus': "Plus grand et plus fort que tout dragon légendaire : vie, armure, attaque, souffle, énergie et vol augmentés.",
    'message.bdragon.alpha.look': "Ton corps a grandi, ton aura brille de la couleur de ta race et, en forme hybride, l'écaille alpha brille sur ton front.",
    'message.bdragon.alpha.broadcast': "✦ %s est devenu un %s Alpha !",
    'message.bdragon.alpha.hint_1': "Prochain palier : l'Alpha.",
    'message.bdragon.alpha.hint_2': "Offre ici %s écailles alpha (%s de rang V) et 1 étoile du Nether : l'Orbe du pouvoir.",
    # --- the forge of the altar ---
    'message.bdragon.forge.scales_full': "L'autel a déjà les écailles du %s.",
    'message.bdragon.forge.star_full': "L'autel a déjà son étoile du Nether.",
    'message.bdragon.forge.status': "Orbe du %s : écailles %s/%s · étoile %s/1",
    'message.bdragon.forge.done': "✦ L'Orbe du pouvoir du %s est née !",
    'message.bdragon.forge.use': "Prends-la, pose-toi au sol et fais clic droit avec : l'éveil de l'alpha commence.",
    'message.bdragon.forge.taken': "Tu reprends les offrandes de l'orbe.",
    'message.bdragon.forge.take_hint': "Accroupi + clic droit main vide : reprendre les offrandes.",
    'message.bdragon.horn_nothing_alpha': "Aucun repaire de %s de rang V\nà moins de 8 000 blocs. Essaie le Nether ou l'End.",
    # --- cinematic ---
    'screen.bdragon.alpha': "Éveil de l'alpha",
    'cinematic.bdragon.alpha.1': "L'Orbe du pouvoir s'éveille…",
    'cinematic.bdragon.alpha.2': "Le sang des anciens dragons bouillonne…",
    'cinematic.bdragon.alpha.3': "L'alpha de sa race est né.",
    'cinematic.bdragon.alpha.title': "ALPHA",
    'cinematic.bdragon.skip': "Échap : passer",
    # --- codex ---
    'codex.bdragon.alpha': "L'Alpha",
    'codex.bdragon.alpha.intro': "Au-delà du stade légendaire, un dragon peut devenir l'Alpha de sa race, pour toujours : plus grand et plus fort que tous les autres.",
    'codex.bdragon.alpha.scales': "1. Les écailles alpha",
    'codex.bdragon.alpha.scales.text': "Chaque dragon sauvage de rang V lâche une écaille alpha de sa race. Il t'en faut %s de ta propre race. Au stade V, la Corne du Défi te montre le repaire de rang V de ta race le plus proche.",
    'codex.bdragon.alpha.forge': "2. La forge de l'orbe",
    'codex.bdragon.alpha.forge.text': "Offre à l'Autel draconique (clic droit avec l'objet) %s écailles alpha et 1 étoile du Nether : l'Orbe du pouvoir de la race apparaît au-dessus de l'autel. Tu peux en forger autant que tu veux, par exemple pour un ami de la même race. Accroupi + clic droit main vide : reprendre les offrandes.",
    'codex.bdragon.alpha.awaken': "3. L'éveil",
    'codex.bdragon.alpha.awaken.text': "Au stade V, pose-toi au sol et fais clic droit avec l'orbe de ta race. Tu passes en forme dragon et l'éveil commence, vu en 3e personne : l'orbe s'élève, tu montes dans les airs, l'orbe plonge dans ton cœur… et l'alpha naît. Rien ne peut te blesser pendant l'éveil. Échap : passer la cinématique.",
    'codex.bdragon.alpha.power': "Ce que l'alpha gagne",
    'codex.bdragon.alpha.power.text': "Vie +%s cœurs (au lieu de +%s), armure %s (au lieu de %s), attaque +%s (au lieu de +%s), souffle de %s blocs et %s dégâts, énergie %s (au lieu de %s).",
    'codex.bdragon.alpha.power.more': "Vol +%s plus rapide, sens des repaires %s blocs, rugissement toutes les %s s, plus grand que tout dragon légendaire.",
    'codex.bdragon.alpha.power.hybrid': "Forme hybride : coup de poing de %s, +%s de vitesse au sol.",
    'codex.bdragon.alpha.look': "Son apparence",
    'codex.bdragon.alpha.look.text': "L'alpha est plus grand et son aura brille de la couleur de sa race. En forme hybride, une écaille alpha à contour d'or, aux couleurs de sa race, brille sur son front.",
    'codex.bdragon.progress.alpha': "Tu es l'Alpha de ta race !",
    'codex.bdragon.progress.alpha_next': "Prochain palier : l'Alpha (chapitre L'Alpha).",
    'codex.bdragon.progress.forge': "Forge : écailles %s/%s · étoile %s/1",
    'codex.bdragon.stages.alpha': "Après le stade V : l'Alpha (chapitre suivant).",
    'codex.bdragon.items.alpha_scale.title': "Écailles alpha",
    'codex.bdragon.items.alpha_scale': "Rang V : une par dragon, de sa race.",
    'codex.bdragon.items.alpha_orb.title': "Orbes du pouvoir",
    'codex.bdragon.items.alpha_orb': "Forgée à l'autel. Clic droit : l'alpha.",
    # --- museum ---
    'museum.bdragon.sign.alpha.1': "Devenir",
    'museum.bdragon.sign.alpha.2': "ALPHA",
    'museum.bdragon.sign.alpha.3': "(cinématique)",
    'museum.bdragon.sign.alpha.4': "stade V auto",
    'museum.bdragon.sign.alpha_chest.1': "Forge alpha",
    'museum.bdragon.sign.alpha_chest.2': "4 écailles et",
    'museum.bdragon.sign.alpha_chest.3': "1 étoile du",
    'museum.bdragon.sign.alpha_chest.4': "Nether à l'autel",
    'museum.bdragon.panel.alpha.title': "L'Alpha",
    'museum.bdragon.panel.alpha.text': "Au stade V : 4 écailles alpha de ta race (lâchées par les dragons de rang V) + 1 étoile du Nether, offertes à l'autel = l'Orbe du pouvoir.\nClic droit avec l'orbe, posé au sol : l'éveil de l'alpha.\nTouche B : pouvoir alpha (forme dragon, 45 s).\nCe bouton joue l'éveil tout de suite.",
    # --- 1.4.1: alpha power and forehead scale ---
    'key.bdragon.alpha_power': "Pouvoir alpha",
    'hud.bdragon.alpha_power': "Alpha",
    'message.bdragon.alpha_power.locked': "Deviens l'alpha de ta race pour éveiller ce pouvoir.",
    'message.bdragon.alpha_power.dragon_only': "Le pouvoir alpha ne s'utilise qu'en forme dragon.",
    'message.bdragon.alpha_power.cooldown': "Pouvoir alpha : encore %s s",
    'message.bdragon.alpha_power.wave': "Onde alpha ! (pouvoir provisoire, en attendant celui de ta race)",
    'message.bdragon.alpha.power': "Touche %s : pouvoir alpha (forme dragon, recharge %s s).",
    'codex.bdragon.alpha.key': "Le pouvoir alpha",
    'codex.bdragon.alpha.key.text': "Touche %s (modifiable dans Options > Commandes), en forme dragon seulement. Il se recharge en %s s, même pendant que tu es en forme hybride (la barre « Alpha » du panneau est alors cachée). Chaque race aura son propre pouvoir ; pour l'instant, c'est une onde alpha qui repousse les monstres.",
    'codex.bdragon.powers.alpha.text': "Touche %s, pour l'alpha en forme dragon (recharge %s s, elle continue en forme hybride). Voir le chapitre L'Alpha.",
}
EN = {
    'item.bdragon.alpha_scale.tooltip_1': "Dropped by a rank V %s.",
    'item.bdragon.alpha_scale.tooltip_2': "%s scales + 1 Nether Star at the altar = the orb.",
    'item.bdragon.alpha_orb.tooltip_1': "Awakens the alpha of a legendary %s.",
    'item.bdragon.alpha_orb.tooltip_2': "Right-click, standing on the ground: forever.",
    'dragon_stage.bdragon.alpha': "Alpha",
    'hud.bdragon.alpha_form': "Alpha · %s",
    'hud.bdragon.alpha': "Alpha power",
    'message.bdragon.orb.not_dragon': "Only a dragon can absorb this orb.",
    'message.bdragon.orb.wrong_race': "This orb belongs to the %s.",
    'message.bdragon.orb.already': "You already are the alpha of your race.",
    'message.bdragon.orb.need_legendary': "You must be a %s dragon (stage V).",
    'message.bdragon.orb.ground': "Land on the ground for the awakening of the alpha.",
    'message.bdragon.orb.start': "The Orb of Power rises from your claws…",
    'message.bdragon.alpha.done': "✦ You became an Alpha %s!",
    'message.bdragon.alpha.bonus': "Bigger and stronger than any legendary dragon: more health, armor, attack, breath, energy and flight speed.",
    'message.bdragon.alpha.look': "Your body grew, your aura shines with the colour of your race and, in hybrid form, the alpha scale shines on your forehead.",
    'message.bdragon.alpha.broadcast': "✦ %s became an Alpha %s!",
    'message.bdragon.alpha.hint_1': "Next step: the Alpha.",
    'message.bdragon.alpha.hint_2': "Offer here %s alpha scales (rank V %s) and 1 Nether Star: the Orb of Power.",
    'message.bdragon.forge.scales_full': "The altar already has the scales of the %s.",
    'message.bdragon.forge.star_full': "The altar already has its Nether Star.",
    'message.bdragon.forge.status': "Orb of the %s: scales %s/%s · star %s/1",
    'message.bdragon.forge.done': "✦ The Orb of Power of the %s is born!",
    'message.bdragon.forge.use': "Take it, land on the ground and right-click with it: the awakening of the alpha begins.",
    'message.bdragon.forge.taken': "You take the offerings of the orb back.",
    'message.bdragon.forge.take_hint': "Sneak + right-click with an empty hand: take the offerings back.",
    'message.bdragon.horn_nothing_alpha': "No rank V %s lair\nwithin 8,000 blocks. Try the Nether or the End.",
    'screen.bdragon.alpha': "Awakening of the alpha",
    'cinematic.bdragon.alpha.1': "The Orb of Power awakens…",
    'cinematic.bdragon.alpha.2': "The blood of the ancient dragons boils…",
    'cinematic.bdragon.alpha.3': "The alpha of its race is born.",
    'cinematic.bdragon.alpha.title': "ALPHA",
    'cinematic.bdragon.skip': "Esc: skip",
    'codex.bdragon.alpha': "The Alpha",
    'codex.bdragon.alpha.intro': "Beyond the legendary stage, a dragon can become the Alpha of its race, forever: bigger and stronger than all the others.",
    'codex.bdragon.alpha.scales': "1. The alpha scales",
    'codex.bdragon.alpha.scales.text': "Every wild dragon of rank V drops one alpha scale of its race. You need %s of your own race. At stage V, the Challenge Horn shows you the nearest rank V lair of your race.",
    'codex.bdragon.alpha.forge': "2. The forge of the orb",
    'codex.bdragon.alpha.forge.text': "Offer to the Dragon Altar (right-click with the item) %s alpha scales and 1 Nether Star: the Orb of Power of the race appears above the altar. You can forge as many as you want, for a friend of the same race for example. Sneak + right-click with an empty hand: take the offerings back.",
    'codex.bdragon.alpha.awaken': "3. The awakening",
    'codex.bdragon.alpha.awaken.text': "At stage V, stand on the ground and right-click with the orb of your race. You turn into your dragon form and the awakening begins, seen in third person: the orb rises, you float into the air, the orb dives into your heart… and the alpha is born. Nothing can hurt you during the awakening. Esc: skip the cinematic.",
    'codex.bdragon.alpha.power': "What the alpha gets",
    'codex.bdragon.alpha.power.text': "Health +%s hearts (instead of +%s), armor %s (instead of %s), attack +%s (instead of +%s), breath of %s blocks and %s damage, energy %s (instead of %s).",
    'codex.bdragon.alpha.power.more': "Flight %s faster, lair sense %s blocks, roar every %s s, bigger than any legendary dragon.",
    'codex.bdragon.alpha.power.hybrid': "Hybrid form: punch of %s, +%s ground speed.",
    'codex.bdragon.alpha.look': "Its look",
    'codex.bdragon.alpha.look.text': "The alpha is bigger and its aura shines with the colour of its race. In hybrid form, an alpha scale with a golden rim, in the colours of its race, shines on its forehead.",
    'codex.bdragon.progress.alpha': "You are the Alpha of your race!",
    'codex.bdragon.progress.alpha_next': "Next step: the Alpha (chapter The Alpha).",
    'codex.bdragon.progress.forge': "Forge: scales %s/%s · star %s/1",
    'codex.bdragon.stages.alpha': "After stage V: the Alpha (next chapter).",
    'codex.bdragon.items.alpha_scale.title': "Alpha scales",
    'codex.bdragon.items.alpha_scale': "Rank V: one per dragon, of its race.",
    'codex.bdragon.items.alpha_orb.title': "Orbs of Power",
    'codex.bdragon.items.alpha_orb': "Forged at the altar. Right-click: alpha.",
    'museum.bdragon.sign.alpha.1': "Become",
    'museum.bdragon.sign.alpha.2': "ALPHA",
    'museum.bdragon.sign.alpha.3': "(cinematic)",
    'museum.bdragon.sign.alpha.4': "stage V auto",
    'museum.bdragon.sign.alpha_chest.1': "Alpha forge",
    'museum.bdragon.sign.alpha_chest.2': "4 scales and",
    'museum.bdragon.sign.alpha_chest.3': "1 Nether Star",
    'museum.bdragon.sign.alpha_chest.4': "at the altar",
    'museum.bdragon.panel.alpha.title': "The Alpha",
    'museum.bdragon.panel.alpha.text': "At stage V: 4 alpha scales of your race (dropped by the rank V dragons) + 1 Nether Star, offered to the altar = the Orb of Power.\nRight-click with the orb, on the ground: the awakening of the alpha.\nKey B: alpha power (dragon form, 45 s).\nThis button plays the awakening right away.",
    # --- 1.4.1: alpha power and forehead scale ---
    'key.bdragon.alpha_power': "Alpha power",
    'hud.bdragon.alpha_power': "Alpha",
    'message.bdragon.alpha_power.locked': "Become the alpha of your race to awaken this power.",
    'message.bdragon.alpha_power.dragon_only': "The alpha power only works in dragon form.",
    'message.bdragon.alpha_power.cooldown': "Alpha power: %s s left",
    'message.bdragon.alpha_power.wave': "Alpha wave! (temporary power, until the one of your race)",
    'message.bdragon.alpha.power': "Key %s: alpha power (dragon form, recharge %s s).",
    'codex.bdragon.alpha.key': "The alpha power",
    'codex.bdragon.alpha.key.text': "Key %s (can be changed in Options > Controls), in dragon form only. It recharges in %s s, even while you are in hybrid form (the \"Alpha\" bar of the panel is hidden then). Each race will get its own power; for now it is an alpha wave that pushes the monsters back.",
    'codex.bdragon.powers.alpha.text': "Key %s, for the alpha in dragon form (recharge %s s, it goes on in hybrid form). See the chapter The Alpha.",
}


def race_names(lang):
    d = json.load(open(os.path.join(LANG, lang + '.json'), encoding='utf-8'))
    return {r: d['dragon_type.bdragon.' + r] for r in RACES}


def main():
    for lang, extra in (('fr_fr', FR), ('en_us', EN)):
        path = os.path.join(LANG, lang + '.json')
        data = json.load(open(path, encoding='utf-8'))
        names = race_names(lang)
        for r in RACES:
            if lang == 'fr_fr':
                data['item.bdragon.alpha_scale_' + r] = 'Écaille alpha — ' + names[r]
                data['item.bdragon.alpha_orb_' + r] = 'Orbe du pouvoir — ' + names[r]
            else:
                data['item.bdragon.alpha_scale_' + r] = names[r] + ' Alpha Scale'
                data['item.bdragon.alpha_orb_' + r] = names[r] + ' Orb of Power'
        data.update(extra)
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(dict(sorted(data.items())), f, ensure_ascii=False, indent=2)
            f.write('\n')
        print(lang, len(data))


if __name__ == '__main__':
    main()
