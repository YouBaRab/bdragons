package com.yb.dragons.dragon;

import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The Alpha, the step after the legendary stage.
 * <ol>
 *   <li>Every wild dragon of rank V drops one Alpha Scale of its race.</li>
 *   <li>At the Dragon Altar, 4 scales of a race + 1 Nether Star are forged into the Orb of Power of that race.</li>
 *   <li>A legendary dragon of that race consumes the orb: he is forced into his dragon form and the awakening
 *       ritual (a cinematic in third person) turns him into the Alpha of his race, forever.</li>
 * </ol>
 * The timeline of the ritual is shared by the server (effects, sounds) and the clients (animation, camera).
 */
public final class DragonAlpha {
    /** Alpha scales needed for one orb (+ 1 Nether Star). */
    public static final int SCALES_PER_ORB = 4;
    /**
     * Recharge of the alpha power (ticks): 45 seconds. It recharges in both forms (the bar is only shown in
     * the dragon form, the only one that can use the power).
     */
    public static final int POWER_COOLDOWN = 45 * 20;

    /** Recharge of the alpha power of a race: 45 seconds, 3 minutes for the Storm of the alpha of the storm. */
    /** Tests (command "/bdragon alpha cooldown off"): the alpha powers have no recharge until the server stops. */
    public static boolean noCooldown;

    public static int powerCooldown(@Nullable DragonType type) {
        if (type == DragonType.NETHER) return AlphaNether.POWER_COOLDOWN;
        return type == DragonType.STORM ? AlphaStorm.POWER_COOLDOWN : POWER_COOLDOWN;
    }

    // --- timeline of the awakening ritual (ticks) ------------------------------------------------------------
    /** The orb leaves the claws and rises above the head. */
    public static final int ORB_RISE_END = 40;
    /** The dragon rises into the air, a vortex of its element turns around it. */
    public static final int LIFT_START = 30;
    public static final int LIFT_END = 70;
    /** The orb dives into the chest of the dragon and breaks. */
    public static final int ORB_BREAK = 88;
    /** The explosion: the dragon becomes the alpha (bigger, new look). */
    public static final int CLIMAX = 100;
    /** The roar of the new alpha. */
    public static final int ROAR_END = 128;
    /** The alpha lands again. */
    public static final int LAND_START = 128;
    public static final int LAND_END = 150;
    /** End of the ritual (the camera is back behind the player). */
    public static final int DURATION = 170;

    /** Height (blocks) of the levitation at the top of the ritual. */
    public static final float LIFT_HEIGHT = 1.6F;

    private DragonAlpha() {}

    /** 0 -> 1 with a smooth start and end. */
    public static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Progress of {@code age} between {@code from} and {@code to}, smoothed. */
    public static float phase(float age, float from, float to) {
        return smooth((age - from) / (to - from));
    }

    /** Height of the dragon above the ground during the ritual (blocks). */
    public static float lift(float age) {
        if (age < 0 || age > DURATION) return 0;
        float up = phase(age, LIFT_START, LIFT_END);
        float down = phase(age, LAND_START, LAND_END);
        float bob = age > LIFT_END && age < LAND_START ? Mth.sin(age * 0.18F) * 0.12F : 0;
        return LIFT_HEIGHT * (up - down) + bob;
    }

    /** Body pitch in degrees (the dragon rears up to roar). */
    public static float pitch(float age) {
        if (age < 0 || age > DURATION) return 0;
        float charge = 18 * phase(age, LIFT_START, ORB_BREAK);
        float roar = 14 * phase(age, CLIMAX - 4, CLIMAX + 4) * (1 - phase(age, ROAR_END - 6, LAND_END));
        float rest = 1 - phase(age, LAND_START, LAND_END);
        return (charge + roar) * rest;
    }

    /** White glow of the body: it pulses faster and faster, then the flash. */
    public static float flash(float age) {
        if (age < 60 || age > CLIMAX + 12) return 0;
        if (age >= CLIMAX) return 1 - (age - CLIMAX) / 12F;
        float build = (age - 60) / (CLIMAX - 60F);
        float pulse = 0.5F + 0.5F * Mth.sin(age * (0.4F + build * 1.6F));
        return Mth.clamp(build * (0.35F + 0.65F * pulse), 0, 1);
    }

    /** Size of the dragon: legendary before the climax, alpha after (with a short overshoot). */
    public static float renderScale(float age, float legendary, float alpha) {
        if (age < CLIMAX) return legendary;
        float grow = phase(age, CLIMAX, CLIMAX + 14);
        float overshoot = Mth.sin(Mth.clamp((age - CLIMAX) / 22F, 0, 1) * Mth.PI) * 0.08F;
        return Mth.lerp(grow, legendary, alpha) * (1 + overshoot);
    }

    /** The mouth is open during the roar. */
    public static boolean roaring(float age) {
        return age >= CLIMAX && age < ROAR_END;
    }

    /** The wings beat while the dragon floats. */
    public static boolean floating(float age) {
        return age >= LIFT_START && age < LAND_END;
    }
}
