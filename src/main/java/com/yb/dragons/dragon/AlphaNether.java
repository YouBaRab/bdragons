package com.yb.dragons.dragon;

import org.jspecify.annotations.Nullable;

/**
 * The Alpha of the Nether: the Demon of the Nether ("Démon du Nether"), a winged red demon that walks on four legs but
 * raised, like a gorilla, its long arms on their knuckles. Head with ram horns, a lower jaw larger than the upper one, two
 * giant lower canines rising in front of its snout and two upper canines going down outside its jaw, three thin tails
 * ending in tridents, nether warts and crimson and warped fungi on its back, its red skin cracked with lava.
 *
 * <p>Its body comes from the Jersey Devil of the mod "Jersey Devil" (Andre's 5000W Bass-M, MCreator, Academic Free
 * License 3.0): the head, the torso, the wings and the legs, drawn twice bigger. The model is the Blockbench model
 * {@code tools/models/demon_nether_alpha.bbmodel} (built by {@code tools/build_nether_demon.py}). It keeps the size of the
 * other alphas: the player keeps the box of the alpha, no hit zones are needed.</p>
 *
 * <ul>
 *   <li>On the ground it walks on its four limbs (the arms and the legs of each diagonal together).</li>
 *   <li>It flies like the dragons, beating its wings at a steady pace (its speed is the one of the alphas).</li>
 *   <li>Still for {@link #YAWN_AFTER} ticks (on the ground or in the air), it yawns: its mouth opens wide, its head rises
 *       a little and turns to its right with a small roll, like a roaring lion.</li>
 * </ul>
 *
 * <p>Its power (key B): its Territory ({@link com.yb.dragons.player.NetherDomain}).</p>
 */
public final class AlphaNether {
    /** World blocks per model block (16 model pixels): 4.9 blocks high, 7 long (without the tails). */
    public static final float MODEL_SCALE = 0.85F;

    // --- the Territory (key B) -------------------------------------------------------------------------------
    /** The incantation: the demon raises its right hand, its body bends to its left, a circle is drawn above it. */
    public static final int CAST_TICKS = 5 * 20;
    /** Then the ground rises: the territory takes this long to form fully. */
    public static final int FORM_TICKS = 10 * 20;
    /** The acid rain starts at the end of the incantation and follows the demon this long. */
    public static final int RAIN_TICKS = 30 * 20;
    /** The whole power, from the key to the end of the rain. */
    public static final int DURATION = CAST_TICKS + RAIN_TICKS;
    /** Radius of the territory (a disc around the demon) and of the acid rain. */
    public static final int RADIUS = 16;
    /** The highest peaks of the territory reach this height (y). */
    public static final int PEAK_Y = 145;
    /** Damage of the acid rain every second (half a heart: 15 hearts in 30 seconds); it goes through armour. */
    public static final float ACID_DAMAGE = 1.0F;
    /** Recharge of the power: 3 minutes (from the moment it is called). */
    public static final int POWER_COOLDOWN = 3 * 60 * 20;

    // --- the yawn ---------------------------------------------------------------------------------------------
    /** Ticks without moving before the demon yawns: 30 seconds (then again every 30 seconds). */
    public static final int YAWN_AFTER = 30 * 20;
    /** Length of a yawn. */
    public static final int YAWN_TICKS = 70;

    private AlphaNether() {}

    /** The full dragon form of the alpha of the Nether is the demon. */
    public static boolean isDemon(@Nullable DragonType type, int stage, boolean alpha, DragonForm form) {
        return type == DragonType.NETHER && alpha && stage >= 5 && form == DragonForm.DRAGON;
    }
}
