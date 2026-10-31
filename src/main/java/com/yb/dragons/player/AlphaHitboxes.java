package com.yb.dragons.player;

import com.yb.dragons.ModRegistry;
import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.dragon.StormSpine;
import com.yb.dragons.entity.AlphaHitbox;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The hit zones of the giant alphas (server side): invisible boxes that follow the body. Minecraft gives a player a
 * single square box that never turns, so the solid box of the player is only a small part of these bodies; with the
 * zones, a sword or an arrow that touches any part of the body really hits it.
 * <ul>
 *   <li>The Sylvan Tortoise (forest): nine fixed zones that cover its volume of 17 x 12 x 12 blocks (the shell, the
 *       forest of its back, the head, the legs and the tail).</li>
 *   <li>The Sun Steed (sunlight): eight fixed zones on its body of 12 blocks high (the body, the legs, the neck and the
 *       head, the muzzle, the tail).</li>
 *   <li>The Tempest Dragon (storm): nine zones along its spine (the head, the neck, the torso and six stretches of the body and tail),
 *       moved every tick with the pose of the serpent ({@link StormSpine}: rearing, slithering, flying, diving).</li>
 * </ul>
 */
public final class AlphaHitboxes {
    /**
     * Zones in the frame of the tortoise (blocks): x (left), bottom, z (forward), width, height.
     * Together they cover x -6..6, y 0..12 and z -7.25..9.9.
     */
    static final float[][] ZONES = {
            {0, 2.0F, 2.75F, 10.0F, 6.0F},    // front of the shell
            {0, 2.0F, -2.75F, 10.0F, 6.0F},   // back of the shell
            {0, 8.0F, 0, 8.0F, 4.3F},         // the hills and the trees of the back
            {0, 3.0F, 8.0F, 3.6F, 3.2F},      // head
            {4.6F, 0, 3.3F, 2.6F, 3.0F},      // legs
            {-4.6F, 0, 3.3F, 2.6F, 3.0F},
            {4.6F, 0, -3.6F, 2.6F, 3.0F},
            {-4.6F, 0, -3.6F, 2.6F, 3.0F},
            {0, 2.4F, -6.0F, 2.0F, 1.6F}};    // tail

    /** Zones of the steed, in its frame (blocks): x (left), bottom, z (forward), width, height. */
    static final float[][] STEED_ZONES = {
            {0, 4.4F, 2.0F, 3.6F, 3.4F},      // front of the body
            {0, 4.4F, -1.75F, 3.6F, 3.4F},    // back of the body
            {0, 0, 2.9F, 3.0F, 4.6F},         // front legs
            {0, 0, -2.75F, 3.0F, 4.6F},       // hind legs
            {0, 7.4F, 4.4F, 2.6F, 3.8F},      // neck and head
            {0, 7.6F, 6.3F, 1.6F, 1.6F},      // muzzle
            {0, 3.6F, -5.0F, 1.8F, 2.6F},     // tail
            {0, 2.6F, -7.0F, 1.2F, 1.6F}};    // end of the tail

    /** The zones of the serpent: the elements of the spine each one covers (after the head zone). */
    static final int[][] SERPENT_ZONES = {{0, 3}, {4, 4}, {5, 7}, {8, 10}, {11, 13}, {14, 16}, {17, 19}, {20, 24}};
    /** Size of the head zone of the serpent (blocks) and its centre in front of the joint of the head (pixels). */
    static final float SERPENT_HEAD = 2.4F, SERPENT_HEAD_FORWARD = 22.0F;

    private enum Kind { TORTOISE, SERPENT, STEED }

    /** The zones of one player, and for the serpent the movement that poses its spine. */
    private static final class Body {
        final Kind kind;
        final List<AlphaHitbox> parts;
        final StormSpine.Motion motion = new StormSpine.Motion();
        final StormSpine.Pose pose = new StormSpine.Pose();
        double lastX, lastY, lastZ;
        float lastYaw;
        boolean moved;

        Body(Kind kind, List<AlphaHitbox> parts) {
            this.kind = kind;
            this.parts = parts;
        }
    }

    private static final Map<UUID, Body> BODIES = new HashMap<>();

