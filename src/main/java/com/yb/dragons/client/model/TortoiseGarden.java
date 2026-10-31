package com.yb.dragons.client.model;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The little forest of the back of the Sylvan Tortoise: real blocks of the game drawn at half size
 * (one cell = half a block) on the moss of its shell. Four trees (an oak, the tallest birch whose top is at
 * exactly 12 blocks, a flowering azalea and a small spruce), two wild nests and a hive full of honey, grass,
 * ferns, flowers, mushrooms, a mossy rock pile, and lianas hanging down the sides of the shell.
 *
 * <p>The list is built once; positions are in blocks in the frame of the tortoise (x to its left, y up,
 * z forward), at the lowest corner of each cell.</p>
 */
public final class TortoiseGarden {
    /** Size of a cell (blocks). */
    public static final float CELL = 0.5F;
    /** Top of the moss of the shell: the ground cells stand on it (their top is at 8.0). */
    public static final float GROUND = 7.5F;

    /** One small block: its state, the lowest corner of its cell (blocks) and its size (blocks). */
    public record Cell(BlockState state, float x, float y, float z, float size) {}

    private static final List<Cell> CELLS = Collections.unmodifiableList(build());

    private TortoiseGarden() {}

    public static List<Cell> cells() {
        return CELLS;
    }

    // ------------------------------------------------------------------------------------------------------

    private static final class Builder {
        final List<Cell> list = new ArrayList<>();
        final Set<Long> used = new HashSet<>();

        static long key(int gx, int gy, int gz) {
            return ((long) (gx + 64) << 20) | ((long) (gy + 64) << 10) | (gz + 64);
        }

        boolean free(int gx, int gy, int gz) {
            return !used.contains(key(gx, gy, gz));
        }

        /** A cell of the grid: gx, gz from the centre, gy = 0 for the ground layer. */
        void put(BlockState state, int gx, int gy, int gz) {
            if (!used.add(key(gx, gy, gz))) return;
            list.add(new Cell(state, gx * CELL, GROUND + gy * CELL, gz * CELL, CELL));
        }

        void free(BlockState state, float x, float y, float z) {
            list.add(new Cell(state, x, y, z, CELL));
        }
    }

    static boolean onShell(int gx, int gz) {
        float cx = (gx + 0.5F) / 7.0F, cz = (gz + 0.5F + 0.25F) / 7.75F;
        return cx * cx + cz * cz <= 1.0F;
    }

