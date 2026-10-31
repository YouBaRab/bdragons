package com.yb.dragons.world.lair;

import com.mojang.serialization.Codec;
import com.yb.dragons.Config;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.network.ModNetwork;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The dragon lairs of one dimension.
 *
 * <p>The world is cut into big regions (like the structures of vanilla Minecraft). Each region has
 * one candidate position chosen from the seed; a lair exists there only if the biome is one of the
 * favourite biomes of a race and is large enough (the biome is checked around the candidate too).
 * Lairs are therefore rare, never two in the same small biome, and the same for everybody.</p>
 *
 * <p>The rank of a lair grows with the distance from the spawn point: rank I-II near the spawn,
 * rank V only far away, in the Nether or on the outer End islands.</p>
 */
public class LairManager extends SavedData {
    public static final Codec<LairManager> CODEC = CompoundTag.CODEC.xmap(LairManager::fromTag, LairManager::toTag);
    public static final SavedDataType<LairManager> TYPE = new SavedDataType<>(YBDragons.id("lairs"), LairManager::new, CODEC, null);

    /** Distance at which the dragon of a lair appears. */
    public static final int SPAWN_DISTANCE = 112;
    /** Distance at which a player discovers a lair for good (it stays on his map). */
    public static final int DISCOVER_DISTANCE = 160;

    /** Region grid: spacing and minimum separation in chunks, like a vanilla "random spread" structure. */
    private record Placement(int spacing, int separation, int salt) {
        int regionBlocks() { return spacing * 16; }
    }

    /**
     * The nests are about as frequent as the villages (34 chunks apart in vanilla): a nest of rank III is 1.25 times
     * rarer than a village, of rank IV 2 times, of rank V 2.5 times. Together they are 1 / 1.25 + 1 / 2 + 1 / 2.5 = 1.7
     * times as frequent as the villages, hence a grid of 34 / sqrt(1.7) = 26 chunks, the rank drawn with the same
     * proportions ({@link #rankFor}).
     */
    private static final Placement OVERWORLD = new Placement(26, 8, 0x1D4A62);
    private static final Placement NETHER = new Placement(20, 6, 0x2E7B3F);
    private static final Placement END = new Placement(28, 8, 0x5C9013);

    private final Map<Long, DragonLair> lairs = new HashMap<>();
    /** Regions already evaluated that do not contain any lair. */
    private final Set<Long> empty = new HashSet<>();
    /** Last list sent to each player (hash), to only send changes. */
    private final Map<UUID, Integer> lastSync = new HashMap<>();

    public LairManager() {}

