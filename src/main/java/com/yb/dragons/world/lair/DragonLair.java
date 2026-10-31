package com.yb.dragons.world.lair;

import com.yb.dragons.dragon.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The lair of one wild dragon. Every lair is decided once and for all by the world seed (like a
 * structure): its position, its race (from the biome) and its rank (from the distance to the spawn).
 * The dragon itself only appears when a player comes close, and comes back some days after a defeat.
 */
public final class DragonLair {
    /** Region of the lair grid ({@link net.minecraft.world.level.ChunkPos#asLong} of the region coordinates). */
    public final long region;
    public final int x;
    public final int z;
    public final DragonType type;
    public final int rank;
    /** Altitude where the dragon was spawned the first time (Integer.MIN_VALUE until then). */
    public int y = Integer.MIN_VALUE;
    /** Game time until which the lair is empty after a defeat (0 = a dragon lives here). */
    public long defeatedUntil;
    /** The dragon currently living in this lair, if it has been spawned. */
    @Nullable public UUID dragon;
    /** The nest was built (or there was no room for it). */
    public boolean built;
    /** Where the dragon appears in its nest (the pink wool), null without a nest. */
    @Nullable public BlockPos nest;

    public DragonLair(long region, int x, int z, DragonType type, int rank) {
        this.region = region;
        this.x = x;
        this.z = z;
        this.type = type;
        this.rank = rank;
    }

    public boolean isDefeated(long gameTime) {
        return defeatedUntil > gameTime;
    }

    public BlockPos center(int fallbackY) {
        return new BlockPos(x, y == Integer.MIN_VALUE ? fallbackY : y, z);
    }

    public double distanceSqr(double px, double pz) {
        double dx = px - x, dz = pz - z;
        return dx * dx + dz * dz;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("region", region);
        tag.putInt("x", x);
        tag.putInt("z", z);
        tag.putInt("y", y);
        tag.putString("type", type.id());
        tag.putInt("rank", rank);
        tag.putLong("defeatedUntil", defeatedUntil);
        if (dragon != null) tag.putString("dragon", dragon.toString());
        tag.putBoolean("built", built);
        if (nest != null) {
            tag.putInt("nestX", nest.getX());
            tag.putInt("nestY", nest.getY());
            tag.putInt("nestZ", nest.getZ());
        }
        return tag;
    }

    @Nullable
    public static DragonLair load(CompoundTag tag) {
        DragonType type = tag.getString("type").map(DragonType::byId).orElse(null);
        if (type == null) return null;
        DragonLair lair = new DragonLair(tag.getLongOr("region", 0), tag.getIntOr("x", 0), tag.getIntOr("z", 0), type,
                Math.max(1, Math.min(5, tag.getIntOr("rank", 1))));
        lair.y = tag.getIntOr("y", Integer.MIN_VALUE);
        lair.defeatedUntil = tag.getLongOr("defeatedUntil", 0);
        lair.dragon = tag.getString("dragon").map(s -> {
            try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
        }).orElse(null);
        lair.built = tag.getBooleanOr("built", false);
        if (tag.getInt("nestY").isPresent()) {
            lair.nest = new BlockPos(tag.getIntOr("nestX", lair.x), tag.getIntOr("nestY", 64), tag.getIntOr("nestZ", lair.z));
        }
        return lair;
    }
}
