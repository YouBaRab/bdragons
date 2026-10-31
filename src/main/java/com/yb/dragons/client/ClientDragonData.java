package com.yb.dragons.client;

import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.AlphaNether;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.AlphaMoon;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.client.Minecraft;
import com.yb.dragons.network.ModNetwork;
import com.yb.dragons.player.DragonPlayerData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

/** Client copy of the dragon data: public info of every player + full data of the local player. */
public final class ClientDragonData {
    /**
     * @param ritualStart game time when the awakening ritual of the alpha started (-1 = no ritual)
     * @param ritualYaw   body yaw of the dragon during the ritual
     * @param beamStart   game time when the Beam of Life of the alpha of the forest started (-1 = none)
     * @param stormStart  game time when the Storm of the alpha of the storm was called (-1 = none)
     * @param domainStart game time when the Territory of the alpha of the Nether was called (-1 = none)
     * @param sunStart    game time when the alpha of the sunlight last threw its sun (-1 = never)
     */
    public record Info(@Nullable DragonType type, int stage, DragonForm form, boolean breathing, boolean alpha, long ritualStart,
                       float ritualYaw, long beamStart, long stormStart, long domainStart, long sunStart) {
        /** Drawn with the dragon model. */
        public boolean isActiveDragon() {
            return type != null && form == DragonForm.DRAGON;
        }

        /** Drawn as a human with horns, wings and tail. */
        public boolean isHybrid() {
            return type != null && form == DragonForm.HYBRID;
        }

        /** The Alpha of his race (legendary dragon who consumed the orb of power). */
        public boolean isAlpha() {
            return type != null && alpha && stage >= 5;
        }

        public DragonStats stats() {
            return DragonStats.of(type, DragonStage.byNumber(stage), isAlpha());
        }

        /** Drawn as the Sylvan Tortoise (the alpha of the forest in his dragon form). */
        public boolean isTortoise() {
            return AlphaForest.isTortoise(type, stage, alpha, form);
        }

        /** Drawn as the Tempest Dragon (the alpha of the storm in his dragon form). */
        public boolean isSerpent() {
            return AlphaStorm.isSerpent(type, stage, alpha, form);
        }

        /** Drawn as the Demon of the Nether (the alpha of the Nether in his dragon form). */
        public boolean isDemon() {
            return AlphaNether.isDemon(type, stage, alpha, form);
        }

        /** Drawn as the Moon Dragon (the alpha of the moonlight in his dragon form). */
        public boolean isMoon() {
            return AlphaMoon.isMoon(type, stage, alpha, form);
        }

        /** Drawn as the Sun Steed (the alpha of the sunlight in his dragon form). */
        public boolean isSteed() {
            return AlphaSun.isSteed(type, stage, alpha, form);
        }

        /** Ticks since the sun was thrown (with the partial tick), or -1 when its sun is whole again. */
        public float sunAge(float partialTick) {
            if (sunStart < 0) return -1;
            var level = Minecraft.getInstance().level;
            if (level == null) return -1;
            float age = (level.getGameTime() - sunStart) + partialTick;
            return age > AlphaSun.POWER_COOLDOWN ? -1 : Math.max(0, age);
        }

        /** Ticks since the Territory was called (with the partial tick), or -1. */
        public float domainAge(float partialTick) {
            if (domainStart < 0) return -1;
            var level = Minecraft.getInstance().level;
            if (level == null) return -1;
            float age = (level.getGameTime() - domainStart) + partialTick;
            return age > AlphaNether.DURATION + 5 ? -1 : Math.max(0, age);
        }

        /** Ticks since the Storm was called (with the partial tick), or -1. */
        public float stormAge(float partialTick) {
            if (stormStart < 0) return -1;
            var level = Minecraft.getInstance().level;
            if (level == null) return -1;
            float age = (level.getGameTime() - stormStart) + partialTick;
            return age > AlphaStorm.STORM_DURATION + 5 ? -1 : Math.max(0, age);
        }

        /** Ticks since the Beam of Life started (with the partial tick), or -1. */
        public float beamAge(float partialTick) {
            if (beamStart < 0) return -1;
            var level = Minecraft.getInstance().level;
            if (level == null) return -1;
            float age = (level.getGameTime() - beamStart) + partialTick;
            return age > AlphaForest.BEAM_DURATION + 5 ? -1 : Math.max(0, age);
        }

        /** Ticks since the start of the ritual (with the partial tick), or -1 when there is no ritual. */
        public float ritualAge(float partialTick) {
            if (ritualStart < 0) return -1;
            var level = Minecraft.getInstance().level;
            if (level == null) return -1;
            float age = (level.getGameTime() - ritualStart) + partialTick;
            return age > DragonAlpha.DURATION + 10 ? -1 : Math.max(0, age);
        }
    }

    private static final Int2ObjectMap<Info> PLAYERS = new Int2ObjectOpenHashMap<>();
    private static DragonPlayerData own = new DragonPlayerData();

    private ClientDragonData() {}

    public static void handlePublic(ModNetwork.PublicSync msg) {
        if (msg.type() < 0) {
            PLAYERS.remove(msg.entityId());
        } else {
            PLAYERS.put(msg.entityId(), new Info(DragonType.byOrdinal(msg.type()), msg.stage() + 1, DragonForm.byOrdinal(msg.form()),
                    msg.breathing(), msg.alpha(), msg.ritualStart(), msg.ritualYaw(), msg.beamStart(), msg.stormStart(), msg.domainStart(), msg.sunStart()));
        }
    }

    public static void handlePrivate(ModNetwork.PrivateSync msg) {
        own = msg.data();
    }

    @Nullable
    public static Info get(int entityId) {
        return PLAYERS.get(entityId);
    }

    public static DragonPlayerData own() {
        return own;
    }

    public static void clear() {
        PLAYERS.clear();
        own = new DragonPlayerData();
    }
}
