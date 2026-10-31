package com.yb.dragons.client;

import com.yb.dragons.YBDragons;
import com.yb.dragons.client.screen.AlphaCinematicScreen;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

/**
 * The third person cinematic of the awakening of the alpha, for the player who consumed the orb.
 *
 * <p>While the ritual plays, a transparent screen ({@link AlphaCinematicScreen}) holds the mouse and the
 * keyboard (Escape skips the cinematic) and draws the black bands and the titles. The camera is the
 * vanilla third person camera: its angle follows the view of the player, so turning the view turns the
 * camera around the dragon (the dragon itself is drawn facing its own way, see
 * {@link com.yb.dragons.client.render.PlayerDragonRenderer}). Its distance is changed with a temporary
 * modifier of the camera distance attribute, on the client only.</p>
 *
 * <pre>
 *   0- 20  from the view of the player to a 3/4 front view
 *  20- 88  slow travelling around the dragon while the orb rises and the vortex grows, the camera gets closer
 *  88-100  closest: the orb breaks, the dragon glows white
 * 100-112  explosion: the camera is thrown back and shakes
 * 112-150  the new alpha roars and lands, wide view
 * 150-170  back behind the player
 * </pre>
 */
public final class AlphaCinematic {
    private static final Identifier ZOOM = YBDragons.id("alpha_cinematic_zoom");

    /** age, azimuth around the dragon (0 = in front of it), pitch, distance factor. */
    private static final float[][] KEYS = {
            {20, -50, 18, 0.86F},
            {60, -15, 15, 0.84F},
            {88, 25, 12, 0.80F},
            {98, 30, 10, 0.74F},
            {108, 38, 16, 1.18F},
            {130, 60, 15, 1.02F},
            {150, 80, 16, 0.96F}};
    private static final float BLEND_IN = 20;
    private static final float BLEND_OUT_START = 150;

    private static boolean active;
    /** Ritual for which the cinematic was played (or skipped): it is never opened twice. */
    private static long shownRitual = Long.MIN_VALUE;
    @Nullable private static CameraType previousCamera;
    private static boolean hidHud;
    private static float startYaw, startPitch;
    private static float lastYaw, lastPitch;

    private AlphaCinematic() {}

    public static boolean isActive() {
        return active;
    }

    /** Client tick: starts the cinematic when the ritual of the local player begins, moves the camera. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (active) stop(false);
            return;
        }
        ClientDragonData.Info info = ClientDragonData.get(player.getId());
        float age = info == null ? -1 : info.ritualAge(0);
        if (!active) {
            if (age >= 0 && age < DragonAlpha.DURATION - 30 && info.ritualStart() != shownRitual && mc.gui.screen() == null) {
                begin(mc, player, info);
            }
            return;
        }
        if (age < 0 || age >= DragonAlpha.DURATION) {
            stop(true);
            return;
        }
        moveCamera(player, info, age);
    }

    private static void begin(Minecraft mc, LocalPlayer player, ClientDragonData.Info info) {
        active = true;
        shownRitual = info.ritualStart();
        previousCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        startYaw = lastYaw = player.getYRot();
        startPitch = lastPitch = player.getXRot();
        hidHud = !mc.gui.hud.isHidden();
        if (hidHud) mc.gui.hud.toggle();
        mc.gui.setScreen(new AlphaCinematicScreen(info.type()));
    }

    /** The screen was closed (Escape): the rest of the ritual is seen with the normal camera. */
    public static void onScreenClosed() {
        if (active) stop(false);
    }

    private static void stop(boolean closeScreen) {
        active = false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            player.setYRot(startYaw);
            player.yRotO = startYaw;
            player.setXRot(startPitch);
            player.xRotO = startPitch;
            AttributeInstance distance = player.getAttribute(Attributes.CAMERA_DISTANCE);
            if (distance != null) distance.removeModifier(ZOOM);
        }
        if (previousCamera != null) mc.options.setCameraType(previousCamera);
        previousCamera = null;
        if (hidHud && mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        hidHud = false;
        if (closeScreen && mc.gui.screen() instanceof AlphaCinematicScreen) mc.gui.setScreen(null);
    }

