package com.yb.dragons.dragon;

import org.jspecify.annotations.Nullable;

/**
 * The real numbers of a player dragon: those of his stage, or the stronger ones of the Alpha
 * (the legendary dragon who consumed the Orb of Power of his race, see {@link DragonAlpha}).
 * Every system reads its values here, so the alpha bonus is applied everywhere at once.
 */
public record DragonStats(int bonusHealth, int bonusArmor, int bonusAttack, double hybridAttack, float scaleAttribute,
                          float renderScale, float hybridScale, float hybridSpeed, float dragonSpeed, float flyingSpeed,
                          int breathRange, float breathDamage, int maxEnergy, float energyRegen, float cameraDistance,
                          float reach, float stepHeight, int safeFall, int senseRadius, int roarCooldown) {

    /** Energy regained every tick. */
    public static final float ENERGY_REGEN = 0.5F;
    /** Ticks between two roars. */
    public static final int ROAR_COOLDOWN = 400;

    private static final DragonStats[] STAGES = new DragonStats[DragonStage.values().length];
    private static final DragonStats ALPHA;
    /** The alpha of the forest is the Sylvan Tortoise: 17 x 12 x 12 blocks, slow, very tough (see {@link AlphaForest}). */
    private static final DragonStats FOREST_ALPHA;
    /**
     * The alpha of the storm is the Tempest Dragon: a long serpent, the fastest of the alphas (its talons grip the
     * clouds), see {@link AlphaStorm}.
     */
    private static final DragonStats STORM_ALPHA;
    /** The alpha of the sunlight is the Sun Steed: a winged horse of 12 blocks high, fast in the air (see {@link AlphaSun}). */
    private static final DragonStats SUN_ALPHA;
    /** The alpha of the moonlight is the Moon Dragon: the long body of the Tempest Dragon (see {@link AlphaMoon}). */
    private static final DragonStats MOON_ALPHA;

    static {
        for (DragonStage s : DragonStage.values()) {
            STAGES[s.ordinal()] = new DragonStats(s.bonusHealth(), s.bonusArmor(), s.bonusAttack(), DragonForm.HYBRID.attackBonus(s),
                    s.scaleAttribute(), s.renderScale(), s.hybridScale(), DragonForm.HYBRID.speedBonus(s), s.bonusSpeed(), s.flyingSpeed(),
                    s.breathRange(), s.breathDamage(), s.maxEnergy(), ENERGY_REGEN, s.cameraDistance(), s.reach(), s.stepHeight(),
                    s.safeFall(), s.senseRadius(), ROAR_COOLDOWN);
        }
        // the alpha: bigger and stronger than any legendary dragon
        ALPHA = new DragonStats(40, 14, 12, 8.5, 2.05F, 1.32F, 1.28F, 0.40F, 0.11F, 0.14F,
                16, 9.0F, 280, 0.65F, 6.0F, 2.5F, 1.2F, 26, 1536, 300);
        // the hybrid numbers are those of every alpha (the human body does not become a giant)
        FOREST_ALPHA = new DragonStats(60, 20, 14, ALPHA.hybridAttack, AlphaForest.SCALE, 4.0F, ALPHA.hybridScale, ALPHA.hybridSpeed,
                0.55F, 0.11F, 20, 9.0F, 320, 0.7F, 0.5F, 9.0F, 2.4F, 60, 1536, 300);
        // in the air the serpent flies at about 60 blocks per second (120 when sprinting), twice the other alphas (about 30):
        // the flying speed of the game is
        // reached when speed x 0.98 / 0.09 blocks are gained per tick (5.4 was so fast that the world could not load in time)
        STORM_ALPHA = new DragonStats(44, 15, 13, ALPHA.hybridAttack, AlphaStorm.SCALE, 2.5F, ALPHA.hybridScale, ALPHA.hybridSpeed,
                0.70F, AlphaStorm.FLYING_SPEED, 18, 9.5F, 300, 0.8F, 3.0F, 6.0F, 1.5F, 60, 1536, 300);
        // the Moon Dragon: the body of the Tempest Dragon, the flight of the other alphas
        MOON_ALPHA = new DragonStats(44, 15, 13, ALPHA.hybridAttack, AlphaStorm.SCALE, 2.5F, ALPHA.hybridScale, ALPHA.hybridSpeed,
                0.70F, ALPHA.flyingSpeed, 18, 9.5F, 300, 0.8F, 3.0F, 6.0F, 1.5F, 60, 1536, 300);
        // the steed gallops on the ground and flies like a pegasus
        SUN_ALPHA = new DragonStats(52, 18, 14, ALPHA.hybridAttack, AlphaSun.SCALE, 3.5F, ALPHA.hybridScale, ALPHA.hybridSpeed,
                0.75F, 0.30F, 18, 9.0F, 300, 0.75F, 1.5F, 8.0F, 2.0F, 60, 1536, 300);
    }

    /** Numbers of a stage, or of the generic alpha (the same for every race but the forest, the storm and the sunlight). */
    public static DragonStats of(DragonStage stage, boolean alpha) {
        return alpha && stage.isMax() ? ALPHA : STAGES[stage.ordinal()];
    }

    /** Numbers of a dragon of this race (the alpha of each race can have its own body). */
    public static DragonStats of(@Nullable DragonType type, DragonStage stage, boolean alpha) {
        return alpha && stage.isMax() ? alpha(type) : STAGES[stage.ordinal()];
    }

    /** The generic alpha. */
    public static DragonStats alpha() {
        return ALPHA;
    }

    public static DragonStats alpha(@Nullable DragonType type) {
        if (type == DragonType.FOREST) return FOREST_ALPHA;
        if (type == DragonType.STORM) return STORM_ALPHA;
        if (type == DragonType.SUNLIGHT) return SUN_ALPHA;
        if (type == DragonType.MOONLIGHT) return MOON_ALPHA;
        return ALPHA;
    }

    /** Speed of the flight (blocks per second, without sprinting) for a flying speed of the game. */
    public static float blocksPerSecond(float flyingSpeed) {
        return flyingSpeed * 0.98F / 0.09F * 20;
    }

    /** Attack added to the fist in the given form. */
    public double attack(DragonForm form) {
        return form == DragonForm.HYBRID ? hybridAttack : bonusAttack;
    }

    /** Extra ground speed (multiplier of the base speed) in the given form. */
    public float speed(DragonForm form) {
        return form == DragonForm.HYBRID ? hybridSpeed : dragonSpeed;
    }
}