    public static LairManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }

    /** Key stored in the player data: "dimension|region". */
    public static String knownKey(Level level, long region) {
        return dimensionId(level) + "|" + region;
    }

    private static Placement placement(ServerLevel level) {
        if (level.dimension() == Level.NETHER) return NETHER;
        if (level.dimension() == Level.END) return END;
        return OVERWORLD;
    }

    // ------------------------------------------------------------------------------------------------------
    // Lair generation (deterministic)
    // ------------------------------------------------------------------------------------------------------

    @Nullable
    public DragonLair lair(long region) {
        return lairs.get(region);
    }

    public boolean isEvaluated(long region) {
        return lairs.containsKey(region) || empty.contains(region);
    }

    /** Returns the lair of the region (deciding it if needed), or null if the region has none. */
    @Nullable
    public DragonLair evaluate(ServerLevel level, int rx, int rz) {
        long key = ChunkPos.pack(rx, rz);
        DragonLair known = lairs.get(key);
        if (known != null || empty.contains(key)) return known;
        DragonLair lair = decide(level, rx, rz, key);
        if (lair == null) empty.add(key);
        else lairs.put(key, lair);
        setDirty();
        return lair;
    }

    @Nullable
    private static DragonLair decide(ServerLevel level, int rx, int rz, long key) {
        Placement p = placement(level);
        Random random = new Random(level.getSeed() ^ (rx * 341873128712L + rz * 132897987541L + p.salt()));
        int range = p.spacing() - p.separation();
        int cx = rx * p.spacing() + random.nextInt(range);
        int cz = rz * p.spacing() + random.nextInt(range);
        int x = cx * 16 + 8, z = cz * 16 + 8;
        int roll = random.nextInt(100);

        // the End dragons live on the outer islands only
        if (level.dimension() == Level.END && (long) x * x + (long) z * z < 1100L * 1100L) return null;

        int y = 64;
        if (level.dimension() == Level.OVERWORLD) {
            var source = level.getChunkSource();
            y = Math.max(level.getSeaLevel(), source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState()));
        }
        DragonType type = LairBiomes.typeFor(biomeAt(level, x, y, z));
        if (type == null) return null;
        // the biome must be large: at least 2 of the 4 points around the lair are in a biome of the same race
        int same = 0;
        int d = 48;
        int[][] around = {{d, 0}, {-d, 0}, {0, d}, {0, -d}};
        for (int[] o : around) {
            if (LairBiomes.typeFor(biomeAt(level, x + o[0], y, z + o[1])) == type) same++;
        }
        if (same < 2) return null;
        DragonLair lair = new DragonLair(key, x, z, type, rankFor(level, x, z, roll));
        return lair;
    }

    private static Holder<Biome> biomeAt(ServerLevel level, int x, int y, int z) {
        return level.getUncachedNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z));
    }

    /**
     * Rank III..V (the dragons of rank I and II never live in a nest: the Challenge Horn calls them). Far from the
     * spawn: 47 % of rank III, 29 % of rank IV, 24 % of rank V (the proportions 1 / 1.25, 1 / 2 and 1 / 2.5 of the
     * rarity of a village); near the spawn the weakest only.
     */
    private static int rankFor(ServerLevel level, int x, int z, int roll) {
        int[] far = {0, 0, 47, 29, 24};
        int[] weights;
        if (level.dimension() == Level.END) {
            weights = far;
        } else if (level.dimension() == Level.NETHER) {
            double d = Math.sqrt((double) x * x + (double) z * z);
            if (d < 250) weights = new int[] {0, 0, 100, 0, 0};
            else if (d < 700) weights = new int[] {0, 0, 62, 38, 0};
            else weights = far;
        } else {
            BlockPos spawn = level.getRespawnData().pos();
            double dx = x - spawn.getX(), dz = z - spawn.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 800) weights = new int[] {0, 0, 100, 0, 0};
            else if (d < 2000) weights = new int[] {0, 0, 62, 38, 0};
            else weights = far;
        }
        int acc = 0;
        for (int i = 0; i < 5; i++) {
            acc += weights[i];
            if (roll < acc) return i + 1;
        }
        return 1;
    }

    public static int regionCoord(ServerLevel level, double block) {
        return Math.floorDiv((int) Math.floor(block), placement(level).regionBlocks());
    }

    // ------------------------------------------------------------------------------------------------------
    // Ticking: evaluation around the players, discovery, dragon spawning and sync
    // ------------------------------------------------------------------------------------------------------

    /** Called every second for every level. */
    public static void tick(ServerLevel level) {
        if (level.players().isEmpty() || !Config.LAIRS.get()) return;
        LairManager manager = get(level);
        long now = level.getGameTime();
        boolean sync = level.getGameTime() % 100 == 0;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            manager.evaluateAround(level, player, 3);
            manager.visit(level, player, now);
            if (sync) manager.syncTo(player, false);
        }
    }

    /** Decides a few lairs per second around the player (the lair sense radius + the spawn distance). */
    private void evaluateAround(ServerLevel level, ServerPlayer player, int budget) {
        DragonPlayerData data = DragonPlayerManager.get(player);
        int sense = data.isDragon() ? data.stats().senseRadius() : DISCOVER_DISTANCE;
        int radius = (sense + SPAWN_DISTANCE) / placement(level).regionBlocks() + 1;
        int prx = regionCoord(level, player.getX()), prz = regionCoord(level, player.getZ());
        // spiral: closest regions first
        for (int r = 0; r <= radius && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    if (isEvaluated(ChunkPos.pack(prx + dx, prz + dz))) continue;
                    evaluate(level, prx + dx, prz + dz);
                    budget--;
                }
            }
        }
    }

    private void visit(ServerLevel level, ServerPlayer player, long now) {
        int prx = regionCoord(level, player.getX()), prz = regionCoord(level, player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                DragonLair lair = lairs.get(ChunkPos.pack(prx + dx, prz + dz));
                if (lair == null) continue;
                double distSqr = lair.distanceSqr(player.getX(), player.getZ());
                // the nest is built as soon as its chunks are loaded (it is there before the player sees it)
                if (!lair.built && NestBuilder.ready(level, lair)) {
                    lair.built = true;
                    lair.nest = NestBuilder.build(level, lair);
                    setDirty();
                }
                if (distSqr < DISCOVER_DISTANCE * DISCOVER_DISTANCE) discover(level, player, lair);
                if (distSqr < SPAWN_DISTANCE * SPAWN_DISTANCE) ensureDragon(level, lair, now);
            }
        }
    }

    private void discover(ServerLevel level, ServerPlayer player, DragonLair lair) {
        DragonPlayerData data = DragonPlayerManager.get(player);
        if (!data.addKnownLair(knownKey(level, lair.region))) return;
        player.sendSystemMessage(Component.translatable("message.bdragon.lair_found", lair.type.displayName(), roman(lair.rank))
                .withStyle(s -> s.withColor(lair.type.textColor())), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE,
                0.4F, 1.6F - lair.rank * 0.15F);
        syncTo(player, true);
    }

    /** Makes sure the dragon of the lair is present (spawns it when needed). */
    private void ensureDragon(ServerLevel level, DragonLair lair, long now) {
        if (lair.isDefeated(now) || level.getDifficulty() == Difficulty.PEACEFUL) return;
        BlockPos column = new BlockPos(lair.x, level.getSeaLevel(), lair.z);
        if (!level.isPositionEntityTicking(column) || !level.areEntitiesLoaded(ChunkPos.pack(column))) return;
        if (lair.dragon != null) {
            Entity existing = level.getEntity(lair.dragon);
            if (existing instanceof WildDragon dragon && dragon.isAlive()) return;
        }
        // a dragon of this lair saved with the chunk?
        AABB box = new AABB(lair.x - 160, level.getMinY(), lair.z - 160, lair.x + 160, level.getMaxY(), lair.z + 160);
        List<WildDragon> present = level.getEntitiesOfClass(WildDragon.class, box, d -> d.isAlive() && d.getLairKey() == lair.region);
        if (!present.isEmpty()) {
            lair.dragon = present.get(0).getUUID();
            setDirty();
            return;
        }
        if (!lair.built) return;
        // in its nest (the pink wool), or in the air above the lair when there is no nest
        Integer y = lair.nest != null ? Integer.valueOf(lair.nest.getY()) : findSpawnY(level, lair);
        if (y == null) return;
        double sx = lair.nest != null ? lair.nest.getX() + 0.5 : lair.x + 0.5, sz = lair.nest != null ? lair.nest.getZ() + 0.5 : lair.z + 0.5;
        WildDragon dragon = ModRegistry.WILD_DRAGON.get().create(level, EntitySpawnReason.STRUCTURE);
        if (dragon == null) return;
        dragon.snapTo(sx, y, sz, level.getRandom().nextFloat() * 360F, 0);
        dragon.setup(lair.type, lair.rank);
        dragon.setLair(lair.region, BlockPos.containing(sx, y, sz));
        level.addFreshEntity(dragon);
        lair.dragon = dragon.getUUID();
        lair.y = y;
        setDirty();
    }

    @Nullable
    private static Integer findSpawnY(ServerLevel level, DragonLair lair) {
        var dims = ModRegistry.WILD_DRAGON.get().getDimensions();
        if (level.dimensionType().hasCeiling()) {
            // Nether: an open cave space, from the top
            for (int y = 100; y >= 36; y -= 2) {
                AABB box = dims.makeBoundingBox(lair.x + 0.5, y, lair.z + 0.5).inflate(1.5);
                if (level.noCollision(box)) return y;
            }
            return null;
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, lair.x, lair.z);
        int y = ground <= level.getMinY() + 1 ? 72 : ground + 8 + lair.rank * 2;
        y = Math.min(y, level.getMaxY() - 8);
        AABB box = dims.makeBoundingBox(lair.x + 0.5, y, lair.z + 0.5).inflate(1);
        return level.noCollision(box) ? y : null;
    }

    // ------------------------------------------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------------------------------------------

    /** The dragon of the lair was killed: the lair stays empty for a few days. */
    public void markDefeated(ServerLevel level, long region) {
        DragonLair lair = lairs.get(region);
        if (lair == null) return;
        lair.defeatedUntil = level.getGameTime() + Config.LAIR_RESPAWN_DAYS.get() * 24000L;
        lair.dragon = null;
        setDirty();
        for (ServerPlayer p : level.players()) syncTo(p, true);
    }

    /**
     * Places a lair at a chosen spot, whatever the biome (used by the museum world, which is a flat
     * world without any dragon biome). Replaces what the region of that spot contained.
     */
    public DragonLair addDemoLair(ServerLevel level, int x, int z, DragonType type, int rank) {
        long key = ChunkPos.pack(regionCoord(level, x), regionCoord(level, z));
        DragonLair lair = new DragonLair(key, x, z, type, Math.max(1, Math.min(5, rank)));
        // (no nest in the museum)
        lair.built = true;
        lairs.put(key, lair);
        empty.remove(key);
        setDirty();
        return lair;
    }

    /** True if {@code uuid} is the dragon that currently lives in the lair (old copies remove themselves). */
    public boolean isCurrentDragon(ServerLevel level, long region, UUID uuid) {
        DragonLair lair = lairs.get(region);
        if (lair == null || lair.isDefeated(level.getGameTime())) return false;
        if (lair.dragon == null) {
            lair.dragon = uuid;
            setDirty();
            return true;
        }
        return lair.dragon.equals(uuid);
    }

    /**
     * Nearest lair (alive) with a rank of at least {@code minRank}, searching ring after ring around
     * the position, up to {@code maxRadius} regions. Used by the Challenge Horn.
     */
    @Nullable
    public DragonLair findNearest(ServerLevel level, double px, double pz, int minRank, int maxRadius) {
        return findNearest(level, px, pz, minRank, maxRadius, null);
    }

    /** The nearest lair of at least this rank, only of the given race when {@code race} is not null (hunt for the alpha scales). */
    public DragonLair findNearest(ServerLevel level, double px, double pz, int minRank, int maxRadius, @org.jspecify.annotations.Nullable DragonType race) {
        int prx = regionCoord(level, px), prz = regionCoord(level, pz);
        long now = level.getGameTime();
        DragonLair best = null;
        double bestDist = Double.MAX_VALUE;
        for (int r = 0; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    DragonLair lair = evaluate(level, prx + dx, prz + dz);
                    if (lair == null || lair.rank < minRank || lair.isDefeated(now)) continue;
                    if (race != null && lair.type != race) continue;
                    double d = lair.distanceSqr(px, pz);
                    if (d < bestDist) {
                        bestDist = d;
                        best = lair;
                    }
                }
            }
            // a lair found in ring r cannot be beaten by a lair two rings further
            if (best != null && Math.sqrt(bestDist) < (r) * placement(level).regionBlocks()) break;
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------------------
    // Sync with the client (map markers)
    // ------------------------------------------------------------------------------------------------------

    /** Lairs shown to the player: the ones he discovered + the ones his dragon senses around him. */
    public List<ModNetwork.LairSync.Entry> visibleTo(ServerLevel level, ServerPlayer player) {
        DragonPlayerData data = DragonPlayerManager.get(player);
        int sense = data.isDragon() ? data.stats().senseRadius() : 0;
        long now = level.getGameTime();
        String dim = dimensionId(level);
        List<ModNetwork.LairSync.Entry> list = new ArrayList<>();
        for (DragonLair lair : lairs.values()) {
            boolean known = data.knownLairs().contains(dim + "|" + lair.region);
            boolean sensed = sense > 0 && lair.distanceSqr(player.getX(), player.getZ()) < (double) sense * sense;
            if (!known && !sensed) continue;
            list.add(new ModNetwork.LairSync.Entry(lair.region, lair.x, lair.z, lair.type.ordinal(), lair.rank, lair.isDefeated(now), known));
        }
        list.sort(Comparator.comparingLong(ModNetwork.LairSync.Entry::region));
        return list;
    }

    public void syncTo(ServerPlayer player, boolean force) {
        ServerLevel level = player.level();
        List<ModNetwork.LairSync.Entry> list = visibleTo(level, player);
        int hash = list.hashCode() * 31 + dimensionId(level).hashCode();
        Integer last = lastSync.get(player.getUUID());
        if (!force && last != null && last == hash) return;
        lastSync.put(player.getUUID(), hash);
        ModNetwork.sendToPlayer(player, new ModNetwork.LairSync(dimensionId(level), list));
    }

    public void forget(UUID player) {
        lastSync.remove(player);
    }

    /** Sends the lairs of the player's current dimension right away (login, respawn, dimension change). */
    public static void fullSync(ServerPlayer player) {
        LairManager manager = get(player.level());
        manager.lastSync.remove(player.getUUID());
        manager.syncTo(player, true);
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    public static Component direction(double fromX, double fromZ, double toX, double toZ) {
        double angle = Math.toDegrees(Math.atan2(toX - fromX, -(toZ - fromZ)));
        if (angle < 0) angle += 360;
        String[] dirs = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return Component.translatable("direction.bdragon." + dirs[(int) Math.round(angle / 45.0) % 8]);
    }

    // ------------------------------------------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------------------------------------------

    /** 2: the nests (1.8): the lairs decided before are decided again (ranks III to V, new biomes, new grid). */
    static final int VERSION = 2;

    private static LairManager fromTag(CompoundTag tag) {
        LairManager m = new LairManager();
        if (tag.getIntOr("version", 1) < VERSION) {
            m.setDirty();
            return m;
        }
        tag.getList("lairs").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                list.getCompound(i).map(DragonLair::load).ifPresent(l -> m.lairs.put(l.region, l));
            }
        });
        tag.getLongArray("empty").ifPresent(arr -> {
            for (long k : arr) m.empty.add(k);
        });
        return m;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (DragonLair lair : lairs.values()) list.add(lair.save());
        tag.put("lairs", list);
        tag.putLongArray("empty", empty.stream().mapToLong(Long::longValue).toArray());
        tag.putInt("version", VERSION);
        return tag;
    }
}
