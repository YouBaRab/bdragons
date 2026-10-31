package com.yb.dragons.entity;

import com.yb.dragons.ModRegistry;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.player.BreathHandler;
import com.yb.dragons.player.DragonPowers;
import com.yb.dragons.world.lair.LairBiomes;
import com.yb.dragons.world.lair.LairManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A wild dragon of rank I to V. Wild dragons live in lairs (see {@link LairManager}) placed in the
 * favourite biomes of their race; they fly around their lair and defend it. Defeating one is the
 * "challenge" that lets a player dragon evolve. They drop dragon scales, bones and essences; the rank V
 * dragons also drop an Alpha Scale of their race (see {@link com.yb.dragons.dragon.DragonAlpha}).
 */
public class WildDragon extends Monster implements DragonVisual {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(WildDragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(WildDragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(WildDragon.class, EntityDataSerializers.BOOLEAN);

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean initialized;
    private boolean summoned;
    @Nullable private UUID challenger;
    /** Region key of the lair this dragon lives in (0 = no lair, e.g. summoned by a command). */
    private long lairKey;
    @Nullable private BlockPos lairPos;
    /** Museum exhibit: stands still on its pedestal, cannot be hurt nor pushed, no boss bar. */
    private boolean display;

    public WildDragon(EntityType<? extends WildDragon> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.xpReward = 30;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DragonAttackGoal(this));
        this.goalSelector.addGoal(4, new ReturnToLairGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // rank I-II dragons only fight back; rank III+ defend their territory
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (target, level) -> isChallengerOf(target) || isIntruder(target)));
    }

