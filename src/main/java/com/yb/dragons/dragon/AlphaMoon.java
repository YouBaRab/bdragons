package com.yb.dragons.dragon;

import org.jspecify.annotations.Nullable;

/**
 * The Alpha of the moonlight: the Moon Dragon ("Dragon lunaire"), after the sketch of the author: the long body of the
 * Tempest Dragon (without its horns, its whiskers nor its crests) on four clawed legs, the head and the neck of an
 * Ender Dragon with a crescent moon standing on its head, a ball of moon at the end of its tail, stars in orbit all
 * around its body, painted like a well lit night sky (night blue, moons yellow and white, stars white and blue, blue
 * eyes). The model is the Blockbench model {@code tools/models/dragon_lune_alpha.bbmodel} (started by
 * {@code tools/build_moon_alpha.py}, then retouched by the author), drawn like the Tempest Dragon
 * ({@link AlphaStorm#MODEL_SCALE}); it moves like it ({@link StormSpine}, with the short neck {@link StormSpine#MOON_LENGTHS}).
 *
 * <ul>
 *   <li>Moving in the air: no clouds in its talons, only a breeze of the night wind.</li>
 *   <li>Still in the air: a platform shaped like a star carries it (instead of the tornado of the Tempest Dragon).</li>
 *   <li>Its stars turn slowly around its body, like in orbit.</li>
 * </ul>
 *
 * <p>Its power (key B): its roar brings the night, then it throws a moon like the Sun Steed throws its sun, with the
 * same damage and the same explosion ({@link com.yb.dragons.player.SunStrike}).</p>
 */
public final class AlphaMoon {
    /** The roar that brings the night, before the moon is thrown (ticks). */
    public static final int ROAR_TICKS = AlphaSun.LAUNCH_TICKS;

    private AlphaMoon() {}

    /** The full dragon form of the alpha of the moonlight is the Moon Dragon. */
    public static boolean isMoon(@Nullable DragonType type, int stage, boolean alpha, DragonForm form) {
        return type == DragonType.MOONLIGHT && alpha && stage >= 5 && form == DragonForm.DRAGON;
    }
}
