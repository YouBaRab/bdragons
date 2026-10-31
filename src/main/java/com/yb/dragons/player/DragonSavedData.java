package com.yb.dragons.player;

import com.mojang.serialization.Codec;
import com.yb.dragons.YBDragons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide storage of every player's dragon data, keyed by UUID.
 * Kept in the overworld data storage so it survives deaths, respawns and dimension travel.
 */
public class DragonSavedData extends SavedData {
    public static final Codec<DragonSavedData> CODEC = CompoundTag.CODEC.xmap(DragonSavedData::fromTag, DragonSavedData::toTag);
    // Forge allows a null DataFixTypes for mod data.
    public static final SavedDataType<DragonSavedData> TYPE = new SavedDataType<>(YBDragons.id("players"), DragonSavedData::new, CODEC, null);

    private final Map<UUID, DragonPlayerData> players = new HashMap<>();

    public DragonSavedData() {}

    public static DragonSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public DragonPlayerData get(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> {
            setDirty();
            return new DragonPlayerData();
        });
    }

    public Map<UUID, DragonPlayerData> all() {
        return players;
    }

    private static DragonSavedData fromTag(CompoundTag tag) {
        DragonSavedData data = new DragonSavedData();
        for (String key : tag.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                tag.getCompound(key).ifPresent(c -> data.players.put(uuid, DragonPlayerData.load(c)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        players.forEach((uuid, d) -> tag.put(uuid.toString(), d.save()));
        return tag;
    }
}
