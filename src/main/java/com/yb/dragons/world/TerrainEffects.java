package com.yb.dragons.world;

import com.mojang.serialization.Codec;
import com.yb.dragons.Config;
import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonType;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Iterator;
import java.util.List;

/**
 * Legendary (stage 5) dragons transform the ground under their feet, like Frost Walker boots:
 * magma for fire, snow and frosted ice for frost, coral and obsidian for water, moss and flowers for forest...
 * Every block goes back to its original state after a few seconds (never permanent, never lootable).
 */
public class TerrainEffects extends SavedData {
    public static final Codec<TerrainEffects> CODEC = CompoundTag.CODEC.xmap(TerrainEffects::fromTag, TerrainEffects::toTag);
    public static final SavedDataType<TerrainEffects> TYPE = new SavedDataType<>(YBDragons.id("terrain_effects"), TerrainEffects::new, CODEC, null);

    private static final List<Block> CORALS = List.of(Blocks.BRAIN_CORAL_BLOCK, Blocks.TUBE_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK,
            Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK);
    private static final List<Block> FLOWERS = List.of(Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET,
            Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS);

    private record Entry(BlockState original, BlockState temp, long revertAt) {}

    private final Long2ObjectMap<Entry> entries = new Long2ObjectOpenHashMap<>();

    public TerrainEffects() {}

    public static TerrainEffects get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // ------------------------------------------------------------------------------------------------------

    public static void tickPlayer(ServerPlayer player, DragonType type) {
        tickPlayer(player, type, 2);
    }

    /** @param radius 2 for the full dragon form, 1 for the hybrid form */
    public static void tickPlayer(ServerPlayer player, DragonType type, int radius) {
        if (!Config.TERRAIN_EFFECT.get() || player.tickCount % 2 != 0) return;
        if (!player.onGround() || player.getAbilities().flying || player.isSpectator()) return;
        ServerLevel level = player.level();
        TerrainEffects data = get(level);
        RandomSource random = level.getRandom();
        long now = level.getGameTime();
        int revert = Config.TERRAIN_REVERT_TICKS.get();
        BlockPos center = player.getOnPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) continue;
                pos.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                BlockState state = level.getBlockState(pos);
                BlockPos above = pos.above();
                if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) continue;

                // liquids first
                FluidState fluid = state.getFluidState();
                if (type == DragonType.ICE && fluid.is(FluidTags.WATER) && fluid.isSource() && state.is(Blocks.WATER)) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, 60 + random.nextInt(60));
                    continue;
                }
                if (type == DragonType.WATER && fluid.is(FluidTags.LAVA) && fluid.isSource() && state.is(Blocks.LAVA)) {
                    data.convert(level, pos.immutable(), state, Blocks.OBSIDIAN.defaultBlockState(), now + revert + random.nextInt(60));
                    continue;
                }

                long key = pos.asLong();
                Entry existing = data.entries.get(key);
                if (existing != null) {
                    // still standing on it: keep it transformed a bit longer
                    data.entries.put(key, new Entry(existing.original, existing.temp, now + revert + random.nextInt(60)));
                    continue;
                }
                if (!isNaturalGround(level, pos, state)) continue;

                BlockState target = targetFor(type, random);
                if (target == null || state.is(target.getBlock())) continue;
                if (type == DragonType.FOREST && state.is(Blocks.GRASS_BLOCK) && random.nextFloat() < 0.08F
                        && level.isEmptyBlock(above)) {
                    // the forest blooms (permanent, it is only vegetation)
                    BlockState flower = FLOWERS.get(random.nextInt(FLOWERS.size())).defaultBlockState();
                    if (flower.canSurvive(level, above)) level.setBlockAndUpdate(above, flower);
                }
                data.convert(level, pos.immutable(), state, target, now + revert + random.nextInt(60));
            }
        }
    }

    private static BlockState targetFor(DragonType type, RandomSource random) {
        if (type == DragonType.WATER) return CORALS.get(random.nextInt(CORALS.size())).defaultBlockState();
        return type.groundBlock().defaultBlockState();
    }

    private static boolean isNaturalGround(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.hasBlockEntity() || !state.isCollisionShapeFullBlock(level, pos)) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.SAND) || state.is(BlockTags.TERRACOTTA) || state.is(BlockTags.NYLIUM)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.END_STONE)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.CALCITE) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.CLAY) || state.is(Blocks.DIRT_PATH);
    }

    private void convert(ServerLevel level, BlockPos pos, BlockState original, BlockState temp, long revertAt) {
        level.setBlock(pos, temp, Block.UPDATE_ALL);
        entries.put(pos.asLong(), new Entry(original, temp, revertAt));
        if (level.getRandom().nextFloat() < 0.15F) {
            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.3, 0.0, 0.3, 0.0);
        }
        setDirty();
    }

    // ------------------------------------------------------------------------------------------------------

    /** Reverts expired blocks (called every level tick on the server). */
    public static void tickLevel(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) return;
        TerrainEffects data = level.getDataStorage().get(TYPE);
        if (data == null || data.entries.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Long2ObjectMap.Entry<Entry>> it = data.entries.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entry entry = e.getValue();
            if (entry.revertAt > now) continue;
            BlockPos pos = BlockPos.of(e.getLongKey());
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockState(pos).is(entry.temp.getBlock())) {
                level.setBlock(pos, entry.original, Block.UPDATE_ALL);
            }
            it.remove();
            data.setDirty();
        }
    }

    /** A temporary block is being mined: give the original block back instead (no loot exploit). */
    public static boolean protect(ServerLevel level, BlockPos pos) {
        TerrainEffects data = level.getDataStorage().get(TYPE);
        if (data == null) return false;
        Entry entry = data.entries.remove(pos.asLong());
        if (entry == null) return false;
        level.setBlock(pos, entry.original, Block.UPDATE_ALL);
        data.setDirty();
        return true;
    }

    /** Server stopping: put everything back so that the world is always clean. */
    public static void revertAll(ServerLevel level) {
        TerrainEffects data = level.getDataStorage().get(TYPE);
        if (data == null) return;
        Iterator<Long2ObjectMap.Entry<Entry>> it = data.entries.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            BlockPos pos = BlockPos.of(e.getLongKey());
            if (level.isLoaded(pos)) {
                if (level.getBlockState(pos).is(e.getValue().temp.getBlock())) level.setBlock(pos, e.getValue().original, Block.UPDATE_ALL);
                it.remove();
            }
        }
        data.setDirty();
    }

    // ------------------------------------------------------------------------------------------------------

    private static TerrainEffects fromTag(CompoundTag tag) {
        TerrainEffects data = new TerrainEffects();
        ListTag list = tag.getListOrEmpty("entries");
        for (Tag t : list) {
            if (!(t instanceof CompoundTag c)) continue;
            long pos = c.getLongOr("pos", 0L);
            BlockState original = c.read("original", BlockState.CODEC).orElse(null);
            BlockState temp = c.read("temp", BlockState.CODEC).orElse(null);
            if (original != null && temp != null) data.entries.put(pos, new Entry(original, temp, c.getLongOr("at", 0L)));
        }
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        entries.forEach((pos, e) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", pos);
            c.store("original", BlockState.CODEC, e.original);
            c.store("temp", BlockState.CODEC, e.temp);
            c.putLong("at", e.revertAt);
            list.add(c);
        });
        tag.put("entries", list);
        return tag;
    }
}