    /** Territory: rank III protects 20 blocks around its lair, rank IV 28, rank V 36. */
    private boolean isIntruder(LivingEntity target) {
        int rank = getRank();
        if (rank < 3) return false;
        int radius = 20 + (rank - 3) * 8;
        if (lairPos == null) return rank >= 4 && distanceToSqr(target) < 24 * 24;
        double dx = target.getX() - (lairPos.getX() + 0.5), dz = target.getZ() - (lairPos.getZ() + 0.5);
        return dx * dx + dz * dz < radius * radius;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, DragonType.FIRE.ordinal());
        builder.define(DATA_RANK, 1);
        builder.define(DATA_BREATHING, false);
    }

    // ------------------------------------------------------------------------------------------------------

    public void setup(DragonType type, int rank) {
        rank = Mth.clamp(rank, 1, 5);
        this.entityData.set(DATA_TYPE, type.ordinal());
        this.entityData.set(DATA_RANK, rank);
        int i = rank - 1;
        setBase(Attributes.MAX_HEALTH, DragonStage.WILD_HEALTH[i]);
        setBase(Attributes.ATTACK_DAMAGE, DragonStage.WILD_DAMAGE[i]);
        setBase(Attributes.ARMOR, DragonStage.WILD_ARMOR[i]);
        setBase(Attributes.SCALE, DragonStage.values()[i].scaleAttribute());
        setBase(Attributes.FLYING_SPEED, 0.5 + rank * 0.05);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 30 * rank;
        this.initialized = true;
        updateBossBar();
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        var inst = this.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    /** Summoned by a Challenge Horn: always aggressive against the challenger, never despawns. */
    public void setChallenger(@Nullable Player player) {
        this.summoned = true;
        this.challenger = player == null ? null : player.getUUID();
        this.setPersistenceRequired();
        updateBossBar();
    }

    public boolean isChallengerOf(LivingEntity entity) {
        return challenger != null && challenger.equals(entity.getUUID());
    }

    /** This dragon lives in a lair: it stays around it and comes back after a fight. */
    public void setLair(long key, BlockPos pos) {
        this.lairKey = key;
        this.lairPos = pos;
        this.setHomeTo(pos, 28);
        updateBossBar();
    }

    public long getLairKey() {
        return lairKey;
    }

    /** Turns this dragon into a museum exhibit (see {@link #display}). */
    public void setDisplay(boolean display) {
        this.display = display;
        if (display) {
            this.goalSelector.removeAllGoals(g -> true);
            this.targetSelector.removeAllGoals(g -> true);
            this.setPermanentlyInvulnerable(true);
            this.setSilent(true);
            this.setPersistenceRequired();
            this.setNoGravity(false);
        }
        updateBossBar();
    }

    public boolean isDisplay() {
        return display;
    }

    @Override
    public boolean isPushable() {
        return !display && super.isPushable();
    }

    @Nullable
    public BlockPos getLairPos() {
        return lairPos;
    }

    @Override
    public DragonType getDragonType() {
        return DragonType.byOrdinal(this.entityData.get(DATA_TYPE));
    }

    public int getRank() {
        return this.entityData.get(DATA_RANK);
    }

    @Override
    public int getVisualStage() {
        return getRank();
    }

    @Override
    public boolean isBreathing() {
        return this.entityData.get(DATA_BREATHING);
    }

    public void setBreathing(boolean breathing) {
        this.entityData.set(DATA_BREATHING, breathing);
    }

    @Override
    public boolean isDragonSitting() {
        return false;
    }

    @Override
    public float getRenderScale() {
        return DragonStage.values()[getRank() - 1].renderScale();
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.bdragon.wild_dragon.named", getDragonType().displayName(), ROMAN[getRank() - 1]);
    }

    // ------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData groupData) {
        if (!initialized) {
            setup(typeAt(level.getLevel(), blockPosition()), 1);
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    /** Race of a dragon created without a lair (spawn egg...): the race of the biome, or a random one. */
    private DragonType typeAt(ServerLevel level, BlockPos pos) {
        DragonType type = LairBiomes.typeFor(level.getBiome(pos));
        if (type != null) return type;
        return DragonType.SELECTABLE.get(getRandom().nextInt(DragonType.SELECTABLE.size()));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.isAlive()) {
            // dragons always fly (they only fall when they die); exhibits stand on their pedestal
            this.setNoGravity(!display);
            if (this.tickCount % 10 == 0) this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            // only one dragon per lair: an old copy loaded from the disk removes itself
            if (lairKey != 0 && this.tickCount % 40 == 0 && !LairManager.get(level).isCurrentDragon(level, lairKey, getUUID())) {
                this.discard();
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!initialized) setup(typeAt(level, blockPosition()), 1);
    }

    private void updateBossBar() {
        this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setColor(colorFor(getDragonType()));
        // every wild dragon is a boss (not the museum exhibits)
        this.bossEvent.setVisible(!display);
    }

    private static BossEvent.BossBarColor colorFor(DragonType type) {
        return switch (type) {
            case FIRE, NETHER -> BossEvent.BossBarColor.RED;
            case ICE, WATER, STORM -> BossEvent.BossBarColor.BLUE;
            case FOREST -> BossEvent.BossBarColor.GREEN;
            case SUNLIGHT, TERRA -> BossEvent.BossBarColor.YELLOW;
            case MOONLIGHT, WITHER -> BossEvent.BossBarColor.WHITE;
            default -> BossEvent.BossBarColor.PURPLE;
        };
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (display || DragonPowers.isImmune(getDragonType(), source)) return false;
        // a dragon player hits harder the dragons of his rank and below: x1.5 against his own rank, up to x2.5
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            com.yb.dragons.player.DragonPlayerData data = com.yb.dragons.player.DragonPlayerManager.get(player);
            if (data.isDragon()) {
                int lead = data.stage().number() - getRank() + 1;
                if (lead > 0) damage *= Math.min(2.5F, 1 + 0.5F * lead);
            }
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean fireImmune() {
        DragonType t = getDragonType();
        return t == DragonType.FIRE || t == DragonType.NETHER || super.fireImmune();
    }

    @Override
    public void die(DamageSource source) {
        boolean wasAlive = !this.isRemoved() && !this.dead;
        super.die(source);
        this.setNoGravity(false);
        this.setBreathing(false);
        if (wasAlive && lairKey != 0 && this.level() instanceof ServerLevel level) {
            LairManager.get(level).markDefeated(level, lairKey);
        }
    }

    /**
     * Lair dragons go back into their lair (they are removed) when nobody is within 128 blocks,
     * and come back at full health when a player approaches again. Never removed at random.
     */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        if (summoned || this.isPersistenceRequired()) return false;
        return distSqr > 128 * 128;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int rank = getRank();
        this.spawnAtLocation(level, new ItemStack(ModRegistry.DRAGON_SCALE.get(), 1 + rank * 2));
        this.spawnAtLocation(level, new ItemStack(Items.BONE, 2 + rank));
        // the essence of its element: guaranteed from rank IV
        var essence = ModRegistry.ESSENCES.get(getDragonType());
        if (essence != null && getRandom().nextFloat() < 0.25F + 0.2F * rank) {
            this.spawnAtLocation(level, new ItemStack(essence.get()));
        }
        if (rank >= 3) this.spawnAtLocation(level, new ItemStack(Items.GOLD_INGOT, rank * 2));
        if (rank >= 5) {
            this.spawnAtLocation(level, new ItemStack(Items.DIAMOND, 2));
            // a legendary wild dragon always leaves one Alpha Scale of its race (4 of them make an Orb of Power)
            var scale = ModRegistry.ALPHA_SCALES.get(getDragonType());
            if (scale != null) {
                var drop = this.spawnAtLocation(level, new ItemStack(scale.get()));
                if (drop != null) drop.setGlowingTag(true);
            }
        }
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    // --- sounds ---------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 1.7F - getRank() * 0.18F + (getRandom().nextFloat() - 0.5F) * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F + getRank() * 0.25F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // --- save -----------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("DragonType", getDragonType().id());
        output.putInt("Rank", getRank());
        output.putBoolean("Summoned", summoned);
        if (challenger != null) output.putString("Challenger", challenger.toString());
        if (display) output.putBoolean("Display", true);
        if (lairKey != 0 && lairPos != null) {
            output.putLong("Lair", lairKey);
            output.putInt("LairX", lairPos.getX());
            output.putInt("LairY", lairPos.getY());
            output.putInt("LairZ", lairPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        DragonType type = DragonType.byId(input.getStringOr("DragonType", "fire"));
        this.entityData.set(DATA_TYPE, (type == null ? DragonType.FIRE : type).ordinal());
        this.entityData.set(DATA_RANK, Mth.clamp(input.getIntOr("Rank", 1), 1, 5));
        this.summoned = input.getBooleanOr("Summoned", false);
        this.challenger = input.getString("Challenger").map(s -> {
            try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
        }).orElse(null);
        this.initialized = true;
        this.xpReward = 30 * getRank();
        long lair = input.getLongOr("Lair", 0L);
        if (lair != 0) {
            setLair(lair, new BlockPos(input.getIntOr("LairX", 0), input.getIntOr("LairY", 80), input.getIntOr("LairZ", 0)));
        }
        if (input.getBooleanOr("Display", false)) setDisplay(true);
        updateBossBar();
    }

    /** A lair dragon does not chase its target more than 72 blocks away from its lair. */
    boolean isLeashedTo(LivingEntity target) {
        if (lairPos == null) return true;
        double dx = target.getX() - (lairPos.getX() + 0.5), dz = target.getZ() - (lairPos.getZ() + 0.5);
        return dx * dx + dz * dz < 72 * 72;
    }

    /** Flies back above the lair when it wandered too far (after a fight). */
    static class ReturnToLairGoal extends Goal {
        private final WildDragon dragon;

        ReturnToLairGoal(WildDragon dragon) {
            this.dragon = dragon;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos lair = dragon.lairPos;
            return lair != null && dragon.getTarget() == null && dragon.blockPosition().distSqr(lair) > 30 * 30;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos lair = dragon.lairPos;
            return lair != null && dragon.getTarget() == null && dragon.blockPosition().distSqr(lair) > 10 * 10;
        }

        @Override
        public void tick() {
            BlockPos lair = dragon.lairPos;
            if (lair != null) dragon.getMoveControl().setWantedPosition(lair.getX() + 0.5, lair.getY() + 2, lair.getZ() + 0.5, 1.1);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // AI: circle around the target, breathe on it, bite when close
    // ------------------------------------------------------------------------------------------------------

    static class DragonAttackGoal extends Goal {
        private final WildDragon dragon;
        private int breathCooldown = 40;
        private int breathTime;
        private int biteCooldown;
        private float circleAngle;

        DragonAttackGoal(WildDragon dragon) {
            this.dragon = dragon;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = dragon.getTarget();
            return target != null && target.isAlive() && dragon.isLeashedTo(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = dragon.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!dragon.isLeashedTo(target)) {
                // the intruder fled far from the lair: give up and go home
                dragon.setTarget(null);
                return false;
            }
            return !(target instanceof Player p && (p.isCreative() || p.isSpectator()));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            dragon.setBreathing(false);
            breathTime = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = dragon.getTarget();
            if (target == null) return;
            ServerLevel level = (ServerLevel) dragon.level();
            int rank = dragon.getRank();
            double dist = dragon.distanceTo(target);
            float range = 6 + rank * 1.5F;

            // movement: approach, then circle above the target
            circleAngle += 0.035F + rank * 0.004F;
            double radius = 5 + rank * 1.2;
            Vec3 wanted;
            if (dist > range + 6) {
                wanted = target.position().add(0, 4 + rank, 0);
            } else {
                wanted = target.position().add(Math.cos(circleAngle) * radius, 3 + rank * 0.8 + Math.sin(circleAngle * 2) * 1.5,
                        Math.sin(circleAngle) * radius);
            }
            dragon.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, 1.0 + rank * 0.1);
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // breath
            if (breathTime > 0) {
                breathTime--;
                Vec3 toTarget = target.getEyePosition().subtract(dragon.getEyePosition()).normalize();
                Vec3 origin = dragon.getEyePosition().add(toTarget.scale(0.4 + dragon.getRenderScale() * 1.6));
                Vec3 look = target.getEyePosition().subtract(origin).normalize();
                BreathHandler.spawnBreathParticles(level, dragon.getDragonType(), origin, look, (int) range, dragon.tickCount, dragon.getRandom());
                if (breathTime % 5 == 0) {
                    List<LivingEntity> hit = BreathHandler.targetsInCone(level, dragon, origin, look, range, 0.4);
                    for (LivingEntity e : hit) {
                        if (e instanceof WildDragon) continue;
                        DragonPowers.breathHit(level, dragon, e, dragon.getDragonType(), rank, DragonStage.WILD_BREATH[rank - 1]);
                    }
                }
                if (breathTime % 12 == 0) {
                    level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), BreathHandler.breathSound(dragon.getDragonType()),
                            SoundSource.HOSTILE, 1.0F, 0.8F + dragon.getRandom().nextFloat() * 0.3F);
                }
                if (breathTime == 0) {
                    dragon.setBreathing(false);
                    breathCooldown = 60 + dragon.getRandom().nextInt(60) - rank * 6;
                }
            } else if (--breathCooldown <= 0 && dist < range && dragon.hasLineOfSight(target)) {
                breathTime = 30 + rank * 6;
                dragon.setBreathing(true);
            }

            // bite
            if (biteCooldown > 0) biteCooldown--;
            double reach = dragon.getBbWidth() * 0.8 + target.getBbWidth() + 1.5;
            if (biteCooldown == 0 && dist < reach) {
                dragon.doHurtTarget(level, target);
                biteCooldown = 30 - rank * 2;
            }
        }
    }
}
