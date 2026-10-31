package com.yb.dragons.dragon;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The two shapes of a dragon player (switch with the form key). There is no way back to the human
 * body once the ritual is done: the hybrid form is the "human" side of the dragon.
 * <ul>
 *   <li>HYBRID: half human, half dragon (horns, wings and tail on a human body). Every power is
 *       available at reduced strength, it is the fastest form on the ground and its claws dig like
 *       an enchanted netherite pickaxe (silk touch). Glides, cannot fly.</li>
 *   <li>DRAGON: the full dragon. Maximum power, flight, but heavier on the ground and its big claws
 *       cannot dig.</li>
 * </ul>
 */
public enum DragonForm {
    //      power  breath range
    HYBRID (0.7F,  0.7F),
    DRAGON (1.0F,  1.0F);

    private final float powerFactor;
    private final float rangeFactor;

    DragonForm(float powerFactor, float rangeFactor) {
        this.powerFactor = powerFactor;
        this.rangeFactor = rangeFactor;
    }

    /** Multiplier applied to the breath damage, the roar and the stat bonuses. */
    public float powerFactor() { return powerFactor; }
    /** Multiplier applied to the breath range. */
    public float rangeFactor() { return rangeFactor; }
    public boolean hasPowers() { return true; }
    public boolean hasWings() { return true; }
    /** The hybrid digs with its claws, the big dragon cannot. */
    public boolean hasClaws() { return this == HYBRID; }

    /** Extra ground speed (multiplier of the base speed). The dragon form uses its stage value instead. */
    public float speedBonus(DragonStage stage) {
        return switch (this) {
            case HYBRID -> 0.20F + 0.03F * stage.number();
            case DRAGON -> stage.bonusSpeed();
        };
    }

    /**
     * Attack damage added to the fist. The hybrid hits hard: at the legendary stage a villager
     * (20 health) falls in 3 punches (1 + 6.5 = 7.5 damage).
     */
    public double attackBonus(DragonStage stage) {
        return switch (this) {
            case HYBRID -> switch (stage) {
                case HATCHLING -> 2.0;
                case JUVENILE -> 3.0;
                case ADULT -> 4.0;
                case ELDER -> 5.0;
                case LEGENDARY -> 6.5;
            };
            case DRAGON -> stage.bonusAttack();
        };
    }

    /** Hybrid <-> dragon. */
    public DragonForm next() {
        return this == HYBRID ? DRAGON : HYBRID;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("form.bdragon." + id());
    }

    public static DragonForm byOrdinal(int ordinal) {
        DragonForm[] v = values();
        return ordinal >= 0 && ordinal < v.length ? v[ordinal] : DRAGON;
    }

    /** Old saves may still say "human": it became the hybrid form. */
    public static DragonForm byId(String id) {
        for (DragonForm f : values()) if (f.id().equals(id)) return f;
        return "human".equals(id) ? HYBRID : DRAGON;
    }
}
