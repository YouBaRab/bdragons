package com.yb.dragons.client.model;

import com.yb.dragons.dragon.AlphaSun;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Animation of one Sun Steed (client): ground or air (blended), the times of the walk and flight cycles, the slow spin of
 * its sun and its implosion when the player stays away from the keyboard ({@link AlphaSun#AFK_AFTER}). Ticked with the
 * client, interpolated every frame into a {@link SunSteedRenderState}.
 */
public class SunSteedAnimator {
    /** Distance walked (blocks of the vanilla walk animation) for one cycle of the walk animation. */
    static final float WALK_CYCLE = 14.0F;
    /** The slow spin of the sun (degrees per tick): a turn in 60 seconds around y, in 90 seconds around x. */
    static final float SPIN_Y = 360F / (60 * 20), SPIN_X = 360F / (90 * 20);

    private float ground = 1, prevGround = 1;
    private float flyTime, prevFlyTime;
    private float spinX, spinY, prevSpinX, prevSpinY;
    /** Ticks without moving nor turning, and the age of the implosion (-1 = none). */
    private int still;
    private int afkAge = -1;
    private float lastYaw, lastPitch;

    /**
     * @param inAir    flying or falling
     * @param sunWhole the sun is on its head, whole (not thrown, not growing back)
     */
    public void tick(LivingEntity e, boolean inAir, boolean sunWhole) {
        prevGround = ground;
        ground += ((inAir ? 0 : 1) - ground) * 0.15F;
        prevFlyTime = flyTime;
        flyTime += 0.05F;
        if (flyTime > 3600) {
            flyTime -= 3600;
            prevFlyTime -= 3600;
        }

        // away from the keyboard: not moving and not turning for a minute
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        boolean turned = Math.abs(e.getYRot() - lastYaw) > 0.01F || Math.abs(e.getXRot() - lastPitch) > 0.01F;
        lastYaw = e.getYRot();
        lastPitch = e.getXRot();
        boolean idle = vx * vx + vy * vy + vz * vz < 1.0E-6 && !turned && sunWhole;
        still = idle ? still + 1 : 0;

        prevSpinX = spinX;
        prevSpinY = spinY;
        if (afkAge >= 0) {
            afkAge++;
            if (afkAge >= AlphaSun.AFK_TICKS || !sunWhole) {
                // the sun keeps the angle where the implosion left it, then turns slowly again
                float[] end = SunSteedAnimations.SUN_AFK.value(SunSteedAnimations.bone("sun"), 0,
                        Math.min(afkAge, AlphaSun.AFK_TICKS) / 20F);
                if (end != null) {
                    spinX = (spinX + end[0]) % 360;
                    spinY = (spinY + end[1]) % 360;
                    prevSpinX = spinX;
                    prevSpinY = spinY;
                }
                afkAge = -1;
                still = 0;
            }
        } else {
            spinX = (spinX + SPIN_X) % 360;
            spinY = (spinY + SPIN_Y) % 360;
            if (prevSpinX > spinX) prevSpinX -= 360;
            if (prevSpinY > spinY) prevSpinY -= 360;
            if (still >= AlphaSun.AFK_AFTER) {
                afkAge = 0;
                still = 0;
            }
        }
    }

    /** The sun turns to stardust this tick. */
    public boolean dustNow() {
        return afkAge == AlphaSun.DUST_AT;
    }

    /** Fills the animation values of the render state (interpolated). */
    public void fill(SunSteedRenderState s, float partialTick) {
        s.ground = Mth.lerp(partialTick, prevGround, ground);
        s.flyTime = Mth.lerp(partialTick, prevFlyTime, flyTime);
        s.walk = Math.min(1, s.walkAnimationSpeed * 1.5F);
        s.walkTime = s.walkAnimationPos / WALK_CYCLE;
        s.sunSpinX = Mth.lerp(partialTick, prevSpinX, spinX);
        s.sunSpinY = Mth.lerp(partialTick, prevSpinY, spinY);
        s.afkTime = afkAge < 0 ? -1 : (afkAge + partialTick) / 20F;
    }
}
