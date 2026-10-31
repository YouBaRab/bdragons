package com.yb.dragons.dragon;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Alpha of the sunlight: the Sun Steed ("Destrier solaire"), a golden winged horse after the sketch of the author:
 * the body and the head of a horse, gold with a yellow and orange finish, blue eyes, feathered wings, a dragon /
 * crocodile tail of 4 blocks, an angel halo above its head and a miniature sun (a cube of 4 x 4 x 4 blocks) above and
 * behind its head. 12 blocks high to the top of its wings. The model is the Blockbench model
 * {@code tools/models/dragon_soleil_alpha.bbmodel}, drawn {@link #MODEL_SCALE} times smaller than in the game.
 *
 * <ul>
 *   <li>On the ground it walks like a horse; in the air it flies like a pegasus (big wing beats, the legs tucked).</li>
 *   <li>Its sun turns very slowly. When the player has not moved for {@link #AFK_AFTER} ticks, the sun spins faster and
 *       faster for 10 seconds, shrinks from the 3rd second, turns to stardust, appears again at once and grows back;
 *       then again every minute.</li>
 * </ul>
 *
 * <p>Its power (key B): it throws its sun like a meteorite ({@link com.yb.dragons.player.SunStrike}).</p>
 *
 * <p>Coordinates in this class are in blocks, in the frame of the steed (like {@link AlphaForest#toWorld}): origin at
 * the feet of the player, x to its left, y up, z forward.</p>
 */
public final class AlphaSun {
    /** World blocks per model block (16 model pixels): the model is 4 times smaller than in the game. */
    public static final float MODEL_SCALE = 4.0F;
    /** World blocks per model pixel. */
    public static final float PX = MODEL_SCALE / 16.0F;
    /** Scale attribute of the player: box 0.6 x 1.8 -> 3.3 x 9.9 blocks, the eyes at the height of its head. */
    public static final float SCALE = 5.5F;

    // --- the model (Blockbench pixels: x to the right of the steed, y up, z backwards) -----------------------------
    /** The centre of the sun. */
    static final float SUN_X = 1.21752F, SUN_Y = 65.0F, SUN_Z = -8.58671F;
    /** The pivot of the body (the hips): it rears up around it. */
    static final float HIP_Y = 24.0F, HIP_Z = 12.0F;
    /** The mouth (the end of the muzzle). */
    static final float MOUTH_Y = 33.0F, MOUTH_Z = -27.5F;

    // --- the power: the sun thrown like a meteorite ----------------------------------------------------------------
    /** The whole animation of the power: it rears up, paws the air and throws its sun (3 seconds). */
    public static final int ULT_TICKS = 60;
    /** The sun leaves its place at this moment (0.9 second). */
    public static final int LAUNCH_TICKS = 18;
    /** How much the body is reared up then (degrees). */
    public static final float LAUNCH_REAR = 46.0F;
    /** Speed of the sun (blocks per tick) and the farthest it goes (blocks). */
    public static final double SUN_SPEED = 3.0, RANGE = 160.0;
    /** The white ball of the explosion grows to {@link #RADIUS} blocks in this many ticks. */
    public static final int EXPLODE_TICKS = 30;
    /** Radius of the explosion: everything but the bedrock disappears (a crater of 10 blocks deep). */
    public static final int RADIUS = 10;
    /** Damage at the centre of the explosion, and at its edge. */
    public static final float DAMAGE_CENTRE = 60.0F, DAMAGE_EDGE = 20.0F;
    /** Recharge of the power: the sun grows back on its head in 45 seconds. */
    public static final int POWER_COOLDOWN = DragonAlpha.POWER_COOLDOWN;

    // --- the sun when the player is away from the keyboard ---------------------------------------------------------
    /** Ticks without moving nor turning before the sun implodes (then again every minute). */
    public static final int AFK_AFTER = 60 * 20;
    /** Length of the implosion: 10 seconds of acceleration (shrinking from the 3rd), the stardust, the sun back. */
    public static final int AFK_TICKS = 230;
    /** The sun turns to stardust at this moment of the implosion. */
    public static final int DUST_AT = 200;

    private AlphaSun() {}

    /** The full dragon form of the alpha of the sunlight is the steed. */
    public static boolean isSteed(@Nullable DragonType type, int stage, boolean alpha, DragonForm form) {
        return type == DragonType.SUNLIGHT && alpha && stage >= 5 && form == DragonForm.DRAGON;
    }

    /** A point of the model (Blockbench pixels), with the body reared up by {@code rear} degrees, in the frame of the steed. */
    static Vec3 local(float x, float y, float z, float rear) {
        float a = rear * Mth.DEG_TO_RAD;
        float dy = y - HIP_Y, dz = z - HIP_Z;
        // Blockbench: a rotation around x by a positive angle raises the front (-z)
        float ry = dy * Mth.cos(a) - dz * Mth.sin(a) + HIP_Y;
        float rz = dy * Mth.sin(a) + dz * Mth.cos(a) + HIP_Z;
        return new Vec3(-x * PX, ry * PX, -rz * PX);
    }

    /** The centre of the sun in the world (the body reared up by {@code rear} degrees). */
    public static Vec3 sun(Vec3 feet, float bodyYaw, float rear, float scale) {
        return feet.add(AlphaForest.toWorld(local(SUN_X, SUN_Y, SUN_Z, rear).scale(scale), bodyYaw));
    }

    /** The mouth in the world (the start of the breath). */
    public static Vec3 mouth(Vec3 feet, float bodyYaw) {
        return feet.add(AlphaForest.toWorld(local(0, MOUTH_Y, MOUTH_Z, 0), bodyYaw));
    }

    /**
     * Size of the sun on its head: whole, gone from the throw, then growing back until the end of the recharge.
     * @param age ticks since the power was called (-1 = never)
     */
    public static float sunSize(float age) {
        if (age < 0) return 1;
        if (age < LAUNCH_TICKS) return 1;
        float k = Mth.clamp((age - LAUNCH_TICKS) / (POWER_COOLDOWN - LAUNCH_TICKS), 0, 1);
        if (k <= 0) return 0;
        // a spark first, then it grows steadily, slowing at the end
        return 0.08F + 0.92F * (1 - (1 - k) * (1 - k));
    }
}
