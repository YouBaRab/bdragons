package com.yb.dragons.world.lair;

import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The nests of the wild dragons: the structure of the author ({@code data/bdragon/structure/nid_dragon.nbt}), the same
 * for every race and every rank, built where the lair is the first time its chunk is loaded:
 * <ul>
 *   <li>its blocks take the colours of the race: the obsidian (X), the crying obsidian (Y) and the magma (Z) of the
 *       structure are replaced by the three blocks of the race ({@link #palette});</li>
 *   <li>the pink wool marks where the dragon appears (it becomes air), the green wool becomes a chest with the loot of
 *       its rank: like a shipwreck (rank III), a bastion (IV) or an End city (V);</li>
 *   <li>it is always out in the open, like a village: on the ground (never under the water, under the ground nor under
 *       another structure), on the water for the water dragons; in the Nether on the floor of a large open cave.</li>
 * </ul>
 */
public final class NestBuilder {
    public static final Identifier NEST = YBDragons.id("nid_dragon");
    /** The deepest foundation under a nest on uneven ground. */
    static final int FOUNDATION = 8;
    /** Above the nest, up to this height, trees and plants are cleared (it is never under the leaves). */
    static final int CLEAR_ABOVE = 24;

    /** The three blocks of a race: in place of the obsidian (X), the crying obsidian (Y) and the magma (Z). */
    public record Palette(Block x, Block y, Block z) {}

    private NestBuilder() {}

    public static Palette palette(DragonType type) {
        return switch (type) {
            case FIRE -> new Palette(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.MAGMA_BLOCK);
            case ICE -> new Palette(Blocks.SNOW_BLOCK, Blocks.BLUE_ICE, Blocks.PACKED_ICE);
            case FOREST -> new Palette(Blocks.GRASS_BLOCK, Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK);
            case NETHER -> new Palette(Blocks.NETHERRACK, Blocks.BASALT, Blocks.MAGMA_BLOCK);
            case END -> new Palette(Blocks.END_STONE_BRICKS, Blocks.OBSIDIAN, Blocks.PURPUR_BLOCK);
            case SUNLIGHT -> new Palette(Blocks.CUT_SANDSTONE, Blocks.SAND, Blocks.GLASS);
            case MOONLIGHT -> new Palette(Blocks.DEEPSLATE_TILES, Blocks.AMETHYST_BLOCK, Blocks.SEA_LANTERN);
            case WITHER -> new Palette(Blocks.SOUL_SAND, Blocks.COAL_BLOCK, Blocks.OBSIDIAN);
            case STORM -> new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.TUFF_BRICKS, Blocks.PACKED_ICE);
            case TERRA -> new Palette(Blocks.TERRACOTTA, Blocks.RED_SANDSTONE, Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE));
            case WATER -> new Palette(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN);
        };
    }

    /** The loot of the chest of a nest: like a shipwreck (rank III), a bastion (IV), an End city (V). */
    static ResourceKey<LootTable> loot(int rank) {
        if (rank >= 5) return BuiltInLootTables.END_CITY_TREASURE;
        if (rank == 4) return BuiltInLootTables.BASTION_TREASURE;
        return BuiltInLootTables.SHIPWRECK_TREASURE;
    }

    /** The chunks of the nest are loaded (it can be built now). */
    static boolean ready(ServerLevel level, DragonLair lair) {
        for (int dx = -12; dx <= 12; dx += 12) {
            for (int dz = -12; dz <= 12; dz += 12) {
                if (!level.isLoaded(new BlockPos(lair.x + dx, level.getSeaLevel(), lair.z + dz))) return false;
            }
        }
        return true;
    }

    /**
     * Builds the nest of the lair.
     * @return where its dragon appears (the pink wool), or null when there is no room for a nest here
     */
    @Nullable
    static BlockPos build(ServerLevel level, DragonLair lair) {
        Optional<StructureTemplate> found = level.getStructureTemplateManager().get(NEST);
        if (found.isEmpty()) return null;
        StructureTemplate template = found.get();
        Vec3i size = template.getSize();
        int x0 = lair.x - size.getX() / 2, z0 = lair.z - size.getZ() / 2;
        Integer floor = level.dimensionType().hasCeiling() ? caveFloor(level, x0, z0, size) : surface(level, lair, x0, z0, size);
        if (floor == null) return null;
        BlockPos origin = new BlockPos(x0, floor, z0);
        // never under or inside another structure (a village, a ruin...)
        if (level.structureManager().getStructureWithPieceAt(origin.offset(size.getX() / 2, 1, size.getZ() / 2), s -> true).isValid()) {
            return null;
        }
        RandomSource random = level.getRandom();
        Palette palette = palette(lair.type);
        if (!level.dimensionType().hasCeiling()) clearAbove(level, origin, size);
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), random, Block.UPDATE_CLIENTS);

        BlockPos spawn = null;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                for (int dy = 0; dy < size.getY(); dy++) {
                    p.set(x0 + dx, floor + dy, z0 + dz);
                    BlockState s = level.getBlockState(p);
                    BlockState to = null;
                    if (s.is(Blocks.OBSIDIAN)) to = palette.x().defaultBlockState();
                    else if (s.is(Blocks.CRYING_OBSIDIAN)) to = palette.y().defaultBlockState();
                    else if (s.is(Blocks.MAGMA_BLOCK)) to = palette.z().defaultBlockState();
                    else if (s.is(Blocks.WOOL.pick(DyeColor.PINK))) {
                        to = Blocks.AIR.defaultBlockState();
                        spawn = p.immutable();
                    } else if (s.is(Blocks.WOOL.pick(DyeColor.GREEN)) || s.is(Blocks.WOOL.pick(DyeColor.LIME))) {
                        level.setBlock(p, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
                        RandomizableContainer.setBlockEntityLootTable(level, random, p.immutable(), loot(lair.rank));
                        continue;
                    }
                    if (to == null) continue;
                    // sand never floats: where there is nothing under it, smooth sandstone instead
                    if (to.is(Blocks.SAND) && level.getBlockState(p.below()).isAir()) to = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                    level.setBlock(p, to, Block.UPDATE_CLIENTS);
                }
                // the foundation: the floor of the nest rests on the ground
                BlockState bottom = level.getBlockState(new BlockPos(x0 + dx, floor, z0 + dz));
                if (!bottom.isAir()) {
                    for (int d = 1; d <= FOUNDATION; d++) {
                        p.set(x0 + dx, floor - d, z0 + dz);
                        BlockState under = level.getBlockState(p);
                        if (!under.isAir() && !under.canBeReplaced() && under.getFluidState().isEmpty()) break;
                        level.setBlock(p, palette.x().defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return spawn != null ? spawn : origin.offset(size.getX() / 2, 1, size.getZ() / 2);
    }

    /**
     * The height of the floor of the nest on the surface: the lowest ground of its footprint (the water surface for
     * the water dragons and for the nests on a lake), the trees and the leaves not counted.
     */
    @Nullable
    private static Integer surface(ServerLevel level, DragonLair lair, int x0, int z0, Vec3i size) {
        int low = Integer.MAX_VALUE;
        for (int dx = 0; dx < size.getX(); dx += Math.max(1, size.getX() - 1) / 2) {
            for (int dz = 0; dz < size.getZ(); dz += Math.max(1, size.getZ() - 1) / 2) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x0 + dx, z0 + dz);
                // trunks are not ground
                BlockPos top = new BlockPos(x0 + dx, h - 1, z0 + dz);
                while (top.getY() > level.getMinY() && level.getBlockState(top).is(BlockTags.LOGS)) top = top.below();
                low = Math.min(low, top.getY());
            }
        }
        if (low <= level.getMinY() + 1 || low >= level.getMaxY() - 16) return null;
        return low;
    }

    /** In the Nether: the floor of an open cave, from the top. */
    @Nullable
    private static Integer caveFloor(ServerLevel level, int x0, int z0, Vec3i size) {
        int top = Math.min(level.getMaxY() - 8, 110);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top; y >= level.getMinY() + 32; y--) {
            boolean ok = true;
            for (int dx = 0; dx < size.getX() && ok; dx += Math.max(1, size.getX() - 1) / 2) {
                for (int dz = 0; dz < size.getZ() && ok; dz += Math.max(1, size.getZ() - 1) / 2) {
                    p.set(x0 + dx, y, z0 + dz);
                    BlockState ground = level.getBlockState(p);
                    if (ground.isAir() || !ground.getFluidState().isEmpty()) ok = false;
                    for (int h = 1; h <= size.getY() + 2 && ok; h++) {
                        if (!level.getBlockState(p.setY(y + h)).isAir()) ok = false;
                    }
                }
            }
            if (ok) return y;
        }
        return null;
    }

    /** Trees and plants above the nest are cleared: it is out in the open. */
    private static void clearAbove(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= size.getX(); dx++) {
            for (int dz = -1; dz <= size.getZ(); dz++) {
                for (int dy = size.getY(); dy < CLEAR_ABOVE; dy++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || (s.canBeReplaced() && s.getFluidState().isEmpty())) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** The dimension of the nests of a race (for the codex). */
    public static boolean inNether(DragonType type) {
        return type == DragonType.FIRE || type == DragonType.NETHER || type == DragonType.WITHER;
    }
}
