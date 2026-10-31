package com.yb.dragons.item;

import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.player.AlphaRitual;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Orb of Power: forged at the altar from 4 Alpha Scales and a Nether Star. A legendary dragon of the
 * same race consumes it (use it) to awaken as the Alpha of his race: he is forced into the dragon form
 * and the awakening ritual begins. One orb is enough forever; the others can be given to other players.
 */
public class AlphaOrbItem extends Item {
    private final DragonType type;

    public AlphaOrbItem(DragonType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public DragonType getDragonType() {
        return type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        if (!AlphaRitual.start(sp, type)) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.bdragon.alpha_orb.tooltip_1", type.displayName())
                .withStyle(s -> s.withColor(type.textColor())));
        builder.accept(Component.translatable("item.bdragon.alpha_orb.tooltip_2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
