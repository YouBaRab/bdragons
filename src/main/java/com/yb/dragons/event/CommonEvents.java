package com.yb.dragons.event;

import com.yb.dragons.ModRegistry;
import com.yb.dragons.command.DragonCommand;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.entity.AlphaHitbox;
import com.yb.dragons.player.AlphaHitboxes;
import com.yb.dragons.player.AlphaRitual;
import com.yb.dragons.player.LifeBeam;
import com.yb.dragons.player.NetherDomain;
import com.yb.dragons.player.SunStrike;
import com.yb.dragons.player.StormCall;
import com.yb.dragons.player.DragonClaws;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import com.yb.dragons.player.DragonPowers;
import com.yb.dragons.world.AbandonedVillages;
import com.yb.dragons.world.TerrainEffects;
import com.yb.dragons.world.lair.LairManager;
import com.yb.dragons.world.museum.MuseumBuilder;
import com.yb.dragons.world.museum.MuseumData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

import java.util.function.Consumer;

/** Every game event the mod listens to on the logical server (Forge EventBus 7 style). */
public final class CommonEvents {
    private CommonEvents() {}

    public static void register() {
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(ModRegistry.WILD_DRAGON.get(), WildDragon.createAttributes().build());
        });

        RegisterCommandsEvent.BUS.addListener(event -> DragonCommand.register(event.getDispatcher()));

        // --- ticks ---------------------------------------------------------------------------------------------
        TickEvent.PlayerTickEvent.Post.BUS.addListener(event -> {
            if (event.player() instanceof ServerPlayer sp) {
                DragonPlayerManager.tick(sp);
                AbandonedVillages.tick(sp);
            }
        });
        TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
            if (event.level() instanceof ServerLevel level) {
                TerrainEffects.tickLevel(level);
                if (level.getGameTime() % 20 == 0) {
                    LairManager.tick(level);
                    MuseumData.tick(level);
                }
            }
        });
        // the museum world type builds its museum the first time the world is opened
        ServerStartedEvent.BUS.addListener(event -> MuseumBuilder.onServerStarted(event.getServer()));
        ServerStoppingEvent.BUS.addListener(event -> {
            // an alpha ritual in progress ends now (the player keeps his alpha and the floating orb is removed)
            for (ServerPlayer sp : event.getServer().getPlayerList().getPlayers()) {
                AlphaRitual.end(sp, DragonPlayerManager.get(sp));
                LifeBeam.stop(sp, DragonPlayerManager.get(sp));
                StormCall.stop(sp, DragonPlayerManager.get(sp));
                NetherDomain.stop(sp, DragonPlayerManager.get(sp));
                SunStrike.stop(sp, DragonPlayerManager.get(sp));
            }
            AlphaHitboxes.removeAll();
            for (ServerLevel level : event.getServer().getAllLevels()) TerrainEffects.revertAll(level);
        });
        // the floating orb of a ritual is never kept in a save
        EntityJoinLevelEvent.BUS.addListener((EntityJoinLevelEvent event) ->
                !event.getLevel().isClientSide()
                        && ((event.loadedFromDisk() && AlphaRitual.isStaleOrb(event.getEntity()))
                        // nobody lives in an abandoned village
                        || AbandonedVillages.blocksSpawn(event.getEntity())));

        // --- player lifecycle --------------------------------------------------------------------------------
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer sp) {
                DragonPlayerManager.fullSync(sp);
                DragonPlayerManager.giveCodexIfNeeded(sp);
                LairManager.fullSync(sp);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer sp) {
                AlphaRitual.end(sp, DragonPlayerManager.get(sp));
                LifeBeam.stop(sp, DragonPlayerManager.get(sp));
                StormCall.stop(sp, DragonPlayerManager.get(sp));
                NetherDomain.stop(sp, DragonPlayerManager.get(sp));
                SunStrike.stop(sp, DragonPlayerManager.get(sp));
                AlphaHitboxes.remove(sp.getUUID());
                DragonPlayerManager.get(sp).breathing = false;
                DragonPlayerManager.get(sp).gliding = false;
                for (ServerLevel level : sp.level().getServer().getAllLevels()) LairManager.get(level).forget(sp.getUUID());
            }
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer sp) {
                AlphaRitual.end(sp, DragonPlayerManager.get(sp));
                LifeBeam.stop(sp, DragonPlayerManager.get(sp));
                StormCall.stop(sp, DragonPlayerManager.get(sp));
                NetherDomain.stop(sp, DragonPlayerManager.get(sp));
                SunStrike.stop(sp, DragonPlayerManager.get(sp));
                DragonPlayerManager.get(sp).gliding = false;
                DragonPlayerManager.fullSync(sp);
                LairManager.fullSync(sp);
                if (!event.isEndConquered()) sp.setHealth(sp.getMaxHealth());
            }
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer sp) {
                AlphaRitual.end(sp, DragonPlayerManager.get(sp));
                LifeBeam.stop(sp, DragonPlayerManager.get(sp));
                StormCall.stop(sp, DragonPlayerManager.get(sp));
                NetherDomain.stop(sp, DragonPlayerManager.get(sp));
                SunStrike.stop(sp, DragonPlayerManager.get(sp));
                DragonPlayerManager.fullSync(sp);
                LairManager.fullSync(sp);
            }
        });
        PlayerEvent.StartTracking.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
                DragonPlayerManager.sendTo(watcher, target);
            }
        });

        // the arrows (and every projectile) of the giant alphas go through their own hit zones
        ProjectileImpactEvent.BUS.addListener((ProjectileImpactEvent event) -> {
            if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof AlphaHitbox part
                    && event.getProjectile().getOwner() != null && event.getProjectile().getOwner().getId() == part.ownerId()) {
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            }
        });

        // --- combat ------------------------------------------------------------------------------------------
        LivingAttackEvent.BUS.addListener((LivingAttackEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return false;
            DragonPlayerData data = DragonPlayerManager.get(player);
            // nothing can hurt a dragon during the awakening of the alpha
            if (data.inRitual()) return true;
            DragonType type = data.type();
            if (!data.hasPowers() || type == null) return false;
            if (DragonPowers.isImmune(type, event.getSource())) return true;
            int s = data.stage().number();
            boolean projectile = event.getSource().is(DamageTypeTags.IS_PROJECTILE);
            // ender dragons blink away from projectiles
            if (projectile && type == DragonType.END && s >= 3 && player.getRandom().nextFloat() < 0.3F) {
                double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 12;
                double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 12;
                player.randomTeleport(x, player.getY() + 2, z, true, s2 -> false);
                return true;
            }
            return false;
        });

        LivingHurtEvent.BUS.addListener((LivingHurtEvent event) -> {
            LivingEntity victim = event.getEntity();
            if (victim instanceof ServerPlayer player && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
                DragonPlayerData data = DragonPlayerManager.get(player);
                if (data.hasPowers()) DragonPowers.onDragonHurt(player, data, attacker);
            }
            if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim) {
                DragonPlayerData data = DragonPlayerManager.get(attacker);
                if (data.hasPowers()) DragonPowers.onDragonAttack(attacker, data, victim, event.getAmount());
            }
            return false;
        });

        LivingDeathEvent.BUS.addListener((LivingDeathEvent event) -> {
            LivingEntity dead = event.getEntity();
            if (dead.level().isClientSide()) return false;
            if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return false;
            if (dead instanceof WildDragon dragon) {
                DragonPlayerManager.onWildDragonDefeated(killer, dragon.getRank());
            } else if (dead instanceof net.minecraft.world.entity.monster.Enemy) {
                DragonPlayerData data = DragonPlayerManager.get(killer);
                if (data.isDragon()) {
                    int power = Math.max(1, Math.round(dead.getMaxHealth() / 10F));
                    DragonPlayerManager.addPower(killer, power);
                }
            }
            return false;
        });

        // --- temporary terrain blocks cannot be farmed -------------------------------------------------------
        // (Forge only looks at the result of this event: DENY, not the cancellation, keeps the block)
        BlockEvent.BreakEvent.BUS.addListener(Priority.HIGH, (Consumer<BlockEvent.BreakEvent>) event -> {
            if (event.getLevel() instanceof ServerLevel level && TerrainEffects.protect(level, event.getPos())) {
                event.setResult(Result.DENY);
            }
        });

        // --- claws of the hybrid form: mining speed, harvest and silk touch ----------------------------------
        PlayerEvent.BreakSpeed.BUS.addListener(DragonClaws::onBreakSpeed);
        PlayerEvent.HarvestCheck.BUS.addListener(DragonClaws::onHarvestCheck);
        BlockEvent.BreakEvent.BUS.addListener(Priority.LOWEST, (Consumer<BlockEvent.BreakEvent>) DragonClaws::onBlockBreak);
    }
}