    private static BlockState leaves(net.minecraft.world.level.block.Block block) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(LeavesBlock.PERSISTENT)) s = s.setValue(LeavesBlock.PERSISTENT, true);
        return s;
    }

    private static List<Cell> build() {
        Builder b = new Builder();
        RandomSource random = RandomSource.create(0x5E1A7E);

        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState(), birchLog = Blocks.BIRCH_LOG.defaultBlockState();
        BlockState spruceLog = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState oak = leaves(Blocks.OAK_LEAVES), birch = leaves(Blocks.BIRCH_LEAVES), spruce = leaves(Blocks.SPRUCE_LEAVES);
        BlockState azalea = leaves(Blocks.AZALEA_LEAVES), flowering = leaves(Blocks.FLOWERING_AZALEA_LEAVES);

        // --- the trees (trunk first, so that the leaves never replace it) --------------------------------------------
        // oak, front left: top at 11.5 blocks
        tree(b, oakLog, oak, null, -4, 3, 4, new int[][] {{4, 2}, {5, 2}, {6, 1}, {7, 0}});
        // birch, back right, the tallest: the top of its crown is at exactly 12 blocks
        tree(b, birchLog, birch, null, 3, -5, 5, new int[][] {{5, 2}, {6, 2}, {7, 1}, {8, 0}});
        // flowering azalea, back left
        tree(b, oakLog, azalea, flowering, -4, -4, 2, new int[][] {{3, 1}, {4, 1}, {5, 0}});
        // small spruce, front right
        tree(b, spruceLog, spruce, null, 3, 3, 4, new int[][] {{2, 1}, {3, 0}, {4, 1}, {5, 0}, {6, -1}});

        // --- the hives: two wild nests under the crowns and a hive full of honey on the ground ------------------------
        BlockState nest = Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS);
        b.put(nest.setValue(BeehiveBlock.FACING, Direction.EAST), -3, 3, 3);
        b.put(nest.setValue(BeehiveBlock.FACING, Direction.WEST), 2, 4, -4);
        b.put(Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS)
                .setValue(BeehiveBlock.FACING, Direction.SOUTH), 5, 1, 2);

        // --- a mossy rock pile at the back ------------------------------------------------------------------------
        b.put(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), -1, 1, -6);
        b.put(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 0, 1, -6);
        b.put(Blocks.COBBLESTONE.defaultBlockState(), -1, 1, -7);
        b.put(Blocks.MOSS_BLOCK.defaultBlockState(), 0, 2, -6);
        b.put(Blocks.MOSS_CARPET.defaultBlockState(), -1, 2, -6);

        // --- the ground: grass, with moss and podzol under the trees ------------------------------------------------
        for (int gx = -7; gx <= 6; gx++) {
            for (int gz = -8; gz <= 7; gz++) {
                if (!onShell(gx, gz)) continue;
                boolean shade = near(gx, gz, -4, 3, 2) || near(gx, gz, -4, -4, 1);
                // the moss creeps over the edge of the little forest
                boolean edge = !onShell(gx + 1, gz) || !onShell(gx - 1, gz) || !onShell(gx, gz + 1) || !onShell(gx, gz - 1);
                int r = random.nextInt(100);
                BlockState ground = edge ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : shade ? (r < 45 ? Blocks.PODZOL.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState())
                        : r < 14 ? Blocks.MOSS_BLOCK.defaultBlockState() : r < 17 ? Blocks.ROOTED_DIRT.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                b.put(ground, gx, 0, gz);
            }
        }

        // --- plants on the free ground ----------------------------------------------------------------------------
        BlockState[] flowers = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.ALLIUM.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState(),
                Blocks.AZURE_BLUET.defaultBlockState(), Blocks.RED_TULIP.defaultBlockState(), Blocks.PINK_TULIP.defaultBlockState(),
                Blocks.BLUE_ORCHID.defaultBlockState()};
        for (int gx = -7; gx <= 6; gx++) {
            for (int gz = -8; gz <= 7; gz++) {
                if (!onShell(gx, gz) || !b.free(gx, 1, gz)) continue;
                int r = random.nextInt(100);
                boolean shade = near(gx, gz, -4, 3, 2) || near(gx, gz, 3, -5, 2) || near(gx, gz, -4, -4, 1);
                if (shade && r < 8) {
                    b.put((r < 4 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(), gx, 1, gz);
                } else if (r < 34) {
                    b.put(Blocks.SHORT_GRASS.defaultBlockState(), gx, 1, gz);
                } else if (r < 42) {
                    b.put(Blocks.FERN.defaultBlockState(), gx, 1, gz);
                } else if (r < 54 && !shade) {
                    b.put(flowers[random.nextInt(flowers.length)], gx, 1, gz);
                }
            }
        }

        // --- lianas hanging down the sides of the shell, moss on its lower step ----------------------------------------
        BlockState vineW = Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true);
        BlockState vineE = Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true);
        BlockState vineN = Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true);
        BlockState vineS = Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true);
        // each liana covers the upper step of the shell, then the golden rim below it
        for (float z : new float[] {-2.25F, 0.75F, 2.75F}) {
            liana(b, vineW, 4.5F, z, 6.0F, 2);
            liana(b, vineW, 5.0F, z, 4.75F, 2);
        }
        for (float z : new float[] {-1.25F, 2.0F}) {
            liana(b, vineE, -5.0F, z, 6.0F, 2);
            liana(b, vineE, -5.5F, z, 4.75F, 2);
        }
        for (float x : new float[] {-2.75F, 2.0F}) {
            liana(b, vineN, x, 4.75F, 6.0F, 2);
            liana(b, vineN, x, 5.25F, 4.75F, 1);
        }
        for (float x : new float[] {-1.25F, 1.75F}) {
            liana(b, vineS, x, -5.5F, 6.0F, 2);
            liana(b, vineS, x, -6.0F, 4.75F, 1);
        }
        BlockState carpet = Blocks.MOSS_CARPET.defaultBlockState();
        for (float z = -4.5F; z < 4.25F; z += 0.5F) {
            if (random.nextInt(3) > 0) b.free(carpet, 3.75F, 6.0F, z);
            if (random.nextInt(3) > 0) b.free(carpet, -4.25F, 6.0F, z);
        }
        for (float x = -3.5F; x < 3.5F; x += 0.5F) {
            if (random.nextInt(3) > 0) b.free(carpet, x, 6.0F, 4.0F);
            if (random.nextInt(3) > 0) b.free(carpet, x, 6.0F, -4.75F);
        }
        return b.list;
    }

    private static boolean near(int gx, int gz, int cx, int cz, int r) {
        return Math.abs(gx - cx) <= r && Math.abs(gz - cz) <= r;
    }

    /**
     * A tree: a trunk of {@code trunk} cells (from the ground layer + 1), then the layers of leaves
     * ({gy, radius}: radius 2 = 5 x 5 without the corners, 1 = 3 x 3, 0 = a cross).
     */
    private static void tree(Builder b, BlockState log, BlockState leaves, BlockState other, int gx, int gz, int trunk, int[][] layers) {
        for (int y = 1; y <= trunk; y++) b.put(log, gx, y, gz);
        int i = 0;
        for (int[] layer : layers) {
            int gy = layer[0], r = layer[1];
            for (int dx = -Math.max(1, r); dx <= Math.max(1, r); dx++) {
                for (int dz = -Math.max(1, r); dz <= Math.max(1, r); dz++) {
                    boolean in = switch (r) {
                        case 2 -> !(Math.abs(dx) == 2 && Math.abs(dz) == 2);
                        case 1 -> true;
                        case 0 -> Math.abs(dx) + Math.abs(dz) <= 1;
                        default -> dx == 0 && dz == 0;
                    };
                    if (!in) continue;
                    BlockState s = other != null && (i++ % 3 == 0) ? other : leaves;
                    b.put(s, gx + dx, gy, gz + dz);
                }
            }
        }
    }

    /** A piece of liana of {@code length} cells hanging against a side of the shell, from the height {@code top}. */
    private static void liana(Builder b, BlockState vine, float x, float z, float top, int length) {
        for (int k = 0; k < length; k++) b.free(vine, x, top - (k + 1) * CELL, z);
    }
}
