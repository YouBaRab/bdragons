package com.yb.dragons.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.function.IntPredicate;

/**
 * One hit zone of a giant alpha: the Sylvan Tortoise (forest) or the Tempest Dragon (storm), far bigger than the single
 * box a player can have. The zones follow the body (see {@link com.yb.dragons.player.AlphaHitboxes}): a blow or a
 * projectile that touches one of them hurts the player who is this alpha. They are invisible, never saved, never
 * pushed, and the alpha itself cannot hit them (its crosshair, its arrows and its attacks go through).
 */
public class AlphaHitbox extends Entity {
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(AlphaHitbox.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(AlphaHitbox.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(AlphaHitbox.class, EntityDataSerializers.INT);

    /** Set on the client: true for the entity id of the local player (his own zones are not under his crosshair). */
    public static IntPredicate isLocalPlayer = id -> false;

    public AlphaHitbox(EntityType<? extends AlphaHitbox> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(Entity owner, float width, float height) {
        entityData.set(DATA_OWNER, owner.getId());
        entityData.set(DATA_WIDTH, width);
        entityData.set(DATA_HEIGHT, height);
        refreshDimensions();
    }

    /** New size (the zones of the Tempest Dragon follow its pose): nothing is sent when it does not change. */
    public void resize(float width, float height) {
        if (entityData.get(DATA_WIDTH) == width && entityData.get(DATA_HEIGHT) == height) return;
        entityData.set(DATA_WIDTH, width);
        entityData.set(DATA_HEIGHT, height);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 1.0F);
        builder.define(DATA_HEIGHT, 1.0F);
        builder.define(DATA_OWNER, -1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_WIDTH.equals(accessor) || DATA_HEIGHT.equals(accessor)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(entityData.get(DATA_WIDTH), entityData.get(DATA_HEIGHT));
    }

    public int ownerId() {
        return entityData.get(DATA_OWNER);
    }

    /** The alpha (a player), on both sides. */
    @Nullable
    public Entity owner() {
        int id = ownerId();
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    public void tick() {
        // no physics, no fire, no fluids: the zone only follows its owner (moved by AlphaHitboxes)
        if (!level().isClientSide()) {
            Entity owner = owner();
            if (owner == null || owner.isRemoved() || tickCount > 40 && owner.level() != level()) discard();
        }
    }

    /** The crosshair of the alpha goes through its own zones. */
    @Override
    public boolean isPickable() {
        return !(level().isClientSide() && isLocalPlayer.test(ownerId()));
    }

    @Override
    public boolean canBeHitByProjectile() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** The alpha does not hit itself. */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return attacker.getId() == ownerId();
    }

    /** A blow on the zone is a blow on the alpha. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity owner = owner();
        if (owner == null || !owner.isAlive() || source.getEntity() == owner || source.getDirectEntity() == owner) return false;
        return owner.hurtServer(level, source, amount);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
