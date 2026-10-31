package com.yb.dragons.client;

import com.yb.dragons.network.ModNetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dragon lairs the local player knows or senses, per dimension. Read by the codex and by the
 * JourneyMap plugin (which redraws its markers when {@link #version()} changes).
 */
public final class ClientLairData {
    private static final Map<String, List<ModNetwork.LairSync.Entry>> BY_DIMENSION = new HashMap<>();
    private static String currentDimension = "minecraft:overworld";
    private static int version;

    private ClientLairData() {}

    public static void handle(ModNetwork.LairSync msg) {
        BY_DIMENSION.put(msg.dimension(), List.copyOf(msg.lairs()));
        currentDimension = msg.dimension();
        version++;
    }

    public static int version() {
        return version;
    }

    public static String currentDimension() {
        return currentDimension;
    }

    public static Map<String, List<ModNetwork.LairSync.Entry>> all() {
        return BY_DIMENSION;
    }

    public static List<ModNetwork.LairSync.Entry> current() {
        return BY_DIMENSION.getOrDefault(currentDimension, List.of());
    }

    public static void clear() {
        BY_DIMENSION.clear();
        version++;
    }
}
