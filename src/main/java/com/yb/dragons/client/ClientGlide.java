package com.yb.dragons.client;

import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.network.ModNetwork;
import com.yb.dragons.player.DragonPlayerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * Gliding of the local player (dragon and hybrid forms), like the elytra: when the dragon falls
 * (wings tired, flight stopped, jump from a cliff), its wings open and it glides instead of
 * crashing.
 * <ul>
 *   <li>Mouse: steering. Looking down dives (full dive already at 45°), looking up trades speed
 *       for height.</li>
 *   <li>Forward key: the dive ("piqué") — the dragon folds its wings and accelerates while
 *       descending, without having to look under its feet. Left / right: drift sideways.</li>
 *   <li>Back key: air brake (slow and gentle descent). Sneak: fold the wings and drop.</li>
 * </ul>
 * <p>The glide keeps its own speed (the vanilla air friction is not applied to it). The movement
 * is computed on the client like vanilla flight; the server is told when the glide starts and
 * stops so that it cancels the fall damage.</p>
 *
 * <p>The Sylvan Tortoise (alpha of the forest) has no wings: its clouds carry it down instead, a slow and
 * heavy descent (forward: it drifts forward and sinks a little faster, back: it brakes, sneak: it drops).
 * Its flight is heavy too: its vertical speed changes slowly and it never dives faster than its clouds allow.</p>
 */
public final class ClientGlide {
    private static boolean gliding;
    /** Horizontal speed of the glide (blocks per tick) and vertical speed. */
    private static double speed;
    private static double vy;
    /** Smoothed horizontal heading (unit vector). */
    private static double dirX, dirZ;
    /** 0..1, how much the dragon is diving right now (read by the animation). */
    private static float diveAmount;
    /** Vertical speed of the heavy flight of the tortoise. */
    private static double flightVy;

    private ClientGlide() {}

    public static boolean isGliding() {
        return gliding;
    }

    public static float diveAmount() {
        return gliding ? diveAmount : 0;
    }

    /** Called after every tick of the local player. */
    public static void tick(LocalPlayer player) {
        DragonPlayerData data = ClientDragonData.own();
        boolean tortoise = data.isTortoise();
        if (tortoise && player.getAbilities().flying) {
            heavyFlight(player);
        } else {
            flightVy = player.getDeltaMovement().y;
        }
        boolean possible = data.isDragon() && data.form().hasWings() && !player.onGround()
                && !player.getAbilities().flying && !player.isInWater() && !player.isInLava() && !player.isPassenger()
                && !player.onClimbable() && !player.isSpectator() && !player.isFallFlying();
        if (!possible || player.isShiftKeyDown()) {
            set(false);
            return;
        }
        Vec3 v = player.getDeltaMovement();
        if (!gliding) {
            // a real fall, not a simple jump (the clouds of the tortoise catch it sooner)
            if (tortoise ? v.y < -0.5 || player.fallDistance > 2.0 : v.y < -0.42 || player.fallDistance > 2.5) {
                speed = v.horizontalDistance();
                vy = v.y;
                float yaw = player.getYRot() * Mth.DEG_TO_RAD;
                dirX = -Mth.sin(yaw);
                dirZ = Mth.cos(yaw);
                set(true);
            } else {
                return;
            }
        }
        if (tortoise) {
            clouds(player);
        } else {
            apply(player, data);
        }
    }

    /** The flight of the tortoise: heavy, its vertical speed changes slowly, and it never dives too fast. */
    private static void heavyFlight(LocalPlayer p) {
        Vec3 v = p.getDeltaMovement();
        double target = Math.max(v.y, -0.42);
        flightVy += Mth.clamp(target - flightVy, -0.035, 0.06);
        p.setDeltaMovement(v.x, flightVy, v.z);
        p.fallDistance = 0;
    }

