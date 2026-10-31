package com.yb.dragons.player;

import com.yb.dragons.Config;
import com.yb.dragons.dragon.AlphaNether;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.world.Terraform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The power of the alpha of the Nether (key B, dragon form, recharge of 3 minutes): its Territory.
 * <ol>
 *   <li>The incantation ({@link AlphaNether#CAST_TICKS}, 5 seconds): the demon raises its right hand, its body bends to
 *       its left and a circle of fire is drawn above it (client side).</li>
 *   <li>Then its territory rises out of the ground, in {@link AlphaNether#FORM_TICKS} (10 seconds): on a disc of
 *       {@link AlphaNether#RADIUS} blocks around it, the natural ground becomes basalt, blackstone, netherrack, soul soil,
 *       magma and nether wart blocks, and sharp peaks like stalagmites grow up to the height {@link AlphaNether#PEAK_Y}
 *       (never columns: each one gets thinner up to its point). This change of the world is permanent; only natural
 *       ground is changed and only air or plants are filled (never a building), spawn protection is respected, and it
 *       can be turned off in the configuration.</li>
 *   <li>At the same moment an acid rain starts and follows the demon for {@link AlphaNether#RAIN_TICKS} (30 seconds): every
 *       second it burns every creature under it (but the demon and its own pets; the invisible hit zones of the giant alphas are not living) for half a heart, through the armour
 *       (15 hearts in all).</li>
 * </ol>
 * The power stops when the demon dies, leaves the world or changes dimension (the terrain already made stays).
 */
public final class NetherDomain {
    /** The territory being built for each player: blocks ordered by the moment they appear (0..1). */
    private static final class Plan {
        final List<BlockPos> pos = new ArrayList<>();
        final List<BlockState> state = new ArrayList<>();
        final List<Float> order = new ArrayList<>();
        final List<BlockPos> peaks = new ArrayList<>();
        int done;

        void add(BlockPos p, BlockState s, float o) {
            pos.add(p);
            state.add(s);
            order.add(o);
        }

        /** Sorts the blocks by their moment. */
        void sort() {
            Integer[] idx = new Integer[pos.size()];
            for (int i = 0; i < idx.length; i++) idx[i] = i;
            java.util.Arrays.sort(idx, (a, b) -> Float.compare(order.get(a), order.get(b)));
            List<BlockPos> p = new ArrayList<>(idx.length);
            List<BlockState> s = new ArrayList<>(idx.length);
            List<Float> o = new ArrayList<>(idx.length);
            for (int i : idx) {
                p.add(pos.get(i));
                s.add(state.get(i));
                o.add(order.get(i));
            }
            pos.clear();
            pos.addAll(p);
            state.clear();
            state.addAll(s);
            order.clear();
            order.addAll(o);
        }
    }

    private static final Map<UUID, Plan> PLANS = new HashMap<>();
    /** Number of peaks, the first ones reaching {@link AlphaNether#PEAK_Y}. */
    static final int PEAKS = 9, HIGHEST = 3;
    /** No peak closer than this to the demon (it must not be caught inside). */
    static final double KEEP_AWAY = 6.0;

    private NetherDomain() {}

    /** @return true when the power started */
    public static boolean start(ServerPlayer player, DragonPlayerData data) {
        if (!data.isDemon() || data.inRitual() || data.domaining()) return false;
        ServerLevel level = player.level();
        data.domainTicks = 0;
        data.domainStart = level.getGameTime();
        data.breathing = false;
        data.markAll();
        PLANS.remove(player.getUUID());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 3.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.5F, 0.7F);
        player.sendSystemMessage(Component.translatable("message.bdragon.alpha_power.territory", AlphaNether.RAIN_TICKS / 20)
                .withStyle(s -> s.withColor(DragonType.NETHER.textColor())), true);
        return true;
    }

    public static void stop(ServerPlayer player, DragonPlayerData data) {
        PLANS.remove(player.getUUID());
        if (!data.domaining()) return;
        data.domainTicks = -1;
        data.domainStart = -1;
        data.markAll();
    }

    public static void tick(ServerPlayer player, DragonPlayerData data) {
        if (!data.domaining()) return;
        if (data.type() != DragonType.NETHER || !data.isAlpha() || player.isDeadOrDying() || player.isSpectator()) {
            stop(player, data);
            return;
        }
        int t = data.domainTicks;
        ServerLevel level = player.level();
        RandomSource random = level.getRandom();

        // the incantation
        if (t < AlphaNether.CAST_TICKS) {
            if (t % 10 == 0) {
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 20, 2.5, 0.3, 2.5, 0.02);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 6, player.getZ(), 12, 2.0, 1.0, 2.0, 0.01);
            }
            if (t == AlphaNether.CAST_TICKS - 20) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 3.0F, 0.5F);
            }
        }

        // the end of the incantation: the ground rises and the acid rain falls
        int formed = t - AlphaNether.CAST_TICKS;
        if (formed == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 3.0F, 0.6F);
            if (Config.ALPHA_NETHER_TERRAFORM.get()) PLANS.put(player.getUUID(), plan(level, player, random));
        }
        if (formed >= 0 && formed <= AlphaNether.FORM_TICKS) {
            Plan plan = PLANS.get(player.getUUID());
            if (plan != null) grow(level, player, plan, (formed + 1) / (float) AlphaNether.FORM_TICKS, random, formed);
        }
        if (formed >= 0 && formed < AlphaNether.RAIN_TICKS && formed % 20 == 0) acid(level, player);

        data.domainTicks++;
        if (data.domainTicks >= AlphaNether.DURATION) stop(player, data);
    }

    // ------------------------------------------------------------------------------------------------------
    // The acid rain
    // ------------------------------------------------------------------------------------------------------

    /** Every creature under the rain (a cylinder around the demon) loses half a heart, through the armour. */
    private static void acid(ServerLevel level, ServerPlayer player) {
        double r = AlphaNether.RADIUS;
        AABB box = new AABB(player.getX() - r, player.getY() - 16, player.getZ() - r, player.getX() + r, player.getY() + 32, player.getZ() + r);
        DamageSource source = player.damageSources().indirectMagic(player, player);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (e instanceof WildDragon d && d.isDisplay()) continue;
            if (e instanceof OwnableEntity pet && pet.getOwner() == player) continue;
            double dx = e.getX() - player.getX(), dz = e.getZ() - player.getZ();
            if (dx * dx + dz * dz > r * r) continue;
            // under a roof the rain does not reach it
            if (!level.canSeeSky(e.blockPosition().above())) continue;
            if (e.hurtServer(level, source, AlphaNether.ACID_DAMAGE)) {
                level.sendParticles(ParticleTypes.ITEM_SLIME, e.getX(), e.getY(0.8), e.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.5F, 1.6F);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // The territory
    // ------------------------------------------------------------------------------------------------------

    /** The ground under the demon (it may be flying), or its own height when there is none near. */
    private static int groundUnder(ServerLevel level, BlockPos start) {
        BlockPos.MutableBlockPos p = start.mutable();
        for (int i = 0; i < 64 && p.getY() > level.getMinY(); i++) {
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !s.canBeReplaced()) return p.getY();
            p.move(Direction.DOWN);
        }
        return start.getY() - 1;
    }

    /** The natural ground of a column near the height y0, or {@link Integer#MIN_VALUE} (a building, water...). */
    private static int surface(ServerLevel level, int x, int y0, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = y0 + 8; y >= y0 - 10; y--) {
            if (y <= level.getMinY() || y >= level.getMaxY() - 1) continue;
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty())) continue;
            return natural(s) ? y : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean natural(BlockState s) {
        return s.is(Terraform.TERRAFORMABLE) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MYCELIUM)
                || s.is(Blocks.PODZOL) || s.is(Blocks.BASALT) || s.is(Blocks.BLACKSTONE) || s.is(Blocks.MAGMA_BLOCK);
    }

    /** May this block become a block of the territory: air and plants (peaks), natural ground (the floor). */
    private static boolean replaceable(ServerLevel level, ServerPlayer player, BlockPos p, boolean ground) {
        if (!level.isLoaded(p) || !level.getWorldBorder().isWithinBounds(p) || !level.mayInteract(player, p)) return false;
        BlockState s = level.getBlockState(p);
        if (s.isAir()) return true;
        if (s.canBeReplaced() && !s.getFluidState().is(FluidTags.LAVA) && !s.getFluidState().is(FluidTags.WATER)) return true;
        return ground && natural(s);
    }

    /** The floor of the Nether: mostly basalt and blackstone, with netherrack, soul soil, magma and nether wart. */
    private static BlockState floor(RandomSource random) {
        int n = random.nextInt(100);
        if (n < 34) return Blocks.BASALT.defaultBlockState();
        if (n < 54) return Blocks.BLACKSTONE.defaultBlockState();
        if (n < 74) return Blocks.NETHERRACK.defaultBlockState();
        if (n < 84) return Blocks.SOUL_SOIL.defaultBlockState();
        if (n < 93) return Blocks.MAGMA_BLOCK.defaultBlockState();
        return Blocks.NETHER_WART_BLOCK.defaultBlockState();
    }

    /** The rock of a peak: basalt (standing), blackstone, netherrack, magma at its foot. */
    private static BlockState rock(RandomSource random, float height, boolean core) {
        if (height < 0.12F && random.nextInt(4) == 0) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (core) return Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        int n = random.nextInt(100);
        if (n < 45) return Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        if (n < 80) return Blocks.BLACKSTONE.defaultBlockState();
        if (n < 92) return Blocks.NETHERRACK.defaultBlockState();
        return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    }

    /** Plans the whole territory around the demon (the blocks are placed little by little afterwards). */
    private static Plan plan(ServerLevel level, ServerPlayer player, RandomSource random) {
        Plan plan = new Plan();
        BlockPos center = player.blockPosition();
        int cx = center.getX(), cz = center.getZ();
        int base = groundUnder(level, center);
        int r = AlphaNether.RADIUS;
        int[][] ground = new int[2 * r + 1][2 * r + 1];
        // the floor: the top of the natural ground, spreading out from the demon
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                ground[dx + r][dz + r] = Integer.MIN_VALUE;
                if (dx * dx + dz * dz > r * r + r) continue;
                int x = cx + dx, z = cz + dz;
                if (!level.isLoaded(new BlockPos(x, base, z))) continue;
                int y = surface(level, x, base, z);
                if (y == Integer.MIN_VALUE) continue;
                ground[dx + r][dz + r] = y;
                float o = 0.18F * Mth.sqrt(dx * dx + dz * dz) / r;
                for (int k = 0; k < 2; k++) {
                    BlockPos p = new BlockPos(x, y - k, z);
                    if (replaceable(level, player, p, true)) plan.add(p, floor(random), o);
                }
                // the plants of the surface burn away
                BlockPos above = new BlockPos(x, y + 1, z);
                BlockState top = level.getBlockState(above);
                if (!top.isAir() && top.canBeReplaced() && top.getFluidState().isEmpty() && level.mayInteract(player, above)) {
                    plan.add(above, Blocks.AIR.defaultBlockState(), o);
                }
            }
        }
        // the peaks: sharp stalagmites, the highest ones reaching PEAK_Y
        int made = 0;
        for (int attempt = 0; attempt < 60 && made < PEAKS; attempt++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double dist = KEEP_AWAY + random.nextDouble() * (r - 2 - KEEP_AWAY);
            int px = cx + (int) Math.round(Math.cos(a) * dist), pz = cz + (int) Math.round(Math.sin(a) * dist);
            int gx = px - cx + r, gz = pz - cz + r;
            if (gx < 0 || gz < 0 || gx > 2 * r || gz > 2 * r) continue;
            int gy = ground[gx][gz];
            if (gy == Integer.MIN_VALUE) continue;
            boolean close = false;
            for (BlockPos q : plan.peaks) {
                if ((q.getX() - px) * (q.getX() - px) + (q.getZ() - pz) * (q.getZ() - pz) < 25) close = true;
            }
            if (close) continue;
            int top;
            if (gy + 20 >= AlphaNether.PEAK_Y) {
                top = gy + 12 + random.nextInt(18);
            } else if (made < HIGHEST) {
                top = AlphaNether.PEAK_Y;
            } else {
                top = gy + 14 + random.nextInt(Math.max(1, AlphaNether.PEAK_Y - 10 - gy - 14));
            }
            top = Math.min(top, level.getMaxY() - 2);
            float radius = 2.5F + random.nextFloat() * 2.0F;
            if (top - gy > 50) radius += 1.0F;
            peak(level, player, plan, random, px, gy, pz, top, radius, a);
            plan.peaks.add(new BlockPos(px, top, pz));
            made++;
        }
        plan.sort();
        return plan;
    }

    /** One stalagmite: a cone that leans a little outwards, its rock wavy, thinner and thinner up to its point. */
    private static void peak(ServerLevel level, ServerPlayer player, Plan plan, RandomSource random, int px, int gy, int pz, int top,
                             float radius, double outwards) {
        int height = top - gy;
        double leanX = Math.cos(outwards) * (1.5 + random.nextDouble() * 2), leanZ = Math.sin(outwards) * (1.5 + random.nextDouble() * 2);
        double phase = random.nextDouble() * Mth.TWO_PI;
        for (int h = -2; h <= height; h++) {
            float u = Math.max(0, h) / (float) height;
            double rr = radius * Math.pow(1 - u, 1.35) + 0.4;
            // waves on its sides: never a smooth column
            rr *= 1 + 0.12 * Math.sin(h * 0.45 + phase);
            double ox = px + leanX * u, oz = pz + leanZ * u;
            int ir = (int) Math.ceil(rr);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double x = Math.floor(ox) + dx, z = Math.floor(oz) + dz;
                    double d2 = (x + 0.5 - ox) * (x + 0.5 - ox) + (z + 0.5 - oz) * (z + 0.5 - oz);
                    if (d2 > rr * rr) continue;
                    BlockPos p = new BlockPos((int) x, gy + h, (int) z);
                    if (!replaceable(level, player, p, h <= 0)) continue;
                    // the peaks grow all together during the 10 seconds, from their foot to their point
                    plan.add(p, rock(random, u, d2 < rr * rr * 0.25), 0.12F + 0.88F * Math.max(0, h + 2) / (height + 2F));
                }
            }
        }
    }

    /** Places the blocks of the territory whose moment has come; dust and sounds where the peaks rise. */
    private static void grow(ServerLevel level, ServerPlayer player, Plan plan, float progress, RandomSource random, int tick) {
        int n = plan.pos.size();
        while (plan.done < n && plan.order.get(plan.done) <= progress) {
            BlockPos p = plan.pos.get(plan.done);
            BlockState s = plan.state.get(plan.done);
            plan.done++;
            // the world may have changed since the plan: still only air, plants and natural ground
            if (!s.isAir() && !replaceable(level, player, p, true)) continue;
            level.setBlock(p, s, Block.UPDATE_CLIENTS);
        }
        if (tick % 5 == 0) {
            for (BlockPos peak : plan.peaks) {
                double y = peak.getY() - (peak.getY() - player.getY()) * (1 - progress);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, peak.getX() + 0.5, y, peak.getZ() + 0.5, 3, 0.6, 0.4, 0.6, 0.02);
                level.sendParticles(ParticleTypes.LAVA, peak.getX() + 0.5, y, peak.getZ() + 0.5, 1, 0.5, 0.3, 0.5, 0);
            }
        }
        if (tick % 8 == 0 && !plan.peaks.isEmpty()) {
            BlockPos peak = plan.peaks.get(random.nextInt(plan.peaks.size()));
            level.playSound(null, peak.getX(), player.getY(), peak.getZ(), random.nextBoolean() ? SoundEvents.BASALT_PLACE : SoundEvents.DEEPSLATE_PLACE,
                    SoundSource.BLOCKS, 3.0F, 0.5F + random.nextFloat() * 0.2F);
        }
    }
}
