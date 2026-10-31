package com.yb.dragons.client.screen;

import com.yb.dragons.client.AlphaCinematic;
import com.yb.dragons.client.ClientDragonData;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Transparent screen of the alpha cinematic: it holds the mouse and the keys during the ritual (Escape
 * skips it), and draws the black cinema bands, the captions, the flashes and the big "ALPHA" title.
 * The world keeps playing behind it (not a pause screen, no blur).
 */
public class AlphaCinematicScreen extends Screen {
    private final DragonType type;

    public AlphaCinematicScreen(@Nullable DragonType type) {
        super(Component.translatable("screen.bdragon.alpha"));
        this.type = type == null ? DragonType.FIRE : type;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void removed() {
        super.removed();
        AlphaCinematic.onScreenClosed();
    }

    /** No blur, no dark background: the cinematic is the world itself. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        var player = this.minecraft.player;
        if (player == null) return;
        ClientDragonData.Info info = ClientDragonData.get(player.getId());
        float age = info == null ? -1 : info.ritualAge(partialTick);
        if (age < 0) return;
        int w = this.width, h = this.height;

        // --- flashes: the orb breaks, then the explosion ---
        float flash = 0;
        if (age >= DragonAlpha.ORB_BREAK && age < DragonAlpha.ORB_BREAK + 6) flash = 0.35F * (1 - (age - DragonAlpha.ORB_BREAK) / 6F);
        if (age >= DragonAlpha.CLIMAX && age < DragonAlpha.CLIMAX + 16) flash = 0.9F * (1 - (age - DragonAlpha.CLIMAX) / 16F);
        if (flash > 0.01F) g.fill(0, 0, w, h, argb(flash, 0xFFFFFF));

        // --- the black bands of the cinema ---
        float bands = DragonAlpha.phase(age, 0, 14) * (1 - DragonAlpha.phase(age, DragonAlpha.DURATION - 16, DragonAlpha.DURATION));
        int band = Math.round(h * 0.11F * bands);
        if (band > 0) {
            g.fill(0, 0, w, band, 0xFF000000);
            g.fill(0, h - band, w, h, 0xFF000000);
        }

        int light = 0xFFFFFF;
        int race = type.textColor();
        // --- captions at the bottom, in the black band ---
        caption(g, Component.translatable("cinematic.bdragon.alpha.1"), age, 8, 62, h - band / 2 - 4, light);
        caption(g, Component.translatable("cinematic.bdragon.alpha.2"), age, 64, 98, h - band / 2 - 4, light);
        caption(g, Component.translatable("cinematic.bdragon.alpha.3"), age, 128, 166, h - band / 2 - 4, race);

        // --- the title of the new alpha ---
        if (age >= DragonAlpha.CLIMAX + 2 && age < DragonAlpha.LAND_END + 12) {
            float in = DragonAlpha.phase(age, DragonAlpha.CLIMAX + 2, DragonAlpha.CLIMAX + 10);
            float out = 1 - DragonAlpha.phase(age, DragonAlpha.LAND_END, DragonAlpha.LAND_END + 12);
            float alpha = in * out;
            if (alpha > 0.02F) {
                Component title = Component.translatable("cinematic.bdragon.alpha.title").withStyle(ChatFormatting.BOLD);
                float grow = 1 + (1 - in) * 0.6F;
                drawScaled(g, title, w / 2F, h * 0.24F, 4.0F * grow, argb(alpha, race));
                drawScaled(g, type.displayName(), w / 2F, h * 0.24F + 38, 1.6F, argb(alpha, light));
            }
        }

        // --- skip hint ---
        if (age < DragonAlpha.LAND_END) {
            Component skip = Component.translatable("cinematic.bdragon.skip");
            g.text(this.font, skip, w - this.font.width(skip) - 6, h - 12, 0xA0B0B0B0, false);
        }
    }

    /** A line of text that fades in and out between {@code from} and {@code to} (ticks of the ritual). */
    private void caption(GuiGraphicsExtractor g, Component text, float age, float from, float to, int y, int rgb) {
        if (age < from || age > to) return;
        float alpha = DragonAlpha.phase(age, from, from + 8) * (1 - DragonAlpha.phase(age, to - 8, to));
        if (alpha < 0.03F) return;
        Component styled = text.copy().withStyle(ChatFormatting.ITALIC);
        g.centeredText(this.font, styled, this.width / 2, y, argb(alpha, rgb));
    }

    private void drawScaled(GuiGraphicsExtractor g, Component text, float x, float y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.centeredText(this.font, text, 0, -this.font.lineHeight / 2, color);
        g.pose().popMatrix();
    }

    private static int argb(float alpha, int rgb) {
        int a = Mth.clamp(Math.round(alpha * 255), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