    /** The descent on the clouds of the tortoise: slow, heavy and smooth. */
    private static void clouds(LocalPlayer p) {
        Input in = p.input.keyPresses;
        double vyTarget = in.backward() && !in.forward() ? -0.10 : in.forward() ? -0.30 : -0.20;
        vy += (vyTarget - vy) * 0.08;
        double target = in.forward() ? 0.32 : in.backward() ? 0.0 : 0.06;
        speed += (target - speed) * 0.04;
        if (p.horizontalCollision) speed *= 0.5;
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        dirX = Mth.lerp(0.08, dirX, -Mth.sin(yaw));
        dirZ = Mth.lerp(0.08, dirZ, Mth.cos(yaw));
        double dl = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (dl > 1.0E-4) {
            dirX /= dl;
            dirZ /= dl;
        }
        diveAmount = 0;
        p.setDeltaMovement(dirX * speed, vy, dirZ * speed);
        p.fallDistance = 0;
    }

    private static void set(boolean value) {
        if (gliding == value) return;
        gliding = value;
        if (!value) diveAmount = 0;
        ModNetwork.sendToServer(new ModNetwork.Action(ModNetwork.Action.GLIDE, value));
    }

    public static void reset() {
        gliding = false;
        diveAmount = 0;
    }

    private static void apply(LocalPlayer p, DragonPlayerData data) {
        Input in = p.input.keyPresses;
        int stage = data.stage().number();
        boolean hybrid = data.form() == DragonForm.HYBRID;
        float pitch = p.getXRot(); // positive = looking down

        // --- how much we dive: looking down (full at 45°) or the forward key ("piqué") ---
        double divePitch = Mth.clamp((pitch - 5.0F) / 40.0F, 0.0F, 1.0F);
        double dive = in.forward() ? Math.max(divePitch, 0.8) : divePitch;
        if (in.forward() && pitch > 5.0F) dive = Math.min(1.0, dive + 0.2);
        double climb = in.forward() ? 0 : Mth.clamp(-pitch / 45.0F, 0.0F, 1.0F);
        boolean brake = in.backward() && !in.forward();
        diveAmount = (float) dive;

        // --- speed: bigger wings glide faster, the dive accelerates up to about 30 blocks/s ---
        double cruise = (0.40 + 0.03 * stage) * (hybrid ? 0.85 : 1.0);
        double maxDive = cruise + (0.80 + 0.08 * stage) * (hybrid ? 0.85 : 1.0);
        double target = brake ? cruise * 0.45 : cruise + (maxDive - cruise) * dive;
        double rate = target > speed ? 0.025 + 0.05 * dive : (brake ? 0.12 : 0.03);
        speed += (target - speed) * rate;
        if (p.horizontalCollision) speed *= 0.5;

        // --- vertical speed ---
        double vyTarget;
        if (climb > 0.05 && speed > 0.35 && !brake) {
            // pull up: speed becomes height
            vyTarget = climb * speed * 0.5;
            speed = Math.max(0.2, speed - climb * 0.03);
        } else if (brake) {
            vyTarget = -0.06;
        } else {
            vyTarget = -((stage == 1 ? 0.11 : 0.075) + dive * (0.55 + 0.04 * stage));
        }
        vy += (vyTarget - vy) * 0.25;

        // --- heading: where the dragon looks, plus a sideways drift with the left / right keys ---
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double strafe = (in.left() ? 1 : 0) - (in.right() ? 1 : 0);
        double tx = fx + fz * strafe * 0.45, tz = fz - fx * strafe * 0.45;
        double tl = Math.sqrt(tx * tx + tz * tz);
        tx /= tl;
        tz /= tl;
        dirX = Mth.lerp(0.3, dirX, tx);
        dirZ = Mth.lerp(0.3, dirZ, tz);
        double dl = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (dl > 1.0E-4) {
            dirX /= dl;
            dirZ /= dl;
        }

        p.setDeltaMovement(dirX * speed, vy, dirZ * speed);
        p.fallDistance = 0;
    }
}
