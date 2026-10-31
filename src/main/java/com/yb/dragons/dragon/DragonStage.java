package com.yb.dragons.dragon;

import net.minecraft.network.chat.Component;

/**
 * The 5 growth stages of a dragon. Each stage adds one clearly visible physical feature
 * (see {@link #featureKey()}) and stronger powers. Wild dragons use the same table for their rank.
 */
public enum DragonStage {
    //        power  scaleAttr renderScale health armor attack speed  flySpeed breathRange breathDmg energy camera reach step safeFall
    HATCHLING(0,     0.80F,    0.38F,      0,     2,    1,     0.00F, 0.000F,  4,          1.5F,     100,   0.0F,  0.0F, 0.0F, 3),
    JUVENILE (150,   1.00F,    0.50F,      6,     4,    2,     0.03F, 0.060F,  6,          2.5F,     120,   1.0F,  0.5F, 0.4F, 5),
    ADULT    (600,   1.20F,    0.66F,      12,    6,    4,     0.05F, 0.080F,  8,          3.5F,     150,   2.0F,  1.0F, 0.5F, 8),
    ELDER    (1800,  1.45F,    0.85F,      20,    8,    6,     0.07F, 0.100F,  10,         5.0F,     180,   4.0F,  1.5F, 0.6F, 12),
    LEGENDARY(4500,  1.75F,    1.10F,      30,    10,   9,     0.09F, 0.120F,  13,         7.0F,     220,   6.0F,  2.0F, 1.0F, 20);

    private final int powerRequired;
    private final float scaleAttribute;
    private final float renderScale;
    private final int bonusHealth;
    private final int bonusArmor;
    private final int bonusAttack;
    private final float bonusSpeed;
    private final float flyingSpeed;
    private final int breathRange;
    private final float breathDamage;
    private final int maxEnergy;
    private final float cameraDistance;
    private final float reach;
    private final float stepHeight;
    private final int safeFall;

    DragonStage(int powerRequired, float scaleAttribute, float renderScale, int bonusHealth, int bonusArmor, int bonusAttack,
                float bonusSpeed, float flyingSpeed, int breathRange, float breathDamage, int maxEnergy, float cameraDistance,
                float reach, float stepHeight, int safeFall) {
        this.powerRequired = powerRequired;
        this.scaleAttribute = scaleAttribute;
        this.renderScale = renderScale;
        this.bonusHealth = bonusHealth;
        this.bonusArmor = bonusArmor;
        this.bonusAttack = bonusAttack;
        this.bonusSpeed = bonusSpeed;
        this.flyingSpeed = flyingSpeed;
        this.breathRange = breathRange;
        this.breathDamage = breathDamage;
        this.maxEnergy = maxEnergy;
        this.cameraDistance = cameraDistance;
        this.reach = reach;
        this.stepHeight = stepHeight;
        this.safeFall = safeFall;
    }

    /** Stage number shown to the player, 1..5. */
    public int number() { return ordinal() + 1; }
    public int powerRequired() { return powerRequired; }
    /** Rank of wild dragon that must have been defeated to reach this stage (0 = none). */
    public int challengeRank() { return this == HATCHLING ? 0 : number(); }
    public float scaleAttribute() { return scaleAttribute; }
    public float renderScale() { return renderScale; }
    public int bonusHealth() { return bonusHealth; }
    public int bonusArmor() { return bonusArmor; }
    public int bonusAttack() { return bonusAttack; }
    public float bonusSpeed() { return bonusSpeed; }
    public float flyingSpeed() { return flyingSpeed; }
    public boolean canFly() { return this != HATCHLING; }
    /** Juveniles can only fly for a limited time (stamina). */
    public boolean limitedFlight() { return this == JUVENILE; }
    public int breathRange() { return breathRange; }
    public float breathDamage() { return breathDamage; }
    public int maxEnergy() { return maxEnergy; }
    public float cameraDistance() { return cameraDistance; }
    public float reach() { return reach; }
    public float stepHeight() { return stepHeight; }
    public int safeFall() { return safeFall; }
    public boolean hasAura() { return ordinal() >= ELDER.ordinal(); }
    public boolean hasRoar() { return ordinal() >= ADULT.ordinal(); }
    public boolean hasTerrainEffect() { return this == LEGENDARY; }
    /** Size of the hybrid form (the dragon blood makes the human body grow a little at each stage). */
    public float hybridScale() { return 1.0F + 0.05F * ordinal(); }
    /** Radius (blocks) in which a dragon of this stage senses the lairs of the wild dragons. */
    public int senseRadius() {
        return switch (this) {
            case HATCHLING -> 256;
            case JUVENILE -> 384;
            case ADULT -> 512;
            case ELDER -> 768;
            case LEGENDARY -> 1024;
        };
    }

    public Component displayName() {
        return Component.translatable("dragon_stage.bdragon." + name().toLowerCase(java.util.Locale.ROOT));
    }

    /** The new physical feature unlocked at this stage. */
    public Component featureKey() {
        return Component.translatable("dragon_stage.bdragon." + name().toLowerCase(java.util.Locale.ROOT) + ".feature");
    }

    public DragonStage next() {
        return this == LEGENDARY ? LEGENDARY : values()[ordinal() + 1];
    }

    public boolean isMax() { return this == LEGENDARY; }

    public static DragonStage byNumber(int number) {
        int idx = Math.max(1, Math.min(5, number)) - 1;
        return values()[idx];
    }

    /** Wild dragon stats by rank (1..5). */
    public static final float[] WILD_HEALTH = {30, 60, 110, 190, 320};
    public static final float[] WILD_DAMAGE = {5, 8, 12, 17, 24};
    public static final float[] WILD_ARMOR = {2, 4, 6, 9, 12};
    public static final float[] WILD_BREATH = {2, 3, 4.5F, 6, 8};

    /** Power given when a dragon-player kills a wild dragon of the given rank. */
    public static int powerForRank(int rank) {
        return 40 * rank * rank;
    }
}
