package com.yb.dragons.item;

import com.yb.dragons.dragon.DragonType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Elemental essence: offered to the altar during the 3rd quest, it decides the race forever. */
public class DragonEssenceItem extends Item {
    private final DragonType type;

    public DragonEssenceItem(DragonType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public DragonType getDragonType() {
        return type;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(type.description().copy().withStyle(s -> s.withColor(type.textColor())));
        builder.accept(Component.translatable("item.bdragon.essence.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
