package com.yb.dragons.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.ToIntFunction;

/**
 * The claws of the hybrid form. With an empty hand (or any item that is not a tool for the block),
 * the hybrid digs like a netherite pickaxe, shovel or axe enchanted with Efficiency N (N = stage)
 * and Silk Touch. The full dragon has no claws: each form has its use.
 *
 * <p>The breaking speed must be the same on both sides (the client predicts the progress), so the
 * client installs its own lookup of the local player's claw level in {@link #clientLevel}.</p>
 */
public final class DragonClaws {
    /** Set by the client setup: claw level of the local player (the dedicated server never calls it). */
    public static ToIntFunction<Player> clientLevel = p -> 0;

    private static ItemStack @Nullable [] tools;

    private DragonClaws() {}

    private static ItemStack[] tools() {
        if (tools == null) {
            tools = new ItemStack[] {new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_SHOVEL), new ItemStack(Items.NETHERITE_AXE)};
        }
        return tools;
    }

    /** 0 = no claws, else the stage (1..5) = the Efficiency level. */
    public static int level(Player player) {
        if (player.level().isClientSide()) return clientLevel.applyAsInt(player);
        return player instanceof ServerPlayer sp ? DragonPlayerManager.get(sp).clawLevel() : 0;
    }

    /** Netherite base speed + Efficiency N (N² + 1), like the enchanted tool. */
    public static float speed(int level) {
        return 9.0F + level * level + 1.0F;
    }

    /**
     * The netherite tool the claws act as on this block, or null when the claws do not apply: the
     * block needs no pickaxe/shovel/axe, or the player holds a real tool for it (which then works
     * normally, with its own enchantments like Fortune).
     */
    @Nullable
    private static ItemStack toolFor(Player player, BlockState state) {
        if (player.getMainHandItem().getDestroySpeed(state) > 1.0F) return null;
        for (ItemStack tool : tools()) {
            if (tool.getDestroySpeed(state) > 1.0F) return tool;
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------
    // Events (both sides for the speed and the harvest check, server side for the drops)
    // ------------------------------------------------------------------------------------------------------

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        int level = level(player);
        if (level <= 0) return;
        BlockState state = event.getState();
        if (toolFor(player, state) == null) return;
        // the event speed already contains haste, mining fatigue, water and "not on ground": keep them
        float hand = Math.max(1.0F, player.getMainHandItem().getDestroySpeed(state));
        event.setNewSpeed(event.getNewSpeed() / hand * speed(level));
    }

    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.canHarvest()) return;
        Player player = event.getEntity();
        if (level(player) <= 0) return;
        ItemStack tool = toolFor(player, event.getTargetBlock());
        if (tool != null && tool.isCorrectToolForDrops(event.getTargetBlock())) event.setCanHarvest(true);
    }

    /**
     * Silk touch: the block is broken here with an enchanted netherite tool as "destroyed with"
     * item (the same steps as the vanilla game mode), then the vanilla breaking is denied so it
     * does not happen twice. Mined with silk touch, the blocks give no experience (like vanilla).
     */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getResult().isDenied()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (player.preventsBlockDrops()) return; // creative: nothing drops anyway
        if (level(player) <= 0) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack base = toolFor(player, state);
        if (base == null) return;
        if (state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) return;
        if (player.getMainHandItem().onBlockStartBreak(pos, player)) return;

        ItemStack tool = base.copy();
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .ifPresent(silk -> tool.enchant(silk, 1));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean canHarvest = state.canHarvestBlock(level, pos, player);
        boolean removed = state.onDestroyedByPlayer(level, pos, player, canHarvest, level.getFluidState(pos));
        if (removed) state.getBlock().destroy(level, pos, state);
        if (removed && canHarvest) state.getBlock().playerDestroy(level, player, pos, state, blockEntity, tool);
        event.setResult(Result.DENY);
    }
}
