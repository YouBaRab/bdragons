package com.yb.dragons.dragon;

import org.jspecify.annotations.Nullable;

/**
 * The Alpha of the storm: the Tempest Dragon ("Dragon des Tempêtes"), a long serpent of storm clouds cracked with
 * lightning, with cat ears, curled goat horns, brows, electric whiskers and two long arms ending in eagle talons that
 * grip the clouds. Its anatomy and its way of moving follow the long serpent dragons of Cobblemon (Rayquaza, Gyarados):
 *
 * <ul>
 *   <li>Still on the ground, it rears like Rayquaza: the torso straight up, the neck in an S, the head level, the arms
 *       out, the body spiralling on the ground; it breathes slowly.</li>
 *   <li>Still in the air, it is held up by a tornado: the head and the arms at the centre, its body winds down around
 *       its chest in a cone that gets narrower, and the wind turns around it.</li>
 *   <li>Moving on the ground, the front raised, humps run back along its body.</li>
 *   <li>Flying, a wave runs back along its body like Gyarados in the air, the whole body tilted with its path: nose up
 *       when it climbs. Diving, it goes straight like an arrow, nose down, the arms swept back. It is the fastest of the
 *       alphas.</li>
 * </ul>
 *
 * <p>The body is a chain of 25 pieces posed by {@link StormSpine} (shared by the model and the hit zones of the
 * server). The model is the Blockbench model {@code tools/models/dragon_tempete_alpha_v2.bbmodel}, drawn
 * {@link #MODEL_SCALE} times bigger than its pixels.</p>
 *
 * <p>Its power (key B, recharge 3 minutes): the Storm ({@link com.yb.dragons.player.StormCall}).</p>
 */
public final class AlphaStorm {
    /** World blocks per model block (16 model pixels): the body is about 1.25 blocks thick and 40 blocks long when straight. */
    public static final float MODEL_SCALE = 1.25F;
    /** Blocks per model pixel. */
    public static final float PX = MODEL_SCALE / 16.0F;
    /**
     * Scale attribute of the player: box 0.6 x 1.8 -> 2 x 5.9 blocks. The eyes are then near the height of the raised
     * head of the rearing serpent.
     */
    public static final float SCALE = 3.3F;

    // --- the Storm (key B) ------------------------------------------------------------------------------------
    /** The storm lasts one minute. */
    public static final int STORM_DURATION = 60 * 20;
    /** Recharge of the power: 3 minutes (from the moment it is called). */
    public static final int POWER_COOLDOWN = 3 * 60 * 20;
    /** The roar that calls the storm (the mouth is open). */
    public static final int ROAR_TICKS = 30;
    /** Aimed lightning bolts of one storm, one after the other. */
    public static final int STRIKES = 10;
    /** First aimed bolt, then one every {@link #STRIKE_INTERVAL} ticks (when there is no target, it waits {@link #STRIKE_RETRY}). */
    public static final int FIRST_STRIKE = 30;
    public static final int STRIKE_INTERVAL = 60;
    public static final int STRIKE_RETRY = 10;
    /** The aimed bolts look for hostile mobs up to this distance. */
    public static final double STRIKE_RADIUS = 40.0;
    /** Damage of one aimed bolt: 3 of them kill the Ender Dragon (200 health, even healed by its crystals). */
    public static final float BOLT_DAMAGE = 80.0F;
    /** The harmless bolts that fall everywhere during the storm, around the dragon: one chance in this many each tick. */
    public static final double AMBIENT_RADIUS = 32.0;
    public static final int AMBIENT_EVERY = 18;
    /** Chance for the target of the last bolt to be chosen again (relative weight). */
    public static final float SAME_TARGET_WEIGHT = 0.2F;

    /** Flying speed of the game: about 60 blocks per second (120 when sprinting), twice the other alphas. */
    public static final float FLYING_SPEED = 0.276F;

    private AlphaStorm() {}

    /** The full dragon form of the alpha of the storm is the serpent. */
    public static boolean isSerpent(@Nullable DragonType type, int stage, boolean alpha, DragonForm form) {
        // the Moon Dragon (alpha of the moonlight) has the long body of the Tempest Dragon
        return (type == DragonType.STORM || type == DragonType.MOONLIGHT) && alpha && stage >= 5 && form == DragonForm.DRAGON;
    }
}
