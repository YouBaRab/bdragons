package com.yb.dragons.client.hud;

import com.yb.dragons.client.ClientDragonData;
import com.yb.dragons.client.ClientGlide;
import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.AlphaNether;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Small panel at the top left. Each value has its own line (label on the left, bar on the right)
 * so that nothing overlaps, whatever the GUI scale:
 * race and stage, form, energy, wings, roar, power and the missing challenge.
 */
public final class DragonHud {
    private static final int W = 128;

    private DragonHud() {}

    public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) return;
        DragonPlayerData data = ClientDragonData.own();
        DragonType type = data.type();
        if (type == null) return;
        DragonStage stage = data.stage();
        DragonForm form = data.form();
        Font font = mc.font;
        int color = 0xFF000000 | type.textColor();

        // --- content, measured first to size the panel ---
        // second line: "Stade IV · Hybride" (the race name alone on the first line, so that nothing overlaps)
        Component stageText = data.isAlpha()
                ? Component.translatable("hud.bdragon.alpha_form", form.displayName())
                : Component.translatable("hud.bdragon.stage_form", roman(stage.number()), form.displayName());
        Component energy = Component.translatable("hud.bdragon.energy");
        // the tortoise has no wings: its clouds carry it down
        Component wings = Component.translatable(ClientGlide.isGliding() ? (data.isTortoise() ? "hud.bdragon.clouds" : "hud.bdragon.gliding")
                : "hud.bdragon.wings");
        Component roar = Component.translatable("hud.bdragon.roar");
        Component alphaPower = Component.translatable(data.beaming() ? "hud.bdragon.life_beam"
                : data.storming() ? "hud.bdragon.storm" : data.domaining() ? "hud.bdragon.territory" : "hud.bdragon.alpha_power");
        int labelW = Math.max(Math.max(font.width(energy), font.width(alphaPower)), Math.max(font.width(wings), font.width(roar))) + 5;

        boolean powers = form.hasPowers();
        boolean showWings = form == DragonForm.DRAGON && stage.limitedFlight() || ClientGlide.isGliding();
        boolean showRoar = powers && stage.hasRoar();
        // the alpha power: shown in the dragon form only, but it keeps recharging in the hybrid form
        boolean showAlpha = data.showsAlphaPowerBar();
        List<FormattedCharSequence> challenge = List.of();
        DragonStage next = stage.isMax() ? null : stage.next();
        if (next != null && data.power() >= next.powerRequired() && data.maxDefeatedRank() < next.challengeRank()) {
            challenge = font.split(Component.translatable("hud.bdragon.challenge", roman(next.challengeRank())), W);
        }

        int lines = 2 + 2 + (powers ? 1 : 0) + (showWings ? 1 : 0) + (showRoar ? 1 : 0) + (showAlpha ? 1 : 0) + challenge.size();
        int x = 6, y = 6;
        int h = lines * 10 + 4;
        g.fill(x - 3, y - 3, x + W + 3, y + h, 0xA0000000);
        g.fill(x - 3, y - 3, x + W + 3, y - 2, color);

        // race, then stage and form
        g.text(font, type.displayName(), x, y, color, true);
        y += 10;
        g.text(font, stageText, x, y, 0xFFE8D8B0, true);
        y += 11;

        if (powers) {
            row(g, font, energy, x, y, labelW, data.energy / data.stats().maxEnergy(), 0xFFFF9A2E, 0xFF5A2A00);
            y += 10;
        }
        if (showWings) {
            float wingValue = ClientGlide.isGliding() && !stage.limitedFlight() ? 1 : data.flightStamina / (float) DragonPlayerManager.MAX_STAMINA;
            row(g, font, wings, x, y, labelW, wingValue, 0xFF7FD3FF, 0xFF0D3550);
            y += 10;
        }
        if (showRoar) {
            float ready = 1.0F - data.roarCooldown / (float) data.stats().roarCooldown();
            row(g, font, roar, x, y, labelW, ready, ready >= 1 ? 0xFFFF5050 : 0xFF884040, 0xFF301010);
            y += 10;
        }
        if (showAlpha) {
            int c = 0xFF000000 | type.textColor();
            if (data.beaming()) {
                // the Beam of Life is firing: what is left of it
                float left = 1.0F - data.beamTicks / (float) AlphaForest.BEAM_DURATION;
                row(g, font, alphaPower, x, y, labelW, left, 0xFFD8FF9A, 0xFF203010);
            } else if (data.storming()) {
                // the Storm is raging: what is left of it
                float left = 1.0F - data.stormTicks / (float) AlphaStorm.STORM_DURATION;
                row(g, font, alphaPower, x, y, labelW, left, 0xFF8EF0FF, 0xFF0A2440);
            } else if (data.domaining()) {
                // the Territory: what is left of the incantation and of the acid rain
                float left = 1.0F - data.domainTicks / (float) AlphaNether.DURATION;
                row(g, font, alphaPower, x, y, labelW, left, 0xFFFF7A2A, 0xFF301008);
            } else {
                float ready = data.alphaPowerReady();
                row(g, font, alphaPower, x, y, labelW, ready, ready >= 1 ? c : (c & 0xFFFFFF) | 0x90000000, 0xFF302010);
            }
            y += 10;
        }

        // power towards the next stage: the numbers on one line, the bar under them
        if (next == null) {
            boolean alpha = data.isAlpha();
            int c = alpha ? 0xFF000000 | type.textColor() : 0xFFE0B0FF;
            g.text(font, Component.translatable(alpha ? "hud.bdragon.alpha" : "hud.bdragon.legendary"), x, y, c, true);
            y += 10;
            bar(g, x, y, W, 1, c, 0xFF301040);
        } else {
            Component powerText = Component.translatable("hud.bdragon.power", data.power(), next.powerRequired());
            g.text(font, powerText, x, y, 0xFFD8B8FF, true);
            y += 10;
            float from = stage.powerRequired();
            float progress = (data.power() - from) / (next.powerRequired() - from);
            bar(g, x, y, W, progress, challenge.isEmpty() ? 0xFFB070FF : 0xFFFF4040, 0xFF301040);
        }
        y += 6;
        for (FormattedCharSequence line : challenge) {
            g.text(font, line, x, y, 0xFFFF8080, true);
            y += 10;
        }
    }

    private static void row(GuiGraphicsExtractor g, Font font, Component label, int x, int y, int labelW, float value, int fg, int bg) {
        g.text(font, label, x, y, 0xFFFFFFFF, true);
        int bx = x + labelW;
        bar(g, bx, y + 1, W - labelW, value, fg, bg);
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float fraction, int fg, int bg) {
        fraction = Math.max(0, Math.min(1, fraction));
        g.fill(x, y + 2, x + w, y + 6, bg);
        g.fill(x, y + 2, x + Math.round(w * fraction), y + 6, fg);
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
