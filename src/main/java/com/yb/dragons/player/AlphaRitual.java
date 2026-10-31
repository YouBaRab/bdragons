package com.yb.dragons.player;

import com.mojang.math.Transformation;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The awakening of the Alpha (server side): the legendary dragon consumes the Orb of Power of his race.
 * The orb floats out of his claws and above his head, the dragon rises into the air in a vortex of his
 * element, the orb dives into his chest and breaks, he glows white... and explodes into the Alpha of
 * his race, bigger, with a roar that pushes everything around. He then lands again.
 *
 * <p>During the ritual the dragon cannot move, act nor be hurt. The same timeline ({@link DragonAlpha})
 * drives the animation of the model and the third person camera on the clients.</p>
 */
public final class AlphaRitual {
    /** Tag of the floating orb entity, so that an orb saved by accident (server stopped during the ritual) is removed. */
    public static final String ORB_TAG = "bdragon_ritual_orb";

    private AlphaRitual() {}

    // ------------------------------------------------------------------------------------------------------
    // Start
    // ------------------------------------------------------------------------------------------------------

    /**
     * Use of an Orb of Power.
     * @return true when the ritual started (the caller consumes the orb)
     */
    public static boolean start(ServerPlayer player, DragonType orbType) {
        DragonPlayerData data = DragonPlayerManager.get(player);
        if (data.inRitual()) return false;
        DragonType type = data.type();
        if (type == null) {
            refuse(player, Component.translatable("message.bdragon.orb.not_dragon"));
            return false;
        }
        if (type != orbType) {
            refuse(player, Component.translatable("message.bdragon.orb.wrong_race", orbType.displayName()));
            return false;
        }
        if (data.isAlpha()) {
            refuse(player, Component.translatable("message.bdragon.orb.already"));
            return false;
        }
        if (!data.stage().isMax()) {
            refuse(player, Component.translatable("message.bdragon.orb.need_legendary", DragonStage.LEGENDARY.displayName()));
            return false;
        }
        if (!player.onGround() || player.isPassenger()) {
            refuse(player, Component.translatable("message.bdragon.orb.ground"));
            return false;
        }
        // the alpha of the forest becomes a tortoise of 17 x 12 x 12 blocks, the alpha of the storm a serpent of 43
        // blocks: no ceiling above them
        if (!AlphaHitboxes.hasRoom(player, type)) {
            refuse(player, Component.translatable(AlphaHitboxes.noRoomKey(type)));
            return false;
        }

        ServerLevel level = player.level();
        // the orb can only be consumed by the full dragon: the hybrid is forced into his dragon form
        if (data.form() != DragonForm.DRAGON) {
            data.setForm(DragonForm.DRAGON);
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
            level.sendParticles(type.breathParticle(), player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.9, 0.6, 0.05);
            sound(level, player, SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 1.0F);
        }
        LifeBeam.stop(player, data);
        StormCall.stop(player, data);
        NetherDomain.stop(player, data);
        SunStrike.stop(player, data);
        data.ritualTick = 0;
        data.ritualStart = level.getGameTime();
        data.ritualYaw = player.getYRot();
        data.breathing = false;
        data.gliding = false;
        data.markAll();
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.needsSync = true;
        DragonPlayerManager.refreshAttributes(player, data);

        spawnOrb(level, player, data, type);
        sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.6F, 0.7F);
        sound(level, player, SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("message.bdragon.orb.start").withStyle(s -> s.withColor(type.textColor()).withItalic(true)));
        return true;
    }

    private static void refuse(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED), true);
    }

    // ------------------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------------------

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        DragonType type = data.type();
        if (type == null || player.isDeadOrDying() || player.isRemoved()) {
            end(player, data);
            return;
        }
        if (data.form() != DragonForm.DRAGON) data.setForm(DragonForm.DRAGON);
        player.resetFallDistance();
        data.breathing = false;

        int t = data.ritualTick;
        ServerLevel level = player.level();
        float s = t < DragonAlpha.CLIMAX ? DragonStats.of(DragonStage.LEGENDARY, false).renderScale() : DragonStats.alpha(type).renderScale();
        float lift = DragonAlpha.lift(t);
        double x = player.getX(), y = player.getY(), z = player.getZ();
        double yaw = data.ritualYaw * Mth.DEG_TO_RAD;
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        // centre of the body and position of the chest (where the orb goes in)
        double cy = y + lift + 1.15 * s;
        double chestX = x + fx * 0.6 * s, chestY = y + lift + 1.1 * s, chestZ = z + fz * 0.6 * s;
        ParticleOptions breath = type.breathParticle();
        ParticleOptions aura = type.auraParticle();

        // --- 1. the orb rises from the claws to above the head, runes fly into it -------------------------
        Vec3 orb = orbPosition(t, x, y, z, fx, fz, s, lift);
        if (t <= DragonAlpha.ORB_BREAK) {
            moveOrb(level, data, orb);
            if (t % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (level.getRandom().nextDouble() - 0.5) * 6, oy = level.getRandom().nextDouble() * 3, oz = (level.getRandom().nextDouble() - 0.5) * 6;
                    level.sendParticles(ParticleTypes.ENCHANT, orb.x, orb.y, orb.z, 0, ox, oy, oz, 1.0);
                }
            }
            double a = t * 0.45;
            for (int k = 0; k < 2; k++) {
                double ak = a + k * Math.PI;
                level.sendParticles(breath, orb.x + Math.cos(ak) * 0.7, orb.y + Math.sin(t * 0.2) * 0.2, orb.z + Math.sin(ak) * 0.7, 1, 0, 0, 0, 0);
            }
            if (t % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 1, 0.15, 0.15, 0.15, 0.01);
        }
        if (t == DragonAlpha.ORB_RISE_END) sound(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 0.6F);

        // --- 2. the dragon rises, a vortex of its element turns around it ------------------------------------
        if (t >= DragonAlpha.LIFT_START && t < DragonAlpha.CLIMAX) {
            float k = (t - DragonAlpha.LIFT_START) / (float) (DragonAlpha.CLIMAX - DragonAlpha.LIFT_START);
            double radius = (3.4 - 1.6 * k) * s;
            for (int arm = 0; arm < 3; arm++) {
                double a = t * (0.22 + 0.25 * k) + arm * Mth.TWO_PI / 3;
                double h = ((t * 0.045 + arm / 3.0) % 1.0) * 2.8 * s;
                double px = x + Math.cos(a) * radius, pz = z + Math.sin(a) * radius;
                // the particles turn around the dragon (tangent speed) and climb
                level.sendParticles(breath, px, y + h, pz, 0, -Math.sin(a), 0.25, Math.cos(a), 0.18);
            }
            if (t % 3 == 0) {
                for (int i = 0; i < 16; i++) {
                    double a = i / 16.0 * Mth.TWO_PI + t * 0.05;
                    level.sendParticles(aura, x + Math.cos(a) * 3.2 * s, y + 0.1, z + Math.sin(a) * 3.2 * s, 1, 0, 0, 0, 0);
                }
            }
        }
        // the heart of the dragon beats faster and faster
        for (int beat : new int[] {44, 58, 70, 79, 86, 91, 95, 98}) {
            if (t == beat) sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.8F + 0.04F * (beat - 44) / 6F);
        }
        // lightning around the dragon (visual only)
        if (t == 62 || t == 74 || t == 84) {
            double a = t * 1.7;
            lightning(level, x + Math.cos(a) * 7, y, z + Math.sin(a) * 7);
        }

        // --- 3. the orb dives into the chest and breaks; the energy is sucked into the body -------------------
        if (t == DragonAlpha.ORB_BREAK) {
            removeOrb(level, data);
            level.sendParticles(ParticleTypes.END_ROD, true, true, chestX, chestY, chestZ, 40, 0.2, 0.2, 0.2, 0.25);
            level.sendParticles(breath, true, true, chestX, chestY, chestZ, 60, 0.3, 0.3, 0.3, 0.3);
            level.sendParticles(DragonType.flash(type.color()), true, true, chestX, chestY, chestZ, 1, 0, 0, 0, 0);
            sound(level, player, SoundEvents.GLASS_BREAK, 1.6F, 0.7F);
            sound(level, player, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.5F);
        }
        if (t > DragonAlpha.ORB_BREAK && t < DragonAlpha.CLIMAX) {
            for (int i = 0; i < 6; i++) {
                double a = level.getRandom().nextDouble() * Mth.TWO_PI, b = (level.getRandom().nextDouble() - 0.3) * 1.2;
                double r = 4.5 * s;
                double px = x + Math.cos(a) * r, py = cy + b * r * 0.5, pz = z + Math.sin(a) * r;
                level.sendParticles(breath, px, py, pz, 0, x - px, cy - py, z - pz, 0.12);
            }
        }

        // --- 4. the explosion: the alpha is born ---------------------------------------------------------------
        if (t == DragonAlpha.CLIMAX) {
            awaken(player, data, false);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, true, x, cy, z, 1, 0, 0, 0, 0);
            for (int i = 0; i < 3; i++) level.sendParticles(DragonType.flash(type.secondaryColor()), true, true, x, cy, z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SONIC_BOOM, true, true, x, cy, z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, true, true, x, cy, z, 160, 1.0, 1.0, 1.0, 0.7);
            level.sendParticles(ParticleTypes.END_ROD, true, true, x, cy, z, 50, 1.2, 1.2, 1.2, 0.3);
            sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.6F);
            sound(level, player, SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.6F);
            sound(level, player, SoundEvents.TOTEM_USE, 1.5F, 0.8F);
            shockwave(level, player, 10 + 2 * s);
        }
        if (t == DragonAlpha.CLIMAX || t == DragonAlpha.CLIMAX + 4 || t == DragonAlpha.CLIMAX + 8) {
            // rings of the element, faster and faster
            double speed = 0.5 + (t - DragonAlpha.CLIMAX) * 0.12;
            for (int i = 0; i < 72; i++) {
                double a = i / 72.0 * Mth.TWO_PI;
                level.sendParticles(breath, true, false, x, y + 0.4 + lift * 0.5, z, 0, Math.cos(a), 0.02, Math.sin(a), speed);
            }
        }
        if (t > DragonAlpha.CLIMAX && t < DragonAlpha.ROAR_END) {
            for (int i = 0; i < 4; i++) {
                double a = level.getRandom().nextDouble() * Mth.TWO_PI;
                double r = (1.2 + level.getRandom().nextDouble() * 1.4) * s;
                level.sendParticles(aura, x + Math.cos(a) * r, y + level.getRandom().nextDouble() * 2.6 * s, z + Math.sin(a) * r, 1, 0, 0.02, 0, 0.01);
            }
        }
        if (t == DragonAlpha.CLIMAX + 10) announce(player, type);

        // --- 5. landing ----------------------------------------------------------------------------------------
        if (t == DragonAlpha.LAND_END) {
            ParticleOptions ground = new BlockParticleOption(ParticleTypes.BLOCK, type.groundBlock().defaultBlockState());
            level.sendParticles(ground, true, false, x, y + 0.1, z, 120, 2.0 * s, 0.1, 2.0 * s, 0.2);
            level.sendParticles(ParticleTypes.POOF, x, y + 0.2, z, 30, 1.5 * s, 0.1, 1.5 * s, 0.05);
            sound(level, player, SoundEvents.GENERIC_BIG_FALL, 1.5F, 0.6F);
            sound(level, player, SoundEvents.ENDER_DRAGON_FLAP, 1.5F, 0.7F);
        }

        data.ritualTick++;
        if (data.ritualTick > DragonAlpha.DURATION) end(player, data);
    }

    /** Where the orb floats at the given tick (in front of the mouth, then above the head, then into the chest). */
    private static Vec3 orbPosition(int t, double x, double y, double z, double fx, double fz, float s, float lift) {
        // claws (in front of the chest) -> above the head
        double startF = 1.8 * s, startY = 0.9 * s;
        double topF = 1.4 * s, topY = 3.0 * s + 0.6;
        double chestF = 0.6 * s, chestY = 1.1 * s;
        double f, h;
        if (t < DragonAlpha.ORB_RISE_END) {
            float k = DragonAlpha.phase(t, 0, DragonAlpha.ORB_RISE_END);
            f = Mth.lerp(k, startF, topF);
            h = Mth.lerp(k, startY, topY);
        } else if (t < DragonAlpha.ORB_BREAK - 8) {
            f = topF;
            h = topY + Math.sin(t * 0.15) * 0.15;
        } else {
            float k = DragonAlpha.phase(t, DragonAlpha.ORB_BREAK - 8, DragonAlpha.ORB_BREAK);
            f = Mth.lerp(k, topF, chestF);
            h = Mth.lerp(k, topY, chestY);
        }
        // during the levitation the orb keeps its place above the dragon
        return new Vec3(x + fx * f, y + h + lift, z + fz * f);
    }

    /** The new alpha pushes everything around him (no damage). */
    private static void shockwave(ServerLevel level, ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        for (Entity e : level.getEntities(player, box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
            if (e instanceof WildDragon d && d.isDisplay()) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 push = e.position().subtract(player.position());
            double dist = Math.max(0.5, push.horizontalDistance());
            double strength = 1.6 * (1 - Math.min(1, dist / radius)) + 0.4;
            Vec3 dir = new Vec3(push.x / dist, 0, push.z / dist);
            e.push(dir.x * strength, 0.35 + strength * 0.2, dir.z * strength);
            e.needsSync = true;
        }
    }

    private static void lightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private static void sound(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ------------------------------------------------------------------------------------------------------
    // End
    // ------------------------------------------------------------------------------------------------------

    /** The moment the dragon becomes the alpha (the climax of the ritual, or its interruption). */
    private static void awaken(ServerPlayer player, DragonPlayerData data, boolean quiet) {
        if (data.isAlpha() || data.type() == null) return;
        data.setAlpha(true);
        DragonPlayerManager.refreshAttributes(player, data);
        data.energy = data.stats().maxEnergy();
        player.setHealth(player.getMaxHealth());
        if (quiet) announce(player, data.type());
    }

    /** Messages: to the new alpha, and to everybody on the server. */
    private static void announce(ServerPlayer player, DragonType type) {
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha.done", type.displayName())
                .withStyle(s -> s.withColor(type.textColor()).withBold(true)));
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha.bonus").withStyle(ChatFormatting.YELLOW));
        // the alphas of the forest and of the storm get a whole new body: the Sylvan Tortoise, the Tempest Dragon
        boolean own = type == DragonType.FOREST || type == DragonType.STORM || type == DragonType.NETHER || type == DragonType.SUNLIGHT || type == DragonType.MOONLIGHT;
        player.sendSystemMessage(Component.translatable(own ? "message.bdragon.alpha.look." + type.id() : "message.bdragon.alpha.look")
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha.power", Component.keybind("key.bdragon.alpha_power"),
                DragonAlpha.powerCooldown(type) / 20).withStyle(ChatFormatting.YELLOW));
        if (own) {
            player.sendSystemMessage(Component.translatable("message.bdragon.alpha.power." + type.id()).withStyle(s -> s.withColor(type.textColor())));
        }
        Component everyone = Component.translatable("message.bdragon.alpha.broadcast", player.getDisplayName(), type.displayName())
                .withStyle(s -> s.withColor(type.textColor()));
        for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
            if (other != player) other.sendSystemMessage(everyone);
        }
    }

    /**
     * Ends the ritual now. Interrupted (disconnection, death, change of dimension...) the dragon still
     * becomes the alpha: the orb was consumed.
     */
    public static void end(ServerPlayer player, DragonPlayerData data) {
        if (!data.inRitual()) return;
        boolean wasAlpha = data.isAlpha();
        removeOrb(player.level(), data);
        data.ritualTick = -1;
        data.ritualStart = -1;
        data.markAll();
        if (!wasAlpha) awaken(player, data, true);
        DragonPlayerManager.refreshAttributes(player, data);
    }

    // ------------------------------------------------------------------------------------------------------
    // The floating orb (an item display, full bright, always facing the camera)
    // ------------------------------------------------------------------------------------------------------

    private static void spawnOrb(ServerLevel level, ServerPlayer player, DragonPlayerData data, DragonType type) {
        removeOrb(level, data);
        Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return;
        CompoundTag tag = new CompoundTag();
        tag.putString("billboard", "center");
        tag.putInt("teleport_duration", 2);
        tag.store("brightness", Brightness.CODEC, new Brightness(15, 15));
        tag.store("transformation", Transformation.EXTENDED_CODEC,
                new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.4F, 1.4F, 1.4F), new Quaternionf()));
        display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        var slot = display.getSlot(0);
        if (slot != null) slot.set(new ItemStack(ModRegistry.ALPHA_ORBS.get(type).get()));
        display.addTag(ORB_TAG);
        double yaw = data.ritualYaw * Mth.DEG_TO_RAD;
        float s = DragonStats.of(DragonStage.LEGENDARY, false).renderScale();
        Vec3 pos = orbPosition(0, player.getX(), player.getY(), player.getZ(), -Math.sin(yaw), Math.cos(yaw), s, 0);
        display.snapTo(pos.x, pos.y, pos.z, 0, 0);
        if (level.addFreshEntity(display)) data.ritualOrb = display.getId();
    }

    private static void moveOrb(ServerLevel level, DragonPlayerData data, Vec3 pos) {
        if (data.ritualOrb < 0) return;
        Entity e = level.getEntity(data.ritualOrb);
        if (e != null) e.snapTo(pos.x, pos.y, pos.z, 0, 0);
    }

    private static void removeOrb(ServerLevel level, DragonPlayerData data) {
        if (data.ritualOrb < 0) return;
        // entity ids are unique on the whole server: the orb is found even if the player changed dimension
        for (ServerLevel l : level.getServer().getAllLevels()) {
            Entity e = l.getEntity(data.ritualOrb);
            if (e != null && e.entityTags().contains(ORB_TAG)) {
                e.discard();
                break;
            }
        }
        data.ritualOrb = -1;
    }

    /** An orb loaded from the disk belongs to no ritual anymore. */
    public static boolean isStaleOrb(Entity entity) {
        return entity.entityTags().contains(ORB_TAG);
    }
}