    /** Leaving the world in the middle of the cinematic. */
    public static void reset() {
        if (active) stop(false);
        shownRitual = Long.MIN_VALUE;
    }

    // ------------------------------------------------------------------------------------------------------

    private static void moveCamera(LocalPlayer player, ClientDragonData.Info info, float age) {
        float[] key = keyAt(age);
        float azimuth = key[1], pitch = key[2], zoom = key[3];
        // the explosion shakes the camera
        if (age >= DragonAlpha.CLIMAX && age < DragonAlpha.CLIMAX + 14) {
            float amp = 2.6F * (1 - (age - DragonAlpha.CLIMAX) / 14F);
            azimuth += Mth.sin(age * 3.1F) * amp;
            pitch += Mth.cos(age * 2.7F) * amp;
        }
        // third person back: the camera is behind the view, so looking back at the dragon puts it in front of it
        float yaw = info.ritualYaw() + 180 + azimuth;
        float out = 0;
        // blend from / to the real view of the player
        if (age < BLEND_IN) {
            float k = DragonAlpha.smooth(age / BLEND_IN);
            yaw = startYaw + Mth.wrapDegrees(yaw - startYaw) * k;
            pitch = Mth.lerp(k, startPitch, pitch);
            zoom = Mth.lerp(k, 1, zoom);
        } else if (age > BLEND_OUT_START) {
            float k = DragonAlpha.smooth((age - BLEND_OUT_START) / (DragonAlpha.DURATION - BLEND_OUT_START - 2));
            float from = yaw;
            yaw = from + Mth.wrapDegrees(startYaw - from) * k;
            pitch = Mth.lerp(k, pitch, startPitch);
            zoom = Mth.lerp(k, zoom, 1);
            out = k;
        }
        // keep the yaw continuous (no turn of 360 degrees between two ticks)
        yaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw);
        player.yRotO = lastYaw;
        player.xRotO = lastPitch;
        player.setYRot(yaw);
        player.setXRot(pitch);
        lastYaw = yaw;
        lastPitch = pitch;
        AttributeInstance distance = player.getAttribute(Attributes.CAMERA_DISTANCE);
        if (distance != null) {
            // the vanilla camera stands at (scale x camera distance attribute) blocks. The wanted distance goes
            // smoothly from the one of the legendary dragon to the one of the alpha while it grows (the alpha of
            // the forest is a tortoise of 16 blocks, seen from further away), whatever the moment the server
            // changes the attributes.
            DragonType type = info.type();
            float before = cameraBase(DragonStats.of(DragonStage.LEGENDARY, false));
            float after = cameraBase(DragonStats.alpha(type));
            float grow = age < DragonAlpha.CLIMAX ? 0 : DragonAlpha.phase(age, DragonAlpha.CLIMAX, DragonAlpha.CLIMAX + 18);
            if (type == DragonType.FOREST) after *= 1 + 0.2F * (1 - out);
            float wanted = zoom * Mth.lerp(grow, before, after);
            AttributeModifier current = distance.getModifier(ZOOM);
            double raw = distance.getValue() / (1 + (current == null ? 0 : current.amount()));
            double actual = Math.max(0.1, player.getScale() * raw);
            distance.addOrUpdateTransientModifier(new AttributeModifier(ZOOM, wanted / actual - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Distance of the camera of a dragon with these numbers (blocks). */
    private static float cameraBase(DragonStats stats) {
        return stats.scaleAttribute() * (4.0F + stats.cameraDistance());
    }

    /** The camera keyframes, smoothly interpolated. */
    private static float[] keyAt(float age) {
        if (age <= KEYS[0][0]) return KEYS[0];
        for (int i = 0; i + 1 < KEYS.length; i++) {
            float[] a = KEYS[i], b = KEYS[i + 1];
            if (age <= b[0]) {
                float k = DragonAlpha.smooth((age - a[0]) / (b[0] - a[0]));
                return new float[] {age, Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2]), Mth.lerp(k, a[3], b[3])};
            }
        }
        return KEYS[KEYS.length - 1];
    }
}