    private AlphaHitboxes() {}

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        Kind kind = data.isTortoise() ? Kind.TORTOISE : data.isSerpent() ? Kind.SERPENT : data.isSteed() ? Kind.STEED : null;
        boolean wanted = kind != null && player.isAlive() && !player.isSpectator() && !data.inRitual();
        Body body = BODIES.get(player.getUUID());
        if (!wanted) {
            if (body != null) remove(player.getUUID());
            return;
        }
        ServerLevel level = player.level();
        if (body == null || body.kind != kind || body.parts.stream().anyMatch(p -> p.isRemoved() || p.level() != level)) {
            remove(player.getUUID());
            body = new Body(kind, spawn(level, player, kind));
            BODIES.put(player.getUUID(), body);
        }
        // the Moon Dragon has the short neck of the Ender Dragon
        float[] lengths = data.type() == DragonType.MOONLIGHT ? StormSpine.MOON_LENGTHS : StormSpine.LENGTHS_OF_STORM;
        body.motion.lengths = lengths;
        body.pose.lengths = lengths;
        if (kind == Kind.TORTOISE) {
            placeZones(player, body.parts, ZONES);
        } else if (kind == Kind.STEED) {
            placeZones(player, body.parts, STEED_ZONES);
        } else {
            moveSerpent(player, body);
            placeSerpent(player, body);
        }
    }

    private static List<AlphaHitbox> spawn(ServerLevel level, ServerPlayer player, Kind kind) {
        List<AlphaHitbox> list = new ArrayList<>();
        float[][] zones = kind == Kind.TORTOISE ? ZONES : kind == Kind.STEED ? STEED_ZONES : null;
        int count = zones != null ? zones.length : SERPENT_ZONES.length + 1;
        for (int i = 0; i < count; i++) {
            AlphaHitbox part = ModRegistry.ALPHA_HITBOX.get().create(level, EntitySpawnReason.TRIGGERED);
            if (part == null) continue;
            // the zones of the serpent get their size from its pose every tick
            if (zones != null) {
                part.setup(player, zones[i][3], zones[i][4]);
            } else {
                part.setup(player, 1.0F, 1.0F);
            }
            part.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
            if (level.addFreshEntity(part)) list.add(part);
        }
        return list;
    }

    // --- the tortoise and the steed: fixed zones -----------------------------------------------------------------

    private static void placeZones(ServerPlayer player, List<AlphaHitbox> parts, float[][] zones) {
        for (int i = 0; i < parts.size() && i < zones.length; i++) {
            float[] zone = zones[i];
            Vec3 p = player.position().add(AlphaForest.toWorld(new Vec3(zone[0], zone[1], zone[2]), player.yBodyRot));
            AlphaHitbox part = parts.get(i);
            if (part.position().distanceToSqr(p) > 1.0E-4) part.setPos(p.x, p.y, p.z);
        }
    }

    // --- the serpent -----------------------------------------------------------------------------------------

    /** The same movement as the model on the clients (the zones follow the coil, the slither and the flight). */
    private static void moveSerpent(ServerPlayer player, Body body) {
        double x = player.getX(), y = player.getY(), z = player.getZ();
        double vx = body.moved ? x - body.lastX : 0, vy = body.moved ? y - body.lastY : 0, vz = body.moved ? z - body.lastZ : 0;
        float turn = body.moved ? Mth.wrapDegrees(player.yBodyRot - body.lastYaw) : 0;
        body.lastX = x;
        body.lastY = y;
        body.lastZ = z;
        body.lastYaw = player.yBodyRot;
        body.moved = true;
        boolean inAir = (player.getAbilities().flying || !player.onGround()) && !player.isInWater() && !player.isPassenger();
        body.motion.tick(inAir, Math.sqrt(vx * vx + vz * vz), vy, turn);
        float headYaw = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
        body.motion.track(x, y, z, player.yBodyRot, player.tickCount, headYaw, player.getXRot());
        body.motion.solve(body.pose, 1.0F, player.tickCount, headYaw, player.getXRot());
        // in flight the zones follow the path of the head, like the model
        body.motion.follow(body.pose, body.motion.weight(StormSpine.FLY, 1.0F) + body.motion.weight(StormSpine.DESCEND, 1.0F), x, y, z, player.yBodyRot);
    }

    private static void placeSerpent(ServerPlayer player, Body body) {
        StormSpine.Pose pose = body.pose;
        float yaw = player.yBodyRot;
        List<AlphaHitbox> parts = body.parts;
        if (parts.isEmpty()) return;
        // the head: a cube in front of the joint of the head, where the head looks
        Vec3 j0 = pose.joint(0);
        double hy = Math.toRadians(pose.headYaw), hp = Math.toRadians(pose.headPitch);
        double f = SERPENT_HEAD_FORWARD * AlphaStorm.PX;
        // a positive head yaw turns the head to the right of the serpent (towards -x)
        Vec3 head = j0.add(-Math.sin(hy) * Math.cos(hp) * f, -Math.sin(hp) * f - SERPENT_HEAD / 2, Math.cos(hy) * Math.cos(hp) * f);
        place(player, parts.get(0), head, yaw, SERPENT_HEAD, SERPENT_HEAD);
        // the body: each zone wraps the joints of its elements
        for (int i = 0; i < SERPENT_ZONES.length && i + 1 < parts.size(); i++) {
            int from = SERPENT_ZONES[i][0], to = SERPENT_ZONES[i][1];
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            for (int k = from; k <= to + 1; k++) {
                Vec3 j = pose.joint(k);
                minX = Math.min(minX, j.x);
                minY = Math.min(minY, j.y);
                minZ = Math.min(minZ, j.z);
                maxX = Math.max(maxX, j.x);
                maxY = Math.max(maxY, j.y);
                maxZ = Math.max(maxZ, j.z);
            }
            double thick = StormSpine.WIDTHS[from] * AlphaStorm.PX;
            // the zone is a square box that does not turn: its width covers the rotated extent of the piece
            double width = Math.max(maxX - minX, maxZ - minZ) * 0.75 + thick;
            double height = maxY - minY + thick;
            Vec3 bottom = new Vec3((minX + maxX) / 2, minY - thick / 2, (minZ + maxZ) / 2);
            place(player, parts.get(i + 1), bottom, yaw, (float) width, (float) height);
        }
    }

    private static void place(ServerPlayer player, AlphaHitbox part, Vec3 local, float yaw, float width, float height) {
        // sizes rounded to a quarter of a block: the clients are only told when they really change
        part.resize(Math.max(0.5F, Math.round(width * 4) / 4.0F), Math.max(0.5F, Math.round(height * 4) / 4.0F));
        Vec3 p = player.position().add(AlphaForest.toWorld(local, yaw));
        if (part.position().distanceToSqr(p) > 1.0E-4) part.setPos(p.x, p.y, p.z);
    }

    /**
     * The mouth of the serpent in the world (start of its breath), from the pose of its hit zones; the eyes of the
     * player when there is no pose yet.
     */
    public static Vec3 serpentMouth(ServerPlayer player) {
        Body body = BODIES.get(player.getUUID());
        if (body == null || body.kind != Kind.SERPENT) return player.getEyePosition().add(player.getLookAngle().scale(2.0));
        return player.position().add(AlphaForest.toWorld(body.pose.mouthBlocks(), player.yBodyRot));
    }

    public static void remove(UUID player) {
        Body body = BODIES.remove(player);
        if (body != null) body.parts.forEach(AlphaHitbox::discard);
    }

    public static void removeAll() {
        for (UUID id : new ArrayList<>(BODIES.keySet())) remove(id);
    }

    // --- room -----------------------------------------------------------------------------------------------

    /** Scale attribute of the giant alpha body of a race in its dragon form, 0 when it is not a giant. */
    public static float giantScale(@Nullable DragonType type) {
        if (type == DragonType.FOREST) return AlphaForest.SCALE;
        if (type == DragonType.STORM || type == DragonType.MOONLIGHT) return AlphaStorm.SCALE;
        if (type == DragonType.SUNLIGHT) return AlphaSun.SCALE;
        return 0;
    }

    /**
     * Room for the giant body of the alpha of this race: its solid box must not be under a ceiling. The width is not
     * checked (a wall at the side only stops it, it can always move away from it). Always true for the other races.
     */
    public static boolean hasRoom(ServerPlayer player, @Nullable DragonType type) {
        float scale = giantScale(type);
        if (scale <= 0) return true;
        double x = player.getX(), y = player.getY(), z = player.getZ();
        // the height of the solid box, a little less (touching a ceiling is not being under it)
        AABB column = new AABB(x - 0.3, y + 0.05, z - 0.3, x + 0.3, y + 1.8 * scale - 0.05, z + 0.3);
        return player.level().noBlockCollision(player, column);
    }

    /** The message when there is no room: the Sylvan Tortoise or the Tempest Dragon. */
    public static String noRoomKey(@Nullable DragonType type) {
        if (type == DragonType.SUNLIGHT) return "message.bdragon.steed.no_room";
        if (type == DragonType.MOONLIGHT) return "message.bdragon.moon.no_room";
        return type == DragonType.STORM ? "message.bdragon.serpent.no_room" : "message.bdragon.tortoise.no_room";
    }

    public static String stuckKey(@Nullable DragonType type) {
        if (type == DragonType.SUNLIGHT) return "message.bdragon.steed.stuck";
        if (type == DragonType.MOONLIGHT) return "message.bdragon.moon.stuck";
        return type == DragonType.STORM ? "message.bdragon.serpent.stuck" : "message.bdragon.tortoise.stuck";
    }
}
