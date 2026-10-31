# Bdragon (« be dragon ») — L'Éveil du Dragon

Mod **Forge 66.1.0** pour **Minecraft Java 26.3** (Java 25).

Deviens un dragon ! Accomplis les quêtes d'ingrédients à l'Autel draconique et choisis ta race parmi
**11**, chacune avec son propre corps. Passe de la forme **hybride** (mi-homme, mi-dragon, avec des
griffes qui creusent) à la forme **dragon**. Grandis en **5 stades** en défiant les dragons sauvages
dans leurs **repaires**, que tu peux voir sur la carte **JourneyMap**, puis deviens l'**Alpha** de ta
race grâce à l'**Orbe du pouvoir**.

Tout est expliqué en jeu dans le **Codex draconique**, donné à chaque joueur à sa première arrivée
dans le monde (touche **G**).

---

## 1. Compiler et tester le mod

### Ce qu'il faut
- **Java 25 (JDK)** : *Eclipse Temurin 25* sur https://adoptium.net (fichier `.msi` Windows x64).
  Pendant l'installation, active l'option **« Set JAVA_HOME variable »** (désactivée par défaut).
- Une connexion Internet au premier lancement (Gradle télécharge Minecraft, Forge et l'API JourneyMap).
- **IntelliJ IDEA Community** (gratuit) : facultatif, seulement pour lire ou modifier le code.

### Lancer le jeu de test
Dans le dossier du projet (celui qui contient `build.gradle`) :
- dans **PowerShell** (le terminal par défaut de Windows 11) : `.\gradlew.bat runClient`
- dans **cmd** : `gradlew.bat runClient`

La première fois, Gradle télécharge Minecraft et Forge (10 à 20 minutes). Pour vérifier la version
lancée : menu **Mods** → Bdragon doit afficher **1.6.0**.

### Tester avec JourneyMap (facultatif)
1. Télécharge **journeymap-forge-26.3-6.0.10.jar** (CurseForge ou Modrinth, version **Forge** pour **26.3**).
2. Mets-le dans le dossier `run\mods\` du projet (crée le dossier `mods` s'il n'existe pas).
3. Relance `gradlew.bat runClient`.

Sans JourneyMap, le mod marche normalement : les repaires connus sont listés dans le codex.

### Créer le fichier .jar du mod
- `.\gradlew.bat build` → le mod est dans `build\libs\bdragon-1.8.0.jar`
- Copie ce jar (et JourneyMap si tu le veux) dans le dossier `mods` de ton instance CurseForge.

### Si quelque chose plante
Envoie-moi le message d'erreur de la console, le fichier `run\logs\latest.log`, ou le
crash-report dans `run\crash-reports\`.

### Le monde « Musée Bdragon » (visite guidée)
Un monde plat de 3 couches (bedrock, terre, herbe) que le mod construit tout seul : une allée vers le
sud avec 5 étapes, des panneaux qui disent où aller et quoi faire, des boutons pour tout essayer, et
tous les dragons exposés.
Trois façons d'y aller :
1. **Le plus simple** : Solo → **Créer un nouveau monde** → bouton **« Musée Bdragon »** en bas à
   gauche de l'écran. Un clic crée le monde (plat, créatif, commandes activées).
2. Ou : onglet **Monde** → **Type de monde** : clique jusqu'à **« Musée Bdragon »**
   (ou Type **Plat** → **Personnaliser** → **Préréglages** → « Musée Bdragon »).
3. Ou, dans n'importe quel monde (même sans triche en solo) : `/bdragon musee` construit le musée là
   où tu te trouves.

Le musée se construit à la première ouverture (quelques secondes), tu apparais au départ.

| Étape | À gauche | À droite |
|---|---|---|
| Départ | boutons mode survie / créatif | recevoir le Codex, redevenir humain |
| 1 | le vrai rituel : autel + coffres avec tous les ingrédients (+ un coffre « Forge alpha » : 4 écailles alpha de chaque race et des étoiles du Nether) | un bouton par race (transformation immédiate) |
| 2 | les 2 formes (boutons) + le mur des griffes à creuser | les 5 stades (boutons) + le bouton **Alpha** (joue l'éveil) + le bouton **Alpha de la Forêt** (devient la Tortue-Sylve) + le bouton **Alpha de la Tempête** (devient le Dragon des Tempêtes) |
| 3 | tour du plané (30 blocs), bassin pour la glace, cibles d'entraînement | évolution du Dragon de Feu, stades I à V |
| 4 | les 11 races sur leurs socles, avec leur fiche | (suite) |
| 5 | 3 repaires de démonstration (coordonnées) + Cornes du Défi | boutons de l'arène (rangs I à V), soins, vider l'arène |
| Fin | l'arène des défis (dôme de verre) | |

Toujours midi, pas de monstres, inventaire conservé à la mort. Le plan est dans `docs/plan_musee.png`.

---

## 2. Comment jouer

### Devenir un dragon
1. Fabrique l'**Autel draconique** :
   ```
   Or         Diamant     Or
   Bloc d'os  Obsidienne  Bloc d'os
   Obsidienne Obsidienne  Obsidienne
   ```
2. Offre les ingrédients **sur l'autel** (clic droit avec l'objet en main).
   Clic droit main vide sur l'autel : affiche la quête en cours.

| Quête | Ingrédients |
|---|---|
| 1. Les Ossements Anciens | 32 os, 16 cuirs, 8 lingots d'or |
| 2. Le Trésor du Dragon | 4 diamants, 4 émeraudes, 4 lingots d'or, 4 lingots de fer |
| 3. L'Essence Élémentaire | **1 essence au choix** → décide de ta race, **pour toujours** |
| 4. Les Offrandes | offrandes propres à ta race (voir le codex) |

3. Quand tout est offert : **clic droit main vide sur l'autel** → le rituel (éclair) te transforme
   en **Dragonnet** de ta race.

### Les essences (4 ingrédients + 1 fiole vide, recette sans forme)
| Race | Ingrédients |
|---|---|
| Feu | bâton de blaze, crème de magma, boule de feu, bloc de charbon |
| Glace | glace bleue, glace compactée, bloc de neige, boule de neige |
| Eau | cristaux de prismarine, éclat de prismarine, coquille de nautile, algue |
| Forêt | pousse d'arbre, bloc de mousse, baies sucrées, pomme |
| Nether | netherrack, champignon carmin, champignon biscornu, quartz |
| End | œil de l'Ender, perle de l'Ender, obsidienne, obsidienne pleureuse |
| Terra | briques, terre cuite, argile, or brut |
| Solaire | poudre de pierre lumineuse, tournesol, lingot d'or, rayon de miel |
| Tempête | paratonnerre, lingot de cuivre, redstone, poudre à canon |
| Lunaire | membrane de phantom, baies lumineuses, quartz, calcite |
| Wither | sable des âmes, bloc d'os, bloc de charbon, brique du Nether |

### Les 2 formes (touche H : Hybride ⇄ Dragon)
Après le rituel, il n'y a plus de retour au corps humain.

| Forme | Ce qu'elle fait |
|---|---|
| **Hybride** | un corps humain avec les **cornes, ailes et queue** de ta race. Pouvoirs à **70 %** (souffle à 70 % de portée). **La plus rapide au sol** (+23 % à +35 %). Frappe fort : 3 dégâts au poing au stade I, **7,5 au stade V** (un villageois en 3 coups). Ne vole pas, mais **plane**. **Griffes** : voir ci-dessous |
| **Dragon** | 100 % des pouvoirs, **vol** (dès le stade II), santé/armure/attaque maximales, plus lourd au sol. Ses grosses griffes **ne creusent pas** |

**Les griffes de l'hybride** : main vide (ou avec un objet qui n'est pas un outil), tu creuses comme
avec une pioche, une pelle ou une hache **en netherite**, avec **Efficacité = ton stade** (stade IV =
Efficacité IV) et **Toucher de soie**. Avec un vrai outil en main, c'est l'outil qui travaille
(pratique pour Fortune).

### Contrôles (modifiables dans Options > Commandes > Bdragon)
| Touche | Action |
|---|---|
| **R** (maintenir) | Souffle (consomme l'énergie) |
| **V** | Rugissement : repousse et frappe tout autour (dès le stade III) |
| **H** | Changer de forme |
| **G** | Codex draconique |
| **B** | Pouvoir alpha (seulement pour l'alpha, en forme dragon ; recharge 45 s, 3 min pour la Tempête) |
| Double saut | Voler (forme dragon, dès le stade II ; le stade II se fatigue vite) |
| *(automatique)* | **Planer** quand tu tombes (dragon ou hybride). La souris dirige |
| **Avancer** (en planant) | **Piqué** : tu plonges et tu accélères sans regarder sous tes pieds (regarder un peu vers le bas marche aussi) |
| **Gauche / droite** (en planant) | glisser sur le côté |
| **Reculer** (en planant) | freiner, descente douce |
| **Accroupi** (en planant) | replier les ailes et tomber |

### Les 5 stades
| Stade | Puissance | Défi à remporter | Nouveauté visible |
|---|---|---|---|
| I Dragonnet | 0 | — | grosse tête, cornes naissantes, petites ailes |
| II Jeune dragon | 150 | dragon de rang II | les cornes poussent, crête sur la nuque, vol |
| III Dragon adulte | 600 | rang III | cornes complètes, grandes ailes, rugissement |
| IV Dragon ancien | 1800 | rang IV | 2ᵉ paire de cornes recourbées, ailes immenses, épines dorsales, aura |
| V Dragon légendaire | 4500 | rang V | la couronne de sa race, l'apparence complète, **le sol se transforme** |

Les cornes, couronnes et queues dépendent de la race (tableau ci-dessous). La forme hybride évolue
aussi et porte les mêmes traits, en petit. Le codex montre chaque race aux 5 stades.

### L'apparence des 11 races
| Race | Ce qui la distingue |
|---|---|
| Feu | corps d'obsidienne fissuré de lave, ailes à membrane de lave, **bulles de lave en ébullition** sur le dos (dès II), **crête de flammes** sur l'échine (dès III), étincelles et fumée, une queue avec une **flamme** au bout (dès II) |
| Glace | **veines de givre** scintillantes, double queue au stade V et une **couronne de cristaux de glace** qui relie ses cornes |
| Eau | **serpent de mer** (corps fin, long cou, longue queue qui ondule), écailles bleu-vert des abysses **lumineuses**, **dents en aiguilles**, **nageoires** sur le dos, en oreilles et au bout de la queue, deux grosses cornes couvertes de **mousse sous-marine** |
| Forêt | corps de **bûche** couvert de **mousse**, de **fleurs** (dès II) et de **champignons** (dès III), cornes en **branches** feuillues, queue en faisceau de **8 lianes** |
| Nether | **champignons du Nether** partout (verrues), **braises** qui luisent entre les écailles, 2 **cornes de diable**, **mâchoire inférieure** plus grande avec 2 **dents de sabre**, **3 queues fines** terminées par des **tridents** |
| End | le dragon classique, écailles fendues de **failles violettes** étoilées : couronne et double queue au stade V |
| Terra | **potelé**, peau de **roche craquelée** semée de **pépites d'or**, **plaques de roche** sur le dos avec des **cristaux d'or** (dès III), une seule corne sur le **nez**, queue massive à **massue** couverte de verrues, **2 paires d'ailes** (dès III) |
| Solaire | **une seule couleur dorée**, aucune corne, ailes en **lames de lumière**, une **épée de lumière** au bout de la queue (dès II), une **auréole** de lumière (dès III) |
| Tempête | **serpent d'orage** (corps fin, long cou, longue queue qui ondule), écailles gris d'orage parcourues d'**éclairs lumineux**, longues **moustaches électriques**, **nuages** sur le dos et au bout de la queue, cornes en **éclairs** |
| Lunaire | le dragon de la nuit : peau et ailes **étoilées**, **pierre de lune** sur le front (dès II), **croissant de lune** argenté sur la tête (dès III), double queue au stade V |
| Wither | **veines d'âmes** bleues, **couronne de petits crânes** noirs aux yeux bleus (1 au stade III, 3 au IV, 5 au V), une seule queue faite uniquement d'**os** |

- La **puissance** s'obtient en tuant des monstres (≈ PV max / 10) et surtout des dragons
  sauvages (40 × rang², donc 1000 pour un rang V).
- L'évolution est **automatique** dès que la puissance ET le défi sont validés.

### L'Alpha (après le stade V)
Un dragon légendaire peut devenir, **pour toujours**, l'**Alpha** de sa race.
1. **Les écailles alpha** : chaque dragon sauvage de **rang V** lâche **1 écaille alpha de sa race**
   (« Écaille alpha — Dragon de Feu »...). Il en faut **4 de ta propre race**. Au stade V, la **Corne du
   Défi** montre le repaire de rang V **de ta race** le plus proche (jusqu'à 8 000 blocs).
2. **La forge** : à l'**Autel draconique**, offre (clic droit avec l'objet) **4 écailles alpha** + **1 étoile
   du Nether** → l'**Orbe du pouvoir** de la race apparaît au-dessus de l'autel. Une orbe par race ; on peut
   en forger autant qu'on veut (pour un ami de la même race). Accroupi + clic droit main vide : reprendre
   les offrandes. Clic droit main vide : voir où en est la forge.
3. **L'éveil** : au stade V, **posé au sol**, clic droit avec l'orbe de ta race. Tu passes de force en
   **forme dragon** et la **cinématique en 3ᵉ personne** commence (8 s) : bandes noires, la caméra tourne
   autour de toi, l'orbe s'élève au-dessus de ta tête, tu montes dans les airs dans un tourbillon de ton
   élément, l'orbe plonge dans ton cœur, ton corps brille de blanc… explosion, onde de choc qui repousse
   tout autour, tu grandis et tu rugis, puis tu te poses. **Rien ne peut te blesser pendant l'éveil.**
   Échap : passer la cinématique. Tous les joueurs du serveur sont prévenus.

| | Stade V | **Alpha** |
|---|---|---|
| Vie | +15 cœurs | **+20 cœurs** |
| Armure / attaque | 10 / +9 | **14 / +12** |
| Souffle | 13 blocs, 7 dégâts | **16 blocs, 9 dégâts** |
| Énergie | 220 | **280** (se recharge plus vite) |
| Vol, sens des repaires | 0,12 · 1 024 blocs | **0,14 · 1 536 blocs** |
| Rugissement | toutes les 20 s | **toutes les 15 s**, plus large |
| Taille | dragon 1,10 · hybride ×1,20 | **dragon 1,32 · hybride ×1,28** |
| Hybride | poing 7,5 · +35 % de vitesse | **poing 9,5 · +40 %** |

L'alpha a aussi une aura plus dense, avec des étincelles de la couleur de sa race.

**En forme hybride**, l'alpha porte l'**écaille alpha sur le front** : un petit blason en relief aux couleurs
de sa race, au contour d'or, avec une arête au milieu (il remplace la pierre de lune de l'hybride lunaire).

**Le pouvoir alpha** (touche **B**, modifiable dans Options > Commandes comme toutes les touches) :
- en **forme dragon** seulement ; en hybride, la touche affiche un message ;
- il se **recharge en 45 s**, et la recharge **continue en forme hybride** : la barre « Alpha » du panneau
  est seulement cachée tant que tu es hybride, et réapparaît (avec sa vraie avancée) en forme dragon ;
- la recharge est sauvegardée : quitter le monde ne la remet pas à zéro.
- Chaque race reçoit son design alpha et son vrai pouvoir, une par une. Déjà fait : la **Forêt** et la **Tempête** (ci-dessous).
  Les autres races ont pour l'instant le pouvoir provisoire, l'**onde alpha** (un anneau de leur élément
  qui repousse les monstres).

#### L'alpha de la Forêt : la Tortue-Sylve
En forme dragon, l'alpha de la Forêt devient une **tortue alligator géante et volante** (modèle Blockbench
`tools/models/tortue_sylve_v2.bbmodel`, d'après la tortue alligator d'*Alex's Mobs*, avec nos proportions) :
**17 blocs de long, 12 de large, 12 de haut**. Bec crochu gris, yeux jaunes, barbe de lianes, carapace vert
sombre hérissée d'épines, ventre et dessous du cou en pierre taillée moussue, genoux et griffes grises. Sur son dos
vit un **petit biome de prairie** : deux collines d'herbe fleurie (une petite à l'avant gauche, une grande à
l'arrière droit) et **cinq arbres** de hauteurs différentes, d'où tombent des feuilles.

Après une modification du modèle dans Blockbench : `python tools/gen_tortoise_model.py` (réécrit la couche du
modèle dans `TortoiseModel.java`, la texture, la texture lumineuse des yeux et celle des nuages).

| | Tortue-Sylve |
|---|---|
| Vie / armure / attaque | +30 cœurs · 20 · +14 |
| Au sol | +55 % de vitesse, monte 3 blocs d'un pas, rien ne la repousse |
| En vol | plus lente que les autres alphas, vol lourd (la vitesse verticale change doucement) |

**Ses 3 états en vol :**
- **Propulsion** (elle avance) : pattes avant tendues vers l'avant et vers le haut, pattes arrière vers l'arrière,
  comme un cheval qui saute.
- **Sur place** : des nuages se rassemblent et tourbillonnent sous ses 4 pattes.
- **Dans sa carapace** : si elle reste **immobile pendant 30 secondes** (en vol comme au sol), elle rentre
  lentement la tête, les pattes et la queue dans sa carapace (un bruit de carapace qui se ferme) ; en vol ses
  nuages remontent sous son ventre, au sol elle se pose sur son ventre. Au moindre mouvement (ou avec le Rayon de Vie), elle ressort. Ce n'est qu'un effet visuel.
- **Descente** (elle perd de l'altitude) : les nuages glissent vers le bas, la descente est lente et lourde.
  Quand elle tombe sans voler, ses nuages la rattrapent (pas de dégâts) : avancer la fait dériver, reculer freine,
  s'accroupir la lâche.

**Le Rayon de Vie** (touche **B**, recharge 45 s) : un rayon continu de **6 s** sort de sa gueule, là où tu
regardes (B à nouveau pour l'arrêter ; la tortue avance au ralenti pendant le tir).
- Là où il touche un **sol naturel**, une forêt pousse **aussitôt et pour toujours** : arbres adultes (chêne,
  bouleau, épicéa, azalée, cerisier), herbe haute, fleurs, et parfois des récoltes mûres. Sur l'eau : des
  nénuphars. Seul le sol naturel change (terre, sable, gravier, pierre, netherrack, pierre de l'End... liste dans
  le tag `data/bdragon/tags/block/terraformable.json`), jamais les blocs d'une construction, ni la zone
  protégée du spawn.
- Il **soigne** les joueurs et les créatures amies qu'il traverse (5 cœurs par seconde + régénération), inflige
  **20 dégâts par seconde** aux monstres, et **terrasse l'Ender Dragon d'un coup** (avec sa vraie mort : portail
  et œuf).
- **Sa portée est ta distance d'affichage** : il s'arrête au bord des chunks chargés pour toi. Avec 4 chunks
  d'affichage, il ne va jamais plus loin que ces 4 chunks. Il ne lit ni ne touche jamais un chunk non chargé.
- Option `alphaForestTerraform` dans la configuration pour désactiver la pousse de la forêt (sur un serveur).

**Sa hitbox** : Minecraft ne donne à un joueur qu'**une seule boîte carrée, qui ne tourne pas**. La boîte solide de
la tortue est donc son « tronc » (3,6 × 10,8 blocs), et **9 zones de touche** invisibles la suivent et couvrent tout
son volume de 17 × 12 × 12 : un coup d'épée ou une flèche qui touche la carapace, la forêt, la tête ou une patte la
blesse vraiment (ses propres flèches et ses propres coups passent à travers). Il lui faut **11 blocs libres
au-dessus d'elle** pour se transformer (éveil, ou passage hybride → dragon).

En forme hybride, elle reste un humain avec l'écaille alpha sur le front, comme les autres alphas. Si la tortue
se retrouve sous un plafond (réapparition dans une maison, téléportation dans une grotte), elle reprend
d'elle-même sa forme hybride.

Aperçus (dessinés avec le vrai modèle) : `docs/apercu_tortue_sylve.png` (tailles), `docs/apercu_tortue_vol.png`
(les états du vol, ancien modèle), `docs/apercu_rayon_de_vie.png`, `docs/apercu_tortue_hitbox.png`, `docs/apercu_tortue_eveil.png`.

#### L'alpha de la Tempête : le Dragon des Tempêtes
En forme dragon, l'alpha de la Tempête devient un **long serpent d'orage** (modèle Blockbench original,
`tools/models/dragon_tempete_alpha_v2.bbmodel`, dont l'**anatomie** suit les longs serpents de *Cobblemon* comme
Rayquaza) : **40 blocs de long** une fois déroulé, un cou de 4 pièces, un torse, 15 pièces de corps (anneau fin, pièce
longue, pièce longue, anneau fin, pièce épaisse, trois fois) et 5 de queue. Un corps noir de nuages d'orage couvert
d'écailles et **craquelé d'éclairs bleus**, des oreilles de chat, des cornes de bouc recourbées, des sourcils, des
moustaches électriques, une crête sur tout le dos, des **nuages d'orage** posés de part et d'autre de la crête (les
nuages du dragon de stade V, `tools/add_storm_clouds.py`) et **deux longs bras** (épaule, bras, avant-bras, main à trois doigts
de deux phalanges, serres d'aigle). Ses yeux, ses éclairs, ses crêtes, ses moustaches et ses serres brillent dans le noir.

| | Dragon des Tempêtes |
|---|---|
| Vie / armure / attaque | +22 cœurs · 15 · +13 |
| Au sol | +70 % de vitesse, rien ne le repousse |
| En vol | **l'alpha le plus rapide** : environ **60 blocs par seconde** (120 en sprint), le double des autres alphas |

**Ses mouvements** (son corps est une chaîne de 25 pièces ; la façon de bouger suit Rayquaza, Léviator et Dracolosse
de *Cobblemon*, mais les animations sont recalculées par Bdragon) :
- **Immobile au sol** : il se **dresse comme Rayquaza**, le torse droit, le **cou en S**, la tête à l'horizontale (elle
  suit ton regard), les **bras tendus** et le corps **enroulé en spirale** au sol. Il **respire** lentement.
- **Immobile dans les airs** : une **tornade** le porte. La tête et les bras sont **au centre**, son corps s'enroule
  autour de lui en un **cône qui descend et se resserre** (jusqu'à environ 4 blocs sous ses pieds), et le **vent**
  tourne autour de lui (nuages, rafales, souffle de vent). Ses mains lâchent leurs nuages.
- **Au sol, en mouvement** (comme Léviator) : l'avant dressé, des **bosses** courent le long du corps.
- **En vol** (comme Léviator dans les airs) : une **vague** court le long du corps, plus forte vers la queue, avec un
  léger balancement de côté ; tout le corps **s'incline avec sa trajectoire** : **nez en l'air quand il monte** (jusqu'à
  60°), un peu vers le bas quand il descend doucement, et il se courbe dans les virages. Ses **serres s'accrochent à
  deux petits nuages** qui le portent (seulement quand il bouge).
- **En piqué** (il descend vite et raide, comme Dracolosse) : **droit comme une flèche**, nez en bas (jusqu'à 75°), les
  vagues presque arrêtées, les **bras plaqués en arrière**.
- **En vol et en piqué, son corps suit le chemin de sa tête** (comme le serpent des vieux jeux *Snake*) : chaque pièce
  se pose là où la précédente est passée, la queue passe là où la tête est passée, et dans un virage serré la tête peut
  croiser sa queue. Ses zones de touche suivent le même chemin.
- Il passe d'un état à l'autre en douceur.

**L'Orage** (touche **B**, recharge **3 minutes**) : il **rugit** et appelle un orage d'**une minute** autour de lui
(nuages noirs, pluie, éclairs de partout, sans dégâts) ; l'orage le suit. **10 éclairs** frappent les monstres autour de
lui (jusqu'à 40 blocs), **l'un après l'autre** (un toutes les 3 s ; s'il n'y a aucun monstre, l'éclair attend qu'il en
vienne un) :
- chaque éclair choisit un monstre **au hasard**, mais **celui qui a le plus de vie a le plus de chances** (jusqu'à
  3 fois plus qu'un petit monstre), et **celui qui vient d'être frappé a 5 fois moins de chances** d'être frappé encore ;
- **80 dégâts** par éclair (et il met le feu) : **3 éclairs abattent l'Ender Dragon**, même soigné par ses cristaux ;
- il est **immunisé contre la foudre** (comme tous les dragons de la Tempête), et il a Force II pendant son Orage.

**Son corps** : comme la tortue, **9 zones de touche** suivent sa tête et son corps (dressé, au sol ou en vol) : un coup
ou une flèche qui le touche le blesse vraiment. Sa boîte solide fait 2 × 5,9 blocs : il lui faut **6 blocs libres
au-dessus de lui** pour se transformer, sinon il reprend sa forme hybride.

Les réglages de son animation et de son pouvoir sont tous dans `dragon/AlphaStorm.java` et `dragon/StormSpine.java`.
Aperçu (dessiné avec le vrai modèle) : `docs/apercu_dragon_tempete.png`.

**Modifier son modèle** : ouvre `tools/models/dragon_tempete_alpha_v2.bbmodel` dans Blockbench, change ce que tu veux
(en gardant les noms des groupes et la longueur des pièces du corps, qui sont dans `StormSpine.java`), enregistre, puis
lance `python tools/gen_storm_model.py` (le modèle Java et la texture) et `python tools/gen_storm_textures.py` (ses
parties lumineuses). L'ancien modèle reste dans `tools/models/dragon_tempete_alpha.bbmodel`.

#### L'alpha du Nether : le Démon du Nether
L'alpha du Nether devient un **démon ailé** (modèle Blockbench `tools/models/demon_nether_alpha.bbmodel`), de la
taille des autres alphas, aux **couleurs sombres du dragon du Nether de stade V** (sa texture), la peau craquelée de
**lave** (elle brille dans le noir, comme ses yeux) :
- torse et jambes repris du **Jersey Devil** (dessinés deux fois plus grands), longs bras de gorille ;
- la **tête du dragon du Nether de stade V** (cornes de démon, mâchoire du bas plus grande et plus large que celle du
  haut), avec deux **canines géantes** qui montent de la mâchoire du bas et se recourbent vers le museau, et deux autres
  qui descendent hors de la gueule (vers l'extérieur) et se recourbent vers l'intérieur : elles ne se croisent jamais ;
- les **ailes du dragon de stade V** (aux couleurs sombres du démon), repliées le long du dos sans toucher le corps ;
- les **3 queues à trident** du stade V sur une **longue base commune** (6 pièces, puis elles se séparent en 3) ;
- sur le dos **7 verrues** au hasard (verrues du Nether, champignons carmin et biscornus).

**Ses mouvements** : au sol il marche à **quatre pattes mais dressé, comme un gorille**, sur les phalanges de ses longs
bras. En vol il bat des ailes **à un rythme régulier**, comme les dragons (sa vitesse est celle des autres alphas).
Immobile dans les airs, il se tient droit et bat des ailes. En vol, ses ailes battent comme celles des dragons.
Immobile **30 s** (au sol ou en l'air), il **bâille** : sa
gueule s'ouvre grand, sa tête se lève un peu et tourne vers sa droite, comme un lion qui rugit.

**Son pouvoir (touche B, recharge 3 min) : le Territoire.**
1. **L'incantation (5 s)** : il lève la main droite, le dos et le corps courbés vers sa gauche (le bras gauche reste à
   sa place, à côté du corps), et un **cercle
   d'incantation** de feu se dessine au-dessus de lui.
2. **Le sol surgit (10 s)** : sur un **cercle de 16 blocs** autour de lui, le sol naturel devient basalte, roche noire,
   netherrack, sable des âmes, magma et blocs de verrues, et **9 pics pointus comme des stalagmites** (jamais des
   colonnes) montent du sol, les plus hauts **jusqu'à y = 145**. Ce changement est **définitif** ; il ne remplit que l'air
   et les plantes et ne change que le sol naturel (jamais une construction), respecte la protection du spawn, et peut
   être coupé dans la configuration (`alphaNetherTerraform`).
3. **La pluie acide (30 s)**, en même temps que le sol : elle **le suit** et, chaque seconde, brûle d'**un demi-cœur, à
   travers l'armure**, toutes les créatures sous elle (16 blocs autour de lui), soit **15 cœurs** en tout. Lui et ses
   animaux n'ont rien ; un toit protège de la pluie.

Les réglages sont dans `dragon/AlphaNether.java` (temps, rayon, hauteur, dégâts) et `player/NetherDomain.java` (le
terrain). **Modifier son modèle** : ouvre `tools/models/demon_nether_alpha.bbmodel` dans Blockbench (en gardant les noms
des groupes), enregistre, puis lance `python tools/gen_demon_model.py` (le modèle Java, sa texture et ses parties
lumineuses). `tools/build_nether_demon.py` reconstruit le modèle depuis le Jersey Devil et les pièces du dragon de stade V
(`tools/models/dragon_nether_stage5.json`) : il écrase tes changements.

#### L'alpha de la Lumière : le Destrier solaire (1.8)
L'alpha de la Lumière devient un **cheval ailé en or** (modèle Blockbench `tools/models/dragon_soleil_alpha.bbmodel`,
d'après le croquis `alpha/dragon_solaire/croquis_alpha_dragon_soleil.png`), de **12 blocs de haut** jusqu'au bout des
ailes : corps et tête de cheval, finition jaune et orange, **yeux bleus**, grandes **ailes à plumes**, **queue de
dragon / crocodile** de 4 blocs hérissée de pics, **auréole d'ange** au-dessus de la tête et un **soleil miniature**
(4 × 4 × 4 blocs) derrière et au-dessus de la tête. Le soleil, l'auréole et les yeux brillent dans le noir. Comme la
Tortue-Sylve, son corps est géant (boîte du joueur 3,3 × 9,9 blocs, zones de touche sur tout le corps).

**Ses mouvements** (animations du fichier Blockbench) : au sol il **marche comme un cheval** ; en vol il **vole comme un
pégase** (grands battements d'ailes, pattes avant repliées, pattes arrière tendues). Son soleil **tourne très
lentement** sur X et Y. **Immobile 1 min** (sans bouger ni tourner la tête), son soleil **accélère pendant 10 s**,
**rétrécit à partir de la 3e seconde**, devient **poussière d'étoile**, réapparaît aussitôt et **reprend sa taille** en
moins d'une seconde ; puis ça recommence chaque minute tant que le joueur ne bouge pas.

**Son pouvoir (touche B, recharge 45 s) : le Soleil.**
1. **Il se cabre** sur ses pattes arrière, comme le cheval qui fait tomber son cavalier, les pattes avant battant l'air
   (3 s).
   **S'il fait nuit** (quand on peut dormir dans un lit), **le jour se lève** aussitôt (comme `/time set day`).
2. **Le lancer (à 0,9 s)** : le soleil quitte sa place et file **tout droit là où tu vises** (jusqu'à 160 blocs,
   3 blocs par tick), comme une météorite. Il **explose au premier obstacle** sur sa route : un bloc ou une créature.
3. **L'explosion** : une **boule de lumière blanche** grandit jusqu'à **10 blocs de rayon** (1,5 s). **Tout disparaît**
   à l'intérieur **sauf la bedrock** (et les blocs incassables) : un cratère de 10 blocs de profondeur. Les créatures
   prises dedans sont gravement blessées (60 au centre, 20 au bord) et projetées ; lui et ses animaux n'ont rien. La
   protection du spawn est respectée et la destruction des blocs peut être coupée dans la configuration
   (`alphaSunDestroy`).
4. **Le soleil se reconstruit** au-dessus de sa tête pendant la recharge : on peut le relancer quand il est entier.

Les réglages sont dans `dragon/AlphaSun.java` (taille, temps, vitesse, portée, rayon, dégâts) et `player/SunStrike.java`
(le lancer et l'explosion). **Modifier son modèle ou ses animations** : ouvre `tools/models/dragon_soleil_alpha.bbmodel`
dans Blockbench (en gardant les noms des groupes et des animations), enregistre, puis lance
`python tools/gen_sun_model.py` (le modèle Java, ses animations, sa texture et ses parties lumineuses).
`tools/build_sun_alpha.py` reconstruit le modèle de départ : il écrase tes changements.

#### L'alpha de la Lune : le Dragon lunaire (1.8)
L'alpha de la Lune garde le **long corps du Dragon des Tempêtes** (sans cornes, moustaches ni crêtes) sur **4 pattes
griffues**, avec la **tête et le cou d'un Ender Dragon** (mêmes proportions), un **croissant de lune** sur la tête et une
**boule de lune** au bout de la queue (modèle Blockbench `tools/models/dragon_lune_alpha.bbmodel`, d'après le croquis
`alpha/dragon_lunaire/alpha_lunaire.png`). Couleur d'un **ciel de pleine nuit bien éclairé** (bleu nuit, étoiles),
lunes jaunes/blanches, étoiles blanches/bleues et yeux bleus qui brillent dans le noir. Des **étoiles tournent
lentement autour de son corps**, comme en orbite.

**Ses mouvements** : ceux du Dragon des Tempêtes, avec le cou court de l'Ender Dragon. En vol, **plus de nuages** :
juste une **brise du vent de la nuit** le long du corps. **Immobile dans les airs**, une **plateforme en forme
d'étoile** le porte (à la place de la tornade).

**Son pouvoir (touche B, recharge 45 s) : la Lune.** Il **rugit** et, s'il fait jour, **la nuit tombe** (comme
`/time set night`). Puis il **lance une lune de sa gueule** là où tu vises : **mêmes dégâts et même explosion que le
soleil** du Destrier solaire (boule blanche de 10 blocs de rayon, tout disparaît sauf la bedrock, option
`alphaSunDestroy`).

**Modifier son modèle** : ouvre `tools/models/dragon_lune_alpha.bbmodel` dans Blockbench (en gardant les noms des groupes),
enregistre, puis lance `python tools/gen_moon_model.py`. Ne relance pas `tools/build_moon_alpha.py` : il repartirait de
zéro.

### Les nids des dragons sauvages (1.8)
Chaque dragon sauvage de rang III à V vit dans un **nid** (structure `data/bdragon/structure/nid_dragon.nbt`,
la même pour toutes les races : seuls les blocs et le butin changent). Le nid est toujours **à découvert**,
comme un village : jamais sous l'eau, sous terre ni sous une autre structure (dans le Nether : au sol d'une
grande caverne). Laine rose = où le dragon apparaît, laine verte = coffre.

| Race | Biomes des nids | Blocs (à la place d'obsidienne / obsidienne pleureuse / magma) |
|---|---|---|
| Feu | forêts carmin et biscornues (Nether) | obsidienne / obsidienne pleureuse / magma |
| Glace | là où vivent les ours polaires (océans gelés, icebergs, plaines enneigées, pics de glace) | neige / glace bleue / glace compactée |
| Forêt | toutes les forêts sauf les marais | herbe / pierre moussue / bloc de mousse |
| Nether | deltas de basalte | netherrack / basalte / magma |
| End | îles extérieures de l'End | briques de l'End / obsidienne / purpur |
| Solaire | savanes et déserts | grès taillé / sable / verre |
| Lunaire | marais | tuiles d'ardoise des abîmes / améthyste / lanterne aquatique |
| Wither | vallée des âmes | sable des âmes / bloc de charbon / obsidienne |
| Tempête | hautes montagnes (pics, pentes enneigées) | briques d'ardoise des abîmes / briques de tuf / glace compactée |
| Terra | badlands (mesa) | terre cuite / grès rouge / terre cuite orange |
| Eau | océans et rivières | briques de prismarine / prismarine sombre / lanterne aquatique |

- **Rareté** (par rapport à un village) : rang III 1,25× plus rare, rang IV 2×, rang V 2,5×.
- **Rang** selon la distance au spawn : < 800 blocs → III, 800-2000 → III-IV, plus loin → III à V
  (Nether : < 250 → III, 250-700 → III-IV, plus loin → III à V).
- **Butin du coffre** : rang III ≈ épave, rang IV ≈ bastion, rang V ≈ cité de l'End.
- **Rangs I et II** : ils n'ont pas de nid. Souffle dans la **Corne du Défi** quand ton défi est de
  rang I ou II : un jeune dragon de ce rang arrive et t'attaque.
- Vaincu, un autre dragon reprend le nid **3 jours** plus tard (configurable).
- **JourneyMap** : les nids découverts et ressentis s'affichent sur les cartes ; sans JourneyMap, la
  liste est dans le codex.
- **Combat (1.8)** : les dragons sauvages ont moins de vie et d'armure, ton souffle ignore leur temps
  d'invulnérabilité, et tu frappes plus fort un dragon de rang inférieur ou égal à ton stade.

### Les villages abandonnés (1.8)
Des villages en ruine, vides (ni villageois, ni golem, ni chat), « comme si tous les villageois
s'étaient transformés et avaient quitté le village ». À la place de la cloche ou de la fontaine se
dresse un **Autel du Dragon** (posé quand un joueur entre dans le village). Plaines, désert, savane,
neige et taïga ; plus rares que les villages normaux et jamais collés à eux.

### Effet du stade V sur le sol (temporaire, comme les bottes Frost Walker)
Feu → magma · Glace → neige + l'eau gèle · Eau → coraux + la lave se fige · Forêt → mousse + fleurs ·
Nether → nylium cramoisi · End → pierre de l'End · Terra → briques · Solaire → or · Tempête → cuivre ·
Lunaire → purpur · Wither → terre des âmes.
Les blocs redeviennent normaux après quelques secondes et ne peuvent pas être minés.

### Commandes d'administration (triche / tests)
Pour les opérateurs, et pour le joueur qui héberge une partie solo (même sans triche).
```
/bdragon transform <race>      devenir directement un dragon
/bdragon stage <1-5>           changer de stade
/bdragon form <hybrid|dragon>
/bdragon power <n>             fixer la puissance
/bdragon defeated <rang>       marquer un défi comme gagné
/bdragon lair                  révèle le repaire le plus proche de chaque rang
/bdragon musee                 construit le musée là où tu te trouves
/bdragon summon <race> <rang>  faire apparaître un dragon sauvage (hors repaire)
/bdragon alpha [joueur]        passer au stade V si besoin et jouer l'éveil de l'alpha (cinématique)
/bdragon alpha on|off [joueur] devenir / ne plus être alpha tout de suite
/bdragon alpha race <race> [joueur]  devenir un dragon de cette race si besoin, puis jouer son éveil
/bdragon alpha cooldown off|on   plus de recharge du pouvoir alpha (touche B) : on l'enchaîne, même pendant qu'il agit (jusqu'à l'arrêt du serveur)
/bdragon alpha recharge [joueur]  le pouvoir alpha est prêt tout de suite
/bdragon info | reset
```

### Configuration
`config/bdragon-common.toml` : effet du sol, durée avant retour des blocs, repaires activés,
jours avant qu'un repaire se repeuple, codex donné à l'arrivée, souffle de feu qui enflamme les blocs,
forêt du Rayon de Vie (`alphaForestTerraform`), terrain du Territoire de l'alpha du Nether (`alphaNetherTerraform`),
cratère du Soleil de l'alpha de la Lumière (`alphaSunDestroy`).

---

## 3. Crédits et licence
- **Modèle 3D et animations du dragon** : *Dragon Mounts: Legacy* — Kay9 et Nico Bergemann
  (barracuda415), licence **GPL-3.0**. Le modèle a été porté sur 26.3 et enrichi des parties de
  stades, de l'anatomie de chaque race (flamme, couronne de glace, nageoires, branches, lianes,
  tridents, massue, auréole, éclairs, os...) et de la forme hybride.
- Textures repeintes (feu volcanique, bûche de la forêt, dragon solaire uni) et parties des races :
  générées par script à partir des textures d'origine.
- **Textures des dragons** : *Dragon Mounts: Legacy* (GPL-3.0) et *Dragon Mounts: Even More Dragons*
  — EvilNick, BarracudaAta4, Lintha437, Kay9Unit, FuriusMaximus (licence **MIT**).
- Inspiré de **Morph** (iChun) pour la transformation (seulement l'idée : le code a été réécrit).
- **JourneyMap API** (Techbrew) : utilisée seulement à la compilation, elle n'est pas incluse dans le mod.
- Code, quêtes, races, stades, formes, repaires, alpha et codex : Bdragon, par YouBaRab.
- Textures des écailles alpha et des orbes : dessinées par script (`tools/gen_alpha_items.py`).
- Tortue-Sylve (alpha de la Forêt) : modèle et texture faits dans Blockbench par Bdragon
  (`tools/models/tortue_sylve_v2.bbmodel`, converti par `tools/gen_tortoise_model.py`), d'après la tortue alligator
  d'**Alex's Mobs** (Alexthe666, licence **LGPL-3.0**) pour la forme de la tête, du corps, des pattes et de la queue.
- Dragon des Tempêtes (alpha de la Tempête) : modèle et texture originaux de Bdragon, faits dans Blockbench
  (`tools/models/dragon_tempete_alpha_v2.bbmodel`), découpés en chaîne d'os par `tools/gen_storm_model.py`. Son
  anatomie et sa façon de bouger s'inspirent de Rayquaza, Léviator et Dracolosse de **Cobblemon** (seulement l'idée :
  aucun cube, aucune texture ni aucune animation de Cobblemon n'est repris ; Pokémon est une marque de Nintendo).
- Démon du Nether (alpha du Nether) : tête, torse, ailes et jambes repris du modèle du **Jersey Devil** (mod
  *Jersey Devil*, Andre's 5000W Bass-M, fait avec MCreator, licence **Academic Free License 3.0**), agrandis et
  complétés par Bdragon (canines, mâchoire, bras, 3 queues, verrues, texture de lave : `tools/build_nether_demon.py`).
  Sa posture de gorille s'inspire du gorille d'**Alex's Mobs** (seulement l'idée). Attention : la Free Software
  Foundation considère l'AFL-3.0 comme **incompatible avec la GPL** ; avant de publier le mod, demande l'accord de
  l'auteur du Jersey Devil (ou remplace ces cubes par des cubes à toi).

Comme le modèle vient d'un projet GPL-3.0, **Bdragon est sous licence GPL-3.0** (voir
`LICENSE.txt`) : tu peux le partager et le publier, à condition de fournir aussi le code source.
