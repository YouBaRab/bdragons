package com.yb.dragons.world;

import com.mojang.serialization.Codec;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.YBDragons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.material.Fluids;

import java.util.HashSet;
import java.util.Set;

/**
 * The abandoned villages ({@code data/bdragon/worldgen/structure/abandoned_village_*.json}): the ruined villages of
 * vanilla (the zombie villages), but empty, as if all the villagers had become dragons and left. Nobody lives there
 * (no villager, no zombie villager, no golem, no cat) and in the middle, in place of the bell or of the fountain,
 * stands a dragon altar.
 * <p>
 * The altar is put the first time a player walks into the village (the structure itself comes from the vanilla
 * pieces); the villages already done are kept in a small save.
 */
public final class AbandonedVillages extends SavedData {
    public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE, YBDragons.id("abandoned_villages"));
    public static final Codec<AbandonedVillages> CODEC = CompoundTag.CODEC.xmap(AbandonedVillages::fromTag, AbandonedVillages::toTag);
    public static final SavedDataType<AbandonedVillages> TYPE =
            new SavedDataType<>(YBDragons.id("abandoned_villages"), AbandonedVillages::new, CODEC, null);

    /** The villages (start chunk) that already have their altar. */
    private final Set<Long> done = new HashSet<>();

    public AbandonedVillages() {}

    public static AbandonedVillages get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Nobody lives in an abandoned village: the villagers (also turned into zombies), the golems, the cats. */
    public static boolean isInhabitant(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof ZombieVillager
                || entity instanceof IronGolem || entity instanceof Cat;
    }

    /** An inhabitant that would appear in an abandoned village does not. */
    public static boolean blocksSpawn(Entity entity) {
        if (!isInhabitant(entity) || !(entity.level() instanceof ServerLevel level)) return false;
        if (!level.isLoaded(entity.blockPosition())) return false;
        return level.structureManager().getStructureWithPieceAt(entity.blockPosition(), TAG).isValid();
    }

    /** Every 2 seconds for each player: the village he walks into gets its altar. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 40 != 0 || !(player.level() instanceof ServerLevel level)) return;
        BlockPos at = player.blockPosition();
        if (!level.isLoaded(at)) return;
        StructureStart start = level.structureManager().getStructureWithPieceAt(at, TAG);
        if (!start.isValid() || start.getPieces().isEmpty()) return;
        AbandonedVillages data = get(level);
        long key = start.getChunkPos().pack();
        if (data.done.contains(key)) return;
        BoundingBox box = start.getPieces().get(0).getBoundingBox();
        if (!level.isLoaded(new BlockPos(box.minX(), box.minY(), box.minZ()))
                || !level.isLoaded(new BlockPos(box.maxX(), box.minY(), box.maxZ()))) return;
        placeAltar(level, box);
        data.done.add(key);
        data.setDirty();
    }

    /** In the centre piece: the bell and the water go, the altar takes the place of the bell (or of the fountain). */
    private static void placeAltar(ServerLevel level, BoundingBox box) {
        BlockPos bell = null;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.BELL)) {
                        bell = p.immutable();
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (s.getFluidState().is(Fluids.WATER) || s.getFluidState().is(Fluids.FLOWING_WATER)) {
                        // the dry fountain
                        level.setBlock(p, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        BlockPos altar;
        if (bell != null) {
            altar = bell;
        } else {
            int cx = (box.minX() + box.maxX()) / 2, cz = (box.minZ() + box.maxZ()) / 2;
            altar = new BlockPos(cx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz), cz);
        }
        // it stands on something
        if (level.getBlockState(altar.below()).canBeReplaced()) {
            level.setBlock(altar.below(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(altar, ModRegistry.DRAGON_ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLongArray("done", done.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    private static AbandonedVillages fromTag(CompoundTag tag) {
        AbandonedVillages v = new AbandonedVillages();
        tag.getLongArray("done").ifPresent(arr -> {
            for (long k : arr) v.done.add(k);
        });
        return v;
    }
}
