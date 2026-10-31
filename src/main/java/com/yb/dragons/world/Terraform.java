package com.yb.dragons.world;

import com.yb.dragons.YBDragons;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The forest of the Beam of Life (alpha of the forest): where the beam touches the ground, the ground
 * becomes grass and a forest grows at once, like with an endless bone meal: full grown trees, thick grass,
 * flowers and ripe crops. This change of the world is permanent.
 *
 * <p>Only natural ground is changed (the block tag {@code bdragon:terraformable}: dirt, sand, gravel,
 * natural stone, netherrack, end stone...), never the blocks of a building. Spawn protection and the world
 * border are respected, and nothing is ever read or written in a chunk that is not loaded for the player
 * (see {@link View}).</p>
 */
public final class Terraform {
    /** Ground that the beam turns into grass (data driven, can be changed by a data pack). */
    public static final TagKey<Block> TERRAFORMABLE = TagKey.create(Registries.BLOCK, YBDragons.id("terraformable"));

    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.AZURE_BLUET,
            Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP,
            Blocks.BLUE_ORCHID};
    private static final Block[] TALL_FLOWERS = {Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC, Blocks.SUNFLOWER};
    private static final Block[] CROPS = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
    /** Trees of the forest of the tortoise, with their sapling (weights out of 100). */
    private static final TreeGrower[] TREES = {TreeGrower.OAK, TreeGrower.BIRCH, TreeGrower.SPRUCE, TreeGrower.AZALEA, TreeGrower.CHERRY};
    private static final Block[] SAPLINGS = {Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.AZALEA, Blocks.CHERRY_SAPLING};
    private static final int[] TREE_WEIGHTS = {42, 30, 12, 10, 6};

    /** No tree closer than this to the tortoise (a trunk growing inside its box would trap it). */
    private static final double TREE_KEEP_AWAY = 7.0;

    private Terraform() {}

    /**
     * The chunks a player has loaded: the square of his view distance around his chunk, and only the chunks
     * really loaded on the server. {@link #has(int, int)} never loads anything.
     */
    public record View(ServerLevel level, int centerX, int centerZ, int radius) {
        public boolean hasChunk(int chunkX, int chunkZ) {
            return Math.abs(chunkX - centerX) <= radius && Math.abs(chunkZ - centerZ) <= radius && level.hasChunk(chunkX, chunkZ);
        }

        public boolean has(int x, int z) {
            return hasChunk(x >> 4, z >> 4);
        }

        /** Every chunk of the square [x-r, x+r] x [z-r, z+r] is loaded and in view. */
        public boolean hasArea(int x, int z, int r) {
            return has(x - r, z - r) && has(x + r, z - r) && has(x - r, z + r) && has(x + r, z + r);
        }
    }

    /**
     * Grows the forest on a disc around {@code center}.
     * @param done   columns already done by this beam (they are not rolled again)
     * @param budget trees that may still grow during this tick
     * @return the trees grown
     */
    public static int grow(ServerLevel level, ServerPlayer player, View view, BlockPos center, int radius, RandomSource random,
                           LongSet done, int budget) {
        int trees = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + radius) continue;
                int x = center.getX() + dx, z = center.getZ() + dz;
                // the column and its neighbours (block updates read them) must be loaded
                if (!view.hasArea(x, z, 1)) continue;
                long key = BlockPos.asLong(x, 0, z);
                if (done.contains(key)) continue;
                done.add(key);
                int surface = findSurface(level, pos, x, center.getY(), z);
                if (surface == Integer.MIN_VALUE) continue;
                pos.set(x, surface, z);
                if (!level.mayInteract(player, pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    waterColumn(level, pos.immutable(), random);
                    continue;
                }
                if (column(level, player, view, pos.immutable(), random, trees < budget)) trees++;
            }
        }
        return trees;
    }

    /**
     * The top of the natural ground of a column, near the height where the beam touched (or a water surface).
     * @return the y of the ground block, or {@link Integer#MIN_VALUE} when there is none (a building, a cliff...)
     */
    private static int findSurface(ServerLevel level, BlockPos.MutableBlockPos pos, int x, int y0, int z) {
        for (int y = y0 + 3; y >= y0 - 4; y--) {
            if (y < level.getMinY() || y >= level.getMaxY()) continue; // (the block above must exist too)
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || isPlant(state)) continue;
            BlockState above = level.getBlockState(pos.above());
            if (!(above.isAir() || isPlant(above))) return Integer.MIN_VALUE;
            if (state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource()) return above.isAir() ? y : Integer.MIN_VALUE;
            if (isGround(state)) return y;
            // another solid block: a building, a road... nothing grows there
            return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isGround(BlockState state) {
        return state.is(TERRAFORMABLE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK);
    }

    /** Grass, flowers, snow layers, dead bushes...: what the forest replaces. */
    private static boolean isPlant(BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** @return true when a tree grew */
    private static boolean column(ServerLevel level, ServerPlayer player, View view, BlockPos ground, RandomSource random, boolean treeAllowed) {
        BlockState state = level.getBlockState(ground);
        BlockPos above = ground.above();
        if (!state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.MOSS_BLOCK)) {
            level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
        // the beam is the bone meal: a new plant every time (the old grass and flowers are replaced)
        BlockState top = level.getBlockState(above);
        if (!top.isAir() && isPlant(top)) {
            if (top.getBlock() instanceof DoublePlantBlock) return false;
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, above.getX() + 0.5, above.getY() + 0.3, above.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0);

        int roll = random.nextInt(100);
        if (roll < 6 && treeAllowed && canHaveTree(player, view, ground)) {
            return tree(level, above, random);
        }
        if (roll < 10) {
            crops(level, ground, random);
        } else if (roll < 25) {
            level.setBlock(above, FLOWERS[random.nextInt(FLOWERS.length)].defaultBlockState(), Block.UPDATE_ALL);
        } else if (roll < 27) {
            tallPlant(level, above, TALL_FLOWERS[random.nextInt(TALL_FLOWERS.length)]);
        } else if (roll < 60) {
            level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
        } else if (roll < 67) {
            level.setBlock(above, Blocks.FERN.defaultBlockState(), Block.UPDATE_ALL);
        } else if (roll < 73) {
            tallPlant(level, above, random.nextInt(3) == 0 ? Blocks.LARGE_FERN : Blocks.TALL_GRASS);
        }
        return false;
    }

    private static boolean canHaveTree(ServerPlayer player, View view, BlockPos ground) {
        double dx = ground.getX() + 0.5 - player.getX(), dz = ground.getZ() + 0.5 - player.getZ();
        if (dx * dx + dz * dz < TREE_KEEP_AWAY * TREE_KEEP_AWAY) return false;
        // a tree can be up to 5 blocks wide around its trunk: its whole area must be loaded
        return view.hasArea(ground.getX(), ground.getZ(), 6);
    }

    /** A full grown tree at once, from a sapling (a sapling stays if there is not enough room). */
    private static boolean tree(ServerLevel level, BlockPos pos, RandomSource random) {
        int pick = random.nextInt(100), i = 0;
        while (pick >= TREE_WEIGHTS[i]) pick -= TREE_WEIGHTS[i++];
        BlockState sapling = SAPLINGS[i].defaultBlockState();
        level.setBlock(pos, sapling, Block.UPDATE_ALL);
        boolean grown = TREES[i].growTree(level, level.getChunkSource().getGenerator(), pos, sapling, random);
        if (grown) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, true, false, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 30, 1.5, 2.0, 1.5, 0);
        }
        return grown;
    }

    /** A small field of ripe crops (on farmland, kept moist by the crops). */
    private static void crops(ServerLevel level, BlockPos ground, RandomSource random) {
        Block crop = CROPS[random.nextInt(CROPS.length)];
        level.setBlock(ground, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_ALL);
        BlockState ripe = crop instanceof CropBlock c ? c.getStateForAge(c.getMaxAge()) : crop.defaultBlockState();
        level.setBlock(ground.above(), ripe, Block.UPDATE_ALL);
    }

    private static void tallPlant(ServerLevel level, BlockPos pos, Block block) {
        if (!level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        DoublePlantBlock.placeAt(level, block.defaultBlockState(), pos, Block.UPDATE_ALL);
    }

    /** On water: lily pads. */
    private static void waterColumn(ServerLevel level, BlockPos water, RandomSource random) {
        BlockPos above = water.above();
        if (random.nextInt(4) == 0 && level.getBlockState(above).isAir()) {
            level.setBlock(above, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, above.getX() + 0.5, above.getY() + 0.2, above.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0);
        }
    }
}
