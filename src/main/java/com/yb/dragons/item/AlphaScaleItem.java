package com.yb.dragons.item;

import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Alpha Scale: dropped by every wild dragon of rank V, of its race. Four of them and a Nether Star,
 * offered at the Dragon Altar, become the Orb of Power of that race.
 */
public class AlphaScaleItem extends Item {
    private final DragonType type;

    public AlphaScaleItem(DragonType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public DragonType getDragonType() {
        return type;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.bdragon.alpha_scale.tooltip_1", type.displayName())
                .withStyle(s -> s.withColor(type.textColor())));
        builder.accept(Component.translatable("item.bdragon.alpha_scale.tooltip_2", DragonAlpha.SCALES_PER_ORB)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
