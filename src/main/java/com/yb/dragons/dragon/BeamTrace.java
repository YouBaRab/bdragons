package com.yb.dragons.dragon;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Where the Beam of Life stops: the first solid block or water surface on its way, or the edge of the
 * chunks loaded for the player (his view distance), whichever comes first. The beam goes from block to
 * block (a voxel walk) and asks the {@link ChunkFilter} before reading any block, so it never reads (nor
 * loads) a chunk that is not loaded. Used by the server (effects) and by the clients (drawing).
 */
public final class BeamTrace {
    /** The chunks the beam may enter. */
    @FunctionalInterface
    public interface ChunkFilter {
        boolean has(int chunkX, int chunkZ);
    }

    /** One block on the way of the beam, entered at the distance {@code t}; returns true to stop the walk there. */
    @FunctionalInterface
    public interface Visitor {
        boolean visit(int x, int y, int z, double t);
    }

    /**
     * @param end    where the beam stops
     * @param block  the block it touched (null when it stopped at the edge of the loaded chunks)
     * @param water  the block is a water surface
     * @param capped the beam was stopped by the edge of the loaded chunks
     */
    public record Result(Vec3 end, @Nullable BlockPos block, boolean water, boolean capped) {}

    private BeamTrace() {}

    /** Longest beam for a view distance (chunks): to the far corner of the square of loaded chunks. */
    public static double maxLength(int viewChunks) {
        return (viewChunks + 1) * 16 * Math.sqrt(2) + 16;
    }

    /**
     * Visits, in order, every block crossed by the segment from {@code start} along {@code dir} (normalized)
     * up to {@code maxLength}.
     * @return the distance where the visitor stopped, or -1 when the end was reached
     */
    public static double walk(Vec3 start, Vec3 dir, double maxLength, Visitor visitor) {
        int x = Mth.floor(start.x), y = Mth.floor(start.y), z = Mth.floor(start.z);
        int stepX = dir.x > 0 ? 1 : dir.x < 0 ? -1 : 0;
        int stepY = dir.y > 0 ? 1 : dir.y < 0 ? -1 : 0;
        int stepZ = dir.z > 0 ? 1 : dir.z < 0 ? -1 : 0;
        double tDeltaX = stepX == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dir.x);
        double tDeltaY = stepY == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dir.y);
        double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dir.z);
        double tMaxX = stepX == 0 ? Double.MAX_VALUE : (stepX > 0 ? (x + 1 - start.x) : (start.x - x)) * tDeltaX;
        double tMaxY = stepY == 0 ? Double.MAX_VALUE : (stepY > 0 ? (y + 1 - start.y) : (start.y - y)) * tDeltaY;
        double tMaxZ = stepZ == 0 ? Double.MAX_VALUE : (stepZ > 0 ? (z + 1 - start.z) : (start.z - z)) * tDeltaZ;
        double t = 0;
        while (t <= maxLength) {
            if (visitor.visit(x, y, z, t)) return t;
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX;
                tMaxX += tDeltaX;
                x += stepX;
            } else if (tMaxY < tMaxZ) {
                t = tMaxY;
                tMaxY += tDeltaY;
                y += stepY;
            } else {
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                z += stepZ;
            }
        }
        return -1;
    }

    /** Reads the block of a voxel of the way: the end of the beam there, or null when the beam goes through. */
    @FunctionalInterface
    public interface BlockReader {
        @Nullable Result read(int x, int y, int z, double t);
    }

    public static Result trace(BlockGetter level, ChunkFilter view, Vec3 start, Vec3 direction, double maxLength) {
        Vec3 dir = direction.normalize();
        Vec3 far = start.add(dir.scale(maxLength));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        return trace(view, level.getMinY(), level.getMaxY(), start, dir, maxLength, (x, y, z, t) -> {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) return null;
            FluidState fluid = state.getFluidState();
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) return new Result(start.add(dir.scale(t)), pos.immutable(), true, false);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) return null;
            BlockHitResult r = shape.clip(start, far, pos);
            return r == null ? null : new Result(r.getLocation(), pos.immutable(), false, false);
        });
    }

    /**
     * The walk of the beam: for each block of the way, the chunk is asked first (a block of a chunk that is not
     * loaded is never read: the beam stops just before it), then the block is read.
     * @param dir normalized direction
     */
    public static Result trace(ChunkFilter view, int minY, int maxY, Vec3 start, Vec3 dir, double maxLength, BlockReader reader) {
        Result[] hit = new Result[1];
        double stop = walk(start, dir, maxLength, (x, y, z, t) -> {
            if (!view.has(x >> 4, z >> 4)) {
                hit[0] = new Result(start.add(dir.scale(Math.max(0, t - 0.05))), null, false, true);
                return true;
            }
            if (y < minY || y > maxY) return false;
            Result r = reader.read(x, y, z, t);
            if (r == null) return false;
            hit[0] = r;
            return true;
        });
        return stop >= 0 && hit[0] != null ? hit[0] : new Result(start.add(dir.scale(maxLength)), null, false, false);
    }
}
